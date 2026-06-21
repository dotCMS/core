import { ActiveRunRegistry } from './active-run';
import { decodeSubject, extractBearer, parseBearer } from './auth';
import { createA11yRoutes, type A11yRoutesDeps } from './routes';

import type { FixReport, FixRequest } from '../domain/contract';
import type { ScanFinding } from '../dotcms/dotcms-client';
import type { RunFixDeps } from '../fix/run-fix';

/** A JWT with sub="user-42" (header.payload.sig; unsigned — agent doesn't verify). */
function makeJwt(sub: string): string {
    const b64 = (o: unknown) => Buffer.from(JSON.stringify(o)).toString('base64url');
    return `${b64({ alg: 'none' })}.${b64({ sub })}.sig`;
}

const TOKEN = makeJwt('user-42');

const validBody: FixRequest = {
    runId: 'r_http',
    dotcmsBaseUrl: 'https://demo.dotcms.com',
    page: {
        identifier: 'a9f3',
        uri: '/index',
        liveUrl: 'https://demo.dotcms.com/index',
        host: 'demo.dotcms.com',
        hostId: '48190c8c',
        languageId: 1
    },
    options: { skipCss: false }
};

const fakeReport: FixReport = {
    runId: 'r_http',
    page: { uri: '/index', host: 'demo.dotcms.com', languageId: 1 },
    scan: { before: { violations: 3 }, after: { violations: 1 } },
    results: [{ ruleId: 'image-alt', status: 'fixed-to-working', file: '//x/h.vtl' }],
    publishRequired: true
};

/** A client whose scan throws — drives the 401/422/502 paths without network. */
function throwingDeps(): A11yRoutesDeps['makeRunDeps'] {
    return () =>
        ({
            client: {
                scan: async () => {
                    throw new Error('network blocked in test');
                }
            }
        }) as unknown as RunFixDeps;
}

/** A client that yields a clean zero-violation run (drives the 200 success path). */
function cleanRunDeps(): A11yRoutesDeps['makeRunDeps'] {
    return () =>
        ({
            client: {
                scan: async () => ({
                    ok: true,
                    totalIssues: 0,
                    counts: { errors: 0, warnings: 0, notices: 0 },
                    findings: {
                        total: 0,
                        violations: 0,
                        needsReview: 0,
                        items: [] as ScanFinding[]
                    }
                }),
                locate: async () => ({
                    containers: {},
                    page: { identifier: 'a9f3', languageId: 1, uri: '/index' },
                    theme: { folderPath: '', id: '', name: '', vtls: [] as string[] },
                    widgets: [] as string[]
                }),
                read: async () => '',
                saveWorking: async () => ({
                    fileSize: 0,
                    identifier: '',
                    inode: '',
                    lang: 'en-us',
                    live: false,
                    name: '',
                    path: '',
                    working: true
                })
            }
        }) as unknown as RunFixDeps;
}

describe('auth helpers', () => {
    it('extracts a bearer token (case-insensitive)', () => {
        expect(extractBearer('Bearer abc.def.ghi')).toBe('abc.def.ghi');
        expect(extractBearer('bearer  xyz ')).toBe('xyz');
        expect(extractBearer(undefined)).toBeNull();
        expect(extractBearer('Basic abc')).toBeNull();
    });

    it('decodes the JWT subject without verifying', () => {
        expect(decodeSubject(makeJwt('user-42'))).toBe('user-42');
        expect(decodeSubject('not-a-jwt')).toBeNull();
    });

    it('parseBearer returns token + userId, falling back to unknown', () => {
        expect(parseBearer(`Bearer ${TOKEN}`)).toEqual({ token: TOKEN, userId: 'user-42' });
        expect(parseBearer('Bearer opaque-token')).toEqual({
            token: 'opaque-token',
            userId: 'unknown'
        });
    });
});

describe('POST /a11y/fix', () => {
    it('401s without a bearer token', async () => {
        const app = createA11yRoutes({ makeRunDeps: throwingDeps() });
        const res = await app.request('/fix', { method: 'POST', body: '{}' });
        expect(res.status).toBe(401);
    });

    it('422s on a body that violates the contract', async () => {
        const app = createA11yRoutes({ makeRunDeps: throwingDeps() });
        const res = await app.request('/fix', {
            method: 'POST',
            headers: { Authorization: `Bearer ${TOKEN}` },
            body: JSON.stringify({ runId: 'x' }) // missing required fields
        });
        expect(res.status).toBe(422);
    });

    it('runs the loop and returns the §6 report on success', async () => {
        const registry = new ActiveRunRegistry();
        const app = createA11yRoutes({ registry, makeRunDeps: cleanRunDeps() });

        const res = await app.request('/fix', {
            method: 'POST',
            headers: { Authorization: `Bearer ${TOKEN}` },
            body: JSON.stringify(validBody)
        });
        expect(res.status).toBe(200);
        const report = (await res.json()) as FixReport;
        expect(report.runId).toBe('r_http');
        expect(report.publishRequired).toBe(true);
        // The slot is now finished for this user.
        expect(registry.get('user-42')?.status).toBe('done');
    });

    it('502s and marks the slot errored when the run throws', async () => {
        const registry = new ActiveRunRegistry();
        const app = createA11yRoutes({ registry, makeRunDeps: throwingDeps() });
        const res = await app.request('/fix', {
            method: 'POST',
            headers: { Authorization: `Bearer ${TOKEN}` },
            body: JSON.stringify(validBody)
        });
        expect(res.status).toBe(502);
        expect(registry.get('user-42')?.status).toBe('error');
    });
});

describe('GET /a11y/active-run', () => {
    it('401s without a token', async () => {
        const app = createA11yRoutes();
        const res = await app.request('/active-run');
        expect(res.status).toBe(401);
    });

    it('returns null when the user has no run', async () => {
        const app = createA11yRoutes({ registry: new ActiveRunRegistry() });
        const res = await app.request('/active-run', {
            headers: { Authorization: `Bearer ${TOKEN}` }
        });
        expect(res.status).toBe(200);
        expect(await res.json()).toBeNull();
    });

    it('reflects a finished run after a fix', async () => {
        const registry = new ActiveRunRegistry();
        registry.start('user-42', 'r_http');
        registry.finish('user-42', 'r_http', fakeReport);
        const app = createA11yRoutes({ registry });
        const res = await app.request('/active-run', {
            headers: { Authorization: `Bearer ${TOKEN}` }
        });
        const body = await res.json();
        expect(body).toMatchObject({ runId: 'r_http', status: 'done' });
    });
});

describe('ActiveRunRegistry (plan §8.7)', () => {
    it('start → finish records the report; get returns it', () => {
        const r = new ActiveRunRegistry();
        r.start('u1', 'run1');
        expect(r.get('u1')?.status).toBe('running');
        r.finish('u1', 'run1', fakeReport);
        expect(r.get('u1')).toMatchObject({ runId: 'run1', status: 'done' });
    });

    it('a stale run does not overwrite a newer slot (replace-on-retrigger)', () => {
        const r = new ActiveRunRegistry();
        r.start('u1', 'run1');
        r.start('u1', 'run2'); // re-trigger replaces
        r.finish('u1', 'run1', fakeReport); // late finish from the old run — ignored
        expect(r.get('u1')?.runId).toBe('run2');
        expect(r.get('u1')?.status).toBe('running');
    });

    it('isolates users', () => {
        const r = new ActiveRunRegistry();
        r.start('u1', 'run1');
        expect(r.get('u2')).toBeNull();
    });
});
