import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Injectable } from '@angular/core';

import { MessageService } from 'primeng/api';

import {
    DotWizardService,
    DotWorkflowActionsFireService,
    PushPublishService,
    DotMessageService,
    DotFormatDateService
} from '@dotcms/data-access';
import {
    DotEventsSocketURL,
    LoginService,
    CoreWebService,
    CoreWebServiceMock
} from '@dotcms/dotcms-js';
import {
    DotCMSWorkflowActionEvent,
    DotWizardStep,
    DotWizardInput,
    DotWorkflowPayload,
    DotProcessedWorkflowPayload,
    DotCMSWorkflowAction,
    DotActionBulkResult,
    DotActionBulkRequestOptions
} from '@dotcms/dotcms-models';
import {
    mockWorkflowsActions,
    MockDotMessageService,
    LoginServiceMock,
    DotFormatDateServiceMock,
    dotcmsContentletMock
} from '@dotcms/utils-testing';

import { DotEmaWorkflowActionsService } from './dot-ema-workflow-actions.service';

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

describe('DotEmaWorkflowActionsService', () => {
    let spectator: SpectatorService<DotEmaWorkflowActionsService>;
    let dotWizardService: DotWizardService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let pushPublishService: PushPublishService;
    let messageService: MessageService;

    const createService = createServiceFactory({
        service: DotEmaWorkflowActionsService,
        providers: [
            DotWizardService,
            mockProvider(DotWorkflowActionsFireService),
            MessageService,
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
        dotWizardService = spectator.inject(DotWizardService);
        dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
        pushPublishService = spectator.inject(PushPublishService);
        messageService = spectator.inject(MessageService);
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

    describe('handleWorkflowAction', () => {
        it('should open with the correct data', (done) => {
            jest.spyOn(dotWizardService, 'open');
            jest.spyOn(dotWorkflowActionsFireService, 'fireTo').mockReturnValue(
                of(dotcmsContentletMock)
            );
            spectator.service.handleWorkflowAction({ ...mockWAEvent }).subscribe(() => {
                expect(dotWizardService.open).toHaveBeenCalledWith(mockWizardInput);

                done();
            });
            dotWizardService.output$({ ...mockWizardOutputData });
        });

        it('should fire the workflow action with the correct data, execute the callback and send a message on output', (done) => {
            jest.spyOn(dotWorkflowActionsFireService, 'fireTo').mockReturnValue(
                of(dotcmsContentletMock)
            );

            spectator.service.handleWorkflowAction({ ...mockWAEvent }).subscribe(() => {
                expect(dotWorkflowActionsFireService.fireTo).toHaveBeenCalledWith({
                    inode: mockWAEvent.inode,
                    actionId: mockWAEvent.workflow.id,
                    data: mockWizardOutputTransformedData
                });
                done();
            });
            dotWizardService.output$({ ...mockWizardOutputData });
        });

        it('should fire BULK action with the correct data, execute the callback and send a message on output', (done) => {
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

            const embeddedFunction = jest.fn();

            spectator.service
                .handleWorkflowAction(
                    {
                        ...mockWAEvent,
                        selectedInodes: 'query'
                    },
                    embeddedFunction
                )
                .subscribe(() => {
                    expect(dotWorkflowActionsFireService.bulkFire).toHaveBeenCalledWith(
                        mockBulkRequest
                    );

                    expect(embeddedFunction).toHaveBeenCalledWith('fireActionLoadingIndicator', []);
                    done();
                });
            dotWizardService.output$({ ...mockWizardOutputData });
        });
    });

    describe('checkPublishEnvironments', () => {
        it('should return true if there are environments', () => {
            spectator.service.checkPublishEnvironments().subscribe((flag: boolean) => {
                expect(flag).toEqual(true);
            });
        });
        it('should return false and display a notification is there are no environments ', (done) => {
            jest.spyOn(pushPublishService, 'getEnvironments').mockReturnValue(of([]));
            jest.spyOn(messageService, 'add');

            spectator.service.checkPublishEnvironments().subscribe((flag: boolean) => {
                expect(flag).toEqual(false);
                expect(messageService.add).toHaveBeenCalledWith({
                    life: 3000,
                    detail: 'publisher_dialog_environment_mandatory',
                    summary: 'Workflow Action',
                    severity: 'error'
                });
                done();
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
