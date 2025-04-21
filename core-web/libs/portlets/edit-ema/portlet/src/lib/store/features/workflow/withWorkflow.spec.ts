import { describe, expect } from '@jest/globals';
import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { signalStore, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { DotWorkflowsActionsService } from '@dotcms/data-access';
import { mockWorkflowsActions } from '@dotcms/utils-testing';

import { withWorkflow } from './withWorkflow';

import { UVEPageParams } from '../../../services/dot-page-api.service';
import { UVE_STATUS } from '../../../shared/enums';
import { MOCK_RESPONSE_HEADLESS } from '../../../shared/mocks';
import { UVEState } from '../../models';

const pageParams: UVEPageParams = {
    languageId: '1',
    personaId: '2'
};

const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    pageAPIResponse: MOCK_RESPONSE_HEADLESS,
    currentUser: null,
    experiment: null,
    errorCode: null,
    pageParams,
    status: UVE_STATUS.LOADING,
    isTraditionalPage: true
};

export const uveStoreMock = signalStore(withState<UVEState>(initialState), withWorkflow());

describe('withLoad', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;
    let dotWorkflowsActionsService: SpyObject<DotWorkflowsActionsService>;

    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [
            {
                provide: DotWorkflowsActionsService,
                useValue: {
                    getByInode: () => of(mockWorkflowsActions)
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        dotWorkflowsActionsService = spectator.inject(DotWorkflowsActionsService);
    });

    it('should start with the initial state', () => {
        expect(store.workflowActions()).toEqual([]);
        expect(store.workflowLoading()).toBe(true);
    });

    describe('withMethods', () => {
        describe('getWorkflowActions', () => {
            it('should call get workflow actions using the provided inode', () => {
                const spyWorkflowActions = jest.spyOn(dotWorkflowsActionsService, 'getByInode');
                store.getWorkflowActions('123');
                expect(store.workflowLoading()).toBe(false);
                expect(store.workflowActions()).toEqual(mockWorkflowsActions);
                expect(spyWorkflowActions).toHaveBeenCalledWith('123');
            });
        });

        it('should set workflowLoading to true', () => {
            store.setWorkflowActionLoading(true);
            expect(store.workflowLoading()).toBe(true);
        });
    });

    afterEach(() => jest.clearAllMocks());
});
