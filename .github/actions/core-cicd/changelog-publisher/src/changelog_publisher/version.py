"""Current-track CalVer validation — defense-in-depth exclusion of CLI/LTS (FR-007).

Skeleton seam; logic lands in US1 (T017).
"""
from __future__ import annotations


def is_current_track(version: str) -> bool:
    """True only for a current-track `yy.mm.dd-##` version (not `_lts_`, not CLI)."""
    raise NotImplementedError
