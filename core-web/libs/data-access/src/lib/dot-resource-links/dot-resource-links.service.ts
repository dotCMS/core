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
     * Helper method to get resource links with common HTTP GET and entity plucking logic
     *
     * @private
     * @param {string} url - The complete URL to make the GET request
     * @return {*}  {Observable<DotResourceLinks>}
     * @memberof DotResourceLinksService
     */
    private getResourceLinks(url: string): Observable<DotResourceLinks> {
        return this.httpClient.get(url).pipe(pluck('entity'));
    }

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
        return this.getResourceLinks(`${this.basePath}/${fieldVariable}?identifier=${identifier}`);
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
        return this.getResourceLinks(`${this.basePath}/${fieldVariable}?inode=${inode}`);
    }
}
