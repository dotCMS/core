import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

import { DotAiResolvedConfig } from '@dotcms/dotcms-models';

import { DotAiConfigService } from './dot-ai-config.service';
import { AI_API_ENDPOINT } from './dot-ai.constants';

/**
 * The `getResolvedConfig` cases below are the ones that matter. Everything they assert was
 * verified against a live instance while planning:
 *   - `providerConfig` comes back as a JSON **string**, and is **omitted entirely** when blank
 *   - `chat.model` is a comma-separated fallback list whose first entry is the default
 *   - credentials are already redacted to `"*****"` server-side
 *   - a redaction failure returns a literal sentinel that is not JSON (note the em dash)
 */
describe('DotAiConfigService', () => {
    let spectator: SpectatorHttp<DotAiConfigService>;

    const createHttp = createHttpFactory(DotAiConfigService);

    const CONFIG_URL = `${AI_API_ENDPOINT}/completions/config`;

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('getConfig / saveConfig', () => {
        it('should GET the config without params when no siteId is given', () => {
            spectator.service.getConfig().subscribe();
            spectator.expectOne(CONFIG_URL, HttpMethod.GET);
        });

        it('should GET the config with a siteId param', () => {
            spectator.service.getConfig('site-1').subscribe();
            spectator.expectOne(`${CONFIG_URL}?siteId=site-1`, HttpMethod.GET);
        });

        it('should PUT the raw json body on save', () => {
            const json = '{"chat":{"provider":"openai"}}';
            spectator.service.saveConfig(json).subscribe();

            const req = spectator.expectOne(CONFIG_URL, HttpMethod.PUT);
            expect(req.request.body).toBe(json);
        });
    });

    describe('getProviders', () => {
        it('should unwrap the entity envelope', () => {
            let result: unknown;
            spectator.service.getProviders().subscribe((r) => (result = r));

            spectator
                .expectOne(`${AI_API_ENDPOINT}/providers`, HttpMethod.GET)
                .flush({ entity: [{ provider: 'openai' }] });

            expect(result).toEqual([{ provider: 'openai' }]);
        });
    });

    describe('testConnection', () => {
        it('should POST to the capability path and unwrap the entity', () => {
            let result: unknown;
            spectator.service
                .testConnection('chat', { provider: 'openai' }, 'site-1')
                .subscribe((r) => (result = r));

            const req = spectator.expectOne(
                `${AI_API_ENDPOINT}/providers/test/chat?siteId=site-1`,
                HttpMethod.POST
            );
            req.flush({ entity: { success: true } });

            expect(result).toEqual({ success: true });
        });
    });

    describe('checkPluginInstallation', () => {
        it('should be true when providerConfig is present', () => {
            let result: boolean | undefined;
            spectator.service.checkPluginInstallation().subscribe((r) => (result = r));

            spectator.expectOne(CONFIG_URL, HttpMethod.GET).flush({ providerConfig: '{}' });

            expect(result).toBe(true);
        });

        it('should be false when providerConfig is absent', () => {
            let result: boolean | undefined;
            spectator.service.checkPluginInstallation().subscribe((r) => (result = r));

            spectator.expectOne(CONFIG_URL, HttpMethod.GET).flush({ configHost: 'demo' });

            expect(result).toBe(false);
        });

        it('should be false and not throw when the request errors', () => {
            let result: boolean | undefined;
            spectator.service.checkPluginInstallation().subscribe((r) => (result = r));

            spectator
                .expectOne(CONFIG_URL, HttpMethod.GET)
                .flush(null, { status: 500, statusText: 'Server Error' });

            expect(result).toBe(false);
        });
    });

    describe('getResolvedConfig', () => {
        const flush = (body: unknown) => {
            spectator.expectOne(CONFIG_URL, HttpMethod.GET).flush(body);
        };

        it('should parse providerConfig and split chat.model into an ordered model list', () => {
            let result: DotAiResolvedConfig;
            spectator.service.getResolvedConfig().subscribe((r) => (result = r));

            flush({
                configHost: 'demo.dotcms.com (falls back to system host)',
                settings: { temperature: '0.7' },
                providerConfig: JSON.stringify({
                    chat: { provider: 'openrouter', apiKey: '*****', model: 'a,b,c' }
                })
            });

            expect(result.chatModels).toEqual(['a', 'b', 'c']);
            expect(result.chatModels[0]).toBe('a');
            expect(result.isConfigured).toBe(true);
            expect(result.redactionFailed).toBe(false);
            expect(result.settings).toEqual({ temperature: '0.7' });
        });

        it('should keep configHost verbatim, since the server returns a display string', () => {
            let result: DotAiResolvedConfig;
            spectator.service.getResolvedConfig().subscribe((r) => (result = r));

            flush({
                configHost: 'demo.dotcms.com (falls back to system host)',
                settings: {},
                providerConfig: '{}'
            });

            expect(result.configHost).toBe('demo.dotcms.com (falls back to system host)');
        });

        it('should report isConfigured false when providerConfig is omitted', () => {
            let result: DotAiResolvedConfig;
            spectator.service.getResolvedConfig().subscribe((r) => (result = r));

            flush({ configHost: 'demo', settings: {} });

            expect(result.isConfigured).toBe(false);
            expect(result.providerConfig).toBeNull();
            expect(result.chatModels).toEqual([]);
        });

        it('should return an empty model list and not throw on malformed JSON', () => {
            let result: DotAiResolvedConfig;
            let errored = false;
            spectator.service
                .getResolvedConfig()
                .subscribe({ next: (r) => (result = r), error: () => (errored = true) });

            flush({ configHost: 'demo', settings: {}, providerConfig: '{not json' });

            expect(errored).toBe(false);
            expect(result.chatModels).toEqual([]);
            expect(result.providerConfig).toBeNull();
        });

        it('should flag the redaction-failed sentinel instead of parsing it', () => {
            let result: DotAiResolvedConfig;
            let errored = false;
            spectator.service
                .getResolvedConfig()
                .subscribe({ next: (r) => (result = r), error: () => (errored = true) });

            // Literal sentinel from CompletionsResource — note the em dash, not a hyphen.
            flush({
                configHost: 'demo',
                settings: {},
                providerConfig: '[CONFIG PRESENT — REDACTION FAILED]'
            });

            expect(errored).toBe(false);
            expect(result.redactionFailed).toBe(true);
            expect(result.providerConfig).toBeNull();
            expect(result.chatModels).toEqual([]);
        });

        it('should trim whitespace around comma-separated models', () => {
            let result: DotAiResolvedConfig;
            spectator.service.getResolvedConfig().subscribe((r) => (result = r));

            flush({
                configHost: 'demo',
                settings: {},
                providerConfig: JSON.stringify({ chat: { model: ' a , b ' } })
            });

            expect(result.chatModels).toEqual(['a', 'b']);
        });
    });
});
