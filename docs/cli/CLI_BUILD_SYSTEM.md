# CLI Build System

## Overview

The dotCMS CLI uses a specialized build system based on Quarkus and Maven, designed for creating both JVM and native executables. This document details the build process, configuration, and integration with the main dotCMS project.

## Build Architecture

### Maven Structure
```
tools/dotcms-cli/
├── pom.xml                    # Parent POM with Quarkus BOM
├── api-data-model/
│   └── pom.xml               # API data model module
└── cli/
    ├── pom.xml               # CLI application module
    └── src/assembly/
        └── assembly.xml      # Distribution assembly
```

### Parent POM Configuration
**File**: [`tools/dotcms-cli/pom.xml`](../../tools/dotcms-cli/pom.xml)

**Key Features**:
- Inherits from `dotcms-parent` for version management
- Defines Quarkus BOM version (3.6.0)
- Includes JReleaser for distribution
- Multi-module structure with `api-data-model` and `cli`

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

## Build Profiles

### Standard Build
```bash
# Build all modules
./mvnw clean install

# Build specific module
./mvnw clean install -pl cli

# Skip tests for faster build
./mvnw clean install -DskipTests
```

### Distribution Profile (`-Pdist`)
**Purpose**: Creates distribution packages with assembly plugin

**Activation**: `./mvnw package -Pdist -pl cli`

**Generated Artifacts**:
- `dotcms-cli-{version}-{os-classifier}.tar.gz`
- `dotcms-cli-{version}-{os-classifier}.zip`
- Native executables (when combined with `-Pnative`)

**Configuration**:
```xml
<profile>
    <id>dist</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <configuration>
                    <finalName>${project.artifactId}-${project.version}-${os.detected.classifier}</finalName>
                    <outputDirectory>${distribution.directory}</outputDirectory>
                    <descriptors>
                        <descriptor>src/assembly/assembly.xml</descriptor>
                    </descriptors>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

### Native Profile (`-Pnative`)
**Purpose**: Builds GraalVM native executables

**Activation**: `./mvnw package -Pnative -pl cli`

**Requirements**:
- GraalVM installed
- Native image toolchain
- Reflection configuration files

**Configuration**:
```xml
<profile>
    <id>native</id>
    <properties>
        <quarkus.package.type>native</quarkus.package.type>
    </properties>
    <build>
        <plugins>
            <plugin>
                <artifactId>maven-failsafe-plugin</artifactId>
                <configuration>
                    <systemPropertyVariables>
                        <native.image.path>${project.build.directory}/${project.build.finalName}-runner</native.image.path>
                    </systemPropertyVariables>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

### Release Profile (`-Prelease`)
**Purpose**: Releases CLI using JReleaser

**Activation**: `./mvnw jreleaser:release -Prelease`

**Features**:
- Automated GitHub releases
- Multi-platform distribution
- NPM package publishing
- Docker image creation

## Quarkus Integration

### Quarkus Maven Plugin
**Configuration**:
```xml
<plugin>
    <groupId>io.quarkus.platform</groupId>
    <artifactId>quarkus-maven-plugin</artifactId>
    <version>${quarkus.platform.version}</version>
    <extensions>true</extensions>
    <executions>
        <execution>
            <goals>
                <goal>build</goal>
                <goal>generate-code</goal>
                <goal>generate-code-tests</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Development Mode
```bash
# Run in development mode (hot reload)
./mvnw quarkus:dev -pl cli

# Run with arguments
./mvnw quarkus:dev -pl cli -Dquarkus.args="status"

# Run with debug logging
./mvnw quarkus:dev -pl cli -Dquarkus.log.handler.console.\"DOTCMS_CONSOLE\".level=DEBUG
```

### Build Modes
- **JVM Mode**: Standard Java application (default)
- **Native Mode**: GraalVM native executable (`-Pnative`)
- **Uber JAR**: Single JAR with all dependencies

## Testing Integration

### Test Configuration
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <testFailureIgnore>${test.failure.ignore}</testFailureIgnore>
        <includes>
            <include>**/*IT.java</include>
        </includes>
        <systemPropertyVariables>
            <testcontainers.docker.image>${testcontainers.docker.image}</testcontainers.docker.image>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

### Test Commands
```bash
# Run unit tests
./mvnw test -pl cli

