import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';
import { filter, map } from 'rxjs/operators';

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
        return this._actions.asObservable().pipe(filter((action: string) => action === 'reload'));
    }

    /**
     * Get functions to run in the iframe window
     *
     * @returns {Observable<string>}
     * @memberof DotIframeService
     */
    ran(): Observable<string> {
        return this._actions.asObservable().pipe(
            filter((action: any) => !!action.run),
            map((action: any) => action.run)
        );
    }

    /**
     * Trigger function to run
     *
     * @param {string} name
     * @memberof DotIframeService
     */
    run(name: string): void {
        this._actions.next({
            run: name
        });
    }
}
