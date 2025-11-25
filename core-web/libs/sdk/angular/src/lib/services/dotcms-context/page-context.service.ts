import { BehaviorSubject, Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import { isInsideEditor } from '@dotcms/client';

import { DotCMSPageComponent, DotCMSPageContext } from '../../models';
import { DotCMSPageAsset } from '../../models/dotcms.model';

/**
 * @author dotCMS
 * @description This service is responsible for managing the page context.
 * @export
 * @class PageContextService
 */
@Injectable({
    providedIn: 'root'
})
export class PageContextService {
    private context$ = new BehaviorSubject<DotCMSPageContext | null>(null);

    /**
     * @description Get the context
     * @readonly
     * @type {DotCMSPageContext}
     * @memberof PageContextService
     */
    get context(): DotCMSPageContext {
        return this.context$.getValue() as DotCMSPageContext;
    }

    /**
     * @description Get the context as an observable
     * @readonly
     * @memberof PageContextService
     */
    get contextObs$() {
        return this.context$.asObservable();
    }

    /**
     * @description Get the current page asset
     * @readonly
     * @type {(Observable<DotCMSPageAsset | null>)}
     * @memberof PageContextService
     */
    get currentPage$(): Observable<DotCMSPageAsset | null> {
        return this.contextObs$.pipe(map((context) => context?.pageAsset || null));
    }

    /**
     *
     * @description Set the context
     * @param {DotCMSPageAsset} value
     * @memberof DotcmsContextService
     */
    setContext(pageAsset: DotCMSPageAsset, components: DotCMSPageComponent) {
        this.context$.next({
            pageAsset,
            components,
            isInsideEditor: isInsideEditor()
        });
    }

    /**
     * @description Set the page asset in the context
     * @param {DotCMSPageAsset} pageAsset
     * @memberof PageContextService
     */
    setPageAsset(pageAsset: DotCMSPageAsset) {
        this.context$.next({
            ...this.context,
            pageAsset
        });
    }
}
