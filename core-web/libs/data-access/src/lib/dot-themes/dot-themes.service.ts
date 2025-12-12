import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { CoreWebService, ResponseView } from '@dotcms/dotcms-js';
import { DotTheme } from '@dotcms/dotcms-models';

/**
 * Provide util methods to get themes information.
 * @export
 * @class DotThemesService
 */
@Injectable()
export class DotThemesService {
    private coreWebService = inject(CoreWebService);

    /**
     * Get Theme information based on the inode.
     *
     * @param string inode
     * @returns Observable<DotTheme>
     * @memberof DotThemesService
     */
    get(inode: string): Observable<DotTheme> {
        return this.coreWebService
            .requestView<DotTheme>({
                url: 'v1/themes/id/' + inode
            })
            .pipe(map((x: ResponseView<DotTheme>) => x?.entity));
    }
}
