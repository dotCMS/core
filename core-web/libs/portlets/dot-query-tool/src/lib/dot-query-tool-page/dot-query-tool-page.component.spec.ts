import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { NEVER, of } from 'rxjs';
import { Mock, MockInstance, vi } from 'vitest';

import { Location, PopStateEvent } from '@angular/common';
import { Signal } from '@angular/core';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    buildPersistedQueryKey,
    DotActionUrlService,
    DotContentletEditUrlService,
    DotContentletService,
    DotContentSearchService,
    DotCurrentUserService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotPropertiesService
} from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import {
    DotEditContentOpenerService,
    DotEditContentOpenOptions,
    DotEditContentRequest
} from '@dotcms/edit-content';
import { DotClipboardUtil, DotSidePanelRef, DotSidePanelService } from '@dotcms/ui';

import { DotQueryToolPageComponent } from './dot-query-tool-page.component';
import { DEFAULT_LIMIT, DotQueryToolStore } from './store/dot-query-tool.store';

import { DotQueryToolService } from '../services/dot-query-tool.service';

/** Content the page opened, as the fake {@link DotEditContentOpenerService} records it. */
interface OpenedContent {
    request: DotEditContentRequest | Signal<DotEditContentRequest>;
    options: DotEditContentOpenOptions;
    ref: DotSidePanelRef;
}

/**
 * Stands in for the opener: records each open and hands back a real panel ref, so `closed` and
 * `isClosed` behave as the outlet's would. Like the real opener, it reports `closed` from the ref.
 * `requestClose` is a spy that does nothing, as a guard holding the panel open would.
 */
const createOpenerFake = () => {
    const opened: OpenedContent[] = [];

    return {
        opened,
        open: vi.fn(
            (
                request: DotEditContentRequest | Signal<DotEditContentRequest>,
                options: DotEditContentOpenOptions
            ) => {
                const ref = new DotSidePanelRef(() => undefined);
                vi.spyOn(ref, 'requestClose').mockImplementation(() => undefined);
                ref.closed.subscribe(() => options.closed?.());
                opened.push({ request, options, ref });

                return ref;
            }
        )
    };
};

/** The request the page handed the opener, read through the signal when it passed one. */
const requestOf = (value: OpenedContent['request']): DotEditContentRequest =>
    typeof value === 'function' ? value() : value;

/** The route the page is on, with `params` in its query string, read either way the router offers. */
const routeWith = (params: Record<string, string> = {}) => ({
    snapshot: { queryParams: params, queryParamMap: convertToParamMap(params) }
});

/** The Query Tool's URL with `params`, as the address bar holds it. */
const queryToolUrl = (params: Record<string, string> = {}): string => {
    const query = new URLSearchParams(params).toString();

    return query ? `/c/query-tool?${query}` : '/c/query-tool';
};

/**
 * Stands in for Angular's `Location`: keeps the URL the last write left, as the browser does, and
 * plays Back or Forward to everyone subscribed.
 */
const createLocationDouble = (initialUrl: string) => {
    let current = initialUrl;
    const listeners: Array<(event: PopStateEvent) => void> = [];
    const write = (path: string, query = ''): void => {
        current = query ? `${path}?${query.replace(/^\?/, '')}` : path;
    };

    return {
        path: vi.fn(() => current),
        go: vi.fn(write),
        replaceState: vi.fn(write),
        subscribe: vi.fn((onNext: (event: PopStateEvent) => void) => {
            listeners.push(onNext);

            return { unsubscribe: vi.fn() };
        }),
        /** Browser Back or Forward, landing on `url`. */
        pop(url: string): void {
            current = url;
            listeners.forEach((listener) =>
                listener({ url, pop: true, type: 'popstate', state: null })
            );
        }
    };
};

