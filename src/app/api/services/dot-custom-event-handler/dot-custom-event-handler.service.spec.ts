import { DotLoadingIndicatorService } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { RouterTestingModule } from '@angular/router/testing';
import { DotMenuService } from '@services/dot-menu.service';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import {
    ApiRoot,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    DotPushPublishDialogService,
    LoggerService, LoginService,
    StringUtils,
    UserModel
} from 'dotcms-js';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotWorkflowEventHandlerService } from '@services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { TestBed } from '@angular/core/testing';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { CoreWebServiceMock } from '../../../../../projects/dotcms-js/src/lib/core/core-web.service.mock';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '@tests/dot-test-bed';
import { FormatDateService } from '@services/format-date-service';
import { DotCurrentUserService } from '@services/dot-current-user/dot-current-user.service';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotWizardService } from '@services/dot-wizard/dot-wizard.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { ConfirmationService } from 'primeng/api';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('DotCustomEventHandlerService', () => {
    let service: DotCustomEventHandlerService;
    let dotLoadingIndicatorService: DotLoadingIndicatorService;
    let dotRouterService: DotRouterService;
    let dotUiColorsService: DotUiColorsService;
    let dotContentletEditorService: DotContentletEditorService;
    let dotPushPublishDialogService: DotPushPublishDialogService;
    let dotDownloadBundleDialogService: DotDownloadBundleDialogService;
    let dotWorkflowEventHandlerService: DotWorkflowEventHandlerService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotCustomEventHandlerService,
                DotLoadingIndicatorService,
                DotMenuService,
                DotPushPublishDialogService,
                DotWorkflowEventHandlerService,
                DotRouterService,
                DotContentletEditorService,
                PushPublishService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                ApiRoot,
                FormatDateService,
                UserModel,
                StringUtils,
                DotcmsEventsService,
                LoggerService,
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
                DotDownloadBundleDialogService,
                LoginService
            ],
            imports: [RouterTestingModule, HttpClientTestingModule]
        });

        service = TestBed.get(DotCustomEventHandlerService);
        dotLoadingIndicatorService = TestBed.get(DotLoadingIndicatorService);
        dotRouterService = TestBed.get(DotRouterService);
        dotUiColorsService = TestBed.get(DotUiColorsService);
        dotContentletEditorService = TestBed.get(DotContentletEditorService);
        dotPushPublishDialogService = TestBed.get(DotPushPublishDialogService);
        dotDownloadBundleDialogService = TestBed.get(DotDownloadBundleDialogService);
        dotWorkflowEventHandlerService = TestBed.get(DotWorkflowEventHandlerService);
    });

    it('should show loading indicator and go to edit page when event is emited by iframe', () => {
        spyOn(dotLoadingIndicatorService, 'show');

        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'edit-page',
                    data: {
                        url: 'some/url',
                        languageId: '2',
                        hostId: '123'
                    }
                }
            })
        );

        expect(dotLoadingIndicatorService.show).toHaveBeenCalledTimes(1);
        expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
            url: 'some/url',
            language_id: '2',
            host_id: '123'
        });
    });

    it('should create a contentlet', () => {
        spyOn(dotContentletEditorService, 'create');
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'create-contentlet',
                    data: {
                        url: 'hello.world.com'
                    }
                }
            })
        );

        expect(dotContentletEditorService.create).toHaveBeenCalledWith({
            data: {
                url: 'hello.world.com'
            }
        });
    });

    it('should edit a contentlet', () => {
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'edit-contentlet',
                    data: {
                        inode: '123'
                    }
                }
            })
        );
        expect(dotRouterService.goToEditContentlet).toHaveBeenCalledWith('123');
    });

    it('should edit a a workflow task', () => {
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'edit-task',
                    data: {
                        inode: '123'
                    }
                }
            })
        );
        expect(dotRouterService.goToEditTask).toHaveBeenCalledWith('123');
    });

    it('should set colors in the ui', () => {
        spyOn(dotUiColorsService, 'setColors');
        const fakeHtmlEl = { hello: 'html' };
        spyOn(document, 'querySelector').and.returnValue(fakeHtmlEl);

        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'company-info-updated',
                    payload: {
                        colors: {
                            primary: '#fff',
                            secondary: '#000',
                            background: '#ccc'
                        }
                    }
                }
            })
        );
        expect(dotUiColorsService.setColors).toHaveBeenCalledWith(fakeHtmlEl, {
            primary: '#fff',
            secondary: '#000',
            background: '#ccc'
        });
    });

    it('should notify to open push publish dialog', () => {
        const dataMock = {
            assetIdentifier: '123',
            dateFilter: true,
            removeOnly: true,
            isBundle: false
        };

        spyOn(dotPushPublishDialogService, 'open');
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'push-publish',
                    data: dataMock
                }
            })
        );

        expect(dotPushPublishDialogService.open).toHaveBeenCalledWith(dataMock);
    });

    it('should notify to open download bundle dialog', () => {
        spyOn(dotDownloadBundleDialogService, 'open');
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'download-bundle',
                    data: 'testID'
                }
            })
        );
        expect(dotDownloadBundleDialogService.open).toHaveBeenCalledWith('testID');
    });

    it('should notify to open download bundle dialog', () => {
        spyOn(dotWorkflowEventHandlerService, 'open');
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'workflow-wizard',
                    data: 'testData'
                }
            })
        );
        expect(dotWorkflowEventHandlerService.open).toHaveBeenCalledWith('testData');
    });
});
