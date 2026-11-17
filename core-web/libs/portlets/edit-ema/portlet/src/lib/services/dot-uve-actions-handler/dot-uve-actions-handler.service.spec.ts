import { Dispatcher } from '@ngrx/signals/events';
import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';
import { Mock, vi } from 'vitest';

import { signal } from '@angular/core';

import { MessageService } from 'primeng/api';

import { DotMessageService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';
import { DotCMSInlineEditingPayload, DotCMSUVEAction } from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';
import { DotCopyContentModalService } from '@dotcms/ui';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotUveActionsHandlerService } from './dot-uve-actions-handler.service';

import { uveContentEditorEvents } from '../../edit-ema-editor/components/dot-uve-content-editor/dot-uve-content-editor.events';
import { UpdatedContentlet } from '../../edit-ema-editor/components/ema-page-dropzone/types';
import { EDITOR_STATE, UVE_STATUS } from '../../shared/enums';
import { UVEStore } from '../../store/dot-uve.store';
import { PageType } from '../../store/models';
import { InlineEditService } from '../inline-edit/inline-edit.service';

const MOCK_UPDATED_CONTENTLET: UpdatedContentlet = {
    dataset: {
        inode: 'test-inode',
        fieldName: 'title',
        language: 'en',
        mode: 'edit'
    },
    content: 'New title value',
    eventType: 'update',
    isNotDirty: false
};

function buildMockStore(variantId = DEFAULT_VARIANT_ID) {
    return {
        pageVariantId: signal(variantId),
        // Edit mode, with edit permission on the page.
        editorCanEditContent: signal(true),
        setEditorState: vi.fn(),
        setUveStatus: vi.fn(),
        pageReload: vi.fn()
    };
}

