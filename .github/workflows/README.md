# dotCMS CI/CD Workflows - Getting Started

Welcome to the dotCMS CI/CD documentation! This guide will help you understand and work with our GitHub Actions workflows.

## 📚 Documentation Index

- **[WORKFLOW_ARCHITECTURE.md](WORKFLOW_ARCHITECTURE.md)** - **START HERE!** 
  - Complete architecture with Mermaid diagrams
  - All workflows, phases, actions, and their relationships
  - Troubleshooting guide and common issues
  - Performance optimization tips

- **[maven-release-process.md](maven-release-process.md)** - Release How-To
  - Step-by-step release instructions
  - Field explanations with screenshots
  - Post-release verification

- **[test-matrix.yml](../.github/test-matrix.yml)** - Test Configuration
  - All test suite definitions
  - DRY test configuration reference

- **[filters.yaml](../.github/filters.yaml)** - Change Detection
  - Path-based filters for conditional testing

## 🚀 Quick Start

### For Developers

**Understanding Workflow Failures:**
1. Check the failed workflow run in GitHub Actions
2. Look for the failed phase (Initialize, Build, Test, etc.)
3. Review logs for specific errors
4. See [Troubleshooting Guide](WORKFLOW_ARCHITECTURE.md#troubleshooting-guide) for common issues

**Creating a PR:**
- Your PR triggers `cicd_1-pr.yml` automatically
- Only runs tests for changed components (via filters)
- Must pass before merge queue entry
- See [PR Check Flow](WORKFLOW_ARCHITECTURE.md#1-pr-check-workflow-cicd_1-pryml) diagram

**Merging a PR:**
- Enters merge queue → `cicd_2-merge-queue.yml`
- Runs ALL tests to catch flaky issues
- Success → auto-merge to main
- Failure blocks all PRs behind it in queue

### For Release Managers

**Triggering a Release:**
1. Navigate to Actions → `-6 Release Process`
2. Click "Run workflow"
3. Enter release version (e.g., `24.12.31-01`)
4. Configure options (usually keep defaults)
5. Monitor progress in workflow run

See [How to Trigger a Release](WORKFLOW_ARCHITECTURE.md#how-to-trigger-a-release) for details.

### For DevOps/Maintainers

**Adding a New Test Suite:**
1. Update `test-matrix.yml` with new configuration
2. No workflow changes needed!
3. Matrix auto-generates and parallelizes

**Modifying Workflows:**
1. Check if change belongs in a reusable phase
2. Test in PR workflow first
3. Document changes in this README

See [Maintenance Guide](WORKFLOW_ARCHITECTURE.md#maintenance) for more.

## 📊 Workflow Overview

### Main CI/CD Pipeline (6 Workflows)

| # | Workflow | Trigger | Purpose |
|---|----------|---------|---------|
| 1 | **PR Check** | PR opened/updated | Fast validation of changes |
| 2 | **Merge Queue** | PR ready to merge | Comprehensive testing |
| 3 | **Trunk** | Push to main | Deploy snapshots, build CLI |
| 4 | **Nightly** | 3:18 AM daily | Comprehensive validation |
| 5 | **LTS** | Push to release-* | LTS branch validation |
| 6 | **Release** | Manual trigger | Official releases |

### Standard Phase Pattern

All workflows follow this pattern:
```
Initialize → Build → Test → (Semgrep) → (CLI Build) → (Deploy) → Finalize → Report
```

See [Architecture Diagram](WORKFLOW_ARCHITECTURE.md#architecture-diagram) for complete relationships.

## 🎯 Table of Contents

1.  [File Structure](#file-structure)
2.  [Critical Information](#critical-information)
3.  [Architecture Overview](#architecture-overview)
4.  [Top-Level Workflows](#top-level-workflows)
5.  [Reusable Workflow Phases](#reusable-workflow-phases)
6.  [Custom Actions](#custom-actions)
7.  [Workflow Configurations](#workflow-configurations)
8.  [Benefits of Our Approach](#benefits-of-our-approach)

## File Structure

GitHub only allows workflows in `.github/workflows/` (no subfolders). We use a folder-like naming convention:

**Format**: `category_subcategory_name.yml`
- Example: `cicd/comp/build-phase.yml` → `cicd_comp_build-phase.yml`

**Prefixes**:
- **Numbers (1-6)**: Main CICD workflows in PR progression order
- **Dash prefix (-)**: Ensures top placement in GitHub UI (e.g., `-1 PR Check`)

**Actions**: Can use subfolders (`.github/actions/category/action-name/`)

See [File Naming Convention](WORKFLOW_ARCHITECTURE.md#file-naming-convention) for details.

## Critical Information

⚠️ **Security Rules**:
- PR workflows run on **untrusted code** - never use secrets
- Secrets unavailable for fork PRs
- Use `cicd_post-workflow-reporting.yml` for notifications requiring secrets

⚠️ **Job Naming Rules**:
- First job: `"Initialize / Initialize"` 
- Last job: `"Finalize / Final Status"`
- These names signal workflow status to GitHub Checks
- Changing them breaks check detection (workflows hang until timeout)

⚠️ **Workflow Design**:
- Don't create duplicate workflows for same trigger
- Extend existing workflows instead
- Leverage reusable phase workflows
- Follow the standard phase pattern 

⚠️ **Semgrep Gating** (see [private-issues#630](https://github.com/dotCMS/private-issues/issues/630)):
- Semgrep blocks merges **via `Finalize / Final Status` on the PR workflow**, not via a required status check
- The `semgrep` job in `cicd_1-pr.yml` runs `semgrep ci` and is wired into `finalize: needs: [ semgrep, test ]` — a failing scan turns `Finalize / Final Status` red, which blocks the PR from entering the merge queue
- Controlled by repo variables: `DISABLE_SEMGREP` (skips the job entirely) and `SEMGREP_NO_FAIL` (runs the scan but ignores failures) — both must be `false` for the gate to be active
- **NEVER add `semgrep-cloud-platform/scan` to required status checks** (branch protection or rulesets): the Semgrep Cloud app only posts that check on `pull_request` events, never on `merge_group` commits, so requiring it makes the merge queue time out and dequeue every PR (incident of 2026-05-19)
- The disabled ruleset "Default Merge Queue" (id 3651671) still lists `semgrep-cloud-platform/scan` as required — do not re-enable it as-is
- `cicd_2-merge-queue.yml` intentionally has **no** semgrep job: the queue never waits on Semgrep

## Architecture Overview

Our CI/CD uses a **three-tier architecture** for modularity and maintainability:

```
┌─────────────────────────────────────────────────────┐
│  Top-Level Workflows (6)                             │
│  cicd_1-pr.yml, cicd_2-merge-queue.yml, etc.       │
│  • Define triggers and orchestration                 │
│  • Call reusable phases                              │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│  Reusable Phase Workflows (10)                       │
│  Initialize, Build, Test, Deploy, etc.              │
│  • Shared business logic                             │
│  • Used by multiple top-level workflows             │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│  Composite Actions (15+)                             │
│  maven-job, setup-java, deploy-docker, etc.        │
│  • Atomic operations                                 │
│  • Reusable across all workflows                    │
└─────────────────────────────────────────────────────┘
```

**Benefits**: 70% less code duplication, consistent behavior, easier maintenance.

See [Architecture Diagram](WORKFLOW_ARCHITECTURE.md#architecture-diagram) for complete visualization.

## Top-Level Workflows

Located in `.github/workflows/cicd_*.yml`:

| Workflow | Trigger | Duration | Key Features |
|----------|---------|----------|--------------|
| **1-PR** | PR open/update | 15-25 min | Selective tests, no secrets, fast feedback |
| **2-Merge Queue** | Ready to merge | 30-45 min | ALL tests, catches flaky tests |
| **3-Trunk** | Push to main | 20-30 min | Artifact reuse, CLI builds, snapshots |
| **4-Nightly** | 3:18 AM daily | 45-60 min | Trunk health monitor, early breakage detection |
| **5-LTS** | Push to release-* | 30-45 min | LTS branch validation |
| **6-Release** | Manual | 25-35 min | Production release, full deployment |

Each orchestrates the process by calling reusable phases and actions.

See [Workflow Configurations](WORKFLOW_ARCHITECTURE.md#workflow-configurations-path-to-main-branch) for detailed comparison.

## Reusable Workflow Phases

Located in `.github/workflows/cicd_comp_*-phase.yml`:

| Phase | Purpose | Outputs |
|-------|---------|---------|
| **Initialize** | Detect changes, check for reusable artifacts | `found_artifacts`, `backend`, `frontend`, `build` |
| **Build** | Compile code, generate artifacts | `maven-repo` artifact |
| **Test** | Matrix-driven parallel test execution | Test results, build reports |
| **Semgrep** | Security and code quality scanning | Quality gate status |
| **CLI Build** | Multi-platform native CLI builds | CLI artifacts (Linux, macOS x2) |
| **Deployment** | Docker images, NPM packages, Artifactory | Docker tags, NPM versions |
| **Release Prepare** | Version validation, branch creation | Release version, tag, branch |
| **Release** | Artifactory, Javadocs, SBOM, labels | Release artifacts |
| **Finalize** | Aggregate results, determine status | `aggregate_status` |
| **Reporting** | Generate reports, send notifications | Slack messages, test reports |

**Key Principle**: Each phase can be independently configured and reused across different workflows.

See [Detailed Flow Diagrams](WORKFLOW_ARCHITECTURE.md#detailed-flow-diagrams) for visual representation.

## Custom Actions

Located in `.github/actions/`:

### Core CI/CD Actions
- **maven-job**: Standardized Maven execution with caching, artifact handling
- **setup-java**: Java & GraalVM installation (supports multiple versions)
- **prepare-runner**: Pre-build environment setup
- **cleanup-runner**: Free disk space (critical for large builds)
- **api-limits-check**: Monitor GitHub API rate limits

### Deployment Actions  
- **deploy-docker**: Multi-platform Docker builds and pushes
- **deploy-jfrog**: Artifactory deployments
- **deploy-cli-npm**: CLI NPM package publishing
- **deploy-javadoc**: S3 javadoc uploads
- **deploy-javascript-sdk**: SDK NPM publishing

### Notification & Support
- **notify-slack**: Slack message formatting and posting
- **issue-fetcher**: Fetch and parse issue details
- **issue-labeler**: Label management automation

See [Key Actions](WORKFLOW_ARCHITECTURE.md#key-actions) for complete reference.

## Workflow Configurations

Each workflow has specific optimizations and purposes:

### 1-PR (Pull Request Validation)
**Optimization**: Speed & Safety
- ✅ Selective testing (filters.yaml determines what runs)
- ❌ No secrets (unreviewed code)
- ⚡ Fast feedback (15-25 min typical)
- 📊 Post-workflow reporting (separate workflow with secrets)

### 2-Merge Queue (Pre-Merge Validation)  
**Optimization**: Comprehensive Testing
- ✅ ALL tests run (catches flaky tests)
- ✅ Tests combined code (includes PRs ahead in queue)
- ⚠️ Failures block all developers (monitor closely!)
- 🔒 Commit SHA matches future main HEAD

### 3-Trunk (Post-Merge Deployment)
**Optimization**: Artifact Reuse
- ♻️ Reuses merge queue artifacts (saves 5-10 min)
- 🔨 Native CLI builds (3 platforms)
- 📦 Snapshot deployments (GitHub, Artifactory)
- 📚 Optional SDK publishing

### 4-Nightly (Trunk Health Monitor)
**Optimization**: Early Problem Detection  
- 🩺 Monitors trunk health (NOT release gate)
- 🚨 Catches breakage before changes accumulate
- 🎯 Critical for CI success & release frequency
- 🌙 Runs at 3:18 AM daily
- 🧪 Long-running tests (impractical for PRs)

### 5-LTS (LTS Branch Validation)
**Optimization**: Long-Term Support
- 🔖 Triggered on release-* branches
- ✅ Full test suite
- 📋 Version-specific configuration

### 6-Release (Production Release)
**Optimization**: Complete Deployment
- 🚀 Manual trigger only
- 📦 Full artifact deployment
- 🏷️ GitHub label management
- 📝 Complete documentation

See [Workflow Configurations Table](WORKFLOW_ARCHITECTURE.md#workflow-configurations-path-to-main-branch) for detailed comparison.

## Release Promotion Strategy

**Goal**: Keep main branch always releasable while allowing thorough validation before official releases.

**Philosophy**: 
- PR validation prevents unreleasable commits
- Additional testing happens without blocking development  
- Promotion branches get version changes only (minimal, reproducible)
- Fixes flow through normal PR process (no cherry-picking)

**Release Promotion Flow**:
```
PR → Merge Queue → Main → Manual QA/Smoke Testing (Required) → RC → Release
```

**Trunk Health Monitoring** (Parallel, Not in Promotion Flow):
```
Main → Nightly Tests (3:18 AM) → Alert on Failures
```

**Key Points:**
- **Release Path**: Manual QA/Smoke testing is **always required** before RC
- **Nightly Tests**: Legacy workflow, **NOT part of release promotion**
  - Purpose: Early detection of trunk breakage
  - Critical: Prevents change accumulation (easier debugging)
  - Result: Enabled increased release frequency over recent years
- Each release promotion step increases confidence without blocking development

See [Release Promotion Process](WORKFLOW_ARCHITECTURE.md#release-promotion-process) for detailed diagrams and examples.


## Benefits of Our Approach

### Key Advantages

1. **Modularity** 📦
   - 70% reduction in code duplication
   - Single source of truth for each phase
   - Easy to maintain and extend

2. **Performance** ⚡
   - 40-50% faster PR checks (15-25 min vs 45 min)
   - Parallel test execution (30 min vs 180 min)
   - Artifact reuse saves 5-10 min per workflow

3. **Cost Efficiency** 💰
   - 62% savings on macOS runners ($300/mo vs $800/mo)
   - Strategic runner selection
   - Conditional testing reduces waste

4. **Reliability** 🛡️
   - Consistent behavior across all workflows
   - Catch flaky tests in merge queue
   - Comprehensive error reporting

5. **Developer Experience** 👨‍💻
   - Fast feedback loops
   - Clear failure messages
   - Detailed troubleshooting guides

See [Why This Architecture?](WORKFLOW_ARCHITECTURE.md#why-this-architecture) for detailed metrics and real-world impact.

## Quick Reference

**Common Tasks**:
- 🐛 Debugging failures → [Troubleshooting Guide](WORKFLOW_ARCHITECTURE.md#troubleshooting-guide)
- 🚀 Triggering releases → [How to Trigger a Release](WORKFLOW_ARCHITECTURE.md#how-to-trigger-a-release)
- ➕ Adding tests → [Adding New Tests](WORKFLOW_ARCHITECTURE.md#adding-new-tests)
- 📊 Understanding flows → [Detailed Flow Diagrams](WORKFLOW_ARCHITECTURE.md#detailed-flow-diagrams)

**Need Help?**
- 📖 Full documentation → [WORKFLOW_ARCHITECTURE.md](WORKFLOW_ARCHITECTURE.md)
- 🔧 Release guide → [maven-release-process.md](maven-release-process.md)
- 💬 Questions → #devops on Slack

---

**Last Updated**: December 2024  
**Maintained By**: dotCMS DevOps Team