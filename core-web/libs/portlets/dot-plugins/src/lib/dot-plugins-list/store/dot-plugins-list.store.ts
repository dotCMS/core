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
    DotEventsSocket,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotOsgiService,
    PluginRow
} from '@dotcms/data-access';
import { DotEnvironment, DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';

/** Delay after OSGi mutating calls before reload; matches backend / websocket timing for bundle state to settle. */
const OSGI_ACTION_DELAY_MS = 5000;

/**
 * - `init`       : store created, no request fired yet
 * - `loading`    : initial load (no data yet — shows table spinner)
 * - `refreshing` : reload after an action or explicit refresh (keeps stale rows visible)
 * - `loaded`     : data ready (empty rows → empty state; rows present → table)
 * - `error`      : initial load failed
 * - `restarting` : OSGi framework restart in progress
 * - `uploading`  : JAR files are being transferred to the server
 */
type DotPluginsListStatus =
    | 'init'
    | 'loading'
    | 'loaded'
    | 'refreshing'
    | 'error'
    | 'restarting'
    | 'uploading';

/** Patchable OSGi list state; `rows` is derived for the plugins table. */
interface DotPluginsListState {
    /** Installed bundles from {@link DotOsgiService.getInstalledBundles} (system bundles excluded). */
    bundles: BundleMap[];
    /** JAR filenames present in the deploy folder but not yet installed as bundles. */
    availableJars: string[];
    status: DotPluginsListStatus;
    isEnterprise: boolean;
    pushPublishEnvironments: DotEnvironment[];
}

const initialState: DotPluginsListState = {
    bundles: [],
    availableJars: [],
    status: 'init',
    isEnterprise: false,
    pushPublishEnvironments: []
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
         * Loads bundles and available JARs together. Initial load uses `loading`; subsequent loads
         * use `intermediateStatus` (defaults to `refreshing`). Callers that manage their own loading
         * indicator (e.g. `uploadBundles`) pass their own status to keep the UI consistent.
         * On HTTP error, restores `loaded` so the table stays usable.
         */
        function loadAll(
            onSuccess?: () => void,
            initialLoad = false,
            intermediateStatus: DotPluginsListStatus = 'refreshing'
        ) {
            patchState(store, { status: initialLoad ? 'loading' : intermediateStatus });
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
            /** Sets enterprise license flag and push publish environments resolved from the route. */
            setEnterpriseData(isEnterprise: boolean, pushPublishEnvironments: DotEnvironment[]) {
                patchState(store, { isEnterprise, pushPublishEnvironments });
            },
            /**
             * Transitions to `uploading` status. Called by the list component when the upload
             * dialog closes successfully after the HTTP call completes in the dialog itself.
             * Keeps the uploading indicator visible until `OSGI_BUNDLES_LOADED` triggers a reload.
             */
            setUploadingStatus() {
                patchState(store, { status: 'uploading' });
            },
            /**
             * POSTs JAR files to OSGi. Sets `uploading` on a successful 200 response and keeps
             * that state through the data reload, so the uploading indicator stays visible until
             * the table reflects the new plugins. On upload error the status resets to `loaded`.
             * Used by the drag-and-drop path on the list page (no dialog open to show inline errors).
             */
            uploadBundles(files: File[]) {
                patchState(store, { status: 'uploading' });
                osgiService
                    .uploadBundles(files)
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { status: 'loaded' });
                            return EMPTY;
                        })
                    )
                    .subscribe();
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
            const eventsSocket = inject(DotEventsSocket);
            const dotMessageDisplayService = inject(DotMessageDisplayService);
            const dotMessageService = inject(DotMessageService);
            const destroyRef = inject(DestroyRef);

            store.loadAll(undefined, true);

            eventsSocket
                .on<void>('OSGI_FRAMEWORK_RESTART')
                .pipe(takeUntilDestroyed(destroyRef))
                .subscribe(() => patchState(store, { status: 'restarting' }));

            eventsSocket
                .on<void>('OSGI_BUNDLES_LOADED')
                .pipe(debounceTime(OSGI_ACTION_DELAY_MS), takeUntilDestroyed(destroyRef))
                .subscribe(() =>
                    store.loadAll(
                        undefined,
                        false,
                        store.status() === 'uploading' ? 'uploading' : 'refreshing'
                    )
                );

            eventsSocket
                .on<void>('OSGI_BUNDLES_UPLOAD_FAILED')
                .pipe(takeUntilDestroyed(destroyRef))
                .subscribe(() => {
                    patchState(store, { status: 'loaded' });
                    dotMessageDisplayService.push({
                        life: 5000,
                        message: dotMessageService.get('plugins.upload.failed'),
                        severity: DotMessageSeverity.ERROR,
                        type: DotMessageType.SIMPLE_MESSAGE
                    });
                });
        }
    }))
);
