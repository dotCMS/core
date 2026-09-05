import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { LoggerService } from '@dotcms/dotcms-js';
import { DotAiSearchResult } from '@dotcms/dotcms-models';
import { DotFormatDateService } from '@dotcms/ui';

import DotAiSearchComponent from './dot-ai-search.component';

import { DotAiStore } from '../../store/dot-ai.store';

const result = (overrides: Partial<DotAiSearchResult> = {}): DotAiSearchResult => ({
    identifier: 'id-1',
    inode: 'inode-1',
    title: 'Ecotourism in Costa Rica',
    contentType: 'Blog',
    modDate: '2026-01-01',
    matches: [{ distance: 0.2, extractedText: 'a matching passage' }],
    ...overrides
});

/**
 * Four states, explicitly (FR-054). On this screen "nothing matched" and "you have not
 * searched yet" mean very different things, so they are separate branches rather than one
 * shared empty state.
 */
describe('DotAiSearchComponent', () => {
    let spectator: Spectator<DotAiSearchComponent>;

    const storeMock = {
        searchPrompt: jest.fn().mockReturnValue(''),
        searchResponse: jest.fn().mockReturnValue(null),
        searchResults: jest.fn().mockReturnValue([]),
        searchMissingIndex: jest.fn().mockReturnValue(null),
        isSearching: jest.fn().mockReturnValue(false),
        hasSearched: jest.fn().mockReturnValue(false),
        isConfigured: jest.fn().mockReturnValue(true),
        setSearchPrompt: jest.fn(),
        runSearch: jest.fn(),
        // Read by the settings panel, which is a real child of this component.
        indexesForbidden: jest.fn().mockReturnValue(false),
        indexOptions: jest.fn().mockReturnValue([]),
        chatModels: jest.fn().mockReturnValue([]),
        settingsIndexName: jest.fn().mockReturnValue('default'),
        settingsThreshold: jest.fn().mockReturnValue(0.25),
        settingsOperator: jest.fn().mockReturnValue('cosine'),
        settingsModel: jest.fn().mockReturnValue(''),
        settingsTemperature: jest.fn().mockReturnValue(0),
        settingsResponseLength: jest.fn().mockReturnValue(1024),
        settingsContentTypes: jest.fn().mockReturnValue(''),
        settingsSite: jest.fn().mockReturnValue(null),
        setSettings: jest.fn()
    };

    const createComponent = createComponentFactory({
        component: DotAiSearchComponent,
        componentProviders: [{ provide: DotAiStore, useValue: storeMock }],
        // DotRelativeDatePipe pulls DotFormatDateService -> DotcmsConfigService -> LoggerService;
        // mocking the two the pipe actually injects stops that chain at the boundary.
        providers: [
            mockProvider(DotFormatDateService),
            mockProvider(DotMessageService),
            // LoggerService is the leaf of the chain the date pipe drags in; mocking the
            // root-provided services above it does not intercept, so break it here.
            mockProvider(LoggerService)
        ],
        shallow: true
    });

    beforeEach(() => {
        jest.clearAllMocks();
        storeMock.searchPrompt.mockReturnValue('');
        storeMock.searchResponse.mockReturnValue(null);
        storeMock.searchResults.mockReturnValue([]);
        storeMock.searchMissingIndex.mockReturnValue(null);
        storeMock.isSearching.mockReturnValue(false);
        storeMock.hasSearched.mockReturnValue(false);
        storeMock.isConfigured.mockReturnValue(true);
    });

    it('should show the first-run state before any search', () => {
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-search-empty-first-run'))).toBeTruthy();
        expect(spectator.query(byTestId('dotai-search-results'))).toBeFalsy();
    });

    it('should show a loading state while searching', () => {
        storeMock.isSearching.mockReturnValue(true);
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-search-loading'))).toBeTruthy();
    });

    it('should distinguish "nothing matched" from "not searched yet"', () => {
        storeMock.hasSearched.mockReturnValue(true);
        storeMock.searchResults.mockReturnValue([]);
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-search-no-results'))).toBeTruthy();
        expect(spectator.query(byTestId('dotai-search-empty-first-run'))).toBeFalsy();
    });

    it('should render results with their closeness and match count', () => {
        storeMock.hasSearched.mockReturnValue(true);
        storeMock.searchResults.mockReturnValue([result()]);
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-search-results'))).toBeTruthy();
        expect(spectator.query(byTestId('dotai-search-result-title'))).toHaveText(
            'Ecotourism in Costa Rica'
        );
        expect(spectator.query(byTestId('dotai-search-result-distance'))).toHaveText('0.20');
    });

    it('should drop the date and its separator when the server omits modDate', () => {
        storeMock.hasSearched.mockReturnValue(true);
        storeMock.searchResults.mockReturnValue([result({ modDate: undefined })]);
        spectator = createComponent();

        // The separator is only rendered alongside a date, so it must not dangle.
        const meta = spectator.query(byTestId('dotai-search-results'))?.textContent ?? '';
        expect(meta).not.toContain('·\n');
    });

    it('should name the missing index rather than failing generically', () => {
        storeMock.hasSearched.mockReturnValue(true);
        storeMock.searchMissingIndex.mockReturnValue('gone');
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-search-missing-index'))).toBeTruthy();
    });

    it('should not search on an empty prompt', () => {
        spectator = createComponent();

        spectator.click(spectator.query(byTestId('dotai-search-submit')) as Element);

        expect(storeMock.runSearch).not.toHaveBeenCalled();
    });

    it('should disable the input and submit while unconfigured (FR-047)', () => {
        storeMock.isConfigured.mockReturnValue(false);
        storeMock.searchPrompt.mockReturnValue('a question');
        spectator = createComponent();

        // Angular's [disabled] sets the DOM property, not the attribute.
        expect((spectator.query(byTestId('dotai-search-input')) as HTMLInputElement).disabled).toBe(
            true
        );
        spectator.click(spectator.query(byTestId('dotai-search-submit')) as Element);
        expect(storeMock.runSearch).not.toHaveBeenCalled();
    });

    describe('closeness bar', () => {
        it('should handle the negative distances inner product produces', () => {
            // Measured around -0.33 against a live index. A naive 0..1 bar renders empty.
            storeMock.hasSearched.mockReturnValue(true);
            storeMock.searchResults.mockReturnValue([
                result({ matches: [{ distance: -0.33, extractedText: 'x' }] })
            ]);
            spectator = createComponent();

            // The normalisation itself is covered in dot-ai-distance.utils.spec.ts; here we
            // only care that the raw negative value still reaches the row unmangled.
            expect(spectator.query(byTestId('dotai-search-result-distance'))).toHaveText('-0.33');
        });
    });
});
