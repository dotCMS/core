import { byTestId, mockProvider, Spectator } from '@ngneat/spectator';
import { createComponentFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of, throwError } from 'rxjs';

import { Sidebar } from 'primeng/drawer';

import { BlockEditorModule, DotBlockEditorComponent } from '@dotcms/block-editor';
import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotMessageService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotCMSContentType, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import {
    dotcmsContentTypeBasicMock,
    MockDotMessageService,
    mockResponseView
} from '@dotcms/utils-testing';

import { DotBlockEditorSidebarComponent } from './dot-block-editor-sidebar.component';

const BLOCK_EDITOR_FIELD: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
    contentTypeId: '799f176a-d32e-4844-a07c-1b5fcd107578',
    dataType: 'LONG_TEXT',
    fieldType: 'Story-Block',
    fieldTypeLabel: 'Block Editor',
    fixed: false,
    iDate: 1649791703000,
    id: '71fe962eb681c5ffd6cd1623e5fc575a',
    indexed: false,
    listed: false,
    hint: 'A helper text',
    modDate: 1699364930000,
    name: 'Blog Content',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 13,
    unique: false,
    variable: 'testName',
    fieldVariables: [
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
            fieldId: '71fe962eb681c5ffd6cd1623e5fc575a',
            id: 'b19e1d5d-47ad-40d7-b2bf-ccd0a5a86590',
            key: 'allowedBlocks',
            value: 'heading1'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
            fieldId: '71fe962eb681c5ffd6cd1623e5fc575a',
            id: 'b19e1d5d-47ad-40d7-b2bf-ccd0a5a86590',
            key: 'allowedContentTypes',
            value: 'Activity'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
            fieldId: '71fe962eb681c5ffd6cd1623e5fc575a',
            id: 'b19e1d5d-47ad-40d7-b2bf-ccd0a5a86590',
            key: 'styles',
            value: 'height:50%'
        }
    ]
};

const messageServiceMock = new MockDotMessageService({
    'editpage.inline.error': 'An error occurred',
    error: 'Error'
});

const EVENT_DATA = {
    fieldName: 'testName',
    contentType: 'Blog',
    language: 2,
    inode: 'testInode',
    content: {
        conent: [],
        type: 'doc'
    }
};

const contentTypeMock: DotCMSContentType = {
    ...dotcmsContentTypeBasicMock,
    fields: [BLOCK_EDITOR_FIELD]
};

describe('DotBlockEditorSidebarComponent', () => {
    let spectator: Spectator<DotBlockEditorSidebarComponent>;
    let dotContentTypeService: DotContentTypeService;
    let dotAlertConfirmService: DotAlertConfirmService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;

    const createComponent = createComponentFactory({
        component: DotBlockEditorSidebarComponent,
        imports: [BlockEditorModule],
        declarations: [MockComponent(DotBlockEditorComponent)],
        providers: [
            DotAlertConfirmService,
            { provide: DotMessageService, useValue: messageServiceMock },
            {
                provide: DotWorkflowActionsFireService,
                useValue: {
                    saveContentlet: jest.fn()
                }
            },
            mockProvider(DotContentTypeService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        dotContentTypeService = spectator.inject(DotContentTypeService, true);
        dotAlertConfirmService = spectator.inject(DotAlertConfirmService, true);
        dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService, true);

        jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(of(contentTypeMock));

        spectator.component.open(EVENT_DATA);
        spectator.detectChanges();
    });

    it('should set sidebar with correct inputs', () => {
        const sidebar = spectator.query(Sidebar);
        expect(sidebar.position).toBe('right');
        expect(sidebar.blockScroll).toBe(true);
        expect(sidebar.dismissible).toBe(false);
        expect(sidebar.showCloseIcon).toBe(false);
        expect(sidebar.closeOnEscape).toBe(false);
        expect(sidebar.visible).toBe(true);
    });

    it('should set inputs to the block editor', () => {
        const blockEditor = spectator.query(DotBlockEditorComponent);

        expect(blockEditor.field).toEqual(BLOCK_EDITOR_FIELD);
        expect(blockEditor.languageId).toBe(EVENT_DATA.language);
        expect(blockEditor.value).toEqual(EVENT_DATA.content);
        expect(dotContentTypeService.getContentType).toHaveBeenCalledWith('Blog');
    });

    it('should save changes in the editor', () => {
        const spyWorkflowService = jest.spyOn(dotWorkflowActionsFireService, 'saveContentlet');
        const blockEditor = spectator.query(DotBlockEditorComponent);

        const newValue = { data: 'test value 1' };
        blockEditor.valueChange.emit(newValue);

        spectator.detectChanges();

        const saveBtn = spectator.query(byTestId('save-btn')) as HTMLButtonElement;

        saveBtn.click();
        spectator.detectChanges();

        expect(dotContentTypeService.getContentType).toHaveBeenCalledWith('Blog');
        expect(spyWorkflowService).toHaveBeenCalledWith({ testName: JSON.stringify(newValue) });
    });

    it('should close the sidebar', () => {
        const cancelBtn = spectator.query(byTestId('cancel-btn')) as HTMLButtonElement;

        cancelBtn.click();
        spectator.detectChanges();

        const sidebar = spectator.query(Sidebar);

        expect(sidebar.visible).toBe(false);
    });

    it('should display a toast on saving error', () => {
        const error404 = mockResponseView(404, '', null, {
            error: { message: 'An error occurred' }
        });
        const dotAletConfirmServiceSpy = jest.spyOn(dotAlertConfirmService, 'alert');
        const spyWorkflowService = jest
            .spyOn(dotWorkflowActionsFireService, 'saveContentlet')
            .mockReturnValue(throwError(error404));

        const blockEditor = spectator.query(DotBlockEditorComponent);
        const newValue = { data: 'test value 1' };
        blockEditor.valueChange.emit(newValue);

        spectator.detectChanges();

        const saveBtn = spectator.query(byTestId('save-btn')) as HTMLButtonElement;
        saveBtn.click();
        spectator.detectChanges();

        expect(spyWorkflowService).toHaveBeenCalled();
        expect(dotAletConfirmServiceSpy).toHaveBeenCalled();
    });

    it('should call event.stopPropagation on escape keydown', () => {
        const event = new KeyboardEvent('keydown', { key: 'Escape' });
        jest.spyOn(event, 'stopPropagation');

        const container = spectator.query('[data-testId="dot-container"]');
        container.dispatchEvent(event);

        expect(event.stopPropagation).toHaveBeenCalled();
    });

    afterEach(() => jest.clearAllMocks());
});
