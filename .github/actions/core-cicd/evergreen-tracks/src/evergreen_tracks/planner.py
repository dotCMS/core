"""Pure promotion engine: decide each track's target release. No I/O."""
from __future__ import annotations

import datetime as dt
from dataclasses import dataclass

from .calver import Release, age_days, newest


@dataclass(frozen=True)
class TrackState:
    name: str                       # "latest" | "standard" | "trailing"
    threshold_days: int             # minimum release age to be eligible
    current_version: str | None     # version the track tag points at now, or None


@dataclass(frozen=True)
class Move:
    track: str
    target_version: str


def plan(
    releases: list[Release],
    tainted: set[str],
    held: set[str],
    tracks: list[TrackState],
    today: dt.date,
) -> list[Move]:
    """Return the tag moves to apply. Held tracks are frozen (no move emitted).

    Rules:
      - eligible = age >= threshold AND version not tainted
      - target   = newest eligible
      - forward-only: only move if target is strictly newer than current
    """
    by_version = {r.version: r for r in releases}
    moves: list[Move] = []

    for t in tracks:
        if t.name in held:
            continue  # frozen; executor reconciles track tag to the hold marker separately

        eligible = [
            r for r in releases
            if age_days(r, today) >= t.threshold_days and r.version not in tainted
        ]
        target = newest(eligible)
        if target is None:
            continue

        current = by_version.get(t.current_version) if t.current_version else None
        if current is None or target.sort_key > current.sort_key:
            if target.version != t.current_version:
                moves.append(Move(track=t.name, target_version=target.version))

    return moves
