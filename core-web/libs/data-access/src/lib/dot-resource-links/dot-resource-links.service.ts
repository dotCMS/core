import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { pluck } from 'rxjs/operators';

export interface DotResourceLinks {
    configuredImageURL: string;
    idPath: string;
    mimeType: string;
    text: string;
    versionPath: string;
}

interface DotSourceLinksProps {
    fieldVariable: string;
    identifier: string;
}

interface DotSourceLinksByInodeProps {
    fieldVariable: string;
    inode: string;
}

@Injectable({
    providedIn: 'root'
})
export class DotResourceLinksService {
    private readonly basePath = '/api/v1/content/resourcelinks/field';
    private readonly httpClient = inject(HttpClient);

    /**
     * It returns the source links for a file
     *
     * @param {DotSourceLinksProps}
     * @return {*}  {Observable<DotResourceLinks>}
     * @memberof DotResourceLinksService
     */
    getFileResourceLinksByIdentifier({
        fieldVariable,
        identifier
    }: DotSourceLinksProps): Observable<DotResourceLinks> {
        return this.httpClient
            .get(`${this.basePath}/${fieldVariable}?identifier=${identifier}`)
            .pipe(pluck('entity'));
    }

    /**
     * It returns the source links for a file by inode
     *
     * @param {DotSourceLinksByInodeProps}
     * @return {*}  {Observable<DotResourceLinks>}
     * @memberof DotResourceLinksService
     */
    getFileResourceLinksByInode({
        fieldVariable,
        inode
    }: DotSourceLinksByInodeProps): Observable<DotResourceLinks> {
        return this.httpClient
            .get(`${this.basePath}/${fieldVariable}?inode=${inode}`)
            .pipe(pluck('entity'));
    }
}
