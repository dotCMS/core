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
FROM ubuntu:24.04

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
| **Test code changes only** | `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=YourTestClass` | *(use Maven with -Dit.test)* | Target specific test class (auto-starts Docker services) | ~2-10 min ⚠️ |
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

⚠️ **CRITICAL**: All test modules require explicit skip flags to run:
- **Integration tests**: `-Dcoreit.test.skip=false`
- **Postman tests**: `-Dpostman.test.skip=false` 
- **Karate tests**: `-Dkarate.test.skip=false`
- **E2E tests**: `-De2e.test.skip=false`
- **Without these flags, tests will be silently skipped!**

**✅ CORRECT (tests will run):**
```bash
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTest
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=all     # Run ALL postman tests
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=ai      # Run specific collection group
./mvnw verify -pl :dotcms-test-karate -Dkarate.test.skip=false -Dit.test=ContentAPITest
```

**❌ INCORRECT (tests will be silently skipped):**
```bash
./mvnw verify -pl :dotcms-integration -Dit.test=MyTest          # NO tests run!
./mvnw verify -pl :dotcms-postman -Dpostman.collections=ai     # NO tests run!
./mvnw verify -pl :dotcms-test-karate -Dit.test=ContentAPITest # NO tests run!
```

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
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=ContentTypeAPIImplTest
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=ContentletAPIImplTest  
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=StructureAPIImplTest

# User and permissions
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=UserAPIImplTest
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=RoleAPIImplTest
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=PermissionAPIImplTest

# Workflow
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=WorkflowAPIImplTest
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=WorkflowTaskAPIImplTest

# File and asset management
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=FileAssetAPIImplTest
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=FolderAPIImplTest

# Search and indexing
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=ContentletIndexAPIImplTest
./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=ESIndexAPIImplTest
```

💡 **Key insight**: If you're only changing test code in `dotcms-integration/src/test/`, you can run just `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=YourTestClass` without rebuilding the core!

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
   - Yes → `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=YourTestClass` (target specific test class!)
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

## JMX Remote Monitoring

⚠️ **Base Image Dependency**: JMX functionality requires the dotCMS Java base image to be rebuilt with the `jdk.management.agent` module. The JMX features will not work until the base image includes this module.

### Overview
dotCMS supports JMX (Java Management Extensions) remote monitoring for development and troubleshooting. This allows you to connect tools like JConsole, VisualVM, or Mission Control to monitor JVM metrics, memory usage, garbage collection, thread activity, and MBean operations.

### JMX Configuration
**Default Ports:**
- JMX Remote Port: `9999`
- RMI Registry Port: `9998`

**Security Configuration:**
- Authentication: Disabled (development only)
- SSL: Disabled (development only)
- Hostname: `localhost` (local connections only)

### Starting dotCMS with JMX

#### Basic JMX Monitoring
```bash
# Start dotCMS with JMX enabled
./mvnw -pl :dotcms-core -Pdocker-start -Djmx.enable=true

# Alternative using Just command
just dev-run-jmx
```

#### Custom JMX Ports
```bash
# Specify custom ports
./mvnw -pl :dotcms-core -Pdocker-start -Djmx.enable=true -Djmx.port=7777 -Djmx.rmi.port=7778

# Alternative using Just command
just dev-run-jmx-ports 7777 7778
```

#### Combined JMX + Debug
```bash
# Enable both JMX monitoring and Java debugging
./mvnw -pl :dotcms-core -Pdocker-start,jmx-debug -Djmx.debug.enable=true

# Alternative using Just command
just dev-run-jmx-debug
```

#### Full Monitoring Stack
```bash
# Enable JMX + Debug + Glowroot profiler
./mvnw -pl :dotcms-core -Pdocker-start,jmx-debug,glowroot -Djmx.debug.enable=true -Ddocker.glowroot.enabled=true

# Alternative using Just command  
just dev-run-jmx-debug-glowroot
```

### Just Commands for JMX

The following Just commands provide convenient shortcuts for JMX monitoring:

```bash
# Basic JMX monitoring
just dev-run-jmx

# Custom port configuration
just dev-run-jmx-ports <jmx_port> <rmi_port>

# Combined JMX and debugging
just dev-run-jmx-debug

# Full monitoring stack (JMX + Debug + Glowroot)
just dev-run-jmx-debug-glowroot
```

### Connecting with JConsole

#### Prerequisites
- Java Development Kit (JDK) installed with JConsole
- dotCMS running with JMX enabled
- JConsole typically located at `$JAVA_HOME/bin/jconsole`

#### Connection Steps

**1. Start dotCMS with JMX:**
```bash
just dev-run-jmx
# or
./mvnw -pl :dotcms-core -Pdocker-start -Djmx.enable=true
```

**2. Launch JConsole:**
```bash
# Launch JConsole GUI
jconsole

