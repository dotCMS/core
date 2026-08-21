import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

import { DotAiCapability, DotAiProviderField, DotAiProviderFieldType } from '@dotcms/dotcms-models';

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
        title: 'apps.ai.capability.chat.title',
        description: 'apps.ai.capability.chat.description',
        icon: 'pi pi-comments'
    },
    {
        capability: DotAiCapability.EMBEDDINGS,
        sectionKey: 'embeddings',
        title: 'apps.ai.capability.embeddings.title',
        description: 'apps.ai.capability.embeddings.description',
        icon: 'pi pi-sitemap'
    },
    {
        capability: DotAiCapability.IMAGE,
        sectionKey: 'image',
        title: 'apps.ai.capability.image.title',
        description: 'apps.ai.capability.image.description',
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

/**
 * Field names that identify a provider/model, even when the backend marks them `optional`
 * because a fallback exists (e.g. Azure's `model`/`deploymentName` — either one satisfies the
 * requirement). Almost every user fills these in, so they're kept visible above the "Advanced"
 * panel instead of being buried alongside true tuning knobs like `temperature` or `timeout`.
 * Matched by field name only — not tied to any specific provider, so a future provider reusing
 * this naming pattern gets the same treatment automatically.
 */
const ALWAYS_VISIBLE_OPTIONAL_FIELD_NAMES = new Set(['model', 'deploymentName']);

/**
 * Whether a provider field should render above the "Advanced" panel regardless of its `required`
 * flag. Required fields always qualify; optional fields qualify when they're a credential
 * (`SECRET` type — e.g. AWS's `accessKeyId`/`secretAccessKey`, Vertex AI's `credentialsJson`,
 * both optional only because an AWS/ADC fallback exists) or an identity field (see
 * {@link ALWAYS_VISIBLE_OPTIONAL_FIELD_NAMES}). This is a type/name-based rule, not a per-provider
 * one, so it applies to future providers with no extra maintenance.
 */
export function isFieldAlwaysVisible(field: DotAiProviderField): boolean {
    return (
        field.required ||
        field.type === DotAiProviderFieldType.SECRET ||
        ALWAYS_VISIBLE_OPTIONAL_FIELD_NAMES.has(field.name)
    );
}

/**
 * Cross-field validator for a field declared `optionalUnless` by the backend (e.g. Azure's
 * `model`/`deploymentName` — either one satisfies the requirement). The control is invalid only
 * when BOTH it and the named sibling control are empty; filling either one clears the error on
 * both, since each field's own validator re-checks the other via {@link isEmptyValue}.
 *
 * Driven entirely by the `requiredUnless` field name from provider metadata — no per-provider
 * logic here, so a future provider with the same either-or pattern needs no frontend changes.
 */
export function requiredUnlessValidator(siblingFieldName: string): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
        if (!isEmptyValue(control.value)) {
            return null;
        }

        const sibling = control.parent?.get(siblingFieldName);

        return sibling && !isEmptyValue(sibling.value)
            ? null
            : { requiredUnless: { requires: siblingFieldName } };
    };
}

function isEmptyValue(value: unknown): boolean {
    return value === null || value === undefined || value === '';
}

/**
 * Additional-property values round-trip through a plain text input, but some preserved values
 * (e.g. Vertex AI's `credentialsJson`, or `listenerIndexer`) are objects/arrays in the stored
 * JSON. Parses back when the text looks like JSON, otherwise keeps the raw string.
 */
export function parseIfJson(value: string): unknown {
    const trimmed = value.trim();
    if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) {
        return value;
    }

    try {
        return JSON.parse(trimmed);
    } catch {
        return value;
    }
}

/** Serializes a hydrated value into an additional-property text control without corrupting
 *  non-string values (e.g. `String({a:1})` → `"[object Object]"`). Mirrors {@link parseIfJson}. */
export function stringifyForField(value: unknown): string {
    return typeof value === 'string' ? value : JSON.stringify(value);
}

