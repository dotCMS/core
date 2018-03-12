import { ActivatedRoute } from '@angular/router';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';

import { Observable } from 'rxjs/Observable';

import { DialogModule } from 'primeng/primeng';

import { LoginService } from 'dotcms-js/dotcms-js';

import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotContainerContentletService } from './services/dot-container-contentlet.service';
import { DotContentletLockerService } from '../../../api/services/dot-contentlet-locker/dot-contentlet-locker.service';
import { DotDOMHtmlUtilService } from './services/html/dot-dom-html-util.service';
import { DotDialogService } from '../../../api/services/dot-dialog/index';
import { DotDragDropAPIHtmlService } from './services/html/dot-drag-drop-api-html.service';
import { DotEditContentComponent } from './dot-edit-content.component';
import { DotEditContentHtmlService } from './services/dot-edit-content-html.service';
import { DotEditContentToolbarHtmlService } from './services/html/dot-edit-content-toolbar-html.service';
import { DotEditPageService } from '../../../api/services/dot-edit-page/dot-edit-page.service';
import { DotEditPageToolbarModule } from './components/dot-edit-page-toolbar/dot-edit-page-toolbar.module';
import { DotGlobalMessageService } from '../../../view/components/_common/dot-global-message/dot-global-message.service';
import { DotHttpErrorManagerService } from '../../../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotLoadingIndicatorModule } from '../../../view/components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { DotMenuService } from '../../../api/services/dot-menu.service';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { DotPageState } from '../shared/models/dot-rendered-page-state.model';
import { DotPageStateService } from './services/dot-page-state/dot-page-state.service';
import { DotRenderHTMLService } from '../../../api/services/dot-render-html/dot-render-html.service';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { PageMode } from '../shared/models/page-mode.enum';
import { Workflow } from '../../../shared/models/workflow/workflow.model';
import { WorkflowService } from '../../../api/services/workflow/workflow.service';
import { mockDotRenderedPage, mockDotPage } from '../../../test/dot-rendered-page.mock';

class WorkflowServiceMock {
    getPageWorkflows(): Observable<Workflow[]> {
        return Observable.of([{ name: 'Workflow 1', id: 'one' }, { name: 'Workflow 2', id: 'two' }, { name: 'Workflow 3', id: 'three' }]);
    }
}

export const mockDotPageState: DotPageState = {
    mode: PageMode.PREVIEW,
    locked: false
};

