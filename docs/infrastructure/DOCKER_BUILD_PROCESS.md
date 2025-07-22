# Docker Build Process

## Overview

This document explains the complete Docker build process for the main dotCMS Docker image, including the hierarchical Maven plugin configuration, Tomcat packaging with Cargo, and usage across development and testing environments.

## Docker Build Architecture

### High-Level Process Flow
```
Maven Build → WAR Assembly → Tomcat Distribution → Docker Assembly → Multi-stage Build → Container Image
     ↓              ↓              ↓                  ↓                ↓                ↓
Java Compile   WAR + OSGI    Cargo Plugin     docker-descriptor.xml  dotcms/java-base  Final Image
```

## Docker-Maven-Plugin Structure

### Hierarchical Configuration

The Docker build process uses the **fabric8 docker-maven-plugin** (version 0.43.4) with hierarchical configuration across multiple modules:

#### Parent Configuration (`parent/pom.xml`)
```xml
<plugin>
    <groupId>io.fabric8</groupId>
    <artifactId>docker-maven-plugin</artifactId>
    <version>0.43.4</version>
    <configuration>
        <images>
            <image>
                <name>dotcms/dotcms-seed</name>
                <build>
                    <dockerFileDir>${project.basedir}/src/main/docker/original</dockerFileDir>
                    <assembly>
                        <descriptorRef>docker-descriptor</descriptorRef>
                    </assembly>
                </build>
            </image>
        </images>
    </configuration>
</plugin>
```

**Base Configuration Includes**:
- **Database**: PostgreSQL with pgvector (`ankane/pgvector`)
- **OpenSearch**: Search engine (`opensearchproject/opensearch:1.3.6`)
- **dotCMS**: Main application container
- **Volume Mappings**: Shared directories and configurations
- **Network Configuration**: Inter-container communication

#### Main Module Configuration (`dotCMS/pom.xml`)
```xml
<plugin>
    <groupId>io.fabric8</groupId>
    <artifactId>docker-maven-plugin</artifactId>
    <inherited>false</inherited>
    <configuration>
        <images>
            <image>
                <name>dotcms/dotcms</name>
                <build>
                    <dockerFileDir>${project.basedir}/src/main/docker/original</dockerFileDir>
                    <assembly>
                        <descriptorRef>docker-descriptor</descriptorRef>
                    </assembly>
                    <tags>
                        <tag>master_latest_SNAPSHOT</tag>
                        <tag>latest</tag>
                    </tags>
                </build>
                <run>
                    <ports>
                        <port>8080:8080</port>
                        <port>4000:4000</port>
                        <port>5005:5005</port>
                    </ports>
                    <env>
                        <DOT_DOTCMS_DEV_MODE>true</DOT_DOTCMS_DEV_MODE>
                        <DOT_FEATURE_FLAG_EXPERIMENTS>true</DOT_FEATURE_FLAG_EXPERIMENTS>
                    </env>
                    <volumes>
                        <bind>
                            <volume>/srv/dotserver/tomcat/webapps/ROOT</volume>
                        </bind>
                    </volumes>
                </run>
            </image>
        </images>
    </configuration>
</plugin>
```

## Tomcat Packaging with Cargo Maven Plugin

### Cargo Plugin Configuration
```xml
<plugin>
    <groupId>org.codehaus.cargo</groupId>
    <artifactId>cargo-maven3-plugin</artifactId>
    <version>1.10.6</version>
    <configuration>
        <container>
            <containerId>tomcat9x</containerId>
            <timeout>1800000</timeout>
        </container>
        <packager>
            <outputLocation>${assembly-directory}/dotserver/tomcat-${tomcat.version}</outputLocation>
        </packager>
    </configuration>
</plugin>
```

### Tomcat Distribution Assembly Process

