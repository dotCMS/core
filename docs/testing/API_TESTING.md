# API Testing (Postman)

## Overview

dotCMS API tests are Postman collections run with Newman. They test REST APIs and GraphQL endpoints. For how to write and run a collection, see the Postman module's [Readme](../../dotcms-postman/src/main/resources/postman/Readme.md).

## dotCMS Postman Tests

### Location & Structure
- **Path**: `dotcms-postman/`
- **Framework**: Newman (Postman CLI) with Node.js
- **Test Runner**: Maven with frontend-maven-plugin

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

## CI/CD Integration

### GitHub Actions Integration
**Workflow**: API tests run in `cicd_comp_test-phase.yml`

**Change Detection**: Tests triggered by:
```yaml
backend: &backend
  - 'dotCMS/src/main/java/**'
  - 'dotcms-postman/**'
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

#### 2. API Endpoint Testing
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
```

#### 3. Common Failure Patterns

**Network Issues**:
```javascript
// Debug network in Postman
console.log('Base URL:', pm.environment.get('serverURL'));
console.log('Request URL:', pm.request.url.toString());
console.log('Response status:', pm.response.status);
console.log('Response time:', pm.response.responseTime);
```

## Best Practices

### ✅ API Testing Standards
- **Test realistic scenarios**: Use actual API workflows
- **Include error cases**: Test authentication failures, validation errors
- **Use proper assertions**: Verify response structure and content
- **Manage test data**: Create and clean up test data properly
- **Document API contracts**: Keep tests aligned with API documentation

## Location Information
- **Postman Tests**: `dotcms-postman/src/main/resources/postman/` (see its [Readme](../../dotcms-postman/src/main/resources/postman/Readme.md))
- **Test Reports**: `dotcms-postman/target/newman-results.xml`
