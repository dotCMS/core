# Testing Strategy

## Test Categories

1. **JVM Unit Tests**: Fast unit tests for Java components
2. **CLI Tests**: Command-line interface testing
3. **Frontend Tests**: Angular component and integration tests
4. **Integration Tests**: Database and API integration tests
5. **Postman Tests**: API endpoint testing
6. **Karate Tests**: API behavior testing
7. **E2E Tests**: End-to-end user workflow testing

## Test Execution

- **PR Workflow**: Conditional execution based on changes
- **Merge Queue**: Currently runs all tests for stability validation
- **Trunk**: Configurable test execution with artifact reuse
- **Nightly**: Extended test suites on latest main

## Workflow Testing Strategy

### Core-Workflow-Test Repository

**Repository**: https://github.com/dotCMS/core-workflow-test

**Purpose**: Safe testing environment for workflow changes before applying to main repository

**When to Use**:
- **Highly Recommended**: Core pipeline workflows (`cicd_1-pr.yml` through `cicd_5-lts.yml`)
- **Recommended**: Reusable components (`cicd_comp_*.yml`) affecting multiple workflows
- **Consider**: Deployment workflows or those with external dependencies
- **Optional**: On-demand workflows (`cicd_manual_*.yml`) not affecting main pipeline

### Testing Approach

**Testing Pattern**:
1. **Fork Setup**: Push changes to core-workflow-test repository
2. **PR Creation**: Create test PRs to validate behavior
3. **Mock Credentials**: Use mock credentials for external services
4. **Scenario Testing**: Test different trigger scenarios
5. **Artifact Validation**: Verify artifact generation and consumption

**Critical Fork Management**:
- **Temporary Testing Only**: Use fork exclusively for testing, not permanent changes
- **Fork Reset Capability**: The fork can be reset to match main branch state when needed
- **No Permanent Changes**: Never keep important changes only in the fork - they may be lost during resets

### Test Execution Environments

#### PR Workflow Testing
- **Scope**: Basic functionality and security validation
- **Limitations**: No access to organization secrets
- **Focus**: Unit tests, integration tests, security scans

#### Merge Queue Testing
- **Scope**: Comprehensive test suite for stability
- **Features**: All tests currently run (may be optimized in future)
- **Artifacts**: Generates artifacts for downstream reuse

#### Trunk Testing
- **Scope**: Post-merge validation and deployment preparation
- **Features**: Artifact reuse, extended testing, deployment
- **Access**: Full access to secrets and deployment targets

#### Nightly Testing
- **Scope**: Extended test suites on latest main branch
- **Features**: Long-running tests, performance benchmarks
- **Schedule**: Automated cron-based execution

## Test Configuration

### Change Detection Integration

Tests are intelligently executed based on change detection from the initialization phase:

```yaml
test:
  needs: [initialize]
  if: needs.initialize.outputs.backend == 'true'
  uses: ./.github/workflows/cicd_comp_test-phase.yml
  with:
    jvm_unit_test: ${{ needs.initialize.outputs.jvm_unit_test == 'true' }}
    integration: ${{ needs.initialize.outputs.backend == 'true' }}
    frontend: ${{ needs.initialize.outputs.frontend == 'true' }}
    cli: ${{ needs.initialize.outputs.cli == 'true' }}
```

### Test Matrix Strategy

Tests use matrix strategies for parallel execution:

```yaml
strategy:
  matrix:
    java: [
      { name: "21", version: "21" }
    ]
  fail-fast: false
```

### Test Result Handling

- **Test Reports**: Automatically generated and stored as artifacts
- **Coverage Data**: Collected and preserved for analysis
- **Failure Analysis**: Failed tests are reported with detailed logs
- **Status Aggregation**: Overall test status is calculated across all test types

## Performance Considerations

### Conditional Test Execution

Tests are only run when relevant changes are detected:

```yaml
# Only run backend tests when backend files change
if: needs.initialize.outputs.backend == 'true'
```

### Artifact Reuse

Test artifacts are reused across workflow stages to minimize rebuild time:

```yaml
uses: ./.github/workflows/cicd_comp_test-phase.yml
with:
  artifact-run-id: ${{ needs.initialize.outputs.artifact-run-id }}
```

### Parallel Test Execution

Multiple test suites run in parallel to reduce overall execution time:

```yaml
jobs:
  unit-tests:
    # Runs in parallel with other test jobs
  integration-tests:
    # Runs in parallel with other test jobs
  e2e-tests:
    # Runs in parallel with other test jobs
```

## Test Quality Gates

### PR Requirements

- All applicable tests must pass before merge
- Security scans must complete successfully
- Code quality checks must pass

### Merge Queue Requirements

- Full test suite execution (currently comprehensive)
- Artifact generation for downstream use
- All quality gates must pass

### Trunk Requirements

- Post-merge validation
- Deployment readiness checks
- Extended integration testing

## Future Testing Improvements

### Planned Enhancements

1. **Test Optimization**: Reduce merge queue test execution based on change detection
2. **Flaky Test Detection**: Identify and quarantine unreliable tests
3. **Test Parallelization**: Further optimize test execution time
4. **Coverage Improvements**: Enhance test coverage reporting and analysis
5. **Performance Testing**: Add automated performance regression detection