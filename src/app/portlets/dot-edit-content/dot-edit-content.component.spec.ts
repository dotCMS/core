import { async, ComponentFixture } from '@angular/core/testing';
import { DotEditContentComponent } from './dot-edit-content.component';
import { DialogModule } from 'primeng/primeng';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotEditPageToolbarModule } from './components/dot-edit-page-toolbar/dot-edit-page-toolbar.module';
import { DotConfirmationService } from '../../api/services/dot-confirmation/index';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { DotEditContentHtmlService } from './services/dot-edit-content-html.service';
import { DotContainerContentletService } from './services/dot-container-contentlet.service';
import { DotMessageService } from '../../api/services/dot-messages-service';
import { DOTTestBed } from '../../test/dot-test-bed';
import { LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../test/login-service.mock';
import { MockDotMessageService } from '../../test/dot-message-service.mock';
import { DotDragDropAPIHtmlService } from './services/html/dot-drag-drop-api-html.service';
import { DotDOMHtmlUtilService } from './services/html/dot-dom-html-util.service';
import { DotEditContentToolbarHtmlService } from './services/html/dot-edit-content-toolbar-html.service';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { DotLoadingIndicatorModule } from '../../view/components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { DotMenuService } from '../../api/services/dot-menu.service';
import { WorkflowService } from '../../api/services/workflow/workflow.service';
import { Workflow } from '../../shared/models/workflow/workflow.model';
import { RouterTestingModule } from '@angular/router/testing';
import { EditPageService } from '../../api/services/edit-page/edit-page.service';
import { PageViewService } from '../../api/services/page-view/page-view.service';
import { DotGlobalMessageService } from '../../view/components/_common/dot-global-message/dot-global-message.service';
import { DotRenderedPage } from '../dot-edit-page/shared/models/dot-rendered-page.model';
import { combineAll } from 'rxjs/operator/combineAll';
import { PageMode } from './shared/page-mode.enum';

class WorkflowServiceMock {
    getPageWorkflows(pageIdentifier: string): Observable<Workflow[]> {
        return Observable.of([
            { name: 'Workflow 1', id: 'one' },
            { name: 'Workflow 2', id: 'two' },
            { name: 'Workflow 3', id: 'three' }
        ]);
    }
}

const fakePageRendered: DotRenderedPage = {
    canEdit: true,
    canLock: false,
    identifier: '123',
    languageId: 1,
    liveInode: '456',
    locked: false,
    lockedByAnotherUser: false,
    pageURI: 'A url',
    render: '<html></html>',
    shortyLive: '',
    shortyWorking: '',
    title: 'A title',
    workingInode: ''
};

describe('DotEditContentComponent', () => {
    let component: DotEditContentComponent;
    let de: DebugElement;
    let dotConfirmationService: DotConfirmationService;
    let dotEditContentHtmlService: DotEditContentHtmlService;
    let dotGlobalMessageService: DotGlobalMessageService;
    let editPageService: EditPageService;
    let fixture: ComponentFixture<DotEditContentComponent>;
    let route: ActivatedRoute;
    let workflowService: WorkflowService;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'editpage.toolbar.primary.action': 'Save',
            'editpage.toolbar.secondary.action': 'Cancel',
            'dot.common.message.saving': 'Saving...',
            'dot.common.message.saved': 'Saved',
            'editpage.content.steal.lock.confirmation_message.header': 'Are you sure?',
            'editpage.content.steal.lock.confirmation_message.message': 'This page is locked by bla bla',
            'editpage.content.steal.lock.confirmation_message.reject': 'Lock',
            'editpage.content.steal.lock.confirmation_message.accept': 'Cancel'
        });

        DOTTestBed.configureTestingModule({
            declarations: [DotEditContentComponent],
            imports: [
                DialogModule,
                BrowserAnimationsModule,
                DotEditPageToolbarModule,
                DotLoadingIndicatorModule,
                RouterTestingModule.withRoutes([
                    {
                        component: DotEditContentComponent,
                        path: 'test'
                    }
                ])
            ],
            providers: [
                DotGlobalMessageService,
                DotConfirmationService,
                DotContainerContentletService,
                DotDOMHtmlUtilService,
                DotDragDropAPIHtmlService,
                DotEditContentHtmlService,
                DotEditContentToolbarHtmlService,
                DotMenuService,
                EditPageService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: WorkflowService,
                    useClass: WorkflowServiceMock
                },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: Observable.of({
                            renderedPage: fakePageRendered
                        })
                    }
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotEditContentComponent);

        component = fixture.componentInstance;
        de = fixture.debugElement;
        dotConfirmationService = fixture.debugElement.injector.get(DotConfirmationService);
        dotEditContentHtmlService = fixture.debugElement.injector.get(DotEditContentHtmlService);
        dotGlobalMessageService = fixture.debugElement.injector.get(DotGlobalMessageService);
        editPageService = fixture.debugElement.injector.get(EditPageService);
        route = fixture.debugElement.injector.get(ActivatedRoute);
        workflowService = fixture.debugElement.injector.get(WorkflowService);
    });

    it('should have a toolbar', () => {
        const toolbarElement: DebugElement = fixture.debugElement.query(By.css('dot-edit-page-toolbar'));
        expect(toolbarElement).not.toBeNull();
    });

    it('should pass data to the toolbar', () => {
        fixture.detectChanges();
        expect(component.toolbar.page.title).toEqual('A title', 'toolbar have title');
        expect(component.toolbar.page.pageURI).toEqual('A url', 'toolbar have url');
        expect(component.toolbar.pageWorkflows).toEqual(
            [{ name: 'Workflow 1', id: 'one' }, { name: 'Workflow 2', id: 'two' }, { name: 'Workflow 3', id: 'three' }],
            'toolbar have workflows'
        );
    });

    xit('should check isModelUpdated', () => {});

    it('should show dotLoadingIndicatorService on init', () => {
        const spyLoadingIndicator = spyOn(component.dotLoadingIndicatorService, 'show');

        fixture.detectChanges();

        expect(spyLoadingIndicator).toHaveBeenCalled();
    });

    it('should set the page mode in preview', () => {
        spyOn(dotEditContentHtmlService, 'renderPage');
        spyOn(dotEditContentHtmlService, 'initEditMode');
        fixture.detectChanges();
        expect(component.pageMode).toEqual(PageMode.PREVIEW);

        const toolbar: DebugElement = de.query(By.css('.dot-edit__toolbar'));
        expect(toolbar.componentInstance.mode).toEqual(PageMode.PREVIEW);
        expect(dotEditContentHtmlService.renderPage).toHaveBeenCalledTimes(1);
        expect(dotEditContentHtmlService.initEditMode).not.toHaveBeenCalled();
    });

    it('should set the page mode in edit', () => {
        spyOn(dotEditContentHtmlService, 'renderPage');
        spyOn(dotEditContentHtmlService, 'initEditMode');
        route.data = Observable.of({
            renderedPage: {
                ...fakePageRendered,
                locked: true,
                canLock: true
            }
        });
        fixture.detectChanges();
        expect(component.pageMode).toEqual(PageMode.EDIT);

        const toolbar: DebugElement = de.query(By.css('.dot-edit__toolbar'));
        expect(toolbar.componentInstance.mode).toEqual(PageMode.EDIT);
        expect(dotEditContentHtmlService.renderPage).not.toHaveBeenCalled();
        expect(dotEditContentHtmlService.initEditMode).toHaveBeenCalledTimes(1);
    });

    it('should set the page mode in preview when the page is locked by another user', () => {
        spyOn(dotEditContentHtmlService, 'renderPage');
        spyOn(dotEditContentHtmlService, 'initEditMode');
        route.data = Observable.of({
            renderedPage: {
                ...fakePageRendered,
                lockedByAnotherUser: true,
                locked: true,
                canLock: true
            }
        });
        fixture.detectChanges();
        expect(component.pageMode).toEqual(PageMode.PREVIEW);

        const toolbar: DebugElement = de.query(By.css('.dot-edit__toolbar'));
        expect(toolbar.componentInstance.mode).toEqual(PageMode.PREVIEW);
        expect(dotEditContentHtmlService.renderPage).toHaveBeenCalledTimes(1);
        expect(dotEditContentHtmlService.initEditMode).not.toHaveBeenCalled();
    });

    it('should hide dotLoadingIndicatorService when the component loads', () => {
        const spyLoadingIndicator = spyOn(component.dotLoadingIndicatorService, 'hide');
        const loadingIndicatorElem: DebugElement = de.query(By.css('dot-loading-indicator'));

        component.onLoad(Event);

        expect(loadingIndicatorElem).not.toBeNull();
        expect(spyLoadingIndicator).toHaveBeenCalled();
    });

    it('should display confirmation dialog and remove contentlet when user accepts', () => {
        fixture.detectChanges();

        const mockResEvent = {
            contentletEvents: {},
            dataset: {
                dotIdentifier: '2sfasfk-sd2d-4dxc-sdfnsdkjnajd0',
                dotInode: '26ad1jbj-23xd-4cx3-9cf2-432scc413cc2'
            },
            container: {
                dotIdentifier: '3',
                dotUuid: '4'
            },
            name: 'remove'
        };

        spyOn(dotEditContentHtmlService, 'contentletEvents').and.returnValue(Observable.of(mockResEvent));
        spyOn(dotEditContentHtmlService, 'removeContentlet').and.callFake((res) => {});

        spyOn(dotConfirmationService, 'confirm').and.callFake((conf) => {
            conf.accept();
        });

        component['removeContentlet'](mockResEvent);

        expect(dotEditContentHtmlService.removeContentlet).toHaveBeenCalledWith(
            {
                identifier: mockResEvent.container.dotIdentifier,
                uuid: mockResEvent.container.dotUuid
            },
            {
                inode: mockResEvent.dataset.dotInode,
                identifier: mockResEvent.dataset.dotIdentifier
            }
        );
    });

    it('should call the setPageState in the editPageService correctly', () => {
        spyOn(editPageService, 'setPageState').and.callThrough();

        fixture.detectChanges();

        component.statePageHandler({
            locked: false
        });
        expect(editPageService.setPageState).toHaveBeenCalledWith(fakePageRendered, {
            locked: false
        });
    });


    it('should set the page state (lock)', () => {
        spyOn(component, 'statePageHandler').and.callThrough();
        spyOn(dotGlobalMessageService, 'display').and.callThrough();

        fixture.detectChanges();

        component.toolbar.changeState.emit({
            locked: true,
            mode: null
        });

        expect(component.statePageHandler).toHaveBeenCalledWith({
            locked: true,
            mode: null
        });
        expect(dotGlobalMessageService.display).toHaveBeenCalledWith('Saving...');

        // TODO: figure it out how to test this after the response of the lock method
        // expect(dotGlobalMessageService.display).toHaveBeenCalledWith('Saved');
    });

    it('should set the page mode', () => {
        fixture.detectChanges();

        spyOn(editPageService, 'setPageState').and.returnValue(
            Observable.of({
                dotRenderedPage: {},
                lockState: 'locked'
            })
        );
        spyOn(component, 'statePageHandler').and.callThrough();
        spyOn(component, 'setPage').and.callThrough();
        spyOn(dotGlobalMessageService, 'display').and.callThrough();

        component.toolbar.changeState.emit({
            locked: null,
            mode: PageMode.PREVIEW
        });

        expect(component.statePageHandler).toHaveBeenCalledWith({
            locked: null,
            mode: PageMode.PREVIEW
        });
        expect(dotGlobalMessageService.display).not.toHaveBeenCalled();
        expect(component.setPage).toHaveBeenCalledTimes(1);

        // TODO: figure it out how to test this after the response of the lock method
        // expect(dotGlobalMessageService.display).toHaveBeenCalledWith('Saved');
    });

    it('should set the page state in edit mode', () => {
        spyOn(workflowService, 'getPageWorkflows').and.returnValue(Observable.of([]));
        spyOn(dotEditContentHtmlService, 'initEditMode');
        const mockPageRendered: DotRenderedPage = {
            canEdit: true,
            locked: true,
            canLock: true,
            identifier: '123',
            languageId: 1,
            liveInode: '456',
            title: 'Hello World',
            pageURI: 'url',
            render: '<html></html>',
            shortyLive: '000',
            shortyWorking: '000',
            workingInode: '000'
        };
        fixture.detectChanges();

        component.setPage(mockPageRendered);

        expect(component.page).toBe(mockPageRendered);
        expect(workflowService.getPageWorkflows).toHaveBeenCalledWith('123');
        expect(dotEditContentHtmlService.initEditMode).toHaveBeenCalledWith('<html></html>', component.iframe);
    });

    it('should set the page state in preview mode', () => {
        spyOn(workflowService, 'getPageWorkflows').and.returnValue(Observable.of([]));
        spyOn(dotEditContentHtmlService, 'renderPage');
        const mockPageRendered: DotRenderedPage = {
            canEdit: true,
            locked: false,
            canLock: true,
            identifier: '123',
            languageId: 1,
            liveInode: '456',
            title: 'Hello World',
            pageURI: 'url',
            render: '<html></html>',
            shortyLive: '000',
            shortyWorking: '000',
            workingInode: '000'
        };
        fixture.detectChanges();

        component.setPage(mockPageRendered);

        expect(component.page).toBe(mockPageRendered);
        expect(workflowService.getPageWorkflows).toHaveBeenCalledWith('123');
        expect(dotEditContentHtmlService.renderPage).toHaveBeenCalledWith('<html></html>', component.iframe);
    });
});
