# Integration Tests (dotcms-integration)

## Overview

The `dotcms-integration` module contains comprehensive integration tests that validate the full dotCMS system with real services, databases, and external dependencies. These tests ensure end-to-end functionality and service integration.

## Test Structure

### Location
- **Main Path**: `dotcms-integration/src/test/java`
- **Test Runner**: Maven Failsafe plugin
- **Framework**: JUnit 4/5 with custom base classes
- **Environment**: Docker containers for dependencies

### Module Structure
```
dotcms-integration/
├── src/test/java/
│   ├── com/dotcms/          # Integration tests
│   ├── com/dotmarketing/    # Legacy integration tests
│   └── util/                # Test utilities
├── src/test/resources/      # Test configurations
├── docker/                  # Docker test environment
└── pom.xml                  # Maven configuration
```

### Base Classes
- **`IntegrationTestBase`**: Common setup for integration tests
- **`BaseContainerTest`**: Docker container management
- **`DataProviderRunner`**: Test data generation
- **`APITestCase`**: REST API testing base

## Testing Patterns

### Integration Test Structure
```java
public class ContentTypeAPIImplTest extends IntegrationTestBase {
    
    private ContentTypeAPI contentTypeAPI;
    private ContentletAPI contentletAPI;
    
    @Override
    public void prepare() throws Exception {
        super.prepare();
        contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
        contentletAPI = APILocator.getContentletAPI();
    }
    
    @Test
    public void testCreateContentType() throws Exception {
        // Given
        ContentType contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.getType())
            .name("TestContentType")
            .description("Test content type")
            .build();
        
        // When
        ContentType savedContentType = contentTypeAPI.save(contentType);
        
        // Then
        assertNotNull(savedContentType.id());
        assertEquals("TestContentType", savedContentType.name());
        
        // Cleanup
        contentTypeAPI.delete(savedContentType);
    }
}
```

### Docker Environment Setup
```java
@TestContainers
public class DatabaseIntegrationTest extends BaseContainerTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
        .withDatabaseName("dotcms_test")
        .withUsername("dotcms")
        .withPassword("dotcms")
        .withExposedPorts(5432);
    
    @Test
    public void testDatabaseConnection() {
        assertTrue(postgres.isRunning());
        
        // Test database operations
        DataSource dataSource = createDataSource();
        assertNotNull(dataSource.getConnection());
    }
}
```

### REST API Testing
```java
public class ContentResourceTest extends APITestCase {
    
    private static final String CONTENT_ENDPOINT = "/api/v1/content";
    
    @Test
    public void testCreateContent() throws Exception {
        // Given
        ContentType contentType = createTestContentType();
        Map<String, Object> contentData = Map.of(
            "contentType", contentType.variable(),
            "title", "Test Content",
            "body", "Test body content"
        );
        
        // When
        Response response = given()
            .auth().basic(user.getEmailAddress(), password)
            .contentType(ContentType.JSON)
            .body(contentData)
            .post(CONTENT_ENDPOINT);
        
        // Then
        response.then()
            .statusCode(200)
            .body("entity.identifier", notNullValue())
            .body("entity.title", equalTo("Test Content"));
    }
}
```

## Key Testing Areas

### 1. Content Management
```java
public class ContentletAPITest extends IntegrationTestBase {
    
    @Test
    public void testContentletLifecycle() throws Exception {
        // Create content type
        ContentType contentType = createTestContentType();
        
        // Create contentlet
        Contentlet contentlet = new Contentlet();
        contentlet.setContentTypeId(contentType.id());
        contentlet.setStringProperty("title", "Test Title");
        contentlet.setHost(defaultHost);
        contentlet.setLanguageId(defaultLanguage.getId());
        
        // Save
        contentlet = contentletAPI.checkin(contentlet, systemUser, false);
        assertNotNull(contentlet.getIdentifier());
        
        // Update
        contentlet.setStringProperty("title", "Updated Title");
        contentlet = contentletAPI.checkin(contentlet, systemUser, false);
        assertEquals("Updated Title", contentlet.getStringProperty("title"));
        
        // Delete
        contentletAPI.delete(contentlet, systemUser, false);
        
        // Verify deletion
        assertFalse(contentletAPI.findByIdentifier(contentlet.getIdentifier(), 
            defaultLanguage.getId(), false, systemUser, false).isPresent());
    }
}
```

