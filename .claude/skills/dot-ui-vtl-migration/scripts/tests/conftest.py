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
        # A valid-looking token by default, so tests don't each have to set one just to
        # get past preflight_auth()'s "DOTCMS_TOKEN must be set" check. A test that
        # specifically wants to exercise the missing-token path overrides this back to
        # "" *after* calling mock_dotcms(...).
        monkeypatch.setattr(mcf, "DOTCMS_TOKEN", "test-fixture-default-token")
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
        self.publish_status = 200
        self.publish_new_inode = "inode-shared-002"
        self.publish_response_body: bytes | None = None  # override: raw bytes instead of JSON
        self.field_update_status = 200
        self.publish_calls = 0
        self.field_update_calls = 0
        self.content_get_status = 200  # asset conflict-check GET status (simulate 5xx/transient)
        self.field_get_status = 200  # field conflict-check GET status (simulate 5xx/transient)
        self.v3_puts: list[tuple[str, dict]] = []  # (fieldId, request body) for every v3 field PUT
        # Custom fields as the server holds them, keyed by field id. Each carries an
        # unrelated field variable (hideLabel) so tests can prove it is preserved.
        self.fields: dict[str, dict] = {
            "field-blog-author": {
                "typeId": "ct-blog",
                "variable": "author",
                "values": "<script>dojo.ready(function(){});</script>",  # matches "Blog.author"
                "fieldVariables": [hide_label_variable("field-blog-author")],
            }
        }

    @property
    def field_values(self) -> str:
        return self.fields["field-blog-author"]["values"]

    @field_values.setter
    def field_values(self, value: str) -> None:
        self.fields["field-blog-author"]["values"] = value


def hide_label_variable(field_id: str) -> dict:
    return {
        "clazz": "com.dotcms.contenttype.model.field.ImmutableFieldVariable",
        "fieldId": field_id,
        "id": f"var-hidelabel-{field_id}",
        "key": "hideLabel",
        "value": "true",
    }


def field_json(field_id: str, stored: dict) -> dict:
    return {
        "clazz": "com.dotcms.contenttype.model.field.ImmutableCustomField",
        "contentTypeId": stored["typeId"],
        "id": field_id,
        "variable": stored["variable"],
        "values": stored["values"],
        "fieldVariables": copy.deepcopy(stored["fieldVariables"]),
    }


def build_push_server_handler(state: PushServerState):
    def handler(request: httpx.Request) -> httpx.Response:
        path = request.url.path
        parts = path.strip("/").split("/")

        if path == "/api/v1/contenttype":  # preflight_auth()
            return json_response(load_json("contenttype_list_page1.json"))

        if path == "/api/v1/content/ident-shared-001":
            if state.content_get_status != 200:
                return json_response({"message": "server error"}, status=state.content_get_status)
            return json_response({"entity": {"inode": state.asset_inode, "identifier": "ident-shared-001"}})

        # GET /api/v1/contenttype/{typeId}/fields/id/{fieldId}
        if request.method == "GET" and parts[:3] == ["api", "v1", "contenttype"] and parts[4:6] == ["fields", "id"]:
            stored = state.fields.get(parts[6])
            if state.field_get_status != 200:
                return json_response({"message": "server error"}, status=state.field_get_status)
            if stored is None or stored["typeId"] != parts[3]:
                return json_response({"message": "not found"}, status=404)
            return json_response({"entity": field_json(parts[6], stored)})

        # PUT /api/v3/contenttype/{typeId}/fields/{fieldId}  body: {"field": {...}}
        if request.method == "PUT" and parts[:3] == ["api", "v3", "contenttype"] and parts[4] == "fields":
            field_id = parts[5]
            body = json.loads(request.content)
            state.field_update_calls += 1
            state.v3_puts.append((field_id, body))
            if state.field_update_status == 200:
                # Mirrors a real server: values and the whole variables list are replaced.
                sent = body["field"]
                variables = [{**v, "id": v.get("id") or f"var-new-{v['key']}"} for v in sent.get("fieldVariables", [])]
                state.fields[field_id]["values"] = sent["values"]
                state.fields[field_id]["fieldVariables"] = variables
            return json_response({"entity": []}, status=state.field_update_status)

        if path == "/api/v1/workflow/actions/default/fire/PUBLISH":
            state.publish_calls += 1
            if state.publish_response_body is not None:
                return httpx.Response(state.publish_status, content=state.publish_response_body)
            if state.publish_status == 200:
                # Mirrors a real server: the identifier's current inode is now the new one.
                state.asset_inode = state.publish_new_inode
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


