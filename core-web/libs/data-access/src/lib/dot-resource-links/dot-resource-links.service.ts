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
    inodeOrIdentifier: string;
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
    getFileResourceLinks({
        fieldVariable,
        inodeOrIdentifier
    }: DotSourceLinksProps): Observable<DotResourceLinks> {
        return this.httpClient
            .get(`${this.basePath}/${fieldVariable}?identifier=${inodeOrIdentifier}`)
            .pipe(pluck('entity'));
    }
}
