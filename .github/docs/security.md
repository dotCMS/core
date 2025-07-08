# GitHub Actions and Workflow Security

## Critical Security Overview

GitHub Actions workflows represent a significant attack surface that requires careful security consideration. This section outlines the comprehensive security measures implemented in our CI/CD pipeline and provides guidelines for maintaining security best practices.

**⚠️ Current State vs. Best Practices:**
- **Current Implementation**: Mixed compliance with security best practices
- **Documentation**: Represents both current state and future aspirations
- **Priority**: Security hardening is a high-priority roadmap item

## Security Compliance Status

**✅ Well-Implemented:**
- PR security isolation (zero-trust model)
- Secret management and separation
- Modular workflow architecture
- Sophisticated error handling and status aggregation
- Artifact management and caching

**⚠️ Needs Improvement:**
- Permission management (37/47 workflows use default permissions)
- Action pinning (1 critical @master reference)
- Input validation (inconsistent implementation)
- Supply chain security (no Dependabot integration)

**🔴 Critical Gaps:**
- `ad-m/github-push-action@master` in security_scheduled_pentest.yml
- Default permissions in high-risk workflows (deployment, docs publishing)
- Missing systematic input validation patterns

## Security Threat Model

### Primary Security Risks

**1. Secret Exfiltration**
- **Risk**: Malicious code in PRs could attempt to access and exfiltrate secrets
- **Impact**: Compromise of production systems, external services, and credentials
- **Mitigation**: Zero-trust PR context with complete secret isolation

**2. Code Injection Attacks**
- **Risk**: Malicious input in PR titles, commit messages, or issue content could execute arbitrary code
- **Impact**: Workflow manipulation, secret access, or system compromise
- **Mitigation**: Proper input sanitization and context isolation

**3. Privilege Escalation**
- **Risk**: Workflows with excessive permissions could be exploited
- **Impact**: Unauthorized access to repository, packages, or external systems
- **Mitigation**: Minimal permission principles and environment-based access control

**4. Supply Chain Attacks**
- **Risk**: Compromised third-party actions or dependencies
- **Impact**: Backdoors, malicious code execution, or data theft
- **Mitigation**: Action pinning, dependency scanning, and trusted action usage

**5. Workflow Manipulation**
- **Risk**: Unauthorized modification of workflow files
- **Impact**: Bypassing security controls or introducing vulnerabilities
- **Mitigation**: Code review requirements and immutable workflow patterns

## Security Architecture Layers

### Layer 1: PR Security Isolation

**Zero-Trust PR Context**
```yaml
# PR workflows have NO access to organization secrets
on:
  pull_request:
    branches: [main, master]
# No secrets block in PR workflows
```

**Workflow Separation Pattern**
```yaml
# Sensitive operations isolated to separate workflow
on:
  workflow_run:
    workflows: ['PR Check']
    types: [completed]
secrets:
  SLACK_BOT_TOKEN: ${{ secrets.SLACK_BOT_TOKEN }}  # Only available here
```

**Benefits:**
- PR code cannot access organization secrets
- Fork-based PRs are completely isolated
- Malicious PR code cannot exfiltrate sensitive data
- Workflow logic cannot be modified by PR authors

### Layer 2: Permission-Based Access Control

**Minimal Permission Strategy**

**Current State:**
```yaml
# CURRENT: Only 10 out of 47 workflows define explicit permissions
# Most workflows use default permissions (security risk)

# GOOD EXAMPLES from current workflows:
permissions:
  contents: read          # Repository content access
  packages: write         # GitHub Packages publishing (build workflows)

permissions:
  checks: write          # Check run creation (reporting workflows)

permissions:
  contents: write        # Repository write access (legacy workflows)
  issues: write         # Issue management
  pull-requests: write  # PR management
```

**Security Gap:**
- 🔴 **Critical**: 37 out of 47 workflows use default permissions
- 🔴 **Risk**: Default permissions grant broad access including write to contents, issues, PRs
- ⚠️ **Concerning**: High-risk workflows (deployment, docs publishing) use default permissions

