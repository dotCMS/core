import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { of } from 'rxjs';
import { Mock, MockInstance, vi } from 'vitest';

import { Location } from '@angular/common';
import { Signal } from '@angular/core';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    buildPersistedQueryKey,
    DotContentletEditUrlService,
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
import { DotClipboardUtil, DotSidePanelRef } from '@dotcms/ui';

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
                {
                    provide: ActivatedRoute,
                    useValue: { snapshot: { queryParamMap: convertToParamMap(params) } }
                },
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
                const location = spectator.inject(Location);
                clickResult(SAMPLE_CONTENTLET);
                (location.replaceState as Mock).mockClear();

                opener.opened[0].ref.close();
                spectator.flushEffects();

                expect(store.runSearch).toHaveBeenCalledTimes(1);
                // The cleared request drops `editContent` from the URL.
                expect(location.replaceState).toHaveBeenCalledTimes(1);
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

// Sibling top-level describe (own TestBed): the open panel is reflected in the URL
// (`?editContent=`) and browser Back closes it through the editor's guard (#38303: unchanged,
// now driven through the opener).
describe('DotQueryToolPageComponent (panel URL and Back)', () => {
    let spectator: Spectator<DotQueryToolPageComponent>;
    let windowOpenSpy: MockInstance;
    let opener: ReturnType<typeof createOpenerFake>;

    const createComponent = createComponentFactory({
        component: DotQueryToolPageComponent,
        overrideComponents: [
            [
                DotQueryToolPageComponent,
                { remove: { providers: [DotQueryToolStore, DotCurrentUserService] } }
            ]
        ],
        providers: [
            // Records the panels the page opens instead of rendering the real editor.
            { provide: DotEditContentOpenerService, useFactory: () => opener },
            mockProvider(DotMessageService, { get: vi.fn().mockReturnValue('') }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotGlobalMessageService, { error: vi.fn() }),
            mockProvider(DotQueryToolService),
            mockProvider(DotContentletEditUrlService, { resolveEditUrl: vi.fn() }),
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
        opener = createOpenerFake();
        windowOpenSpy = vi.spyOn(window, 'open').mockReturnValue(null);
        spectator = createComponent({
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: { snapshot: { queryParamMap: convertToParamMap({}) } }
                },
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
    });

    afterEach(() => windowOpenSpy.mockRestore());

    it('reflects the open panel in the URL via editContent (history push, so Back can pop it)', () => {
        const location = spectator.inject(Location);

        clickResult(SAMPLE_CONTENTLET);

        expect(location.go).toHaveBeenCalledWith(expect.stringContaining('editContent=id-1'));
    });

    it('uses replaceState (not go) when the panel closes, so Back cannot resurrect the removed param', () => {
        const location = spectator.inject(Location);

        clickResult(SAMPLE_CONTENTLET);
        (location.go as Mock).mockClear();
        (location.replaceState as Mock).mockClear();

        opener.opened[0].ref.close();
        spectator.flushEffects();

        expect(location.replaceState).toHaveBeenCalledTimes(1);
        expect(location.go).not.toHaveBeenCalled();
    });

    it('routes browser Back through the panel close guard (does not clear silently)', () => {
        clickResult(SAMPLE_CONTENTLET);
        const { requestClose } = opener.opened[0].ref;
        const location = spectator.inject(Location);
        const popstate = (location.subscribe as Mock).mock.calls[0][0] as (event: {
            url: string;
        }) => void;

        popstate({ url: '/query-tool?q=x' });

        expect(requestClose).toHaveBeenCalledTimes(1);
        expect(location.replaceState).toHaveBeenCalled();
    });
});

// Sibling top-level describe (own TestBed): the `?editContent=` deep link is read in `ngOnInit`,
// so — unlike the shell's constructor-time read — it CAN share this factory's shape, but each test
// still needs its own `ActivatedRoute` before construction, hence a dedicated describe.
describe('DotQueryToolPageComponent (editContent deep link)', () => {
    let spectator: Spectator<DotQueryToolPageComponent>;
    let opener: ReturnType<typeof createOpenerFake>;

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

    const mount = (editContent: string) =>
        createComponent({
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: { snapshot: { queryParamMap: convertToParamMap({ editContent }) } }
                },
                {
                    provide: Location,
                    useValue: {
                        replaceState: vi.fn(),
                        go: vi.fn(),
                        path: vi.fn().mockReturnValue(''),
                        subscribe: vi.fn().mockReturnValue({ unsubscribe: vi.fn() })
                    }
                },
                mockProvider(DotContentSearchService, {
                    get: vi
                        .fn()
                        .mockReturnValue(
                            of({ jsonObjectView: { contentlets: [SAMPLE_CONTENTLET] } })
                        )
                })
            ]
        });

    // No flag to wait for any more (#38303): the link resolves and opens on load.
    it('resolves and opens the panel from a shared link', () => {
        spectator = mount('id-1');
        spectator.flushEffects();

        expect(opener.open.mock.calls.length, 'the page opens one panel').toBe(1);
        expect(requestOf(opener.opened[0].request)).toEqual({
            mode: 'edit',
            inode: 'inode-1',
            identifier: 'id-1',
            contentType: 'Blog',
            languageId: 1,
            title: 'Home'
        });
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
                {
                    provide: ActivatedRoute,
                    useValue: { snapshot: { queryParamMap: convertToParamMap(params) } }
                },
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
