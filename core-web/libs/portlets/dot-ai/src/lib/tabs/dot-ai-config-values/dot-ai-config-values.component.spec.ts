import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { DotAiResolvedConfig } from '@dotcms/dotcms-models';

import DotAiConfigValuesComponent from './dot-ai-config-values.component';

import { DotAiStore } from '../../store/dot-ai.store';

const resolved = (overrides: Partial<DotAiResolvedConfig> = {}): DotAiResolvedConfig => ({
    configHost: 'demo.dotcms.com (falls back to system host)',
    settings: { temperature: '0.7', debugLogging: 'false' },
    providerConfig: { chat: { apiKey: '*****', temperature: '0.7' } },
    chatModels: [],
    isConfigured: true,
    redactionFailed: false,
    ...overrides
});

describe('DotAiConfigValuesComponent', () => {
    let spectator: Spectator<DotAiConfigValuesComponent>;

    const storeMock = {
        resolvedConfig: jest.fn().mockReturnValue(resolved()),
        redactionFailed: jest.fn().mockReturnValue(false),
        isConfigured: jest.fn().mockReturnValue(true)
    };

    const createComponent = createComponentFactory({
        component: DotAiConfigValuesComponent,
        componentProviders: [{ provide: DotAiStore, useValue: storeMock }],
        providers: [mockProvider(DotMessageService)],
        shallow: true
    });

    beforeEach(() => {
        jest.clearAllMocks();
        storeMock.resolvedConfig.mockReturnValue(resolved());
        storeMock.redactionFailed.mockReturnValue(false);
        spectator = createComponent();
    });

    it('should render a row per resolved setting', () => {
        expect(spectator.queryAll(byTestId('dotai-config-row')).length).toBeGreaterThan(0);
    });

    it('should render configHost verbatim, since the server sends a display string', () => {
        expect(spectator.query(byTestId('dotai-config-host'))).toContainText(
            'falls back to system host'
        );
    });

    it('should never render the server mask or a real credential (FR-042)', () => {
        const text = spectator.query(byTestId('dotai-config-table'))?.textContent ?? '';

        expect(text).toContain('••••••••');
        expect(text).not.toContain('*****');
    });

    it('should say so when redaction failed instead of rendering the sentinel (FR-046)', () => {
        storeMock.redactionFailed.mockReturnValue(true);
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-config-redaction-failed'))).toBeTruthy();
        expect(spectator.query(byTestId('dotai-config-table'))).toBeFalsy();
    });
});
