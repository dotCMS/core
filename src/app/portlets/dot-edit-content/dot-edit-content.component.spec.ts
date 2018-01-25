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
import { PageState } from './components/dot-edit-page-toolbar/dot-edit-page-toolbar.component';

class WorkflowServiceMock {
    getPageWorkflows(pageIdentifier: string): Observable<Workflow[]> {
        return Observable.of([
            { name: 'Workflow 1', id: 'one' },
            { name: 'Workflow 2', id: 'two' },
            { name: 'Workflow 3', id: 'three' }
        ]);
    }
}

describe('DotEditContentComponent', () => {
    let component: DotEditContentComponent;
    let fixture: ComponentFixture<DotEditContentComponent>;
    let de: DebugElement;
    let dotConfirmationService: DotConfirmationService;
    let pageViewService: PageViewService;
    let dotGlobalMessageService: DotGlobalMessageService;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'editpage.toolbar.primary.action': 'Save',
            'editpage.toolbar.secondary.action': 'Cancel',
            'dot.common.message.locking': 'Locking...',
            'dot.common.message.locked': 'Locked',
            'dot.common.message.unlocking': 'Unlocking...',
            'dot.common.message.unlocked': 'Unlocked'
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
                PageViewService,
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
                            renderedPage: {
                                title: 'A title',
                                url: 'A url',
                                identifier: '123',
                                inode: '456',
                                render: '<html></html>'
                            }
                        })
                    }
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotEditContentComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        dotConfirmationService = fixture.debugElement.injector.get(DotConfirmationService);
        pageViewService = fixture.debugElement.injector.get(PageViewService);
        dotGlobalMessageService = fixture.debugElement.injector.get(DotGlobalMessageService);
    });

    it('should have a toolbar', () => {
        const toolbarElement: DebugElement = fixture.debugElement.query(By.css('dot-edit-page-toolbar'));
        expect(toolbarElement).not.toBeNull();
    });

    it('should pass data to the toolbar', () => {
        fixture.detectChanges();
        expect(component.toolbar.pageTitle).toEqual('A title', 'toolbar have title');
        expect(component.toolbar.pageUrl).toEqual('A url', 'toolbar have url');
        expect(component.toolbar.pageWorkflows).toEqual([
            { name: 'Workflow 1', id: 'one' },
            { name: 'Workflow 2', id: 'two' },
            { name: 'Workflow 3', id: 'three' }
        ], 'toolbar have workflows');
    });

    xit('should check isModelUpdated', () => {});

    it('should show dotLoadingIndicatorService on init', () => {
        const spyLoadingIndicator = spyOn(component.dotLoadingIndicatorService, 'show');

        fixture.detectChanges();

        expect(spyLoadingIndicator).toHaveBeenCalled();
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
                dotContentIdentifier: '2sfasfk-sd2d-4dxc-sdfnsdkjnajd0',
                dotContentInode: '26ad1jbj-23xd-4cx3-9cf2-432scc413cc2',
                dotContainerIdentifier: '3',
                dotContainerUuid: '4'
            },
            name: 'remove'
        };
        const dotEditContentHtmlService = fixture.debugElement.injector.get(DotEditContentHtmlService);

        spyOn(dotEditContentHtmlService, 'contentletEvents').and.returnValue(Observable.of(mockResEvent));
        spyOn(dotEditContentHtmlService, 'removeContentlet').and.callFake(res => {});

        spyOn(dotConfirmationService, 'confirm').and.callFake(conf => {
            conf.accept();
        });

        component['removeContentlet'](mockResEvent);

        expect(dotEditContentHtmlService.removeContentlet).toHaveBeenCalledWith(
            {
                identifier: mockResEvent.dataset.dotContainerIdentifier,
                uuid: mockResEvent.dataset.dotContainerUuid
            },
            {
                inode: mockResEvent.dataset.dotContentInode,
                identifier: mockResEvent.dataset.dotContentIdentifier
            });
    });

    it('should lock the page on toolbar lock event', () => {
        spyOn(component, 'lockPageHandler').and.callThrough();
        spyOn(pageViewService, 'lock').and.callThrough();
        spyOn(dotGlobalMessageService, 'display').and.callThrough();

        fixture.detectChanges();

        component.toolbar.lockPage.emit(true);

        expect(component.lockPageHandler).toHaveBeenCalledWith(true);
        expect(pageViewService.lock).toHaveBeenCalledWith('123');
        expect(dotGlobalMessageService.display).toHaveBeenCalledWith('Locking...');

        // TODO: figure it out how to test this after the response of the lock method
        // expect(dotGlobalMessageService.display).toHaveBeenCalledWith('Locked');
    });

    it('should unlock the page on toolbar lock event', () => {
        spyOn(component, 'lockPageHandler').and.callThrough();
        spyOn(pageViewService, 'lock').and.callThrough();
        spyOn(dotGlobalMessageService, 'display').and.callThrough();

        fixture.detectChanges();

        component.toolbar.lockPage.emit(false);

        expect(component.lockPageHandler).toHaveBeenCalledWith(false);
        expect(pageViewService.lock).toHaveBeenCalledWith('123');
        expect(dotGlobalMessageService.display).toHaveBeenCalledWith('Unlocking...');

        // TODO: figure it out how to test this after the response of the method
        // expect(dotGlobalMessageService.display).toHaveBeenCalledWith('Unlocked');
    });

    it('should update the page on toolbar page state event', () => {
        spyOn(component, 'statePageHandler').and.callThrough();
        fixture.detectChanges();

        component.toolbar.pageState.emit(PageState.LIVE);
        expect(component.statePageHandler).toHaveBeenCalledWith(PageState.LIVE);
    });
});
