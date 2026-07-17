import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { NavigationEnd, Router } from '@angular/router';

import { distinctUntilChanged, filter, map } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { EditContentHost, EditContentIdentity } from './edit-content-host.model';

import {
    DotRelatedContentCrumb,
    DotRelatedContentNavigationStore,
    RELATED_TRAIL_PARAM
} from '../../store/dot-related-content-navigation.store';

/** Message key for the platform title suffix appended to the document title. */
const DEFAULT_TITLE_PLATFORM = 'dotcms.content.management.platform.title';

/**
 * Full-screen {@link EditContentHost}: navigation goes through the Angular
 * router (so the `unsavedChangesGuard` still fires), the document title and the
 * global shell breadcrumb are kept in sync, and post-save navigation reconciles
 * the related-content trail (`rc` query param).
 *
 * Provided by the edit-content shell so it is inherited by the layout component
 * and its store.
 */
@Injectable()
export class RouterEditContentHost implements EditContentHost {
    readonly #router = inject(Router);
    readonly #title = inject(Title);
    readonly #globalStore = inject(GlobalStore);
    readonly #relatedNav = inject(DotRelatedContentNavigationStore);
    readonly #dotMessageService = inject(DotMessageService);

    /** Full-screen navigates through the router, not in place. */
    readonly inPlaceNavigation = false;

    /** URL-derived trail (the `rc` query param), owned by the shared nav store. */
    readonly trail = this.#relatedNav.trail;

    /**
     * Emits when the edited inode changes in the URL, AFTER the initial load. The
     * editor's route is reused, so the layout listens here to re-initialize on
     * subsequent navigations instead of relying on a fresh component. The initial
     * `NavigationEnd` is not delivered to the layout's subscription (it fires
     * before the subscription is set up), so the first load is done by the layout's
     * constructor `initialize()`; this stream only carries later changes. Driven
     * off `NavigationEnd` (not the route param stream) so it also covers browser
     * back/forward, and de-duplicated on the inode so query-param-only changes
     * (e.g. the `rc` trail) don't reload.
     */
    readonly identityChanges$: Observable<void> = this.#router.events.pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        map(() => this.resolveIdentity().inode),
        distinctUntilChanged(),
        map(() => undefined)
    );

    /**
     * Dirty-navigation guard registered by the layout. Defaults to a pass-through
     * so navigation still works before the layout wires it up. Replaced via
     * {@link setNavigationGuard} with a check that prompts on unsaved changes.
     */
    #navigationGuard: (proceed: () => void) => void = (proceed) => proceed();

    setNavigationGuard(guard: (proceed: () => void) => void): void {
        this.#navigationGuard = guard;
    }

    setTrail(_inodes: string[]): void {
        // no-op: the full-screen trail lives in the URL, which this host's own
        // navigation already updates via the `rc` query param.
    }

    resolveIdentity(): EditContentIdentity {
        // Read the current leaf route so this works regardless of where the host
        // is provided (the shell) versus where the `:id` param lives (the child).
        let route = this.#router.routerState.snapshot.root;
        while (route.firstChild) {
            route = route.firstChild;
        }

        return {
            inode: route.params['id'],
            contentTypeId: route.params['contentType'],
            folderPath: route.queryParams['folderPath']
        };
    }

    reportSaved(_contentlet: DotCMSContentlet): void {
        // no-op: in full-screen the URL is the source of truth; there is no
        // separate opener to notify.
    }

    reloadContent(inode: string): void {
        // Locale switch mints a new inode; repoint the trail's current crumb to it
        // so the breadcrumb keeps labeling the content actually being edited (AC-B4).
        this.#navigationGuard(() => this.#navigateRepointingCurrentCrumb(inode));
    }

    setContentTitle(label: string): void {
        this.#title.setTitle(`${label} - ${this.#dotMessageService.get(DEFAULT_TITLE_PLATFORM)}`);
    }

    addBreadcrumb(crumb: { label: string; url: string }): void {
        this.#globalStore.addNewBreadcrumb({
            label: crumb.label,
            target: '_self',
            url: crumb.url
        });
    }

    goToSavedContent(
        contentlet: { inode: string; title: string },
        previousInode: string | undefined
    ): void {
        // Nothing to navigate to when the save did not mint a new inode.
        if (contentlet.inode === previousInode) {
            return;
        }

        // Saving created a new inode. Register its title and repoint the trail's
        // current crumb to it so the breadcrumb stays consistent.
        this.#relatedNav.registerTitle(contentlet.inode, contentlet.title);
        this.#navigateRepointingCurrentCrumb(contentlet.inode);
    }

    goToRestoredVersion(inode: string, previousInode: string | undefined): void {
        if (inode === previousInode) {
            return;
        }

        // Restore mints a new inode; repoint the trail's current crumb to it so the
        // breadcrumb keeps labeling the content actually being edited (AC-B4).
        this.#navigationGuard(() => this.#navigateRepointingCurrentCrumb(inode));
    }

    goToRelatedContent(current: DotRelatedContentCrumb, target: DotRelatedContentCrumb): void {
        const trail = this.#relatedNav.appendToTrail(
            this.#relatedNav.trailInodes(),
            current,
            target
        );
        this.#navigationGuard(() => this.#navigateToTrail(target.inode, trail));
    }

    goToCrumb(inode: string, trailInodes: string[]): void {
        this.#navigationGuard(() => this.#navigateToTrail(inode, trailInodes));
    }

    /**
     * Navigates to `inode`, writing the trail to the `rc` query param (cleared when
     * the trail collapses to a single content). The trail store re-derives from the
     * URL after navigation, so there is no in-memory copy to keep in sync.
     */
    #navigateToTrail(inode: string, trailInodes: string[]): void {
        const rc = trailInodes.length >= 2 ? trailInodes.join(',') : null;

        this.#router.navigate(['/content', inode], {
            queryParams: { [RELATED_TRAIL_PARAM]: rc },
            queryParamsHandling: 'merge'
        });
    }

    /**
     * Navigates to `inode` while repointing the trail's current (last) crumb to it,
     * so the breadcrumb keeps labeling the content actually being edited when the
     * inode changes in place (save, locale switch, version restore — AC-B4).
     * `buildTrailForSavedInode` returns null when no trail is active, which clears
     * `rc` via the merge. The new inode's title is (re)registered when the editor
     * reloads, so the crumb label follows.
     */
    #navigateRepointingCurrentCrumb(inode: string): void {
        const rc = this.#relatedNav.buildTrailForSavedInode(inode);

        this.#router.navigate(['/content', inode], {
            replaceUrl: true,
            queryParams: { [RELATED_TRAIL_PARAM]: rc },
            queryParamsHandling: 'merge'
        });
    }
}
