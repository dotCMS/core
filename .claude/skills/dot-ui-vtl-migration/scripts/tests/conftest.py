"""Shared fixtures for migrate_custom_fields.py's test suite.

Every test talks to a fake dotCMS instance via `httpx.MockTransport` (research.md's
mocking decision) — no real network call is ever made. `mock_dotcms` installs a routing
handler as the script's module-level `client` and records every request it received, so
tests can assert both on responses returned AND on which requests were (or weren't) made
(e.g. "push --dry-run issued zero PUT/POST/DELETE").
"""

from __future__ import annotations

import copy
import json
import sys
from pathlib import Path

import httpx
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import migrate_custom_fields as mcf  # noqa: E402

FIXTURES = Path(__file__).parent / "fixtures"


def load_json(name: str) -> dict:
    """Returns a deep copy so tests can freely mutate what they get back (e.g. to
    simulate a changed inode) without polluting the fixture for other tests."""
    return copy.deepcopy(json.loads((FIXTURES / name).read_text()))


def load_text(name: str) -> str:
    return (FIXTURES / name).read_text()


def json_response(data: dict, status: int = 200) -> httpx.Response:
    return httpx.Response(status, json=data)


def text_response(text: str, status: int = 200) -> httpx.Response:
    return httpx.Response(status, text=text)


def parse_last_json(stdout: str) -> dict:
    """The CLI Result contract (FR-015): the last non-empty line of stdout is one
    JSON object."""
    lines = [line for line in stdout.splitlines() if line.strip()]
    return json.loads(lines[-1])


class RecordingTransport(httpx.MockTransport):
    """A MockTransport that remembers every request it handled."""

    def __init__(self, handler):
        self.requests: list[httpx.Request] = []

        def recording_handler(request: httpx.Request) -> httpx.Response:
            self.requests.append(request)
            return handler(request)

        super().__init__(recording_handler)

    def requests_with_method(self, *methods: str) -> list[httpx.Request]:
        wanted = {m.upper() for m in methods}
        return [r for r in self.requests if r.method in wanted]


@pytest.fixture
def mock_dotcms(monkeypatch):
    """`mock_dotcms(handler)` installs `handler(request) -> httpx.Response` as the
    script's HTTP client and returns the RecordingTransport so the test can inspect
    `.requests` afterwards."""

    def install(handler) -> RecordingTransport:
        transport = RecordingTransport(handler)
        test_client = httpx.Client(base_url=mcf.BASE_URL, transport=transport)
        monkeypatch.setattr(mcf, "client", test_client)
        return transport

    return install


@pytest.fixture
def workdir(tmp_path: Path) -> Path:
    return tmp_path / "vtl-migration"


# ─── the "full instance" scenario shared by every US1 pull test ───────────────────
#
# 4 content types: Blog.author and Event.organizer both point at the SAME shared
# dotAsset (asset-shared-001) — the dedup case (FR-003). Banner.widget has in-field
# legacy VTL. Page has one core-file field, one non-/dA #dotParse field, one clean
# inline field, and one already-migrated field — covering all 5 FR-002 categories.


class InstanceCallLog:
    """Counts calls to endpoints tests care about being deduplicated/idempotent."""

    def __init__(self) -> None:
        self.content_type_detail_calls: list[str] = []
        self.asset_resolve_calls: list[str] = []
        self.asset_download_calls: list[str] = []


def build_full_instance_handler(log: InstanceCallLog):
    def handler(request: httpx.Request) -> httpx.Response:
        path = request.url.path
        params = dict(request.url.params)

        if path == "/api/v1/contenttype":
            if params.get("per_page") == "1":  # preflight_auth()
                return json_response(load_json("contenttype_list_page1.json"))
            if params.get("page") == "2":
                return json_response(load_json("contenttype_list_page2.json"))
            return json_response(load_json("contenttype_list_page1.json"))

        if path.startswith("/api/v1/contenttype/id/"):
            ct_id = path.rsplit("/", 1)[-1]
            log.content_type_detail_calls.append(ct_id)
            fixture = {
                "ct-blog": "contenttype_blog.json",
                "ct-event": "contenttype_event.json",
                "ct-banner": "contenttype_banner.json",
                "ct-page": "contenttype_page.json",
            }[ct_id]
            return json_response(load_json(fixture))

        if path == "/api/v1/content/asset-shared-001":
            log.asset_resolve_calls.append("asset-shared-001")
            return json_response(load_json("content_asset_shared.json"))

        if path == "/dA/asset-shared-001/asset/userID.vtl":
            log.asset_download_calls.append("asset-shared-001")
            return text_response(load_text("asset_shared_original.vtl"))

        raise AssertionError(f"unexpected request in full_instance fixture: {request.method} {request.url}")

    return handler


