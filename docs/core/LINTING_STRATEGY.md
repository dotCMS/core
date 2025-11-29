# DotCMS Linting Strategy

## Overview

The DotCMS project implements a **comprehensive multi-layer linting strategy** across three primary enforcement points:

1. **Git Pre-Commit Hook** - First line of defense, runs locally
2. **Frontend Tooling (Nx/ESLint)** - Core-web linting via Nx workspace
3. **GitHub Actions CI/CD** - Final validation before merge

This document provides a comprehensive review of all linting mechanisms and recommendations for optimization.

---

## 1. Git Pre-Commit Hook Strategy

**Location**: `core-web/.husky/pre-commit`

### What It Does

The pre-commit hook provides **immediate developer feedback** before code reaches CI/CD. It's designed to catch issues early while allowing developers to bypass when necessary.

### Linting Layers

#### Layer 1: Frontend Code (Lines 214-299)
```bash
# Auto-formatting + validation
nx affected -t lint --exclude='tag:skip:lint' --fix=true
nx format:write
```

**Tools Used**:
- **ESLint** - TypeScript/JavaScript linting with Angular rules
- **Prettier** (via nx format) - Auto-formatting for TS/JS/HTML/SCSS/JSON
- **Nx affected** - Only checks changed projects for performance

**What It Catches**:
- TypeScript errors (`@typescript-eslint/no-explicit-any`, `no-unused-vars`)
- Import order violations
- Circular dependencies
- Focused tests (`fit`, `fdescribe`)
- Module boundary violations
- Code style violations

**Auto-fixes**:
- ✅ Import ordering
- ✅ Code formatting (indentation, spacing, quotes)
- ✅ Trailing whitespace
- ❌ Logic errors (require manual fix)

#### Layer 2: Workflow Files (Lines 561-633)
```bash
# Auto-formatting + validation
prettier --write (YAML files)
yamllint (validation)
actionlint (GitHub Actions validation)
```

**Tools Used**:
- **Prettier** - Auto-formats YAML files
- **yamllint** - YAML syntax and style validation
- **actionlint** - GitHub Actions-specific validation

**What It Catches**:
- YAML syntax errors
- Indentation issues
- Line length violations
- Deprecated GitHub Actions
- Script injection vulnerabilities
- Invalid workflow expressions

**Auto-fixes**:
- ✅ YAML formatting (indentation, spacing, line breaks)
- ❌ Structural issues (require manual fix)

### Error Handling Strategy

**Philosophy**: Warn but don't block, unless critical

```bash
# Frontend linting - warns but continues
if [ $lint_exit_code -ne 0 ]; then
    print_color "$YELLOW" "⚠️ nx affected lint failed, but continuing..."
    has_errors=true
fi

# Workflow linting - blocks on errors
if ! yamllint; then
    print_color "$RED" "❌ yamllint found issues"
    has_errors=true
fi
```

**Bypass Mechanism**: Developers can use `git commit --no-verify` for urgent commits

### Performance Optimizations

1. **Conditional Execution** - Only runs when relevant files change
2. **Nx Affected** - Only lints changed projects
3. **Parallel Execution** - Independent tools run concurrently
4. **Caching** - Nx caches lint results for unchanged code
5. **ENOBUFS Auto-Fix** - Automatically recovers from macOS buffer issues

---

## 2. Frontend Linting (core-web/)

**Location**: `core-web/.eslintrc.base.json`, `core-web/nx.json`

### ESLint Configuration

**Base Rules** (`.eslintrc.base.json`):

#### TypeScript Rules
```json
{
  "@typescript-eslint/no-explicit-any": ["error"],
  "@typescript-eslint/no-unused-vars": ["error", {"argsIgnorePattern": "^_"}],
  "no-console": ["error", {"allow": ["warn", "error"]}],
  "no-duplicate-imports": "error"
}
```

#### Module Boundaries
```json
{
  "@nx/enforce-module-boundaries": "error",
  "@nx/dependency-checks": "error"
}
```

#### Import Order Enforcement
- External libraries first
- Angular/PrimeNG/RxJS grouped together
- Internal `@dotcms/*` packages
- Relative imports last
- Alphabetically sorted within groups

#### Test Quality Rules
```json
{
  "ban/ban": [
    {"name": ["describe", "only"], "message": "don't focus tests"},
    {"name": "fdescribe", "message": "don't focus tests"},
    {"name": ["it", "only"], "message": "don't focus tests"},
    {"name": "fit", "message": "don't focus tests"}
  ]
}
```