#### 1. WAR Assembly
The build creates an exploded WAR structure:
```
target/dist/dotserver/tomcat-${tomcat.version}/
├── webapps/
│   └── ROOT/                    # Exploded WAR
│       ├── WEB-INF/
│       ├── html/
│       └── application/
├── lib/                         # Additional libraries
├── conf/                        # Tomcat configuration
├── felix/                       # OSGI bundles
│   ├── core/
│   └── system/
└── glowroot/                    # Profiler (optional)
```

#### 2. Component Integration
**Core Components**:
- **WAR File**: Main application (exploded)
- **OSGI Bundles**: Felix core and system bundles
- **Glowroot Profiler**: Performance monitoring
- **Redis Session Manager**: Session clustering
- **Log4j2 Libraries**: Logging framework
- **Custom Configurations**: Tomcat server.xml, context.xml

#### 3. Assembly Execution
```bash
# Build and assemble Tomcat distribution
./mvnw clean package -pl :dotcms-core

# Assembly location
target/dist/dotserver/tomcat-${tomcat.version}/
```

### Docker Assembly Descriptor
**File**: `dotCMS/src/main/docker/original/docker-descriptor.xml`
```xml
<assembly>
    <id>docker</id>
    <formats>
        <format>dir</format>
    </formats>
    <includeBaseDirectory>false</includeBaseDirectory>
    <fileSets>
        <fileSet>
            <directory>${assembly-directory}</directory>
            <outputDirectory>/</outputDirectory>
            <includes>
                <include>**/*</include>
            </includes>
        </fileSet>
    </fileSets>
</assembly>
```

## Dockerfile Multi-stage Build

### Stage 1: Build Context
**File**: `dotCMS/src/main/docker/original/Dockerfile`
```dockerfile
FROM dotcms/java-base:${java.base.image.version} as builder

# Copy assembled distribution
COPY maven /srv/

# Set permissions and prepare runtime structure
RUN chown -R dotcms:dotcms /srv && \
    chmod +x /srv/entrypoint.sh
```

### Stage 2: Runtime Image
```dockerfile
FROM ubuntu:20.04

# Install runtime dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        openjdk-11-jre-headless \
        && rm -rf /var/lib/apt/lists/*

# Copy from builder stage
COPY --from=builder /srv /srv

# Set runtime user
USER dotcms

# Expose ports
EXPOSE 8080 4000 5005

# Set entrypoint
ENTRYPOINT ["/srv/entrypoint.sh"]
```

## Development Startup: docker-start Profile

⚠️ **IMPORTANT**: The `docker-start` profile is for RUNNING pre-built Docker images, NOT for building them. All dotCMS builds automatically create docker-ready builds unless explicitly skipped with `-Ddocker.skip`.

### Build vs Run Distinction

**Building dotCMS (creates Docker images)**:
```bash
# Full clean build (use when starting fresh or major changes)
./mvnw clean install -DskipTests

# Fast core-only build (use for simple code changes in dotcms-core)
./mvnw install -pl :dotcms-core -DskipTests

# Build core with dependencies (use when core changes affect dependencies)
./mvnw install -pl :dotcms-core --am -DskipTests

# Skip Docker image creation (faster when you don't need containers)
./mvnw install -pl :dotcms-core -DskipTests -Ddocker.skip
```

**Running dotCMS (uses built images)**:
```bash
# This CANNOT be combined with build commands like clean install
./mvnw -pl :dotcms-core -Pdocker-start
```

**❌ INCORRECT - These commands will NOT work**:
```bash
# DON'T DO THIS - mixing build and run profiles doesn't work
./mvnw clean install -Pdocker-start -DskipTests
./mvnw install -Pdocker-start -DskipTests
```

## Build Optimization Strategies

### When to Use Different Build Commands

**Choose the right build command based on your changes:**

