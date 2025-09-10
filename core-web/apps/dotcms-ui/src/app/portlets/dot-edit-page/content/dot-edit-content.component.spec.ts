/* eslint-disable @typescript-eslint/no-explicit-any */

import { mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, ElementRef, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, fakeAsync, flush, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DialogService } from 'primeng/dynamicdialog';

import {
    DotAlertConfirmService,
    DotContentletLockerService,
    DotContentTypeService,
    DotEditPageService,
    DotESContentService,
    DotEventsService,
    DotExperimentsService,
    DotFavoritePageService,
    DotGenerateSecurePasswordService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotLicenseService,
    DotMessageDisplayService,
    DotMessageService,
    DotPageRenderService,
    DotPageStateService,
    DotPropertiesService,
    DotRouterService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService,
    DotSessionStorageService,
    DotWorkflowActionsFireService,
    DotWorkflowService
} from '@dotcms/data-access';
import {
    ApiRoot,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    mockSites,
    SiteService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import {
    DEFAULT_VARIANT_NAME,
    DotCMSContentlet,
    DotCMSContentType,
    DotPageContainer,
    DotPageContent,
    DotPageMode,
    DotPageRender,
    DotPageRenderState,
    PageModelChangeEventType
} from '@dotcms/dotcms-models';
import { DotCopyContentModalService } from '@dotcms/ui';
import { DotLoadingIndicatorService } from '@dotcms/utils';
import {
    CoreWebServiceMock,
    dotcmsContentletMock,
    DotMessageDisplayServiceMock,
    DotWorkflowServiceMock,
    getExperimentMock,
    LoginServiceMock,
    mockDotLanguage,
    MockDotMessageService,
    mockDotRenderedPage,
    mockDotRenderedPageState,
    MockDotRouterService,
    mockUser,
    processedContainers,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { DotEditPageWorkflowsActionsModule } from './components/dot-edit-page-workflows-actions/dot-edit-page-workflows-actions.module';
import {
    DotEditContentComponent,
    EDIT_BLOCK_EDITOR_CUSTOM_EVENT
} from './dot-edit-content.component';
import { DotContainerContentletService } from './services/dot-container-contentlet.service';
import { DotEditContentHtmlService } from './services/dot-edit-content-html/dot-edit-content-html.service';
import { DotDOMHtmlUtilService } from './services/html/dot-dom-html-util.service';
import { DotDragDropAPIHtmlService } from './services/html/dot-drag-drop-api-html.service';
import { DotEditContentToolbarHtmlService } from './services/html/dot-edit-content-toolbar-html.service';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotDownloadBundleDialogService } from '../../../api/services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotUiColorsService } from '../../../api/services/dot-ui-colors/dot-ui-colors.service';
import { DotShowHideFeatureDirective } from '../../../shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '../../../test/dot-test-bed';
import { DotOverlayMaskModule } from '../../../view/components/_common/dot-overlay-mask/dot-overlay-mask.module';
import { DotWizardModule } from '../../../view/components/_common/dot-wizard/dot-wizard.module';
import { DotLoadingIndicatorModule } from '../../../view/components/_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { IframeOverlayService } from '../../../view/components/_common/iframe/service/iframe-overlay.service';
import { DotContentletEditorModule } from '../../../view/components/dot-contentlet-editor/dot-contentlet-editor.module';
import { DotContentletEditorService } from '../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotEditPageInfoModule } from '../components/dot-edit-page-info/dot-edit-page-info.module';
import { DotPaletteComponent } from '../components/dot-palette/dot-palette.component';

const EXPERIMENT_MOCK = getExperimentMock(1);

@Component({
    selector: 'dot-global-message',
    template: '',
    standalone: false
})
class MockGlobalMessageComponent {}

@Component({
    selector: 'dot-test',
    template: '<dot-edit-content></dot-edit-content>',
    standalone: false
})
class HostTestComponent {}

@Component({
    selector: 'dot-icon',
    template: '',
    standalone: false
})
class MockDotIconComponent {
    @Input() name: string;
}

@Component({
    selector: 'dot-whats-changed',
    template: '',
    standalone: false
})
class MockDotWhatsChangedComponent {
    @Input() pageId: string;
    @Input() languageId: string;
}

@Component({
    selector: 'dot-form-selector',
    template: '',
    standalone: false
})
export class MockDotFormSelectorComponent {
    @Input() show = false;
    @Output() pick = new EventEmitter<DotCMSContentType>();
    @Output() shutdown = new EventEmitter<any>();
}

@Component({
    selector: 'dot-edit-page-toolbar',
    template: '',
    standalone: false
})
export class MockDotEditPageToolbarComponent {
    @Input() pageState = mockDotRenderedPageState;
    @Input() variant;
    @Input() runningExperiment;
    @Output() actionFired = new EventEmitter<DotCMSContentlet>();
    @Output() cancel = new EventEmitter<boolean>();
    @Output() favoritePage = new EventEmitter<boolean>();
    @Output() whatschange = new EventEmitter<boolean>();
}

@Component({
    selector: 'dot-edit-page-toolbar-seo',
    template: '',
    standalone: false
})
export class MockDotEditPageToolbarSeoComponent {
    @Input() pageState = mockDotRenderedPageState;
    @Input() variant;
    @Input() runningExperiment;
    @Output() actionFired = new EventEmitter<DotCMSContentlet>();
    @Output() cancel = new EventEmitter<boolean>();
    @Output() favoritePage = new EventEmitter<boolean>();
    @Output() whatschange = new EventEmitter<boolean>();
}

@Component({
    selector: 'dot-palette',
    template: '',
    standalone: false
})
export class MockDotPaletteComponent {
    @Input() languageId = '1';
    @Input() allowedContent: string[];
}

const mockRenderedPageState = new DotPageRenderState(
    mockUser(),
    new DotPageRender(mockDotRenderedPage()),
    null,
    EXPERIMENT_MOCK
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
    let dialogService: DialogService;
    let dotDialogService: DotAlertConfirmService;
    let dotCustomEventHandlerService: DotCustomEventHandlerService;
    let dotConfigurationService: DotPropertiesService;
    let dotLicenseService: DotLicenseService;
    let dotEventsService: DotEventsService;
    let dotSessionStorageService: DotSessionStorageService;
    let siteService: SiteServiceMock;
    let router: Router;

    function detectChangesForIframeRender(fix) {
        fix.detectChanges();
        tick(1);
        fix.detectChanges();
        tick(10);
    }

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
            'editpage.content.contentlet.remove.confirmation_message.message': 'message',
            'Edit-Content': 'Edit Content'
        });

        TestBed.configureTestingModule({
            declarations: [
                DotEditContentComponent,
                MockDotWhatsChangedComponent,
                MockDotFormSelectorComponent,
                MockDotEditPageToolbarComponent,
                MockDotIconComponent,
                MockDotPaletteComponent,
                HostTestComponent,
                MockGlobalMessageComponent,
                MockDotEditPageToolbarSeoComponent
            ],
            imports: [
                HttpClientTestingModule,
                BrowserAnimationsModule,
                ButtonModule,
                DialogModule,
                DotContentletEditorModule,
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
                ]),
                DotShowHideFeatureDirective
            ],
            providers: [
                DotSessionStorageService,
                DialogService,
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
                DotWorkflowActionsFireService,
                DotGenerateSecurePasswordService,
                DotCustomEventHandlerService,
                DotPropertiesService,
                DotESContentService,
                DotSessionStorageService,
                DotCopyContentModalService,
                DotFavoritePageService,
                DotExperimentsService,
                DotSeoMetaTagsService,
                DotSeoMetaTagsUtilService,
                mockProvider(DotContentTypeService),
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
                                    content: mockRenderedPageState,
                                    experiment: EXPERIMENT_MOCK
                                })
                            }
                        },
                        snapshot: {
                            queryParams: {
                                url: '/an/url/test',
                                variantName: EXPERIMENT_MOCK.trafficProportion.variants[1].id,
                                mode: DotPageMode.PREVIEW
                            }
                        },
                        data: of({}),
                        queryParams: of({ language_id: '1' })
                    }
                },
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
                ConfirmationService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
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
        dialogService = de.injector.get(DialogService);
        dotDialogService = de.injector.get(DotAlertConfirmService);
        dotCustomEventHandlerService = de.injector.get(DotCustomEventHandlerService);
        dotConfigurationService = de.injector.get(DotPropertiesService);
        dotLicenseService = de.injector.get(DotLicenseService);
        dotEventsService = de.injector.get(DotEventsService);
        dotSessionStorageService = de.injector.get(DotSessionStorageService);
        siteService = TestBed.inject(SiteService) as unknown as SiteServiceMock;
        router = de.injector.get(Router);
        jest.spyOn(dotPageStateService, 'reload');

        jest.spyOn(dotEditContentHtmlService, 'renderAddedForm');

        jest.spyOn(component, 'reload');
    });

    describe('elements', () => {
        beforeEach(() => {
            jest.spyOn<any>(dotEditPageService, 'save').mockReturnValue(of({}));

            jest.spyOn(dotConfigurationService, 'getKey').mockReturnValue(of('false'));
            jest.spyOn(dotConfigurationService, 'getKeyAsList').mockReturnValue(
                of(['host', 'vanityurl', 'persona', 'languagevariable'])
            );
        });

        describe('dot-form-selector', () => {
            let dotFormSelector: DebugElement;

            beforeEach(() => {
                jest.spyOn(dotGlobalMessageService, 'success');

                fixture.detectChanges();
                dotFormSelector = de.query(By.css('dot-form-selector'));
            });

            it('should have', () => {
                expect(dotFormSelector).not.toBeNull();
                expect(dotFormSelector.componentInstance.show).toBe(false);
            });

            describe('events', () => {
                it('select > should add form', () => {
                    dotFormSelector.triggerEventHandler('pick', {
                        baseType: 'string',
                        clazz: 'string',
                        id: '123'
                    });
                    fixture.detectChanges();

                    expect<any>(dotEditContentHtmlService.renderAddedForm).toHaveBeenCalledWith(
                        '123'
                    );
                    expect(dotFormSelector.componentInstance.show).toBe(false);
                });

                it('close > should close form', () => {
                    component.editForm = true;
                    dotFormSelector.triggerEventHandler('shutdown', {});
                    expect(component.editForm).toBe(false);
                });
            });
        });

        describe('dot-edit-page-toolbar', () => {
            let toolbarElement: DebugElement;

            beforeEach(() => {
                jest.spyOn(dialogService, 'open');

                fixture.detectChanges();
                toolbarElement = de.query(By.css('dot-edit-page-toolbar'));
            });

            it('should have', () => {
                expect(toolbarElement).not.toBeNull();
            });

            it('should pass pageState', () => {
                expect(toolbarElement.componentInstance.pageState).toEqual(mockRenderedPageState);
            });

            it('should pass variant information', () => {
                const variant = EXPERIMENT_MOCK.trafficProportion.variants[1];

                expect(toolbarElement.componentInstance.variant).toEqual({
                    variant: {
                        id: variant.id,
                        url: variant.url,
                        title: variant.name,
                        isOriginal: variant.name === DEFAULT_VARIANT_NAME
                    },
                    pageId: EXPERIMENT_MOCK.pageId,
                    experimentId: EXPERIMENT_MOCK.id,
                    experimentStatus: EXPERIMENT_MOCK.status,
                    experimentName: EXPERIMENT_MOCK.name,
                    mode: DotPageMode.PREVIEW
                });
            });

            it('should pass running experiment', () => {
                expect(toolbarElement.componentInstance.runningExperiment).toEqual(EXPERIMENT_MOCK);
            });

            describe('events', () => {
                it('cancel > should go to site browser', () => {
                    toolbarElement.triggerEventHandler('cancel', {});
                    expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
                });

                it('actionFired > should reload', () => {
                    toolbarElement.triggerEventHandler('actionFired', null);
                    expect(dotPageStateService.reload).toHaveBeenCalledTimes(1);
                });

                it('actionFired > should reload', () => {
                    const contentlet: DotCMSContentlet = {
                        url: '/test',
                        host: '123',
                        languageId: 1
                    } as DotCMSContentlet;

                    toolbarElement.triggerEventHandler('actionFired', contentlet);
                    expect(dotRouterService.goToEditPage).toHaveBeenCalledOnceWith({
                        url: contentlet.url,
                        host_id: contentlet.host,
                        language_id: contentlet.languageId
                    });
                });

                it('whatschange > should show dot-whats-changed', () => {
                    let whatschange = de.query(By.css('dot-whats-changed'));
                    expect(whatschange).toBeNull();
                    toolbarElement.triggerEventHandler('whatschange', true);
                    fixture.detectChanges();
                    whatschange = de.query(By.css('dot-whats-changed'));
                    expect(whatschange).not.toBeNull();
                });

                it('should instantiate dialog with DotFavoritePageComponent', () => {
                    toolbarElement.triggerEventHandler('favoritePage', true);
                    expect(dialogService.open).toHaveBeenCalledTimes(1);
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
                jest.spyOn(dotCustomEventHandlerService, 'handle');
                dotEditContentlet.triggerEventHandler('custom', { data: 'test' });

                expect<any>(dotCustomEventHandlerService.handle).toHaveBeenCalledWith({
                    data: 'test'
                });
            });

            it('should reload page when triggering save-page', () => {
                dotEditContentlet.triggerEventHandler('custom', {
                    detail: {
                        name: 'save-page',
                        payload: {}
                    }
                });
                dotContentletEditorService.close$.next(true);

                expect(component.reload).toHaveBeenCalledWith(null);
            });
        });

        describe('dot-create-contentlet', () => {
            let dotCreateContentlet;

            beforeEach(() => {
                fixture.detectChanges();
                dotCreateContentlet = de.query(By.css('dot-create-contentlet'));
            });

            it('should call dotCustomEventHandlerService on customEvent', () => {
                jest.spyOn(dotCustomEventHandlerService, 'handle');
                dotCreateContentlet.triggerEventHandler('custom', { data: 'test' });

                expect<any>(dotCustomEventHandlerService.handle).toHaveBeenCalledWith({
                    data: 'test'
                });
            });

            it('should reload page when triggering save-page', () => {
                dotCreateContentlet.triggerEventHandler('custom', {
                    detail: {
                        name: 'save-page',
                        payload: {}
                    }
                });
                dotContentletEditorService.close$.next(true);

                expect(component.reload).toHaveBeenCalledWith(null);
            });

            it('should remove Contentlet Placeholder on close', () => {
                jest.spyOn(dotEditContentHtmlService, 'removeContentletPlaceholder');
                dotCreateContentlet.triggerEventHandler('shutdown', {});

                expect(dotEditContentHtmlService.removeContentletPlaceholder).toHaveBeenCalledTimes(
                    1
                );
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
                expect(wrapper.classes['dot-edit__page-wrapper--deviced']).toBeUndefined();

                expect(deviceWrapper).not.toBeNull();
                expect(iframeWrapper).not.toBeNull();
            });

            describe('with device selected', () => {
                beforeEach(() => {
                    route.parent.parent.data = of({
                        content: new DotPageRenderState(
                            mockUser(),
                            new DotPageRender({
                                ...mockDotRenderedPage(),
                                viewAs: {
                                    ...mockDotRenderedPage().viewAs,
                                    device: {
                                        cssHeight: '100',
                                        cssWidth: '100',
                                        name: 'Watch',
                                        inode: '1234',
                                        identifier: 'abc'
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

                xit('should add inline styles to iframe', (done) => {
                    setTimeout(() => {
                        const iframeEl = de.query(By.css('.dot-edit__iframe'));
                        expect(iframeEl.styles).toEqual({
                            position: '',
                            visibility: ''
                        });
                        done();
                    }, 1000);
                });

                it('should add inline styles to device wrapper', (done) => {
                    setTimeout(() => {
                        const deviceWraper = de.query(By.css('.dot-edit__iframe-wrapper'));
                        expect(deviceWraper.styles.cssText).toEqual('width: 100px; height: 100px;');
                        done();
                    }, 100);
                });
            });
        });

        describe('iframe', () => {
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

            it('should show', fakeAsync(() => {
                detectChangesForIframeRender(fixture);
                const iframeEl = getIframe();
                expect(iframeEl).not.toBeNull();
            }));

            it('should have attr setted', fakeAsync(() => {
                detectChangesForIframeRender(fixture);
                const iframeEl = getIframe();
                expect(iframeEl.attributes.class).toContain('dot-edit__iframe');
                expect(iframeEl.attributes.frameborder).toBe('0');
                expect(iframeEl.attributes.height).toBe('100%');
                expect(iframeEl.attributes.width).toBe('100%');
            }));

            describe('render html ', () => {
                beforeEach(() => {
                    jest.spyOn(dotEditContentHtmlService, 'renderPage');
                    jest.spyOn(dotEditContentHtmlService, 'initEditMode');
                    jest.spyOn(dotEditContentHtmlService, 'setCurrentPage');
                });

                it('should render in preview mode', fakeAsync(() => {
                    detectChangesForIframeRender(fixture);
                    component.isEditMode = false;
                    expect(dotEditContentHtmlService.renderPage).toHaveBeenCalledWith(
                        mockRenderedPageState,
                        expect.any(ElementRef)
                    );
                    fixture.detectChanges();
                    const wrapperEdit = de.query(By.css('[data-testId="edit-content-wrapper"]'));

                    expect(dotEditContentHtmlService.initEditMode).not.toHaveBeenCalled();
                    expect(dotEditContentHtmlService.setCurrentPage).toHaveBeenCalledWith(
                        mockRenderedPageState.page
                    );
                    expect(wrapperEdit.nativeElement).toHaveClass('dot-edit-content__preview');
                }));

                it('should render in edit mode', fakeAsync(() => {
                    jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(true));
                    component.isEditMode = true;
                    const state = new DotPageRenderState(
                        mockUser(),
                        new DotPageRender({
                            ...mockDotRenderedPage(),
                            page: {
                                ...mockDotRenderedPage().page,
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
                    fixture.detectChanges();

                    const wrapperEdit = de.query(By.css('[data-testId="edit-content-wrapper"]'));
                    expect(dotEditContentHtmlService.initEditMode).toHaveBeenCalledWith(
                        state,
                        expect.any(ElementRef)
                    );
                    expect(dotEditContentHtmlService.renderPage).not.toHaveBeenCalled();
                    expect(dotEditContentHtmlService.setCurrentPage).toHaveBeenCalledWith(
                        state.page
                    );
                    expect(wrapperEdit.nativeElement).not.toHaveClass('dot-edit-content__preview');
                }));

                it('should show/hide content palette in edit mode with correct content', fakeAsync(() => {
                    jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(true));
                    const state = new DotPageRenderState(
                        mockUser(),
                        new DotPageRender({
                            ...mockDotRenderedPage(),
                            page: {
                                ...mockDotRenderedPage().page,
                                lockedBy: null
                            },
                            viewAs: {
                                mode: DotPageMode.EDIT,
                                language: mockDotLanguage
                            }
                        })
                    );
                    route.parent.parent.data = of({
                        content: state
                    });
                    detectChangesForIframeRender(fixture);
                    fixture.detectChanges();
                    const contentPaletteWrapper = de.query(By.css('.dot-edit-content__palette'));
                    const contentPalette: DotPaletteComponent = de.query(
                        By.css('dot-palette')
                    ).componentInstance;
                    const paletteController = de.query(
                        By.css('.dot-edit-content__palette-visibility')
                    );
                    const classList = contentPaletteWrapper.nativeElement.classList;

                    expect(parseInt(contentPalette.languageId)).toEqual(
                        mockDotRenderedPage().page.languageId
                    );
                    expect(classList.contains('editMode')).toEqual(true);
                    paletteController.triggerEventHandler('click', '');
                    fixture.detectChanges();
                    expect(classList.contains('collapsed')).toEqual(true);

                    expect(dotEditContentHtmlService.setCurrentPage).toHaveBeenCalledWith(
                        state.page
                    );
                }));

                it('should not display palette when is not enterprise', fakeAsync(() => {
                    jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));
                    const state = new DotPageRenderState(
                        mockUser(),
                        new DotPageRender({
                            ...mockDotRenderedPage(),
                            page: {
                                ...mockDotRenderedPage().page,
                                lockedBy: null
                            },
                            viewAs: {
                                mode: DotPageMode.EDIT,
                                language: mockDotLanguage
                            }
                        })
                    );
                    route.parent.parent.data = of({
                        content: state
                    });
                    detectChangesForIframeRender(fixture);
                    fixture.detectChanges();
                    const contentPaletteWrapper = de.query(By.css('.dot-edit-content__palette'));
                    expect(contentPaletteWrapper).toBeNull();
                    expect(dotEditContentHtmlService.setCurrentPage).toHaveBeenCalledWith(
                        state.page
                    );
                }));

                it('should reload the page because of EMA', fakeAsync(() => {
                    jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));
                    const state = new DotPageRenderState(
                        mockUser(),
                        new DotPageRender({
                            ...mockDotRenderedPage(),
                            page: {
                                ...mockDotRenderedPage().page,
                                lockedBy: null,
                                remoteRendered: true
                            },
                            viewAs: {
                                mode: DotPageMode.EDIT,
                                language: mockDotLanguage
                            }
                        })
                    );
                    route.parent.parent.data = of({
                        content: state
                    });
                    detectChangesForIframeRender(fixture);
                    fixture.detectChanges();

                    dotEditContentHtmlService.pageModel$.next({
                        model: [{ identifier: 'test', uuid: '111' }],
                        type: PageModelChangeEventType.MOVE_CONTENT
                    });

                    expect(dotPageStateService.reload).toHaveBeenCalledTimes(1);

                    flush();
                }));

                it('should NOT reload the page', fakeAsync(() => {
                    jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));

                    const state = new DotPageRenderState(
                        mockUser(),
                        new DotPageRender({
                            ...mockDotRenderedPage(),
                            page: {
                                ...mockDotRenderedPage().page,
                                lockedBy: null
                            },
                            viewAs: {
                                mode: DotPageMode.EDIT,
                                language: mockDotLanguage
                            }
                        })
                    );

                    route.parent.parent.data = of({
                        content: state
                    });

                    detectChangesForIframeRender(fixture);

                    fixture.detectChanges();

                    dotEditContentHtmlService.pageModel$.next({
                        model: [{ identifier: 'test', uuid: '111' }],
                        type: PageModelChangeEventType.MOVE_CONTENT
                    });

                    expect(dotPageStateService.reload).toHaveBeenCalledTimes(0);

                    flush();
                }));
            });

            describe('events', () => {
                beforeEach(() => {
                    route.parent.parent.data = of({
                        content: new DotPageRenderState(
                            mockUser(),
                            new DotPageRender({
                                ...mockDotRenderedPage(),
                                viewAs: {
                                    mode: DotPageMode.EDIT
                                }
                            })
                        )
                    });
                });

                it('should handle load', fakeAsync(() => {
                    jest.spyOn(dotLoadingIndicatorService, 'hide');
                    jest.spyOn(dotUiColorsService, 'setColors');
                    detectChangesForIframeRender(fixture);

                    expect(dotLoadingIndicatorService.hide).toHaveBeenCalled();
                    expect(dotUiColorsService.setColors).toHaveBeenCalled();
                }));

                describe('custom', () => {
                    it('should handle remote-render-edit', fakeAsync(() => {
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
                    }));

                    it('should handle in-iframe', fakeAsync(() => {
                        detectChangesForIframeRender(fixture);

                        triggerIframeCustomEvent({
                            name: 'in-iframe'
                        });

                        expect(dotPageStateService.reload).toHaveBeenCalled();
                    }));

                    it('should handle reorder-menu', fakeAsync(() => {
                        detectChangesForIframeRender(fixture);

                        triggerIframeCustomEvent({
                            name: 'reorder-menu',
                            data: 'some/url/to/reorder/menu'
                        });

                        fixture.detectChanges();

                        const menu = de.query(By.css('dot-reorder-menu'));
                        expect(menu.componentInstance.url).toBe('some/url/to/reorder/menu');
                    }));

                    it('should handle load-edit-mode-page to internal navigation', fakeAsync(() => {
                        jest.spyOn(dotPageStateService, 'setLocalState').mockImplementation(() => {
                            //
                        });
                        detectChangesForIframeRender(fixture);

                        triggerIframeCustomEvent({
                            name: 'load-edit-mode-page',
                            data: mockDotRenderedPage()
                        });

                        fixture.detectChanges();
                        const dotRenderedPageStateExpected = new DotPageRenderState(
                            mockUser(),
                            mockDotRenderedPage()
                        );
                        expect(dotPageStateService.setLocalState).toHaveBeenCalledWith(
                            dotRenderedPageStateExpected
                        );
                    }));

                    it('should handle load-edit-mode-page to internal navigation', fakeAsync(() => {
                        jest.spyOn(
                            dotPageStateService,
                            'setInternalNavigationState'
                        ).mockImplementation(() => {
                            //
                        });

                        detectChangesForIframeRender(fixture);

                        const mockDotRenderedPageCopy = mockDotRenderedPage();
                        mockDotRenderedPageCopy.page.pageURI = '/another/url/test';

                        triggerIframeCustomEvent({
                            name: 'load-edit-mode-page',
                            data: mockDotRenderedPageCopy
                        });

                        fixture.detectChanges();

                        const dotRenderedPageStateExpected = new DotPageRenderState(
                            mockUser(),
                            mockDotRenderedPageCopy
                        );

                        expect(dotPageStateService.setInternalNavigationState).toHaveBeenCalledWith(
                            dotRenderedPageStateExpected
                        );
                        expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                            url: mockDotRenderedPageCopy.page.pageURI
                        });
                    }));

                    it('should handle save-menu-order', fakeAsync(() => {
                        detectChangesForIframeRender(fixture);

                        triggerIframeCustomEvent({
                            name: 'save-menu-order'
                        });

                        fixture.detectChanges();

                        expect(dotPageStateService.reload).toHaveBeenCalled();

                        const menu = de.query(By.css('dot-reorder-menu'));
                        expect(menu.componentInstance.url).toBe('');
                    }));

                    it('should handle error-saving-menu-order', fakeAsync(() => {
                        jest.spyOn(dotGlobalMessageService, 'error').mockImplementation(() => {
                            //
                        });

                        detectChangesForIframeRender(fixture);

                        triggerIframeCustomEvent({
                            name: 'error-saving-menu-order'
                        });

                        fixture.detectChanges();
                        dotGlobalMessageService.error('Error msg');

                        const menu = de.query(By.css('dot-reorder-menu'));
                        expect(menu.componentInstance.url).toBe('');
                    }));

                    it('should handle cancel-save-menu-order', fakeAsync(() => {
                        jest.spyOn(dotGlobalMessageService, 'error').mockImplementation(() => {
                            //
                        });

                        detectChangesForIframeRender(fixture);

                        triggerIframeCustomEvent({
                            name: 'cancel-save-menu-order'
                        });

                        fixture.detectChanges();

                        const menu = de.query(By.css('dot-reorder-menu'));
                        expect(menu.componentInstance.url).toBe('');
                        expect(dotPageStateService.reload).toHaveBeenCalledTimes(1);
                    }));

                    it('should handle edit-block-editor', fakeAsync(() => {
                        detectChangesForIframeRender(fixture);
                        jest.spyOn(dotEventsService, 'notify');

                        triggerIframeCustomEvent({
                            name: 'edit-block-editor',
                            data: 'test'
                        });
                        fixture.detectChanges();

                        expect(dotEventsService.notify).toHaveBeenCalledWith(
                            EDIT_BLOCK_EDITOR_CUSTOM_EVENT,
                            'test'
                        );
                    }));
                });

                describe('iframe events', () => {
                    it('should handle edit event', (done) => {
                        jest.spyOn(dotContentletEditorService, 'edit').mockImplementation(
                            (param) => {
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
                            }
                        );

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

                    it('should handle code event', (done) => {
                        jest.spyOn(dotContentletEditorService, 'edit').mockImplementation(
                            (param) => {
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
                            }
                        );

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
                        jest.spyOn(
                            dotEditContentHtmlService,
                            'setContainterToAppendContentlet'
                        ).mockImplementation(() => {
                            //
                        });

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

                    it('should handle add content event', (done) => {
                        jest.spyOn(
                            dotEditContentHtmlService,
                            'setContainterToAppendContentlet'
                        ).mockImplementation(() => {
                            //
                        });
                        jest.spyOn(dotContentletEditorService, 'add').mockImplementation(
                            (param) => {
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
                            }
                        );

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

                    it('should handle create new content event', (done) => {
                        const data = {
                            container: {
                                dotIdentifier: 'identifier',
                                dotUuid: 'uuid'
                            },
                            contentType: { variable: 'blog' }
                        };
                        jest.spyOn(
                            dotEditContentHtmlService,
                            'setContainterToAppendContentlet'
                        ).mockImplementation(() => {
                            //
                        });

                        jest.spyOn(dotContentletEditorService, 'getActionUrl').mockReturnValue(
                            of('/url/test?_content_lang=23&test=random')
                        );

                        jest.spyOn(dotContentletEditorService, 'create').mockImplementation(
                            (param) => {
                                //checking the replace of lang.
                                expect(param.data).toEqual({
                                    url: '/url/test?_content_lang=1&test=random'
                                });

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
                            }
                        );

                        fixture.detectChanges();

                        dotEditContentHtmlService.iframeActions$.next({
                            name: 'add-content',
                            data: data
                        });

                        expect(dotContentletEditorService.getActionUrl).toHaveBeenCalledOnceWith(
                            'blog'
                        );

                        const container: DotPageContainer = {
                            identifier: 'identifier',
                            uuid: 'uuid'
                        };

                        expect(
                            dotEditContentHtmlService.setContainterToAppendContentlet
                        ).toHaveBeenCalledWith(container);
                    });

                    it('should display Form Selector when handle add content event of form Type', () => {
                        jest.spyOn(
                            dotEditContentHtmlService,
                            'setContainterToAppendContentlet'
                        ).mockImplementation(() => {
                            /**/
                        });
                        jest.spyOn(
                            dotEditContentHtmlService,
                            'removeContentletPlaceholder'
                        ).mockImplementation(() => {
                            /**/
                        });
                        jest.spyOn(component, 'addFormContentType');

                        fixture.detectChanges();

                        const data = {
                            container: {
                                dotIdentifier: 'identifier',
                                dotUuid: 'uuid'
                            },
                            contentType: { variable: 'forms' }
                        };

                        dotEditContentHtmlService.iframeActions$.next({
                            name: 'add-content',
                            data: data
                        });

                        const container: DotPageContainer = {
                            identifier: data.container.dotIdentifier,
                            uuid: data.container.dotUuid
                        };

                        expect(
                            dotEditContentHtmlService.setContainterToAppendContentlet
                        ).toHaveBeenCalledWith(container);
                        expect(
                            dotEditContentHtmlService.removeContentletPlaceholder
                        ).toHaveBeenCalled();
                        expect(component.addFormContentType).toHaveBeenCalled();
                        expect(component.editForm).toBeTruthy();
                    });

                    it('should handle remove event', (done) => {
                        jest.spyOn(
                            dotEditContentHtmlService,
                            'removeContentlet'
                        ).mockImplementation(() => {
                            //
                        });
                        jest.spyOn(dotDialogService, 'confirm').mockImplementation((param) => {
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
                        jest.spyOn(dotContentletEditorService, 'clear').mockImplementation(() => {
                            //
                        });

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
                        mockUser(),
                        new DotPageRender({
                            ...mockDotRenderedPage(),
                            viewAs: {
                                ...mockDotRenderedPage().viewAs,
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
                dotFormSelector.triggerEventHandler('pick', {
                    baseType: 'string',
                    clazz: 'string',
                    id: '123'
                });
                fixture.detectChanges();

                expect<any>(dotEditContentHtmlService.renderAddedForm).toHaveBeenCalledWith('123');
            });
        });
    });

    describe('dot-edit-page-toolbar-seo', () => {
        let toolbarElement: DebugElement;

        beforeEach(() => {
            jest.spyOn(dialogService, 'open');
            jest.spyOn(dotConfigurationService, 'getKey').mockReturnValue(of('true'));

            fixture.detectChanges();
            toolbarElement = de.query(By.css('dot-edit-page-toolbar-seo'));
        });

        it('should have', () => {
            expect(toolbarElement).not.toBeNull();
        });
    });

    describe('errors', () => {
        beforeEach(() => {
            jest.spyOn(dotConfigurationService, 'getKeyAsList').mockReturnValue(
                of(['host', 'vanityurl', 'persona', 'languagevariable'])
            );
            fixture.detectChanges();
        });

        describe('iframe events', () => {
            it('should reload content on SAVE_ERROR', () => {
                dotEditContentHtmlService.pageModel$.next({
                    model: [{ identifier: 'test', uuid: '111' }],
                    type: PageModelChangeEventType.SAVE_ERROR
                });

                expect(dotPageStateService.reload).toHaveBeenCalledTimes(1);
            });
        });
    });

    describe('allowedContent', () => {
        it('should set the allowedContent correctly', fakeAsync(() => {
            const blackList = ['host', 'vanityurl', 'persona', 'languagevariable'];
            jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(true));
            jest.spyOn(dotConfigurationService, 'getKeyAsList').mockReturnValue(of(blackList));

            const state = new DotPageRenderState(
                mockUser(),
                new DotPageRender({
                    ...mockDotRenderedPage(),
                    page: {
                        ...mockDotRenderedPage().page,
                        lockedBy: null
                    },
                    viewAs: {
                        mode: DotPageMode.EDIT,
                        language: mockDotLanguage
                    },
                    containers: {
                        ...mockDotRenderedPage().containers,
                        '/persona/': {
                            container: processedContainers[0].container,
                            containerStructures: [{ contentTypeVar: 'persona' }]
                        },
                        '/host/': {
                            container: processedContainers[0].container,
                            containerStructures: [{ contentTypeVar: 'host' }]
                        }
                    }
                })
            );

            const allowedContent: Set<string> = new Set();
            Object.values(state.containers).forEach((container) => {
                Object.values(container.containerStructures).forEach((containerStructure) => {
                    allowedContent.add(containerStructure.contentTypeVar.toLocaleLowerCase());
                });
            });

            blackList.forEach((content) => allowedContent.delete(content.toLocaleLowerCase()));

            route.parent.parent.data = of({ content: state });
            detectChangesForIframeRender(fixture);
            fixture.detectChanges();
            expect(component.allowedContent).toEqual([...allowedContent]);
        }));
    });

    it('should remove variant key from session storage on destoy', () => {
        jest.spyOn(dotSessionStorageService, 'removeVariantId');
        component.ngOnDestroy();
        expect(dotSessionStorageService.removeVariantId).toHaveBeenCalledTimes(1);
    });

    it('should keep variant key from session storage if going to layout portlet', () => {
        router.routerState.snapshot.url = '/edit-page/layout';
        jest.spyOn(dotSessionStorageService, 'removeVariantId');
        component.ngOnDestroy();
        expect(dotSessionStorageService.removeVariantId).toHaveBeenCalledTimes(0);
    });

    it("should set reload to null when site is changed and it's not the first time", () => {
        fixture.detectChanges(); // Initialize component and set up subscriptions
        siteService.setFakeCurrentSite(mockSites[1]); // Trigger site change after subscription is active
        expect(dotPageStateService.reload).toHaveBeenCalledTimes(1);
    });
});
