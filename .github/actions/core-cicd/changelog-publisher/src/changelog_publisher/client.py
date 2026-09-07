"""HTTP client for the corpsites-headless dotCMS backend.

Read side (`_search`) locates an existing `Dotcmsbuilds` entry by version; write side
(`fire`) fires the System Workflow Publish action to create/update-and-publish it. The
bearer token is read once from the environment and never logged (Constitution II/III).
"""
from __future__ import annotations

import logging
import os
from dataclasses import dataclass

import requests

# The site backend URL comes from the DOTCMS_DEVSITE_URL repo variable — intentionally
# not hardcoded (PR #36606 review): a backend migration is then a variable change, not a
# code change. The 2026-07 authoring-backend migration is exactly why.
BASE_URL = os.environ.get("DOTCMS_DEVSITE_URL", "").rstrip("/")
TOKEN_ENV = "DOTCMS_DEVSITE_RELEASENOTES_TOKEN"
_TIMEOUT = 30

# System Workflow Publish action (no approval step → firing publishes live). See data-model.md D5.
PUBLISH_ACTION_ID = "b9d89c80-3d88-4311-8365-187323c96436"

log = logging.getLogger("changelog_publisher.client")


@dataclass(frozen=True)
class SearchHit:
    """The control-flow fields read back from a `_search` match.

    `mod_user` (the immutable user id) is the preferred identity for human-edit
    protection; `mod_user_name` (the mutable display name) is the fallback (US2).
    """

    identifier: str
    mod_user: str | None
    mod_user_name: str | None


class CorpsitesClient:
    """Thin wrapper over the corpsites Workflow/Content REST API.

    The token is read once at construction. It is sent only as the Authorization header
    and is never logged or echoed.
    """

    def __init__(self, token: str | None = None, base_url: str | None = None) -> None:
        token = token if token is not None else os.environ.get(TOKEN_ENV)
        if not token:
            raise RuntimeError(f"{TOKEN_ENV} is not set")
        base_url = base_url or BASE_URL  # resolved at call time, not def time
        if not base_url:
            raise RuntimeError("DOTCMS_DEVSITE_URL is not set")
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()
        self.session.headers.update({"Authorization": f"Bearer {token}"})

    def _search(self, version: str) -> list[SearchHit]:
        """Locate Dotcmsbuilds entries whose `minor` exactly equals `version`.

        Uses the `minor_dotraw` exact-match field. `limit: 2` so a `>1` ambiguity is
        detectable rather than silently truncated to one.
        """
        query = f"+contentType:Dotcmsbuilds +Dotcmsbuilds.minor_dotraw:{version}"
        resp = self.session.post(
            f"{self.base_url}/api/content/_search",
            json={"query": query, "limit": 2},
            timeout=_TIMEOUT,
        )
        resp.raise_for_status()
        contentlets = resp.json().get("entity", {}).get("jsonObjectView", {}).get("contentlets", [])
        return [
            SearchHit(
                identifier=c.get("identifier"),
                mod_user=c.get("modUser"),
                mod_user_name=c.get("modUserName"),
            )
            for c in contentlets
        ]

    def fire(self, contentlet: dict, *, apply: bool) -> None:
        """Fire the System Workflow Publish action with the given contentlet payload.

        Dry-run (apply=False) logs the intended action and issues no request — the
        `evergreen-tracks` safety convention.
        """
        minor = contentlet.get("minor")
        if not apply:
            log.info("DRY-RUN would fire Publish for %s", minor)
            return
        log.info("firing Publish for %s", minor)
        resp = self.session.put(
            f"{self.base_url}/api/v1/workflow/actions/{PUBLISH_ACTION_ID}/fire",
            json={"contentlet": contentlet},
            timeout=_TIMEOUT,
        )
        resp.raise_for_status()
