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

### Basic Usage
```bash
# Start development environment
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