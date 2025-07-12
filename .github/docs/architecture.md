# Pipeline Architecture

## Overview

The pipeline implements a sophisticated **directed acyclic graph (DAG)** of dependencies designed to provide fast feedback to developers while maintaining comprehensive quality gates and balanced risk management. The system is built around the principle of **DRY (Don't Repeat Yourself)** workflows and modular, reusable components.

## Workflow Interdependency Model

```
PR Created → cicd_1-pr.yml → Merge Queue → cicd_2-merge-queue.yml → 
Merge to Main → cicd_3-trunk.yml → cicd_4-nightly.yml
                    ↓
All workflows trigger → cicd_post-workflow-reporting.yml

**On-Demand LTS Releases**: cicd_5-lts.yml (manual trigger only)
```

## Workflow Naming Convention

**Systematic Naming Pattern:**
- `cicd_1-pr.yml` through `cicd_4-nightly.yml` - Main pipeline workflows (automatic progression)
- `cicd_5-lts.yml` - On-demand LTS workflow (manual trigger only)
- `cicd_comp_*.yml` - Reusable component workflows
- `cicd_manual_*.yml` - Manual trigger workflows
- `cicd_scheduled_*.yml` - Cron-based workflows
- `issue_*.yml` - Issue and project management workflows
- `legacy-*.yml` - Deprecated workflows (DO NOT USE for new features)
- `utility_*.yml` - General utility workflows

## Main Entry Points (Ordered by Execution Flow)

The main workflows follow a numbered naming convention to show the natural progression of code through the CI/CD pipeline:

### 1. [`cicd_1-pr.yml`](../workflows/cicd_1-pr.yml) - Pull Request Workflow
- **Trigger**: Pull requests to main/master branches
- **Purpose**: Comprehensive PR validation and quality gates
- **Key Features**:
  - Concurrency management to cancel in-progress runs
  - Security-conscious (no secrets in PR context)
  - Conditional execution based on file changes
  - Comprehensive testing (unit, integration, E2E)
  - Semgrep analysis (replaced SonarQube)
  - PR status notifications

### 2. [`cicd_2-merge-queue.yml`](../workflows/cicd_2-merge-queue.yml) - Merge Queue Workflow
- **Trigger**: Merge group checks requested
- **Purpose**: Final validation before merge to main
- **Key Features**:
  - Runs all tests (currently comprehensive for flaky test detection)
  - Artifact generation for trunk reuse
  - Same test suite as PR but different context

### 3. [`cicd_3-trunk.yml`](../workflows/cicd_3-trunk.yml) - Trunk Workflow
- **Trigger**: Push to main branch or manual dispatch
- **Purpose**: Post-merge processing and deployment preparation
- **Key Features**:
  - Artifact reuse from merge queue build
  - CLI native binary building
  - Deployment to trunk environment
  - SDK library publishing
  - Comprehensive reporting

### 4. [`cicd_4-nightly.yml`](../workflows/cicd_4-nightly.yml) - Nightly Workflow
- **Trigger**: Scheduled cron job
- **Purpose**: Extended testing and nightly deployments
- **Key Features**:
  - Runs on latest main at scheduled time
  - Deploys to separate nightly environment
  - Extended test suites

### 5. [`cicd_5-lts.yml`](../workflows/cicd_5-lts.yml) - LTS Workflow
- **Trigger**: **Manual dispatch only** for LTS releases
- **Purpose**: Long-term support release processing (on-demand)
- **Key Features**:
  - Manual trigger for LTS release preparation
  - Comprehensive testing and validation
  - Special release artifact generation
  - Not part of the automatic CI/CD progression

## Reusable Workflow Components (`cicd_comp_*.yml`)

### Core Phase Components
- **[`cicd_comp_initialize-phase.yml`](../workflows/cicd_comp_initialize-phase.yml)**: Determines what needs to be built/tested based on changes
- **[`cicd_comp_build-phase.yml`](../workflows/cicd_comp_build-phase.yml)**: Handles Maven builds and artifact generation
- **[`cicd_comp_test-phase.yml`](../workflows/cicd_comp_test-phase.yml)**: Orchestrates various test suites (JVM, CLI, frontend, integration, E2E)
- **[`cicd_comp_sonarqube-phase.yml`](../workflows/cicd_comp_sonarqube-phase.yml)**: SonarQube analysis (**DEPRECATED** - disabled, should be removed)
- **[`cicd_comp_semgrep-phase.yml`](../workflows/cicd_comp_semgrep-phase.yml)**: Security and code quality analysis with Semgrep (**ACTIVE** - replaced SonarQube)

### Code Quality and Security Analysis Transition

**⚠️ Important Change**: We have transitioned from SonarQube to Semgrep for code quality and security analysis:

- **SonarQube**: Currently disabled but workflow file still exists (scheduled for removal)
- **Semgrep**: Active replacement providing both security and code quality analysis
- **Migration Status**: Complete - all workflows now use Semgrep instead of SonarQube

### Additional Components
- **[`cicd_comp_cli-native-build-phase.yml`](../workflows/cicd_comp_cli-native-build-phase.yml)**: Native binary compilation for CLI
- **[`cicd_comp_deployment-phase.yml`](../workflows/cicd_comp_deployment-phase.yml)**: Handles deployments to various environments
- **[`cicd_comp_finalize-phase.yml`](../workflows/cicd_comp_finalize-phase.yml)**: Aggregates results and determines overall status

### Utility Components
- **[`cicd_comp_pr-notifier.yml`](../workflows/cicd_comp_pr-notifier.yml)**: PR status notifications
- **[`cicd_post-workflow-reporting.yml`](../workflows/cicd_post-workflow-reporting.yml)**: Post-workflow reporting and notifications

## Change Detection System

### Path Filter Configuration ([`.github/filters.yaml`](../filters.yaml))

The system implements **hierarchical change detection** with YAML anchors for DRY configuration:

```yaml
full_build_test: &full_build_test
  - '.sdkmanrc'
  - 'core-web/.nvmrc'
  - '.nvmrc'

backend: &backend
  - '.github/workflows/cicd_comp_*.yml'
  - '.github/actions/core-cicd/**/action.yml'
  - 'dotCMS/!(src/main/webapp/html/)**'
  - *full_build_test

cli: &cli
  - 'tools/dotcms-cli/**'
  - *full_build_test
  - *backend  # CLI depends on backend changes
```

### Component Mapping

- **`backend`**: Java backend changes (dotCMS core, APIs, plugins, workflow configs)
- **`frontend`**: Angular frontend changes (core-web directory, CSS/JS assets)
- **`cli`**: CLI tool changes (includes backend dependencies)
- **`sdk_libs`**: SDK library changes (core-web/libs/sdk)
- **`build`**: Aggregated changes requiring full build
- **`jvm_unit_test`**: Changes requiring JVM unit tests

### Intelligent Build Logic

The initialization phase (`cicd_comp_initialize-phase.yml`) implements **multi-level decision trees**:

1. **Change Detection**: Analyzes modified files against path filters
2. **Dependency Resolution**: Determines component interdependencies
3. **Test Selection**: Chooses appropriate test suites based on changes
4. **Artifact Reuse**: Evaluates whether previous build artifacts can be reused
5. **Phase Skipping**: Intelligently skips unnecessary phases

### Validation Levels

- **`none`**: No change detection (runs all components)
- **`full`**: Complete change detection with all filters
- **`custom`**: Selective validation of specific modules

## Artifact Management

### Artifact Reuse Strategy

The system implements sophisticated artifact reuse to minimize build times:

1. **Merge Queue Artifacts**: Generated during merge queue validation
2. **Trunk Reuse**: Trunk workflow can reuse merge queue artifacts (same commit hash)
3. **Conditional Building**: Only builds if artifacts don't exist or are explicitly requested

### Artifact Types

- **Maven Repository**: Compiled Java artifacts
- **Docker Images**: Application containers
- **CLI Binaries**: Native compiled executables
- **Test Results**: Test reports and coverage data
- **Security Reports**: Semgrep analysis (SonarQube deprecated)

## Actions Structure

### Core CI/CD Actions (`.github/actions/core-cicd/`)

- **`api-limits-check/`**: GitHub API rate limit monitoring
- **`cleanup-runner/`**: Runner cleanup for disk space
- **`prepare-runner/`**: Runner preparation and setup
- **`setup-java/`**: Java and GraalVM installation
- **`maven-job/`**: Standardized Maven job execution
- **`deployment/`**: Various deployment actions (Docker, JFrog, NPM, etc.)

### Issue Management Actions (`.github/actions/issues/`)

- **`issue-fetcher/`**: GitHub issue data retrieval
- **`issue-labeler/`**: Automated issue labeling

### Legacy Actions (`.github/actions/legacy-release/`)

- **`changelog-report/`**: Release changelog generation
- **`rc-changelog/`**: Release candidate changelog
- **`sbom-generator/`**: Software Bill of Materials generation

## Core Setup Actions

The core setup actions provide the foundation for consistent build environments across all workflows. These actions are designed to be modular and reusable, ensuring that all builds use the same Java version and configuration.

### prepare-runner ([`.github/actions/core-cicd/prepare-runner`](../actions/core-cicd/prepare-runner/))

**Purpose**: Prepares the GitHub Actions runner with consistent environment setup

**Key Features**:
- **Optional Runner Cleanup**: Frees up disk space when needed via `cleanup-runner` input
- **Java Environment Setup**: Configures Java/GraalVM through the `setup-java` action
- **Branch Management**: Ensures main branch is available locally when required
- **Flexible Configuration**: Supports various combinations of Java, GraalVM, and cleanup options

**Usage Pattern**:
```yaml
- uses: ./.github/actions/core-cicd/prepare-runner
  with:
    cleanup-runner: true
    require-java: true
    require-graalvm: false
    require-main: true
```

### setup-java ([`.github/actions/core-cicd/setup-java`](../actions/core-cicd/setup-java/))

**Purpose**: Provides standardized Java environment setup using SDKMan with version control via `.sdkmanrc`

**Critical Version Control**:
- **Single Source of Truth**: Java version is controlled by `.sdkmanrc`
- **Current Version**: `java=21.0.7-ms` (Microsoft build of OpenJDK 21)
- **Automatic Resolution**: If no version is specified via inputs, automatically reads from `.sdkmanrc`
- **Consistency Guarantee**: Ensures all builds use the exact same Java version

**Key Features**:
- **SDKMan Integration**: Uses SDKMan for Java installation and management
- **Multi-Architecture Support**: Handles different CPU architectures (x86_64, arm64)
- **Intelligent Caching**: Caches both SDKMan installation and Java SDKs by architecture and version
- **GraalVM Support**: Optional GraalVM installation for native image builds
- **Environment Variables**: Sets `JAVA_HOME`, `GRAALVM_HOME`, and updates `PATH`

**Version Resolution Logic**:
1. **Input Override**: If `java-version` input is provided, use that version
2. **Default from .sdkmanrc**: If no input, read version from `.sdkmanrc` file
3. **Fallback**: Uses hardcoded default if neither is available

**Cache Strategy**:
- **SDKMan Installation**: Cached by OS and architecture
- **Java SDKs**: Cached by OS, architecture, and specific version
- **GraalVM**: Separate cache for GraalVM installations

### maven-job ([`.github/actions/core-cicd/maven-job`](../actions/core-cicd/maven-job/))

**Purpose**: Comprehensive Maven build orchestration with extensive configuration options

**Key Features**:
- **Runner Preparation**: Automatically calls `prepare-runner` with appropriate settings
- **Multi-Level Caching**: Implements sophisticated caching strategy for optimal performance
- **Artifact Management**: Handles artifact restoration and generation
- **Docker Integration**: Supports Docker image building and management
- **Test Report Generation**: Produces comprehensive test reports and coverage data
- **Flexible Configuration**: Supports various Maven profiles and build configurations

## Workflow Categories

### CI/CD Workflows (`cicd_*.yml`)
- **Main Pipeline**: `cicd_1-pr.yml` through `cicd_4-nightly.yml` (automatic progression)
- **On-Demand**: `cicd_5-lts.yml` (manual LTS releases)
- **Components**: `cicd_comp_*.yml` (reusable workflows)
- **Manual**: `cicd_manual_*.yml` (manual trigger workflows)
- **Scheduled**: `cicd_scheduled_*.yml` (cron-based workflows)

### Issue Management (`issue_*.yml`)
- **Component**: `issue_comp_*.yml` (reusable issue workflows)
- **Manual**: `issue_manual_*.yml` (manual issue management)
- **Event-driven**: `issue_on-change_*.yml` (triggered by issue events)
- **Scheduled**: `issue_scheduled_*.yml` (cron-based issue workflows)

### Legacy Workflows (`legacy-*.yml`)
- **Release**: `legacy-release_*.yml` (old release processes)
- **Component**: `legacy-release_comp_*.yml` (legacy reusable workflows)

### Utility Workflows
- **Security**: `security_*.yml` (security-related workflows)
- **Utilities**: `utility_*.yml` (general utility workflows)
- **Documentation**: `publish_docs.yml` (documentation publishing)

## Environment Configuration

### Variables and Secrets

The workflows use GitHub repository variables and secrets for configuration:

- **`UBUNTU_RUNNER_VERSION`**: Ubuntu runner version (default: 24.04)
- **`CICD_SKIP_TESTS`**: Global test skipping flag
- **`DISABLE_SONAR`**: Disable SonarQube analysis (deprecated - SonarQube no longer used)
- **`DISABLE_SEMGREP`**: Disable Semgrep analysis

### Environment-Specific Settings

- **trunk**: Main development environment
- **nightly**: Nightly build environment
- **lts**: Long-term support environment

## Legacy Workflows (`legacy-*.yml`)

These workflows represent the previous CI/CD structure and are **NOT** representative of current best practices. They are marked with `legacy-` prefix and should not be used as reference for new implementations. These will be modularized in future iterations.

## Performance Optimization

### Fail-Fast Strategy

**Problem**: Developers wait 10+ minutes to learn about simple syntax errors
**Solution**: Run critical checks first, fail immediately on problems

**Implementation**:
```yaml
jobs:
  validate:
    name: Quick Validation
    steps:
      - name: Check syntax
      - name: Run linting
      - name: Check dependencies
  
  test:
    needs: validate  # Only run if validation passes
    name: Full Test Suite
```

**Benefits**:
- **Faster Feedback**: Developers know about syntax errors in 30 seconds, not 10 minutes
- **Resource Efficiency**: Don't waste time on full builds if basic checks fail
- **Better Developer Experience**: Quick fixes for common issues

### Caching Strategies

**Multi-Level Caching**:
1. **SDKMan Cache**: Java SDK installations cached by architecture and version
2. **Maven Cache**: Dependencies cached by pom.xml hash
3. **Node Cache**: Node modules cached by package-lock.json hash
4. **Docker Cache**: Docker layers cached for faster image builds

### Artifact Reuse

**Sophisticated Artifact Reuse**:
- Merge queue artifacts are reused by trunk workflow
- Conditional building only occurs when artifacts don't exist
- Intelligent artifact-run-id resolution

## Core Design Principles

### 1. **Modular Architecture**
- **Reusable Components**: Every workflow phase is a reusable component
- **DRY Principle**: No duplication of build logic across workflows
- **Composable Workflows**: Main workflows compose reusable components

### 2. **Security-First Design**
- **Zero-Trust PR Model**: PR workflows have no access to secrets
- **Principle of Least Privilege**: Minimal permissions for each workflow
- **Input Validation**: All user inputs are validated and sanitized

### 3. **Developer Experience**
- **Fast Feedback**: Quick validation before expensive operations
- **Clear Documentation**: Comprehensive guides for common tasks
- **Intuitive Structure**: Logical naming and organization

### 4. **Performance Optimization**
- **Intelligent Change Detection**: Only run necessary tests
- **Sophisticated Caching**: Multi-level caching for optimal performance
- **Parallel Execution**: Jobs run in parallel where possible

### 5. **Reliability and Resilience**
- **Error Handling**: Comprehensive error handling and status aggregation
- **Graceful Degradation**: Workflows continue even if non-critical steps fail
- **Retry Logic**: Automatic retry for transient failures

## Future Architectural Improvements

### Build Once, Deploy Anywhere

**Current State**: Multiple rebuilds of the same code for different environments
**Future Vision**: Build artifacts once, deploy to multiple environments

**Implementation Strategy**:
1. **Artifact Reuse Architecture**: Environment-agnostic build artifacts
2. **Version/Environment Separation**: Separate version metadata from build artifacts
3. **Environment-Specific Deployment**: Deploy same artifacts to different environments

**Benefits**:
- **Reduced Build Times**: Eliminate redundant compilation and testing
- **Consistency**: Same artifacts across all environments
- **Resource Efficiency**: Less CI/CD resource consumption
- **Faster Releases**: Quicker deployment cycles
- **Reduced Risk**: Same tested artifacts in all environments