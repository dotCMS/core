# dotCMS CLI Development Guide

## Overview

The dotCMS CLI is a Quarkus-based command-line tool for interacting with dotCMS instances. It provides automated operations for content management, file handling, and site operations through a command shell interface.

## Architecture

### Technology Stack
- **Framework**: Quarkus 3.6.0
- **Language**: Java 21 (CLI tools exception - uses full Java 21 features)
- **Build Tool**: Maven
- **CLI Framework**: PicocLI
- **Native Compilation**: GraalVM Native Image support
- **Security**: Java Keytar for credential storage

### Project Structure
```
tools/dotcms-cli/
├── api-data-model/          # API data models and client interfaces
├── cli/                     # Main CLI application
├── pom.xml                  # Parent POM with Quarkus BOM
└── README.md               # CLI documentation
```

### Module Organization

#### API Data Model (`api-data-model/`)
**Purpose**: Shared data models and API interfaces

**Key Components**:
- **API Interfaces**: `ContentTypeAPI`, `AssetAPI`, `SiteAPI`, `AuthenticationAPI`
- **Data Models**: Content types, assets, sites, authentication
- **Client Management**: `RestClientFactory`, `ServiceManager`
- **Traversal System**: File and content tree navigation

#### CLI Application (`cli/`)
**Purpose**: Command-line interface and business logic

**Key Components**:
- **Commands**: Organized by domain (content-type, files, language, site)
- **Services**: Push/pull operations, analytics, file handling
- **Security**: Password storage, encryption utilities
- **Common**: Shared utilities, mixins, exception handling

## Java 21 Features (CLI Exception)

Unlike the main dotCMS application, the CLI uses **full Java 21 features**:

### Modern Java Patterns
```java
// Record classes for data transfer
public record PushContext(
    String workspace,
    List<String> patterns,
    boolean dryRun,
    boolean interactive
) {}

// Text blocks for multi-line strings
var help = """
    Usage: dotcli push [OPTIONS] [PATTERNS...]
    
    Push content to dotCMS instance
    
    Options:
        --dry-run    Show what would be pushed
        --interactive    Prompt for confirmation
    """;

// Pattern matching and switch expressions
var result = switch (status) {
    case SUCCESS -> "Operation completed successfully";
    case FAILED -> "Operation failed: " + error.getMessage();
    case PENDING -> "Operation is pending";
    default -> "Unknown status";
};
```

### Quarkus-Specific Features
```java
// CDI with Quarkus annotations
@ApplicationScoped
public class PushServiceImpl implements PushService {
    
    @Inject
    ServiceManager serviceManager;
    
    @ConfigProperty(name = "dotcli.batch.size", defaultValue = "100")
    int batchSize;
}

// Native image configuration
@RegisterForReflection
public class ContentTypeCommand {
    // Command implementation
}
```

## Command Structure

### Base Command Pattern
```java
@Command(
    name = "content-type",
    description = "Operations over content types"
)
public class ContentTypeCommand implements Callable<Integer> {
    
    @Mixin
    GlobalMixin globalMixin;
    
    @Mixin
    AuthenticationMixin authMixin;
    
    @Override
    public Integer call() {
        return CommandLine.ExitCode.OK;
    }
}
```

### Subcommand Pattern
```java
@Command(
    name = "push",
    description = "Push content types to dotCMS"
)
public class ContentTypePush extends AbstractContentTypeCommand {
    
    @Mixin
    ContentTypePushMixin pushMixin;
    
    @Override
    public Integer call() throws Exception {
        // Push implementation
        return CommandLine.ExitCode.OK;
    }
}
```

## Build System

### Maven Configuration
**Parent POM**: [`tools/dotcms-cli/pom.xml`](../../tools/dotcms-cli/pom.xml)

