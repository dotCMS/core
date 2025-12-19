import { describe, it } from '@jest/globals';
import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { Subject, of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { signal } from '@angular/core';

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

import { DotUveWorkflowActionsComponent } from './dot-uve-workflow-actions.component';

import { MOCK_RESPONSE_VTL } from '../../../../../shared/mocks';

// Mock window.matchMedia for PrimeNG components
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: jest.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: jest.fn(),
        removeListener: jest.fn(),
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn()
    }))
});
import { UVEStore } from '../../../../../store/dot-uve.store';

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

const expectedInode = MOCK_RESPONSE_VTL.page.inode;

const messageServiceMock = new MockDotMessageService({
    'Workflow-Action': 'Workflow Action',
    'edit.content.fire.action.success': 'Success',
    'edit.ema.page.error.executing.workflow.action': 'Error',
    'edit.ema.page.executing.workflow.action': 'Executing',
    Loading: 'loading'
});

const pageParams = {
    url: 'test-url',
    language_id: '1'
};

const uveStoreMock = {
    pageAPIResponse: signal(MOCK_RESPONSE_VTL),
    workflowActions: signal([]),
    workflowLoading: signal(false),
    $canEditPage: signal(true),
    pageParams: signal(pageParams),
    loadPageAsset: jest.fn(),
    reloadCurrentPage: jest.fn(),
    setWorkflowActionLoading: jest.fn()
};

describe('DotUveWorkflowActionsComponent', () => {
    let spectator: Spectator<DotUveWorkflowActionsComponent>;
    let dotWizardService: DotWizardService;
    let dotWorkflowEventHandlerService: DotWorkflowEventHandlerService;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let messageService: MessageService;

    let store: InstanceType<typeof UVEStore>;

    const createComponent = createComponentFactory({
        component: DotUveWorkflowActionsComponent,
        imports: [HttpClientTestingModule],
        componentProviders: [
            DotWizardService,
            DotWorkflowEventHandlerService,
            DotWorkflowActionsFireService,
            MessageService,
            mockProvider(UVEStore, uveStoreMock),
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

        store = spectator.inject(UVEStore, true);
        dotWizardService = spectator.inject(DotWizardService, true);
        dotWorkflowEventHandlerService = spectator.inject(DotWorkflowEventHandlerService, true);
        dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService, true);
        messageService = spectator.inject(MessageService, true);
    });

    describe('Without Workflow Actions', () => {
        it('should set action as an empty array and loading to true', () => {
            uveStoreMock.workflowLoading.set(true);
            spectator.detectChanges();

            const dotWorkflowActionsComponent = spectator.query(DotWorkflowActionsComponent);
            expect(dotWorkflowActionsComponent.actions()).toEqual([]);
            expect(dotWorkflowActionsComponent.loading()).toBeTruthy();
            expect(dotWorkflowActionsComponent.size()).toBe('small');
        });

        it("should be disabled if user can't edit", () => {
            uveStoreMock.$canEditPage.set(false);
            spectator.detectChanges();

            const dotWorkflowActionsComponent = spectator.query(DotWorkflowActionsComponent);
            expect(dotWorkflowActionsComponent.disabled()).toBeTruthy();
        });
    });

    describe('With Workflow Actions', () => {
        beforeEach(() => {
            uveStoreMock.workflowLoading.set(false);
            uveStoreMock.$canEditPage.set(true);
            uveStoreMock.workflowActions.set(mockWorkflowsActions);
            spectator.detectChanges();
        });

        it('should load workflow actions', () => {
            const dotWorkflowActionsComponent = spectator.query(DotWorkflowActionsComponent);

            expect(dotWorkflowActionsComponent.actions()).toEqual(mockWorkflowsActions);
            expect(dotWorkflowActionsComponent.loading()).toBeFalsy();
            expect(dotWorkflowActionsComponent.disabled()).toBeFalsy();
        });

        it('should fire workflow actions and loadPageAssets', () => {
            const spySetWorkflowActionLoading = jest.spyOn(store, 'setWorkflowActionLoading');
            const spyLoadPageAsset = jest.spyOn(store, 'loadPageAsset');
            const dotWorkflowActionsComponent = spectator.query(DotWorkflowActionsComponent);
            const spy = jest
                .spyOn(dotWorkflowActionsFireService, 'fireTo')
                .mockReturnValue(of(dotcmsContentletMock));
            const spyMessage = jest.spyOn(messageService, 'add');

            dotWorkflowActionsComponent.actionFired.emit({
                ...mockWorkflowsActions[0],
                actionInputs: []
            });

            expect(spy).toHaveBeenCalledWith({
                inode: expectedInode,
                actionId: mockWorkflowsActions[0].id,
                data: undefined
            });

            expect(spySetWorkflowActionLoading).toHaveBeenCalledWith(true);
            expect(spyLoadPageAsset).toHaveBeenCalledWith({
                language_id: dotcmsContentletMock.languageId.toString(),
                url: '/'
            });
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

        it('should fire workflow actions and reloadPage', () => {
            const spySetWorkflowActionLoading = jest.spyOn(store, 'setWorkflowActionLoading');
            const spyReloadCurrentPage = jest.spyOn(store, 'reloadCurrentPage');
            const dotWorkflowActionsComponent = spectator.query(DotWorkflowActionsComponent);
            const spy = jest
                .spyOn(dotWorkflowActionsFireService, 'fireTo')
                .mockReturnValue(of({ ...dotcmsContentletMock, ...pageParams }));

            dotWorkflowActionsComponent.actionFired.emit({
                ...mockWorkflowsActions[0],
                actionInputs: []
            });

            expect(spy).toHaveBeenCalledWith({
                inode: expectedInode,
                actionId: mockWorkflowsActions[0].id,
                data: undefined
            });

            expect(spySetWorkflowActionLoading).toHaveBeenCalledWith(true);
            expect(spyReloadCurrentPage).toHaveBeenCalledWith();
        });

        it('should open Wizard if it has inputs ', () => {
            const output$ = new Subject<DotWorkflowPayload>();

            const wizardInputMock = {
                steps: [],
                title: 'title'
            };

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
                inode: expectedInode,
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