# Run integration tests
./mvnw verify -pl cli

# Run with TestContainers
./mvnw verify -pl cli -Dtestcontainers.docker.image=dotcms/dotcms-test

# Run specific test
./mvnw test -pl cli -Dtest=ContentTypeCommandIT
```

## Documentation Generation

### Man Pages Generation
**Plugin**: `exec-maven-plugin` with PicocLI codegen

**Configuration**:
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>generateManPages</id>
            <phase>process-classes</phase>
            <goals>
                <goal>java</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <mainClass>picocli.codegen.docgen.manpage.ManPageGenerator</mainClass>
        <arguments>
            <argument>--outdir=${project.basedir}/docs</argument>
            <argument>com.dotcms.cli.command.ConfigCommand</argument>
            <argument>com.dotcms.cli.command.PushCommand</argument>
            <argument>com.dotcms.cli.command.PullCommand</argument>
            <!-- Additional commands -->
        </arguments>
    </configuration>
</plugin>
```

**Usage**:
```bash
# Generate man pages
./mvnw process-classes -pl cli

# Output location
./cli/docs/*.adoc
```

## Assembly Configuration

### Distribution Assembly
**File**: [`tools/dotcms-cli/cli/src/assembly/assembly.xml`](../../tools/dotcms-cli/cli/src/assembly/assembly.xml)

**Generated Structure**:
```
dotcms-cli-{version}-{os}/
├── bin/
│   └── dotcli              # Executable script
├── lib/
│   └── dotcms-cli.jar      # Application JAR
├── docs/
│   └── *.adoc              # Command documentation
└── README.md               # Usage instructions
```

### Platform-Specific Builds
**Windows Profile**:
```xml
<profile>
    <id>dist-windows</id>
    <activation>
        <os>
            <family>windows</family>
        </os>
    </activation>
    <properties>
        <executable-suffix>.exe</executable-suffix>
    </properties>
</profile>
```

**OS Detection**:
```xml
<extensions>
    <extension>
        <groupId>kr.motd.maven</groupId>
        <artifactId>os-maven-plugin</artifactId>
        <version>${os-maven-plugin.version}</version>
    </extension>
</extensions>
```

## CI/CD Integration

### GitHub Actions Build
**Change Detection**: CLI builds triggered by changes to:
```yaml
cli: &cli
  - 'tools/dotcms-cli/**'
  - 'cli/**'
```

**Build Steps**:
1. **Initialize**: Change detection via `.github/filters.yaml`
2. **Build**: Maven build with artifact generation
3. **Test**: Integration tests with TestContainers
4. **Native**: Native executable generation
5. **Release**: JReleaser distribution

### Artifact Generation
```bash
# CLI artifacts in trunk workflow
./mvnw package -Pdist -Pnative -pl tools/dotcms-cli/cli

# Generated artifacts
tools/dotcms-cli/cli/target/distributions/
├── dotcms-cli-{version}-linux-x86_64.tar.gz
├── dotcms-cli-{version}-osx-x86_64.tar.gz
└── dotcms-cli-{version}-windows-x86_64.zip
```

## Native Image Configuration

### Reflection Configuration
**File**: [`tools/dotcms-cli/cli/src/main/resources/reflection-config.json`](../../tools/dotcms-cli/cli/src/main/resources/reflection-config.json)

**Purpose**: Configures classes for native image reflection

### Resources Configuration
**File**: [`tools/dotcms-cli/cli/src/main/resources/resources-config.json`](../../tools/dotcms-cli/cli/src/main/resources/resources-config.json)

**Purpose**: Includes resources in native image

