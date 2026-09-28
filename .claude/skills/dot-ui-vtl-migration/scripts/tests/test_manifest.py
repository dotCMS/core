"""contracts/manifest-schema.md: a manifest.json written by `pull` round-trips through
the manifest module and satisfies the schema's invariants — `usedBy` non-empty,
`statusReason` null while status is pending/published."""

import json

import pytest

import migrate_custom_fields as mcf


def test_pull_manifest_round_trips_and_satisfies_schema_invariants(full_instance, workdir):
    mcf.pull(workdir)

    manifest = mcf.read_manifest(workdir)
    assert manifest["baseUrl"] == mcf.BASE_URL
    assert "pulledAt" in manifest
    assert isinstance(manifest["entries"], list) and manifest["entries"], "must have discovered something"

    for entry in manifest["entries"]:
        assert entry["status"] in mcf.MANIFEST_STATUSES
        assert entry["usedBy"], "usedBy must always have at least one owner (data-model.md invariant)"
        if entry["status"] in ("pending", "published"):
            assert entry["statusReason"] is None


def test_set_entry_status_persists_immediately_to_disk(full_instance, workdir):
    mcf.pull(workdir)
    manifest = mcf.read_manifest(workdir)
    key = manifest["entries"][0]["key"]

    mcf.set_entry_status(workdir, manifest, key, "skipped", "changed on the server since pull")

    reloaded = mcf.read_manifest(workdir)
    entry = next(e for e in reloaded["entries"] if e["key"] == key)
    assert entry["status"] == "skipped"
    assert entry["statusReason"] == "changed on the server since pull"


def test_read_manifest_converts_corrupt_json_to_config_error(workdir):
    workdir.mkdir(parents=True, exist_ok=True)
    (workdir / "manifest.json").write_text("{not valid json at all")

    with pytest.raises(mcf.ConfigError):
        mcf.read_manifest(workdir)


def test_write_manifest_is_atomic_a_crash_mid_write_does_not_corrupt_the_original(
    seeded_push_instance, workdir, monkeypatch
):
    manifest_file = workdir / "manifest.json"
    original_content = manifest_file.read_text()
    manifest = mcf.read_manifest(workdir)

    def exploding_replace(*args, **kwargs):
        raise OSError("simulated crash mid-write")

    monkeypatch.setattr(mcf.os, "replace", exploding_replace)

    with pytest.raises(OSError):
        mcf.write_manifest(workdir, manifest)

    assert manifest_file.read_text() == original_content, "the original manifest must survive an interrupted write"
    json.loads(manifest_file.read_text())  # still valid, parseable JSON
