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

    describe('the provider JSON view', () => {
        const viewProviderButton = (): HTMLButtonElement | null =>
            spectator.query(byTestId('dotai-config-view-provider'))?.querySelector('button') ??
            null;

        it('should mask credentials rather than printing the server mask (FR-042)', () => {
            spectator.click(viewProviderButton() as HTMLButtonElement);
            const json = spectator.query(byTestId('dotai-config-provider-json'))?.textContent ?? '';

            expect(json).toContain('••••••••');
            expect(json).not.toContain('*****');
        });

        it('should not be offered when redaction failed', () => {
            // providerConfig is null there while isConfigured stays true, so the dialog would
            // open on `{}` — which reads as "no provider configuration" rather than "withheld".
            storeMock.redactionFailed.mockReturnValue(true);
            spectator = createComponent();

            expect(viewProviderButton()?.hasAttribute('disabled')).toBe(true);
        });
    });

    describe('the filter bar', () => {
        // jsdom does no layout, so these assert the class contract; the geometry it produces
        // was measured in the browser (280px filter, one line box on the button).
        it('should cap the filter instead of letting it eat the bar', () => {
            // dot-search-input's host is `block w-full`, so uncapped it took 930px of an
            // 1100px bar and squeezed everything after it.
            const filter = spectator.query(byTestId('dotai-config-filter')) as HTMLElement;

            expect(filter.className).toContain('max-w-xs');
        });

        it('should keep the filter first, so it reads as sitting at the left', () => {
            const filter = spectator.query(byTestId('dotai-config-filter')) as HTMLElement;
            const host = spectator.query(byTestId('dotai-config-host')) as HTMLElement;

            expect(
                filter.compareDocumentPosition(host) & Node.DOCUMENT_POSITION_FOLLOWING
            ).toBeTruthy();
            // The host line takes the slack; that is what holds the filter left and the
            // action right without either being positioned.
            expect(host.className).toContain('flex-1');
        });

        it('should not let the view-provider label wrap', () => {
            // PrimeNG sets no white-space on the button or its label, so a squeezed button
            // broke "View provider config" across two lines — measured at 1100px, not just
            // at narrow widths.
            const button = spectator.query(byTestId('dotai-config-view-provider')) as HTMLElement;

            expect(button.className).toContain('whitespace-nowrap');
            expect(button.className).toContain('shrink-0');
        });
    });
});
