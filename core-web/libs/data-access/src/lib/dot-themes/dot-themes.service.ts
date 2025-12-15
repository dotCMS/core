import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { DotTheme } from '@dotcms/dotcms-models';

const THEMES_API_URL = '/api/v1/themes';

/**
 * Provide util methods to get themes information.
 * @export
 * @class DotThemesService
 */
@Injectable({
    providedIn: 'root'
})
export class DotThemesService {
    private readonly http = inject(HttpClient);

    /**
     * Get Theme information based on the inode.
     *
     * @param string inode
     * @returns Observable<DotTheme>
     * @memberof DotThemesService
     */
    get(inode: string): Observable<DotTheme> {
        return this.http
            .get<{ entity: DotTheme }>(`${THEMES_API_URL}/id/${inode}`)
            .pipe(pluck('entity'));
    }
}
