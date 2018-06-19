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
import { DotAlertConfirmService } from '../../../api/services/dot-alert-confirm/index';
import { DotDragDropAPIHtmlService } from './services/html/dot-drag-drop-api-html.service';
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
import { mockDotDevices } from '../../../test/dot-device.mock';
import { mockDotEditPageViewAs } from '../../../test/dot-edit-page-view-as.mock';
import { mockResponseView } from '../../../test/response-view.mock';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';
import { DotEditPageDataService } from '../shared/services/dot-edit-page-resolver/dot-edit-page-data.service';
import { DotEditPageToolbarComponent } from './components/dot-edit-page-toolbar/dot-edit-page-toolbar.component';
import { DotContentletEditorService } from '../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotPageContainer } from '../shared/models/dot-page-container.model';
import { DotEditContentComponent } from './dot-edit-content.component';
import { ContentType } from '../../content-types/shared/content-type.model';
import { DotContentletEditorModule } from '../../../view/components/dot-contentlet-editor/dot-contentlet-editor.module';

export const mockDotPageState: DotPageState = {
    mode: PageMode.PREVIEW,
    locked: false
};

@Component({
    selector: 'dot-edit-content-view-as-toolbar',
    template: ''
})
class MockDotEditContentViewAsToolbarComponent {
    @Input() pageState: DotRenderedPageState;
    @Output() changeViewAs = new EventEmitter<DotEditPageViewAs>();
}

@Component({
    selector: 'dot-whats-changed',
    template: ''
})
class MockDotWhatsChangedComponent {
    @Input() pageId: string;
}

@Component({
    selector: 'dot-form-selector',
    template: ''
})
export class MockDotFormSelectorComponent {
    @Input() show = false;
    @Output() select = new EventEmitter<ContentType>();
    @Output() close = new EventEmitter<any>();
}