### Nx Integration

**Cacheable Operations** (`nx.json`):
```json
{
  "cacheableOperations": ["build", "lint", "test", "e2e", "build-storybook"]
}
```

**Target Defaults**:
- Lint inputs: `["default", "{workspaceRoot}/.eslintrc.json"]`
- Cache enabled for `@nx/eslint:lint` target
- Parallel execution: 1 (sequential due to resource constraints)

### Prettier Configuration

**Auto-Formatting** (via `nx format:write`):
- ✅ TypeScript/JavaScript files
- ✅ HTML templates
- ✅ SCSS stylesheets
- ✅ JSON files
- ✅ Markdown files
- ✅ YAML workflow files (`.prettierrc.yml`)

---

## 3. Backend Linting (Maven)

**Location**: `parent/pom.xml`

### Current State: LIMITED BACKEND LINTING

#### What's Available

**Spotless Maven Plugin** (v2.37.0):
```xml
<spotless.skip>true</spotless.skip>  <!-- DISABLED by default -->
```

**Configured For**:
- ✅ `.gitignore` formatting (trimming, newlines, indentation)
- ✅ `pom.xml` sorting and formatting
- ❌ TypeScript/JavaScript (commented out, replaced by Nx)

**Status**: **SKIPPED** - `<spotless.skip>true</spotless.skip>` means it doesn't run in builds

**Maven Checkstyle Plugin** (v3.3.0):
- ✅ Defined in parent POM
- ❌ No active profile or execution binding
- ❌ No checkstyle.xml configuration file
- **Status**: **NOT ACTIVELY USED**

#### Java Compiler Warnings

**Active Linting** (`maven-compiler-plugin`):
```xml
<compilerArgs>
    <arg>-Xlint:unchecked</arg>
</compilerArgs>
<showDeprecation>true</showDeprecation>
<showWarnings>true</showWarnings>
```

**What It Catches**:
- Unchecked type conversions
- Raw type usage
- Deprecated API usage
- Java warnings during compilation

**Limitations**:
- Only runs during compilation
- No auto-fix capability
- No style enforcement
- No pattern detection beyond compiler rules

---

## 4. CI/CD Linting (GitHub Actions)

**Location**: `.github/workflows/cicd_1-pr.yml`

### Workflow Lint Job (Lines 50-87)

```yaml
workflow-lint:
  needs: [initialize]
  if: needs.initialize.outputs.workflows == 'true'  # Conditional execution
  steps:
    - Install yamllint
    - Run yamllint on .github/workflows/
    - Install actionlint
    - Run actionlint
```

**Triggers**:
- Only runs when `.github/workflows/` or `.github/actions/` files change
- Detected via `.github/filters.yaml`

**Tools**:
- **yamllint** - YAML syntax validation
- **actionlint** - GitHub Actions semantic validation

**Benefits**:
- Catches issues missed by local hooks
- Enforces consistency across all contributors
- Runs in clean CI environment
- Blocks PR merge on failure

### Frontend CI Linting

**Execution**: Part of build phase, runs via Nx targets
- `nx affected -t lint`
- `nx affected -t test`
- `nx affected -t build`

**Strategy**: Nx affected ensures only changed code is validated

---

## 5. Gap Analysis & Recommendations

### ✅ Strengths

1. **Comprehensive Frontend Coverage**
   - Multi-tool approach (ESLint + Prettier)
   - Auto-fixing reduces developer friction
   - Nx affected optimizes performance
   - Strong TypeScript enforcement

2. **Robust Workflow Validation**
   - Two-tier validation (yamllint + actionlint)
   - Auto-formatting reduces manual fixes
   - Security-focused (script injection detection)
   - Deprecated action detection

3. **Layered Defense**
   - Pre-commit hook (local)
   - CI/CD validation (remote)
   - Graceful degradation when tools missing

### ❌ Gaps & Issues

#### 1. **No Active Backend Java Linting**

**Current State**:
- ✅ Compiler warnings enabled
- ❌ Checkstyle plugin **not activated**
- ❌ Spotless plugin **skipped by default**
- ❌ No PMD or Error Prone

**Impact**:
- No code style enforcement
- No pattern detection (e.g., deprecated patterns, security issues)
- No auto-formatting for Java code
- Inconsistent code style across Java modules

**Recommendation**: See Section 6

#### 2. **Spotless Disabled for Java**

