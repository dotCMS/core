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
        and writes <workdir>/manifest.json. It also queues every custom field
        that must be switched to the "Recommended" (component) render mode —
        the newRenderMode=component field variable — once its code is migrated.

  (run the dot-ui-vtl-migration skill on every file — inline, its default
   output since dotCMS/core#37757 — and write the single resulting file to
   <workdir>/migrated/ under the same relative path)

  push  Uploads what changed in migrated/: binaries through the workflow API
        (PUBLISH), inline VTL by updating the field. Skips anything that
        changed on the server since `pull`, and anything that is not a valid
        inline migration (see inline_problem). Every migrated field is left
        with newRenderMode=component; a field that loads a /dA/ asset is only
        switched once that asset is published.

Usage:
    uv run scripts/migrate_custom_fields.py pull
    uv run scripts/migrate_custom_fields.py push --dry-run
    uv run scripts/migrate_custom_fields.py push --only 0ec62ffd3666f23fc5228ef84a481b09 Blog.urlTitle

    BASE_URL=https://my-env.dotcms.dev DOTCMS_TOKEN=... \\
    uv run scripts/migrate_custom_fields.py pull

Exit codes:
    0   success — pull completed, or push completed with no failed entries
    1   pull or push completed but at least one item failed (an unresolvable
        /dA/ reference, or a publish/update request that failed)
    2   invalid arguments or configuration (bad --workdir, unreachable
        BASE_URL, `push` with no manifest.json to read)
    3   authentication failure — DOTCMS_TOKEN is missing, or the instance
        rejected it; nothing is written
"""

import argparse
import copy
import json
import os
import re
import sys
from datetime import datetime
from pathlib import Path
from urllib.parse import urlparse

import httpx

BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080").rstrip("/")
DOTCMS_TOKEN = os.environ.get("DOTCMS_TOKEN", "")

MANIFEST_STATUSES = {"pending", "published", "skipped", "failed"}

CUSTOM_FIELD_CLAZZ = "com.dotcms.contenttype.model.field.ImmutableCustomField"
MIGRATED_MARKER = "isNewEditModeEnabled"
DOTPARSE_RE = re.compile(r"""#dotParse\(\s*["']([^"']+)["']\s*\)""")
DA_RE = re.compile(r"^/dA/([A-Za-z0-9-]+)(?:/.*)?$")
# $velutil.mergeTemplate('/static/...') / #parse('static/...'): files shipped inside dotCMS itself
CORE_FILE_RE = re.compile(r"""(?:mergeTemplate|#parse)\s*\(\s*["']([^"']+)["']""")
LEGACY_RE = re.compile(r"dojo\.|dijit|dojoType|DotCustomFieldApi\.(?:get|set|onChangeField)\s*\(")
_PATH_SEPARATORS_RE = re.compile(r"[\\/]")

# A custom field only renders natively in the new editor (the admin UI's "Recommended"
# implementation) when this field variable is "component"; otherwise it's an iframe.
RENDER_MODE_KEY = "newRenderMode"
RENDER_MODE_COMPONENT = "component"
FIELD_VARIABLE_CLAZZ = "com.dotcms.contenttype.model.field.ImmutableFieldVariable"

client = httpx.Client(base_url=BASE_URL, headers={"Authorization": f"Bearer {DOTCMS_TOKEN}"}, timeout=60)


class AuthError(Exception):
    """DOTCMS_TOKEN is missing, or the instance rejected it (HTTP 401/403). Maps to exit code 3."""


class ConfigError(Exception):
    """Invalid CLI arguments/configuration, or the instance couldn't be reached at all.
    Maps to exit code 2. Never raised for a single push entry's own conflict/validity
    checks — those are reported per-entry instead (FR-011)."""


# ─── logging & credential redaction ────────────────────────────────────────


