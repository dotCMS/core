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
    });

    describe('checkReachable (FR-005)', () => {
        afterEach(() => { jest.restoreAllMocks(); });

        it('uses /api/v1/appconfiguration, not /probes/alive', async () => {
            const fetchMock = jest
                .spyOn(globalThis, 'fetch')
                .mockResolvedValue(new Response(JSON.stringify({ entity: {} }), { status: 200 }));
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
