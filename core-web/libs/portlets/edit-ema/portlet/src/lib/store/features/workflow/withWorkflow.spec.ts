import { describe, expect, it } from '@jest/globals';
import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

import {
    DotContentletLockerService,
    DotLanguagesService,
    DotPropertiesService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { DotCMSPageAsset, UVE_MODE } from '@dotcms/types';
import { DotLanguagesServiceMock, mockWorkflowsActions } from '@dotcms/utils-testing';

import { withWorkflow } from './withWorkflow';

import { DotPageApiService } from '../../../services/dot-page-api.service';
import { PERSONA_KEY } from '../../../shared/consts';
import { MOCK_RESPONSE_HEADLESS, mockCurrentUser } from '../../../shared/mocks';
import { UVEState } from '../../models';
import { createInitialUVEState } from '../../testing/mocks';
import { withFlags } from '../flags/withFlags';
import { withPage } from '../page/withPage';

const pageParams = {
    url: 'new-url',
    language_id: '1',
    [PERSONA_KEY]: '2',
    mode: UVE_MODE.EDIT
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
            store.setPageAsset({ pageAsset: pageAssetResponse });
        }
    }))
);

describe('withLoad', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;
    let dotWorkflowsActionsService: SpyObject<DotWorkflowsActionsService>;
    let dotContentletLockerService: SpyObject<DotContentletLockerService>;

    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of({}))
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
                useValue: {
                    unlock: jest.fn().mockReturnValue(of({})),
                    lock: jest.fn().mockReturnValue(of({}))
                }
            },
            { provide: DotLanguagesService, useValue: new DotLanguagesServiceMock() }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        dotWorkflowsActionsService = spectator.inject(DotWorkflowsActionsService);
        dotContentletLockerService = spectator.inject(DotContentletLockerService);
    });

    it('should start with the initial state', () => {
        expect(store.workflowActions()).toEqual([]);
        expect(store.workflowIsLoading()).toBe(true);
        expect(store.workflowLockIsLoading()).toBe(false);
    });

    it('should fetch workflow actions when page asset inode changes (effect)', () => {
        const getByInodeSpy = jest.spyOn(dotWorkflowsActionsService, 'getByInode');
        store.setPageAPIResponse(MOCK_RESPONSE_HEADLESS);
        spectator.flushEffects();
        expect(getByInodeSpy).toHaveBeenCalledWith(MOCK_RESPONSE_HEADLESS.page.inode);
        expect(store.workflowActions()).toEqual(mockWorkflowsActions);
        expect(store.workflowIsLoading()).toBe(false);
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

        describe('workflowToggleLock', () => {
            it('should call lock when page is not locked', () => {
                const inode = 'page-inode-123';
                store.setPageAPIResponse(MOCK_RESPONSE_HEADLESS);
                spectator.flushEffects();

                store.workflowToggleLock(inode, false, false);

                expect(dotContentletLockerService.lock).toHaveBeenCalledWith(inode);
                expect(dotContentletLockerService.unlock).not.toHaveBeenCalled();
            });

            it('should call unlock when page is locked by current user', () => {
                const inode = 'page-inode-123';
                store.setPageAPIResponse(MOCK_RESPONSE_HEADLESS);
                spectator.flushEffects();

                store.workflowToggleLock(inode, true, true);

                expect(dotContentletLockerService.unlock).toHaveBeenCalledWith(inode);
                expect(dotContentletLockerService.lock).not.toHaveBeenCalled();
            });

            it('should call unlock when page is locked by another user', () => {
                const inode = 'page-inode-123';
                store.setPageAPIResponse(MOCK_RESPONSE_HEADLESS);
                spectator.flushEffects();

                store.workflowToggleLock(inode, true, false);

                expect(dotContentletLockerService.unlock).toHaveBeenCalledWith(inode);
                expect(dotContentletLockerService.lock).not.toHaveBeenCalled();
            });
        });
    });

    describe('computed signals', () => {
        it('should expose $lockIsPageLocked from page and user state', () => {
            expect(store.$lockIsPageLocked()).toBe(false);

            store.setPageAPIResponse(MOCK_RESPONSE_HEADLESS);
            spectator.flushEffects();
            // MOCK_RESPONSE_HEADLESS has locked: false
            expect(store.$lockIsPageLocked()).toBe(false);
        });

        it('should expose $lockFeatureEnabled from flags', () => {
            // Flags are loaded from DotPropertiesService; empty object => no toggle lock flag
            expect(store.$lockFeatureEnabled()).toBeFalsy();
        });

        it('should expose $lockOptions when page is set and mode is EDIT', () => {
            store.setPageAPIResponse(MOCK_RESPONSE_HEADLESS);
            spectator.flushEffects();

            const options = store.$lockOptions();
            expect(options).not.toBeNull();
            expect(options?.inode).toBe(MOCK_RESPONSE_HEADLESS.page.inode);
            expect(options?.isLocked).toBe(false);
            expect(options?.canLock).toBe(true);
            expect(options?.lockedBy).toBe('');
        });

        describe('$lockOptions.shouldShowButton', () => {
            it('should be false when page is not locked and feature flag is disabled', () => {
                store.setPageAPIResponse(MOCK_RESPONSE_HEADLESS);
                spectator.flushEffects();

                expect(store.$lockOptions()?.shouldShowButton).toBe(false);
            });

            it('should be true when feature flag is enabled regardless of lock state', () => {
                patchState(store, { flags: { FEATURE_FLAG_UVE_TOGGLE_LOCK: true } });
                store.setPageAPIResponse(MOCK_RESPONSE_HEADLESS);
                spectator.flushEffects();

                expect(store.$lockOptions()?.shouldShowButton).toBe(true);
            });

            it('should be true when page is locked by the current user (feature flag disabled)', () => {
                patchState(store, { uveCurrentUser: mockCurrentUser });
                store.setPageAPIResponse({
                    ...MOCK_RESPONSE_HEADLESS,
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        locked: true,
                        lockedBy: mockCurrentUser.userId,
                        lockedByName: mockCurrentUser.givenName,
                        canLock: true
                    }
                });
                spectator.flushEffects();

                expect(store.$lockOptions()?.shouldShowButton).toBe(true);
            });

            it('should be true when page is locked and canLock is true (admin can unlock, feature flag disabled)', () => {
                store.setPageAPIResponse({
                    ...MOCK_RESPONSE_HEADLESS,
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        locked: true,
                        lockedBy: 'another-user',
                        lockedByName: 'Another User',
                        canLock: true
                    }
                });
                spectator.flushEffects();

                expect(store.$lockOptions()?.shouldShowButton).toBe(true);
            });

            it('should be false when page is locked by another user with no canLock (feature flag disabled)', () => {
                store.setPageAPIResponse({
                    ...MOCK_RESPONSE_HEADLESS,
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        locked: true,
                        lockedBy: 'another-user',
                        lockedByName: 'Another User',
                        canLock: false
                    }
                });
                spectator.flushEffects();

                expect(store.$lockOptions()?.shouldShowButton).toBe(false);
            });
        });
    });

    afterEach(() => jest.clearAllMocks());
});
