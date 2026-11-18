"""Marker-tag naming and parsing. State lives in registry tags. No I/O."""
from __future__ import annotations

# Track names, conservative -> fresh order is irrelevant here; this is the closed set.
TRACKS = ("latest", "standard", "trailing")

_TAINT_SUFFIX = "_tainted"
_HOLD_SUFFIX = "_hold"

TAINT_TITLE_MARKER = "⚠️ TAINTED "


def taint_title(title: str) -> str:
    return title if title.startswith(TAINT_TITLE_MARKER) else TAINT_TITLE_MARKER + title


def untaint_title(title: str) -> str:
    return title.removeprefix(TAINT_TITLE_MARKER)


def taint_tag(version: str) -> str:
    return f"{version}{_TAINT_SUFFIX}"


def hold_tag(track: str) -> str:
    return f"{track}{_HOLD_SUFFIX}"


def tainted_versions(tags: list[str]) -> set[str]:
    """Versions quarantined from advancing, derived from <version>_tainted markers."""
    return {
        t[: -len(_TAINT_SUFFIX)]
        for t in tags
        if t.endswith(_TAINT_SUFFIX)
    }


def held_tracks(tags: list[str]) -> set[str]:
    """Tracks frozen by a <track>_hold marker. Unknown track names are ignored."""
    out = set()
    for t in tags:
        if t.endswith(_HOLD_SUFFIX):
            name = t[: -len(_HOLD_SUFFIX)]
            if name in TRACKS:
                out.add(name)
    return out
