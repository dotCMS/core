import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { Location } from '@angular/common';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import {
    DotContentletEditUrlService,
    DotCurrentUserService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotPropertiesService
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
            DotClipboardUtil,
            // Flag off by default → results open in a new tab (placeholder path). In
            // componentProviders so the field-initializer toSignal resolves it at construction.
            { provide: DotPropertiesService, useValue: { getFeatureFlag: () => of(false) } }
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
            spectator.fixture.componentRef.changeDetectorRef.markForCheck();
            spectator.detectChanges();
            const btn = spectator.query(byTestId('query-tool-run-btn'))?.querySelector('button');
            expect(btn).toBeTruthy();
            if (btn) spectator.click(btn);
            expect(store.resetOffset).toHaveBeenCalled();
            expect(store.runSearch).toHaveBeenCalled();
        });

        it('syncs URL via Location.replaceState only after a search settles (LOADED)', () => {
            // status = LOADED + non-default query produces a URL that differs from the
            // current router URL, exercising the replaceState branch.
            setup(
                {},
                {
                    query: jest.fn().mockReturnValue('+live:true'),
                    status: jest.fn().mockReturnValue(ComponentStatus.LOADED)
                }
            );
            expect(locationReplaceStateSpy).toHaveBeenCalled();
        });

        it('also syncs URL on ERROR so users can share the failing query', () => {
            setup(
                {},
                {
                    query: jest.fn().mockReturnValue('+broken:('),
                    status: jest.fn().mockReturnValue(ComponentStatus.ERROR)
                }
            );
            expect(locationReplaceStateSpy).toHaveBeenCalled();
        });

        it('does not sync URL while the search is pending (LOADING) or before any run (INIT)', () => {
            setup(
                {},
                {
                    query: jest.fn().mockReturnValue('+live:true'),
                    status: jest.fn().mockReturnValue(ComponentStatus.LOADING)
                }
            );
            expect(locationReplaceStateSpy).not.toHaveBeenCalled();
        });

        it('does not sync URL on first mount with no run yet (status INIT)', () => {
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

// Sibling top-level describe (own TestBed): the side panel feature flag is ON. New-editor results
// open in the in-page panel instead of a new tab; legacy/page results still open in a new tab.
describe('DotQueryToolPageComponent (side panel enabled)', () => {
    let spectator: Spectator<DotQueryToolPageComponent>;
    let windowOpenSpy: jest.SpyInstance;

    const createComponent = createComponentFactory({
        component: DotQueryToolPageComponent,
        overrideComponents: [
            [
                DotQueryToolPageComponent,
                { remove: { providers: [DotQueryToolStore, DotCurrentUserService] }, add: {} }
            ]
        ],
        providers: [
            mockProvider(DotMessageService, { get: jest.fn().mockReturnValue('') }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotGlobalMessageService, { error: jest.fn() }),
            mockProvider(DotQueryToolService),
            mockProvider(DotContentletEditUrlService, { resolveEditUrl: jest.fn() })
        ],
        componentProviders: [
            { provide: DotQueryToolStore, useFactory: () => buildStoreMock() },
            DotClipboardUtil,
            // Flag on → new-editor results open in the side panel. In componentProviders (node
            // injector) so the component's field-initializer `toSignal(getFeatureFlag(...))`
            // resolves the mock at construction.
            { provide: DotPropertiesService, useValue: { getFeatureFlag: () => of(true) } }
        ]
    });

    beforeEach(() => {
        windowOpenSpy = jest.spyOn(window, 'open').mockReturnValue(null);
        spectator = createComponent({
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: { snapshot: { queryParamMap: convertToParamMap({}) } }
                },
                { provide: Location, useValue: { replaceState: jest.fn() } }
            ]
        });
    });

    afterEach(() => windowOpenSpy.mockRestore());

    it('opens the new-editor result in the side panel (no new tab)', () => {
        const resolver = spectator.inject(DotContentletEditUrlService);
        (resolver.resolveEditUrl as jest.Mock).mockReturnValue(of('/dotAdmin/#/content/inode-1'));

        spectator.component.onResultClick(SAMPLE_CONTENTLET as never, new MouseEvent('click'));

        expect(windowOpenSpy).not.toHaveBeenCalled();
        expect(spectator.component.$editPanelRequest()).toEqual({
            mode: 'edit',
            contentletInode: 'inode-1',
            identifier: 'id-1',
            title: 'Home'
        });
    });

    it('opens legacy-editor results in a new tab (not the panel)', () => {
        const resolver = spectator.inject(DotContentletEditUrlService);
        (resolver.resolveEditUrl as jest.Mock).mockReturnValue(of('/dotAdmin/#/c/content/inode-1'));

        spectator.component.onResultClick(SAMPLE_CONTENTLET as never, new MouseEvent('click'));

        expect(windowOpenSpy).toHaveBeenCalledWith('/dotAdmin/#/c/content/inode-1', '_blank');
        expect(spectator.component.$editPanelRequest()).toBeNull();
    });

    it('clears the panel request on close and reloads results on save', () => {
        const store = spectator.inject(DotQueryToolStore, true);
        spectator.component.$editPanelRequest.set({ mode: 'edit', contentletInode: 'inode-1' });

        spectator.component.onEditPanelSaved();
        expect(store.runSearch).toHaveBeenCalledTimes(1);

        spectator.component.onEditPanelClosed();
        expect(spectator.component.$editPanelRequest()).toBeNull();
    });
});
