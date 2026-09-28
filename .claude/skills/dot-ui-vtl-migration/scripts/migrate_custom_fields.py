#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.11"
# dependencies = ["httpx==0.28.1"]
# ///
"""
Migrates legacy (Dojo/Dijit) custom-field VTL to DotCustomFieldApi, two phases:

  pull  Scans every content type's custom fields and downloads what needs
        migrating into <workdir>/original/:
          assets/<id>__<file>   binary referenced via #dotParse("/dA/<id>")
                                (FileAsset or dotAsset)
          fields/<Type>.<field>.vtl
                                VTL written directly in the field's `values`
                                that still uses legacy patterns
        and writes <workdir>/manifest.json.

  (run the dot-ui-vtl-migration skill on every file — inline, its default
   output since dotCMS/core#37757 — and write the single resulting file to
   <workdir>/migrated/ under the same relative path)

  push  Uploads what changed in migrated/: binaries through the workflow API
        (PUBLISH), inline VTL by updating the field. Skips anything that
        changed on the server since `pull`, and anything that is not a valid
        inline migration (see inline_problem).

Usage:
    uv run scripts/migrate_custom_fields.py pull
    uv run scripts/migrate_custom_fields.py push --dry-run
    uv run scripts/migrate_custom_fields.py push --only 0ec62ffd3666f23fc5228ef84a481b09 Blog.urlTitle

    BASE_URL=https://my-env.dotcms.dev DOTCMS_USER=... DOTCMS_PASS=... \\
    uv run scripts/migrate_custom_fields.py pull

Exit codes:
    0   success — pull completed, or push completed with no failed entries
    1   push completed but at least one entry's publish/update request failed
    2   invalid arguments or configuration (bad --workdir, unreachable
        BASE_URL, `push` with no manifest.json to read)
    3   authentication failure — the instance rejected DOTCMS_USER/DOTCMS_PASS,
        or basic auth may be disabled on the instance; nothing is written
"""

import argparse
import json
import os
import re
import sys
from datetime import datetime
from pathlib import Path
from urllib.parse import urlparse

import httpx

BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080").rstrip("/")
DOTCMS_USER = os.environ.get("DOTCMS_USER", "admin@dotcms.com")
DOTCMS_PASS = os.environ.get("DOTCMS_PASS", "admin")

MANIFEST_STATUSES = {"pending", "published", "skipped", "failed"}

CUSTOM_FIELD_CLAZZ = "com.dotcms.contenttype.model.field.ImmutableCustomField"
MIGRATED_MARKER = "isNewEditModeEnabled"
DOTPARSE_RE = re.compile(r"""#dotParse\(\s*["']([^"']+)["']\s*\)""")
DA_RE = re.compile(r"^/dA/([A-Za-z0-9-]+)(?:/.*)?$")
# $velutil.mergeTemplate('/static/...') / #parse('static/...'): files shipped inside dotCMS itself
CORE_FILE_RE = re.compile(r"""(?:mergeTemplate|#parse)\s*\(\s*["']([^"']+)["']""")
LEGACY_RE = re.compile(r"dojo\.|dijit|dojoType|DotCustomFieldApi\.(?:get|set|onChangeField)\s*\(")

client = httpx.Client(base_url=BASE_URL, auth=(DOTCMS_USER, DOTCMS_PASS), timeout=60)


class AuthError(Exception):
    """The instance rejected DOTCMS_USER/DOTCMS_PASS (HTTP 401/403). Maps to exit code 3."""


class ConfigError(Exception):
    """Invalid CLI arguments/configuration, or the instance couldn't be reached at all.
    Maps to exit code 2. Never raised for a single push entry's own conflict/validity
    checks — those are reported per-entry instead (FR-011)."""


# ─── logging & credential redaction ────────────────────────────────────────


def redact(text: str) -> str:
    """Never let DOTCMS_PASS's value reach stdout/stderr, even inside an error message
    (Constitution Principle III; FR-014 requires naming the *variable*, never its value)."""
    if DOTCMS_PASS and DOTCMS_PASS in text:
        return text.replace(DOTCMS_PASS, "***")
    return text


