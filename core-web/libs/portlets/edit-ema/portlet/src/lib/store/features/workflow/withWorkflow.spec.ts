import { describe, expect } from '@jest/globals';
import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { DotWorkflowsActionsService } from '@dotcms/data-access';
import { DotCMSPageAsset } from '@dotcms/types';
import { mockWorkflowsActions } from '@dotcms/utils-testing';

import { withWorkflow } from './withWorkflow';

import { DotPageApiParams } from '../../../services/dot-page-api.service';
import { PERSONA_KEY } from '../../../shared/consts';
import { UVE_STATUS } from '../../../shared/enums';
import { MOCK_RESPONSE_HEADLESS } from '../../../shared/mocks';
import { UVEState } from '../../models';

const pageParams: DotPageApiParams = {
    url: 'new-url',
    language_id: '1',
    [PERSONA_KEY]: '2'
};

const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    pageAPIResponse: null,
    currentUser: null,
    experiment: null,
    errorCode: null,
    pageParams,
    status: UVE_STATUS.LOADING,
    isTraditionalPage: true,
    canEditPage: false,
    pageIsLocked: true,
    isClientReady: false
};

export const uveStoreMock = signalStore(
    { protectedState: false },
    withState<UVEState>(initialState),
    withWorkflow(),
    withMethods((store) => ({
        setPageAPIResponse: (pageAPIResponse: DotCMSPageAsset) => {
            patchState(store, { pageAPIResponse });
        }
    }))
);

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

    it('should react to the pageAPIResponse', () => {
        store.setPageAPIResponse(MOCK_RESPONSE_HEADLESS);
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
