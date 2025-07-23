# API Testing (Postman & Karate)

## Overview

dotCMS uses two complementary API testing frameworks: **Postman** (legacy) and **Karate** (modern replacement). Both test REST APIs and GraphQL endpoints, with Karate being the preferred approach for new tests.

## dotCMS Postman Tests

### Location & Structure
- **Path**: `dotcms-postman/`
- **Framework**: Newman (Postman CLI) with Node.js
- **Test Runner**: Maven with frontend-maven-plugin
- **Status**: Legacy - being replaced by Karate

### Collection Structure
```
dotcms-postman/
├── src/main/resources/
│   ├── collections/              # Postman collections
│   │   ├── ContentResource.json  # Content API tests
│   │   ├── GraphQL.json          # GraphQL API tests  
│   │   ├── Workflow.json         # Workflow API tests
│   │   └── UserResource.json     # User management tests
│   └── environments/             # Environment configurations
│       ├── localhost.json        # Local environment
│       └── demo.json             # Demo environment
├── package.json                  # Node.js dependencies
└── pom.xml                       # Maven configuration
```

### Test Collection Example
```json
{
    "info": {
        "name": "Content Resource Tests",
        "description": "Tests for Content API endpoints",
        "version": "1.0.0"
    },
    "item": [
        {
            "name": "Create Content",
            "request": {
                "method": "POST",
                "header": [
                    {
                        "key": "Content-Type",
                        "value": "application/json"
                    }
                ],
                "url": {
                    "raw": "{{serverURL}}/api/v1/content",
                    "host": ["{{serverURL}}"],
                    "path": ["api", "v1", "content"]
                },
                "body": {
                    "mode": "raw",
                    "raw": "{\n  \"contentType\": \"webPageContent\",\n  \"title\": \"Test Page\",\n  \"body\": \"Test content body\"\n}"
                }
            },
            "event": [
                {
                    "listen": "test",
                    "script": {
                        "type": "text/javascript",
                        "exec": [
                            "pm.test('Status code is 200', function () {",
                            "    pm.response.to.have.status(200);",
                            "});",
                            "",
                            "pm.test('Response has identifier', function () {",
                            "    var jsonData = pm.response.json();",
                            "    pm.expect(jsonData.entity.identifier).to.exist;",
                            "});",
                            "",
                            "// Store identifier for subsequent tests",
                            "var responseJson = pm.response.json();",
                            "pm.globals.set('contentId', responseJson.entity.identifier);"
                        ]
                    }
                }
            ]
        },
        {
            "name": "Get Content",
            "request": {
                "method": "GET",
                "url": {
                    "raw": "{{serverURL}}/api/v1/content/{{contentId}}",
                    "host": ["{{serverURL}}"],
                    "path": ["api", "v1", "content", "{{contentId}}"]
                }
            },
            "event": [
                {
                    "listen": "test",
                    "script": {
                        "type": "text/javascript",
                        "exec": [
                            "pm.test('Status code is 200', function () {",
                            "    pm.response.to.have.status(200);",
                            "});",
                            "",
                            "pm.test('Content matches created content', function () {",
                            "    var jsonData = pm.response.json();",
                            "    pm.expect(jsonData.entity.title).to.eql('Test Page');",
                            "});"
                        ]
                    }
                }
            ]
        }
    ]
}
```

### Running Postman Tests
```bash
# Run all Postman tests (requires -Dpostman.collections=all to execute all collections)
./mvnw verify -Dpostman.test.skip=false -pl :dotcms-postman -Dpostman.collections=all

# Run specific collection group (see config.json for available groups)
./mvnw verify -Dpostman.test.skip=false -pl :dotcms-postman -Dpostman.collections=ai

# Run specific collection
./mvnw verify -Dpostman.test.skip=false -pl :dotcms-postman -Dpostman.collection=ContentResource

# Run with specific environment
./mvnw verify -Dpostman.test.skip=false -pl :dotcms-postman -Dpostman.environment=localhost

# Run with debug output
./mvnw verify -Dpostman.test.skip=false -pl :dotcms-postman -Dpostman.debug=true
```

