import { TestBed } from '@angular/core/testing';

import { PushPublishService } from '@services/push-publish/push-publish.service';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotWizardService } from '@services/dot-wizard/dot-wizard.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotCMSWorkflowActionEvent } from 'dotcms-models';
import { mockWorkflowsActions } from '@tests/dot-workflows-actions.mock';
import { MockPushPublishService } from '@portlets/shared/dot-content-types-listing/dot-content-types.component.spec';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotWizardStep } from '@models/dot-wizard-step/dot-wizard-step.model';
import { DotCommentAndAssignFormComponent } from '@components/_common/forms/dot-comment-and-assign-form/dot-comment-and-assign-form.component';
import { DotPushPublishFormComponent } from '@components/_common/forms/dot-push-publish-form/dot-push-publish-form.component';
import { DotWizardInput } from '@models/dot-wizard-input/dot-wizard-input.model';
import { of } from 'rxjs';
import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import { DotWorkflowEventHandlerService } from '@services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import {
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils
} from 'dotcms-js';
import { dotEventSocketURLFactory } from '@tests/dot-test-bed';
import { BaseRequestOptions, ConnectionBackend, Http, RequestOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { RouterTestingModule } from '@angular/router/testing';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { ConfirmationService } from 'primeng/primeng';
import { LoginServiceMock } from '@tests/login-service.mock';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotCMSWorkflowAction } from 'dotcms-models';
import { DotActionBulkResult } from '@models/dot-action-bulk-result/dot-action-bulk-result.model';
import { DotActionBulkRequestOptions } from '@models/dot-action-bulk-request-options/dot-action-bulk-request-options.model';

const mockWAEvent: DotCMSWorkflowActionEvent = {
    workflow: mockWorkflowsActions[0],
    callback: 'test',
    inode: '123Inode',
    selectedInodes: []
};

const mockWizardSteps: DotWizardStep<any>[] = [
    {
        component: DotCommentAndAssignFormComponent,
        data: {
            assignable: true,
            commentable: true,
            roleId: mockWorkflowsActions[0].nextAssign,
            roleHierarchy: mockWorkflowsActions[0].roleHierarchyForAssign
        }
    },
    {
        component: DotPushPublishFormComponent,
        data: {}
    }
];

const mockWizardInput: DotWizardInput = {
    title: 'Workflow Action',
    steps: mockWizardSteps
};

const mockWizardOutputData = {
    assign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
    comments: 'ds',
    environment: ['37fe23d5-588d-4c61-a9ea-70d01e913344'],
    expireDate: '2020-08-11 19:59',
    filterKey: 'Intelligent.yml',
    publishDate: '2020-08-05 17:59',
    pushActionSelected: 'publishexpire'
};

const mockWizardOutputTransformedData = {
    assign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
    comments: 'ds',
    expireDate: '2020-08-11',
    expireTime: '19-59',
    filterKey: 'Intelligent.yml',
    iWantTo: 'publishexpire',
    publishDate: '2020-08-05',
    publishTime: '17-59',
    whereToSend: '37fe23d5-588d-4c61-a9ea-70d01e913344'
};

