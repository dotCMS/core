"""Upsert decision + contentlet payload construction.

US1 implements the create-and-publish path (0 search hits). Update-in-place, human-edit
protection, and the `>1` guard are the US2 decision table (T031).
"""
from __future__ import annotations

import sys
import time
from dataclasses import dataclass

# Verified current-track stored values (T004, data-model.md). Not the frontend GraphQL
# filter artifact — these are the values a current-track contentlet actually stores.
_LTS_CURRENT = 3
_RELEASED = True
_DOWNLOAD = 1


class AmbiguousMatchError(RuntimeError):
    """Raised when >1 entry matches a version — the tool refuses to guess (D4)."""


# dotCMS _search is Elasticsearch-backed and indexes asynchronously after Publish fires.
# After a create, wait until the new row is searchable before exiting, so a re-run started
# after this run finished cannot miss it and double-create (the workflow's concurrency
# group serializes truly concurrent runs). Timeout is a warning, not a failure — the
# write itself succeeded.
_INDEX_POLL_ATTEMPTS = 10
_INDEX_POLL_DELAY_SECONDS = 2.0


def _wait_until_searchable(client, version: str) -> bool:
    for _ in range(_INDEX_POLL_ATTEMPTS):
        time.sleep(_INDEX_POLL_DELAY_SECONDS)
        if client._search(version):
            return True
    return False


@dataclass(frozen=True)
class PublishResult:
    """Outcome of a publish attempt, consumed by the CLI to set exit code / skip marker."""

    status: str  # "created" | "updated" | "skipped"
    version: str
    reason: str | None = None


def build_contentlet(
    *,
    version: str,
    release_notes: str,
    docker_image: str,
    released_date: str,
    identifier: str | None = None,
) -> dict:
    """Build the fire payload's contentlet.

    `disabledWYSIWYG: ["releaseNotes"]` is ALWAYS present or the markdown collapses into a
    single `<p>` on the site (FR-005). `identifier` is included only on the update path —
    its presence is what makes the fire an update-in-place instead of a create (FR-003).
    """
    contentlet = {
        "contentType": "Dotcmsbuilds",
        "minor": version,
        "releaseNotes": release_notes,
        "dockerImage": docker_image,
        "releasedDate": released_date,
        "showInChangeLog": True,
        "lts": _LTS_CURRENT,
        "released": _RELEASED,
        "download": _DOWNLOAD,
        "disabledWYSIWYG": ["releaseNotes"],
    }
    if identifier:
        contentlet["identifier"] = identifier
    return contentlet


def publish(
    client,
    *,
    version: str,
    release_notes: str,
    docker_image: str,
    released_date: str,
    service_account: str | None = None,
    force: bool = False,
    apply: bool,
) -> PublishResult:
    """Search for an existing entry, decide create/update/skip, and fire Publish."""
    hits = client._search(version)

    # >1 hit → never guess which row to update; surface as a failure (D4).
    if len(hits) > 1:
        raise AmbiguousMatchError(
            f"{len(hits)} entries match version {version}; refusing to guess which to update"
        )

    if not hits:
        contentlet = build_contentlet(
            version=version,
            release_notes=release_notes,
            docker_image=docker_image,
            released_date=released_date,
        )
        client.fire(contentlet, apply=apply)
        if apply and not _wait_until_searchable(client, version):
            print(
                f"warning: created entry for {version} is not yet searchable; "
                "an immediate re-run may not see it",
                file=sys.stderr,
            )
        return PublishResult(status="created", version=version)

    # Exactly one hit → update-in-place, unless a human last touched it (FR-011).
    hit = hits[0]
    if not force and not _owned_by_service_account(hit, service_account):
        return PublishResult(
            status="skipped",
            version=version,
            reason=(
                f"last modified by {hit.mod_user_name or hit.mod_user} "
                f"(not the automation service account); use --force to override"
            ),
        )

    contentlet = build_contentlet(
        version=version,
        release_notes=release_notes,
        docker_image=docker_image,
        released_date=released_date,
        identifier=hit.identifier,
    )
    client.fire(contentlet, apply=apply)
    return PublishResult(status="updated", version=version)


def _owned_by_service_account(hit, service_account: str | None) -> bool:
    """True if the automation service account was the last modifier.

    Compares the immutable `modUser` id when present (a display-name rename must not
    silently disable FR-011 protection); falls back to `modUserName` only when the entry
    carries no `modUser` id. See data-model.md.
    """
    if not service_account:
        return False
    owner = hit.mod_user if hit.mod_user else hit.mod_user_name
    return owner is not None and owner == service_account
