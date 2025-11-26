# Java Development Standards

## Runtime vs Syntax Compatibility
- **Runtime Environment**: Java 21 (production)
- **Syntax Requirement**: Java 11 compatible (core modules)
- **CLI Tools Exception**: Java 21 features allowed in `tools/dotcms-cli` only

## Core Development Patterns

### API Locator Pattern
```java
// Service access - ALWAYS use APILocator
UserAPI userAPI = APILocator.getUserAPI();
ContentletAPI contentletAPI = APILocator.getContentletAPI();

// Web services
UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
```

### Configuration Management
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

### Logging Standards
```java
import com.dotmarketing.util.Logger;

Logger.info(this, "Operation completed successfully");
Logger.error(this, "Operation failed: " + error.getMessage(), error);
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
```

## Build Requirements

### Maven Dependencies
- Add dependency versions to `bom/application/pom.xml`
- Never add versions to `dotCMS/pom.xml`
- Run `./mvnw compile` after `@Value.Immutable` changes

### Integration Testing
```java
// Integration tests for REST endpoints
@ExtendWith(MockitoExtension.class)
class MyResourceIntegrationTest extends APITestCase {
    
    @Test
    void testEndpoint() {
        // Test implementation
    }
}
```

## Location Information
- **Package Structure**: `com.dotcms.api`, `com.dotcms.business`, `com.dotcms.rest`, `com.dotcms.util`
- **Legacy Packages**: `com.dotmarketing.*`
- **Configuration**: Use `Config` class for all configuration access
- **Exceptions**: Use dotCMS exception hierarchy (`DotDataException`, `DotSecurityException`, etc.)