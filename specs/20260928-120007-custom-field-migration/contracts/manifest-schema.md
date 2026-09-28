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
