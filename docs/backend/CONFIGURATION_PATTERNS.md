# dotCMS Configuration Patterns

## Property File Hierarchy

### Main Configuration File
- **Primary file**: `dotCMS/src/main/resources/dotmarketing-config.properties`
- **Use for**: General application configuration, new features, most properties

### Specialized Configuration Files  
For properties related to specific domains that already have dedicated files:

- **Cluster config**: `dotCMS/src/main/resources/dotcms-config-cluster.properties`
  - Use for: Clustering, ES endpoints, reindexing, heartbeat settings
  - Example properties: `ES_ENDPOINTS`, `HEARTBEAT_TIMEOUT`, `REINDEX_THREAD_*`

- **SAML config**: `dotCMS/src/main/resources/dotcms-saml-default.properties`  
  - Use for: SAML authentication configuration

- **Database config**: `dotCMS/src/main/resources/db.properties`
  - Use for: Database connection settings

- **Portal config**: `dotCMS/src/main/resources/portal.properties`
  - Use for: Portal/legacy configuration

### Decision Rules
1. **Check existing files first**: If your property relates to functionality already configured in a specialized file, add it there
2. **Use main config for new domains**: For new features or general configuration, use `dotmarketing-config.properties`
3. **Don't create new specialized files**: Avoid creating new `.properties` files unless absolutely necessary

## Property Naming Convention

### In Properties File
Use **lowercase dot-separated** format:
```properties
# Correct format
shutdown.timeout.seconds=30
shutdown.component.timeout.seconds=10
experiments.enabled=true
health.detailed.authentication.required=true
```

### In Java Code
Use the **same lowercase dot-separated** format with `Config` class:
```java
// Correct usage
Config.getIntProperty("shutdown.timeout.seconds", 30)
Config.getBooleanProperty("experiments.enabled", false)
Config.getStringProperty("health.detailed.authentication.required", "true")
```

### Environment Variables (Automatic)
The `Config` class automatically transforms properties:
- `shutdown.timeout.seconds` → `DOT_SHUTDOWN_TIMEOUT_SECONDS`
- `experiments.enabled` → `DOT_EXPERIMENTS_ENABLED`
- `health.detailed.authentication.required` → `DOT_HEALTH_DETAILED_AUTHENTICATION_REQUIRED`

## Search Index Source Storage

These permanent index-storage settings use ordinary `Config.getBooleanProperty` resolution,
including runtime system-table overrides. They are not cached in static fields:

| Property | Default | Environment override |
|---|---|---|
| `index.content.source.enabled` | `false` | `DOT_INDEX_CONTENT_SOURCE_ENABLED` |
| `index.sitesearch.source.enabled` | `true` | `DOT_INDEX_SITESEARCH_SOURCE_ENABLED` |

Both apply to Elasticsearch and OpenSearch. Each is read when its own kind of index is created;
changing the system-table value requires no application restart, but **does not change an existing
index's mapping**. Rebuild the affected indexes to apply either direction of a change.

- **Content source off:** only `identifier` and `inode` are separately stored for content hydration.
  Queries use stored identity and the existing `identifier_dotraw` / `inode_dotraw` keyword doc
  values, so older indexes without stored identity fields remain readable without fetching source.
  Sorting, filtering and counts still use indexed values/doc values, not source.
- **Site Search source off:** result fields are separately stored so document retrieval, titles,
  URLs, keywords and highlight snippets continue to work. Additional crawler string fields are also
  stored. This is not a promise of lower disk usage: individually stored result fields still cost
  space. Raw source and its original JSON shape are no longer available.
- The neutral `SearchHit.getSourceAsMap()` accessor falls back to explicitly requested stored
  values when source is unavailable. REST adapters may therefore still return an identifier/inode
  map under `_source`; the native ES API preserves that identifier-only view too. This does not
  mean the engine retained the original source document.
- Source-dependent engine operations, including `_update`, `_update_by_query` and `_reindex`, are
  unavailable on source-disabled indexes. Rebuild content indexes from the dotCMS database instead.
  Caller-supplied Site Search `_source` include/exclude filters must also be removed when source is disabled.
- This is **not an access-control mechanism**: indexed values can remain searchable or aggregatable.
  Keep permission checks and public-query restrictions in place.

