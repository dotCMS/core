import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStoreFeature,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, Observable, of, pipe } from 'rxjs';

import { computed, inject } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';

import { catchError, filter, map, startWith, switchMap, take, tap } from 'rxjs/operators';

import { DotMessageService, DotSystemConfigService } from '@dotcms/data-access';
import { Auth, LoginService } from '@dotcms/dotcms-js';
import { DotSystemConfig } from '@dotcms/dotcms-models';

/**
 * State interface for the System feature.
 * Contains the system configuration data from the DotCMS server.
 */
export interface SystemState {
    /**
     * The system configuration data from the server.
     *
     * Contains essential server configuration including logos, colors, release info,
     * system timezone, available languages, license information, and cluster data.
     * Set to `null` when not loaded yet.
     */
    systemConfig: DotSystemConfig | null;
    /** Errors permit authenticated entry with available labels and are retried on navigation. */
    _sessionStatus: 'idle' | 'loading' | 'ready' | 'error';
}

/**
 * Initial state for the System feature.
 */
const initialSystemState: SystemState = {
    systemConfig: null,
    _sessionStatus: 'idle'
};

/**
 * Custom Store Feature for managing DotCMS system configuration.
 *
 * This feature provides state management for system-level configuration data
 * including logos, colors, release information, timezone, languages, license,
 * and cluster information. It loads configuration and translations after authentication
 * and provides convenient computed selectors for accessing specific parts of the config.
 *
 * ## Features
 * - Refreshes configuration and translations on authentication changes
 * - Provides typed access to system configuration data
 * - Includes computed selectors for common use cases (colors, license, languages)
 * - Modern HttpClient-based service (no deprecated dependencies)
 * - Full TypeScript support with strict typing
 *
 */
export function withSystem() {
    return signalStoreFeature(
        withState(initialSystemState),
        withMethods(
            (
                store,
                systemConfigService = inject(DotSystemConfigService),
                messages = inject(DotMessageService),
                loginService = inject(LoginService)
            ) => ({
                _loadSession: rxMethod<Auth | null>(
                    pipe(
                        switchMap((auth) => {
                            messages.invalidatePendingSession();
                            if (!auth?.user) {
                                patchState(store, { systemConfig: null, _sessionStatus: 'idle' });

                                return EMPTY;
                            }

                            const language =
                                auth.loginAsUser?.languageId ||
                                loginService.currentUserLanguageId ||
                                auth.sessionLanguageId ||
                                'default';
                            patchState(store, { _sessionStatus: 'loading' });

                            return systemConfigService.getSystemConfig().pipe(
                                take(1),
                                catchError((error) => {
                                    console.warn(
                                        'Unable to load configuration; fetching uncached translations.',
                                        error
                                    );

                                    return of(null);
                                }),
                                tap((systemConfig) => {
                                    if (systemConfig) patchState(store, { systemConfig });
                                }),
                                switchMap((config) =>
                                    messages
                                        .init({
                                            language,
                                            languageBundleVersion: config?.languageBundleVersion
                                        })
                                        .pipe(map(() => config != null))
                                ),
                                tapResponse({
                                    next: (configured) =>
                                        patchState(store, {
                                            _sessionStatus: configured ? 'ready' : 'error'
                                        }),
                                    error: (error) => {
                                        console.error(
                                            'Unable to load translations; continuing with available messages.',
                                            error
                                        );
                                        patchState(store, { _sessionStatus: 'error' });
                                    }
                                })
                            );
                        })
                    )
                )
            })
        ),
        withMethods((store, loginService = inject(LoginService)) => {
            const status$ = toObservable(store._sessionStatus);

            return {
                /** Refreshes configuration and the versioned translations for the current session. */
                loadSystemConfig(): void {
                    store._loadSession(loginService.auth);
                },
                /** Waits for post-login loading, retrying a previous failure on later navigation. */
                waitForSessionReady(): Observable<void> {
                    if (store._sessionStatus() === 'idle' || store._sessionStatus() === 'error') {
                        store._loadSession(loginService.auth);
                    }

                    return status$.pipe(
                        // Check current state: toObservable may still replay readiness from the previous session.
                        filter(
                            () =>
                                store._sessionStatus() === 'ready' ||
                                store._sessionStatus() === 'error'
                        ),
                        take(1),
                        map(() => void 0)
                    );
                }
            };
        }),
        withComputed(({ systemConfig }) => ({
            /**
             * Computed signal indicating whether the system configuration is loaded.
             *
             * @returns `true` if system configuration is loaded, `false` otherwise
             */
            isSystemConfigLoaded: computed(() => systemConfig() != null),

            /**
             * Computed signal that returns the current system colors.
             *
             * @returns The UI colors configuration or null if not loaded
             */
            systemColors: computed(() => systemConfig()?.colors ?? null),

            /**
             * Computed signal that returns the system license information.
             *
             * @returns The license information or null if not loaded
             */
            systemLicense: computed(() => systemConfig()?.license ?? null),

            /**
             * Computed signal that returns the available system languages.
             *
             * @returns Array of available languages or empty array if not loaded
             */
            systemLanguages: computed(() => systemConfig()?.languages ?? []),

            /**
             * Computed signal that returns the system logos configuration.
             *
             * @returns The logos configuration or null if not loaded
             */
            systemLogos: computed(() => systemConfig()?.logos ?? null),

            /**
             * Computed signal that returns the system release information.
             *
             * @returns The release information or null if not loaded
             */
            systemReleaseInfo: computed(() => systemConfig()?.releaseInfo ?? null),

            /**
             * Computed signal that returns the bulk-upload ceilings the server advertises.
             *
             * Null both when the configuration has not loaded and when the instance is older than
             * the field, which callers must treat the same way: no readable ceiling, so leave the
             * refusing to the server.
             *
             * @returns The ceilings or null if not available
             */
            systemBulkUpload: computed(() => systemConfig()?.bulkUpload ?? null),

            /**
             * The bulk folder delete ceiling the server advertises, or null when it advertises none.
             *
             * Null for the same two reasons as {@link systemBulkUpload}, treated the same way: no
             * readable ceiling, so leave the refusing to the server.
             */
            systemFolderBulkDelete: computed(() => systemConfig()?.folderBulkDelete ?? null),

            /**
             * The bulk folder duplicate ceiling the server advertises, or null when it advertises
             * none. Same reading as {@link systemFolderBulkDelete}.
             */
            systemFolderBulkDuplicate: computed(() => systemConfig()?.folderBulkDuplicate ?? null),

            /**
             * Computed signal that returns the system timezone configuration.
             *
             * @returns The system timezone or null if not loaded
             */
            systemTimezone: computed(() => systemConfig()?.systemTimezone ?? null),

            /**
             * Computed signal that returns every time zone the server accepts.
             *
             * @returns The available time zones or an empty array if not loaded
             */
            systemTimezones: computed(() => systemConfig()?.timezones ?? []),

            /**
             * Computed signal that returns the cluster information.
             *
             * @returns The cluster information or null if not loaded
             */
            systemCluster: computed(() => systemConfig()?.cluster ?? null)
        })),
        withHooks({
            onInit(store) {
                const loginService = inject(LoginService);
                // Every auth change matters: the principal ID alone misses locale and login-as changes.
                store._loadSession(loginService.auth$.pipe(startWith(loginService.auth)));
            }
        })
    );
}
