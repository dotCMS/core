import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { NEVER, of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';

import {
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService,
    DotRolesService
} from '@dotcms/data-access';
import { DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUsersListStore } from './dot-users-list.store';

import { DotUsersService } from '../../services/dot-users.service';
import { createFakeUser } from '../../testing/dot-user.mock';

const MESSAGES = {
    'users.delete.success.one': 'User deleted.',
    'users.delete.success.many': 'Deleted {0} users.',
    'users.delete.partial-success': 'Deleted {0} of {1} users. {2} failed.',
    'users.create.success': 'User created.',
    'users.update.success': 'User updated.'
};

const MOCK_USERS = [
    createFakeUser({
        userId: 'dotcms.org.1',
        id: 'dotcms.org.1',
        firstName: 'Admin',
        lastName: 'User',
        fullName: 'Admin User',
        name: 'Admin User',
        emailAddress: 'admin@dotcms.com',
        gravitar: 'abc',
        admin: true,
        lastLoginDate: 1717977600000,
        lastLoginIP: '10.0.0.1'
    }),
    createFakeUser({
        userId: 'dotcms.org.2',
        id: 'dotcms.org.2',
        firstName: 'Dave',
        lastName: 'Smith',
        fullName: 'Dave Smith',
        name: 'Dave Smith',
        emailAddress: 'dave@dotcms.com',
        gravitar: 'def',
        frontendUser: true,
        lastLoginDate: 1717891200000,
        lastLoginIP: '10.0.0.2'
    })
];

const MOCK_RESPONSE = {
    entity: MOCK_USERS,
    errors: [],
    messages: [],
    permissions: [],
    i18nMessagesMap: {},
    pagination: { currentPage: 1, perPage: 20, totalEntries: 2 }
};

describe('DotUsersListStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotUsersListStore>>;
    let store: InstanceType<typeof DotUsersListStore>;
    let usersService: jest.Mocked<DotUsersService>;
    let rolesService: jest.Mocked<DotRolesService>;

    const createService = createServiceFactory({
        service: DotUsersListStore,
        providers: [
            mockProvider(DotUsersService, {
                getUsersPaginated: jest.fn().mockReturnValue(of(MOCK_RESPONSE)),
                deleteUser: jest.fn().mockReturnValue(of({})),
                createUser: jest.fn().mockReturnValue(of(MOCK_USERS[0])),
                updateUser: jest.fn().mockReturnValue(of(MOCK_USERS[0]))
            }),
            mockProvider(DotRolesService, {
                getForUser: jest.fn().mockReturnValue(of([]))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotMessageDisplayService, { push: jest.fn() }),
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        usersService = spectator.inject(DotUsersService) as jest.Mocked<DotUsersService>;
        rolesService = spectator.inject(DotRolesService) as jest.Mocked<DotRolesService>;
        // Clear call history from other tests (mockProvider's jest.fn() is shared).
        // Implementations set via mockReturnValue are preserved.
        jest.clearAllMocks();
        usersService.getUsersPaginated.mockReturnValue(of(MOCK_RESPONSE));
        usersService.deleteUser.mockReturnValue(of({}));
        usersService.createUser.mockReturnValue(of(MOCK_USERS[0]));
        usersService.updateUser.mockReturnValue(of(MOCK_USERS[0]));
        rolesService.getForUser.mockReturnValue(of([]));
    });

    it('loadUsers passes the current state as query params (opts into includeRoles)', () => {
        store.loadUsers();

        expect(usersService.getUsersPaginated).toHaveBeenCalledWith({
            filter: undefined,
            roleKey: undefined,
            page: 1,
            perPage: 20,
            orderBy: 'lastLoginDate',
            direction: 'DESC',
            // #37236: the list opts into inline roles on every load;
            // the store's 403 handler downgrades to `false` when the
            // viewer lacks the required portlet gate.
            includeRoles: true
        });
        expect(store.users()).toEqual(MOCK_USERS);
        expect(store.totalRecords()).toBe(2);
        expect(store.status()).toBe('loaded');
    });

    describe('includeRoles fast path (#37236)', () => {
        it('reads role names from the inline `roles` field and skips the per-user fetch', () => {
            usersService.getUsersPaginated.mockReturnValueOnce(
                of({
                    ...MOCK_RESPONSE,
                    entity: [
                        {
                            ...MOCK_USERS[0],
                            roles: [
                                {
                                    id: 'r-1',
                                    name: 'CMS Administrator',
                                    roleKey: 'CMS Administrator'
                                },
                                {
                                    id: 'r-2',
                                    name: 'Back-end User',
                                    roleKey: 'DOTCMS_BACK_END_USER'
                                }
                            ]
                        },
                        {
                            ...MOCK_USERS[1],
                            roles: []
                        }
                    ]
                })
            );

            store.loadUsers();

            expect(rolesService.getForUser).not.toHaveBeenCalled();
            expect(store.userRoles()).toEqual({
                'dotcms.org.1': ['CMS Administrator', 'Back-end User'],
                'dotcms.org.2': []
            });
        });

        it('falls back to per-row rolesService.getForUser when the response omits `roles`', () => {
            // Envelope without the `roles` field — models an older
            // backend that predates #37236.
            const legacyRow = { ...MOCK_USERS[0] };
            delete (legacyRow as Partial<(typeof MOCK_USERS)[0]>).roles;
            usersService.getUsersPaginated.mockReturnValueOnce(
                of({ ...MOCK_RESPONSE, entity: [legacyRow] })
            );
            rolesService.getForUser.mockReturnValueOnce(
                of([
                    { id: 'r-1', name: 'Admin', roleKey: 'CMS Administrator', childCount: 0 },
                    // Filtered: personal role's key === userId.
                    { id: 'r-p', name: 'Personal', roleKey: 'dotcms.org.1', childCount: 0 }
                ])
            );

            store.loadUsers();

            expect(rolesService.getForUser).toHaveBeenCalledWith('dotcms.org.1');
            expect(store.userRoles()).toEqual({ 'dotcms.org.1': ['Admin'] });
        });

        it('retries without includeRoles when the backend gates the flag with 403, then N+1s', () => {
            usersService.getUsersPaginated
                .mockReturnValueOnce(
                    throwError(
                        () =>
                            new HttpErrorResponse({
                                status: 403,
                                statusText: 'Forbidden',
                                url: '/api/v1/users/filter?includeRoles=true'
                            })
                    )
                )
                .mockReturnValueOnce(of({ ...MOCK_RESPONSE, entity: [MOCK_USERS[0]] }));
            rolesService.getForUser.mockReturnValueOnce(
                of([{ id: 'r-1', name: 'Admin', roleKey: 'CMS Administrator', childCount: 0 }])
            );

            store.loadUsers();

            expect(usersService.getUsersPaginated).toHaveBeenCalledTimes(2);
            expect(usersService.getUsersPaginated).toHaveBeenNthCalledWith(
                1,
                expect.objectContaining({ includeRoles: true })
            );
            expect(usersService.getUsersPaginated).toHaveBeenNthCalledWith(
                2,
                expect.objectContaining({ includeRoles: false })
            );
            expect(rolesService.getForUser).toHaveBeenCalledWith('dotcms.org.1');
            expect(store.userRoles()).toEqual({ 'dotcms.org.1': ['Admin'] });
        });

        it('does NOT retry on non-403 errors — they bubble up to the error state', () => {
            usersService.getUsersPaginated.mockReturnValueOnce(
                throwError(
                    () =>
                        new HttpErrorResponse({
                            status: 500,
                            statusText: 'Server Error',
                            url: '/api/v1/users/filter?includeRoles=true'
                        })
                )
            );

            store.loadUsers();

            expect(usersService.getUsersPaginated).toHaveBeenCalledTimes(1);
            expect(store.status()).toBe('error');
        });
    });

    it('setRoleFilter should reset page and trigger a reload with roleKey', () => {
        usersService.getUsersPaginated.mockClear();

        store.setRoleFilter('DOTCMS_BACK_END_USER');
        spectator.flushEffects();

        expect(store.roleFilter()).toBe('DOTCMS_BACK_END_USER');
        expect(store.page()).toBe(1);
        expect(usersService.getUsersPaginated).toHaveBeenCalledWith(
            expect.objectContaining({ roleKey: 'DOTCMS_BACK_END_USER', page: 1 })
        );
    });

    it('setRoleFilter with empty string should send roleKey undefined', () => {
        store.setRoleFilter('DOTCMS_FRONT_END_USER');
        spectator.flushEffects();
        usersService.getUsersPaginated.mockClear();

        store.setRoleFilter('');
        spectator.flushEffects();

        expect(usersService.getUsersPaginated).toHaveBeenCalledWith(
            expect.objectContaining({ roleKey: undefined })
        );
    });

    it('setFilter should reset page and trigger a reload', () => {
        usersService.getUsersPaginated.mockClear();

        store.setFilter('jane');
        spectator.flushEffects();

        expect(store.filter()).toBe('jane');
        expect(store.page()).toBe(1);
        expect(usersService.getUsersPaginated).toHaveBeenCalledWith(
            expect.objectContaining({ filter: 'jane', page: 1 })
        );
    });

    it('applyLazyLoad should batch page + sort into a single reload', () => {
        usersService.getUsersPaginated.mockClear();

        store.applyLazyLoad({
            page: 3,
            rows: 40,
            sortField: 'emailAddress',
            sortOrder: 'ASC'
        });

        expect(store.page()).toBe(3);
        expect(store.rows()).toBe(40);
        expect(store.sortField()).toBe('emailAddress');
        expect(store.sortOrder()).toBe('ASC');
        expect(usersService.getUsersPaginated).toHaveBeenCalledTimes(1);
        expect(usersService.getUsersPaginated).toHaveBeenCalledWith(
            expect.objectContaining({
                page: 3,
                perPage: 40,
                orderBy: 'emailAddress',
                direction: 'ASC'
            })
        );
    });

    it('setSelectedUsers should update selection', () => {
        store.setSelectedUsers([MOCK_USERS[0]]);

        expect(store.selectedUsers()).toEqual([MOCK_USERS[0]]);
    });

    it('deleteSelectedUsers should delete each selected user, reload, and push a success toast', () => {
        const messageDisplay = spectator.inject(DotMessageDisplayService);
        store.setSelectedUsers([MOCK_USERS[0], MOCK_USERS[1]]);
        usersService.getUsersPaginated.mockClear();

        store.deleteSelectedUsers();

        expect(usersService.deleteUser).toHaveBeenCalledWith('dotcms.org.1', undefined);
        expect(usersService.deleteUser).toHaveBeenCalledWith('dotcms.org.2', undefined);
        expect(store.selectedUsers()).toEqual([]);
        expect(usersService.getUsersPaginated).toHaveBeenCalled();
        expect(messageDisplay.push).toHaveBeenCalledWith(
            expect.objectContaining({
                severity: DotMessageSeverity.SUCCESS,
                type: DotMessageType.SIMPLE_MESSAGE,
                message: 'Deleted 2 users.'
            })
        );
    });

    it('deleteSelectedUsers should push the singular success toast when only one is deleted', () => {
        const messageDisplay = spectator.inject(DotMessageDisplayService);
        store.setSelectedUsers([MOCK_USERS[0]]);

        store.deleteSelectedUsers();

        expect(messageDisplay.push).toHaveBeenCalledWith(
            expect.objectContaining({
                severity: DotMessageSeverity.SUCCESS,
                message: 'User deleted.'
            })
        );
    });

    it('deleteSelectedUsers should push a WARNING partial-success toast when some deletes fail', () => {
        const messageDisplay = spectator.inject(DotMessageDisplayService);
        const errorManager = spectator.inject(DotHttpErrorManagerService);
        usersService.deleteUser
            .mockReturnValueOnce(of({}))
            .mockReturnValueOnce(throwError(() => new Error('boom')));
        store.setSelectedUsers([MOCK_USERS[0], MOCK_USERS[1]]);

        store.deleteSelectedUsers();

        expect(errorManager.handle).toHaveBeenCalled();
        expect(messageDisplay.push).toHaveBeenCalledWith(
            expect.objectContaining({
                severity: DotMessageSeverity.WARNING,
                message: 'Deleted 1 of 2 users. 1 failed.'
            })
        );
        expect(store.selectedUsers()).toEqual([]);
    });

    it('deleteSelectedUsers should not push a toast when every delete fails', () => {
        const messageDisplay = spectator.inject(DotMessageDisplayService);
        usersService.deleteUser.mockReturnValue(throwError(() => new Error('boom')));
        store.setSelectedUsers([MOCK_USERS[0], MOCK_USERS[1]]);

        store.deleteSelectedUsers();

        expect(messageDisplay.push).not.toHaveBeenCalled();
    });

    it('deleteSelectedUsers should no-op when nothing selected', () => {
        usersService.deleteUser.mockClear();

        store.deleteSelectedUsers();

        expect(usersService.deleteUser).not.toHaveBeenCalled();
    });

    it('deleteSelectedUsers should no-op on double-click while a delete is in flight', () => {
        // NEVER keeps the first request in flight so the status stays 'loading'.
        usersService.deleteUser.mockReturnValueOnce(NEVER);
        store.setSelectedUsers([MOCK_USERS[0]]);

        store.deleteSelectedUsers(); // first click — starts the delete, status becomes 'loading'
        usersService.deleteUser.mockClear();

        store.deleteSelectedUsers(); // second click — should be ignored

        expect(usersService.deleteUser).not.toHaveBeenCalled();
    });

    it('should set status to error and delegate to httpErrorManager when load fails', () => {
        const errorManager = spectator.inject(DotHttpErrorManagerService);
        usersService.getUsersPaginated.mockReturnValueOnce(throwError(() => new Error('boom')));

        store.setFilter('will-error');
        spectator.flushEffects();

        expect(store.status()).toBe('error');
        expect(errorManager.handle).toHaveBeenCalled();
    });

    it('createUser should call the service, push a success toast, and reload the list', () => {
        const messageDisplay = spectator.inject(DotMessageDisplayService);
        usersService.getUsersPaginated.mockClear();

        store.createUser({
            payload: {
                firstName: 'Ada',
                lastName: 'Lovelace',
                email: 'ada@dotcms.com',
                active: true,
                password: 'Xy7#abcdef'
            }
        });

        expect(usersService.createUser).toHaveBeenCalledWith(
            expect.objectContaining({ firstName: 'Ada', email: 'ada@dotcms.com' })
        );
        expect(messageDisplay.push).toHaveBeenCalledWith(
            expect.objectContaining({
                severity: DotMessageSeverity.SUCCESS,
                message: 'User created.'
            })
        );
        expect(usersService.getUsersPaginated).toHaveBeenCalled();
    });

    it('createUser should surface HTTP errors and keep the list in `loaded`', () => {
        const errorManager = spectator.inject(DotHttpErrorManagerService);
        usersService.createUser.mockReturnValueOnce(throwError(() => new Error('boom')));

        store.createUser({
            payload: {
                firstName: 'Ada',
                lastName: 'Lovelace',
                email: 'ada@dotcms.com',
                active: true
            }
        });

        expect(errorManager.handle).toHaveBeenCalled();
        expect(store.status()).toBe('loaded');
    });

    it('updateUser should call the service, push a success toast, and reload the list', () => {
        const messageDisplay = spectator.inject(DotMessageDisplayService);
        usersService.getUsersPaginated.mockClear();

        store.updateUser({
            payload: {
                userId: 'dotcms.org.1',
                firstName: 'Admin',
                lastName: 'User',
                email: 'admin@dotcms.com',
                active: true
            }
        });

        expect(usersService.updateUser).toHaveBeenCalledWith(
            expect.objectContaining({ userId: 'dotcms.org.1' })
        );
        expect(messageDisplay.push).toHaveBeenCalledWith(
            expect.objectContaining({
                severity: DotMessageSeverity.SUCCESS,
                message: 'User updated.'
            })
        );
        expect(usersService.getUsersPaginated).toHaveBeenCalled();
    });

    it('deleteSingleUser should forward the replacementUserId when provided', () => {
        const messageDisplay = spectator.inject(DotMessageDisplayService);
        usersService.getUsersPaginated.mockClear();

        store.deleteSingleUser({
            userId: 'dotcms.org.1',
            replacementUserId: 'dotcms.org.42'
        });

        expect(usersService.deleteUser).toHaveBeenCalledWith('dotcms.org.1', 'dotcms.org.42');
        expect(messageDisplay.push).toHaveBeenCalled();
        expect(usersService.getUsersPaginated).toHaveBeenCalled();
    });

    it('deleteSelectedUsers should forward the replacementUserId to every delete call', () => {
        store.setSelectedUsers([MOCK_USERS[0], MOCK_USERS[1]]);
        usersService.deleteUser.mockClear();

        store.deleteSelectedUsers('dotcms.org.42');

        expect(usersService.deleteUser).toHaveBeenCalledWith('dotcms.org.1', 'dotcms.org.42');
        expect(usersService.deleteUser).toHaveBeenCalledWith('dotcms.org.2', 'dotcms.org.42');
    });
});