### Native Build Arguments
```xml
<properties>
    <quarkus.native.additional-build-args>
        --native-compiler-options=-Wno-nullability-completeness,
        -H:ReflectionConfigurationFiles=reflection-config.json,
        -H:ResourceConfigurationFiles=resources-config.json
    </quarkus.native.additional-build-args>
</properties>
```

## Distribution Channels

### NPM Package
**Registry**: `@dotcms/dotcli`

**Structure**:
```
@dotcms/dotcli/
├── package.json
├── bin/
│   └── dotcli
└── lib/
    └── dotcms-cli.jar
```

### Manual JAR Download
**Repository**: `https://repo.dotcms.com/artifactory/libs-snapshot-local/com/dotcms/dotcms-cli/`

**Usage**:
```bash
# Download and run
wget https://repo.dotcms.com/artifactory/libs-snapshot-local/com/dotcms/dotcms-cli/latest/dotcms-cli.jar
java -jar dotcms-cli.jar --help
```

### Docker Images
**Registry**: Docker Hub

**Usage**:
```bash
# Run CLI in container
docker run dotcms/dotcli status

# Mount workspace
docker run -v $(pwd):/workspace dotcms/dotcli push
```

## Troubleshooting

### Common Build Issues

#### 1. Quarkus Version Conflicts
**Error**: `Quarkus version mismatch`

**Solution**:
```bash
# Check Quarkus version
./mvnw dependency:tree | grep quarkus

# Update parent POM version
# Edit tools/dotcms-cli/pom.xml
<quarkus.platform.version>3.6.0</quarkus.platform.version>
```

#### 2. Native Image Build Failures
**Error**: `Native image generation failed`

**Solution**:
```bash
# Check GraalVM installation
java -version  # Should show GraalVM

# Install native-image tool
gu install native-image

# Build with verbose output
./mvnw package -Pnative -pl cli -Dquarkus.native.enable-https-url-handler=true
```

#### 3. TestContainers Issues
**Error**: `Could not start container`

**Solution**:
```bash
# Check Docker daemon
docker info

# Use specific image
./mvnw test -Dtestcontainers.docker.image=dotcms/dotcms-test:latest

# Skip integration tests
./mvnw test -Dtest.failure.ignore=true
```

### Build Performance

#### Parallel Builds
```bash
# Use multiple threads
./mvnw clean install -T 1C

# Build specific modules
./mvnw clean install -pl api-data-model,cli
```

#### Skip Unnecessary Steps
```bash
# Skip tests
./mvnw clean install -DskipTests

# Skip documentation
./mvnw clean install -Dmaven.javadoc.skip=true

# Skip code generation
./mvnw clean install -Dquarkus.generate-code.skip=true
```

## Best Practices

### ✅ CLI Build Standards
- Use parent POM for version management
- Follow Quarkus build conventions
- Generate native images for distribution
- Include comprehensive test coverage
- Document build commands clearly

### ✅ Performance Optimization
- Use Maven parallel builds (`-T 1C`)
- Skip unnecessary phases for development
- Cache dependencies effectively
- Use incremental builds when possible

### ✅ Distribution Quality
- Test both JVM and native modes
- Include platform-specific packages
- Validate all distribution channels
- Maintain backward compatibility

## Integration with Main Project

### Version Management
- CLI version follows main project version
- Uses `${revision}${sha1}${changelist}` pattern
- Inherits from `dotcms-parent` POM

### Dependency Management
- Shared dependencies through parent BOM
- CLI-specific dependencies isolated
- No conflicts with main project dependencies

### Build Coordination
- CLI builds integrated in main reactor
- Artifact generation coordinated with releases
- CI/CD pipeline includes CLI validation

## Location Information
- **Parent POM**: `tools/dotcms-cli/pom.xml`
- **CLI Module**: `tools/dotcms-cli/cli/pom.xml`
- **Assembly Config**: `tools/dotcms-cli/cli/src/assembly/assembly.xml`
- **Native Config**: `tools/dotcms-cli/cli/src/main/resources/`
- **Distribution**: `tools/dotcms-cli/cli/target/distributions/`
- **Documentation**: `tools/dotcms-cli/cli/docs/`