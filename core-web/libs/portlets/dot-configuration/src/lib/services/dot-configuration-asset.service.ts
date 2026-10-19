import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, switchMap } from 'rxjs/operators';

import { DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';

/** dotAsset field holding the binary; the server returns it as a `/dA/...` path. */
const ASSET_FIELD = 'asset';

/**
 * Uploads the images the Configuration page uses (login background, login logo, navbar logo)
 * the same way the previous Configuration screen did: the file goes to temporary storage, then a
 * dotAsset is created from it and published. Publishing matters: the login page loads these
 * images anonymously, so an unpublished asset would not show there.
 */
@Injectable({ providedIn: 'root' })
export class DotConfigurationAssetService {
    readonly #http = inject(HttpClient);
    readonly #workflowActionsFireService = inject(DotWorkflowActionsFireService);

    /**
     * Uploads and publishes an image.
     *
     * @param file - Image chosen by the user.
     * @returns The `/dA/<identifier>/asset/<file name>` path to store in the configuration.
     */
    uploadImage(file: File): Observable<string> {
        const formData = new FormData();
        formData.append('file', file);

        return this.#http.post<{ tempFiles: DotCMSTempFile[] }>('/api/v1/temp', formData).pipe(
            switchMap(({ tempFiles: [tempFile] }) =>
                this.#workflowActionsFireService.publishContentletAndWaitForIndex<DotCMSContentlet>(
                    'dotAsset',
                    { [ASSET_FIELD]: tempFile.id, hostFolder: '' }
                )
            ),
            map((contentlet) => contentlet[ASSET_FIELD] as string)
        );
    }
}
