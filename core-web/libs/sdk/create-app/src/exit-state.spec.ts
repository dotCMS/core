/**
 * Contract spec for `src/exit-state.ts` (task T022/T028/T029, dotCMS #37262).
 *
 * Written BEFORE the implementation, so this file DEFINES the API the implementation must
 * satisfy. `src/exit-state.ts` does not exist yet — the failing import is the deliberate Red
 * state of TDD (constitution Principle V).
 *
 * ---------------------------------------------------------------------------------------
 * WHY THIS MODULE EXISTS
 * ---------------------------------------------------------------------------------------
 *
 * Contract X1: once `token` and `siteId` are non-null, EVERY terminal path — success, handled
 * failure, or unexpected throw — emits `host`, `token`, `siteId` and writes `.env`.
 *
 * Today the only code that prints the token is `displayFinalSteps()` (`src/index.ts:532`), and
 * it sits downstream of all 17 `process.exit(1)` call sites — 13 of them inside the single
 * `try` opened at `src/index.ts:93`. So a run that already holds a working token can, and does,
 * exit having printed nothing. `finally` cannot fix this: it does NOT run on `process.exit()`
 * (D1). A `process.on('exit')` handler does, and it also covers exit paths nobody has written
 * yet — which is the whole point of the decision.
 *
 * ---------------------------------------------------------------------------------------
 * API PINNED BY THIS SPEC
 * ---------------------------------------------------------------------------------------
 *
 *   export interface RecoverableState {
 *       host: string;              // e.g. 'http://localhost:8082'
 *       token: string;             // the issued API token — the thing that must never be lost
 *       siteId: string;            // resolved default site identifier
 *       projectDirectory: string;  // absolute dir that receives `.env`
 *       framework?: string;        // decides which variable names go in the block
 *   }
 *
 *   // Merges into module-level state. Callers record what they know, when they know it
 *   // (`projectDirectory` is known long before `token`), so this takes a Partial.
 *   export function recordRecoverableState(state: Partial<RecoverableState>): void;
 *
 *   // Registers the single `process.on('exit')` handler. Idempotent: calling it more than
 *   // once must NOT register a second listener.
 *   export function installExitStateHandler(): void;
 *
 *   // Test seam: unregisters the handler and clears recorded state. Exists so specs cannot
 *   // leak a live exit listener (or a stale token) into each other.
 *   export function resetExitState(): void;
 *
 * Behaviour pinned:
 *   1. With host+token+siteId recorded, the handler prints all three to STDOUT. Stdout — not
 *      stderr — because a wrapper piping the CLI's output is exactly the consumer that must be
 *      able to recover these values.
 *   2. The handler is registered on `'exit'`, never on `'beforeExit'`. `'beforeExit'` is
 *      skipped on `process.exit()`, which is the case that actually loses the token today.
 *   3. `.env` is written when absent, and contains the recorded values (D6: always `.env`,
 *      every framework).
 *   4. An existing `.env` is left byte-for-byte alone; the paste block is printed instead
 *      (D6 / X8 / research R7).
 *   5. Nothing is printed and nothing is written when no token has been recorded — there is
 *      no successful state to recover, so a plain `--help` run stays silent.
 *   6. The handler is synchronous: `writeFileSync`, never `writeFile`/`fs.promises`, and it
 *      returns `undefined`, not a promise. Node runs no async work during `'exit'`, so an
 *      async write is the same as no write at all.
 *   7. Registering twice prints once and writes once.
 *
 * NOTE ON HOW THE HANDLER IS FIRED HERE
 * A spec cannot exit the process it is running in, and emitting `'exit'` on the real `process`
 * would also fire Jest's own listeners. So these tests capture the listener the module adds
 * (by diffing `process.listeners('exit')` across `installExitStateHandler()`) and invoke just
 * that one, with the exit code Node itself would pass: `0` for an ordinary exit, non-zero for
 * an explicit `process.exit(1)`. The "is it on the right event" half of the guarantee is
 * asserted structurally, in the `'beforeExit'` test.
 *
 * Contract: specs/37262-create-app-docker-uve/contracts/cli-exit-contract.md — X1 (and X8).
 * Decisions: specs/37262-create-app-docker-uve/cli-design-decisions.md — D1, D6.
 * Acceptance criterion: AC-004.
 */

import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

import {
    flushRecoverableState,
    installExitStateHandler,
    recordRecoverableState,
    resetExitState
} from './exit-state';