@pytest.fixture
def full_instance(mock_dotcms):
    log = InstanceCallLog()
    transport = mock_dotcms(build_full_instance_handler(log))
    return transport, log


# ─── a single-content-type instance where everything is ALREADY migrated ──────────
#
# Used by the idempotency tests (FR-013): one field loads a dA asset whose downloaded
# body already contains the modern-editor marker, and one field has the marker inline.


def build_already_migrated_instance_handler(log: InstanceCallLog):
    def handler(request: httpx.Request) -> httpx.Response:
        path = request.url.path
        params = dict(request.url.params)

        if path == "/api/v1/contenttype":
            if params.get("per_page") == "1":  # preflight_auth()
                return json_response(load_json("contenttype_list_migrated_page1.json"))
            if params.get("page") == "2":
                return json_response(load_json("contenttype_list_migrated_page2.json"))
            return json_response(load_json("contenttype_list_migrated_page1.json"))

        if path == "/api/v1/contenttype/id/ct-migrated":
            log.content_type_detail_calls.append("ct-migrated")
            return json_response(load_json("contenttype_migrated.json"))

        if path == "/api/v1/content/asset-migrated-001":
            log.asset_resolve_calls.append("asset-migrated-001")
            return json_response(load_json("content_asset_migrated.json"))

        if path == "/dA/asset-migrated-001/asset/already.vtl":
            log.asset_download_calls.append("asset-migrated-001")
            return text_response(load_text("asset_migrated_already.vtl"))

        raise AssertionError(f"unexpected request in already_migrated_instance fixture: {request.method} {request.url}")

    return handler


@pytest.fixture
def already_migrated_instance(mock_dotcms):
    log = InstanceCallLog()
    transport = mock_dotcms(build_already_migrated_instance_handler(log))
    return transport, log


# ─── an instance whose content-type/field/fileName strings are path-traversal attempts ──
#
# One content type named "../../evil" with an in-field legacy VTL field ("widget" — exercises
# Scan.add_field's owner->file path) and a /dA/ reference whose fileName is
# "../../../../etc/passwd" (exercises the asset-name->file path).


def build_malicious_paths_instance_handler():
    def handler(request: httpx.Request) -> httpx.Response:
        path = request.url.path
        params = dict(request.url.params)

        if path == "/api/v1/contenttype":
            if params.get("per_page") == "1":  # preflight_auth()
                return json_response(load_json("contenttype_list_malicious_page1.json"))
            if params.get("page") == "2":
                return json_response(load_json("contenttype_list_malicious_page2.json"))
            return json_response(load_json("contenttype_list_malicious_page1.json"))

        if path == "/api/v1/contenttype/id/ct-malicious":
            return json_response(load_json("contenttype_malicious.json"))

        if path == "/api/v1/content/asset-malicious-001":
            return json_response(load_json("content_asset_malicious.json"))

        if path == "/dA/asset-malicious-001/asset/passwd":
            return text_response(load_text("asset_malicious_original.vtl"))

        raise AssertionError(f"unexpected request in malicious_paths fixture: {request.method} {request.url}")

    return handler


@pytest.fixture
def malicious_paths_instance(mock_dotcms):
    transport = mock_dotcms(build_malicious_paths_instance_handler())
    return transport


# ─── an instance whose /dA/ asset has no fileName/name/title at all ──────────────
#
# Must be reported as unresolved, not crash pull() with an uncaught KeyError.


def build_noname_asset_instance_handler():
    def handler(request: httpx.Request) -> httpx.Response:
        path = request.url.path
        params = dict(request.url.params)

        if path == "/api/v1/contenttype":
            if params.get("per_page") == "1":  # preflight_auth()
                return json_response(load_json("contenttype_list_noname_page1.json"))
            if params.get("page") == "2":
                return json_response(load_json("contenttype_list_noname_page2.json"))
            return json_response(load_json("contenttype_list_noname_page1.json"))

        if path == "/api/v1/contenttype/id/ct-noname":
            return json_response(load_json("contenttype_noname.json"))

        if path == "/api/v1/content/asset-noname-001":
            return json_response(load_json("content_asset_noname.json"))

        if path == "/dA/asset-noname-001/asset/unnamed":
            return text_response(load_text("asset_noname_original.vtl"))

        raise AssertionError(f"unexpected request in noname_asset fixture: {request.method} {request.url}")

    return handler


@pytest.fixture
def noname_asset_instance(mock_dotcms):
    transport = mock_dotcms(build_noname_asset_instance_handler())
    return transport


# ─── a ready-to-push workdir (manifest + original/migrated files already on disk) ──
#
# Used by push tests (US2/US3): simulates the state right after `pull` + the agent's
# migration step, before any `push` has run. Two pending entries: one asset, one field.


