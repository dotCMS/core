import { pluck } from 'rxjs/operators';
import { CoreWebService } from '@dotcms/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

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
    lock(inode: string): Observable<any> {
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
    unlock(inode: string): Observable<any> {
        return this.coreWebService
            .requestView({
                method: 'PUT',
                url: `/api/content/unlock/inode/${inode}`
            })
            .pipe(pluck('bodyJsonObject'));
    }
}
