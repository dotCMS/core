import { Subject } from 'rxjs';

import { Injectable, OnDestroy, computed, inject, signal } from '@angular/core';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import {
    EditContentHost,
    EditContentIdentity,
    InPlaceNavigationRequest
} from './edit-content-host.model';

import { EditContentDialogData } from '../../models/dot-edit-content-dialog.interface';
import {
    DotRelatedContentCrumb,
    DotRelatedContentNavigationStore,
    toRelatedContentCrumbs
} from '../../store/dot-related-content-navigation.store';

/**
 * Overlay {@link EditContentHost}: the editor is mounted in an overlay (a dialog
 * or a side panel) on top of another route context (e.g. UVE or a "create related
 * content" flow). It must not touch the router, the document title, or the shell
 * breadcrumb — doing so would navigate the host page away or stack duplicate trails.
 *
 * - **Identity** comes from the dialog config, not the route.
 * - **Navigation** (related content, locale switch) reloads the editor **in
 *   place** via {@link inPlaceNavigation$} instead of a route change. The trail is
 *   emitted with the request and committed by the layout only after the
 *   unsaved-changes check passes — so cancelling never leaves a stale breadcrumb.
 * - **The trail** is a per-instance signal (not the shared root store), so an open
 *   overlay never blanks the breadcrumb of a full-screen editor behind it.
 * - **The save result** is forwarded to the opener via {@link saved$}.
 *
 * Presentation-agnostic across overlays: the same host backs both the dialog and a
 * side panel — only the chrome (the component that provides it) differs.
 */
@Injectable()
export class OverlayEditContentHost implements EditContentHost, OnDestroy {
    readonly #relatedNav = inject(DotRelatedContentNavigationStore);
    readonly #config = inject(DynamicDialogConfig, { optional: true });
    readonly #navigation$ = new Subject<InPlaceNavigationRequest>();
    readonly #saved$ = new Subject<DotCMSContentlet>();

    /** Per-instance trail; starts empty and never touches the shared root store. */
    readonly #trailInodes = signal<string[]>([]);

    /** The dialog reloads the editor in place rather than via the router. */
    readonly inPlaceNavigation = true;

    readonly inPlaceNavigation$ = this.#navigation$.asObservable();

    /** Emits each successful save so the dialog can notify its opener. */
    readonly saved$ = this.#saved$.asObservable();

    readonly trail = computed<DotRelatedContentCrumb[]>(() =>
        toRelatedContentCrumbs(this.#trailInodes(), this.#relatedNav.titleCache())
    );

    resolveIdentity(): EditContentIdentity {
        const data = this.#config?.data as EditContentDialogData | undefined;

        return {
            inode: data?.contentletInode,
            contentTypeId: data?.contentTypeId,
            folderPath: data?.folderPath
        };
    }

    reportSaved(contentlet: DotCMSContentlet): void {
        this.#saved$.next(contentlet);
    }

    reloadContent(inode: string): void {
        // Locale switch: reload the content, keep the current trail (no `trail`).
        this.#navigation$.next({ inode });
    }

    setTrail(inodes: string[]): void {
        this.#trailInodes.set(inodes);
    }

    setContentTitle(_label: string): void {
        // no-op: the dialog must not overwrite the host page title.
    }

    addBreadcrumb(_crumb: { label: string; url: string }): void {
        // no-op: the dialog must not stack a trail onto the shell breadcrumb.
    }

    goToSavedContent(
        contentlet: { inode: string; title: string },
        previousInode: string | undefined
    ): void {
        // The dialog does not navigate, but a save mints a NEW inode. Repoint the
        // current (last) crumb of the in-memory trail from the pre-save inode to the
        // new one — otherwise the breadcrumb keeps labeling the stale inode and a
        // title change made in the dialog never shows. Mirrors RouterEditContentHost,
        // which does the same via the `rc` query param.
        if (contentlet.inode === previousInode) {
            return;
        }

        this.#relatedNav.registerTitle(contentlet.inode, contentlet.title);

        const trail = this.#trailInodes();
        if (trail.length) {
            this.#trailInodes.set([...trail.slice(0, -1), contentlet.inode]);
        }
    }

    goToRestoredVersion(inode: string): void {
        // Reload the editor in place so the restored version is reflected. The router
        // host re-navigates; the overlay has no route, so it reloads via the in-place
        // navigation stream (mirrors reloadContent). The trail is left untouched.
        this.#navigation$.next({ inode });
    }

    goToRelatedContent(current: DotRelatedContentCrumb, target: DotRelatedContentCrumb): void {
        // Compute the next trail from THIS host's current trail but do not commit it
        // yet — the layout commits it (setTrail) only after the dirty check passes.
        const trail = this.#relatedNav.appendToTrail(this.#trailInodes(), current, target);
        this.#navigation$.next({ inode: target.inode, trail });
    }

    goToCrumb(inode: string, trailInodes: string[]): void {
        this.#navigation$.next({ inode, trail: trailInodes });
    }

    ngOnDestroy(): void {
        this.#navigation$.complete();
        this.#saved$.complete();
    }
}
