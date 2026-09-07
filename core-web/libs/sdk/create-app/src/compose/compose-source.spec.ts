/**
 * Contract spec for `src/compose/compose-source.ts` (task T010, dotCMS #37262).
 *
 * This file is written BEFORE the implementation and therefore DEFINES the API the
 * implementation must satisfy. The module does not exist yet — the failing import is the
 * deliberate Red state of TDD.
 *
 * ---------------------------------------------------------------------------------------
 * API PINNED BY THIS SPEC
 * ---------------------------------------------------------------------------------------
 *
 *   export type ComposeSource = BundledComposeSource | RemoteComposeSource;
 *
 *   interface ComposeSourceBase {
 *       readonly describe: string;          // human-readable, shown in diagnostics (D4a)
 *       read(): Promise<string>;            // returns the file CONTENTS, never writes to disk
 *   }
 *
 *   interface BundledComposeSource extends ComposeSourceBase {
 *       readonly kind: 'bundled';
 *       readonly path: string;              // absolute path to the packaged asset
 *   }
 *
 *   interface RemoteComposeSource extends ComposeSourceBase {
 *       readonly kind: 'remote';
 *       readonly url: string;
 *   }
 *
 *   export function resolveComposeSource(): ComposeSource;
 *
 * `kind` is the discriminant that makes "the default path performs no network access"
 * assertable BY CONSTRUCTION — no http mocking required. A `'bundled'` source carries a
 * filesystem `path` and no `url`; there is nothing for it to fetch.
 *
 * Behaviour pinned:
 *   1. No `DOTCMS_COMPOSE_URL`      -> bundled asset at `<pkg>/assets/docker-compose.yml`.
 *   2. `DOTCMS_COMPOSE_URL=<url>`   -> remote source carrying exactly that URL verbatim.
 *   3. `DOTCMS_COMPOSE_URL=''`      -> falsy override, falls back to bundled (D4a uses a
 *                                      truthiness check; an empty var must not disable the
 *                                      default source).
 *   4. `resolveComposeSource()` reads the env var at CALL time, not at module load time,
 *      so the escape hatch works without a re-import.
 *
 * Contract: specs/37262-create-app-docker-uve/contracts/compose-service-contract.md — C7.
 * Decision: specs/37262-create-app-docker-uve/cli-design-decisions.md — D4/D4a.
 */

import { resolveComposeSource } from './compose-source';

const ENV_VAR = 'DOTCMS_COMPOSE_URL';
const OVERRIDE_URL =
    'https://raw.githubusercontent.com/dotCMS/core/main/docker/docker-compose-examples/single-node-demo-site/docker-compose.yml';

describe('resolveComposeSource', () => {
    let savedOverride: string | undefined;

    beforeEach(() => {
        savedOverride = process.env[ENV_VAR];
        delete process.env[ENV_VAR];
    });

    afterEach(() => {
        if (savedOverride === undefined) {
            delete process.env[ENV_VAR];
        } else {
            process.env[ENV_VAR] = savedOverride;
        }
    });

    describe('by default (no DOTCMS_COMPOSE_URL)', () => {
        it('resolves to the bundled asset shipped inside the npm package', () => {
            const source = resolveComposeSource();

            expect(source.kind).toBe('bundled');
        });

        it('identifies the local packaged file by path, not by URL', () => {
            const source = resolveComposeSource();

            if (source.kind !== 'bundled') {
                throw new Error(
                    `expected the default source to be 'bundled', got '${source.kind}'`
                );
            }

            expect(typeof source.path).toBe('string');
            expect(source.path.length).toBeGreaterThan(0);
            // An absolute filesystem path to the shipped asset — resolvable from the
            // installed package, not relative to the user's cwd.
            expect(source.path.startsWith('/')).toBe(true);
            expect(source.path).toMatch(/assets[\\/]docker-compose\.yml$/);
        });

        it('performs no network access by construction — it carries no URL to fetch', () => {
            const source = resolveComposeSource();

            // The security/reliability point of #37262: today's downloadFile() uses a bare
            // https.get with no timeout, no retry and no redirect handling. A 'bundled'
            // source has no URL at all, so that code path is unreachable by default.
            expect(source.kind).toBe('bundled');
            expect(source).not.toHaveProperty('url');
            expect(JSON.stringify(source)).not.toMatch(/https?:/);
        });

        it('exposes the ComposeSource shape: a describe string and a read() returning contents', () => {
            const source = resolveComposeSource();

            expect(typeof source.describe).toBe('string');
            expect(source.describe.length).toBeGreaterThan(0);
            expect(typeof source.read).toBe('function');
        });

        it('falls back to the bundled asset when DOTCMS_COMPOSE_URL is set but empty', () => {
            process.env[ENV_VAR] = '';

            expect(resolveComposeSource().kind).toBe('bundled');
        });
    });

    describe('with DOTCMS_COMPOSE_URL set', () => {
        it('resolves to a remote source', () => {
            process.env[ENV_VAR] = OVERRIDE_URL;

            expect(resolveComposeSource().kind).toBe('remote');
        });

        it('carries the override URL verbatim', () => {
            process.env[ENV_VAR] = OVERRIDE_URL;

            const source = resolveComposeSource();

            if (source.kind !== 'remote') {
                throw new Error(
                    `expected the override source to be 'remote', got '${source.kind}'`
                );
            }

            expect(source.url).toBe(OVERRIDE_URL);
            expect(source).not.toHaveProperty('path');
            expect(typeof source.read).toBe('function');
        });

        it('reads the environment variable at call time, so the escape hatch needs no re-import', () => {
            expect(resolveComposeSource().kind).toBe('bundled');

            process.env[ENV_VAR] = OVERRIDE_URL;
            expect(resolveComposeSource().kind).toBe('remote');

            delete process.env[ENV_VAR];
            expect(resolveComposeSource().kind).toBe('bundled');
        });
    });
});
