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

## Anonymous Content API Access — Upgrade Note

`CONTENT_APIS_ALLOW_ANONYMOUS` defaults to `NONE` in both the bundled configuration and
when the property is absent. Anonymous requests to APIs governed by this setting require
authentication; authenticated requests remain subject to their existing permissions.
This includes content REST APIs, GraphQL, and custom `/api/vtl/*` endpoints that use the
same access checks. Unrelated public/bootstrap endpoints are unchanged.

**Breaking default change:** installations relying on the previous implicit `READ` default
must authenticate their API clients or explicitly opt in before upgrading. To retain public
API reads, set `CONTENT_APIS_ALLOW_ANONYMOUS=READ` in your configuration override, or
`DOT_CONTENT_APIS_ALLOW_ANONYMOUS=READ` in the deployment environment. Anonymous content
submission requires an explicit `WRITE` opt-in. Existing explicit `NONE`, `READ`, and `WRITE`
overrides are preserved, and enabling anonymous access does not bypass content permissions.
Values are case-insensitive (`none`, `read`, and `write` are accepted); use the documented
uppercase property/environment variable names. Review headless clients and custom VTL
endpoints when preparing an upgrade. Servers providing public content feeds, including an
announcements feed, must explicitly opt into `READ`; instances merely consuming a remote
public feed do not need to enable anonymous access themselves.

**Rollback note:** these are compiled-in defaults, not stored settings, so rolling the binary
back to a previous release silently restores the older, more permissive behavior (implicit
anonymous `READ` on content APIs, and re-enabled Basic/DOTAUTH **password** credentials) without
any operator action. An installation that must keep the hardened posture across a rollback
should pin `DOT_CONTENT_APIS_ALLOW_ANONYMOUS=NONE` in the deployment environment instead of
relying on the in-binary default. A previous binary without the `ALLOW_BASIC_AUTH` policy
ignores `DOT_ALLOW_BASIC_AUTH`, so that override cannot preserve either `NONE` or `JWT_TOKEN`
across rollback. Block password-based Basic/DOTAUTH and legacy credential access at an upstream
gateway, or use a binary that supports the policy. Call the default-change and this rollback
limitation out in the release notes.

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

### Runtime Reindex Worker Limits

Background journal consumption has two settings in `dotcms-config-cluster.properties`:

| Property | Default | Meaning |
| --- | --- | --- |
| `MAX_REINDEXING_NODES` | `2` | Maximum workers for capped entries. Explicit `0` or a negative value restores unrestricted participation. |
| `REINDEX_NODE_LIMIT_MIN_PRIORITY` | `200` | Inclusive priority cutoff. A negative value is reported and falls back to `200`. |

Malformed integer values are reported and fall back to the documented defaults rather than
stopping the journal consumer. Both values are read through `Config.getIntProperty` before
every batch, not cached at startup. Changes visible to Config apply to the same consumer's next batch, including prefetched work;
a batch already in flight can finish. Use the existing runtime/system-table configuration
mechanism. Environment variables (`DOT_MAX_REINDEXING_NODES` and
`DOT_REINDEX_NODE_LIMIT_MIN_PRIORITY`) take precedence over system-table changes: do not pin
an environment override if the property must be adjustable through the system table.

With six application nodes and the default settings, the first two nodes in the existing
startup-time/server-ID membership order process structure updates (`200`) and full reindexes
(`300`); ASAP (`0`) and normal saves (`100`, including retries such as `101`) remain eligible
on all six. Queue deletes follow the same priority rule. A one-node installation keeps its
one available worker; a maximum above the membership size uses every available node.

Alternative cutoffs: `0` caps all eligible journal work, `100` also caps normal saves, and
`300` leaves structure updates unrestricted. Equality with the cutoff is capped. The cutoff
does not revive failed entries outside the journal's existing retry/eligibility rules.

Admission and journal partitions use the same priority-aware ownership. A nonselected node
cannot bypass the cap through stale prefetch or stalled-share recovery. When membership or
settings change the ownership split, cursors and recovery history reset so newly assigned
older rows remain reachable. Membership-based failover and normal worker wake-up rules still apply; this is
not a distributed lease or an instantaneous cluster-wide configuration transaction.

**Bulk size is unchanged:** provider adapters still divide the configured bulk-action value
by the full eligible membership. A value of `20` with six application nodes retains a count
threshold of `3`; selecting two bulk workers does not silently increase it to `10`.
`REINDEX_THREAD_CONCURRENT_REQUESTS=0` makes a processor synchronous; it is not a substitute
for a worker cap or a guarantee of half the traffic. `REINDEX_THREAD_SLEEP` controls paused
waiting, and `REINDEX_THREAD_MINIMUM_RUNTIME_IN_SEC` delays full-reindex switchover; neither
paces active ingestion in this path.

**Upgrade/rollback:** two capped workers is a changed default for existing clusters. Set
`MAX_REINDEXING_NODES=0` explicitly to retain unrestricted participation. All nodes must run
the supporting binary with the same configuration. Older binaries ignore these properties,
so mixed-version deployment or rollback can restore unrestricted bulk processing. Existing
in-flight work and membership/configuration propagation can temporarily overlap.

This limits background worker participation, not documents per second or search traffic.
Fewer workers can delay structure propagation and full-reindex completion. Direct synchronous
indexing and explicit save-request `indexPolicy` overrides are unchanged. Validate queue-drain
speed and search CPU/latency under a representative workload before production rollout.

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