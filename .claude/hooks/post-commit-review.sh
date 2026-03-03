#!/usr/bin/env bash
# post-commit-review.sh
# PostToolUse hook: fires after every Bash tool call.
# If the command was a successful git commit touching frontend files,
# outputs a lightweight reviewer recommendation to Claude's context.

set -euo pipefail

INPUT=$(cat)

# Extract command and tool response from Claude Code's hook JSON
COMMAND=$(echo "$INPUT" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_input', {}).get('command', ''))
except Exception:
    print('')
" 2>/dev/null || true)

OUTPUT=$(echo "$INPUT" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    resp = d.get('tool_response', '')
    if isinstance(resp, dict):
        print(resp.get('output', ''))
    else:
        print(str(resp))
except Exception:
    print('')
" 2>/dev/null || true)

# Only act on git commit commands
if ! echo "$COMMAND" | grep -qE "git commit"; then
    exit 0
fi

# Only act on successful commits (output contains a commit hash line like [branch abc1234])
if ! echo "$OUTPUT" | grep -qE "\[[a-zA-Z0-9_/.-]+ [a-f0-9]{7}\]"; then
    exit 0
fi

# Get frontend files from the last commit
FRONTEND_FILES=$(git diff --name-only HEAD~1 HEAD 2>/dev/null \
    | grep -E "^core-web/.*\.(ts|html|scss|css)$" \
    | grep -v "\.spec\.ts$" \
    || true)

SPEC_FILES=$(git diff --name-only HEAD~1 HEAD 2>/dev/null \
    | grep -E "^core-web/.*\.spec\.ts$" \
    || true)

# If no frontend files were committed, stay silent
if [ -z "$FRONTEND_FILES" ] && [ -z "$SPEC_FILES" ]; then
    exit 0
fi

# Build reviewer recommendations
REVIEWERS=()

if echo "$FRONTEND_FILES" | grep -qE "\.(ts)$"; then
    REVIEWERS+=("dotcms-typescript-reviewer")
fi
if echo "$FRONTEND_FILES" | grep -qE "\.(ts|html)$"; then
    REVIEWERS+=("dotcms-angular-reviewer")
fi
if [ -n "$SPEC_FILES" ]; then
    REVIEWERS+=("dotcms-test-reviewer")
fi
if echo "$FRONTEND_FILES" | grep -qE "\.(scss|css|html)$"; then
    REVIEWERS+=("dotcms-scss-html-style-reviewer")
fi

FILE_COUNT=$(echo "$FRONTEND_FILES $SPEC_FILES" | wc -w | tr -d ' ')
REVIEWER_LIST=$(IFS=", "; echo "${REVIEWERS[*]}")

cat <<EOF
[Post-commit] Frontend commit detected ($FILE_COUNT files). Recommended reviewers: $REVIEWER_LIST.
Run \`/review\` on the resulting PR, or ask me to run the reviewers on the committed files now.
EOF

exit 0