describe('DotUveActionsHandlerService – UPDATE_CONTENTLET_INLINE_EDITING', () => {
    let spectator: SpectatorService<DotUveActionsHandlerService>;
    let service: DotUveActionsHandlerService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;

    const createService = createServiceFactory({
        service: DotUveActionsHandlerService,
        providers: [
            mockProvider(DotWorkflowActionsFireService, {
                saveContentlet: vi.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotMessageService),
            mockProvider(MessageService),
            mockProvider(DotCopyContentModalService),
            {
                provide: UVEStore,
                useValue: buildMockStore()
            }
        ]
    });

    beforeEach(() => {
        vi.clearAllMocks();
        spectator = createService();
        service = spectator.service;
        dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
    });

    it('should call saveContentlet with DEFAULT variantName when on the default variant', () => {
        const mockStore = buildMockStore(DEFAULT_VARIANT_ID);

        service.handleAction(
            {
                action: DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING,
                payload: MOCK_UPDATED_CONTENTLET
            },
            {
                uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                inlineEditingService: null,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: vi.fn()
            }
        );

        expect(dotWorkflowActionsFireService.saveContentlet).toHaveBeenCalledWith(
            expect.objectContaining({ variantName: DEFAULT_VARIANT_ID })
        );
    });

    it('should call saveContentlet with the active experiment variantName', () => {
        const mockStore = buildMockStore('my-experiment-variant');

        service.handleAction(
            {
                action: DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING,
                payload: MOCK_UPDATED_CONTENTLET
            },
            {
                uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                inlineEditingService: null,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: vi.fn()
            }
        );

        expect(dotWorkflowActionsFireService.saveContentlet).toHaveBeenCalledWith(
            expect.objectContaining({ variantName: 'my-experiment-variant' })
        );
    });

    it('should set status to LOADING before saving and call pageReload on success', () => {
        const mockStore = buildMockStore();

        service.handleAction(
            {
                action: DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING,
                payload: MOCK_UPDATED_CONTENTLET
            },
            {
                uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                inlineEditingService: null,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: vi.fn()
            }
        );

        expect(mockStore.setUveStatus).toHaveBeenCalledWith(UVE_STATUS.LOADING);
        expect(mockStore.pageReload).toHaveBeenCalled();
    });

    it('should set editor state to IDLE and skip save when payload is null', () => {
        const mockStore = buildMockStore();

        service.handleAction(
            { action: DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING, payload: null },
            {
                uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                inlineEditingService: null,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: vi.fn()
            }
        );

        expect(mockStore.setEditorState).toHaveBeenCalledWith(EDITOR_STATE.IDLE);
        expect(dotWorkflowActionsFireService.saveContentlet).not.toHaveBeenCalled();
    });

    it('should show an error toast when saveContentlet fails', () => {
        vi.spyOn(dotWorkflowActionsFireService, 'saveContentlet').mockReturnValue(
            throwError(() => new Error('save failed'))
        );
        const messageService = spectator.inject(MessageService);
        const mockStore = buildMockStore();

        service.handleAction(
            {
                action: DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING,
                payload: MOCK_UPDATED_CONTENTLET
            },
            {
                uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                inlineEditingService: null,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: vi.fn()
            }
        );

        expect(messageService.add).toHaveBeenCalledWith(
            expect.objectContaining({ severity: 'error' })
        );
    });
});

describe('DotUveActionsHandlerService – SECTION_OFFSET', () => {
    let spectator: SpectatorService<DotUveActionsHandlerService>;
    let service: DotUveActionsHandlerService;

    const createService = createServiceFactory({
        service: DotUveActionsHandlerService,
        providers: [
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotMessageService),
            mockProvider(MessageService),
            mockProvider(DotCopyContentModalService),
            { provide: UVEStore, useValue: buildMockStore() }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
    });

    /** Answers a scroll-to-section request the way the SDK does, with the section's offset. */
    const handleSectionOffset = (offsetTop: number, contentWindow: Window | null) =>
        service.handleAction(
            {
                action: DotCMSUVEAction.SECTION_OFFSET,
                payload: { sectionIndex: 2, offsetTop }
            },
            {
                uveStore: buildMockStore() as unknown as InstanceType<typeof UVEStore>,
                inlineEditingService: null,
                contentWindow,
                host: 'http://localhost',
                onCopyContent: vi.fn()
            }
        );

    it('should scroll the page in the iframe smoothly to the section the SDK reports', () => {
        const contentWindow = { scrollTo: vi.fn() } as unknown as Window;

        handleSectionOffset(450, contentWindow);

        expect(contentWindow.scrollTo).toHaveBeenCalledWith({
            top: 450,
            left: 0,
            behavior: 'smooth'
        });
    });

    it('should scroll to the top of the page when the SDK reports a negative offset', () => {
        const contentWindow = { scrollTo: vi.fn() } as unknown as Window;

        handleSectionOffset(-100, contentWindow);

        expect(contentWindow.scrollTo).toHaveBeenCalledWith({
            top: 0,
            left: 0,
            behavior: 'smooth'
        });
    });

    it('should do nothing when the iframe window is unavailable', () => {
        expect(() => handleSectionOffset(450, null)).not.toThrow();
    });

    describe('NAVIGATION_UPDATE', () => {
        it('should call pageLoad when navigating to a different page', () => {
            const pageLoad = vi.fn();
            const mockStore = {
                ...buildMockStore(),
                pageParams: vi.fn().mockReturnValue({ url: '/home' }),
                pageLoad
            };

            service.handleAction(
                {
                    action: DotCMSUVEAction.NAVIGATION_UPDATE,
                    payload: { url: '/about' }
                },
                {
                    uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                    inlineEditingService: null,
                    contentWindow: null,
                    host: 'http://localhost',
                    onCopyContent: vi.fn()
                }
            );

            expect(pageLoad).toHaveBeenCalledWith({
                url: '/about',
                'com.dotmarketing.persona.id': 'modes.persona.no.persona'
            });
        });

        it('should call pageLoad when navigating to a different page with hash', () => {
            const pageLoad = vi.fn();
            const mockStore = {
                ...buildMockStore(),
                pageParams: vi.fn().mockReturnValue({ url: '/home' }),
                pageLoad
            };

            service.handleAction(
                {
                    action: DotCMSUVEAction.NAVIGATION_UPDATE,
                    payload: { url: '/about#section' }
                },
                {
                    uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                    inlineEditingService: null,
                    contentWindow: null,
                    host: 'http://localhost',
                    onCopyContent: vi.fn()
                }
            );

            expect(pageLoad).toHaveBeenCalledWith(
                expect.objectContaining({ url: '/about#section' })
            );
        });

        describe('same-page navigation', () => {
            it('should not call pageLoad for hash-only navigation on same page', () => {
                const pageLoad = vi.fn();
                const setEditorState = vi.fn();
                const mockStore = {
                    ...buildMockStore(),
                    pageParams: vi.fn().mockReturnValue({ url: '/home' }),
                    pageLoad,
                    setEditorState
                };

                service.handleAction(
                    {
                        action: DotCMSUVEAction.NAVIGATION_UPDATE,
                        payload: { url: '#sectionA' }
                    },
                    {
                        uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                        inlineEditingService: null,
                        contentWindow: null,
                        host: 'http://localhost',
                        onCopyContent: vi.fn()
                    }
                );

                expect(pageLoad).not.toHaveBeenCalled();
                expect(setEditorState).not.toHaveBeenCalled();
            });

            it('should not call pageLoad for hash with full path on same page', () => {
                const pageLoad = vi.fn();
                const mockStore = {
                    ...buildMockStore(),
                    pageParams: vi.fn().mockReturnValue({ url: '/home' }),
                    pageLoad
                };

                service.handleAction(
                    {
                        action: DotCMSUVEAction.NAVIGATION_UPDATE,
                        payload: { url: '/home#faq' }
                    },
                    {
                        uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                        inlineEditingService: null,
                        contentWindow: null,
                        host: 'http://localhost',
                        onCopyContent: vi.fn()
                    }
                );

                expect(pageLoad).not.toHaveBeenCalled();
            });

            it('should not call pageLoad for query-only navigation on same page', () => {
                const pageLoad = vi.fn();
                const mockStore = {
                    ...buildMockStore(),
                    pageParams: vi.fn().mockReturnValue({ url: '/home' }),
                    pageLoad
                };

                service.handleAction(
                    {
                        action: DotCMSUVEAction.NAVIGATION_UPDATE,
                        payload: { url: '/home?tab=2' }
                    },
                    {
                        uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                        inlineEditingService: null,
                        contentWindow: null,
                        host: 'http://localhost',
                        onCopyContent: vi.fn()
                    }
                );

                expect(pageLoad).not.toHaveBeenCalled();
            });

            it('should not call pageLoad for multiple query params on same page', () => {
                const pageLoad = vi.fn();
                const mockStore = {
                    ...buildMockStore(),
                    pageParams: vi.fn().mockReturnValue({ url: '/search' }),
                    pageLoad
                };

                service.handleAction(
                    {
                        action: DotCMSUVEAction.NAVIGATION_UPDATE,
                        payload: { url: '/search?query=test&sort=date' }
                    },
                    {
                        uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                        inlineEditingService: null,
                        contentWindow: null,
                        host: 'http://localhost',
                        onCopyContent: vi.fn()
                    }
                );

                expect(pageLoad).not.toHaveBeenCalled();
            });

            it('should not call pageLoad or setEditorState when both hash and query are present on same page', () => {
                const pageLoad = vi.fn();
                const setEditorState = vi.fn();
                const mockStore = {
                    ...buildMockStore(),
                    setEditorState,
                    pageParams: vi.fn().mockReturnValue({ url: '/home' }),
                    pageLoad
                };

                service.handleAction(
                    {
                        action: DotCMSUVEAction.NAVIGATION_UPDATE,
                        payload: { url: '/home?tab=2#section' }
                    },
                    {
                        uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                        inlineEditingService: null,
                        contentWindow: null,
                        host: 'http://localhost',
                        onCopyContent: vi.fn()
                    }
                );

                expect(pageLoad).not.toHaveBeenCalled();
                expect(setEditorState).not.toHaveBeenCalled();
            });

            it('should handle root path hash navigation', () => {
                const pageLoad = vi.fn();
                const mockStore = {
                    ...buildMockStore(),
                    pageParams: vi.fn().mockReturnValue({ url: '/' }),
                    pageLoad
                };

                service.handleAction(
                    {
                        action: DotCMSUVEAction.NAVIGATION_UPDATE,
                        payload: { url: '/#top' }
                    },
                    {
                        uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                        inlineEditingService: null,
                        contentWindow: null,
                        host: 'http://localhost',
                        onCopyContent: vi.fn()
                    }
                );

                expect(pageLoad).not.toHaveBeenCalled();
            });
        });
    });
});

describe('DotUveActionsHandlerService – REGISTER_STYLE_SCHEMAS', () => {
    let spectator: SpectatorService<DotUveActionsHandlerService>;
    let service: DotUveActionsHandlerService;

    const createService = createServiceFactory({
        service: DotUveActionsHandlerService,
        providers: [
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotMessageService),
            mockProvider(MessageService),
            mockProvider(DotCopyContentModalService),
            {
                provide: UVEStore,
                useValue: buildMockStore()
            }
        ]
    });

    beforeEach(() => {
        vi.clearAllMocks();
        spectator = createService();
        service = spectator.service;
    });

    it('should call setStyleSchemas on the store with the received schemas', () => {
        const mockSchemas = [{ variable: 'Banner', schema: { color: { type: 'color' } } }];
        const setStyleSchemas = vi.fn();
        const mockStore = { ...buildMockStore(), setStyleSchemas };

        service.handleAction(
            {
                action: DotCMSUVEAction.REGISTER_STYLE_SCHEMAS,
                payload: { schemas: mockSchemas }
            },
            {
                uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                inlineEditingService: null,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: vi.fn()
            }
        );

        expect(setStyleSchemas).toHaveBeenCalledWith(mockSchemas);
    });
});

