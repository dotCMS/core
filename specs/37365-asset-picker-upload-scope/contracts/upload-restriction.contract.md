# Contract: Asset Picker upload restriction

**Feature**: `specs/37365-asset-picker-upload-scope` | **Date**: 2026-09-03

The picker exposes no REST endpoint. Its contracts are the **UI surfaces** other code and other
teams depend on: a shared component's inputs, a rendered DOM attribute, and a message bundle. Those
are what this document pins down, because breaking one of them is how AC-007 and AC-008 get
violated by accident.

---

## C1 — `DotUploadTypeSelectorComponent` public API

The only public API change in this fix.

### Existing inputs (unchanged)

| Input | Type | Default |
|---|---|---|
| `targetFolder` | `TreeNodeData \| undefined` | `undefined` |
| `files` | `FileList \| undefined` | `undefined` |

### New input

| Input | Type | Default | Meaning |
|---|---|---|---|
| `restrictionLabel` | `string` | `''` | The already-translated human name for what the host allows (e.g. `"images"`). Empty means unrestricted. |

**Contract rules**:

1. **Absent or empty ⇒ today's behavior exactly.** Both options render, with
   `content-drive.dialog.upload-selector.asset.description` and
   `...file.description`. Content Drive passes nothing, so its output is unchanged. *This is the
   AC-008 guarantee and must be asserted, not assumed.*
2. **Set ⇒ both options still render.** The option list is never filtered. Per the Q1 decision, both
   `DOTASSET` and `FILEASSET` can legitimately hold an image, a video or an audio file, and the
   per-folder default upload-type preference can pin either.
3. **Set ⇒ the scoped description keys are used**, parameterized with the label (C3).
4. The component does **not** translate. It receives a translated string and interpolates it. Adding
   `DotMessageService` lookups for the restriction inside this shared component is out of contract.
5. `selectUploadType` emits the same `DotUploadSelection` shape regardless. The restriction changes
   what is *said*, never what is *emitted*.

### Unchanged by contract

