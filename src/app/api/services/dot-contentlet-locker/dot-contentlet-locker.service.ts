import { CoreWebService } from 'dotcms-js/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { RequestMethod } from '@angular/http';

@Injectable()
export class DotContentletLockerService {

    constructor(private coreWebService: CoreWebService) { }

    /**
     * Lock a content asset
     *
     * @param {string} inode
     * @returns {Observable<any>}
     * @memberof PageViewService
     */
    lock(inode: string): Observable<any> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Put,
                url: `content/lock/inode/${inode}`
            })
            .pluck('bodyJsonObject');
    }

    /**
     * Unlock a content asset
     *
     * @param {string} inode
     * @returns {Observable<any>}
     * @memberof PageViewService
     */
    unlock(inode: string): Observable<any> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Put,
                url: `content/unlock/inode/${inode}`
            })
            .pluck('bodyJsonObject');
    }
}