/** The params of the last URL written with `write`, decoded, whatever their encoding or order. */
const lastWrittenParams = (write: { mock: { calls: unknown[][] } }): Record<string, string> => {
    const [path, query = ''] = write.mock.calls[write.mock.calls.length - 1] as [string, string?];
    const url = new URL(query ? `${path}?${query.replace(/^\?/, '')}` : path, 'http://dotcms.test');

    return Object.fromEntries(url.searchParams);
};

/** An identifier a shared link names: only identifier-shaped values open anything (FR-049). */
const LINKED_IDENTIFIER = '3f0c8e5a-7b2d-4c1e-9a6f-0d4b8e2c1a57';

/** A result of a content type on the new editor. */
const SAMPLE_CONTENTLET = {
    inode: 'inode-1',
    identifier: 'id-1',
    title: 'Home',
    contentType: 'Blog',
    baseType: 'CONTENT',
    languageId: 1
};

/** A result of a content type still on the legacy editor. The page cannot tell them apart. */
const LEGACY_CONTENTLET = {
    inode: 'inode-2',
    identifier: 'id-2',
    title: 'Promo',
    contentType: 'Banner',
    baseType: 'CONTENT',
    languageId: 2
};

/** A page result: it still opens the page editor in a new tab. */
const PAGE_CONTENTLET = {
    inode: 'inode-3',
    identifier: 'id-3',
    title: 'About us',
    contentType: 'htmlpageasset',
    baseType: 'HTMLPAGE',
    languageId: 1
};

const buildStoreMock = (overrides: Partial<Record<string, Mock>> = {}) => ({
    query: vi.fn().mockReturnValue(''),
    sort: vi.fn().mockReturnValue(''),
    offset: vi.fn().mockReturnValue(0),
    limit: vi.fn().mockReturnValue(DEFAULT_LIMIT),
    userId: vi.fn().mockReturnValue(''),
    isAdmin: vi.fn().mockReturnValue(false),
    status: vi.fn().mockReturnValue(ComponentStatus.INIT),
    response: vi.fn().mockReturnValue(null),
    contentlets: vi.fn().mockReturnValue([]),
    resultsSize: vi.fn().mockReturnValue(0),
    queryTook: vi.fn().mockReturnValue(0),
    contentTook: vi.fn().mockReturnValue(0),
    rawJson: vi.fn().mockReturnValue(''),
    queryTimeMs: vi.fn().mockReturnValue(null),
    activeTab: vi.fn().mockReturnValue('results'),
    isLoading: vi.fn().mockReturnValue(false),
    hasLoadedResults: vi.fn().mockReturnValue(false),
    showingFrom: vi.fn().mockReturnValue(0),
    showingTo: vi.fn().mockReturnValue(0),
    apiRequestBody: vi
        .fn()
        .mockReturnValue({ query: '', sort: '', offset: 0, limit: DEFAULT_LIMIT }),
    limitWasCapped: vi.fn().mockReturnValue(false),
    emptyStateConfig: vi.fn().mockReturnValue({
        title: 'Empty',
        icon: 'search',
        iconStyle: 'material-symbols-rounded',
        subtitle: ''
    }),
    setQuery: vi.fn(),
    setSort: vi.fn(),
    setOffset: vi.fn(),
    setLimit: vi.fn(),
    setUserId: vi.fn(),
    setActiveTab: vi.fn(),
    resetOffset: vi.fn(),
    runSearch: vi.fn(),
    ...overrides
});

/**
 * The injected store is a SpyObject, so its signal members type as spies rather than plain
 * signals and will not accept a bare mock. Writing through a record view keeps the one-line
 * stubs below readable.
 */
const stubbed = (store: unknown) => store as Record<string, unknown>;

const MONACO_CTRL_CMD = 2048;
const MONACO_ENTER = 3;

/** Stubs the global Monaco namespace and returns a fake editor that records its actions. */
const createFakeEditor = () => {
    vi.stubGlobal('monaco', {
        ...((globalThis as { monaco?: object }).monaco ?? {}),
        KeyMod: { CtrlCmd: MONACO_CTRL_CMD },
        KeyCode: { Enter: MONACO_ENTER }
    });
    const addAction = vi.fn();

    return {
        editor: { addAction },
        /** Simulates Monaco dispatching the Cmd/Ctrl + Enter keybinding. */
        pressRunShortcut: () => addAction.mock.calls[0][0].run()
    };
};

