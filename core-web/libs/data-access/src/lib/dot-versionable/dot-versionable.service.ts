import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

export interface DotVersionable {
    inode: string;
}

@Injectable()
export class DotVersionableService {
    private httpClient = inject(HttpClient);

    /**
     * Bring back specific version of based on the inode.
     *
     * @param string inode
     * @returns Observable<DotVersionable>
     * @memberof DotVersionableService
     */
    bringBack(inode: string): Observable<DotVersionable> {
        return this.httpClient
            .put(`/api/v1/versionables/${inode}/_bringback`, {})
            .pipe(map((x) => x?.entity));
    }

    /**
     * Delete specific version based on the inode.
     *
     * @param string inode
     * @returns Observable<unknown>
     * @memberof DotVersionableService
     */
    deleteVersion(inode: string): Observable<unknown> {
        return this.httpClient.delete(`/api/v1/versionables/${inode}`).pipe(map((x) => x?.entity));
    }
}
