import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import {
    DotEsSearchService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotEsSearchPageComponent } from './dot-es-search-page.component';
import { DotEsSearchStore } from './store/dot-es-search.store';

const buildStoreMock = (overrides: Partial<Record<string, jest.Mock>> = {}) => ({
    query: jest.fn().mockReturnValue(''),
    params: jest
        .fn()
        .mockReturnValue({
            live: true,
            depth: 1,
            allCategoriesInfo: false,
            userid: '',
            wrapCode: false
        }),
    status: jest.fn().mockReturnValue(ComponentStatus.INIT),
    response: jest.fn().mockReturnValue(null),
    rawResponse: jest.fn().mockReturnValue(null),
    rawJson: jest.fn().mockReturnValue(''),
    hits: jest.fn().mockReturnValue([]),
    hitCount: jest.fn().mockReturnValue(0),
    aggregations: jest.fn().mockReturnValue(null),
    suggestions: jest.fn().mockReturnValue(null),
    queryTimeMs: jest.fn().mockReturnValue(null),
    activeTab: jest.fn().mockReturnValue('results'),
    isLoading: jest.fn().mockReturnValue(false),
    hasResults: jest.fn().mockReturnValue(false),
    emptyStateConfig: jest
        .fn()
        .mockReturnValue({ title: 'No results', icon: 'pi-search', subtitle: '' }),
    setQuery: jest.fn(),
    setParam: jest.fn(),
    setActiveTab: jest.fn(),
    runSearch: jest.fn(),
    loadRaw: jest.fn(),
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
                    remove: { providers: [DotEsSearchStore, DotEsSearchService] },
                    add: {}
                }
            ]
        ],
        providers: [
            mockProvider(DotMessageService, { get: jest.fn().mockReturnValue('') }),
            mockProvider(DotHttpErrorManagerService)
        ],
        componentProviders: [{ provide: DotEsSearchStore, useValue: buildStoreMock() }]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should render the toolbar with Help, Export and Run buttons', () => {
        expect(spectator.query(byTestId('es-search-help-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('es-search-export-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('es-search-run-btn'))).toBeTruthy();
    });

    it('should render the query editor', () => {
        expect(spectator.query(byTestId('es-search-query-editor'))).toBeTruthy();
    });

    it('should render the parameters panel toggle', () => {
        expect(spectator.query(byTestId('es-search-params-toggle'))).toBeTruthy();
    });

    it('should show empty container when status is INIT', () => {
        expect(spectator.query('dot-empty-container')).toBeTruthy();
    });

    it('should call store.runSearch() when Run button is clicked', () => {
        const store = spectator.inject(DotEsSearchStore, true);
        const btn = spectator.query(byTestId('es-search-run-btn'))?.querySelector('button');
        if (btn) spectator.click(btn);
        expect(store.runSearch).toHaveBeenCalled();
    });

    it('should reset active tab to results when Run is clicked', () => {
        const store = spectator.inject(DotEsSearchStore, true);
        spectator.component.onRun();
        expect(store.setActiveTab).toHaveBeenCalledWith('results');
    });

    it('should render the help popover element', () => {
        expect(spectator.query(byTestId('es-search-help-dialog'))).toBeTruthy();
    });

    it('should toggle params panel visibility', () => {
        expect(spectator.component.paramsOpen()).toBe(true);
        spectator.click(byTestId('es-search-params-toggle'));
        expect(spectator.component.paramsOpen()).toBe(false);
    });

    describe('when results are loaded', () => {
        beforeEach(() => {
            const store = spectator.inject(DotEsSearchStore, true);
            store.status = jest.fn().mockReturnValue(ComponentStatus.LOADED);
            store.hasResults = jest.fn().mockReturnValue(true);
            store.hitCount = jest.fn().mockReturnValue(5);
            store.queryTimeMs = jest.fn().mockReturnValue(142);
            store.hits = jest
                .fn()
                .mockReturnValue([
                    {
                        _id: 'abc',
                        _index: 'live',
                        _type: 'content',
                        _score: 1,
                        _source: { title: 'Test Post', contentType: 'Blog' }
                    }
                ]);
            store.setActiveTab = jest.fn();
            store.loadRaw = jest.fn();
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

        it('should call loadRaw when switching to raw tab', () => {
            const store = spectator.inject(DotEsSearchStore, true);
            spectator.component.onTabChange('raw');
            expect(store.setActiveTab).toHaveBeenCalledWith('raw');
            expect(store.loadRaw).toHaveBeenCalled();
        });

        it('should NOT call loadRaw again if rawResponse already exists', () => {
            const store = spectator.inject(DotEsSearchStore, true);
            store.rawResponse = jest
                .fn()
                .mockReturnValue({
                    hits: { total: 1, hits: [] },
                    took: 10,
                    timed_out: false,
                    _shards: { total: 5, successful: 5, skipped: 0, failed: 0 }
                });
            store.loadRaw = jest.fn();
            spectator.component.onTabChange('raw');
            expect(store.loadRaw).not.toHaveBeenCalled();
        });
    });
});
