# Issue Resolution Specification: Asset Picker — the upload flow ignores the field that opened the picker

**Feature Branch**: `nicobytes/37365-asset-picker-scope-the-upload-flow-to-the-field-that-opened-the-picker`

**Created**: 2026-09-03

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: dotCMS/core#37365 (split from dotCMS/core#37174, finding 8; parent epic dotCMS/core#36702)

**Input**: User description: "https://github.com/dotCMS/core/issues/37365 — The Asset Picker already restricts what you can *browse* to the field that opened it, but not what you can *upload*. An Image field, or a Story Block `dotVideo` node, lets you upload any file type from inside the picker."

## Problem Statement *(mandatory)*

The Asset Picker is opened from a specific place: an Image field, a File field, a Story Block
image / video / audio node, or the generic browse entry point. It already knows which one, and it
uses that knowledge to narrow **what the editor can see** — an Image field lists only images, a
video node only video.

It does not use that knowledge for **what the editor can add**. Every upload route inside the
picker is unrestricted:

- the OS file dialog opens with no filter, so it offers every file on the machine;
- a file dragged onto the list is accepted whatever it is;
- the Asset / File prompt offers the same two options with the same wording in every mode, one of
  which describes itself as being "for images, documents, and media" even when the picker was
  opened for video only;
- nothing checks the file before the upload request is sent.

The result is that an Image field can be made to hold a PDF and a video node an mp3 — the exact
outcome the browse-side restriction exists to prevent. Worse, the upload succeeds and then the new
asset **disappears**: the list is filtered by the mode, so the file the editor just uploaded is not
shown and cannot be selected. From the editor's point of view the upload silently did nothing,
while a stray file has in fact been written into the site's folder tree.

**Severity / Impact**: Medium. Affects every author who uploads from inside the picker in a
media-scoped context — Image fields in the new Edit Content, and the Story Block image / video /
audio nodes. It is deterministic, not intermittent, and it is reachable by the ordinary
upload path, not an edge case. Two distinct harms: content typed as an image can end up pointing at
a non-image, and authors lose work to an upload that appears to do nothing. It also leaves orphan
files in the folder tree that nobody sees from the picker that created them.

## Reproduction *(mandatory)*