### 2. Workflow Integration
```java
public class WorkflowAPITest extends IntegrationTestBase {
    
    @Test
    public void testWorkflowExecution() throws Exception {
        // Given
        WorkflowScheme scheme = createTestWorkflowScheme();
        Contentlet contentlet = createTestContentlet();
        
        // When
        WorkflowAction action = workflowAPI.findAction("publish", systemUser);
        WorkflowProcessor processor = WorkflowProcessorFactory.getInstance()
            .getProcessor(action, contentlet);
        
        processor.fireWorkflow(contentlet, systemUser);
        
        // Then
        Contentlet updatedContentlet = contentletAPI.findContentletByIdentifier(
            contentlet.getIdentifier(), false, defaultLanguage.getId(), systemUser, false);
        
        assertTrue(updatedContentlet.isLive());
    }
}
```

### 3. Database Operations
```java
public class DatabaseIntegrationTest extends IntegrationTestBase {
    
    @Test
    public void testDatabaseTransaction() throws Exception {
        // Given
        final String testData = "test_data_" + System.currentTimeMillis();
        
        // When - Test transaction rollback
        try {
            HibernateUtil.startTransaction();
            
            // Perform database operations
            DotConnect dc = new DotConnect();
            dc.setSQL("INSERT INTO test_table (data) VALUES (?)");
            dc.addParam(testData);
            dc.loadResult();
            
            // Force rollback
            throw new RuntimeException("Test rollback");
            
        } catch (RuntimeException e) {
            HibernateUtil.rollbackTransaction();
        } finally {
            HibernateUtil.closeSession();
        }
        
        // Then - Verify rollback
        DotConnect dc = new DotConnect();
        dc.setSQL("SELECT COUNT(*) as count FROM test_table WHERE data = ?");
        dc.addParam(testData);
        Map<String, Object> result = dc.loadObjectResults().get(0);
        
        assertEquals(0L, result.get("count"));
    }
}
```

### 4. Caching Integration
```java
public class CacheIntegrationTest extends IntegrationTestBase {
    
    @Test
    public void testCacheInvalidation() throws Exception {
        // Given
        String cacheKey = "test_cache_key";
        String testValue = "test_value";
        CacheLocator.getCacheAdministrator().put(cacheKey, testValue, "test_region");
        
        // When
        Object cachedValue = CacheLocator.getCacheAdministrator().get(cacheKey, "test_region");
        assertEquals(testValue, cachedValue);
        
        // Invalidate
        CacheLocator.getCacheAdministrator().flushGroup("test_region");
        
        // Then
        Object invalidatedValue = CacheLocator.getCacheAdministrator().get(cacheKey, "test_region");
        assertNull(invalidatedValue);
    }
}
```

## Running Tests

### Command Line Execution
```bash
# Run all integration tests
./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false

# Run specific test class
./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false -Dtest=ContentTypeAPIImplTest

# Run with specific environment
./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false -Dtest.environment=postgres

# Run with Docker cleanup
./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false -Ddocker.cleanup=true

# Run with debug logging
./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false -Dlog.level=DEBUG
```

### Maven Configuration
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <skipTests>${coreit.test.skip}</skipTests>
        <includes>
            <include>**/*Test.java</include>
            <include>**/*IT.java</include>
        </includes>
        <systemPropertyVariables>
            <test.environment>${test.environment}</test.environment>
            <docker.cleanup>${docker.cleanup}</docker.cleanup>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

### Docker Environment Management
```bash
# Start test environment
docker-compose -f docker/docker-compose.test.yml up -d

# Run tests against running environment
./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false -Duse.running.environment=true

# Cleanup test environment
docker-compose -f docker/docker-compose.test.yml down -v
```

## Test Data Management

### Test Data Builders
```java
public class TestDataBuilder {
    
    public static ContentType createTestContentType() throws Exception {
        return ContentTypeBuilder.builder(BaseContentType.CONTENT.getType())
            .name("TestContentType_" + System.currentTimeMillis())
            .description("Test content type")
            .host(Host.SYSTEM_HOST)
            .folder(FolderAPI.SYSTEM_FOLDER)
            .build();
    }
    
    public static Contentlet createTestContentlet(ContentType contentType) throws Exception {
        Contentlet contentlet = new Contentlet();
        contentlet.setContentTypeId(contentType.id());
        contentlet.setHost(Host.SYSTEM_HOST);
        contentlet.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
        contentlet.setStringProperty("title", "Test Content " + System.currentTimeMillis());
        return contentlet;
    }
}
```