def log(msg: str) -> None:
    """Timestamped progress line. Always stderr — stdout is reserved for the final
    CLI Result JSON (FR-015; the issue's own Agent-Friendly-Interface AC: "progress
    and warnings go to stderr")."""
    print(f"[{datetime.now():%H:%M:%S}] {redact(msg)}", file=sys.stderr, flush=True)


def diag(msg: str) -> None:
    """Freeform (untimestamped, possibly multi-line) diagnostic/report text for a human
    reading the run live. Also always stderr, for the same reason as `log()` — only the
    final CLI Result JSON belongs on stdout."""
    print(redact(msg), file=sys.stderr, flush=True)


# ─── HTTP ───────────────────────────────────────────────────────────────────


def request(method: str, path: str, *, fatal: bool = True, **kwargs) -> httpx.Response | None:
    """Performs a request.

    A 401/403 always raises AuthError, fatal or not — bad credentials abort the whole
    run regardless of which call first noticed them. Otherwise: on failure, `fatal=True`
    raises ConfigError (used for run-level discovery calls where there is no single
    "entry" to blame); `fatal=False` logs to stderr and returns None (used for a single
    push entry's own request, so one entry's failure never aborts the batch — FR-011).
    """
    try:
        response = client.request(method, path, **kwargs)
    except httpx.HTTPError as e:
        message = f"Could not reach {BASE_URL}: {redact(str(e))}"
        if fatal:
            raise ConfigError(message) from e
        log(f"FAILED: {method} {path} -> {redact(str(e))}")
        return None

    if response.status_code in (401, 403):
        raise AuthError(
            f"{BASE_URL} rejected the given DOTCMS_USER/DOTCMS_PASS (HTTP {response.status_code}). "
            "Check the credentials — note basic auth may be disabled on this instance."
        )
    if response.is_success:
        return response

    error = f"HTTP {response.status_code}\n{redact(response.text)}"
    if fatal:
        raise ConfigError(f"FAILED: {method} {path} -> {error}")
    log(f"FAILED: {method} {path} -> {error}")
    return None


def call(method: str, path: str, **kwargs) -> dict:
    response = request(method, path, **kwargs)
    return response.json() if response is not None and response.content else {}


def preflight_auth() -> None:
    """Fails fast with AuthError, before any file is written, if credentials are bad
    (FR-014). Both `pull` and `push` call this before doing anything else, since a
    push's own per-entry requests use fatal=False and would not otherwise abort early."""
    request("GET", "/api/v1/contenttype", params={"per_page": 1, "page": 1})


# ─── manifest (the pull/push handoff contract — see contracts/manifest-schema.md) ──


def manifest_path(workdir: Path) -> Path:
    return workdir / "manifest.json"


def read_manifest(workdir: Path) -> dict:
    path = manifest_path(workdir)
    if not path.exists():
        raise ConfigError(f"No manifest at {path}; run `pull` first.")
    return json.loads(path.read_text())


def write_manifest(workdir: Path, manifest: dict) -> None:
    manifest_path(workdir).write_text(json.dumps(manifest, indent=2))


def new_manifest() -> dict:
    return {"baseUrl": BASE_URL, "pulledAt": datetime.now().isoformat(), "entries": []}


def set_entry_status(workdir: Path, manifest: dict, key: str, status: str, reason: str | None = None) -> None:
    """Updates one entry's status in memory AND immediately persists the whole manifest
    back to disk, so a `push` interrupted mid-batch still leaves an accurate record of
    exactly what happened to each entry attempted so far (Edge Cases; research.md)."""
    assert status in MANIFEST_STATUSES, f"unknown status {status!r}"
    for entry in manifest["entries"]:
        if entry["key"] == key:
            entry["status"] = status
            entry["statusReason"] = reason
            break
    write_manifest(workdir, manifest)


# ─── CLI result (the final, agent-parseable stdout line — FR-015) ─────────────────


