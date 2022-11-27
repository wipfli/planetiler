package com.onthegomap.planetiler.examples;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.Planetiler;
import com.onthegomap.planetiler.Profile;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.reader.osm.OsmElement;
import com.onthegomap.planetiler.reader.osm.OsmSourceFeature;
import com.onthegomap.planetiler.util.ZoomFunction;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

/**
 * Generates tiles with a raw copy of OSM data in a single "osm" layer at one zoom level, similar to
 * <a href="http://osmlab.github.io/osm-qa-tiles/">OSM QA Tiles</a>.
 * <p>
 * Nodes are mapped to points and ways are mapped to polygons or linestrings, and multipolygon relations are mapped to
 * polygons. Each output feature contains all key/value tags from the input feature, plus these extra attributes:
 * <ul>
 * <li>{@code @type}: node, way, or relation</li>
 * <li>{@code @id}: OSM element ID</li>
 * <li>{@code @changeset}: Changeset that last modified the element</li>
 * <li>{@code @timestamp}: Timestamp at which the element was last modified</li>
 * <li>{@code @version}: Version number of the OSM element</li>
 * <li>{@code @uid}: User ID that last modified the element</li>
 * <li>{@code @user}: User name that last modified the element</li>
 * </ul>
 * <p>
 * To run this example:
 * <ol>
 * <li>build the examples: {@code mvn clean package}</li>
 * <li>then run this example:
 * {@code java -cp target/*-fatjar.jar com.onthegomap.planetiler.examples.OsmQaTiles --area=monaco --download}</li>
 * <li>then run the demo tileserver: {@code tileserver-gl-light data/output.mbtiles}</li>
 * <li>and view the output at <a href="http://localhost:8080">localhost:8080</a></li>
 * </ol>
 */
public class OsmQaTiles implements Profile {
  static HashMap<String, String> qranks;

  public static void main(String[] args) throws Exception {
    run(Arguments.fromArgsOrConfigFile(args));
  }

  static void run(Arguments inArgs) throws Exception {
    qranks = new HashMap<String, String>();

    System.out.println("Start reading qrank.csv...");
    BufferedReader reader;
    reader = new BufferedReader(new FileReader("src/main/java/com/onthegomap/planetiler/examples/qrank.csv"));
    String line = reader.readLine();
    while (line != null) {
      String[] parts = line.split(",");
      qranks.put(parts[0].trim(), parts[1].trim());
      line = reader.readLine();
    }
    reader.close();

    var args = inArgs.orElse(Arguments.of(
      "minzoom", 0,
      "maxzoom", 10,
      "tile_warning_size_mb", 100
    ));
    String area = args.getString("area", "geofabrik area to download", "monaco");
    Planetiler.create(args)
      .setProfile(new OsmQaTiles())
      .addOsmSource("osm",
        Path.of("data", "sources", area + ".osm.pbf"),
        "planet".equalsIgnoreCase(area) ? "aws:latest" : ("geofabrik:" + area)
      )
      .overwriteOutput("mbtiles", Path.of("data", "qa.mbtiles"))
      .run();
  }

  static int getQRank(Object wikidata) {
    String qrank = qranks.get(wikidata.toString());
    if (qrank == null) {
      return 0;
    }
    else {
      return Integer.parseInt(qrank);
    }
  }

  @Override
  public void processFeature(SourceFeature sourceFeature, FeatureCollector features) {
    if (!sourceFeature.tags().isEmpty() && sourceFeature instanceof OsmSourceFeature osmFeature) {
      var feature = sourceFeature.isPoint() && sourceFeature.hasTag("wikidata") && sourceFeature.hasTag("place") && sourceFeature.hasTag("name")? features.point("osm") : null;
      if (feature != null) {
        feature
          .setZoomRange(0, 10)
          .setSortKey(-getQRank(sourceFeature.getTag("wikidata")))
          .setPointLabelGridSizeAndLimit(
            12, // only limit at z12 and below
            32, // break the tile up into 32x32 px squares
            4 // any only keep the 4 nodes with lowest sort-key in each 32px square
          )
          .setBufferPixelOverrides(ZoomFunction.maxZoom(12, 32));
        feature.setAttr("name", sourceFeature.getTag("name"));
        feature.setAttr("@qrank", getQRank(sourceFeature.getTag("wikidata")));
        feature.setAttr("wikidata", sourceFeature.getTag("wikidata"));
      }
    }
  }

  @Override
  public String name() {
    return "osm qa";
  }

  @Override
  public String attribution() {
    return """
      <a href="https://www.openstreetmap.org/copyright" target="_blank">&copy; OpenStreetMap contributors</a>
      """.trim();
  }
}
