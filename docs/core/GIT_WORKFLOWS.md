# Git Workflows and CI/CD Integration

## Overview

This document provides comprehensive guidance on git workflows and CI/CD integration for the dotCMS project. It covers git best practices, GitHub Actions CI/CD pipeline, and development workflow patterns.

## Git Workflow Strategy

### Branch Strategy
- **Main Branch**: `main` - Production-ready code
- **Feature Branches**: `issue-{issue_number}-{description}` - New features and bug fixes linked to GitHub issues
- **Release Branches**: `release/x.y.z` - LTS release preparation
- **Hotfix Branches**: `hotfix/description` - Critical production fixes

### Branch Naming Convention (Required)
All feature branches **MUST** follow this pattern:
```
issue-{issue_number}-{short-description}
```

**Examples:**
- `issue-32668-need-to-optimize-and-shrink-claudemd`
- `issue-25620-alert-when-api-keys-about-to-expire`
- `issue-32238-content-types`
- `issue-32333-update-folder-ids`

**Rules:**
- Always start with `issue-` prefix
- Use the GitHub issue number (without #)
- Add descriptive suffix with hyphens (no spaces)
- Keep descriptions concise but meaningful

### Commit and PR Title Strategy

#### Individual Commit Messages
Commit messages can be descriptive and developer-friendly:
```bash
# Individual commits - use whatever is helpful for development
git commit -m "Add user authentication logic"
git commit -m "Fix null pointer exception in login"
git commit -m "Update tests for new auth flow"
git commit -m "Address PR feedback - simplify validation"
```

#### PR Title (Critical - This becomes the final commit)
**PR titles MUST follow conventional commit format** since PRs are squashed:
```bash
# PR title format (becomes the squashed commit message)
feat(auth): add user authentication endpoint
fix(api): resolve null pointer exception in login validation
docs(readme): update installation instructions
test(auth): add comprehensive authentication test coverage
refactor(service): improve user service error handling
chore(deps): update dependency versions
```

**Why this matters:**
- PRs are squashed into single commits on merge
- The PR title becomes the final commit message in main branch
- Individual commit messages are only visible during development
- PR title should summarize the entire change set

### Pull Request Workflow
1. **Create Feature Branch**:
   ```bash
   git checkout -b issue-12345-add-user-authentication
   ```

2. **Regular Commits**:
   ```bash
   git add .
   git commit -m "Add user authentication logic"
   # Individual commits can be informal and descriptive
   ```

3. **Push and Create PR**:
   ```bash
   git push origin issue-12345-add-user-authentication
   # Create PR via GitHub UI
   ```

4. **PR Review Process**:
   - Automated CI/CD validation
   - Code review requirements
   - Security and quality checks
   - Documentation updates

## Pre-Commit Hook System

The dotCMS project uses a sophisticated pre-commit hook system that ensures code quality and consistency before commits are made. The system includes both Maven-based and Husky-based hooks.

### Main Pre-Commit Hook (`.git/hooks/pre-commit`)
```bash
#!/usr/bin/env bash
echo "Running Maven pre-commit hook"
./mvnw generate-resources -Pauto-format
```

This hook:
- Runs Maven resource generation with auto-formatting
- Ensures generated files are up-to-date
- Applies consistent formatting across the codebase

### Husky Pre-Commit Hook (`core-web/.husky/pre-commit`)
The frontend uses a comprehensive Husky-based pre-commit hook that:

#### Environment Setup
- **SDKMAN Integration**: Ensures correct Java version via `.sdkmanrc`
- **Node Version Management**: Uses `.nvmrc` for Node.js version consistency
- **Yarn Installation**: Manages dependencies and lockfile updates

#### Intelligent Build Detection
- **Maven Analysis**: Determines if Maven compile is needed based on staged files
- **OpenAPI Generation**: Automatically regenerates OpenAPI specs when REST endpoints change
- **Conditional Execution**: Skips unnecessary builds for performance

#### Code Quality Enforcement
- **ESLint**: Runs `nx affected -t lint` with automatic fixing
- **Prettier**: Runs `nx format:write` for consistent code formatting
- **Dependency Updates**: Automatically updates `yarn.lock` when `package.json` changes

#### File Management
- **Generated Files**: Automatically stages generated files (OpenAPI specs, compiled assets)
- **Backup System**: Creates backups of untracked files during processing
- **Cleanup**: Removes temporary files and restores workspace state

#### Error Handling
- **ENOBUFS Auto-Fix**: Automatically handles Node.js buffer issues
- **Cache Management**: Resets Nx cache when needed
- **Graceful Degradation**: Continues processing even if non-critical steps fail

### Pre-Commit Hook Workflow

#### For Backend Changes
1. **Java Environment**: Sets up correct Java version via SDKMAN
2. **Maven Compile**: Runs if REST API files are modified
3. **OpenAPI Update**: Regenerates and stages OpenAPI specifications
4. **Resource Generation**: Ensures all generated resources are current

#### For Frontend Changes
1. **Node Environment**: Sets up correct Node.js and Yarn versions
2. **Dependency Check**: Validates and updates `yarn.lock`
3. **Linting**: Runs ESLint with auto-fix on affected files
4. **Formatting**: Applies Prettier formatting to all code
5. **SDK Build**: Builds SDK-UVE components if affected

#### For Mixed Changes
1. **Full Environment Setup**: Configures both Java and Node environments
2. **Maven Processing**: Handles backend compilation and resource generation
3. **Frontend Processing**: Runs full frontend quality checks
4. **File Coordination**: Ensures all generated files are properly staged

### Pre-Commit Hook Configuration

#### Required Environment
- **SDKMAN**: For Java version management
- **Node.js**: Project-specific version in `installs/node`
- **Yarn**: Project-specific version in `installs/node/yarn`

#### Key Files
- `.git/hooks/pre-commit`: Main Maven hook
- `core-web/.husky/pre-commit`: Frontend hook
- `.sdkmanrc`: Java version specification
- `core-web/.nvmrc`: Node.js version specification

### Bypassing Pre-Commit Hooks
In exceptional cases, pre-commit hooks can be bypassed:
```bash
git commit --no-verify -m "commit message"
```

**⚠️ Warning**: Only use `--no-verify` for emergency fixes. All bypassed commits must be fixed in follow-up commits.

### Troubleshooting Pre-Commit Issues

#### Common Issues
1. **SDKMAN Not Found**: Install SDKMAN from https://github.com/dotCMS/dotcms-utilities
2. **Node Version Mismatch**: Run `nvm use` in project root
3. **Yarn Lock Conflicts**: Run `yarn install` to update lockfile
4. **ENOBUFS Errors**: Hook auto-fixes, but manual `yarn nx reset` may be needed

#### Manual Fix Commands
```bash
# Reset environment
cd core-web
yarn nx reset
yarn install

# Run checks manually
yarn nx affected -t lint --fix=true
yarn nx format:write
```

## CI/CD Pipeline Architecture

### Pipeline Flow
```
PR Created → PR Workflow → Merge Queue → Trunk → Nightly → LTS (manual)
     ↓
All workflows → Post-Workflow Reporting
```

### Core Workflows (Execution Order)

#### 1. PR Workflow (`cicd_1-pr.yml`)
- **Trigger**: Pull requests to main/master
- **Purpose**: Comprehensive PR validation
- **Security**: No secrets in PR context (zero-trust)
- **Features**:
  - Conditional execution based on file changes
  - Unit, integration, and E2E tests
  - Semgrep security analysis
  - PR status notifications

#### 2. Merge Queue (`cicd_2-merge-queue.yml`)
- **Trigger**: Merge group checks requested
- **Purpose**: Final validation before merge
- **Features**:
  - Comprehensive test suite
  - Artifact generation for reuse
  - Flaky test detection

#### 3. Trunk Workflow (`cicd_3-trunk.yml`)
- **Trigger**: Push to main branch
- **Purpose**: Post-merge processing
- **Features**:
  - Artifact reuse from merge queue
  - CLI native binary building
  - Deployment to trunk environment
  - SDK library publishing

#### 4. Nightly Workflow (`cicd_4-nightly.yml`)
- **Trigger**: Scheduled cron job
- **Purpose**: Extended testing
- **Features**:
  - Extended test suites
  - Nightly environment deployment
  - Performance benchmarking

#### 5. LTS Workflow (`cicd_5-lts.yml`)
- **Trigger**: Manual dispatch only
- **Purpose**: Long-term support releases
- **Features**:
  - On-demand release preparation
  - Comprehensive validation
  - Special release artifacts

## Reusable Workflow Components

### Core Components
- **Initialize Phase**: Change detection and build planning
- **Build Phase**: Maven builds and artifact generation
- **Test Phase**: Test orchestration (JVM, CLI, frontend, integration, E2E)
- **Semgrep Phase**: Security and code quality analysis
- **Deployment Phase**: Environment deployments
- **Finalize Phase**: Status aggregation

### Change Detection System
**Configuration**: [`.github/filters.yaml`](.github/filters.yaml)

The system uses YAML anchors and references to avoid duplication:

```yaml
# Example: Backend filter with anchor
backend: &backend
  - 'dotCMS/!(src/main/webapp/html/)**'
  - 'dotcms-integration/**'
  - 'pom.xml'
  - *full_build_test  # References full_build_test anchor

# Example: Frontend filter 
frontend: &frontend
  - 'core-web/**'
  - *full_build_test
```

**Key Filters:**
- `backend`: Java code, Maven files, integration tests
- `frontend`: Angular code, CSS/JS files  
- `cli`: CLI tools and related backend changes
- `full_build_test`: Infrastructure files that require full rebuilds

## Security Guidelines

### Critical Security Rules
- **NO secrets in PR workflows** (zero-trust model)
- **Validate all user inputs** using environment variables
- **Use minimal explicit permissions**
- **Pin actions to specific versions** (never @master)

### Security Patterns
```yaml
# ❌ NEVER: Secrets in PR workflows
on: pull_request
secrets:
  ANY_SECRET: ${{ secrets.ANY_SECRET }}  # SECURITY VIOLATION

# ✅ ALWAYS: Environment variables for user input
env:
  USER_INPUT: ${{ github.event.issue.title }}
run: |
  if [[ "$USER_INPUT" =~ ^[a-zA-Z0-9\ \-\_]+$ ]]; then
    echo "Valid: $USER_INPUT"
  fi

# ✅ ALWAYS: Explicit minimal permissions
permissions:
  contents: read
  packages: write  # Only if needed
```

## Development Workflow Integration

### Backend Development
When working on backend Java code:

1. **Follow Java Standards**: See [Java Standards](../backend/JAVA_STANDARDS.md)
2. **Test Triggering**: Changes to `/dotCMS/src/main/java/**` trigger backend tests
3. **Build Commands**:
   ```bash
   # Fast build for development
   ./mvnw install -pl :dotcms-core -DskipTests
   
   # Full build with tests
   ./mvnw clean install
   ```

### Frontend Development
When working on Angular frontend code:

1. **Follow Angular Standards**: See [Angular Standards](../frontend/ANGULAR_STANDARDS.md)
2. **Test Triggering**: Changes to `/core-web/**` trigger frontend tests
3. **Build Commands**:
   ```bash
   # Development server
   cd core-web && nx run dotcms-ui:serve
   
   # Run tests
   cd core-web && nx run dotcms-ui:test
   ```

### Cross-Domain Changes
For changes affecting both backend and frontend:

1. **Review API Contracts**: See [API Contracts](../integration/API_CONTRACTS.md)
2. **Comprehensive Testing**: Both backend and frontend tests will run
3. **Integration Testing**: E2E tests validate full workflows

## Testing Strategy

### Test Categories
- **JVM Unit Tests**: Fast Java unit tests
- **CLI Tests**: Command-line interface testing
- **Frontend Tests**: Angular component tests
- **Integration Tests**: Database and API integration
- **E2E Tests**: End-to-end user workflows

### Test Execution Context
- **PR Context**: Limited tests based on changes
- **Merge Queue**: Comprehensive test suite
- **Trunk**: Configurable with artifact reuse
- **Nightly**: Extended test suites

## Troubleshooting Common Issues

### Workflow Not Running
```bash
# Check syntax
yamllint .github/workflows/your-workflow.yml

# Verify file location
ls -la .github/workflows/

# Test trigger conditions
# Create test PR or push to monitored branch
```

### Tests Being Skipped
```yaml
# Debug change detection
- name: Debug Change Detection
  run: |
    echo "Backend changes: ${{ needs.initialize.outputs.backend }}"
    echo "Frontend changes: ${{ needs.initialize.outputs.frontend }}"
    echo "Build required: ${{ needs.initialize.outputs.build }}"
```

Fix by updating `.github/filters.yaml`:
```yaml
backend: &backend
  - 'dotCMS/!(src/main/webapp/html/)**'
  - 'your-new-path/**'  # Add missing paths
  - *full_build_test
```

### Build Failures
```bash
# Check Java version
cat .sdkmanrc

# Verify Maven dependencies
./mvnw dependency:tree

# Check for conflicts
./mvnw dependency:analyze
```

## Performance Optimization

### Caching Strategy
- **Maven Dependencies**: Cached between builds
- **Node Modules**: Cached for frontend builds
- **Build Artifacts**: Reused across workflow stages

### Parallel Execution
- **Test Matrix**: Multiple Java versions and configurations
- **Independent Jobs**: Run in parallel where possible
- **Change Detection**: Skip unnecessary work

## Monitoring and Reporting

### Status Aggregation
- **Real-time Status**: Slack notifications via `#guild-dev-pipeline`
- **Failure Analysis**: Detailed error reporting
- **Performance Metrics**: Build time and test execution tracking

### Support Channels
- **Primary**: `#guild-dev-pipeline` Slack channel
- **GitHub Issues**: Technical issues and bug reports
- **Documentation**: Comprehensive guides in `/docs`

## Best Practices Summary

### ✅ Always Do
- Use reusable workflow components
- Follow security patterns (no secrets in PR context)
- Implement intelligent change detection
- Test changes thoroughly before deployment
- Document workflow purpose and features

### ❌ Never Do
- Create duplicate workflows for same trigger
- Add secrets to PR workflows
- Modify legacy workflows unnecessarily
- Use hardcoded values (use variables)
- Implement build logic directly in main workflows

## Quick Reference Commands

### Git Operations
```bash
# Create feature branch (REQUIRED pattern)
git checkout -b issue-12345-short-description

# Commit with descriptive message (informal is fine)
git commit -m "Add authentication logic and tests"

# Push and create PR
git push origin issue-12345-short-description
```

### Build Operations
```bash
# Fast backend build
./mvnw install -pl :dotcms-core -DskipTests

# Frontend development
cd core-web && nx run dotcms-ui:serve

# Full build with tests
./mvnw clean install
```

### Dependency Management
```bash
# Check dependency tree
./mvnw dependency:tree

# Find conflicts
./mvnw dependency:tree -Dverbose

# Analyze dependencies
./mvnw dependency:analyze
```

## Integration with Main Development

This workflow integrates with the main dotCMS development patterns:

- **Security**: Follows [Security Principles](SECURITY_PRINCIPLES.md)
- **Java**: Uses [Java Standards](../backend/JAVA_STANDARDS.md)
- **Angular**: Uses [Angular Standards](../frontend/ANGULAR_STANDARDS.md)
- **Maven**: Uses [Maven Build System](../backend/MAVEN_BUILD_SYSTEM.md)
- **Testing**: Follows [Testing Patterns](../frontend/TESTING_FRONTEND.md)

## Location Information
- **Workflow Files**: `.github/workflows/`
- **Custom Actions**: `.github/actions/core-cicd/`
- **Change Detection**: `.github/filters.yaml`
- **Documentation**: `docs/core/GIT_WORKFLOWS.md`
- **Support Channel**: `#guild-dev-pipeline` Slack