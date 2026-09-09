# Phase 1 Data Model: Asset Picker upload restriction

**Feature**: `specs/37365-asset-picker-upload-scope` | **Date**: 2026-09-03

No persisted entity, no API payload and no store slice changes. The "data model" here is the
in-memory shape of the restriction as it travels from the entry point to each upload surface, plus
the message keys that describe it to the user.

---

## 1. The restriction value

### `mimeTypes: string[] | undefined`

Already present on `DotAssetPickerConfig`
(`core-web/libs/ui/src/lib/components/dot-asset-picker/store/models.ts`). **No shape change.** This
plan only adds readers.

| Property | Value |
|---|---|
| Origin | `buildAssetPickerConfig()` — from `ASSET_PICKER_MIME_TYPES[mode]`, or caller-supplied for `browse` |
| Read from | `store.config()?.mimeTypes` |
| Absent means | No restriction. `file` and `browse` (without caller-supplied types) produce `undefined` |
| Values today | `['image/*']`, `['video/*']`, `['audio/*']`, or arbitrary caller strings in `browse` |

**Invariant**: absence, not a sentinel, is the unrestricted state. Every new reader must treat
`undefined` and `[]` identically and permissively. A reader that defaults to "restrict everything"
when the value is missing would break the File field (AC-007).

### Derived: `acceptAttribute: string | null`

| | |
|---|---|
| Derivation | `mimeTypes.join(',')`, or `null` when there is no restriction |
| Consumer | `[attr.accept]` on the hidden `<input type="file" #fileInput>` |
| Why `null`, not `''` | `[attr.accept]="null"` removes the attribute; `''` leaves an empty `accept`, which is not the same thing to the browser |

### Derived: `restrictionLabel: string | undefined`

The human name for the restriction, already translated.

| | |
|---|---|
| Derivation | family (segment before `/`) of the `mimeTypes` entries → message key → translated string |
| Known families | `image`, `video`, `audio` |
| Unknown family | Fall back to listing the raw patterns, so the message is still correct if less friendly |
| Mixed families | Join the resolved labels; the picker produces single-family restrictions today, but a `browse` caller may pass several |
| Absent when | There is no restriction |
| Consumers | The refusal toast (AC-004), and the Asset/File prompt's scoped descriptions (AC-005) |

---

## 2. Validation rules

Applied by the pure matcher in `upload-restriction.ts`. Sourced from the spec's AC-004 and AC-010
and the Q2 decision.

| # | Input | Result | Source |
|---|---|---|---|
| V1 | No restriction (`undefined` / `[]`) | **Accept** | AC-007 |
| V2 | `file.type` empty or missing | **Accept** — the server remains the authority | AC-010 |
| V3 | Pattern `family/*`, `file.type` in that family | **Accept** | AC-001 |
| V4 | Pattern `family/*`, `file.type` in another family | **Reject** | AC-001 |
| V5 | Exact pattern (`application/pdf`), equal ignoring case | **Accept** | `browse` callers |
| V6 | Exact pattern, not equal | **Reject** | `browse` callers |
| V7 | Several patterns | **Accept** if any matches | — |

**Deliberately not a rule**: the filename extension is never consulted. Adding an extension fallback
would reintroduce the hand-maintained list AC-002 forbids — see
[research.md R5](./research.md#r5--matching-a-file-against-image-and-the-unclassifiable-case).

**Scope of the check**: the file that would actually be uploaded. The picker warns on a multi-file
selection and uploads only the first (unchanged, tracked separately in #37166), so the guard judges
that file. See spec Assumptions.

---

## 3. State transitions — where the guard sits

All four routes converge on `#resolveFilesUpload()`, which is the mandatory gate. `onRequestUpload()`
carries an early copy so a dropped file is refused before the user is asked to pick a storage type.

```text
  Upload button                         Drag and drop
       │                                      │
       ▼                                      ▼
  onUpload()                          onRequestUpload()  ◀── EARLY GUARD (AC-003)
       │                                      │
  folder pins a base type?             folder pins a base type?
       │                                      │
   yes ├── fileInput.click()            yes ──┼─────────────┐
       │        │                             │             │
    no │        │                          no │             │
       ▼        │                             ▼             │
  Asset/File popover                   Asset/File modal     │
       │        │                             │             │
       ▼        │                             ▼             │
  onUploadTypeSelected()  ────────────────────┤             │
       │        │                             │             │
       ├── fileInput.click()                  │             │
       │        │                             │             │
       ▼        ▼                             ▼             ▼
   onFileChange()  ──────────────▶  #resolveFilesUpload()  ◀── MANDATORY GUARD
                                              │                (AC-004, AC-009)
                                              ▼
                                    #uploadByBaseType()
```

**Rejected transition**: guard fails → no upload request, an error toast naming the allowed types,
and the picker stays open on the same folder. Nothing else is mutated: no `$activeSelection` left
dangling, no prompt opened, no list refresh.

**Why both gates**: `#resolveFilesUpload()` alone would satisfy AC-004 and AC-009 for all four
routes, but a dropped PDF would first open the prompt and make the user choose a storage type before
being refused. The early gate is UX, not correctness — the mandatory one is the guarantee.

---

## 4. Component contract deltas

| Component | Change | Compatibility |
|---|---|---|
| `DotAssetPickerComponent` | Internal only — a computed `accept`, a computed label, a private guard | None public |
| `DotUploadTypeSelectorComponent` | **One new optional input**: the already-translated restriction label | Absent ⇒ today's exact option list and description keys. Content Drive passes nothing (AC-008) |
| `DotUploadDropzoneComponent` | **None** | Untouched by design ([R3](./research.md#r3--refusing-a-dropped-file-without-touching-the-shared-dropzone)) |
| `DotAssetPickerConfig` | **None** | No new field — the value already exists |

The selector takes an already-translated string rather than a mode or a key so that
`DotMessageService` stays out of the shared component's new branch, and the picker remains the only
place that knows what its own restriction is called.

---

## 5. Message keys

New keys in `dotCMS/src/main/webapp/WEB-INF/messages/Language.properties`. Existing keys are **not**
edited — Content Drive keeps rendering them (AC-008).

| Purpose | Count | Notes |
|---|---|---|
| Refusal toast summary + detail | 2 | Detail takes the restriction label as a parameter |
| Family labels (`image`, `video`, `audio`) | 3 | The human name; also feeds the scoped descriptions |
| Scoped option descriptions (Asset, File) | 2 | Parameterized with the family label; used only when the label input is set |

Unchanged and still the default: `content-drive.dialog.upload-selector.asset.description`,
`content-drive.dialog.upload-selector.file.description`, and the existing upload toasts.

Exact key names and wording are settled in
[contracts/upload-restriction.contract.md](./contracts/upload-restriction.contract.md).
