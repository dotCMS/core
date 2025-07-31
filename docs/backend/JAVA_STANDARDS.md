# Java Development Standards

## Runtime vs Syntax Compatibility
- **Runtime Environment**: Java 21 (production)
- **Syntax Requirement**: Java 11 compatible (core modules)
- **CLI Tools Exception**: Java 21 features allowed in `tools/dotcms-cli` only

## ✅ Use Java 11 Syntax in Core Modules
```java
// Java 11 compatible patterns
var users = userAPI.findActiveUsers();
var contentTypes = contentTypeAPI.findAll();

// Java 11 compatible Optional and Stream operations
Optional<String> value = getValue();
String result = value.orElse("default");

List<String> names = users.stream()
    .map(User::getName)
    .filter(Objects::nonNull)
    .collect(Collectors.toList());

// Traditional string concatenation or String.format()
String query = "SELECT c.identifier, c.title FROM contentlet c " +
               "WHERE c.structure_inode = ?";

// Traditional switch statements
String status;
switch (contentlet.getBaseType()) {
    case CONTENT:
        status = "Content";
        break;
    case HTMLPAGE:
        status = "Page";
        break;
    default:
        status = "Unknown";
}
```

## ✅ Java 21 Syntax (CLI/Tools Modules Only)
```java
// ONLY in tools/dotcms-cli and test modules
var query = """
    SELECT c.identifier, c.title FROM contentlet c 
    WHERE c.structure_inode = ?
    """;

var status = switch (contentlet.getBaseType()) {
    case CONTENT -> "Content";
    case HTMLPAGE -> "Page";
    default -> "Unknown";
};

public record UserInfo(String id, String email, String name) {}
```

## Core Development Patterns

### API Locator Pattern (Required)
```java
// Service access - ALWAYS use APILocator
UserAPI userAPI = APILocator.getUserAPI();
ContentletAPI contentletAPI = APILocator.getContentletAPI();

// Web services
UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();

// ❌ NEVER instantiate services directly
// UserAPIImpl userAPI = new UserAPIImpl();
```

### Configuration Management (Required)
> **Security**: All configuration must follow [Security Principles](../core/SECURITY_PRINCIPLES.md)
```java
import com.dotmarketing.util.Config;

// Hierarchical naming for new properties
boolean enabled = Config.getBooleanProperty("experiments.enabled", false);
String url = Config.getStringProperty("experiments.auto-js-injection.url", "");

// Environment variables automatically get DOT_ prefix
// experiments.enabled → DOT_EXPERIMENTS_ENABLED
```

### Property Resolution Order
1. Environment variables with `DOT_` prefix (e.g., `DOT_EXPERIMENTS_ENABLED`)
2. System table for both transformed and original names
3. Properties files for both transformed and original names

### New Property Naming Convention
```properties
# Use hierarchical domain-driven naming (RECOMMENDED)
experiments.enabled=true
experiments.auto-js-injection.enabled=true
experiments.auto-js-injection.url=https://example.com/script.js
experiments.auto-js-injection.max-retries=3

health.checks.database.timeout-seconds=30
health.monitoring.include-system-details=true
```

### Logging Standards (Required)
```java
import com.dotmarketing.util.Logger;

Logger.info(this, "Operation completed successfully");
Logger.error(this, "Operation failed: " + error.getMessage(), error);

// ❌ NEVER use: System.out.println(), printStackTrace(), System.err.println()
```

### Immutable Objects (Critical Pattern)
```java
@Value.Immutable
@JsonSerialize(as = ImmutableMyEntity.class)
@JsonDeserialize(as = ImmutableMyEntity.class)
public abstract class MyEntity {
    public abstract String name();
    public abstract Optional<String> description();
    
    @Value.Default
    public boolean enabled() { return true; }
    
    public static Builder builder() { return ImmutableMyEntity.builder(); }
}

// Usage: MyEntity.builder().name("test").description("desc").build()
// IMPORTANT: Run ./mvnw compile after creating @Value.Immutable classes
```