type ExitListener = (code: number) => unknown;

const HOST = 'http://localhost:8082';
const TOKEN = 'eyJhbGciOiJIUzI1NiJ9.recoverable-token-37262';
const SITE_ID = '48190c8c-42c4-46af-8d1a-0cd5db894797';

describe('exit-state (contract X1 — no successful state is ever discarded)', () => {
    let tmpDir: string;
    let envPath: string;
    let stdout: string[];
    let listenersBefore: ExitListener[];

    /** The listeners `installExitStateHandler()` added, and nothing else. */
    const installedListeners = (): ExitListener[] =>
        (process.listeners('exit') as ExitListener[]).filter(
            (listener) => !listenersBefore.includes(listener)
        );

    /** Simulate Node emitting `'exit'` with `code`, without touching Jest's own listeners. */
    const fireExit = (code = 0): unknown[] =>
        installedListeners().map((listener) => listener(code));

    const output = (): string => stdout.join('');

    beforeEach(() => {
        tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'dotcms-exit-state-'));
        envPath = path.join(tmpDir, '.env');

        stdout = [];
        jest.spyOn(console, 'log').mockImplementation((...args: unknown[]) => {
            stdout.push(args.map(String).join(' ') + '\n');
        });
        jest.spyOn(process.stdout, 'write').mockImplementation((chunk: unknown) => {
            stdout.push(String(chunk));

            return true;
        });

        listenersBefore = process.listeners('exit') as ExitListener[];
    });

    afterEach(() => {
        // Unregister before restoring the spies, so a leaked handler cannot print into a
        // later test — or into Jest's own shutdown.
        resetExitState();
        installedListeners().forEach((listener) => process.off('exit', listener));

        jest.restoreAllMocks();
        fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    describe('printing recovered state', () => {
        /**
         * When `.env` is written, the token belongs in the FILE, not echoed into the terminal.
         * The CLI used to print it twice — once in a "paste this into .env" block and again in
         * the recovery block — while having already written the file, so it both duplicated a
         * JWT into scrollback and told the user to do something already done.
         */
        it('confirms the file it wrote, and does NOT echo the token into scrollback', () => {
            installExitStateHandler();
            recordRecoverableState({
                host: HOST,
                token: TOKEN,
                siteId: SITE_ID,
                projectDirectory: tmpDir,
                framework: 'nextjs'
            });

            fireExit(0);

            expect(output()).toContain('.env');
            expect(output()).toContain(HOST);
            expect(output()).toContain(SITE_ID);
            // The value is safe on disk; scrollback and CI logs do not need a copy.
            expect(output()).not.toContain(TOKEN);
            expect(fs.readFileSync(envPath, 'utf8')).toContain(TOKEN);
        });

        it('DOES print the token when it could not write it anywhere', () => {
            fs.writeFileSync(envPath, 'PRE_EXISTING=1\n', 'utf8');

            installExitStateHandler();
            recordRecoverableState({
                host: HOST,
                token: TOKEN,
                siteId: SITE_ID,
                projectDirectory: tmpDir,
                framework: 'nextjs'
            });

            fireExit(0);

            // Nothing was written, so the terminal is the only place the run survives.
            expect(output()).toContain(TOKEN);
        });

        it('prints host, token and siteId once state has been recorded', () => {
            recordRecoverableState({
                host: HOST,
                token: TOKEN,
                siteId: SITE_ID,
                projectDirectory: tmpDir,
                framework: 'angular'
            });
            installExitStateHandler();

            fireExit(0);

            expect(output()).toContain(HOST);
            expect(output()).toContain(TOKEN);
            expect(output()).toContain(SITE_ID);
        });

        it('prints on an ordinary exit (code 0)', () => {
            recordRecoverableState({
                host: HOST,
                token: TOKEN,
                siteId: SITE_ID,
                projectDirectory: tmpDir,
                framework: 'angular'
            });
            installExitStateHandler();

            fireExit(0);

            expect(output()).toContain(TOKEN);
        });

        it('prints on an explicit process.exit(1) — the path `finally` never reaches', () => {
            recordRecoverableState({
                host: HOST,
                token: TOKEN,
                siteId: SITE_ID,
                projectDirectory: tmpDir,
                framework: 'angular'
            });
            installExitStateHandler();

            // Node passes the code given to `process.exit()` straight to `'exit'` listeners.
            // This is the 17-call-site case the whole module exists for.
            fireExit(1);

            expect(output()).toContain(HOST);
            expect(output()).toContain(TOKEN);
            expect(output()).toContain(SITE_ID);
        });

        it("registers on 'exit', not on 'beforeExit'", () => {
            const beforeExitCount = process.listeners('beforeExit').length;

            installExitStateHandler();

            // 'beforeExit' does not fire on process.exit(); registering there would reproduce
            // the exact bug this module fixes.
            expect(installedListeners()).toHaveLength(1);
            expect(process.listeners('beforeExit')).toHaveLength(beforeExitCount);
        });
    });

    /**
     * Found by running the CLI end to end (T054), not by any unit test that existed.
     *
     * The recovery block and the written file MUST use the same variable names the scaffolded
     * app actually reads, and those differ per framework: Next.js takes `NEXT_PUBLIC_*`, Astro
     * takes `PUBLIC_*`, and Angular does not use a dotenv file at all — it reads a TypeScript
     * `environment` object. A .env that looks plausible but names the token wrong is worse than
     * no .env: `npm run dev` fails to authenticate and nothing says why.
     */
    /**
     * The handler runs at process exit, so anything it prints necessarily lands AFTER the
     * "Next Steps" block — which meant a successful run reported its connection details twice,
     * once inside the summary and once tacked on the end.
     *
     * `flushRecoverableState()` lets the success path do the write and claim the reporting, so
     * the details appear inside Next Steps where they belong. The handler then stays silent —
     * it is a fallback for paths that never reach the summary, not a second announcement.
     */
    describe('who reports the state', () => {
        it('flush writes the file and hands back what to render', () => {
            recordRecoverableState({
                host: HOST,
                token: TOKEN,
                siteId: SITE_ID,
                projectDirectory: tmpDir,
                framework: 'nextjs'
            });

            const report = flushRecoverableState();

            expect(report).toMatchObject({
                wroteEnv: true,
                filename: '.env',
                host: HOST,
                siteId: SITE_ID
            });
            expect(fs.readFileSync(envPath, 'utf8')).toContain(TOKEN);
        });

        it('the exit handler stays silent once flush has reported', () => {
            installExitStateHandler();
            recordRecoverableState({
                host: HOST,
                token: TOKEN,
                siteId: SITE_ID,
                projectDirectory: tmpDir,
                framework: 'nextjs'
            });

            flushRecoverableState();
            fireExit(0);

            // No second announcement after the summary the caller already printed.
            expect(output()).toBe('');
        });

        it('the exit handler still speaks when nothing flushed — the recovery case', () => {
            installExitStateHandler();
            recordRecoverableState({
                host: HOST,
                token: TOKEN,
                siteId: SITE_ID,
                projectDirectory: tmpDir,
                framework: 'nextjs'
            });

            fireExit(0);

            expect(output()).toContain(HOST);
        });

        it('flush returns null when there is nothing to report', () => {
            expect(flushRecoverableState()).toBeNull();
        });
    });

    describe('the written file matches what the scaffolded app reads', () => {
        it('uses NEXT_PUBLIC_ names for a Next.js project', () => {
            installExitStateHandler();
            recordRecoverableState({
                host: HOST,
                token: TOKEN,
                siteId: SITE_ID,
                projectDirectory: tmpDir,
                framework: 'nextjs'
            });
            fireExit(0);

            const written = fs.readFileSync(envPath, 'utf8');

            expect(written).toContain(`NEXT_PUBLIC_DOTCMS_AUTH_TOKEN=${TOKEN}`);
            expect(written).toContain(`NEXT_PUBLIC_DOTCMS_HOST=${HOST}`);
            expect(written).toContain(`NEXT_PUBLIC_DOTCMS_SITE_ID=${SITE_ID}`);
            // The bare name is what the bug wrote; it must not appear on its own.
            expect(written).not.toMatch(/^\s*DOTCMS_AUTH_TOKEN=/m);
        });

        it('uses PUBLIC_ names for an Astro project', () => {
            installExitStateHandler();
            recordRecoverableState({
                host: HOST,
                token: TOKEN,
                siteId: SITE_ID,
                projectDirectory: tmpDir,
                framework: 'astro'
            });
            fireExit(0);

            const written = fs.readFileSync(envPath, 'utf8');

            expect(written).toContain(`PUBLIC_DOTCMS_AUTH_TOKEN=${TOKEN}`);
            expect(written).not.toContain('NEXT_PUBLIC_DOTCMS_AUTH_TOKEN=');
        });

        it('writes no .env for Angular, which reads a TypeScript environment object', () => {
            installExitStateHandler();
            recordRecoverableState({
                host: HOST,
                token: TOKEN,
                siteId: SITE_ID,
                projectDirectory: tmpDir,
                framework: 'angular'
            });
            fireExit(0);

            expect(fs.existsSync(envPath)).toBe(false);
            // The values still have to reach the user.
            expect(output()).toContain(TOKEN);
        });
    });

    describe('.env (D6 — always `.env`, write-if-absent)', () => {
        it('writes .env with the recorded values when the file is absent', () => {
            recordRecoverableState({
                host: HOST,
                token: TOKEN,
                siteId: SITE_ID,
                projectDirectory: tmpDir,
                framework: 'nextjs'
            });
            installExitStateHandler();

            expect(fs.existsSync(envPath)).toBe(false);

            fireExit(0);

            expect(fs.existsSync(envPath)).toBe(true);

            const written = fs.readFileSync(envPath, 'utf8');
            expect(written).toContain(HOST);
            expect(written).toContain(TOKEN);
            expect(written).toContain(SITE_ID);
        });

        it('leaves an existing .env untouched and prints the paste block instead', () => {
            const existing =
                '# shipped by the scaffolded example\nNEXT_PUBLIC_DOTCMS_HOST=keep-me\n';
            fs.writeFileSync(envPath, existing, 'utf8');

            recordRecoverableState({
                host: HOST,
                token: TOKEN,
                siteId: SITE_ID,
                projectDirectory: tmpDir,
                framework: 'nextjs'
            });
            installExitStateHandler();

            fireExit(0);

            // Byte-for-byte: the user's file is never clobbered.
            expect(fs.readFileSync(envPath, 'utf8')).toBe(existing);

            // ...but the values still have to reach the user, as a block they can paste.
            expect(output()).toContain(HOST);
            expect(output()).toContain(TOKEN);
            expect(output()).toContain(SITE_ID);
            expect(output()).toContain('.env');
        });
    });

    describe('nothing to recover', () => {
        it('prints nothing and writes nothing when no token has been recorded', () => {
            recordRecoverableState({ host: HOST, projectDirectory: tmpDir });
            installExitStateHandler();

            fireExit(0);

            expect(output()).toBe('');
            expect(fs.readdirSync(tmpDir)).toEqual([]);
        });

        it('prints nothing and writes nothing when nothing at all has been recorded', () => {
            installExitStateHandler();

            fireExit(0);

            expect(output()).toBe('');
            expect(fs.readdirSync(tmpDir)).toEqual([]);
        });
    });

    describe('the handler is synchronous (D1)', () => {
        it('writes with writeFileSync, never writeFile, and returns no promise', () => {
            const writeFileSync = jest.spyOn(fs, 'writeFileSync');
            const writeFile = jest.spyOn(fs, 'writeFile');
            const writeFileAsync = jest.spyOn(fs.promises, 'writeFile');

            recordRecoverableState({
                host: HOST,
                token: TOKEN,
                siteId: SITE_ID,
                projectDirectory: tmpDir
            });
            installExitStateHandler();

            const returned = fireExit(0);

            expect(writeFileSync).toHaveBeenCalled();
            expect(writeFile).not.toHaveBeenCalled();
            expect(writeFileAsync).not.toHaveBeenCalled();

            // Node runs nothing async during 'exit', so a returned thenable would mean the
            // write never lands.
            returned.forEach((value) => {
                expect(value).toBeUndefined();
            });

            // No `await`, no tick: the file is already on disk the instant the call returns.
            expect(fs.existsSync(envPath)).toBe(true);
        });
    });

    describe('idempotent installation', () => {
        it('registering twice prints once and writes once', () => {
            const writeFileSync = jest.spyOn(fs, 'writeFileSync');

            recordRecoverableState({
                host: HOST,
                token: TOKEN,
                siteId: SITE_ID,
                projectDirectory: tmpDir
            });

            installExitStateHandler();
            installExitStateHandler();

            expect(installedListeners()).toHaveLength(1);

            fireExit(0);

            expect(writeFileSync).toHaveBeenCalledTimes(1);
            expect(output().split(SITE_ID)).toHaveLength(2); // reported exactly once
        });
    });
});
