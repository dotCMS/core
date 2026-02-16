# Legacy Workflow Migration Guide

## Overview

This document tracks the deprecation and migration of legacy release workflows to the new modular CI/CD pipeline architecture.

## Summary of Changes (February 2026)

### ✅ New Workflow Created
- **`cicd_8-manual-deploy.yml`** - Manual deployment from any branch/tag
  - Replaces: `legacy-release_publish-dotcms-docker-image.yml`
  - Purpose: On-demand Docker image deployment for testing feature branches
  - Status: **READY FOR USE**

### ⚠️ Workflows Deprecated (Active Migration)
These workflows are deprecated but still functional during migration period:

| Workflow | Status | Replacement | Timeline |
|----------|--------|-------------|----------|
| `legacy-release_maven-release-process.yml` | Last used Jan 30, 2026 | `cicd_6-release.yml` | Remove after 2+ stable releases |
| `legacy-release_publish-dotcms-docker-image.yml` | Used TODAY (Feb 16) | `cicd_8-manual-deploy.yml` | Remove after 2-week validation |
| `legacy-release_comp_maven-build-docker-image.yml` | Internal component | Modular phases | Remove when dependencies gone |

### 🗑️ Workflows Safe to Delete (No Recent Usage)
These workflows have no recent usage and can be deleted immediately:

| Workflow | Last Used | Usage Count | Safe to Delete |
|----------|-----------|-------------|----------------|
| `legacy-release_release-trigger.yml` | May 26, 2025 (9 months ago) | 6 total uses | ✅ YES |
| `legacy-release_publish-docker-image-on-release.yml` | Never | 0 uses | ✅ YES |
| `legacy-release_release-candidate.yml` | Never | 0 uses | ✅ YES |

## Migration Paths

### For Manual Docker Deployments

**Old workflow:** `legacy-release_publish-dotcms-docker-image.yml`

```yaml
# Old way - manual trigger, select branch in UI
Workflow: Build/Push dotCMS docker image
Inputs:
  - multi_arch: true/false
  - docker_registry: DOCKER.IO/GHCR.IO/BOTH
  - custom_tag: optional
```

**New workflow:** `cicd_8-manual-deploy.yml`

```yaml
# New way - manual trigger with explicit inputs and safety checks
Workflow: -8 Manual Deploy
Inputs:
  - ref: branch-name-or-tag               # e.g., issue-123-feature, release-25.01.01
  - environment: categorical-name         # GitHub UI grouping: manual, feature-test, hotfix, release-java25
  - version: unique-version-name          # Artifact naming: feature-test-123, manual-20240216
  - deploy_dev_image: true/false
  - latest: true/false
  - custom_tag: optional                  # Additional Docker tag
  - java_version: optional                # e.g., 25.0.2-ms
  - artifact_suffix: optional (default: manual)  # Maven namespace: java25-ms, manual
  - allow_release_override: false         # Safety flag - blocks release version patterns
```

**Benefits:**
- ✅ Reuses modular build/deployment phases
- ✅ Consistent with modern pipeline architecture
- ✅ Supports Java version overrides
- ✅ Better artifact management
- ✅ **Safety checks prevent accidental artifact collision**
- ✅ **Separate environment (GitHub UI) and version (artifacts) naming**
- ✅ **Artifact suffix default "manual" prevents Maven/Artifactory namespace collision**

### For Release Process

**Old workflow:** `legacy-release_maven-release-process.yml`

**New workflow:** `cicd_6-release.yml`

```yaml
# New modular release process
Workflow: -6 Release Process
Phases:
  1. Initialize
  2. Release Prepare (branch creation, versioning)
  3. Build
  4. Deployment (Docker images)
  5. Release (Artifactory, Javadocs, Plugins)
  6. Finalize

Java Variants:
  - Automatic parallel builds via cicd_7-release-java-variant.yml
  - Triggered automatically on release branch creation
```

**Benefits:**
- ✅ All legacy functionality preserved
- ✅ Modular phases (reusable, maintainable)
- ✅ Parallel Java variant builds
- ✅ Better error handling and observability
- ✅ Improved artifact management

