/* eslint-disable @typescript-eslint/no-explicit-any */

import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
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
import {
    DotcmsConfigServiceMock,
    LoginServiceMock,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { DotContentletsComponent } from './dot-contentlets.component';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotDownloadBundleDialogService } from '../../../api/services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '../../../test/dot-test-bed';
import { IframeOverlayService } from '../../../view/components/_common/iframe/service/iframe-overlay.service';
import { DotEditContentletComponent } from '../../../view/components/dot-contentlet-editor/components/dot-edit-contentlet/dot-edit-contentlet.component';
import { DotContentletEditorService } from '../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';

class MockDotContentletEditorService {
    edit = jest.fn();
}

const mockContentletEditorService = new MockDotContentletEditorService();

describe('DotContentletsComponent', () => {
    let spectator: Spectator<DotContentletsComponent>;
    let dotRouterService: DotRouterService;
    let dotIframeService: DotIframeService;
    let dotCustomEventHandlerService: DotCustomEventHandlerService;

    const createComponent = createComponentFactory({
        component: DotContentletsComponent,
        detectChanges: false,
        imports: [DotEditContentletComponent, RouterTestingModule, HttpClientTestingModule],
        componentProviders: [
            { provide: DotContentletEditorService, useValue: mockContentletEditorService }
        ],
        providers: [
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
            { provide: LoginService, useClass: LoginServiceMock },
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
            ApiRoot,
            DotFormatDateService,
            UserModel,
            StringUtils,
            DotcmsEventsService,
            LoggerService,
            DotEventsSocket,
            { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
            { provide: DotcmsConfigService, useClass: DotcmsConfigServiceMock },
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

    beforeEach(() => {
        mockContentletEditorService.edit.mockClear();
        spectator = createComponent();
        dotRouterService = spectator.inject(DotRouterService);
        dotIframeService = spectator.inject(DotIframeService);
        dotCustomEventHandlerService = spectator.inject(DotCustomEventHandlerService);
        jest.spyOn(dotIframeService, 'reloadData');
    });

    it('should call contentlet modal', fakeAsync(() => {
        spectator.detectChanges();
        tick(0);
        const params = {
            data: {
                inode: '5cd3b647-e465-4a6d-a78b-e834a7a7331a'
            }
        };
        expect(mockContentletEditorService.edit).toHaveBeenCalledWith(params);
        expect(mockContentletEditorService.edit).toHaveBeenCalledTimes(1);
    }));

    it('should go current portlet and reload data when modal closed', () => {
        spectator.detectChanges();
        const edit = spectator.debugElement.query(By.css('dot-edit-contentlet'));
        edit.triggerEventHandler('shutdown', {});
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('this/is/an', {
            queryParamsHandling: 'preserve'
        });
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('123-567');
        expect(dotIframeService.reloadData).toHaveBeenCalledTimes(1);
    });

    it('should call dotCustomEventHandlerService on customEvent', () => {
        spectator.detectChanges();
        jest.spyOn(dotCustomEventHandlerService, 'handle').mockImplementation(() => {
            /* mock implementation */
        });
        const edit = spectator.debugElement.query(By.css('dot-edit-contentlet'));
        const mockEvent = { detail: { name: 'test-event', data: 'test' } };
        edit.triggerEventHandler('custom', mockEvent);
        expect(dotCustomEventHandlerService.handle).toHaveBeenCalledWith(mockEvent);
    });
});
