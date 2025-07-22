---
description: Java-specific development context for dotCMS
globs: ["**/*.java", "**/pom.xml"]
alwaysApply: false
---

# Java Development Context

## Key Imports to Always Use
```java
import com.dotmarketing.util.Config;       // Never System.getProperty
import com.dotmarketing.util.Logger;       // Never System.out.println
import com.dotmarketing.util.UtilMethods;  // For null checking
```

## Maven Dependency Pattern
- **Add versions**: `bom/application/pom.xml` only
- **Use without versions**: `dotCMS/pom.xml` 
- **Never override**: BOM-managed versions

## Java Version Constraints
- **Core modules**: Java 11 syntax only (no text blocks, switch expressions)
- **CLI/Tools**: Full Java 21 syntax allowed
- **Runtime**: Java 21 everywhere

## Quick Patterns
```java
// Configuration
Config.getBooleanProperty("feature.enabled", false)

// Logging  
Logger.info(this, "Message");
Logger.error(this, "Error: " + e.getMessage(), e);

// Null safety
if (UtilMethods.isSet(value)) { /* use value */ }

// Service access
UserAPI userAPI = APILocator.getUserAPI();
```

Complete patterns: @docs/backend/JAVA_STANDARDS.md