| Change Type | Maven Command | Just Command | Why | Build Time |
|-------------|---------------|--------------|-----|------------|
| **Test code changes only** | `./mvnw verify -pl :dotcms-integration -Dit.test=YourTestClass` | *(use Maven with -Dit.test)* | Target specific test class (auto-starts Docker services) | ~2-10 min ⚠️ |
| **Simple code changes** in dotcms-core only | `./mvnw install -pl :dotcms-core -DskipTests` | `just build-quicker` | Fastest - only builds core module | ~2-3 min |
| **Core changes affecting dependencies** | `./mvnw install -pl :dotcms-core --am -DskipTests` | *(use Maven)* | Builds core + upstream dependencies | ~3-5 min |
| **Major changes or clean start** | `./mvnw clean install -DskipTests` | `just build` | Full clean build of all modules | ~8-15 min |
| **Quick iteration without Docker** | `./mvnw install -pl :dotcms-core -DskipTests -Ddocker.skip` | `just build-no-docker` | Fastest - skips Docker image creation | ~1-2 min |
| **No tests, no Docker** | `./mvnw install -pl :dotcms-core -DskipTests -Ddocker.skip` | *(use Maven)* | Ultimate speed for unit test cycles | ~1-2 min |

### Maven Reactor Flags Explained

- **`-pl :dotcms-core`** - Only build the dotcms-core module
- **`--am` (also-make)** - Also build any modules that dotcms-core depends on
- **`-DskipTests`** - Skip running tests (much faster)
- **`-Ddocker.skip`** - Skip Docker image creation entirely
- **`clean`** - Delete previous build artifacts (forces complete rebuild)

### Test Module Optimization

**When you ONLY change test source code, don't rebuild core!**

| Test Module | Maven Command | Just Command | Purpose | Build Time |
|------------|---------------|--------------|---------|------------|
| **Integration Tests** | `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false` | `just test-integration` | JUnit integration tests (auto-starts DB/ES) | **Full suite: 60+ min** ⚠️ |
| **Postman Tests** | `./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false` | `just test-postman` | API testing with Postman/Newman (auto-starts services) | ~1-3 min |
| **Karate Tests** | `./mvnw verify -pl :dotcms-test-karate -Dkarate.test.skip=false` | `just test-karate` | BDD API testing (auto-starts services) | ~1-2 min |
| **E2E Node Tests** | `./mvnw verify -pl :dotcms-e2e-node -De2e.test.skip=false` | *(use Maven)* | Playwright E2E tests (full Docker environment) | ~2-5 min |
| **E2E Java Tests** | `./mvnw verify -pl :dotcms-e2e-java -De2e.test.skip=false` | *(use Maven)* | Selenium E2E tests (full Docker environment) | ~3-8 min |

⚠️ **PERFORMANCE WARNING**: The **FULL** integration test suite takes 30+ minutes to complete. Always target specific test classes during development!

💡 **For IDE debugging**: Use `just test-integration-ide` to start services manually, then run tests in your IDE.

### Integration Test Performance Guide

**Full Suite Runtime**: 60+ minutes (thousands of tests)
**Recommended approach**: Target specific test classes (2-10 minutes)

```bash
# ❌ DON'T run full suite during development (60+ minutes)
just test-integration  # Runs ALL integration tests - very slow!

# ✅ DO target specific test classes (2-10 minutes)
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=ContentTypeAPIImplTest
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=WorkflowAPIImplTest  
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=UserAPIImplTest

# ✅ DO run specific test methods for focused debugging
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=ContentTypeAPIImplTest#testCreateContentType

# ✅ DO use IDE testing for fastest iteration
just test-integration-ide  # Start services once
# → Run individual tests/methods in IDE repeatedly
```

**When to run full suite:**
- Before submitting major PRs
- In CI/CD pipelines  
- Final validation before release
- Never during active development iteration

**Common test classes to target:**
```bash
# Content management
-Dit.test=ContentTypeAPIImplTest
-Dit.test=ContentletAPIImplTest  
-Dit.test=StructureAPIImplTest

# User and permissions
-Dit.test=UserAPIImplTest
-Dit.test=RoleAPIImplTest
-Dit.test=PermissionAPIImplTest

# Workflow
-Dit.test=WorkflowAPIImplTest
-Dit.test=WorkflowTaskAPIImplTest

# File and asset management
-Dit.test=FileAssetAPIImplTest
-Dit.test=FolderAPIImplTest

# Search and indexing
-Dit.test=ContentletIndexAPIImplTest
-Dit.test=ESIndexAPIImplTest
```