def seed_push_workdir(workdir: Path) -> dict:
    """Writes manifest.json plus matching original/migrated files for 2 pending
    entries ("asset-a" and "Blog.author") and returns the manifest dict."""
    (workdir / "original" / "assets").mkdir(parents=True, exist_ok=True)
    (workdir / "original" / "fields").mkdir(parents=True, exist_ok=True)
    (workdir / "migrated" / "assets").mkdir(parents=True, exist_ok=True)
    (workdir / "migrated" / "fields").mkdir(parents=True, exist_ok=True)

    asset_original = load_text("asset_shared_original.vtl")
    asset_migrated = (
        f"#if( $structures.isNewEditModeEnabled() )\n<p>migrated</p>\n#else\n{asset_original}\n#end"
    )
    (workdir / "original" / "assets" / "asset-a__userID.vtl").write_text(asset_original)
    (workdir / "migrated" / "assets" / "asset-a__userID.vtl").write_text(asset_migrated)

    field_original = "<script>dojo.ready(function(){});</script>"
    field_migrated = (
        f"#if( $structures.isNewEditModeEnabled() )\n<p>migrated</p>\n#else\n{field_original}\n#end"
    )
    (workdir / "original" / "fields" / "Blog.author.vtl").write_text(field_original)
    (workdir / "migrated" / "fields" / "Blog.author.vtl").write_text(field_migrated)

    manifest = {
        "baseUrl": mcf.BASE_URL,
        "pulledAt": "2026-09-28T12:00:00",
        "entries": [
            {
                "kind": "asset",
                "key": "asset-a",
                "identifier": "ident-shared-001",
                "inode": "inode-shared-001",
                "languageId": 1,
                "contentType": "dotAsset",
                "binaryField": "asset",
                "fileName": "userID.vtl",
                "label": "dotAsset userID.vtl (asset-a)",
                "usedBy": ["Blog.author"],
                "file": "assets/asset-a__userID.vtl",
                "status": "pending",
                "statusReason": None,
            },
            {
                "kind": "field",
                "key": "Blog.author",
                "typeId": "ct-blog",
                "fieldId": "field-blog-author",
                "label": "Blog.author",
                "usedBy": ["Blog.author"],
                "file": "fields/Blog.author.vtl",
                "status": "pending",
                "statusReason": None,
            },
        ],
    }
    mcf.write_manifest(workdir, manifest)
    return manifest


class PushServerState:
    """The live dotCMS instance's current state, as `push`'s conflict/publish requests
    would see it. Defaults to matching what `seed_push_workdir` recorded in the
    manifest (no conflict); a test mutates a field to simulate a conflict or a
    failure, per FR-009/FR-011."""

    def __init__(self) -> None:
        self.asset_inode = "inode-shared-001"  # matches the "asset-a" manifest entry
        self.field_values = "<script>dojo.ready(function(){});</script>"  # matches "Blog.author"
        self.publish_status = 200
        self.publish_new_inode = "inode-shared-002"
        self.field_update_status = 200
        self.publish_calls = 0
        self.field_update_calls = 0


def build_push_server_handler(state: PushServerState):
    def handler(request: httpx.Request) -> httpx.Response:
        path = request.url.path

        if path == "/api/v1/contenttype":  # preflight_auth()
            return json_response(load_json("contenttype_list_page1.json"))

        if path == "/api/v1/content/ident-shared-001":
            return json_response({"entity": {"inode": state.asset_inode, "identifier": "ident-shared-001"}})

        if path == "/api/v1/contenttype/ct-blog/fields/id/field-blog-author":
            if request.method == "GET":
                return json_response(
                    {"entity": {"id": "field-blog-author", "variable": "author", "values": state.field_values}}
                )
            if request.method == "PUT":
                state.field_update_calls += 1
                return json_response({"entity": {"id": "field-blog-author"}}, status=state.field_update_status)

        if path == "/api/v1/workflow/actions/default/fire/PUBLISH":
            state.publish_calls += 1
            return json_response({"entity": {"inode": state.publish_new_inode}}, status=state.publish_status)

        raise AssertionError(f"unexpected request in push_server fixture: {request.method} {request.url}")

    return handler


@pytest.fixture
def seeded_push_instance(mock_dotcms, workdir):
    """A workdir already seeded by `pull` + migration, backed by a fake server whose
    state (`PushServerState`) starts matched to the manifest — a test mutates it to
    simulate a conflict, a publish failure, etc."""
    state = PushServerState()
    transport = mock_dotcms(build_push_server_handler(state))
    manifest = seed_push_workdir(workdir)
    return transport, manifest, state
