import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { DotMessageService } from '@dotcms/data-access';
import { DotAiResolvedConfig } from '@dotcms/dotcms-models';

import DotAiConfigValuesComponent from './dot-ai-config-values.component';

import { DotAiStore } from '../../store/dot-ai.store';

const resolved = (overrides: Partial<DotAiResolvedConfig> = {}): DotAiResolvedConfig => ({
    configHost: 'demo.dotcms.com',
    configHostInherited: false,
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
        resolvedConfig: vi.fn().mockReturnValue(resolved()),
        redactionFailed: vi.fn().mockReturnValue(false),
        isConfigured: vi.fn().mockReturnValue(true)
    };

    const createComponent = createComponentFactory({
        component: DotAiConfigValuesComponent,
        componentProviders: [{ provide: DotAiStore, useValue: storeMock }],
        providers: [mockProvider(DotMessageService)],
        shallow: true
    });

    beforeEach(() => {
        vi.clearAllMocks();
        storeMock.resolvedConfig.mockReturnValue(resolved());
        storeMock.redactionFailed.mockReturnValue(false);
        spectator = createComponent();
    });

    it('should render a row per resolved setting', () => {
        expect(spectator.queryAll(byTestId('dotai-config-row')).length).toBeGreaterThan(0);
    });

    it('should label the host from a message key rather than a server-built sentence', () => {
        const messageService = spectator.inject(DotMessageService, true);

        expect(messageService.get).toHaveBeenCalledWith('dotai.config.host', 'demo.dotcms.com');
    });

    it('should say when the configuration was inherited from System Host', () => {
        // Two different facts: which site is on screen, and whose settings those are. The old
        // server-built string claimed the fallback unconditionally.
        storeMock.resolvedConfig.mockReturnValue(resolved({ configHostInherited: true }));
        spectator = createComponent();

        expect(spectator.inject(DotMessageService, true).get).toHaveBeenCalledWith(
            'dotai.config.host.inherited',
            'demo.dotcms.com'
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

    it('should not offer a provider config view at all', () => {
        // Removed on review: the raw JSON dump was not something the screen needed to carry.
        expect(spectator.query(byTestId('dotai-config-view-provider'))).toBeFalsy();
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
    });
});
