/* eslint-disable @typescript-eslint/no-explicit-any */

import { mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement, Injectable } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';

import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotCurrentUserService,
    DotEventsService,
    DotFormatDateService,
    DotGenerateSecurePasswordService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotLicenseService,
    DotMessageDisplayService,
    DotRouterService,
    DotUiColorsService,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    PushPublishService
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
import { LoginServiceMock, MockDotRouterService } from '@dotcms/utils-testing';

import { DotContentletsComponent } from './dot-contentlets.component';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotDownloadBundleDialogService } from '../../../api/services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '../../../test/dot-test-bed';
import { IframeOverlayService } from '../../../view/components/_common/iframe/service/iframe-overlay.service';
import { DotEditContentletComponent } from '../../../view/components/dot-contentlet-editor/components/dot-edit-contentlet/dot-edit-contentlet.component';
import { DotContentletEditorService } from '../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';

@Injectable()
class MockDotContentletEditorService {
    edit = jest.fn();
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
            imports: [
                DotEditContentletComponent,
                RouterTestingModule,
                HttpClientTestingModule,
                DotContentletsComponent
            ],
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
                {
                    provide: CoreWebService,
                    useValue: {
                        request: jest.fn().mockReturnValue(of({})),
                        requestView: jest.fn().mockReturnValue(of({ entity: {} }))
                    }
                },
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
                DotDownloadBundleDialogService,
                mockProvider(DotContentTypeService),
                {
                    provide: IframeOverlayService,
                    useValue: {
                        overlay: of(false),
                        show: jest.fn(),
                        hide: jest.fn(),
                        toggle: jest.fn()
                    }
                }
            ]
        });

        fixture = TestBed.createComponent(DotContentletsComponent);
        de = fixture.debugElement;
        dotRouterService = de.injector.get(DotRouterService);
        dotIframeService = de.injector.get(DotIframeService);
        dotContentletEditorService = de.injector.get(DotContentletEditorService);
        dotCustomEventHandlerService = de.injector.get(DotCustomEventHandlerService);

        jest.spyOn(dotIframeService, 'reloadData');
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
        expect(dotContentletEditorService.edit).toHaveBeenCalledTimes(1);
    });

    it('should go current portlet and reload data when modal closed', () => {
        const edit = de.query(By.css('dot-edit-contentlet'));
        edit.triggerEventHandler('shutdown', {});
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('this/is/an', {
            queryParamsHandling: 'preserve'
        });
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('123-567');
        expect(dotIframeService.reloadData).toHaveBeenCalledTimes(1);
    });

    it('should call dotCustomEventHandlerService on customEvent', () => {
        jest.spyOn(dotCustomEventHandlerService, 'handle').mockImplementation(() => {
            /* mock implementation */
        });
        const edit = de.query(By.css('dot-edit-contentlet'));
        const mockEvent = { detail: { name: 'test-event', data: 'test' } };
        edit.triggerEventHandler('custom', mockEvent);
        expect<any>(dotCustomEventHandlerService.handle).toHaveBeenCalledWith(mockEvent);
    });
});
