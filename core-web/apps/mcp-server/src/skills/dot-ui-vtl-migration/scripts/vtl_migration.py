#!/usr/bin/env python3
"""
Local helper for the dot-ui-vtl-migration skill's "migrate a whole instance" workflow.

It never talks to dotCMS. The dotCMS MCP server does all the I/O (reading content types,
downloading and publishing files, updating fields); this script does the parts that must be
exact and are easy to get wrong by hand, with no network and no credentials:

  classify        Sort every custom field into one of the migration categories.
  assets-plan     Decide which /dA/ files to download, from the metadata the agent fetched.
  build-manifest  Build manifest.json from the downloaded files.
  status          List the manifest entries (what is pending, what is done).
  check           Run the safety gates on the migrated files and say what is ready to publish.
  payload         Print what the agent needs to publish one asset.
  field-body      Build the PUT body for a field update or a render-mode switch.
  record          Persist one entry's outcome in the manifest, right after the write.

Working directory layout (default ./vtl-migration, standard library only, Python 3.9+):

  scan/*.json              input: arrays of custom fields written by the agent (see `classify`)
  assets/<id>.json         input: metadata of each /dA/ reference (see `assets-plan`)
  original/assets/<id>__<file>      downloaded files
  original/fields/<Type.field>.vtl  in-field VTL that still uses legacy patterns
  migrated/...                      the migrated result, same relative path as original/
  manifest.json            the handoff state; written by build-manifest and record

Every command prints one JSON document on stdout. Exit codes: 0 success, 2 invalid
arguments or missing/corrupt input.
"""

from __future__ import annotations

import argparse
import base64
import copy
import json
import os
import re
import sys
from datetime import datetime
from pathlib import Path

MANIFEST_STATUSES = {"pending", "published", "skipped", "failed"}
TERMINAL_STATUSES = {"published", "skipped", "failed"}

CUSTOM_FIELD_CLAZZ = "com.dotcms.contenttype.model.field.ImmutableCustomField"
MIGRATED_MARKER = "isNewEditModeEnabled"
DOTPARSE_RE = re.compile(r"""#dotParse\(\s*["']([^"']+)["']\s*\)""")
DA_RE = re.compile(r"^/dA/([A-Za-z0-9-]+)(?:/.*)?$")
# $velutil.mergeTemplate('/static/...') / #parse('static/...'): files shipped inside dotCMS itself
CORE_FILE_RE = re.compile(r"""(?:mergeTemplate|#parse)\s*\(\s*["']([^"']+)["']""")
LEGACY_RE = re.compile(r"dojo\.|dijit|dojoType|DotCustomFieldApi\.(?:get|set|onChangeField)\s*\(")
_PATH_SEPARATORS_RE = re.compile(r"[\\/]")


class InputError(Exception):
    """Missing, corrupt or invalid input. Maps to exit code 2."""


# ─── files ───────────────────────────────────────────────────────────────


def sanitize_path_component(value: str, *, fallback: str) -> str:
    """Neutralizes a server-controlled string (a content-type/field `variable`, or an
    asset's `fileName`) before it becomes part of a path under the working directory.
    Strips path separators and rejects a bare "." or "..". Never raises."""
    cleaned = _PATH_SEPARATORS_RE.sub("_", value.strip())
    return cleaned if cleaned not in ("", ".", "..") else fallback


def write_inside(root: Path, relative: str, data) -> None:
    """Writes `data` to `root / relative`; refuses a path that resolves outside `root`."""
    target = (root / relative).resolve()
    if not target.is_relative_to(root.resolve()):
        raise InputError(f"refusing to write outside {root}: {relative!r} resolved to {target}")
    target.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(data, bytes):
        target.write_bytes(data)
    else:
        write_text_verbatim(target, data)


def read_text_verbatim(path: Path) -> str:
    """Reads text without newline translation, so CRLF survives a comparison or a PUT body."""
    with open(path, encoding="utf-8", newline="") as f:
        return f.read()


def write_text_verbatim(path: Path, data: str) -> None:
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write(data)


def read_json(path: Path):
    if not path.exists():
        raise InputError(f"missing {path}")
    try:
        return json.loads(read_text_verbatim(path))
    except (ValueError, OSError) as e:  # ValueError covers JSONDecodeError and UnicodeDecodeError
        raise InputError(f"{path} is unreadable or corrupt: {e}") from e


def write_json(path: Path, data) -> None:
    """Writes atomically: a crash between the temp write and the rename leaves the previous
    file intact, never a truncated one."""
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    write_text_verbatim(tmp, json.dumps(data, indent=2))
    os.replace(tmp, path)


