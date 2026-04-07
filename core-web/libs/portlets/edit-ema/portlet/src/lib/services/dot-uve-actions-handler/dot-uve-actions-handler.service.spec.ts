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
