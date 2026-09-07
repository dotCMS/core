"""CLI entrypoint: `evergreen-tracks promote` and `evergreen-tracks admin`.

Dry-run is the default everywhere. Pass --apply to mutate the registry.
"""
from __future__ import annotations

import argparse
import datetime as dt
import logging
import os
import sys

from .calver import age_days, newest, parse_release
from .executor import delete_tag, hub_login, point_tag
from .markers import TRACKS, held_tracks, hold_tag, tainted_versions, taint_tag
from .planner import TrackState, plan
from .registry import list_tags

log = logging.getLogger("evergreen_tracks")

# How many months the narrow read path walks back before giving up and full-scanning.
# Releases ship weekly, so promotion needs one or two months; the extra headroom is for
# a quiet stretch or a track parked on something old by a hold.
_MONTH_WALK_LIMIT = 6


def _state(repo: str):
    """Return (releases, tainted, held, name->digest) read from the registry.

    The full ~79-page listing. Still the right tool for `admin` (rare, human-triggered,
    and it addresses arbitrarily old versions) and the terminal fallback for promote.
    """
    tags = list_tags(repo)
    names = [t.name for t in tags]
    digests = {t.name: t.digest for t in tags}
    releases = [r for r in (parse_release(n) for n in names) if r is not None]
    return releases, tainted_versions(names), held_tracks(names), digests


def _month_str(date: dt.date) -> str:
    return f"{date.year % 100:02d}.{date.month:02d}"


def _prev_month(ym: str) -> str:
    yy, mm = int(ym[:2]), int(ym[3:5])
    return f"{yy - 1:02d}.12" if mm == 1 else f"{yy:02d}.{mm - 1:02d}"


def _promote_state(repo: str, wanted: list[str], max_days: int, today: dt.date):
    """`_state`, assembled from a handful of filtered reads instead of the full listing.

    Moving a floating tag needs a few GA CalVer tags and three marker names, but the
    full listing pages the whole repo — 7.8k tags, 79 calls, ~45s, against a Hub budget
    of 180 requests/60s per IP. Hub's `name=` substring filter turns that into:

      * one call per requested track  -> the `<track>` tag's digest, plus its
        `<track>_hold` marker if one exists (both match the track name as a substring)
      * one call per month of history -> that month's GA releases AND their
        `<version>_tainted` markers, which share the version's month prefix

    A month is one page (6-28 tags), so the daily standard+trailing promote is 3-4 calls.

    Months are fetched newest-first and CONTIGUOUSLY, which is what makes the narrowed
    pool safe for `planner.plan`'s forward-only guard. That guard only matters when a
    track already sits on something NEWER than the newest eligible release — and any
    such version lives in a month at or above the one the walk stopped on, so it has
    necessarily already been read. A track on something OLDER resolves to "move
    forward", which is the same decision the full listing produces. Nothing here can
    move a track backwards that the full scan would have held.

    Returns None if the walk window turns up nothing promotable at all, so the caller
    can fall back to the full listing rather than silently reporting "no moves" — a
    quiet stall is exactly how a broken read path would look.
    """
    digests: dict[str, str] = {}
    names: list[str] = []

    def absorb(tags) -> None:
        for tag in tags:
            digests.setdefault(tag.name, tag.digest)
            names.append(tag.name)

    # Track tags and their hold markers. `name=standard` also matches `standard_hold`;
    # `name=latest` additionally drags in a dozen `*_latest_SNAPSHOT` build tags, which
    # parse as neither releases nor markers and are simply ignored.
    for track in wanted:
        absorb(list_tags(repo, name_filter=track))

    month = _month_str(today)
    for walked in range(_MONTH_WALK_LIMIT):
        absorb(list_tags(repo, name_filter=month))
        releases = [r for r in (parse_release(n) for n in names) if r is not None]
        tainted = tainted_versions(names)
        if any(age_days(r, today) >= max_days and r.version not in tainted for r in releases):
            log.info("read registry state in %d filtered call(s)", len(wanted) + walked + 1)
            return releases, tainted, held_tracks(names), digests
        month = _prev_month(month)
    return None


def _current_version(track: str, digests: dict[str, str], releases) -> str | None:
    """Which GA version the floating <track> tag currently points at, by digest match."""
    track_digest = digests.get(track)
    if not track_digest:
        return None
    matches = [r for r in releases if digests.get(r.version) == track_digest]
    if not matches:
        return None
    return newest(matches).version


def cmd_promote(args: argparse.Namespace) -> int:
    # Optional subset (e.g. --tracks latest): the release pipeline invokes this
    # engine on-demand to move only `latest` the instant a GA ships, while an
    # operator manually dispatches a full promote to age standard/trailing.
    # One engine, two triggers.
    #
    # Resolved BEFORE any registry read, so an unwanted track costs no calls at all.
    wanted = set(TRACKS)
    if args.tracks:
        wanted = {t.strip() for t in args.tracks.split(",") if t.strip()}
        unknown = wanted - set(TRACKS)
        if unknown:
            log.error("unknown track(s): %s", ", ".join(sorted(unknown)))
            return 2
    thresholds = {
        "latest": args.latest_days,
        "standard": args.standard_days,
        "trailing": args.trailing_days,
    }
    ordered = [t for t in TRACKS if t in wanted]      # stable, TRACKS order
    today = dt.date.today()

    state = _promote_state(args.repo, ordered, max(thresholds[t] for t in ordered), today)
    if state is None:
        log.info("no promotable release in the filtered window; reading the full tag listing")
        state = _state(args.repo)
    releases, tainted, held, digests = state
    held = held & wanted

    tracks = [
        TrackState(t, thresholds[t], _current_version(t, digests, releases)) for t in ordered
    ]

    moves = plan(releases, tainted, held, tracks, today=today)

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


def _delete_marker(repo: str, marker: str, *, apply: bool) -> int:
    """Delete a marker tag. Only logs into Hub when applying, so dry-runs need no creds."""
    token = ""
    if apply:
        username = os.environ.get("DOCKER_USERNAME")
        token_val = os.environ.get("DOCKER_TOKEN")
        if not username or not token_val:
            log.error("DOCKER_USERNAME and DOCKER_TOKEN must be set to apply this action")
            return 2
        token = hub_login(username, token_val)
    delete_tag(repo, marker, token, apply=apply)
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
            return _delete_marker(args.repo, marker, apply=args.apply)
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
            return _delete_marker(args.repo, marker, apply=args.apply)
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
    # Log to stdout (not the default stderr) so the plan is the command's stdout:
    # the promote workflow captures a dry-run's stdout and diffs it against the
    # approved plan, and uv's own chatter stays on stderr where it's discarded.
    logging.basicConfig(level=logging.INFO, format="%(message)s", stream=sys.stdout)
    args = build_parser().parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
