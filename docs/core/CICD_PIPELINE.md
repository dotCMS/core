# CI/CD Pipeline Implementation

## Overview

This document provides technical implementation details for the dotCMS CI/CD pipeline built on GitHub Actions. It covers pipeline architecture, security implementation, troubleshooting, and development patterns.

## Pipeline Architecture

### Workflow Interdependency Model
```
PR Created → cicd_1-pr.yml → Merge Queue → cicd_2-merge-queue.yml → 
Merge to Main → cicd_3-trunk.yml → cicd_4-nightly.yml
                    ↓
All workflows → cicd_post-workflow-reporting.yml

Manual Trigger: cicd_5-lts.yml (LTS releases)
```

### File Structure
```
.github/
├── workflows/
│   ├── cicd_1-pr.yml           # PR validation
│   ├── cicd_2-merge-queue.yml  # Merge queue validation
│   ├── cicd_3-trunk.yml        # Post-merge processing
│   ├── cicd_4-nightly.yml      # Nightly builds
│   ├── cicd_5-lts.yml          # LTS releases (manual)
│   ├── cicd_comp_*.yml         # Reusable components
│   ├── cicd_post-workflow-reporting.yml  # Status aggregation
│   └── legacy-*.yml            # Legacy workflows (deprecated)
├── actions/
│   └── core-cicd/
│       ├── prepare-runner/     # Runner environment setup
│       ├── setup-java/         # Java installation
│       └── maven-job/          # Maven build orchestration
├── docs/                       # Pipeline documentation
└── filters.yaml                # Change detection patterns
```

## Workflow Components

### Main Pipeline Workflows

#### 1. PR Workflow ([`cicd_1-pr.yml`](.github/workflows/cicd_1-pr.yml))
**Purpose**: Comprehensive PR validation with zero-trust security

**Key Features**:
- Conditional execution based on file changes
- No secrets in PR context (security isolation)
- Workflow linting with yamllint and actionlint
- Calls reusable components for build, test, and security analysis

**Example trigger and security**:
```yaml
on:
  pull_request:
    branches: [main, master]

permissions:
  contents: read
  checks: write
  pull-requests: write
```

#### 2. Merge Queue Workflow ([`cicd_2-merge-queue.yml`](.github/workflows/cicd_2-merge-queue.yml))
**Purpose**: Final validation before merge to main branch

**Key Features**:
- Comprehensive test suite for stability validation
- Artifact generation for downstream reuse
- Runs all tests (may be optimized in future)

#### 3. Trunk Workflow ([`cicd_3-trunk.yml`](.github/workflows/cicd_3-trunk.yml))
**Purpose**: Post-merge processing and deployment preparation

**Key Features**:
- Artifact reuse from merge queue build
- CLI native binary building
- Deployment to trunk environment
- SDK library publishing

### Reusable Components

#### Initialize Phase ([`cicd_comp_initialize-phase.yml`](.github/workflows/cicd_comp_initialize-phase.yml))
**Purpose**: Change detection and build planning

**Key Features**:
- Uses `dorny/paths-filter@v3` with `.github/filters.yaml`
- Outputs boolean flags for backend, frontend, build, test
- Determines what needs to be built/tested based on file changes

#### Build Phase ([`cicd_comp_build-phase.yml`](.github/workflows/cicd_comp_build-phase.yml))
**Purpose**: Maven builds and artifact generation

**Key Features**:
- Uses custom `.github/actions/core-cicd/maven-job` action
- Conditional artifact generation based on inputs
- Handles both development and production builds

#### Test Phase ([`cicd_comp_test-phase.yml`](.github/workflows/cicd_comp_test-phase.yml))
**Purpose**: Orchestrates various test suites

**Key Features**:
- JVM unit tests with Java 21
- Integration tests with `-Pcoreit` profile
- Frontend tests using Nx and Yarn
- E2E tests (conditional)
- Matrix strategy for parallel execution

## Security Implementation

### Zero-Trust PR Model
```yaml
# PR workflows have NO access to secrets
on:
  pull_request:
    branches: [main, master]
    
# No secrets block in PR workflows
# Sensitive operations moved to post-workflow reporting
```

