"""HTTP client for the corpsites-headless dotCMS backend.

Read side (`_search`) locates an existing `Dotcmsbuilds` entry by version; write side
(`fire`) fires the System Workflow Publish action to create/update-and-publish it. The
bearer token is read once from the environment and never logged (Constitution II/III).

Skeleton in this commit: config + session + method seams. The request bodies land in
US1 (`_search`, `fire`).
"""
from __future__ import annotations

import logging
import os
from dataclasses import dataclass

import requests

# corpsites-headless is the site's backend; overridable for local testing only.
BASE_URL = os.environ.get("DOTCMS_DEVSITE_URL", "https://corpsites-headless.dotcms.cloud")
TOKEN_ENV = "DOTCMS_DEVSITE_TOKEN"
_TIMEOUT = 30

# System Workflow Publish action (no approval step → firing publishes live). See data-model.md D5.
PUBLISH_ACTION_ID = "b9d89c80-3d88-4311-8365-187323c96436"

log = logging.getLogger("changelog_publisher.client")


@dataclass(frozen=True)
class SearchHit:
    """The control-flow fields read back from a `_search` match."""

    identifier: str
    mod_user_name: str | None


class CorpsitesClient:
    """Thin wrapper over the corpsites Workflow/Content REST API.

    The token is read once at construction. It is sent only as the Authorization header
    and is never logged or echoed.
    """

    def __init__(self, token: str | None = None, base_url: str = BASE_URL) -> None:
        token = token if token is not None else os.environ.get(TOKEN_ENV)
        if not token:
            raise RuntimeError(f"{TOKEN_ENV} is not set")
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()
        self.session.headers.update({"Authorization": f"Bearer {token}"})

    def _search(self, version: str) -> list[SearchHit]:
        """Locate Dotcmsbuilds entries whose `minor` exactly equals `version`."""
        raise NotImplementedError

    def fire(self, contentlet: dict, *, apply: bool) -> None:
        """Fire the System Workflow Publish action with the given contentlet payload."""
        raise NotImplementedError
