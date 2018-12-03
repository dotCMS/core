import { throwError as observableThrowError, of as observableOf } from 'rxjs';
import { SiteServiceMock, mockSites } from './../../../test/site-service.mock';
import { ActivatedRoute } from '@angular/router';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { DialogModule } from 'primeng/primeng';
import { LoginService, SiteService } from 'dotcms-js';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotContainerContentletService } from './services/dot-container-contentlet.service';
import { DotContentletLockerService } from '@services/dot-contentlet-locker/dot-contentlet-locker.service';
import { DotDOMHtmlUtilService } from './services/html/dot-dom-html-util.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/index';
import { DotDragDropAPIHtmlService } from './services/html/dot-drag-drop-api-html.service';
import { DotEditContentHtmlService } from './services/dot-edit-content-html/dot-edit-content-html.service';
import { DotEditContentToolbarHtmlService } from './services/html/dot-edit-content-toolbar-html.service';
import { DotEditPageService } from '@services/dot-edit-page/dot-edit-page.service';
import { DotEditPageToolbarModule } from './components/dot-edit-page-toolbar/dot-edit-page-toolbar.module';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotLoadingIndicatorModule } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { DotMenuService } from '@services/dot-menu.service';
import { DotMessageService } from '@services/dot-messages-service';
import {
    DotPageState,
    DotRenderedPageState
} from '@portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { DotPageStateService } from './services/dot-page-state/dot-page-state.service';
import { DotRenderHTMLService } from '@services/dot-render-html/dot-render-html.service';
import { LoginServiceMock, mockUser } from '../../../test/login-service.mock';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { PageMode } from '@portlets/dot-edit-page/shared/models/page-mode.enum';
import { DotWorkflowService } from '@services/dot-workflow/dot-workflow.service';
import { DotWorkflowServiceMock } from '../../../test/dot-workflow-service.mock';
import { mockDotRenderedPage, mockDotPage } from '../../../test/dot-rendered-page.mock';
import { DotEditPageViewAs } from '@shared/models/dot-edit-page-view-as/dot-edit-page-view-as.model';
import { mockDotDevices } from '../../../test/dot-device.mock';
import { mockDotEditPageViewAs } from '../../../test/dot-edit-page-view-as.mock';
import { mockResponseView } from '../../../test/response-view.mock';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotEditPageDataService } from '@portlets/dot-edit-page/shared/services/dot-edit-page-resolver/dot-edit-page-data.service';
import { DotRenderedPage } from '@portlets/dot-edit-page/shared/models/dot-rendered-page.model';
import { DotEditPageToolbarComponent } from './components/dot-edit-page-toolbar/dot-edit-page-toolbar.component';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotPageContainer } from '@portlets/dot-edit-page/shared/models/dot-page-container.model';
import { DotEditContentComponent } from './dot-edit-content.component';
import { ContentType } from '../../content-types/shared/content-type.model';
import { DotContentletEditorModule } from '@components/dot-contentlet-editor/dot-contentlet-editor.module';
import { DotEditPageInfoModule } from '../components/dot-edit-page-info/dot-edit-page-info.module';
import { DotEditPageInfoComponent } from '../components/dot-edit-page-info/dot-edit-page-info.component';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import * as _ from 'lodash';

export const mockDotPageState: DotPageState = {
    mode: PageMode.PREVIEW,
    locked: false
};

@Component({
    selector: 'dot-test',
    template: '<dot-edit-content></dot-edit-content>'
})
class HostTestComponent {}

@Component({
    selector: 'dot-edit-content-view-as-toolbar',
    template: ''
})
class MockDotEditContentViewAsToolbarComponent {
    @Input()
    pageState: DotRenderedPageState;
    @Output()
    changeViewAs = new EventEmitter<DotEditPageViewAs>();
}

@Component({
    selector: 'dot-whats-changed',
    template: ''
})
class MockDotWhatsChangedComponent {
    @Input()
    pageId: string;
    @Input()
    languageId: string;
}

@Component({
    selector: 'dot-form-selector',
    template: ''
})
export class MockDotFormSelectorComponent {
    @Input()
    show = false;
    @Output()
    select = new EventEmitter<ContentType>();
    @Output()
    close = new EventEmitter<any>();
}

