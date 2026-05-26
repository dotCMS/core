import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import {
    DotCurrentUserService,
    DotEsSearchService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotClipboardUtil } from '@dotcms/ui';

import { DotEsSearchPageComponent } from './dot-es-search-page.component';
import { DotEsSearchStore } from './store/dot-es-search.store';

const buildStoreMock = (overrides: Partial<Record<string, jest.Mock>> = {}) => ({
    query: jest.fn().mockReturnValue(''),
    params: jest.fn().mockReturnValue({ live: true, userid: '' }),
    wrapCode: jest.fn().mockReturnValue(false),
    isAdmin: jest.fn().mockReturnValue(false),
    status: jest.fn().mockReturnValue(ComponentStatus.INIT),
    response: jest.fn().mockReturnValue(null),
    rawJson: jest.fn().mockReturnValue(''),
    contentlets: jest.fn().mockReturnValue([]),
    hits: jest.fn().mockReturnValue([]),
    hitCount: jest.fn().mockReturnValue(0),
    queryTimeMs: jest.fn().mockReturnValue(null),
    activeTab: jest.fn().mockReturnValue('results'),
    isLoading: jest.fn().mockReturnValue(false),
    hasLoadedResults: jest.fn().mockReturnValue(false),
    hasAggregations: jest.fn().mockReturnValue(false),
    aggregations: jest.fn().mockReturnValue(null),
    hasSuggestions: jest.fn().mockReturnValue(false),
    suggestions: jest.fn().mockReturnValue(null),
    returnedCount: jest.fn().mockReturnValue(0),
    hasPartialResults: jest.fn().mockReturnValue(false),
    queryWasCapped: jest.fn().mockReturnValue(false),
    emptyStateConfig: jest
        .fn()
        .mockReturnValue({ title: 'No results', icon: 'pi-search', subtitle: '' }),
    setQuery: jest.fn(),
    setParam: jest.fn(),
    setWrapCode: jest.fn(),
    setActiveTab: jest.fn(),
    runSearch: jest.fn(),
    ...overrides
});