describe('DotWorkflowEventHandlerService', () => {
    let dotWorkflowEventHandlerService: DotWorkflowEventHandlerService;
    let dotWizardService: DotWizardService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let dotGlobalMessageService: DotGlobalMessageService;
    let dotIframeService: DotIframeService;
    let pushPublishService: PushPublishService;
    let dotMessageDisplayService: DotMessageDisplayService;

    const messageServiceMock = new MockDotMessageService({
        'editpage.actions.fire.confirmation': 'The action "{0}" was executed correctly',
        'editpage.actions.fire.error.add.environment': 'place holder text',
        'Workflow-Action': 'Workflow Action'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [RouterTestingModule],
            providers: [
                DotWorkflowEventHandlerService,
                DotMessageDisplayService,
                DotWizardService,
                DotIframeService,
                DotHttpErrorManagerService,
                DotWorkflowActionsFireService,
                DotGlobalMessageService,
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: PushPublishService,
                    useClass: MockPushPublishService
                },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                DotcmsEventsService,
                DotEventsSocket,
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotcmsConfigService,
                { provide: ConnectionBackend, useClass: MockBackend },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: RequestOptions, useClass: BaseRequestOptions },
                Http,
                LoggerService,
                StringUtils,
                DotRouterService,
                DotAlertConfirmService,
                ConfirmationService,
                DotEventsService
            ]
        });

        dotWorkflowEventHandlerService = TestBed.get(DotWorkflowEventHandlerService);
        dotWizardService = TestBed.get(DotWizardService);
        dotWorkflowActionsFireService = TestBed.get(DotWorkflowActionsFireService);
        dotGlobalMessageService = TestBed.get(DotGlobalMessageService);
        dotIframeService = TestBed.get(DotIframeService);
        pushPublishService = TestBed.get(PushPublishService);
        dotMessageDisplayService = TestBed.get(DotMessageDisplayService);
    });

    describe('wizard', () => {
        it('should open with the correct data', () => {
            spyOn(dotWizardService, 'open').and.callThrough();
            dotWorkflowEventHandlerService.open({ ...mockWAEvent });
            expect(dotWizardService.open).toHaveBeenCalledWith(mockWizardInput);
        });
        it('should fire the workflow action with the correct data, execute the callback and send a message on output', () => {
            spyOn(dotWorkflowActionsFireService, 'fireTo').and.returnValue(of({}));

            spyOn(dotGlobalMessageService, 'display');
            spyOn(dotIframeService, 'run');
            dotWorkflowEventHandlerService.open({ ...mockWAEvent });
            dotWizardService.output$({ ...mockWizardOutputData });

            expect(dotWorkflowActionsFireService.fireTo).toHaveBeenCalledWith(
                mockWAEvent.inode,
                mockWAEvent.workflow.id,
                mockWizardOutputTransformedData
            );

            expect(dotGlobalMessageService.display).toHaveBeenCalledWith(
                `The action "${mockWorkflowsActions[0].name}" was executed correctly`
            );

            expect(dotIframeService.run).toHaveBeenCalledWith({ name: mockWAEvent.callback });
        });

        it('should fire BULK action with the correct data, execute the callback and send a message on output', () => {
            const mockBulkResponse: DotActionBulkResult = {
                skippedCount: 1,
                successCount: 2,
                fails: null
            };

            const mockBulkRequest: DotActionBulkRequestOptions = {
                workflowActionId: '44d4d4cd-c812-49db-adb1-1030be73e69a',
                additionalParams: {
                    assignComment: {
                        comment: 'ds',
                        assign: '654b0931-1027-41f7-ad4d-173115ed8ec1'
                    },
                    pushPublish: {
                        whereToSend: '37fe23d5-588d-4c61-a9ea-70d01e913344',
                        iWantTo: 'publishexpire',
                        expireDate: '2020-08-11',
                        expireTime: '19-59',
                        publishDate: '2020-08-05',
                        publishTime: '17-59',
                        filterKey: 'Intelligent.yml'
                    }
                },
                query: 'query'
            };

            spyOn(dotWorkflowActionsFireService, 'bulkFire').and.returnValue(of(mockBulkResponse));

            spyOn(dotGlobalMessageService, 'display');
            spyOn(dotIframeService, 'run');
            dotWorkflowEventHandlerService.open({ ...mockWAEvent, selectedInodes: 'query' });
            dotWizardService.output$({ ...mockWizardOutputData });

            expect(dotWorkflowActionsFireService.bulkFire).toHaveBeenCalledWith(mockBulkRequest);

            expect(dotGlobalMessageService.display).toHaveBeenCalledWith(
                `The action "${mockWorkflowsActions[0].name}" was executed correctly`
            );

            expect(dotIframeService.run).toHaveBeenCalledWith({
                name: mockWAEvent.callback,
                args: [mockBulkResponse]
            });
        });
    });

    describe('checkPublishEnvironments', () => {
        it('should return true if there are environments', () => {
            dotWorkflowEventHandlerService.checkPublishEnvironments().subscribe((flag: boolean) => {
                expect(flag).toEqual(true);
            });
        });
        it('should return false and display a notification is there are no environments ', () => {
            spyOn(pushPublishService, 'getEnvironments').and.returnValue(of([]));
            spyOn(dotMessageDisplayService, 'push');

            dotWorkflowEventHandlerService.checkPublishEnvironments().subscribe((flag: boolean) => {
                expect(flag).toEqual(false);
            });

            expect(dotMessageDisplayService.push).toHaveBeenCalledWith({
                life: 3000,
                message: messageServiceMock.get('editpage.actions.fire.error.add.environment'),
                severity: DotMessageSeverity.ERROR,
                type: DotMessageType.SIMPLE_MESSAGE
            });
        });
    });

    describe('containsPushPublish', () => {
        it('should return true if there are Push Publish inputs', () => {
            expect(
                dotWorkflowEventHandlerService.containsPushPublish(
                    mockWorkflowsActions[0].actionInputs
                )
            ).toEqual(true);
        });
        it('should return false if there are no Push Publish inputs', () => {
            expect(
                dotWorkflowEventHandlerService.containsPushPublish([
                    {
                        body: {},
                        id: 'assignable'
                    }
                ])
            ).toEqual(false);
        });
    });

    it('should set wizard input', () => {
        const input: DotWizardInput = dotWorkflowEventHandlerService.setWizardInput(
            mockWorkflowsActions[0],
            mockWizardInput.title
        );

        expect(input).toEqual(mockWizardInput);
    });

    it('should return only valid Components ', () => {
        const mockWorkflowActions: DotCMSWorkflowAction = {
            ...mockWorkflowsActions[0]
        };
        mockWorkflowActions.actionInputs = [
            {
                body: {},
                id: 'invalidID'
            },
            {
                body: {},
                id: 'invalidID2'
            }
        ];
        const wizardInput: DotWizardInput = dotWorkflowEventHandlerService.setWizardInput(
            mockWorkflowActions,
            'Title Test'
        );
        expect(wizardInput).toEqual(null);
    });

    it('should process workflow payload', () => {
        const data = dotWorkflowEventHandlerService.processWorkflowPayload(
            { ...mockWizardOutputData },
            mockWorkflowsActions[0].actionInputs
        );

        expect(data).toEqual(mockWizardOutputTransformedData);
    });
});
