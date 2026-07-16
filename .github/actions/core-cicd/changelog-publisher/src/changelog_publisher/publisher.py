"""Upsert decision + contentlet payload construction.

Skeleton seam: the `PublishResult` shape is fixed here; the create/update/skip decision
and payload build land in US1 (T019) and US2 (T031).
"""
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class PublishResult:
    """Outcome of a publish attempt, consumed by the CLI to set exit code / skip marker."""

    status: str  # "created" | "updated" | "skipped"
    version: str
    reason: str | None = None


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
    raise NotImplementedError
