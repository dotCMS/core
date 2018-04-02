import { SiteServiceMock, mockSites } from './../../../test/site-service.mock';
import { ActivatedRoute } from '@angular/router';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture } from '@angular/core/testing';
import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { Observable } from 'rxjs/Observable';
import { DialogModule } from 'primeng/primeng';
import { LoginService, SiteService } from 'dotcms-js/dotcms-js';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotContainerContentletService } from './services/dot-container-contentlet.service';
import { DotContentletLockerService } from '../../../api/services/dot-contentlet-locker/dot-contentlet-locker.service';
import { DotDOMHtmlUtilService } from './services/html/dot-dom-html-util.service';
import { DotDialogService } from '../../../api/services/dot-dialog/index';
import { DotDragDropAPIHtmlService } from './services/html/dot-drag-drop-api-html.service';
import { DotEditContentComponent } from './dot-edit-content.component';
import { DotEditContentHtmlService } from './services/dot-edit-content-html/dot-edit-content-html.service';
import { DotEditContentToolbarHtmlService } from './services/html/dot-edit-content-toolbar-html.service';
import { DotEditPageService } from '../../../api/services/dot-edit-page/dot-edit-page.service';
import { DotEditPageToolbarModule } from './components/dot-edit-page-toolbar/dot-edit-page-toolbar.module';
import { DotGlobalMessageService } from '../../../view/components/_common/dot-global-message/dot-global-message.service';
import { DotHttpErrorManagerService } from '../../../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotLoadingIndicatorModule } from '../../../view/components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { DotMenuService } from '../../../api/services/dot-menu.service';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { DotPageState, DotRenderedPageState } from '../shared/models/dot-rendered-page-state.model';
import { DotPageStateService } from './services/dot-page-state/dot-page-state.service';
import { DotRenderHTMLService } from '../../../api/services/dot-render-html/dot-render-html.service';
import { LoginServiceMock, mockUser } from '../../../test/login-service.mock';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { PageMode } from '../shared/models/page-mode.enum';
import { DotWorkflowService } from '../../../api/services/dot-workflow/dot-workflow.service';
import { DotWorkflowServiceMock } from '../../../test/dot-workflow-service.mock';
import { mockDotRenderedPage, mockDotPage } from '../../../test/dot-rendered-page.mock';
import { DotEditPageViewAs } from '../../../shared/models/dot-edit-page-view-as/dot-edit-page-view-as.model';
import { mockDotDevice } from '../../../test/dot-device.mock';
import { mockDotEditPageViewAs } from '../../../test/dot-edit-page-view-as.mock';
import { mockResponseView } from '../../../test/response-view.mock';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';

export const mockDotPageState: DotPageState = {
    mode: PageMode.PREVIEW,
    locked: false
};

@Component({
    selector: 'dot-edit-content-view-as-toolbar',
    template: ''
})
class MockDotEditContentViewAsToolbarComponent {
    @Input() value: DotEditPageViewAs;
    @Output() changeViewAs = new EventEmitter<DotEditPageViewAs>();
}
describe('DotEditContentComponent', () => {
    let component: DotEditContentComponent;
    let de: DebugElement;
    let dotDialogService: DotDialogService;
    let dotEditContentHtmlService: DotEditContentHtmlService;
    let dotGlobalMessageService: DotGlobalMessageService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let dotPageStateService: DotPageStateService;
    let dotRouterService: DotRouterService;
    let fixture: ComponentFixture<DotEditContentComponent>;
    const siteServiceMock = new SiteServiceMock();
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
            declarations: [DotEditContentComponent, MockDotEditContentViewAsToolbarComponent],
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
                    provide: DotWorkflowService,
                    useClass: DotWorkflowServiceMock
                },
                {
                    provide: SiteService,
                    useValue: siteServiceMock
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
                        },
                        snapshot: {
                            queryParams: {
                                url: 'an/url/fake'
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
        dotHttpErrorManagerService = de.injector.get(DotHttpErrorManagerService);
        dotPageStateService = de.injector.get(DotPageStateService);
        dotRouterService = de.injector.get(DotRouterService);
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

    describe('set new View As configuration', () => {
        let viewAsToolbar: DebugElement;

        beforeEach(() => {
            viewAsToolbar = fixture.debugElement.query(By.css('dot-edit-content-view-as-toolbar'));
            component.pageState = new DotRenderedPageState(mockUser, mockDotRenderedPage, null);
        });

        it('should have a View As toolbar', () => {
            expect(viewAsToolbar).not.toBeNull();
        });

        it('should set the page wrapper dimensions based on device', () => {
            const pageWrapper: DebugElement = de.query(By.css('.dot-edit__page-wrapper'));
            component.pageState.viewAs.device = mockDotDevice;
            fixture.detectChanges();
            expect(pageWrapper.styles).toEqual({ width: mockDotDevice.cssWidth + 'px', height: mockDotDevice.cssHeight + 'px' });
            expect(pageWrapper.nativeElement.classList.contains('dot-edit__page-wrapper--deviced')).toBe(true);
        });

        it('should change the Language/Persona of the page when viewAs configuration changes and set the dev', () => {
            spyOn(component, 'changeViewAsHandler').and.callThrough();
            spyOn(dotPageStateService, 'set');
            viewAsToolbar.componentInstance.changeViewAs.emit(mockDotEditPageViewAs);

            expect(component.changeViewAsHandler).toHaveBeenCalledWith(mockDotEditPageViewAs);
            expect(dotPageStateService.set).toHaveBeenCalledWith(
                component.pageState.page,
                component.pageState.state,
                mockDotEditPageViewAs
            );
        });

        it('should send the View As initial configuration to the toolbar', () => {
            fixture.detectChanges();
            expect(viewAsToolbar.componentInstance.value).toEqual(mockDotRenderedPage.viewAs);
        });
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
                        mode: PageMode.EDIT
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
                    },
                    viewAs: {}
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

        it('should set live mode', () => {
            spyStateSet({
                ...mockDotRenderedPage,
                state: {
                    mode: PageMode.LIVE,
                    locked: true
                }
            });

            fixture.detectChanges();

            component.toolbar.changeState.emit({
                mode: PageMode.LIVE
            });

            expect(component.statePageHandler).toHaveBeenCalledWith({
                mode: PageMode.LIVE
            });

            expect(component.pageState.page).toEqual(mockDotPage);
            expect(component.pageState.state).toEqual({
                mode: PageMode.LIVE,
                locked: true
            });
            expect(dotGlobalMessageService.display).not.toHaveBeenCalled();
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

    describe('handle switch site', () => {
        const mockRenderedPageState = new DotRenderedPageState(mockUser, mockDotRenderedPage);

        beforeEach(() => {
            component.pageState = null;

        });

        it('should rerender pagestate after switch site', () => {
            expect(component.pageState).toBe(null);

            spyOn(dotPageStateService, 'get').and.returnValue(Observable.of(mockRenderedPageState));
            fixture.detectChanges();

            siteServiceMock.setFakeCurrentSite(mockSites[1]);
            expect(dotPageStateService.get).toHaveBeenCalledWith('an/url/fake');
            expect(component.pageState).toBe(mockRenderedPageState);
        });

        it('should handle error', () => {
            const fake500Response = mockResponseView(500);
            spyOn(dotPageStateService, 'get').and.returnValue(Observable.throw(fake500Response));
            spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
            spyOn(dotRouterService, 'gotoPortlet');
            fixture.detectChanges();

            siteServiceMock.setFakeCurrentSite(mockSites[1]);
            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(fake500Response);
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/c/site-browser');

        });
    });
});