💡 **Key insight**: If you're only changing test code in `dotcms-integration/src/test/`, you can run just `./mvnw verify -pl :dotcms-integration` without rebuilding the core!

#### Test Services and Docker Dependencies

⚠️ **IMPORTANT**: Integration tests automatically start Docker services (PostgreSQL, Elasticsearch, dotCMS application). These services are required for tests to run.

**Services started for integration tests:**
- **PostgreSQL database** (port 5432) - Test data storage
- **Elasticsearch/OpenSearch** (port 9200) - Search functionality  
- **dotCMS application** (port 8080) - Main application for API testing
- **Additional services** may be started based on specific test requirements

**For automated test execution (services start/stop automatically):**
```bash
# Run specific integration test class
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=ContentTypeAPIImplTest

# Run specific test method
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=ContentTypeAPIImplTest#testCreateContentType

# Run specific Postman collection
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=ai

# Run specific Karate test
./mvnw verify -pl :dotcms-test-karate -Dkarate.test.skip=false -Dit.test=ContentAPITest

# Run E2E test with specific spec
./mvnw verify -pl :dotcms-e2e-node -De2e.test.skip=false -De2e.test.specific=login.spec.ts
```

**For IDE-based testing (start services manually, then run tests in IDE):**
```bash
# Start integration test services (PostgreSQL, Elasticsearch) without running tests
./mvnw -pl :dotcms-integration pre-integration-test -Dcoreit.test.skip=false -Dtomcat.port=8080
just test-integration-ide  # Same as above, shorter command

# Start Postman test services
./mvnw -pl :dotcms-postman pre-integration-test -Dpostman.test.skip=false -Dtomcat.port=8080
just test-postman-ide  # Same as above, shorter command

# Start Karate test services  
./mvnw -pl :dotcms-test-karate pre-integration-test -Dkarate.test.skip=false -Dtomcat.port=8080
just test-karate-ide  # Same as above, shorter command

# Then run individual tests in your IDE (IntelliJ, Eclipse, VS Code)
# Services will remain running until you stop them

# Stop test services when done
./mvnw -pl :dotcms-integration -Pdocker-stop -Dcoreit.test.skip=false
just test-integration-stop  # Same as above, shorter command
```

#### Typical IDE Testing Workflow
```bash
# 1. Make sure you have a built core (only needed once)
just build-quicker  # Build core if not already built

# 2. Start test services for IDE debugging
just test-integration-ide

# 3. In your IDE (IntelliJ/Eclipse/VS Code):
#    - Set breakpoints in your test code
#    - Right-click on individual test methods and run them
#    - Debug step through your test logic
#    - Services remain running between test executions

# 4. Make test changes, re-run tests in IDE (fast iteration)
#    - No need to restart services for test code changes
#    - Services stay running for quick iteration

# 5. When finished with testing session
just test-integration-stop
```

### Module Dependencies
```
dotcms-core depends on:
├── parent (build configuration)
├── bom/application (dependency versions)
└── osgi-base/* (OSGI bundles)

NOT included with -pl :dotcms-core:
├── dotcms-integration (test module)
├── dotcms-postman (test module)  
├── tools/dotcms-cli (separate tool)
├── core-web (frontend)
└── e2e/* (end-to-end tests)
```

### Development Workflow Decision Tree

**Ask yourself these questions to choose the optimal build:**

1. **Did you ONLY change test source code?**
   - Yes → `./mvnw verify -pl :dotcms-integration -Dit.test=YourTestClass` (target specific test class!)
   - **⚠️ Don't run full test suite** - it takes 60+ minutes
   - **Skip all other questions - you're done!**

