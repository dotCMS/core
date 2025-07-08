# Claude Code Guidelines for GitHub Actions

## Overview

This document provides specific AI workflow guidance for Claude Code when working with GitHub Actions in this repository. For comprehensive documentation on workflows, architecture, and procedures, see:

- **[README.md](./README.md)** - Overview and navigation hub
- **[Getting Started](./docs/getting-started.md)** - Developer guide to GitHub Actions and modular architecture
- **[Architecture](./docs/architecture.md)** - Pipeline architecture and component structure
- **[Testing Strategy](./docs/testing.md)** - Test categories and execution strategies
- **[Security](./docs/security.md)** - Security guidelines, threat model, and best practices
- **[Troubleshooting](./docs/troubleshooting.md)** - Common issues and debugging procedures

## AI-Specific Workflow Planning

### 🎯 Pre-Development Analysis

**Before making any changes, AI should:**

1. **Understand the Goal**: What specific outcome is the user requesting?
2. **Identify Scope**: Which workflows/components need modification?
3. **Check Security**: Will this change affect security boundaries?
4. **Plan Testing**: What testing approach is needed for the changes?

### 🎯 Quick AI Decision Tree

**AI workflow planning based on user request:**

```
🤔 User wants to...
├── 🧪 **Add/modify tests**
│   ├── Reference: [Testing Strategy](./docs/testing.md)
│   ├── Primary Files: cicd_comp_test-phase.yml, filters.yaml
│   └── Security: No secrets in PR context
├── 🔨 **Change build process**
│   ├── Reference: [Architecture](./docs/architecture.md)
│   ├── Primary Files: cicd_comp_build-phase.yml, maven-job action
│   └── Security: Check artifact permissions
├── 🔐 **Handle secrets/security**
│   ├── Reference: [Security](./docs/security.md)
│   ├── Key Rule: Never add secrets to PR workflows
│   └── Solution: Use cicd_post-workflow-reporting.yml
├── 🐛 **Fix broken workflow**
│   ├── Reference: [Troubleshooting](./docs/troubleshooting.md)
│   ├── Debug: Add context outputs for diagnosis
│   └── Check: filters.yaml, conditional logic, artifact-run-id
└── 📊 **Optimize performance**
    ├── Reference: [Architecture](./docs/architecture.md)
    ├── Focus: Change detection, caching, parallelization
    └── Tool: Initialization phase outputs
```

## AI Security Guidelines

### Critical Security Rules for AI

**🚨 NEVER do these in AI-generated workflows:**
```yaml
# ❌ NEVER: Secrets in PR workflows
on: pull_request
secrets:
  ANY_SECRET: ${{ secrets.ANY_SECRET }}  # SECURITY VIOLATION

# ❌ NEVER: Direct input injection
run: echo "Title: ${{ github.event.issue.title }}"  # INJECTION RISK

# ❌ NEVER: Default permissions without explicit justification
# (No permissions block = default permissions = security risk)
```

**✅ ALWAYS do these in AI-generated workflows:**
```yaml
# ✅ Environment variables for user input
env:
  USER_INPUT: ${{ github.event.issue.title }}
run: |
  # Validate input in shell
  if [[ "$USER_INPUT" =~ ^[a-zA-Z0-9\ \-\_]+$ ]]; then
    echo "Valid: $USER_INPUT"
  fi

# ✅ Explicit minimal permissions
permissions:
  contents: read
  packages: write  # Only if needed
  checks: write    # Only if needed

# ✅ Use post-workflow reporting for secrets
# Place secret-dependent operations in cicd_post-workflow-reporting.yml
```

### AI Security Checklist

**Before generating/modifying workflows:**
- [ ] Will this workflow be triggered by PRs? → No secrets allowed
- [ ] Does this use user input? → Must validate and use env vars
- [ ] What permissions are needed? → Use minimal explicit permissions
- [ ] Are actions pinned to versions? → Never use @master

## AI Development Patterns

### Pattern 1: Adding a New Test Job
```yaml
# AI should modify: .github/workflows/cicd_comp_test-phase.yml
# Add input parameter and job with proper conditionals
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

### Pattern 2: Modifying Change Detection
```yaml
# AI should modify: .github/filters.yaml
# Add new feature filter with proper YAML anchors
my-feature: &my-feature
  - 'src/main/java/com/dotcms/myfeature/**'
  - 'src/test/java/com/dotcms/myfeature/**'
  - *full_build_test

backend: &backend
  # ... existing paths ...
  - *my-feature
```

### Pattern 3: Debugging Workflow Issues
```yaml
# AI should add debugging context when troubleshooting
- name: Debug Workflow Context
  run: |
    echo "Event: ${{ github.event_name }}"
    echo "Ref: ${{ github.ref }}"
    echo "Actor: ${{ github.actor }}"
    echo "Backend changes: ${{ needs.initialize.outputs.backend }}"
    echo "Build required: ${{ needs.initialize.outputs.build }}"
    echo "Found artifacts: ${{ needs.initialize.outputs.found_artifacts }}"