def manifest_path(workdir: Path) -> Path:
    return workdir / "manifest.json"


def read_manifest(workdir: Path) -> dict:
    path = manifest_path(workdir)
    if not path.exists():
        raise InputError(f"No manifest at {path}; run classify, assets-plan and build-manifest first.")
    return read_json(path)


def try_read_previous_manifest(workdir: Path) -> dict:
    """Best-effort read of an earlier manifest, used only to skip re-downloading what an
    earlier run already confirmed migrated. A missing or unreadable file is always safe."""
    try:
        return read_json(manifest_path(workdir))
    except InputError:
        return {}


# ─── classify ────────────────────────────────────────────────────────────


class Scan:
    """Classification of every custom field on the instance."""

    def __init__(self) -> None:
        self.asset_refs: dict[str, list[str]] = {}  # dA id -> [Type.field, ...]
        self.inline_fields: list[dict] = []  # inline VTL with legacy patterns (values kept apart)
        self.core_files: list[str] = []
        self.inline_clean: list[str] = []
        self.inline_migrated: list[str] = []
        self.other_paths: list[str] = []
        self.other_path_owners: list[str] = []
        # Type.field -> {"typeId", "fieldId", "component"}: every custom field seen
        self.field_refs: dict[str, dict] = {}

    def add_field(self, field: dict) -> str | None:
        """Classifies one field record. Returns the inline VTL to write when it is legacy."""
        owner = f"{field['type']}.{field['field']}"
        values = field.get("values") or ""
        self.field_refs[owner] = {
            "typeId": field["typeId"],
            "fieldId": field["fieldId"],
            "component": bool(field.get("component")),
        }

        paths = DOTPARSE_RE.findall(values)
        for path in paths:
            if match := DA_RE.match(path):
                self.asset_refs.setdefault(match.group(1), []).append(owner)
            else:
                self.other_paths.append(f"{owner} -> {path}")
                if owner not in self.other_path_owners:
                    self.other_path_owners.append(owner)
        if paths:
            return None

        if match := CORE_FILE_RE.search(values):
            self.core_files.append(f"{owner} -> {match.group(1)}")
        elif is_inline_migrated(values.encode("utf-8")):
            self.inline_migrated.append(owner)
        elif LEGACY_RE.search(values):
            safe_owner = sanitize_path_component(owner, fallback=f"field-{field['fieldId']}")
            self.inline_fields.append(
                {
                    "kind": "field",
                    "key": owner,
                    "typeId": field["typeId"],
                    "fieldId": field["fieldId"],
                    "file": f"fields/{safe_owner}.vtl",
                }
            )
            return values
        else:
            self.inline_clean.append(owner)
        return None

    def to_json(self) -> dict:
        return {
            "assetRefs": self.asset_refs,
            "inlineFields": self.inline_fields,
            "coreFiles": self.core_files,
            "inlineClean": self.inline_clean,
            "inlineMigrated": self.inline_migrated,
            "otherPaths": self.other_paths,
            "otherPathOwners": self.other_path_owners,
            "fieldRefs": self.field_refs,
        }

    @classmethod
    def from_json(cls, data: dict) -> "Scan":
        scan = cls()
        scan.asset_refs = data["assetRefs"]
        scan.inline_fields = data["inlineFields"]
        scan.core_files = data["coreFiles"]
        scan.inline_clean = data["inlineClean"]
        scan.inline_migrated = data["inlineMigrated"]
        scan.other_paths = data["otherPaths"]
        scan.other_path_owners = data["otherPathOwners"]
        scan.field_refs = data["fieldRefs"]
        return scan