describe('DotQueryToolPageComponent', () => {
    let spectator: Spectator<DotQueryToolPageComponent>;
    let locationReplaceStateSpy: Mock;
    let pendingStoreOverrides: Partial<Record<string, Mock>> = {};
    let opener: ReturnType<typeof createOpenerFake>;

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
            mockProvider(DotMessageService, { get: vi.fn().mockReturnValue('') }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotGlobalMessageService, { error: vi.fn() }),
            mockProvider(DotQueryToolService),
            mockProvider(DotContentletEditUrlService, {
                resolveEditUrl: vi.fn()
            }),
            // Deep-link resolve for `?editContent=`; empty by default (no panel opens on load).
            mockProvider(DotContentSearchService, {
                get: vi.fn().mockReturnValue(of({ jsonObjectView: { contentlets: [] } }))
            }),
            // Records the panels the page opens instead of rendering an editor (#38303).
            { provide: DotEditContentOpenerService, useFactory: () => opener }
        ],
        componentProviders: [
            { provide: DotQueryToolStore, useFactory: () => buildStoreMock(pendingStoreOverrides) },
            DotClipboardUtil
        ]
    });

    const setup = (
        params: Record<string, string> = {},
        storeOverrides: Partial<Record<string, Mock>> = {}
    ) => {
        opener = createOpenerFake();
        locationReplaceStateSpy = vi.fn();
        pendingStoreOverrides = storeOverrides;
        spectator = createComponent({
            providers: [
                { provide: ActivatedRoute, useValue: routeWith(params) },
                {
                    provide: Location,
                    useValue: {
                        replaceState: locationReplaceStateSpy,
                        go: vi.fn(),
                        path: vi.fn().mockReturnValue(''),
                        subscribe: vi.fn().mockReturnValue({ unsubscribe: vi.fn() })
                    }
                }
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

        it('leaves the store query untouched when the URL carries no q', () => {
            // The store hydrates `query` from localStorage in its own onInit, which runs before
            // ngOnInit. Writing the (absent) `q` param over it wipes the restored query from the
            // UI. Storage itself survives — the persistence pipeline's `skip(1)` absorbs the stray
            // '' — so the damage is confined to the editor the user is looking at.
            setup({});
            const store = spectator.inject(DotQueryToolStore, true);
            expect(store.setQuery).not.toHaveBeenCalled();
        });

        it('applies an explicitly empty q, so a shared ?q= link still clears the editor', () => {
            setup({ q: '' });
            const store = spectator.inject(DotQueryToolStore, true);
            expect(store.setQuery).toHaveBeenCalledWith('');
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
            stubbed(store)['query'] = vi.fn().mockReturnValue('+live:true');
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
                    query: vi.fn().mockReturnValue('+live:true'),
                    status: vi.fn().mockReturnValue(ComponentStatus.LOADED)
                }
            );
            expect(locationReplaceStateSpy).toHaveBeenCalled();
        });

        it('also syncs URL on ERROR so users can share the failing query', () => {
            setup(
                {},
                {
                    query: vi.fn().mockReturnValue('+broken:('),
                    status: vi.fn().mockReturnValue(ComponentStatus.ERROR)
                }
            );
            expect(locationReplaceStateSpy).toHaveBeenCalled();
        });

        it('does not sync URL while the search is pending (LOADING) or before any run (INIT)', () => {
            setup(
                {},
                {
                    query: vi.fn().mockReturnValue('+live:true'),
                    status: vi.fn().mockReturnValue(ComponentStatus.LOADING)
                }
            );
            expect(locationReplaceStateSpy).not.toHaveBeenCalled();
        });

        it('does not sync URL on first mount with no run yet (status INIT)', () => {
            setup();
            expect(locationReplaceStateSpy).not.toHaveBeenCalled();
        });
    });

    describe('Run keyboard shortcut', () => {
        afterEach(() => {
            vi.unstubAllGlobals();
        });

        it('binds Cmd/Ctrl + Enter on the query editor', () => {
            setup();
            const { editor } = createFakeEditor();
            spectator.triggerEventHandler(
                '[data-testid="query-tool-query-editor"]',
                'init',
                editor
            );
            expect(editor.addAction).toHaveBeenCalledWith(
                expect.objectContaining({ keybindings: [MONACO_CTRL_CMD | MONACO_ENTER] })
            );
        });

        it('runs the query when the shortcut is pressed', () => {
            const store = setup({}, { query: vi.fn().mockReturnValue('+live:true') });
            const { editor, pressRunShortcut } = createFakeEditor();
            spectator.triggerEventHandler(
                '[data-testid="query-tool-query-editor"]',
                'init',
                editor
            );
            pressRunShortcut();
            expect(store.resetOffset).toHaveBeenCalled();
            expect(store.runSearch).toHaveBeenCalled();
        });

        it('is a no-op while a search is already in flight', () => {
            const store = setup(
                {},
                {
                    query: vi.fn().mockReturnValue('+live:true'),
                    isLoading: vi.fn().mockReturnValue(true)
                }
            );
            const { editor, pressRunShortcut } = createFakeEditor();
            spectator.triggerEventHandler(
                '[data-testid="query-tool-query-editor"]',
                'init',
                editor
            );
            pressRunShortcut();
            expect(store.runSearch).not.toHaveBeenCalled();
        });

        it('states the shortcut in the Run button tooltip', () => {
            setup();
            const messageService = spectator.inject(DotMessageService);
            expect(messageService.get).toHaveBeenCalledWith(
                'queryTool.action.run.tooltip',
                spectator.component.runShortcutLabel
            );
        });
    });

    /**
     * #38303 (R13) — every non-page result opens in the side panel over the results, in whichever
     * editor its type uses: the opener decides, so the page no longer resolves an editor URL or
     * reads a flag. Pages still open the page editor in a new tab, as today.
     */
    describe('Result title click', () => {
        let windowOpenSpy: MockInstance;
        let resolveEditUrl: Mock;

        /** Clicks a result's title, as the template does, and lets the panel binding run. */
        const clickResult = (contentlet: object): void => {
            spectator.component.onResultClick(contentlet as never, new MouseEvent('click'));
            spectator.flushEffects();
        };

        beforeEach(() => {
            windowOpenSpy = vi.spyOn(window, 'open').mockReturnValue(null);
            setup();
            resolveEditUrl = spectator.inject(DotContentletEditUrlService).resolveEditUrl as Mock;
            resolveEditUrl.mockReturnValue(
                of('/dotAdmin/#/edit-page/content?url=%2Fabout-us&language_id=1&mId=edit')
            );
        });

        afterEach(() => {
            windowOpenSpy.mockRestore();
        });

        describe.each([
            ['a new-editor type', SAMPLE_CONTENTLET],
            ['a legacy type', LEGACY_CONTENTLET]
        ])('a result of %s', (_label, contentlet) => {
            it('opens it in the side panel for editing', () => {
                clickResult(contentlet);

                expect(opener.open.mock.calls.length, 'the page opens one panel').toBe(1);
                expect(requestOf(opener.opened[0].request)).toEqual({
                    mode: 'edit',
                    inode: contentlet.inode,
                    identifier: contentlet.identifier,
                    contentType: contentlet.contentType,
                    languageId: contentlet.languageId,
                    title: contentlet.title
                });
            });

            it('never opens a tab', () => {
                clickResult(contentlet);

                expect(windowOpenSpy.mock.calls.length).toBe(0);
            });

            // The opener picks the editor; the page no longer resolves a URL to tell them apart.
            it('resolves no edit URL', () => {
                clickResult(contentlet);

                expect(resolveEditUrl.mock.calls.length).toBe(0);
            });
        });

        describe('a page result', () => {
            it('still opens the page editor in a new tab', () => {
                clickResult(PAGE_CONTENTLET);

                expect(resolveEditUrl).toHaveBeenCalledWith(PAGE_CONTENTLET);
                expect(windowOpenSpy).toHaveBeenCalledWith(
                    '/dotAdmin/#/edit-page/content?url=%2Fabout-us&language_id=1&mId=edit',
                    '_blank'
                );
            });

            it('opens no panel', () => {
                clickResult(PAGE_CONTENTLET);

                expect(opener.open.mock.calls.length).toBe(0);
            });
        });

        describe('the panel', () => {
            // The panel then closes with the Query Tool, not with the admin root.
            it("opens with the page's injector", () => {
                clickResult(SAMPLE_CONTENTLET);

                const { injector } = opener.opened[0].options;
                // Compared by identity: printing an injector runs the worker out of memory.
                expect(injector.get(DotQueryToolPageComponent, null) === spectator.component).toBe(
                    true
                );
            });

            it('reruns the search on every save', () => {
                const store = spectator.inject(DotQueryToolStore, true);
                clickResult(SAMPLE_CONTENTLET);

                opener.opened[0].options.saved?.(SAMPLE_CONTENTLET as never);
                opener.opened[0].options.saved?.(SAMPLE_CONTENTLET as never);

                expect(store.runSearch).toHaveBeenCalledTimes(2);
            });

            it('reruns the search and clears the request when it closes', () => {
                const store = spectator.inject(DotQueryToolStore, true);
                clickResult(SAMPLE_CONTENTLET);

                opener.opened[0].ref.close();
                spectator.flushEffects();

                expect(store.runSearch).toHaveBeenCalledTimes(1);
                expect(spectator.component.$editRequest()).toBeNull();
            });

            // The opener writes the panel to the URL; the page only asks for it (FR-050).
            it('opens with the url option', () => {
                clickResult(SAMPLE_CONTENTLET);

                expect(opener.opened[0].options.url).toEqual({});
            });

            it('writes nothing to the URL itself when the panel opens or closes', () => {
                const location = spectator.inject(Location);

                clickResult(SAMPLE_CONTENTLET);
                opener.opened[0].ref.close();
                spectator.flushEffects();

                expect(location.go).not.toHaveBeenCalled();
                expect(location.replaceState).not.toHaveBeenCalled();
            });
        });

        // The flag read, the flag-off placeholder tab and the new-editor URL check are gone.
        it('reads no side-panel flag', () => {
            expect('$sidePanelEnabled' in spectator.component).toBe(false);
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
            return vi.spyOn(clipboard, 'copy').mockResolvedValue(true);
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
            stubbed(store)['apiRequestBody'] = vi.fn().mockReturnValue({
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
            stubbed(store)['apiRequestBody'] = vi
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

// Sibling top-level describe (own TestBed): the open panel is in the URL and browser Back closes it
// through the editor's guard, as before, now written by the shared DotPanelUrlStateService behind
// the real opener (#38303, FR-050). Only the panel itself is faked: the side-panel service hands
// out refs whose guard holds the panel open, and the editor is never chosen, so no editor loads.
describe('DotQueryToolPageComponent (panel URL and Back)', () => {
    let spectator: Spectator<DotQueryToolPageComponent>;
    let windowOpenSpy: MockInstance;
    let location: ReturnType<typeof createLocationDouble>;
    /** The panels the opener put on screen, in order. */
    let panels: DotSidePanelRef[];

    /** The page's URL after a search: no panel write may change its params (FR-044). */
    const SEARCHED_URL = queryToolUrl({ q: '+live:true' });

    const createComponent = createComponentFactory({
        component: DotQueryToolPageComponent,
        overrideComponents: [
            [
                DotQueryToolPageComponent,
                { remove: { providers: [DotQueryToolStore, DotCurrentUserService] } }
            ]
        ],
        providers: [
            mockProvider(DotSidePanelService, {
                open: vi.fn(() => {
                    const ref = new DotSidePanelRef(() => undefined);
                    vi.spyOn(ref, 'requestClose').mockImplementation(() => undefined);
                    panels.push(ref);

                    return ref;
                })
            }),
            mockProvider(DotContentletEditUrlService, {
                resolveEditUrl: vi.fn(),
                resolveEditor: vi.fn(() => NEVER)
            }),
            mockProvider(DotContentletService),
            mockProvider(DotActionUrlService),
            mockProvider(DotMessageService, { get: vi.fn().mockReturnValue('') }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotGlobalMessageService, { error: vi.fn() }),
            mockProvider(DotQueryToolService),
            mockProvider(DotContentSearchService, {
                get: vi.fn().mockReturnValue(of({ jsonObjectView: { contentlets: [] } }))
            })
        ],
        componentProviders: [
            { provide: DotQueryToolStore, useFactory: () => buildStoreMock() },
            DotClipboardUtil
        ]
    });

    /** Clicks a result's title, as the template does, and lets the panel binding run. */
    const clickResult = (contentlet: object): void => {
        spectator.component.onResultClick(contentlet as never, new MouseEvent('click'));
        spectator.flushEffects();
    };

    beforeEach(() => {
        panels = [];
        location = createLocationDouble(SEARCHED_URL);
        windowOpenSpy = vi.spyOn(window, 'open').mockReturnValue(null);
        spectator = createComponent({
            providers: [
                { provide: ActivatedRoute, useValue: routeWith() },
                { provide: Location, useValue: location }
            ]
        });
    });

    afterEach(() => windowOpenSpy.mockRestore());

    it('names the open panel and its language in one history entry, beside the search params', () => {
        clickResult(SAMPLE_CONTENTLET);

        expect(location.go).toHaveBeenCalledTimes(1);
        expect(lastWrittenParams(location.go)).toEqual({
            q: '+live:true',
            editContent: 'id-1',
            editContentLang: '1'
        });
    });

    it('clears the params in place when the panel closes, so Forward cannot reopen it', () => {
        clickResult(SAMPLE_CONTENTLET);

        panels[0].close();
        spectator.flushEffects();

        expect(location.go).toHaveBeenCalledTimes(1);
        expect(location.replaceState).toHaveBeenCalledTimes(1);
        expect(lastWrittenParams(location.replaceState)).toEqual({ q: '+live:true' });
    });

    it("routes browser Back through the panel's guard, and keeps naming the panel meanwhile", () => {
        clickResult(SAMPLE_CONTENTLET);

        location.pop(SEARCHED_URL);

        expect(panels[0].requestClose).toHaveBeenCalledTimes(1);
        expect(lastWrittenParams(location.replaceState)).toEqual({
            q: '+live:true',
            editContent: 'id-1',
            editContentLang: '1'
        });
    });
});

// Sibling top-level describe (own TestBed): a shared link is read in `ngOnInit` with the shared
// `readPanelUrlIntent` (#38303, FR-049), so each test needs its own `ActivatedRoute` and address bar
// before construction, hence a dedicated describe.
describe('DotQueryToolPageComponent (panel deep link)', () => {
    let spectator: Spectator<DotQueryToolPageComponent>;
    let opener: ReturnType<typeof createOpenerFake>;
    let location: ReturnType<typeof createLocationDouble>;
    let search: Mock;

    const createComponent = createComponentFactory({
        component: DotQueryToolPageComponent,
        overrideComponents: [
            [
                DotQueryToolPageComponent,
                { remove: { providers: [DotQueryToolStore, DotCurrentUserService] } }
            ]
        ],
        providers: [
            // Records the panel a deep link opens instead of rendering the real editor.
            { provide: DotEditContentOpenerService, useFactory: () => opener },
            mockProvider(DotMessageService, { get: vi.fn().mockReturnValue('') }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotGlobalMessageService, { error: vi.fn() }),
            mockProvider(DotQueryToolService),
            mockProvider(DotContentletEditUrlService, { resolveEditUrl: vi.fn() })
        ],
        componentProviders: [
            { provide: DotQueryToolStore, useFactory: () => buildStoreMock() },
            DotClipboardUtil
        ]
    });

    beforeEach(() => {
        opener = createOpenerFake();
    });

    /** Loads the Query Tool on a URL carrying `params`. */
    const mount = (params: Record<string, string>) => {
        location = createLocationDouble(queryToolUrl(params));
        spectator = createComponent({
            providers: [
                { provide: ActivatedRoute, useValue: routeWith(params) },
                { provide: Location, useValue: location },
                mockProvider(DotContentSearchService, {
                    get: vi
                        .fn()
                        .mockReturnValue(
                            of({ jsonObjectView: { contentlets: [SAMPLE_CONTENTLET] } })
                        )
                })
            ]
        });
        search = spectator.inject(DotContentSearchService).get as Mock;
        spectator.flushEffects();
    };

    // No flag to wait for any more (#38303): the link resolves and opens on load.
    it('resolves and opens the panel from a shared link', () => {
        mount({ editContent: LINKED_IDENTIFIER });

        expect(opener.open.mock.calls.length, 'the page opens one panel').toBe(1);
        expect(requestOf(opener.opened[0].request)).toEqual({
            mode: 'edit',
            inode: 'inode-1',
            identifier: LINKED_IDENTIFIER,
            contentType: 'Blog',
            languageId: 1,
            title: 'Home'
        });
    });

    // The opener keeps the panel in the URL, and Back closes it, as for a panel opened by a click.
    it('opens it with the url option', () => {
        mount({ editContent: LINKED_IDENTIFIER });

        expect(opener.opened[0].options.url).toEqual({});
    });

    // An identifier has one version per language; the link names the one that was open.
    it('reopens the version in the language the link names', () => {
        mount({ editContent: LINKED_IDENTIFIER, editContentLang: '2' });

        const { query } = search.mock.calls[0][0] as { query: string };
        expect(query).toContain(`+identifier:${LINKED_IDENTIFIER}`);
        expect(query).toContain('+languageId:2');
    });

    // The value reaches a Lucene query, so only an identifier's shape is looked up (FR-049).
    it.each(['new', 'id-1', 'abc OR +live:true'])(
        'opens nothing and searches nothing for editContent=%j',
        (editContent) => {
            mount({ editContent });

            expect(search).not.toHaveBeenCalled();
            expect(opener.open).not.toHaveBeenCalled();
        }
    );

    it('opens nothing for a link naming two panels, and removes their params in place', () => {
        mount({ q: '+live:true', editContent: LINKED_IDENTIFIER, createContent: 'Blog' });

        expect(opener.open).not.toHaveBeenCalled();
        expect(search).not.toHaveBeenCalled();
        expect(location.go).not.toHaveBeenCalled();
        expect(location.replaceState).toHaveBeenCalledTimes(1);
        expect(lastWrittenParams(location.replaceState)).toEqual({ q: '+live:true' });
    });

    // A create stacked over a result's panel names itself in the URL, and a refresh reopens only the
    // front panel (FR-048), so the Query Tool reopens a create too.
    it('opens a create the link names', () => {
        mount({ createContent: 'Blog' });

        expect(opener.open.mock.calls.length).toBe(1);
        expect(requestOf(opener.opened[0].request)).toEqual({ mode: 'new', contentType: 'Blog' });
    });
});

// Sibling top-level describe with the REAL store (every other describe here mocks it, so the
// store's own `onInit` never runs and the ngOnInit/hydration ordering can't be observed). This is
// the QA-reported path: type a query, leave the portlet, come back, query is gone.
describe('DotQueryToolPageComponent (persisted query, real store)', () => {
    let spectator: Spectator<DotQueryToolPageComponent>;
    const persistedKey = buildPersistedQueryKey('query-tool');

    const createComponent = createComponentFactory({
        component: DotQueryToolPageComponent,
        providers: [
            mockProvider(DotMessageService, { get: vi.fn().mockReturnValue('') }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotGlobalMessageService, { error: vi.fn() }),
            mockProvider(DotQueryToolService, {
                search: vi
                    .fn()
                    .mockReturnValue(of({ resultsSize: 0, jsonObjectView: { contentlets: [] } }))
            }),
            mockProvider(DotContentletEditUrlService, { resolveEditUrl: vi.fn() }),
            mockProvider(DotContentSearchService, {
                get: vi.fn().mockReturnValue(of({ jsonObjectView: { contentlets: [] } }))
            }),
            mockProvider(DotPropertiesService, {
                getFeatureFlags: vi.fn().mockReturnValue(of({}))
            }),
            // The page binds its panel to the opener (#38303); nothing opens in these cases.
            mockProvider(DotEditContentOpenerService)
        ],
        // Spectator REPLACES the component's own `providers` with this list, so the real store has
        // to be named here — that is the whole point of this describe.
        componentProviders: [
            DotQueryToolStore,
            mockProvider(DotCurrentUserService, {
                getCurrentUser: vi.fn().mockReturnValue(of({ admin: true }))
            }),
            // Neutral: the store reads no flag once its `withFlags` goes (#38303); until then it is
            // built by the component injector, so its DotPropertiesService has to be reachable here.
            mockProvider(DotPropertiesService, {
                getFeatureFlags: vi.fn().mockReturnValue(of({}))
            }),
            DotClipboardUtil,
            MessageService
        ]
    });

    const mount = (params: Record<string, string> = {}) =>
        createComponent({
            providers: [
                { provide: ActivatedRoute, useValue: routeWith(params) },
                {
                    provide: Location,
                    useValue: {
                        replaceState: vi.fn(),
                        go: vi.fn(),
                        path: vi.fn().mockReturnValue(''),
                        subscribe: vi.fn().mockReturnValue({ unsubscribe: vi.fn() })
                    }
                }
            ]
        });

    beforeEach(() => {
        vi.useFakeTimers();
        window.localStorage.clear();
    });

    afterEach(() => {
        vi.useRealTimers();
        window.localStorage.clear();
    });

    it('restores the persisted query when the portlet is reopened without params', () => {
        window.localStorage.setItem(persistedKey, JSON.stringify('+contentType:Blog'));

        spectator = mount();

        expect(spectator.component.store.query()).toBe('+contentType:Blog');
    });

    it('never erases the stored entry on reopen, whatever the restore path does', () => {
        // NOT a regression test for the ngOnInit guard — this passes with that guard reverted,
        // because the persistence pipeline's `skip(1)` absorbs the stray '' before it can reach
        // `removeKey`. It guards the invariant underneath: reopening the portlet is a read, and
        // must never destroy the saved query. That `skip(1)` is the only thing standing between
        // a stray write-on-init and silent data loss, so it is worth pinning on its own.
        window.localStorage.setItem(persistedKey, JSON.stringify('+contentType:Blog'));

        spectator = mount();
        // flushEffects() pushes the field signal through the rxMethod effect; only then does the
        // debounce timer exist for the fake clock to advance. Skipping it is what makes fake
        // timers look like they do not drive this pipeline.
        spectator.flushEffects();
        vi.advanceTimersByTime(500);

        expect(window.localStorage.getItem(persistedKey)).toBe(JSON.stringify('+contentType:Blog'));
    });

    it('lets a ?q= deep link win over the persisted query', () => {
        window.localStorage.setItem(persistedKey, JSON.stringify('+contentType:Blog'));

        spectator = mount({ q: '+live:true' });

        expect(spectator.component.store.query()).toBe('+live:true');
    });
});
