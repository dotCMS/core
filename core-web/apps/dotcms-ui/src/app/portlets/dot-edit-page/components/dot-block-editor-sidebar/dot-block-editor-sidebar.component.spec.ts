import { of, throwError } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, Injectable, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Sidebar, SidebarModule } from 'primeng/sidebar';

import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotEventsService,
    DotMessageService,
    DotPropertiesService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { CoreWebServiceMock, MockDotMessageService, mockResponseView } from '@dotcms/utils-testing';

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

@Component({
    selector: 'dot-block-editor',
    template: '',
    standalone: false
})
export class MockDotBlockEditorComponent {
    @Input() languageId = 1;
    @Input() field: DotCMSContentTypeField;
    @Input() value: { [key: string]: string } | string = '';

    editor = {
        getJSON: () => {
            return { data: 'test value ' };
        }
    };
}

@Injectable()
class MockDotContentTypeService {
    getContentType() {
        return of({
            fields: [
                BLOCK_EDITOR_FIELD,
                {
                    variable: 'otherName',
                    fieldVariables: [{ key: 'invalidKey', value: 'test' }]
                }
            ]
        });
    }
}

const messageServiceMock = new MockDotMessageService({
    'editpage.inline.error': 'An error occurred',
    error: 'Error'
});

const clickEvent = {
    dataset: {
        fieldName: 'testName',
        contentType: 'Blog',
        language: 2,
        inode: 'testInode',
        blockEditorContent: '{"field":"field value"}'
    }
};

describe('DotBlockEditorSidebarComponent', () => {
    let component: DotBlockEditorSidebarComponent;
    let fixture: ComponentFixture<DotBlockEditorSidebarComponent>;
    let dotEventsService: DotEventsService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let dotAlertConfirmService: DotAlertConfirmService;
    let dotContentTypeService: DotContentTypeService;
    let de: DebugElement;
    let dotPropertiesService: DotPropertiesService;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotBlockEditorSidebarComponent, MockDotBlockEditorComponent],
            imports: [
                HttpClientTestingModule,
                BrowserAnimationsModule,
                SidebarModule,
                DotMessagePipe,
                ButtonModule
            ],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotContentTypeService, useClass: MockDotContentTypeService },
                {
                    provide: DotPropertiesService,
                    useValue: {
                        getKey: () => of('true')
                    }
                },
                DotWorkflowActionsFireService,
                DotEventsService,
                DotAlertConfirmService,
                ConfirmationService
            ]
        }).compileComponents();
        dotEventsService = TestBed.inject(DotEventsService);
        dotWorkflowActionsFireService = TestBed.inject(DotWorkflowActionsFireService);
        dotAlertConfirmService = TestBed.inject(DotAlertConfirmService);
        dotContentTypeService = TestBed.inject(DotContentTypeService);
        dotPropertiesService = TestBed.inject(DotPropertiesService);
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotBlockEditorSidebarComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should set sidebar with correct inputs', () => {
        const sidebar: Sidebar = de.query(By.css('[data-testId="sidebar"]')).componentInstance;
        expect(sidebar.blockScroll).toEqual(true);
        expect(sidebar.position).toEqual('right');
        expect(sidebar.dismissible).toEqual(false);
        expect(sidebar.showCloseIcon).toEqual(false);
        expect(sidebar.closeOnEscape).toEqual(false);
        expect(component).toBeTruthy();
    });

    it('should set inputs to the block editor', async () => {
        jest.spyOn(dotContentTypeService, 'getContentType');
        jest.spyOn(dotPropertiesService, 'getKey').mockReturnValue(of('true'));
        dotEventsService.notify('edit-block-editor', clickEvent);

        await fixture.whenRenderingDone();
        fixture.detectChanges();

        const blockEditor: MockDotBlockEditorComponent = de.query(
            By.css('dot-block-editor')
        ).componentInstance;

        expect(dotContentTypeService.getContentType).toHaveBeenCalledWith('Blog');
        expect(dotContentTypeService.getContentType).toHaveBeenCalledTimes(1);
        expect(blockEditor.field).toEqual(BLOCK_EDITOR_FIELD);
        expect(blockEditor.languageId).toEqual(clickEvent.dataset.language);
        expect(blockEditor.value).toEqual(JSON.parse(clickEvent.dataset.blockEditorContent));
    });

    it('should save changes in the editor', () => {
        dotEventsService.notify('edit-block-editor', clickEvent);
        jest.spyOn(dotWorkflowActionsFireService, 'saveContentlet').mockReturnValue(of({}));
        fixture.detectChanges();

        const updateBtn = de.query(By.css('[data-testId="updateBtn"]'));
        updateBtn.triggerEventHandler('click');
        expect(dotWorkflowActionsFireService.saveContentlet).toHaveBeenCalledWith({
            testName: JSON.stringify({ data: 'test value ' }),
            inode: clickEvent.dataset.inode,
            indexPolicy: 'WAIT_FOR'
        });
    });

    it('should display a toast on saving error', () => {
        const error404 = mockResponseView(404, '', null, {
            error: { message: 'An error occurred' }
        });

        dotEventsService.notify('edit-block-editor', clickEvent);
        jest.spyOn(dotAlertConfirmService, 'alert');
        jest.spyOn(dotWorkflowActionsFireService, 'saveContentlet').mockReturnValue(
            throwError(error404)
        );
        fixture.detectChanges();

        const updateBtn = de.query(By.css('[data-testId="updateBtn"]'));
        updateBtn.triggerEventHandler('click');

        expect(dotAlertConfirmService.alert).toHaveBeenCalledTimes(1);
    });

    it('should close the sidebar', () => {
        dotEventsService.notify('edit-block-editor', clickEvent);
        fixture.detectChanges();

        const cancelBtn = de.query(By.css('[data-testId="cancelBtn"]'));
        const sidebar: Sidebar = de.query(By.css('[data-testId="sidebar"]')).componentInstance;

        cancelBtn.triggerEventHandler('click');
        fixture.detectChanges();

        expect(sidebar.visible).toEqual(false);
        expect(component.blockEditorInput).toBeNull();
    });
});
