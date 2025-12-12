import { describe, expect, it } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { patchState, signalStore, withState } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { ConfirmationService, MessageService } from 'primeng/api';

import {
    DotContentletLockerService,
    DotContentletLockResponse,
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotPropertiesService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import {
    CurrentUserDataMock,
    DotLanguagesServiceMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { withLock } from './withLock';

import { DotPageApiService } from '../../../services/dot-page-api.service';
import { dotPropertiesServiceMock, MOCK_RESPONSE_HEADLESS } from '../../../shared/mocks';
import { UVEState } from '../../models';
import { withLoad } from '../load/withLoad';

const mockLockResponse: DotContentletLockResponse = {
    id: 'test-id',
    inode: 'test-inode',
    message: 'success'
};

const initialState: UVEState = {
    isEnterprise: false,
    languages: [],
    pageAPIResponse: MOCK_RESPONSE_HEADLESS,
    currentUser: null,
    experiment: null,
    errorCode: null,
    pageParams: null,
    status: null,
    isTraditionalPage: true,
    isClientReady: false
};

export const uveStoreMock = signalStore(
    { protectedState: false },
    withState<UVEState>(initialState),
    withLoad(),
    withLock()
);

describe('withLock', () => {
    let spectator: SpectatorService<InstanceType<typeof uveStoreMock>>;
    let store: InstanceType<typeof uveStoreMock>;
    let dotContentletLockerService: DotContentletLockerService;
    let messageService: MessageService;
    let confirmationService: ConfirmationService;

    const createService = createServiceFactory({
        service: uveStoreMock,
        providers: [
            MessageService,
            ConfirmationService,
            {
                provide: DotPageApiService,
                useValue: {
                    get: jest.fn().mockReturnValue(of(MOCK_RESPONSE_HEADLESS)),
                    getGraphQLPage: jest.fn().mockReturnValue(of(MOCK_RESPONSE_HEADLESS))
                }
            },
            {
                provide: DotContentletLockerService,
                useValue: {
                    lock: jest.fn().mockReturnValue(of(mockLockResponse)),
                    unlock: jest.fn().mockReturnValue(of(mockLockResponse))
                }
            },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'edit.ema.page.lock': 'Lock',
                    'edit.ema.page.lock.success': 'Page locked successfully',
                    'edit.ema.page.lock.error': 'Error locking page',
                    'edit.ema.page.unlock': 'Unlock',
                    'edit.ema.page.unlock.success': 'Page unlocked successfully',
                    'edit.ema.page.unlock.error': 'Error unlocking page',
                    'uve.editor.unlock.confirm.header': 'Unlock Page?',
                    'uve.editor.unlock.confirm.message': 'Page is locked by {0}. Unlock?',
                    'uve.editor.unlock.confirm.accept': 'Yes, Unlock',
                    'dot.common.dialog.reject': 'Cancel'
                })
            },
            mockProvider(DotExperimentsService),
            {
                provide: DotWorkflowsActionsService,
                useValue: {
                    getByInode: () => of([])
                }
            },
            {
                provide: DotLanguagesService,
                useValue: new DotLanguagesServiceMock()
            },
            {
                provide: DotLicenseService,
                useValue: {
                    isEnterprise: () => of(true)
                }
            },
            {
                provide: LoginService,
                useValue: {
                    getCurrentUser: () => of(CurrentUserDataMock)
                }
            },
            {
                provide: DotPropertiesService,
                useValue: dotPropertiesServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        dotContentletLockerService = spectator.inject(DotContentletLockerService);
        messageService = spectator.inject(MessageService);
        confirmationService = spectator.inject(ConfirmationService);

        patchState(store, initialState);

        jest.clearAllMocks();
    });

    describe('withState', () => {
        it('should initialize lockLoading as false', () => {
            expect(store.lockLoading()).toBe(false);
        });
    });

    describe('withMethods', () => {
        describe('toggleLock', () => {
            it('should lock the page when it is not locked', () => {
                const lockSpy = jest
                    .spyOn(dotContentletLockerService, 'lock')
                    .mockReturnValue(of(mockLockResponse));
                const messageServiceSpy = jest.spyOn(messageService, 'add');

                store.toggleLock('test-inode', false, false);

                expect(lockSpy).toHaveBeenCalledWith('test-inode');
                expect(store.lockLoading()).toBe(false); // Returns to false after completion
                expect(messageServiceSpy).toHaveBeenCalledWith({
                    severity: 'success',
                    summary: 'Lock',
                    detail: 'Page locked successfully'
                });
            });

            it('should unlock the page when it is locked by current user', () => {
                const unlockSpy = jest
                    .spyOn(dotContentletLockerService, 'unlock')
                    .mockReturnValue(of(mockLockResponse));
                const messageServiceSpy = jest.spyOn(messageService, 'add');

                store.toggleLock('test-inode', true, true);

                expect(unlockSpy).toHaveBeenCalledWith('test-inode');
                expect(store.lockLoading()).toBe(false);
                expect(messageServiceSpy).toHaveBeenCalledWith({
                    severity: 'success',
                    summary: 'Unlock',
                    detail: 'Page unlocked successfully'
                });
            });

            it('should show confirmation dialog when page is locked by another user', () => {
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            lockedByName: 'Another User'
                        }
                    }
                });

                const confirmSpy = jest.spyOn(confirmationService, 'confirm');

                store.toggleLock('test-inode', true, false);

                expect(confirmSpy).toHaveBeenCalledWith({
                    header: 'Unlock Page?',
                    message: 'Page is locked by Another User. Unlock?',
                    acceptLabel: 'Yes, Unlock',
                    rejectLabel: 'Cancel',
                    accept: expect.any(Function)
                });
            });

            it('should unlock page when user confirms unlocking page locked by another user', () => {
                patchState(store, {
                    pageAPIResponse: {
                        ...MOCK_RESPONSE_HEADLESS,
                        page: {
                            ...MOCK_RESPONSE_HEADLESS.page,
                            lockedByName: 'Another User'
                        }
                    }
                });

                const unlockSpy = jest
                    .spyOn(dotContentletLockerService, 'unlock')
                    .mockReturnValue(of(mockLockResponse));

                let acceptCallback: (() => void) | undefined;
                jest.spyOn(confirmationService, 'confirm').mockImplementation((config) => {
                    acceptCallback = config.accept as () => void;

                    return confirmationService;
                });

                store.toggleLock('test-inode', true, false);

                expect(acceptCallback).toBeDefined();
                acceptCallback?.();

                expect(unlockSpy).toHaveBeenCalledWith('test-inode');
            });

            it('should not toggle lock when already loading', () => {
                patchState(store, { lockLoading: true });

                const lockSpy = jest.spyOn(dotContentletLockerService, 'lock');
                const unlockSpy = jest.spyOn(dotContentletLockerService, 'unlock');

                store.toggleLock('test-inode', false, false);

                expect(lockSpy).not.toHaveBeenCalled();
                expect(unlockSpy).not.toHaveBeenCalled();
            });

            it('should handle lock error', () => {
                const lockSpy = jest
                    .spyOn(dotContentletLockerService, 'lock')
                    .mockReturnValue(throwError(() => new Error('Lock failed')));
                const messageServiceSpy = jest.spyOn(messageService, 'add');

                store.toggleLock('test-inode', false, false);

                expect(lockSpy).toHaveBeenCalledWith('test-inode');
                expect(store.lockLoading()).toBe(false);
                expect(messageServiceSpy).toHaveBeenCalledWith({
                    severity: 'error',
                    summary: 'Lock',
                    detail: 'Error locking page'
                });
            });

            it('should handle unlock error', () => {
                const unlockSpy = jest
                    .spyOn(dotContentletLockerService, 'unlock')
                    .mockReturnValue(throwError(() => new Error('Unlock failed')));
                const messageServiceSpy = jest.spyOn(messageService, 'add');

                store.toggleLock('test-inode', true, true);

                expect(unlockSpy).toHaveBeenCalledWith('test-inode');
                expect(store.lockLoading()).toBe(false);
                expect(messageServiceSpy).toHaveBeenCalledWith({
                    severity: 'error',
                    summary: 'Unlock',
                    detail: 'Error unlocking page'
                });
            });

            it('should set lockLoading to true when operation starts', () => {
                let lockingState = false;
                jest.spyOn(dotContentletLockerService, 'lock').mockImplementation(() => {
                    lockingState = store.lockLoading();

                    return of(mockLockResponse);
                });

                store.toggleLock('test-inode', false, false);

                expect(lockingState).toBe(true);
            });
        });
    });
});
