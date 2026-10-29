# Contract: panel events

**Lives in**: `core-web/libs/edit-content/src/lib/models/dot-edit-content-panel-events.model.ts` (new), exported from the barrel.

Both panel components expose these as Angular `output()`s with these exact names. The opener re-exposes them to surfaces as a handler map (see opener-api.md).

```ts
import { DotCMSContentlet } from '@dotcms/dotcms-models';

/** The legacy editor asked to open a page in the page editor (the first save of a new page). */
export interface DotPageEditorRequest {
    url: string;
    languageId: number;
}

/** What an editor panel reports to whoever opened it. Both editors produce the same set. */
export interface DotEditContentPanelEvents {
    /** Every successful save, and every restored version. Always the full saved contentlet. */
    saved: (contentlet: DotCMSContentlet) => void;

    /** The author switched language inside the editor; the panel stays open in the new language. */
    languageChanged: (languageId: number) => void;

    /** Close the panel and open the page editor at this URL and language. */
    pageEditorRequested: (request: DotPageEditorRequest) => void;

    /** The content was removed from inside the editor. Always followed by `closed`. Never preceded by `saved` for the delete. */
    deleted: () => void;

    /** The panel closed, for any reason. Emitted exactly once. */
    closed: () => void;
}

/** A surface listens to any subset. */
export type DotEditContentPanelHandlers = Partial<DotEditContentPanelEvents>;
```

## Guarantees

| Guarantee | How it is met |
|---|---|
| `saved` carries a full `DotCMSContentlet` from both editors | The new editor's host already emits one. For the legacy editor, the panel fetches by the saved inode (`save-page` payload `contentletInode`) before emitting |
| A restored version is a `saved` | New editor: `history.feature.ts` `restoreVersion` calls `host.reportSaved` after `bringBack`. Legacy: after `getVersionBack`, the panel reads the restored inode from the iframe's `?inode=` on reload, fetches, emits |
| A workflow action is reported through the same events | Legacy: the editor saves and sends `save-page`, or sends `close`/`deleted-page` when the action removed the content. New editor: `workflowActionSuccess` → `reportSaved`, as today |
| `deleted` is never a `saved` | New editor: `OverlayEditContentHost` suppresses `reportSaved` after `leaveDeletedContent`, as today. Legacy: the panel sets `iframeWindow.ngEditContentletEvents = { next: (e) => … }` on every iframe load; the editor calls it with `deleted-contenlet` (`edit_contentlet_js_inc.jsp:716-720`) when the saved content no longer exists, and sends `deleted-page` for a page; both map to `deleted`, never to `saved`. The generic `close` that follows maps to `closed` |
| `closed` exactly once | Taken from `DotSidePanelRef.closed`, which emits once and completes |

## Surfaces refresh on `closed` as well as on `saved`

A move to another folder or a translation saved after a language switch inside the legacy editor sends no usable event, and the surface does not know which editor the opener picked, so every surface refreshes on `closed` too (spec FR-017). One quiet refresh per close.

## Events the panel handles itself and does not forward

From the legacy editor: `workflow-wizard` (→ root `DotWorkflowEventHandlerService.open`), `compare-contentlet` (→ root `DotEventsService.notify`, handled by the root compare dialog), `push-publish` (→ root `DotPushPublishDialogService.open`), `edit-contentlet-data-updated` (dirty flag), `edit-contentlet-loaded`, `update-workflow-action`, `language-is-changed` (the language is read from the reload instead).

From the new editor: everything inside `DotEditContentStore`; the panel only sees the host observables.
