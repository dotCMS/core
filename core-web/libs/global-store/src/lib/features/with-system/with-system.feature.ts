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
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { switchMap } from 'rxjs/operators';

import { DotSystemConfigService } from '@dotcms/data-access';
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
}

/**
 * Initial state for the System feature.
 */
const initialSystemState: SystemState = {
    systemConfig: null
};

/**
 * Custom Store Feature for managing DotCMS system configuration.
 *
 * This feature provides state management for system-level configuration data
 * including logos, colors, release information, timezone, languages, license,
 * and cluster information. It automatically loads the configuration on initialization
 * and provides convenient computed selectors for accessing specific parts of the config.
 *
 * ## Features
 * - Auto-loads system configuration on initialization
 * - Provides typed access to system configuration data
 * - Includes computed selectors for common use cases (colors, license, languages)
 * - Modern HttpClient-based service (no deprecated dependencies)
 * - Full TypeScript support with strict typing
 *
 */
export function withSystem() {
    return signalStoreFeature(
        withState(initialSystemState),
        withMethods((store, systemConfigService = inject(DotSystemConfigService)) => ({
            /**
             * Loads the system configuration from DotCMS and updates the store.
             *
             * Fetches the system configuration from the DotSystemConfigService and stores it
             * in the feature state. This includes logos, colors, release info, timezone,
             * languages, license, and cluster information. This method is automatically
             * called on store initialization.
             *
             */
            loadSystemConfig: rxMethod<void>(
                pipe(
                    switchMap(() =>
                        systemConfigService.getSystemConfig().pipe(
                            tapResponse({
                                next: (systemConfig) => {
                                    patchState(store, {
                                        systemConfig
                                    });
                                },
                                error: (error) => {
                                    console.warn(
                                        '[withSystem] Error loading system configuration:',
                                        error
                                    );
                                }
                            })
                        )
                    )
                )
            )
        })),
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
             * Computed signal that returns the system timezone configuration.
             *
             * @returns The system timezone or null if not loaded
             */
            systemTimezone: computed(() => systemConfig()?.systemTimezone ?? null),

            /**
             * Computed signal that returns the cluster information.
             *
             * @returns The cluster information or null if not loaded
             */
            systemCluster: computed(() => systemConfig()?.cluster ?? null)
        })),
        withHooks({
            /**
             * Automatically loads the system configuration when the feature is initialized.
             *
             * This ensures the system configuration is available immediately after
             * the store is created, without requiring manual initialization.
             */
            onInit(store) {
                // Auto-load system configuration on feature initialization
                store.loadSystemConfig();
            }
        })
    );
}