2. **Is this your first build or after major changes?**
   - Yes → `./mvnw clean install -DskipTests` or `just build`

3. **Did you only change code in dotCMS/src/ (no dependency changes)?**
   - Yes → `./mvnw install -pl :dotcms-core -DskipTests` or `just build-quicker`

4. **Did you change dependencies or need upstream modules rebuilt?**
   - Yes → `./mvnw install -pl :dotcms-core --am -DskipTests`

5. **Are you only running unit tests (no Docker needed)?**
   - Yes → Add `-Ddocker.skip` to any command above

6. **Are you working on frontend only?**
   - Yes → `cd core-web && npm install && nx build dotcms-ui`

### Test Execution Modes

**🤖 Automated Test Execution (Command Line)**
- **How**: `just test-integration` or `./mvnw verify -pl :dotcms-integration`
- **Services**: Docker services start automatically, run tests, then stop automatically
- **Use case**: CI/CD, quick validation, running full test suites
- **Pros**: Fully automated, no manual setup
- **Cons**: Services restart for each run (slightly slower for multiple runs)

**🛠️ IDE-Based Test Execution (Manual Services)**
- **How**: `just test-integration-ide` → start services, then run tests in IDE
- **Services**: Start Docker services manually, keep running while you debug
- **Use case**: Test debugging, iterative development, running individual tests
- **Pros**: Services stay running, faster iteration, full IDE debugging capabilities
- **Cons**: Manual service lifecycle management

### When Do You Need to Rebuild Core?

**✅ NO need to rebuild core when:**
- Only changing test source code in test modules (`**/src/test/**`)
- Only changing test resources (`**/src/test/resources/**`)
- Only changing test configurations (test-specific properties, Docker configs for tests)
- Only changing documentation or README files

**⚠️ REBUILD core needed when:**
- Changing any source code in `dotCMS/src/main/`
- Adding/changing dependencies in any `pom.xml`
- Changing configuration that affects runtime behavior
- Changing Docker configurations that affect the main image

### Performance Tips
- **Development workflow**: Use `install` (not `clean install`) for faster rebuilds
- **Test-first optimization**: If only changing tests, skip core rebuild entirely
- **⚠️ CRITICAL: Target specific test classes** - Full integration suite = 60+ minutes!
- **Docker optional**: Add `-Ddocker.skip` if you're only running unit tests  
- **Parallel builds**: Add `-T 1C` for multi-threaded builds on powerful machines
- **Incremental compilation**: Maven only recompiles changed files
- **Watch for dependencies**: If your build fails, you might need `--am` flag

### Integration Test Time Savings

| Approach | Time | Use Case |
|----------|------|----------|
| **Full integration suite** | 60+ minutes | ❌ Never during development |
| **Specific test class** | 2-10 minutes | ✅ Development iteration |
| **Single test method** | 30 seconds - 2 minutes | ✅ Focused debugging |
| **IDE with running services** | 10-30 seconds | ✅ Rapid iteration |

### Basic Usage
```bash
# FIRST: Build dotCMS (this automatically creates docker-ready build unless docker is skipped)
./mvnw clean install -pl :dotcms-core -DskipTests

# THEN: Start development environment (runs the built image)
./mvnw -pl :dotcms-core -Pdocker-start

# With Glowroot profiler
./mvnw -pl :dotcms-core -Pdocker-start -Ddocker.glowroot.enabled=true

# With debug enabled
./mvnw -pl :dotcms-core -Pdocker-start -Ddebug.enable=true

# With debug suspend (wait for debugger)
./mvnw -pl :dotcms-core -Pdocker-start -Ddebug.enable=true -Ddebug.suspend=true
```

### Profile Configuration
**Maven Profile**: `docker-start`
```xml
<profile>
    <id>docker-start</id>
    <build>
        <defaultGoal>validate</defaultGoal>
        <plugins>
            <plugin>
                <groupId>io.fabric8</groupId>
                <artifactId>docker-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>docker-stop</id>
                        <phase>initialize</phase>
                        <goals>
                            <goal>stop</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>docker-start</id>
                        <phase>validate</phase>
                        <goals>
                            <goal>start</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</profile>
```

