import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';

import {
    DotContentTypeService,
    DotCurrentUserService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotQueryToolPageComponent } from './dot-query-tool-page.component';
import { DEFAULT_LIMIT, DotQueryToolStore } from './store/dot-query-tool.store';

import { DotQueryToolService } from '../services/dot-query-tool.service';

const SAMPLE_CONTENTLET = {
    inode: 'inode-1',
    identifier: 'id-1',
    title: 'Home',
    contentType: 'htmlpageasset'
};

const buildStoreMock = (overrides: Partial<Record<string, jest.Mock>> = {}) => ({
    query: jest.fn().mockReturnValue(''),
    sort: jest.fn().mockReturnValue(''),
    offset: jest.fn().mockReturnValue(0),
    limit: jest.fn().mockReturnValue(DEFAULT_LIMIT),
    userId: jest.fn().mockReturnValue(''),
    isAdmin: jest.fn().mockReturnValue(false),
    status: jest.fn().mockReturnValue(ComponentStatus.INIT),
    response: jest.fn().mockReturnValue(null),
    contentlets: jest.fn().mockReturnValue([]),
    resultsSize: jest.fn().mockReturnValue(0),
    queryTook: jest.fn().mockReturnValue(0),
    contentTook: jest.fn().mockReturnValue(0),
    rawJson: jest.fn().mockReturnValue(''),
    queryTimeMs: jest.fn().mockReturnValue(null),
    activeTab: jest.fn().mockReturnValue('results'),
    isLoading: jest.fn().mockReturnValue(false),
    hasLoadedResults: jest.fn().mockReturnValue(false),
    showingFrom: jest.fn().mockReturnValue(0),
    showingTo: jest.fn().mockReturnValue(0),
    emptyStateConfig: jest
        .fn()
        .mockReturnValue({ title: 'Empty', icon: 'pi-search', subtitle: '' }),
    setQuery: jest.fn(),
    setSort: jest.fn(),
    setOffset: jest.fn(),
    setLimit: jest.fn(),
    setUserId: jest.fn(),
    setActiveTab: jest.fn(),
    resetOffset: jest.fn(),
    runSearch: jest.fn(),
    ...overrides
});

