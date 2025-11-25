import { describe, it } from '@jest/globals';
import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { Subject, of } from 'rxjs';

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
import { DotProcessedWorkflowPayload, DotWorkflowPayload } from '@dotcms/dotcms-models';
import { DotWorkflowActionsComponent } from '@dotcms/ui';
import {
    LoginServiceMock,
    MockDotMessageService,
    dotcmsContentletMock,
    mockWorkflowsActions
} from '@dotcms/utils-testing';

import { DotEditEmaWorkflowActionsComponent } from './dot-edit-ema-workflow-actions.component';

const DOT_WORKFLOW_PAYLOAD_MOCK: DotWorkflowPayload = {
    assign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
    comments: 'ds',
    pathToMove: '/test/',
    environment: ['37fe23d5-588d-4c61-a9ea-70d01e913344'],
    expireDate: '2020-08-11 19:59',
    filterKey: 'Intelligent.yml',
    publishDate: '2020-08-05 17:59',
    pushActionSelected: 'publishexpire',
    timezoneId: 'America/New_York'
};

const DOT_PROCESSED_WORKFLOW_PAYLOAD_MOCK: DotProcessedWorkflowPayload = {
    assign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
    comments: 'ds',
    pathToMove: '/test/',
    environment: ['37fe23d5-588d-4c61-a9ea-70d01e913344'],
    expireDate: '2020-08-11 19:59',
    filterKey: 'Intelligent.yml',
    publishDate: '2020-08-05 17:59',
    pushActionSelected: 'publishexpire',
    timezoneId: 'America/New_York'
};

const workflowActionMock = {
    ...mockWorkflowsActions[0],
    actionInputs: [
        {
            id: '1232',
            body: []
        }
    ]
};

const messageServiceMock = new MockDotMessageService({
    'Workflow-Action': 'Workflow Action',
    'edit.content.fire.action.success': 'Success',
    'edit.ema.page.error.executing.workflow.action': 'Error',
    'edit.ema.page.executing.workflow.action': 'Executing',
    Loading: 'loading'
});