### Services Started
1. **PostgreSQL Database** (port 5432)
2. **OpenSearch** (port 9200)
3. **dotCMS Application** (port 8080)
4. **Glowroot Profiler** (port 4000, if enabled)
5. **Debug Port** (port 5005, if enabled)

### Alternative: dev-run Script
```bash
# Simple development startup
./dev-run

# With specific options
./dev-run --debug --glowroot
```

## Environment Configuration

### Configuration Hierarchy
The environment configuration follows a layered approach:

1. **Base Configuration**: `environments/environment.properties`
2. **Environment Specific**: `environments/dev/dev.properties`
3. **User Override**: `environments/dev/user-dev.properties`

### Feature Flag Configuration
**File**: `environments/dev/user-dev.properties`
```properties
# Example developer-specific feature flags
docker.dotcms-core.ext-master_latest_SNAPSHOT.dotcms.envRun.DOT_FEATURE_FLAG_EXPERIMENTS=true
docker.dotcms-core.ext-master_latest_SNAPSHOT.dotcms.envRun.DOT_FEATURE_FLAG_GRAPHQL_PROVIDER=true
docker.dotcms-core.ext-master_latest_SNAPSHOT.dotcms.envRun.DOT_FEATURE_FLAG_ANALYTICS=true

# Debug configuration
docker.dotcms-core.ext-master_latest_SNAPSHOT.dotcms.envRun.DOT_DOTCMS_DEV_MODE=true
docker.dotcms-core.ext-master_latest_SNAPSHOT.dotcms.envRun.DOT_DOTCMS_LOGGING_LEVEL=DEBUG

# Custom database configuration
docker.dotcms-core.ext-master_latest_SNAPSHOT.dotcms.envRun.DOT_DATASOURCE_PROVIDER_STRATEGY_CLASS=SystemEnvDataSourceStrategy
```

### Environment Variable Patterns
```properties
# For specific context
docker.{module-name}.{context}.dotcms.envRun.{VARIABLE_NAME}={value}

# For default context
docker.{module-name}.default.dotcms.envRun.{VARIABLE_NAME}={value}

# Example contexts
docker.dotcms-core.ext-master_latest_SNAPSHOT.dotcms.envRun.DOT_FEATURE_FLAG_NAME=true
docker.dotcms-core.default.dotcms.envRun.DOT_FEATURE_FLAG_NAME=true
```

### Creating User-Specific Configuration
```bash
# Copy example file
cp environments/dev/user-dev.properties.example environments/dev/user-dev.properties

# Edit with your specific settings
vim environments/dev/user-dev.properties
```

**Example Configuration**:
```properties
# Personal development settings
docker.dotcms-core.ext-master_latest_SNAPSHOT.dotcms.envRun.DOT_FEATURE_FLAG_EXPERIMENTS=true
docker.dotcms-core.ext-master_latest_SNAPSHOT.dotcms.envRun.DOT_FEATURE_FLAG_GRAPHQL_PROVIDER=true
docker.dotcms-core.ext-master_latest_SNAPSHOT.dotcms.envRun.DOT_DOTCMS_DEV_MODE=true
docker.dotcms-core.ext-master_latest_SNAPSHOT.dotcms.envRun.DOT_DOTCMS_LOGGING_LEVEL=DEBUG
docker.dotcms-core.ext-master_latest_SNAPSHOT.dotcms.envRun.DOT_STARTER_DATA_LOAD=true
```

## Docker Image Usage in Test Modules

### dotcms-postman Module
**Purpose**: API testing using Newman/Postman collections

