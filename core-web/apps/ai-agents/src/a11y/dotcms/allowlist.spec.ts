import type { Adapter, AdapterMethod } from '@dotcms/ai/sandbox';

import { DisallowedRequestError, isAllowed, withAllowlist } from './allowlist';

describe('a11y path allowlist (plan §3 stage B)', () => {
    describe('isAllowed — the four loop operations', () => {
        it('permits SCAN (POST page-scanner/a11y/check)', () => {
            expect(isAllowed('POST', '/api/v1/page-scanner/a11y/check')).toBe(true);
        });

        it('permits LOCATE (GET _render-sources/{uri}) by prefix', () => {
            expect(isAllowed('GET', '/api/v1/page/_render-sources/index')).toBe(true);
            expect(isAllowed('GET', '/api/v1/page/_render-sources/about-us/team')).toBe(true);
        });

        it('permits READ (GET /api/v2/assets)', () => {
            expect(isAllowed('GET', '/api/v2/assets')).toBe(true);
        });

        it('permits SAVE-WORKING (PUT /api/v2/assets/save)', () => {
            expect(isAllowed('PUT', '/api/v2/assets/save')).toBe(true);
        });

        it('is case-insensitive on method and ignores an appended query string', () => {
            expect(isAllowed('get', '/api/v2/assets?path=//demo/x.vtl')).toBe(true);
        });
    });

    describe('isAllowed — hard rejections', () => {
        // The DoD rejection: publish must never be expressible.
        it('rejects PUT /api/v2/assets/publish (the publish path)', () => {
            expect(isAllowed('PUT', '/api/v2/assets/publish')).toBe(false);
        });

        it('rejects POST /api/v2/assets/publish', () => {
            expect(isAllowed('POST', '/api/v2/assets/publish')).toBe(false);
        });

        it('rejects DELETE on an allowed path (wrong method)', () => {
            expect(isAllowed('DELETE', '/api/v2/assets')).toBe(false);
        });

        it('rejects /api/v2/assets/delete (exact-match guards against prefix sneak)', () => {
            expect(isAllowed('PUT', '/api/v2/assets/delete')).toBe(false);
        });

        it('rejects POST to /api/v2/assets (save must use the /save path, not the base)', () => {
            expect(isAllowed('POST', '/api/v2/assets')).toBe(false);
        });

        it('rejects workflow and config endpoints', () => {
            expect(isAllowed('PUT', '/api/v1/workflow/actions/fire')).toBe(false);
            expect(isAllowed('GET', '/api/v1/configuration')).toBe(false);
        });

        it('rejects POST to a non-scanner path', () => {
            expect(isAllowed('POST', '/api/v1/page-scanner/a11y/check/evil')).toBe(false);
        });
    });

    describe('withAllowlist — wrapping a real adapter shape', () => {
        // A fake "api" adapter whose request.execute stands in for the
        // token-bearing inner. We assert it is/ isn't reached.
        const makeAdapter = (execute: AdapterMethod['execute']): Adapter => ({
            name: 'api',
            description: 'fake',
            version: '1.0.0',
            methods: new Map([
                [
                    'request',
                    {
                        name: 'request',
                        parameters: [],
                        execute
                    }
                ]
            ])
        });

        it('delegates allowed calls to the inner execute', async () => {
            const inner = jest.fn().mockResolvedValue({ ok: true });
            const guarded = withAllowlist(makeAdapter(inner));

            const result = await guarded.methods
                .get('request')!
                .execute({
                    method: 'GET',
                    path: '/api/v2/assets',
                    query: { path: '//demo/x.vtl' }
                });

            expect(result).toEqual({ ok: true });
            expect(inner).toHaveBeenCalledTimes(1);
        });

        it('throws before reaching the inner (token never used) on a disallowed call', async () => {
            const inner = jest.fn().mockResolvedValue({ ok: true });
            const guarded = withAllowlist(makeAdapter(inner));

            await expect(
                guarded.methods
                    .get('request')!
                    .execute({ method: 'PUT', path: '/api/v2/assets/publish' })
            ).rejects.toBeInstanceOf(DisallowedRequestError);

            expect(inner).not.toHaveBeenCalled();
        });

        it('preserves the adapter shape (name + request method)', () => {
            const guarded = withAllowlist(makeAdapter(jest.fn()));
            expect(guarded.name).toBe('api');
            expect(guarded.methods.has('request')).toBe(true);
        });

        it('throws if the adapter has no request method', () => {
            const bad: Adapter = {
                name: 'api',
                version: '1.0.0',
                methods: new Map()
            };
            expect(() => withAllowlist(bad)).toThrow();
        });
    });
});