describe('DotEditEmaWorkflowActionsComponent', () => {
    let spectator: Spectator<DotEditEmaWorkflowActionsComponent>;
    let dotWizardService: DotWizardService;
    let dotWorkflowsActionsService: DotWorkflowsActionsService;
    let dotWorkflowEventHandlerService: DotWorkflowEventHandlerService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let messageService: MessageService;

    const createComponent = createComponentFactory({
        component: DotEditEmaWorkflowActionsComponent,
        imports: [HttpClientTestingModule],
        componentProviders: [
            DotWizardService,
            DotWorkflowsActionsService,
            DotWorkflowEventHandlerService,
            DotWorkflowActionsFireService,
            MessageService,
            mockProvider(DotAlertConfirmService),
            mockProvider(DotMessageDisplayService),
            mockProvider(DotHttpErrorManagerService),
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
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        dotWizardService = spectator.inject(DotWizardService, true);
        dotWorkflowsActionsService = spectator.inject(DotWorkflowsActionsService, true);
        dotWorkflowEventHandlerService = spectator.inject(DotWorkflowEventHandlerService, true);
        dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService, true);
        messageService = spectator.inject(MessageService, true);
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('Without Workflow Actions', () => {
        beforeEach(() => {
            spectator.setInput('inode', '123');
            spectator.detectChanges();
        });

        it('should set action as an empty array and loading to true', () => {
            const dotWorkflowActionsComponent = spectator.query(DotWorkflowActionsComponent);
            expect(dotWorkflowActionsComponent.actions()).toEqual([]);
            expect(dotWorkflowActionsComponent.loading()).toBeTruthy();
            expect(dotWorkflowActionsComponent.size()).toBe('small');
        });
    });

    describe('With Workflow Actions', () => {
        beforeEach(() => {
            jest.spyOn(dotWorkflowsActionsService, 'getByInode').mockReturnValue(
                of(mockWorkflowsActions)
            );

            spectator.setInput('inode', '123');
            spectator.detectChanges();
        });

        it('should load workflow actions', () => {
            const dotWorkflowActionsComponent = spectator.query(DotWorkflowActionsComponent);

            expect(dotWorkflowsActionsService.getByInode).toHaveBeenCalledWith('123');
            expect(dotWorkflowActionsComponent.actions()).toEqual(mockWorkflowsActions);
        });

        it('should fire workflow actions when it does not have inputs', () => {
            jest.spyOn(dotWorkflowEventHandlerService, 'containsPushPublish').mockReturnValue(
                false
            );
            const dotWorkflowActionsComponent = spectator.query(DotWorkflowActionsComponent);
            const spy = jest
                .spyOn(dotWorkflowActionsFireService, 'fireTo')
                .mockReturnValue(of(dotcmsContentletMock));
            const spyNewPage = jest.spyOn(spectator.component.newPage, 'emit');
            const spyMessage = jest.spyOn(messageService, 'add');

            dotWorkflowActionsComponent.actionFired.emit({
                ...mockWorkflowsActions[0],
                actionInputs: []
            });

            expect(spy).toHaveBeenCalledWith({
                inode: '123',
                actionId: mockWorkflowsActions[0].id,
                data: undefined
            });

            expect(spyNewPage).toHaveBeenCalledWith(dotcmsContentletMock);
            expect(dotWorkflowsActionsService.getByInode).toHaveBeenCalledWith(
                dotcmsContentletMock.inode
            );

            expect(spyMessage).toHaveBeenCalledTimes(2);

            // Check the first message
            expect(spyMessage).toHaveBeenCalledWith({
                severity: 'info',
                summary: 'Workflow Action',
                detail: 'Executing',
                life: 1000
            });
            // Check the second message
            expect(spyMessage).toHaveBeenCalledWith({
                severity: 'info',
                summary: 'Workflow Action',
                detail: 'Success',
                life: 2000
            });
        });

        it('should open Wizard if it has inputs ', () => {
            const output$ = new Subject<DotWorkflowPayload>();

            const wizardInputMock = {
                steps: [],
                title: 'title'
            };

            jest.spyOn(dotWorkflowEventHandlerService, 'containsPushPublish').mockReturnValue(
                false
            );
            jest.spyOn(dotWorkflowEventHandlerService, 'setWizardInput').mockReturnValue(
                wizardInputMock
            );
            const spyProcessWorkflowPayload = jest
                .spyOn(dotWorkflowEventHandlerService, 'processWorkflowPayload')
                .mockReturnValue(DOT_PROCESSED_WORKFLOW_PAYLOAD_MOCK);
            const spyWizard = jest.spyOn(dotWizardService, 'open').mockImplementation(() => {
                return output$.asObservable();
            });
            const spyFireTo = jest
                .spyOn(dotWorkflowActionsFireService, 'fireTo')
                .mockReturnValue(of(dotcmsContentletMock));

            const dotWorkflowActionsComponent = spectator.query(DotWorkflowActionsComponent);
            dotWorkflowActionsComponent.actionFired.emit(workflowActionMock);

            expect(spyWizard).toHaveBeenCalledWith(wizardInputMock);

            // Close the wizard
            output$.next(DOT_WORKFLOW_PAYLOAD_MOCK);

            expect(spyProcessWorkflowPayload).toHaveBeenCalledWith(
                DOT_WORKFLOW_PAYLOAD_MOCK,
                workflowActionMock.actionInputs
            );
            expect(spyFireTo).toHaveBeenCalledWith({
                inode: '123',
                actionId: workflowActionMock.id,
                data: DOT_PROCESSED_WORKFLOW_PAYLOAD_MOCK
            });
        });

        it('should check Publish Environments and open wizard component if it has Enviroments ', () => {
            jest.spyOn(dotWorkflowEventHandlerService, 'containsPushPublish').mockReturnValue(true);
            const spyCheckPublishEnvironments = jest
                .spyOn(dotWorkflowEventHandlerService, 'checkPublishEnvironments')
                .mockReturnValue(of(true));
            const spyWizard = jest.spyOn(dotWizardService, 'open');

            const dotWorkflowActionsComponent = spectator.query(DotWorkflowActionsComponent);

            dotWorkflowActionsComponent.actionFired.emit(workflowActionMock);

            expect(spyCheckPublishEnvironments).toHaveBeenCalled();
            expect(spyWizard).toHaveBeenCalled();
        });

        it('should check Publish Environments and do not open wizard component if it do not has Enviroments ', () => {
            jest.spyOn(dotWorkflowEventHandlerService, 'containsPushPublish').mockReturnValue(true);
            const spyCheckPublishEnvironments = jest
                .spyOn(dotWorkflowEventHandlerService, 'checkPublishEnvironments')
                .mockReturnValue(of(false));
            const spyWizard = jest.spyOn(dotWizardService, 'open');

            const dotWorkflowActionsComponent = spectator.query(DotWorkflowActionsComponent);

            dotWorkflowActionsComponent.actionFired.emit(workflowActionMock);

            expect(spyCheckPublishEnvironments).toHaveBeenCalled();
            expect(spyWizard).not.toHaveBeenCalled();
        });
    });
});
