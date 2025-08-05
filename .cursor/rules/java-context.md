---
description: Java backend development context - loads only for Java files
globs: ["**/*.java", "**/pom.xml", "dotCMS/src/**/*"]
alwaysApply: false
---

# Java Backend Context

## Immediate Patterns (Copy-Paste Ready)

### Required Imports
```java
import com.dotmarketing.util.Config;        // NEVER System.getProperty()
import com.dotmarketing.util.Logger;        // NEVER System.out.println() 
import com.dotmarketing.util.UtilMethods;   // Null checking
import com.dotcms.util.CollectionsUtils;    // Safe collections
```

### Code Templates
```java
// Configuration access
boolean enabled = Config.getBooleanProperty("feature.enabled", false);
String endpoint = Config.getStringProperty("api.endpoint", "http://localhost");

// Logging  
Logger.info(this, "Operation completed successfully");
Logger.error(this, "Operation failed: " + e.getMessage(), e);

// Null safety
if (UtilMethods.isSet(myString)) {
    processString(myString);
}

// Service access (API Locator pattern)
UserAPI userAPI = APILocator.getUserAPI();
ContentletAPI contentletAPI = APILocator.getContentletAPI();

// Exception handling
try {
    riskyOperation();
} catch (SQLException e) {
    throw new DotDataException("Failed to process request", e);
}

// Immutable objects (@Value.Immutable pattern)
@Value.Immutable
public abstract class MyEntity {
    public abstract String name();
    public static Builder builder() { return ImmutableMyEntity.builder(); }
}
```

### Maven Dependency Rules (CRITICAL)
- **Add versions**: `bom/application/pom.xml` ONLY
- **Use dependencies**: `dotCMS/pom.xml` WITHOUT versions
- **Never override**: BOM-managed versions

### Java Version Constraints
- **Core modules**: Java 11 syntax only (no records, text blocks, switch expressions)  
- **CLI modules**: Full Java 21 syntax allowed
- **Runtime**: Java 21 everywhere

### Quick Build Commands
```bash
# Fast development build
./mvnw install -pl :dotcms-core -DskipTests

# With Docker (for servlets/endpoints)
./mvnw clean install -DskipTests
./mvnw -pl :dotcms-core -Pdocker-start -Dtomcat.port=8080

# Integration tests
./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false
```

## On-Demand Documentation
**Load only when needed to preserve context:**

- **Comprehensive Java patterns**: `@docs/backend/JAVA_STANDARDS.md`
- **Maven build system**: `@docs/backend/MAVEN_BUILD_SYSTEM.md`  
- **Configuration management**: `@docs/backend/CONFIGURATION_PATTERNS.md`
- **REST API development**: `@docs/backend/REST_API_PATTERNS.md`
- **Database patterns**: `@docs/backend/DATABASE_PATTERNS.md`