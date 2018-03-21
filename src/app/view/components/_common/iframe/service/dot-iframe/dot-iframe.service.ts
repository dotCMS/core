import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';

@Injectable()
export class DotIframeService {
    private _actions: Subject<any> = new Subject();

    constructor() {}

    /**
     * Trigger reload action
     *
     * @memberof DotIframeService
     */
    reload(): void {
        this._actions.next('reload');
    }

    /**
     * Get reload action
     *
     * @returns {Observable<any>}
     * @memberof DotIframeService
     */
    reloaded(): Observable<any> {
        return this._actions.asObservable().filter((action: string) => action === 'reload');
    }
}
