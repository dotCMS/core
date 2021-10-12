import { of as observableOf, Subject } from 'rxjs';
import { mockUser } from './../../../../test/login-service.mock';
import { mockDotRenderedPage } from '../../../../test/dot-page-render.mock';
import { DotPageLayoutService } from '@services/dot-page-layout/dot-page-layout.service';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditPageMainComponent } from './dot-edit-page-main.component';
import { DotEditPageNavModule } from '../dot-edit-page-nav/dot-edit-page-nav.module';
import { RouterTestingModule } from '@angular/router/testing';
import { By } from '@angular/platform-browser';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { ActivatedRoute } from '@angular/router';
import { DotEditPageNavComponent } from '../dot-edit-page-nav/dot-edit-page-nav.component';
import { PageViewServiceMock } from '../../../../test/page-view.mock';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { Injectable, Component, Output, EventEmitter } from '@angular/core';
import { DotPageStateService } from '../../content/services/dot-page-state/dot-page-state.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { DotPageRender } from '@models/dot-page/dot-rendered-page.model';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotLoadingIndicatorService } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { DotWorkflowEventHandlerService } from '@services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import {
    ApiRoot,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '../../../../test/core-web.service.mock';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '@tests/dot-test-bed';
import { DotCurrentUserService } from '@services/dot-current-user/dot-current-user.service';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotWizardService } from '@services/dot-wizard/dot-wizard.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { ConfirmationService } from 'primeng/api';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotDownloadBundleDialogModule } from '@components/_common/dot-download-bundle-dialog/dot-download-bundle-dialog.module';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DotGenerateSecurePasswordService } from '@services/dot-generate-secure-password/dot-generate-secure-password.service';

@Injectable()
class MockDotContentletEditorService {
    close$ = new Subject();
}

@Injectable()
class MockDotPageStateService {
    reload$ = new Subject();
    state$ = new Subject();
    get(): void {}
    reload(): void {
        this.reload$.next(
            new DotPageRenderState(mockUser(), new DotPageRender(mockDotRenderedPage()))
        );
    }
}

@Component({
    selector: 'dot-edit-contentlet',
    template: ''
})
class MockDotEditContentletComponent {
    @Output() custom = new EventEmitter<any>();
}

