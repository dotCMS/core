# Java 25 Upgrade Inventory

## Objective
Upgrade dotCMS from Java 21 to Java 25, build a working Docker container, and test functionality.

## Reference
- Official PR #34264 (Part I - Pre-work): MERGED - Dependency updates and code refactoring
- Official PR #34269 (Part II - Version bump): DRAFT - Java version configuration changes
- Issue #33865: Move to Java 25

## Changes Aligned with Official PR #34269

## Breaking Changes and Fixes

### Configuration Changes (Aligned with Official PR #34269)

#### Core Configuration Files
1. **`.sdkmanrc`** ✅
   - Changed: `java=21.0.8-ms` → `java=25.0.1-ms`
   - Uses Microsoft OpenJDK build

2. **`docker/java-base/Dockerfile`** ✅
   - Line 9: `ARG SDKMAN_JAVA_VERSION="21.0.8-ms"` → `ARG SDKMAN_JAVA_VERSION="25.0.1-ms"`

3. **`parent/pom.xml`** ✅
   - Line 29: `<maven.compiler.release>11</maven.compiler.release>` → `<maven.compiler.release>25</maven.compiler.release>`
   - Line 223: `<glowroot.version>0.14.4</glowroot.version>` → `<glowroot.version>0.14.5-beta.3-java25</glowroot.version>`
   - **Critical**: Compiler now targets Java 25 bytecode (was Java 11)

#### Additional Changes (Not in Official PR but Needed for Consistency)
4. **`docker/dev-env/Dockerfile`** ✅
   - Line 8: `FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu` → `FROM mcr.microsoft.com/openjdk/jdk:25-ubuntu`

5. **`test-jmeter/pom.xml`** ✅
   - Lines 18-20: Updated compiler source/target/release from 21 to 25

6. **`test-karate/pom.xml`** ✅
   - Lines 15-17: Updated compiler source/target/release from 21 to 25

#### Documentation
7. **`CLAUDE.md`** ✅
   - Lines 500, 785, 789: Updated all "Java 21" references to "Java 25"

### Dependency Issues
- **Status**: Pending
- **Issues found**: TBD
- **Resolution**: TBD

### Build Issues Encountered

#### Network Restrictions (Current Blocker)
- **Status**: Blocking build completion
- **Issues**:
  1. Cannot download Maven extensions:
     - `maven-build-cache-extension:1.2.0`
     - `build-reporter-maven-extension:3.1.0`
  2. Cannot download dependency BOMs from `repo.dotcms.com`:
     - `aws-java-sdk-bom:1.12.488`
     - `jackson-bom:2.17.2`
     - `jersey-bom:2.47`
     - `micrometer-bom:1.13.10`
     - `junit-bom:5.10.2`
     - `quarkus-bom:3.6.0`
  3. Cannot resolve hundreds of transitive dependencies

- **Workaround Applied**:
  - Build-reporter-maven-extension commented out in:
    - `pom.xml`
    - `build-parent/pom.xml`
    - `parent/pom.xml`
    - `dotCMS/pom.xml`
  - Maven build cache extension restored (will need network access)

- **Resolution Required**:
  - Network access to Maven Central (`repo.maven.apache.org`)
  - Network access to dotCMS Artifactory (`repo.dotcms.com`)
  - OR: Build in environment with internet access
  - OR: Use pre-populated local Maven repository

### Runtime Issues
- **Status**: Pending
- **Issues found**: TBD
- **Resolution**: TBD

### Testing Results
- **Status**: Pending
- **Tests run**: TBD
- **Results**: TBD

## Timeline
- **Started**: 2026-02-05
- **Completed**: TBD

## Summary of Changes

### Files Modified (7 total)
1. `.sdkmanrc` - Java version
2. `docker/java-base/Dockerfile` - SDKMAN Java version ARG
3. `docker/dev-env/Dockerfile` - Base image updated to Java 25
4. `parent/pom.xml` - Compiler release version + Glowroot version
5. `test-jmeter/pom.xml` - Compiler versions
6. `test-karate/pom.xml` - Compiler versions
7. `CLAUDE.md` - Documentation updates

### Files Temporarily Modified (4 total - for network workaround)
1. `pom.xml` - build-reporter-maven-extension commented out
2. `build-parent/pom.xml` - build-reporter-maven-extension commented out
3. `parent/pom.xml` - build-reporter-maven-extension commented out
4. `dotCMS/pom.xml` - build-reporter-maven-extension commented out

## Next Steps

### To Complete the Upgrade:
1. **Restore network access** or build in environment with internet connectivity
2. **Uncomment Maven extensions** in POM files once network is available
3. **Run full build**: `mvn clean install -DskipTests`
4. **Build Docker image**: `mvn -pl :dotcms-core -Pdocker-start`
5. **Test container startup** and verify Java 25 runtime
6. **Run integration tests**: Targeted test suite with Java 25
7. **Verify functionality**: Health endpoints, basic operations

### Key Validation Points:
- [ ] Maven build completes successfully with Java 25
- [ ] Docker image builds with Java 25 base
- [ ] Container starts and runs without Java version errors
- [ ] Health endpoints respond correctly
- [ ] Integration tests pass
- [ ] Glowroot monitoring works with Java 25 beta

## Final Status
- **Configuration Changes**: Complete ✅ (aligned with PR #34269)
- **Build Status**: Blocked by network restrictions ⚠️
- **Docker Image**: Not created (pending successful build)
- **Tests Passed**: Not run yet