### Database Fixtures
```java
public class DatabaseFixtures {
    
    @BeforeClass
    public static void setupTestData() throws Exception {
        // Create test site
        Host testSite = createTestSite();
        
        // Create test user
        User testUser = createTestUser();
        
        // Create test content types
        ContentType newsType = createNewsContentType();
        ContentType pageType = createPageContentType();
        
        // Store in test context
        TestContext.setTestSite(testSite);
        TestContext.setTestUser(testUser);
    }
    
    @AfterClass
    public static void cleanupTestData() throws Exception {
        // Clean up in reverse order
        deleteTestContentTypes();
        deleteTestUsers();
        deleteTestSites();
    }
}
```

## CI/CD Integration

### GitHub Actions Integration
**Workflow**: Integration tests run in `cicd_comp_test-phase.yml`

**Change Detection**: Tests triggered by:
```yaml
backend: &backend
  - 'dotCMS/src/main/java/**'
  - 'dotcms-integration/**'
  - 'pom.xml'
  - '**/pom.xml'
```

**Execution**:
```yaml
- name: Run Integration Tests
  run: |
    ./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false
  env:
    MAVEN_OPTS: -Xmx4g
    DOCKER_BUILDKIT: 1
```

### Test Environment Setup
```yaml
services:
  postgres:
    image: postgres:13
    env:
      POSTGRES_DB: dotcms_test
      POSTGRES_USER: dotcms
      POSTGRES_PASSWORD: dotcms
    options: >-
      --health-cmd pg_isready
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
```

### Test Results
- **Failsafe Reports**: `dotcms-integration/target/failsafe-reports/`
- **JUnit XML**: `dotcms-integration/target/failsafe-reports/TEST-*.xml`
- **Logs**: `dotcms-integration/target/logs/`

## Debugging Test Failures

### Local Debugging

#### 1. Enable Debug Logging
```bash
# Run with debug logging
./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false -Dlog.level=DEBUG

# Enable SQL logging
./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false -Dhibernate.show_sql=true
```

#### 2. Docker Environment Debugging
```bash
# Check container status
docker ps

# View container logs
docker logs dotcms-integration-postgres-1

# Connect to database
docker exec -it dotcms-integration-postgres-1 psql -U dotcms -d dotcms_test
```

#### 3. Test Isolation
```java
@Test
public void testWithDebugInfo() throws Exception {
    // Enable debug logging for specific test
    Logger.getLogger("com.dotcms").setLevel(Level.DEBUG);
    
    try {
        // Test implementation
        ContentType contentType = createTestContentType();
        
        // Debug output
        System.out.println("Created content type: " + contentType.id());
        
    } finally {
        // Reset logging
        Logger.getLogger("com.dotcms").setLevel(Level.INFO);
    }
}
```

### GitHub Actions Debugging

#### 1. Environment Variables
```yaml
- name: Run Integration Tests
  run: ./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false
  env:
    MAVEN_OPTS: -Xmx4g
    DEBUG: true
    DOCKER_BUILDKIT: 1
    TESTCONTAINERS_RYUK_DISABLED: true
```

#### 2. Upload Test Artifacts
```yaml
- name: Upload Test Results
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: integration-test-results
    path: |
      dotcms-integration/target/failsafe-reports/
      dotcms-integration/target/logs/
      dotcms-integration/target/surefire-reports/
```

#### 3. Common Failure Patterns

**Docker Issues**:
```bash
# Check Docker daemon
docker version

# Clean up containers
docker system prune -f

# Check disk space
df -h
```

**Database Connection Issues**:
```java
@Test
public void testDatabaseConnection() {
    // Add connection debugging
    DataSource dataSource = DbConnectionFactory.getDataSource();
    try (Connection conn = dataSource.getConnection()) {
        assertTrue(conn.isValid(5));
        
        // Test basic query
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT 1");
            assertTrue(rs.next());
        }
    }
}
```

**Memory Issues**:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <forkCount>1</forkCount>
        <reuseForks>false</reuseForks>
        <argLine>-Xmx4g -XX:MaxPermSize=512m</argLine>
    </configuration>
</plugin>
```

## Best Practices

### ✅ Integration Test Standards
- **Test realistic scenarios**: Use actual services and data
- **Manage test data**: Create and clean up test data properly
- **Use transactions**: Wrap tests in transactions when possible
- **Test edge cases**: Include error conditions and boundary cases
- **Verify side effects**: Check that operations have expected impacts

### ✅ Docker Environment Management
```java
@TestContainers
public class ServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
        .withDatabaseName("dotcms_test")
        .withUsername("dotcms")
        .withPassword("dotcms")
        .withInitScript("init-test-db.sql");
    
    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer("elasticsearch:7.10.2")
        .withEnv("discovery.type", "single-node");
    
    @BeforeAll
    static void setupEnvironment() {
        // Configure connections
        System.setProperty("db.url", postgres.getJdbcUrl());
        System.setProperty("es.host", elasticsearch.getHost());
        System.setProperty("es.port", elasticsearch.getMappedPort(9200).toString());
    }
}
```

### ✅ Test Data Management
```java
public class TestDataManager {
    
