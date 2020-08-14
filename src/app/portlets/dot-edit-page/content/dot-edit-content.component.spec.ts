import { of } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ComponentFixture, tick, fakeAsync, TestBed } from '@angular/core/testing';
import { Component, DebugElement, EventEmitter, Input, Output, ElementRef } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { DialogModule, ButtonModule, ConfirmationService } from 'primeng/primeng';
import {
    ApiRoot,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    SiteService,
    StringUtils,
    UserModel
} from 'dotcms-js';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/index';
import { DotEditContentHtmlService } from './services/dot-edit-content-html/dot-edit-content-html.service';
import { DotEditPageService } from '@services/dot-edit-page/dot-edit-page.service';
import { DotEditPageToolbarModule } from './components/dot-edit-page-toolbar/dot-edit-page-toolbar.module';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotLoadingIndicatorModule } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { DotPageStateService } from './services/dot-page-state/dot-page-state.service';
import { DotWorkflowService } from '@services/dot-workflow/dot-workflow.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotPageRender } from '@portlets/dot-edit-page/shared/models/dot-rendered-page.model';
import { DotEditContentComponent } from './dot-edit-content.component';
import { DotCMSContentType } from 'dotcms-models';
import { DotContentletEditorModule } from '@components/dot-contentlet-editor/dot-contentlet-editor.module';
import { DotEditPageInfoModule } from '../components/dot-edit-page-info/dot-edit-page-info.module';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import * as _ from 'lodash';
import { DotEditPageWorkflowsActionsModule } from './components/dot-edit-page-workflows-actions/dot-edit-page-workflows-actions.module';
import { DotOverlayMaskModule } from '@components/_common/dot-overlay-mask/dot-overlay-mask.module';
import { DotContentletLockerService } from '@services/dot-contentlet-locker/dot-contentlet-locker.service';
import { DotPageRenderService } from '@services/dot-page-render/dot-page-render.service';
import { DotContainerContentletService } from './services/dot-container-contentlet.service';
import { DotDragDropAPIHtmlService } from './services/html/dot-drag-drop-api-html.service';
import { DotDOMHtmlUtilService } from './services/html/dot-dom-html-util.service';
import { DotEditContentToolbarHtmlService } from './services/html/dot-edit-content-toolbar-html.service';

import { SiteServiceMock } from '@tests/site-service.mock';
import { LoginServiceMock, mockUser } from '@tests/login-service.mock';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotWorkflowServiceMock } from '@tests/dot-workflow-service.mock';
import { mockDotRenderedPage, mockDotLayout } from '@tests/dot-page-render.mock';
import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { DotLoadingIndicatorService } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { DotPageMode, DotPageContainer, DotPageContent } from '../shared/models';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { dotcmsContentletMock } from '@tests/dotcms-contentlet.mock';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotWizardModule } from '@components/_common/dot-wizard/dot-wizard.module';
import { CoreWebServiceMock } from '../../../../../projects/dotcms-js/src/lib/core/core-web.service.mock';
import { BaseRequestOptions, ConnectionBackend, Http, RequestOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '@tests/dot-test-bed';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotLicenseService } from '@services/dot-license/dot-license.service';

@Component({
    selector: 'dot-global-message',
    template: ''
})
class MockGlobalMessageComponent {}

@Component({
    selector: 'dot-test',
    template: '<dot-edit-content></dot-edit-content>'
})
class HostTestComponent {}

@Component({
    selector: 'dot-whats-changed',
    template: ''
})
class MockDotWhatsChangedComponent {
    @Input() pageId: string;
    @Input() languageId: string;
}

@Component({
    selector: 'dot-form-selector',
    template: ''
})
export class MockDotFormSelectorComponent {
    @Input() show = false;
    @Output() select = new EventEmitter<DotCMSContentType>();
    @Output() close = new EventEmitter<any>();
}

const mockRenderedPageState = new DotPageRenderState(
    mockUser,
    new DotPageRender(mockDotRenderedPage)
);

