/* eslint-disable @typescript-eslint/no-explicit-any */
import { BehaviorSubject, Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { isInsideEditor } from '@dotcms/client';

import { DotCMSPageAsset, DynamicComponentEntity } from '../../models';

export interface ComponentItem {
    component: Promise<any>;
    module?: Promise<any>;
}

export interface DotCMSPageContext extends DotCMSPageAsset {
    isInsideEditor: boolean;
}

@Injectable({
    providedIn: 'root'
})
export class PageContextService {
    private componentsMap!: Record<string, DynamicComponentEntity>;
    private pageContext = new BehaviorSubject<DotCMSPageContext | null>(null);
    readonly pageContext$ = this.pageContext.asObservable() as Observable<DotCMSPageContext>;

    get pageContextValue(): DotCMSPageContext {
        return this.pageContext.value as DotCMSPageContext;
    }

    getComponentMap(): Record<string, DynamicComponentEntity> {
        return this.componentsMap;
    }

    setComponentMap(components: Record<string, DynamicComponentEntity>) {
        this.componentsMap = components;
    }

    /**
     * Set the context
     *
     * @protected
     * @param {DotCMSPageAsset} value
     * @memberof DotcmsContextService
     */
    setContext(value: DotCMSPageAsset) {
        this.pageContext.next({
            ...value,
            isInsideEditor: isInsideEditor()
        });
    }
}
