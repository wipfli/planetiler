package com.onthegomap.planetiler.examples;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.Planetiler;
import com.onthegomap.planetiler.Profile;
import com.onthegomap.planetiler.config.Arguments;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.reader.osm.OsmElement;
import com.onthegomap.planetiler.reader.osm.OsmSourceFeature;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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

    BufferedReader reader;
    try {
      reader = new BufferedReader(new FileReader("src/main/java/com/onthegomap/planetiler/examples/qrank.csv"));
      String line = reader.readLine();
      while (line != null) {
        String[] parts = line.split(",");
        qranks.put(parts[0].trim(), parts[1].trim());
        line = reader.readLine();
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.print(qranks);

    int zoom = inArgs.getInteger("zoom", "zoom level to generate tiles at", 12);
    var args = inArgs.orElse(Arguments.of(
      "minzoom", zoom,
      "maxzoom", zoom,
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
      var feature = sourceFeature.isPoint() && sourceFeature.hasTag("wikidata") && sourceFeature.hasTag("place") ? features.point("osm") : null;
      if (feature != null) {
        var element = osmFeature.originalElement();
        feature
          .setMinPixelSize(0)
          .setPixelTolerance(0)
          .setBufferPixels(0);
        for (var entry : sourceFeature.tags().entrySet()) {
          feature.setAttr(entry.getKey(), entry.getValue());
          if (entry.getKey().equals("wikidata")) {
            feature.setAttr("@qrank", getQRank(entry.getValue()));
          }
        }
        feature
          .setAttr("@id", sourceFeature.id())
          .setAttr("@type", element instanceof OsmElement.Node ? "node" :
            element instanceof OsmElement.Way ? "way" :
            element instanceof OsmElement.Relation ? "relation" : null
          );
        var info = element.info();
        if (info != null) {
          feature
            .setAttr("@version", info.version() == 0 ? null : info.version())
            .setAttr("@timestamp", info.timestamp() == 0L ? null : info.timestamp())
            .setAttr("@changeset", info.changeset() == 0L ? null : info.changeset())
            .setAttr("@uid", info.userId() == 0 ? null : info.userId())
            .setAttr("@user", info.user() == null || info.user().isBlank() ? null : info.user());
        }
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
