import { mintToken, verifyToken } from './auth';
import { checkReachable } from './instance';

const URL_ = 'https://demo.dotcms.com';

describe('auth', () => {
    afterEach(() => { jest.restoreAllMocks(); });

    describe('mintToken (FR-006, FR-007)', () => {
        it('returns the token from entity.token on 200', async () => {
            jest.spyOn(globalThis, 'fetch').mockResolvedValue(
                new Response(JSON.stringify({ entity: { token: 'dot_abc123' } }), { status: 200 })
            );
            const token = await mintToken({ url: URL_, user: 'a@b.com', password: 'pw' });
            expect(token.value).toBe('dot_abc123');
            expect(token.origin).toBe('minted');
            expect(token.verified).toBe(false);
        });

        it('sends expirationDays as a STRING', async () => {
            const fetchMock = jest.spyOn(globalThis, 'fetch').mockResolvedValue(
                new Response(JSON.stringify({ entity: { token: 't' } }), { status: 200 })
            );
            await mintToken({ url: URL_, user: 'a@b.com', password: 'pw' });
            const body = JSON.parse(String((fetchMock.mock.calls[0][1] as RequestInit).body));
            expect(typeof body.expirationDays).toBe('string');
        });

        it('never puts the password in the URL', async () => {
            const fetchMock = jest.spyOn(globalThis, 'fetch').mockResolvedValue(
                new Response(JSON.stringify({ entity: { token: 't' } }), { status: 200 })
            );
            await mintToken({ url: URL_, user: 'a@b.com', password: 'SuperSecret1' });
            expect(String(fetchMock.mock.calls[0][0])).not.toContain('SuperSecret1');
        });

        it('says the username and password were rejected on 401', async () => {
            jest.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('', { status: 401 }));
            await expect(
                mintToken({ url: URL_, user: 'a@b.com', password: 'wrong' })
            ).rejects.toThrow(/username and password|rejected/i);
        });

        it('gives a connection message on ECONNREFUSED, not a raw fetch error', async () => {
            jest.spyOn(globalThis, 'fetch').mockRejectedValue(
                Object.assign(new TypeError('fetch failed'), { cause: { code: 'ECONNREFUSED' } })
            );
            await expect(
                mintToken({ url: URL_, user: 'a@b.com', password: 'pw' })
            ).rejects.toThrow(/connect|refused/i);
        });
    });

    describe('verifyToken (FR-008)', () => {
        it('calls /api/v1/users/current with a bearer token', async () => {
            const fetchMock = jest
                .spyOn(globalThis, 'fetch')
                .mockResolvedValue(new Response(JSON.stringify({ entity: {} }), { status: 200 }));
            await verifyToken(URL_, { value: 'dot_x', origin: 'supplied', verified: false });
            expect(String(fetchMock.mock.calls[0][0])).toContain('/api/v1/users/current');
            const headers = new Headers((fetchMock.mock.calls[0][1] as RequestInit).headers);
            expect(headers.get('authorization')).toBe('Bearer dot_x');
        });

        it('marks a good token verified', async () => {
            jest.spyOn(globalThis, 'fetch').mockResolvedValue(
                new Response(JSON.stringify({ entity: {} }), { status: 200 })
            );
            const out = await verifyToken(URL_, { value: 'dot_x', origin: 'minted', verified: false });
            expect(out.verified).toBe(true);
        });

        it('rejects a SUPPLIED token the instance refuses — not only minted ones', async () => {
            jest.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('', { status: 401 }));
            await expect(
                verifyToken(URL_, { value: 'expired', origin: 'supplied', verified: false })
            ).rejects.toThrow(/token|rejected/i);
        });

        it('distinguishes an unreachable instance from a rejected token (FR-008b)', async () => {
            jest.spyOn(globalThis, 'fetch').mockRejectedValue(
                Object.assign(new TypeError('fetch failed'), { cause: { code: 'ECONNREFUSED' } })
            );
            await expect(
                verifyToken(URL_, { value: 'dot_x', origin: 'supplied', verified: false })
            ).rejects.toThrow(/reach|connect/i);
        });
    });
});

/**
 * `fetch` fails with a famously opaque `TypeError: fetch failed`; the actionable detail lives
 * in `error.cause.code`. With no verbose mode, letting that text reach a user is FR-032a's
 * definition of a defect — so assert it at every network call site rather than trusting that
 * each one remembered to wrap.
 */
describe('no raw fetch error ever reaches the user (FR-032a)', () => {
    const CAUSES = ['ECONNREFUSED', 'ENOTFOUND', 'CERT_HAS_EXPIRED', 'UND_ERR_CONNECT_TIMEOUT'];

    const callers: { name: string; call: () => Promise<unknown> }[] = [
        { name: 'checkReachable', call: () => checkReachable(URL_) },
        { name: 'mintToken', call: () => mintToken({ url: URL_, user: 'a@b.com', password: 'pw' }) },
        {
            name: 'verifyToken',
            call: () => verifyToken(URL_, { value: 'x', origin: 'supplied', verified: false })
        }
    ];

    describe.each(callers)('$name', ({ call }) => {
        it.each(CAUSES)('translates %s into an actionable message', async (code) => {
            jest.spyOn(globalThis, 'fetch').mockRejectedValue(
                Object.assign(new TypeError('fetch failed'), { cause: { code } })
            );
            const err = (await call().catch((e: Error) => e)) as Error;
            expect(err.message).not.toBe('fetch failed');
            expect(err.message).not.toMatch(/^TypeError/);
            expect(err.message).toContain('demo.dotcms.com');
            expect(err.message.length).toBeGreaterThan(30);
        });
    });
});
