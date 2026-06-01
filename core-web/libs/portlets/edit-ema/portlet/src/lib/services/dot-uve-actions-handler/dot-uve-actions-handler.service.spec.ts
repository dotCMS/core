import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { signal } from '@angular/core';

import { MessageService } from 'primeng/api';

import { DotMessageService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';
import { DotCMSUVEAction } from '@dotcms/types';
import { DotCopyContentModalService } from '@dotcms/ui';

import { DotUveActionsHandlerService } from './dot-uve-actions-handler.service';

import { UpdatedContentlet } from '../../edit-ema-editor/components/ema-page-dropzone/types';
import { EDITOR_STATE, UVE_STATUS } from '../../shared/enums';
import { UVEStore } from '../../store/dot-uve.store';

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
        setEditorState: jest.fn(),
        setUveStatus: jest.fn(),
        pageReload: jest.fn()
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
                saveContentlet: jest.fn().mockReturnValue(of({}))
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
        jest.clearAllMocks();
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
                dialog: null,
                inlineEditingService: null,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: jest.fn()
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
                dialog: null,
                inlineEditingService: null,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: jest.fn()
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
                dialog: null,
                inlineEditingService: null,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: jest.fn()
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
                dialog: null,
                inlineEditingService: null,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: jest.fn()
            }
        );

        expect(mockStore.setEditorState).toHaveBeenCalledWith(EDITOR_STATE.IDLE);
        expect(dotWorkflowActionsFireService.saveContentlet).not.toHaveBeenCalled();
    });

    it('should show an error toast when saveContentlet fails', () => {
        jest.spyOn(dotWorkflowActionsFireService, 'saveContentlet').mockReturnValue(
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
                dialog: null,
                inlineEditingService: null,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: jest.fn()
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

    it('should call onSectionOffset with the payload when the action is SECTION_OFFSET', () => {
        const onSectionOffset = jest.fn();
        const payload = { sectionIndex: 2, offsetTop: 450 };

        service.handleAction(
            { action: DotCMSUVEAction.SECTION_OFFSET, payload },
            {
                uveStore: buildMockStore() as unknown as InstanceType<typeof UVEStore>,
                dialog: null,
                inlineEditingService: null,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: jest.fn(),
                onSectionOffset
            }
        );

        expect(onSectionOffset).toHaveBeenCalledWith(payload);
    });

    it('should not throw when onSectionOffset is not provided', () => {
        expect(() => {
            service.handleAction(
                {
                    action: DotCMSUVEAction.SECTION_OFFSET,
                    payload: { sectionIndex: 1, offsetTop: 100 }
                },
                {
                    uveStore: buildMockStore() as unknown as InstanceType<typeof UVEStore>,
                    dialog: null,
                    inlineEditingService: null,
                    contentWindow: null,
                    host: 'http://localhost',
                    onCopyContent: jest.fn()
                }
            );
        }).not.toThrow();
    });

    it('should call dialog.createContentletFromPalette when action is CREATE_CONTENTLET', () => {
        const createContentletFromPalette = jest.fn();
        const mockStore = {
            ...buildMockStore(),
            pageLanguageId: signal(1)
        };

        service.handleAction(
            { action: DotCMSUVEAction.CREATE_CONTENTLET, payload: { contentType: 'Event' } },
            {
                uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                dialog: { createContentletFromPalette } as never,
                inlineEditingService: null,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: jest.fn()
            }
        );

        expect(createContentletFromPalette).toHaveBeenCalledWith({
            variable: 'Event',
            name: 'Event',
            language_id: 1
        });
    });

    describe('NAVIGATION_UPDATE', () => {
        it('should call pageLoad when navigating to a different page', () => {
            const pageLoad = jest.fn();
            const mockStore = {
                ...buildMockStore(),
                pageParams: jest.fn().mockReturnValue({ url: '/home' }),
                pageLoad
            };

            service.handleAction(
                {
                    action: DotCMSUVEAction.NAVIGATION_UPDATE,
                    payload: { url: '/about' }
                },
                {
                    uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                    dialog: null,
                    inlineEditingService: null,
                    contentWindow: null,
                    host: 'http://localhost',
                    onCopyContent: jest.fn()
                }
            );

            expect(pageLoad).toHaveBeenCalledWith({
                url: '/about',
                'com.dotmarketing.persona.id': 'modes.persona.no.persona'
            });
        });

        it('should call pageLoad when navigating to a different page with hash', () => {
            const pageLoad = jest.fn();
            const mockStore = {
                ...buildMockStore(),
                pageParams: jest.fn().mockReturnValue({ url: '/home' }),
                pageLoad
            };

            service.handleAction(
                {
                    action: DotCMSUVEAction.NAVIGATION_UPDATE,
                    payload: { url: '/about#section' }
                },
                {
                    uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                    dialog: null,
                    inlineEditingService: null,
                    contentWindow: null,
                    host: 'http://localhost',
                    onCopyContent: jest.fn()
                }
            );

            expect(pageLoad).toHaveBeenCalledWith(
                expect.objectContaining({ url: '/about#section' })
            );
        });

        describe('same-page navigation', () => {
            it('should not call pageLoad for hash-only navigation on same page', () => {
                const pageLoad = jest.fn();
                const setEditorState = jest.fn();
                const mockStore = {
                    ...buildMockStore(),
                    pageParams: jest.fn().mockReturnValue({ url: '/home' }),
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
                        dialog: null,
                        inlineEditingService: null,
                        contentWindow: null,
                        host: 'http://localhost',
                        onCopyContent: jest.fn()
                    }
                );

                expect(pageLoad).not.toHaveBeenCalled();
                expect(setEditorState).not.toHaveBeenCalled();
            });

            it('should not call pageLoad for hash with full path on same page', () => {
                const pageLoad = jest.fn();
                const mockStore = {
                    ...buildMockStore(),
                    pageParams: jest.fn().mockReturnValue({ url: '/home' }),
                    pageLoad
                };

                service.handleAction(
                    {
                        action: DotCMSUVEAction.NAVIGATION_UPDATE,
                        payload: { url: '/home#faq' }
                    },
                    {
                        uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                        dialog: null,
                        inlineEditingService: null,
                        contentWindow: null,
                        host: 'http://localhost',
                        onCopyContent: jest.fn()
                    }
                );

                expect(pageLoad).not.toHaveBeenCalled();
            });

            it('should not call pageLoad for query-only navigation on same page', () => {
                const pageLoad = jest.fn();
                const mockStore = {
                    ...buildMockStore(),
                    pageParams: jest.fn().mockReturnValue({ url: '/home' }),
                    pageLoad
                };

                service.handleAction(
                    {
                        action: DotCMSUVEAction.NAVIGATION_UPDATE,
                        payload: { url: '/home?tab=2' }
                    },
                    {
                        uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                        dialog: null,
                        inlineEditingService: null,
                        contentWindow: null,
                        host: 'http://localhost',
                        onCopyContent: jest.fn()
                    }
                );

                expect(pageLoad).not.toHaveBeenCalled();
            });

            it('should not call pageLoad for multiple query params on same page', () => {
                const pageLoad = jest.fn();
                const mockStore = {
                    ...buildMockStore(),
                    pageParams: jest.fn().mockReturnValue({ url: '/search' }),
                    pageLoad
                };

                service.handleAction(
                    {
                        action: DotCMSUVEAction.NAVIGATION_UPDATE,
                        payload: { url: '/search?query=test&sort=date' }
                    },
                    {
                        uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                        dialog: null,
                        inlineEditingService: null,
                        contentWindow: null,
                        host: 'http://localhost',
                        onCopyContent: jest.fn()
                    }
                );

                expect(pageLoad).not.toHaveBeenCalled();
            });

            it('should not call pageLoad or setEditorState when both hash and query are present on same page', () => {
                const pageLoad = jest.fn();
                const setEditorState = jest.fn();
                const mockStore = {
                    ...buildMockStore(),
                    setEditorState,
                    pageParams: jest.fn().mockReturnValue({ url: '/home' }),
                    pageLoad
                };

                service.handleAction(
                    {
                        action: DotCMSUVEAction.NAVIGATION_UPDATE,
                        payload: { url: '/home?tab=2#section' }
                    },
                    {
                        uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                        dialog: null,
                        inlineEditingService: null,
                        contentWindow: null,
                        host: 'http://localhost',
                        onCopyContent: jest.fn()
                    }
                );

                expect(pageLoad).not.toHaveBeenCalled();
                expect(setEditorState).not.toHaveBeenCalled();
            });

            it('should handle root path hash navigation', () => {
                const pageLoad = jest.fn();
                const mockStore = {
                    ...buildMockStore(),
                    pageParams: jest.fn().mockReturnValue({ url: '/' }),
                    pageLoad
                };

                service.handleAction(
                    {
                        action: DotCMSUVEAction.NAVIGATION_UPDATE,
                        payload: { url: '/#top' }
                    },
                    {
                        uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                        dialog: null,
                        inlineEditingService: null,
                        contentWindow: null,
                        host: 'http://localhost',
                        onCopyContent: jest.fn()
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
        jest.clearAllMocks();
        spectator = createService();
        service = spectator.service;
    });

    it('should call setStyleSchemas on the store with the received schemas', () => {
        const mockSchemas = [{ variable: 'Banner', schema: { color: { type: 'color' } } }];
        const setStyleSchemas = jest.fn();
        const mockStore = { ...buildMockStore(), setStyleSchemas };

        service.handleAction(
            {
                action: DotCMSUVEAction.REGISTER_STYLE_SCHEMAS,
                payload: { schemas: mockSchemas }
            },
            {
                uveStore: mockStore as unknown as InstanceType<typeof UVEStore>,
                dialog: null,
                inlineEditingService: null,
                contentWindow: null,
                host: 'http://localhost',
                onCopyContent: jest.fn()
            }
        );

        expect(setStyleSchemas).toHaveBeenCalledWith(mockSchemas);
    });
});
