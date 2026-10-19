# Contract: `manifest.json`

The manifest is the handoff contract between three actors: `pull` (writes it), the agent
running the `dot-ui-vtl-migration` skill on each file in `original/` (reads `file`/`label`/
`usedBy` to know what to migrate and where to write the result in `migrated/`, never edits the
manifest itself), and `push` (reads and rewrites it). Field meanings are in data-model.md; this
is the literal shape.

```json
{
  "baseUrl": "https://my-env.dotcms.dev",
  "pulledAt": "2026-09-28T12:00:00.000000",
  "knownMigrated": {
    "27d8569d3f029ea3ddaa2d0aaec3286a": "fae0edb3-1b58-4f6c-bdc9-1ff2ebfd689e"
  },
  "entries": [
    {
      "kind": "asset",
      "key": "0ec62ffd3666f23fc5228ef84a481b09",
      "identifier": "abc123...",
      "inode": "def456...",
      "languageId": 1,
      "contentType": "dotAsset",
      "binaryField": "asset",
      "fileName": "userID.vtl",
      "label": "dotAsset userID.vtl (0ec62ffd3666f23fc5228ef84a481b09)",
      "usedBy": ["Blog.author", "Event.organizer"],
      "file": "assets/0ec62ffd3666f23fc5228ef84a481b09__userID.vtl",
      "status": "pending",
      "statusReason": null,
      "previousInode": "abc123..."
    },
    {
      "kind": "field",
      "key": "Blog.urlTitle",
      "typeId": "type-id-123",
      "fieldId": "field-id-456",
      "label": "Blog.urlTitle",
      "usedBy": ["Blog.urlTitle"],
      "file": "fields/Blog.urlTitle.vtl",
      "status": "pending",
      "statusReason": null
    },
    {
      "kind": "renderMode",
      "key": "Blog.author",
      "typeId": "type-id-123",
      "fieldId": "field-id-789",
      "requires": ["0ec62ffd3666f23fc5228ef84a481b09"],
      "label": "Blog.author (enable component render mode)",
      "usedBy": ["Blog.author"],
      "status": "pending",
      "statusReason": null
    }
  ]
}
```

## Invariants

- `entries[].key` is unique per manifest and is what `push --only KEY` matches against
  (alongside `identifier`, for asset entries, per the draft script's existing `--only`
  matching).
- `entries[].status` starts `"pending"` from `pull` and only ever moves forward per the state
  machine in data-model.md — `push` never resets a terminal status back to `pending`.
- `entries[].statusReason` is `null` while `status == "pending"` or `"published"`, and a
  human-readable explanation once `status` is `"skipped"` or `"failed"` (FR-012).
- `usedBy` always has at least one entry — an item with no owning field would never have been
  discovered in the first place.
- `renderMode` entries have no `file` and always come after the asset/field entries, so the
  assets they `require` are processed first in the same `push`. They only ever change the
  field's `newRenderMode` variable to `component`; every other variable, and `values`, is sent
  back unchanged. An entry whose `requires` aren't all `published` stays `pending`.
- `baseUrl` must equal the current `BASE_URL` for `push` to proceed — a mismatch is a
  configuration error (exit 2) raised before any entry is touched.
- `push` never reprocesses an entry already in a terminal status (`published`, `skipped`,
  `failed`) unless it is named explicitly with `--only`. Naming a `/dA/` asset with `--only`
  also processes the `renderMode` entries that require it, but only those not yet in a
  terminal status. An entry with no migrated file yet stays `pending`, never `skipped`.
- `knownMigrated` maps a dA id to the inode that was confirmed migrated. `pull` writes it,
  seeded from the previous manifest's `knownMigrated` plus its `published` asset entries, and
  skips the binary download for an asset only while the server still reports that exact inode.
  It is rebuilt from scratch on every `pull`, holds only ids seen in that run, and is never read
  by `push`. A manifest without it (older versions) is valid.
- `previousInode` (asset entries only) is absent until `push` successfully publishes that
  entry for the first time; from then on it holds the inode that was live immediately before
  that publish, as an audit trail. It is never removed once set, and is not itself used for any
  conflict check (the conflict check compares the *current* `inode` against the live server).
- The manifest on disk after an interrupted `push` reflects every entry's *true* status at the
  moment of interruption (research.md) — an agent or customer resuming work must be able to
  trust it without re-running `pull` first, though re-running `pull` is still the correct way
  to also pick up anything that changed on the server in the meantime.

## Compatibility

This schema is internal to this feature (not exposed as a public dotCMS API or persisted
anywhere the server knows about), so it is free to evolve between versions of this script
without a formal deprecation path — but a `pull`/`push` pair from mismatched script versions
against the same `workdir` is out of scope to support; the recommended usage is always
`pull` then `push` from the same script invocation lineage.
