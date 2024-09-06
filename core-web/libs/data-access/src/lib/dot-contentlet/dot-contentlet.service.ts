import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { pluck, take } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Injectable()
export class DotContentletService {
    constructor(private http: HttpClient) {}

    /**
     * Get the Contentlet versions by language.
     *
     * @param string identifier
     * @param string language
     * @returns Observable<DotCMSContentlet[]>
     * @memberof DotContentletService
     */
    getContentletVersions(identifier: string, language: string): Observable<DotCMSContentlet[]> {
        return this.http
            .get(`/api/v1/content/versions?identifier=${identifier}&groupByLang=1`)
            .pipe(take(1), pluck('entity', 'versions', language));
    }

    /**
     * Get the Contentlet versions by the inode.
     *
     * @param string inode
     * @returns Observable<DotCMSContentlet>
     * @memberof DotContentletService
     */
    getContentletByInode(inode: string): Observable<DotCMSContentlet> {
        return this.http.get(`/api/v1/content/${inode}`).pipe(take(1), pluck('entity'));
    }
}
