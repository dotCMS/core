import { Subject } from 'rxjs';

import { Injectable, Injector, OnDestroy, computed, inject, signal } from '@angular/core';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import {
    EditContentHost,
    EditContentIdentity,
    InPlaceNavigationRequest
} from './edit-content-host.model';

import { DotEditContentRequest } from '../../models/dot-edit-content-request.model';
import { DotOpenContentEvent } from '../../models/dot-open-content-event.model';
import { DotEditContentOpenerService } from '../../opener/dot-edit-content-opener.service';
import {
    DotRelatedContentCrumb,
    DotRelatedContentNavigationStore,
    toRelatedContentCrumbs
} from '../../store/dot-related-content-navigation.store';

/**
 * Overlay {@link EditContentHost}: the editor is mounted in a side panel on top of another
 * route context (e.g. UVE or a "create related content" flow). It must not touch the router,
 * the document title, or the shell breadcrumb — doing so would navigate the host page away or
 * stack duplicate trails.
 *
 * - **Identity** comes from the panel's request, which it reads from the dialog config, not the
 *   route.
 * - **Navigation** (related content, locale switch) reloads the editor **in
 *   place** via {@link inPlaceNavigation$} instead of a route change. The trail is
 *   emitted with the request and committed by the layout only after the
 *   unsaved-changes check passes — so cancelling never leaves a stale breadcrumb.
 * - **The trail** is a per-instance signal (not the shared root store), so an open
 *   overlay never blanks the breadcrumb of a full-screen editor behind it.
 * - **The save result** is forwarded to the opener via {@link saved$}.
 *
 * Provided by `DotEditContentSidePanelComponent`, the only overlay that mounts the editor since
 * the centered dialog was removed (#38303).
 */
@Injectable()
export class OverlayEditContentHost implements EditContentHost, OnDestroy {
    readonly #relatedNav = inject(DotRelatedContentNavigationStore);
    readonly #opener = inject(DotEditContentOpenerService);
    /** The panel's injector: whatever this host stacks closes with the panel, not with a field. */
    readonly #injector = inject(Injector);
    /** Carries the request the side panel shows (see {@link resolveIdentity}). */
    readonly #config = inject(DynamicDialogConfig, { optional: true });
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

    /** Emits when the edited content was deleted and the side panel must close. */
    readonly left$ = this.#left$.asObservable();

    readonly trail = computed<DotRelatedContentCrumb[]>(() =>
        toRelatedContentCrumbs(this.#trailInodes(), this.#relatedNav.titleCache())
    );

    /**
     * The content to open, read from the request the side panel puts on the dialog config
     * (#38303): an inode to edit, or a content type to create, with the folder and the language a
     * new content starts in, and the experiment variant, when the request names them.
     */
    resolveIdentity(): EditContentIdentity {
        const request = this.#config?.data as DotEditContentRequest | null | undefined;

        return {
            inode: request?.mode === 'edit' ? request.inode : undefined,
            contentTypeId: request?.contentType,
            folderPath: request?.folderPath,
            languageId: request?.languageId,
            variantName: request?.variantName
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

    readonly #languageChanged$ = new Subject<number>();

    /**
     * A locale switch loaded another language of the content. The opener can name it in its URL,
     * so a refresh reopens that language (#37759, FR-020).
     */
    readonly languageChanged$ = this.#languageChanged$.asObservable();

    leaveDeletedContent(_contentType: string): void {
        // The side panel has no listing to return to; it closes on `left$`.
        this.#contentDeleted = true;
        this.#left$.next();
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

    /**
     * An edit from a Relationship field replaces this panel's content in place, with a breadcrumb,
     * as related-content navigation always has: from the content the field names as `from`, or on
     * a fresh trail when it names none. The field's row refreshes when the author navigates back,
     * so `onSaved` is not called for it.
     *
     * Everything else (any create, a block editor's edit, an edit naming no source) stacks a panel
     * over this one, opened with this panel's injector rather than the field's: a re-render of the
     * form underneath must not close the panel on top.
     */
    openContent({ request, source, from, onSaved }: DotOpenContentEvent): void {
        if (source === 'relationship' && request.mode === 'edit' && request.inode) {
            const target = { inode: request.inode, title: request.title ?? '' };

            if (from) {
                this.goToRelatedContent(from, target);
            } else {
                this.goToCrumb(target.inode, [target.inode]);
            }

            return;
        }

        this.#opener.open(request, {
            injector: this.#injector,
            saved: (contentlet) => onSaved?.(contentlet)
        });
    }

    ngOnDestroy(): void {
        this.#navigation$.complete();
        this.#saved$.complete();
        this.#left$.complete();
    }
}