describe('DotQueryToolPageComponent', () => {
    let spectator: Spectator<DotQueryToolPageComponent>;
    let navigateSpy: jest.Mock;

    const createComponent = createComponentFactory({
        component: DotQueryToolPageComponent,
        overrideComponents: [
            [
                DotQueryToolPageComponent,
                {
                    remove: { providers: [DotQueryToolStore, DotCurrentUserService] },
                    add: {}
                }
            ]
        ],
        providers: [
            mockProvider(DotMessageService, { get: jest.fn().mockReturnValue('') }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotGlobalMessageService, { error: jest.fn() }),
            mockProvider(DotQueryToolService),
            mockProvider(DotContentTypeService, { getContentType: jest.fn() })
        ],
        componentProviders: [{ provide: DotQueryToolStore, useFactory: () => buildStoreMock() }]
    });

    const setup = (params: Record<string, string> = {}) => {
        navigateSpy = jest.fn();
        spectator = createComponent({
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: { snapshot: { queryParamMap: convertToParamMap(params) } }
                },
                { provide: Router, useValue: { navigate: navigateSpy } }
            ]
        });
        return spectator.inject(DotQueryToolStore, true);
    };

    it('creates the component', () => {
        setup();
        expect(spectator.component).toBeTruthy();
    });

    describe('URL state hydration', () => {
        it('hydrates store from query params and auto-runs when q is present', () => {
            setup({
                q: '+live:true',
                offset: '40',
                limit: '50',
                sort: 'modDate desc',
                userId: 'admin@dotcms.com'
            });
            const store = spectator.inject(DotQueryToolStore, true);
            expect(store.setQuery).toHaveBeenCalledWith('+live:true');
            expect(store.setOffset).toHaveBeenCalledWith(40);
            expect(store.setLimit).toHaveBeenCalledWith(50);
            expect(store.setSort).toHaveBeenCalledWith('modDate desc');
            expect(store.setUserId).toHaveBeenCalledWith('admin@dotcms.com');
            expect(store.runSearch).toHaveBeenCalled();
        });

        it('does not auto-run when q is empty', () => {
            setup({});
            const store = spectator.inject(DotQueryToolStore, true);
            expect(store.runSearch).not.toHaveBeenCalled();
        });
    });

    describe('Submit button', () => {
        it('is disabled when the query is empty', () => {
            setup();
            const runBtn = spectator
                .query(byTestId('query-tool-run-btn'))
                ?.querySelector('button') as HTMLButtonElement | null;
            expect(runBtn?.disabled).toBe(true);
        });

        it('resets offset, syncs URL, and triggers runSearch when clicked', () => {
            const store = setup();
            store.query = jest.fn().mockReturnValue('+live:true');
            spectator.component.onRun();
            expect(store.resetOffset).toHaveBeenCalled();
            expect(navigateSpy).toHaveBeenCalled();
            expect(store.runSearch).toHaveBeenCalled();
        });
    });

    describe('Result title click', () => {
        let windowOpenSpy: jest.SpyInstance;
        let placeholderWindow: { location: { href: string }; close: jest.Mock };

        beforeEach(() => {
            placeholderWindow = { location: { href: '' }, close: jest.fn() };
            windowOpenSpy = jest
                .spyOn(window, 'open')
                .mockReturnValue(placeholderWindow as unknown as Window);
        });

        afterEach(() => {
            windowOpenSpy.mockRestore();
        });

        it('opens HTML pages directly in the page editor (new tab)', () => {
            setup();
            const page = {
                inode: 'p1',
                baseType: 'HTMLPAGE',
                url: '/about-us',
                languageId: 1
            };
            spectator.component.onResultClick(page as never, new MouseEvent('click'));
            expect(windowOpenSpy).toHaveBeenCalledTimes(1);
            const [url, target] = windowOpenSpy.mock.calls[0];
            expect(url).toContain('/dotAdmin/#/edit-page/content?');
            expect(url).toContain('url=%2Fabout-us');
            expect(target).toBe('_blank');
        });

        it('opens new editor URL for content types with CONTENT_EDITOR2_ENABLED', () => {
            setup();
            const ctService = spectator.inject(DotContentTypeService);
            (ctService.getContentType as jest.Mock).mockReturnValue(
                of({ metadata: { CONTENT_EDITOR2_ENABLED: true } })
            );
            spectator.component.onResultClick(SAMPLE_CONTENTLET as never, new MouseEvent('click'));
            expect(windowOpenSpy).toHaveBeenCalledWith('about:blank', '_blank');
            expect(placeholderWindow.location.href).toBe('/dotAdmin/#/content/inode-1');
        });

        it('opens legacy editor URL when CONTENT_EDITOR2_ENABLED is missing', () => {
            setup();
            const ctService = spectator.inject(DotContentTypeService);
            (ctService.getContentType as jest.Mock).mockReturnValue(of({ metadata: {} }));
            spectator.component.onResultClick(SAMPLE_CONTENTLET as never, new MouseEvent('click'));
            expect(placeholderWindow.location.href).toBe('/dotAdmin/#/c/content/inode-1');
        });
    });

    describe('User ID field gating', () => {
        it('hides the User ID input when the user is not admin', () => {
            setup();
            expect(spectator.query(byTestId('query-tool-userid-input'))).toBeFalsy();
        });
    });

    describe('Help popover', () => {
        it('renders 4 canonical Lucene example queries', () => {
            setup();
            expect(spectator.component.helpExamples).toHaveLength(4);
            const queries = spectator.component.helpExamples.map((e) => e.query);
            expect(queries).toEqual(
                expect.arrayContaining([
                    expect.stringContaining('+contentType:htmlpageasset'),
                    expect.stringContaining('+contentType:fileAsset'),
                    expect.stringContaining('+title:*demo*'),
                    expect.stringContaining('+languageId:1')
                ])
            );
        });
    });
});
