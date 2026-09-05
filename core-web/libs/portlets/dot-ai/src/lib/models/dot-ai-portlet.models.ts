import {
    ComponentStatus,
    DOT_AI_VECTOR_OPERATOR,
    DotAiIndex,
    DotAiIndexStatus,
    DotAiSearchResponse,
    DotAiVectorOperator
} from '@dotcms/dotcms-models';

/**
 * View-model-only types for the dotAI portlet. Wire shapes and their conversions live in
 * `@dotcms/data-access`; nothing here crosses the network.
 */

/**
 * The five tabs, in display order. The `id` is also the route segment, so this array is the
 * single source for both the tab bar and `lib.routes.ts` — they cannot drift apart.
 */
export const DOT_AI_TABS = [
    { id: 'search', labelKey: 'dotai.tab.search', icon: 'search' },
    { id: 'chat', labelKey: 'dotai.tab.chat', icon: 'forum' },
    { id: 'image', labelKey: 'dotai.tab.image', icon: 'imagesmode' },
    { id: 'embeddings', labelKey: 'dotai.tab.embeddings', icon: 'database' },
    { id: 'config', labelKey: 'dotai.tab.config', icon: 'tune' }
] as const;

export type DotAiTab = (typeof DOT_AI_TABS)[number];
export type DotAiTabId = DotAiTab['id'];

/* ------------------------------------------------------------------------------------------- */

/** Default closeness threshold, matching the backend's own CompletionsForm default. */
export const DOT_AI_DEFAULT_THRESHOLD = 0.25;

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
    configHost: string;
    settings: Record<string, string>;
    chatModels: string[];
    redactionFailed: boolean;

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
}

export const DOT_AI_INITIAL_STATE: DotAiPortletState = {
    isConfigured: false,
    configHost: '',
    settings: {},
    chatModels: [],
    redactionFailed: false,

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
    hasSearched: false
};