def cmd_classify(workdir: Path) -> dict:
    """Reads scan/*.json: arrays of {type, typeId, field, fieldId, values, component}, one
    record per custom field, where `type`/`field` are the variables and `component` says the
    field already has newRenderMode=component. Writes each legacy inline field to
    original/fields/ and the classification to scan-result.json."""
    parts = sorted((workdir / "scan").glob("*.json"))
    if not parts:
        raise InputError(f"no scan/*.json files in {workdir}: write the custom fields there first")

    scan = Scan()
    seen: set[str] = set()
    original_dir = workdir / "original"
    for part in parts:
        records = read_json(part)
        if not isinstance(records, list):
            raise InputError(f"{part} must hold a JSON array of custom fields")
        for record in records:
            missing = [k for k in ("type", "typeId", "field", "fieldId") if not record.get(k)]
            if missing:
                raise InputError(f"{part}: a record is missing {', '.join(missing)}")
            owner = f"{record['type']}.{record['field']}"
            if owner in seen:
                continue  # the same field sent twice (overlapping batches)
            seen.add(owner)
            code = scan.add_field(record)
            if code is not None:
                file = next(f["file"] for f in scan.inline_fields if f["key"] == owner)
                write_inside(original_dir, file, code)

    write_json(workdir / "scan-result.json", scan.to_json())
    return {
        "command": "classify",
        "scanParts": len(parts),
        "counts": {
            "customFields": len(seen),
            "assetReferences": len(scan.asset_refs),
            "inlineLegacy": len(scan.inline_fields),
            "alreadyMigratedInline": len(scan.inline_migrated),
            "coreFiles": len(scan.core_files),
            "inlineClean": len(scan.inline_clean),
            "otherPaths": len(scan.other_paths),
        },
        "assetsToResolve": sorted(scan.asset_refs),
        "report": {
            "coreFiles": scan.core_files,
            "otherPaths": scan.other_paths,
            "alreadyMigratedInline": scan.inline_migrated,
        },
    }


# ─── assets-plan ─────────────────────────────────────────────────────────


def known_migrated_inodes(previous_manifest: dict) -> dict[str, str]:
    """da_id -> inode already confirmed migrated by an earlier run in this workdir."""
    known = dict(previous_manifest.get("knownMigrated") or {})
    for entry in previous_manifest.get("entries", []):
        if entry.get("kind") == "asset" and entry.get("status") == "published":
            known[entry["key"]] = entry["inode"]
    return known


def asset_file_name(meta: dict) -> str | None:
    return meta.get("fileName") or meta.get("name") or meta.get("title")


def cmd_assets_plan(workdir: Path) -> dict:
    """Reads scan-result.json and assets/<id>.json, one per /dA/ reference, each holding the
    fields of `GET /api/v1/content/<id>` that matter: identifier, inode, languageId,
    contentType, live, fileName (or name/title), binaryField (the variable of the binary
    field), plus path/hostName if the download needs them. Decides what to download."""
    scan = Scan.from_json(read_json(workdir / "scan-result.json"))
    known = known_migrated_inodes(try_read_previous_manifest(workdir))

    to_download, unresolved, pinned, already_migrated = [], [], [], []
    already_migrated_ids: dict[str, str] = {}
    for da_id, owners in scan.asset_refs.items():
        used_by = ", ".join(owners)
        meta_path = workdir / "assets" / f"{da_id}.json"
        try:
            meta = read_json(meta_path) if meta_path.exists() else None
        except InputError as e:
            unresolved.append(f"{da_id} ({e}, used by {used_by})")
            continue
        if not meta or not meta.get("binaryField") or not meta.get("identifier") or not meta.get("inode"):
            unresolved.append(f"{da_id} (no usable content metadata, used by {used_by})")
            continue
        if meta["identifier"] != da_id:
            # The field pins one version (an inode), not the identifier. PUBLISH always lands
            # on the latest version, so this can never be migrated safely.
            pinned.append(
                f"{da_id} -> identifier {meta['identifier']} (used by {used_by}); "
                "edit the field to reference the identifier if it needs migrating"
            )
            continue
        file_name = asset_file_name(meta)
        if known.get(da_id) == meta["inode"]:
            already_migrated.append(f"{meta.get('contentType')} {file_name or da_id} ({da_id})")
            already_migrated_ids[da_id] = meta["inode"]
            continue
        if not file_name:
            unresolved.append(f"{da_id} (no fileName/name/title on this content item, used by {used_by})")
            continue
        safe = sanitize_path_component(file_name, fallback=f"asset-{da_id}")
        to_download.append(
            {
                "id": da_id,
                "file": f"assets/{da_id}__{safe}",
                "destination": str(workdir / "original" / f"assets/{da_id}__{safe}"),
                "fileName": file_name,
                "path": meta.get("path"),
                "hostName": meta.get("hostName"),
                "usedBy": owners,
            }
        )

    plan = {
        "toDownload": to_download,
        "unresolved": unresolved,
        "pinned": pinned,
        "alreadyMigrated": already_migrated,
        # id -> inode, established by this run's metadata; build-manifest trusts only these
        "alreadyMigratedIds": already_migrated_ids,
    }
    write_json(workdir / "assets-plan.json", plan)
    return {"command": "assets-plan", **plan}


# ─── build-manifest ──────────────────────────────────────────────────────


