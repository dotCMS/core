import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStore,
    withComputed,
    withMethods,
    withProps,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, Subject } from 'rxjs';

import { computed, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotAppsService, DotMessageService } from '@dotcms/data-access';
import {
    ComponentStatus,
    dialogAction,
    DotApp,
    DotAppsExportConfiguration,
    DotAppsImportConfiguration,
    DotAppsSite
} from '@dotcms/dotcms-models';

export interface DotAppsImportExportDialogState {
    visible: boolean;
    action: dialogAction | null;
    app: DotApp | null;
    site: DotAppsSite | null;
    status: ComponentStatus;
    errorMessage: string | null;
}

const initialState: DotAppsImportExportDialogState = {
    visible: false,
    action: null,
    app: null,
    site: null,
    status: ComponentStatus.INIT,
    errorMessage: null
};

// Subject to emit when import succeeds
const importSuccessSubject = new Subject<void>();

export const DotAppsImportExportDialogStore = signalStore(
    { providedIn: 'root' },
    withState(initialState),
    withComputed((state) => ({
        isLoading: computed(() => state.status() === ComponentStatus.LOADING),
        isExport: computed(() => state.action() === dialogAction.EXPORT),
        isImport: computed(() => state.action() === dialogAction.IMPORT),
        dialogHeaderKey: computed(() => {
            const action = state.action();
            if (action === dialogAction.EXPORT) {
                return 'apps.confirmation.export.header';
            } else if (action === dialogAction.IMPORT) {
                return 'apps.confirmation.import.header';
            }

            return '';
        })
    })),
    withProps(() => ({
        /**
         * Observable that emits when import succeeds
         */
        importSuccess$: importSuccessSubject.asObservable()
    })),
    withMethods((store) => {
        const dotAppsService = inject(DotAppsService);
        const dotMessageService = inject(DotMessageService);

        return {
            /**
             * Open the export dialog
             */
            openExport: (app: DotApp, site?: DotAppsSite) => {
                patchState(store, {
                    visible: true,
                    action: dialogAction.EXPORT,
                    app,
                    site: site ?? null,
                    status: ComponentStatus.INIT,
                    errorMessage: null
                });
            },

            /**
             * Open the import dialog
             */
            openImport: () => {
                patchState(store, {
                    visible: true,
                    action: dialogAction.IMPORT,
                    app: null,
                    site: null,
                    status: ComponentStatus.INIT,
                    errorMessage: null
                });
            },

            /**
             * Close the dialog and reset state
             */
            close: () => {
                patchState(store, initialState);
            },

            /**
             * Set error message
             */
            setError: (errorMessage: string) => {
                patchState(store, {
                    status: ComponentStatus.ERROR,
                    errorMessage
                });
            },

            /**
             * Export configuration
             */
            exportConfiguration: rxMethod<{ password: string }>(
                pipe(
                    tap(() => patchState(store, { status: ComponentStatus.LOADING })),
                    switchMap(({ password }) => {
                        const app = store.app();
                        const site = store.site();

                        const getAllKeySitesConfig = (): { [key: string]: string[] } => {
                            const keySitesConf: { [key: string]: string[] } = {};
                            if (app) {
                                app.sites?.forEach((s: DotAppsSite) => {
                                    if (s.configured) {
                                        keySitesConf[s.id] = [app.key];
                                    }
                                });
                            }

                            return keySitesConf;
                        };

                        const requestConfiguration: DotAppsExportConfiguration = {
                            password,
                            exportAll: app ? false : true,
                            appKeysBySite: site
                                ? { [site.id]: [app?.key ?? ''] }
                                : getAllKeySitesConfig()
                        };

                        return dotAppsService
                            .exportConfiguration(requestConfiguration)
                            .then((errorMsg: string) => {
                                if (errorMsg) {
                                    patchState(store, {
                                        status: ComponentStatus.ERROR,
                                        errorMessage:
                                            dotMessageService.get(
                                                'apps.confirmation.export.error'
                                            ) +
                                            ': ' +
                                            errorMsg
                                    });
                                } else {
                                    patchState(store, initialState);
                                }

                                return errorMsg;
                            });
                    })
                )
            ),

            /**
             * Import configuration
             */
            importConfiguration: rxMethod<DotAppsImportConfiguration>(
                pipe(
                    tap(() => patchState(store, { status: ComponentStatus.LOADING })),
                    switchMap((config) => {
                        return dotAppsService.importConfiguration(config).pipe(
                            tapResponse({
                                next: (status: string) => {
                                    if (status !== '400') {
                                        patchState(store, initialState);
                                        importSuccessSubject.next();
                                    } else {
                                        patchState(store, {
                                            status: ComponentStatus.ERROR,
                                            errorMessage: 'Import failed'
                                        });
                                    }
                                },
                                error: () => {
                                    patchState(store, {
                                        status: ComponentStatus.ERROR,
                                        errorMessage: 'Import failed'
                                    });
                                }
                            })
                        );
                    })
                )
            )
        };
    })
);