describe('DotEditContentComponent', () => {
    const siteServiceMock = new SiteServiceMock();
    let component: DotEditContentComponent;
    let de: DebugElement;
    let dotEditContentHtmlService: DotEditContentHtmlService;
    let dotGlobalMessageService: DotGlobalMessageService;
    let dotPageStateService: DotPageStateService;
    let dotRouterService: DotRouterService;
    let fixture: ComponentFixture<DotEditContentComponent>;
    let route: ActivatedRoute;
    let dotUiColorsService: DotUiColorsService;
    let dotEditPageService: DotEditPageService;
    let iframeOverlayService: IframeOverlayService;
    let dotLoadingIndicatorService: DotLoadingIndicatorService;
    let dotContentletEditorService: DotContentletEditorService;
    let dotDialogService: DotAlertConfirmService;
    let dotCustomEventHandlerService: DotCustomEventHandlerService;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'dot.common.cancel': 'CANCEL',
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
            'an-unexpected-system-error-occurred': 'Error msg',
            'editpage.content.contentlet.remove.confirmation_message.header': 'header',
            'editpage.content.contentlet.remove.confirmation_message.message': 'message'
        });

        TestBed.configureTestingModule({
            declarations: [
                DotEditContentComponent,
                MockDotWhatsChangedComponent,
                MockDotFormSelectorComponent,
                HostTestComponent,
                MockGlobalMessageComponent
            ],
            imports: [
                BrowserAnimationsModule,
                ButtonModule,
                DialogModule,
                DotContentletEditorModule,
                DotEditPageToolbarModule,
                DotEditPageInfoModule,
                DotLoadingIndicatorModule,
                DotEditPageWorkflowsActionsModule,
                DotOverlayMaskModule,
                DotWizardModule,
                RouterTestingModule.withRoutes([
                    {
                        component: DotEditContentComponent,
                        path: 'test'
                    }
                ])
            ],
            providers: [
                DotContentletLockerService,
                DotPageRenderService,
                DotContainerContentletService,
                DotDragDropAPIHtmlService,
                DotEditContentToolbarHtmlService,
                DotDOMHtmlUtilService,
                DotAlertConfirmService,
                DotEditContentHtmlService,
                DotEditPageService,
                DotGlobalMessageService,
                DotPageStateService,
                DotCustomEventHandlerService,
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
                                data: of({
                                    content: mockRenderedPageState
                                })
                            }
                        },
                        snapshot: {
                            queryParams: {
                                url: '/an/url/test'
                            }
                        }
                    }
                },
                DotMessageDisplayService,
                ConfirmationService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                Http,
                { provide: ConnectionBackend, useClass: MockBackend },
                { provide: RequestOptions, useClass: BaseRequestOptions },
                DotEventsService,
                DotHttpErrorManagerService,
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                DotIframeService,
                DotDownloadBundleDialogService,
                DotLicenseService,
                DotcmsEventsService,
                DotEventsSocket,
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotcmsConfigService,
                LoggerService,
                StringUtils,
                ApiRoot,
                UserModel
            ]
        });

        fixture = TestBed.createComponent(DotEditContentComponent);

        component = fixture.componentInstance;
        de = fixture.debugElement;

        dotEditContentHtmlService = de.injector.get(DotEditContentHtmlService);
        dotGlobalMessageService = de.injector.get(DotGlobalMessageService);
        dotUiColorsService = de.injector.get(DotUiColorsService);
        dotPageStateService = de.injector.get(DotPageStateService);
        dotRouterService = de.injector.get(DotRouterService);
        route = de.injector.get(ActivatedRoute);
        dotEditPageService = de.injector.get(DotEditPageService);
        iframeOverlayService = de.injector.get(IframeOverlayService);
        dotLoadingIndicatorService = de.injector.get(DotLoadingIndicatorService);
        dotContentletEditorService = de.injector.get(DotContentletEditorService);
        dotDialogService = de.injector.get(DotAlertConfirmService);
        dotCustomEventHandlerService = de.injector.get(DotCustomEventHandlerService);

        spyOn(dotPageStateService, 'reload');

        spyOn(dotEditContentHtmlService, 'renderAddedForm').and.returnValue(
            of([{ identifier: '123', uuid: 'uui-1' }])
        );
        spyOn(dotEditPageService, 'save').and.returnValue(of({}));
    });

    describe('elements', () => {
        describe('dot-form-selector', () => {
            let dotFormSelector: DebugElement;

            beforeEach(() => {
                spyOn(dotGlobalMessageService, 'success');

                fixture.detectChanges();
                dotFormSelector = de.query(By.css('dot-form-selector'));
            });

            it('should have', () => {
                expect(dotFormSelector).not.toBeNull();
                expect(dotFormSelector.componentInstance.show).toBe(false);
            });

            describe('events', () => {
                it('select > should add form', () => {
                    dotFormSelector.triggerEventHandler('select', {
                        baseType: 'string',
                        clazz: 'string'
                    });
                    fixture.detectChanges();

                    expect(dotEditContentHtmlService.renderAddedForm).toHaveBeenCalledWith({
                        baseType: 'string',
                        clazz: 'string'
                    });

                    expect(dotEditPageService.save).toHaveBeenCalledWith('123', [
                        { identifier: '123', uuid: 'uui-1' }
                    ]);

                    expect(dotGlobalMessageService.success).toHaveBeenCalledTimes(1);
                    expect(dotPageStateService.reload).toHaveBeenCalledTimes(1);
                    expect(dotFormSelector.componentInstance.show).toBe(false);
                });

                it('close > should close form', () => {
                    component.editForm = true;
                    dotFormSelector.triggerEventHandler('close', {});
                    expect(component.editForm).toBe(false);
                });
            });
        });

        describe('dot-edit-page-toolbar', () => {
            let toolbarElement: DebugElement;

            beforeEach(() => {
                fixture.detectChanges();
                toolbarElement = de.query(By.css('dot-edit-page-toolbar'));
            });

            it('should have', () => {
                expect(toolbarElement).not.toBeNull();
            });

            it('should pass pageState', () => {
                expect(toolbarElement.componentInstance.pageState).toEqual(mockRenderedPageState);
            });

            describe('events', () => {
                it('cancel > should go to site browser', () => {
                    toolbarElement.triggerEventHandler('cancel', {});
                    expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
                });

                it('actionFired > should reload', () => {
                    toolbarElement.triggerEventHandler('actionFired', {});
                    expect(dotPageStateService.reload).toHaveBeenCalledTimes(1);
                });

                it('whatschange > should show dot-whats-changed', () => {
                    let whatschange = de.query(By.css('dot-whats-changed'));
                    expect(whatschange).toBeNull();
                    toolbarElement.triggerEventHandler('whatschange', true);
                    fixture.detectChanges();
                    whatschange = de.query(By.css('dot-whats-changed'));
                    expect(whatschange).not.toBeNull();
                });
            });
        });

        describe('dot-add-contentlet', () => {
            let dotAddContentlet;

            beforeEach(() => {
                fixture.detectChanges();
                dotAddContentlet = de.query(By.css('dot-add-contentlet'));
            });

            it('should have', () => {
                expect(dotAddContentlet).not.toBeNull();
            });
        });

        describe('dot-edit-contentlet', () => {
            let dotEditContentlet;

            beforeEach(() => {
                fixture.detectChanges();
                dotEditContentlet = de.query(By.css('dot-edit-contentlet'));
            });

            it('should have', () => {
                expect(dotEditContentlet).not.toBeNull();
            });

            it('should call dotCustomEventHandlerService on customEvent', () => {
                spyOn(dotCustomEventHandlerService, 'handle');
                dotEditContentlet.triggerEventHandler('custom', { data: 'test' });

                expect(dotCustomEventHandlerService.handle).toHaveBeenCalledWith({ data: 'test' });
            });
        });

        describe('dot-reorder-menu', () => {
            let dotReorderMenu;

            beforeEach(() => {
                fixture.detectChanges();
                dotReorderMenu = de.query(By.css('dot-reorder-menu'));
            });

            it('should have', () => {
                expect(dotReorderMenu).not.toBeNull();
            });

            xdescribe('events', () => {
                component.reorderMenuUrl = 'some/test';
                dotReorderMenu.triggerEventHandler('close', {});
                expect(component.reorderMenuUrl).toBe('xxx');
            });
        });

        describe('dot-loading-indicator', () => {
            let dotLoadingIndicator;

            beforeEach(() => {
                fixture.detectChanges();
                dotLoadingIndicator = de.query(By.css('dot-loading-indicator'));
            });

            it('should have', () => {
                expect(dotLoadingIndicator).not.toBeNull();
                expect(dotLoadingIndicator.attributes.fullscreen).toBe('true');
            });
        });

        describe('iframe wrappers', () => {
            it('should show all elements nested correctly', () => {
                fixture.detectChanges();
                const wrapper = de.query(By.css('.dot-edit__page-wrapper'));
                const deviceWrapper = wrapper.query(By.css('.dot-edit__device-wrapper'));
                const iframeWrapper = deviceWrapper.query(By.css('.dot-edit__iframe-wrapper'));

                expect(wrapper).not.toBeNull();
                expect(wrapper.classes['dot-edit__page-wrapper--deviced']).toBe(false);

                expect(deviceWrapper).not.toBeNull();
                expect(iframeWrapper).not.toBeNull();
            });

            describe('with device selected', () => {
                beforeEach(() => {
                    route.parent.parent.data = of({
                        content: new DotPageRenderState(
                            mockUser,
                            new DotPageRender({
                                ...mockDotRenderedPage,
                                viewAs: {
                                    ...mockDotRenderedPage.viewAs,
                                    device: {
                                        cssHeight: '100',
                                        cssWidth: '100',
                                        name: 'Watch',
                                        inode: '1234'
                                    }
                                }
                            })
                        )
                    });
                    fixture.detectChanges();
                });

                it('should add "deviced" class to main wrapper', () => {
                    const wrapper = de.query(By.css('.dot-edit__page-wrapper'));
                    expect(wrapper.classes['dot-edit__page-wrapper--deviced']).toBe(true);
                });

                it('should add inline styles to iframe', done => {
                    setTimeout(() => {
                        fixture.detectChanges();
                        const iframeEl = de.query(By.css('iframe.dot-edit__iframe'));
                        expect(iframeEl.styles).toEqual({
                            height: '100px',
                            position: '',
                            visibility: '',
                            width: '100px'
                        });
                        done();
                    }, 0);
                });
            });
        });

        describe('iframe', () => {
            function detectChangesForIframeRender(fix) {
                fix.detectChanges();
                tick(1);
                fix.detectChanges();
                tick(10);
            }

            function getIframe() {
                return de.query(
                    By.css(
                        '.dot-edit__page-wrapper .dot-edit__device-wrapper .dot-edit__iframe-wrapper iframe.dot-edit__iframe'
                    )
                );
            }

            function triggerIframeCustomEvent(detail) {
                const event = new CustomEvent('ng-event', {
                    detail
                });
                window.document.dispatchEvent(event);
            }

            it(
                'should show',
                fakeAsync(() => {
                    detectChangesForIframeRender(fixture);
                    const iframeEl = getIframe();
                    expect(iframeEl).not.toBeNull();
                })
            );

            it(
                'should have attr setted',
                fakeAsync(() => {
                    detectChangesForIframeRender(fixture);
                    const iframeEl = getIframe();
                    expect(iframeEl.attributes.class).toBe('dot-edit__iframe');
                    expect(iframeEl.attributes.frameborder).toBe('0');
                    expect(iframeEl.attributes.height).toBe('100%');
                    expect(iframeEl.attributes.width).toBe('100%');
                })
            );

            describe('render html ', () => {
                beforeEach(() => {
                    spyOn(dotEditContentHtmlService, 'renderPage');
                    spyOn(dotEditContentHtmlService, 'initEditMode');
                });

                it(
                    'should render in preview mode',
                    fakeAsync(() => {
                        detectChangesForIframeRender(fixture);

                        expect(dotEditContentHtmlService.renderPage).toHaveBeenCalledWith(
                            mockRenderedPageState,
                            jasmine.any(ElementRef)
                        );
                        expect(dotEditContentHtmlService.initEditMode).not.toHaveBeenCalled();
                    })
                );

                it(
                    'should render in edit mode',
                    fakeAsync(() => {
                        const state = new DotPageRenderState(
                            mockUser,
                            new DotPageRender({
                                ...mockDotRenderedPage,
                                page: {
                                    ...mockDotRenderedPage.page,
                                    lockedBy: null
                                },
                                viewAs: {
                                    mode: DotPageMode.EDIT
                                }
                            })
                        );
                        route.parent.parent.data = of({
                            content: state
                        });
                        detectChangesForIframeRender(fixture);

                        expect(dotEditContentHtmlService.initEditMode).toHaveBeenCalledWith(
                            state,
                            jasmine.any(ElementRef)
                        );
                        expect(dotEditContentHtmlService.renderPage).not.toHaveBeenCalled();
                    })
                );
            });

            describe('events', () => {
                beforeEach(() => {
                    route.parent.parent.data = of({
                        content: new DotPageRenderState(
                            mockUser,
                            new DotPageRender({
                                ...mockDotRenderedPage,
                                viewAs: {
                                    mode: DotPageMode.EDIT
                                }
                            })
                        )
                    });
                });

                it(
                    'should handle load',
                    fakeAsync(() => {
                        spyOn(dotLoadingIndicatorService, 'hide');
                        spyOn(dotUiColorsService, 'setColors');
                        spyOn(dotEditContentHtmlService, 'setContaintersChangeHeightListener');
                        detectChangesForIframeRender(fixture);

                        expect(dotLoadingIndicatorService.hide).toHaveBeenCalled();
                        expect(dotUiColorsService.setColors).toHaveBeenCalled();
                        expect(
                            dotEditContentHtmlService.setContaintersChangeHeightListener
                        ).toHaveBeenCalledWith(jasmine.objectContaining(mockDotLayout));
                    })
                );

                describe('custom', () => {
                    it(
                        'should handle remote-render-edit',
                        fakeAsync(() => {
                            detectChangesForIframeRender(fixture);

                            triggerIframeCustomEvent({
                                name: 'remote-render-edit',
                                data: {
                                    pathname: '/url/from/event'
                                }
                            });

                            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                                url: 'url/from/event'
                            });
                        })
                    );

                    it(
                        'should handle in-iframe',
                        fakeAsync(() => {
                            detectChangesForIframeRender(fixture);

                            triggerIframeCustomEvent({
                                name: 'in-iframe'
                            });

                            expect(dotPageStateService.reload).toHaveBeenCalled();
                        })
                    );

                    it(
                        'should handle reorder-menu',
                        fakeAsync(() => {
                            detectChangesForIframeRender(fixture);

                            triggerIframeCustomEvent({
                                name: 'reorder-menu',
                                data: 'some/url/to/reorder/menu'
                            });

                            fixture.detectChanges();

                            const menu = de.query(By.css('dot-reorder-menu'));
                            expect(menu.componentInstance.url).toBe('some/url/to/reorder/menu');
                        })
                    );

                    it(
                        'should handle load-edit-mode-page to internal navigation',
                        fakeAsync(() => {
                            spyOn(dotPageStateService, 'setLocalState').and.callFake(() => {});
                            detectChangesForIframeRender(fixture);

                            triggerIframeCustomEvent({
                                name: 'load-edit-mode-page',
                                data: mockDotRenderedPage
                            });

                            fixture.detectChanges();

                            const dotRenderedPageStateExpected = new DotPageRenderState(
                                mockUser,
                                mockDotRenderedPage
                            );

                            expect(dotPageStateService.setLocalState).toHaveBeenCalledWith(
                                dotRenderedPageStateExpected
                            );
                        })
                    );

                    it(
                        'should handle load-edit-mode-page to internal navigation',
                        fakeAsync(() => {
                            spyOn(
                                dotPageStateService,
                                'setInternalNavigationState'
                            ).and.callFake(() => {});

                            detectChangesForIframeRender(fixture);

                            const mockDotRenderedPageCopy = _.cloneDeep(mockDotRenderedPage);
                            mockDotRenderedPageCopy.page.pageURI = '/another/url/test';

                            triggerIframeCustomEvent({
                                name: 'load-edit-mode-page',
                                data: mockDotRenderedPageCopy
                            });

                            fixture.detectChanges();

                            const dotRenderedPageStateExpected = new DotPageRenderState(
                                mockUser,
                                mockDotRenderedPageCopy
                            );

                            expect(
                                dotPageStateService.setInternalNavigationState
                            ).toHaveBeenCalledWith(dotRenderedPageStateExpected);
                            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                                url: mockDotRenderedPageCopy.page.pageURI
                            });
                        })
                    );

                    it(
                        'should handle save-menu-order',
                        fakeAsync(() => {
                            detectChangesForIframeRender(fixture);

                            triggerIframeCustomEvent({
                                name: 'save-menu-order'
                            });

                            fixture.detectChanges();

                            expect(dotPageStateService.reload).toHaveBeenCalled();

                            const menu = de.query(By.css('dot-reorder-menu'));
                            expect(menu.componentInstance.url).toBe('');
                        })
                    );

                    it(
                        'should handle error-saving-menu-order',
                        fakeAsync(() => {
                            spyOn(dotGlobalMessageService, 'error').and.callFake(() => {});

                            detectChangesForIframeRender(fixture);

                            triggerIframeCustomEvent({
                                name: 'error-saving-menu-order'
                            });

                            fixture.detectChanges();
                            dotGlobalMessageService.error('Error msg');

                            const menu = de.query(By.css('dot-reorder-menu'));
                            expect(menu.componentInstance.url).toBe('');
                        })
                    );

                    it(
                        'should handle cancel-save-menu-order',
                        fakeAsync(() => {
                            spyOn(dotGlobalMessageService, 'error').and.callFake(() => {});

                            detectChangesForIframeRender(fixture);

                            triggerIframeCustomEvent({
                                name: 'cancel-save-menu-order'
                            });

                            fixture.detectChanges();

                            const menu = de.query(By.css('dot-reorder-menu'));
                            expect(menu.componentInstance.url).toBe('');
                        })
                    );
                });

                describe('iframe events', () => {
                    it('should handle edit event', done => {
                        spyOn(dotContentletEditorService, 'edit').and.callFake(param => {
                            expect(param.data.inode).toBe('test_inode');

                            const event: any = {
                                target: {
                                    contentWindow: {}
                                }
                            };
                            param.events.load(event);
                            expect(event.target.contentWindow.ngEditContentletEvents).toBe(
                                dotEditContentHtmlService.contentletEvents$
                            );
                            done();
                        });

                        fixture.detectChanges();

                        dotEditContentHtmlService.iframeActions$.next({
                            name: 'edit',
                            dataset: {
                                dotInode: 'test_inode'
                            },
                            target: {
                                contentWindow: {
                                    ngEditContentletEvents: null
                                }
                            }
                        });
                    });

                    it('should handle code event', done => {
                        spyOn(dotContentletEditorService, 'edit').and.callFake(param => {
                            expect(param.data.inode).toBe('test_inode');

                            const event: any = {
                                target: {
                                    contentWindow: {}
                                }
                            };
                            param.events.load(event);
                            expect(event.target.contentWindow.ngEditContentletEvents).toBe(
                                dotEditContentHtmlService.contentletEvents$
                            );
                            done();
                        });

                        fixture.detectChanges();

                        dotEditContentHtmlService.iframeActions$.next({
                            name: 'code',
                            dataset: {
                                dotInode: 'test_inode'
                            },
                            target: {
                                contentWindow: {
                                    ngEditContentletEvents: null
                                }
                            }
                        });
                    });

                    it('should handle add form event', () => {
                        component.editForm = false;
                        spyOn(
                            dotEditContentHtmlService,
                            'setContainterToAppendContentlet'
                        ).and.callFake(() => {});

                        fixture.detectChanges();

                        dotEditContentHtmlService.iframeActions$.next({
                            name: 'add',
                            dataset: {
                                dotAdd: 'form',
                                dotIdentifier: 'identifier',
                                dotUuid: 'uuid'
                            }
                        });

                        const container: DotPageContainer = {
                            identifier: 'identifier',
                            uuid: 'uuid'
                        };

                        expect(
                            dotEditContentHtmlService.setContainterToAppendContentlet
                        ).toHaveBeenCalledWith(container);
                        expect(component.editForm).toBe(true);
                    });

                    it('should handle add content event', done => {
                        spyOn(
                            dotEditContentHtmlService,
                            'setContainterToAppendContentlet'
                        ).and.callFake(() => {});
                        spyOn(dotContentletEditorService, 'add').and.callFake(param => {
                            expect(param.data).toEqual({
                                container: 'identifier',
                                baseTypes: 'content'
                            });

                            expect(param.header).toEqual('Content Search');

                            const event: any = {
                                target: {
                                    contentWindow: {}
                                }
                            };
                            param.events.load(event);
                            expect(event.target.contentWindow.ngEditContentletEvents).toBe(
                                dotEditContentHtmlService.contentletEvents$
                            );
                            done();
                        });

                        fixture.detectChanges();

                        dotEditContentHtmlService.iframeActions$.next({
                            name: 'add',
                            dataset: {
                                dotAdd: 'content',
                                dotIdentifier: 'identifier',
                                dotUuid: 'uuid'
                            },
                            target: {
                                contentWindow: {
                                    ngEditContentletEvents: null
                                }
                            }
                        });

                        const container: DotPageContainer = {
                            identifier: 'identifier',
                            uuid: 'uuid'
                        };

                        expect(
                            dotEditContentHtmlService.setContainterToAppendContentlet
                        ).toHaveBeenCalledWith(container);
                    });

                    it('should handle remove event', done => {
                        spyOn(dotEditContentHtmlService, 'removeContentlet').and.callFake(() => {});
                        spyOn(dotDialogService, 'confirm').and.callFake(param => {
                            expect(param.header).toEqual('header');
                            expect(param.message).toEqual('message');

                            param.accept();

                            const pageContainer: DotPageContainer = {
                                identifier: 'container_identifier',
                                uuid: 'container_uuid'
                            };

                            const pageContent: DotPageContent = {
                                inode: 'test_inode',
                                identifier: 'test_identifier'
                            };
                            expect(dotEditContentHtmlService.removeContentlet).toHaveBeenCalledWith(
                                pageContainer,
                                pageContent
                            );
                            done();
                        });

                        fixture.detectChanges();

                        dotEditContentHtmlService.iframeActions$.next({
                            name: 'remove',
                            dataset: {
                                dotInode: 'test_inode',
                                dotIdentifier: 'test_identifier'
                            },
                            container: {
                                dotIdentifier: 'container_identifier',
                                dotUuid: 'container_uuid'
                            }
                        });
                    });

                    it('should handle select event', () => {
                        spyOn(dotContentletEditorService, 'clear').and.callFake(() => {});

                        fixture.detectChanges();

                        dotEditContentHtmlService.iframeActions$.next({
                            name: 'select'
                        });

                        expect(dotContentletEditorService.clear).toHaveBeenCalled();
                    });

                    it('should handle save event', () => {
                        fixture.detectChanges();

                        dotEditContentHtmlService.iframeActions$.next({
                            name: 'save'
                        });

                        expect(dotPageStateService.reload).toHaveBeenCalled();
                    });
                });
            });
        });

        describe('dot-overlay-mask', () => {
            it('should be hidden', () => {
                const dotOverlayMask = de.query(By.css('dot-overlay-mask'));
                expect(dotOverlayMask).toBeNull();
            });

            it('should show', () => {
                iframeOverlayService.show();
                fixture.detectChanges();
                const dotOverlayMask = de.query(
                    By.css('.dot-edit__iframe-wrapper dot-overlay-mask')
                );
                expect(dotOverlayMask).not.toBeNull();
            });
        });

        describe('dot-whats-changed', () => {
            it('should be hidden', () => {
                const dotWhatsChange = de.query(By.css('dot-whats-changed'));
                expect(dotWhatsChange).toBeNull();
            });

            it('should show', () => {
                fixture.detectChanges();
                const toolbarElement = de.query(By.css('dot-edit-page-toolbar'));
                toolbarElement.triggerEventHandler('whatschange', true);
                fixture.detectChanges();
                const dotWhatsChange = de.query(
                    By.css('.dot-edit__iframe-wrapper dot-whats-changed')
                );
                expect(dotWhatsChange).not.toBeNull();
            });
        });

        describe('personalized', () => {
            let dotFormSelector: DebugElement;

            beforeEach(() => {
                route.parent.parent.data = of({
                    content: new DotPageRenderState(
                        mockUser,
                        new DotPageRender({
                            ...mockDotRenderedPage,
                            viewAs: {
                                ...mockDotRenderedPage.viewAs,
                                persona: {
                                    ...dotcmsContentletMock,
                                    name: 'Super Persona',
                                    keyTag: 'SuperPersona',
                                    personalized: true
                                }
                            }
                        })
                    )
                });
                fixture.detectChanges();
                dotFormSelector = de.query(By.css('dot-form-selector'));
            });

            it('should save form', () => {
                dotFormSelector.triggerEventHandler('select', {
                    baseType: 'string',
                    clazz: 'string'
                });
                fixture.detectChanges();

                expect(dotEditContentHtmlService.renderAddedForm).toHaveBeenCalledWith({
                    baseType: 'string',
                    clazz: 'string'
                });

                expect(dotEditPageService.save).toHaveBeenCalledWith('123', [
                    { identifier: '123', uuid: 'uui-1', personaTag: 'SuperPersona' }
                ]);
            });
        });
    });
});
