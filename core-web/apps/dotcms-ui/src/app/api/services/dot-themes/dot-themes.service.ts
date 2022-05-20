import { pluck } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { CoreWebService } from '@dotcms/dotcms-js';
import { Observable } from 'rxjs';

import { DotTheme } from '@shared/models/dot-edit-layout-designer';

/**
 * Provide util methods to get themes information.
 * @export
 * @class DotThemesService
 */
@Injectable()
export class DotThemesService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Get Theme information based on the inode.
     *
     * @param string inode
     * @returns Observable<DotTheme>
     * @memberof DotThemesService
     */
    get(inode: string): Observable<DotTheme> {
        return this.coreWebService
            .requestView({
                url: 'v1/themes/id/' + inode
            })
            .pipe(pluck('entity'));
    }
}
