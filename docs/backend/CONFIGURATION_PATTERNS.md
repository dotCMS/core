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
# Don't use uppercase
SHUTDOWN_TIMEOUT_SECONDS=30

# Don't use dotcms prefix in properties file
dotcms.shutdown.timeout=30
```

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