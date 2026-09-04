import { appConfiguration, appConfigurationResponse } from './__fixtures__/appconfiguration';
import { checkReachable, compatibilityWarning, resolveUrl } from './instance';

describe('instance', () => {
    describe('resolveUrl (FR-004)', () => {
        const OLD_ENV = process.env;
        beforeEach(() => { process.env = { ...OLD_ENV }; });
        afterAll(() => { process.env = OLD_ENV; });

        it('prefers the supplied option over the environment', async () => {
            process.env['DOTCMS_URL'] = 'https://from-env.example.com';
            await expect(resolveUrl({ url: 'https://from-option.example.com' })).resolves.toBe(
                'https://from-option.example.com'
            );
        });

        it('falls back to the environment when no option is supplied', async () => {
            process.env['DOTCMS_URL'] = 'https://from-env.example.com';
            await expect(resolveUrl({})).resolves.toBe('https://from-env.example.com');
        });

        it('strips trailing slashes', async () => {
            await expect(resolveUrl({ url: 'https://demo.dotcms.com///' })).resolves.toBe(
                'https://demo.dotcms.com'
            );
        });

        it('rejects an address with no scheme rather than writing it through verbatim', async () => {
            await expect(resolveUrl({ url: 'demo.dotcms.com' })).rejects.toThrow(/scheme|protocol|https?:\/\//i);
        });

        /**
         * `new URL()` alone is not enough: it happily parses ftp:, file: and javascript:. Only
         * an explicit http(s) check rejects those, and nothing else in the flow would — the
         * address is handed to fetch and written into an editor's config.
         */
        it.each(['ftp://demo.dotcms.com', 'file:///etc/passwd', 'javascript:alert(1)'])(
            'rejects the non-HTTP scheme %s',
            async (bad) => {
                await expect(resolveUrl({ url: bad })).rejects.toThrow(/scheme|protocol|https?:\/\//i);
            }
        );

        it('accepts plain http, not only https — local instances are the common case', async () => {
            await expect(resolveUrl({ url: 'http://localhost:8082' })).resolves.toBe('http://localhost:8082');
        });
    });

    describe('checkReachable (FR-005)', () => {
        afterEach(() => { jest.restoreAllMocks(); });

        it('uses /api/v1/appconfiguration, not /probes/alive', async () => {
            const fetchMock = jest
                .spyOn(globalThis, 'fetch')
                .mockResolvedValue(appConfigurationResponse());
            await checkReachable('https://demo.dotcms.com');
            expect(fetchMock.mock.calls[0][0]).toContain('/api/v1/appconfiguration');
            expect(fetchMock.mock.calls[0][0]).not.toContain('/probes/');
        });

        it('names the address it tried when the instance is unreachable', async () => {
            jest.spyOn(globalThis, 'fetch').mockRejectedValue(
                Object.assign(new TypeError('fetch failed'), { cause: { code: 'ECONNREFUSED' } })
            );
            await expect(checkReachable('https://nope.example.com')).rejects.toThrow(
                /nope\.example\.com/
            );
        });

        it('translates the transport cause into an actionable message (FR-032a)', async () => {
            jest.spyOn(globalThis, 'fetch').mockRejectedValue(
                Object.assign(new TypeError('fetch failed'), { cause: { code: 'ECONNREFUSED' } })
            );
            // Positive assertions: a bare `not.toThrow(/^fetch failed$/)` is satisfied by ANY
            // error, including "not implemented", so it can never go Red.
            const err = await checkReachable('https://nope.example.com').catch((e: Error) => e);
            expect((err as Error).message).toMatch(/refused|not accepting connections/i);
            expect((err as Error).message).toContain('nope.example.com');
        });
    });

    describe('it must actually BE dotCMS (FR-005b)', () => {
        afterEach(() => { jest.restoreAllMocks(); });

        it('rejects a host that answers 404 — reached, but not dotCMS', async () => {
            jest.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('Not Found', { status: 404 }));
            const err = (await checkReachable('https://example.com').catch((e: Error) => e)) as Error;
            expect(err.message).toMatch(/not a valid dotCMS instance/i);
            expect(err.message).toContain('example.com');
            // Distinct from unreachable: the host answered.
            expect(err.message).not.toMatch(/could not reach/i);
            // Says WHAT is wrong, not HOW we found out. The endpoint probed and the shape
            // expected are implementation detail the developer does not need.
            expect(err.message).not.toMatch(/appconfiguration|api\/v1|proxy|CDN|payload|entity/i);
            expect(err.message.length).toBeLessThan(120);
        });

        it('rejects a host that answers 200 with something else entirely', async () => {
            jest.spyOn(globalThis, 'fetch').mockResolvedValue(
                new Response(JSON.stringify({ hello: 'world' }), { status: 200 })
            );
            await expect(checkReachable('https://proxy.example.com')).rejects.toThrow(
                /not a valid dotCMS instance/i
            );
        });

        it('rejects an HTML page served with 200 — a CDN or site root', async () => {
            jest.spyOn(globalThis, 'fetch').mockResolvedValue(
                new Response('<!doctype html><html><body>hi</body></html>', { status: 200 })
            );
            await expect(checkReachable('https://cdn.example.com')).rejects.toThrow(
                /not a valid dotCMS instance/i
            );
        });

        it('rejects an empty entity — the shape dotCMS never returns', async () => {
            jest.spyOn(globalThis, 'fetch').mockResolvedValue(
                new Response(JSON.stringify({ entity: {} }), { status: 200 })
            );
            await expect(checkReachable('https://x.example.com')).rejects.toThrow(
                /not a valid dotCMS instance/i
            );
        });

        it('accepts a real dotCMS body', async () => {
            jest.spyOn(globalThis, 'fetch').mockResolvedValue(appConfigurationResponse());
            await expect(checkReachable('https://demo.dotcms.com')).resolves.toEqual({
                url: 'https://demo.dotcms.com',
                version: '26.09.03-01'
            });
        });

        it('reads the version from entity.config.releaseInfo.version', async () => {
            // The path that matters: an earlier version read entity.version, which does not
            // exist, so the ADR-0019 warning could never fire.
            jest.spyOn(globalThis, 'fetch').mockResolvedValue(appConfigurationResponse('26.07.14-01'));
            const info = await checkReachable('https://demo.dotcms.com');
            expect(info.version).toBe('26.07.14-01');
        });

        it('still accepts an instance that reports no version — the warning is fail-open', async () => {
            const body = appConfiguration();
            delete (body.entity.config as Record<string, unknown>)['releaseInfo'];
            jest.spyOn(globalThis, 'fetch').mockResolvedValue(
                new Response(JSON.stringify(body), { status: 200 })
            );
            const info = await checkReachable('https://demo.dotcms.com');
            expect(info.version).toBeNull();
        });
    });

    describe('compatibilityWarning (FR-005a, ADR-0019)', () => {
        it('warns and names the exact version to install when the instance is older', () => {
            const warning = compatibilityWarning('2026.6.24', '2026.9.4');
            expect(warning).toContain('2026.6.24');
            expect(warning).toContain('2026.9.4');
        });

        it('is silent when the instance is newer or equal', () => {
            expect(compatibilityWarning('2026.9.4', '2026.9.4')).toBeNull();
            expect(compatibilityWarning('2026.10.1', '2026.9.4')).toBeNull();
        });

        it('fails OPEN when the version is absent — never an error, never blocking', () => {
            expect(() => compatibilityWarning(null, '2026.9.4')).not.toThrow();
            expect(compatibilityWarning(null, '2026.9.4')).toBeNull();
        });

        it('fails OPEN when the version is unparseable', () => {
            expect(() => compatibilityWarning('not-a-version', '2026.9.4')).not.toThrow();
            expect(compatibilityWarning('not-a-version', '2026.9.4')).toBeNull();
        });
    });
});
