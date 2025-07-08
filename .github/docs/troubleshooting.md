# Troubleshooting

## Common Issues and Solutions

### 🚨 **Quick Issue Identifier**

**Use this flowchart to identify your issue:**
```
❓ What's happening?
├── 🔇 Workflow not running at all
│   ├── Check: Syntax errors → Run yamllint
│   ├── Check: File location → Verify .github/workflows/
│   └── Check: Repository settings → Workflow disabled?
├── ⏭️ Jobs being skipped
│   ├── Check: Environment variables → CICD_SKIP_TESTS=true?
│   ├── Check: Change detection → Files match filters.yaml?
│   └── Check: Conditional logic → Job conditions false?
├── 💥 Jobs failing
│   ├── Check: Dependencies → Previous jobs failed?
│   ├── Check: Permissions → Secrets accessible?
│   └── Check: Resources → Runner out of space?
├── 🔍 Artifacts missing
│   ├── Check: Artifact run ID → Correct workflow run?
│   ├── Check: Timing → Generated before consumed?
│   └── Check: Naming → Exact name match?
└── 🐌 Workflows too slow
    ├── Check: Change detection → Running unnecessary jobs?
    ├── Check: Caching → Artifacts being reused?
    └── Check: Parallelization → Jobs running in series?
```

### 🔧 **Detailed Solutions**

#### 1. "My workflow isn't running"

**🎯 Goal**: Get workflow to execute when expected

**🔍 Diagnosis Checklist**:
- [ ] Workflow file exists in `.github/workflows/`
- [ ] YAML syntax is valid
- [ ] Trigger conditions are met
- [ ] Repository settings allow workflow

**🛠️ Step-by-Step Fix**:
```bash
# 1. Check for syntax errors
yamllint .github/workflows/your-workflow.yml

# 2. Verify file location
ls -la .github/workflows/

# 3. Test trigger conditions
# For PR workflows: Create a test PR
# For push workflows: Push to monitored branch

# 4. Check repository settings
# Go to: Settings → Actions → General → Workflow permissions
```

**✅ Success Criteria**: Workflow appears in GitHub Actions UI when triggered

#### 2. "Tests are skipped unexpectedly"

**Symptoms**: Test jobs show as "skipped" when they should run
**Common Causes**:
- Change detection filters exclude your changes
- Conditional logic evaluates to false
- Missing required inputs

**Solution Steps**:
```yaml
# Check the initialization phase outputs
- name: Debug Change Detection
  run: |
    echo "Backend changes: ${{ needs.initialize.outputs.backend }}"
    echo "Frontend changes: ${{ needs.initialize.outputs.frontend }}"
    echo "Test changes: ${{ needs.initialize.outputs.test }}"
```

**Fix**: Update `.github/filters.yaml` to include your file paths:
```yaml
backend: &backend
  - 'src/main/java/**'
  - 'your-new-path/**'  # Add this line
```

#### 3. "Build fails with Java version mismatch"

**Symptoms**: Build fails with "Java version X required, but Y found"
**Common Causes**:
- `.sdkmanrc` file not being read
- Action not using `setup-java` action
- Cache corruption

**Solution Steps**:
```bash
# Check the .sdkmanrc file
cat .sdkmanrc

# Verify the action is using setup-java
grep -r "setup-java" .github/workflows/
```

**Fix**: Use the standard pattern:
```yaml
steps:
  - uses: ./.github/actions/core-cicd/prepare-runner
    # This automatically uses .sdkmanrc version
```

#### 3a. "PR fails with '.mvn/maven.config should not be modified'"

**Symptoms**: PR build fails with error about maven.config modification
**Common Causes**:
- Developer accidentally committed `.mvn/maven.config` file
- Trying to change version in PR instead of using release process
- Local build artifacts committed to PR

**Solution Steps**:
```bash
# Check if the file exists in your PR
git diff --name-only origin/main...HEAD | grep maven.config

# Remove the file if it exists
rm -f .mvn/maven.config
git add .mvn/maven.config
git commit -m "Remove maven.config - not allowed in PRs"
```

**Fix**: Never modify versions in PRs:
```yaml
# ❌ WRONG - Don't create this file in PRs
.mvn/maven.config

# ✅ CORRECT - PRs always use snapshot version
# Version changes happen in release workflows only
```

#### 4. "Artifact not found errors"

**Symptoms**: Workflow fails when trying to download artifacts
**Common Causes**:
- Wrong artifact-run-id
- Previous build didn't generate artifacts
- Artifact expired

**Solution Steps**:
```yaml
# Debug artifact settings
- name: Debug Artifacts
  run: |
    echo "Artifact run ID: ${{ inputs.artifact-run-id }}"
    echo "Current run ID: ${{ github.run_id }}"
```

**Fix**: Ensure artifact reuse is set up correctly:
```yaml
uses: ./.github/workflows/cicd_comp_test-phase.yml
with:
  artifact-run-id: ${{ needs.initialize.outputs.artifact-run-id }}
```

#### 5. "Permission denied errors"

