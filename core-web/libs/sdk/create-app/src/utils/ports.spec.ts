/**
 * Contract spec for port-conflict resolution (task T031, dotCMS #37262, AC-006).
 *
 * Written before the implementation; this file defines the API.
 *
 * THE BUG. Reproduction step 6: once a run leaves a dotCMS stack up, re-running the CLI aborts
 * with "Required ports are already in use", because `checkPortsAvailability()` hard-fails on
 * 8082/8443/9200/9600 — exactly the ports a SUCCESSFUL previous run now holds. The CLI's own
 * side effect blocks its own recovery, which is what made the reported failure unrecoverable
 * rather than merely annoying.
 *
 * DECISION D3 (already taken; this spec implements it, it does not re-open it):
 *   - Probe before failing. A busy 8082 with a healthy dotCMS behind it is a reusable instance,
 *     not a conflict.
 *   - Non-interactive (CI, or no TTY): auto-reuse, but PRINT A NOTICE. "Silent" means no prompt,
 *     not no output — a scripted run that quietly attaches to an unknown instance is precisely
 *     the failure this is meant to prevent.
 *   - Interactive: a real choice, reuse OR abort. Someone who did not expect a dotCMS on 8082
 *     needs to stop and look, not be pushed onward.
 *   - Only reuse something that passes readiness AND can issue a token. A busy port with
 *     anything else behind it stays a hard failure.
 *
 * API PINNED
 *   export interface BusyPort { port: number; service: string }
 *   export type PortConflictOutcome =
 *       | { kind: 'free' }
 *       | { kind: 'reuse'; host: string }
 *       | { kind: 'abort'; message: string };
 *   export function resolvePortConflict(options: {
 *       busyPorts: BusyPort[];
 *       isInteractive: boolean;
 *       host: string;
 *       probeInstance: () => Promise<boolean>;   // readiness AND token issuance
 *       askReuse: () => Promise<boolean>;
 *       notify: (message: string) => void;
 *   }): Promise<PortConflictOutcome>;
 *
 * A STRING discriminant, not a boolean: this workspace sets "strict": false, and without
 * strictNullChecks TypeScript will not narrow a union on a boolean-literal discriminant.
 *
 * Contract: contracts/cli-exit-contract.md X6. Decision: cli-design-decisions.md D3.
 */

import { resolvePortConflict } from './ports';

const HOST = 'http://localhost:8082';

const DOTCMS_HTTP: { port: number; service: string } = { port: 8082, service: 'dotCMS HTTP' };
const ES_HTTP: { port: number; service: string } = { port: 9200, service: 'Elasticsearch HTTP' };

function options(overrides: Partial<Parameters<typeof resolvePortConflict>[0]> = {}) {
    return {
        busyPorts: [],
        isInteractive: false,
        host: HOST,
        probeInstance: jest.fn().mockResolvedValue(true),
        askReuse: jest.fn().mockResolvedValue(true),
        notify: jest.fn(),
        ...overrides
    };
}

describe('resolvePortConflict', () => {
    let exitSpy: jest.SpyInstance;

    beforeEach(() => {
        exitSpy = jest.spyOn(process, 'exit').mockImplementation(((code?: number) => {
            throw new Error(`process.exit(${code}) must never be called here`);
        }) as never);
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe('nothing in the way', () => {
        it('proceeds when no port is busy, without probing or prompting', async () => {
            const opts = options();

            const outcome = await resolvePortConflict(opts);

            expect(outcome.kind).toBe('free');
            expect(opts.probeInstance).not.toHaveBeenCalled();
            expect(opts.askReuse).not.toHaveBeenCalled();
        });
    });

    describe('a healthy dotCMS is already on 8082', () => {
        it('reuses it without prompting when non-interactive', async () => {
            const opts = options({ busyPorts: [DOTCMS_HTTP], isInteractive: false });

            const outcome = await resolvePortConflict(opts);

            expect(outcome).toEqual({ kind: 'reuse', host: HOST });
            expect(opts.askReuse).not.toHaveBeenCalled();
        });

        it('still prints a notice when it auto-reuses (D3: silent means no prompt, not no output)', async () => {
            const opts = options({ busyPorts: [DOTCMS_HTTP], isInteractive: false });

            await resolvePortConflict(opts);

            expect(opts.notify).toHaveBeenCalled();

            const said = (opts.notify as jest.Mock).mock.calls.flat().join('\n');

            expect(said).toContain('8082');
            expect(said).toMatch(/reus/i);
        });

        it('asks the user when interactive, and reuses on yes', async () => {
            const opts = options({
                busyPorts: [DOTCMS_HTTP],
                isInteractive: true,
                askReuse: jest.fn().mockResolvedValue(true)
            });

            const outcome = await resolvePortConflict(opts);

            expect(opts.askReuse).toHaveBeenCalledTimes(1);
            expect(outcome).toEqual({ kind: 'reuse', host: HOST });
        });

        it('aborts on no — the prompt is a real choice, not a formality', async () => {
            const opts = options({
                busyPorts: [DOTCMS_HTTP],
                isInteractive: true,
                askReuse: jest.fn().mockResolvedValue(false)
            });

            const outcome = await resolvePortConflict(opts);

            expect(outcome.kind).toBe('abort');
        });
    });

    describe('the port is busy but it is not a usable dotCMS', () => {
        it('aborts when the probe fails, naming the busy port', async () => {
            const opts = options({
                busyPorts: [DOTCMS_HTTP],
                isInteractive: false,
                probeInstance: jest.fn().mockResolvedValue(false)
            });

            const outcome = await resolvePortConflict(opts);

            expect(outcome.kind).toBe('abort');

            if (outcome.kind === 'abort') {
                expect(outcome.message).toContain('8082');
            }
        });

        it('never reuses an instance whose readiness or token issuance failed', async () => {
            const opts = options({
                busyPorts: [DOTCMS_HTTP],
                isInteractive: true,
                probeInstance: jest.fn().mockResolvedValue(false)
            });

            const outcome = await resolvePortConflict(opts);

            expect(outcome.kind).toBe('abort');
            // It must not even offer the choice: there is nothing safe to reuse.
            expect(opts.askReuse).not.toHaveBeenCalled();
        });

        it('aborts when a required port other than 8082 is taken', async () => {
            const opts = options({ busyPorts: [ES_HTTP], isInteractive: false });

            const outcome = await resolvePortConflict(opts);

            expect(outcome.kind).toBe('abort');

            if (outcome.kind === 'abort') {
                expect(outcome.message).toContain('9200');
            }
        });
    });

    describe('contract X2 — abort is a value, not an exit', () => {
        it('never calls process.exit on any path', async () => {
            await resolvePortConflict(options());
            await resolvePortConflict(options({ busyPorts: [DOTCMS_HTTP] }));
            await resolvePortConflict(
                options({ busyPorts: [ES_HTTP], probeInstance: jest.fn().mockResolvedValue(false) })
            );

            expect(exitSpy).not.toHaveBeenCalled();
        });
    });
});
