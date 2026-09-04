import { ENV_KEYS, readEnv } from './env';
import { MissingInputError } from './errors';
import { normalizeUrl, validateUrl } from './instance';

import type { RunOptions } from './types';

export interface ResolvedInputs {
    url: string;
    user?: string;
    password?: string;
    authToken?: string;
    /** True when a value had to be asked for. */
    prompted: boolean;
}

export interface PromptPort {
    text(message: string, defaultValue?: string): Promise<string>;
    /** MUST NOT echo (FR-003g). */
    password(message: string): Promise<string>;
    select<T extends string>(message: string, choices: { name: string; value: T }[]): Promise<T>;
    /** Multi-select. Detected editors arrive pre-checked (FR-010). */
    multiSelect<T extends string>(
        message: string,
        choices: { name: string; value: T; checked: boolean }[]
    ): Promise<T[]>;
}

const DEFAULT_URL = 'http://localhost:8082';

/** "Non-interactive" means no terminal to prompt on — not the presence of a flag (FR-003k). */
export function canPrompt(): boolean {
    return Boolean(process.stdin.isTTY);
}

/**
 * Resolve the only two required inputs: the instance address and ONE auth mode (FR-003i).
 *
 * Two rules here are easy to implement conventionally and wrongly:
 *
 * 1. Prompting is driven by a MISSING REQUIRED INPUT, not by a mode. Supply the url and one
 *    auth mode and nothing is asked, terminal or not — the run does not become "interactive"
 *    merely because a terminal exists (FR-003i).
 * 2. `--yes` governs CONFIRMATIONS ONLY. The usual reading of -y is "assume defaults for
 *    everything", which here would silently skip a required input. It must not shortcut this
 *    function at all (FR-003l).
 */
export async function resolveRequiredInputs(
    opts: Partial<RunOptions>,
    port?: PromptPort,
    // Being handed a port IS the permission to ask. The caller decides whether a terminal
    // exists (index.ts passes no port when `canPrompt()` is false), so reading that global
    // again here would second-guess it — and made the function untestable without a TTY.
    interactive = Boolean(port)
): Promise<ResolvedInputs> {
    let prompted = false;

    const ask = async <T>(what: string, run: () => Promise<T>): Promise<T> => {
        if (!interactive || !port) throw new MissingInputError(what);
        prompted = true;
        return run();
    };

    // --- instance address ---
    let url = opts.url ?? readEnv(ENV_KEYS.url);
    if (!url) {
        url = await ask('An instance address', () =>
            (port as PromptPort).text('dotCMS instance address', DEFAULT_URL)
        );
    }
    url = normalizeUrl(url);
    validateUrl(url);

    // --- authentication: exactly one mode ---
    const authToken = opts.authToken ?? readEnv(ENV_KEYS.authToken);
    const user = opts.user;
    const password = opts.password ?? readEnv(ENV_KEYS.password);

    if (authToken) return { url, authToken, prompted };
    if (user && password) return { url, user, password, prompted };

    if (user) {
        const typed = await ask('A password', () => (port as PromptPort).password('Password'));
        return { url, user, password: typed, prompted };
    }

    if (password) {
        const typed = await ask('A username', () => (port as PromptPort).text('Username'));
        return { url, user: typed, password, prompted };
    }

    // Neither mode supplied. Name BOTH when we cannot ask (FR-003h).
    if (!interactive || !port) {
        throw new MissingInputError('A username and password, or an authentication token,');
    }
    prompted = true;
    const mode = await port.select('How should we authenticate?', [
        { name: 'Sign in with a username and password', value: 'signin' as const },
        { name: 'Paste an existing authentication token', value: 'token' as const }
    ]);
    if (mode === 'token') {
        return { url, authToken: await port.password('Authentication token'), prompted };
    }
    return {
        url,
        user: await port.text('Username'),
        password: await port.password('Password'),
        prompted
    };
}
