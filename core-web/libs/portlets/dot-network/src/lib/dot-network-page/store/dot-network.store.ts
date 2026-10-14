import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';

import { exhaustMap, tap } from 'rxjs/operators';

import { DotClusterService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { ComponentStatus, DotClusterNode, DotClusterNodes } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

interface DotNetworkState {
    nodes: DotClusterNode[];
    currentServerId: string;
    selectedServerId: string | null;
    licensed: boolean;
    status: ComponentStatus;
}

const initialState: DotNetworkState = {
    nodes: [],
    currentServerId: '',
    selectedServerId: null,
    licensed: true,
    status: ComponentStatus.INIT
};

/**
 * `error-key` the backend sets on a license refusal (`InvalidLicenseExceptionMapper`). It tells
 * "this install's license does not cover cluster data" apart from any other 403.
 */
const LICENSE_ERROR_KEY = 'dotcms.api.error.license.required';

function isLicenseRefusal(error: HttpErrorResponse): boolean {
    return error.status === 403 && error.headers?.get('error-key') === LICENSE_ERROR_KEY;
}

/**
 * Keeps the node the user picked when it is still in the list. Otherwise falls back to the node
 * that served the request, then to the first node.
 */
function resolveSelection(previous: string | null, result: DotClusterNodes): string | null {
    const ids = new Set(result.nodes.map((node) => node.serverId));

    if (previous && ids.has(previous)) {
        return previous;
    }

    if (ids.has(result.currentServerId)) {
        return result.currentServerId;
    }

    return result.nodes[0]?.serverId ?? null;
}

/** The node serving the request comes first; the rest keep the order the backend sent. */
function currentNodeFirst(nodes: DotClusterNode[], currentServerId: string): DotClusterNode[] {
    const current = nodes.filter((node) => node.serverId === currentServerId);

    return [...current, ...nodes.filter((node) => node.serverId !== currentServerId)];
}

export const DotNetworkStore = signalStore(
    withState<DotNetworkState>(initialState),
    withComputed((store, globalStore = inject(GlobalStore)) => {
        const hasNodes = computed(() => store.nodes().length > 0);
        const isLoading = computed(() => store.status() === ComponentStatus.LOADING);
        const isLoaded = computed(() => store.status() === ComponentStatus.LOADED);

        return {
            selectedNode: computed(
                () =>
                    store.nodes().find((node) => node.serverId === store.selectedServerId()) ?? null
            ),
            sortedNodes: computed(() => currentNodeFirst(store.nodes(), store.currentServerId())),
            /** First load only. A refresh keeps the current data on screen. */
            showSkeleton: computed(
                () => (isLoading() || store.status() === ComponentStatus.INIT) && !hasNodes()
            ),
            isRefreshing: computed(() => isLoading() && hasNodes()),
            /** A failed refresh keeps the previous data on screen; only the toast reports it. */
            showError: computed(() => store.status() === ComponentStatus.ERROR && !hasNodes()),
            showUnlicensed: computed(() => isLoaded() && !store.licensed()),
            showEmpty: computed(() => isLoaded() && store.licensed() && !hasNodes()),
            /**
             * Follows the data on screen, including while refreshing or after a failed refresh.
             * Never shown on its own: no nodes or no license have their own state.
             */
            // The flag comes from the app configuration (`cluster.heartbeatEnabled`), not from
            // the node response. Absent on older instances, which counts as on.
            showHeartbeatNotice: computed(
                () =>
                    store.licensed() &&
                    hasNodes() &&
                    globalStore.systemCluster()?.heartbeatEnabled === false
            )
        };
    }),
    withMethods(
        (
            store,
            clusterService = inject(DotClusterService),
            httpErrorManager = inject(DotHttpErrorManagerService)
        ) => ({
            /**
             * Loads every node. Also used by Refresh, which keeps the current selection.
             * A call made while a request is in flight is ignored: every status request makes
             * each node write a test file to the shared assets volume, so it is never duplicated.
             */
            load: rxMethod<void>(
                pipe(
                    tap(() => patchState(store, { status: ComponentStatus.LOADING })),
                    exhaustMap(() =>
                        clusterService.getNodes().pipe(
                            tapResponse({
                                next: (result) =>
                                    patchState(store, {
                                        nodes: result.nodes,
                                        currentServerId: result.currentServerId,
                                        licensed: true,
                                        selectedServerId: resolveSelection(
                                            store.selectedServerId(),
                                            result
                                        ),
                                        status: ComponentStatus.LOADED
                                    }),
                                error: (error: HttpErrorResponse) => {
                                    // A license refusal is a state the page renders (the
                                    // Clustering upsell), not an error: the shared handler would
                                    // add a generic "license required" toast on top of it.
                                    if (isLicenseRefusal(error)) {
                                        patchState(store, {
                                            nodes: [],
                                            currentServerId: '',
                                            selectedServerId: null,
                                            licensed: false,
                                            status: ComponentStatus.LOADED
                                        });

                                        return;
                                    }

                                    httpErrorManager.handle(error);
                                    patchState(store, { status: ComponentStatus.ERROR });
                                }
                            })
                        )
                    )
                )
            ),
            selectNode(serverId: string): void {
                patchState(store, { selectedServerId: serverId });
            }
        })
    ),
    withHooks({
        onInit(store) {
            store.load();
        }
    })
);