**Symptoms**: Actions fail with permission errors
**Common Causes**:
- Wrong permissions in workflow
- Trying to access secrets in PR context
- Missing repository permissions

**Solution Steps**:
```yaml
# Check current permissions
permissions:
  contents: read
  packages: write
  checks: write
```

**Fix**: Never add secrets to PR workflows:
```yaml
# ❌ WRONG - in PR workflow
secrets:
  SLACK_TOKEN: ${{ secrets.SLACK_TOKEN }}

# ✅ CORRECT - use post-workflow reporting
# PRs automatically trigger post-workflow reporting
```

#### 6. "Workflow runs too long"

**Symptoms**: Builds take 30+ minutes, exceed timeout
**Common Causes**:
- Not using change detection
- Running all tests instead of conditional tests
- Missing cache optimization

**Solution Steps**:
```yaml
# Enable change detection
test:
  needs: [initialize]
  if: needs.initialize.outputs.backend == 'true'
  # Only run if backend files changed
```

**Fix**: Use the initialization phase:
```yaml
jobs:
  initialize:
    uses: ./.github/workflows/cicd_comp_initialize-phase.yml
    
  test:
    needs: [initialize]
    if: needs.initialize.outputs.backend == 'true'
    uses: ./.github/workflows/cicd_comp_test-phase.yml
```

## Debugging Workflow Issues

### Step 1: Check Workflow Syntax
```bash
# Use yamllint to validate syntax
yamllint .github/workflows/your-workflow.yml

# Check for common YAML issues
python -c "import yaml; yaml.safe_load(open('.github/workflows/your-workflow.yml'))"
```

### Step 2: Add Debug Outputs
```yaml
- name: Debug Context
  run: |
    echo "Event: ${{ github.event_name }}"
    echo "Ref: ${{ github.ref }}"
    echo "SHA: ${{ github.sha }}"
    echo "Actor: ${{ github.actor }}"
  env:
    GITHUB_CONTEXT: ${{ toJson(github) }}
```

### Step 3: Check Dependencies
```yaml
- name: Check Dependencies
  run: |
    echo "Needs: ${{ toJson(needs) }}"
    echo "Job status: ${{ job.status }}"
```

### Step 4: Verify File Changes
```yaml
- name: Check Changed Files
  run: |
    git diff --name-only ${{ github.event.before }} ${{ github.sha }}
```

## Emergency Procedures

### Workflow Blocking All PRs
1. **Immediate**: Disable the workflow in GitHub UI (Settings → Actions → Workflows)
2. **Quick Fix**: Create emergency PR with minimal fix
3. **Long-term**: Investigate root cause and add safeguards

### Critical Build Failure
1. **Check**: #guild-dev-pipeline Slack for known issues
2. **Diagnose**: Look at recent runs in GitHub Actions UI
3. **Escalate**: If affects production, contact DevOps team immediately

## Performance Optimization Tips

### 1. Use Change Detection
```yaml
# Instead of running all tests
test:
  steps:
    - name: Run All Tests
      run: ./mvnw test

# Run tests conditionally
test:
  if: needs.initialize.outputs.backend == 'true'
  steps:
    - name: Run Backend Tests
      run: ./mvnw test
```

### 2. Enable Caching
```yaml
# The maven-job action handles caching automatically
- uses: ./.github/actions/core-cicd/maven-job
  with:
    generate-artifacts: true  # Enables all caching
```

### 3. Parallel Execution
```yaml
strategy:
  matrix:
    test-group: [unit, integration, e2e]
  fail-fast: false  # Don't cancel other tests if one fails
```

### 4. Future: Build Once, Deploy Anywhere
```yaml
# Long-term goal: Reduce rebuilds by reusing artifacts
# Current: Multiple rebuilds for same code
# Future: Build once, tag and deploy to multiple environments
# See "Long-Term Vision: Build Once, Deploy Anywhere" section for details
```

## Getting Help

### For Internal Developers
- **Primary Channel**: #guild-dev-pipeline Slack channel
- **Best For**: Questions, troubleshooting, implementation guidance
- **Response Time**: Real-time during business hours
- **Expertise**: Direct access to CI/CD team and community knowledge

### For External Contributors
- **GitHub Issues**: Bug reports and technical issues
- **GitHub Discussions**: Feature requests and architectural questions
- **Documentation**: This README and inline workflow documentation

## Common Debug Commands

### Check Workflow Status
```bash
# View workflow runs
gh run list

# View specific run details
gh run view <run-id>

# View run logs
gh run view <run-id> --log
```

### Analyze File Changes
```bash
# Check what files changed in PR
git diff --name-only origin/main...HEAD

# Check if changes match filters
# Compare with .github/filters.yaml patterns
```

### Test Workflow Locally
```bash
# Use act to test workflows locally (limited functionality)
act pull_request

# Validate YAML syntax
yamllint .github/workflows/*.yml
```

### Check Repository Settings
```bash
# View repository variables
gh variable list

# View repository secrets (names only)
gh secret list
```