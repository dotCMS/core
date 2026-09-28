"""contracts/manifest-schema.md: a manifest.json written by `pull` round-trips through
the manifest module and satisfies the schema's invariants — `usedBy` non-empty,
`statusReason` null while status is pending/published."""

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
