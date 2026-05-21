import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Location } from '@angular/common';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import {
    DotContentletEditUrlService,
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
    let locationReplaceStateSpy: jest.Mock;
    let pendingStoreOverrides: Partial<Record<string, jest.Mock>> = {};

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
            mockProvider(DotContentletEditUrlService, {
                resolveEditUrl: jest.fn()
            })
        ],
        componentProviders: [
            { provide: DotQueryToolStore, useFactory: () => buildStoreMock(pendingStoreOverrides) },
            DotClipboardUtil
        ]
    });

    const setup = (
        params: Record<string, string> = {},
        storeOverrides: Partial<Record<string, jest.Mock>> = {}
    ) => {
        locationReplaceStateSpy = jest.fn();
        pendingStoreOverrides = storeOverrides;
        spectator = createComponent({
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: { snapshot: { queryParamMap: convertToParamMap(params) } }
                },
                { provide: Location, useValue: { replaceState: locationReplaceStateSpy } }
            ]
        });
        return spectator.inject(DotQueryToolStore, true);
    };

    afterEach(() => {
        pendingStoreOverrides = {};
    });

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

        it('syncs URL via Location.replaceState (not Location.go) so typing does not pollute history', () => {
            // Non-default query forces the effect to produce a URL that differs from the
            // current router URL, exercising the replaceState branch. With default state
            // the effect intentionally no-ops (see "skips no-op syncs" below).
            setup({}, { query: jest.fn().mockReturnValue('+live:true') });
            expect(locationReplaceStateSpy).toHaveBeenCalled();
        });

        it('skips no-op syncs on first mount when the URL already matches', () => {
            setup();
            expect(locationReplaceStateSpy).not.toHaveBeenCalled();
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

        it('opens a placeholder tab synchronously, then assigns the resolved URL', () => {
            setup();
            const resolver = spectator.inject(DotContentletEditUrlService);
            (resolver.resolveEditUrl as jest.Mock).mockReturnValue(
                of('/dotAdmin/#/edit-page/content?url=%2Fabout-us&language_id=1&mId=edit')
            );

            spectator.component.onResultClick(SAMPLE_CONTENTLET as never, new MouseEvent('click'));

            expect(windowOpenSpy).toHaveBeenCalledWith('about:blank', '_blank');
            expect(resolver.resolveEditUrl).toHaveBeenCalledWith(SAMPLE_CONTENTLET);
            expect(placeholderWindow.location.href).toBe(
                '/dotAdmin/#/edit-page/content?url=%2Fabout-us&language_id=1&mId=edit'
            );
        });

        it('forwards the contentlet to the resolver and assigns the new-editor URL', () => {
            setup();
            const resolver = spectator.inject(DotContentletEditUrlService);
            (resolver.resolveEditUrl as jest.Mock).mockReturnValue(
                of('/dotAdmin/#/content/inode-1')
            );

            spectator.component.onResultClick(SAMPLE_CONTENTLET as never, new MouseEvent('click'));

            expect(placeholderWindow.location.href).toBe('/dotAdmin/#/content/inode-1');
        });

        it('assigns the legacy-editor URL when the resolver returns the legacy path', () => {
            setup();
            const resolver = spectator.inject(DotContentletEditUrlService);
            (resolver.resolveEditUrl as jest.Mock).mockReturnValue(
                of('/dotAdmin/#/c/content/inode-1')
            );

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
