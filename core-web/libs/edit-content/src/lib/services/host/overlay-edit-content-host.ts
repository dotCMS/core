import { Subject } from 'rxjs';

import { DOCUMENT } from '@angular/common';
import { Injectable, OnDestroy, computed, inject, signal } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

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
    readonly #dialogRef = inject(DynamicDialogRef, { optional: true });
    readonly #document = inject(DOCUMENT);
    readonly #navigation$ = new Subject<InPlaceNavigationRequest>();
    readonly #saved$ = new Subject<DotCMSContentlet>();
    readonly #left$ = new Subject<void>();

    /** Set once the edited content is deleted; later save reports would point at content that is gone. */
    #contentDeleted = false;

    /** Per-instance trail; starts empty and never touches the shared root store. */
    readonly #trailInodes = signal<string[]>([]);

    /** The dialog reloads the editor in place rather than via the router. */
    readonly inPlaceNavigation = true;

    readonly inPlaceNavigation$ = this.#navigation$.asObservable();

    /** Emits each successful save so the dialog can notify its opener. */
    readonly saved$ = this.#saved$.asObservable();

    /**
     * Emits when the edited content was deleted and the overlay must close. Overlays not opened
     * through `DialogService` (the side panel) have no `DynamicDialogRef`, so they close on this.
     */
    readonly left$ = this.#left$.asObservable();

    readonly trail = computed<DotRelatedContentCrumb[]>(() =>
        toRelatedContentCrumbs(this.#trailInodes(), this.#relatedNav.titleCache())
    );

    /**
     * The content to open, read from the dialog config: an inode to edit, or a content type to
     * create, with the folder and the language a new content starts in when the opener gave them.
     */
    resolveIdentity(): EditContentIdentity {
        const data = this.#config?.data as EditContentDialogData | undefined;

        return {
            inode: data?.contentletInode,
            contentTypeId: data?.contentTypeId,
            folderPath: data?.folderPath,
            languageId: data?.languageId
        };
    }

    reportSaved(contentlet: DotCMSContentlet): void {
        // A delete also flags a successful action; it must not reach the opener as a save.
        if (this.#contentDeleted) {
            return;
        }

        this.#saved$.next(contentlet);
    }

    reloadContent(inode: string, languageId?: number): void {
        // Locale switch: reload the content, keep the current trail (no `trail`). The language
        // is only reported once the layout runs the reload (`reportLanguage`).
        this.#navigation$.next(languageId ? { inode, languageId } : { inode });
    }

    /** Reports the language on {@link languageChanged$}. */
    reportLanguage(languageId: number): void {
        this.#languageChanged$.next(languageId);
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

    readonly #loadFailed$ = new Subject<void>();
    readonly #languageChanged$ = new Subject<number>();

    /**
     * A locale switch loaded another language of the content. The opener can name it in its URL,
     * so a refresh or a switch to the old editor reopens that language (#37759, FR-020, FR-028).
     */
    readonly languageChanged$ = this.#languageChanged$.asObservable();

    /** The content failed to load; the opener decides how to close (#37759, FR-029). */
    readonly loadFailed$ = this.#loadFailed$.asObservable();

    /**
     * "Switch to the old editor": reloads the page, so whatever opened this overlay reopens from
     * its own URL, now in the old editor. Content Drive's URL names the open content or create,
     * so it reopens in its legacy panel (#37759, FR-028). The URL already says what to reopen, so
     * the arguments the full-page editor navigates with are not needed here.
     */
    switchToLegacyEditor(_contentlet: DotCMSContentlet | null, _contentTypeVariable: string): void {
        this.#document.defaultView?.location.reload();
    }

    /** Reports the load failure on {@link loadFailed$}. */
    leaveOnLoadError(): void {
        this.#loadFailed$.next();
    }

    leaveDeletedContent(_contentType: string): void {
        // The overlay has no listing to return to; just close it. The DialogService dialog closes
        // through its ref; the side panel (no ref in its injector) closes on `left$`.
        this.#contentDeleted = true;
        this.#left$.next();
        this.#dialogRef?.close();
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
        this.#left$.complete();
    }
}
