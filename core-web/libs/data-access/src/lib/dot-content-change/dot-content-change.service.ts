import { Observable, Subject } from 'rxjs';

import { Injectable } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

/** A save or a delete in a side panel, at any depth (#38303, T174). */
export interface DotContentChange {
    /** `saved` for every save and restored version; `deleted` when the content was removed. */
    kind: 'saved' | 'deleted';
    /** The saved contentlet, in full; for a delete, the content the panel had. */
    contentlet: DotCMSContentlet;
    /**
     * How deep the panel was: `0` for a panel a surface opened, `1` for one stacked from inside it
     * (a relationship's "New", a block editor's "Edit contentlet"), and so on.
     */
    depth: number;
}

/**
 * How a surface learns that content changed in a side panel, at any depth (#38303, T174). The
 * opener announces every save and delete of every panel it opens, stacked ones included, after the
 * panel's own handlers ran; a listing surface subscribes once, for as long as it lives, and
 * refreshes. A panel's per-open handlers are for what only its opener can do (UVE's insert, the
 * page editor request); refreshing a listing is this notification's job.
 *
 * In `@dotcms/data-access` rather than beside the opener in `@dotcms/edit-content`, so the admin
 * app's screens, which load the editor library on demand only, can listen without loading it.
 */
@Injectable({ providedIn: 'root' })
export class DotContentChangeService {
    readonly #changes = new Subject<DotContentChange>();

    /** Every change, as it happens. */
    readonly changes$: Observable<DotContentChange> = this.#changes.asObservable();

    /**
     * Announces a change. Called by the opener only.
     *
     * @param change What changed, and in how deep a panel.
     */
    notify(change: DotContentChange): void {
        this.#changes.next(change);
    }
}
