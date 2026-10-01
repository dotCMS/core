"""Reconcile the corpsites `EvergreenState` record with a Hub snapshot (`sync-site`)."""
from __future__ import annotations

import json
import logging
import sys
import time
from dataclasses import dataclass, field

import requests

from .client import CorpsitesClient
from .publisher import AmbiguousMatchError
from .version import is_current_track

log = logging.getLogger("changelog_publisher.sync")

TRACKS = ("latest", "standard", "trailing")
RECORD_TITLE = "evergreen-state"
_RETRY_DELAYS_SECONDS = (60, 180)
_READBACK_ATTEMPTS = 10
_READBACK_DELAY_SECONDS = 2.0


@dataclass
class SyncResult:
    status: str  # unchanged | would-update | updated
    fields: list[str]
    desired: dict
    current: dict
    missing_rows: dict[str, str] = field(default_factory=dict)


def load_state_file(path: str) -> dict:
    with open(path, encoding="utf-8") as fh:
        hub = json.load(fh)
    if not isinstance(hub, dict) or set(hub) != {*TRACKS, "tainted"}:
        raise ValueError("state file must be an object with latest, standard, trailing, tainted")
    tainted = hub["tainted"]
    if not isinstance(tainted, list):
        raise ValueError("tainted must be a list")
    for v in [*(hub[t] for t in TRACKS), *tainted]:
        if not isinstance(v, str) or not is_current_track(v):
            raise ValueError(f"not a current-track version: {v!r}")
    return hub


def parse_record_state(raw) -> dict:
    if isinstance(raw, dict):
        return raw
    if isinstance(raw, str):
        try:
            parsed = json.loads(raw)
        except ValueError:
            return {}
        return parsed if isinstance(parsed, dict) else {}
    return {}


def diff_fields(current: dict, desired: dict) -> list[str]:
    changed = [t for t in TRACKS if current.get(t) != desired.get(t)]
    if set(current.get("tainted") or []) != set(desired.get("tainted") or []):
        changed.append("tainted")
    return changed


def reconcile(client: CorpsitesClient, hub: dict, *, apply: bool) -> SyncResult:
    hits = client.find_evergreen_state()
    if not hits:
        raise RuntimeError(
            "EvergreenState record 'evergreen-state' not found on corpsites (prerequisite FR-044)"
        )
    if len(hits) > 1:
        raise AmbiguousMatchError("more than one 'evergreen-state' EvergreenState record")
    current = parse_record_state(hits[0].get("state"))

    desired: dict = {"tainted": hub["tainted"]}
    missing_rows: dict[str, str] = {}
    for t in TRACKS:
        if client.has_release_row(hub[t]):
            desired[t] = hub[t]
        else:
            missing_rows[t] = hub[t]
            if t in current:
                desired[t] = current[t]

    fields = diff_fields(current, desired)
    result = SyncResult("unchanged", fields, desired, current, missing_rows)
    if not fields:
        return result
    if not apply:
        result.status = "would-update"
        return result

    client.fire({
        "contentType": "EvergreenState",
        "identifier": hits[0]["identifier"],
        "title": RECORD_TITLE,
        "state": json.dumps(desired, separators=(",", ":")),
    }, apply=True)
    for _ in range(_READBACK_ATTEMPTS):
        back = client.find_evergreen_state()
        if back and not diff_fields(parse_record_state(back[0].get("state")), desired):
            break
        time.sleep(_READBACK_DELAY_SECONDS)
    else:
        print("warning: EvergreenState read-back did not match after publish", file=sys.stderr)
    result.status = "updated"
    return result


def sync_with_retries(client: CorpsitesClient, hub: dict, *, attempts: int = 3,
                      sleep=time.sleep) -> SyncResult:
    for i in range(1, attempts + 1):
        try:
            return reconcile(client, hub, apply=True)
        except (requests.RequestException, RuntimeError) as exc:
            log.error("attempt %d/%d failed: %s", i, attempts, exc)
            if i == attempts:
                raise
            sleep(_RETRY_DELAYS_SECONDS[i - 1])