```xml
<properties>
    <quarkus.platform.version>3.6.0</quarkus.platform.version>
    <jreleaser-plugin.version>1.8.0</jreleaser-plugin.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.quarkus.platform</groupId>
            <artifactId>quarkus-bom</artifactId>
            <version>${quarkus.platform.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### CLI Module POM
**CLI POM**: [`tools/dotcms-cli/cli/pom.xml`](../../tools/dotcms-cli/cli/pom.xml)

**Key Dependencies**:
- `quarkus-resteasy-reactive-jackson` - REST client
- `quarkus-picocli` - Command-line interface
- `quarkus-config-yaml` - Configuration management
- `java-keytar` - Secure credential storage

## Development Workflow

### Local Development
```bash
# Navigate to CLI directory
cd tools/dotcms-cli

# Build the CLI
./mvnw clean install

# Run in development mode (from CLI parent directory)
./mvnw quarkus:dev -pl cli

# Alternative: Run from cli subdirectory
cd cli && ../mvnw quarkus:dev

# Run with arguments in dev mode
./mvnw quarkus:dev -pl cli -Dquarkus.args="status"

# Run specific command from built JAR
java -jar cli/target/quarkus-app/quarkus-run.jar --help
```

### Testing
```bash
# Run unit tests
./mvnw test

# Run integration tests (uses TestContainers)
./mvnw verify

# Run with specific dotCMS instance
./mvnw test -Ddotcms.test.url=http://localhost:8080

# Run with test failure tolerance
./mvnw test -Dtest.failure.ignore=true

# Run specific test class
./mvnw test -Dtest=ContentTypeCommandIT
```

### Native Image Build
```bash
# Build native executable (requires GraalVM)
./mvnw package -Pnative -pl cli

# Run native executable
./cli/target/dotcms-cli-*-runner

# Build distribution with native binary
./mvnw package -Pdist -pl cli

# Distribution files created in
./cli/target/distributions/
```

## Configuration Management

### Application Properties
**File**: [`tools/dotcms-cli/cli/src/main/resources/application.properties`](../../tools/dotcms-cli/cli/src/main/resources/application.properties)

```properties
# Quarkus configuration
quarkus.application.name=dotcms-cli
quarkus.log.level=INFO
quarkus.log.console.format=%d{HH:mm:ss} %-5p [%c{2.}] %s%e%n

# CLI specific
dotcli.batch.size=100
dotcli.timeout.seconds=30
```

### Native Image Configuration
**Reflection**: [`reflection-config.json`](../../tools/dotcms-cli/cli/src/main/resources/reflection-config.json)
**Resources**: [`resources-config.json`](../../tools/dotcms-cli/cli/src/main/resources/resources-config.json)

## Security Patterns

### Credential Storage
```java
@ApplicationScoped
public class KeyTarPasswordStoreImpl implements SecurePasswordStore {
    
    @Override
    public void store(String service, String account, String password) {
        // Store using Java Keytar
        Keytar.setPassword(service, account, password);
    }
    
    @Override
    public Optional<String> retrieve(String service, String account) {
        return Optional.ofNullable(Keytar.getPassword(service, account));
    }
}
```

### Authentication Context
```java
@ApplicationScoped
public class DefaultAuthenticationContextImpl implements AuthenticationContext {
    
    @Inject
    SecurePasswordStore passwordStore;
    
    @Override
    public Optional<String> getToken(String instance) {
        return passwordStore.retrieve("dotcms-cli", instance);
    }
}
```

## Service Layer Patterns

### Push/Pull Services
```java
@ApplicationScoped
public class PushServiceImpl implements PushService {
    
    @Inject
    ServiceManager serviceManager;
    
    @Override
    public PushAnalysisResult analyze(PushOptions options) {
        // Analyze what needs to be pushed
        return analysisResult;
    }
    
    @Override
    public void push(PushOptions options) {
        // Execute push operation
    }
}
```

### File Operations
```java
@ApplicationScoped
public class FileHashCalculatorServiceImpl implements FileHashCalculatorService {
    
    @Override
    public String calculateHash(Path file) throws IOException {
        // Calculate file hash for change detection
        return hash;
    }
}
```

## Error Handling

### Exception Mapping
```java
@ApplicationScoped
public class ExceptionHandlerImpl implements ExceptionHandler {
    
