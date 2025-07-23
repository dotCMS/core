import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { take } from 'rxjs/operators';

import { CoreWebService, ResponseView } from '@dotcms/dotcms-js';

/**
 * Provide util methods of backend Site Browser.
 * @export
 * @class DotSiteBrowserService
 */
@Injectable()
export class DotSiteBrowserService {
    private coreWebService = inject(CoreWebService);

    /**
     * Set the selected folder in the Site Browser portlet.
     * @returns Observable<{}>
     * @memberof DotSiteBrowserService
     */
    setSelectedFolder(path: string): Observable<ResponseView<Record<string, unknown>>> {
        return this.coreWebService
            .requestView<Record<string, unknown>>({
                body: {
                    path: path
                },
                method: 'PUT',
                url: '/api/v1/browser/selectedfolder'
            })
            .pipe(take(1));
    }
}
