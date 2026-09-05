import { DotAiResolvedConfig } from '@dotcms/dotcms-models';

import { DOT_AI_CONFIG_SOURCE, SECRET_MASK, toConfigRows } from './dot-ai-config.utils';

const config = (overrides: Partial<DotAiResolvedConfig> = {}): DotAiResolvedConfig => ({
    configHost: 'demo.dotcms.com (falls back to system host)',
    settings: { temperature: '0.7', imageSize: '1024x1024' },
    providerConfig: { chat: { provider: 'openrouter', apiKey: '*****', temperature: '0.7' } },
    chatModels: [],
    isConfigured: true,
    redactionFailed: false,
    ...overrides
});

describe('toConfigRows', () => {
    it('should mark an explicitly set value as App Config', () => {
        const rows = toConfigRows(config());

        expect(rows.find((r) => r.key === 'temperature')?.source).toBe(
            DOT_AI_CONFIG_SOURCE.APP_CONFIG
        );
    });

    it('should mark an unset value as Default', () => {
        const rows = toConfigRows(config());

        expect(rows.find((r) => r.key === 'imageSize')?.source).toBe(DOT_AI_CONFIG_SOURCE.DEFAULT);
    });

    it('should add a masked Secret row per credential field', () => {
        const rows = toConfigRows(config());
        const secret = rows.find((r) => r.key === 'chat.apiKey');

        expect(secret?.source).toBe(DOT_AI_CONFIG_SOURCE.SECRET);
        expect(secret?.value).toBe(SECRET_MASK);
    });

    it('should never echo the server mask or the stored value (FR-042)', () => {
        const rows = toConfigRows(config());

        expect(rows.every((r) => r.value !== '*****')).toBe(true);
    });

    it('should treat a blank provider value as not explicitly set', () => {
        const rows = toConfigRows(config({ providerConfig: { chat: { temperature: '   ' } } }));

        expect(rows.find((r) => r.key === 'temperature')?.source).toBe(
            DOT_AI_CONFIG_SOURCE.DEFAULT
        );
    });

    it('should return nothing for a null config rather than throwing', () => {
        expect(toConfigRows(null)).toEqual([]);
    });

    it('should list rows alphabetically so a key is findable', () => {
        const keys = toConfigRows(config()).map((r) => r.key);

        expect(keys).toEqual([...keys].sort((a, b) => a.localeCompare(b)));
    });
});
