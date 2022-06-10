import { pluck } from 'rxjs/operators';
import { CoreWebService, DotCMSResponse } from '@dotcms/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DotContentletLockResponse } from '@models/contentlet/contentlet-locker.model';

@Injectable()
export class DotContentletLockerService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Lock a content asset
     *
     * @param string inode
     * @returns Observable<any>
     * @memberof PageViewService
     */
    lock(inode: string): Observable<DotCMSResponse<DotContentletLockResponse>> {
        return this.coreWebService
            .requestView({
                method: 'PUT',
                url: `/api/content/lock/inode/${inode}`
            })
            .pipe(pluck('body'));
    }

    /**
     * Unlock a content asset
     *
     * @param string inode
     * @returns Observable<any>
     * @memberof PageViewService
     */
    unlock(inode: string): Observable<DotCMSResponse<DotContentletLockResponse>> {
        return this.coreWebService
            .requestView<DotContentletLockResponse>({
                method: 'PUT',
                url: `/api/content/unlock/inode/${inode}`
            })
            .pipe(pluck('body'));
    }
}