def redact(text: str) -> str:
    """Never let DOTCMS_TOKEN's value reach stdout/stderr, even inside an error message
    (Constitution Principle III; FR-014 requires naming the *variable*, never its value).

    This is an exact substring replace, so a short/common `DOTCMS_TOKEN` could in principle
    over-redact unrelated words that happen to contain it — same trade-off the old password
    field had. A minted API token is essentially never a short/common word in practice, so
    this residual risk is smaller than it was for a password, and still only ever makes a
    message harder to read, never leaks the credential."""
    if DOTCMS_TOKEN and DOTCMS_TOKEN in text:
        return text.replace(DOTCMS_TOKEN, "***")
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
            f"{BASE_URL} rejected the given DOTCMS_TOKEN (HTTP {response.status_code}). "
            "Check it hasn't been revoked or expired, and that Bearer auth is enabled on "
            "this instance."
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
    """Fails fast with AuthError, before any file is written or request is made, if
    credentials are bad (FR-014). Both `pull` and `push` call this before doing anything
    else, since a push's own per-entry requests use fatal=False and would not otherwise
    abort early."""
    if not DOTCMS_TOKEN:
        raise AuthError(
            "DOTCMS_TOKEN must be set — generate one from the instance's admin UI "
            "(Users -> select user -> API Access Tokens) or POST /api/v1/authentication/api-token"
        )
    request("GET", "/api/v1/contenttype", params={"per_page": 1, "page": 1})


# ─── custom field render mode ───────────────────────────────────────────────


def has_component_render_mode(field: dict) -> bool:
    return any(
        v.get("key") == RENDER_MODE_KEY and v.get("value") == RENDER_MODE_COMPONENT
        for v in field.get("fieldVariables") or []
    )


def with_component_render_mode(field: dict) -> dict:
    """A copy of `field` whose `newRenderMode` variable is "component". Every other field
    variable is kept as-is: the v3 field PUT replaces the variables list wholesale, so any
    variable left out would be deleted. An existing `newRenderMode` keeps its id."""
    updated = copy.deepcopy(field)
    variables = updated.get("fieldVariables") or []
    existing = next((v for v in variables if v.get("key") == RENDER_MODE_KEY), {})
    flag = {
        **existing,
        "clazz": FIELD_VARIABLE_CLAZZ,
        "key": RENDER_MODE_KEY,
        "value": RENDER_MODE_COMPONENT,
        "fieldId": field.get("id"),
    }
    updated["fieldVariables"] = [v for v in variables if v.get("key") != RENDER_MODE_KEY] + [flag]
    return updated


def update_field(type_id: str, field_id: str, field: dict) -> httpx.Response | None:
    """Saves a full field the way the admin UI does. Unlike the v1 field PUT, the v3 one
    also persists `fieldVariables`; it rebuilds the field from the body, so `field` must be
    complete (as fetched), not a partial."""
    return request("PUT", f"/api/v3/contenttype/{type_id}/fields/{field_id}", json={"field": field}, fatal=False)


# ─── manifest (the pull/push handoff contract — see contracts/manifest-schema.md) ──


def manifest_path(workdir: Path) -> Path:
    return workdir / "manifest.json"


def read_manifest(workdir: Path) -> dict:
    path = manifest_path(workdir)
    if not path.exists():
        raise ConfigError(f"No manifest at {path}; run `pull` first.")
    try:
        return json.loads(path.read_text())
    except (json.JSONDecodeError, OSError) as e:
        raise ConfigError(f"manifest at {path} is unreadable/corrupt: {e}") from e


def write_manifest(workdir: Path, manifest: dict) -> None:
    """Writes manifest.json atomically: a crash between the temp-file write and the
    final rename leaves the previous manifest.json fully intact, never a truncated or
    partial file (the per-entry interrupt-safety this design leans on depends on it)."""
    path = manifest_path(workdir)
    tmp = path.with_suffix(".json.tmp")
    tmp.write_text(json.dumps(manifest, indent=2))
    os.replace(tmp, path)


def new_manifest() -> dict:
    return {"baseUrl": BASE_URL, "pulledAt": datetime.now().isoformat(), "entries": [], "knownMigrated": {}}