### Maven Configuration
```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>install-node-and-npm</id>
            <goals>
                <goal>install-node-and-npm</goal>
            </goals>
            <configuration>
                <nodeVersion>v18.16.0</nodeVersion>
                <npmVersion>9.5.1</npmVersion>
            </configuration>
        </execution>
        <execution>
            <id>npm-install</id>
            <goals>
                <goal>npm</goal>
            </goals>
            <configuration>
                <arguments>install</arguments>
            </configuration>
        </execution>
        <execution>
            <id>run-postman-tests</id>
            <goals>
                <goal>npx</goal>
            </goals>
            <configuration>
                <arguments>newman run collections/${postman.collection}.json --environment environments/${postman.environment}.json --reporters cli,junit --reporter-junit-export target/newman-results.xml</arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Karate Framework Tests

### Location & Structure
- **Path**: `test-karate/`
- **Framework**: Karate (BDD for API testing)
- **Test Runner**: Maven Failsafe plugin
- **Status**: Modern - preferred for new tests

### Project Structure
```
test-karate/
├── src/test/java/
│   ├── com/dotcms/
│   │   ├── graphql/              # GraphQL tests
│   │   ├── rest/                 # REST API tests
│   │   ├── timemachine/          # Time machine tests
│   │   └── KarateTestRunner.java # Test runner
│   └── resources/
│       ├── karate-config.js      # Global configuration
│       └── com/dotcms/           # Feature files
├── docker/                       # Docker test environment
└── pom.xml                       # Maven configuration
```

### Karate Configuration
```javascript
// karate-config.js
function fn() {
    var config = {
        baseUrl: 'http://localhost:8080',
        adminUser: 'admin@dotcms.com',
        adminPassword: 'admin',
        testDataPath: 'classpath:test-data/',
        waitTime: 5000
    };
    
    // Environment-specific overrides
    if (karate.env === 'demo') {
        config.baseUrl = 'https://demo.dotcms.com';
    }
    
    if (karate.env === 'ci') {
        config.baseUrl = 'http://dotcms-container:8080';
    }
    
    return config;
}
```

### REST API Test Example
```gherkin
Feature: Content API Testing

Background:
    * url baseUrl
    * def adminAuth = { username: '#(adminUser)', password: '#(adminPassword)' }

Scenario: Create and retrieve content
    # Authentication
    Given path '/api/v1/authentication/api-token'
    And header Content-Type = 'application/json'
    And request adminAuth
    When method POST
    Then status 200
    And def token = response.entity.token
    
    # Create content
    Given path '/api/v1/content'
    And header Authorization = 'Bearer ' + token
    And header Content-Type = 'application/json'
    And request
    """
    {
        "contentType": "webPageContent",
        "title": "Test Page from Karate",
        "body": "This is test content created by Karate"
    }
    """
    When method POST
    Then status 200
    And match response.entity.identifier == '#present'
    And def contentId = response.entity.identifier
    
    # Retrieve content
    Given path '/api/v1/content/' + contentId
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response.entity.title == 'Test Page from Karate'
    And match response.entity.body == 'This is test content created by Karate'
    
    # Cleanup
    Given path '/api/v1/content/' + contentId
    And header Authorization = 'Bearer ' + token
    When method DELETE
    Then status 200
```

### GraphQL Test Example
```gherkin
Feature: GraphQL API Testing

Background:
    * url baseUrl
    * def adminAuth = { username: '#(adminUser)', password: '#(adminPassword)' }

Scenario: Query content types via GraphQL
    # Authentication
    Given path '/api/v1/authentication/api-token'
    And header Content-Type = 'application/json'
    And request adminAuth
    When method POST
    Then status 200
    And def token = response.entity.token
    
    # GraphQL query
    Given path '/api/v1/graphql'
    And header Authorization = 'Bearer ' + token
    And header Content-Type = 'application/json'
    And request
    """
    {
        "query": "query { contentTypes { variable name description } }"
    }
    """
    When method POST
    Then status 200
    And match response.data.contentTypes == '#[]'
    And match response.data.contentTypes[0].variable == '#string'
    And match response.data.contentTypes[0].name == '#string'
```

### Time Machine Test Example
```gherkin
Feature: Time Machine API Testing

Background:
    * url baseUrl
    * def adminAuth = { username: '#(adminUser)', password: '#(adminPassword)' }

Scenario: Test content versioning with time machine
    # Authentication
    Given path '/api/v1/authentication/api-token'
    And header Content-Type = 'application/json'
    And request adminAuth
    When method POST
    Then status 200
    And def token = response.entity.token
    
    # Create content at time T1
    Given path '/api/v1/content'
    And header Authorization = 'Bearer ' + token
    And header Content-Type = 'application/json'
    And request
    """
    {
        "contentType": "webPageContent",
        "title": "Version 1",
        "body": "Initial version"
    }
    """
    When method POST
    Then status 200
    And def contentId = response.entity.identifier
    And def timestamp1 = response.entity.modDate
    
    # Wait to ensure timestamp difference
    * def sleep = function(ms) { java.lang.Thread.sleep(ms) }
    * sleep(1000)
    
    # Update content at time T2
    Given path '/api/v1/content/' + contentId
    And header Authorization = 'Bearer ' + token
    And header Content-Type = 'application/json'
    And request
    """
    {
        "title": "Version 2",
        "body": "Updated version"
    }
    """
    When method PUT
    Then status 200
    And def timestamp2 = response.entity.modDate
    
    # Query content at T1 using time machine
    Given path '/api/v1/content/' + contentId
    And header Authorization = 'Bearer ' + token
    And header timemachine-date = timestamp1
    When method GET
    Then status 200
    And match response.entity.title == 'Version 1'
    And match response.entity.body == 'Initial version'
    
    # Query content at T2 (current)
    Given path '/api/v1/content/' + contentId
    And header Authorization = 'Bearer ' + token
    When method GET
    Then status 200
    And match response.entity.title == 'Version 2'
    And match response.entity.body == 'Updated version'
