import { Observable } from 'rxjs';

import { InjectionToken, Signal } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotRelatedContentCrumb } from '../../store/dot-related-content-navigation.store';

/**
 * A request to reload the editor in place (in-place hosts only). `trail`, when
 * present, is the related-content trail to commit **once the navigation is
 * confirmed** (i.e. after the unsaved-changes check) — so cancelling never leaves
 * a stale breadcrumb. It is absent for a locale reload, which keeps the current
 * trail untouched.
 */
export interface InPlaceNavigationRequest {
    inode: string;
    trail?: string[];
}

/**
 * The content the editor should open, resolved by the host from wherever its
 * identity lives: the route params (full-screen) or the dialog config (overlay).
 * `inode` opens existing content; `contentTypeId` starts new content.
 */
export interface EditContentIdentity {
    /** Inode of existing content to edit. */
    inode?: string;
    /** Content type id/variable to create new content for. */
    contentTypeId?: string;
    /** Optional pre-fill path for a Host-or-Folder field. */
    folderPath?: string;
}

/**
 * Presentation-agnostic port for the Edit Content editor.
 *
 * The editor and its store features depend on this abstraction instead of the
 * Angular `Router`, `Title`, `ActivatedRoute` or the global breadcrumb store
 * directly. The host that mounts the editor decides how each intent is
 * fulfilled:
 *
 * - **Full-screen** ({@link RouterEditContentHost}) — identity from the route,
 *   real router navigation, document title and shell breadcrumb updates.
 * - **Dialog / overlay** ({@link OverlayEditContentHost}) — identity from the
 *   dialog config, in-place reloads, and chrome updates are no-ops.
 *
 * This is the seam that removes the scattered `isDialogMode` conditionals: the
 * editor stops asking "what mode am I in?" and simply states its intent.
 */
export interface EditContentHost {
    /**
     * Resolves which content the editor should open. Called once by the store on
     * initialization; the host reads it from the route or the dialog config.
     */
    resolveIdentity(): EditContentIdentity;

    /**
     * Reports a successful save to the opener. Full-screen ignores it (the URL is
     * the source of truth); the dialog forwards it to whoever opened the editor.
     */
    reportSaved(contentlet: DotCMSContentlet): void;

    /**
     * Reloads the editor with a different content (locale switch, etc.). Full-screen
     * navigates via the router (guard handles dirty state); the dialog reloads in
     * place (the layout handles the dirty check).
     */
    reloadContent(inode: string): void;

    /**
     * Sets the browser document title to reflect the content being edited.
     * @param label Human-readable content label (the host adds any suffix).
     */
    setContentTitle(label: string): void;

    /**
     * Contributes a breadcrumb entry to the global shell trail.
     * @param crumb Label and target URL for the breadcrumb.
     */
    addBreadcrumb(crumb: { label: string; url: string }): void;

    /**
     * Navigates to the content produced by a successful save. A save can mint a
     * new inode; the host reconciles the URL/breadcrumb trail and only acts when
     * the inode actually changed.
     *
     * @param contentlet The freshly-saved content (inode + title).
     * @param previousInode The inode being edited before the save, if any.
     */
    goToSavedContent(
        contentlet: { inode: string; title: string },
        previousInode: string | undefined
    ): void;

    /**
     * Navigates to a restored historical version. Only acts when the restored
     * inode differs from the one currently being edited.
     *
     * @param inode The inode of the restored version.
     * @param previousInode The inode being edited before the restore, if any.
     */
    goToRestoredVersion(inode: string, previousInode: string | undefined): void;

    /**
     * Whether this host navigates the editor in place (dialog/overlay) rather than
     * through the router. Consumers use it to decide, e.g., whether a breadcrumb
     * crumb is a `routerLink` (full-screen) or a `command` (in-place).
     */
    readonly inPlaceNavigation: boolean;

    /**
     * Emits a reload request the host wants the editor to perform in place. Only
     * defined for in-place hosts (dialog); the full-screen host navigates via the
     * router and leaves this undefined. The layout subscribes, runs the dirty check,
     * and — only if the user proceeds — reloads and commits the request's trail.
     */
    readonly inPlaceNavigation$?: Observable<InPlaceNavigationRequest>;

    /**
     * The related-content breadcrumb trail for this editor's presentation. URL-derived
     * for the router host (shared, reflects the `rc` param); a per-instance in-memory
     * signal for the dialog host (so an overlay never disturbs a full-screen editor's
     * trail behind it).
     */
    readonly trail: Signal<DotRelatedContentCrumb[]>;

    /**
     * Commits a trail as the editor's current trail. In-place hosts store it; the
     * router host is a no-op because its trail is derived from the URL, which its
     * own navigation already updated.
     */
    setTrail(inodes: string[]): void;

    /**
     * Navigates into a related content, extending the navigation trail. The host
     * decides the mechanism: router URL (full-screen) or in-place reload (dialog).
     *
     * @param current The content currently open (trail origin when starting fresh).
     * @param target The related content to open.
     */
    goToRelatedContent(current: DotRelatedContentCrumb, target: DotRelatedContentCrumb): void;

    /**
     * Navigates back to an earlier crumb in the trail, trimmed to that crumb.
     *
     * @param inode The crumb's content inode.
     * @param trailInodes The trail trimmed to (and including) that crumb.
     */
    goToCrumb(inode: string, trailInodes: string[]): void;
}

/**
 * DI token for the {@link EditContentHost}. Provided by whichever component
 * mounts the editor (the shell for full-screen, the dialog component for
 * overlay mode).
 */
export const EDIT_CONTENT_HOST = new InjectionToken<EditContentHost>('EDIT_CONTENT_HOST');