describe('DotUveActionsHandlerService – EDIT_CONTENTLET', () => {
    let spectator: SpectatorService<DotUveActionsHandlerService>;
    let service: DotUveActionsHandlerService;
    let dispatcher: SpyObject<Dispatcher>;

    const createService = createServiceFactory({
        service: DotUveActionsHandlerService,
        providers: [
            Dispatcher,
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotMessageService),
            mockProvider(MessageService),
            mockProvider(DotCopyContentModalService),
            { provide: UVEStore, useValue: buildMockStore() }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
        dispatcher = spectator.inject(Dispatcher);
        vi.spyOn(dispatcher, 'dispatch');
    });

    it("should ask the content editor to edit the client's contentlet", () => {
        // The same request every other edit sends: the content editor picks the editor from the
        // content type, so the SDK's editContentlet() no longer forces the legacy dialog.
        const contentlet = createFakeContentlet({ inode: 'sdk-inode', contentType: 'Blog' });

        service.handleAction(
            { action: DotCMSUVEAction.EDIT_CONTENTLET, payload: contentlet },
            {
                uveStore: buildMockStore() as unknown as InstanceType<typeof UVEStore>,
                // Not used by EDIT_CONTENTLET.
                inlineEditingService: {} as InlineEditService,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: vi.fn()
            }
        );

        expect(dispatcher.dispatch).toHaveBeenCalledWith(
            uveContentEditorEvents.uveEditContentRequested({ contentlet }),
            { scope: 'self' }
        );
    });

    it('should not ask the content editor to edit the contentlet when the user cannot edit content', () => {
        // Preview or live mode, or no edit permission on the page. The SDK sends the request
        // without checking either, so the page's request has to open nothing here.
        const uveStore = buildMockStore();
        uveStore.editorCanEditContent.set(false);

        service.handleAction(
            {
                action: DotCMSUVEAction.EDIT_CONTENTLET,
                payload: createFakeContentlet({ inode: 'sdk-inode', contentType: 'Blog' })
            },
            {
                uveStore: uveStore as unknown as InstanceType<typeof UVEStore>,
                // Not used by EDIT_CONTENTLET.
                inlineEditingService: {} as InlineEditService,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: vi.fn()
            }
        );

        expect(dispatcher.dispatch).not.toHaveBeenCalled();
    });
});

