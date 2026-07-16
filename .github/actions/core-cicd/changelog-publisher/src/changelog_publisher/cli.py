"""CLI entrypoint: `changelog-publisher publish`.

Dry-run is the default; pass --apply to search + fire the Publish action.

Exit/stdout contract (README): 0 = success or protective skip (skip is marked on stdout
with `::changelog-skip::`), 2 = usage/validation error, 1 = runtime failure. The bearer
token is read once by the client from the environment and never logged.
"""
from __future__ import annotations

import argparse
import datetime as dt
import logging
import sys

import requests

from .client import CorpsitesClient
from .publisher import publish
from .version import is_current_track

log = logging.getLogger("changelog_publisher")


def cmd_publish(args: argparse.Namespace) -> int:
    # Defense-in-depth exclusion (FR-007): refuse out-of-scope versions before any network call.
    if not is_current_track(args.version):
        log.error(
            "refusing out-of-scope version %r: not current-track (yy.mm.dd-##); "
            "CLI/LTS releases are excluded (FR-007)",
            args.version,
        )
        return 2

    with open(args.notes_file, encoding="utf-8") as fh:
        release_notes = fh.read()
    released_date = args.released_date or dt.date.today().isoformat()

    try:
        client = CorpsitesClient()
        result = publish(
            client,
            version=args.version,
            release_notes=release_notes,
            docker_image=args.docker_image,
            released_date=released_date,
            apply=args.apply,
        )
    except requests.RequestException as exc:
        # Concise, token-free failure (the token lives in the session header, not the URL/body).
        log.error("publish failed for %s: %s", args.version, exc)
        return 1

    if result.status == "skipped":
        # Machine-readable skip marker on stdout so the workflow posts skip (not failure) wording.
        print(f"::changelog-skip::{result.version} reason={result.reason}")
        return 0

    verb = "published" if args.apply else "DRY-RUN would publish"
    print(f"{verb} {result.version} ({result.status})")
    return 0


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog="changelog-publisher")
    sub = p.add_subparsers(dest="command", required=True)

    pub = sub.add_parser("publish", help="upsert + publish a release changelog entry")
    pub.add_argument("--version", required=True, help="current-track version, e.g. 26.07.10-01")
    pub.add_argument("--notes-file", required=True, help="path to the site-format markdown notes")
    pub.add_argument("--docker-image", required=True, help="deployment docker image tag")
    pub.add_argument("--released-date", default="", help="availability date (yyyy-MM-dd); defaults to today")
    pub.add_argument("--apply", action="store_true", help="actually search + fire the Publish action")
    pub.set_defaults(func=cmd_publish)
    return p


def main(argv: list[str] | None = None) -> int:
    logging.basicConfig(level=logging.INFO, format="%(message)s")
    args = build_parser().parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
