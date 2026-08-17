import { DotAiCapability } from '@dotcms/dotcms-models';

/**
 * The JSON key each capability occupies inside the `providerConfig` payload
 * (`{ chat, embeddings, image, settings }`), as read by the backend's `AppConfig`.
 */
export type DotAiCapabilitySectionKey = 'chat' | 'embeddings' | 'image';

/**
 * Sentinel value the backend substitutes for a credential field (`apiKey`, `secretAccessKey`,
 * `accessKeyId`, `credentialsJson`) that's already saved — the real secret never reaches the
 * browser. Mirrors `com.dotcms.ai.app.ProviderConfigMerger.MASKED`.
 */
export const MASKED_SECRET_VALUE = '*****';

export const CAPABILITY_SECTION_KEYS: Record<DotAiCapability, DotAiCapabilitySectionKey> = {
    [DotAiCapability.CHAT]: 'chat',
    [DotAiCapability.EMBEDDINGS]: 'embeddings',
    [DotAiCapability.IMAGE]: 'image'
};

export interface DotAiCapabilityMeta {
    capability: DotAiCapability;
    sectionKey: DotAiCapabilitySectionKey;
    title: string;
    description: string;
    icon: string;
}

export const CAPABILITY_META: DotAiCapabilityMeta[] = [
    {
        capability: DotAiCapability.CHAT,
        sectionKey: 'chat',
        title: 'Chat',
        description: 'Text generation for AI Blocks, workflows and the $ai viewtool.',
        icon: 'pi pi-comments'
    },
    {
        capability: DotAiCapability.EMBEDDINGS,
        sectionKey: 'embeddings',
        title: 'Embeddings',
        description: 'Vector indexing for semantic search over your content.',
        icon: 'pi pi-sitemap'
    },
    {
        capability: DotAiCapability.IMAGE,
        sectionKey: 'image',
        title: 'Image Generation',
        description: 'Generated imagery for content items and Block Editor.',
        icon: 'pi pi-image'
    }
];

/** Presentation-only display names — the provider list itself always comes from the backend. */
export const PROVIDER_DISPLAY_NAMES: Record<string, string> = {
    openai: 'OpenAI',
    azure_openai: 'Azure OpenAI',
    google_ai: 'Google AI',
    bedrock: 'Amazon Bedrock',
    vertex_ai: 'Vertex AI',
    anthropic: 'Anthropic',
    openrouter: 'OpenRouter'
};

/** Fixed visual ordering for the currently-known providers; unknown providers sort last. */
export const PROVIDER_ORDER = [
    'openai',
    'azure_openai',
    'google_ai',
    'bedrock',
    'vertex_ai',
    'anthropic',
    'openrouter'
];

export const CAPABILITY_LABELS: Record<DotAiCapability, string> = {
    [DotAiCapability.CHAT]: 'chat',
    [DotAiCapability.EMBEDDINGS]: 'embeddings',
    [DotAiCapability.IMAGE]: 'images'
};

export const IMAGE_SIZE_OPTIONS = [
    { label: '256x256', value: '256x256' },
    { label: '512x512', value: '512x512' },
    { label: '1024x1024', value: '1024x1024' },
    { label: '1024x1792', value: '1024x1792' },
    { label: '1792x1024', value: '1792x1024' }
];

export type SettingsFieldType = 'text' | 'textarea' | 'number' | 'checkbox';

export interface DotAiSettingsField {
    key: string;
    label: string;
    hint?: string;
    type: SettingsFieldType;
    /** Backend default from `com.dotcms.ai.app.AppKeys`, used to seed checkbox controls so an
     *  untouched checkbox doesn't silently save a wrong value when the key was never set. */
    defaultValue?: boolean;
}

/** Always-visible shared settings — surfaced across every capability. */
export const SETTINGS_COMMON_FIELDS: DotAiSettingsField[] = [
    {
        key: 'rolePrompt',
        label: 'Role prompt',
        hint: 'Describes the role the AI plays for content authors.',
        type: 'textarea'
    },
    {
        key: 'textPrompt',
        label: 'Text prompt',
        hint: 'Use Descriptive writing style.',
        type: 'text'
    },
    { key: 'imagePrompt', label: 'Image prompt', hint: 'Use 16:9 aspect ratio.', type: 'text' }
];

/** Advanced embeddings/indexing settings, mirroring `com.dotcms.ai.app.AppKeys`. */
export const SETTINGS_ADVANCED_FIELDS: DotAiSettingsField[] = [
    {
        key: 'embeddingsSplitAtTokens',
        label: 'Split into (tokens)',
        hint: 'Token count used to chunk content before indexing.',
        type: 'number'
    },
    {
        key: 'embeddingsMinimumTextLength',
        label: 'Minimum text length to index',
        type: 'number'
    },
    {
        key: 'embeddingsMinimumFileSize',
        label: 'Minimum file size (bytes)',
        type: 'number'
    },
    {
        key: 'embeddingsFileExtensions',
        label: 'File extensions',
        hint: 'Comma-separated, e.g. pdf,doc,docx,txt,html',
        type: 'text'
    },
    {
        key: 'embeddingsSearchThreshold',
        label: 'Search threshold',
        type: 'number'
    },
    { key: 'embeddingsThreads', label: 'Threads', type: 'number' },
    { key: 'embeddingsThreadsMax', label: 'Max threads', type: 'number' },
    { key: 'embeddingsThreadsQueue', label: 'Thread queue size', type: 'number' },
    { key: 'embeddingsCacheTtlSeconds', label: 'Cache TTL (s)', type: 'number' },
    { key: 'embeddingsCacheSize', label: 'Cache size', type: 'number' },
    {
        key: 'embeddingsDeleteOldOnUpdate',
        label: 'Delete old embeddings on content update',
        type: 'checkbox',
        defaultValue: true
    },
    {
        key: 'debugLogging',
        label: 'Enable verbose debug logging',
        type: 'checkbox',
        defaultValue: false
    }
];
