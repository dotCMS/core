# Issue Resolution Specification: New Edit Content shows "This URL does not exist" after deleting content and when opening archived content

**Feature Branch**: `37719-delete-archived-url-error`

**Created**: 2026-09-23

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37719](https://github.com/dotCMS/core/issues/37719) (sub-issue of [#35757](https://github.com/dotCMS/core/issues/35757))

**Input**: User description: "New Edit Content shows 'This URL does not exist' after deleting content (the Delete/Destroy workflow action refetches the deleted contentlet, which returns 404 and opens the global error dialog) and when opening archived content (the push publish history endpoint returns 404 for archived content, and the editor sends that error to the global error dialog)."

## Problem Statement *(mandatory)*

In the New Edit Content editor, a blocking error dialog — *"This URL does not exist. The page
you are trying to access does not exist."* — appears in two situations where nothing actually
went wrong for the user:

1. **After deleting content.** The user archives a contentlet and clicks **Delete** (or
   **Destroy**). The content is deleted, but the dialog appears and the editor stays on a page
   for content that no longer exists, still offering Unarchive and Delete.
2. **When opening archived content.** The user opens any archived contentlet. The content
   loads and is fully usable, but the same dialog appears on top of it every time.

Both cases tell the user something failed when it did not, and the first one leaves them on a
dead page with actions that can no longer work.

**Severity / Impact**: Medium. It affects every content type that has New Edit Content
enabled, for every user who archives or deletes content from the new editor — the normal
clean-up path for any content. It was found while migrating the dotcms.com content types to
New Edit Content (#35757), where every test contentlet is archived and deleted, so it shows up
on every content type migrated. No data is lost or corrupted: the delete does complete and the
archived content is intact.

## Reproduction *(mandatory)*

**Environment**: dotCMS `main` (1.0.0-SNAPSHOT) locally, and the dotcms-corp-headless-auth.dotcms.dev
instance. Any browser. Any content type with New Edit Content enabled (reproduced with Code
Snippet, Case Study Carousel, Chart and a plain test widget type), using the default System
Workflow.

**Steps to Reproduce**:

1. Enable New Edit Content on a content type (e.g. Code Snippet).
2. Create a contentlet in the new editor and click **Save**.
3. Click **Archive**.
4. Reload the page (or open any other archived contentlet of any type).
5. Click **Accept** on the dialog, then click **Delete** in the sidebar.

**Expected Behavior**:

- Step 4: the archived contentlet opens with no error dialog.
- Step 5: a success message is shown and the user leaves the editor — in full-screen mode they
  land on the content listing for that content type; in an overlay (dialog or side panel) the
  overlay closes.

**Actual Behavior**:

- Step 4: the archived contentlet opens, and the "This URL does not exist" dialog appears on
  top of it. The push publish history request
  (`GET /api/v1/content/{identifier}/push/history`) returns
  `404 {"message":"Content ID '<identifier>' does not exist"}`.
- Step 5: the delete succeeds (`PUT /api/v1/workflow/actions/{actionId}/fire` → 200), then the
  editor requests the contentlet it just deleted (`GET /api/v1/content/{inode}?depth=2` →
  `404 "The contentlet <inode> and language 1 does not exist"`), the dialog appears, and the
  editor stays on the deleted content.

**Reproducibility**: Always. Step 4 needs any archived contentlet; step 5 needs a workflow
action whose actionlets include Delete or Destroy (the System Workflow's **Delete** and
**Destroy** actions).

## Scope of Investigation *(mandatory)*

- **Affected area**: Content editing — the New Edit Content editor (`core-web/libs/edit-content`):
  firing workflow actions, and the sidebar's push publish history panel. Plus the content REST
  API's push history endpoint.
- **Suspected surface**: Modern on both sides. Frontend: the edit-content signal store
  (`workflow` and `history` features) and the editor host port (`EditContentHost`, with its
  full-screen and overlay implementations). Backend: `com.dotcms.rest.api.v1.content.ContentResource`
  (modern REST layer). It calls into `ContentletAPI` (`com.dotmarketing.portlets.contentlet.business`,
  legacy) only through an existing public method; the legacy API itself is not changed.
- **Related known decisions**: None known. The plan formally consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

Three causes, confirmed while reproducing:

1. **The editor always reloads the content after a workflow action.** After any successful
   action, the store refetches the contentlet, its available actions and its workflow status by
   inode, so it can repaint and move to the new version. For an action that deletes the content,
   there is nothing to refetch, and the 404 is treated as a failure of the action itself.
2. **The push history endpoint does not find archived content.** It looks the content up with the
   variant of `findContentletByIdentifierAnyLanguage` that skips archived ("deleted") content, then
   answers 404 when nothing is found. Archived content still exists, still has a push history, and
   is still editable.
3. **A secondary panel's failure blocks the whole editor.** The push publish history is loaded for
   a sidebar panel, but any error loading it is sent to the global HTTP error manager, which opens
   the full-page dialog — so even a legitimate failure of that panel stops the user from working.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- After a Delete or Destroy workflow action succeeds, the editor does not reload the deleted
  content: it shows the success message and leaves — full-screen goes to the content listing
  filtered by that content type, an overlay closes itself.
- The push history endpoint finds archived content and returns its history (possibly empty); it
  keeps returning 404 for identifiers that do not exist at all.
- A failure loading the push publish history puts that panel in its error state without opening
  the global error dialog.

**Explicitly out of scope / non-goals**:

- Changing what any workflow action does on the server, or which actions are offered in which
  state (e.g. the Issues workflow's Archive/Delete "Show When" configuration seen in corp).
- Reworking the post-action flow for non-deleting actions (Save, Publish, Archive, Unarchive,
  Reset, locale switch, version restore).
- Making the other sidebar panels (versions, comments, locales) stop using the global error
  dialog — only the push publish history panel is in scope, because it is the one that fails
  for valid content.
- Changing `ContentletAPI` or how "archived" maps to "deleted" in the legacy layer.
- Other findings from the #35757 QA pass that are unrelated to this dialog (the related-content
  empty-state flicker, the accumulating breadcrumb trail, the Code field defaulting to Plain Text
  for new content). They are tracked separately.

## Regression Risk *(mandatory)*

- **Blast radius**:
  - Every workflow action fired from the New Edit Content editor goes through the same store
    method. A wrong "is this a delete?" check would either send a normal Save/Publish down the
    leave-the-editor path, or leave a delete on the old reload path. The check keys only off the
    fired action's own `hasDeleteActionlet` / `hasDestroyActionlet` flags.
  - The editor host port gains one method, so every host implementation (full-screen router host,
    overlay host used by the dialog and the Content Drive side panel) and every test double of it
    must implement it.
  - The push history endpoint is also used outside the new editor (it is a public REST endpoint);
    widening the lookup to archived content changes its answer from 404 to 200 for archived
    identifiers only.
- **Backward compatibility**: The REST change only turns an error response into a success
  response for archived content; the response shape, pagination and the 404 for unknown
  identifiers are unchanged. No DB, Elasticsearch/OpenSearch mapping or serialized-state change.
  `openapi.yaml` is unaffected (no annotation changes). Rollback-safe.
- **Data considerations**: None. No existing data is wrong; nothing to migrate or repair.

## Acceptance & Verification *(mandatory)*

- **AC-001**: In the full-screen editor, after a Delete or Destroy action succeeds, a success
  message is shown and the user lands on the content listing filtered by that content type
  (`/c/content?filter=<contentType>`), with no error dialog.
- **AC-002**: In the overlay editor (dialog or side panel), after a Delete or Destroy action
  succeeds, the overlay closes with no error dialog.
- **AC-003**: After a Delete or Destroy action, the editor makes no request for the deleted
  contentlet (no `GET /api/v1/content/{inode}`, workflow actions or workflow status for it).
- **AC-004**: Every other workflow action (Save, Publish, Unpublish, Archive, Unarchive, Reset…)
  keeps its current behavior: the editor reloads the resulting content and navigates to a new
  inode when one was minted.
- **AC-005**: If a Delete or Destroy action fails, the existing error handling still applies and
  the user stays on the content.
- **AC-006**: Opening an archived contentlet in the New Edit Content editor shows no error dialog.
- **AC-007**: `GET /api/v1/content/{identifier}/push/history` returns 200 for an archived
  contentlet (with its history, or an empty list), and still returns 404 for an identifier that
  does not exist.
- **AC-008**: If loading the push publish history fails for any reason, the sidebar panel shows
  its error state and the global error dialog is not opened.
- **Verification method**:
  - Vitest/Spectator specs in `core-web/libs/edit-content`:
    - `workflow.feature.spec.ts` — a Delete/Destroy action calls the host's leave method with the
      content type, shows the success message and does not call `getContentById`,
      `getByInode` or `getWorkflowStatus`; a non-deleting action keeps the reload path; a failed
      delete calls the error manager (AC-001–AC-005 at store level).
    - `router-edit-content-host.spec.ts` / `overlay-edit-content-host.spec.ts` — the leave method
      navigates to the filtered listing / closes the dialog (AC-001, AC-002).
    - `history.feature.spec.ts` — a push history error sets the error state and does not call the
      error manager (AC-008).
  - Backend: integration test for `ContentResource#getPushHistory` — archived contentlet → 200;
    unknown identifier → 404 (AC-007); registered in the relevant `MainSuite`.
  - Manual: the reproduction above on a local instance with New Edit Content enabled, in
    full-screen and from the Content Drive side panel (AC-001, AC-002, AC-006).

## Assumptions

- "Deletes the content" is decided by the fired action's own flags (`hasDeleteActionlet` or
  `hasDestroyActionlet`), which the workflow actions API already returns; custom workflows that
  delete through those standard actionlets are covered, and no other actionlet removes content.
- The content listing (`/c/content?filter=<contentType variable>`) is the right place to land after
  a delete in full-screen mode, matching where users came from when they opened the content
  from search.
- A working prototype of this fix already exists uncommitted on this branch (it was used to
  confirm the root causes). Under the constitution's test-first principle, the tests listed above
  are written and confirmed failing against the pre-fix code before the implementation is
  finalized; the plan must account for this.