def try_read_previous_manifest(workdir: Path) -> dict:
    """Best-effort read of a manifest.json left by an earlier pull/push in this workdir,
    used only to seed pull()'s "already migrated at this inode" skip (FR-013). Never
    raises: a missing or unreadable file just means nothing can be skipped, which is
    always safe — every asset then gets the full download + marker check."""
    path = manifest_path(workdir)
    if not path.exists():
        return {}
    try:
        return json.loads(path.read_text())
    except (json.JSONDecodeError, OSError) as e:
        log(f"WARNING: ignoring unreadable previous manifest at {path} ({e}); re-checking every /dA/ reference")
        return {}


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


def sanitize_path_component(value: str, *, fallback: str) -> str:
    """Neutralizes a server-controlled string (a content-type/field `variable`, or an
    asset's `fileName`) before it becomes part of a filesystem path under
    `<workdir>/original/`. Strips path separators and rejects a bare "." or ".." so it can
    never introduce extra path segments or resolve to the current/parent directory. Never
    raises — falls back to `fallback` if nothing safe remains."""
    cleaned = _PATH_SEPARATORS_RE.sub("_", value.strip())
    return cleaned if cleaned not in ("", ".", "..") else fallback


def write_inside(root: Path, relative: str, data: bytes | str) -> None:
    """Writes `data` to `root / relative`; raises ConfigError if the resolved path would
    fall outside `root` — the safety net behind sanitize_path_component, in case a future
    call site forgets to sanitize first."""
    target = (root / relative).resolve()
    if not target.is_relative_to(root.resolve()):
        raise ConfigError(f"refusing to write outside {root}: sanitized path resolved to {target}")
    target.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(data, bytes):
        target.write_bytes(data)
    else:
        target.write_text(data)


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
        self.other_path_owners: set[str] = set()
        # Type.field -> {"typeId", "fieldId", "component"}: every custom field seen, so pull
        # can queue the ones that still need the component render-mode flag.
        self.field_refs: dict[str, dict] = {}

    def add_field(self, content_type: dict, field: dict) -> None:
        owner = f"{content_type['variable']}.{field['variable']}"
        values = field.get("values") or ""
        self.field_refs[owner] = {
            "typeId": content_type["id"],
            "fieldId": field["id"],
            "component": has_component_render_mode(field),
        }

        paths = DOTPARSE_RE.findall(values)
        for path in paths:
            if match := DA_RE.match(path):
                self.asset_refs.setdefault(match.group(1), []).append(owner)
            else:
                self.other_paths.append(f"{owner} -> {path}")
                self.other_path_owners.add(owner)
        if paths:
            return

        if match := CORE_FILE_RE.search(values):
            self.core_files.append(f"{owner} -> {match.group(1)}")
        elif MIGRATED_MARKER in values:
            self.inline_migrated.append(owner)
        elif LEGACY_RE.search(values):
            safe_owner = sanitize_path_component(owner, fallback=f"field-{field['id']}")
            self.inline_fields.append(
                {
                    "kind": "field",
                    "key": owner,
                    "typeId": content_type["id"],
                    "fieldId": field["id"],
                    "file": f"fields/{safe_owner}.vtl",
                    "values": values,
                }
            )
        else:
            self.inline_clean.append(owner)


def scan_content_types() -> Scan:
    # Sequential, one GET per content type (N+1) — fine at the documented scale (spec.md:
    # "tens to low hundreds of content types," no formal SLA). If a customer instance is
    # large enough for this to matter, batch/concurrent fetching is the next step, not a
    # rewrite of the classification logic itself.
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


def known_migrated_inodes(previous_manifest: dict) -> dict[str, str]:
    """da_id -> inode already confirmed migrated, from an earlier run in this workdir:
    either an earlier pull's own marker check (`knownMigrated`) or an earlier push's
    successful publish (an asset entry with status "published", whose `inode` push set to
    the post-publish version). Only ever used to skip a download while the server still
    reports that exact inode."""
    known = dict(previous_manifest.get("knownMigrated") or {})
    for entry in previous_manifest.get("entries", []):
        if entry.get("kind") == "asset" and entry.get("status") == "published":
            known[entry["key"]] = entry["inode"]
    return known