describe('DotUveActionsHandlerService – CREATE_CONTENTLET', () => {
    let spectator: SpectatorService<DotUveActionsHandlerService>;
    let service: DotUveActionsHandlerService;
    let dispatcher: SpyObject<Dispatcher>;

    const createService = createServiceFactory({
        service: DotUveActionsHandlerService,
        providers: [
            Dispatcher,
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotMessageService),
            mockProvider(MessageService),
            mockProvider(DotCopyContentModalService),
            { provide: UVEStore, useValue: buildMockStore() }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
        dispatcher = spectator.inject(Dispatcher);
        vi.spyOn(dispatcher, 'dispatch');
    });

    it('should ask the content editor to create a contentlet of the content type', () => {
        // The same request a palette drop sends, without a container position: the contentlet is
        // only created, and the page reloads once it is saved. The content editor picks the
        // editor from the content type, so the SDK's createContentlet() no longer forces the
        // legacy dialog.
        service.handleAction(
            { action: DotCMSUVEAction.CREATE_CONTENTLET, payload: { contentType: 'Event' } },
            {
                uveStore: buildMockStore() as unknown as InstanceType<typeof UVEStore>,
                // Not used by CREATE_CONTENTLET.
                inlineEditingService: {} as InlineEditService,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: vi.fn()
            }
        );

        expect(dispatcher.dispatch).toHaveBeenCalledWith(
            uveContentEditorEvents.uveCreateContentRequested({
                contentType: { variable: 'Event', name: 'Event' }
            }),
            { scope: 'self' }
        );
    });

    it('should not ask the content editor to create a contentlet when the user cannot edit content', () => {
        // Preview or live mode, or no edit permission on the page.
        const uveStore = buildMockStore();
        uveStore.editorCanEditContent.set(false);

        service.handleAction(
            { action: DotCMSUVEAction.CREATE_CONTENTLET, payload: { contentType: 'Event' } },
            {
                uveStore: uveStore as unknown as InstanceType<typeof UVEStore>,
                // Not used by CREATE_CONTENTLET.
                inlineEditingService: {} as InlineEditService,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: vi.fn()
            }
        );

        expect(dispatcher.dispatch).not.toHaveBeenCalled();
    });
});

