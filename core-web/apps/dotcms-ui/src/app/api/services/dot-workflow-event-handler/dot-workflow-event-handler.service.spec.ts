/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';

import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotCommentAndAssignFormComponent } from '@components/_common/forms/dot-comment-and-assign-form/dot-comment-and-assign-form.component';
import { DotPushPublishFormComponent } from '@components/_common/forms/dot-push-publish-form/dot-push-publish-form.component';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotWizardService } from '@dotcms/app/api/services/dot-wizard/dot-wizard.service';
import { DotWorkflowEventHandlerService } from '@dotcms/app/api/services/dot-workflow-event-handler/dot-workflow-event-handler.service';
import { PushPublishService } from '@dotcms/app/api/services/push-publish/push-publish.service';
import { dotEventSocketURLFactory } from '@dotcms/app/test/dot-test-bed';
import {
    DotAlertConfirmService,
    DotEventsService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import {
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils
} from '@dotcms/dotcms-js';
import {
    DotActionBulkRequestOptions,
    DotActionBulkResult,
    DotCMSWorkflowAction,
    DotCMSWorkflowActionEvent,
    DotMessageSeverity,
    DotMessageType,
    DotProcessedWorkflowPayload,
    DotWorkflowPayload
} from '@dotcms/dotcms-models';
import { DotFormatDateService } from '@dotcms/ui';
import {
    CoreWebServiceMock,
    DotFormatDateServiceMock,
    DotMessageDisplayServiceMock,
    LoginServiceMock,
    MockDotMessageService,
    mockWorkflowsActions
} from '@dotcms/utils-testing';
import { DotWizardInput } from '@models/dot-wizard-input/dot-wizard-input.model';
import { DotWizardStep } from '@models/dot-wizard-step/dot-wizard-step.model';
import { MockPushPublishService } from '@portlets/shared/dot-content-types-listing/dot-content-types.component.spec';

const mockWAEvent: DotCMSWorkflowActionEvent = {
    workflow: mockWorkflowsActions[0],
    callback: 'angularWorkflowEventCallback',
    inode: '123Inode',
    selectedInodes: []
};

const mockWizardSteps: DotWizardStep<any>[] = [
    {
        component: DotCommentAndAssignFormComponent,
        data: {
            assignable: true,
            commentable: true,
            moveable: true,
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

const mockWizardOutputData: DotWorkflowPayload = {
    assign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
    comments: 'ds',
    pathToMove: '/test/',
    environment: ['37fe23d5-588d-4c61-a9ea-70d01e913344'],
    expireDate: '2020-08-11 19:59',
    filterKey: 'Intelligent.yml',
    publishDate: '2020-08-05 17:59',
    pushActionSelected: 'publishexpire',
    timezoneId: 'America/Costa_Rica'
};

const mockWizardOutputTransformedData: DotProcessedWorkflowPayload = {
    assign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
    comments: 'ds',
    expireDate: '2020-08-11',
    expireTime: '19-59',
    filterKey: 'Intelligent.yml',
    iWantTo: 'publishexpire',
    publishDate: '2020-08-05',
    publishTime: '17-59',
    whereToSend: '37fe23d5-588d-4c61-a9ea-70d01e913344',
    timezoneId: 'America/Costa_Rica',
    pathToMove: '/test/',
    contentlet: {}
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
            imports: [RouterTestingModule, HttpClientTestingModule],
            providers: [
                DotWorkflowEventHandlerService,
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
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
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                LoggerService,
                StringUtils,
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
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
            spyOn<any>(dotWorkflowActionsFireService, 'fireTo').and.returnValue(of({}));

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
            expect(dotIframeService.run).toHaveBeenCalledWith({
                name: mockWAEvent.callback
            });
        });

        it('should run iframe function for legacy call', () => {
            spyOn(dotIframeService, 'run');
            dotWorkflowEventHandlerService.open({
                ...mockWAEvent,
                callback: 'saveAssignCallBackAngular'
            });
            dotWizardService.output$({ ...mockWizardOutputData });

            expect(dotIframeService.run).toHaveBeenCalledWith({
                name: 'saveAssignCallBackAngular',
                args: [mockWAEvent.workflow.id, mockWizardOutputTransformedData]
            });
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
                        comment: mockWizardOutputData.comments,
                        assign: mockWizardOutputData.assign
                    },
                    pushPublish: {
                        whereToSend: '37fe23d5-588d-4c61-a9ea-70d01e913344',
                        iWantTo: 'publishexpire',
                        expireDate: '2020-08-11',
                        expireTime: '19-59',
                        publishDate: '2020-08-05',
                        publishTime: '17-59',
                        filterKey: 'Intelligent.yml',
                        timezoneId: 'America/Costa_Rica'
                    },
                    additionalParamsMap: {
                        _path_to_move: mockWizardOutputData.pathToMove
                    }
                },
                query: 'query'
            };

            spyOn(dotWorkflowActionsFireService, 'bulkFire').and.returnValue(of(mockBulkResponse));

            spyOn(dotGlobalMessageService, 'display');
            spyOn(dotIframeService, 'run');
            dotWorkflowEventHandlerService.open({
                ...mockWAEvent,
                selectedInodes: 'query'
            });
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