def render_mode_entries(scan: Scan, queued_assets: set[str], migrated_assets: set[str]) -> list[dict]:
    """One "renderMode" entry per custom field that still needs newRenderMode=component
    and whose code is (or, once push publishes it, will be) migrated:
      - owners of a /dA/ asset, depending on every asset of theirs that push still has to
        publish (`requires`); skipped if any of their assets was unresolved or pinned;
      - fields whose inline code already carries the edit-mode switch.
    Inline legacy fields need no entry — push sets the flag in the same PUT as their code.
    Fields that also #dotParse a non-/dA/ path are left alone: part of their code is out
    of scope, so flagging them could switch unmigrated code to component mode."""
    owner_assets: dict[str, set[str]] = {}
    for da_id, owners in scan.asset_refs.items():
        for owner in owners:
            owner_assets.setdefault(owner, set()).add(da_id)

    candidates: list[tuple[str, list[str]]] = []
    for owner, assets in owner_assets.items():
        if owner in scan.other_path_owners or not assets <= (queued_assets | migrated_assets):
            continue
        candidates.append((owner, sorted(assets & queued_assets)))
    candidates.extend((owner, []) for owner in scan.inline_migrated)

    entries = []
    for owner, requires in candidates:
        ref = scan.field_refs[owner]
        if ref["component"]:
            continue
        entries.append(
            {
                "kind": "renderMode",
                "key": owner,
                "typeId": ref["typeId"],
                "fieldId": ref["fieldId"],
                "requires": requires,
                "label": f"{owner} (enable component render mode)",
                "usedBy": [owner],
                "status": "pending",
                "statusReason": None,
            }
        )
    return entries


