/* eslint-disable @typescript-eslint/no-explicit-any */

import { Injectable } from '@angular/core';

import { isInsideEditor } from '@dotcms/client';

import { DotCMSPageAsset, DotCMSPageComponent, DotCMSPageContext } from '../../models';

@Injectable({
    providedIn: 'root'
})
export class PageContextService {
    private pageContext: DotCMSPageContext | null = null;

    get pageContextValue(): DotCMSPageContext {
        return this.pageContext as DotCMSPageContext;
    }

    /**
     * Set the context
     *
     * @protected
     * @param {DotCMSPageAsset} value
     * @memberof DotcmsContextService
     */
    setContext(pageAsset: DotCMSPageAsset, components: DotCMSPageComponent) {
        this.pageContext = {
            components,
            pageAsset,
            isInsideEditor: isInsideEditor()
        };
    }
}
