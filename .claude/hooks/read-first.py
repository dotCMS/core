#!/usr/bin/env python3
"""Reminds Claude Code to read the guide behind CLAUDE.md's "Required Reading Before You Edit".

Wired from .claude/settings.json as two hooks:

  PreToolUse  (Edit|Write)  -> `read-first.py pre`
  PostToolUse (Read)        -> `read-first.py post`

`pre` matches the file about to be edited (and, for a few rows, the text being written) against
the table and, the first time a guide applies in a session, returns it as additional context.
It never blocks the edit. `post` records that a guide was read, so a guide opened before the
first edit never triggers the reminder.

State is one empty marker file per session and guide under the system temp directory. The
hook is silent on any error: a reminder is never worth a failed edit.
"""

import json
import os
import re
import sys
import tempfile

# (name, [path regexes], [content regexes], guide, directive)
# Path regexes run against the path relative to the project root, with forward slashes.
RULES = [
    ("integration-tests", [r"^dotcms-integration/src/test/"], [],
     "docs/testing/INTEGRATION_TESTS.md",
     "suite chosen by CI runtime (sample 3+ recent PRs), appended at the END of the @SuiteClasses "
     "list, @AfterClass cleanup of test data, -Dmaven.build.cache.enabled=false when running"),
    ("unit-tests", [r"^dotCMS/src/test/"], [],
     "docs/testing/BACKEND_UNIT_TESTS.md", ""),
    ("feature-flags", [r"FeatureFlagName\.java$", r"dotmarketing-config\.properties$"],
     [r"FeatureFlagName\.", r"FEATURE_FLAG_"],
     "docs/backend/CONFIGURATION_PATTERNS.md#feature-flags",
     "constant in FeatureFlagName, FEATURE_FLAG_ prefix, default stated in the Javadoc"),
    ("rest", [r"^dotCMS/src/main/java/com/dotcms/rest/", r"openapi\.yaml$"],
     [r"@Schema\(", r"@Operation\(", r"@Path\("],
     "docs/backend/REST_API_PATTERNS.md and dotCMS/src/main/java/com/dotcms/rest/CLAUDE.md",
     "@Schema must match the real return type; regenerate and commit openapi.yaml"),
    ("database", [], [r"\bDotConnect\b", r"@WrapInTransaction", r"@CloseDBIfOpened"],
     "docs/backend/DATABASE_PATTERNS.md", ""),
    ("search", [r"content/elasticsearch/", r"/opensearch/", r"IndexAPI"], [],
     "docs/backend/SEARCH_API_MIGRATION.md and docs/backend/OPENSEARCH_MIGRATION.md", ""),
    ("maven", [r"pom\.xml$"], [],
     "docs/backend/MAVEN_BUILD_SYSTEM.md", "versions only in bom/application/pom.xml"),
    ("e2e", [r"^core-web/apps/dotcms-ui-e2e/"], [],
     "docs/testing/E2E_TESTS.md", ""),
    ("frontend", [r"^core-web/"], [],
     "core-web/CLAUDE.md and docs/frontend/README.md", ""),
    ("java", [r"^dotCMS/src/main/java/.*\.java$"], [],
     "docs/backend/JAVA_STANDARDS.md", "immutables, exceptions, utilities, Javadoc on every method you touch"),
    ("rollback", [r"^dotCMS/src/main/resources/postgres\.sql$", r"/sql/.*\.sql$", r"mapping.*\.json$"], [],
     "docs/core/ROLLBACK_UNSAFE_CATEGORIES.md", ""),
    ("threads", [], [r"Executors\.", r"ExecutorService", r"\bsynchronized\b", r"new Thread\("],
     "docs/backend/VIRTUAL_THREADS.md", "virtual threads for socket I/O, not file I/O; watch synchronized pinning"),
    ("index-emission", [], [r"\bloadFields\b", r"\btoMap\("],
     "docs/backend/INDEX_FIELD_EMISSION.md", ""),
    ("system-events", [r"/system/event/", r"/api/system/event/"], [r"SystemEventsAPI", r"EventSubscriber"],
     "docs/backend/SYSTEM_EVENTS.md", "at-least-once delivery, consumers must be idempotent"),
    ("telemetry", [r"/com/dotcms/telemetry/"], [],
     "docs/backend/TELEMETRY_IMPLEMENTATION.md", ""),
    ("cli", [r"^tools/dotcms-cli/"], [],
     "docs/cli/CLI_OVERVIEW.md", "compiles to a lower maven.compiler.release than core"),
    ("ci", [r"^\.github/workflows/"], [],
     "docs/core/CICD_PIPELINE.md", ""),
    ("docker", [r"(^|/)Dockerfile", r"^docker/"], [],
     "docs/infrastructure/DOCKER_BUILD_PROCESS.md", ""),
]

