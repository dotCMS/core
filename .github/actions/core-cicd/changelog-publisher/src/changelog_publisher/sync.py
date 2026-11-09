"""Reconcile the auth instance's `EvergreenState` record with a Hub snapshot (`sync-site`)."""
from __future__ import annotations

import json
import logging
import sys
import time
from dataclasses import dataclass, field

import requests

from .client import CorpsitesClient, wait_for_prod
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
    if isinstance(raw, str):
        try:
            raw = json.loads(raw)
        except ValueError:
            return {}
    if isinstance(raw, dict):
        tainted = raw.get("tainted", [])
        ok = isinstance(tainted, list) and all(isinstance(v, str) for v in tainted)
        return raw if ok else {}
    return {}


def diff_fields(current: dict, desired: dict) -> list[str]:
    changed = [t for t in TRACKS if current.get(t) != desired.get(t)]
    if set(current.get("tainted") or []) != set(desired.get("tainted") or []):
        changed.append("tainted")
    return changed


def reconcile(
    client: CorpsitesClient, hub: dict, *, apply: bool, prod: CorpsitesClient | None = None
) -> SyncResult:
    """Bring the record in line with Hub.

    With a `prod` reader, prod is compared as well as auth: a push that failed earlier
    leaves auth matching Hub but prod stale, and only reading prod notices that and
    re-fires. Release rows are then checked on prod, and a write waits until it is live
    there.
    """
    hits = client.find_evergreen_state()
    if not hits:
        raise RuntimeError(
            "EvergreenState record 'evergreen-state' not found on the auth instance (prerequisite FR-044)"
        )
    if len(hits) > 1:
        raise AmbiguousMatchError("more than one 'evergreen-state' EvergreenState record")
    current = parse_record_state(hits[0].get("state"))
    prod_current = current
    if prod is not None:
        prod_hits = prod.find_evergreen_state(live=True)
        if len(prod_hits) > 1:
            raise AmbiguousMatchError("more than one 'evergreen-state' EvergreenState record on prod")
        prod_current = parse_record_state(prod_hits[0].get("state")) if prod_hits else {}

    desired: dict = {"tainted": hub["tainted"]}
    missing_rows: dict[str, str] = {}
    # With prod in play, rows and the fallback both come from prod: keeping auth's value
    # for a track whose row is missing could carry a version to prod that has no row there.
    rows, fallback = (prod, prod_current) if prod is not None else (client, current)
    for t in TRACKS:
        if rows.has_release_row(hub[t]):
            desired[t] = hub[t]
        else:
            missing_rows[t] = hub[t]
            if t in fallback:
                desired[t] = fallback[t]

    fields = diff_fields(current, desired)
    if not fields and prod is not None:
        fields = diff_fields(prod_current, desired)
        if fields:
            log.info("auth matches but prod differs in %s; re-pushing", ",".join(fields))
            current = prod_current  # report the stale side, so the diff shows what changes
    result = SyncResult("unchanged", fields, desired, current, missing_rows)
    if not fields:
        return result
    if not apply:
        result.status = "would-update"
        return result

    inode = client.fire({
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
    if prod is not None:
        if not inode:
            raise RuntimeError("fire response for EvergreenState had no inode; cannot confirm the push to prod")
        wait_for_prod(lambda: prod.is_live(inode), "EvergreenState record")
    result.status = "updated"
    return result


def sync_with_retries(
    client: CorpsitesClient, hub: dict, *, prod: CorpsitesClient | None = None
) -> SyncResult:
    attempts = len(_RETRY_DELAYS_SECONDS) + 1
    for i in range(1, attempts + 1):
        try:
            return reconcile(client, hub, apply=True, prod=prod)
        except AmbiguousMatchError:
            raise  # duplicate record is a permanent data-integrity error, not transient
        except (requests.RequestException, RuntimeError) as exc:
            log.error("attempt %d/%d failed: %s", i, attempts, exc)
            if i == attempts:
                raise
            time.sleep(_RETRY_DELAYS_SECONDS[i - 1])