```

### Running Karate Tests
```bash
# Run all Karate tests
./mvnw verify -Dkarate.test.skip=false -pl :dotcms-test-karate

# Run specific test
./mvnw verify -Dkarate.test.skip=false -pl :dotcms-test-karate -Dtest=ContentAPITest

# Run with specific environment
./mvnw verify -Dkarate.test.skip=false -pl :dotcms-test-karate -Dkarate.env=demo

# Run with parallel execution
./mvnw verify -Dkarate.test.skip=false -pl :dotcms-test-karate -Dkarate.options="--threads 4"

# Run with debug output
./mvnw verify -Dkarate.test.skip=false -pl :dotcms-test-karate -Dkarate.options="--debug"
```

### Test Runner Configuration
```java
@RunWith(Karate.class)
public class KarateTestRunner {
    
    @Test
    public Karate testAll() {
        return Karate.run("classpath:com/dotcms")
            .relativeTo(getClass())
            .parallel(Runtime.getRuntime().availableProcessors());
    }
    
    @Test
    public Karate testGraphQL() {
        return Karate.run("classpath:com/dotcms/graphql")
            .relativeTo(getClass());
    }
    
    @Test
    public Karate testRest() {
        return Karate.run("classpath:com/dotcms/rest")
            .relativeTo(getClass());
    }
}
```

## CI/CD Integration

### GitHub Actions Integration
**Workflow**: API tests run in `cicd_comp_test-phase.yml`

**Change Detection**: Tests triggered by:
```yaml
backend: &backend
  - 'dotCMS/src/main/java/**'
  - 'dotcms-postman/**'
  - 'test-karate/**'
  - 'pom.xml'
```

**Postman Execution**:
```yaml
- name: Run Postman Tests
  run: |
    ./mvnw verify -Dpostman.test.skip=false -pl :dotcms-postman
  env:
    POSTMAN_ENVIRONMENT: ci
```

**Karate Execution**:
```yaml
- name: Run Karate Tests
  run: |
    ./mvnw verify -Dkarate.test.skip=false -pl :dotcms-test-karate
  env:
    KARATE_ENV: ci
    KARATE_OPTIONS: "--threads 2"
```

### Docker Environment
```yaml
services:
  dotcms:
    image: dotcms/dotcms:latest
    environment:
      - DB_BASE_URL=jdbc:postgresql://postgres:5432/dotcms
      - DB_USERNAME=dotcms
      - DB_PASSWORD=dotcms
    depends_on:
      - postgres
    
  postgres:
    image: postgres:13
    environment:
      - POSTGRES_DB=dotcms
      - POSTGRES_USER=dotcms
      - POSTGRES_PASSWORD=dotcms
```

### Test Results
- **Postman**: `dotcms-postman/target/newman-results.xml`
- **Karate**: `test-karate/target/karate-reports/`
- **Coverage**: Integrated with backend coverage reports

## Debugging API Test Failures

### Local Debugging

#### 1. Postman Debug Mode
```bash
# Run with debug output
./mvnw verify -Dpostman.test.skip=false -pl :dotcms-postman -Dpostman.debug=true

# Run single collection with verbose output
npx newman run collections/ContentResource.json --environment environments/localhost.json --verbose
```

#### 2. Karate Debug Mode
```bash
# Run with debug output
./mvnw verify -Dkarate.test.skip=false -pl :dotcms-test-karate -Dkarate.options="--debug"

# Run single feature file
./mvnw verify -Dkarate.test.skip=false -pl :dotcms-test-karate -Dtest=ContentAPITest
```

#### 3. API Endpoint Testing
```bash
# Test API endpoint directly
curl -X POST http://localhost:8080/api/v1/authentication/api-token \
  -H "Content-Type: application/json" \
  -d '{"username": "admin@dotcms.com", "password": "admin"}'

# Test with authentication
curl -X GET http://localhost:8080/api/v1/content \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### GitHub Actions Debugging

#### 1. Environment Variables
```yaml
- name: Debug API Tests
  run: |
    echo "Base URL: ${{ env.BASE_URL }}"
    echo "Environment: ${{ env.KARATE_ENV }}"
    curl -I ${{ env.BASE_URL }}/api/v1/health
```