def pull(workdir: Path) -> int:
    preflight_auth()
    scan = scan_content_types()
    known_migrated = known_migrated_inodes(try_read_previous_manifest(workdir))

    original_dir = workdir / "original"
    for sub in ("assets", "fields"):
        (original_dir / sub).mkdir(parents=True, exist_ok=True)
        (workdir / "migrated" / sub).mkdir(parents=True, exist_ok=True)

    entries: list[dict] = []
    already_migrated: list[str] = []
    unresolved: list[str] = []
    unpublished: list[str] = []
    pinned_versions: list[str] = []
    confirmed_migrated: dict[str, str] = {}

    # Sequential, two GETs per distinct asset (N+1, same trade-off as scan_content_types).
    log(f"Resolving {len(scan.asset_refs)} /dA/ references...")
    for da_id, owners in scan.asset_refs.items():
        content = call("GET", f"/api/v1/content/{da_id}", fatal=False).get("entity")
        field_var = binary_field(content or {})
        if not content or not field_var:
            unresolved.append(f"{da_id} (used by {', '.join(owners)})")
            continue

        if content.get("identifier") != da_id:
            # The field pins a specific version (an inode), not the identifier. PUBLISH
            # always lands on the identifier's latest version and push's conflict check
            # compares against it, so this can never be migrated safely by this tool.
            pinned_versions.append(
                f"{da_id} -> identifier {content.get('identifier')} (used by {', '.join(owners)}); "
                "edit the field to reference the identifier if it needs migrating"
            )
            continue

        if known_migrated.get(da_id) == content["inode"]:
            # Nothing changed since an earlier run confirmed this exact inode migrated —
            # the cheap metadata GET above is enough, skip the binary download (FR-013).
            file_name = content.get("fileName") or content.get("name") or content.get("title") or da_id
            already_migrated.append(f"{content.get('contentType')} {file_name} ({da_id})")
            confirmed_migrated[da_id] = content["inode"]
            continue

        # `<field>Version` is the /dA/ URL pinned to this exact inode
        raw = request("GET", content[f"{field_var}Version"], fatal=False)
        if raw is None:
            unresolved.append(f"{da_id} (download failed, used by {', '.join(owners)})")
            continue

        file_name = content.get("fileName") or content.get("name") or content.get("title")
        if not file_name:
            unresolved.append(f"{da_id} (no fileName/name/title on this content item, used by {', '.join(owners)})")
            continue

        label = f"{content['contentType']} {file_name} ({da_id})"
        if MIGRATED_MARKER in raw.text:
            already_migrated.append(label)
            confirmed_migrated[da_id] = content["inode"]
            continue
        if not content.get("live"):
            unpublished.append(label)

        safe_file_name = sanitize_path_component(file_name, fallback=f"asset-{da_id}")
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
            "file": f"assets/{da_id}__{safe_file_name}",
            "status": "pending",
            "statusReason": None,
        }
        write_inside(original_dir, entry["file"], raw.content)
        entries.append(entry)

    for field in scan.inline_fields:
        values = field.pop("values")
        write_inside(original_dir, field["file"], values)
        entries.append(
            {
                **field,
                "label": field["key"],
                "usedBy": [field["key"]],
                "status": "pending",
                "statusReason": None,
            }
        )

    queued_assets = {e["key"] for e in entries if e["kind"] == "asset"}
    flags = render_mode_entries(scan, queued_assets, set(confirmed_migrated))
    migration_entries = list(entries)
    entries.extend(flags)  # after the assets they may depend on — push processes in order

    manifest = new_manifest()
    manifest["entries"] = entries
    manifest["knownMigrated"] = confirmed_migrated
    write_manifest(workdir, manifest)

    diag(f"\n{'=' * 72}\nTo migrate: {len(migration_entries)} files in {original_dir}\n")
    for entry in migration_entries:
        diag(f"  {entry['file']}")
        if entry["kind"] == "asset":
            diag(f"      {entry['label']}, used by: {', '.join(entry['usedBy'])}")
    report(
        "Switch to component render mode (Recommended) — push sets newRenderMode=component",
        [f"{e['key']}" + (f" (after {', '.join(e['requires'])} is published)" if e["requires"] else "") for e in flags],
    )
    report("Already migrated /dA/ files (skipped)", already_migrated)
    report("Already migrated inline fields (skipped)", scan.inline_migrated)
    report("Not live — push will publish pending working changes too", unpublished)
    report("Unresolved /dA/ ids", unresolved)
    report("Load a file shipped with dotCMS (migrate in core, not here)", scan.core_files)
    report("Inline VTL without legacy patterns (nothing to migrate)", scan.inline_clean)
    report("#dotParse with non-/dA/ paths (not handled)", scan.other_paths)
    report("Pinned /dA/ version references (not handled)", pinned_versions)
    diag(f"\nNext: migrate each file into {workdir / 'migrated'} (same relative path), then run `push`.")

    counts = {
        "pending": len(entries),
        "alreadyMigrated": len(already_migrated) + len(scan.inline_migrated),
        "coreFiles": len(scan.core_files),
        "inlineClean": len(scan.inline_clean),
        "otherPaths": len(scan.other_paths),
        "pinnedVersions": len(pinned_versions),
        "renderMode": len(flags),
        "failed": len(unresolved),
    }
    result = emit_result("pull", counts, entries=entries)
    return result["exitCode"]


# ─── push ────────────────────────────────────────────────────────────────


def entry_matches_only(entry: dict, only: list[str]) -> bool:
    if not only:
        return True
    return entry["key"] in only or entry.get("identifier") in only


_INLINE_HEADER_RE = re.compile(rb"^#if\(\s*\$structures\.isNewEditModeEnabled\(\)\s*\)")
_DIRECTIVE_NAME_RE = re.compile(rb"[A-Za-z0-9_@]+")
_BLOCK_OPENERS = {b"if", b"foreach", b"macro", b"define", b"literal"}