def render_mode_entry(key: str, type_id: str, field_id: str, requires: list[str]) -> dict:
    return {
        "kind": "renderMode",
        "key": key,
        "typeId": type_id,
        "fieldId": field_id,
        "requires": requires,
        "label": f"{key} (enable component render mode)",
        "usedBy": [key],
        "status": "pending",
        "statusReason": None,
    }


@pytest.fixture
def seeded_render_mode_instance(mock_dotcms, workdir):
    """The seeded push workdir plus two render-mode-only entries: "Blog.teaser" loads the
    "asset-a" dA file (so it may only be flagged once that asset is published) and
    "Page.done" whose code was already migrated (no dependency)."""
    state = PushServerState()
    state.fields["field-blog-teaser"] = {
        "typeId": "ct-blog",
        "variable": "teaser",
        "values": '#dotParse("/dA/asset-a")',
        "fieldVariables": [hide_label_variable("field-blog-teaser")],
    }
    state.fields["field-page-done"] = {
        "typeId": "ct-page",
        "variable": "done",
        "values": "#if( $structures.isNewEditModeEnabled() )\nnew\n#else\nold\n#end",
        "fieldVariables": [],
    }
    transport = mock_dotcms(build_push_server_handler(state))
    manifest = seed_push_workdir(workdir)
    manifest["entries"].append(render_mode_entry("Blog.teaser", "ct-blog", "field-blog-teaser", ["asset-a"]))
    manifest["entries"].append(render_mode_entry("Page.done", "ct-page", "field-page-done", []))
    mcf.write_manifest(workdir, manifest)
    return transport, manifest, state


# ─── one asset whose live inode a test can advance (publish) or reset (revert) ─────
#
# Drives a full pull -> push -> pull flow against a single dA asset, so tests can prove a
# re-pull skips the binary download when the inode is unchanged since our last publish,
# and falls back to a real download when it isn't (FR-013).

WIDGET_DOWNLOAD_PATH = "/dA/asset-w1/asset/widget.vtl"


class ReversionableAssetState:
    def __init__(self) -> None:
        self.inode = "inode-w1-v1"
        self.migrated_inode = "inode-w1-v2"
        self.download_calls = 0
        self.publish_calls = 0
        self.body_variables: list[dict] = []  # Widget.body's field variables on the server

    def body_field(self) -> dict:
        return {
            "id": "field-widget-body",
            "variable": "body",
            "clazz": mcf.CUSTOM_FIELD_CLAZZ,
            "values": '#dotParse("/dA/asset-w1")',
            "fieldVariables": copy.deepcopy(self.body_variables),
        }


