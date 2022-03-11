package com.onthegomap.planetiler.basemap.layers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.basemap.generated.OpenMapTilesSchema;
import com.onthegomap.planetiler.basemap.generated.Tables;
import com.onthegomap.planetiler.config.PlanetilerConfig;
import com.onthegomap.planetiler.stats.Stats;
import com.onthegomap.planetiler.util.Translations;

public class Power implements
  OpenMapTilesSchema.Power,
  Tables.OsmPowerLinestring.Handler {

  public Power(Translations translations, PlanetilerConfig config, Stats stats) {}

  @Override
  public void process(Tables.OsmPowerLinestring element, FeatureCollector features) {
    features.line(LAYER_NAME)
      .setMinZoom(10)
      .setAttr(Fields.POWER, element.power());
  }
}
