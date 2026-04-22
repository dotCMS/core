import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { EMPTY, Observable } from 'rxjs';

import { computed, inject } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import { DotAuthService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotAuthConfigPayload, DotAuthSiteRow, DotAuthSystemView } from '@dotcms/dotcms-models';

type DotAuthListStatus = 'init' | 'loading' | 'loaded';

interface DotAuthListState {
    system: DotAuthSystemView;
    sites: DotAuthSiteRow[];
    filter: string;
    status: DotAuthListStatus;
}

const initialState: DotAuthListState = {
    system: { configured: false, protocol: null },
    sites: [],
    filter: '',
    status: 'init'
};

export const DotAuthListStore = signalStore(
    withState<DotAuthListState>(initialState),
    withComputed((store) => ({
        /** Sites filtered client-side by hostname match. Case-insensitive. */
        filteredSites: computed(() => {
            const query = store.filter().trim().toLowerCase();
            const rows = store.sites();
            if (!query) {
                return rows;
            }
            return rows.filter((row) => row.hostName.toLowerCase().includes(query));
        })
    })),
    withMethods((store) => {
        const service = inject(DotAuthService);
        const httpErrorManager = inject(DotHttpErrorManagerService);

        function loadSites(): void {
            patchState(store, { status: 'loading' });
            service
                .listSites()
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'loaded' });
                        return EMPTY;
                    })
                )
                .subscribe((view) => {
                    patchState(store, {
                        system: view.system,
                        sites: view.sites,
                        status: 'loaded'
                    });
                });
        }

        function handle(source$: Observable<unknown>, onSuccess: () => void): void {
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
            loadSites,

            setFilter(filter: string): void {
                patchState(store, { filter });
            },

            saveSite(hostId: string, payload: DotAuthConfigPayload): void {
                handle(service.saveConfig(hostId, payload), () => loadSites());
            },

            clearSite(hostId: string): void {
                handle(service.clearConfig(hostId), () => loadSites());
            }
        };
    }),
    withHooks((store) => ({
        onInit() {
            store.loadSites();
        }
    }))
);
