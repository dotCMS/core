import {
    ComponentStatus,
    DotAiChatAnswer,
    DotAIImageOrientation,
    DOT_AI_VECTOR_OPERATOR,
    DotAiIndex,
    DotAiIndexStatus,
    DotAiSearchResponse,
    DotAiVectorOperator
} from '@dotcms/dotcms-models';

/** A generated image, plus whether it has been deliberately published. */
export interface DotAiGeneratedImage {
    response: string;
    tempFileName: string;
    originalPrompt: string;
    revisedPrompt: string;
    published: boolean;
}

/**
 * View-model-only types for the dotAI portlet. Wire shapes and their conversions live in
 * `@dotcms/data-access`; nothing here crosses the network.
 */

/**
 * The five tabs, in display order. The `id` is also the route segment, so this array is the
 * single source for both the tab bar and `lib.routes.ts` — they cannot drift apart.
 */
export const DOT_AI_TABS = [
    { id: 'search', labelKey: 'dotai.tab.search' },
    { id: 'chat', labelKey: 'dotai.tab.chat' },
    { id: 'image', labelKey: 'dotai.tab.image' },
    { id: 'embeddings', labelKey: 'dotai.tab.embeddings' },
    { id: 'config', labelKey: 'dotai.tab.config' }
] as const;

export type DotAiTab = (typeof DOT_AI_TABS)[number];
export type DotAiTabId = DotAiTab['id'];

/**
 * The outcome of the last index build, surfaced in the tab.
 *
 * `empty` is its own case on purpose: the server answers 200 with `totalToEmbed: 0` when the
 * query matches nothing, and an index with no rows does not come back from `indexCount` at
 * all — so without this the build looks like it silently did nothing.
 */
export interface DotAiIndexBuildNotice {
    kind: 'built' | 'empty' | 'failed';
    indexName: string;
    /** Rows embedded, for `built`; the server's reason, for `failed`. */
    detail?: string;
}

/* ------------------------------------------------------------------------------------------- */

/**
 * Default maximum match distance.
 *
 * Deliberately looser than the backend's own CompletionsForm default of .25f: at 0.25 a
 * reasonable question routinely retrieves nothing and the answer is "no matching content found
 * in the index for your query". The payload always carries this value, so the server default
 * never applies and the divergence is intentional rather than accidental drift.
 */
export const DOT_AI_DEFAULT_THRESHOLD = 0.75;

/** The backend declares @Min(128) but does not enforce it — the client is the only guard. */
export const DOT_AI_MIN_RESPONSE_TOKENS = 128;
export const DOT_AI_DEFAULT_RESPONSE_TOKENS = 1024;

export const DOT_AI_TEMPERATURE_RANGE = { min: 0, max: 2 } as const;

/**
 * Flat, prefixed portlet state — the `edit-ema` convention. Every slice owns a prefix
 * (`settings*`, `search*`, `index*`) so a reader can tell at a glance which feature owns a
 * field, without the state being nested.
 */
export interface DotAiPortletState {
    // config
    isConfigured: boolean;
    /**
     * Whether the config request has come back at all — distinct from `isConfigured`.
     * Before this is true the answer is *unknown*, not *no*, and treating the two the same
     * is what made the not-configured banner flash on every load.
     */
    configLoaded: boolean;
    configHost: string;
    settings: Record<string, string>;
    chatModels: string[];
    redactionFailed: boolean;
    providerConfig: Record<string, unknown> | null;

    // indexes
    indexes: DotAiIndex[];
    indexStatuses: Record<string, DotAiIndexStatus>;
    indexFragmentSnapshot: Record<string, number>;
    indexBuildSeeds: string[];
    indexesForbidden: boolean;
    indexesStatus: ComponentStatus;
    settingsIndexSeeded: boolean;

    // shared retrieval settings
    settingsIndexName: string;
    settingsSite: string | null;
    settingsContentTypes: string;
    settingsThreshold: number;
    settingsOperator: DotAiVectorOperator;
    settingsModel: string;
    settingsTemperature: number;
    settingsResponseLength: number;

    // search
    searchPrompt: string;
    searchResponse: DotAiSearchResponse | null;
    searchStatus: ComponentStatus;
    searchMissingIndex: string | null;
    hasSearched: boolean;

    // chat
    /** The one answer on screen. Replaced per submit — there is no transcript. */
    chatAnswer: DotAiChatAnswer | null;
    chatStreaming: boolean;

    // embeddings screen (client-side filters — the whole dataset arrives in one response)
    indexFilter: string;
    indexBuildNotice: DotAiIndexBuildNotice | null;

    // image
    image: DotAiGeneratedImage | null;
    imageGenerating: boolean;
    imageSaving: boolean;
    imageOrientation: string;
}

export const DOT_AI_INITIAL_STATE: DotAiPortletState = {
    isConfigured: false,
    configLoaded: false,
    configHost: '',
    settings: {},
    chatModels: [],
    redactionFailed: false,
    providerConfig: null,

    indexes: [],
    indexStatuses: {},
    indexFragmentSnapshot: {},
    indexBuildSeeds: [],
    indexesForbidden: false,
    indexesStatus: ComponentStatus.INIT,
    settingsIndexSeeded: false,

    settingsIndexName: 'default',
    settingsSite: null,
    settingsContentTypes: '',
    settingsThreshold: DOT_AI_DEFAULT_THRESHOLD,
    settingsOperator: DOT_AI_VECTOR_OPERATOR.COSINE,
    settingsModel: '',
    settingsTemperature: 0,
    settingsResponseLength: DOT_AI_DEFAULT_RESPONSE_TOKENS,

    searchPrompt: '',
    searchResponse: null,
    searchStatus: ComponentStatus.INIT,
    searchMissingIndex: null,
    hasSearched: false,

    chatAnswer: null,
    chatStreaming: false,

    indexFilter: '',
    indexBuildNotice: null,

    image: null,
    imageGenerating: false,
    imageSaving: false,
    // 16:9 rather than the shared DEFAULT_IMAGE_SIZE (1024x1024): landscape is the useful
    // default for page and blog imagery. Set here rather than on the shared constant, which
    // the block editor's image prompt in libs/ui also reads.
    imageOrientation: DotAIImageOrientation.HORIZONTAL
};