def render_mode_entries(scan: Scan, queued_assets: set[str], migrated_assets: set[str]) -> list[dict]:
    """One "renderMode" entry per custom field that still needs newRenderMode=component
    and whose code is (or, once published, will be) migrated:
      - owners of a /dA/ asset, depending on every asset of theirs that still has to be
        published (`requires`); skipped if any of their assets was unresolved or pinned;
      - fields whose inline code already carries the edit-mode switch.
    Inline legacy fields need no entry: the flag is set in the same update as their code.
    Fields that also #dotParse a non-/dA/ path are left alone: part of their code is out of
    scope, so flagging them could switch unmigrated code to the new editor."""
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


def carry_over_progress(entries: list[dict], previous: dict) -> None:
    """A rebuild must not undo work: copy status, reason and previousInode from the previous
    manifest's entry with the same key and kind, unless an asset's inode changed since.
    An entry that no longer shows up (a field already published is now classified as
    migrated) is simply dropped."""
    earlier = {(e["kind"], e["key"]): e for e in previous.get("entries", [])}
    for entry in entries:
        old = earlier.get((entry["kind"], entry["key"]))
        if not old:
            continue
        if entry["kind"] == "asset" and old.get("inode") != entry["inode"]:
            continue  # a newer version of the asset: earlier progress belongs to another file
        entry["status"] = old.get("status", entry["status"])
        entry["statusReason"] = old.get("statusReason")
        if old.get("previousInode"):
            entry["previousInode"] = old["previousInode"]


def cmd_build_manifest(workdir: Path, instance: str | None) -> dict:
    """Reads scan-result.json, assets-plan.json and the downloaded files, writes manifest.json."""
    scan = Scan.from_json(read_json(workdir / "scan-result.json"))
    plan = read_json(workdir / "assets-plan.json")
    previous = try_read_previous_manifest(workdir)
    original_dir = workdir / "original"

    entries: list[dict] = []
    unresolved = list(plan["unresolved"])
    already_migrated = list(plan["alreadyMigrated"])
    unpublished: list[str] = []
    confirmed_migrated = dict(plan.get("alreadyMigratedIds") or {})

    for item in plan["toDownload"]:
        da_id = item["id"]
        try:
            meta = read_json(workdir / "assets" / f"{da_id}.json")
        except InputError as e:
            unresolved.append(f"{da_id} ({e}, used by {', '.join(item['usedBy'])})")
            continue
        downloaded = original_dir / item["file"]
        label = f"{meta.get('contentType')} {item['fileName']} ({da_id})"
        if not downloaded.is_file():
            unresolved.append(f"{da_id} (download failed, used by {', '.join(item['usedBy'])})")
            continue
        if is_inline_migrated(downloaded.read_bytes()):
            already_migrated.append(label)
            confirmed_migrated[da_id] = meta["inode"]
            continue
        if not meta.get("live"):
            unpublished.append(label)
        entries.append(
            {
                "kind": "asset",
                "key": da_id,
                "identifier": meta["identifier"],
                "inode": meta["inode"],
                "languageId": meta.get("languageId", 1),
                "contentType": meta.get("contentType"),
                "binaryField": meta["binaryField"],
                "fileName": item["fileName"],
                "label": label,
                "usedBy": item["usedBy"],
                "file": item["file"],
                "status": "pending",
                "statusReason": None,
            }
        )

    for field in scan.inline_fields:
        entries.append(
            {**field, "label": field["key"], "usedBy": [field["key"]], "status": "pending", "statusReason": None}
        )

    queued_assets = {e["key"] for e in entries if e["kind"] == "asset"}
    flags = render_mode_entries(scan, queued_assets, set(confirmed_migrated))
    migration_entries = list(entries)
    entries.extend(flags)  # after the assets they may depend on: check processes in order

    carry_over_progress(entries, previous)
    manifest = {
        "instance": instance,
        "builtAt": datetime.now().isoformat(),
        "knownMigrated": confirmed_migrated,
        "entries": entries,
    }
    write_json(manifest_path(workdir), manifest)

    return {
        "command": "build-manifest",
        "counts": {
            "pending": len(migration_entries),
            "renderMode": len(flags),
            "alreadyMigrated": len(already_migrated) + len(scan.inline_migrated),
            "coreFiles": len(scan.core_files),
            "inlineClean": len(scan.inline_clean),
            "otherPaths": len(scan.other_paths),
            "pinnedVersions": len(plan["pinned"]),
            "unresolved": len(unresolved),
        },
        "entries": [{k: e[k] for k in ("kind", "key", "label", "usedBy", "file", "requires") if k in e} for e in entries],
        "report": {
            "alreadyMigrated": already_migrated + scan.inline_migrated,
            "notLive": unpublished,
            "unresolved": unresolved,
            "coreFiles": scan.core_files,
            "inlineClean": scan.inline_clean,
            "otherPaths": scan.other_paths,
            "pinnedVersions": plan["pinned"],
        },
    }


