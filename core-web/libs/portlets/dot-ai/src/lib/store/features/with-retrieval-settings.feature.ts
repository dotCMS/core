import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotAiRetrievalPayload } from '@dotcms/dotcms-models';

import {
    DOT_AI_MIN_RESPONSE_TOKENS,
    DOT_AI_TEMPERATURE_RANGE,
    DotAiPortletState
} from '../../models/dot-ai-portlet.models';

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max);

/**
 * The shared retrieval-settings panel, and the **single** assembler of a CompletionsForm body.
 *
 * Search and Chat both spread `retrievalPayload()` and add only their own `prompt`, which is
 * what makes FR-017 (settings survive tab navigation) structural rather than a thing to
 * remember. It also means the backend's three fussy rules are enforced in exactly one place.
 */
export function withRetrievalSettings() {
    return signalStoreFeature(
        type<{ state: DotAiPortletState }>(),
        withComputed((store) => ({
            /**
             * The retrieval criteria as the API wants them, minus `prompt`.
             *
             * Three rules the backend cares about, all handled here:
             * - an empty content-type selection **omits** the field; sending `''` would be
             *   parsed as one empty content type rather than "no restriction" (FR-020)
             * - `temperature` is clamped to 0..2 (FR-022)
             * - `responseLengthTokens` is raised to the declared 128 minimum, which the server
             *   documents but does not enforce (FR-023)
             */
            retrievalPayload: computed<DotAiRetrievalPayload>(() => {
                const contentTypes = store
                    .settingsContentTypes()
                    .split(',')
                    .map((type) => type.trim())
                    .filter(Boolean);

                const payload: DotAiRetrievalPayload = {
                    indexName: store.settingsIndexName(),
                    site: store.settingsSite() ?? '',
                    threshold: store.settingsThreshold(),
                    operator: store.settingsOperator(),
                    temperature: clamp(
                        store.settingsTemperature(),
                        DOT_AI_TEMPERATURE_RANGE.min,
                        DOT_AI_TEMPERATURE_RANGE.max
                    ),
                    responseLengthTokens: Math.max(
                        store.settingsResponseLength(),
                        DOT_AI_MIN_RESPONSE_TOKENS
                    )
                };

                const model = store.settingsModel();

                if (model) {
                    payload.model = model;
                }

                if (contentTypes.length) {
                    payload.contentType = contentTypes;
                }

                return payload;
            })
        })),
        withMethods((store) => ({
            setSettings(settings: Partial<DotAiPortletState>): void {
                patchState(store, settings);
            },
            setSearchPrompt(searchPrompt: string): void {
                patchState(store, { searchPrompt });
            }
        }))
    );
}
