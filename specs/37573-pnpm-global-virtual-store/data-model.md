# Phase 1 Data Model: Adopt pnpm's global virtual store for git worktrees

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md)

This feature stores no application data — no database table, no search-index document, no API
payload. It does, however, introduce one durable record type that outlives the change and has to be
readable by a maintainer months from now, so it is modelled here rather than left implicit in YAML.

---

## Entity: Correction

One manifest defect in one third-party package, and the declaration added to compensate for it.
Lives as a `packageExtensions` entry in `core-web/pnpm-workspace.yaml` with an adjacent comment
carrying the fields the YAML cannot hold.

### Fields

| Field | Meaning | Where it lives | Required |
|---|---|---|---|
| `package` | Name of the offending package | `packageExtensions` key | yes |
| `versionRange` | Semver range the correction was validated against | `packageExtensions` key suffix (`pkg@1.0.1`) | yes |
| `undeclaredImport` | The specifier the package imports without declaring | Comment | yes |
| `addedField` | `peerDependencies`, optionally with `peerDependenciesMeta` | Entry body | yes |
| `addedRange` | Version range for the added peer — must be satisfied by `core-web`'s own direct dependency | Entry body | yes |
| `optional` | Whether the import is conditional, making the peer optional | `peerDependenciesMeta.<name>.optional` | no |
| `discoveredBy` | `scan` or `build` — how it was found | Comment | yes |
| `upstreamIssue` | Link to the upstream report, once filed | Comment | no |

### Rules

- **R1 — Peer, never dependency.** `addedField` is `peerDependencies`. Adding the module as a
  `dependency` risks a second instance of a singleton package (`yjs`, `rxjs`, `@angular/*`), which
  fails at runtime or, worse, silently. See
  [research R-003](./research.md#r-003--peer-dependency-not-dependency-the-load-bearing-decision).
- **R2 — The range key is the expiry.** `versionRange` must be the exact version validated, not a
  loose range. A bump past it stops the correction applying, which is the condition FR-018 and
  FR-019 exist to surface.
- **R3 — The peer must have a consumer.** `addedRange` must be satisfiable by a direct dependency of
  `core-web`. A peer with no consumer instance produces an unmet-peer warning and does not fix the
  resolution.
- **R4 — One correction per defect.** A package importing two undeclared modules gets two entries in
  the same body, not a merged one, so each can be removed independently as upstream fixes land.
- **R5 — Every correction is traceable.** `undeclaredImport` and `discoveredBy` are mandatory: a
  future maintainer must be able to tell whether an upgrade makes the entry removable without
  re-deriving the investigation.

### Lifecycle

```text
discovered (scan or build)
    └─> validated   — build green under the global store with the entry present
          └─> committed  — entry + regenerated lockfile, FR-014 verified
                ├─> superseded  — upstream declares it; entry removed, nightly confirms
                └─> expired     — package bumped past versionRange; nightly goes red, entry
                                  is re-validated and re-keyed, or removed
```

The `expired` transition is the whole reason the nightly detector exists. Nothing else notices it.

---

## Entity: Scan Finding

Produced by `core-web/tools/scan-undeclared-imports.mjs`; transient, not committed. It is the input
to Correction, and it is the artifact the Red gate asserts on (a scan that finds nothing is broken).

| Field | Meaning |
|---|---|
| `package` | Package containing the undeclared import |
| `version` | Its installed version |
| `specifier` | The bare import specifier found in its shipped JavaScript |
| `sourceFile` | The file it was found in, for verification |
| `declared` | Whether the specifier appears in any of the package's dependency fields |
| `rescuedFrom` | Where it currently resolves — `core-web` top level means accidental rescue |

Only findings with `declared: false` **and** `rescuedFrom` pointing at `core-web`'s own top-level
directory become Corrections. The rest are noise: a package may import something genuinely optional
and guarded, and the walk-up is irrelevant if nothing resolves it today either.

---

## Not modelled

- **The store setting itself.** It is machine state in the developer's own pnpm config, deliberately
  outside the repository (FR-006). The repository never reads it and cannot assert on it.
- **Asset baselines.** The 217 / 1068 file counts are constants in
  `core-web/tools/assert-dist-assets.mjs`, re-taken on this branch during the Red phase. A constant
  in a script, not a record.