**Rollback risk:** older application versions that read content or Site Search from source cannot
read source-disabled indexes. Set both source settings to `true` and rebuild the affected indexes
before rolling back, or restore compatible indexes. Merely changing the settings is not enough.
Do not rebuild source-disabled indexes while older application nodes are still serving traffic.
See [Rollback-Unsafe Categories](../core/ROLLBACK_UNSAFE_CATEGORIES.md#c-2--elasticsearch-mapping-change).

## Feature Flags

A feature flag is a boolean property that switches a whole behavior on or off. It follows the
same `Config` rules as any other property, plus three of its own:

- **The name is a constant in `FeatureFlagName`** (`com.dotcms.featureflag.FeatureFlagName`), never a
  string literal at the call site. The constant and its value are the same upper-case,
  underscore-separated name with the `FEATURE_FLAG_` prefix, e.g.
  `FEATURE_FLAG_DB_UNIQUE_FIELD_VALIDATION`. The `DOT_` environment variable form is automatic:
  `DOT_FEATURE_FLAG_DB_UNIQUE_FIELD_VALIDATION`.
- **The default is stated in the constant's Javadoc**, together with what the flag turns on or off
  and what is left untouched. The default lives at the read site
  (`Config.getBooleanProperty(FeatureFlagName.X, true)`), so the Javadoc is the only place a reader
  can learn it without finding every caller. Say whether it is an opt-in (off by default) or a
  kill switch (on by default).
- **Read it once per request, not per loop iteration.** Cache it in a `Lazy` or a field when the
  same code path reads it repeatedly.

```java
// FeatureFlagName.java
/**
 * Kill switch for X. On by default: ... Off: ... Pages are not affected either way.
 */
String FEATURE_FLAG_X = "FEATURE_FLAG_X";

// Call site
if (Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_X, true)) { ... }
```

Only add the property to `dotmarketing-config.properties` when the default must be visible there
(for example when a frontend reader treats an absent property as enabled); otherwise the Javadoc
and the read site are enough.

## Config Class Resolution Order
1. **Environment variables** (with `DOT_` prefix)
2. **System table** (database)
3. **Properties files** (all files are loaded)

## Configuration Documentation Pattern
When adding new properties, include:
```properties
# Purpose description (default: value)
# Environment variable: DOT_PROPERTY_NAME
property.name=default_value
```

## Examples from Codebase

### Health Check Configuration (Main Config)
```properties
# In dotmarketing-config.properties
health.detailed.authentication.required=true

# In Java code
Config.getBooleanProperty("health.detailed.authentication.required", true)

# Environment variable override
DOT_HEALTH_DETAILED_AUTHENTICATION_REQUIRED=false
```

### Cluster Configuration (Specialized File)
```properties
# In dotcms-config-cluster.properties
ES_ENDPOINTS=http://localhost:9200
HEARTBEAT_TIMEOUT=600

# In Java code
Config.getStringProperty("ES_ENDPOINTS", "http://localhost:9200")
Config.getIntProperty("HEARTBEAT_TIMEOUT", 600)
```

### Shutdown Configuration (Main Config)
```properties
# In dotmarketing-config.properties  
shutdown.timeout.seconds=30
shutdown.graceful.logging=true

# In Java code - ShutdownCoordinator
Config.getIntProperty("shutdown.timeout.seconds", 30)
Config.getBooleanProperty("shutdown.graceful.logging", true)

# Environment variable override
DOT_SHUTDOWN_TIMEOUT_SECONDS=45
DOT_SHUTDOWN_GRACEFUL_LOGGING=false
```

## Anti-Patterns to Avoid

### ❌ Wrong Property Naming
```properties
# In dotmarketing-config.properties: don't use uppercase — this file follows
# the hierarchical dot-notation convention shown above
SHUTDOWN_TIMEOUT_SECONDS=30

# Don't use dotcms prefix in properties file
dotcms.shutdown.timeout=30
```

> **Exception:** specialized config files that predate this convention — `dotcms-config-cluster.properties`
> (see above), and the OpenSearch-related config in `docs/backend/OPENSEARCH_CLIENT_CONFIGURATION.md` /
> `OPENSEARCH_MIGRATION.md` — correctly use uppercase property names. The anti-pattern above applies to
> `dotmarketing-config.properties`, not to those files.

### ❌ Wrong Config Usage
```java
// Don't use uppercase in code
Config.getIntProperty("SHUTDOWN_TIMEOUT_SECONDS", 30)

// Don't use System.getProperty directly
System.getProperty("shutdown.timeout.seconds")
```

### ❌ Wrong File Usage
```properties
# Don't put cluster config in main file if it belongs in cluster file
# In dotmarketing-config.properties (WRONG)
es.endpoints=http://localhost:9200

# Should be in dotcms-config-cluster.properties (CORRECT)
ES_ENDPOINTS=http://localhost:9200
```

### ❌ Creating Unnecessary Files
```
# Don't create new specialized files unnecessarily
dotCMS/src/main/resources/shutdown.properties  # Bad
dotCMS/src/main/resources/my-feature.properties  # Bad
```

## Key Points
- Check specialized files first for domain-specific properties
- Use `Config.getProperty()` for all configuration access
- Properties use lowercase dot-separated naming in code
- Environment variables automatically get `DOT_` prefix and uppercase conversion
- Add general properties to `dotmarketing-config.properties`
- Add domain-specific properties to existing specialized files
- Document environment variable names in comments