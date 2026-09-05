import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotAiConfigService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotAiResolvedConfig } from '@dotcms/dotcms-models';

import { DOT_AI_DEFAULT_THRESHOLD, DotAiPortletState } from '../../models/dot-ai-portlet.models';

/**
 * dotAI configuration.
 *
 * Composed **first**, because it seeds two defaults the retrieval panel needs: the closeness
 * threshold (from the resolved `embeddingsSearchThreshold` setting) and the default chat
 * model (the first entry of the provider's comma-separated fallback list).
 *
 * `isConfigured` gates Search / Send / Generate / Build (FR-047) but deliberately does not
 * blank the portlet — Config Values and Embeddings stay reachable, which is exactly when you
 * need them (FR-048).
 */
export function withAiConfig() {
    return signalStoreFeature(
        type<{ state: DotAiPortletState }>(),
        withComputed((store) => ({
            /**
             * Whether to tell the user dotAI is unconfigured.
             *
             * Gated on `configLoaded`, not just `!isConfigured`: the store starts out
             * unconfigured because nothing has loaded yet, so binding the banner straight to
             * `isConfigured` renders it during every initial async window and then animates
             * it away — a flash on every load (FR-047).
             */
            showNotConfigured: computed(() => store.configLoaded() && !store.isConfigured()),

            /** The resolved config reassembled from state, for the Config Values screen. */
            resolvedConfig: computed<DotAiResolvedConfig>(() => ({
                configHost: store.configHost(),
                settings: store.settings(),
                providerConfig: store.providerConfig(),
                chatModels: store.chatModels(),
                isConfigured: store.isConfigured(),
                redactionFailed: store.redactionFailed()
            }))
        })),
        withMethods((store) => {
            const configService = inject(DotAiConfigService);
            const httpErrorManager = inject(DotHttpErrorManagerService);

            return {
                loadConfig: rxMethod<void>(
                    pipe(
                        // switchMap: a re-load supersedes an in-flight one.
                        switchMap(() =>
                            configService.getResolvedConfig().pipe(
                                tap((config: DotAiResolvedConfig) => {
                                    const threshold = Number(
                                        config.settings?.['embeddingsSearchThreshold']
                                    );

                                    patchState(store, {
                                        configLoaded: true,
                                        isConfigured: config.isConfigured,
                                        configHost: config.configHost,
                                        settings: config.settings,
                                        chatModels: config.chatModels,
                                        redactionFailed: config.redactionFailed,
                                        providerConfig: config.providerConfig,
                                        settingsThreshold: Number.isFinite(threshold)
                                            ? threshold
                                            : DOT_AI_DEFAULT_THRESHOLD,
                                        settingsModel: config.chatModels[0] ?? ''
                                    });
                                }),
                                catchError((error: HttpErrorResponse) => {
                                    httpErrorManager.handle(error);
                                    // Loaded, just unsuccessfully — otherwise a failed request
                                    // would suppress the banner forever.
                                    patchState(store, { configLoaded: true });

                                    return EMPTY;
                                })
                            )
                        )
                    )
                )
            };
        })
    );
}
