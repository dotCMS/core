#!/usr/bin/env bash
set -euo pipefail

#
# Creates a test harness in the current GitHub repo for validating
# the AI release notes pipeline end-to-end.
#
# What it creates:
#   - 5 PRs with specific labels (feature, bug, rollback-unsafe, skip, infrastructure)
#   - 2 standard releases (v26.03.17-01 after PRs 1-3, v26.03.17-02 after PRs 4-5)
#   - 1 LTS-style tag (v26.03.17_lts_v01) for filter testing
#   - 1 CLI-style tag (dotcms-cli-26.03.17-01) for filter testing
#
# Prerequisites:
#   - gh CLI authenticated with repo write access
#   - git configured with push access to origin
#   - Run from the repo root
#
# Usage:
#   .github/scripts/gather-release-data/test/create-test-harness.sh [--repo owner/repo]
#
# To tear down:
#   .github/scripts/gather-release-data/test/create-test-harness.sh --cleanup [--repo owner/repo]
#

REPO="${REPO:-}"
CLEANUP=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo) REPO="$2"; shift 2 ;;
    --cleanup) CLEANUP=true; shift ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done

if [[ -z "$REPO" ]]; then
  REPO=$(gh repo view --json nameWithOwner -q '.nameWithOwner')
fi

echo "Target repo: $REPO"

# ─── Cleanup mode ───────────────────────────────────────────────────────────

if $CLEANUP; then
  echo "Cleaning up test harness..."
  for tag in v26.03.17-01 v26.03.17-02 v26.03.17_lts_v01 dotcms-cli-26.03.17-01; do
    echo "  Deleting release $tag..."
    gh release delete "$tag" --repo "$REPO" --yes --cleanup-tag 2>/dev/null || true
  done
  for branch in test/feature-widget-api test/fix-cache-collision test/schema-migration test/internal-tooling test/infra-docker-upgrade; do
    echo "  Deleting branch $branch..."
    git push origin --delete "$branch" 2>/dev/null || true
  done
  echo "Cleanup complete. Note: merged PRs cannot be deleted — close manually if needed."
  exit 0
fi

# ─── Ensure required labels exist ───────────────────────────────────────────

ensure_label() {
  local name="$1" color="$2" desc="${3:-}"
  if ! gh label list --repo "$REPO" --json name -q ".[].name" | grep -qxF "$name"; then
    echo "  Creating label: $name"
    gh label create "$name" --repo "$REPO" --color "$color" --description "$desc"
  fi
}

echo "Ensuring labels..."
ensure_label "feature"                     "0E8A16" "New feature"
ensure_label "bug"                         "D73A4A" "Bug report"
ensure_label "infrastructure"              "D4C5F9" "Infrastructure change"
ensure_label "Changelog: Skip"             "BFDADC" "Omit from changelog"
ensure_label "AI: Not Safe To Rollback"    "B60205" "AI-detected rollback risk"
ensure_label "Human: Not Safe To Rollback" "B60205" "Human-confirmed rollback risk"

# ─── Helper: create branch, commit, push, open PR, merge ───────────────────

create_and_merge_pr() {
  local branch="$1" file="$2" content="$3" commit_msg="$4" pr_title="$5" pr_body="$6" labels="$7"

  echo ""
  echo "Creating PR: $pr_title"

  git checkout main && git pull origin main --quiet

  git checkout -b "$branch"
  mkdir -p "$(dirname "$file")"
  echo "$content" > "$file"
  git add "$file"
  git commit -m "$commit_msg" --quiet

  git push origin "$branch" --quiet 2>/dev/null

  PR_URL=$(gh pr create --repo "$REPO" \
    --base main --head "$branch" \
    --title "$pr_title" \
    --body "$pr_body" \
    --label "$labels")

  PR_NUM=$(echo "$PR_URL" | grep -oE '[0-9]+$')
  echo "  Created PR #$PR_NUM — merging..."

  gh pr merge "$PR_NUM" --repo "$REPO" --squash --admin --delete-branch
  echo "  Merged."
}

# ─── PR 1: Feature enhancement ─────────────────────────────────────────────

create_and_merge_pr \
  "test/feature-widget-api" \
  "dotCMS/src/main/java/com/dotcms/widget/WidgetAPIv2.java" \
  "// Widget API v2 - enhanced rendering pipeline" \
  "$(cat <<'EOF'
feat(widgets): add Widget API v2 with enhanced rendering pipeline

New widget rendering system that supports:
- Server-side rendering for improved performance
- Lazy-loading widget dependencies
- Widget-scoped caching layer
EOF
)" \
  "feat(widgets): add Widget API v2 with enhanced rendering pipeline" \
  "New widget rendering system with SSR, lazy-loading, and widget-scoped caching." \
  "feature,Area : Backend"

# ─── PR 2: Bug fix ─────────────────────────────────────────────────────────

create_and_merge_pr \
  "test/fix-cache-collision" \
  "dotCMS/src/main/java/com/dotcms/cache/PageCacheKeyFix.java" \
  "// Fixed cache key generation to include contentlet inode" \
  "$(cat <<'EOF'
