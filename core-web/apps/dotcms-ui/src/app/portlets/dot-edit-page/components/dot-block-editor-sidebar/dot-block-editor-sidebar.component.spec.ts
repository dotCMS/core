import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotBlockEditorSidebarComponent } from '@portlets/dot-edit-page/components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { Component, DebugElement, Injectable, Input } from '@angular/core';
import { DEFAULT_LANG_ID } from '@dotcms/block-editor';
import { By } from '@angular/platform-browser';
import { Sidebar, SidebarModule } from 'primeng/sidebar';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { ButtonModule } from 'primeng/button';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { mockResponseView } from '@tests/response-view.mock';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotContentTypeService } from '@services/dot-content-type';

@Component({
    selector: 'dotcms-block-editor' /* eslint-disable-line */,
    template: ''
})
export class MockDotBlockEditorComponent {
    @Input() lang = DEFAULT_LANG_ID;
    @Input() allowedContentTypes = '';
    @Input() customStyles = '';
    @Input() allowedBlocks = '';
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
                {
                    variable: 'testName',
                    fieldVariables: [
                        { key: 'allowedBlocks', value: 'heading1' },
                        { key: 'allowedContentTypes', value: 'Activity' },
                        { key: 'styles', value: 'height:50%' }
                    ]
                },
                {
                    variable: 'otherName',
                    fieldVariables: [{ key: 'invalidKey', value: 'test' }]
                }
            ]
        });
    }
}

const messageServiceMock = new MockDotMessageService({
    'editpage.inline.error': 'An error occurred'
});

const clickEvent = {
    dataset: {
        fieldName: 'testName',
        contentType: 'Blog',
        language: 2,
        inode: 'testInode',
        content: '{"field":"field value"}'
    }
};

describe('DotBlockEditorSidebarComponent', () => {
    let component: DotBlockEditorSidebarComponent;
    let fixture: ComponentFixture<DotBlockEditorSidebarComponent>;
    let dotEventsService: DotEventsService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let dotGlobalMessageService: DotGlobalMessageService;
    let dotContentTypeService: DotContentTypeService;
    let de: DebugElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotBlockEditorSidebarComponent, MockDotBlockEditorComponent],
            imports: [
                HttpClientTestingModule,
                BrowserAnimationsModule,
                SidebarModule,
                DotMessagePipeModule,
                ButtonModule
            ],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotContentTypeService, useClass: MockDotContentTypeService },
                DotWorkflowActionsFireService,
                DotEventsService,
                DotGlobalMessageService
            ]
        }).compileComponents();
        dotEventsService = TestBed.inject(DotEventsService);
        dotWorkflowActionsFireService = TestBed.inject(DotWorkflowActionsFireService);
        dotGlobalMessageService = TestBed.inject(DotGlobalMessageService);
        dotContentTypeService = TestBed.inject(DotContentTypeService);
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

    it('should set inputs to the block editor', () => {
        spyOn(dotContentTypeService, 'getContentType').and.callThrough();
        dotEventsService.notify('edit-block-editor', clickEvent);
        fixture.detectChanges();
        const blockEditor: MockDotBlockEditorComponent = de.query(
            By.css('dotcms-block-editor')
        ).componentInstance;

        expect(dotContentTypeService.getContentType).toHaveBeenCalledWith('Blog');
        expect(blockEditor.lang).toEqual(clickEvent.dataset.language);
        expect(blockEditor.allowedBlocks).toEqual('heading1');
        expect(blockEditor.allowedContentTypes).toEqual('Activity');
        expect(blockEditor.value).toEqual(JSON.parse(clickEvent.dataset.content));
        expect(blockEditor.customStyles).toEqual('height:50%');
    });

    it('should save changes in the editor', () => {
        dotEventsService.notify('edit-block-editor', clickEvent);
        spyOn(dotWorkflowActionsFireService, 'saveContentlet').and.returnValue(of({}));
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
            errors: [{ message: 'An error occurred' }]
        });

        dotEventsService.notify('edit-block-editor', clickEvent);
        spyOn(dotGlobalMessageService, 'error').and.callThrough();
        spyOn(dotWorkflowActionsFireService, 'saveContentlet').and.returnValue(
            throwError(error404)
        );
        fixture.detectChanges();

        const updateBtn = de.query(By.css('[data-testId="updateBtn"]'));
        updateBtn.triggerEventHandler('click');

        expect(dotGlobalMessageService.error).toHaveBeenCalledWith('An error occurred');
    });

    it('should close the sidebar', () => {
        dotEventsService.notify('edit-block-editor', clickEvent);
        fixture.detectChanges();

        const cancelBtn = de.query(By.css('[data-testId="cancelBtn"]'));
        const sidebar: Sidebar = de.query(By.css('[data-testId="sidebar"]')).componentInstance;

        cancelBtn.triggerEventHandler('click');
        fixture.detectChanges();

        expect(sidebar.visible).toEqual(false);
        expect(component.data).toBeNull();
    });
});