def _iter_directives(data: bytes):
    """Yields (name, start, end) for every real Velocity directive in `data` relevant to
    block nesting/branching (`if`/`foreach`/`macro`/`define`/`literal`/`@name` as block
    openers, plus `elseif`/`else`/`end`), in source order — mirroring the exact rules
    SKILL.md's own "Split" algorithm specifies: `##` line comments, `#* *#` block
    comments and `#[[ ]]#` unparsed blocks are skipped entirely; `\\#end` is an escaped
    literal, not a directive; a directive name ends at the first character that is not a
    letter/digit/`_`/`@` (so `#end-date` IS a real `#end`, while `#endDate` is not one of
    the names tracked here). Directives that never open/close a block (`#set`, `#parse`,
    `#dotParse`, ...) are real Velocity but irrelevant to locating the top-level `#else`,
    so they're not yielded."""
    i, n = 0, len(data)
    while i < n:
        b = data[i : i + 1]
        if b == b"\\":
            if data[i + 1 : i + 2] in (b"\\", b"#"):
                i += 2
                continue
            i += 1
            continue
        if b == b"#":
            two = data[i : i + 2]
            if two == b"##":
                nl = data.find(b"\n", i)
                i = n if nl == -1 else nl
                continue
            if two == b"#*":
                end = data.find(b"*#", i + 2)
                i = n if end == -1 else end + 2
                continue
            if data[i : i + 3] == b"#[[":
                end = data.find(b"]]#", i + 3)
                i = n if end == -1 else end + 3
                continue
            j = i + 1
            brace = data[j : j + 1] == b"{"
            if brace:
                j += 1
            m = _DIRECTIVE_NAME_RE.match(data, j)
            if not m:
                i += 1
                continue
            name, end_j = m.group(0), m.end()
            if brace:
                if data[end_j : end_j + 1] == b"}":
                    end_j += 1
                else:
                    i += 1
                    continue
            lname = name.lower()
            if lname in (b"elseif", b"else", b"end") or lname in _BLOCK_OPENERS:
                yield (lname, i, end_j)
            elif name.startswith(b"@"):
                yield (b"@", i, end_j)
            i = end_j
            continue
        i += 1


def locate_top_level_else(migrated: bytes) -> tuple[int, int] | str:
    """Locates the legacy (`#else`) branch's body in an inline Mode-1 file, reusing the
    exact rules SKILL.md's `Split` algorithm specifies: line 1 is the fixed header;
    counting block openers/`#end`s from there, the top-level `#else` is the first one
    reached while nothing opened after the header is still open, and the matching final
    `#end` closes the header's own `#if`. Returns (start, end) byte offsets spanning the
    legacy body (exclusive of the `#else`/`#end` directives themselves), or a diagnostic
    string — never raises — for a malformed shape (missing header, no top-level `#else`,
    unbalanced blocks, `#end` before `#else`)."""
    header = _INLINE_HEADER_RE.match(migrated)
    if not header:
        return "migrated file does not start with #if( $structures.isNewEditModeEnabled() ) on line 1"

    depth = 0
    else_body_start = None
    end_start = None
    for name, start, end in _iter_directives(migrated):
        if start < header.end():
            continue  # the header's own #if is not itself counted
        if name == b"end":
            if depth == 0:
                end_start = start
                break
            depth -= 1
        elif name == b"else":
            if depth == 0 and else_body_start is None:
                else_body_start = end
        elif name in _BLOCK_OPENERS or name == b"@":
            depth += 1
        # 'elseif' never changes depth and is never treated as a branch start

    if else_body_start is None:
        return "no top-level #else branch found in migrated content (legacy code not located)"
    if end_start is None:
        return (
            "no matching top-level #end found in migrated content "
            "(unbalanced #if/#foreach/#macro/#define/#literal/#@ block)"
        )
    if end_start <= else_body_start:
        return "malformed structure: #end appears before the top-level #else"
    return (else_body_start, end_start)