# ─── inline validity ─────────────────────────────────────────────────────

_INLINE_HEADER_RE = re.compile(rb"^#if\(\s*\$structures\.isNewEditModeEnabled\(\)\s*\)")
_DIRECTIVE_NAME_RE = re.compile(rb"[A-Za-z0-9_@]+")
_BLOCK_OPENERS = {b"if", b"foreach", b"macro", b"define", b"literal"}


# The inline-validity logic below (_iter_directives, locate_top_level_else, inline_problem) is
# copied from the original custom-field migration script and mirrors the "Before emitting inline" and
# "Converting between modes" rules in SKILL.md and reference/migration-guide.md. Change them together.


def _iter_directives(data: bytes):
    """Yields (name, start, end) for every real Velocity directive in `data` relevant to
    block nesting/branching (`if`/`foreach`/`macro`/`define`/`literal`/`@name` as block
    openers, plus `elseif`/`else`/`end`), in source order, following the rules SKILL.md's
    "Split" algorithm specifies: `##` line comments, `#* *#` block comments and `#[[ ]]#`
    unparsed blocks are skipped; `\\#end` is an escaped literal, not a directive; a
    directive name ends at the first character that is not a letter/digit/`_`/`@` (so
    `#end-date` IS a real `#end`, while `#endDate` is not one of the names tracked here)."""
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
    """Locates the legacy (`#else`) branch's body in an inline file: line 1 is the fixed
    header; counting block openers and `#end`s from there, the top-level `#else` is the first
    one reached while nothing opened after the header is still open, and the matching final
    `#end` closes the header's own `#if`. Returns (start, end) byte offsets spanning the
    legacy body (exclusive of the `#else`/`#end` directives), or a diagnostic string, never
    raising, for a malformed shape. Every directive after the outer `#end` is checked too: the
    whole file must parse, so a second top-level `#else` or an unmatched `#end` is rejected."""
    header = _INLINE_HEADER_RE.match(migrated)
    if not header:
        return "migrated file does not start with #if( $structures.isNewEditModeEnabled() ) on line 1"

    depth = 0
    else_body_start = None
    end_start = None
    for name, start, end in _iter_directives(migrated):
        if start < header.end():
            continue  # the header's own #if is not itself counted
        if end_start is not None:
            # trailing content: only balanced blocks may follow the outer #end
            if name in _BLOCK_OPENERS or name == b"@":
                depth += 1
            elif name == b"end":
                if depth == 0:
                    return "unmatched #end after the end of the inline block"
                depth -= 1
            elif name in (b"else", b"elseif") and depth == 0:
                return f"#{name.decode()} after the end of the inline block"
            continue
        if name == b"end":
            if depth == 0:
                end_start = start
                continue
            depth -= 1
        elif name == b"else":
            if depth == 0:
                if else_body_start is not None:
                    return "more than one top-level #else in the inline block"
                else_body_start = end
        elif name == b"elseif":
            if depth == 0 and else_body_start is not None:
                return "#elseif after the top-level #else"
        elif name in _BLOCK_OPENERS or name == b"@":
            depth += 1

    if else_body_start is None:
        return "no top-level #else branch found in migrated content (legacy code not located)"
    if end_start is None:
        return (
            "no matching top-level #end found in migrated content "
            "(unbalanced #if/#foreach/#macro/#define/#literal/#@ block)"
        )
    if depth != 0:
        return "unbalanced #if/#foreach/#macro/#define/#literal/#@ block after the inline block"
    if end_start <= else_body_start:
        return "malformed structure: #end appears before the top-level #else"
    return (else_body_start, end_start)


_MACRO_NAME_RE = re.compile(rb"\(\s*([A-Za-z_][A-Za-z0-9_\-]*)")


def macro_names(data: bytes, lo: int, hi: int) -> set[bytes]:
    """Names of the `#macro( name ...)` definitions that start between `lo` and `hi`."""
    names = set()
    for name, start, end in _iter_directives(data):
        if name == b"macro" and lo <= start < hi and (m := _MACRO_NAME_RE.match(data, end)):
            names.add(m.group(1))
    return names