    @Override
    public int handleException(Exception e) {
        return switch (e) {
            case DotCMSException dotcmsEx -> {
                outputHelper.error("dotCMS Error: " + dotcmsEx.getMessage());
                yield ExitCode.SOFTWARE;
            }
            case SecurityException secEx -> {
                outputHelper.error("Security Error: " + secEx.getMessage());
                yield ExitCode.NOPERM;
            }
            default -> {
                outputHelper.error("Unexpected error: " + e.getMessage());
                yield ExitCode.SOFTWARE;
            }
        };
    }
}
```

## Testing Patterns

### Integration Tests
```java
@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
public class ContentTypeCommandIT {
    
    @Test
    @Order(1)
    void testPushContentType() {
        // Test content type push
    }
    
    @Test
    @Order(2)
    void testPullContentType() {
        // Test content type pull
    }
}
```

### Test Containers
```java
@TestContainers
public class DotCMSITProfile {
    
    @Container
    static GenericContainer<?> dotcms = new GenericContainer<>("dotcms/dotcms-test")
        .withExposedPorts(8080)
        .withStartupTimeout(Duration.ofMinutes(5));
}
```

## Distribution

### Assembly Configuration
**File**: [`tools/dotcms-cli/cli/src/assembly/assembly.xml`](../../tools/dotcms-cli/cli/src/assembly/assembly.xml)

Creates distribution packages:
- JAR with dependencies
- Native executable
- NPM package structure

### Release Process
```bash
# Build release (from CLI parent directory)
./mvnw clean install -Prelease

# Create distribution packages
./mvnw package -Pdist -pl cli

# Generate man pages
./mvnw process-classes -pl cli

# Release with JReleaser (from CLI parent directory)
./mvnw jreleaser:release -pl .
```

## Common Patterns

### Command Mixins
```java
public class ContentTypePushMixin {
    
    @Option(names = {"--dry-run"}, description = "Show what would be pushed")
    boolean dryRun;
    
    @Option(names = {"--format"}, description = "Output format")
    InputOutputFormat format = InputOutputFormat.YAML;
}
```

### Service Integration
```java
@ApplicationScoped
public class HybridServiceManagerImpl implements ServiceManager {
    
    @Override
    public <T> T getService(Class<T> serviceClass) {
        // Return appropriate service implementation
        return CDI.current().select(serviceClass).get();
    }
}
```

## Best Practices

### ✅ CLI Development Standards
- Use Java 21 features freely (records, text blocks, pattern matching)
- Follow Quarkus patterns for dependency injection
- Implement proper error handling with exit codes
- Use PicocLI annotations for command structure
- Secure credential storage with Java Keytar

### ✅ Testing Requirements
- Write integration tests for command workflows
- Use TestContainers for dotCMS integration
- Test both JVM and native modes
- Validate command-line argument parsing

### ✅ Performance Considerations
- Optimize for GraalVM native compilation
- Use reflection configuration for native builds
- Implement efficient file operations
- Consider memory usage for large operations

## Integration with Main Project

### Build Integration
- CLI builds are part of main Maven reactor
- Uses parent POM for version management  
- Follows main project security standards
- Integrates with CI/CD pipeline (see [CI/CD Pipeline](../core/CICD_PIPELINE.md))
- CLI changes trigger specific build paths via `.github/filters.yaml`

### Deployment
- Published to npm registry as `@dotcms/dotcli`
- Available as uber JAR from dotCMS repository
- Native binaries for major platforms
- Docker images for containerized usage

## Location Information
- **CLI Source**: `tools/dotcms-cli/`
- **Main Documentation**: `tools/dotcms-cli/README.md`
- **Command Documentation**: `tools/dotcms-cli/cli/docs/`
- **Tests**: Integration tests in each module
- **Examples**: See individual command documentation
- **Distribution**: Built artifacts in `cli/target/distributions/`
- **CI/CD Integration**: See [CI/CD Pipeline](../core/CICD_PIPELINE.md)
- **Parent POM**: `tools/dotcms-cli/pom.xml`
- **CLI Module POM**: `tools/dotcms-cli/cli/pom.xml`