def build_reversionable_asset_handler(state: ReversionableAssetState):
    def handler(request: httpx.Request) -> httpx.Response:
        path = request.url.path
        params = dict(request.url.params)

        if path == "/api/v1/contenttype":
            if params.get("page") == "2":
                return json_response({"entity": []})
            return json_response({"entity": [{"id": "ct-widget", "variable": "Widget"}]})

        if path == "/api/v1/contenttype/id/ct-widget":
            return json_response({"entity": {"id": "ct-widget", "variable": "Widget", "fields": [state.body_field()]}})

        if path == "/api/v1/contenttype/ct-widget/fields/id/field-widget-body":
            return json_response({"entity": state.body_field()})

        if path == "/api/v3/contenttype/ct-widget/fields/field-widget-body" and request.method == "PUT":
            state.body_variables = json.loads(request.content)["field"]["fieldVariables"]
            return json_response({"entity": []})

        if path == "/api/v1/content/asset-w1":
            return json_response(
                {
                    "entity": {
                        "identifier": "asset-w1",
                        "inode": state.inode,
                        "languageId": 1,
                        "contentType": "dotAsset",
                        "assetContentAsset": "hash",
                        "assetVersion": WIDGET_DOWNLOAD_PATH,
                        "fileName": "widget.vtl",
                        "live": True,
                    }
                }
            )

        if path == WIDGET_DOWNLOAD_PATH:
            state.download_calls += 1
            if state.inode == state.migrated_inode:
                return text_response(f"#if( $structures.{mcf.MIGRATED_MARKER}() )\nnew\n#else\nold\n#end")
            return text_response("<script>dojo.ready(function(){});</script>")

        if path == "/api/v1/workflow/actions/default/fire/PUBLISH":
            state.publish_calls += 1
            state.inode = state.migrated_inode
            return json_response({"entity": {"inode": state.inode}})

        raise AssertionError(f"unexpected request in reversionable_asset fixture: {request.method} {request.url}")

    return handler


@pytest.fixture
def reversionable_asset_instance(mock_dotcms):
    state = ReversionableAssetState()
    transport = mock_dotcms(build_reversionable_asset_handler(state))
    return transport, state


def write_inline_migration(workdir: Path, entry: dict) -> None:
    """Simulates the agent's migration step for one pulled entry: wraps the original
    bytes verbatim under #else, the way the skill's inline mode does."""
    original = (workdir / "original" / entry["file"]).read_bytes()
    migrated = b"#if( $structures.isNewEditModeEnabled() )\n<p>new</p>\n#else\n" + original + b"\n#end"
    (workdir / "migrated" / entry["file"]).write_bytes(migrated)


# ─── a /dA/<id> where <id> is a pinned inode, not the identifier ───────────────────


def build_pinned_asset_handler():
    def handler(request: httpx.Request) -> httpx.Response:
        path = request.url.path
        params = dict(request.url.params)

        if path == "/api/v1/contenttype":
            if params.get("page") == "2":
                return json_response({"entity": []})
            return json_response({"entity": [{"id": "ct-pinned", "variable": "Pinned"}]})

        if path == "/api/v1/contenttype/id/ct-pinned":
            return json_response(
                {
                    "entity": {
                        "id": "ct-pinned",
                        "variable": "Pinned",
                        "fields": [
                            {
                                "id": "field-pinned-code",
                                "variable": "code",
                                "clazz": mcf.CUSTOM_FIELD_CLAZZ,
                                "values": '#dotParse("/dA/inode-pinned-001")',
                            }
                        ],
                    }
                }
            )

        if path == "/api/v1/content/inode-pinned-001":
            # The content API resolves the pinned inode, but reports the real identifier.
            return json_response(
                {
                    "entity": {
                        "identifier": "ident-real-001",
                        "inode": "inode-pinned-001",
                        "languageId": 1,
                        "contentType": "dotAsset",
                        "assetContentAsset": "hash",
                        "assetVersion": "/dA/inode-pinned-001/asset/pinned.vtl",
                        "fileName": "pinned.vtl",
                        "live": True,
                    }
                }
            )

        raise AssertionError(f"unexpected request in pinned_asset fixture: {request.method} {request.url}")

    return handler


@pytest.fixture
def pinned_asset_instance(mock_dotcms):
    return mock_dotcms(build_pinned_asset_handler())


# ─── pull: which custom fields need the newRenderMode=component flag ──────────────
#
# Legacy.code          -> dA asset that still needs migrating: renderMode entry that
#                         depends on that asset
# Done.inlineDone      -> inline code already migrated, no flag: renderMode entry, no deps
# Done.inlineFlagged   -> inline code already migrated, already flagged: nothing to do
# Done.legacyInline    -> inline legacy code: a "field" entry (push flags it in the same PUT)
# Shared.a / .b / .c   -> share one dA asset whose code is already migrated; .a has no
#                         flag, .b is already "component", .c is explicitly "iframe"


