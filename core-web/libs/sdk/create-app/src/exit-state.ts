import fs from 'node:fs';
import path from 'node:path';

/**
 * Guarantees that a run never throws away state it already obtained.
 *
 * The failure reported in #37262 is a contract violation more than a bug: the CLI held a
 * working API token and site ID, hit a non-essential configuration error, called
 * `process.exit(1)`, and left the user with an empty directory — the token and site ID never
 * printed, never written anywhere. Everything up to that point had *succeeded*.
 *
 * `displayFinalSteps()` is the only code that prints those values, and it sits downstream of
 * all 17 `process.exit` call sites (13 of them inside the single `try` opened at
 * `src/index.ts:93`). A `finally` cannot close that gap: `finally` does not run on
 * `process.exit()`. A `process.on('exit')` handler does — and it also covers exit paths nobody
 * has written yet, which is the durable part of the decision (D1).
 *
 * Everything here is synchronous. Node runs no async work during `'exit'`, so an async write
 * is indistinguishable from no write at all.
 */

export interface RecoverableState {
    /** Instance base URL — e.g. `http://localhost:8082`. */
    host: string;
    /** The issued API token: the thing that must never be lost. */
    token: string;
    siteId: string;
    /** Absolute directory that receives `.env`. */
    projectDirectory: string;
    /** Chooses the variable names in the printed block. */
    framework?: string;
}

let recorded: Partial<RecoverableState> = {};
let handler: (() => void) | null = null;

/**
 * Records what the run knows so far. Callers merge in values as they arrive — the project
 * directory is settled long before a token exists — so this deliberately takes a `Partial`
 * rather than demanding the whole shape up front.
 */
export function recordRecoverableState(state: Partial<RecoverableState>): void {
    recorded = { ...recorded, ...state };
}

/** True once there is genuinely something worth recovering. */
function hasRecoverableState(
    state: Partial<RecoverableState>
): state is RecoverableState & { projectDirectory: string } {
    return Boolean(state.host && state.token && state.siteId);
}

function envContents({ host, token, siteId }: RecoverableState): string {
    return [
        '# Written by @dotcms/create-app so this run is never lost.',
        `NEXT_PUBLIC_DOTCMS_HOST=${host}`,
        `NEXT_PUBLIC_DOTCMS_SITE_ID=${siteId}`,
        `DOTCMS_AUTH_TOKEN=${token}`,
        ''
    ].join('\n');
}

function emit(state: RecoverableState): void {
    const lines: string[] = [
        '',
        'dotCMS connection details for this run:',
        `  host    : ${state.host}`,
        `  site id : ${state.siteId}`,
        `  token   : ${state.token}`
    ];

    const directory = state.projectDirectory;
    let wroteEnv = false;

    if (directory) {
        const envPath = path.join(directory, '.env');

        // Write-if-absent (D6). An existing .env is the user's — or the scaffolded example's —
        // and silently overwriting it would trade one kind of data loss for another.
        if (!fs.existsSync(envPath)) {
            try {
                fs.writeFileSync(envPath, envContents(state), 'utf8');
                wroteEnv = true;
            } catch {
                // Never let recovery reporting be the thing that fails the run. Falling through
                // prints the paste block, which still gets the values to the user.
            }
        }
    }

    if (wroteEnv) {
        lines.push('', 'Written to .env in your project directory.');
    } else {
        lines.push('', 'Add these to your .env:', ...envContents(state).trimEnd().split('\n'));
    }

    console.log(lines.join('\n'));
}

/**
 * Registers the single `'exit'` handler.
 *
 * Deliberately `'exit'` and never `'beforeExit'`: `'beforeExit'` is skipped on
 * `process.exit()`, which is precisely the path that loses the token today.
 *
 * Idempotent — repeated calls must not stack listeners, or the recovery block prints twice.
 */
export function installExitStateHandler(): void {
    if (handler) {
        return;
    }

    handler = () => {
        if (!hasRecoverableState(recorded)) {
            return;
        }

        emit(recorded as RecoverableState);
    };

    process.on('exit', handler);
}

/** Test seam: unregister the handler and clear state so specs cannot leak into each other. */
export function resetExitState(): void {
    if (handler) {
        process.off('exit', handler);
        handler = null;
    }

    recorded = {};
}