def compute_exit_code(counts: dict) -> int:
    return 1 if counts.get("failed", 0) > 0 else 0


def emit_result(command: str, counts: dict, entries: list | None = None) -> dict:
    result = {"command": command, "counts": counts, "exitCode": compute_exit_code(counts)}
    if entries is not None:
        result["entries"] = entries
    print(json.dumps(result))
    return result


# ─── pull ────────────────────────────────────────────────────────────────


def binary_field(content: dict) -> str | None:
    """Variable of the binary field holding the code: `asset` (dotAsset), `fileAsset` (FileAsset)."""
    for candidate in ("asset", "fileAsset"):
        if f"{candidate}ContentAsset" in content:
            return candidate
    return next((k[: -len("ContentAsset")] for k in content if k.endswith("ContentAsset")), None)


def list_content_types() -> list[dict]:
    types, page = [], 1
    while True:
        batch = call("GET", "/api/v1/contenttype", params={"per_page": 100, "page": page})["entity"]
        if not batch:
            return types
        types.extend(batch)
        page += 1


class Scan:
    """FR-002's 5-way classification of every custom field on the instance."""

    def __init__(self) -> None:
        self.asset_refs: dict[str, list[str]] = {}  # dA id -> [Type.field, ...]
        self.inline_fields: list[dict] = []  # inline VTL with legacy patterns
        self.core_files: list[str] = []
        self.inline_clean: list[str] = []
        self.inline_migrated: list[str] = []
        self.other_paths: list[str] = []

    def add_field(self, content_type: dict, field: dict) -> None:
        owner = f"{content_type['variable']}.{field['variable']}"
        values = field.get("values") or ""

        paths = DOTPARSE_RE.findall(values)
        for path in paths:
            if match := DA_RE.match(path):
                self.asset_refs.setdefault(match.group(1), []).append(owner)
            else:
                self.other_paths.append(f"{owner} -> {path}")
        if paths:
            return

        if match := CORE_FILE_RE.search(values):
            self.core_files.append(f"{owner} -> {match.group(1)}")
        elif MIGRATED_MARKER in values:
            self.inline_migrated.append(owner)
        elif LEGACY_RE.search(values):
            self.inline_fields.append(
                {
                    "kind": "field",
                    "key": owner,
                    "typeId": content_type["id"],
                    "fieldId": field["id"],
                    "file": f"fields/{owner}.vtl",
                    "values": values,
                }
            )
        else:
            self.inline_clean.append(owner)


def scan_content_types() -> Scan:
    scan = Scan()
    content_types = list_content_types()
    log(f"Scanning {len(content_types)} content types...")
    for ct in content_types:
        full_type = call("GET", f"/api/v1/contenttype/id/{ct['id']}")["entity"]
        for field in full_type.get("fields", []):
            if field.get("clazz") == CUSTOM_FIELD_CLAZZ:
                scan.add_field(full_type, field)
    return scan


def report(title: str, items: list[str]) -> None:
    if items:
        diag(f"\n{title}: {len(items)}")
        for item in items:
            diag(f"  - {item}")


