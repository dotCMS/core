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
    params: jest.fn().mockReturnValue({
        live: true,
        userid: '',
        wrapCode: false
    }),
    status: jest.fn().mockReturnValue(ComponentStatus.INIT),
    response: jest.fn().mockReturnValue(null),
    rawJson: jest.fn().mockReturnValue(''),
    contentlets: jest.fn().mockReturnValue([]),
    hits: jest.fn().mockReturnValue([]),
    hitCount: jest.fn().mockReturnValue(0),
    queryTimeMs: jest.fn().mockReturnValue(null),
    activeTab: jest.fn().mockReturnValue('results'),
    isLoading: jest.fn().mockReturnValue(false),
    hasResults: jest.fn().mockReturnValue(false),
    hasAggregations: jest.fn().mockReturnValue(false),
    aggregations: jest.fn().mockReturnValue(null),
    hasSuggestions: jest.fn().mockReturnValue(false),
    suggestions: jest.fn().mockReturnValue(null),
    emptyStateConfig: jest
        .fn()
        .mockReturnValue({ title: 'No results', icon: 'pi-search', subtitle: '' }),
    setQuery: jest.fn(),
    setParam: jest.fn(),
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

    it('should render the parameters panel', () => {
        expect(spectator.query(byTestId('es-search-params-panel'))).toBeTruthy();
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

    it('should call store.runSearch() when onRun is invoked', () => {
        const store = spectator.inject(DotEsSearchStore, true);
        spectator.component.onRun();
        expect(store.runSearch).toHaveBeenCalled();
    });

    it('should render the help popover element', () => {
        expect(spectator.query(byTestId('es-search-help-dialog'))).toBeTruthy();
    });

    describe('when the editor has JSON syntax errors', () => {
        beforeEach(() => {
            spectator.component.hasEditorErrors.set(true);
            spectator.fixture.componentRef.changeDetectorRef.markForCheck();
            spectator.detectChanges();
        });

        it('should show the error indicator strip', () => {
            expect(spectator.query(byTestId('es-search-editor-error'))).toBeTruthy();
        });

        it('should disable the Run button', () => {
            const btn = spectator.query(byTestId('es-search-run-btn'))?.querySelector('button');
            expect(btn?.disabled).toBe(true);
        });
    });

    it('should toggle params panel visibility via signal', () => {
        expect(spectator.component.paramsOpen()).toBe(true);
        spectator.component.paramsOpen.set(false);
        expect(spectator.component.paramsOpen()).toBe(false);
    });

    describe('when results are loaded with no hits', () => {
        beforeEach(() => {
            const store = spectator.inject(DotEsSearchStore, true);
            store.status = jest.fn().mockReturnValue(ComponentStatus.LOADED);
            store.hasResults = jest.fn().mockReturnValue(true);
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
            store.hasResults = jest.fn().mockReturnValue(true);
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
    });
});