describe('DotEditContentComponent', () => {
    const siteServiceMock = new SiteServiceMock();
    let component: DotEditContentComponent;
    let de: DebugElement;
    let dotDialogService: DotAlertConfirmService;
    let dotEditContentHtmlService: DotEditContentHtmlService;
    let dotEditPageDataService: DotEditPageDataService;
    let dotGlobalMessageService: DotGlobalMessageService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let dotPageStateService: DotPageStateService;
    let dotRouterService: DotRouterService;
    let fixture: ComponentFixture<DotEditContentComponent>;
    let route: ActivatedRoute;
    let toolbarComponent: DotEditPageToolbarComponent;
    let toolbarElement: DebugElement;
    let dotContentletEditorService: DotContentletEditorService;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'dot.common.message.saving': 'Saving...',
            'dot.common.message.saved': 'Saved',
            'editpage.content.steal.lock.confirmation_message.header': 'Are you sure?',
            'editpage.content.steal.lock.confirmation_message.message': 'This page is locked by bla bla',
            'editpage.content.steal.lock.confirmation_message.reject': 'Lock',
            'editpage.content.steal.lock.confirmation_message.accept': 'Cancel',
            'editpage.content.save.changes.confirmation.header': 'Save header',
            'editpage.content.save.changes.confirmation.message': 'Save message',
            'dot.common.content.search': 'Content Search'
        });

        DOTTestBed.configureTestingModule({
            declarations: [
                DotEditContentComponent,
                MockDotEditContentViewAsToolbarComponent,
                MockDotWhatsChangedComponent,
                MockDotFormSelectorComponent
            ],
            imports: [
                BrowserAnimationsModule,
                DialogModule,
                DotContentletEditorModule,
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
                DotAlertConfirmService,
                DotDragDropAPIHtmlService,
                DotEditContentHtmlService,
                DotEditContentToolbarHtmlService,
                DotEditPageService,
                DotGlobalMessageService,
                DotHttpErrorManagerService,
                DotMenuService,
                DotPageStateService,
                DotRenderHTMLService,
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
                DotEditPageDataService,
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
        dotContentletEditorService = de.injector.get(DotContentletEditorService);
        dotDialogService = de.injector.get(DotAlertConfirmService);
        dotEditContentHtmlService = de.injector.get(DotEditContentHtmlService);
        dotEditPageDataService = de.injector.get(DotEditPageDataService);
        dotGlobalMessageService = de.injector.get(DotGlobalMessageService);
        dotHttpErrorManagerService = de.injector.get(DotHttpErrorManagerService);
        dotPageStateService = de.injector.get(DotPageStateService);
        dotRouterService = de.injector.get(DotRouterService);
        toolbarElement = de.query(By.css('dot-edit-page-toolbar'));
        toolbarComponent = toolbarElement.componentInstance;
        route = de.injector.get(ActivatedRoute);
    });

    it('should have a toolbar', () => {
        expect(toolbarElement).not.toBeNull();
    });

    it('should pass data to the toolbar', () => {
        fixture.detectChanges();
        expect(toolbarComponent.pageState.page).toEqual(mockDotPage);
        expect(toolbarComponent.pageState.state).toEqual(mockDotPageState);
    });

    it('should redirect to site browser on toolbar cancel', () => {
        spyOn(dotRouterService, 'goToSiteBrowser');
        toolbarElement.triggerEventHandler('cancel', {});

        expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
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

    it('should reload when toolbar emit actionFired event', () => {
        spyOn(component, 'reload');
        toolbarElement.triggerEventHandler('actionFired', '');
        expect(component.reload).toHaveBeenCalledTimes(1);
    });

    describe('what\'s change', () => {
        let viewAsToolbar: DebugElement;

        beforeEach(() => {
            viewAsToolbar = fixture.debugElement.query(By.css('dot-edit-content-view-as-toolbar'));
            component.pageState = new DotRenderedPageState(mockUser, mockDotRenderedPage, null);
        });

        it('should not show by default', () => {
            fixture.detectChanges();
            expect(de.query(By.css('dot-whats-changed'))).toBe(null);
            expect(component.showWhatsChanged).toBe(false);
        });

        describe('show', () => {
            beforeEach(() => {
                fixture.detectChanges();
                viewAsToolbar.triggerEventHandler('whatschange', true);
                fixture.detectChanges();
            });

            it('should show', () => {
                expect(de.query(By.css('dot-whats-changed'))).toBeTruthy();
                expect(component.showWhatsChanged).toBe(true);
            });

            it('should hide edit iframe', () => {
                const editIframe: DebugElement = de.query(By.css('.dot-edit__iframe'));
                expect(editIframe.styles).toEqual({
                    width: '',
                    height: '',
                    visibility: 'hidden',
                    position: 'absolute'
                });
            });
        });
    });

    describe('reload', () => {
        const mockRenderedPageState = new DotRenderedPageState(mockUser, mockDotRenderedPage);

        beforeEach(() => {
            component.pageState = null;
        });

        it('should reload', () => {
            expect(component.pageState).toBe(null);

            spyOn(dotPageStateService, 'get').and.returnValue(Observable.of(mockRenderedPageState));

            component.reload();

            expect(dotPageStateService.get).toHaveBeenCalledWith('an/url/fake');
            expect(component.pageState).toBe(mockRenderedPageState);
        });

        it('should handle error on reload', () => {
            const fake500Response = mockResponseView(500);
            spyOn(dotPageStateService, 'get').and.returnValue(Observable.throw(fake500Response));
            spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
            spyOn(dotRouterService, 'goToSiteBrowser');

            component.reload();

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(fake500Response);
            expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
        });
    });

    describe('set new view as configuration', () => {
        let viewAsToolbar: DebugElement;

        beforeEach(() => {
            viewAsToolbar = fixture.debugElement.query(By.css('dot-edit-content-view-as-toolbar'));
            component.pageState = new DotRenderedPageState(mockUser, mockDotRenderedPage, null);
        });

        it('should have a View As toolbar', () => {
            expect(viewAsToolbar).not.toBeNull();
        });

        it('should NOT set configuration skin for the content', () => {
            fixture.detectChanges();
            const pageWrapper: DebugElement = de.query(By.css('.dot-edit__page-wrapper'));
            expect(pageWrapper.classes['dot-edit__page-wrapper--deviced']).toBeFalsy();
        });

        it('should set configuration skin for the content', () => {
            component.pageState.viewAs.device = mockDotDevices[0];
            fixture.detectChanges();
            const pageWrapper: DebugElement = de.query(By.css('.dot-edit__page-wrapper'));

            expect(pageWrapper.classes['dot-edit__page-wrapper--deviced']).toBeTruthy();
        });

        it('should set the page wrapper dimensions based on device', () => {
            const pageWrapper: DebugElement = de.query(By.css('.dot-edit__page-wrapper'));
            const editIframe: DebugElement = de.query(By.css('.dot-edit__iframe'));
            component.pageState.viewAs.device = mockDotDevices[0];
            fixture.detectChanges();
            expect(editIframe.styles).toEqual({
                width: mockDotDevices[0].cssWidth + 'px',
                height: mockDotDevices[0].cssHeight + 'px',
                visibility: '',
                position: ''
            });
            expect(pageWrapper.nativeElement.classList.contains('dot-edit__page-wrapper--deviced')).toBe(true);
        });

        it('should change the Language/Persona of the page when viewAs configuration changes and set the dev', () => {
            spyOn(component, 'changeViewAsHandler').and.callThrough();
            spyOn(dotPageStateService, 'set').and.callThrough();
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
            expect(viewAsToolbar.componentInstance.pageState.viewAs).toEqual(mockDotRenderedPage.viewAs);
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
            const customMockDotRenderedPage = {
                ...mockDotRenderedPage,
                page: {
                    ...mockDotRenderedPage.page,
                    lockedBy: mockUser.userId,
                    canLock: true
                }
            };

            spyStateSet(new DotRenderedPageState(mockUser, customMockDotRenderedPage));
            fixture.detectChanges();

            toolbarComponent.changeState.emit({
                locked: true,
                mode: PageMode.EDIT
            });

            expect(component.statePageHandler).toHaveBeenCalledWith({
                locked: true,
                mode: PageMode.EDIT
            });
            expect(component.pageState.state).toEqual({
                mode: PageMode.EDIT,
                locked: true,
                lockedByAnotherUser: false
            });
            expect(component.pageState.page).toEqual(customMockDotRenderedPage.page);
            expect(dotEditContentHtmlService.initEditMode).toHaveBeenCalledWith('<html></html>', component.iframe);
        });

        it('should set preview mode', () => {
            spyStateSet(new DotRenderedPageState(mockUser, mockDotRenderedPage, PageMode.PREVIEW));

            fixture.detectChanges();

            toolbarComponent.changeState.emit({
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
                locked: true,
                lockedByAnotherUser: true
            });
            expect(dotEditContentHtmlService.initEditMode).not.toHaveBeenCalled();
            expect(dotEditContentHtmlService.renderPage).toHaveBeenCalledWith('<html></html>', component.iframe);
        });

        it('should set live mode', () => {
            spyStateSet(new DotRenderedPageState(mockUser, mockDotRenderedPage, PageMode.LIVE));
            fixture.detectChanges();

            toolbarComponent.changeState.emit({
                mode: PageMode.LIVE
            });

            expect(component.statePageHandler).toHaveBeenCalledWith({
                mode: PageMode.LIVE
            });

            expect(component.pageState.page).toEqual(mockDotPage);
            expect(component.pageState.state).toEqual({
                mode: PageMode.LIVE,
                locked: true,
                lockedByAnotherUser: true
            });
            expect(dotGlobalMessageService.display).not.toHaveBeenCalled();
            expect(dotEditContentHtmlService.initEditMode).not.toHaveBeenCalled();
            expect(dotEditContentHtmlService.renderPage).toHaveBeenCalledWith('<html></html>', component.iframe);
        });
    });

    describe('contentlets', () => {
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

            spyOn(dotEditContentHtmlService, 'contentletEvents$').and.returnValue(Observable.of(mockResEvent));
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
        beforeEach(() => {
            component.pageState = null;
            fixture.detectChanges();
        });

        it('should reload page', () => {
            spyOn(component, 'reload');

            siteServiceMock.setFakeCurrentSite(mockSites[1]);
            expect(component.reload).toHaveBeenCalledTimes(1);
        });

        it('should unsubscribe before destroy', () => {
            spyOn(dotPageStateService, 'get');

            fixture.detectChanges();
            fixture.componentInstance.ngOnDestroy();

            siteServiceMock.setFakeCurrentSite(mockSites[1]);
            expect(dotPageStateService.get).not.toHaveBeenCalled();
        });
    });

    describe('actions', () => {
        beforeEach(() => {
            spyOn(dotEditContentHtmlService, 'setContainterToAppendContentlet');
            fixture.detectChanges();
        });

        describe('add', () => {
            beforeEach(() => {
                spyOn(dotContentletEditorService, 'add').and.callThrough();

                dotEditContentHtmlService.iframeActions$.next({
                    name: 'add',
                    dataset: {
                        dotAdd: 'content',
                        dotIdentifier: '123',
                        dotUuid: '456'
                    }
                });

                fixture.detectChanges();
            });


            describe('content or widget', () => {
                beforeEach(() => {
                    dotEditContentHtmlService.iframeActions$.next({
                        name: 'add',
                        dataset: {
                            dotAdd: 'content',
                            dotIdentifier: '123',
                            dotUuid: '456'
                        }
                    });

                    fixture.detectChanges();
                });

                it('should have dot-add-contentlet', () => {
                    expect(de.query(By.css('dot-add-contentlet')) !== null).toBe(true);
                });

                it('should set container to add', () => {
                    expect(dotEditContentHtmlService.setContainterToAppendContentlet).toHaveBeenCalledWith({
                        identifier: '123',
                        uuid: '456'
                    });

                    expect(dotContentletEditorService.add).toHaveBeenCalledWith({
                        header: 'Content Search',
                        data: {
                            container: '123',
                            baseTypes: 'content'
                        },
                        events: {
                            load: jasmine.any(Function)
                        }
                    });
                });

                it('should bind contentlet events', () => {
                    const fakeEvent = {
                        target: {
                            contentWindow: {
                                ngEditContentletEvents: undefined
                            }
                        }
                    };
                    dotContentletEditorService.load(fakeEvent);
                    expect(fakeEvent.target.contentWindow.ngEditContentletEvents).toBeDefined();
                });
            });

            describe('form', () => {
                let dotFormSelector;

                beforeEach(() => {
                    dotEditContentHtmlService.iframeActions$.next({
                        name: 'add',
                        dataset: {
                            dotAdd: 'form',
                            dotIdentifier: '123',
                            dotUuid: '456'
                        }
                    });

                    fixture.detectChanges();

                    dotFormSelector = de.query(By.css('dot-form-selector'));
                });

                it('should show form-selector and set container to add', () => {
                    expect(dotEditContentHtmlService.setContainterToAppendContentlet).toHaveBeenCalledWith({
                        identifier: '123',
                        uuid: '456'
                    });

                    fixture.detectChanges();

                    expect(dotFormSelector.componentInstance.show).toBe(true);
                });


                it('select a form to add into the page', () => {
                    const mockContentType = {};

                    spyOn(dotEditContentHtmlService, 'renderAddedForm').and.callFake(() => {});

                    dotFormSelector.componentInstance.select.emit(mockContentType);

                    fixture.detectChanges();

                    expect(dotFormSelector.componentInstance.show).toBe(false);
                    expect(dotEditContentHtmlService.renderAddedForm).toHaveBeenCalledWith(mockContentType);
                });
            });
        });

        describe('edit', () => {
            beforeEach(() => {
                spyOn(dotContentletEditorService, 'edit').and.callThrough();

                dotEditContentHtmlService.iframeActions$.next({
                    name: 'edit',
                    container: {
                        dotIdentifier: '123',
                        dotUuid: '456'
                    },
                    dataset: {
                        dotInode: '999'
                    }
                });

                fixture.detectChanges();
            });

            it('should have dot-edit-contentlet', () => {
                expect(de.query(By.css('dot-edit-contentlet')) !== null).toBe(true);
            });

            it('should call edit service', () => {
                expect(dotContentletEditorService.edit).toHaveBeenCalledWith({
                    data: {
                        inode: '999'
                    },
                    events: {
                        load: jasmine.any(Function)
                    }
                });
            });

            it('should bind contentlet events', () => {
                const fakeEvent = {
                    target: {
                        contentWindow: {
                            ngEditContentletEvents: undefined
                        }
                    }
                };
                dotContentletEditorService.load(fakeEvent);
                expect(fakeEvent.target.contentWindow.ngEditContentletEvents).toBeDefined();
            });
        });

        describe('select', () => {
            it('should close dialog on select contentlet', () => {
                spyOn(dotContentletEditorService, 'clear').and.callThrough();

                dotEditContentHtmlService.iframeActions$.next({
                    name: 'select'
                });

                expect(dotContentletEditorService.clear).toHaveBeenCalledTimes(1);
            });
        });
    });

    describe('dialog configuration', () => {
        describe('page iframe', () => {
            let keypressFunction = null;
            let event;

            beforeEach(() => {
                event = {
                    target: {
                        contentDocument: {
                            body: {
                                innerHTML: ''
                            }
                        },
                        contentWindow: {
                            focus: jasmine.createSpy('focus'),
                            addEventListener: (_type, listener) => {
                                keypressFunction = listener;
                            }
                        }
                    }
                };
                spyOn(event.target.contentWindow, 'addEventListener').and.callThrough();
            });
        });

        describe('listen load-edit-mode-page event', () => {
            beforeEach(() => {
                route.parent.parent.data = Observable.of({
                    content: new DotRenderedPageState(mockUser, mockDotRenderedPage, PageMode.EDIT)
                });

                spyOn(dotRouterService, 'goToEditPage');
                spyOn(dotEditPageDataService, 'set');
                spyOn(dotEditContentHtmlService, 'renderPage');
            });

            it('should reload the current page', () => {
                fixture.detectChanges();

                const customEvent = document.createEvent('CustomEvent');
                customEvent.initCustomEvent('ng-event', false, false, {
                    name: 'load-edit-mode-page',
                    data: {
                        ...mockDotRenderedPage,
                        page: {
                            ...mockDotRenderedPage.page,
                            pageURI: 'an/url/fake'
                        }
                    }
                });
                document.dispatchEvent(customEvent);

                expect(dotEditPageDataService.set).not.toHaveBeenCalled();
                expect(dotRouterService.goToEditPage).not.toHaveBeenCalled();
                expect(component.pageState.page).toEqual({
                    ...mockDotRenderedPage.page,
                    pageURI: 'an/url/fake'
                });
                expect(dotEditContentHtmlService.renderPage).toHaveBeenCalled();
            });

            it('should go to edit-page and set data for the resolver', () => {
                fixture.detectChanges();

                const customEvent = document.createEvent('CustomEvent');
                customEvent.initCustomEvent('ng-event', false, false, {
                    name: 'load-edit-mode-page',
                    data: mockDotRenderedPage
                });
                document.dispatchEvent(customEvent);

                expect(dotEditPageDataService.set).toHaveBeenCalledWith(
                    new DotRenderedPageState(mockUser, mockDotRenderedPage, PageMode.EDIT)
                );

                expect(dotRouterService.goToEditPage).toHaveBeenCalledWith(mockDotRenderedPage.page.pageURI);
            });

            it('unsubcribe before destroy', () => {
                fixture.detectChanges();
                fixture.componentInstance.ngOnDestroy();

                const customEvent = document.createEvent('CustomEvent');
                customEvent.initCustomEvent('ng-event', false, false, {
                    name: 'load-edit-mode-page',
                    data: mockDotRenderedPage
                });
                document.dispatchEvent(customEvent);

                expect(dotEditPageDataService.set).not.toHaveBeenCalled();
                expect(dotRouterService.goToEditPage).not.toHaveBeenCalled();
            });
        });
    });

    describe('Auto save', () => {
        it('should call the save endpoint after a model change happens', () => {
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

            const model: DotPageContainer[] = [
                {
                    identifier: '1',
                    uuid: '2',
                    contentletsId: ['3', '4']
                }
            ];

            const newModel: DotPageContainer[] = [
                {
                    identifier: '2',
                    uuid: '3',
                    contentletsId: ['4', '5']
                }
            ];

            let dotEditPageService: DotEditPageService;
            dotEditPageService = de.injector.get(DotEditPageService);

            spyOn(dotEditPageService, 'save').and.returnValue(Observable.of(true));
            spyOn(dotEditContentHtmlService, 'getContentModel').and.returnValue({});
            spyOn(dotEditContentHtmlService, 'setContaintersSameHeight');

            fixture.detectChanges();
            dotEditContentHtmlService.pageModel$.next(model);
            dotEditContentHtmlService.pageModel$.next(newModel);
            expect(dotEditPageService.save).toHaveBeenCalledTimes(2);
            expect(dotEditContentHtmlService.setContaintersSameHeight).toHaveBeenCalledTimes(2);
        });

        it('should not execute setContaintersSameHeight() when layout is null', () => {

            route.parent.parent.data = Observable.of({
                content: {
                    ...mockDotRenderedPage,
                    layout: null,
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

            const model: DotPageContainer[] = [
                {
                    identifier: '1',
                    uuid: '2',
                    contentletsId: ['3', '4']
                }
            ];

            spyOn(dotEditContentHtmlService, 'setContaintersSameHeight');

            fixture.detectChanges();
            dotEditContentHtmlService.pageModel$.next(model);
            expect(dotEditContentHtmlService.setContaintersSameHeight).not.toHaveBeenCalled();
        });
    });

    it('should set listener to change containers height', () => {
        spyOn(dotEditContentHtmlService, 'setContaintersChangeHeightListener');
        fixture.detectChanges();
        expect(dotEditContentHtmlService.setContaintersChangeHeightListener).not.toHaveBeenCalled();
        component.pageState.state.mode = PageMode.EDIT;
        component.onLoad(Event);
        expect(dotEditContentHtmlService.setContaintersChangeHeightListener).toHaveBeenCalledWith(component.pageState.layout);
    });

});
