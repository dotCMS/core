import { describe, it } from '@jest/globals';
import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { MessageService } from 'primeng/api';

import {
    DotAlertConfirmService,
    DotCurrentUserService,
    DotFormatDateService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService,
    PushPublishService
} from '@dotcms/data-access';
import { CoreWebService, LoginService } from '@dotcms/dotcms-js';
import { DotWorkflowActionsComponent } from '@dotcms/ui';
import { LoginServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditEmaWorkflowActionsComponent } from './dot-edit-ema-workflow-actions.component';

const messageServiceMock = new MockDotMessageService({
    'Workflow-Action': 'Workflow Action',
    'edit.content.fire.action.success': 'Success',
    'edit.ema.page.error.executing.workflow.action': 'Error',
    'edit.ema.page.executing.workflow.action': 'Executing'
});

describe('DotEditEmaWorkflowActionsComponent', () => {
    let spectator: Spectator<DotEditEmaWorkflowActionsComponent>;
    const createComponent = createComponentFactory({
        component: DotEditEmaWorkflowActionsComponent,
        declarations: [MockComponent(DotWorkflowActionsComponent)],
        imports: [HttpClientTestingModule],
        providers: [
            DotWizardService,
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotAlertConfirmService),
            mockProvider(DotMessageDisplayService),
            mockProvider(DotWorkflowsActionsService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotWorkflowEventHandlerService),
            mockProvider(MessageService),
            mockProvider(DotRouterService),
            mockProvider(PushPublishService),
            mockProvider(DotCurrentUserService),
            mockProvider(DotFormatDateService),
            mockProvider(CoreWebService),
            mockProvider(DotIframeService),
            mockProvider(DotGlobalMessageService),
            {
                provide: LoginService,
                useClass: LoginServiceMock
            },
            { provide: DotMessageService, useValue: messageServiceMock }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });
});
