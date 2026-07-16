import { Observable, Subject } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { filter } from 'rxjs/operators';

import { DotFunctionInfo } from '@dotcms/dotcms-models';

import { DotRouterService } from '../dot-router/dot-router.service';

@Injectable()
export class DotIframeService {
    private dotRouterService = inject(DotRouterService);

    private _actions: Subject<DotFunctionInfo> = new Subject();

    /**
     * Trigger reload action
     *
     * @memberof DotIframeService
     */
    reload(): void {
        this._actions.next({ name: 'reload' });
    }

    /**
     * Get reload action
     *
     * @returns Observable<DotFunctionInfo>
     * @memberof DotIframeService
     */
    reloaded(): Observable<DotFunctionInfo> {
        return this._actions
            .asObservable()
            .pipe(filter((func: DotFunctionInfo) => func.name === 'reload'));
    }

    /**
     * Get reload colors action
     *
     * @returns Observable<DotFunctionInfo>
     * @memberof DotIframeService
     */
    reloadedColors(): Observable<DotFunctionInfo> {
        return this._actions
            .asObservable()
            .pipe(filter((func: DotFunctionInfo) => func.name === 'colors'));
    }

    /**
     * Get functions to run in the iframe window
     *
     * @returns Observable<DotFunctionInfo>
     * @memberof DotIframeService
     */
    ran(): Observable<DotFunctionInfo> {
        return this._actions.asObservable().pipe(
            filter((action: DotFunctionInfo) => {
                return !!action.name;
            })
        );
    }

    /**
     * Trigger function to run
     *
     * @param DotFunctionInfo func
     * @memberof DotIframeService
     */
    run(func: DotFunctionInfo): void {
        this._actions.next(func);
    }

    /**
     * Run a function to reload the data in the portlet
     *
     * @param string portlet
     * @memberof DotIframeService
     */
    reloadData(portlet: string): void {
        const functionToRun = this.getFunctionToRefreshIframe(portlet);

        if (functionToRun) {
            this.run(functionToRun);
        }
    }

    /**
     * Reload the colors in the jsp
     *
     * @memberof DotIframeService
     */
    reloadColors(): void {
        this._actions.next({ name: 'colors' });
    }

    private getFunctionToRefreshIframe(portlet: string): DotFunctionInfo {
        const mapOfFunctions: Record<string, string> = {
            content: 'doSearch',
            'site-browser': 'reloadContent',
            'vanity-urls': 'doSearch',
            sites: 'refreshHostTable',
            calendar: 'initializeCalendar',
            workflow: 'doFilter'
        };

        return this.dotRouterService.isCustomPortlet(portlet)
            ? { name: mapOfFunctions['content'] }
            : { name: mapOfFunctions[portlet] };
    }
}
