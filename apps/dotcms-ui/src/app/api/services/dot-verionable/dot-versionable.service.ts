import { Injectable } from '@angular/core';
import { CoreWebService } from '@dotcms/dotcms-js';
import { Observable } from 'rxjs';
import { pluck } from 'rxjs/operators';

export interface DotVersionable {
    inode: string;
}

@Injectable()
export class DotVersionableService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Bring back specific version of based on the inode.
     *
     * @param string inode
     * @returns Observable<DotVersionable>
     * @memberof DotVersionableService
     */
    bringBack(inode: string): Observable<DotVersionable> {
        return this.coreWebService
            .requestView({
                method: 'PUT',
                url: `/api/v1/versionables/${inode}/_bringback`
            })
            .pipe(pluck('entity'));
    }
}
