"""A /dA/ asset with no fileName/name/title at all must be reported as unresolved (and
counted under the final result's `failed`), not crash the whole `pull()` run."""

import json

import migrate_custom_fields as mcf
from conftest import parse_last_json


def test_asset_missing_all_name_fields_is_unresolved_not_a_crash(noname_asset_instance, workdir, capsys):
    mcf.pull(workdir)

    manifest = json.loads((workdir / "manifest.json").read_text())
    assert manifest["entries"] == []

    result = parse_last_json(capsys.readouterr().out)
    assert result["counts"]["failed"] == 1
