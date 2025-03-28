import { Injectable, signal } from '@angular/core';

import { getUVEState } from '@dotcms/uve';
import { DEVELOPMENT_MODE } from '@dotcms/uve/internal';
import { DotCMSPageRendererMode, UVE_MODE } from '@dotcms/uve/types';

import { DotCMSPageContext } from '../../models';

/**
 *
 * @internal
 * @author dotCMS
 * @description This service is responsible for managing the page context.
 * @export
 * @class DotCMSContextService
 */
@Injectable({
    providedIn: 'root'
})
export class DotCMSContextService {
    private $context = signal<DotCMSPageContext | null>(null);

    /**
     * @description Get the context
     * @readonly
     * @type {DotCMSPageContext}
     * @memberof DotCMSContextService
     */
    get context(): DotCMSPageContext | null {
        return this.$context();
    }

    /**
     *
     * @description Set the context
     * @param {DotCMSPageAsset} value
     * @memberof DotCMSContextService
     */
    setContext(context: DotCMSPageContext): void {
        this.$context.set(context);
    }

    /**
     *
     * @description Check if the current mode is development
     * @param {DotCMSPageRendererMode} mode
     * @returns {boolean}
     * @memberof DotCMSContextService
     */
    isDevMode(mode?: DotCMSPageRendererMode): boolean {
        const uveState = getUVEState();

        if (uveState?.mode) {
            return uveState?.mode === UVE_MODE.EDIT;
        }

        const effectiveMode = mode ?? this.context?.mode;

        return effectiveMode === DEVELOPMENT_MODE;
    }
}