def _strip_layout_newline(body: bytes) -> bytes:
    """Drops the one newline the inline layout adds after `#else` and before `#end`."""
    for nl in (b"\r\n", b"\n"):
        if body.startswith(nl):
            body = body[len(nl):]
            break
    for nl in (b"\r\n", b"\n"):
        if body.endswith(nl):
            return body[: -len(nl)]
    return body


def is_inline_migrated(data: bytes) -> bool:
    """True when `data` is an inline migration: the edit-mode wrapper opens line 1 and closes
    around a top-level `#else`. A legacy file that merely mentions `isNewEditModeEnabled` in a
    comment, a string or unrelated JavaScript is not one."""
    return not isinstance(locate_top_level_else(data.lstrip()), str)


def inline_problem(original: bytes, migrated: bytes) -> str | None:
    """The inline output keeps the legacy code verbatim under #else, next to the new code
    under #if( $structures.isNewEditModeEnabled() ). Returns why `migrated` is not a valid
    inline migration of `original`, or None. The #else branch must equal the original
    (minus the layout newlines), the #if branch must not carry legacy patterns, and the two
    branches must not define the same #macro."""
    if MIGRATED_MARKER.encode() not in migrated:
        return f"no {MIGRATED_MARKER}() branch (three-file output or blocked migration?)"

    located = locate_top_level_else(migrated)
    if isinstance(located, str):
        return located

    start, end = located
    if _strip_layout_newline(migrated[start:end]) != original:
        return "the #else branch is not the original code verbatim"

    header_end = _INLINE_HEADER_RE.match(migrated).end()
    new_branch = migrated[header_end:start].decode("utf-8", errors="replace")
    if match := LEGACY_RE.search(new_branch):
        return f"the #if branch still has legacy code ({match.group(0).strip()!r}); migrate it or offer three files"

    shared = macro_names(migrated, header_end, start) & macro_names(migrated, start, end)
    if shared:
        names = ", ".join(sorted(n.decode("utf-8", errors="replace") for n in shared))
        return f"#macro defined in both branches ({names}): the first definition wins, so the legacy editor would run the new macro"
    return None


# ─── field-body ──────────────────────────────────────────────────────────

RENDER_MODE_KEY = "newRenderMode"
RENDER_MODE_COMPONENT = "component"
FIELD_VARIABLE_CLAZZ = "com.dotcms.contenttype.model.field.ImmutableFieldVariable"


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


def cmd_field_body(workdir: Path, key: str, field_file: Path) -> dict:
    """Builds the body of `PUT /api/v3/contenttype/{typeId}/fields/{fieldId}` from the field
    as the agent just fetched it (`field_file`). For a `field` entry it first checks the field
    still holds the original text, then sets the migrated code; for a `renderMode` entry it
    only switches the render mode. Either way `newRenderMode` ends as "component" and every
    other field variable is kept. Returns a verdict: ok (with `body`) or skipped (with `reason`)."""
    manifest = read_manifest(workdir)
    entry = find_entry(manifest, key)
    if entry["kind"] == "asset":
        raise InputError(f"{entry['key']} is an asset; field-body is for fields and render-mode switches")
    field = read_json(field_file)
    if not isinstance(field, dict) or not field.get("id"):
        raise InputError(f"{field_file} must hold the field object as fetched (with its id)")

    if entry["kind"] == "field":
        migrated = workdir / "migrated" / entry["file"]
        original = workdir / "original" / entry["file"]
        if not migrated.exists() or not original.exists():
            raise InputError(f"missing the original or migrated file for {entry['key']}")
        if (field.get("values") or "") != read_text_verbatim(original):
            return {"command": "field-body", "key": entry["key"], "verdict": "skipped",
                    "reason": "changed on the server since download"}
        updated = with_component_render_mode(field)
        updated["values"] = read_text_verbatim(migrated)
    else:
        if has_component_render_mode(field):
            return {"command": "field-body", "key": entry["key"], "verdict": "skipped",
                    "reason": "already in component render mode"}
        updated = with_component_render_mode(field)
    return {"command": "field-body", "key": entry["key"], "verdict": "ok", "body": {"field": updated}}


# ─── check / payload / record / status ───────────────────────────────────


def entry_matches_only(entry: dict, only: list[str]) -> bool:
    if not only:
        return True
    return entry["key"] in only or entry.get("identifier") in only