**Environment**: dotCMS `main` (includes PR #36848, merged as `8c725747c0`). New Edit Content UI,
any browser. No special configuration; a site the user can add content to.

**Steps to Reproduce**:

*Path A — Image field, OS file dialog:*

1. Open a content type that has an **Image** field in the new Edit Content editor.
2. Open that field's Asset Picker.
3. Click **Upload** → the Asset / File prompt appears, with both options and their generic copy.
4. Pick either option → the OS file dialog opens with **no file-type filter**; every file on the
   machine is selectable.
5. Choose a PDF (or a `.zip`) and confirm.

*Path B — Image field, drag and drop:*

1. Steps 1–2 above.
2. Drag a PDF from the desktop onto the picker's asset list and drop it.

*Path C — Story Block media node:*

1. In a Story Block field, type `/video` to open the video node's Asset Picker.
2. Repeat step 3–5 of Path A with an mp3 or a PDF.

**Expected Behavior**:

The picker offers only what the field can hold. In an Image field the OS dialog lists images only,
a dropped PDF is refused with a message saying which types are allowed, and no upload request is
sent for it. The Asset / File prompt describes the choice in terms that are true for the mode it
was opened in. Uploading an allowed file works as it does today, and the new asset appears in the
list ready to select.

**Actual Behavior**:

The OS dialog offers every file type; a dropped PDF is accepted; the upload request is sent and
succeeds; a success toast is shown; the list refreshes and the uploaded file is **not** in it,
because the browse filter excludes it. The file remains in the folder, unreferenced by the field
that created it. The generic **File** field behaves correctly, since it intentionally has no
restriction.

**Reproducibility**: Always, in every media-scoped mode (`image`, `video`, `audio`), through all
three upload routes (Upload button → OS dialog, drag and drop, and a folder whose settings pin an
upload type and so skip the prompt).

## Scope of Investigation *(mandatory)*

- **Affected area**: Content authoring UI — the Asset Picker's upload flow, reached from the new
  Edit Content Image / File fields and from the Story Block image / video / audio nodes. The
  picker's shared upload building blocks (the drop zone and the Asset / File prompt) are also used
  by Content Drive, which is a different host with no mode restriction.
- **Suspected surface**: Modern frontend only (`core-web`, `libs/ui` Asset Picker plus the shared
  upload components, and the Story Block / Edit Content hosts that open the picker). No backend
  change is expected: the server-side upload contract is unchanged, and this defect is about what
  the UI offers and permits before the request is made. Confirmed during planning.
- **Related known decisions**: The restriction must come from the configuration the picker already
  carries for browsing — the same per-mode mimetype narrowing — and not from a second,
  independently maintained list of file types. This is the stated decision from refinement and is
  binding on the fix. The plan formally consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

The picker's entry point is translated into a browse restriction and nothing else. The
configuration built when the picker opens carries a per-mode mimetype narrowing, and every browse
request applies it — but no upload surface reads it:

- the hidden file input that opens the OS dialog carries no `accept` attribute, so the dialog is
  unfiltered;
- the drop zone is presentational and emits whatever was dropped, with no notion of allowed types;
- the Asset / File prompt renders a fixed, mode-independent list of options and copy;
- the upload handler goes straight from "files chosen" to "send the request", with no type check
  in between.

In other words the restriction exists in exactly one of the four places it needs to exist. The fix
is to make the same configured restriction the single source for all of them.

A secondary consequence explains the "upload vanished" symptom: because the list is filtered on
the same restriction, any file that gets through the unrestricted upload is invisible in the list
that refreshes right after it.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Scoping every upload route inside the Asset Picker to the mode that opened it, derived from the
  restriction the picker already carries for browsing:
  - the OS file dialog is pre-filtered to the allowed types;
  - a dropped file outside the allowed types is refused, with a message that says why;
  - a file that reaches the upload handler anyway is refused before the request is sent, with a
    message naming the allowed types;
  - the Asset / File prompt keeps both storage options but describes them in wording that is true
    for the mode it was opened in.
- Keeping the unrestricted modes unrestricted: the generic File field and the browse entry point
  continue to accept every file type, unless the caller explicitly asked for a narrowing.
- Regression coverage for both halves — restriction applied per media mode, and no restriction
  where none is configured.
- Any new user-facing wording added as translatable message keys.

**Explicitly out of scope / non-goals**:

- Server-side enforcement of the field's type. The upload endpoint keeps its current behavior;
  this fix is about the picker offering and permitting only what the field can hold. Server-side
  validation is a separate, larger change.
- Content Drive's own upload flow. It shares the drop zone and the Asset / File prompt, and must
  keep accepting every file type; the shared components gain an optional restriction that Content
  Drive does not set.
- The multi-file upload limitation. Today the picker warns and uploads only the first file of a
  multi-file selection; that behavior is unchanged here and is covered by #37166.
- Repairing files already uploaded into the wrong place by this defect. No data migration.
- Widening or changing which base types the picker may browse, or the per-folder default
  upload-type preference.
- The `accept`-style narrowing of any other upload surface in the product.

## Regression Risk *(mandatory)*

- **Blast radius**:
  - The drop zone and the Asset / File prompt are shared with **Content Drive**. Any restriction
    they gain must be opt-in, so Content Drive's uploads stay unrestricted.
  - The picker's own **File field** and **browse** modes must keep accepting everything — an
    over-reaching fix would block legitimate uploads of code, templates and documents.
  - The **per-folder default upload type** path skips the Asset / File prompt entirely and goes
    straight to the OS dialog; the restriction has to apply there too, or one of the three routes
    stays open.
  - Browse-mode callers that pass their own explicit mimetype narrowing (the legacy custom-field
    browser entry point) would newly have that narrowing applied to uploads as well.
- **Backward compatibility**: No API, content, or persisted-state change. The picker's public
  configuration shape gains no required field. Existing call sites that pass no restriction keep
  today's behavior exactly. Not rollback-unsafe.
- **Data considerations**: None. Files already uploaded through the defect stay where they are and
  remain reachable from Content Drive and the File field; nothing is moved or deleted.

## Acceptance & Verification *(mandatory)*

- **AC-001**: The reproduction steps above no longer produce the actual behavior. In an Image
  field, the OS file dialog offers only image files; in a Story Block `video` node only video; in
  an `audio` node only audio.
- **AC-002**: The restriction is derived from the configuration the picker already applies to
  browsing. Adding a new media mode to that configuration restricts its uploads with no second
  list to update, and no upload surface carries its own hard-coded list of types.
- **AC-003**: Dragging a file outside the allowed types onto the picker is refused: no upload
  request is sent, and the user is told why, naming what is allowed.
- **AC-004**: A file that reaches the upload handler despite the dialog filter — the filter being a
  hint the OS may let the user override — is refused before the upload request is sent, with a
  message naming the allowed types.
- **AC-005**: The Asset / File prompt keeps offering both storage options in every mode, but its
  wording reflects the mode it was opened in — no option's description promises types the mode does
  not allow. In a video-only context, nothing in the prompt says "images, documents, and media".
- **AC-006**: After a successful upload in a media mode, the new asset appears in the list and can
  be selected without reopening the picker.
- **AC-007** *(regression — no over-reach)*: The generic File field's picker still offers and
  accepts every file type through all three upload routes, and the browse entry point is unchanged
  unless its caller asked for a narrowing.
- **AC-008** *(regression — shared components)*: Content Drive's upload flow — Upload button, drag
  and drop, and the Asset / File prompt — still accepts every file type and shows its current
  wording.
- **AC-009**: The restriction applies on all three routes into an upload, including a folder whose
  settings pin a default upload type and therefore skip the Asset / File prompt.
- **AC-010**: A file whose type the browser does not report is allowed through rather than refused
  — the upload proceeds and the server remains the authority. The picker never blocks a file it
  cannot classify.

**Verification method**:

- Frontend unit/component specs (Jest + Spectator) in the Asset Picker and the shared upload
  components, covering per media mode: the pre-filtered dialog, the refused drop, the refused
  pre-upload file, the prompt's mode-dependent copy, and the pinned-folder route
  (AC-001 → AC-006, AC-009, AC-010).
- Frontend specs asserting the unrestricted modes and the Content Drive host are untouched
  (AC-007, AC-008).
- Manual verification of the three reproduction paths above, plus the same three paths in a File
  field and in Content Drive to confirm no over-reach.
- No backend test change expected; if planning finds a server-side element, integration coverage is
  added then.

## Assumptions

- **Frontend-only fix.** The defect is that the UI offers what the field cannot hold. The upload
  endpoint's behavior is treated as correct-as-is and out of scope; the picker is not made the
  place where server-side type enforcement is introduced.
- **The three media modes are the whole restricted set.** `image`, `video` and `audio` are the
  modes that carry a browse restriction today; `file` and `browse` carry none and stay
  unrestricted. The fix keys off "does this mode carry a restriction", not off a list of mode
  names, so a future media mode is covered automatically.
- **Browse-mode explicit narrowing also scopes uploads.** When the legacy custom-field browser
  entry point asks for specific types, the same restriction applies to its uploads. This follows
  from deriving the restriction from one configuration value rather than from the mode name, and is
  the behavior a caller asking for "images only" would expect.
- **Multi-file drops are judged on what would actually be uploaded.** The picker already warns and
  uploads only the first file of a multi-file selection. A drop whose uploaded file is outside the
  allowed types is refused; the existing multi-file warning is unchanged.
- **The refusal message names the allowed types in user terms** (for example "images"), not raw
  mimetype patterns, and is added as a translatable message key.
- **The restriction is expressed as the same broad type families the browse filter uses**
  (images / video / audio), not an enumerated extension list — the issue explicitly rules out a
  second hand-maintained list.
- **Both storage options stay on offer in every mode** *(decided during specification)*. Nothing
  about this fix changes which base type an upload is stored as; it changes which *files* may be
  uploaded. An Asset and a File can each legitimately hold an image, a video or an audio file, so
  removing an option would take away a real choice. What changes in the prompt is the wording, not
  the option list — the fix is scoped to correcting descriptions that over-promise, keeping it out
  of the way of the per-folder default upload-type preference.
- **An unclassifiable file is allowed, not blocked** *(decided during specification)*. When the
  browser reports no type for a file, the picker lets the upload proceed. The alternative — refusing
  it — would occasionally block a legitimate image behind a message saying only images are allowed,
  with no way forward; and closing the gap by inspecting filename extensions would reintroduce the
  hand-maintained list the issue rules out. The residual exposure is narrow and the server remains
  the authority.