**Recommended Improvements:**
```yaml
# RECOMMENDED: Explicit minimal permissions for all workflows
permissions:
  contents: read          # Repository content access
  packages: write         # GitHub Packages publishing
  checks: write          # Check run creation
  pull-requests: write   # PR commenting and status
  actions: read          # Workflow and run access
  statuses: write        # Status check updates
  # Deny all other permissions implicitly
```

**Environment-Based Secrets**
```yaml
# Different environments have different secret access
deployment:
  environment: ${{ inputs.environment }}  # trunk, nightly, lts
  secrets:
    DOCKER_USERNAME: ${{ secrets.DOCKER_USERNAME }}
    SLACK_BOT_TOKEN: ${{ secrets.SLACK_BOT_TOKEN }}
```

### Layer 3: Input Validation and Sanitization

**Safe Input Handling**
```yaml
# NEVER use untrusted input directly in shell commands
- name: Process PR Title
  env:
    PR_TITLE: ${{ github.event.pull_request.title }}
  run: |
    # Use environment variables, not direct substitution
    echo "Processing PR: $PR_TITLE"
    
# AVOID: Direct injection risk
# run: echo "Processing PR: ${{ github.event.pull_request.title }}"
```

**JSON Processing for Complex Data**
```yaml
- name: Process Complex Data
  env:
    GITHUB_CONTEXT: ${{ toJson(github) }}
  run: |
    # Use jq for safe JSON processing
    echo "$GITHUB_CONTEXT" | jq '.event.pull_request.title'
```

**Input Validation Patterns**
```yaml
# Validate user input before processing
- name: Validate Input
  env:
    USER_INPUT: ${{ github.event.inputs.user_input }}
  run: |
    if [[ "$USER_INPUT" =~ ^[a-zA-Z0-9_-]+$ ]]; then
      echo "Valid input: $USER_INPUT"
    else
      echo "Invalid input detected"
      exit 1
    fi
```

### Layer 4: Action Security and Supply Chain

**Action Pinning Strategy**

**Current State:**
```yaml
# CURRENT PRACTICE: Major version pinning for most actions
- uses: actions/checkout@v4
- uses: docker/build-push-action@v6.15.0

# CRITICAL SECURITY ISSUE: This exists in production
- uses: ad-m/github-push-action@master  # SECURITY RISK - IMMEDIATE FIX NEEDED
```

**Security Gap:**
- 🔴 **Critical**: `ad-m/github-push-action@master` reference in security_scheduled_pentest.yml
- ⚠️ **Risk**: @master references can introduce supply chain vulnerabilities
- 📋 **Recommendation**: Pin to specific commit SHA for third-party actions

**Recommended Action Security:**
```yaml
# RECOMMENDED: Pin to specific commit SHA for third-party actions
- uses: docker/build-push-action@2eb1c1961a95fc15694676618e422e8ba1d63825

# ACCEPTABLE: Major version pinning for trusted actions
- uses: actions/checkout@v4
- uses: actions/setup-java@v4
```

