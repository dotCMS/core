"""CalVer release parsing, age, and ordering. No I/O."""
from __future__ import annotations

import datetime as dt
import re
from dataclasses import dataclass

# GA tags only: YY.0M.0D-NN  (e.g. 26.06.11-01). Excludes SNAPSHOT, RC, _lts*, _javaNN, markers.
GA_RE = re.compile(r"^(\d{2})\.(\d{2})\.(\d{2})-(\d{1,2})$")


@dataclass(frozen=True)
class Release:
    version: str        # original tag, e.g. "26.06.11-01"
    date: dt.date       # parsed from YY.0M.0D
    build: int          # NN

    @property
    def sort_key(self) -> tuple[dt.date, int]:
        return (self.date, self.build)


def parse_release(tag: str) -> Release | None:
    """Return a Release for a GA tag, or None for anything else."""
    m = GA_RE.match(tag)
    if not m:
        return None
    yy, mm, dd, nn = m.groups()
    try:
        date = dt.date(2000 + int(yy), int(mm), int(dd))
    except ValueError:
        return None
    return Release(version=tag, date=date, build=int(nn))


def age_days(release: Release, today: dt.date) -> int:
    return (today - release.date).days


def newest(releases: list[Release]) -> Release | None:
    """Newest by (date, build); None if the list is empty."""
    return max(releases, key=lambda r: r.sort_key, default=None)