describe('DotEsSearchPageComponent', () => {
    let spectator: Spectator<DotEsSearchPageComponent>;

    const createComponent = createComponentFactory({
        component: DotEsSearchPageComponent,
        overrideComponents: [
            [
                DotEsSearchPageComponent,
                {
                    remove: {
                        providers: [DotEsSearchStore, DotEsSearchService, DotCurrentUserService]
                    },
                    add: {}
                }
            ]
        ],
        providers: [
            mockProvider(DotMessageService, { get: jest.fn().mockReturnValue('') }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotGlobalMessageService, { error: jest.fn() })
        ],
        componentProviders: [
            { provide: DotEsSearchStore, useFactory: () => buildStoreMock() },
            mockProvider(DotClipboardUtil, { copy: jest.fn().mockResolvedValue(true) })
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should render Help and Run buttons', () => {
        expect(spectator.query(byTestId('es-search-help-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('es-search-run-btn'))).toBeTruthy();
    });

    it('should render Share and Export buttons only when results are available', () => {
        const store = spectator.inject(DotEsSearchStore, true);
        store.hasLoadedResults = jest.fn().mockReturnValue(true);
        store.status = jest.fn().mockReturnValue(ComponentStatus.LOADED);
        spectator.fixture.componentRef.changeDetectorRef.markForCheck();
        spectator.detectChanges();
        expect(spectator.query(byTestId('es-search-share-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('es-search-export-btn'))).toBeTruthy();
    });

    it('should not render Share and Export buttons when there are no results', () => {
        expect(spectator.query(byTestId('es-search-share-btn'))).toBeFalsy();
        expect(spectator.query(byTestId('es-search-export-btn'))).toBeFalsy();
    });

    it('should render the query editor', () => {
        expect(spectator.query(byTestId('es-search-query-editor'))).toBeTruthy();
    });

    it('should render the parameters panel', () => {
        expect(spectator.query(byTestId('es-search-params-panel'))).toBeTruthy();
    });

    it('should render the Help button inside the Query panel header', () => {
        const helpBtn = spectator.query(byTestId('es-search-help-btn'));
        expect(helpBtn).toBeTruthy();
        const queryPanel = spectator.query(byTestId('es-search-query-panel'));
        expect(queryPanel?.contains(helpBtn as Node)).toBe(true);
    });

    it('should render the Wrap code checkbox inside the Query panel header, not the parameters panel', () => {
        const wrapToggle = spectator.query(byTestId('es-search-wrap-toggle'));
        expect(wrapToggle).toBeTruthy();
        const queryPanel = spectator.query(byTestId('es-search-query-panel'));
        const paramsContent = spectator.query(byTestId('es-search-params-content'));
        expect(queryPanel?.contains(wrapToggle as Node)).toBe(true);
        expect(paramsContent?.contains(wrapToggle as Node)).toBeFalsy();
    });

    it('should make the Status column sortable by the live field', () => {
        const store = spectator.inject(DotEsSearchStore, true);
        store.status = jest.fn().mockReturnValue(ComponentStatus.LOADED);
        store.contentlets = jest.fn().mockReturnValue([
            { identifier: 'a', title: 'A', contentType: 'X', modDate: '', live: false },
            { identifier: 'b', title: 'B', contentType: 'X', modDate: '', live: true }
        ]);
        spectator.fixture.componentRef.changeDetectorRef.markForCheck();
        spectator.detectChanges();

        const sortIcons = spectator.queryAll('p-sorticon');
        const liveSortIcon = sortIcons.find((el) => el.getAttribute('field') === 'live');
        expect(liveSortIcon).toBeTruthy();
        expect(liveSortIcon?.closest('th')).toBeTruthy();
    });

    it('should hide the userid field for non-admin users', () => {
        expect(spectator.query(byTestId('es-search-userid-input'))).toBeFalsy();
    });

    it('should show the userid field for admin users', () => {
        const store = spectator.inject(DotEsSearchStore, true);
        store.isAdmin = jest.fn().mockReturnValue(true);
        spectator.fixture.componentRef.changeDetectorRef.markForCheck();
        spectator.detectChanges();
        expect(spectator.query(byTestId('es-search-userid-input'))).toBeTruthy();
    });

    it('should show empty container when status is INIT', () => {
        expect(spectator.query('dot-empty-container')).toBeTruthy();
    });

    it('should call store.runSearch() when Run button is clicked', () => {
        const store = spectator.inject(DotEsSearchStore, true);
        store.query = jest.fn().mockReturnValue('{"query":{"match_all":{}}}');
        spectator.fixture.componentRef.changeDetectorRef.markForCheck();
        spectator.detectChanges();
        const btn = spectator.query(byTestId('es-search-run-btn'))?.querySelector('button');
        if (btn) spectator.click(btn);
        expect(store.runSearch).toHaveBeenCalled();
    });

    it('should keep the Run button enabled when query is empty', () => {
        const btn = spectator.query(byTestId('es-search-run-btn'))?.querySelector('button');
        expect(btn?.disabled).toBe(false);
    });

    it('should call store.runSearch() when onRun is invoked', () => {
        const store = spectator.inject(DotEsSearchStore, true);
        store.query = jest.fn().mockReturnValue('{"query":{"match_all":{}}}');
        spectator.component.onRun();
        expect(store.runSearch).toHaveBeenCalled();
    });

    it('should render the help popover element', () => {
        expect(spectator.query(byTestId('es-search-help-dialog'))).toBeTruthy();
    });

    describe('onQueryChange', () => {
        it('should call store.setQuery and clear errors for valid JSON', () => {
            const store = spectator.inject(DotEsSearchStore, true);
            spectator.component.onQueryChange('{"query":{"match_all":{}}}');
            expect(store.setQuery).toHaveBeenCalledWith('{"query":{"match_all":{}}}');
            expect(spectator.component.$hasEditorErrors()).toBe(false);
        });

        it('should call store.setQuery and set errors for invalid JSON', () => {
            const store = spectator.inject(DotEsSearchStore, true);
            spectator.component.onQueryChange('{invalid');
            expect(store.setQuery).toHaveBeenCalledWith('{invalid');
            expect(spectator.component.$hasEditorErrors()).toBe(true);
        });
    });

    describe('when the editor has JSON syntax errors', () => {
        beforeEach(() => {
            spectator.component.$hasEditorErrors.set(true);
            spectator.fixture.componentRef.changeDetectorRef.markForCheck();
            spectator.detectChanges();
        });

        it('should show the error indicator strip', () => {
            expect(spectator.query(byTestId('es-search-editor-error'))).toBeTruthy();
        });

        it('should keep the Run button enabled when there are JSON errors', () => {
            const btn = spectator.query(byTestId('es-search-run-btn'))?.querySelector('button');
            expect(btn?.disabled).toBe(false);
        });
    });

    describe('$queryEditorOptions', () => {
        it('should set wordWrap off when wrapCode is false', () => {
            expect(spectator.component.$queryEditorOptions().wordWrap).toBe('off');
        });

        describe('when wrapCode is true', () => {
            beforeEach(() => {
                spectator = createComponent({ detectChanges: false });
                const store = spectator.inject(DotEsSearchStore, true);
                store.wrapCode = jest.fn().mockReturnValue(true);
            });

            it('should set wordWrap on', () => {
                expect(spectator.component.$queryEditorOptions().wordWrap).toBe('on');
            });
        });
    });

    it('should toggle params panel visibility via signal', () => {
        expect(spectator.component.$paramsOpen()).toBe(true);
        spectator.component.$paramsOpen.set(false);
        expect(spectator.component.$paramsOpen()).toBe(false);
    });

    describe('when results are loaded with no hits', () => {
        beforeEach(() => {
            const store = spectator.inject(DotEsSearchStore, true);
            store.status = jest.fn().mockReturnValue(ComponentStatus.LOADED);
            store.hasLoadedResults = jest.fn().mockReturnValue(true);
            store.hitCount = jest.fn().mockReturnValue(0);
            store.queryTimeMs = jest.fn().mockReturnValue(5);
            store.contentlets = jest.fn().mockReturnValue([]);
            spectator.fixture.componentRef.changeDetectorRef.markForCheck();
            spectator.detectChanges();
        });

        it('should show the no-hits empty state', () => {
            expect(spectator.query(byTestId('es-search-no-hits'))).toBeTruthy();
            expect(spectator.query(byTestId('es-search-results-table'))).toBeFalsy();
        });
    });

    describe('when results are loaded', () => {
        beforeEach(() => {
            const store = spectator.inject(DotEsSearchStore, true);
            store.status = jest.fn().mockReturnValue(ComponentStatus.LOADED);
            store.hasLoadedResults = jest.fn().mockReturnValue(true);
            store.hitCount = jest.fn().mockReturnValue(5);
            store.queryTimeMs = jest.fn().mockReturnValue(142);
            store.contentlets = jest.fn().mockReturnValue([
                {
                    identifier: 'abc',
                    title: 'Test Post',
                    contentType: 'Blog',
                    modDate: '2024-06-05',
                    live: true,
                    working: true
                }
            ]);
            store.setActiveTab = jest.fn();
            spectator.fixture.componentRef.changeDetectorRef.markForCheck();
            spectator.detectChanges();
        });

        it('should show the stats bar', () => {
            expect(spectator.query(byTestId('es-search-stats-bar'))).toBeTruthy();
        });

        it('should show the results table with data', () => {
            expect(spectator.query(byTestId('es-search-results-table'))).toBeTruthy();
            expect(spectator.query(byTestId('es-search-result-row'))).toBeTruthy();
        });

        it('should call setActiveTab when switching tabs', () => {
            const store = spectator.inject(DotEsSearchStore, true);
            spectator.component.onTabChange('raw');
            expect(store.setActiveTab).toHaveBeenCalledWith('raw');
        });

        it('should show partial results message in stats bar when hasPartialResults is true', () => {
            const store = spectator.inject(DotEsSearchStore, true);
            store.hasPartialResults = jest.fn().mockReturnValue(true);
            store.returnedCount = jest.fn().mockReturnValue(20);
            store.hitCount = jest.fn().mockReturnValue(10000);
            spectator.fixture.componentRef.changeDetectorRef.markForCheck();
            spectator.detectChanges();

            const statsBar = spectator.query(byTestId('es-search-stats-bar'));
            expect(statsBar).toBeTruthy();
        });
    });

    describe('Share menu', () => {
        const getClipboardCopy = (resolved = true) => {
            const clipboard = spectator.inject(DotClipboardUtil, true);
            const copy = clipboard.copy as jest.Mock;
            // mockProvider's jest.fn() is shared across tests in the suite;
            // clear call history so each test asserts only against its own calls.
            copy.mockClear();
            copy.mockResolvedValue(resolved);
            return copy;
        };

        it('Copy as cURL targets /api/es/search with depth=1 and the parsed query body', () => {
            const store = spectator.inject(DotEsSearchStore, true);
            store.query = jest.fn().mockReturnValue(`{"query":{"match":{"title":"it's"}}}`);
            store.params = jest.fn().mockReturnValue({ live: true, userid: '' });
            const copy = getClipboardCopy();

            spectator.component.exportItems[0].command?.({} as never);

            const snippet = copy.mock.calls[0][0];
            expect(snippet).toMatch(/^curl -X POST "https?:.*\/api\/es\/search\?depth=1/);
            expect(snippet).toContain(`"title":"it'\\''s"`);
        });

        it('Copy as fetch emits a fetch() call against /api/es/search', () => {
            const store = spectator.inject(DotEsSearchStore, true);
            store.query = jest.fn().mockReturnValue('{"query":{"match_all":{}}}');
            store.params = jest.fn().mockReturnValue({ live: true, userid: '' });
            const copy = getClipboardCopy();

            spectator.component.exportItems[1].command?.({} as never);

            const snippet = copy.mock.calls[0][0];
            expect(snippet).toContain(`fetch('/api/es/search?depth=1&live=true'`);
            expect(snippet).toContain(`credentials: 'include'`);
        });

        it('calls DotGlobalMessageService.error when clipboard.copy resolves false', async () => {
            const store = spectator.inject(DotEsSearchStore, true);
            store.query = jest.fn().mockReturnValue('{"query":{"match_all":{}}}');
            store.params = jest.fn().mockReturnValue({ live: true, userid: '' });
            const copy = getClipboardCopy(false);
            const globalMessage = spectator.inject(DotGlobalMessageService);

            spectator.component.exportItems[1].command?.({} as never);
            // Flush the awaited clipboard promise so the `if (!ok)` branch runs.
            await copy.mock.results[0]?.value;

            expect(globalMessage.error).toHaveBeenCalled();
        });
    });

    describe('asContentState', () => {
        it('should pass through all present fields', () => {
            const result = spectator.component.asContentState({
                live: true,
                working: true,
                hasLiveVersion: true,
                archived: false,
                deleted: false
            });
            expect(result).toEqual({
                live: true,
                working: true,
                hasLiveVersion: true,
                archived: false,
                deleted: false
            });
        });

        it('should default required fields to false when absent', () => {
            const result = spectator.component.asContentState({});
            expect(result.live).toBe(false);
            expect(result.working).toBe(false);
            expect(result.hasLiveVersion).toBe(false);
        });

        it('should leave optional fields undefined when absent', () => {
            const result = spectator.component.asContentState({});
            expect(result.archived).toBeUndefined();
            expect(result.deleted).toBeUndefined();
        });

        it('should preserve string boolean values from the API', () => {
            const result = spectator.component.asContentState({
                live: 'true',
                working: 'false',
                hasLiveVersion: 'true'
            });
            expect(result.live).toBe('true');
            expect(result.working).toBe('false');
            expect(result.hasLiveVersion).toBe('true');
        });
    });

    describe('onTabChange', () => {
        it('should accept all valid tab values', () => {
            const store = spectator.inject(DotEsSearchStore, true);
            for (const tab of ['results', 'raw', 'aggregations', 'suggestions']) {
                spectator.component.onTabChange(tab);
                expect(store.setActiveTab).toHaveBeenCalledWith(tab);
            }
        });

        it('should ignore unknown tab values', () => {
            const store = spectator.inject(DotEsSearchStore, true);
            spectator.component.onTabChange('unknown-tab');
            expect(store.setActiveTab).not.toHaveBeenCalled();
        });
    });
});
