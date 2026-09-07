"""Current-track CalVer validation — defense-in-depth exclusion of CLI/LTS (FR-007).

The release workflow already gates on `release-prepare.outputs.is_latest`, which is true
only for the current-track `yy.mm.dd-##` format; this is the tool-side second line of
defense so an out-of-scope version is refused even if the tool is invoked directly.
"""
from __future__ import annotations

import re

# yy.mm.dd-## — two-digit date segments, 1-2 digit counter. Rejects `_lts_v##` and
# `dotcms-cli-*` forms (and anything v-prefixed, which the `minor` field never carries).
_CURRENT_TRACK = re.compile(r"^[0-9]{2}\.[0-9]{2}\.[0-9]{2}-[0-9]{1,2}$")


def is_current_track(version: str) -> bool:
    """True only for a current-track `yy.mm.dd-##` version (not `_lts_`, not CLI)."""
    return bool(_CURRENT_TRACK.match(version))
