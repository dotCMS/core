"""CLI entrypoint: `evergreen-tracks promote` and `evergreen-tracks admin`.

Dry-run is the default everywhere. Pass --apply to mutate the registry.
"""
from __future__ import annotations

import argparse
import datetime as dt
import logging
import os
import sys

from .calver import parse_release
from .executor import delete_tag, hub_login, point_tag
from .markers import TRACKS, held_tracks, hold_tag, tainted_versions, taint_tag
from .planner import TrackState, plan
from .registry import list_tags

log = logging.getLogger("evergreen_tracks")


def _state(repo: str):
    """Return (releases, tainted, held, name->digest) read from the registry."""
    tags = list_tags(repo)
    names = [t.name for t in tags]
    digests = {t.name: t.digest for t in tags}
    releases = [r for r in (parse_release(n) for n in names) if r is not None]
    return releases, tainted_versions(names), held_tracks(names), digests


def _current_version(track: str, digests: dict[str, str], releases) -> str | None:
    """Which GA version the floating <track> tag currently points at, by digest match."""
    track_digest = digests.get(track)
    if not track_digest:
        return None
    matches = [r for r in releases if digests.get(r.version) == track_digest]
    if not matches:
        return None
    return max(matches, key=lambda r: r.sort_key).version


def cmd_promote(args: argparse.Namespace) -> int:
    releases, tainted, held, digests = _state(args.repo)
    tracks = [
        TrackState("latest", args.latest_days, _current_version("latest", digests, releases)),
        TrackState("standard", args.standard_days, _current_version("standard", digests, releases)),
        TrackState("trailing", args.trailing_days, _current_version("trailing", digests, releases)),
    ]

    # Optional subset (e.g. --tracks latest): the release pipeline invokes this
    # engine on-demand to move only `latest` the instant a GA ships, while the
    # daily cron ages standard/trailing. One engine, two triggers.
    if args.tracks:
        wanted = {t.strip() for t in args.tracks.split(",") if t.strip()}
        unknown = wanted - set(TRACKS)
        if unknown:
            log.error("unknown track(s): %s", ", ".join(sorted(unknown)))
            return 2
        tracks = [t for t in tracks if t.name in wanted]
        held = held & wanted

    moves = plan(releases, tainted, held, tracks, today=dt.date.today())

    # Held tracks are frozen against promotion; instead reconcile the floating
    # <track> tag to its <track>_hold marker digest so a divergence self-heals.
    for track in held:
        marker = hold_tag(track)
        hold_digest = digests.get(marker)
        if hold_digest is None:
            continue
        if digests.get(track) == hold_digest:
            log.info("%s: held at %s, skipping promotion", track, marker)
            continue
        log.info("%s (held) -> reconcile to %s (%s)", track, marker, hold_digest)
        point_tag(args.repo, track, hold_digest, apply=args.apply)

    if not moves:
        log.info("no track moves needed")
        return 0
    for m in moves:
        digest = digests[m.target_version]
        log.info("%s -> %s (%s)", m.track, m.target_version, digest)
        point_tag(args.repo, m.track, digest, apply=args.apply)
    return 0


def cmd_admin(args: argparse.Namespace) -> int:
    releases, tainted, held, digests = _state(args.repo)

    if args.action in ("taint", "untaint"):
        if not parse_release(args.version):
            log.error("not a GA version: %s", args.version)
            return 2
        marker = taint_tag(args.version)
        if args.action == "taint":
            if args.version not in digests:
                log.error("version %s not found in %s", args.version, args.repo)
                return 2
            point_tag(args.repo, marker, digests[args.version], apply=args.apply)
        else:
            username = os.environ.get("DOCKER_USERNAME")
            token_val = os.environ.get("DOCKER_TOKEN")
            if not username or not token_val:
                log.error("DOCKER_USERNAME and DOCKER_TOKEN must be set for untaint")
                return 2
            token = hub_login(username, token_val)
            delete_tag(args.repo, marker, token, apply=args.apply)
        return 0

    if args.action in ("hold", "release-hold"):
        if args.track not in TRACKS:
            log.error("unknown track: %s", args.track)
            return 2
        marker = hold_tag(args.track)
        if args.action == "hold":
            if not parse_release(args.version) or args.version not in digests:
                log.error("hold needs an existing GA --version; got %s", args.version)
                return 2
            if args.version in tainted and not args.force:
                log.error("refusing to hold %s onto tainted %s (use --force)",
                          args.track, args.version)
                return 2
            point_tag(args.repo, marker, digests[args.version], apply=args.apply)
            point_tag(args.repo, args.track, digests[args.version], apply=args.apply)
        else:
            username = os.environ.get("DOCKER_USERNAME")
            token_val = os.environ.get("DOCKER_TOKEN")
            if not username or not token_val:
                log.error("DOCKER_USERNAME and DOCKER_TOKEN must be set for release-hold")
                return 2
            token = hub_login(username, token_val)
            delete_tag(args.repo, marker, token, apply=args.apply)
        return 0

    log.error("unknown action: %s", args.action)
    return 2


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog="evergreen-tracks")
    sub = p.add_subparsers(dest="command", required=True)

    pr = sub.add_parser("promote", help="advance track tags by release age")
    pr.add_argument("--repo", required=True)
    pr.add_argument("--apply", action="store_true", help="actually mutate the registry")
    pr.add_argument("--tracks", default="",
                    help="comma-separated subset to move (latest,standard,trailing); default all")
    pr.add_argument("--latest-days", type=int, default=0)
    pr.add_argument("--standard-days", type=int, default=14)
    pr.add_argument("--trailing-days", type=int, default=28)
    pr.set_defaults(func=cmd_promote)

    ad = sub.add_parser("admin", help="taint / untaint / hold / release-hold")
    ad.add_argument("--repo", required=True)
    ad.add_argument("--apply", action="store_true", help="actually mutate the registry")
    ad.add_argument("--action", required=True,
                    choices=["taint", "untaint", "hold", "release-hold"])
    ad.add_argument("--version", default="")
    ad.add_argument("--track", default="")
    ad.add_argument("--force", action="store_true")
    ad.set_defaults(func=cmd_admin)
    return p


def main(argv: list[str] | None = None) -> int:
    logging.basicConfig(level=logging.INFO, format="%(message)s")
    args = build_parser().parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