describe('DotUveActionsHandlerService – REORDER_MENU', () => {
    let spectator: SpectatorService<DotUveActionsHandlerService>;
    let service: DotUveActionsHandlerService;
    let dispatcher: SpyObject<Dispatcher>;

    const createService = createServiceFactory({
        service: DotUveActionsHandlerService,
        providers: [
            Dispatcher,
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotMessageService),
            mockProvider(MessageService),
            mockProvider(DotCopyContentModalService),
            { provide: UVEStore, useValue: buildMockStore() }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        service = spectator.service;
        dispatcher = spectator.inject(Dispatcher);
        vi.spyOn(dispatcher, 'dispatch');
    });

    it("should ask the content editor to open the menu reorder for the client's start level and depth", () => {
        // The content editor owns the legacy dialog, and builds the reorder screen's URL from the
        // page it shows.
        service.handleAction(
            { action: DotCMSUVEAction.REORDER_MENU, payload: { startLevel: 1, depth: 2 } },
            {
                uveStore: buildMockStore() as unknown as InstanceType<typeof UVEStore>,
                // Not used by REORDER_MENU.
                inlineEditingService: {} as InlineEditService,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: vi.fn()
            }
        );

        expect(dispatcher.dispatch).toHaveBeenCalledWith(
            uveContentEditorEvents.uveReorderMenuRequested({ startLevel: 1, depth: 2 }),
            { scope: 'self' }
        );
    });

    it('should not ask the content editor to open the menu reorder when the user cannot edit content', () => {
        // Preview or live mode, or no edit permission on the page.
        const uveStore = buildMockStore();
        uveStore.editorCanEditContent.set(false);

        service.handleAction(
            { action: DotCMSUVEAction.REORDER_MENU, payload: { startLevel: 1, depth: 2 } },
            {
                uveStore: uveStore as unknown as InstanceType<typeof UVEStore>,
                // Not used by REORDER_MENU.
                inlineEditingService: {} as InlineEditService,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: vi.fn()
            }
        );

        expect(dispatcher.dispatch).not.toHaveBeenCalled();
    });
});