# Or connect directly to localhost:9999
jconsole localhost:9999

# Connect to custom port
jconsole localhost:7777  # if using custom ports
```

**3. Connection Options in JConsole:**
- **Remote Process**: Select "Remote Process"
- **Connection String**: Enter `localhost:9999` (or your custom port)
- **Username/Password**: Leave blank (authentication disabled for development)
- Click "Connect"

#### JConsole Monitoring Capabilities

Once connected, JConsole provides access to:

**Memory Monitoring:**
- Heap memory usage (Eden, Survivor, Old Generation spaces)
- Non-heap memory (Metaspace, Code Cache)  
- Garbage collection statistics and trends
- Memory leak detection

**Thread Analysis:**
- Thread count and CPU usage
- Thread states (Running, Waiting, Blocked)
- Deadlock detection
- Thread stack traces

**Runtime Information:**
- JVM version and system properties
- Classpath and boot classpath
- Environment variables
- JVM arguments

**MBeans Management:**
- dotCMS custom MBeans
- Application server MBeans
- JVM platform MBeans
- Custom application metrics

### Alternative JMX Tools

#### VisualVM
```bash
# Install VisualVM (if not included with JDK)
# Download from: https://visualvm.github.io/

# Connect to JMX
visualvm --jdkhome $JAVA_HOME
# Add JMX connection: localhost:9999
```

#### Mission Control (Oracle JDK)
```bash
# Launch Mission Control
jmc

# Add connection: localhost:9999
```

#### Command Line Tools
```bash
# View JVM information
jinfo <pid>

# Heap dump analysis  
jmap -dump:format=b,file=heapdump.hprof <pid>

# Thread dump
jstack <pid>
```

### JMX Profile Configuration

The JMX functionality is implemented through Maven profiles:

#### JMX Profile (`-Djmx.enable=true`)
```xml
<profile>
    <id>jmx</id>
    <activation>
        <property>
            <name>jmx.enable</name>
        </property>
    </activation>
    <properties>
        <jmx.args>${jmx.args.default}</jmx.args>
        <docker.debug.args>${docker.jmx.args.default}</docker.debug.args>
    </properties>
</profile>
```

#### JMX + Debug Profile (`-Djmx.debug.enable=true`)
```xml
<profile>
    <id>jmx-debug</id>
    <activation>
        <property>
            <name>jmx.debug.enable</name>
        </property>
    </activation>
    <properties>
        <jmx.args>${jmx.args.default}</jmx.args>
        <debug.args>${debug.args.default}</debug.args>
        <docker.debug.args>${docker.debug.jmx.args.default}</docker.debug.args>
    </properties>
</profile>
```

### JVM Arguments Applied

When JMX is enabled, the following JVM arguments are automatically applied:

```bash
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9999  
-Dcom.sun.management.jmxremote.rmi.port=9998
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
-Djava.rmi.server.hostname=localhost
```

### Docker Port Exposure

JMX profiles automatically expose the required Docker ports:

```yaml
ports:
  - "9999:9999"  # JMX Remote Port
  - "9998:9998"  # RMI Registry Port
  - "8080:8080"  # dotCMS Application
  - "8000:8000"  # Java Debug Port (if debug enabled)
  - "4000:4000"  # Glowroot Profiler (if enabled)
```

### Troubleshooting JMX Connections

#### Common Issues

**1. Connection Refused**
- Ensure dotCMS is running with JMX enabled
- Check that ports 9999 and 9998 are not blocked by firewall
- Verify Docker port mappings are correct

**2. Authentication Failed**
- Ensure username/password fields are empty in JConsole
- JMX authentication is disabled for development

**3. Wrong JConsole Version**
- Use JConsole from same JDK version as dotCMS (Java 21)
- Avoid mixing different Java versions

**4. Base Image Issues**
- Verify Java base image includes `jdk.management.agent` module
- Rebuild base image if JMX module is missing

#### Debug JMX Connection
```bash
# Check JMX port is listening
netstat -an | grep 9999

# Test connection
telnet localhost 9999

# Check Docker port mapping
docker port <container_name>
```

### Production Considerations

⚠️ **Security Warning**: The current JMX configuration is designed for development only:

- **Authentication disabled**: No username/password required
- **SSL disabled**: Unencrypted communication
- **Localhost only**: Connections limited to local machine

For production environments, additional security configuration is required:
- Enable JMX authentication
- Configure SSL/TLS encryption
- Restrict network access with firewalls
- Use secure connection methods

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
```
```
