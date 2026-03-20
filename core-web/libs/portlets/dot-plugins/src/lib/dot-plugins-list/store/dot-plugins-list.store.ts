import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { EMPTY, forkJoin, Observable } from 'rxjs';

import { computed, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { catchError, debounceTime, delay, take } from 'rxjs/operators';

import {
    BundleMap,
    DotHttpErrorManagerService,
    DotOsgiService,
    PluginRow
} from '@dotcms/data-access';
import { DotcmsEventsService } from '@dotcms/dotcms-js';

/** Delay after OSGi mutating calls before reload; matches backend / websocket timing for bundle state to settle. */
const OSGI_ACTION_DELAY_MS = 5000;

/**
 * - `init`       : store created, no request fired yet
 * - `loading`    : initial load (no data yet — shows table spinner)
 * - `refreshing` : reload after an action or explicit refresh (keeps stale rows visible)
 * - `loaded`     : data ready (empty rows → empty state; rows present → table)
 * - `error`      : initial load failed
 * - `restarting` : OSGi framework restart in progress
 */
type DotPluginsListStatus = 'init' | 'loading' | 'loaded' | 'refreshing' | 'error' | 'restarting';

/** Patchable OSGi list state; `rows` is derived for the plugins table. */
interface DotPluginsListState {
    /** Installed bundles from {@link DotOsgiService.getInstalledBundles} (system bundles excluded). */
    bundles: BundleMap[];
    /** JAR filenames present in the deploy folder but not yet installed as bundles. */
    availableJars: string[];
    status: DotPluginsListStatus;
}

const initialState: DotPluginsListState = {
    bundles: [],
    availableJars: [],
    status: 'init'
};

/**
 * Signal store for the Plugins portlet list: installed OSGi bundles, undeployed JARs, and OSGi actions.
 * Subscribes to `OSGI_FRAMEWORK_RESTART` and `OSGI_BUNDLES_LOADED` to keep UI in sync cluster-wide.
 */
export const DotPluginsListStore = signalStore(
    withState<DotPluginsListState>(initialState),
    withComputed((store) => ({
        /**
         * Unified table model: one row per installed bundle plus one row per available (undeployed) JAR.
         * Undeployed rows use `state: 'undeployed'` and omit `bundleId` until deployed.
         */
        rows: computed<PluginRow[]>(() => [
            ...store.bundles().map((b) => ({
                jarFile: b.jarFile,
                symbolicName: b.symbolicName || b.jarFile,
                state: b.state,
                bundleId: b.bundleId,
                version: b.version
            })),
            ...store.availableJars().map((jar) => ({
                jarFile: jar,
                symbolicName: jar,
                state: 'undeployed' as const
            }))
        ])
    })),
    withMethods((store) => {
        const osgiService = inject(DotOsgiService);
        const httpErrorManager = inject(DotHttpErrorManagerService);

        /** Fetches installed bundles only; sets `refreshing` then `loaded` or `error`. */
        function loadBundles() {
            patchState(store, { status: 'refreshing' });
            osgiService
                .getInstalledBundles(true)
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'error' });
                        return EMPTY;
                    })
                )
                .subscribe((response) => {
                    patchState(store, {
                        bundles: response.entity ?? [],
                        status: 'loaded'
                    });
                });
        }

        /** Fetches JARs available to deploy; does not change `status` (used with `loadAll` or after errors). */
        function loadAvailablePlugins() {
            osgiService
                .getAvailablePlugins()
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        return EMPTY;
                    })
                )
                .subscribe((response) => {
                    patchState(store, { availableJars: response.entity ?? [] });
                });
        }

        /**
         * Loads bundles and available JARs together. Initial load uses `loading`; subsequent loads use `refreshing`.
         * On HTTP error, restores `loaded` so the table stays usable.
         */
        function loadAll(onSuccess?: () => void, initialLoad = false) {
            patchState(store, { status: initialLoad ? 'loading' : 'refreshing' });
            forkJoin([osgiService.getInstalledBundles(true), osgiService.getAvailablePlugins()])
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'loaded' });
                        return EMPTY;
                    })
                )
                .subscribe(([bundlesResponse, pluginsResponse]) => {
                    patchState(store, {
                        bundles: bundlesResponse.entity ?? [],
                        availableJars: pluginsResponse.entity ?? [],
                        status: 'loaded'
                    });
                    onSuccess?.();
                });
        }

        /**
         * Runs a single OSGi HTTP action, shows `refreshing`, then invokes `onSuccess` (typically reload).
         * Observable payload is ignored; errors are surfaced via {@link DotHttpErrorManagerService.handle}.
         */
        function handlePluginAction(
            source$: Observable<unknown>,
            onSuccess: () => void,
            reloadDelay = 0
        ) {
            patchState(store, { status: 'refreshing' });
            source$
                .pipe(
                    take(1),
                    delay(reloadDelay),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'loaded' });
                        return EMPTY;
                    })
                )
                .subscribe(() => onSuccess());
        }

        return {
            loadBundles,
            loadAvailablePlugins,
            loadAll,
            /** POSTs JAR files to OSGi, then reloads bundles and available JARs. */
            uploadBundles(files: File[], onSuccess?: () => void) {
                handlePluginAction(osgiService.uploadBundles(files), () => loadAll(onSuccess));
            },
            /** Deploys an undeployed JAR by filename; delayed reload so the framework can finish. */
            deploy(jar: string) {
                handlePluginAction(osgiService.deploy(jar), () => loadAll(), OSGI_ACTION_DELAY_MS);
            },
            /** Starts a bundle by JAR name; reloads bundle list only (available JARs unchanged). */
            start(jar: string) {
                handlePluginAction(
                    osgiService.start(jar),
                    () => loadBundles(),
                    OSGI_ACTION_DELAY_MS
                );
            },
            /** Stops a bundle by JAR name; reloads bundle list when the call completes. */
            stop(jar: string) {
                handlePluginAction(osgiService.stop(jar), () => loadBundles());
            },
            /** Removes a bundle; reloads both bundles and available JARs. */
            undeploy(jar: string) {
                handlePluginAction(osgiService.undeploy(jar), () => loadAll());
            },
            /** Triggers export processing for the given bundle symbolic name; reloads bundles. */
            processExports(bundle: string) {
                handlePluginAction(osgiService.processExports(bundle), () => loadBundles());
            },
            /** Restarts the OSGi framework cluster-wide, waits, then full reload. Sets `restarting` immediately. */
            restart(onSuccess?: () => void) {
                patchState(store, { status: 'restarting' });
                osgiService
                    .restart()
                    .pipe(
                        take(1),
                        delay(OSGI_ACTION_DELAY_MS),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { status: 'loaded' });
                            return EMPTY;
                        })
                    )
                    .subscribe(() => {
                        loadAll(onSuccess);
                    });
            }
        };
    }),
    withHooks((store) => ({
        /** Initial full load; listens for OSGi websocket events to update status and refresh data. */
        onInit() {
            const dotcmsEventsService = inject(DotcmsEventsService);
            const destroyRef = inject(DestroyRef);

            store.loadAll(undefined, true);

            dotcmsEventsService
                .subscribeTo('OSGI_FRAMEWORK_RESTART')
                .pipe(takeUntilDestroyed(destroyRef))
                .subscribe(() => patchState(store, { status: 'restarting' }));

            dotcmsEventsService
                .subscribeTo('OSGI_BUNDLES_LOADED')
                .pipe(debounceTime(OSGI_ACTION_DELAY_MS), takeUntilDestroyed(destroyRef))
                .subscribe(() => store.loadAll());
        }
    }))
);
