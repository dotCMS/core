import { SpectatorService, createServiceFactory, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Injectable } from '@angular/core';

import { CoreWebService, DotEventsSocketURL, LoginService } from '@dotcms/dotcms-js';
import {
    DotActionBulkRequestOptions,
    DotActionBulkResult,
    DotCMSWorkflowAction,
    DotCMSWorkflowActionEvent,
    DotMessageSeverity,
    DotMessageType,
    DotProcessedWorkflowPayload,
    DotWizardInput,
    DotWizardStep,
    DotWorkflowPayload
} from '@dotcms/dotcms-models';
import {
    CoreWebServiceMock,
    DotFormatDateServiceMock,
    DotMessageDisplayServiceMock,
    LoginServiceMock,
    MockDotMessageService,
    dotcmsContentletMock,
    mockWorkflowsActions
} from '@dotcms/utils-testing';

import { DotWorkflowEventHandlerService } from './dot-workflow-event-handler.service';

import { DotFormatDateService } from '../dot-format-date/dot-format-date.service';
import { DotGlobalMessageService } from '../dot-global-message/dot-global-message.service';
import { DotHttpErrorManagerService } from '../dot-http-error-manager/dot-http-error-manager.service';
import { DotIframeService } from '../dot-iframe/dot-iframe.service';
import { DotMessageDisplayService } from '../dot-message-display/dot-message-display.service';
import { DotMessageService } from '../dot-messages/dot-messages.service';
import { DotWizardService } from '../dot-wizard/dot-wizard.service';
import { DotWorkflowActionsFireService } from '../dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { PushPublishService } from '../push-publish/push-publish.service';

const dotEventSocketURLFactory = () => {
    return new DotEventsSocketURL(
        `${window.location.hostname}:${window.location.port}/api/ws/v1/system/events`,
        window.location.protocol === 'https:'
    );
};

@Injectable()
export class MockPushPublishService {
    getEnvironments() {
        return of([
            {
                id: '123',
                name: 'Environment 1'
            },
            {
                id: '456',
                name: 'Environment 2'
            }
        ]);
    }
}

const mockWAEvent: DotCMSWorkflowActionEvent = {
    workflow: mockWorkflowsActions[0],
    callback: 'angularWorkflowEventCallback',
    inode: '123Inode',
    selectedInodes: []
};

const mockWizardSteps: DotWizardStep[] = [
    {
        component: 'commentAndAssign',
        data: {
            assignable: true,
            commentable: true,
            moveable: true,
            roleId: mockWorkflowsActions[0].nextAssign,
            roleHierarchy: mockWorkflowsActions[0].roleHierarchyForAssign
        }
    },
    {
        component: 'pushPublish',
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

const messageServiceMock = new MockDotMessageService({
    'editpage.actions.fire.confirmation': 'The action "{0}" was executed correctly',
    'editpage.actions.fire.error.add.environment': 'place holder text',
    'Workflow-Action': 'Workflow Action'
});

describe('DotWorkflowEventHandlerService', () => {
    let spectator: SpectatorService<DotWorkflowEventHandlerService>;
    let dotIframeService: DotIframeService;
    let dotWizardService: DotWizardService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let dotGlobalMessageService: DotGlobalMessageService;
    let pushPublishService: PushPublishService;
    let dotMessageDisplayService: DotMessageDisplayService;

    const createService = createServiceFactory({
        service: DotWorkflowEventHandlerService,
        providers: [
            DotWizardService,
            mockProvider(DotIframeService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(DotGlobalMessageService),
            {
                provide: DotMessageDisplayService,
                useClass: DotMessageDisplayServiceMock
            },
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
            { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            { provide: DotFormatDateService, useClass: DotFormatDateServiceMock }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        dotIframeService = spectator.inject(DotIframeService);
        dotWizardService = spectator.inject(DotWizardService);
        dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
        dotGlobalMessageService = spectator.inject(DotGlobalMessageService);
        pushPublishService = spectator.inject(PushPublishService);
        dotMessageDisplayService = spectator.inject(DotMessageDisplayService);
    });

    it('should set wizard input', () => {
        const input: DotWizardInput = spectator.service.setWizardInput(
            mockWorkflowsActions[0],
            mockWizardInput.title
        ) as DotWizardInput;

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
        const wizardInput = spectator.service.setWizardInput(mockWorkflowActions, 'Title Test');
        expect(wizardInput).toEqual(null);
    });

    it('should process workflow payload', () => {
        const data = spectator.service.processWorkflowPayload(
            { ...mockWizardOutputData },
            mockWorkflowsActions[0].actionInputs
        );

        expect(data).toEqual(mockWizardOutputTransformedData);
    });

    describe('wizard', () => {
        it('should open with the correct data', () => {
            jest.spyOn(dotWizardService, 'open');
            spectator.service.open({ ...mockWAEvent });
            expect(dotWizardService.open).toHaveBeenCalledWith(mockWizardInput);
        });

        it('should fire the workflow action with the correct data, execute the callback and send a message on output', () => {
            jest.spyOn(dotWorkflowActionsFireService, 'fireTo').mockReturnValue(
                of(dotcmsContentletMock)
            );

            jest.spyOn(dotGlobalMessageService, 'display');
            jest.spyOn(dotIframeService, 'run');

            spectator.service.open({ ...mockWAEvent });
            dotWizardService.output$({ ...mockWizardOutputData });

            expect(dotWorkflowActionsFireService.fireTo).toHaveBeenCalledWith({
                inode: mockWAEvent.inode,
                actionId: mockWAEvent.workflow.id,
                data: mockWizardOutputTransformedData
            });

            expect(dotGlobalMessageService.display).toHaveBeenCalledWith(
                `The action "${mockWorkflowsActions[0].name}" was executed correctly`
            );
            expect(dotIframeService.run).toHaveBeenCalledWith({
                name: mockWAEvent.callback
            });
        });

        it('should run iframe function for legacy call', () => {
            jest.spyOn(dotIframeService, 'run');
            spectator.service.open({
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
                fails: []
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

            jest.spyOn(dotWorkflowActionsFireService, 'bulkFire').mockReturnValue(
                of(mockBulkResponse)
            );

            jest.spyOn(dotGlobalMessageService, 'display');
            jest.spyOn(dotIframeService, 'run');
            spectator.service.open({
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
            spectator.service.checkPublishEnvironments().subscribe((flag: boolean) => {
                expect(flag).toEqual(true);
            });
        });
        it('should return false and display a notification is there are no environments ', () => {
            jest.spyOn(pushPublishService, 'getEnvironments').mockReturnValue(of([]));
            jest.spyOn(dotMessageDisplayService, 'push');

            spectator.service.checkPublishEnvironments().subscribe((flag: boolean) => {
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
                spectator.service.containsPushPublish(mockWorkflowsActions[0].actionInputs)
            ).toEqual(true);
        });
        it('should return false if there are no Push Publish inputs', () => {
            expect(
                spectator.service.containsPushPublish([
                    {
                        body: {},
                        id: 'assignable'
                    }
                ])
            ).toEqual(false);
        });
    });
});
