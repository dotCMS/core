import { patchState, signalStoreFeature, withHooks, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { merge, Observable, pipe } from 'rxjs';

import { inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotEventsSocket, WebSocketStatus } from '@dotcms/data-access';

export interface WebSocketState {
    wsStatus: WebSocketStatus;
}

const initialWebSocketState: WebSocketState = {
    wsStatus: 'connecting'
};

/**
 * Store feature that manages the WebSocket connection lifecycle and exposes
 * its status as a signal.
 *
 * - Starts the connection automatically in `onInit`
 * - Destroys the connection in `onDestroy`
 * - Keeps `wsStatus` in sync via `status$()`
 *
 * Consumers read `globalStore.wsStatus()` — the UI indicator only shows when not 'connected'.
 */
export function withWebSocket() {
    return signalStoreFeature(
        withState(initialWebSocketState),
        withMethods((store, eventsSocket = inject(DotEventsSocket)) => ({
            startConnection: rxMethod<void>(pipe(switchMap(() => eventsSocket.connect()))),
            trackStatus: rxMethod<void>(
                pipe(
                    switchMap(() =>
                        eventsSocket
                            .status$()
                            .pipe(tap((wsStatus) => patchState(store, { wsStatus })))
                    )
                )
            ),
            /**
             * Observable that emits when the backend sends UPDATE_PORTLET_LAYOUTS.
             * Use this instead of the deprecated DotcmsEventsService.
             */
            portletLayoutUpdated$: (): Observable<void> =>
                eventsSocket.on<void>('UPDATE_PORTLET_LAYOUTS'),

            /**
             * Observable that emits whenever a site is created, published,
             * archived, unarchived, or updated. Use this to refresh site lists.
             */
            siteEvents$: (): Observable<void> =>
                merge(
                    eventsSocket.on<void>('SAVE_SITE'),
                    eventsSocket.on<void>('PUBLISH_SITE'),
                    eventsSocket.on<void>('UN_PUBLISH_SITE'),
                    eventsSocket.on<void>('UPDATE_SITE'),
                    eventsSocket.on<void>('ARCHIVE_SITE'),
                    eventsSocket.on<void>('UN_ARCHIVE_SITE'),
                    eventsSocket.on<void>('DELETE_SITE')
                )
        })),
        withHooks({
            onInit(store) {
                store.startConnection();
                store.trackStatus();
            },
            onDestroy(_store, eventsSocket = inject(DotEventsSocket)) {
                eventsSocket.destroy();
            }
        })
    );
}