### Permission Management
```yaml
# Explicit minimal permissions
permissions:
  contents: read        # Required for checkout
  checks: write         # Required for test results
  pull-requests: write  # Required for PR comments
  # No other permissions unless explicitly needed
```

### Input Validation
```yaml
# User input validation pattern
env:
  USER_INPUT: ${{ github.event.pull_request.title }}
run: |
  # Validate input format
  if [[ "$USER_INPUT" =~ ^[a-zA-Z0-9\ \-\_\.]+$ ]]; then
    echo "Processing: $USER_INPUT"
  else
    echo "Invalid input format"
    exit 1
  fi
```

### Action Security
```yaml
# Pin actions to specific versions
- uses: actions/checkout@v4              # ✅ Pinned version
- uses: actions/setup-java@v4            # ✅ Pinned version
- uses: dorny/paths-filter@v3            # ✅ Pinned version

# Never use master/main references
- uses: some-action@master               # ❌ Security risk
```

## Workflow Linting

### Overview
All GitHub Actions workflows are automatically validated on every PR to prevent syntax errors and workflow issues from being merged into the main branch.

### Linting Tools

#### yamllint
Validates YAML syntax and formatting:
- Line length (max 120 characters)
- Indentation (2 spaces)
- Trailing whitespace
- Document structure

Configuration file: `.yamllint`

#### actionlint
Validates GitHub Actions-specific syntax:
- Workflow syntax errors
- Undefined variables and contexts
- Invalid action references
- Deprecated features
- Type mismatches in expressions

### Running Linting Locally

#### Using mise (Recommended)
```bash
# Install tools automatically via mise
mise install

# Verify installation
yamllint --version
actionlint --version

# Run yamllint
yamllint .github/workflows/

# Run actionlint
actionlint
```

#### Manual Installation
```bash
# Install yamllint via pip
pip install yamllint

# Install actionlint (Linux)
curl -sSL https://github.com/rhysd/actionlint/releases/download/v1.7.8/actionlint_1.7.8_linux_amd64.tar.gz | \
  sudo tar xz -C /usr/local/bin

# Install actionlint (macOS)
brew install actionlint

# Run yamllint
yamllint .github/workflows/

# Run actionlint
actionlint
```

### Git Pre-Commit Hook Integration

Workflow linting runs automatically via the Husky pre-commit hook when `.github/workflows/` or `.github/actions/` files are staged:

```bash
# The pre-commit hook will automatically:
# 1. Detect staged workflow files
# 2. Run yamllint on changed files
# 3. Run actionlint on all workflows
# 4. Block commit if issues are found

# To bypass pre-commit checks (not recommended):
git commit --no-verify
```

**Setup Requirements:**
1. Run `mise install` to ensure linting tools are available
2. Pre-commit hook is automatically configured by Husky
3. Tools are installed in `.mise.toml` configuration

### CI Integration
The `workflow-lint` job runs automatically in PRs when workflow files are modified:
- Only triggered when `.github/workflows/**`, `.github/actions/**`, or `.yamllint` files change
- Runs in parallel with other PR checks
- Fails the PR if syntax errors are found
- Takes ~30-60 seconds to complete

### Common Linting Issues

**yamllint errors:**
```yaml
# ❌ Wrong: line too long
- name: This is a very long step name that exceeds the maximum line length and should be split

# ✅ Correct: split long lines
- name: >
    This is a very long step name that has been properly
    split across multiple lines

# ❌ Wrong: inconsistent indentation
jobs:
  build:
      runs-on: ubuntu-latest

# ✅ Correct: consistent 2-space indentation
jobs:
  build:
    runs-on: ubuntu-latest
```

**actionlint errors:**
```yaml
# ❌ Wrong: undefined output reference
needs: initialize
if: needs.initialize.outputs.nonexistent_output == 'true'

# ✅ Correct: defined output reference
needs: initialize
if: needs.initialize.outputs.build == 'true'

# ❌ Wrong: action version not pinned
- uses: actions/checkout@main

# ✅ Correct: action version pinned
- uses: actions/checkout@v4
```

## Change Detection System

