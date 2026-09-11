import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotAiConfigService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotAiResolvedConfig } from '@dotcms/dotcms-models';

import { DotAiPortletState } from '../../models/dot-ai-portlet.models';

/**
 * dotAI configuration.
 *
 * Composed **first**, because it seeds the default chat model the retrieval panel needs (the
 * first entry of the provider's comma-separated fallback list).
 *
 * It deliberately does **not** seed the closeness threshold from the resolved
 * `embeddingsSearchThreshold` setting. That setting is an app-level default of `.25`, tight
 * enough that ordinary questions retrieve nothing, and seeding it here made the portlet's own
 * default unreachable. Worse, it ran on every load and so overwrote whatever the user had
 * chosen and persisted, quietly breaking FR-018 for that control. The panel's threshold now
 * comes from `DOT_AI_DEFAULT_THRESHOLD` or the stored preference, and nothing else.
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
             *
             * Also gated on the load having succeeded. A failed request leaves `isConfigured`
             * false too, so a transient 500 told the user to go and configure something that
             * is already configured; `configUnavailable` says what actually happened.
             */
            showNotConfigured: computed(
                () => store.configLoaded() && !store.configLoadFailed() && !store.isConfigured()
            ),

            /** The config could not be read at all — retryable, unlike "no provider". */
            configUnavailable: computed(() => store.configLoadFailed()),

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
                /**
                 * Loads the config for a site.
                 *
                 * Takes the site rather than reading it, so this slice stays free of
                 * `GlobalStore` and a test can drive a switch by calling it twice. `null` —
                 * which `currentSiteId` reports until the site resolves — means "whatever the
                 * session is on", the endpoint's own default.
                 */
                loadConfig: rxMethod<string | null>(
                    pipe(
                        // switchMap: a re-load supersedes an in-flight one, which is what
                        // makes a rapid site switch land on the last site rather than
                        // whichever response happens back last.
                        switchMap((siteId) =>
                            configService.getResolvedConfig(siteId ?? undefined).pipe(
                                tap((config: DotAiResolvedConfig) => {
                                    // Keep the selected model when the provider still offers
                                    // it, and only fall back when it has gone away (FR-018).
                                    // Unconditionally taking chatModels[0] overwrote a stored
                                    // choice on every load.
                                    const selected = store.settingsModel();
                                    const model = config.chatModels.includes(selected)
                                        ? selected
                                        : (config.chatModels[0] ?? '');

                                    patchState(store, {
                                        configLoaded: true,
                                        configLoadFailed: false,
                                        isConfigured: config.isConfigured,
                                        configHost: config.configHost,
                                        settings: config.settings,
                                        chatModels: config.chatModels,
                                        redactionFailed: config.redactionFailed,
                                        providerConfig: config.providerConfig,
                                        settingsModel: model
                                    });
                                }),
                                catchError((error: HttpErrorResponse) => {
                                    httpErrorManager.handle(error);
                                    // Loaded, just unsuccessfully — otherwise a failed request
                                    // would suppress the banner forever (FR-051). The failure
                                    // is recorded so it does not masquerade as "no provider".
                                    patchState(store, {
                                        configLoaded: true,
                                        configLoadFailed: true
                                    });

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