def pull(workdir: Path) -> int:
    preflight_auth()
    scan = scan_content_types()

    original_dir = workdir / "original"
    for sub in ("assets", "fields"):
        (original_dir / sub).mkdir(parents=True, exist_ok=True)
        (workdir / "migrated" / sub).mkdir(parents=True, exist_ok=True)

    entries: list[dict] = []
    already_migrated: list[str] = []
    unresolved: list[str] = []
    unpublished: list[str] = []

    log(f"Resolving {len(scan.asset_refs)} /dA/ references...")
    for da_id, owners in scan.asset_refs.items():
        content = call("GET", f"/api/v1/content/{da_id}", fatal=False).get("entity")
        field_var = binary_field(content or {})
        if not content or not field_var:
            unresolved.append(f"{da_id} (used by {', '.join(owners)})")
            continue

        # `<field>Version` is the /dA/ URL pinned to this exact inode
        raw = request("GET", content[f"{field_var}Version"], fatal=False)
        if raw is None:
            unresolved.append(f"{da_id} (download failed, used by {', '.join(owners)})")
            continue

        file_name = content.get("fileName") or content.get("name") or content["title"]
        label = f"{content['contentType']} {file_name} ({da_id})"
        if MIGRATED_MARKER in raw.text:
            already_migrated.append(label)
            continue
        if not content.get("live"):
            unpublished.append(label)

        entry = {
            "kind": "asset",
            "key": da_id,
            "identifier": content["identifier"],
            "inode": content["inode"],
            "languageId": content["languageId"],
            "contentType": content["contentType"],
            "binaryField": field_var,
            "fileName": file_name,
            "label": label,
            "usedBy": owners,
            "file": f"assets/{da_id}__{file_name}",
            "status": "pending",
            "statusReason": None,
        }
        (original_dir / entry["file"]).write_bytes(raw.content)
        entries.append(entry)

    for field in scan.inline_fields:
        values = field.pop("values")
        (original_dir / field["file"]).write_text(values)
        entries.append(
            {
                **field,
                "label": field["key"],
                "usedBy": [field["key"]],
                "status": "pending",
                "statusReason": None,
            }
        )

    manifest = new_manifest()
    manifest["entries"] = entries
    write_manifest(workdir, manifest)

    diag(f"\n{'=' * 72}\nTo migrate: {len(entries)} files in {original_dir}\n")
    for entry in entries:
        diag(f"  {entry['file']}")
        if entry["kind"] == "asset":
            diag(f"      {entry['label']}, used by: {', '.join(entry['usedBy'])}")
    report("Already migrated /dA/ files (skipped)", already_migrated)
    report("Already migrated inline fields (skipped)", scan.inline_migrated)
    report("Not live — push will publish pending working changes too", unpublished)
    report("Unresolved /dA/ ids", unresolved)
    report("Load a file shipped with dotCMS (migrate in core, not here)", scan.core_files)
    report("Inline VTL without legacy patterns (nothing to migrate)", scan.inline_clean)
    report("#dotParse with non-/dA/ paths (not handled)", scan.other_paths)
    diag(f"\nNext: migrate each file into {workdir / 'migrated'} (same relative path), then run `push`.")

    counts = {
        "pending": len(entries),
        "alreadyMigrated": len(already_migrated) + len(scan.inline_migrated),
        "coreFiles": len(scan.core_files),
        "inlineClean": len(scan.inline_clean),
        "otherPaths": len(scan.other_paths),
        "failed": len(unresolved),
    }
    result = emit_result("pull", counts, entries=entries)
    return result["exitCode"]


# ─── push ────────────────────────────────────────────────────────────────


def entry_matches_only(entry: dict, only: list[str]) -> bool:
    if not only:
        return True
    return entry["key"] in only or entry.get("identifier") in only


def inline_problem(original: bytes, migrated: bytes) -> str | None:
    """The skill's inline output keeps the legacy code verbatim under #else, next to
    the new code under #if( $structures.isNewEditModeEnabled() ). Per FR-010."""
    if MIGRATED_MARKER.encode() not in migrated:
        return f"no {MIGRATED_MARKER}() branch (three-file output or blocked migration?)"
    if original.strip() not in migrated:
        return "original code is not preserved verbatim in the #else branch"
    return None


def push_asset(entry: dict, code: bytes, dry_run: bool) -> str:
    """Publishes one dA/FileAsset entry. Returns "published", "dry-run: ...",
    "skipped: ..." (FR-009 conflict), or "failed: ...". Never raises for a single
    entry's own failure (FR-011) — request() is always called with fatal=False here."""
    current = call("GET", f"/api/v1/content/{entry['identifier']}", fatal=False).get("entity")
    if not current or current.get("inode") != entry["inode"]:
        return "skipped: changed on the server since pull (re-run `pull`)"
    if dry_run:
        return f"dry-run: would publish {len(code)} bytes"

    payload = {
        "contentlet": {"contentType": entry["contentType"], "identifier": entry["identifier"]},
        "binaryFields": [entry["binaryField"]],
    }
    response = request(
        "PUT",
        "/api/v1/workflow/actions/default/fire/PUBLISH",
        params={"identifier": entry["identifier"], "language": entry["languageId"], "indexPolicy": "WAIT_FOR"},
        files={
            "json": (None, json.dumps(payload), "application/json"),
            "file": (entry["fileName"], code, "text/plain"),
        },
        fatal=False,
    )
    if response is None:
        return "failed: publish request failed"
    entry["previousInode"] = entry["inode"]
    entry["inode"] = response.json()["entity"]["inode"]
    return "published"