    private static final List<String> createdContentTypes = new ArrayList<>();
    private static final List<String> createdContentlets = new ArrayList<>();
    
    public static ContentType createAndTrackContentType(String name) throws Exception {
        ContentType contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.getType())
            .name(name)
            .build();
        
        contentType = APILocator.getContentTypeAPI(systemUser).save(contentType);
        createdContentTypes.add(contentType.id());
        return contentType;
    }
    
    @AfterClass
    public static void cleanupTestData() throws Exception {
        // Clean up in reverse order of creation
        for (String contentletId : Lists.reverse(createdContentlets)) {
            cleanupContentlet(contentletId);
        }
        
        for (String contentTypeId : Lists.reverse(createdContentTypes)) {
            cleanupContentType(contentTypeId);
        }
    }
}
```

### ✅ Async Testing
```java
@Test
public void testAsyncOperation() throws Exception {
    // Given
    CompletableFuture<String> future = new CompletableFuture<>();
    
    // When
    asyncService.processAsync(data, result -> {
        future.complete(result);
    });
    
    // Then
    String result = future.get(10, TimeUnit.SECONDS);
    assertEquals("expected result", result);
}
```

## Common Issues and Solutions

### 1. Database Connection Issues
```java
// Problem: Connection pool exhaustion
// Solution: Proper connection management
@Test
public void testDatabaseOperations() throws Exception {
    try (Connection conn = DbConnectionFactory.getConnection()) {
        // Use connection
        // Automatically closed by try-with-resources
    }
}
```

### 2. Docker Container Issues
```bash
# Problem: Container not starting
# Solution: Check logs and configuration
docker logs container_name

# Check port conflicts
netstat -tulpn | grep 5432
```

### 3. Test Data Conflicts
```java
// Problem: Tests interfering with each other
// Solution: Use unique test data
@Test
public void testCreateUser() throws Exception {
    String uniqueEmail = "test_" + System.currentTimeMillis() + "@example.com";
    User user = createTestUser(uniqueEmail);
    
    // Test operations
    
    // Cleanup
    APILocator.getUserAPI().delete(user, systemUser, false);
}
```

### 4. Memory Leaks
```java
// Problem: Memory accumulation in long-running tests
// Solution: Explicit cleanup
@After
public void tearDown() throws Exception {
    // Clear caches
    CacheLocator.getCacheAdministrator().flushAll();
    
    // Close sessions
    HibernateUtil.closeSession();
    
    // Clear ThreadLocal variables
    ThreadLocalManager.clearAll();
}
```

## Performance Optimization

### Parallel Test Execution
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <parallel>methods</parallel>
        <threadCount>4</threadCount>
        <perCoreThreadCount>true</perCoreThreadCount>
    </configuration>
</plugin>
```

### Test Categorization
```java
// Fast integration tests
@Category(FastIntegrationTest.class)
public class ContentAPIQuickTest {
    // Quick API tests
}

// Slow integration tests
@Category(SlowIntegrationTest.class)
public class FullSystemTest {
    // Comprehensive system tests
}
```

### Database Optimization
```properties
# Test database configuration
hibernate.connection.pool_size=10
hibernate.jdbc.batch_size=25
hibernate.cache.use_second_level_cache=false
hibernate.cache.use_query_cache=false
```

## Integration with Development Workflow

### Pre-commit Integration Tests
```bash
# Run subset of integration tests before commit
./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false -Dtest=*APITest
```

### IDE Integration
- **IntelliJ**: Configure remote debugging for integration tests
- **Eclipse**: Set up test configurations for different environments
- **VS Code**: Use Java Test Runner with integration test profiles

## Location Information
- **Test Source**: `dotcms-integration/src/test/java`
- **Test Resources**: `dotcms-integration/src/test/resources`
- **Test Reports**: `dotcms-integration/target/failsafe-reports/`
- **Docker Configs**: `dotcms-integration/docker/`
- **Maven Plugin**: Failsafe plugin configuration in `dotcms-integration/pom.xml`