**Configuration**:
```xml
<image>
    <name>dotcms/dotcms</name>
    <run>
        <ports>
            <port>8080:8080</port>
        </ports>
        <env>
            <DOT_DOTCMS_DEV_MODE>true</DOT_DOTCMS_DEV_MODE>
            <DOT_FEATURE_FLAG_EXPERIMENTS>true</DOT_FEATURE_FLAG_EXPERIMENTS>
        </env>
        <wait>
            <http>
                <url>http://localhost:8080/api/v1/health</url>
            </http>
            <time>300000</time>
        </wait>
    </run>
</image>
```

**Services**:
- dotCMS container with test-specific environment variables
- WireMock container for API mocking
- PostgreSQL database
- OpenSearch

### dotcms-integration Module
**Purpose**: Integration testing with TestContainers

**Configuration**:
```xml
<image>
    <name>dotcms/dotcms</name>
    <run>
        <ports>
            <port>8080:8080</port>
        </ports>
        <env>
            <DOT_DOTCMS_DEV_MODE>true</DOT_DOTCMS_DEV_MODE>
            <DOT_DATASOURCE_PROVIDER_STRATEGY_CLASS>SystemEnvDataSourceStrategy</DOT_DATASOURCE_PROVIDER_STRATEGY_CLASS>
        </env>
        <links>
            <link>db:db</link>
            <link>opensearch:opensearch</link>
        </links>
    </run>
</image>
```

**Usage**:
```bash
# Run integration tests
./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false
```

### test-karate Module
**Purpose**: Karate BDD API testing

**Configuration**:
```xml
<image>
    <name>dotcms/dotcms</name>
    <run>
        <ports>
            <port>8080:8080</port>
        </ports>
        <env>
            <DOT_DOTCMS_DEV_MODE>true</DOT_DOTCMS_DEV_MODE>
            <DOT_FEATURE_FLAG_EXPERIMENTS>true</DOT_FEATURE_FLAG_EXPERIMENTS>
        </env>
        <wait>
            <http>
                <url>http://localhost:8080/api/v1/health</url>
            </http>
            <time>300000</time>
        </wait>
    </run>
</image>
```

**Usage**:
```bash
# Run Karate tests
./mvnw verify -Dkarate.test.skip=false -pl :dotcms-test-karate
```

## CI/CD Integration & Consistency

### GitHub Actions Integration
**Workflow Files**: `.github/workflows/cicd_*.yml`

**Key Principles**:
- Same Docker images used in development and CI/CD
- Consistent environment variables and configurations
- Same Maven commands and profiles
- Docker layer caching for performance

**Example CI/CD Docker Usage**:
```yaml
- name: Build Docker Image
  run: |
    ./mvnw clean package -pl :dotcms-core
    docker build -t dotcms/dotcms:ci .

- name: Run Integration Tests
  run: |
    ./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false
```

### Maintaining Consistency

#### 1. "Works on My Machine" Prevention
```bash
# Always use Docker for development
./mvnw -pl :dotcms-core -Pdocker-start

# Use same Java version as CI/CD
docker run --rm dotcms/java-base:latest java -version

# Use same database version
docker run --rm ankane/pgvector:latest psql --version
```

#### 2. Environment Parity
```properties
# Development environment should match CI/CD
docker.dotcms-core.ext-master_latest_SNAPSHOT.dotcms.envRun.DOT_DOTCMS_DEV_MODE=true
docker.dotcms-core.ext-master_latest_SNAPSHOT.dotcms.envRun.DOT_FEATURE_FLAG_EXPERIMENTS=true

# Same database configuration
docker.dotcms-core.ext-master_latest_SNAPSHOT.dotcms.envRun.DOT_DATASOURCE_PROVIDER_STRATEGY_CLASS=SystemEnvDataSourceStrategy
```

#### 3. Debugging CI/CD Issues
```bash
# Run locally with CI/CD environment
./mvnw -pl :dotcms-core -Pdocker-start -Denv=ci

# Check environment variables
docker exec dotcms-container env | grep DOT_

# Compare logs
docker logs dotcms-container
```