function waitForDetectChanges(fixture) {
    fixture.detectChanges();
    tick(1);
    fixture.detectChanges();
    tick(10);
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
    let dotUiColorsService: DotUiColorsService;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'dot.common.message.saving': 'Saving...',
            'dot.common.message.saved': 'Saved',
            'editpage.content.steal.lock.confirmation_message.header': 'Are you sure?',
            'editpage.content.steal.lock.confirmation_message.message':
                'This page is locked by bla bla',
            'editpage.content.steal.lock.confirmation_message.reject': 'Lock',
            'editpage.content.steal.lock.confirmation_message.accept': 'Cancel',
            'editpage.content.save.changes.confirmation.header': 'Save header',
            'editpage.content.save.changes.confirmation.message': 'Save message',
            'dot.common.content.search': 'Content Search',
            'an-unexpected-system-error-occurred': 'Error msg'
        });

        DOTTestBed.configureTestingModule({
            declarations: [
                DotEditContentComponent,
                MockDotEditContentViewAsToolbarComponent,
                MockDotWhatsChangedComponent,
                MockDotFormSelectorComponent,
                HostTestComponent
            ],
            imports: [
                BrowserAnimationsModule,
                DialogModule,
                DotContentletEditorModule,
                DotEditPageToolbarModule,
                DotEditPageInfoModule,
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
                                data: observableOf({
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
        dotUiColorsService = de.injector.get(DotUiColorsService);
        dotPageStateService = de.injector.get(DotPageStateService);
        dotRouterService = de.injector.get(DotRouterService);
        toolbarElement = de.query(By.css('dot-edit-page-toolbar'));
        toolbarComponent = toolbarElement.componentInstance;
        route = de.injector.get(ActivatedRoute);
    });

    it('should have a toolbar', () => {
        expect(toolbarElement).not.toBeNull();
    });

    it(
        'should pass data to the toolbar',
        fakeAsync(() => {
            waitForDetectChanges(fixture);
            expect(toolbarComponent.pageState.page).toEqual(mockDotPage);
            expect(toolbarComponent.pageState.state).toEqual(mockDotPageState);
        })
    );

    it(
        'should have page information',
        fakeAsync(() => {
            waitForDetectChanges(fixture);
            const pageInfo: DotEditPageInfoComponent = de.query(By.css('dot-edit-page-info'))
                .componentInstance;
            expect(pageInfo !== null).toBe(true);
            expect(pageInfo.pageState.page).toEqual(mockDotPage);
            expect(pageInfo.pageState.state).toEqual(mockDotPageState);
        })
    );

    it('should redirect to site browser on toolbar cancel', () => {
        spyOn(dotRouterService, 'goToSiteBrowser');
        toolbarElement.triggerEventHandler('cancel', {});

        expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
    });

    it('should have loading indicator', () => {
        const loadingIndicator: DebugElement = de.query(By.css('dot-loading-indicator'));
        expect(loadingIndicator).not.toBeNull();
    });

    it(
        'should have iframe',
        fakeAsync(() => {
            waitForDetectChanges(fixture);

            const iframe: DebugElement = de.query(By.css('.dot-edit__iframe'));
            expect(iframe).not.toBeNull();
        })
    );

    xit('should check isModelUpdated', () => {});

    it(
        'should show dotLoadingIndicatorService on init',
        fakeAsync(() => {
            const spyLoadingIndicator = spyOn(component.dotLoadingIndicatorService, 'show');

            waitForDetectChanges(fixture);

            expect(spyLoadingIndicator).toHaveBeenCalled();
        })
    );

    it(
        'should hide dotLoadingIndicatorService when the component loads',
        fakeAsync(() => {
            const spyLoadingIndicator = spyOn(component.dotLoadingIndicatorService, 'hide');

            waitForDetectChanges(fixture);

            const loadingIndicatorElem: DebugElement = de.query(By.css('dot-loading-indicator'));

            const iframe: DebugElement = de.query(By.css('.dot-edit__iframe'));
            iframe.triggerEventHandler('load', {
                target: {
                    contentWindow: {
                        document: {
                            querySelector: () => {}
                        }
                    }
                }
            });

            expect(loadingIndicatorElem).not.toBeNull();
            expect(spyLoadingIndicator).toHaveBeenCalled();
        })
    );

    it('should reload when toolbar emit actionFired event', () => {
        spyOn(component, 'reload');
        toolbarElement.triggerEventHandler('actionFired', '');
        expect(component.reload).toHaveBeenCalledTimes(1);
    });

    describe("what's change", () => {
        let viewAsToolbar: DebugElement;

        beforeEach(() => {
            viewAsToolbar = fixture.debugElement.query(By.css('dot-edit-content-view-as-toolbar'));
            component.pageState = new DotRenderedPageState(mockUser, mockDotRenderedPage);
        });

        it(
            'should not show by default',
            fakeAsync(() => {
                waitForDetectChanges(fixture);
                expect(de.query(By.css('dot-whats-changed'))).toBe(null);
                expect(component.showWhatsChanged).toBe(false);
            })
        );

        describe('show', () => {
            beforeEach(
                fakeAsync(() => {
                    waitForDetectChanges(fixture);
                    viewAsToolbar.triggerEventHandler('whatschange', true);
                    fixture.detectChanges();
                })
            );

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

            spyOn(dotPageStateService, 'get').and.returnValue(observableOf(mockRenderedPageState));

            component.reload();

            expect(dotPageStateService.get).toHaveBeenCalledWith('an/url/fake');
            expect(component.pageState).toBe(mockRenderedPageState);
        });

        it('should handle error on reload', () => {
            const fake500Response = mockResponseView(500);
            spyOn(dotPageStateService, 'get').and.returnValue(
                observableThrowError(fake500Response)
            );
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
            component.pageState = new DotRenderedPageState(mockUser, mockDotRenderedPage);
        });

        it('should have a View As toolbar', () => {
            expect(viewAsToolbar).not.toBeNull();
        });

        it(
            'should NOT set configuration skin for the content',
            fakeAsync(() => {
                waitForDetectChanges(fixture);
                const pageWrapper: DebugElement = de.query(By.css('.dot-edit__page-wrapper'));
                expect(pageWrapper.classes['dot-edit__page-wrapper--deviced']).toBeFalsy();
            })
        );

        it(
            'should set configuration skin for the content',
            fakeAsync(() => {
                component.pageState.viewAs.device = mockDotDevices[0];
                waitForDetectChanges(fixture);
                const pageWrapper: DebugElement = de.query(By.css('.dot-edit__page-wrapper'));

                expect(pageWrapper.classes['dot-edit__page-wrapper--deviced']).toBeTruthy();
            })
        );

        it(
            'should set the page wrapper dimensions based on device',
            fakeAsync(() => {
                component.pageState.viewAs.device = mockDotDevices[0];
                waitForDetectChanges(fixture);

                const pageWrapper: DebugElement = de.query(By.css('.dot-edit__page-wrapper'));
                const editIframe: DebugElement = de.query(By.css('.dot-edit__iframe'));

                expect(editIframe.styles).toEqual({
                    width: mockDotDevices[0].cssWidth + 'px',
                    height: mockDotDevices[0].cssHeight + 'px',
                    visibility: '',
                    position: ''
                });
                expect(
                    pageWrapper.nativeElement.classList.contains('dot-edit__page-wrapper--deviced')
                ).toBe(true);
            })
        );

        it('should change the Language/Persona of the page when viewAs configuration changes and set the dev', () => {
            spyOn(component, 'changeViewAsHandler').and.callThrough();
            spyOn(dotPageStateService, 'reload');

            viewAsToolbar.componentInstance.changeViewAs.emit(mockDotEditPageViewAs);

            expect(component.changeViewAsHandler).toHaveBeenCalledWith(mockDotEditPageViewAs);

            expect(dotPageStateService.reload).toHaveBeenCalledWith(
                {
                    url: 'an/url/fake',
                    mode: 2,
                    viewAs: {
                        persona_id: '1c56ba62-1f41-4b81-bd62-b6eacff3ad23',
                        language_id: 1,
                        device_inode: '1'
                    }
                }
            );
        });

        it(
            'should send the View As initial configuration to the toolbar',
            fakeAsync(() => {
                waitForDetectChanges(fixture);
                expect(viewAsToolbar.componentInstance.pageState.viewAs).toEqual(
                    mockDotRenderedPage.viewAs
                );
            })
        );
    });

    describe('set default page state', () => {
        beforeEach(() => {
            spyOn(dotEditContentHtmlService, 'renderPage');
            spyOn(dotEditContentHtmlService, 'initEditMode');
        });

        it(
            'should set page mode in preview',
            fakeAsync(() => {
                waitForDetectChanges(fixture);

                expect(dotEditContentHtmlService.renderPage).toHaveBeenCalledTimes(1);
                expect(dotEditContentHtmlService.initEditMode).not.toHaveBeenCalled();
            })
        );

        it(
            'should set page mode in edit',
            fakeAsync(() => {
                route.parent.parent.data = observableOf({
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
                waitForDetectChanges(fixture);

                expect(dotEditContentHtmlService.renderPage).not.toHaveBeenCalled();
                expect(dotEditContentHtmlService.initEditMode).toHaveBeenCalledTimes(1);
            })
        );

        it(
            'should set page mode in preview when the page is locked by another user',
            fakeAsync(() => {
                route.parent.parent.data = observableOf({
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
                waitForDetectChanges(fixture);

                const toolbar: DebugElement = de.query(By.css('.dot-edit__toolbar'));
                expect(toolbar.componentInstance.mode).toEqual(PageMode.PREVIEW);
                expect(dotEditContentHtmlService.renderPage).toHaveBeenCalledTimes(1);
                expect(dotEditContentHtmlService.initEditMode).not.toHaveBeenCalled();
            })
        );
    });

    describe('set page state when toolbar emit new state', () => {
        const spyStateSet = (val) => {
            spyOn(dotPageStateService, 'set').and.returnValue(observableOf(val));
        };

        beforeEach(() => {
            spyOn(component, 'statePageHandler').and.callThrough();
            spyOn(dotGlobalMessageService, 'display');
            spyOn(dotEditContentHtmlService, 'renderPage');
            spyOn(dotEditContentHtmlService, 'initEditMode');
        });

        it(
            'should set edit mode',
            fakeAsync(() => {
                const customMockDotRenderedPage = {
                    ...mockDotRenderedPage,
                    page: {
                        ...mockDotRenderedPage.page,
                        lockedBy: mockUser.userId,
                        canLock: true
                    },
                    viewAs: {
                        mode: 'EDIT_MODE'
                    }
                };

                spyStateSet(new DotRenderedPageState(mockUser, customMockDotRenderedPage));
                waitForDetectChanges(fixture);

                toolbarComponent.changeState.emit({
                    locked: true,
                    mode: PageMode.EDIT
                });

                tick(2);

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
                expect(dotEditContentHtmlService.initEditMode).toHaveBeenCalledWith(
                    component.pageState,
                    component.iframe
                );
            })
        );

        it(
            'should set preview mode',
            fakeAsync(() => {
                spyStateSet(new DotRenderedPageState(mockUser, mockDotRenderedPage));

                waitForDetectChanges(fixture);

                toolbarComponent.changeState.emit({
                    locked: true,
                    mode: PageMode.PREVIEW
                });

                tick(2);

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
                expect(dotEditContentHtmlService.renderPage).toHaveBeenCalledWith(
                    component.pageState,
                    component.iframe
                );
            })
        );

        it(
            'should set live mode',
            fakeAsync(() => {
                const mockDotRenderedPageCopy: DotRenderedPage = _.cloneDeep(mockDotRenderedPage);
                mockDotRenderedPageCopy.viewAs.mode = PageMode[PageMode.LIVE];

                spyStateSet(new DotRenderedPageState(mockUser, mockDotRenderedPageCopy));
                waitForDetectChanges(fixture);

                toolbarComponent.changeState.emit({
                    mode: PageMode.LIVE
                });

                tick(2);

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
                expect(dotEditContentHtmlService.renderPage).toHaveBeenCalledWith(
                    component.pageState,
                    component.iframe
                );
            })
        );
    });

    describe('contentlets', () => {
        it(
            'should display confirmation dialog and remove contentlet when user accepts',
            fakeAsync(() => {
                waitForDetectChanges(fixture);

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

                spyOn(dotEditContentHtmlService, 'contentletEvents$').and.returnValue(
                    observableOf(mockResEvent)
                );
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
            })
        );
    });

    describe('handle switch site', () => {
        beforeEach(
            fakeAsync(() => {
                component.pageState = null;
                waitForDetectChanges(fixture);
            })
        );

        it('should reload page', () => {
            spyOn(component, 'reload');

            siteServiceMock.setFakeCurrentSite(mockSites[1]);
            expect(component.reload).toHaveBeenCalledTimes(1);
        });

        it('should unsubscribe before destroy', () => {
            spyOn(dotPageStateService, 'get');

            fixture.detectChanges();
            component.ngOnDestroy();

            siteServiceMock.setFakeCurrentSite(mockSites[1]);
            expect(dotPageStateService.get).not.toHaveBeenCalled();
        });
    });

    describe('actions', () => {
        beforeEach(
            fakeAsync(() => {
                spyOn(dotEditContentHtmlService, 'setContainterToAppendContentlet');
                waitForDetectChanges(fixture);
            })
        );

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
                    expect(
                        dotEditContentHtmlService.setContainterToAppendContentlet
                    ).toHaveBeenCalledWith({
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
                    expect(
                        dotEditContentHtmlService.setContainterToAppendContentlet
                    ).toHaveBeenCalledWith({
                        identifier: '123',
                        uuid: '456'
                    });

                    fixture.detectChanges();

                    expect(dotFormSelector.componentInstance.show).toBe(true);
                });

                it(
                    'select a form to add into the page',
                    fakeAsync(() => {
                        const mockContentType = {};

                        spyOn(dotEditContentHtmlService, 'renderAddedForm').and.callFake(() =>
                            observableOf(null)
                        );

                        dotFormSelector.componentInstance.select.emit(mockContentType);

                        fixture.detectChanges();
                        tick(2);

                        expect(component.editForm).toBe(false);
                        expect(dotFormSelector.componentInstance.show).toBe(false);
                        expect(dotEditContentHtmlService.renderAddedForm).toHaveBeenCalledWith(
                            mockContentType
                        );
                    })
                );
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
                            addEventListener: (_type, _listener) => {}
                        }
                    }
                };
                spyOn(event.target.contentWindow, 'addEventListener').and.callThrough();
            });
        });

        describe('listen load-edit-mode-page event', () => {
            beforeEach(() => {
                route.parent.parent.data = observableOf({
                    content: new DotRenderedPageState(mockUser, mockDotRenderedPage)
                });

                spyOn(dotRouterService, 'goToEditPage');
                spyOn(dotEditPageDataService, 'set');
                spyOn(dotEditContentHtmlService, 'renderPage');
            });

            it(
                'should reload the current page',
                fakeAsync(() => {
                    waitForDetectChanges(fixture);

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

                    tick(2);

                    expect(dotEditPageDataService.set).not.toHaveBeenCalled();
                    expect(dotRouterService.goToEditPage).not.toHaveBeenCalled();
                    expect(component.pageState.page).toEqual({
                        ...mockDotRenderedPage.page,
                        pageURI: 'an/url/fake'
                    });
                    expect(dotEditContentHtmlService.renderPage).toHaveBeenCalled();
                })
            );

            it(
                'should go to edit-page and set data for the resolver',
                fakeAsync(() => {
                    const copyMockDotRenderedPage: DotRenderedPage = _.cloneDeep(
                        mockDotRenderedPage
                    );
                    copyMockDotRenderedPage.page.lockedBy = '123';

                    waitForDetectChanges(fixture);

                    const customEvent = document.createEvent('CustomEvent');
                    customEvent.initCustomEvent('ng-event', false, false, {
                        name: 'load-edit-mode-page',
                        data: copyMockDotRenderedPage
                    });
                    document.dispatchEvent(customEvent);

                    expect(dotEditPageDataService.set).toHaveBeenCalledWith(
                        new DotRenderedPageState(mockUser, copyMockDotRenderedPage)
                    );

                    expect(dotRouterService.goToEditPage).toHaveBeenCalledWith(
                        copyMockDotRenderedPage.page.pageURI
                    );
                })
            );

            it('unsubcribe before destroy', () => {
                fixture.detectChanges();
                component.ngOnDestroy();

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

        describe('listen reorder-menu event', () => {
            it('should set the reorder menu url', () => {
                fixture.detectChanges();

                const customEvent = document.createEvent('CustomEvent');
                customEvent.initCustomEvent('ng-event', false, false, {
                    name: 'reorder-menu',
                    data: 'testUrl'
                });
                document.dispatchEvent(customEvent);
                expect(component.reorderMenuUrl).toEqual('testUrl');
            });

            it('should clean the reorder menu url & reload iframe (Saved Menu)', () => {
                spyOn(component, 'reload');
                fixture.detectChanges();

                const customEvent = document.createEvent('CustomEvent');
                customEvent.initCustomEvent('ng-event', false, false, {
                    name: 'save-menu-order',
                    data: ''
                });
                document.dispatchEvent(customEvent);
                expect(component.reorderMenuUrl).toEqual('');
                expect(component.reload).toHaveBeenCalled();
            });

            it('should clean the reorder menu url & display error msg (Error saving Menu)', () => {
                spyOn(dotGlobalMessageService, 'display');
                fixture.detectChanges();

                const customEvent = document.createEvent('CustomEvent');
                customEvent.initCustomEvent('ng-event', false, false, {
                    name: 'error-saving-menu-order',
                    data: ''
                });
                document.dispatchEvent(customEvent);
                expect(component.reorderMenuUrl).toEqual('');
                expect(dotGlobalMessageService.display).toHaveBeenCalledWith('Error msg');
            });

            it('should clean the reorder menu url (Cancel custom event)', () => {
                fixture.detectChanges();
                const customEvent = document.createEvent('CustomEvent');
                customEvent.initCustomEvent('ng-event', false, false, {
                    name: 'cancel-save-menu-order',
                    data: ''
                });
                document.dispatchEvent(customEvent);
                expect(component.reorderMenuUrl).toEqual('');
            });

            it('should close menu dialog on close()', () => {
                const reorderMenuElement = de.query(By.css('dot-reorder-menu')).nativeElement;
                fixture.detectChanges();
                reorderMenuElement.dispatchEvent(new Event('close'));
                expect(component.reorderMenuUrl).toEqual('');
            });
        });
    });

    describe('Auto save', () => {
        it(
            'should call the save endpoint after a model change happens',
            fakeAsync(() => {
                route.parent.parent.data = observableOf({
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

                spyOn(dotEditPageService, 'save').and.returnValue(observableOf(true));
                spyOn(dotEditContentHtmlService, 'getContentModel').and.returnValue({});
                spyOn(dotEditContentHtmlService, 'setContaintersSameHeight');

                waitForDetectChanges(fixture);
                dotEditContentHtmlService.pageModel$.next(model);
                dotEditContentHtmlService.pageModel$.next(newModel);
                expect(dotEditPageService.save).toHaveBeenCalledTimes(2);
                expect(dotEditContentHtmlService.setContaintersSameHeight).toHaveBeenCalledTimes(2);
            })
        );

        it(
            'should not execute setContaintersSameHeight() when layout is null',
            fakeAsync(() => {
                route.parent.parent.data = observableOf({
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

                waitForDetectChanges(fixture);
                dotEditContentHtmlService.pageModel$.next(model);
                expect(dotEditContentHtmlService.setContaintersSameHeight).not.toHaveBeenCalled();
            })
        );
    });

    // TODO: Find The right way to test this by mocking the MutationObserver and spy that it was called with the right args
    it('should have correct mutation Observer config params', () => {
        const config = { attributes: false, childList: true, characterData: false };
        expect(dotEditContentHtmlService.mutationConfig).toEqual(config);
    });

    it(
        'should set listener to change containers height',
        fakeAsync(() => {
            spyOn(dotEditContentHtmlService, 'setContaintersChangeHeightListener');
            expect(
                dotEditContentHtmlService.setContaintersChangeHeightListener
            ).not.toHaveBeenCalled();

            waitForDetectChanges(fixture);
            component.pageState.state.mode = PageMode.EDIT;
            fixture.detectChanges();

            const iframe: DebugElement = de.query(By.css('.dot-edit__iframe'));
            iframe.triggerEventHandler('load', {
                currentTarget: {
                    contentDocument: {
                        body: {
                            innerHTML: 'html'
                        }
                    }
                }
            });

            expect(
                dotEditContentHtmlService.setContaintersChangeHeightListener
            ).toHaveBeenCalledWith(component.pageState.layout);
        })
    );

    xit(
        'should set colors on load',
        fakeAsync(() => {
            const fakeHtmlEl = {
                hello: 'world'
            };

            spyOn(dotUiColorsService, 'setColors');

            waitForDetectChanges(fixture);

            const iframe: DebugElement = de.query(By.css('.dot-edit__iframe'));
            iframe.triggerEventHandler('load', {
                target: {
                    contentWindow: {
                        document: {
                            querySelector: () => fakeHtmlEl
                        }
                    }
                }
            });

            expect(dotUiColorsService.setColors).toHaveBeenCalledWith(fakeHtmlEl);
        })
    );
});