describe('DotEditContentComponent', () => {
    let component: DotEditContentComponent;
    let de: DebugElement;
    let dotDialogService: DotDialogService;
    let dotEditContentHtmlService: DotEditContentHtmlService;
    let dotGlobalMessageService: DotGlobalMessageService;
    let dotPageStateService: DotPageStateService;
    let fixture: ComponentFixture<DotEditContentComponent>;
    let route: ActivatedRoute;

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
                DotContentletLockerService,
                DotDOMHtmlUtilService,
                DotDialogService,
                DotDragDropAPIHtmlService,
                DotEditContentHtmlService,
                DotEditContentToolbarHtmlService,
                DotGlobalMessageService,
                DotHttpErrorManagerService,
                DotMenuService,
                DotPageStateService,
                DotRenderHTMLService,
                DotRouterService,
                DotEditPageService,
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
                        parent: {
                            parent: {
                                data: Observable.of({
                                    content: {
                                        ...mockDotRenderedPage,
                                        state: mockDotPageState
                                    }
                                })
                            }
                        }
                    }
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotEditContentComponent);

        component = fixture.componentInstance;
        de = fixture.debugElement;
        dotDialogService = de.injector.get(DotDialogService);
        dotEditContentHtmlService = de.injector.get(DotEditContentHtmlService);
        dotGlobalMessageService = de.injector.get(DotGlobalMessageService);
        dotPageStateService = de.injector.get(DotPageStateService);
        route = de.injector.get(ActivatedRoute);
    });

    it('should have a toolbar', () => {
        const toolbarElement: DebugElement = de.query(By.css('dot-edit-page-toolbar'));
        expect(toolbarElement).not.toBeNull();
    });

    it('should pass data to the toolbar', () => {
        fixture.detectChanges();
        expect(component.toolbar.pageState.page).toEqual(mockDotPage);
        expect(component.toolbar.pageState.state).toEqual(mockDotPageState);
    });

    it('should have loading indicator', () => {
        const loadingIndicator: DebugElement = de.query(By.css('dot-loading-indicator'));
        expect(loadingIndicator).not.toBeNull();
    });

    it('should have iframe', () => {
        const iframe: DebugElement = de.query(By.css('.dot-edit__iframe'));
        expect(iframe).not.toBeNull();
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

    describe('set default page state', () => {
        beforeEach(() => {
            spyOn(dotEditContentHtmlService, 'renderPage');
            spyOn(dotEditContentHtmlService, 'initEditMode');
        });

        it('should set page mode in preview', () => {
            fixture.detectChanges();

            expect(dotEditContentHtmlService.renderPage).toHaveBeenCalledTimes(1);
            expect(dotEditContentHtmlService.initEditMode).not.toHaveBeenCalled();
        });

        it('should set page mode in edit', () => {
            route.parent.parent.data = Observable.of({
                content: {
                    ...mockDotRenderedPage,
                    page: {
                        ...mockDotRenderedPage.page,
                        canLock: true
                    },
                    state: {
                        locked: true,
                        mode: PageMode.EDIT,
                    }
                }
            });
            fixture.detectChanges();

            expect(dotEditContentHtmlService.renderPage).not.toHaveBeenCalled();
            expect(dotEditContentHtmlService.initEditMode).toHaveBeenCalledTimes(1);
        });

        it('should set page mode in preview when the page is locked by another user', () => {
            route.parent.parent.data = Observable.of({
                content: {
                    page: {
                        ...mockDotRenderedPage,
                        canLock: true
                    },
                    state: {
                        locked: true,
                        mode: PageMode.PREVIEW
                    }
                }
            });
            fixture.detectChanges();

            const toolbar: DebugElement = de.query(By.css('.dot-edit__toolbar'));
            expect(toolbar.componentInstance.mode).toEqual(PageMode.PREVIEW);
            expect(dotEditContentHtmlService.renderPage).toHaveBeenCalledTimes(1);
            expect(dotEditContentHtmlService.initEditMode).not.toHaveBeenCalled();
        });
    });

    describe('set page state when toolbar emit new state', () => {
        const spyStateSet = (val) => {
            spyOn(dotPageStateService, 'set').and.returnValue(Observable.of(val));
        };

        beforeEach(() => {
            spyOn(component, 'statePageHandler').and.callThrough();
            spyOn(dotGlobalMessageService, 'display');
            spyOn(dotEditContentHtmlService, 'renderPage');
            spyOn(dotEditContentHtmlService, 'initEditMode');
        });

        it('should set edit mode', () => {
            spyStateSet({
                ...mockDotRenderedPage,
                state: {
                    mode: PageMode.EDIT,
                    locked: true
                }
            });

            fixture.detectChanges();

            component.toolbar.changeState.emit({
                locked: true,
                mode: PageMode.EDIT
            });

            expect(component.statePageHandler).toHaveBeenCalledWith({
                locked: true,
                mode: PageMode.EDIT
            });

            expect(component.pageState.page).toEqual(mockDotPage);
            expect(component.pageState.state).toEqual({
                mode: PageMode.EDIT,
                locked: true
            });
            expect(dotGlobalMessageService.display).toHaveBeenCalledTimes(2);
            expect(dotEditContentHtmlService.initEditMode).toHaveBeenCalledWith('<html></html>', component.iframe);
        });

        it('should set preview mode', () => {
            spyStateSet({
                ...mockDotRenderedPage,
                state: {
                    mode: PageMode.PREVIEW,
                    locked: true
                }
            });

            fixture.detectChanges();

            component.toolbar.changeState.emit({
                locked: true,
                mode: PageMode.PREVIEW
            });

            expect(component.statePageHandler).toHaveBeenCalledWith({
                locked: true,
                mode: PageMode.PREVIEW
            });

            expect(component.pageState.page).toEqual(mockDotPage);
            expect(component.pageState.state).toEqual({
                mode: PageMode.PREVIEW,
                locked: true
            });
            expect(dotGlobalMessageService.display).toHaveBeenCalledTimes(2);
            expect(dotEditContentHtmlService.initEditMode).not.toHaveBeenCalled();
            expect(dotEditContentHtmlService.renderPage).toHaveBeenCalledWith('<html></html>', component.iframe);
        });
    });

    describe('edit contentlets', () => {
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
    });
});
