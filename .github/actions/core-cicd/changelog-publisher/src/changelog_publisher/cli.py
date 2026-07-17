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
import os
import sys

import requests

from .client import CorpsitesClient
from .publisher import AmbiguousMatchError, publish
from .version import is_current_track

log = logging.getLogger("changelog_publisher")

# Service-account identity (a modUser id) for human-edit protection (FR-011). CLI wins over
# env; either may be unset for a create-only run (a 1-hit then skips protectively).
_SERVICE_ACCOUNT_ENV = "DOTCMS_DEVSITE_SERVICE_ACCOUNT"


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

    service_account = args.service_account or os.environ.get(_SERVICE_ACCOUNT_ENV, "")

    try:
        client = CorpsitesClient()
        result = publish(
            client,
            version=args.version,
            release_notes=release_notes,
            docker_image=args.docker_image,
            released_date=released_date,
            service_account=service_account or None,
            force=args.force,
            apply=args.apply,
        )
    except (requests.RequestException, AmbiguousMatchError, RuntimeError) as exc:
        # Concise, token-free failure (the token lives in the session header, not the URL/body).
        # RuntimeError covers a missing DOTCMS_DEVSITE_TOKEN — a clean one-line error, no traceback.
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
    pub.add_argument("--notes-file", required=True, help="path to the release-notes markdown (normally the GitHub release body)")
    pub.add_argument("--docker-image", required=True, help="deployment docker image tag")
    pub.add_argument("--released-date", default="", help="availability date (yyyy-MM-dd); defaults to today")
    pub.add_argument("--apply", action="store_true", help="actually search + fire the Publish action")
    pub.add_argument(
        "--service-account", default="",
        help=f"service-account modUser id for human-edit protection (or ${_SERVICE_ACCOUNT_ENV})",
    )
    pub.add_argument(
        "--force", action="store_true",
        help="override human-edit protection and update in place (manual operator use only)",
    )
    pub.set_defaults(func=cmd_publish)
    return p


def main(argv: list[str] | None = None) -> int:
    logging.basicConfig(level=logging.INFO, format="%(message)s")
    args = build_parser().parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
