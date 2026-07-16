"""Upsert decision + contentlet payload construction.

US1 implements the create-and-publish path (0 search hits). Update-in-place, human-edit
protection, and the `>1` guard are the US2 decision table (T031).
"""
from __future__ import annotations

from dataclasses import dataclass

# Verified current-track stored values (T004, data-model.md). Not the frontend GraphQL
# filter artifact — these are the values a current-track contentlet actually stores.
_LTS_CURRENT = 3
_RELEASED = True
_DOWNLOAD = 1


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

    if not hits:
        contentlet = build_contentlet(
            version=version,
            release_notes=release_notes,
            docker_image=docker_image,
            released_date=released_date,
        )
        client.fire(contentlet, apply=apply)
        return PublishResult(status="created", version=version)

    # 1-hit update-in-place, human-edit-protection skip, and the >1 guard are US2 (T031).
    raise NotImplementedError("update / human-edit protection / >1 guard land in US2")