```

## AI Workflow Modification Rules

### ✅ AI Should Modify These Files
- **`cicd_comp_*.yml`** - Reusable component workflows
- **`.github/filters.yaml`** - Change detection patterns
- **`.github/actions/core-cicd/**/action.yml`** - Custom actions

### ⚠️ AI Should Be Cautious With These Files
- **`cicd_1-pr.yml` through `cicd_5-lts.yml`** - Main pipeline workflows
  - **Reason**: Changes only take effect after merge
  - **Testing**: Use core-workflow-test repository for validation

### ❌ AI Should NEVER Modify These Files
- **`legacy-*.yml`** - Legacy workflows
  - **Reason**: Requires dedicated modernization task with extensive testing
  - **Exception**: Only for critical security fixes with minimal changes

## AI Testing Strategy

### AI Testing Limitations

**⚠️ Important: AI cannot directly access core-workflow-test repository**

AI assistants cannot:
- Push code changes to external repositories
- Create pull requests in the testing repository
- Authenticate with GitHub to access other repositories
- Clone or interact with repositories outside the current working directory

### When Fork Testing Should Be Used
- **Highly Recommended**: Modifications to core pipeline workflows (`cicd_1-pr.yml` through `cicd_5-lts.yml`)
- **Recommended**: Changes to reusable components affecting multiple workflows
- **Repository**: https://github.com/dotCMS/core-workflow-test

### AI-Assisted Testing Workflow

**What AI Can Do:**
1. **Generate complete workflow files** with proposed changes
2. **Provide testing guidance** on what scenarios to validate
3. **Plan testing strategies** and explain expected behavior
4. **Review workflow changes** for potential issues before testing
5. **Help analyze failures** and suggest fixes based on error logs

**What User Must Do:**
1. **Push AI-generated changes** to core-workflow-test repository
2. **Create test PRs** to validate behavior in different contexts
3. **Test different scenarios** (PR, merge queue, trunk)
4. **Verify artifact generation** and consumption
5. **Report back results** for AI to help analyze and improve

### AI Local Testing Capabilities

**AI can help with:**
- **YAML syntax validation** using local tools
- **Workflow structure analysis** to catch obvious issues
- **Security pattern review** to ensure compliance
- **Change impact assessment** to predict behavior
- **Conditional logic validation** to verify job dependencies

## AI Error Response Patterns

### When Workflows Fail
**AI should check these in order:**
1. **Syntax Issues**: YAML validation, missing required fields
2. **Conditional Logic**: Job dependencies, if conditions
3. **Change Detection**: Files match filters.yaml patterns
4. **Artifact Issues**: Correct artifact-run-id, generation vs consumption
5. **Permissions**: Required permissions for operations

### AI Debugging Commands
```yaml
# Add to workflows for AI debugging
- name: AI Debug Context
  run: |
    echo "=== AI DEBUGGING ==="
    echo "Workflow: ${{ github.workflow }}"
    echo "Event: ${{ github.event_name }}"
    echo "Ref: ${{ github.ref }}"
    echo "Initialization outputs:"
    echo "  Backend: ${{ needs.initialize.outputs.backend }}"
    echo "  Frontend: ${{ needs.initialize.outputs.frontend }}"
    echo "  Build: ${{ needs.initialize.outputs.build }}"
    echo "  Artifacts: ${{ needs.initialize.outputs.found_artifacts }}"
```

## AI Integration with Main Codebase

### Follow dotCMS Patterns
When AI generates workflow code, follow the main codebase standards from [CLAUDE.md](../CLAUDE.md):
- Follow dotCMS logging patterns where applicable
- Use proper error handling patterns

## AI Support Resources

### When AI Gets Stuck
1. **Check Documentation**: Reference [docs/](./docs/) for detailed guidance
2. **Security Questions**: Always refer to [Security Documentation](./docs/security.md)
3. **Architecture Questions**: Reference [Architecture Documentation](./docs/architecture.md)
4. **Troubleshooting**: Use [Troubleshooting Guide](./docs/troubleshooting.md)

### AI Development Principles
1. **Security First**: Always consider security implications
2. **Use Existing Patterns**: Leverage established workflow components
3. **Validate Changes**: Test modifications appropriately
4. **Document Decisions**: Explain AI reasoning for complex changes
5. **Reference Documentation**: Point users to relevant docs/ sections

## Quick AI Reference

**Most Common AI Tasks:**
- **Add Tests**: Modify `cicd_comp_test-phase.yml` + `filters.yaml`
- **Change Build**: Modify `cicd_comp_build-phase.yml` or `maven-job` action
- **Debug Issues**: Add debug context + check [Troubleshooting](./docs/troubleshooting.md)
- **Security Changes**: Never in PR context + use [Security Guidelines](./docs/security.md)

**AI Safety Checks:**
- [ ] No secrets in PR-triggered workflows
- [ ] User input is validated and uses environment variables
- [ ] Permissions are explicitly defined and minimal
- [ ] Actions are pinned to specific versions
- [ ] Changes are tested appropriately

**Remember**: When in doubt, reference the comprehensive documentation in [docs/](./docs/) and follow established patterns from existing workflows.