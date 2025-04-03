import { computed, Injectable, signal } from '@angular/core';

import { getUVEState } from '@dotcms/uve';
import { DEVELOPMENT_MODE, PRODUCTION_MODE } from '@dotcms/uve/internal';
import { DotCMSPageAsset, UVE_MODE } from '@dotcms/uve/types';

import { DotCMSPageStore } from '../models';

export const EMPTY_DOTCMS_PAGE_STORE: DotCMSPageStore = {
    page: {} as DotCMSPageAsset,
    components: {},
    mode: PRODUCTION_MODE
};

/**
 *
 * @internal
 * @author dotCMS
 * @description This service is responsible for managing the page context.
 * @export
 * @class DotCMSStore
 */
@Injectable({
    providedIn: 'root'
})
export class DotCMSStore {
    private $store = signal<DotCMSPageStore>(EMPTY_DOTCMS_PAGE_STORE);

    /**
     * @description Get the store
     * @readonly
     * @type {DotCMSPageStore}
     * @memberof DotCMSStore
     */
    get store(): DotCMSPageStore {
        return this.$store();
    }

    /**
     *
     * @description Set the store
     * @param {DotCMSPageStore} value
     * @memberof DotCMSStore
     */
    setStore(store: DotCMSPageStore): void {
        this.$store.set(store);
    }

    $isDevMode = computed(() => {
        const uveState = getUVEState();

        if (uveState?.mode) {
            return uveState?.mode === UVE_MODE.EDIT;
        }

        return this.$store()?.mode === DEVELOPMENT_MODE;
    });
}
