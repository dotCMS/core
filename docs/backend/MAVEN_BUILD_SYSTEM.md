# Maven Build System (CRITICAL)

## Build Hierarchy Overview
```
parent/pom.xml              # Global properties, plugin management
├── bom/application/pom.xml # Dependency management (versions)
└── dotCMS/pom.xml         # Module dependencies (no versions)
```

## Dependency Management Pattern (CRITICAL)
**⚠️ ALL dependencies must follow this pattern:**

### 1. Define Versions in BOM
Add new dependency versions to `bom/application/pom.xml`:
```xml
<properties>
    <new-library.version>1.2.3</new-library.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>new-library</artifactId>
            <version>${new-library.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. Reference Without Version in Modules
Use in `dotCMS/pom.xml` (NO version):
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>new-library</artifactId>
</dependency>
```

### 3. Existing Swagger/OpenAPI Dependencies
```xml
<!-- In bom/application/pom.xml -->
<swagger.version>2.2.0</swagger.version>

<!-- In dotCMS/pom.xml (versions inherited) -->
<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-jaxrs2</artifactId>
</dependency>
```

## Plugin Management Pattern (CRITICAL)
**⚠️ ALL plugins must follow this pattern:**

### 1. Define in Parent POM
Add to `parent/pom.xml` in `<pluginManagement>`:
```xml
<pluginManagement>
    <plugins>
        <plugin>
            <groupId>com.example</groupId>
            <artifactId>example-maven-plugin</artifactId>
            <version>1.0.0</version>
        </plugin>
    </plugins>
</pluginManagement>
```

### 2. Reference Without Version in Modules
Use in any module POM (NO version):
```xml
<plugin>
    <groupId>com.example</groupId>
    <artifactId>example-maven-plugin</artifactId>
</plugin>
```

## Dependency Analysis & Troubleshooting

### Essential Maven Commands for Dependency Management
```bash
# View complete dependency tree
./mvnw dependency:tree

# Show dependency tree for specific module
./mvnw dependency:tree -pl :dotcms-core

# Find duplicate dependencies (different versions)
./mvnw dependency:tree -Dverbose

# Analyze dependency conflicts
./mvnw dependency:analyze

# Show effective POM (with resolved versions)
./mvnw help:effective-pom

# Display dependency updates available
./mvnw versions:display-dependency-updates

# Check for plugin updates
./mvnw versions:display-plugin-updates

# Resolve dependency convergence issues
./mvnw dependency:analyze-report
```

### Troubleshooting Common Issues

#### 1. Version Conflicts
```bash
# Find conflicting versions
./mvnw dependency:tree -Dverbose | grep -A5 -B5 "omitted for conflict"

# Example output showing conflict:
# [INFO] +- com.fasterxml.jackson.core:jackson-core:jar:2.15.2:compile
# [INFO] |  \- com.fasterxml.jackson.core:jackson-annotations:jar:2.15.2:compile
# [INFO] \- com.example:library:jar:1.0.0:compile
# [INFO]    \- (com.fasterxml.jackson.core:jackson-core:jar:2.12.3:compile - omitted for conflict with 2.15.2)
```

#### 2. Duplicate Dependencies
```bash
# Find duplicate JARs in classpath
./mvnw dependency:tree -Dverbose | grep -E "\+\-.*:.*:.*:.*:.*"

# Check for duplicate classes
./mvnw dependency:analyze-duplicate
```

#### 3. Missing Dependencies
```bash
# Analyze used but undeclared dependencies
./mvnw dependency:analyze

# Example output:
# [WARNING] Used undeclared dependencies found:
# [WARNING]    com.google.guava:guava:jar:31.1-jre:compile
```

#### 4. Unused Dependencies
```bash
# Find declared but unused dependencies
./mvnw dependency:analyze | grep "Unused declared dependencies"

# Remove unused dependencies to reduce JAR size
./mvnw dependency:analyze -DfailOnWarning=true
```

### Dependency Resolution Best Practices

#### 1. Override Transitive Dependencies
When a transitive dependency needs a specific version:
```xml
<!-- In bom/application/pom.xml -->
<properties>
    <jackson.version>2.17.2</jackson.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Force specific version for all Jackson modules -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-core</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 2. Exclude Problematic Transitive Dependencies
```xml
<!-- In dotCMS/pom.xml -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>library-with-conflicts</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-log4j12</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

#### 3. Verify Dependency Convergence
```bash
# Check that all dependencies converge to single versions
./mvnw enforcer:enforce -Denforcer.rules=DependencyConvergence
```

### Integration with Build Pipeline

#### Pre-commit Checks
```bash
# Verify no dependency issues before commit
./mvnw dependency:analyze dependency:analyze-duplicate
```

#### CI/CD Pipeline Integration
```bash
# In build pipeline, fail on dependency issues
./mvnw dependency:analyze -DfailOnWarning=true
./mvnw enforcer:enforce -Denforcer.rules=DependencyConvergence
```

### Common Dependency Patterns

#### 1. Security Vulnerability Updates
```bash
# Check for known vulnerabilities
./mvnw org.owasp:dependency-check-maven:check

# Update vulnerable dependencies in BOM
# Always test thoroughly after security updates
```

#### 2. Major Version Updates
```bash
# Before major version updates, analyze impact
./mvnw dependency:tree -Dincludes=com.fasterxml.jackson.core:*
./mvnw dependency:analyze-report
```

#### 3. Dependency Scope Management
```bash
# Verify test dependencies don't leak to runtime
./mvnw dependency:tree -Dscope=runtime
./mvnw dependency:tree -Dscope=test
```

## Key Rules (NEVER VIOLATE)
- **NEVER** add version numbers to dependencies in `dotCMS/pom.xml`
- **NEVER** add version numbers to plugins in module POMs
- **ALWAYS** add new dependency versions to `bom/application/pom.xml`
- **ALWAYS** add new plugin versions to `parent/pom.xml`
- **ALWAYS** define global properties in `parent/pom.xml`
- **ALWAYS** run `./mvnw dependency:analyze` before major dependency changes
- **ALWAYS** check dependency tree for conflicts: `./mvnw dependency:tree -Dverbose`