def select_entries(manifest: dict, only: list[str]) -> list[dict]:
    """Without --only: every entry that is not finished. With --only: the named entries, plus
    the render-mode switches of any named asset (selecting an asset also switches the fields
    that load it, in the same run)."""
    named_assets = {
        e["key"] for e in manifest["entries"] if e["kind"] == "asset" and only and entry_matches_only(e, only)
    }
    selected = []
    for entry in manifest["entries"]:
        if entry_matches_only(entry, only):
            if only or entry.get("status") not in TERMINAL_STATUSES:
                selected.append(entry)
        elif (
            entry["kind"] == "renderMode"
            and set(entry.get("requires") or []) & named_assets
            and entry.get("status") not in TERMINAL_STATUSES
        ):
            selected.append(entry)
    return selected


def gate_file_entry(workdir: Path, entry: dict) -> tuple[str, str | None]:
    """The safety gates for one asset/field entry: (verdict, reason)."""
    migrated_file = workdir / "migrated" / entry["file"]
    original_file = workdir / "original" / entry["file"]
    if not migrated_file.exists():
        return "waiting", "no migrated file yet"
    if not original_file.exists():
        raise InputError(f"missing {original_file}: re-run classify and build-manifest")
    migrated, original = migrated_file.read_bytes(), original_file.read_bytes()
    if migrated == original:
        return "skipped", "migrated file is identical to original"
    problem = inline_problem(original, migrated)
    if problem:
        return "skipped", problem
    return "ready", None


def action_for(entry: dict) -> dict:
    if entry["kind"] == "asset":
        return {
            "type": "publish-asset",
            "identifier": entry["identifier"],
            "expectedInode": entry["inode"],
            "contentType": entry["contentType"],
            "binaryField": entry["binaryField"],
            "languageId": entry["languageId"],
            "fileName": entry["fileName"],
        }
    if entry["kind"] == "field":
        return {"type": "update-field", "typeId": entry["typeId"], "fieldId": entry["fieldId"]}
    return {"type": "set-render-mode", "typeId": entry["typeId"], "fieldId": entry["fieldId"]}


def cmd_check(workdir: Path, only: list[str], dry_run: bool, retry: bool) -> dict:
    """Runs the gates on the selected entries and reports a verdict per entry:
      ready    safe to publish now; `action` says what to do
      skipped  a gate failed; recorded in the manifest as skipped unless --dry-run
      waiting  no migrated file yet, or a /dA/ asset it depends on is not published yet;
               stays pending
      done     already finished (published/skipped/failed); named again only with --retry
    Read-only apart from recording `skipped` outcomes."""
    manifest = read_manifest(workdir)
    statuses = {e["key"]: e.get("status") for e in manifest["entries"]}
    results = []
    run_verdicts: dict[str, str] = {}

    for entry in select_entries(manifest, only):
        if entry.get("status") in TERMINAL_STATUSES and not retry:
            verdict, reason = "done", f"already {entry['status']}"
        elif entry["kind"] == "renderMode":
            unmet = [
                a
                for a in entry.get("requires") or []
                if statuses.get(a) != "published" and not (dry_run and run_verdicts.get(a) == "ready")
            ]
            verdict, reason = ("waiting", f"{', '.join(unmet)} not published yet") if unmet else ("ready", None)
        else:
            verdict, reason = gate_file_entry(workdir, entry)
        run_verdicts[entry["key"]] = verdict

        result = {"key": entry["key"], "label": entry["label"], "kind": entry["kind"], "verdict": verdict}
        if reason:
            result["reason"] = reason
        if verdict == "ready":
            result["action"] = action_for(entry)
        results.append(result)

        if verdict == "skipped" and not dry_run:
            record_status(workdir, manifest, entry["key"], "skipped", reason)

    counts: dict[str, int] = {}
    for r in results:
        counts[r["verdict"]] = counts.get(r["verdict"], 0) + 1
    out = {"command": "check", "dryRun": dry_run, "counts": counts, "entries": results}
    if only and not results:
        out["warning"] = f"--only {only} matched none of the {len(manifest['entries'])} manifest entries"
    return out


def find_entry(manifest: dict, key: str) -> dict:
    entry = next((e for e in manifest["entries"] if entry_matches_only(e, [key])), None)
    if not entry:
        raise InputError(f"no manifest entry with key {key!r}")
    return entry


def cmd_payload(workdir: Path, key: str) -> dict:
    """What the agent needs to publish one entry. For an asset: the migrated file as base64
    (small VTL files). A field or render-mode entry needs only its ids: use `field-body`."""
    manifest = read_manifest(workdir)
    entry = find_entry(manifest, key)
    out = {"command": "payload", "key": entry["key"], "kind": entry["kind"], **action_for(entry)}
    if entry["kind"] == "renderMode":
        return out
    migrated = workdir / "migrated" / entry["file"]
    original = workdir / "original" / entry["file"]
    if not migrated.exists():
        raise InputError(f"no migrated file at {migrated}")
    if entry["kind"] == "asset":
        out["contentBase64"] = base64.b64encode(migrated.read_bytes()).decode("ascii")
    return out