## Usage Analysis (Based on Actual Runs)

### Active Workflows (February 2026)
```
cicd_6-release.yml:
  - Feb 16, 2026: success (production release)
  - Feb 11, 2026: success
  - Feb 09, 2026: success
  Status: PRIMARY release workflow

legacy-release_maven-release-process.yml:
  - Jan 30, 2026: success (last use)
  - Jan 26, 2026: success
  - 10 runs in January 2026
  Status: Being phased out

legacy-release_publish-dotcms-docker-image.yml:
  - Feb 16, 2026: success (release-24.12.27_lts)
  - Feb 16, 2026: failures (issue-32937 testing)
  - Feb 11-13, 2026: Multiple feature branch tests
  - 10 runs in 6 days
  Status: HEAVILY USED for feature testing
```

### Obsolete Workflows
```
legacy-release_release-trigger.yml:
  - Last used: May 26, 2025 (9 months ago)
  - Total uses: 6 (all in 2025)

legacy-release_publish-docker-image-on-release.yml:
  - Never used

legacy-release_release-candidate.yml:
  - Never used
```

## Recommended Actions

### Phase 1: Immediate (Now)
1. ✅ **CREATED**: `cicd_8-manual-deploy.yml` workflow
2. ✅ **ADDED**: Deprecation notices to all legacy workflows
3. 🔄 **COMMUNICATE**: Notify team about new `cicd_8-manual-deploy.yml` workflow
4. 🔄 **DOCUMENT**: Update runbooks/docs to reference new workflows

### Phase 2: Safe Deletions (This Week)
Delete workflows with no recent usage:
```bash
git rm .github/workflows/legacy-release_release-trigger.yml
git rm .github/workflows/legacy-release_publish-docker-image-on-release.yml
git rm .github/workflows/legacy-release_release-candidate.yml
```

**Impact:** None - these workflows haven't been used

### Phase 3: Validation Period (2 Weeks)
1. Team validates `cicd_8-manual-deploy.yml` works for all use cases
2. Monitor usage patterns via workflow runs
3. Collect feedback on any missing functionality
4. Document any edge cases or special requirements

### Phase 4: Deprecation Enforcement (March 2026)
After 2-week validation with no issues:
```bash
git rm .github/workflows/legacy-release_publish-dotcms-docker-image.yml
```

**Impact:** None - replacement workflow validated

### Phase 5: Final Cleanup (After 2+ Stable Releases)
Once `cicd_6-release.yml` has been used for 2+ production releases successfully:
```bash
git rm .github/workflows/legacy-release_maven-release-process.yml
git rm .github/workflows/legacy-release_comp_maven-build-docker-image.yml
```

**Impact:** None - legacy release process no longer needed

## Migration Checklist

- [x] Create `cicd_8-manual-deploy.yml` workflow
- [x] Add deprecation notices to legacy workflows
- [ ] Communicate new workflow to team
- [ ] Update documentation/runbooks
- [ ] Delete unused workflows (Phase 2)
- [ ] Validate `cicd_8-manual-deploy.yml` for 2 weeks
- [ ] Delete `legacy-release_publish-dotcms-docker-image.yml` (after validation)
- [ ] Monitor `cicd_6-release.yml` for 2+ stable releases
- [ ] Delete remaining legacy workflows (final cleanup)

## Support

For questions or issues during migration:
- Review workflow documentation in `.github/workflows/` headers
- Check workflow run history in GitHub Actions
- Consult this migration guide for timeline and alternatives

## Testing the New Manual Deploy Workflow

### Test Case 1: Feature Branch Deploy
```yaml
Workflow: cicd_8-manual-deploy.yml
Inputs:
  ref: issue-12345-new-feature
  environment: feature-test                # GitHub UI grouping
  version: feature-test-12345              # Unique version for artifacts
  deploy_dev_image: true
  custom_tag: qa-env-1
  artifact_suffix: manual                  # (default - prevents collision)
```

