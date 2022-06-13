import { Injectable } from '@angular/core';
import { CoreWebService, ResponseView } from '@dotcms/dotcms-js';
import { Observable } from 'rxjs';
import { take } from 'rxjs/operators';

/**
 * Provide util methods of backend Site Browser.
 * @export
 * @class DotSiteBrowserService
 */
@Injectable()
export class DotSiteBrowserService {
    constructor(private coreWebService: CoreWebService) {}

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