describe('DotEditPageMainComponent', () => {
    let fixture: ComponentFixture<DotEditPageMainComponent>;
    let route: ActivatedRoute;
    let dotContentletEditorService: DotContentletEditorService;
    let dotPageStateService: DotPageStateService;
    let dotRouterService: DotRouterService;
    let dotCustomEventHandlerService: DotCustomEventHandlerService;
    let editContentlet: MockDotEditContentletComponent;

    const messageServiceMock = new MockDotMessageService({
        'editpage.toolbar.nav.content': 'Content',
        'editpage.toolbar.nav.layout': 'Layout',
        'editpage.toolbar.nav.properties': 'Properties'
    });

    const mockDotRenderedPageState: DotPageRenderState = new DotPageRenderState(
        mockUser(),
        new DotPageRender(mockDotRenderedPage())
    );

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                imports: [
                    RouterTestingModule.withRoutes([
                        {
                            component: DotEditPageMainComponent,
                            path: ''
                        }
                    ]),
                    DotEditPageNavModule,
                    DotDownloadBundleDialogModule,
                    HttpClientTestingModule
                ],
                declarations: [DotEditPageMainComponent, MockDotEditContentletComponent],
                providers: [
                    { provide: DotMessageService, useValue: messageServiceMock },
                    {
                        provide: ActivatedRoute,
                        useValue: {
                            snapshot: {
                                queryParams: {
                                    url: '/about-us/index'
                                }
                            }
                        }
                    },
                    { provide: DotPageLayoutService, useClass: PageViewServiceMock },
                    {
                        provide: DotContentletEditorService,
                        useClass: MockDotContentletEditorService
                    },
                    {
                        provide: DotPageStateService,
                        useClass: MockDotPageStateService
                    },
                    DotCustomEventHandlerService,
                    DotLoadingIndicatorService,
                    DotWorkflowEventHandlerService,
                    PushPublishService,
                    { provide: CoreWebService, useClass: CoreWebServiceMock },
                    { provide: DotRouterService, useClass: MockDotRouterService },
                    { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                    PushPublishService,
                    ApiRoot,
                    DotFormatDateService,
                    UserModel,
                    StringUtils,
                    DotcmsEventsService,
                    LoggerService,
                    DotGenerateSecurePasswordService,
                    DotEventsSocket,
                    { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                    DotcmsConfigService,
                    LoggerService,
                    DotCurrentUserService,
                    DotMessageDisplayService,
                    DotWizardService,
                    DotHttpErrorManagerService,
                    DotAlertConfirmService,
                    ConfirmationService,
                    DotWorkflowActionsFireService,
                    DotGlobalMessageService,
                    DotEventsService,
                    DotIframeService,
                    LoginService,
                    DotLicenseService
                ]
            });
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditPageMainComponent);
        route = fixture.debugElement.injector.get(ActivatedRoute);
        route.data = observableOf({
            content: mockDotRenderedPageState
        });
        dotContentletEditorService = fixture.debugElement.injector.get(DotContentletEditorService);
        dotRouterService = fixture.debugElement.injector.get(DotRouterService);
        dotPageStateService = fixture.debugElement.injector.get(DotPageStateService);
        dotCustomEventHandlerService = fixture.debugElement.injector.get(
            DotCustomEventHandlerService
        );
        editContentlet = fixture.debugElement.query(By.css('dot-edit-contentlet'))
            .componentInstance;
        fixture.detectChanges();
    });

    it('should have router-outlet', () => {
        expect(fixture.debugElement.query(By.css('router-outlet'))).not.toBeNull();
    });

    it('should have dot-edit-page-nav', () => {
        expect(fixture.debugElement.query(By.css('dot-edit-page-nav'))).not.toBeNull();
    });

    it('should bind correctly pageState param', () => {
        const nav: DotEditPageNavComponent = fixture.debugElement.query(By.css('dot-edit-page-nav'))
            .componentInstance;
        expect(nav.pageState).toEqual(mockDotRenderedPageState);
    });

    it('should not call goToEditPage if the dialog is closed without new page properties', () => {
        spyOn(dotPageStateService, 'get').and.callThrough();

        dotContentletEditorService.close$.next(true);
        expect(dotRouterService.goToEditPage).not.toHaveBeenCalled();
        expect(dotPageStateService.get).not.toHaveBeenCalled();
    });

    it('should call goToEditPage if page properties were saved with different URLs', () => {
        spyOn(dotPageStateService, 'get').and.callThrough();
        editContentlet.custom.emit({
            detail: {
                name: 'save-page',
                payload: {
                    htmlPageReferer: '/index'
                }
            }
        });

        dotContentletEditorService.close$.next(true);
        expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
            url: '/index',
            language_id: '1'
        });
        dotContentletEditorService.close$.next(true);
        expect(dotRouterService.goToEditPage).toHaveBeenCalledTimes(1);
        expect(dotPageStateService.get).not.toHaveBeenCalled();
    });

    it('should call get if page properties were saved with equal URLs', () => {
        spyOn(dotPageStateService, 'get').and.callThrough();
        editContentlet.custom.emit({
            detail: {
                name: 'save-page',
                payload: {
                    htmlPageReferer: '/about-us/index'
                }
            }
        });

        dotContentletEditorService.close$.next(true);
        expect(dotPageStateService.get).toHaveBeenCalledWith({
            url: '/about-us/index',
            viewAs: { language: 1 }
        });
    });

    describe('handle custom events from contentlet editor', () => {
        it('should reload page when url attribute in dialog has been changed', () => {
            editContentlet.custom.emit({
                detail: {
                    name: 'save-page',
                    payload: {
                        htmlPageReferer:
                            '/about-us/index2?com.dotmarketing.htmlpage.language=1&host_id=48190c8c-42c4-46af-8d1a-0cd5db894797'
                    }
                }
            });
            dotContentletEditorService.close$.next(true);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/about-us/index2',
                language_id: mockDotRenderedPage().page.languageId.toString()
            });
        });

        it('should go to site-browser when page is deleted', () => {
            editContentlet.custom.emit({
                detail: {
                    name: 'deleted-page'
                }
            });
            expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
        });

        it('should call dotCustomEventHandlerService on customEvent', () => {
            spyOn(dotCustomEventHandlerService, 'handle');
            editContentlet.custom.emit({
                detail: {
                    name: 'random'
                }
            });
            expect<any>(dotCustomEventHandlerService.handle).toHaveBeenCalledWith({
                detail: {
                    name: 'random'
                }
            });
        });
    });
});
