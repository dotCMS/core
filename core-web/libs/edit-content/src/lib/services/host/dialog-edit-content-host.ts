import { Subject } from 'rxjs';

import { Injectable, OnDestroy, inject } from '@angular/core';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { EditContentHost, EditContentIdentity } from './edit-content-host.model';

import { EditContentDialogData } from '../../models/dot-edit-content-dialog.interface';
import {
    DotRelatedContentCrumb,
    DotRelatedContentNavigationStore
} from '../../store/dot-related-content-navigation.store';

/**
 * Overlay {@link EditContentHost}: the editor is mounted inside a dialog that
 * overlays another route context (e.g. UVE or a "create related content" flow).
 * It must not touch the router, the document title, or the shell breadcrumb —
 * doing so would navigate the host page away or stack duplicate trails.
 *
 * - **Identity** comes from the dialog config, not the route.
 * - **Navigation** (related content, locale switch) reloads the editor **in
 *   place** via {@link inPlaceNavigation$} instead of a route change.
 * - **The save result** is forwarded to the opener via {@link saved$}.
 *
 * Provided by {@link DotEditContentDialogComponent}; clears the in-memory trail on
 * destroy so full-screen editors revert to their URL-driven trail.
 */
@Injectable()
export class DialogEditContentHost implements EditContentHost, OnDestroy {
    readonly #relatedNav = inject(DotRelatedContentNavigationStore);
    readonly #config = inject(DynamicDialogConfig, { optional: true });
    readonly #navigation = new Subject<string>();
    readonly #saved = new Subject<DotCMSContentlet>();

    /** The dialog reloads the editor in place rather than via the router. */
    readonly inPlaceNavigation = true;

    readonly inPlaceNavigation$ = this.#navigation.asObservable();

    /** Emits each successful save so the dialog can notify its opener. */
    readonly saved$ = this.#saved.asObservable();

    constructor() {
        // A dialog starts with its own empty trail (in-memory), independent of
        // whatever URL trail a full-screen editor behind it may have.
        this.#relatedNav.setInMemoryTrail([]);
    }

    resolveIdentity(): EditContentIdentity {
        const data = this.#config?.data as EditContentDialogData | undefined;

        return {
            inode: data?.contentletInode,
            contentTypeId: data?.contentTypeId
        };
    }

    reportSaved(contentlet: DotCMSContentlet): void {
        this.#saved.next(contentlet);
    }

    reloadContent(inode: string): void {
        // Same in-place mechanism as related-content navigation: the layout picks
        // this up and reloads after the dirty check.
        this.#navigation.next(inode);
    }

    setContentTitle(): void {
        // no-op: the dialog must not overwrite the host page title.
    }

    addBreadcrumb(): void {
        // no-op: the dialog must not stack a trail onto the shell breadcrumb.
    }

    goToSavedContent(): void {
        // no-op: the dialog stays in place after a save; the store patch and the
        // reported result already surface the outcome to the opener.
    }

    goToRestoredVersion(): void {
        // no-op: version restore does not navigate inside the dialog.
    }

    goToRelatedContent(current: DotRelatedContentCrumb, target: DotRelatedContentCrumb): void {
        const trail = this.#relatedNav.appendToTrail(current, target);
        this.#relatedNav.setInMemoryTrail(trail);
        this.#navigation.next(target.inode);
    }

    goToCrumb(inode: string, trailInodes: string[]): void {
        this.#relatedNav.setInMemoryTrail(trailInodes);
        this.#navigation.next(inode);
    }

    ngOnDestroy(): void {
        // Revert to the URL-driven trail so full-screen editors are unaffected.
        this.#relatedNav.setInMemoryTrail(null);
        this.#navigation.complete();
        this.#saved.complete();
    }
}
