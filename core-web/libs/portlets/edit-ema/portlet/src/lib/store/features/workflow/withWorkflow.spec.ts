import { describe, expect } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { signalStore, withMethods, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import {
    DotContentletLockerService,
    DotLanguagesService,
    DotPropertiesService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotCMSPageAsset } from '@dotcms/types';
import { DotLanguagesServiceMock, mockWorkflowsActions } from '@dotcms/utils-testing';

import { withWorkflow } from './withWorkflow';

import { DotPageApiService } from '../../../services/dot-page-api.service';
import { PERSONA_KEY } from '../../../shared/consts';
import { MOCK_RESPONSE_HEADLESS } from '../../../shared/mocks';
import { UVEState } from '../../models';
import { createInitialUVEState } from '../../testing/mocks';
import { withFlags } from '../flags/withFlags';
import { withPage } from '../page/withPage';

const pageParams = {
    url: 'new-url',
    language_id: '1',
    [PERSONA_KEY]: '2'
};

const initialState = createInitialUVEState({ pageParams });

export const uveStoreMock = signalStore(
    { protectedState: false },
    withState<UVEState>(initialState),
    withFlags([]),
    withPage(),
    withWorkflow(),
    withMethods((store) => ({
        setPageAPIResponse: (pageAssetResponse: DotCMSPageAsset) => {
            store.setPageAssetResponse({ pageAsset: pageAssetResponse });
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
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of(false))
            }),
            {
                provide: DotPageApiService,
                useValue: {
                    get: () => of({}),
                    getClientPage: () => of({}),
                    getGraphQLPage: () => of({}),
                    save: jest.fn()
                }
            },
            {
                provide: DotWorkflowsActionsService,
                useValue: {
                    getByInode: jest.fn().mockReturnValue(of(mockWorkflowsActions))
                }
            },
            {
                provide: DotContentletLockerService,
                useValue: { unlock: () => of({}), lock: () => of({}) }
            },
            { provide: DotLanguagesService, useValue: new DotLanguagesServiceMock() }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        dotWorkflowsActionsService = spectator.inject(DotWorkflowsActionsService);
    });

    it('should start with the initial state', () => {
        expect(store.workflowActions()).toEqual([]);
        expect(store.workflowIsLoading()).toBe(true);
    });

    it('should react to the pageAssetResponse', () => {
        store.setPageAPIResponse(MOCK_RESPONSE_HEADLESS);
        expect(store.workflowActions()).toEqual([]);
        expect(store.workflowIsLoading()).toBe(true);
    });

    describe('withMethods', () => {
        describe('workflowFetch', () => {
            it('should call get workflow actions using the provided inode', () => {
                const spyWorkflowActions = jest.spyOn(dotWorkflowsActionsService, 'getByInode');
                store.workflowFetch('123');
                expect(store.workflowIsLoading()).toBe(false);
                expect(store.workflowActions()).toEqual(mockWorkflowsActions);
                expect(spyWorkflowActions).toHaveBeenCalledWith('123');
            });
        });

        it('should set workflowIsLoading to true', () => {
            store.setWorkflowActionLoading(true);
            expect(store.workflowIsLoading()).toBe(true);
        });
    });

    afterEach(() => jest.clearAllMocks());
});
