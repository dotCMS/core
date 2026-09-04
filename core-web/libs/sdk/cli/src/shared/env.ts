/**
 * The single place environment variables are read.
 *
 * Confining this to one module is what keeps configuration reading from scattering through the
 * codebase — the intent behind Constitution Principle II, which has no literal Node equivalent.
 */
export const ENV_KEYS = {
    url: 'DOTCMS_URL',
    password: 'DOTCMS_PASSWORD',
    authToken: 'DOTCMS_AUTH_TOKEN',
    codexHome: 'CODEX_HOME'
} as const;

export function readEnv(key: (typeof ENV_KEYS)[keyof typeof ENV_KEYS]): string | undefined {
    const value = process.env[key];
    return value && value.trim() !== '' ? value : undefined;
}