/** Message keys for the lowercase, mid-sentence capability word (e.g. "no {0} support"). */
export const CAPABILITY_LABELS: Record<DotAiCapability, string> = {
    [DotAiCapability.CHAT]: 'apps.ai.capability.chat.label',
    [DotAiCapability.EMBEDDINGS]: 'apps.ai.capability.embeddings.label',
    [DotAiCapability.IMAGE]: 'apps.ai.capability.image.label'
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

/**
 * Always-visible shared settings — surfaced across every capability. Only `key` is consumed
 * (to exclude these from the additional-properties list); `label`/`hint` mirror the message keys
 * the fixed markup in `dot-ai-settings-card.component.html` renders directly for these fields.
 */
export const SETTINGS_COMMON_FIELDS: DotAiSettingsField[] = [
    {
        key: 'rolePrompt',
        label: 'apps.ai.settings.role-prompt.label',
        hint: 'apps.ai.settings.role-prompt.hint',
        type: 'textarea'
    },
    {
        key: 'textPrompt',
        label: 'apps.ai.settings.text-prompt.label',
        type: 'text'
    },
    {
        key: 'imagePrompt',
        label: 'apps.ai.settings.image-prompt.label',
        type: 'text'
    },
    {
        // Rendered by the fixed `<p-select>` in the template, not by this list — listed here only
        // so `knownKeys` recognizes it and excludes it from "Additional properties". Without this,
        // a saved `settings.imageSize` hydrates into both the dropdown AND a duplicate additional-
        // property row, and since additional properties are applied last on save, that stale row
        // silently overwrites whatever the user just picked in the dropdown.
        key: 'imageSize',
        label: 'apps.ai.settings.image-size.label',
        type: 'text'
    }
];

/** Advanced embeddings/indexing settings, mirroring `com.dotcms.ai.app.AppKeys`. */
export const SETTINGS_ADVANCED_FIELDS: DotAiSettingsField[] = [
    {
        key: 'embeddingsSplitAtTokens',
        label: 'apps.ai.settings.field.embeddingsSplitAtTokens.label',
        hint: 'apps.ai.settings.field.embeddingsSplitAtTokens.hint',
        type: 'number'
    },
    {
        key: 'embeddingsMinimumTextLength',
        label: 'apps.ai.settings.field.embeddingsMinimumTextLength.label',
        type: 'number'
    },
    {
        key: 'embeddingsMinimumFileSize',
        label: 'apps.ai.settings.field.embeddingsMinimumFileSize.label',
        type: 'number'
    },
    {
        key: 'embeddingsFileExtensions',
        label: 'apps.ai.settings.field.embeddingsFileExtensions.label',
        hint: 'apps.ai.settings.field.embeddingsFileExtensions.hint',
        type: 'text'
    },
    {
        key: 'embeddingsSearchThreshold',
        label: 'apps.ai.settings.field.embeddingsSearchThreshold.label',
        type: 'number'
    },
    {
        key: 'embeddingsThreads',
        label: 'apps.ai.settings.field.embeddingsThreads.label',
        type: 'number'
    },
    {
        key: 'embeddingsThreadsMax',
        label: 'apps.ai.settings.field.embeddingsThreadsMax.label',
        type: 'number'
    },
    {
        key: 'embeddingsThreadsQueue',
        label: 'apps.ai.settings.field.embeddingsThreadsQueue.label',
        type: 'number'
    },
    {
        key: 'embeddingsCacheTtlSeconds',
        label: 'apps.ai.settings.field.embeddingsCacheTtlSeconds.label',
        type: 'number'
    },
    {
        key: 'embeddingsCacheSize',
        label: 'apps.ai.settings.field.embeddingsCacheSize.label',
        type: 'number'
    },
    {
        key: 'embeddingsDeleteOldOnUpdate',
        label: 'apps.ai.settings.field.embeddingsDeleteOldOnUpdate.label',
        type: 'checkbox',
        defaultValue: true
    },
    {
        key: 'debugLogging',
        label: 'apps.ai.settings.field.debugLogging.label',
        type: 'checkbox',
        defaultValue: false
    }
];