`DotUploadDropzoneComponent` gains no input and changes no behavior. Its spec file should need no
edit; if it does, the design in [research.md R3](../research.md#r3--refusing-a-dropped-file-without-touching-the-shared-dropzone)
was not followed.

---

## C2 — Rendered DOM contract

### The hidden file input

`dot-asset-picker.component.html`, currently `<input type="file" (change)="onFileChange($event)" #fileInput hidden />`.

| Picker state | Rendered |
|---|---|
| Media mode (`image`) | `accept="image/*"` |
| Media mode (`video` / `audio`) | `accept="video/*"` / `accept="audio/*"` |
| `browse` with caller-supplied types | `accept` = those types, joined with `,` |
| `file` mode | **no `accept` attribute at all** |
| `browse` with no caller types | **no `accept` attribute at all** |

**Contract rule**: in the unrestricted cases the attribute is *absent*, not empty. `accept=""` is a
different thing to the browser, and a test asserting `toBe('')` would pass against a broken
implementation. Assert absence.

### Test hooks

Existing `data-testid`s are relied on and must not be renamed:
`asset-picker-dropzone`, `asset-picker-upload-selector`, `asset-picker-upload-popover`,
`asset-picker-upload-modal`, `upload-selector-option-<baseType>`,
`upload-selector-recommended`, `upload-selector-settings-hint`.

---

## C3 — Message bundle contract

New keys in `dotCMS/src/main/webapp/WEB-INF/messages/Language.properties`. The picker's own keys use
the `dot.asset.picker.*` namespace (existing convention, line ~8193); the two scoped option
descriptions extend the shared `content-drive.dialog.upload-selector.*` family they belong to.

### Refusal toast (AC-004)

```properties
dot.asset.picker.upload.rejected=Can't upload this file
dot.asset.picker.upload.rejected.detail=Only {0} can be uploaded here.
```

`{0}` is the restriction label from C4.

### Family labels (AC-004, AC-005)

```properties
dot.asset.picker.upload.types.image=images
dot.asset.picker.upload.types.video=video files
dot.asset.picker.upload.types.audio=audio files
```

Lower-case: these are interpolated mid-sentence, never used as a standalone heading.

### Scoped option descriptions (AC-005)

```properties
content-drive.dialog.upload-selector.asset.description.scoped=For {0} used in your content
content-drive.dialog.upload-selector.file.description.scoped=For {0} that need predictable URLs
```

`{0}` is the same restriction label. These are used **only** when `restrictionLabel` is set.

### Unchanged keys

These keep their current wording and remain the default. Content Drive renders them (AC-008):

```properties
content-drive.dialog.upload-selector.asset.description=For images, documents, and media used in your content
content-drive.dialog.upload-selector.file.description=For code, templates, and developer files that need predictable URLs
```

Also unchanged: `content-drive.dialog.upload-selector.header`, `.recommended`, `.asset`, `.file`,
`.settings-hint`, and every existing upload toast key.

---

## C4 — `upload-restriction.ts` module contract

A new pure module, colocated with `asset-picker-config.ts` and `last-asset-path.ts` — the
established shape for the picker's non-component logic. No Angular imports, no injection, directly
unit-testable.

### Exported behavior

| Export | Input | Output |
|---|---|---|
| Matcher | a `File` (or its `type`) + the restriction list | `boolean` — may this file be uploaded |
| `accept` builder | the restriction list | the joined attribute value, or `null` when unrestricted |
| Label resolver | the restriction list + a translate function | the translated human label, or `undefined` when unrestricted |

### Behavioral contract

Full rule table in [data-model.md §2](../data-model.md#2-validation-rules). The three that are easy
to get wrong, and are therefore the ones to write tests for first:

1. **Empty restriction ⇒ accept.** Never default to refusing when the value is missing — that breaks
   the File field (AC-007).
2. **Empty `file.type` ⇒ accept.** The AC-010 decision. The browser sometimes reports no type; the
   server remains the authority. This is the opposite of what
   `DotDropZoneComponent.typeMatch()` does, and the divergence is deliberate — see
   [research.md R5](../research.md#r5--matching-a-file-against-image-and-the-unclassifiable-case).
3. **Never consult the filename extension.** An extension fallback is a hand-maintained type list,
   forbidden by AC-002.

Comparisons are case-insensitive. `family/*` matches on the family; anything else matches in full.

### Label resolution

The translate function is injected as a parameter rather than the module importing
`DotMessageService` — that is what keeps it pure and its spec harness-free.

| Restriction | Label |
|---|---|
| `['image/*']` | *images* |
| `['video/*']` | *video files* |
| `['audio/*']` | *audio files* |
| Unknown family (e.g. `['application/pdf']`) | fall back to the raw patterns |
| Several families | the resolved labels, joined |
| `undefined` / `[]` | `undefined` |

The fallback matters: a `browse` caller may pass anything, and a missing label must still produce a
correct message rather than *"Only  can be uploaded here."*

---

## C5 — What this fix must NOT change

Explicit non-contract, since these are the AC-007 / AC-008 regression lines:

| Surface | Guarantee |
|---|---|
| `file` mode | Every file type accepted, through all four upload routes. No `accept`. No toast. |
| `browse` mode, no caller types | Unrestricted, exactly as today. |
| Content Drive uploads | Unrestricted; same options, same copy, same toasts. |
| `DotUploadDropzoneComponent` | No API or behavior change. |
| `DotAssetPickerConfig` | No new field. |
| `DotUploadFileService` | Untouched — the guard lives in the picker, not the service. |
| Multi-file handling | The existing warn-and-upload-the-first behavior stands (tracked in #37166). |
| Server-side validation | Unchanged. This is a UX guard, not an enforcement boundary. |
