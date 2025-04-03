import { computed, Injectable, signal } from '@angular/core';

import { getUVEState } from '@dotcms/uve';
import { DEVELOPMENT_MODE } from '@dotcms/uve/internal';
import { UVE_MODE } from '@dotcms/uve/types';

import { DotCMSPageStore } from '../models';

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
    private $store = signal<DotCMSPageStore | null>(null);

    /**
     * @description Get the store
     * @readonly
     * @type {DotCMSPageStore}
     * @memberof DotCMSStore
     */
    get store(): DotCMSPageStore | null {
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