# A Read of any of these paths counts as having read the guide of the rule.
GUIDE_FILES = {
    "integration-tests": ["docs/testing/INTEGRATION_TESTS.md"],
    "unit-tests": ["docs/testing/BACKEND_UNIT_TESTS.md"],
    "feature-flags": ["docs/backend/CONFIGURATION_PATTERNS.md"],
    "rest": ["docs/backend/REST_API_PATTERNS.md", "dotCMS/src/main/java/com/dotcms/rest/CLAUDE.md"],
    "database": ["docs/backend/DATABASE_PATTERNS.md"],
    "search": ["docs/backend/SEARCH_API_MIGRATION.md", "docs/backend/OPENSEARCH_MIGRATION.md"],
    "maven": ["docs/backend/MAVEN_BUILD_SYSTEM.md"],
    "e2e": ["docs/testing/E2E_TESTS.md"],
    "frontend": ["core-web/CLAUDE.md", "docs/frontend/README.md"],
    "java": ["docs/backend/JAVA_STANDARDS.md"],
    "rollback": ["docs/core/ROLLBACK_UNSAFE_CATEGORIES.md"],
    "threads": ["docs/backend/VIRTUAL_THREADS.md"],
    "index-emission": ["docs/backend/INDEX_FIELD_EMISSION.md"],
    "system-events": ["docs/backend/SYSTEM_EVENTS.md"],
    "telemetry": ["docs/backend/TELEMETRY_IMPLEMENTATION.md"],
    "cli": ["docs/cli/CLI_OVERVIEW.md"],
    "ci": ["docs/core/CICD_PIPELINE.md"],
    "docker": ["docs/infrastructure/DOCKER_BUILD_PROCESS.md"],
}


def relative_path(payload):
    """The edited file relative to the project root, or None when it is outside it."""
    file_path = (payload.get("tool_input") or {}).get("file_path") or ""
    root = os.environ.get("CLAUDE_PROJECT_DIR") or payload.get("cwd") or os.getcwd()
    file_path = os.path.abspath(file_path)
    root = os.path.abspath(root)
    if not file_path.startswith(root + os.sep):
        return None
    return os.path.relpath(file_path, root).replace(os.sep, "/")


def marker_dir(payload):
    session = re.sub(r"[^A-Za-z0-9_-]", "_", payload.get("session_id") or "no-session")
    path = os.path.join(tempfile.gettempdir(), "claude-read-first", session)
    os.makedirs(path, exist_ok=True)
    return path


def seen(payload, name):
    return os.path.exists(os.path.join(marker_dir(payload), name))


def mark(payload, name):
    with open(os.path.join(marker_dir(payload), name), "a", encoding="utf-8"):
        pass


def matching_rules(rel, text):
    for name, path_res, text_res, guide, directive in RULES:
        if any(re.search(r, rel) for r in path_res) or any(re.search(r, text) for r in text_res):
            yield name, guide, directive


def pre(payload):
    rel = relative_path(payload)
    if rel is None:
        return
    tool_input = payload.get("tool_input") or {}
    text = tool_input.get("new_string") or tool_input.get("content") or ""
    lines = []
    for name, guide, directive in matching_rules(rel, text):
        if seen(payload, name):
            continue
        mark(payload, name)
        line = f"- {guide}"
        if directive:
            line += f" ({directive})"
        lines.append(line)
    if not lines:
        return
    context = (
        "CLAUDE.md \"Required Reading Before You Edit\": this is the first edit in a gated area this "
        "session. Read the guide first and say in your reply which directive you are applying:\n"
        + "\n".join(lines)
    )
    print(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse", "additionalContext": context}}))


def post(payload):
    rel = relative_path(payload)
    if rel is None:
        return
    for name, guides in GUIDE_FILES.items():
        if rel in guides:
            mark(payload, name)


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else "pre"
    payload = json.load(sys.stdin)
    if mode == "post":
        post(payload)
    else:
        pre(payload)


if __name__ == "__main__":
    try:
        main()
    except Exception:  # a reminder must never fail an edit
        pass
    sys.exit(0)
