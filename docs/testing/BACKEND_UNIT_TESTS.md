# Backend Unit Tests

## Overview

Backend unit tests in dotCMS are located in `dotCMS/src/test/java` and use JUnit 4/5 with Mockito for mocking. These tests focus on isolated unit testing of individual classes and methods without external dependencies.

## Test Structure

### Location
- **Main Path**: `dotCMS/src/test/java`
- **Mirror Structure**: Follows the same package structure as `dotCMS/src/main/java`
- **Test Runner**: Maven Surefire plugin
- **Framework**: JUnit 4/5 (primarily JUnit 4)

### Base Classes
- **`UnitTestBase`**: Common setup for unit tests
- **`UnitTestBaseMarker`**: Marker interface for test categorization
- **Mock utilities**: Extensive use of Mockito for dependency mocking
- **Class metadata scanning**: Use `JandexClassMetadataScanner` for high-performance class analysis (see [Jandex Integration](../backend/JANDEX_METADATA_SCANNING.md))

## Testing Patterns

### Standard Unit Test Structure
```java
public class ContentTypeValidatorTest {
    
    @Mock
    private ContentTypeAPI contentTypeAPI;
    
    @InjectMocks
    private ContentTypeValidator validator;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    public void testValidContentType() {
        // Given
        ContentType contentType = createMockContentType();
        when(contentTypeAPI.find(anyString())).thenReturn(contentType);
        
        // When
        ValidationResult result = validator.validate(contentType);
        
        // Then
        assertTrue(result.isValid());
        verify(contentTypeAPI).find(contentType.id());
    }
}
```

### Common Annotations
- **`@Test`**: Marks test methods
- **`@Before`**: Setup before each test
- **`@After`**: Cleanup after each test
- **`@BeforeClass`**: One-time setup for test class
- **`@AfterClass`**: One-time cleanup for test class
- **`@Mock`**: Create mock objects
- **`@InjectMocks`**: Inject mocks into test subject

### Test Naming Conventions
```java
// Pattern: test[MethodName][Scenario][ExpectedResult]
@Test
public void testValidateContentType_WhenValidInput_ShouldReturnSuccess() {
    // Test implementation
}

@Test
public void testValidateContentType_WhenInvalidInput_ShouldThrowException() {
    // Test implementation
}
```

## Key Testing Areas

### 1. Validators
```java
public class DateValidatorTest {
    
    @Test
    public void testValidDate() throws AnalyticsValidator.AnalyticsValidationException {
        DateValidator validator = new DateValidator();
        String validDate = "2025-06-09T14:30:00+02:00";
        
        // Should not throw exception
        validator.validate(validDate);
    }
    
    @Test(expected = AnalyticsValidator.AnalyticsValidationException.class)
    public void testInvalidDate() throws AnalyticsValidator.AnalyticsValidationException {
        DateValidator validator = new DateValidator();
        String invalidDate = "invalid-date";
        
        validator.validate(invalidDate);
    }
}
```

### 2. Utility Classes
```java
public class StringUtilsTest {
    
    @Test
    public void testIsEmpty_WhenNull_ShouldReturnTrue() {
        assertTrue(StringUtils.isEmpty(null));
    }
    
    @Test
    public void testIsEmpty_WhenEmptyString_ShouldReturnTrue() {
        assertTrue(StringUtils.isEmpty(""));
    }
    
    @Test
    public void testIsEmpty_WhenWhitespace_ShouldReturnFalse() {
        assertFalse(StringUtils.isEmpty(" "));
    }
}
```

### 3. Business Logic
```java
public class WorkflowManagerTest {
    
    @Mock
    private WorkflowAPI workflowAPI;
    
    @Mock
    private ContentletAPI contentletAPI;
    
    @InjectMocks
    private WorkflowManager workflowManager;
    
    @Test
    public void testProcessWorkflow_WhenValidContentlet_ShouldExecuteAction() {
        // Given
        Contentlet contentlet = createMockContentlet();
        WorkflowAction action = createMockAction();
        when(workflowAPI.findAction(anyString())).thenReturn(action);
        
        // When
        WorkflowResult result = workflowManager.processWorkflow(contentlet, action);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(workflowAPI).executeAction(contentlet, action);
    }
}
```

## Running Tests

### Command Line Execution
```bash
# Run all unit tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=ContentTypeValidatorTest

# Run tests matching pattern
./mvnw test -Dtest=*ValidatorTest

# Run tests with specific profile
./mvnw test -Punit-tests

# Run tests with coverage
./mvnw test -Pcoverage

# Skip tests
./mvnw install -DskipTests
```

### Maven Configuration
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*Test.java</include>
            <include>**/*Tests.java</include>
        </includes>
        <excludes>
            <exclude>**/*IntegrationTest.java</exclude>
            <exclude>**/*IT.java</exclude>
        </excludes>
    </configuration>
</plugin>
```

### Test Categories
```java
// Fast unit tests
@Category(UnitTestBaseMarker.class)
public class FastUnitTest {
    // Quick tests without external dependencies
}

// Slow unit tests
@Category(SlowTestMarker.class)
public class SlowUnitTest {
    // Tests that require more setup or computation
}
```

## CI/CD Integration

### GitHub Actions Integration
**Workflow**: Unit tests run in `cicd_comp_test-phase.yml`

**Change Detection**: Tests triggered by:
```yaml
backend: &backend
  - 'dotCMS/src/main/java/**'
  - 'dotCMS/src/test/java/**'
  - 'pom.xml'
  - '**/pom.xml'
```

**Execution**:
```yaml
- name: Run Backend Unit Tests
  run: ./mvnw test -pl :dotcms-core
  env:
    MAVEN_OPTS: -Xmx2g