### Exception Handling (dotCMS Hierarchy)
```java
try {
    riskyOperation();
} catch (SQLException e) {
    Logger.error(this, "Database operation failed: " + e.getMessage(), e);
    throw new DotDataException("Failed to process request", e);
} catch (SecurityException e) {
    throw new DotSecurityException("Access denied", e);
}

// Exception types: DotDataException, DotSecurityException, DotRuntimeException, DotStateException
```

### Utility Methods (Null-Safe Patterns)
```java
import com.dotmarketing.util.UtilMethods;

// ALWAYS use UtilMethods.isSet() for null checking
if (UtilMethods.isSet(myString)) {  // checks null, empty, and "null" string
    processString(myString);
}

// Collections
List<String> list = CollectionsUtils.list("item1", "item2");
Map<String, Object> map = CollectionsUtils.map("key1", "value1", "key2", "value2");

// Safe supplier pattern (avoids NullPointerException)
String value = UtilMethods.isSet(() -> complex.getObject().getValue()) 
    ? complex.getObject().getValue() 
    : "default";
```

### Database Access Patterns
```java
import com.dotmarketing.common.db.DotConnect;

// Query with parameters
DotConnect dotConnect = new DotConnect();
List<Map<String, Object>> results = dotConnect
    .setSQL("SELECT * FROM my_table WHERE column1 = ? AND column2 = ?")
    .addParam("value1")
    .addParam("value2")
    .loadResults();

// Use with LocalTransaction for atomic operations
LocalTransaction.wrapReturn(() -> {
    return dotConnect.setSQL("UPDATE my_table SET column1 = ?")
        .addParam("newValue")
        .executeUpdate();
});
```

### CDI Patterns (For New Components)
```java
@ApplicationScoped
public class MyService {
    private final JobQueueManagerAPI jobQueueManagerAPI;
    
    // Default constructor required for CDI proxy
    public MyService() {
        this.jobQueueManagerAPI = null;
    }
    
    @Inject
    public MyService(JobQueueManagerAPI jobQueueManagerAPI) {
        this.jobQueueManagerAPI = jobQueueManagerAPI;
    }
}

// Safe CDI bean access
Optional<MyService> service = CDIUtils.getBean(MyService.class);
MyService service = CDIUtils.getBeanThrows(MyService.class);
```

## Build Integration Requirements

### After Code Changes
- **Immutable classes**: Run `./mvnw compile` after `@Value.Immutable` changes
- **Fast iteration**: `./mvnw install -pl :dotcms-core -DskipTests`
- **Docker updates**: Run `./mvnw clean install` (without `-Ddocker.skip`)

### Critical Docker Build Workflow
Docker image updates only happen when building WITHOUT `-Ddocker.skip`:

```bash
# Fast development cycle (no Docker image update)
./mvnw install -pl :dotcms-core -DskipTests -Ddocker.skip

# When ready to test in Docker (REQUIRED for new servlets/endpoints):
./mvnw -DskipTests clean install  # Updates Docker image
./mvnw -pl :dotcms-core -Pdocker-start -Dtomcat.port=8080
```

## Legacy Patterns to Avoid in New Code
> **See**: [Progressive Enhancement](../core/PROGRESSIVE_ENHANCEMENT.md) for safe improvement strategies
```java
// ❌ Avoid in new development
@Deprecated public class MyPortletAction extends PortletAction {}
@Deprecated public class MyAjax extends WfBaseAction {}
System.out.println("message");
System.getProperty("property");
StructureAPI structureAPI = APILocator.getStructureAPI();

// ✅ Use modern alternatives
@Path("/v1/resource") public class MyResource {}
Logger.info(this, "message");
Config.getStringProperty("property", "default");
ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI();

// ✅ For class metadata analysis, prefer Jandex over reflection
List<Class<?>> annotatedClasses = JandexClassMetadataScanner.findClassesWithAnnotation(
    MyAnnotation.class, "com.dotcms.mypackage");
// See: docs/backend/JANDEX_METADATA_SCANNING.md
```