**Current State**:
```xml
<spotless.skip>true</spotless.skip>
```

**Why It's Disabled**: TypeScript/JavaScript formatting moved to Nx

**Impact**:
- No auto-formatting for Java code
- No enforcement of Google Java Format or similar
- Manual code review required for style issues

#### 3. **Checkstyle Defined But Not Used**

**Current State**:
- Plugin defined in `<pluginManagement>`
- No execution bindings
- No `checkstyle.xml` configuration file
- Not bound to any Maven phase

**Impact**: Zero backend style enforcement

#### 4. **No Java Import Order Enforcement**

**Frontend**: ESLint enforces strict import ordering
**Backend**: No equivalent for Java imports

#### 5. **Dual YAML Linting Not a Problem**

**User Question**: "We now have two YAML linting mechanisms. What is the best way?"

**Answer**: This is intentional and follows best practices:
- **Prettier** = Auto-formatting (fixes spacing, indentation)
- **yamllint** = Validation (catches structural issues)

**Analogy**: Same as frontend (Prettier + ESLint)

---

## 6. Recommendations

### Priority 1: Enable Backend Java Linting

#### Option A: Activate Spotless for Java (Recommended)

**Why**: Already installed, just needs activation

**Implementation**:
```xml
<!-- In parent/pom.xml -->
<properties>
    <spotless.skip>false</spotless.skip>  <!-- Enable it -->
</properties>

<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>  <!-- Validate in CI -->
            </goals>
        </execution>
    </executions>
    <configuration>
        <java>
            <includes>
                <include>dotCMS/src/**/*.java</include>
            </includes>
            <googleJavaFormat>
                <version>1.17.0</version>
                <style>AOSP</style>  <!-- 4-space indent -->
            </googleJavaFormat>
            <importOrder>
                <order>java,javax,org,com,com.dotcms,com.dotmarketing</order>
            </importOrder>
            <removeUnusedImports/>
        </java>
    </configuration>
</plugin>
```

**Commands**:
```bash
# Check formatting
./mvnw spotless:check

# Auto-fix formatting
./mvnw spotless:apply

# Add to pre-commit hook
./mvnw spotless:apply -Dspotless.ratchet=true
```

**Benefits**:
- ✅ Auto-formats Java code
- ✅ Enforces Google Java Format style
- ✅ Import ordering
- ✅ Removes unused imports
- ✅ Fast incremental checking

**Drawbacks**:
- May require initial large commit to format existing code
- Breaks git blame (can be mitigated with `.git-blame-ignore-revs`)

#### Option B: Activate Checkstyle

**Why**: More comprehensive rule checking

**Implementation**:
```xml
<!-- Create checkstyle.xml configuration -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <executions>
        <execution>
            <phase>validate</phase>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <configLocation>checkstyle.xml</configLocation>
        <violationSeverity>warning</violationSeverity>
    </configuration>
</plugin>
```

**Benefits**:
- ✅ Comprehensive rule checking
- ✅ Configurable severity levels
- ✅ Can start with warnings, upgrade to errors

**Drawbacks**:
- ❌ No auto-fix capability
- ❌ Requires manual fixes
- ❌ Configuration overhead

#### Option C: Add Error Prone

**Why**: Compile-time bug detection

**Implementation**:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <compilerArgs>
            <arg>-Xlint:unchecked</arg>
            <arg>-XDcompilePolicy=simple</arg>
            <arg>-Xplugin:ErrorProne</arg>
        </compilerArgs>
        <annotationProcessorPaths>
            <path>
                <groupId>com.google.errorprone</groupId>
                <artifactId>error_prone_core</artifactId>
                <version>2.23.0</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

**Benefits**:
- ✅ Detects common Java bugs at compile-time
- ✅ No additional build time cost
- ✅ Auto-fix available for some patterns

### Priority 2: Integrate Backend Linting with Pre-Commit Hook

**Add to `core-web/.husky/pre-commit`**:

```bash
# Java linting - check if any Java files are staged
java_files_staged=$(git diff --cached --name-only | grep '\.java$' || true)
if [ -n "$java_files_staged" ]; then
    print_color "$BLUE" "☕ Running Java linting on staged files..."

    cd "${root_dir}" || exit 1

    # Auto-fix with Spotless
    if ./mvnw spotless:apply -pl :dotcms-core -Dspotless.ratchet=true -q; then
        print_color "$GREEN" "✅ Java code formatted with Spotless"

        # Re-stage the formatted files
        echo "$java_files_staged" | xargs git add
    else
        print_color "$RED" "❌ Spotless formatting failed"
        has_errors=true
    fi

    cd "${original_pwd}" || exit 1
fi
```

