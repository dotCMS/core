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

interface DotPluginsListState {
    bundles: BundleMap[];
    availableJars: string[];
    status: DotPluginsListStatus;
}

const initialState: DotPluginsListState = {
    bundles: [],
    availableJars: [],
    status: 'init'
};

export const DotPluginsListStore = signalStore(
    withState<DotPluginsListState>(initialState),
    withComputed((store) => ({
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

        function loadBundles() {
            patchState(store, { status: 'loading' });
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
            uploadBundles(files: File[], onSuccess?: () => void) {
                handlePluginAction(osgiService.uploadBundles(files), () => loadAll(onSuccess));
            },
            deploy(jar: string) {
                handlePluginAction(osgiService.deploy(jar), () => loadAll(), OSGI_ACTION_DELAY_MS);
            },
            start(jar: string) {
                handlePluginAction(
                    osgiService.start(jar),
                    () => loadBundles(),
                    OSGI_ACTION_DELAY_MS
                );
            },
            stop(jar: string) {
                handlePluginAction(osgiService.stop(jar), () => loadBundles());
            },
            undeploy(jar: string) {
                handlePluginAction(osgiService.undeploy(jar), () => loadAll());
            },
            processExports(bundle: string) {
                handlePluginAction(osgiService.processExports(bundle), () => loadBundles());
            },
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