def render_mode_variable(field_id: str, value: str) -> dict:
    return {
        "clazz": "com.dotcms.contenttype.model.field.ImmutableFieldVariable",
        "fieldId": field_id,
        "id": f"var-rendermode-{field_id}",
        "key": "newRenderMode",
        "value": value,
    }


def custom_field(field_id: str, variable: str, values: str, variables: list[dict]) -> dict:
    return {
        "id": field_id,
        "variable": variable,
        "clazz": "com.dotcms.contenttype.model.field.ImmutableCustomField",
        "values": values,
        "fieldVariables": variables,
    }


MIGRATED_VTL = "#if( $structures.isNewEditModeEnabled() )\n<p>new</p>\n#else\n<p>old</p>\n#end"
LEGACY_VTL = "<script>dojo.ready(function(){ dijit.byId('x'); });</script>"

RENDER_MODE_TYPES = {
    "ct-legacy": {
        "id": "ct-legacy",
        "variable": "Legacy",
        "fields": [custom_field("f-legacy-code", "code", '#dotParse("/dA/asset-legacy")', [hide_label_variable("f-legacy-code")])],
    },
    "ct-done": {
        "id": "ct-done",
        "variable": "Done",
        "fields": [
            custom_field("f-done-inline", "inlineDone", MIGRATED_VTL, []),
            custom_field("f-done-flagged", "inlineFlagged", MIGRATED_VTL, [render_mode_variable("f-done-flagged", "component")]),
            custom_field("f-done-legacy", "legacyInline", LEGACY_VTL, []),
        ],
    },
    "ct-shared": {
        "id": "ct-shared",
        "variable": "Shared",
        "fields": [
            custom_field("f-shared-a", "a", '#dotParse("/dA/asset-done")', []),
            custom_field("f-shared-b", "b", '#dotParse("/dA/asset-done")', [render_mode_variable("f-shared-b", "component")]),
            custom_field("f-shared-c", "c", '#dotParse("/dA/asset-done")', [render_mode_variable("f-shared-c", "iframe")]),
        ],
    },
}


def asset_content(da_id: str, file_name: str) -> dict:
    return {
        "entity": {
            "identifier": da_id,
            "inode": f"inode-{da_id}",
            "languageId": 1,
            "contentType": "dotAsset",
            "assetContentAsset": "hash",
            "assetVersion": f"/dA/{da_id}/asset/{file_name}",
            "fileName": file_name,
            "live": True,
        }
    }


def build_render_mode_pull_handler():
    def handler(request: httpx.Request) -> httpx.Response:
        path = request.url.path
        params = dict(request.url.params)

        if path == "/api/v1/contenttype":
            if params.get("page") == "2":
                return json_response({"entity": []})
            return json_response({"entity": [{"id": t["id"], "variable": t["variable"]} for t in RENDER_MODE_TYPES.values()]})

        if path.startswith("/api/v1/contenttype/id/"):
            return json_response({"entity": copy.deepcopy(RENDER_MODE_TYPES[path.rsplit("/", 1)[-1]])})

        if path == "/api/v1/content/asset-legacy":
            return json_response(asset_content("asset-legacy", "legacy.vtl"))
        if path == "/api/v1/content/asset-done":
            return json_response(asset_content("asset-done", "done.vtl"))
        if path == "/dA/asset-legacy/asset/legacy.vtl":
            return text_response(LEGACY_VTL)
        if path == "/dA/asset-done/asset/done.vtl":
            return text_response(MIGRATED_VTL)

        raise AssertionError(f"unexpected request in render_mode_pull fixture: {request.method} {request.url}")

    return handler


@pytest.fixture
def render_mode_pull_instance(mock_dotcms):
    return mock_dotcms(build_render_mode_pull_handler())