def record_status(workdir: Path, manifest: dict, key: str, status: str, reason: str | None) -> None:
    """Updates one entry and immediately persists the whole manifest, so an interrupted batch
    still leaves an accurate record of what happened to each entry attempted so far."""
    if status not in MANIFEST_STATUSES:
        raise InputError(f"unknown status {status!r}")
    for entry in manifest["entries"]:
        if entry["key"] == key:
            entry["status"] = status
            entry["statusReason"] = reason
            break
    write_json(manifest_path(workdir), manifest)


def cmd_record(workdir: Path, key: str, status: str, reason: str | None, inode: str | None) -> dict:
    """Persists the outcome of one write. For a published asset, pass the new inode from the
    publish response: it is what a later run compares against to skip re-downloading."""
    if status not in MANIFEST_STATUSES:
        raise InputError(f"status must be one of {sorted(MANIFEST_STATUSES)}")
    manifest = read_manifest(workdir)
    entry = find_entry(manifest, key)
    if status == "published" and entry["kind"] == "asset":
        if not inode:
            raise InputError("recording a published asset needs --inode (the new inode from the publish response)")
        entry["previousInode"] = entry["inode"]
        entry["inode"] = inode
    record_status(workdir, manifest, entry["key"], status, reason)
    return {"command": "record", "key": entry["key"], "status": status}


def cmd_status(workdir: Path) -> dict:
    manifest = read_manifest(workdir)
    counts: dict[str, int] = {}
    for e in manifest["entries"]:
        counts[e["status"]] = counts.get(e["status"], 0) + 1
    entries = [
        {k: e.get(k) for k in ("kind", "key", "label", "usedBy", "file", "requires", "status", "statusReason")}
        for e in manifest["entries"]
    ]
    return {"command": "status", "counts": counts, "entries": entries}


# ─── cli ─────────────────────────────────────────────────────────────────


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--workdir", type=Path, default=Path("vtl-migration"), help="default: %(default)s")
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("classify", help="classify the custom fields in scan/*.json")
    sub.add_parser("assets-plan", help="decide which /dA/ files to download")
    build = sub.add_parser("build-manifest", help="build manifest.json from the downloaded files")
    build.add_argument("--instance", help="host of the instance, recorded in the manifest")
    sub.add_parser("status", help="list the manifest entries")
    check = sub.add_parser("check", help="run the safety gates on migrated files")
    check.add_argument("--only", nargs="+", default=[], metavar="KEY", help="dA ids, identifiers or Type.field")
    check.add_argument("--dry-run", action="store_true", help="do not record skipped entries")
    check.add_argument("--retry", action="store_true", help="re-check entries that already finished")
    body = sub.add_parser("field-body", help="build the PUT body for a field or render-mode switch")
    body.add_argument("key")
    body.add_argument("--field-file", required=True, type=Path, help="the field as just fetched, as JSON")
    payload = sub.add_parser("payload", help="print what is needed to publish one entry")
    payload.add_argument("key")
    record = sub.add_parser("record", help="persist the outcome of one write")
    record.add_argument("key")
    record.add_argument("--status", required=True, choices=sorted(MANIFEST_STATUSES))
    record.add_argument("--reason")
    record.add_argument("--inode", help="new inode of a published asset")
    args = parser.parse_args(argv)

    try:
        if args.command == "classify":
            result = cmd_classify(args.workdir)
        elif args.command == "assets-plan":
            result = cmd_assets_plan(args.workdir)
        elif args.command == "build-manifest":
            result = cmd_build_manifest(args.workdir, args.instance)
        elif args.command == "status":
            result = cmd_status(args.workdir)
        elif args.command == "check":
            result = cmd_check(args.workdir, args.only, args.dry_run, args.retry)
        elif args.command == "field-body":
            result = cmd_field_body(args.workdir, args.key, args.field_file)
        elif args.command == "payload":
            result = cmd_payload(args.workdir, args.key)
        else:
            result = cmd_record(args.workdir, args.key, args.status, args.reason, args.inode)
    except InputError as e:
        print(json.dumps({"error": str(e)}), file=sys.stderr)
        return 2
    print(json.dumps(result))
    return 0


if __name__ == "__main__":
    sys.exit(main())