#### 4. Version Consistency
```xml
<!-- Parent POM ensures version consistency -->
<properties>
    <docker.image.version>master_latest_SNAPSHOT</docker.image.version>
    <java.base.image.version>21-2023-10-24</java.base.image.version>
    <postgres.version>13-pgvector</postgres.version>
    <opensearch.version>1.3.6</opensearch.version>
</properties>
```

## Advanced Development Options

### Debug Configuration
```bash
# Enable debug mode
./mvnw -pl :dotcms-core -Pdocker-start -Ddebug.enable=true

# Debug with suspend (wait for debugger)
./mvnw -pl :dotcms-core -Pdocker-start -Ddebug.enable=true -Ddebug.suspend=true

# Custom debug port
./mvnw -pl :dotcms-core -Pdocker-start -Ddebug.enable=true -Ddebug.port=5006
```

### Profiling with Glowroot
```bash
# Enable Glowroot profiler
./mvnw -pl :dotcms-core -Pdocker-start -Ddocker.glowroot.enabled=true

# Access profiler at http://localhost:4000
```

### Custom JVM Options
```properties
# In user-dev.properties
docker.dotcms-core.ext-master_latest_SNAPSHOT.dotcms.envRun.DOT_JAVA_OPTS=-Xmx4g -XX:+UseG1GC -Dlog4j.configurationFile=log4j2-debug.xml
```

### Volume Mounting for Development
```bash
# Mount local source code
./mvnw -pl :dotcms-core -Pdocker-start -Ddocker.mount.source=true

# Mount specific directories
./mvnw -pl :dotcms-core -Pdocker-start -Ddocker.mount.webapps=true
```

## Troubleshooting Common Issues

### 1. Docker Build Failures
```bash
# Clean Docker cache
docker system prune -a

# Rebuild from scratch
./mvnw clean package -pl :dotcms-core -Ddocker.noCache=true
```

### 2. Port Conflicts
```bash
# Check port usage
netstat -tulpn | grep 8080

# Use different ports
./mvnw -pl :dotcms-core -Pdocker-start -Ddocker.port.http=8081
```

### 3. Memory Issues
```bash
# Increase Docker memory
docker system info | grep Memory

# Increase JVM heap
export MAVEN_OPTS="-Xmx4g"
```

### 4. Environment Variable Issues
```bash
# Check environment variables
docker exec dotcms-container env | grep DOT_

# Verify configuration loading
docker exec dotcms-container cat /srv/dotserver/tomcat/webapps/ROOT/WEB-INF/classes/application.properties
```

## Best Practices

### ✅ Development Standards
- **Always use Docker**: Never run dotCMS directly on host machine
- **Use consistent versions**: Match CI/CD environment versions
- **Environment parity**: Keep development and production environments similar
- **Configuration management**: Use user-dev.properties for personal settings
- **Regular updates**: Keep Docker images updated

### ✅ Performance Optimization
- **Layer caching**: Use Docker layer caching for faster builds
- **Parallel builds**: Use Maven parallel build options
- **Resource limits**: Set appropriate CPU and memory limits
- **Volume mounting**: Use volumes for persistent data

### ✅ Security Considerations
- **Non-root user**: Run containers as non-root user
- **Network isolation**: Use Docker networks for service isolation
- **Secret management**: Use environment variables for sensitive data
- **Image scanning**: Regularly scan images for vulnerabilities

## Location Information
- **Main Dockerfile**: `dotCMS/src/main/docker/original/Dockerfile`
- **Docker Assembly**: `dotCMS/src/main/docker/original/docker-descriptor.xml`
- **Entrypoint Script**: `dotCMS/src/main/docker/original/ROOT/srv/entrypoint.sh`
- **Environment Config**: `environments/dev/user-dev.properties.example`
- **Docker Compose Examples**: `docker/docker-compose-examples/`
- **Maven Plugin Config**: `dotCMS/pom.xml` (docker-maven-plugin)
- **Parent Config**: `parent/pom.xml` (base Docker configuration)
- **CI/CD Workflows**: `.github/workflows/cicd_*.yml`