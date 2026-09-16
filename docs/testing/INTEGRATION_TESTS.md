# Integration Tests (dotcms-integration)

## Overview

The `dotcms-integration` module contains comprehensive integration tests that validate the full dotCMS system with real services, databases, and external dependencies. These tests ensure end-to-end functionality and service integration.

## Test Structure

### Location
- **Main Path**: `dotcms-integration/src/test/java`
- **Test Runner**: Maven Failsafe plugin
- **Framework**: JUnit 4 (a small, growing set of newer tests use JUnit 5 via the `Junit5Suite*` aggregators)
- **Environment**: PostgreSQL + Elasticsearch/OpenSearch, run via Docker Compose (`dotcms-integration/src/docker-compose/it-test`) — started/stopped by Maven profiles, not by the tests themselves

### Module Structure
```
dotcms-integration/
├── src/test/java/
│   ├── com/dotcms/          # Modern integration tests
│   └── com/dotmarketing/    # Legacy integration tests
├── src/test/resources/      # Test configurations, log4j2.xml
├── src/docker-compose/      # Docker Compose files for the test environment
└── pom.xml                  # Maven configuration
```

### Base Classes
- **`IntegrationTestBase`** (`com.dotcms.IntegrationTestBase`): the common base class for most integration tests — JUnit 4, extends `BaseMessageResources`
- **`ContentTypeBaseTest`**, **`BaseWorkflowIntegrationTest`**: specialized subclasses of `IntegrationTestBase` for their respective areas
- **`Junit5WeldBaseTest`** (`com.dotcms.Junit5WeldBaseTest`): a standalone JUnit 5 base class for tests that need a real Weld CDI container, used by a minority of newer tests

### Test Runners (Data-Provider Parameterized Tests)
For JUnit4 tests that need `@DataProvider`-style parameterization (from the
`com.tngtech.junit.dataprovider` library — a real dependency declared in
`dotcms-integration/pom.xml`), annotate the class with one of these two `@RunWith` runners:
- **`DataProviderRunner`** (`com.tngtech.java.junit.dataprovider.DataProviderRunner`): plain data-provider-driven parameterized tests, no CDI container — **68 real usages** (`@RunWith(DataProviderRunner.class)`) in this module (e.g. `LanguageUtilTest.java`)
- **`DataProviderWeldRunner`** (`com.dotcms.DataProviderWeldRunner`): same, but extends `DataProviderRunner` to additionally spin up a real Weld CDI container so test classes can be resolved as CDI beans — **~41 real usages**. This is a CDI-aware superset, not a replacement for the plain runner; both stay in active use for different needs.

### Naming: use the `Test` suffix, not `IT` ⚠️

Integration test classes in this module are named `FooTest` or `FooIntegrationTest` — **not**
`FooIT`. The standard Maven Failsafe `*IT` convention does **not** apply here: failsafe in
`dotcms-integration/pom.xml` includes **only the aggregator suites**
(`**/MainSuite1*.java`, `**/MainSuite2*.java`, `**/MainSuite3*.java`, `**/Junit5Suite*.java`,
plus `**/QuickSuite.java` and `**/OpenSearchUpgradeSuite.java` in their profiles), so discovery
is driven entirely by suite registration and never by the class-name suffix. Surefire is skipped
in this module, so a `*Test` name here can't be mistaken for a unit test either.

Match the siblings already in your package — the module is ~680 `*Test.java` to ~19 `*IT.java`.

### Registering Tests in a MainSuite (CI gate) ⚠️

CI runs integration tests **only** through the JUnit `@SuiteClasses` aggregator suites in
`dotcms-integration/src/test/java/com/dotcms/` — currently `MainSuite1a`, `MainSuite1b`,
`MainSuite2a`, `MainSuite2b`, `MainSuite3a` (plus `Junit5Suite1` and `OpenSearchUpgradeSuite`,
see below). **This set isn't fixed** — a new `MainSuite3b` (or beyond) may get added later as the
module grows; check `dotcms-integration/src/test/java/com/dotcms/MainSuite*.java` for the
current list rather than assuming this doc's names are exhaustive or permanent. **A new test
class that is not listed in one of these suites compiles fine but is silently never executed in
CI** — green build, zero coverage. This is easy to miss because the class runs locally via
`-Dit.test=MyTestClass`.

When you add a new integration test class, register it:

