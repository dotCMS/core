import { signalStore, withHooks, withState } from '@ngrx/signals';

import { withPersistedQuery } from '@dotcms/data-access';

import { withAiChat } from './features/with-ai-chat.feature';
import { withAiConfig } from './features/with-ai-config.feature';
import { withAiEmbeddings } from './features/with-ai-embeddings.feature';
import { withAiImage } from './features/with-ai-image.feature';
import { withAiIndexes } from './features/with-ai-indexes.feature';
import { withAiSearch } from './features/with-ai-search.feature';
import { withRetrievalSettings } from './features/with-retrieval-settings.feature';

import { DOT_AI_INITIAL_STATE, DotAiPortletState } from '../models/dot-ai-portlet.models';

/**
 * One store for the whole portlet, provided on the shell rather than per tab.
 *
 * Two reasons, both structural rather than stylistic:
 *
 * 1. The retrieval-settings panel is shared by Search and Chat, so it needs a provider
 *    *above* both — per-tab stores would reset it on every tab switch (FR-017).
 * 2. The index list has two readers (the Embeddings table and the retrieval picker). One
 *    owner means one fetch and one signal, so a mutation in one place updates the other for
 *    free (FR-033). Split stores would mean a duplicated fetch or a third shared store.
 *
 * **Slice order is load-bearing**: `withAiConfig` seeds the threshold and default model, and
 * `withAiIndexes` seeds the index name, both of which `withRetrievalSettings` then reads.
 */
export const DotAiStore = signalStore(
    withState<DotAiPortletState>(DOT_AI_INITIAL_STATE),
    withAiConfig(),
    withAiIndexes(),
    withRetrievalSettings(),
    withAiSearch(),
    withAiChat(),
    withAiEmbeddings(),
    withAiImage(),
    // Gives `dotcms.devtools.dotai.lastQuery`, matching the three sibling dev-tool portlets.
    // It can only be composed once — it contributes a `clearPersistedQuery()` method that a
    // second instance would collide on.
    withPersistedQuery({ portletKey: 'dotai', field: 'searchPrompt' }),
    withHooks({
        onInit(store) {
            store.loadConfig();
            store.loadIndexes();
        }
    })
);