def inline_problem(original: bytes, migrated: bytes) -> str | None:
    """The skill's inline output keeps the legacy code verbatim under #else, next to
    the new code under #if( $structures.isNewEditModeEnabled() ). Per FR-010.

    Locates the actual top-level #else branch (locate_top_level_else) and requires the
    original bytes to be verbatim *inside that located range* — not merely present
    anywhere in the file (e.g. inside the #if branch, or a comment)."""
    if MIGRATED_MARKER.encode() not in migrated:
        return f"no {MIGRATED_MARKER}() branch (three-file output or blocked migration?)"

    located = locate_top_level_else(migrated)
    if isinstance(located, str):
        return located

    start, end = located
    if original.strip() not in migrated[start:end]:
        return "original code is not preserved verbatim in the #else branch"
    return None


def push_asset(entry: dict, code: bytes, dry_run: bool) -> str:
    """Publishes one dA/FileAsset entry. Returns "published", "dry-run: ...",
    "skipped: ..." (FR-009 conflict), or "failed: ...". Never raises for a single
    entry's own failure (FR-011) — request() is always called with fatal=False here."""
    check = request("GET", f"/api/v1/content/{entry['identifier']}", fatal=False)
    if check is None:
        return "failed: could not verify current state before publishing (request failed)"
    current = (check.json() or {}).get("entity") if check.content else None
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
    try:
        new_inode = response.json()["entity"]["inode"]
    except (json.JSONDecodeError, KeyError, TypeError):
        # The publish itself may well have succeeded server-side — we just can't confirm
        # the new inode from this response, so this must not be silently reported as
        # "published" (FR-011: never crash the batch, but never claim an unconfirmed win).
        return "failed: publish succeeded but the response could not be parsed — verify manually and re-run `pull`"
    entry["previousInode"] = entry["inode"]
    entry["inode"] = new_inode
    return "published"


def push_field(entry: dict, original: str, code: str, dry_run: bool) -> str:
    """Publishes one in-field VTL entry. Same return-value contract as push_asset."""
    path = f"/api/v1/contenttype/{entry['typeId']}/fields/id/{entry['fieldId']}"
    check = request("GET", path, fatal=False)
    if check is None:
        return "failed: could not verify current state before publishing (request failed)"
    field = (check.json() or {}).get("entity") if check.content else None
    if not field or (field.get("values") or "") != original:
        return "skipped: changed on the server since pull (re-run `pull`)"
    if dry_run:
        return f"dry-run: would update field values ({len(code)} chars)"

    field["values"] = code
    # Code and the component render-mode flag go together in one PUT: the migrated code
    # only renders natively in the new editor once newRenderMode is "component".
    if update_field(entry["typeId"], entry["fieldId"], with_component_render_mode(field)) is None:
        return "failed: field update request failed"
    return "published"


def push_render_mode(entry: dict, manifest: dict, dry_run: bool, run_outcomes: dict[str, str]) -> str:
    """Sets newRenderMode=component on one field whose code is (or was just) migrated.
    Returns "published", "dry-run: ...", "skipped: ...", "failed: ...", or "waiting: ..."
    when a dA asset it depends on isn't published yet — the entry then stays pending, since
    flagging a field that still loads legacy code would break it in the new editor."""
    statuses = {e["key"]: e.get("status") for e in manifest["entries"]}
    unmet = [
        da_id
        for da_id in entry.get("requires") or []
        if statuses.get(da_id) != "published" and not (dry_run and run_outcomes.get(da_id, "").startswith("dry-run: "))
    ]
    if unmet:
        return f"waiting: {', '.join(unmet)} not published yet"

    check = request("GET", f"/api/v1/contenttype/{entry['typeId']}/fields/id/{entry['fieldId']}", fatal=False)
    if check is None:
        return "failed: could not read the field (request failed)"
    field = (check.json() or {}).get("entity") if check.content else None
    if not field:
        return "failed: field not found on the server"
    if has_component_render_mode(field):
        return "skipped: already in component render mode"
    if dry_run:
        return "dry-run: would set newRenderMode=component"
    if update_field(entry["typeId"], entry["fieldId"], with_component_render_mode(field)) is None:
        return "failed: could not set newRenderMode=component"
    return "published"


