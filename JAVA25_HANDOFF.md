# Java 25 Upgrade - Handoff Documentation

**Date**: 2026-02-05
**Objective**: Complete Java 25 upgrade for dotCMS, build Docker image, and verify functionality
**Status**: Configuration complete, ready for build in unrestricted environment

---

## 🎯 Current Status

### ✅ Completed
- All Java 25 configuration changes aligned with official PR #34269
- Maven POM files updated
- Docker configurations updated
- Documentation updated
- Changes tracked in inventory document

### ⚠️ Blocked (Requires Unrestricted Environment)
- Maven build (requires access to Maven Central and repo.dotcms.com)
- Docker image build (depends on Maven build)
- Integration testing
- Final verification

---

## 📋 Reference Information

### Official Pull Requests
- **PR #34264** (Part I - Pre-work): **MERGED**
  - URL: https://github.com/dotCMS/core/pull/34264
  - Dependency updates and code refactoring
  - Already merged into main branch

- **PR #34269** (Part II - Version bump): **DRAFT**
  - URL: https://github.com/dotCMS/core/pull/34269
  - Java version configuration changes
  - Our changes align with this PR

- **Related Issue**: #33865 - Move to Java 25

---

## 📝 Files Modified

### Core Configuration Files (From PR #34269)

#### 1. `.sdkmanrc`
```diff
-java=21.0.8-ms
+java=25.0.1-ms
```
**Purpose**: Sets Java version for SDKMAN and Docker builds

#### 2. `docker/java-base/Dockerfile`
```diff
-ARG SDKMAN_JAVA_VERSION="21.0.8-ms"
+ARG SDKMAN_JAVA_VERSION="25.0.1-ms"
```
**Line**: 9
**Purpose**: Base Java image for Docker

#### 3. `parent/pom.xml`
```diff
-<maven.compiler.release>11</maven.compiler.release>
+<maven.compiler.release>25</maven.compiler.release>

-<glowroot.version>0.14.4</glowroot.version>
+<glowroot.version>0.14.5-beta.3-java25</glowroot.version>
```
**Lines**: 29, 223
**Purpose**:
- **CRITICAL**: Changes bytecode compilation target from Java 11 to Java 25
- Updates Glowroot monitoring tool to Java 25-compatible version

### Additional Consistency Changes (Not in PR #34269)

#### 4. `docker/dev-env/Dockerfile`
```diff
-FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu AS dev-env-builder
+FROM mcr.microsoft.com/openjdk/jdk:25-ubuntu AS dev-env-builder
```
**Line**: 8
**Purpose**: Development environment base image

#### 5. `test-jmeter/pom.xml`
```diff
-<maven.compiler.source>21</maven.compiler.source>
-<maven.compiler.target>21</maven.compiler.target>
-<maven.compiler.release>21</maven.compiler.release>
+<maven.compiler.source>25</maven.compiler.source>
+<maven.compiler.target>25</maven.compiler.target>
+<maven.compiler.release>25</maven.compiler.release>
```
**Lines**: 18-20
**Purpose**: JMeter test module compiler settings

#### 6. `test-karate/pom.xml`
```diff
-<maven.compiler.source>21</maven.compiler.source>
-<maven.compiler.target>21</maven.compiler.target>
-<maven.compiler.release>21</maven.compiler.release>
+<maven.compiler.source>25</maven.compiler.source>
+<maven.compiler.target>25</maven.compiler.target>
+<maven.compiler.release>25</maven.compiler.release>
```
**Lines**: 15-17
**Purpose**: Karate test module compiler settings

#### 7. `CLAUDE.md`
```diff
-Backend: Java 21 runtime, Java 11 syntax (core)
+Backend: Java 25 runtime, Java 11 syntax (core)

-Use modern Java 21 syntax (Java 11 compatible)
+Use modern Java 25 syntax (Java 11 compatible)

-Avoid Java 21 runtime features in core modules
+Avoid Java 25 runtime features in core modules
```
**Lines**: 500, 785, 789
**Purpose**: Documentation updates

### Temporary Workaround Files (NEED TO BE RESTORED)

Due to network restrictions, these files were modified to disable Maven extensions. **These should be reverted once in unrestricted environment**:

#### Files with Commented Extensions:
1. `pom.xml` (lines 97-104)
2. `build-parent/pom.xml` (lines 36-42)
3. `parent/pom.xml` (lines 229-235)
4. `dotCMS/pom.xml` (lines 1547-1553)

**Extension commented out**: `io.quarkus.bot:build-reporter-maven-extension:3.1.0`

---

## 🔧 Setup in Unrestricted Environment

### Prerequisites
```bash
# Verify you have Java 25 installed
java -version
# Should show: openjdk version "25.0.1" or similar

# If not, install via SDKMAN
curl -s "https://get.sdkman.io" | bash
source ~/.sdkman/bin/sdkman-init.sh
sdk install java 25.0.1-ms
sdk use java 25.0.1-ms

# Verify Maven is available (or use ./mvnw)
mvn -version
# Should show: Java version: 25.0.1
```

### Step 1: Review Changes
```bash
# Navigate to project directory
cd /path/to/dotcms/source-code/core

# Review all modified files
git status

# See detailed changes
git diff

# Key files to verify:
git diff .sdkmanrc
git diff parent/pom.xml
git diff docker/java-base/Dockerfile
```

### Step 2: Restore Maven Extensions (Important!)
```bash
# Uncomment build-reporter-maven-extension in these files:
# 1. pom.xml (lines 97-104)
# 2. build-parent/pom.xml (lines 36-42)
# 3. parent/pom.xml (lines 229-235)
# 4. dotCMS/pom.xml (lines 1547-1553)

# Or use these commands to restore:
sed -i 's/<!-- Temporarily disabled for Java 25 testing due to network restrictions -->//g' pom.xml build-parent/pom.xml parent/pom.xml dotCMS/pom.xml
sed -i 's/<!--//g' pom.xml build-parent/pom.xml parent/pom.xml dotCMS/pom.xml
sed -i 's/-->//g' pom.xml build-parent/pom.xml parent/pom.xml dotCMS/pom.xml
```

---

## 🚀 Build and Test Process

### Step 3: Full Maven Build
```bash
# Clean build without tests (8-15 minutes)
mvn clean install -DskipTests

# Expected output (final lines):
# [INFO] BUILD SUCCESS
# [INFO] Total time: XX:XX min
```

**If build fails**, check:
- Java version: `mvn -version` should show Java 25
- Network access to Maven Central and repo.dotcms.com
- Review error logs in `/tmp/build-java25-*.log`

### Step 4: Build Docker Image
```bash
# Build Docker image with Java 25
mvn -pl :dotcms-core -Pdocker-start -Dtomcat.port=8080 -Ddocker.glowroot.enabled=true

# Or use justfile command if available:
just dev-run
```

### Step 5: Verify Docker Image
```bash
# Check Java version in built image
docker run --rm dotcms/dotcms:latest java -version

# Expected output:
# openjdk version "25.0.1" 2025-10-21 LTS
# OpenJDK Runtime Environment Temurin-25.0.1+8 (build 25.0.1+8-LTS)
# OpenJDK 64-Bit Server VM Temurin-25.0.1+8 (build 25.0.1+8-LTS, mixed mode, sharing)

# Check image size and layers
docker images dotcms/dotcms:latest
```

### Step 6: Start and Test Container
```bash
# Start dotCMS container
docker run -d \
  --name dotcms-java25-test \
  -p 8080:8080 \
  -p 8090:8090 \
  -e LANG=en_US.UTF-8 \
  dotcms/dotcms:latest

# Watch logs for startup
docker logs -f dotcms-java25-test

# Look for:
# - No Java version errors
# - Successful Tomcat startup
# - Port binding messages
```

### Step 7: Verify Runtime
```bash
# Test health endpoints (once container is up)
# Liveness check
curl http://localhost:8080/livez

# Readiness check
curl http://localhost:8080/readyz

# Detailed health (requires auth)
curl -u admin@dotcms.com:admin http://localhost:8080/api/v1/health

# Verify Glowroot monitoring
open http://localhost:8090  # Or curl http://localhost:8090
```

### Step 8: Run Integration Tests
```bash
# Start services for IDE testing
just test-integration-ide

# Or run specific test class via Maven
mvn verify -pl :dotcms-integration \
  -Dcoreit.test.skip=false \
  -Dit.test=ContentTypeAPIImplTest

# Run Postman tests
just test-postman ai

# Stop services when done
just test-integration-stop
```

