import { DotAiResolvedConfig } from '@dotcms/dotcms-models';

/** Credential fields the backend redacts. Mirrors ProviderConfigMerger.CREDENTIAL_FIELDS. */
export const AI_CREDENTIAL_FIELDS = ['apiKey', 'secretAccessKey', 'accessKeyId', 'credentialsJson'];

/** What the client renders for a secret. Never the server's own mask, never the value. */
export const SECRET_MASK = '••••••••';

export const DOT_AI_CONFIG_SOURCE = {
    APP_CONFIG: 'App Config',
    DEFAULT: 'Default',
    SECRET: 'Secret'
} as const;

export type DotAiConfigSource = (typeof DOT_AI_CONFIG_SOURCE)[keyof typeof DOT_AI_CONFIG_SOURCE];

export interface DotAiConfigValueRow {
    key: string;
    value: string;
    source: DotAiConfigSource;
}

/**
 * Flattens the resolved config into table rows, deriving each value's origin the same way the
 * backend resolves it: an explicitly-set value wins, otherwise the built-in default.
 *
 * Secrets are handled separately and deliberately. The credential AppKeys carry a null
 * settingsKey, so they never appear in `settings` at all — the Secret rows come from
 * `providerConfig`, whose credential fields the server has already rewritten to "*****". The
 * client therefore never holds a real credential, which is what makes the masking structural
 * rather than a rendering promise.
 */
export function toConfigRows(config: DotAiResolvedConfig | null): DotAiConfigValueRow[] {
    if (!config) {
        return [];
    }

    const providerConfig = (config.providerConfig ?? {}) as Record<string, unknown>;

    const settingRows: DotAiConfigValueRow[] = Object.entries(config.settings ?? {}).map(
        ([key, value]) => ({
            key,
            value: value ?? '',
            source: isExplicitlySet(providerConfig, key)
                ? DOT_AI_CONFIG_SOURCE.APP_CONFIG
                : DOT_AI_CONFIG_SOURCE.DEFAULT
        })
    );

    return [...settingRows, ...toSecretRows(providerConfig)].sort((a, b) =>
        a.key.localeCompare(b.key)
    );
}

/** A value counts as App Config when the provider config sets it to something non-blank. */
function isExplicitlySet(providerConfig: Record<string, unknown>, key: string): boolean {
    return Object.values(providerConfig).some((section) => {
        if (!section || typeof section !== 'object') {
            return false;
        }

        const value = (section as Record<string, unknown>)[key];

        return value !== undefined && value !== null && String(value).trim() !== '';
    });
}

/** One masked row per credential field present, qualified by the section that holds it. */
function toSecretRows(providerConfig: Record<string, unknown>): DotAiConfigValueRow[] {
    return Object.entries(providerConfig).flatMap(([sectionName, section]) => {
        if (!section || typeof section !== 'object') {
            return [];
        }

        return AI_CREDENTIAL_FIELDS.filter((field) => field in (section as object)).map(
            (field) => ({
                key: `${sectionName}.${field}`,
                value: SECRET_MASK,
                source: DOT_AI_CONFIG_SOURCE.SECRET
            })
        );
    });
}
