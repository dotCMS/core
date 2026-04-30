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
import {
    DotAuthCapabilityStatus,
    DotAuthConfigPayload,
    DotAuthListFilter,
    DotAuthSiteRow,
    DotAuthSystemView
} from '@dotcms/dotcms-models';

type DotAuthListStatus = 'init' | 'loading' | 'loaded';

interface DotAuthListState {
    system: DotAuthSystemView;
    sites: DotAuthSiteRow[];
    query: string;
    filter: DotAuthListFilter;
    status: DotAuthListStatus;
}

const initialState: DotAuthListState = {
    system: { configured: false, protocol: null, headlessConfigured: false },
    sites: [],
    query: '',
    filter: 'all',
    status: 'init'
};

export const DotAuthListStore = signalStore(
    withState<DotAuthListState>(initialState),
    withComputed((store) => ({
        /** Sites filtered client-side by hostname match. Case-insensitive. */
        filteredSites: computed(() => {
            const query = store.query().trim().toLowerCase();
            const mode = store.filter();
            return store
                .sites()
                .map((row) => ({
                    ...row,
                    ssoStatus: ssoStatus(row),
                    headlessStatus: store.system().headlessConfigured
                        ? ('inherits' as DotAuthCapabilityStatus)
                        : ('disabled' as DotAuthCapabilityStatus)
                }))
                .filter((row) => !query || row.hostName.toLowerCase().includes(query))
                .filter((row) => {
                    if (mode === 'overrides') {
                        return row.ssoStatus === 'override' || row.headlessStatus === 'override';
                    }
                    if (mode === 'sso-on') {
                        return (
                            row.ssoStatus === 'enabled' ||
                            row.ssoStatus === 'override' ||
                            row.ssoStatus === 'inherits'
                        );
                    }
                    if (mode === 'headless-on') {
                        return (
                            row.headlessStatus === 'enabled' ||
                            row.headlessStatus === 'override' ||
                            row.headlessStatus === 'inherits'
                        );
                    }
                    if (mode === 'disabled') {
                        return row.ssoStatus === 'disabled' && row.headlessStatus === 'disabled';
                    }
                    return true;
                });
        }),
        stats: computed(() => {
            const rows = store.sites();
            return {
                sites: rows.length,
                ssoEnabled: rows.filter((row) => row.status !== 'NOT_CONFIGURED').length,
                headlessEnabled: store.system().headlessConfigured ? rows.length : 0,
                overrides: rows.filter((row) => row.status === 'SITE_OVERRIDE').length,
                spas: 0
            };
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

            setQuery(query: string): void {
                patchState(store, { query });
            },

            setFilter(filter: DotAuthListFilter | string): void {
                if (['all', 'overrides', 'sso-on', 'headless-on', 'disabled'].includes(filter)) {
                    patchState(store, { filter: filter as DotAuthListFilter });
                    return;
                }
                patchState(store, { query: filter });
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

function ssoStatus(row: DotAuthSiteRow): DotAuthCapabilityStatus {
    if (row.status === 'SITE_OVERRIDE') return row.protocol ? 'override' : 'disabled';
    if (row.status === 'INHERITED') return 'inherits';
    return 'disabled';
}