```

### Test Results
- **JUnit XML Reports**: `target/surefire-reports/`
- **Code Coverage**: Generated with JaCoCo
- **GitHub Actions**: Test results displayed in PR checks

## Debugging Test Failures

### Local Debugging

#### 1. Run Single Test with Debug
```bash
# Run specific test with debug output
./mvnw test -Dtest=ContentTypeValidatorTest -X

# Run with JVM debug
./mvnw test -Dtest=ContentTypeValidatorTest -Dmaven.surefire.debug
```

#### 2. IDE Integration
```java
// Add debugging breakpoints
@Test
public void testValidateContentType() {
    // Set breakpoint here
    ContentType contentType = createMockContentType();
    
    // Debug step through validation
    ValidationResult result = validator.validate(contentType);
    
    // Examine result
    assertTrue(result.isValid());
}
```

#### 3. Logging Configuration
```xml
<!-- logback-test.xml -->
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.dotcms" level="DEBUG"/>
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

### GitHub Actions Debugging

#### 1. Enable Debug Logging
```yaml
- name: Run Backend Unit Tests
  run: ./mvnw test -pl :dotcms-core -X
  env:
    MAVEN_OPTS: -Xmx2g
    MAVEN_DEBUG: true
```

#### 2. Access Test Reports
```yaml
- name: Upload Test Results
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: test-results
    path: |
      target/surefire-reports/
      target/site/jacoco/
```

#### 3. Common Failure Patterns

**Memory Issues**:
```bash
# Increase memory for tests
export MAVEN_OPTS="-Xmx4g -XX:MaxPermSize=512m"
./mvnw test
```

**Mock Issues**:
```java
// Verify mock interactions
@Test
public void testMethod() {
    // Test code
    
    // Debug mock interactions
    verify(mockService, times(1)).methodCall(any());
    verifyNoMoreInteractions(mockService);
}
```

**Timing Issues**:
```java
// Add appropriate timeouts
@Test(timeout = 5000)
public void testAsyncOperation() {
    // Test asynchronous code
}
```

## Best Practices

### ✅ Writing Effective Unit Tests
- **Test one thing**: Each test should verify one specific behavior
- **Use descriptive names**: Test names should explain what is being tested
- **Follow AAA pattern**: Arrange, Act, Assert
- **Mock external dependencies**: Keep tests isolated and fast
- **Test edge cases**: Include boundary conditions and error scenarios

### ✅ Mock Management
```java
@Mock
private ExternalService externalService;

@Test
public void testServiceCall() {
    // Setup mock behavior
    when(externalService.getData()).thenReturn(expectedData);
    
    // Execute test
    Result result = serviceUnderTest.processData();
    
    // Verify interactions
    verify(externalService).getData();
    assertEquals(expectedResult, result);
}
```

### ✅ Test Data Management
```java
// Use builder pattern for test data
public class ContentTypeTestDataBuilder {
    private String name = "default";
    private String description = "default description";
    
    public ContentTypeTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public ContentType build() {
        return new ContentType(name, description);
    }
}

@Test
public void testContentTypeValidation() {
    ContentType contentType = new ContentTypeTestDataBuilder()
        .withName("TestType")
        .build();
    
    ValidationResult result = validator.validate(contentType);
    assertTrue(result.isValid());
}
```

### ✅ Exception Testing
```java
@Test(expected = ValidationException.class)
public void testValidation_WhenInvalidInput_ShouldThrowException() {
    validator.validate(null);
}

// Or with JUnit 5
@Test
public void testValidation_WhenInvalidInput_ShouldThrowException() {
    assertThrows(ValidationException.class, () -> {
        validator.validate(null);
    });
}
```

## Common Issues and Solutions

### 1. Mock Not Working
```java
// Problem: Mock not being used
@Mock
private SomeService someService;

// Solution: Initialize mocks
@Before
public void setUp() {
    MockitoAnnotations.initMocks(this);
}
```

### 2. Test Isolation Issues
```java
// Problem: Tests affecting each other
@After
public void tearDown() {
    // Reset static state
    SomeStaticClass.reset();
    
    // Clear thread local variables
    ThreadLocalManager.clear();
}
```

### 3. Flaky Tests
```java
// Problem: Tests passing/failing randomly
@Test
public void testTimeSensitiveOperation() {
    // Solution: Use fixed time or mock time
    Clock fixedClock = Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);
    
    // Test with fixed time
    assertTimeBasedOperation(fixedClock);
}
```

## Performance Considerations

### Test Execution Speed
```bash
# Run tests in parallel
./mvnw test -T 1C

# Run only fast tests
./mvnw test -Dgroups=fast

# Use test categories
./mvnw test -Dexclude.groups=slow
```

### Memory Management
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <forkCount>1</forkCount>
        <reuseForks>true</reuseForks>
        <argLine>-Xmx2g -XX:MaxPermSize=256m</argLine>
    </configuration>
</plugin>
```

## Integration with Development Workflow

### Pre-commit Testing
```bash
# Run relevant tests before commit
./mvnw test -Dtest=*$(git diff --name-only | grep Test.java | sed 's/.*\///;s/\.java//')
```

### IDE Integration
- **IntelliJ**: Run tests with Ctrl+Shift+F10
- **Eclipse**: Run tests with Alt+Shift+X, T
- **VS Code**: Use Java Test Runner extension

## Location Information
- **Test Source**: `dotCMS/src/test/java`
- **Test Resources**: `dotCMS/src/test/resources`
- **Test Reports**: `target/surefire-reports/`
- **Coverage Reports**: `target/site/jacoco/`
- **Maven Plugin**: Surefire plugin configuration in `dotCMS/pom.xml`