### Filter Configuration (`.github/filters.yaml`)
```yaml
# Backend changes
backend: &backend
  - 'dotCMS/src/main/java/**'
  - 'src/main/java/**'
  - 'pom.xml'
  - 'bom/**/pom.xml'
  - '**/pom.xml'

# Frontend changes
frontend: &frontend
  - 'core-web/**'
  - 'package.json'
  - 'yarn.lock'
  - '.nvmrc'

# CLI changes
cli: &cli
  - 'tools/dotcms-cli/**'
  - 'cli/**'

# Test-specific changes
test: &test
  - 'dotcms-integration/**'
  - 'src/test/java/**'
  - '**/src/test/**'

# Full build triggers
full_build_test: &full_build_test
  - '.github/workflows/**'
  - '.github/actions/**'
  - 'docker/**'
  - 'Dockerfile'
  - '.sdkmanrc'

# Combine filters for complex logic
backend_with_full: &backend_with_full
  - *backend
  - *full_build_test

frontend_with_full: &frontend_with_full
  - *frontend
  - *full_build_test
```

### Change Detection Usage
```yaml
jobs:
  detect-changes:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: .github/filters.yaml
          
  backend-tests:
    needs: detect-changes
    if: needs.detect-changes.outputs.backend == 'true'
    runs-on: ubuntu-latest
    steps:
      - name: Run backend tests
        run: ./mvnw test
        
  frontend-tests:
    needs: detect-changes
    if: needs.detect-changes.outputs.frontend == 'true'
    runs-on: ubuntu-latest
    steps:
      - name: Run frontend tests
        run: cd core-web && nx run dotcms-ui:test
```

## Artifact Management

### Artifact Generation
```yaml
# Generate artifacts for reuse
- name: Upload Build Artifacts
  uses: actions/upload-artifact@v4
  with:
    name: maven-artifacts-${{ github.run_id }}
    path: |
      target/
      dotCMS/target/
    retention-days: 7
```

### Artifact Consumption
```yaml
# Download artifacts from previous workflow
- name: Download Build Artifacts
  uses: actions/download-artifact@v4
  with:
    name: maven-artifacts-${{ needs.initialize.outputs.artifact-run-id }}
    path: ./
```

### Artifact Strategy
- **Maven Dependencies**: Cached between runs
- **Build Outputs**: Uploaded as artifacts for reuse
- **Test Results**: Stored for reporting
- **Docker Images**: Published to registry

## Caching Strategy

### Maven Cache
```yaml
- name: Cache Maven dependencies
  uses: actions/cache@v4
  with:
    path: ~/.m2/repository
    key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
    restore-keys: |
      ${{ runner.os }}-maven-
```

### Node Cache
```yaml
- name: Setup Node.js
  uses: actions/setup-node@v4
  with:
    node-version-file: 'core-web/.nvmrc'
    cache: 'yarn'
    cache-dependency-path: 'core-web/yarn.lock'
```

### Docker Cache
```yaml
- name: Build Docker image
  uses: docker/build-push-action@v5
  with:
    context: .
    push: true
    tags: dotcms/dotcms:latest
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

## Monitoring and Reporting

### Status Aggregation
```yaml
# Post-workflow reporting
name: Post-Workflow Reporting
on:
  workflow_run:
    workflows: [
      "PR Validation",
      "Merge Queue Validation",
      "Trunk Processing"
    ]
    types: [completed]
    
jobs:
  aggregate-status:
    runs-on: ubuntu-latest
    steps:
      - name: Aggregate Results
        run: |
          echo "Workflow: ${{ github.event.workflow_run.name }}"
          echo "Status: ${{ github.event.workflow_run.conclusion }}"
          echo "URL: ${{ github.event.workflow_run.html_url }}"
```

### Slack Integration
```yaml
# Slack notifications with secrets
- name: Slack Notification
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    webhook_url: ${{ secrets.SLACK_WEBHOOK_URL }}
    channel: '#guild-dev-pipeline'
```

## Troubleshooting Guide

### Common Issues

#### 1. Workflow Not Triggering
**Diagnosis:**
```bash
# Check workflow syntax
yamllint .github/workflows/your-workflow.yml

# Verify trigger conditions
git log --oneline -5  # Check recent commits
```

**Solution:**
- Verify file is in `.github/workflows/`
- Check trigger conditions match event
- Ensure no syntax errors in YAML

#### 2. Tests Being Skipped
**Diagnosis:**
```yaml
# Add debug output
- name: Debug Change Detection
  run: |
    echo "Backend: ${{ needs.initialize.outputs.backend }}"
    echo "Frontend: ${{ needs.initialize.outputs.frontend }}"
    echo "Changed files: ${{ steps.filter.outputs.changes }}"
