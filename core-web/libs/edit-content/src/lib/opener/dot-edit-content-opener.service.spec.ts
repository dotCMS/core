import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';
import { Mock, vi } from 'vitest';

import { Injector, Type, runInInjectionContext, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import {
    DotActionUrlService,
    DotContentletEditUrlService,
    DotContentletService,
    DotHttpErrorManagerService
} from '@dotcms/data-access';
import { DotCMSContentlet, FeaturedFlags } from '@dotcms/dotcms-models';
import { DotSidePanelConfig, DotSidePanelRef, DotSidePanelService } from '@dotcms/ui';
import { createFakeContentlet, createFakeContentType } from '@dotcms/utils-testing';

import { DotEditContentOpenerService } from './dot-edit-content-opener.service';

import { DotEditContentRequest } from '../models/dot-edit-content-request.model';
import { DotOpenContentEvent } from '../models/dot-open-content-event.model';

/**
 * The two panel components, as the loaders hand them to the opener. Stubs with a name each, so a
 * test can tell which editor the opener picked without loading either editor's chunk.
 */
const { NewEditorStub, LegacyEditorStub } = vi.hoisted(() => ({
    NewEditorStub: class NewEditorStub {},
    LegacyEditorStub: class LegacyEditorStub {}
}));

vi.mock('../components/dot-edit-content-side-panel/load-dot-edit-content-side-panel', () => ({
    loadDotEditContentSidePanel: () => Promise.resolve(NewEditorStub)
}));

vi.mock('../components/dot-legacy-editor-side-panel/load-dot-legacy-editor-side-panel', () => ({
    loadDotLegacyEditorSidePanel: () => Promise.resolve(LegacyEditorStub)
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
            mockProvider(DotHttpErrorManagerService, { handle: vi.fn() })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        open = spectator.inject(DotSidePanelService).open as Mock;
        resolveEditor = spectator.inject(DotContentletEditUrlService).resolveEditor as Mock;
        getCreateContentletUrl = spectator.inject(DotActionUrlService)
            .getCreateContentletUrl as Mock;
        getContentletByInode = spectator.inject(DotContentletService).getContentletByInode as Mock;
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

    describe('an edit without a content type', () => {
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

        it('does not fetch when the request already names the content type', async () => {
            spectator.service.open(EDIT_REQUEST, { injector });
            await lastOpen();

            expect(getContentletByInode).not.toHaveBeenCalled();
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

    describe('content that opens other content', () => {
        it('stacks a second panel for an openContent request, with the same opener injector', async () => {
            spectator.service.open(EDIT_REQUEST, { injector });
            const { config: first } = await lastOpen();
            const event: DotOpenContentEvent = {
                request: { mode: 'edit', inode: 'rel-1', contentType: 'Banner' },
                onSaved: vi.fn()
            };

            first.outputs?.['openContent'](event as never);
            const { config: second, component } = await lastOpen();

            expect(open).toHaveBeenCalledTimes(2);
            expect(component).toBe(LegacyEditorStub);
            expect(second.injector).toBe(injector);
            expect(input<DotEditContentRequest>(second, 'request')).toEqual(event.request);
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
});