---

## ✅ Verification Checklist

### Build Verification
- [ ] Maven build completes with `BUILD SUCCESS`
- [ ] No Java version compatibility warnings
- [ ] All modules compile successfully
- [ ] Docker image builds without errors

### Runtime Verification
- [ ] Container starts successfully
- [ ] No Java version errors in logs
- [ ] Health endpoints respond:
  - [ ] `/livez` returns 200 OK
  - [ ] `/readyz` returns 200 OK
  - [ ] `/api/v1/health` returns JSON health status
- [ ] Glowroot monitoring accessible on port 8090
- [ ] Java version in container shows 25.0.1

### Functionality Verification
- [ ] Admin login works
- [ ] Basic content operations work
- [ ] API endpoints respond correctly
- [ ] No runtime Java compatibility errors

### Test Verification
- [ ] At least one integration test suite passes
- [ ] Postman tests pass
- [ ] No Java 25-specific test failures

---

## 🐛 Troubleshooting

### Maven Build Failures

**Issue**: Dependency resolution errors
```bash
# Clear Maven cache and retry
rm -rf ~/.m2/repository
mvn clean install -DskipTests -U
```

**Issue**: Compiler errors related to Java version
```bash
# Verify Maven is using Java 25
mvn -version

# Force Java 25
export JAVA_HOME=/path/to/java-25
mvn clean install -DskipTests
```

**Issue**: Plugin compatibility errors
- Check if plugin versions need updates
- Review build logs for specific plugin errors
- Some plugins may need Java 25 compatible versions

### Docker Build Failures

**Issue**: Base image not found
```bash
# Pull Microsoft OpenJDK 25 image manually
docker pull mcr.microsoft.com/openjdk/jdk:25-ubuntu

# Verify SDKMAN has Java 25.0.1-ms
sdk list java | grep "25.0.1-ms"
sdk install java 25.0.1-ms
```

**Issue**: Build context errors
```bash
# Check Docker daemon is running
docker info

# Verify docker-maven-plugin configuration
mvn help:effective-pom -pl :dotcms-core | grep docker
```

### Runtime Failures

**Issue**: Container crashes on startup
```bash
# Check logs for Java errors
docker logs dotcms-java25-test 2>&1 | grep -i "error\|exception\|fatal"

# Common issues:
# - Java modules/exports errors (check java.module.args in parent/pom.xml)
# - Memory settings (adjust Docker memory limits)
# - Port conflicts (check if 8080/8090 are free)
```

**Issue**: Glowroot not working
- This is expected - using beta version `0.14.5-beta.3-java25`
- Check Glowroot logs in container
- May need to wait for stable Java 25 release

---

## 📊 Inventory and Tracking

### Full Inventory Document
See: `JAVA25_UPGRADE_INVENTORY.md` for detailed tracking of:
- All configuration changes
- Issues encountered and resolutions
- Dependency changes
- Testing results

### Git Workflow
```bash
# Create feature branch if not already
git checkout -b issue-33865-java-25-final

# Review all changes
git status
git diff

# Stage configuration changes
git add .sdkmanrc parent/pom.xml docker/

# Stage test module changes
git add test-jmeter/pom.xml test-karate/pom.xml

# Stage documentation
git add CLAUDE.md JAVA25_UPGRADE_INVENTORY.md JAVA25_HANDOFF.md

# Commit with conventional commit format
git commit -m "feat: upgrade to Java 25

- Update .sdkmanrc to Java 25.0.1-ms (Microsoft OpenJDK)
- Update docker/java-base/Dockerfile to Java 25.0.1-ms
- Update docker/dev-env/Dockerfile to use mcr.microsoft.com/openjdk/jdk:25-ubuntu
- Update parent/pom.xml: maven.compiler.release 11 → 25
- Update parent/pom.xml: glowroot.version to 0.14.5-beta.3-java25
- Update test-jmeter/pom.xml compiler settings to Java 25
- Update test-karate/pom.xml compiler settings to Java 25
- Update CLAUDE.md documentation references

Related to #33865 and PR #34269

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"

# Push to remote
git push origin issue-33865-java-25-final
```

