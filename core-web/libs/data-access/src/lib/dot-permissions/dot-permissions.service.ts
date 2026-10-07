import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSAPIResponse } from '@dotcms/dotcms-models';

export const ASSET_PERMISSIONS_URL = '/api/v1/permissions';

/**
 * The subset of the asset permissions payload this service reads.
 *
 * The endpoint also returns a `permissions` array of per-role grants. That array is deliberately
 * not modelled here: it describes what each *role* was granted, so answering "may I?" from it means
 * resolving the caller's roles and the inheritance chain in the browser — a second implementation
 * of the permission engine that would drift from the real one. `canAddChildren` is the server's own
 * answer for the calling user, resolved across every role they hold.
 */
interface AssetPermissionsView {
    canAddChildren?: boolean;
    canEdit?: boolean;
    canEditPermissions?: boolean;
}

@Injectable({
    providedIn: 'root'
})
export class DotPermissionsService {
    readonly #http = inject(HttpClient);

    /**
     * Resolves what the calling user may do to an asset: edit it, and edit its permissions. The
     * server's own answer for this user across every role they hold, like {@link canAddChildren}.
     *
     * Used to open a folder dialog from a Content Drive link under the same rules as the context
     * menu that would have opened it (#37759, FR-033). An absent flag reads as **not** allowed: the
     * dialogs it gates change the folder or who can reach it, so the cautious answer is the safe one.
     *
     * @param {string} assetId - Identifier of the asset
     * @returns {Observable<{ canEdit: boolean; canEditPermissions: boolean }>} The user's access
     */
    getUserAccess(assetId: string): Observable<{ canEdit: boolean; canEditPermissions: boolean }> {
        // Encoded: the id can come from a Content Drive URL (Constitution III, #37759).
        return this.#http
            .get<
                DotCMSAPIResponse<AssetPermissionsView>
            >(`${ASSET_PERMISSIONS_URL}/${encodeURIComponent(assetId)}`)
            .pipe(
                map((response) => ({
                    canEdit: response.entity?.canEdit === true,
                    canEditPermissions: response.entity?.canEditPermissions === true
                }))
            );
    }

    /**
     * Resolves whether the calling user may add children to an asset.
     *
     * Needed for the site root specifically: a folder's own CAN_ADD_CHILDREN arrives with the
     * folder tree, but the root's parent is the host, and no folder endpoint reports on it.
     * `/api/v1/folder/byPath` looks like it does — its `/` entry carries `addChildrenAllowed` — but
     * that value is resolved against the global `SYSTEM_FOLDER` singleton, which inherits from
     * SYSTEM_HOST and so answers identically for every site.
     *
     * @param {string} assetId - Identifier of the asset (a site identifier, for the drive root)
     * @returns {Observable<boolean>} Whether the calling user holds CAN_ADD_CHILDREN on it
     */
    canAddChildren(assetId: string): Observable<boolean> {
        return this.#http
            .get<
                DotCMSAPIResponse<AssetPermissionsView>
            >(`${ASSET_PERMISSIONS_URL}/${encodeURIComponent(assetId)}`)
            .pipe(
                // An instance predating the field answers without it. Denying on `undefined` would
                // strip the creation buttons from every user there, so absence reads as allowed and
                // the server keeps the final say.
                map((response) => response.entity?.canAddChildren !== false)
            );
    }
}
