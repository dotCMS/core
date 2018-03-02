import { ActivatedRoute } from '@angular/router';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { DialogModule } from 'primeng/primeng';
import { DotContainerContentletService } from './services/dot-container-contentlet.service';
import { DotDOMHtmlUtilService } from './services/html/dot-dom-html-util.service';
import { DotDialogService } from '../../../api/services/dot-dialog/index';
import { DotDragDropAPIHtmlService } from './services/html/dot-drag-drop-api-html.service';
import { DotEditContentComponent } from './dot-edit-content.component';
import { DotEditContentHtmlService } from './services/dot-edit-content-html.service';
import { DotEditContentToolbarHtmlService } from './services/html/dot-edit-content-toolbar-html.service';
import { DotEditPageToolbarModule } from './components/dot-edit-page-toolbar/dot-edit-page-toolbar.module';
import { DotGlobalMessageService } from '../../../view/components/_common/dot-global-message/dot-global-message.service';
import { DotLoadingIndicatorModule } from '../../../view/components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { DotMenuService } from '../../../api/services/dot-menu.service';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { DotRenderedPage } from '../shared/models/dot-rendered-page.model';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';
import { EditPageService } from '../../../api/services/edit-page/edit-page.service';
import { LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { Observable } from 'rxjs/Observable';
import { PageMode } from '../../dot-edit-page/content/shared/page-mode.enum';
import { RouterTestingModule } from '@angular/router/testing';
import { Workflow } from '../../../shared/models/workflow/workflow.model';
import { WorkflowService } from '../../../api/services/workflow/workflow.service';
import { DotHttpErrorManagerService } from '../../../api/services/dot-http-error-manager/dot-http-error-manager.service';

class WorkflowServiceMock {
    getPageWorkflows(): Observable<Workflow[]> {
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
    mode: PageMode.PREVIEW,
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
    let dotEditContentHtmlService: DotEditContentHtmlService;
    let dotGlobalMessageService: DotGlobalMessageService;
    let dotDialogService: DotDialogService;
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
                DotContainerContentletService,
                DotDOMHtmlUtilService,
                DotDialogService,
                DotDragDropAPIHtmlService,
                DotEditContentHtmlService,
                DotEditContentToolbarHtmlService,
                DotGlobalMessageService,
                DotHttpErrorManagerService,
                DotMenuService,
                DotRouterService,
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
                            content: fakePageRendered
                        })
                    }
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotEditContentComponent);

        component = fixture.componentInstance;
        de = fixture.debugElement;
        dotEditContentHtmlService = fixture.debugElement.injector.get(DotEditContentHtmlService);
        dotDialogService = fixture.debugElement.injector.get(DotDialogService);
        editPageService = fixture.debugElement.injector.get(EditPageService);
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

        const toolbar: DebugElement = de.query(By.css('.dot-edit__toolbar'));
        expect(toolbar.componentInstance.mode).toEqual(PageMode.PREVIEW);
        expect(dotEditContentHtmlService.renderPage).toHaveBeenCalledTimes(1);
        expect(dotEditContentHtmlService.initEditMode).not.toHaveBeenCalled();
    });

    it('should set the page mode in edit', () => {
        spyOn(dotEditContentHtmlService, 'renderPage');
        spyOn(dotEditContentHtmlService, 'initEditMode');
        route.data = Observable.of({
            content: {
                ...fakePageRendered,
                locked: true,
                canLock: true,
                mode: PageMode.EDIT
            }
        });
        fixture.detectChanges();

        const toolbar: DebugElement = de.query(By.css('.dot-edit__toolbar'));
        expect(dotEditContentHtmlService.renderPage).not.toHaveBeenCalled();
        expect(dotEditContentHtmlService.initEditMode).toHaveBeenCalledTimes(1);
    });

    it('should set the page mode in preview when the page is locked by another user', () => {
        spyOn(dotEditContentHtmlService, 'renderPage');
        spyOn(dotEditContentHtmlService, 'initEditMode');
        route.data = Observable.of({
            content: {
                ...fakePageRendered,
                locked: true,
                canLock: true
            }
        });
        fixture.detectChanges();

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
        spyOn(dotEditContentHtmlService, 'removeContentlet').and.callFake(() => {});

        spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
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

        const dummyPage: DotRenderedPage = {
            ...fakePageRendered,
            lockedBy: 'Some User',
            pageURI: '/whatever',
        };

        spyOn(editPageService, 'setPageState').and.returnValue(
            Observable.of({
                dotRenderedPage: dummyPage,
                lockState: 'locked'
            })
        );
        spyOn(component, 'statePageHandler').and.callThrough();
        spyOn(dotGlobalMessageService, 'display').and.callThrough();

        component.toolbar.changeState.emit({
            locked: null,
            mode: PageMode.PREVIEW
        });

        expect(component.statePageHandler).toHaveBeenCalledWith({
            locked: null,
            mode: PageMode.PREVIEW
        });
        expect(component.page).toEqual(dummyPage);
        expect(dotGlobalMessageService.display).not.toHaveBeenCalled();

        // TODO: figure it out how to test this after the response of the lock method
        // expect(dotGlobalMessageService.display).toHaveBeenCalledWith('Saved');
    });

    it('should set the page state in edit mode', () => {
        spyOn(workflowService, 'getPageWorkflows').and.returnValue(Observable.of([]));
        spyOn(dotEditContentHtmlService, 'initEditMode');
        route.data = Observable.of({
            content: {
                ...fakePageRendered,
                locked: true,
                canLock: true,
                mode: PageMode.EDIT
            }
        });
        fixture.detectChanges();

        expect(workflowService.getPageWorkflows).toHaveBeenCalledWith('123');
        expect(dotEditContentHtmlService.initEditMode).toHaveBeenCalledWith('<html></html>', component.iframe);
    });

    it('should set the page state in preview mode', () => {
        spyOn(workflowService, 'getPageWorkflows').and.returnValue(Observable.of([]));
        spyOn(dotEditContentHtmlService, 'renderPage');
        route.data = Observable.of({
            content: {
                ...fakePageRendered,
                locked: false,
                mode: PageMode.PREVIEW
            }
        });
        fixture.detectChanges();

        expect(workflowService.getPageWorkflows).toHaveBeenCalledWith('123');
        expect(dotEditContentHtmlService.renderPage).toHaveBeenCalledWith('<html></html>', component.iframe);
    });
});