### Priority 3: Add Backend Linting to CI/CD

**Add to `.github/workflows/cicd_comp_build-phase.yml`**:

```yaml
- name: Run Spotless Check
  run: |
    ./mvnw spotless:check -pl :dotcms-core

- name: Run Checkstyle (if enabled)
  run: |
    ./mvnw checkstyle:check -pl :dotcms-core
```

### Priority 4: Documentation & Onboarding

**Create `docs/backend/JAVA_LINTING.md`**:
- Tool explanations
- How to run locally
- How to fix violations
- IDE integration (IntelliJ, VSCode)

### Priority 5: IDE Integration

**IntelliJ IDEA**:
- Configure Google Java Format plugin
- Import Checkstyle rules
- Enable Error Prone annotations

**VS Code**:
- Install Java formatting extensions
- Configure workspace settings

---

## 7. Migration Strategy

### Phase 1: Enable with Warnings (Week 1)

```xml
<spotless.skip>false</spotless.skip>
<spotless.ratchet>true</spotless.ratchet>  <!-- Only new/changed code -->
```

**Goal**: Run Spotless but don't fail builds, collect metrics

### Phase 2: Auto-Fix Existing Code (Week 2)

```bash
# Format all Java code
./mvnw spotless:apply

# Create git-blame-ignore-revs
echo "$(git rev-parse HEAD) # Spotless auto-format" >> .git-blame-ignore-revs
```

**Goal**: One-time formatting commit, update git blame ignore

### Phase 3: Enforce on New Code (Week 3)

```xml
<spotless.ratchet>false</spotless.ratchet>
```

**Goal**: Fail builds on formatting violations

### Phase 4: Add to Pre-Commit Hook (Week 4)

**Goal**: Auto-fix before commit, reduce CI failures

---

## 8. Comparison Matrix

| Feature | Frontend (Nx/ESLint) | Backend (Java) | Workflows (YAML) |
|---------|---------------------|----------------|------------------|
| **Auto-Format** | ✅ Prettier | ❌ Not enabled | ✅ Prettier |
| **Style Validation** | ✅ ESLint | ❌ Not active | ✅ yamllint |
| **Semantic Validation** | ✅ TypeScript | ✅ Compiler | ✅ actionlint |
| **Import Ordering** | ✅ ESLint | ❌ Not enforced | N/A |
| **Pre-Commit Hook** | ✅ Active | ❌ Not included | ✅ Active |
| **CI/CD Validation** | ✅ Active | ⚠️ Compile only | ✅ Active |
| **IDE Integration** | ✅ Strong | ⚠️ Basic | ✅ Strong |
| **Auto-Fix** | ✅ Yes | ❌ No | ✅ Yes |
| **Caching** | ✅ Nx cache | ⚠️ Maven cache | N/A |
| **Performance** | ✅ Affected only | ⚠️ Full build | ✅ Affected only |

---

## 9. Summary & Next Steps

### Current State

✅ **Strong**:
- Frontend linting (ESLint + Prettier + Nx)
- Workflow linting (yamllint + actionlint + Prettier)
- Pre-commit hook integration
- CI/CD validation

❌ **Weak**:
- Backend Java linting (no active tools)
- No Java auto-formatting
- No Java style enforcement
- No pre-commit hook for Java

### Recommended Actions

**Immediate** (This Sprint):
1. Enable Spotless for Java with ratcheting
2. Run spotless:apply on existing codebase
3. Add backend linting to CI/CD

**Short-Term** (Next Sprint):
4. Add Java linting to pre-commit hook
5. Document Java linting process
6. Configure IDE integration

**Long-Term** (Next Quarter):
7. Consider adding Checkstyle for comprehensive rules
8. Add Error Prone for bug detection
9. Create `.git-blame-ignore-revs` for formatting commits

### Conclusion

The DotCMS project has **excellent frontend and workflow linting** but lacks **backend Java linting**. The recommended approach is to:

1. **Enable Spotless** - Already configured, just needs activation
2. **Auto-format existing code** - One-time commit
3. **Integrate with pre-commit hook** - Auto-fix before commit
4. **Enforce in CI/CD** - Block PRs with violations

This will bring backend linting to the same high standard as frontend linting.