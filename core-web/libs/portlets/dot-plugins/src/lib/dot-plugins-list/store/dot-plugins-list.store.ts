import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { EMPTY, Observable } from 'rxjs';

import { DestroyRef, effect, inject, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { catchError, debounceTime, take } from 'rxjs/operators';

import { BundleMap, DotHttpErrorManagerService, DotOsgiService } from '@dotcms/data-access';
import { DotcmsEventsService } from '@dotcms/dotcms-js';

type DotPluginsListStatus = 'init' | 'loading' | 'loaded' | 'error' | 'restarting';

interface DotPluginsListState {
    bundles: BundleMap[];
    availableJars: string[];
    status: DotPluginsListStatus;
    ignoreSystemBundles: boolean;
}

const initialState: DotPluginsListState = {
    bundles: [],
    availableJars: [],
    status: 'init',
    ignoreSystemBundles: true
};

export const DotPluginsListStore = signalStore(
    withState<DotPluginsListState>(initialState),
    withMethods((store) => {
        const osgiService = inject(DotOsgiService);
        const httpErrorManager = inject(DotHttpErrorManagerService);

        function loadBundles() {
            patchState(store, { status: 'loading' });
            osgiService
                .getInstalledBundles(store.ignoreSystemBundles())
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

        function handlePluginAction(source$: Observable<unknown>, onSuccess: () => void) {
            patchState(store, { status: 'loading' });
            source$
                .pipe(
                    take(1),
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
            setIgnoreSystemBundles(value: boolean) {
                patchState(store, { ignoreSystemBundles: value });
            },
            uploadBundles(files: File[], onSuccess?: () => void) {
                handlePluginAction(osgiService.uploadBundles(files), () => {
                    loadBundles();
                    loadAvailablePlugins();
                    onSuccess?.();
                });
            },
            deploy(jar: string) {
                handlePluginAction(osgiService.deploy(jar), () => {
                    loadBundles();
                    loadAvailablePlugins();
                });
            },
            start(jar: string) {
                handlePluginAction(osgiService.start(jar), () => loadBundles());
            },
            stop(jar: string) {
                handlePluginAction(osgiService.stop(jar), () => loadBundles());
            },
            undeploy(jar: string) {
                handlePluginAction(osgiService.undeploy(jar), () => {
                    loadBundles();
                    loadAvailablePlugins();
                });
            },
            processExports(bundle: string) {
                handlePluginAction(osgiService.processExports(bundle), () => loadBundles());
            },
            restart(onSuccess?: () => void) {
                handlePluginAction(osgiService.restart(), () => {
                    loadBundles();
                    loadAvailablePlugins();
                    onSuccess?.();
                });
            }
        };
    }),
    withHooks((store) => ({
        onInit() {
            const dotcmsEventsService = inject(DotcmsEventsService);
            const destroyRef = inject(DestroyRef);

            effect(() => {
                store.ignoreSystemBundles();
                untracked(() => {
                    store.loadBundles();
                    store.loadAvailablePlugins();
                });
            });

            dotcmsEventsService
                .subscribeTo('OSGI_FRAMEWORK_RESTART')
                .pipe(takeUntilDestroyed(destroyRef))
                .subscribe(() => patchState(store, { status: 'restarting' }));

            dotcmsEventsService
                .subscribeTo('OSGI_BUNDLES_LOADED')
                .pipe(debounceTime(4000), takeUntilDestroyed(destroyRef))
                .subscribe(() => {
                    store.loadBundles();
                    store.loadAvailablePlugins();
                });
        }
    }))
);