**Trusted Action Sources**
- **actions/**: GitHub's official actions (high trust)
- **docker/**: Docker official actions (medium-high trust)
- **Third-party**: Requires careful evaluation and SHA pinning

### Layer 5: Artifact Security

**Artifact Access Control**
```yaml
# Artifacts are scoped to workflow run
- uses: actions/upload-artifact@v4
  with:
    name: secure-artifacts
    path: ./artifacts/
    retention-days: 7  # Minimize exposure window
```

**Artifact Validation**
```yaml
# Validate artifacts before use
- uses: actions/download-artifact@v4
  with:
    name: build-artifacts
    path: ./artifacts/
- name: Validate Artifacts
  run: |
    # Check artifact integrity
    sha256sum -c ./artifacts/checksums.txt
```

## Security Best Practices

### DO: Secure Secret Management

```yaml
# ✅ CORRECT: Use secrets in post-workflow context
on:
  workflow_run:
    workflows: ['PR Check']
    types: [completed]
secrets:
  SLACK_BOT_TOKEN: ${{ secrets.SLACK_BOT_TOKEN }}

# ✅ CORRECT: Environment-based secret access
deployment:
  environment: production
  secrets:
    DEPLOY_KEY: ${{ secrets.PROD_DEPLOY_KEY }}
```

### DON'T: Expose Secrets in PR Context

```yaml
# ❌ WRONG: Never add secrets to PR workflows
on:
  pull_request:
    branches: [main]
secrets:
  SECRET_KEY: ${{ secrets.SECRET_KEY }}  # SECURITY VIOLATION
```

### DO: Validate All Inputs

```yaml
# ✅ CORRECT: Environment variable approach
env:
  USER_INPUT: ${{ github.event.issue.title }}
run: |
  if [[ "$USER_INPUT" =~ ^[a-zA-Z0-9\ \-\_]+$ ]]; then
    echo "Valid input: $USER_INPUT"
  else
    echo "Invalid input detected"
    exit 1
  fi
```

### DON'T: Direct Input Injection

```yaml
# ❌ WRONG: Direct injection vulnerability
run: echo "Input: ${{ github.event.issue.title }}"  # INJECTION RISK
```

### DO: Use Minimal Permissions

```yaml
# ✅ CORRECT: Explicit minimal permissions
permissions:
  contents: read
  packages: write
  checks: write
```

### DON'T: Use Default Permissions

```yaml
# ❌ WRONG: Default permissions (37/47 workflows currently do this)
# No permissions block = default permissions = security risk
```

## Advanced Security Architecture

### Multi-Layer Security Model

**Security Layer Implementation:**
1. **Perimeter Security**: Repository access controls and branch protection
2. **Identity and Access Management**: GitHub token and secret management
3. **Workflow Security**: PR isolation and permission boundaries
4. **Runtime Security**: Input validation and execution controls
5. **Audit and Monitoring**: Activity logging and security scanning

### Post-Workflow Reporting Security

**Critical Security Pattern:**
```yaml
# Post-workflow reporting runs in main branch context
# This is WHY it can access secrets (it's not PR-triggered)
on:
  workflow_run:
    workflows: ['PR Check', 'Merge Group Check']
    types: [completed]
    
# This workflow has access to secrets because:
# 1. It runs from main branch (trusted context)
# 2. Triggered by workflow_run (not PR events)
# 3. Cannot be modified by PR authors
```

**Security Benefits:**
- PR code cannot modify reporting logic
- Secrets are isolated from PR context
- Reporting runs in trusted environment
- Malicious PRs cannot exfiltrate secrets through reporting

### Permission Matrices

**Workflow Context Security:**

| Workflow Type | Contents | Packages | Secrets | Pull Requests | Issues |
|---------------|----------|----------|---------|---------------|--------|
| PR Workflows | Read | Write | ❌ NONE | Write | Read |
| Post-Workflow | Read | Write | ✅ ALL | Write | Write |
| Trunk/Nightly | Read | Write | ✅ ALL | Write | Write |
| Manual/Scheduled | Read | Write | ✅ ALL | Write | Write |

### Secret Categorization

**Build Secrets** (PR Context: ❌ Blocked)
- `GITHUB_TOKEN` (provided automatically)
- Package registry tokens (GitHub Packages)

**Deployment Secrets** (PR Context: ❌ Blocked)
- `DOCKER_USERNAME`, `DOCKER_PASSWORD`
- Cloud provider credentials
- Application deployment keys

**Notification Secrets** (PR Context: ❌ Blocked)
- `SLACK_BOT_TOKEN`
- Email service credentials
- External monitoring tokens

**Development Secrets** (PR Context: ❌ Blocked)
- Third-party API tokens
- Testing service credentials
- External integration keys

## Security Monitoring and Auditing

### Security Scanning Integration

**Current Implementation:**
```yaml
# Semgrep security scanning (replaced SonarQube)
semgrep:
  uses: ./.github/workflows/cicd_comp_semgrep-phase.yml
  with:
    generate-sarif: true
    fail-on-findings: true
```

**Security Scan Types:**
- **SAST**: Static Application Security Testing (Semgrep)
- **Dependency Scanning**: Vulnerable dependency detection
- **Secret Scanning**: GitHub secret scanning (enabled)
- **Container Scanning**: Docker image vulnerability scanning

### Audit Trail Requirements

**GitHub Actions Audit:**
- All workflow runs are logged
- Secret access is audited
- Permission changes are tracked
- Action executions are recorded

**Security Monitoring:**
- Failed authentication attempts
- Unusual workflow patterns
- Secret access anomalies
- Permission escalation attempts

## Security Incident Response

### Immediate Response

**If security incident detected:**
1. **Disable affected workflows** immediately
2. **Rotate compromised secrets** 
3. **Review audit logs** for impact assessment
4. **Notify security team** via established channels

### Investigation Procedure

**Evidence Collection:**
```bash
# Collect workflow run logs
gh run view <run-id> --log

# Review audit logs
gh api repos/owner/repo/events

# Check secret access patterns
gh api repos/owner/repo/actions/secrets
```

**Impact Assessment:**
- Determine scope of compromise
- Identify affected systems
- Assess data exposure risk
- Evaluate timeline and actors

## Security Compliance

### Regulatory Requirements

**SOC 2 Compliance:**
- Access controls and monitoring
- Incident response procedures
- Audit trail maintenance
- Security awareness training

**GDPR Considerations:**
- Data processing limitations
- Access controls for personal data
- Incident notification requirements
- Data retention policies

### Internal Security Policies

**Code Review Requirements:**
- All workflow changes require review
- Security-focused review for sensitive workflows
- Approval required for permission changes
- Documentation updates required

**Security Training:**
- GitHub Actions security best practices
- Incident response procedures
- Secure coding practices
- Threat awareness training

## Future Security Improvements

### High-Priority Security Enhancements

**1. Permission Hardening (Critical)**
- Implement explicit permissions for all 37 workflows using defaults
- Create permission templates for common workflow patterns
- Establish permission review requirements

**2. Action Pinning (Critical)**
- Replace `ad-m/github-push-action@master` with pinned version
- Implement SHA pinning for all third-party actions
- Create action security review process

**3. Input Validation (High)**
- Implement systematic input validation patterns
- Create validation utilities for common input types
- Establish input sanitization requirements

**4. Supply Chain Security (Medium)**
- Implement Dependabot for action updates
- Create approved action registry
- Establish action security scanning

**5. Monitoring and Alerting (Medium)**
- Implement security monitoring for workflow anomalies
- Create alerting for permission changes
- Establish incident response automation

### Long-Term Security Vision

**Zero-Trust Architecture:**
- Comprehensive permission boundaries
- Continuous security validation
- Automated threat detection
- Incident response automation

**Security-First Development:**
- Security-by-design for all workflows
- Automated security testing
- Continuous compliance monitoring
- Security-aware development practices

## Security Checklist for Developers

### Before Creating/Modifying Workflows

- [ ] **Review security implications** of changes
- [ ] **Identify all user inputs** and validate them
- [ ] **Ensure no secrets** are exposed in PR workflows
- [ ] **Verify minimal permissions** are used
- [ ] **Check that actions** are pinned to specific versions

### During Development

- [ ] **Use environment variables** for sensitive data
- [ ] **Implement proper input validation**
- [ ] **Use jq for JSON processing**
- [ ] **Avoid direct shell injection**
- [ ] **Test with minimal permissions**

### Before Deployment

- [ ] **Review code** for security vulnerabilities
- [ ] **Verify secret handling** is correct
- [ ] **Check artifact security** settings
- [ ] **Validate permission** configurations
- [ ] **Test in fork repository** if making significant changes

### Security Review Questions

1. **Secret Access**: Does this workflow need access to secrets?
2. **Permission Scope**: What's the minimum permission required?
3. **Input Validation**: Are all inputs properly validated?
4. **Action Security**: Are all actions from trusted sources and properly pinned?
5. **Attack Surface**: What could an attacker do with this workflow?

## Security Contact Information

**For Security Incidents:**
- **Internal**: #security-incidents Slack channel
- **External**: security@dotcms.com
- **Emergency**: Follow incident response procedures

**For Security Questions:**
- **Internal**: #guild-dev-pipeline Slack channel
- **External**: GitHub Issues with security label
- **Architecture**: #architecture-guild Slack channel

## Security Resources

- **GitHub Security Documentation**: https://docs.github.com/en/actions/security-guides
- **Action Security Best Practices**: https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions
- **OWASP CI/CD Security**: https://owasp.org/www-project-devsecops-guideline/
- **dotCMS Security Policies**: Internal security documentation