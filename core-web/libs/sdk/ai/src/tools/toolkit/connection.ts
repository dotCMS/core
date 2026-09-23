import { ConfigurationError, errorMessage } from './tool-runtime';

import type { RequestCallEvent } from '../../runtime';

/**
 * A value given as-is, or a resolver read on every tool call — for tokens that rotate, come
 * from a secrets manager, or are read lazily from a host's own configuration. A resolver may
 * return `undefined` (e.g. an unset environment variable); the call then fails with a readable
 * `CONFIGURATION` failure instead of the host crashing.
 */
export type Resolvable<T> = T | (() => T | undefined | Promise<T | undefined>);

/** Who is calling which dotCMS instance — everything a tool needs that is not about the tool. */
export interface DotCMSConnectionConfig {
    /** dotCMS instance URL. */
    url: Resolvable<string>;
    /** dotCMS API token. Injected host-side — it never reaches the model or the sandbox. */
    token: Resolvable<string>;
    /** Observability hook fired around each request (token and sensitive bodies are never passed). */
    onCall?: (event: RequestCallEvent) => void;
    /** Called when loading instance context (sites, content types, …) fails. */
    onContextError?: (label: string, error: unknown) => void;
}

/**
 * A dotCMS connection: created once, handed to every tool. Holds no state and makes no request
 * — so a host serving many users creates one per session, each with that user's token.
 */
export interface DotCMSConnection {
    readonly __dotcmsConnection: true;
    readonly config: Readonly<DotCMSConnectionConfig>;
}

/**
 * Describe the dotCMS instance and identity the tools act as.
 *
 * ```ts
 * const dotcms = dotcmsConnection({ url, token: () => secrets.get('dotcms') });
 * const tools = [searchTool(dotcms), pageVerifyTool(dotcms)];
 * ```
 *
 * Nothing is read, validated or fetched here — the values are resolved on each tool call.
 */
export function dotcmsConnection(config: DotCMSConnectionConfig): DotCMSConnection {
    return Object.freeze({
        __dotcmsConnection: true as const,
        config: Object.freeze({ ...config })
    });
}

/** Whether `value` came from {@link dotcmsConnection}. */
export function isDotCMSConnection(value: unknown): value is DotCMSConnection {
    return (
        typeof value === 'object' &&
        value !== null &&
        (value as { __dotcmsConnection?: unknown }).__dotcmsConnection === true
    );
}

async function resolveValue(label: string, value: Resolvable<string>): Promise<string> {
    if (typeof value !== 'function') {
        return value;
    }

    try {
        return (await value()) ?? '';
    } catch (error) {
        // The host's resolver failed (a secrets manager down, say). That is the host's problem,
        // not the call's — reported as CONFIGURATION, with the resolver's own reason.
        throw new ConfigurationError(
            `The dotCMS connection could not resolve its ${label}: ${errorMessage(error)}. This ` +
                `is not a problem with the call and no argument can fix it — report it and stop; ` +
                `do not look for credentials.`
        );
    }
}

/**
 * The URL and token for one call. Throws `ConfigurationError` when the tool was given no
 * connection at all (a JavaScript caller, or a cast) or a resolver failed; an EMPTY value is
 * left to `createToolRuntime`, which reports it the same way.
 */
export async function resolveConnection(
    connection: unknown
): Promise<{ url: string; token: string; config: DotCMSConnectionConfig }> {
    if (!isDotCMSConnection(connection)) {
        throw new ConfigurationError(
            'The dotCMS tools are not configured: the tool was created without a dotCMS ' +
                'connection. Whoever builds the tools passes one — searchTool(dotcmsConnection({ ' +
                'url, token })). This is not a problem with the call and no argument can fix it ' +
                '— report it and stop; do not look for credentials.'
        );
    }

    const [url, token] = await Promise.all([
        resolveValue('url', connection.config.url),
        resolveValue('token', connection.config.token)
    ]);

    return { url, token, config: connection.config };
}