describe('DotUveActionsHandlerService – CLIENT_READY', () => {
    let spectator: SpectatorService<DotUveActionsHandlerService>;
    let service: DotUveActionsHandlerService;

    const createService = createServiceFactory({
        service: DotUveActionsHandlerService,
        providers: [
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotMessageService),
            mockProvider(MessageService),
            mockProvider(DotCopyContentModalService),
            {
                provide: UVEStore,
                useValue: buildMockStore()
            }
        ]
    });

    const buildClientReadyStore = (isClientReady: boolean) => ({
        ...buildMockStore(),
        isClientReady: vi.fn().mockReturnValue(isClientReady),
        setCustomClient: vi.fn(),
        setIsClientReady: vi.fn()
    });

    const CLIENT_READY_PAYLOAD = {
        graphql: {
            query: '{ page { url } }',
            variables: { url: '/page-two', depth: '1' }
        }
    };

    const buildDeps = (mockStore: ReturnType<typeof buildClientReadyStore>) => ({
        uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
        inlineEditingService: null,
        contentWindow: null,
        host: 'http://localhost',
        onCopyContent: vi.fn()
    });

    beforeEach(() => {
        vi.clearAllMocks();
        spectator = createService();
        service = spectator.service;
    });

    it('should install the client request and reload when the client is not ready', () => {
        const mockStore = buildClientReadyStore(false);

        service.handleAction(
            { action: DotCMSUVEAction.CLIENT_READY, payload: CLIENT_READY_PAYLOAD },
            buildDeps(mockStore)
        );

        expect(mockStore.setCustomClient).toHaveBeenCalledWith({
            query: CLIENT_READY_PAYLOAD.graphql.query,
            variables: CLIENT_READY_PAYLOAD.graphql.variables
        });
        expect(mockStore.pageReload).toHaveBeenCalled();
        expect(mockStore.setIsClientReady).toHaveBeenCalledWith(true);
    });

    it('should refresh the stored client request without reloading when already ready', () => {
        // Client-side navigation: the new page's CLIENT_READY arrives while the
        // editor is still initialized for the previous page. The config must be
        // refreshed (so the upcoming NAVIGATION_UPDATE pageLoad uses the new
        // page's query/variables) but no reload should fire.
        const mockStore = buildClientReadyStore(true);

        service.handleAction(
            { action: DotCMSUVEAction.CLIENT_READY, payload: CLIENT_READY_PAYLOAD },
            buildDeps(mockStore)
        );

        expect(mockStore.setCustomClient).toHaveBeenCalledWith({
            query: CLIENT_READY_PAYLOAD.graphql.query,
            variables: CLIENT_READY_PAYLOAD.graphql.variables
        });
        expect(mockStore.pageReload).not.toHaveBeenCalled();
        expect(mockStore.setIsClientReady).not.toHaveBeenCalled();
    });

    it('should not install a client request when the payload has no graphql query', () => {
        const mockStore = buildClientReadyStore(true);

        service.handleAction(
            { action: DotCMSUVEAction.CLIENT_READY, payload: {} },
            buildDeps(mockStore)
        );

        expect(mockStore.setCustomClient).not.toHaveBeenCalled();
        expect(mockStore.pageReload).not.toHaveBeenCalled();
    });
});

