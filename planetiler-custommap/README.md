# Configurable Planetiler Schema
It is possible to customize planetiler's output from configuration files.  This is done using the parameter:
`--schema=schema_file.yml`

The schema file provides information to planetiler about how to construct the tiles and which layers, features, and attributes will be posted to the file.  Schema files are in [YAML](https://yaml.org) format.

## Schema file definition

The root of the schema has the following attributes:
* `schemaName` - A descriptive name for the schema
* `schemaDescription` - A longer description of the schema
* `attribution` - An attribution statement, which may include HTML such as links
* `sources` - A list of data sources for the schema.  See [Data Sources](#data-sources)
* `layers` - A list of vector tile layers and their definitions.  See [Layers](#layers)

### Data Sources
A data source contains geospatial objects with tags that are consumed by planetiler.  The configured data sources in the schema provide complete information on how to access those data sources.
* `name` - Name of this data source, which is referenced in other parts of the schema
* `type` - Either `shapefile` or `osm`
* `url` - Location to download the a shapefile from
* `area` - Location to download osm data from.  Needs to be prefixed with the source, for example `geofabrik:rhode-island`

### Layers 
A layer contains a thematically-related set of features.
* `name` - Name of this layer
* `features` - A list of features contained in this layer.  See [Features](#features)

### Features
A feature is a defined set of objects that meet specified filter criteria.
* `sources` - A list of sources from which features should be extracted, specified as a list of names.  See [Data Sources](#data-sources).
* `zoom` - Specifies the zoom inclusion rules for this feature.  See [Zoom Specification](#zoom-specification).
* `includeWhen` - A filter specification which determines which features to include.  If unspecified, all features from the specified sources are included.  See [Filters](#filters)
* `excludeWhen` - A filter specification which determines which features to exclude.  This rule is applied after `includeWhen`.  If unspecified, no exclusion filter is applied.  See [Filters](#filters)
* `attributes` - Specifies the attributes that should be rendered into the tiles for this feature, and how they are constructed.  See [Attributes](#attributes)

### Zoom Specification
Specifies the zoom inclusion rules for this feature.
* `minZoom` - Minimum zoom to render this feature
* `maxZoom` - Maximum zoom to render this feature

### Attributes
* `key` - Name of this attribute in the tile.
* `constantValue` - Value of the attribute in the tile, as a constant
* `tagValue` - Value of the attribute in the tile, as copied from the value of the specified tag key.
* `dataType` - Whether to perform type alignment, so that rendered attribute values are consistently set.  Valid values include `bool` and `string`.
* `includeWhen` - A filter specification which determines whether to include this attribute.  If unspecified, the attribute will be included unless excluded by `excludeWhen`.  See [Filters](#filters)
* `excludeWhen` - A filter specification which determines whether to exclude this attribute.  This rule is applied after `includeWhen`.  If unspecified, no exclusion filter is applied.  See [Filters](#filters)
* `minZoom` - The minimum zoom at which to render this attribute.

### Filters
A filter is a specification applied to each object in a data source based on a matching criteria.
* `geometry` - Match objects of a certain geometry type.  Options are `polygon`, `linestring`, or `point`.
* `tag` - Match objects that match a certain tagging.    See [Tag Filter](#tag-filter)
* `minTileCoverSize` - Match objects of a certain geometry size, where 1.0 means "is the same size as a tile at this zoom".

### Tag Filter
A tag filter matches an object based on its tagging.
* `key` - Match objects that contain this key.
* `value` - A list of values.  Match objects in the specified key that contains one of these values.