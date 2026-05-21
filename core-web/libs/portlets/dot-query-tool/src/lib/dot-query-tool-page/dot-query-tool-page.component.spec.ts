import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Location } from '@angular/common';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import {
    DotContentTypeService,
    DotCurrentUserService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotClipboardUtil } from '@dotcms/ui';

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
    apiRequestBody: jest
        .fn()
        .mockReturnValue({ query: '', sort: '', offset: 0, limit: DEFAULT_LIMIT }),
    limitWasCapped: jest.fn().mockReturnValue(false),
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
    let locationGoSpy: jest.Mock;

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
        componentProviders: [
            { provide: DotQueryToolStore, useFactory: () => buildStoreMock() },
            DotClipboardUtil
        ]
    });

    const setup = (params: Record<string, string> = {}) => {
        locationGoSpy = jest.fn();
        spectator = createComponent({
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: { snapshot: { queryParamMap: convertToParamMap(params) } }
                },
                { provide: Location, useValue: { go: locationGoSpy } }
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
        it('is a no-op when the query is empty (matches ES Search behavior)', () => {
            const store = setup();
            spectator.component.onRun();
            expect(store.resetOffset).not.toHaveBeenCalled();
            expect(store.runSearch).not.toHaveBeenCalled();
        });

        it('resets offset and triggers runSearch when clicked', () => {
            const store = setup();
            store.query = jest.fn().mockReturnValue('+live:true');
            spectator.component.onRun();
            expect(store.resetOffset).toHaveBeenCalled();
            expect(store.runSearch).toHaveBeenCalled();
        });

        it('does not call Router.navigate (URL sync goes through Location.go, no re-mount)', () => {
            const store = setup();
            store.query = jest.fn().mockReturnValue('+live:true');
            spectator.component.onRun();
            expect(locationGoSpy).toHaveBeenCalled();
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

    describe('Share menu', () => {
        const setupClipboardSpy = () => {
            const clipboard = spectator.inject(DotClipboardUtil, true);
            return jest.spyOn(clipboard, 'copy').mockResolvedValue(true);
        };

        it('exposes three items (URL, cURL, fetch) with no icons, each command bound', () => {
            setup();
            expect(spectator.component.exportItems).toHaveLength(3);
            for (const item of spectator.component.exportItems) {
                expect(item.icon).toBeUndefined();
                expect(typeof item.command).toBe('function');
            }
        });

        it('Copy shareable URL writes location.href to the clipboard', () => {
            setup();
            const copySpy = setupClipboardSpy();
            spectator.component.exportItems[0].command?.({} as never);
            expect(copySpy).toHaveBeenCalledWith(window.location.href);
        });

        it('Copy as cURL targets the _search endpoint with the store request body', () => {
            const store = setup();
            store.apiRequestBody = jest.fn().mockReturnValue({
                query: '+live:true',
                sort: 'modDate desc',
                limit: 50,
                offset: 20,
                userId: 'admin@dotcms.com'
            });
            const copySpy = setupClipboardSpy();
            spectator.component.exportItems[1].command?.({} as never);

            expect(copySpy).toHaveBeenCalledTimes(1);
            const snippet = copySpy.mock.calls[0][0];
            expect(snippet).toMatch(/^curl -X POST "https?:.*\/api\/v1\/content\/_search"/);
            expect(snippet).toContain('"query":"+live:true"');
            expect(snippet).toContain('"sort":"modDate desc"');
            expect(snippet).toContain('"limit":50');
            expect(snippet).toContain('"offset":20');
            expect(snippet).toContain('"userId":"admin@dotcms.com"');
        });

        it('Copy as fetch emits a fetch() call against the _search endpoint', () => {
            const store = setup();
            store.apiRequestBody = jest
                .fn()
                .mockReturnValue({ query: '+live:true', sort: '', limit: 20, offset: 0 });
            const copySpy = setupClipboardSpy();
            spectator.component.exportItems[2].command?.({} as never);

            const snippet = copySpy.mock.calls[0][0];
            expect(snippet).toContain(`fetch('/api/v1/content/_search'`);
            expect(snippet).toContain(`credentials: 'include'`);
            expect(snippet).toContain(`"query": "+live:true"`);
        });
    });
});
