import { tapResponse } from '@ngrx/operators';
import { patchState, signalStoreFeature, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { filter, pipe, switchMap, tap } from 'rxjs';

import { inject } from '@angular/core';

import { DotPermissionsService } from '@dotcms/data-access';
import { DotSite } from '@dotcms/dotcms-models';

import { SYSTEM_HOST } from '../../../shared/constants';

interface WithSitePermissionsState {
    /**
     * Whether this user may add children to the site currently being browsed.
     *
     * `undefined` until the lookup lands, and again while a new site resolves, so a stale answer
     * from the previous site never gates the new one.
     *
     * This exists only for the drive root. Every folder carries its own CAN_ADD_CHILDREN with the
     * tree, but the root's parent is the host, and no folder endpoint reports on it:
     * `/api/v1/folder/search` returns only the root's children, and `byPath`'s `/` entry resolves
     * against the global `SYSTEM_FOLDER` singleton (`FolderHelper#findSubfoldersUnderHost`), which
     * inherits from SYSTEM_HOST and so answers identically no matter which site is open.
     */
    siteCanAddChildren: boolean | undefined;

    /**
     * CAN_ADD_CHILDREN on System Host itself.
     *
     * Kept apart from {@link siteCanAddChildren} rather than folded into it. That one is reset and
     * re-fetched every time the site changes, and System Host belongs to no site, so sharing the
     * slot would have each switch throw away an answer that had not changed and briefly ungate
     * the affordances for a destination nobody navigated away from.
     */
    systemHostCanAddChildren: boolean | undefined;
}

/**
 * Resolves whether the browsed site's root accepts new children.
 *
 * Read by the toolbar to gate New and Upload at the root, the one place a folder's own permissions
 * cannot answer for.
 */
export function withSitePermissions() {
    // No input-state constraint: the site arrives as this method's argument and the feature owns
    // the only slice it touches, so nothing here needs the host store's shape.
    return signalStoreFeature(
        withState<WithSitePermissionsState>({
            siteCanAddChildren: undefined,
            systemHostCanAddChildren: undefined
        }),
        withMethods((store, dotPermissionsService = inject(DotPermissionsService)) => ({
            /**
             * Looks up CAN_ADD_CHILDREN on the site itself.
             *
             * A failure settles on `true`, the opposite of the push publish gate's choice, because
             * the two protect different things: this one only softens an affordance the server
             * still guards, so a transient error should not strip the buttons from a user who has
             * the permission. Push Publish disables on failure because offering a push with nowhere
             * to send it fails later and less legibly.
             */
            /**
             * Looks up CAN_ADD_CHILDREN on System Host.
             *
             * Separate from the site lookup, which filters System Host out deliberately: there it
             * arrives as the seed the drive holds before a real site resolves, and answering for
             * it would answer about the wrong asset. Here it is the destination the user chose, so
             * the same identifier means the opposite thing and needs its own way in.
             *
             * Same failure posture as the site lookup: a transient error settles on allowed, since
             * this only softens an affordance the server still guards.
             */
            loadSystemHostPermissions: rxMethod<void>(
                pipe(
                    switchMap(() =>
                        dotPermissionsService.canAddChildren(SYSTEM_HOST.identifier).pipe(
                            tapResponse({
                                next: (canAddChildren) =>
                                    patchState(store, {
                                        systemHostCanAddChildren: canAddChildren
                                    }),
                                error: () =>
                                    patchState(store, { systemHostCanAddChildren: true })
                            })
                        )
                    )
                )
            ),

            loadSitePermissions: rxMethod<DotSite | null | undefined>(
                pipe(
                    // SYSTEM_HOST is the seed the drive holds before a real site resolves, and it
                    // is not a site anyone browses — asking about it would answer for the wrong
                    // asset. Filtered before the reset so the pseudo-site never clears an answer
                    // the real site already produced.
                    filter(
                        (site): site is DotSite =>
                            !!site && site.identifier !== SYSTEM_HOST.identifier
                    ),
                    // Clear first: while the new site's answer is in flight the previous site's
                    // must not gate it, and `undefined` is what "not known yet" reads as.
                    tap(() => patchState(store, { siteCanAddChildren: undefined })),
                    switchMap((site) =>
                        // Consumed inside the inner `.pipe()`, where the Observable is strongly
                        // typed: the standalone `pipe(...)` outside cannot propagate the response
                        // type through the `switchMap` under Angular's strict production build.
                        dotPermissionsService.canAddChildren(site.identifier).pipe(
                            tapResponse({
                                next: (canAddChildren) =>
                                    patchState(store, { siteCanAddChildren: canAddChildren }),
                                error: () => patchState(store, { siteCanAddChildren: true })
                            })
                        )
                    )
                )
            )
        }))
    );
}
