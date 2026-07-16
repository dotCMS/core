"""CLI entrypoint: `changelog-publisher publish`.

Dry-run is the default; pass --apply to search + fire the Publish action.

Skeleton seam; argument parsing and wiring land in US1 (T020) / US3 (T037).
"""
from __future__ import annotations

import sys


def main(argv: list[str] | None = None) -> int:
    raise NotImplementedError


if __name__ == "__main__":
    sys.exit(main())