Expected outputs:
- Docker tag: `dotcms/dotcms:feature-test-12345_manual`
- Custom tag: `dotcms/dotcms:qa-env-1`
- Dev image: `dotcms/dotcms-dev:feature-test-12345_manual`
- Maven artifacts: Namespaced with `-manual` suffix
- GitHub environment: "feature-test" (groups all feature tests together)

### Test Case 2: Hotfix Deploy
```yaml
Workflow: cicd_8-manual-deploy.yml
Inputs:
  ref: release-25.01.01
  environment: hotfix                      # GitHub UI grouping
  version: hotfix-25.01.01-security        # Unique version (NOT actual release version!)
  deploy_dev_image: false
  custom_tag: customer-emergency-patch
  artifact_suffix: hotfix                  # Prevents collision
```

Expected outputs:
- Docker tag: `dotcms/dotcms:hotfix-25.01.01-security_hotfix`
- Custom tag: `dotcms/dotcms:customer-emergency-patch`
- Maven artifacts: Namespaced with `-hotfix` suffix
- GitHub environment: "hotfix" (groups all hotfixes together)
- Safety check: ✅ PASS (version doesn't match release pattern due to "hotfix-" prefix)

### Test Case 3: Java 25 Testing
```yaml
Workflow: cicd_8-manual-deploy.yml
Inputs:
  ref: main
  environment: release-java25              # GitHub UI grouping for Java variants
  version: test-java25                     # Unique test version
  java_version: 25.0.2-ms
  artifact_suffix: java25-ms               # Explicit suffix for Java 25
  deploy_dev_image: true
```

Expected outputs:
- Docker tag: `dotcms/dotcms:test-java25_java25-ms`
- Dev image: `dotcms/dotcms-dev:test-java25_java25-ms`
- Maven artifacts: Namespaced with `-java25-ms` suffix
- GitHub environment: "release-java25" (groups all Java 25 builds together)
- Built with Java 25.0.2-ms

### Test Case 4: Safety Check - Release Version Pattern (BLOCKED)
```yaml
Workflow: cicd_8-manual-deploy.yml
Inputs:
  ref: main
  environment: manual
  version: 25.01.01-01                     # ❌ Matches release pattern!
  artifact_suffix: manual
  allow_release_override: false            # (default)
```

Expected outcome:
- ❌ **BLOCKED** by safety check
- Error message: "Version '25.01.01-01' matches release version pattern! This could overwrite actual release artifacts."
- Suggested fix: Use "25.01.01-01-test" or "manual-25.01.01-01" instead

### Test Case 5: Intentional Release Override (DANGEROUS)
```yaml
Workflow: cicd_8-manual-deploy.yml
Inputs:
  ref: release-25.01.01
  environment: release                     # GitHub UI grouping
  version: 25.01.01-01                     # Release version pattern
  artifact_suffix: manual                  # Still has suffix for safety
  allow_release_override: true             # ⚠️ EXPLICIT OVERRIDE
```

Expected outcome:
- ⚠️ **WARNING** but allowed to proceed
- Warning: "allow_release_override is true - proceeding with release version pattern. This may overwrite actual release artifacts!"
- Maven artifacts still namespaced with `-manual` suffix (safe)
- Use case: Rebuilding a specific release with modifications for debugging

## Rollback Plan

If issues are discovered with new workflows:
1. Legacy workflows remain functional during deprecation period
2. Simply use legacy workflow until issue is resolved
3. Report issue via GitHub issue tracker
4. Legacy workflows will not be deleted until new workflows are proven stable

## Timeline Summary

| Date | Action |
|------|--------|
| Feb 16, 2026 | New `cicd_8-manual-deploy.yml` created |
| Feb 16, 2026 | Deprecation notices added |
| Feb 16-23, 2026 | Team notification and documentation updates |
| Feb 23, 2026 | Delete unused workflows (Phase 2) |
| Feb 16 - Mar 1, 2026 | 2-week validation period for new manual deploy |
| Mar 1, 2026 | Delete `legacy-release_publish-dotcms-docker-image.yml` |
| TBD (after 2+ releases) | Delete remaining legacy release workflows |