1. **Pick the suite with the shortest current CI runtime** — not by package/feature grouping.
   The `MainSuite*` jobs (currently five — see the note above on this list growing over time)
   run in parallel in CI, so the goal is to keep their wall-clock times roughly balanced; adding
   to whichever suite already finishes fastest keeps the overall PR build time down. **Don't
   hardcode a specific suite name as "the fast one" in this doc or in memory** — suite runtimes
   shift over time as tests are added/removed elsewhere, so check current timings before each new
   registration. The real way to check: pull job start/completion times from **at least 3 recent,
   independent PR CI runs** (not just one — a single run can be skewed by runner contention or
   noise, and durations should agree before you trust them) —
   ```bash
   gh pr view <recent-PR-number> --repo dotCMS/core --json statusCheckRollup \
     -q '.statusCheckRollup[] | select(.name? and (.name | test("Integration Tests"))) | "\(.name) \(.startedAt) \(.completedAt)"'
   ```
   repeated across 3 different recent PR numbers, and compute the duration of each `MainSuite*`
   job in each (ignore `Junit5Suite1` and `OpenSearchUpgradeSuite` — those are structurally
   different, purpose-built suites, not general destinations for a new JUnit4 test). Add the new
   test to whichever `MainSuite*` is consistently fastest across the samples. If every current
   `MainSuite*` is consistently near-saturated and none is meaningfully faster, that's a signal a
   new `MainSuite` class may be needed — that's a call for whoever owns the CI/test infra, not
   something to decide unilaterally from a PR sample.
