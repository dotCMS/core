import { Observable } from 'rxjs';

import { InjectionToken } from '@angular/core';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

/**
 * What a surface must be able to do for the field filter to offer a Relationship picker.
 *
 * Deliberately narrow — one method, taking only what a *filter* needs. Cardinality and parent
 * context are absent on purpose: those drive the edit-time "already related elsewhere" constraint
 * that disables rows, which is meaningless when you are choosing values to match against.
 */
export interface DotRelationshipPicker {
    /**
     * Opens the surface's content-selection dialog for a relationship field.
     *
     * @param field The relationship field being filtered on.
     * @param selectedInodes Inodes to pre-select, if any.
     * @return The chosen contentlets, or an empty list when the editor cancelled. Completes either
     *   way, so callers need no separate cancellation path.
     */
    open(field: DotCMSContentTypeField, selectedInodes: string[]): Observable<DotCMSContentlet[]>;
}

/**
 * Optional capability a surface may supply so the shared field filter can offer a Relationship
 * picker.
 *
 * **Why this is a token and not a direct import.** The dialog that does the picking lives in
 * `@dotcms/edit-content`, and that library already depends on `@dotcms/ui` — in ~98 files. A shared
 * chip importing it would make the dependency circular, and would drag the whole of
 * `edit-content` into the legacy custom-element bundle that `@dotcms/ui` is compiled into. So the
 * capability is inverted: the chip declares what it needs, and whichever surface *can* satisfy it
 * does.
 *
 * **Optional by design** (FR-020). Injected as `{ optional: true }`: a surface that supplies
 * nothing still gets a fully working field filter for every other field type, and only the
 * Relationship type degrades — with the control saying so rather than failing to render.
 *
 * Content Drive provides an implementation backed by `DotSelectExistingContentComponent`, which is
 * exactly what it opens today, so its behaviour is unchanged (FR-021). The AssetPicker provides
 * none: the content types it filters — DotAsset and File Asset — carry no relationship fields, so
 * there is nothing for it to lose.
 */
export const DOT_RELATIONSHIP_PICKER = new InjectionToken<DotRelationshipPicker>(
    'DOT_RELATIONSHIP_PICKER'
);
