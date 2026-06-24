import { fromEvent, map } from 'rxjs';

import { computed, Injectable, Signal, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { DotCMSPageAsset, UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';
import {
    ANALYTICS_READY_EVENT,
    DEVELOPMENT_MODE,
    isDotAnalyticsActive,
    PRODUCTION_MODE
} from '@dotcms/uve/internal';

import { DotCMSPageStore } from '../models';

export const EMPTY_DOTCMS_PAGE_STORE: DotCMSPageStore = {
    page: {} as DotCMSPageAsset,
    components: {},
    mode: PRODUCTION_MODE
};

/**
 * @description This service is responsible for managing the page context.
 * @internal
 * @author dotCMS
 * @export
 * @class DotCMSStore
 */
@Injectable({
    providedIn: 'root'
})
export class DotCMSStore {
    private $store = signal<DotCMSPageStore>(EMPTY_DOTCMS_PAGE_STORE);

    /**
     * @description Get whether DotCMS Analytics is active on the page. Used in
     * live mode to decide if the minimal contentlet attributes Analytics needs
     * should be kept.
     *
     * Analytics may initialize after the page renders, so we track its `ready`
     * event as a stream and project it into a signal. `fromEvent` registers via
     * zone-patched `addEventListener`, so change detection is scheduled without
     * any manual `NgZone` handling.
     * @readonly
     * @type {Signal<boolean>}
     * @memberof DotCMSStore
     */
    $isAnalyticsActive: Signal<boolean> =
        typeof window === 'undefined'
            ? signal(false)
            : toSignal(
                  fromEvent(window, ANALYTICS_READY_EVENT).pipe(map(() => isDotAnalyticsActive())),
                  { initialValue: isDotAnalyticsActive() }
              );

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
     * @description Set the store
     * @param {DotCMSPageStore} value
     * @memberof DotCMSStore
     */
    setStore(store: DotCMSPageStore): void {
        this.$store.set(store);
    }

    /**
     * @description Get if the current context is in development mode
     * @readonly
     * @type {boolean}
     * @memberof DotCMSStore
     */
    $isDevMode = computed(() => {
        const uveState = getUVEState();

        if (uveState?.mode) {
            return uveState?.mode === UVE_MODE.EDIT;
        }

        return this.$store()?.mode === DEVELOPMENT_MODE;
    });
}