describe('DotUveActionsHandlerService – COPY_CONTENTLET_INLINE_EDITING (field switching)', () => {
    let spectator: SpectatorService<DotUveActionsHandlerService>;
    let service: DotUveActionsHandlerService;

    const INODE = 'contentlet-123';
    const HOST = 'http://localhost';

    const createService = createServiceFactory({
        service: DotUveActionsHandlerService,
        providers: [
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotMessageService),
            mockProvider(MessageService),
            mockProvider(DotCopyContentModalService),
            {
                provide: UVEStore,
                useValue: buildMockStore()
            }
        ]
    });

    // Store already in INLINE_EDITING with `INODE` as the contentlet under edit.
    const buildInlineEditingStore = (pageType: PageType) => ({
        ...buildMockStore(),
        editorState: signal(EDITOR_STATE.INLINE_EDITING),
        editorContentArea: signal({
            payload: { contentlet: { inode: INODE }, container: { identifier: 'container-1' } }
        }),
        pageType: signal(pageType),
        getCurrentTreeNode: vi.fn().mockReturnValue({})
    });

    const buildDataset = (inode: string) => ({
        dataset: { inode, fieldName: 'body', mode: 'minimal', language: '1' }
    });

    beforeEach(() => {
        vi.clearAllMocks();
        spectator = createService();
        service = spectator.service;
    });

    it('should move focus to the clicked field without re-opening the copy dialog when switching fields on the same contentlet (headless)', () => {
        const mockStore = buildInlineEditingStore(PageType.HEADLESS);
        const copyModal = spectator.inject(DotCopyContentModalService);
        const contentWindow = { postMessage: vi.fn() } as unknown as Window;

        service.handleAction(
            {
                action: DotCMSUVEAction.COPY_CONTENTLET_INLINE_EDITING,
                payload: buildDataset(INODE)
            },
            {
                uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                inlineEditingService: null,
                contentWindow,
                host: HOST,
                onCopyContent: vi.fn()
            }
        );

        expect(copyModal.open).not.toHaveBeenCalled();
        expect(contentWindow.postMessage).toHaveBeenCalledWith(
            {
                name: __DOTCMS_UVE_EVENT__.UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS,
                payload: {
                    oldInode: INODE,
                    inode: INODE,
                    fieldName: 'body',
                    mode: 'minimal',
                    language: '1'
                }
            },
            HOST
        );
    });

    it('should re-init the inline editor on the clicked field when switching fields on the same contentlet (traditional)', () => {
        const mockStore = buildInlineEditingStore(PageType.TRADITIONAL);
        const copyModal = spectator.inject(DotCopyContentModalService);
        const inlineEditingService = {
            setTargetInlineMCEDataset: vi.fn(),
            initEditor: vi.fn()
        } as unknown as InlineEditService;

        service.handleAction(
            {
                action: DotCMSUVEAction.COPY_CONTENTLET_INLINE_EDITING,
                payload: buildDataset(INODE)
            },
            {
                uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                inlineEditingService,
                contentWindow: null,
                host: HOST,
                onCopyContent: vi.fn()
            }
        );

        expect(copyModal.open).not.toHaveBeenCalled();
        expect(inlineEditingService.setTargetInlineMCEDataset).toHaveBeenCalledWith({
            oldInode: INODE,
            inode: INODE,
            fieldName: 'body',
            mode: 'minimal',
            language: '1'
        });
        expect(inlineEditingService.initEditor).toHaveBeenCalled();
    });

    it('should ignore the click when already inline-editing a different contentlet', () => {
        const mockStore = buildInlineEditingStore(PageType.HEADLESS);
        const copyModal = spectator.inject(DotCopyContentModalService);
        const contentWindow = { postMessage: vi.fn() } as unknown as Window;

        service.handleAction(
            {
                action: DotCMSUVEAction.COPY_CONTENTLET_INLINE_EDITING,
                payload: buildDataset('a-different-inode')
            },
            {
                uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                inlineEditingService: null,
                contentWindow,
                host: HOST,
                onCopyContent: vi.fn()
            }
        );

        expect(copyModal.open).not.toHaveBeenCalled();
        expect(contentWindow.postMessage).not.toHaveBeenCalled();
    });
});

