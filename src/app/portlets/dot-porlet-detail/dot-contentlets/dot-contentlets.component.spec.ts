import { dotEventSocketURLFactory, MockDotUiColorsService } from '../../../test/dot-test-bed';
import { Injectable, DebugElement } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DotContentletsComponent } from './dot-contentlets.component';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotContentletEditorModule } from '@components/dot-contentlet-editor/dot-contentlet-editor.module';
import { ComponentFixture, TestBed } from '@angular/core/testing';
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
} from 'dotcms-js';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { By } from '@angular/platform-browser';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { RouterTestingModule } from '@angular/router/testing';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotWorkflowEventHandlerService } from '@services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { CoreWebServiceMock } from '../../../../../projects/dotcms-js/src/lib/core/core-web.service.mock';
import { BaseRequestOptions, ConnectionBackend, Http, RequestOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
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
import {DotDownloadBundleDialogService} from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';

@Injectable()
class MockDotContentletEditorService {
    edit = jasmine.createSpy('edit');
}

describe('DotContentletsComponent', () => {
    let fixture: ComponentFixture<DotContentletsComponent>;
    let de: DebugElement;

    let dotRouterService: DotRouterService;
    let dotIframeService: DotIframeService;
    let dotContentletEditorService: DotContentletEditorService;
    let dotCustomEventHandlerService: DotCustomEventHandlerService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotContentletsComponent],
            imports: [DotContentletEditorModule, RouterTestingModule],
            providers: [
                DotContentletEditorService,
                DotIframeService,
                DotCustomEventHandlerService,
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            params: {
                                asset: '5cd3b647-e465-4a6d-a78b-e834a7a7331a'
                            }
                        }
                    }
                },
                {
                    provide: DotContentletEditorService,
                    useClass: MockDotContentletEditorService
                },

                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                DotWorkflowEventHandlerService,
                PushPublishService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: ConnectionBackend, useClass: MockBackend },
                { provide: RequestOptions, useClass: BaseRequestOptions },
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                Http,
                PushPublishService,
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
                LoginService,
                DotDownloadBundleDialogService
            ]
        });

        fixture = TestBed.createComponent(DotContentletsComponent);
        de = fixture.debugElement;
        dotRouterService = de.injector.get(DotRouterService);
        dotIframeService = de.injector.get(DotIframeService);
        dotContentletEditorService = de.injector.get(DotContentletEditorService);
        dotCustomEventHandlerService = de.injector.get(DotCustomEventHandlerService);

        spyOn(dotIframeService, 'reloadData');
        fixture.detectChanges();
    });

    it('should call contentlet modal', () => {
        const params = {
            data: {
                inode: '5cd3b647-e465-4a6d-a78b-e834a7a7331a'
            }
        };
        fixture.whenStable().then(() => {
            expect(dotContentletEditorService.edit).toHaveBeenCalledWith(params);
        });
    });

    it('should go current portlet and reload data when modal closed', () => {
        const edit = de.query(By.css('dot-edit-contentlet'));
        edit.triggerEventHandler('close', {});
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/c/123-567');
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('123-567');
    });

    it('should call dotCustomEventHandlerService on customEvent', () => {
        spyOn(dotCustomEventHandlerService, 'handle');
        const edit = de.query(By.css('dot-edit-contentlet'));
        edit.triggerEventHandler('custom', { data: 'test' });
        expect(dotCustomEventHandlerService.handle).toHaveBeenCalledWith({ data: 'test' });
    });
});