### Create Pull Request
```bash
# Using GitHub CLI
gh pr create \
  --title "feat: upgrade to Java 25 (complete)" \
  --body "Complete Java 25 upgrade including all configuration changes aligned with PR #34269.

## Changes
- ✅ Core configuration files updated (.sdkmanrc, parent/pom.xml, Dockerfiles)
- ✅ Test modules updated (test-jmeter, test-karate)
- ✅ Glowroot updated to Java 25-compatible version
- ✅ Documentation updated

## Testing
- [ ] Maven build succeeds
- [ ] Docker image builds successfully
- [ ] Container starts and runs
- [ ] Integration tests pass
- [ ] Health endpoints respond

## Related
- Part I: #34264 (merged)
- Part II: #34269 (draft)
- Issue: #33865

## Verification
\`\`\`bash
# Build
mvn clean install -DskipTests

# Verify Java version
docker run --rm dotcms/dotcms:latest java -version

# Test
mvn verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=ContentTypeAPIImplTest
\`\`\`" \
  --label "enhancement" \
  --label "java-25"
```

---

## 🔑 Key Differences from PR #34269

Our implementation includes additional changes beyond PR #34269:

1. **docker/dev-env/Dockerfile**: Updated to Java 25 (not in PR #34269)
2. **test-jmeter/pom.xml**: Updated compiler settings (not in PR #34269)
3. **test-karate/pom.xml**: Updated compiler settings (not in PR #34269)

These additional changes ensure **complete consistency** across the entire codebase. All modules and Docker images now use Java 25.

---

## 📚 Additional Resources

### Java 25 Documentation
- Release Notes: https://openjdk.org/projects/jdk/25/
- Migration Guide: https://docs.oracle.com/en/java/javase/25/migrate/

### Microsoft OpenJDK
- Container Images: https://learn.microsoft.com/en-us/java/openjdk/containers
- Java 25 Support: https://devblogs.microsoft.com/java/microsofts-openjdk-builds-now-ready-for-java-25/

### Glowroot Java 25 Support
- Java 25 Beta: https://github.com/glowroot/glowroot/releases/tag/v0.14.5-beta.3
- Issue Tracking: Check Glowroot GitHub for Java 25 compatibility updates

---

## 📞 Support and Questions

### If Build Succeeds ✅
1. Update `JAVA25_UPGRADE_INVENTORY.md` with success details
2. Complete verification checklist above
3. Document any issues or warnings encountered
4. Create PR with test results
5. Notify team of completion

### If Build Fails ❌
1. Capture full error logs
2. Document specific failure point in inventory
3. Check troubleshooting section above
4. Review Java 25 compatibility of failing dependencies
5. May need to update additional dependencies or wait for Java 25 support

### Critical Files to Monitor
- Maven build logs: Look for Java version warnings
- Container startup logs: Look for Java runtime errors
- Glowroot: May have issues with beta version
- Test failures: May indicate Java 25 compatibility issues in dependencies

---

## 🎯 Success Criteria

The Java 25 upgrade is **COMPLETE** when:

✅ Maven build succeeds with `BUILD SUCCESS`
✅ Docker image builds successfully
✅ Container starts without Java errors
✅ Health endpoints respond correctly
✅ At least one integration test suite passes
✅ No critical Java 25 compatibility errors
✅ Glowroot monitoring is accessible (beta version)

---

## 📝 Next Steps After Success

1. **Merge PR #34269** if this is part of official effort
2. **Update CI/CD pipelines** to use Java 25
3. **Update developer documentation** with Java 25 setup
4. **Monitor Glowroot** for stable Java 25 release
5. **Plan Java 25 feature adoption** (if using new JDK features)

---

**Generated**: 2026-02-05
**Environment**: Restricted network environment
**Java Version Target**: 25.0.1 (Microsoft OpenJDK)
**Status**: Configuration complete, ready for unrestricted build

---

## Quick Reference Commands

```bash
# Build
mvn clean install -DskipTests

# Docker Build
mvn -pl :dotcms-core -Pdocker-start

# Verify Java in Container
docker run --rm dotcms/dotcms:latest java -version

# Start Container
docker run -d --name dotcms-test -p 8080:8080 -p 8090:8090 dotcms/dotcms:latest

# Test Health
curl http://localhost:8080/livez

# Run Tests
mvn verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MyTest

# View Logs
docker logs -f dotcms-test
```

Good luck! 🚀
