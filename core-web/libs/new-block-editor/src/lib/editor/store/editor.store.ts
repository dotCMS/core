import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, switchMap, tap } from 'rxjs';

import { computed, inject } from '@angular/core';

import { DotAiService, DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';

interface EditorState {
    /** Active language ID used for dotCMS API queries. */
    languageId: number;
    /**
     * Block types the editor is restricted to.
     * An empty array means all blocks are allowed.
     */
    allowedBlocks: string[];
    /**
     * Comma-separated allowlist of content type variables (e.g. `"Blog,News"`) sourced
     * from the legacy `contentTypes` field variable. Empty string ⇒ no restriction.
     * Forwarded to {@link DotContentTypeService.filterContentTypes} when the slash menu opens
     * the content-type sub-menu.
     */
    allowedContentTypes: string;
    /** Full language object fetched from the dotCMS language API. Null until loaded. */
    language: DotLanguage | null;
    /**
     * True when the dotCMS AI plugin is installed and configured. Null while the check is in flight.
     * Slash menu / toolbar use this to gate AI features.
     */
    aiInstalled: boolean | null;
}

const initialState: EditorState = {
    languageId: 1,
    allowedBlocks: [],
    allowedContentTypes: '',
    language: null,
    aiInstalled: null
};

export const EditorStore = signalStore(
    withState(initialState),

    withComputed(({ allowedBlocks, language }) => ({
        /** Set of allowed block names for O(1) lookups. Null when all blocks are allowed. */
        allowedBlocksSet: computed(() => {
            const blocks = allowedBlocks();
            return blocks.length > 0 ? new Set(blocks) : null;
        }),

        /**
         * ISO locale string derived from the loaded language, e.g. `en-us`.
         * Null while the language is still loading.
         */
        languageIso: computed(() => {
            const lang = language();
            if (!lang) return null;
            const code = lang.languageCode?.toLowerCase() ?? '';
            const country = lang.countryCode?.toLowerCase() ?? '';
            return country ? `${code}-${country}` : code;
        })
    })),

    withMethods(
        (
            store,
            languageService = inject(DotLanguagesService),
            aiService = inject(DotAiService)
        ) => ({
            setLanguageId(languageId: number): void {
                patchState(store, { languageId });
            },

            setAllowedBlocks(allowedBlocks: string[]): void {
                patchState(store, { allowedBlocks });
            },

            /**
             * Stores the comma-separated content-type allowlist after stripping incidental
             * whitespace ("Blog , News" → "Blog,News") so consumers can forward the value
             * to the API verbatim.
             */
            setAllowedContentTypes(allowedContentTypes: string): void {
                const normalized = allowedContentTypes
                    .split(',')
                    .map((s) => s.trim())
                    .filter(Boolean)
                    .join(',');
                patchState(store, { allowedContentTypes: normalized });
            },

            /**
             * Returns true when the given block type is allowed.
             * Always true when `allowedBlocks` is empty (all blocks permitted).
             */
            isAllowed(block: string): boolean {
                const set = store.allowedBlocksSet();
                return !set || set.has(block);
            },

            /**
             * Fetches language data for the given ID and updates `language` + `languageLoading`.
             * Accepts a plain number, a Signal<number>, or an Observable<number> —
             * passing the store's own signal makes this auto-reactive to `languageId` changes.
             */
            loadLanguage: rxMethod<number>(
                pipe(
                    switchMap((id) => languageService.getById(id)),
                    tap((language: DotLanguage) => patchState(store, { language }))
                )
            ),

            /** One-shot check at editor init. Result drives slash-menu visibility for AI entries. */
            loadAiInstalled: rxMethod<void>(
                pipe(
                    switchMap(() => aiService.checkPluginInstallation()),
                    tap((aiInstalled: boolean) => patchState(store, { aiInstalled }))
                )
            )
        })
    ),

    withHooks({
        onInit(store) {
            store.loadLanguage(store.languageId);
            store.loadAiInstalled();
        }
    })
);

export type EditorStore = InstanceType<typeof EditorStore>;