describe('DotUveActionsHandlerService – INIT_INLINE_EDITING permission gate (#37376)', () => {
    let spectator: SpectatorService<DotUveActionsHandlerService>;
    let service: DotUveActionsHandlerService;
    let messageService: MessageService;
    let dispatcher: SpyObject<Dispatcher>;
    let inlineEditingService: { initEditor: Mock };

    /** The Block Editor field the user clicked. */
    const BLOCK_EDITOR_FIELD: DotCMSInlineEditingPayload = {
        inode: 'inode-123',
        fieldName: 'body',
        language: 1,
        contentType: 'Blog',
        content: { type: 'doc', content: [] }
    };

    /** Whether the content editor was asked to open the block editor sidebar on that field. */
    const expectBlockEditorRequested = () =>
        expect(dispatcher.dispatch).toHaveBeenCalledWith(
            uveContentEditorEvents.uveEditBlockEditorFieldRequested(BLOCK_EDITOR_FIELD),
            { scope: 'self' }
        );

    const createService = createServiceFactory({
        service: DotUveActionsHandlerService,
        providers: [
            Dispatcher,
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotMessageService, { get: (key: string) => key }),
            mockProvider(MessageService),
            mockProvider(DotCopyContentModalService),
            { provide: UVEStore, useValue: buildMockStore() }
        ]
    });

    /**
     * Build an iframe-like document containing one contentlet wrapper with the
     * given permission, mirroring what the Velocity container renderer emits.
     */
    const buildContentWindow = (canEdit?: string): Window => {
        const doc = document.implementation.createHTMLDocument('iframe');
        const wrapper = doc.createElement('div');
        wrapper.dataset['dotObject'] = 'contentlet';
        wrapper.dataset['dotInode'] = 'inode-123';
        if (canEdit !== undefined) {
            wrapper.dataset['dotCanEdit'] = canEdit;
        }

        const field = doc.createElement('div');
        field.dataset['inode'] = 'inode-123';
        field.dataset['fieldName'] = 'body';
        wrapper.appendChild(field);
        doc.body.appendChild(wrapper);

        return { document: doc } as unknown as Window;
    };

    /**
     * A cross-origin iframe's window, as the browser exposes it to the parent:
     * reading `document` throws a SecurityError instead of returning null.
     */
    const buildCrossOriginContentWindow = (): Window =>
        Object.defineProperty({ postMessage: vi.fn() }, 'document', {
            get: () => {
                throw new DOMException(
                    "Failed to read a named property 'document' from 'Window'",
                    'SecurityError'
                );
            }
        }) as unknown as Window;

    const fireBlockEditor = (
        canEdit?: string,
        contentWindow: Window = buildContentWindow(canEdit)
    ) => {
        service.handleAction(
            {
                action: DotCMSUVEAction.INIT_INLINE_EDITING,
                payload: { type: 'BLOCK_EDITOR', data: BLOCK_EDITOR_FIELD }
            } as never,
            {
                uveStore: buildMockStore() as never,
                inlineEditingService: inlineEditingService as never,
                contentWindow,
                host: 'http://localhost',
                onCopyContent: () => of({}) as never
            }
        );
    };

    beforeEach(() => {
        vi.clearAllMocks();
        spectator = createService();
        service = spectator.service;
        messageService = spectator.inject(MessageService);
        dispatcher = spectator.inject(Dispatcher);
        vi.spyOn(dispatcher, 'dispatch');
        inlineEditingService = { initEditor: vi.fn() };
    });

    it('should not ask to open the block editor sidebar for a contentlet the user cannot edit', () => {
        fireBlockEditor('false');

        expect(dispatcher.dispatch).not.toHaveBeenCalled();
    });

    it('should tell the user why with a toast instead of failing silently', () => {
        // An inline field has no control to grey out and no hover target for a
        // tooltip. A click that does nothing reads as a broken editor and
        // becomes a support ticket, so the refusal must be visible.
        fireBlockEditor('false');

        expect(messageService.add).toHaveBeenCalledWith(
            expect.objectContaining({ detail: 'uve.contentlet.no.edit.permission' })
        );
    });

    it('should ask the content editor to open the block editor sidebar on the field when the user can edit', () => {
        fireBlockEditor('true');

        expectBlockEditorRequested();
        expect(messageService.add).not.toHaveBeenCalled();
    });

    it('should ask the content editor to open the block editor sidebar when the permission attribute is absent', () => {
        // Headless pages never emit the attribute; fail open.
        fireBlockEditor(undefined);

        expectBlockEditorRequested();
    });

    it('should ask the content editor to open the block editor sidebar when the iframe is cross-origin (#37820)', () => {
        // Headless apps are served from another origin, so the iframe document
        // cannot be read. That must fail open like a missing attribute, not
        // throw and leave the click doing nothing.
        expect(() => fireBlockEditor(undefined, buildCrossOriginContentWindow())).not.toThrow();

        expectBlockEditorRequested();
        expect(messageService.add).not.toHaveBeenCalled();
    });
});
