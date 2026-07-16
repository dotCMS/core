import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

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
    private http = inject(HttpClient);

    /**
     * Lock a content asset
     *
     * @param string inode
     * @returns Observable<any>
     * @memberof PageViewService
     */
    lock(inode: string): Observable<DotContentletLockResponse> {
        return this.http.put<DotContentletLockResponse>(`/api/content/lock/inode/${inode}`, {});
    }

    /**
     * Unlock a content asset
     *
     * @param string inode
     * @returns Observable<any>
     * @memberof PageViewService
     */
    unlock(inode: string): Observable<DotContentletLockResponse> {
        return this.http.put<DotContentletLockResponse>(`/api/content/unlock/inode/${inode}`, {});
    }
}
