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
# Find declared but unused dependencies (candidates only — see warning below)
./mvnw dependency:analyze -pl :dotcms-core -am -DskipTests | grep -A200 "Unused declared dependencies"
```

> ⚠️ `dependency:analyze` only sees compile-time bytecode references. In `dotcms-core` most of its
> "unused declared" hits are false positives: ImageIO/ScriptEngine/Jackson modules loaded via
> `ServiceLoader`, Jersey/Log4j runtime jars, class names in config (cache chains, `toolbox.xml`,
> workflow actionlets stored in the DB), and libraries required only by other repackaged
> `com.dotcms.lib:dot.*` jars. Treat every hit as a candidate to investigate, never as proof —
> see [Third-Party Library Policy](#third-party-library-policy).

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

## Third-Party Library Policy

Applies to anyone — human or coding agent — adding, upgrading or removing a third-party library.
Every library we ship is ongoing update, CVE and supply-chain exposure for every customer, so each
one has to earn its place.

### Adding or upgrading
- **Prefer what we already have.** Use the JDK, supported platform APIs, or a library already in
  `bom/application/pom.xml` when it meets the need. For small, narrowly scoped functionality a short
  local implementation can be easier to maintain than a new jar.
- **Never hand-roll security-sensitive code.** Cryptography, authentication, JWT/SAML/OIDC, and
  complex parsing (XML, HTML sanitizing, JSON Schema, archives) use established libraries or vetted
  platform APIs.
- **Inspect the resolved transitive tree** (`./mvnw dependency:tree -pl :dotcms-core`) before and
  after. Prefer options with fewer transitive dependencies; justify any substantial increase.
- **Watch for BOM version mismatches.** The BOM pins shared libraries (e.g. Jackson via
  `jackson-bom`). If a new library was built against a newer version than our pin, do not bump the
  pin as a side effect — choose a release built against our line, or prove compatibility with a test.
- **Verify the package live, never from memory.** Confirm the exact `groupId:artifactId` and
  publisher against the project's own repository or docs, and read versions from
  `repo1.maven.org/.../maven-metadata.xml` (the `search.maven.org` index can be stale). Investigate
  lookalike names, ownership changes, and unfamiliar new packages.
- **Pick the latest stable compatible release.** Scrutinize very recent releases and unexpected
  maintainer changes; explain any choice of an older release. Reject archived, deprecated or
  abandoned projects — judge maintenance by support status and security responsiveness, not just
  release frequency.
- **Check advisories** in OSV.dev, the GitHub Advisory Database and NVD. Record the sources, the
  date checked and whether any advisory applies. Absence of an advisory is not proof of safety.
  If you cannot reach these sources, say so explicitly in the PR ("advisories not checked") so a
  reviewer or CI dependency scan covers it — never describe an unchecked version as clean.
- **Keep updates scoped.** No unrelated upgrades or forced overrides. Prefer a compatible parent
  library update when a transitive dependency needs remediation. Review release notes for breaking
  changes; if an upgrade turns into a migration, stop and report rather than retrying speculative fixes.
- **If you cannot verify these requirements, stop and report the blocker.** Do not add or upgrade the
  dependency unless the developer explicitly accepts the identified risk.

### Removing
- **Every library on the core classpath is potential public API.** OSGi plugins can import any
  exported package (`OSGIUtil.processOsgiPackages` adds plugin imports to
  `org.osgi.framework.system.packages.extra`), including `com.dotcms.repackage.*`. Removing a library
  can break customer plugins we cannot see — call it out in the PR description so it reaches the
  release notes.
- **Confirm non-use beyond grep and `dependency:analyze`.** Check `ServiceLoader`/SPI registration,
  reflection and `Class.forName`, class names in config and the database, OSGi export lists
  (`dotCMS/src/main/resources/osgi/**/osgi-extra.conf`), and bytecode references from other jars.
  Search with `rg -uu --glob '!*.class'` — some sources contain NUL bytes and are skipped as binary
  by default. No grep finds class names assembled at runtime (string concatenation, values read
  from config or the database), so a zero-hit search is evidence, not proof.
- **Remove from both places**: the module `pom.xml` entry and the BOM `dependencyManagement` entry —
  unless the BOM entry still pins a transitive version (often for a CVE fix), in which case keep it.
- **Removing a feature is not a dependency change.** If a library backs a user-visible feature
  (an actionlet, a Velocity tool, a cache provider), get a product decision first.
- **Verify in CI.** Local unit tests are necessary but not sufficient: dependency removals need the
  full CI run (integration suites, Postman), and a before/after diff of `WEB-INF/lib`.

<sub>Adapted from [Trim Reaper](https://github.com/jmanico/trim-reaper) (commit `561cebe`),
Copyright (c) 2026 Jim Manico, MIT License.</sub>

## Key Rules (NEVER VIOLATE)
- **NEVER** add version numbers to dependencies in `dotCMS/pom.xml`
- **NEVER** add version numbers to plugins in module POMs
- **ALWAYS** add new dependency versions to `bom/application/pom.xml`
- **ALWAYS** add new plugin versions to `parent/pom.xml`
- **ALWAYS** define global properties in `parent/pom.xml`
- **ALWAYS** run `./mvnw dependency:analyze` before major dependency changes
- **ALWAYS** follow the [Third-Party Library Policy](#third-party-library-policy) when adding, upgrading or removing a library
- **ALWAYS** check dependency tree for conflicts: `./mvnw dependency:tree -Dverbose`
