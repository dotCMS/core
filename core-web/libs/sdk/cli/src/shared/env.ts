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

/**
 * `process.env` with OUR secrets removed.
 *
 * `spawn`/`spawnSync` default to the full environment, so `npx skills add …` — and every
 * `postinstall` npm runs underneath it — received `DOTCMS_AUTH_TOKEN` and `DOTCMS_PASSWORD`
 * whenever the developer supplied them by environment, which this CLI's own `--password` help
 * text recommends. A child that does not need a credential should not be able to read one.
 */
export function envWithoutSecrets(extra: Record<string, string> = {}): NodeJS.ProcessEnv {
    const clean = { ...process.env };
    delete clean[ENV_KEYS.authToken];
    delete clean[ENV_KEYS.password];
    return { ...clean, ...extra };
}