```

**Solution:**
- Update `.github/filters.yaml` to include missing paths
- Check conditional logic in job definitions
- Verify change detection is working correctly

#### 3. Artifact Issues
**Diagnosis:**
```yaml
# Debug artifact information
- name: Debug Artifacts
  run: |
    echo "Artifact run ID: ${{ needs.initialize.outputs.artifact-run-id }}"
    echo "Expected artifact: maven-artifacts-${{ github.run_id }}"
```

**Solution:**
- Verify artifact names match exactly
- Check artifact retention period
- Ensure artifacts are generated before consumption

#### 4. Permission Errors
**Diagnosis:**
```yaml
# Check permissions
permissions:
  contents: read
  packages: write  # Add if needed
  deployments: write  # Add if needed
```

**Solution:**
- Add required permissions to workflow
- Use secrets for sensitive operations
- Move privileged operations to post-workflow reporting

### Performance Optimization

#### Parallel Execution
```yaml
# Run jobs in parallel
jobs:
  backend-tests:
    runs-on: ubuntu-latest
    # Runs in parallel with frontend-tests
    
  frontend-tests:
    runs-on: ubuntu-latest
    # Runs in parallel with backend-tests
```

#### Matrix Strategy
```yaml
# Test multiple configurations
strategy:
  matrix:
    java: [11, 17, 21]
    os: [ubuntu-latest, windows-latest]
  fail-fast: false
```

#### Conditional Execution
```yaml
# Skip unnecessary work
build:
  needs: initialize
  if: needs.initialize.outputs.build == 'true'
  runs-on: ubuntu-latest
```

## Development Patterns

### Adding New Tests
1. **Update Test Phase**: Modify `cicd_comp_test-phase.yml`
2. **Add Change Detection**: Update `.github/filters.yaml`
3. **Test Locally**: Validate changes work as expected

### Modifying Build Process
1. **Update Build Phase**: Modify `cicd_comp_build-phase.yml`
2. **Update Maven Job**: Modify `.github/actions/core-cicd/maven-job`
3. **Test Changes**: Use core-workflow-test repository

### Security Changes
1. **Never in PR Context**: Use post-workflow reporting
2. **Minimal Permissions**: Add only required permissions
3. **Input Validation**: Validate all external input

## Integration with dotCMS Development

### Java Integration
- **Build System**: Uses [Maven Build System](../backend/MAVEN_BUILD_SYSTEM.md)
- **Code Standards**: Follows [Java Standards](../backend/JAVA_STANDARDS.md)
- **Testing**: Integrates with Maven test lifecycle

### Angular Integration
- **Build System**: Uses Nx workspace
- **Standards**: Follows [Angular Standards](../frontend/ANGULAR_STANDARDS.md)
- **Testing**: Integrates with Spectator testing

### Docker Integration
- **Build Process**: Multi-stage Docker builds
- **Caching**: Layer caching for performance
- **Deployment**: Container deployments to environments

## Best Practices

### ✅ Always Do
- Use reusable workflow components
- Implement proper change detection
- Follow security patterns
- Cache dependencies appropriately
- Monitor workflow performance

### ❌ Never Do
- Put secrets in PR workflows
- Use default permissions
- Hardcode values
- Duplicate workflow logic
- Ignore security warnings

## Support and Maintenance

### Getting Help
- **Primary**: `#guild-dev-pipeline` Slack channel
- **Documentation**: Comprehensive guides in `/docs`
- **GitHub Issues**: Bug reports and feature requests

### Maintenance Tasks
- **Weekly**: Security scan review
- **Monthly**: Performance optimization
- **Quarterly**: Architecture review

## Location Information
- **Main Workflows**: `.github/workflows/cicd_*.yml`
- **Reusable Components**: `.github/workflows/cicd_comp_*.yml`
- **Custom Actions**: `.github/actions/core-cicd/`
- **Change Detection**: `.github/filters.yaml`
- **Documentation**: `docs/core/CICD_PIPELINE.md`