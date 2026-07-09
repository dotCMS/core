import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
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

    setTrail(): void {
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

    reportSaved(): void {
        // no-op: in full-screen the URL is the source of truth; there is no
        // separate opener to notify.
    }

    reloadContent(inode: string): void {
        this.#router.navigate(['/content', inode], {
            replaceUrl: true,
            queryParamsHandling: 'preserve'
        });
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

        // Saving created a new inode. Repoint the related-content breadcrumb's
        // current crumb to it so the trail stays consistent (rc is null when
        // there is no active trail, which just clears the param via merge).
        this.#relatedNav.registerTitle(contentlet.inode, contentlet.title);
        const rc = this.#relatedNav.buildTrailForSavedInode(contentlet.inode);

        this.#router.navigate(['/content', contentlet.inode], {
            replaceUrl: true,
            queryParams: { rc },
            queryParamsHandling: 'merge'
        });
    }

    goToRestoredVersion(inode: string, previousInode: string | undefined): void {
        if (inode === previousInode) {
            return;
        }

        this.#router.navigate(['/content', inode], {
            replaceUrl: true,
            queryParamsHandling: 'preserve'
        });
    }

    goToRelatedContent(current: DotRelatedContentCrumb, target: DotRelatedContentCrumb): void {
        const trail = this.#relatedNav.appendToTrail(
            this.#relatedNav.trailInodes(),
            current,
            target
        );
        this.#navigateToTrail(target.inode, trail);
    }

    goToCrumb(inode: string, trailInodes: string[]): void {
        this.#navigateToTrail(inode, trailInodes);
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
}
