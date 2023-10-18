/* eslint-disable @typescript-eslint/no-explicit-any */

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement, Injectable } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';

import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotContentletEditorModule } from '@components/dot-contentlet-editor/dot-contentlet-editor.module';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotCustomEventHandlerService } from '@dotcms/app/api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotDownloadBundleDialogService } from '@dotcms/app/api/services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotUiColorsService } from '@dotcms/app/api/services/dot-ui-colors/dot-ui-colors.service';
import { DotWizardService } from '@dotcms/app/api/services/dot-wizard/dot-wizard.service';
import { DotWorkflowEventHandlerService } from '@dotcms/app/api/services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { PushPublishService } from '@dotcms/app/api/services/push-publish/push-publish.service';
import {
    DotAlertConfirmService,
    DotCurrentUserService,
    DotEventsService,
    DotGenerateSecurePasswordService,
    DotLicenseService,
    DotWorkflowActionsFireService
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
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { DotFormatDateService } from '@dotcms/ui';
import { CoreWebServiceMock, LoginServiceMock, MockDotRouterService } from '@dotcms/utils-testing';

import { DotContentletsComponent } from './dot-contentlets.component';

import { dotEventSocketURLFactory, MockDotUiColorsService } from '../../../test/dot-test-bed';

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
            imports: [DotContentletEditorModule, RouterTestingModule, HttpClientTestingModule],
            providers: [
                DotContentletEditorService,
                DotIframeService,
                DotCustomEventHandlerService,
                DotLicenseService,
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
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                PushPublishService,
                ApiRoot,
                DotFormatDateService,
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
                DotGenerateSecurePasswordService,
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

    it('should call contentlet modal', async () => {
        const params = {
            data: {
                inode: '5cd3b647-e465-4a6d-a78b-e834a7a7331a'
            }
        };
        await fixture.whenStable();
        expect(dotContentletEditorService.edit).toHaveBeenCalledWith(params);
    });

    it('should go current portlet and reload data when modal closed', () => {
        const edit = de.query(By.css('dot-edit-contentlet'));
        edit.triggerEventHandler('shutdown', {});
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('this/is/an');
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('123-567');
    });

    it('should call dotCustomEventHandlerService on customEvent', () => {
        spyOn(dotCustomEventHandlerService, 'handle');
        const edit = de.query(By.css('dot-edit-contentlet'));
        edit.triggerEventHandler('custom', { data: 'test' });
        expect<any>(dotCustomEventHandlerService.handle).toHaveBeenCalledWith({ data: 'test' });
    });
});