def push_migrated_file(workdir: Path, entry: dict, dry_run: bool) -> str:
    """Runs the safety gates on one asset/field entry's migrated file, then publishes it."""
    migrated_file = workdir / "migrated" / entry["file"]
    original_file = workdir / "original" / entry["file"]

    if not migrated_file.exists():
        # Not migrated yet (e.g. not in this batch): leave it pending for a later push
        # instead of closing it out as skipped.
        return "waiting: no migrated file yet"
    if migrated_file.read_bytes() == original_file.read_bytes():
        return "skipped: migrated file is identical to original"
    problem = inline_problem(original_file.read_bytes(), migrated_file.read_bytes())
    if problem:
        return f"skipped: {problem}"
    if entry["kind"] == "asset":
        return push_asset(entry, migrated_file.read_bytes(), dry_run)
    return push_field(entry, original_file.read_text(), migrated_file.read_text(), dry_run)


TERMINAL_STATUSES = {"published", "skipped", "failed"}


def push(workdir: Path, dry_run: bool, only: list[str]) -> int:
    preflight_auth()
    manifest = read_manifest(workdir)

    if manifest.get("baseUrl") != BASE_URL:
        raise ConfigError(
            f"manifest at {workdir} was pulled from {manifest.get('baseUrl')!r}, but BASE_URL "
            f"is {BASE_URL!r} — re-run `pull` against this instance, or point --workdir at the "
            "workdir that matches this BASE_URL"
        )

    # An explicit --only is treated as "the customer wants this one retried" and bypasses
    # the terminal-status guard; without it, a terminal entry is never reprocessed (it
    # would otherwise be republished identically, or wrongly flip published -> skipped).
    selected_assets = {e["key"] for e in manifest["entries"] if e["kind"] == "asset" and only and entry_matches_only(e, only)}

    def is_selected(entry: dict) -> bool:
        if entry_matches_only(entry, only):
            return bool(only) or entry.get("status") not in TERMINAL_STATUSES
        # Selecting an asset also switches the fields that load it to component render
        # mode in the same run — unless that switch already finished.
        return (
            entry["kind"] == "renderMode"
            and bool(set(entry.get("requires") or []) & selected_assets)
            and entry.get("status") not in TERMINAL_STATUSES
        )

    matched_entries = [entry for entry in manifest["entries"] if is_selected(entry)]
    if only and not matched_entries:
        log(
            f"WARNING: --only {only} matched none of the {len(manifest['entries'])} "
            f"manifest entries in {manifest_path(workdir)} — nothing to push; "
            "check the keys against manifest.json"
        )

    results: list[dict] = []
    counts = {"published": 0, "skipped": 0, "failed": 0, "dryRun": 0}
    run_outcomes: dict[str, str] = {}
    for entry in matched_entries:
        if entry["kind"] == "renderMode":
            outcome = push_render_mode(entry, manifest, dry_run, run_outcomes)
        else:
            outcome = push_migrated_file(workdir, entry, dry_run)
        run_outcomes[entry["key"]] = outcome

        log(f"{entry['label']}: {outcome}")
        results.append({"key": entry["key"], "label": entry["label"], "outcome": outcome})

        if outcome == "published":
            counts["published"] += 1
        elif outcome.startswith("skipped: "):
            counts["skipped"] += 1
        elif outcome.startswith("dry-run: "):
            counts["dryRun"] += 1
        elif outcome.startswith("waiting: "):
            counts["waiting"] = counts.get("waiting", 0) + 1
            continue  # stays pending, so a later push picks it up once its asset is published
        else:
            counts["failed"] += 1

        if not dry_run:
            if outcome == "published":
                status, reason = "published", None
            elif outcome.startswith("skipped: "):
                status, reason = "skipped", outcome.removeprefix("skipped: ")
            else:
                status, reason = "failed", outcome.removeprefix("failed: ")
            # Persisted immediately, not batched to the end of the loop, so an
            # interrupted run still leaves an accurate record (research.md).
            set_entry_status(workdir, manifest, entry["key"], status, reason)

    if not dry_run:
        counts.pop("dryRun")
    if only:
        counts["onlyMatched"] = len(matched_entries)

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
