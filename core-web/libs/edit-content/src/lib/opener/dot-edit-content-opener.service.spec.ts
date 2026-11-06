import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { NEVER, of, Subject, throwError } from 'rxjs';
import { Mock, vi } from 'vitest';

import {
    Component,
    InjectionToken,
    Injector,
    Type,
    inject,
    input,
    output,
    runInInjectionContext,
    signal
} from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import {
    DotActionUrlService,
    DotContentChange,
    DotContentChangeService,
    DotContentletEditUrlService,
    DotContentletService,
    DotContentSearchService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { DotCMSContentlet, FeaturedFlags } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import {
    DotSidePanelConfig,
    DotSidePanelOutletComponent,
    DotSidePanelRef,
    DotSidePanelService
} from '@dotcms/ui';
import { createFakeContentlet, createFakeContentType } from '@dotcms/utils-testing';

import { DotEditContentOpenerService } from './dot-edit-content-opener.service';
import { DotPanelUrlStateService } from './url/dot-panel-url-state.service';

import { DotEditContentRequest } from '../models/dot-edit-content-request.model';
import { DotOpenContentEvent } from '../models/dot-open-content-event.model';

/**
 * The two panel components, as the loaders hand them to the opener. Stubs with a name each, so a
 * test can tell which editor the opener picked without loading either editor's chunk.
 */
const { NewEditorStub, LegacyEditorStub, loaded } = vi.hoisted(() => {
    const LegacyEditorStub = class LegacyEditorStub {};

    return {
        NewEditorStub: class NewEditorStub {},
        LegacyEditorStub,
        /** What the legacy loader hands over; a test that renders the panels swaps in a component. */
        loaded: { legacy: LegacyEditorStub as unknown }
    };
});

vi.mock('../components/dot-edit-content-side-panel/load-dot-edit-content-side-panel', () => ({
    loadDotEditContentSidePanel: () => Promise.resolve(NewEditorStub)
}));

vi.mock('../components/dot-legacy-editor-side-panel/load-dot-legacy-editor-side-panel', () => ({
    loadDotLegacyEditorSidePanel: () => Promise.resolve(loaded.legacy)
}));

const NEW_TYPE = createFakeContentType({
    id: 'blog-id',
    variable: 'Blog',
    name: 'Blog',
    metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
});

const LEGACY_TYPE = createFakeContentType({
    id: 'banner-id',
    variable: 'Banner',
    name: 'Banner',
    metadata: {}
});

const CREATE_URL = '/c/portal/layout?p_p_id=content&_content_cmd=new&selectedStructure=banner-id';

/** Provided only by the injector a test hands over as the asking panel's. */
const ASKING_PANEL = new InjectionToken<string>('ASKING_PANEL');

const EDIT_REQUEST: DotEditContentRequest = {
    mode: 'edit',
    inode: 'abc',
    identifier: 'id-1',
    contentType: 'Blog',
    languageId: 1,
    title: 'My post'
};

const CREATE_REQUEST: DotEditContentRequest = {
    mode: 'new',
    contentType: 'Banner',
    languageId: 2,
    title: 'Banner',
    folderInode: 'f-1'
};

describe('DotEditContentOpenerService', () => {
    let spectator: SpectatorService<DotEditContentOpenerService>;
    let open: Mock;
    let resolveEditor: Mock;
    let getCreateContentletUrl: Mock;
    let getContentletByInode: Mock;
    let searchContent: Mock;
    let handleError: Mock;
    let injector: Injector;

    const createService = createServiceFactory({
        service: DotEditContentOpenerService,
        providers: [
            mockProvider(DotSidePanelService, {
                // A real ref, so `closed` and `isClosed` behave as the outlet's would.
                open: vi.fn(() => new DotSidePanelRef(() => undefined))
            }),
            mockProvider(DotContentletEditUrlService, { resolveEditor: vi.fn() }),
            mockProvider(DotActionUrlService, { getCreateContentletUrl: vi.fn() }),
            mockProvider(DotContentletService, { getContentletByInode: vi.fn() }),
            mockProvider(DotContentSearchService, { get: vi.fn() }),
            mockProvider(DotHttpErrorManagerService, { handle: vi.fn() }),
            mockProvider(DotPanelUrlStateService, { register: vi.fn(), update: vi.fn() })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        open = spectator.inject(DotSidePanelService).open as Mock;
        resolveEditor = spectator.inject(DotContentletEditUrlService).resolveEditor as Mock;
        getCreateContentletUrl = spectator.inject(DotActionUrlService)
            .getCreateContentletUrl as Mock;
        getContentletByInode = spectator.inject(DotContentletService).getContentletByInode as Mock;
        searchContent = spectator.inject(DotContentSearchService).get as Mock;
        handleError = spectator.inject(DotHttpErrorManagerService).handle as Mock;
        injector = TestBed.inject(Injector);

        vi.clearAllMocks();
        open.mockImplementation(() => new DotSidePanelRef(() => undefined));
        resolveEditor.mockImplementation((variable: string) =>
            of(
                variable === 'Blog'
                    ? { editor: 'new', contentType: NEW_TYPE }
                    : { editor: 'legacy', contentType: LEGACY_TYPE }
            )
        );
        getCreateContentletUrl.mockReturnValue(of(CREATE_URL));
    });

    /** The config the opener handed the outlet, and the component it will show once resolved. */
    const lastOpen = async (): Promise<{
        config: DotSidePanelConfig;
        component: Type<unknown>;
    }> => {
        const [component, config] = open.mock.calls[open.mock.calls.length - 1] as [
            Promise<Type<unknown>>,
            DotSidePanelConfig
        ];

        return { config, component: await component };
    };

    /** Reads a bound input, which the opener passes as a signal so it can follow the request. */
    const input = <T>(config: DotSidePanelConfig, name: string): T => {
        const value = config.inputs?.[name];

        return (typeof value === 'function' ? (value as () => T)() : value) as T;
    };

    const heading = (config: DotSidePanelConfig): string =>
        typeof config.heading === 'string' ? config.heading : config.heading();

    describe('validation', () => {
        it('opens nothing and reports the error for an edit without an inode', () => {
            const ref = spectator.service.open({ mode: 'edit' }, { injector });

            expect(open).not.toHaveBeenCalled();
            expect(handleError).toHaveBeenCalledTimes(1);
            expect(ref.isClosed).toBe(true);
        });

        it('opens nothing and reports the error for a create without a content type', () => {
            const ref = spectator.service.open({ mode: 'new' }, { injector });

            expect(open).not.toHaveBeenCalled();
            expect(handleError).toHaveBeenCalledTimes(1);
            expect(ref.isClosed).toBe(true);
        });
    });

    /**
     * A URL names content by `editContent` and `editContentLang`, never by inode, so a surface
     * opening from a URL hands the opener the identifier and the language. The opener finds the
     * working version itself, with Content Drive's two-step lookup, and goes on as for an edit by
     * inode (#38303, FR-049).
     */
    describe('an edit named by its identifier', () => {
        const IDENTIFIER = '3f0c8e5a-7b2d-4c1e-9a6f-0d4b8e2c1a57';

        /** What `/api/content/_search` answers: the matching working versions, at most one. */
        const searchResult = (...contentlets: DotCMSContentlet[]) =>
            of({ jsonObjectView: { contentlets } });

        const SPANISH = createFakeContentlet({
            inode: 'es-inode',
            identifier: IDENTIFIER,
            contentType: 'Blog',
            languageId: 2,
            title: 'Mi entrada'
        });

        const ENGLISH = createFakeContentlet({
            inode: 'en-inode',
            identifier: IDENTIFIER,
            contentType: 'Blog',
            languageId: 1,
            title: 'My post'
        });

        it('opens the working version in the language the request names, as an edit by inode', async () => {
            searchContent.mockReturnValue(searchResult(SPANISH));

            const ref = spectator.service.open(
                { mode: 'edit', identifier: IDENTIFIER, languageId: 2 },
                { injector }
            );
            const { config, component } = await lastOpen();

            expect(ref.isClosed).toBe(false);
            expect(handleError).not.toHaveBeenCalled();
            expect(searchContent).toHaveBeenCalledTimes(1);
            expect(searchContent).toHaveBeenCalledWith({
                query: `+identifier:${IDENTIFIER} +working:true +languageId:2`,
                limit: 1
            });
            expect(component).toBe(NewEditorStub);
            expect(input<DotEditContentRequest>(config, 'request')).toEqual(
                expect.objectContaining({
                    mode: 'edit',
                    inode: 'es-inode',
                    identifier: IDENTIFIER,
                    contentType: 'Blog',
                    languageId: 2,
                    title: 'Mi entrada'
                })
            );
        });

        // A link to content with no version in that language still opens.
        it('falls back to the working version in any language when it has none in that one', async () => {
            searchContent
                .mockReturnValueOnce(searchResult())
                .mockReturnValueOnce(searchResult(ENGLISH));

            spectator.service.open(
                { mode: 'edit', identifier: IDENTIFIER, languageId: 2 },
                { injector }
            );
            const { config } = await lastOpen();

            expect(searchContent).toHaveBeenCalledTimes(2);
            expect(searchContent).toHaveBeenLastCalledWith({
                query: `+identifier:${IDENTIFIER} +working:true`,
                limit: 1
            });
            // The version that opened, not the one asked for.
            expect(input<DotEditContentRequest>(config, 'request')).toEqual(
                expect.objectContaining({ inode: 'en-inode', languageId: 1 })
            );
        });

        it('looks up any language at once when the request names none', async () => {
            searchContent.mockReturnValue(searchResult(ENGLISH));

            spectator.service.open({ mode: 'edit', identifier: IDENTIFIER }, { injector });
            await lastOpen();

            expect(searchContent).toHaveBeenCalledTimes(1);
            expect(searchContent).toHaveBeenCalledWith({
                query: `+identifier:${IDENTIFIER} +working:true`,
                limit: 1
            });
        });

        it('shows the standard error and closes the panel when nothing matches', () => {
            searchContent.mockReturnValue(searchResult());

            const ref = spectator.service.open(
                { mode: 'edit', identifier: IDENTIFIER, languageId: 2 },
                { injector }
            );

            expect(handleError).toHaveBeenCalledTimes(1);
            expect(ref.isClosed).toBe(true);
        });

        /**
         * A link names content by identifier: the URL already names the panel while the lookup
         * runs, so a lookup that finds nothing closes a panel the URL service knows, which takes
         * its params out of the URL (the spec's "URL names content the author cannot open").
         */
        it('names the panel in the URL before the lookup, so a refused link leaves no params', () => {
            const register = spectator.inject(DotPanelUrlStateService).register as Mock;
            const lookup = new Subject<unknown>();
            // Nothing in the language asked for, then nothing in any language.
            searchContent.mockReturnValueOnce(lookup).mockReturnValueOnce(searchResult());

            const ref = spectator.service.open(
                { mode: 'edit', identifier: IDENTIFIER, languageId: 2 },
                { injector, url: {} }
            );

            expect(register).toHaveBeenCalledTimes(1);
            expect(register.mock.calls[0][0] === ref).toBe(true);
            expect(register.mock.calls[0][1]).toEqual({
                editContent: IDENTIFIER,
                editContentLang: '2'
            });

            lookup.next({ jsonObjectView: { contentlets: [] } });

            expect(handleError).toHaveBeenCalledTimes(1);
            expect(ref.isClosed).toBe(true);
        });

        it('renames the panel in place to the version found in another language', () => {
            const urlState = spectator.inject(DotPanelUrlStateService);
            resolveEditor.mockReturnValue(NEVER);
            searchContent
                .mockReturnValueOnce(searchResult())
                .mockReturnValueOnce(searchResult(ENGLISH));

            const ref = spectator.service.open(
                { mode: 'edit', identifier: IDENTIFIER, languageId: 2 },
                { injector, url: {} }
            );

            expect(urlState.register).toHaveBeenCalledTimes(1);
            expect(urlState.update).toHaveBeenCalledTimes(1);
            const [renamed, params] = (urlState.update as Mock).mock.calls[0];
            expect(renamed === ref).toBe(true);
            expect(params).toEqual({ editContent: IDENTIFIER, editContentLang: '1' });
        });

        // The identifier reaches a Lucene query: a stray space would make the lookup miss.
        it('looks an identifier up without the spaces around it', () => {
            searchContent.mockReturnValue(searchResult(ENGLISH));

            spectator.service.open({ mode: 'edit', identifier: ` ${IDENTIFIER} ` }, { injector });

            expect(searchContent).toHaveBeenCalledWith({
                query: `+identifier:${IDENTIFIER} +working:true`,
                limit: 1
            });
        });

        // The identifier reaches a Lucene query.
        it('refuses an identifier that is not one, without searching', () => {
            const ref = spectator.service.open(
                { mode: 'edit', identifier: `${IDENTIFIER} OR +working:true` },
                { injector }
            );

            expect(searchContent).not.toHaveBeenCalled();
            expect(handleError).toHaveBeenCalledTimes(1);
            expect(ref.isClosed).toBe(true);
        });

        it('names the version that opened in the URL', () => {
            const register = spectator.inject(DotPanelUrlStateService).register as Mock;
            resolveEditor.mockReturnValue(NEVER);
            searchContent.mockReturnValue(searchResult(SPANISH));

            spectator.service.open(
                { mode: 'edit', identifier: IDENTIFIER, languageId: 2 },
                { injector, url: {} }
            );

            expect(register).toHaveBeenCalledTimes(1);
            expect(register.mock.calls[0][1]).toEqual({
                editContent: IDENTIFIER,
                editContentLang: '2'
            });
        });
    });

    describe('choosing the editor', () => {
        it('shows the new editor for a content type that opted in', async () => {
            spectator.service.open(EDIT_REQUEST, { injector });

            const { config, component } = await lastOpen();

            expect(component).toBe(NewEditorStub);
            expect(resolveEditor).toHaveBeenCalledWith('Blog');
            expect(input<DotEditContentRequest>(config, 'request')).toEqual(EDIT_REQUEST);
        });

        it('shows the legacy editor for a content type that did not', async () => {
            spectator.service.open({ ...EDIT_REQUEST, contentType: 'Banner' }, { injector });

            const { component } = await lastOpen();

            expect(component).toBe(LegacyEditorStub);
        });

        it('shows the legacy editor when the content type lookup failed, without an error toast', async () => {
            resolveEditor.mockReturnValue(of({ editor: 'legacy', contentType: null }));

            spectator.service.open({ ...EDIT_REQUEST, contentType: 'Gone' }, { injector });

            const { component } = await lastOpen();

            expect(component).toBe(LegacyEditorStub);
            expect(handleError).not.toHaveBeenCalled();
        });

        it('opens the panel at once, so the spinner covers the lookup', () => {
            spectator.service.open(EDIT_REQUEST, { injector });

            expect(open).toHaveBeenCalledTimes(1);
            expect(open.mock.calls[0][0]).toBeInstanceOf(Promise);
        });

        it("hands the opener's injector to the panel", async () => {
            spectator.service.open(EDIT_REQUEST, { injector });

            const { config } = await lastOpen();

            expect(config.injector).toBe(injector);
        });
    });

    describe('an edit without a content type or a title', () => {
        const contentlet: DotCMSContentlet = createFakeContentlet({
            inode: 'host-inode',
            identifier: 'host-id',
            contentType: 'Banner',
            title: 'Fetched title',
            languageId: 3
        });

        beforeEach(() => getContentletByInode.mockReturnValue(of(contentlet)));

        it('fetches the contentlet by inode and fills the request from it', async () => {
            spectator.service.open({ mode: 'edit', inode: 'host-inode' }, { injector });

            const { config, component } = await lastOpen();

            expect(getContentletByInode).toHaveBeenCalledWith('host-inode');
            expect(resolveEditor).toHaveBeenCalledWith('Banner');
            expect(component).toBe(LegacyEditorStub);
            expect(input<DotEditContentRequest>(config, 'request')).toEqual(
                expect.objectContaining({
                    inode: 'host-inode',
                    identifier: 'host-id',
                    contentType: 'Banner',
                    title: 'Fetched title',
                    languageId: 3
                })
            );
            expect(heading(config)).toBe('Fetched title');
        });

        it('does not fetch when the request already names the content type and the title', async () => {
            spectator.service.open(EDIT_REQUEST, { injector });
            await lastOpen();

            expect(getContentletByInode).not.toHaveBeenCalled();
        });

        /**
         * The heading falls back to the contentlet's title for an edit (edit-request.md), not to the
         * type's name: the legacy JSP events and the Publishing Queue name the type and no title.
         */
        it("heads an edit that names its content type but no title with the contentlet's title", async () => {
            spectator.service.open(
                { mode: 'edit', inode: 'host-inode', contentType: 'Banner' },
                { injector }
            );

            const { config } = await lastOpen();

            expect(getContentletByInode).toHaveBeenCalledWith('host-inode');
            expect(resolveEditor).toHaveBeenCalledWith('Banner');
            expect(heading(config)).toBe('Fetched title');
            expect(input<DotEditContentRequest>(config, 'request').title).toBe('Fetched title');
        });

        it('reports the error and closes the panel when the fetch fails', async () => {
            getContentletByInode.mockReturnValue(throwError(() => new Error('404')));

            const ref = spectator.service.open({ mode: 'edit', inode: 'gone' }, { injector });
            await Promise.resolve();
            await Promise.resolve();

            expect(handleError).toHaveBeenCalledTimes(1);
            expect(ref.isClosed).toBe(true);
        });
    });

    describe('a legacy create', () => {
        it('resolves the create screen for the type and language before the editor shows', async () => {
            spectator.service.open(CREATE_REQUEST, { injector });

            const { config, component } = await lastOpen();

            expect(getCreateContentletUrl).toHaveBeenCalledWith('Banner', 2);
            expect(component).toBe(LegacyEditorStub);
            expect(input<string>(config, 'createUrl')).toBe(CREATE_URL);
        });

        it('asks for no create screen for a new-editor create', async () => {
            spectator.service.open({ ...CREATE_REQUEST, contentType: 'Blog' }, { injector });

            const { component } = await lastOpen();

            expect(component).toBe(NewEditorStub);
            expect(getCreateContentletUrl).not.toHaveBeenCalled();
        });

        it('reports the error and closes the panel when the server refuses the create', async () => {
            getCreateContentletUrl.mockReturnValue(throwError(() => new Error('403')));

            const ref = spectator.service.open(CREATE_REQUEST, { injector });
            await Promise.resolve();
            await Promise.resolve();

            expect(handleError).toHaveBeenCalledTimes(1);
            expect(ref.isClosed).toBe(true);
        });
    });

    /**
     * "Switch to the old editor" reopens content whose type was just set back to the legacy
     * editor while its old choice is still remembered, so it forces the legacy editor (#38303).
     */
    describe('a forced legacy editor', () => {
        it('shows the legacy editor without asking which editor the type uses', async () => {
            spectator.service.open(EDIT_REQUEST, { injector, editor: 'legacy' });

            const { component } = await lastOpen();

            expect(component).toBe(LegacyEditorStub);
            expect(resolveEditor).not.toHaveBeenCalled();
        });

        it('still resolves the create screen for a forced legacy create', async () => {
            spectator.service.open(
                { ...CREATE_REQUEST, contentType: 'Blog' },
                { injector, editor: 'legacy' }
            );

            const { config, component } = await lastOpen();

            expect(component).toBe(LegacyEditorStub);
            expect(getCreateContentletUrl).toHaveBeenCalledWith('Blog', 2);
            expect(input<string>(config, 'createUrl')).toBe(CREATE_URL);
        });
    });

    describe('heading', () => {
        it("uses the request's title when it has one", async () => {
            spectator.service.open(EDIT_REQUEST, { injector });

            const { config } = await lastOpen();

            expect(heading(config)).toBe('My post');
        });

        it("falls back to the content type's name for a create", async () => {
            spectator.service.open({ ...CREATE_REQUEST, title: undefined }, { injector });

            const { config } = await lastOpen();

            expect(heading(config)).toBe('Banner');
        });
    });

    describe('events', () => {
        it('binds each handler to the output of the same name', async () => {
            const handlers = {
                saved: vi.fn(),
                languageChanged: vi.fn(),
                pageEditorRequested: vi.fn(),
                deleted: vi.fn()
            };
            spectator.service.open(EDIT_REQUEST, { injector, ...handlers });
            const { config } = await lastOpen();
            const contentlet = createFakeContentlet({ inode: 'abc2' });

            config.outputs?.['saved'](contentlet as never);
            config.outputs?.['languageChanged'](2 as never);
            config.outputs?.['pageEditorRequested']({ url: '/p', languageId: 1 } as never);
            config.outputs?.['deleted'](undefined as never);

            expect(handlers.saved).toHaveBeenCalledWith(contentlet);
            expect(handlers.languageChanged).toHaveBeenCalledWith(2);
            expect(handlers.pageEditorRequested).toHaveBeenCalledWith({
                url: '/p',
                languageId: 1
            });
            expect(handlers.deleted).toHaveBeenCalledTimes(1);
        });

        it('reports closed from the ref, so it fires even before the content loaded', () => {
            const closed = vi.fn();

            const ref = spectator.service.open(EDIT_REQUEST, { injector, closed });
            ref.close();

            expect(closed).toHaveBeenCalledTimes(1);
        });

        it('reports closed once', async () => {
            const closed = vi.fn();
            const ref = spectator.service.open(EDIT_REQUEST, { injector, closed });
            await lastOpen();

            ref.close();
            ref.close();

            expect(closed).toHaveBeenCalledTimes(1);
        });
    });

    /**
     * Every panel the opener opens, at any depth and in either editor, announces its saves and
     * deletes on `DotContentChangeService`, after its own handlers, so a listing surface refreshes
     * on a save in a panel it did not open itself (#38303, T174).
     */
    describe('the content change notification', () => {
        /** Every change announced from now on, in order. */
        const announced = (): DotContentChange[] => {
            const changes: DotContentChange[] = [];
            spectator
                .inject(DotContentChangeService)
                .changes$.subscribe((change) => changes.push(change));

            return changes;
        };

        /** Stacks a panel over `parent`, as a relationship's "New" asks, and returns its config. */
        const stackOver = async (parent: DotSidePanelConfig): Promise<DotSidePanelConfig> => {
            parent.outputs?.['openContent']({
                request: { mode: 'edit', inode: 'rel-1', contentType: 'Blog', title: 'Related' }
            } as never);

            return (await lastOpen()).config;
        };

        it("announces a save of a panel a surface opened, at depth 0, after the panel's own handler", async () => {
            const order: string[] = [];
            const changes = announced();
            spectator
                .inject(DotContentChangeService)
                .changes$.subscribe(() => order.push('notification'));
            spectator.service.open(EDIT_REQUEST, {
                injector,
                saved: () => order.push('handler')
            });
            const { config } = await lastOpen();
            const contentlet = createFakeContentlet({ inode: 'abc2', identifier: 'id-1' });

            config.outputs?.['saved'](contentlet as never);

            expect(changes).toEqual([{ kind: 'saved', contentlet, depth: 0 }]);
            expect(order).toEqual(['handler', 'notification']);
        });

        it('announces a save of a panel stacked from inside another, one level deeper', async () => {
            spectator.service.open(EDIT_REQUEST, { injector });
            const stacked = await stackOver((await lastOpen()).config);
            const deeper = await stackOver(stacked);
            const changes = announced();
            const contentlet = createFakeContentlet({ inode: 'rel-2' });

            stacked.outputs?.['saved'](contentlet as never);
            deeper.outputs?.['saved'](contentlet as never);

            expect(changes).toEqual([
                { kind: 'saved', contentlet, depth: 1 },
                { kind: 'saved', contentlet, depth: 2 }
            ]);
        });

        it("announces a save in the legacy editor's panel", async () => {
            const changes = announced();
            spectator.service.open(CREATE_REQUEST, { injector });
            const { config, component } = await lastOpen();
            const contentlet = createFakeContentlet({ inode: 'new-banner' });

            config.outputs?.['saved'](contentlet as never);

            expect(component === loaded.legacy).toBe(true);
            expect(changes).toEqual([{ kind: 'saved', contentlet, depth: 0 }]);
        });

        it('announces a delete with the content the panel had, after its own handler', async () => {
            const deleted = vi.fn();
            const changes = announced();
            spectator.service.open(EDIT_REQUEST, { injector, deleted });
            const { config } = await lastOpen();

            config.outputs?.['deleted'](undefined as never);

            expect(deleted).toHaveBeenCalledTimes(1);
            expect(changes).toEqual([
                {
                    kind: 'deleted',
                    contentlet: expect.objectContaining({ inode: 'abc', identifier: 'id-1' }),
                    depth: 0
                }
            ]);
        });

        it('announces a delete with the content as it was last saved', async () => {
            const changes = announced();
            spectator.service.open(EDIT_REQUEST, { injector });
            const { config } = await lastOpen();
            const saved = createFakeContentlet({ inode: 'abc3', identifier: 'id-1' });

            config.outputs?.['saved'](saved as never);
            config.outputs?.['deleted'](undefined as never);

            expect(changes[1]).toEqual({ kind: 'deleted', contentlet: saved, depth: 0 });
        });

        it('announces nothing for a panel that only closes', async () => {
            const changes = announced();
            const ref = spectator.service.open(EDIT_REQUEST, { injector });
            await lastOpen();

            ref.close();

            expect(changes).toEqual([]);
        });
    });

    describe('content that opens other content', () => {
        it('stacks a second panel for an openContent request, under the panel that asked', async () => {
            spectator.service.open(EDIT_REQUEST, { injector });
            const { config: first } = await lastOpen();
            // The asking panel's own injector, which the panel puts on the event.
            const panelInjector = Injector.create({
                providers: [{ provide: ASKING_PANEL, useValue: 'the asking panel' }],
                parent: injector
            });
            const event = {
                request: { mode: 'edit', inode: 'rel-1', contentType: 'Banner', title: 'Related' },
                onSaved: vi.fn(),
                injector: panelInjector
            } as DotOpenContentEvent;

            first.outputs?.['openContent'](event as never);
            const { config: second, component } = await lastOpen();

            expect(open).toHaveBeenCalledTimes(2);
            expect(component).toBe(LegacyEditorStub);
            // Compared through a token: a failing `toBe` on two injectors prints both graphs.
            expect(second.injector.get(ASKING_PANEL, null)).toBe('the asking panel');
            expect(input<DotEditContentRequest>(second, 'request')).toEqual(event.request);
        });

        /**
         * #38303 (US6) — stacking appends: the outlet keeps the panel underneath as it was, and
         * closing the one on top leaves it open. The fake outlet records the panels on screen.
         */
        it('appends the stacked panel, and closing it leaves the first open and unchanged', async () => {
            const onScreen: DotSidePanelRef[] = [];
            open.mockImplementation(() => {
                const ref: DotSidePanelRef = new DotSidePanelRef(() =>
                    onScreen.splice(onScreen.indexOf(ref), 1)
                );
                onScreen.push(ref);

                return ref;
            });

            const first = spectator.service.open(EDIT_REQUEST, { injector });
            const { config: firstConfig } = await lastOpen();
            // With a title, so nothing is fetched: an edit without one is fetched for its heading.
            firstConfig.outputs?.['openContent']({
                request: { mode: 'edit', inode: 'rel-1', contentType: 'Banner', title: 'Related' }
            } as never);
            await lastOpen();

            expect(onScreen.length, 'two panels on screen').toBe(2);
            const second = onScreen[1];
            expect(second === first).toBe(false);

            second.close();

            expect(onScreen.length).toBe(1);
            expect(onScreen[0] === first).toBe(true);
            expect(first.isClosed).toBe(false);
            expect(heading(firstConfig)).toBe('My post');
            expect(input<DotEditContentRequest>(firstConfig, 'request')).toEqual(EDIT_REQUEST);
        });

        it("routes the stacked panel's saves to the event's reply", async () => {
            spectator.service.open(EDIT_REQUEST, { injector });
            const { config: first } = await lastOpen();
            const onSaved = vi.fn();
            const contentlet = createFakeContentlet({ inode: 'rel-2' });

            first.outputs?.['openContent']({
                request: { mode: 'new', contentType: 'Blog' },
                onSaved
            } as never);
            const { config: second } = await lastOpen();
            second.outputs?.['saved'](contentlet as never);

            expect(onSaved).toHaveBeenCalledWith(contentlet);
        });
    });

    describe('a request that is a signal', () => {
        it('binds the signal itself, so heading and inputs follow later changes', async () => {
            const request = signal<DotEditContentRequest>(EDIT_REQUEST);
            runInInjectionContext(injector, () => spectator.service.open(request, { injector }));
            const { config } = await lastOpen();

            request.set({ ...EDIT_REQUEST, title: 'Renamed' });

            expect(heading(config)).toBe('Renamed');
            expect(input<DotEditContentRequest>(config, 'request').title).toBe('Renamed');
        });
    });

    /**
     * With the `url` option the opener hands each panel to `DotPanelUrlStateService`, which writes
     * it to the surface's URL; the surface never writes the URL itself (#38303, FR-044 to FR-050).
     */
    describe('the panel in the URL', () => {
        let register: Mock;
        let update: Mock;

        beforeEach(() => {
            const urlState = spectator.inject(DotPanelUrlStateService);
            register = urlState.register as Mock;
            update = urlState.update as Mock;
        });

        /** The ref and params the opener registered, by call. Refs compared by identity. */
        const registered = (call: number) => {
            const [ref, params] = register.mock.calls[call] as [
                DotSidePanelRef,
                Record<string, string>
            ];

            return { ref, params };
        };

        // The editor is still being chosen: the URL names the panel while its spinner shows.
        it('registers an edit that names its identifier and language at once, with them', () => {
            resolveEditor.mockReturnValue(NEVER);

            const ref = spectator.service.open(EDIT_REQUEST, { injector, url: {} });

            expect(register).toHaveBeenCalledTimes(1);
            expect(registered(0).ref === ref).toBe(true);
            expect(registered(0).params).toEqual({ editContent: 'id-1', editContentLang: '1' });
        });

        it('registers an edit without a content type once its contentlet is fetched, with its identifier and language', () => {
            const fetched = new Subject<DotCMSContentlet>();
            getContentletByInode.mockReturnValue(fetched);

            const ref = spectator.service.open(
                { mode: 'edit', inode: 'host-inode' },
                { injector, url: {} }
            );

            expect(register).not.toHaveBeenCalled();

            fetched.next(
                createFakeContentlet({
                    inode: 'host-inode',
                    identifier: 'host-id',
                    contentType: 'Banner',
                    languageId: 3
                })
            );

            expect(register).toHaveBeenCalledTimes(1);
            expect(registered(0).ref === ref).toBe(true);
            expect(registered(0).params).toEqual({
                editContent: 'host-id',
                editContentLang: '3'
            });
        });

        // The Publishing Queue's dialogs name the inode and the content type, not the identifier.
        it('fetches an edit that names no identifier, so the URL can name it', () => {
            getContentletByInode.mockReturnValue(
                of(
                    createFakeContentlet({
                        inode: 'abc',
                        identifier: 'fetched-id',
                        contentType: 'Blog',
                        languageId: 2
                    })
                )
            );

            spectator.service.open(
                { mode: 'edit', inode: 'abc', contentType: 'Blog' },
                { injector, url: {} }
            );

            expect(getContentletByInode).toHaveBeenCalledWith('abc');
            expect(register).toHaveBeenCalledTimes(1);
            expect(registered(0).params).toEqual({
                editContent: 'fetched-id',
                editContentLang: '2'
            });
        });

        it("registers a create at once, with its content type and the caller's extra params", () => {
            resolveEditor.mockReturnValue(NEVER);

            const ref = spectator.service.open(CREATE_REQUEST, {
                injector,
                url: { extraParams: { folder: 'f-1' } }
            });

            expect(register).toHaveBeenCalledTimes(1);
            expect(registered(0).ref === ref).toBe(true);
            expect(registered(0).params).toEqual({ createContent: 'Banner', folder: 'f-1' });
        });

        it('never tells the service about a panel opened without the option', async () => {
            getContentletByInode.mockReturnValue(
                of(createFakeContentlet({ inode: 'host-inode', identifier: 'host-id' }))
            );

            spectator.service.open({ mode: 'edit', inode: 'host-inode' }, { injector });
            spectator.service.open({ ...CREATE_REQUEST, contentType: 'Blog' }, { injector });
            const { config } = await lastOpen();
            config.outputs?.['saved']?.(createFakeContentlet({ identifier: 'new-id' }) as never);
            spectator.service.open(EDIT_REQUEST, { injector });
            const { config: edit } = await lastOpen();
            edit.outputs?.['languageChanged']?.(2 as never);

            expect(register).not.toHaveBeenCalled();
            expect(update).not.toHaveBeenCalled();
        });

        it('rewrites the language in place when the editor switches it, and still tells the caller', async () => {
            const languageChanged = vi.fn();
            const ref = spectator.service.open(EDIT_REQUEST, {
                injector,
                languageChanged,
                url: {}
            });
            const { config } = await lastOpen();

            config.outputs?.['languageChanged'](2 as never);

            expect(update).toHaveBeenCalledTimes(1);
            const [updatedRef, params] = update.mock.calls[0];
            expect(updatedRef === ref).toBe(true);
            expect(params).toEqual({ editContent: 'id-1', editContentLang: '2' });
            expect(languageChanged).toHaveBeenCalledWith(2);
        });

        it("names the new content in place of the create on the create's first save, and only then", async () => {
            const ref = spectator.service.open(
                { ...CREATE_REQUEST, contentType: 'Blog' },
                { injector, url: {} }
            );
            const { config } = await lastOpen();
            const saved = createFakeContentlet({
                inode: 'new-inode',
                identifier: 'new-id',
                languageId: 2
            });

            config.outputs?.['saved'](saved as never);
            config.outputs?.['saved']({ ...saved, inode: 'newer-inode' } as never);

            expect(update).toHaveBeenCalledTimes(1);
            const [updatedRef, params] = update.mock.calls[0];
            expect(updatedRef === ref).toBe(true);
            expect(params).toEqual({ editContent: 'new-id', editContentLang: '2' });
        });

        it("leaves the URL as it is when an edit's content is saved in the language it names", async () => {
            spectator.service.open(EDIT_REQUEST, { injector, url: {} });
            const { config } = await lastOpen();

            config.outputs?.['saved'](
                createFakeContentlet({ inode: 'abc2', identifier: 'id-1', languageId: 1 }) as never
            );

            expect(update).not.toHaveBeenCalled();
        });

        // Switching to a language the content lacks reports no language: the new translation's
        // first save is the first the URL hears of it. Content Drive did this itself until #38303.
        it('names the language of a new translation in place when it is first saved', async () => {
            const ref = spectator.service.open(EDIT_REQUEST, { injector, url: {} });
            const { config } = await lastOpen();

            config.outputs?.['saved'](
                createFakeContentlet({
                    inode: 'abc-es',
                    identifier: 'id-1',
                    languageId: 2
                }) as never
            );

            expect(update).toHaveBeenCalledTimes(1);
            const [updatedRef, params] = update.mock.calls[0];
            expect(updatedRef === ref).toBe(true);
            expect(params).toEqual({ editContent: 'id-1', editContentLang: '2' });
        });

        // Related content opened in place inside the panel saves under another identifier.
        it('leaves the URL as it is when other content is saved inside the panel', async () => {
            spectator.service.open(EDIT_REQUEST, { injector, url: {} });
            const { config } = await lastOpen();

            config.outputs?.['saved'](
                createFakeContentlet({
                    inode: 'rel-2',
                    identifier: 'rel-id',
                    languageId: 2
                }) as never
            );

            expect(update).not.toHaveBeenCalled();
        });

        it('registers the panel in place when the option asks for it', () => {
            resolveEditor.mockReturnValue(NEVER);

            const ref = spectator.service.open(EDIT_REQUEST, {
                injector,
                url: { inPlace: true }
            });

            expect(register).toHaveBeenCalledTimes(1);
            const [registeredRef, params, how] = register.mock.calls[0];
            expect(registeredRef === ref).toBe(true);
            expect(params).toEqual({ editContent: 'id-1', editContentLang: '1' });
            expect(how).toEqual({ inPlace: true });
        });

        it('registers a panel opened from inside one with an entry of its own, even when the first was in place', async () => {
            getContentletByInode.mockReturnValue(
                of(
                    createFakeContentlet({
                        inode: 'rel-1',
                        identifier: 'rel-id',
                        contentType: 'Banner',
                        languageId: 2
                    })
                )
            );
            spectator.service.open(EDIT_REQUEST, { injector, url: { inPlace: true } });
            const { config: first } = await lastOpen();

            first.outputs?.['openContent']({ request: { mode: 'edit', inode: 'rel-1' } } as never);
            await lastOpen();

            expect(register).toHaveBeenCalledTimes(2);
            expect(register.mock.calls[1]).toHaveLength(2);
        });

        it('passes the option to a panel opened from inside one, so the stacked panel registers its own params', async () => {
            getContentletByInode.mockReturnValue(
                of(
                    createFakeContentlet({
                        inode: 'rel-1',
                        identifier: 'rel-id',
                        contentType: 'Banner',
                        languageId: 2
                    })
                )
            );
            spectator.service.open(EDIT_REQUEST, { injector, url: {} });
            const { config: first } = await lastOpen();

            first.outputs?.['openContent']({ request: { mode: 'edit', inode: 'rel-1' } } as never);
            await lastOpen();

            expect(register).toHaveBeenCalledTimes(2);
            expect(registered(1).ref === open.mock.results[1].value).toBe(true);
            expect(registered(1).params).toEqual({ editContent: 'rel-id', editContentLang: '2' });
        });

        // A relationship's "New" from either editor stacks a create (#38303, QA item 1).
        it('writes a create stacked on a written panel to the URL at once', async () => {
            spectator.service.open(EDIT_REQUEST, { injector, url: {} });
            const { config: first } = await lastOpen();
            resolveEditor.mockReturnValue(NEVER);

            first.outputs?.['openContent']({
                request: { mode: 'new', contentType: 'Blog' }
            } as never);

            expect(register).toHaveBeenCalledTimes(2);
            expect(registered(1).params).toEqual({ createContent: 'Blog' });
        });

        // UVE opens its panels unwritten: what they stack stays out of the URL too.
        it('leaves out of the URL what is stacked on a panel opened without the option', async () => {
            getContentletByInode.mockReturnValue(
                of(createFakeContentlet({ inode: 'rel-1', identifier: 'rel-id', languageId: 2 }))
            );
            spectator.service.open(EDIT_REQUEST, { injector });
            const { config: first } = await lastOpen();

            first.outputs?.['openContent']({ request: { mode: 'edit', inode: 'rel-1' } } as never);
            await lastOpen();
            first.outputs?.['openContent']({
                request: { mode: 'new', contentType: 'Blog' }
            } as never);
            await lastOpen();

            expect(open).toHaveBeenCalledTimes(3);
            expect(register).not.toHaveBeenCalled();
        });
    });
});

/** Every {@link AskingPanelComponent} the outlet created, in order. */
const askingPanels: AskingPanelComponent[] = [];

/**
 * Panel content that asks to open other content the way both editor panels do: an `openContent`
 * event carrying its own injector. It has `saved` and `deleted` as both editors do: the opener
 * always listens to them, for the content change notification (T174).
 */
@Component({ selector: 'dot-test-asking-panel', template: '' })
class AskingPanelComponent {
    readonly request = input<DotEditContentRequest | null>(null);
    readonly createUrl = input<string | null>(null);
    readonly saved = output<DotCMSContentlet>();
    readonly deleted = output<void>();
    readonly openContent = output<DotOpenContentEvent>();
    readonly #injector = inject(Injector);

    constructor() {
        askingPanels.push(this);
    }

    ask(request: DotEditContentRequest): void {
        this.openContent.emit({ request, injector: this.#injector } as DotOpenContentEvent);
    }
}

/**
 * A stacked panel belongs to the panel that asked for it (open-content-from-fields.md), so it
 * closes with that panel, whatever closes it: the X, a delete, or the surface clearing its request
 * on browser Back. Through the real side-panel service and outlet.
 */
describe('DotEditContentOpenerService, panels stacked through the outlet', () => {
    let outlet: ComponentFixture<DotSidePanelOutletComponent>;
    let sidePanels: DotSidePanelService;
    let opener: DotEditContentOpenerService;

    const createService = createServiceFactory({
        service: DotEditContentOpenerService,
        providers: [
            mockProvider(DotContentletEditUrlService, {
                resolveEditor: vi.fn(() => of({ editor: 'legacy', contentType: LEGACY_TYPE }))
            }),
            mockProvider(DotActionUrlService),
            mockProvider(DotContentletService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotMessageService, { get: (key: string) => key }),
            // Opening a panel collapses the main navigation.
            mockProvider(GlobalStore, {
                isNavigationCollapsed: vi.fn().mockReturnValue(false),
                collapseNavigation: vi.fn(),
                expandNavigation: vi.fn()
            })
        ]
    });

    beforeEach(() => {
        askingPanels.length = 0;
        loaded.legacy = AskingPanelComponent;

        const spectator = createService();
        opener = spectator.service;
        sidePanels = spectator.inject(DotSidePanelService);
        outlet = TestBed.createComponent(DotSidePanelOutletComponent);
        outlet.detectChanges();
    });

    afterEach(() => {
        loaded.legacy = LegacyEditorStub;
    });

    /** Lets the editor choice resolve and the content load, then renders it. */
    const render = async (): Promise<void> => {
        await new Promise((resolve) => setTimeout(resolve));
        outlet.detectChanges();
    };

    it('closes the stacked panel when the panel that asked for it closes', async () => {
        const host = opener.open(
            { ...EDIT_REQUEST, contentType: 'Banner' },
            { injector: TestBed.inject(Injector) }
        );
        await render();

        askingPanels[0].ask({ mode: 'edit', inode: 'rel-1', contentType: 'Banner', title: 'Rel' });
        await render();

        // Lengths, not the arrays: a failure would print every panel's injector.
        expect(askingPanels.length).toBe(2);
        expect(sidePanels.panels().length).toBe(2);

        host.close();
        outlet.detectChanges();

        expect(sidePanels.panels().length).toBe(0);
    });
});