2. Add the `import` (alphabetized, matching the file's existing import block) **and** append the
   `Foo.class,` entry to the **end** of the `@SuiteClasses({ ... })` list — the real
   `@SuiteClasses` lists are not alphabetized or grouped by feature/package at all; new tests are
   consistently appended at the end.

```java
// Say the check in step 1 found MainSuite2b currently has the shortest CI runtime —
// register the new class there: import alphabetized with the rest, class entry appended
// at the end of the @SuiteClasses list. (MyNewFeatureIntegrationTest below is a placeholder —
// don't copy an existing suite member as your "new" entry, or you'll register it twice.)
// MainSuite2b.java
import com.dotcms.myfeature.MyNewFeatureIntegrationTest; // alphabetized import
...
@SuiteClasses({
    ...
    RoleResourceUsersIntegrationTest.class,
    com.dotmarketing.common.reindex.ReindexDeleteJournalTest.class,
    MyNewFeatureIntegrationTest.class,   // <-- appended at the end
})
```

Verify the suite still resolves the new class:

```bash
./mvnw test-compile -pl :dotcms-integration -DskipTests
```

### The build cache can skip the tests entirely ⚠️

A second way to get a **green build that ran nothing** — distinct from the suite-registration gate
above, and with the identical symptom.

The Maven build cache can short-circuit the failsafe execution:

```
[INFO] Skipping plugin execution (cached): failsafe:integration-test
[INFO] BUILD SUCCESS
```

`target/failsafe-reports/failsafe-summary.xml` then reads `<completed>0</completed>` and the exit
code is **0**. Nothing ran, and nothing said so.

**Disable the cache whenever you actually need the tests to execute:**

```bash
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false \
  -Dmaven.build.cache.enabled=false \
  -Dit.test=com.dotcms.example.MyTest
```

(`just test-integration-ide` already passes this.)

Two related traps in the same area:

- **Select by class, not by method.** `-Dit.test=Class#method` — and the `#methodA+methodB` form —
  have been observed selecting **nothing** on classes with a custom `@RunWith` runner. Run the
  whole class.
- **Use the fully-qualified class name** when the simple name is ambiguous. `MainSuite1a`, for
  example, registers two different classes called `PublisherAPIImplTest`.

**Always confirm `Tests run: N` in `dotcms-integration/target/failsafe-reports/*.txt`.** The exit
code alone does not tell you whether anything executed.

## Testing Patterns

### Integration Test Structure
```java
public class MyResourceIntegrationTest extends IntegrationTestBase {

    private ContentTypeAPI contentTypeAPI;
    private ContentletAPI contentletAPI;

    @Before
    public void setup() {
        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        contentletAPI = APILocator.getContentletAPI();
    }

    @Test
    public void testCreateContentType() throws Exception {
        // Given
        ContentType contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
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

### REST Endpoint Testing
REST resource integration tests exercise the resource class directly (not over HTTP), using
`WebResource.InitBuilder` with a mocked request/response pair — the same pattern real resources
use in production, and the one shown in
[Security Patterns → Authentication Check Pattern](../backend/SECURITY_BACKEND.md#authentication-check-pattern):

```java
public class MyResourceIntegrationTest extends IntegrationTestBase {

    @Test
    public void testGetById() throws Exception {
        HttpServletRequest request = new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/api/v1/myresource").request());
        HttpServletResponse response = new MockHttpResponse().response();

        MyResource resource = new MyResource();
        Response httpResponse = resource.getById(request, response, "some-id");

        assertEquals(200, httpResponse.getStatus());
    }
}
```

## Key Testing Areas

### 1. Content Management
```java
public class ContentletAPITest extends IntegrationTestBase {

    private ContentletAPI contentletAPI;
    private User systemUser;
    private Language defaultLanguage;

    @Before
    public void setup() {
        contentletAPI = APILocator.getContentletAPI();
        systemUser = APILocator.systemUser();
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
    }

    @Test
    public void testContentletLifecycle() throws Exception {
        // Create content type
        ContentType contentType = createTestContentType();

        // Create contentlet
        Contentlet contentlet = new Contentlet();
        contentlet.setContentTypeId(contentType.id());
        contentlet.setStringProperty("title", "Test Title");
        contentlet.setHost(Host.SYSTEM_HOST);
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

        // Verify deletion — findContentletByIdentifier throws DotContentletStateException
        // when nothing is found; it returns a Contentlet directly, never an Optional/null
        try {
            contentletAPI.findContentletByIdentifier(contentlet.getIdentifier(),
                false, defaultLanguage.getId(), systemUser, false);
            fail("Contentlet should have been deleted");
        } catch (DotContentletStateException expected) {
            // expected — content no longer exists
        }
    }
}
```

### 2. Workflow Integration
```java
public class WorkflowAPITest extends IntegrationTestBase {

    private WorkflowAPI workflowAPI;
    private User systemUser;

    @Before
    public void setup() {
        workflowAPI = APILocator.getWorkflowAPI();
        systemUser = APILocator.systemUser();
    }

    @Test
    public void testWorkflowFiresOnCheckin() throws Exception {
        // Given
        ContentType contentType = createTestContentType();
        Contentlet contentlet = createTestContentlet(contentType);

        // When — fires the contentlet's default workflow scheme as part of checkin
        WorkflowProcessor processor = workflowAPI.fireWorkflowPreCheckin(contentlet, systemUser);

        // Then
        assertNotNull(processor);
        assertTrue(processor.getContentlet().isLive());
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
# Run all integration tests (⚠️ 60+ min — avoid unless you mean it)
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false

# Run a specific test class or method
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTestClass
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTestClass#myMethod
```

### Running and Debugging ITs from IntelliJ IDEA

This is the recommended loop for local development — it's what backs the `just test-integration-ide` recipe referenced in the root `CLAUDE.md`.

**1. One-time setup**
Install the **Justfile** plugin for IntelliJ — it lets you run `just` recipes straight from the IDE, no terminal needed.

**2. Start the test environment**
Open the `justfile`, find the `test-integration-ide` recipe, and click the ▶ icon next to it:
```bash
just test-integration-ide     # Starts the Docker Compose test environment (Postgres, ES/OpenSearch) + dotCMS
```
Let it finish starting before continuing.

**3. Run or debug a test**
Open the integration test class you want to exercise. In the gutter next to the class name or a specific test method, click ▶ and choose:
- **Run '\<name\>'** — just run it
- **Debug '\<name\>'** — step through it (set a breakpoint first)

**4. Stop the environment when done**
```bash
just test-integration-stop    # Stops the services
```

## Maven Configuration

The real `maven-failsafe-plugin` config (`dotcms-integration/pom.xml`) is driven by two properties:
- `coreit.test.skip` — gates whether integration tests run at all (must be `false` to run any)
- `it.test.forkcount` — controls parallel JVM forks

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <skip>${coreit.test.skip}</skip>
        <forkCount>${it.test.forkcount}</forkCount>
        <reuseForks>true</reuseForks>
        <rerunFailingTestsCount>3</rerunFailingTestsCount>
    </configuration>
</plugin>
```
Discovery of which tests actually run is controlled by suite registration (see "Registering Tests in a MainSuite" above), not by an `<includes>` glob on individual test classes.

## Test Data Management

### Test Data Builders
```java
public class TestDataBuilder {

    public static ContentType createTestContentType() throws Exception {
        return ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
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

## CI/CD Integration

Integration tests run in `.github/workflows/cicd_comp_test-phase.yml`. The real workflow is
more involved than a simple "run on push" job: it fans out into a matrix across the OpenSearch
migration phases (`-Dopensearch.phase=<N>`) so the suite gets exercised against each phase of the
[ES → OpenSearch migration](../backend/OPENSEARCH_MIGRATION.md), plus a separate
`opensearch.upgrade.test` run. See the workflow file itself for the exact matrix/trigger
configuration rather than relying on a simplified snippet here — it changes independently of
this doc and is easy to let drift.

### Test Results
- **Failsafe Reports**: `dotcms-integration/target/failsafe-reports/`
- **JUnit XML**: `dotcms-integration/target/failsafe-reports/TEST-*.xml`

## Debugging Test Failures

### Local Debugging

#### 1. Adjust Log Levels
For the whole test run, edit a logger's `level` in
`dotcms-integration/src/test/resources/log4j2.xml` (e.g. bump `com.dotcms.yourpackage` to
`DEBUG`) — there's no Maven system property shortcut for this in this module.

For a single test, change the level at runtime with dotCMS's own `com.dotmarketing.util.Logger`
(not `java.util.logging.Logger` — a different class with a different API):

```java
@Test
public void testWithDebugInfo() throws Exception {
    Logger.setLevel("com.dotcms.yourpackage", "DEBUG");

    try {
        ContentType contentType = createTestContentType();
        Logger.debug(this, () -> "Created content type: " + contentType.id());
    } finally {
        Logger.setLevel("com.dotcms.yourpackage", "INFO");
    }
}
```

#### 2. Docker Environment Debugging
```bash
# Check container status
docker ps

# View container logs (container names depend on the compose project — check `docker ps` first)
docker logs <container-name>
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

# Check for port conflicts (e.g. something else already bound to Postgres' port)
netstat -tulpn | grep 5432
```

**Database Connection Issues**:
```java
@Test
public void testDatabaseConnection() throws Exception {
    try (Connection conn = DbConnectionFactory.getConnection()) {
        assertTrue(conn.isValid(5));

        // Test basic query
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT 1");
            assertTrue(rs.next());
        }
    }
}
```

## Best Practices

### ✅ Integration Test Standards
- **Test realistic scenarios**: Use actual services and data
- **Manage test data**: Create and clean up test data properly (see Test Data Builders above)
- **Use transactions**: Wrap tests in `HibernateUtil` transactions when possible (see Database Operations above)
- **Test edge cases**: Include error conditions and boundary cases
- **Verify side effects**: Check that operations have expected impacts

### ✅ Test Data Management
```java
public class TestDataManager {

    private static final List<String> createdContentTypes = new ArrayList<>();
    private static final List<String> createdContentlets = new ArrayList<>();

    public static ContentType createAndTrackContentType(String name) throws Exception {
        ContentType contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
            .name(name)
            .build();

        contentType = APILocator.getContentTypeAPI(APILocator.systemUser()).save(contentType);
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

### ✅ Fast vs. Comprehensive Subsets
There's no `@Category`-based fast/slow split in this module. The real mechanism for running a
fast subset locally is the `QuickSuite` (`**/QuickSuite.java`, activated via its own Maven
profile) — register a fast, cheap test there in addition to its `MainSuite*` entry if it's
meant to run in that quick subset too. Everything else runs through the full `MainSuite*`
aggregators (see "Registering Tests in a MainSuite" above).

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

### 1. Test Data Conflicts
```java
// Problem: Tests interfering with each other
// Solution: Use unique test data
@Test
public void testCreateUser() throws Exception {
    String uniqueEmail = "test_" + System.currentTimeMillis() + "@example.com";
    User user = createTestUser(uniqueEmail);

    // Test operations

    // Cleanup
    APILocator.getUserAPI().delete(user, APILocator.systemUser(), false);
}
```

### 2. Cache/Session State Leaking Between Tests
```java
// Problem: State from a previous test leaks into the next one
// Solution: Explicit cleanup in @After
@After
public void tearDown() throws Exception {
    // Clear caches
    CacheLocator.getCacheAdministrator().flushAll();
    
    // Close sessions
    HibernateUtil.closeSession();
}
```

## Integration with Development Workflow

### Pre-commit Integration Tests
```bash
# Run a name-matched subset of integration tests before commit
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=*APITest
```

## Location Information
- **Test Source**: `dotcms-integration/src/test/java`
- **Test Resources**: `dotcms-integration/src/test/resources`
- **Test Reports**: `dotcms-integration/target/failsafe-reports/`
- **Docker Compose Config**: `dotcms-integration/src/docker-compose/it-test`
- **Maven Plugin**: Failsafe plugin configuration in `dotcms-integration/pom.xml`
