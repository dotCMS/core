# Getting Started for New Developers

## What is GitHub Actions?

GitHub Actions is a CI/CD (Continuous Integration/Continuous Deployment) platform that allows you to automate software workflows directly in your GitHub repository. Think of it as a way to automatically run tests, build your code, and deploy applications whenever certain events happen (like creating a pull request or pushing to a branch).

## Key GitHub Actions Concepts

### 1. **Workflows** (`.github/workflows/*.yml`)
- **What**: YAML files that define automated processes
- **When**: Triggered by events (PR creation, push to branch, schedule, etc.)
- **Example**: `cicd_1-pr.yml` runs when you open a pull request

### 2. **Actions** (`.github/actions/*/action.yml`)
- **What**: Reusable units of code that perform specific tasks
- **Why**: Avoid duplicating common operations across workflows
- **Example**: `setup-java` action installs Java consistently across all workflows

### 3. **Jobs and Steps**
- **Jobs**: A workflow contains one or more jobs that run in parallel or sequence
- **Steps**: Each job contains steps that run sequentially
- **Example**: A job might have steps to checkout code, setup Java, run tests

## Understanding Our Modular Architecture

### Why Modular? (The Problem We Solved)

**Before (Traditional Approach)**:
```yaml
# cicd_1-pr.yml - 200+ lines of duplicated code
name: PR Workflow
on: pull_request
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Setup Java (duplicated everywhere)
        uses: actions/setup-java@v4
        with:
          java-version: '21'
      - name: Run tests (duplicated everywhere)
        run: ./mvnw test
      # ... 50+ more duplicated lines
```

**After (Our Modular Approach)**:
```yaml
# cicd_1-pr.yml - 20 lines, calls reusable components
name: PR Workflow
on: pull_request
jobs:
  test:
    uses: ./.github/workflows/cicd_comp_test-phase.yml
    # That's it! All the complexity is hidden in the reusable component
```

### The Benefits You Get

1. **Less Code to Maintain**: Change test logic in one place, affects all workflows
2. **Consistency**: All workflows use the same Java version, same build steps
3. **Easy to Understand**: Each workflow file focuses on "what" not "how"
4. **Reduced Bugs**: Less duplication means fewer places for bugs to hide

## File Structure Guide

```
.github/
├── workflows/
│   ├── cicd_1-pr.yml           # Main entry point for PRs
│   ├── cicd_2-merge-queue.yml  # Merge queue validation
│   ├── cicd_3-trunk.yml        # Post-merge to main
│   ├── cicd_comp_*.yml         # ✅ Reusable components (USE THESE)
│   └── legacy-*.yml            # ⚠️ Legacy files (see Legacy Guidelines)
├── actions/
│   └── core-cicd/
│       ├── prepare-runner/     # Sets up runner environment
│       ├── setup-java/         # Java installation
│       └── maven-job/          # Maven build orchestration
└── filters.yaml                # Defines what changes trigger what tests
```

## Your First Workflow Modification

### Quick Decision Tree: What Do You Want to Do?

```
🤔 I want to...
├── Add a new test type
│   ├── ✅ Unit tests → Modify cicd_comp_test-phase.yml
│   ├── ✅ Integration tests → Add to integration matrix
│   └── ✅ E2E tests → Update E2E configuration
├── Change build process
│   ├── ✅ Add build step → Modify cicd_comp_build-phase.yml
│   ├── ✅ Change Java version → Update .sdkmanrc
│   └── ✅ Add dependencies → Update maven-job action
├── Modify security/permissions
│   ├── ✅ Add secrets → Use post-workflow reporting
│   └── ✅ Change permissions → Update workflow permissions
└── Fix broken workflow
    ├── ✅ Tests skipped → Check filters.yaml
    ├── ✅ Artifacts missing → Check artifact-run-id
    └── ✅ Jobs failing → Check conditional logic
```

### Detailed Example: Adding a New Test Type

**🎯 Goal**: Add a new test type that runs when specific files change

**📍 Step 1: Understand the Flow**
```
PR Created → cicd_1-pr.yml → calls cicd_comp_test-phase.yml → runs your tests
```

**📍 Step 2: Modify the Test Component** (NOT the main workflow)
```yaml
# Edit: .github/workflows/cicd_comp_test-phase.yml
jobs:
  my-new-test:
    name: My New Test
    runs-on: ubuntu-${{ vars.UBUNTU_RUNNER_VERSION || '24.04' }}
    if: inputs.run-my-new-test || inputs.run-all-tests
    steps:
      - uses: actions/checkout@v4
      - uses: ./.github/actions/core-cicd/maven-job
        with:
          stage-name: "My New Test"
          maven-args: "test -Dtest=MyNewTest"
```

**📍 Step 3: Add Change Detection** (Optional but recommended)
```yaml
# Edit: .github/filters.yaml
my-new-feature: &my-new-feature
  - 'src/my-new-feature/**'
  - *full_build_test

test:
  - *backend
  - *my-new-feature
```

**📍 Step 4: Enable the Test**
```yaml
# Edit: .github/workflows/cicd_comp_test-phase.yml
on:
  workflow_call:
    inputs:
      run-my-new-test:
        required: false
        type: boolean
        default: false
```

**📍 Step 5: Test Your Changes**
```
1. Push to core-workflow-test repository (if major change)
2. Create test PR to validate behavior
3. Check GitHub Actions UI for execution
4. Verify test results appear correctly
```

## Common Modification Patterns

### 1. **Adding a New Build Step**
✅ **DO**: Add it to the appropriate `cicd_comp_*.yml` file
❌ **DON'T**: Add it directly to `cicd_1-pr.yml`

### 2. **Changing Java Version**
✅ **DO**: Update `.sdkmanrc` file (affects all workflows)
❌ **DON'T**: Update individual workflow files

### 3. **Adding New Dependencies**
✅ **DO**: Add to `maven-job` action or create new action
❌ **DON'T**: Duplicate setup code across workflows

### 4. **Modifying Security Settings**
✅ **DO**: Read the security section first, understand implications
❌ **DON'T**: Add secrets to PR workflows (security violation)

### 5. **Working with Legacy Files**
✅ **DO**: Make minimal, necessary changes with thorough testing
✅ **DO**: Use modular actions when already changing that section
✅ **DO**: Use progressive modernization for dedicated refactoring tasks
✅ **DO**: Test changes extensively, especially release-related workflows
❌ **DON'T**: Change unrelated sections just to modernize
❌ **DON'T**: Refactor legacy files as part of unrelated tasks

## Understanding Legacy vs Modern

**You opened a legacy file**: `legacy-release_comp_maven-build-docker-image.yml`

**Key Differences**:
- **Legacy**: Direct Java version (`JAVA_VERSION: 11`)
- **Modern**: Uses `.sdkmanrc` for version control
- **Legacy**: Inline build steps (113 lines of Maven command)
- **Modern**: Uses `maven-job` action (3 lines)
- **Legacy**: Manual Docker context setup
- **Modern**: Automated via actions