#### 2. Upload Test Reports
```yaml
- name: Upload API Test Results
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: api-test-results
    path: |
      dotcms-postman/target/newman-results.xml
      test-karate/target/karate-reports/
      test-karate/target/failsafe-reports/
```

#### 3. Common Failure Patterns

**Authentication Issues**:
```gherkin
# Debug authentication in Karate
Scenario: Debug authentication
    Given path '/api/v1/authentication/api-token'
    And header Content-Type = 'application/json'
    And request { username: '#(adminUser)', password: '#(adminPassword)' }
    When method POST
    Then status 200
    * print 'Auth response:', response
    * def token = response.entity.token
    * print 'Token:', token
```

**Network Issues**:
```javascript
// Debug network in Postman
console.log('Base URL:', pm.environment.get('serverURL'));
console.log('Request URL:', pm.request.url.toString());
console.log('Response status:', pm.response.status);
console.log('Response time:', pm.response.responseTime);
```

**Data Issues**:
```gherkin
# Debug data in Karate
Scenario: Debug test data
    * print 'Test data path:', testDataPath
    * def testData = read('classpath:test-data/sample-content.json')
    * print 'Test data:', testData
```

## Migration from Postman to Karate

### Migration Strategy
1. **Identify collections**: Map Postman collections to Karate features
2. **Convert requests**: Transform Postman requests to Karate scenarios
3. **Migrate assertions**: Convert Postman tests to Karate match statements
4. **Update environments**: Migrate environment variables to karate-config.js

### Conversion Examples

**Postman Request**:
```javascript
// Pre-request script
pm.globals.set('timestamp', Date.now());

// Test script
pm.test('Status code is 200', function () {
    pm.response.to.have.status(200);
});

pm.test('Response has identifier', function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.entity.identifier).to.exist;
    pm.globals.set('contentId', jsonData.entity.identifier);
});
```

**Karate Equivalent**:
```gherkin
Scenario: Create content
    * def timestamp = function() { return java.lang.System.currentTimeMillis() }
    * def currentTime = timestamp()
    
    Given path '/api/v1/content'
    And header Authorization = 'Bearer ' + token
    And header Content-Type = 'application/json'
    And request contentData
    When method POST
    Then status 200
    And match response.entity.identifier == '#present'
    And def contentId = response.entity.identifier
```

## Best Practices

### ✅ API Testing Standards
- **Test realistic scenarios**: Use actual API workflows
- **Include error cases**: Test authentication failures, validation errors
- **Use proper assertions**: Verify response structure and content
- **Manage test data**: Create and clean up test data properly
- **Document API contracts**: Keep tests aligned with API documentation

### ✅ Karate Best Practices
```gherkin
# Use background for common setup
Background:
    * url baseUrl
    * def credentials = { username: '#(adminUser)', password: '#(adminPassword)' }
    * call read('classpath:auth.feature') credentials

# Use scenario outline for data-driven tests
Scenario Outline: Test content creation with different types
    Given path '/api/v1/content'
    And header Authorization = 'Bearer ' + token
    And request { contentType: '<contentType>', title: '<title>' }
    When method POST
    Then status 200
    And match response.entity.contentType == '<contentType>'
    
    Examples:
    | contentType | title |
    | webPage     | Test Page |
    | newsItem    | Test News |
    | blogPost    | Test Blog |
```

### ✅ Test Organization
```
src/test/resources/
├── auth.feature                  # Reusable authentication
├── cleanup.feature               # Test data cleanup
├── com/dotcms/
│   ├── content/
│   │   ├── content-crud.feature  # Content CRUD operations
│   │   └── content-search.feature # Content search
│   ├── workflow/
│   │   └── workflow-actions.feature # Workflow testing
│   └── graphql/
│       └── graphql-queries.feature # GraphQL testing
```

## Performance Considerations

### Parallel Execution
```xml
<!-- Karate parallel execution -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <systemPropertyVariables>
            <karate.options>--threads 4</karate.options>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

### Test Data Management
```gherkin
# Efficient test data creation
Background:
    * def testData = 
    """
    {
        "contentType": "webPageContent",
        "title": "Test Page #(karate.uuid())",
        "body": "Test content body"
    }
    """
    
    * def cleanup = []
    * configure afterScenario = function() { karate.call('cleanup.feature', { items: cleanup }) }
```

## Location Information
- **Postman Tests**: `dotcms-postman/src/main/resources/collections/`
- **Karate Tests**: `test-karate/src/test/resources/com/dotcms/`
- **Test Reports**: `dotcms-postman/target/newman-results.xml`, `test-karate/target/karate-reports/`
- **Configuration**: `test-karate/src/test/resources/karate-config.js`
- **Docker Environment**: `test-karate/docker/`