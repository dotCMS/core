import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

export interface DotContentletLockResponse {
    id: string;
    inode: string;
    message: string;
}

// Response type for endpoints that return bodyJsonObject
interface DotBodyJsonResponse<T> {
    bodyJsonObject: T;
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
        return this.http
            .put<
                DotBodyJsonResponse<DotContentletLockResponse>
            >(`/api/content/lock/inode/${inode}`, {})
            .pipe(map((response) => response.bodyJsonObject));
    }

    /**
     * Unlock a content asset
     *
     * @param string inode
     * @returns Observable<any>
     * @memberof PageViewService
     */
    unlock(inode: string): Observable<DotContentletLockResponse> {
        return this.http
            .put<
                DotBodyJsonResponse<DotContentletLockResponse>
            >(`/api/content/unlock/inode/${inode}`, {})
            .pipe(map((response) => response.bodyJsonObject));
    }
}
