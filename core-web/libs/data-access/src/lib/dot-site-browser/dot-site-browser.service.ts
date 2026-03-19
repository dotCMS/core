import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { take } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-models';

/**
 * Provide util methods of backend Site Browser.
 * @export
 * @class DotSiteBrowserService
 */
@Injectable()
export class DotSiteBrowserService {
    private http = inject(HttpClient);

    /**
     * Set the selected folder in the Site Browser portlet.
     * @returns Observable<DotCMSResponse<Record<string, unknown>>>
     * @memberof DotSiteBrowserService
     */
    setSelectedFolder(path: string): Observable<DotCMSResponse<Record<string, unknown>>> {
        return this.http
            .put<DotCMSResponse<Record<string, unknown>>>('/api/v1/browser/selectedfolder', {
                path
            })
            .pipe(take(1));
    }
}
