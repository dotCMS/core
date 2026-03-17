import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';

export interface DotContentletLockResponse {
    id: string;
    inode: string;
    message: string;
}

/**
 * Service to lock and unlock contentlets.
 *
 * @export
 * @class DotContentletLockerService
 * @deprecated Use DotContentletService.lockContent and DotContentletService.unlockContent instead.
 */
@Injectable()
export class DotContentletLockerService {
    private coreWebService = inject(CoreWebService);

    /**
     * Lock a content asset
     *
     * @param string inode
     * @returns Observable<any>
     * @memberof PageViewService
     */
    lock(inode: string): Observable<DotContentletLockResponse> {
        return this.coreWebService
            .requestView({
                method: 'PUT',
                url: `/api/content/lock/inode/${inode}`
            })
            .pipe(pluck('bodyJsonObject'));
    }

    /**
     * Unlock a content asset
     *
     * @param string inode
     * @returns Observable<any>
     * @memberof PageViewService
     */
    unlock(inode: string): Observable<DotContentletLockResponse> {
        return this.coreWebService
            .requestView({
                method: 'PUT',
                url: `/api/content/unlock/inode/${inode}`
            })
            .pipe(pluck('bodyJsonObject'));
    }
}