fix(cache): URL-mapped content renders same HTML due to page cache collision

Fixed cache key generation to include contentlet inode, preventing
all URL-mapped contentlets of the same type from sharing a cache entry.
EOF
)" \
  "fix(cache): URL-mapped content renders same HTML due to page cache collision" \
  "Fixed cache key generation to include contentlet inode." \
  "bug,Area : Backend"

# ─── PR 3: Rollback-unsafe (database migration) ────────────────────────────

create_and_merge_pr \
  "test/schema-migration" \
  "dotCMS/src/main/java/com/dotcms/upgrade/Task260317AddLocaleColumn.java" \
  "// ALTER TABLE contentlet ADD COLUMN locale_id VARCHAR(64);" \
  "$(cat <<'EOF'
feat(i18n): add locale_id column to contentlet table

Database migration to add locale_id column replacing the legacy
language_id foreign key. One-way migration - old column preserved
but no longer used by query paths.
EOF
)" \
  "feat(i18n): add locale_id column to contentlet table" \
  "Database migration to add locale_id column. One-way migration that cannot be rolled back safely." \
  "feature,Area : Backend,AI: Not Safe To Rollback"

# ─── Release 1: after PRs 1-3 ──────────────────────────────────────────────

echo ""
echo "Creating release v26.03.17-01..."
git checkout main && git pull origin main --quiet
COMMIT_SHA=$(git rev-parse HEAD)
gh release create v26.03.17-01 \
  --repo "$REPO" \
  --target "$COMMIT_SHA" \
  --title "Release 26.03.17-01" \
  --notes ""
echo "  Created at $COMMIT_SHA"

# ─── PR 4: Changelog: Skip (internal tooling) ──────────────────────────────

create_and_merge_pr \
  "test/internal-tooling" \
  "dotCMS/src/main/java/com/dotcms/internal/CIHelper.java" \
  "// Internal CI helper script - not customer-facing" \
  "$(cat <<'EOF'
chore: add internal CI helper for build matrix generation

Internal tooling only - not customer-facing.
EOF
)" \
  "chore: add internal CI helper for build matrix generation" \
  "Internal tooling only - not customer-facing." \
  "Changelog: Skip,Area : CI/CD"

# ─── PR 5: Infrastructure change ───────────────────────────────────────────

create_and_merge_pr \
  "test/infra-docker-upgrade" \
  "Dockerfile.test" \
  "FROM eclipse-temurin:21-jre-noble" \
  "$(cat <<'EOF'
ci: upgrade base Docker image to eclipse-temurin:21-jre-noble

Upgraded from eclipse-temurin:21-jre-jammy to noble (24.04 LTS)
for latest security patches and OpenSSL 3.x support.
EOF
)" \
  "ci: upgrade base Docker image to eclipse-temurin:21-jre-noble" \
  "Upgraded base image from jammy to noble (24.04 LTS) for security patches." \
  "infrastructure,Area : CI/CD"

# ─── Release 2: after PRs 4-5 ──────────────────────────────────────────────

echo ""
echo "Creating release v26.03.17-02..."
git checkout main && git pull origin main --quiet
COMMIT_SHA=$(git rev-parse HEAD)
gh release create v26.03.17-02 \
  --repo "$REPO" \
  --target "$COMMIT_SHA" \
  --title "Release 26.03.17-02" \
  --notes ""
echo "  Created at $COMMIT_SHA"

# ─── Filter test tags ──────────────────────────────────────────────────────

echo ""
echo "Creating filter test tags..."
gh release create v26.03.17_lts_v01 \
  --repo "$REPO" \
  --target "$COMMIT_SHA" \
  --title "LTS Release 26.03.17_lts_v01" \
  --notes ""

gh release create dotcms-cli-26.03.17-01 \
  --repo "$REPO" \
  --target "$COMMIT_SHA" \
  --title "CLI Release 26.03.17-01" \
  --notes ""

# ─── Summary ───────────────────────────────────────────────────────────────

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "Test harness created successfully!"
echo ""
echo "Releases:"
echo "  v26.03.17-01  — contains PRs 1-3 (feature, bug, rollback-unsafe)"
echo "  v26.03.17-02  — contains PRs 4-5 (skip, infrastructure)"
echo ""
echo "Filter test tags:"
echo "  v26.03.17_lts_v01       — should be skipped by release notes"
echo "  dotcms-cli-26.03.17-01  — should be skipped by release notes"
echo ""
echo "Expected behavior for v26.03.17-01 → v26.03.17-02:"
echo "  - 2 commits, 2 PRs"
echo "  - 1 change (infrastructure: Docker image upgrade)"
echo "  - 1 skipped (Changelog: Skip label)"
echo "  - 0 rollback-unsafe"
echo ""
echo "Expected behavior for v26.02.10-99 → v26.03.17-01:"
echo "  - 3+ PRs (feature, bug, rollback-unsafe)"
echo "  - 1 rollback-unsafe (locale_id migration)"
echo "═══════════════════════════════════════════════════════════════"
