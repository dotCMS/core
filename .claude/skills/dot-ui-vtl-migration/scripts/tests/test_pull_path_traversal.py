"""Security: a content-type/field `variable` or an asset's `fileName`, all server-controlled,
must never let `pull` write a file outside `<workdir>/original/`."""

import json

import migrate_custom_fields as mcf


def test_every_written_file_stays_inside_original_dir(malicious_paths_instance, workdir):
    mcf.pull(workdir)

    original_dir = (workdir / "original").resolve()
    written = list(original_dir.rglob("*"))
    assert written, "the malicious fixture must still produce files to migrate"
    for path in written:
        if path.is_file():
            assert path.resolve().is_relative_to(original_dir), f"{path} escaped {original_dir}"


def test_every_manifest_entry_file_resolves_inside_original_dir(malicious_paths_instance, workdir):
    mcf.pull(workdir)

    original_dir = (workdir / "original").resolve()
    manifest = json.loads((workdir / "manifest.json").read_text())
    assert manifest["entries"], "the malicious fixture must still produce manifest entries"
    for entry in manifest["entries"]:
        if "file" not in entry:  # renderMode entries write nothing locally
            continue
        resolved = (original_dir / entry["file"]).resolve()
        assert resolved.is_relative_to(original_dir), f"{entry['file']} escaped {original_dir}"