def push_field(entry: dict, original: str, code: str, dry_run: bool) -> str:
    """Publishes one in-field VTL entry. Same return-value contract as push_asset."""
    path = f"/api/v1/contenttype/{entry['typeId']}/fields/id/{entry['fieldId']}"
    field = call("GET", path, fatal=False).get("entity")
    if not field or (field.get("values") or "") != original:
        return "skipped: changed on the server since pull (re-run `pull`)"
    if dry_run:
        return f"dry-run: would update field values ({len(code)} chars)"

    field["values"] = code
    return "published" if request("PUT", path, json=field, fatal=False) is not None else "failed: field update request failed"


def push(workdir: Path, dry_run: bool, only: list[str]) -> int:
    preflight_auth()
    manifest = read_manifest(workdir)

    results: list[dict] = []
    failed = 0
    for entry in manifest["entries"]:
        if not entry_matches_only(entry, only):
            continue

        migrated_file = workdir / "migrated" / entry["file"]
        original_file = workdir / "original" / entry["file"]

        if not migrated_file.exists():
            outcome = "skipped: no migrated file"
        elif migrated_file.read_bytes() == original_file.read_bytes():
            outcome = "skipped: migrated file is identical to original"
        else:
            problem = inline_problem(original_file.read_bytes(), migrated_file.read_bytes())
            if problem:
                outcome = f"skipped: {problem}"
            elif entry["kind"] == "asset":
                outcome = push_asset(entry, migrated_file.read_bytes(), dry_run)
            else:
                outcome = push_field(entry, original_file.read_text(), migrated_file.read_text(), dry_run)

        log(f"{entry['label']}: {outcome}")
        results.append({"key": entry["key"], "label": entry["label"], "outcome": outcome})

        if not dry_run:
            if outcome == "published":
                status, reason = "published", None
            elif outcome.startswith("skipped: "):
                status, reason = "skipped", outcome.removeprefix("skipped: ")
            else:
                status, reason = "failed", outcome.removeprefix("failed: ")
                failed += 1
            # Persisted immediately, not batched to the end of the loop, so an
            # interrupted run still leaves an accurate record (research.md).
            set_entry_status(workdir, manifest, entry["key"], status, reason)

    counts = {"considered": len(results), "failed": failed}
    result = emit_result("push", counts, entries=results)
    return result["exitCode"]


def default_workdir() -> Path:
    return Path("vtl-migration") / (urlparse(BASE_URL).hostname or "local")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--workdir", type=Path, default=default_workdir(), help="default: %(default)s")
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("pull", help="find and download custom field VTL to migrate")
    push_parser = sub.add_parser("push", help="upload and publish migrated files")
    push_parser.add_argument("--dry-run", action="store_true", help="show what would be uploaded")
    push_parser.add_argument("--only", nargs="+", default=[], metavar="KEY", help="dA ids, identifiers or Type.field")
    args = parser.parse_args()

    try:
        if args.command == "pull":
            exit_code = pull(args.workdir)
        else:
            exit_code = push(args.workdir, args.dry_run, args.only)
        sys.exit(exit_code)
    except AuthError as e:
        print(redact(str(e)), file=sys.stderr)
        sys.exit(3)
    except ConfigError as e:
        print(redact(str(e)), file=sys.stderr)
        sys.exit(2)


if __name__ == "__main__":
    main()
