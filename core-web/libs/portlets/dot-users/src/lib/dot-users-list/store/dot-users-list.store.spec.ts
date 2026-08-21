import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { NEVER, of, throwError } from 'rxjs';

import {
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService
} from '@dotcms/data-access';
import { DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUsersListStore } from './dot-users-list.store';

import { DotUserListItem, DotUsersService } from '../../services/dot-users.service';

const MESSAGES = {
    'users.delete.success.one': 'User deleted.',
    'users.delete.success.many': 'Deleted {0} users.',
    'users.delete.partial-success': 'Deleted {0} of {1} users. {2} failed.',
    'users.create.success': 'User created.',
    'users.update.success': 'User updated.'
};

const MOCK_USERS: DotUserListItem[] = [
    {
        userId: 'dotcms.org.1',
        id: 'dotcms.org.1',
        firstName: 'Admin',
        lastName: 'User',
        fullName: 'Admin User',
        name: 'Admin User',
        emailAddress: 'admin@dotcms.com',
        gravitar: 'abc',
        active: true,
        admin: true,
        backendUser: true,
        frontendUser: false,
        hasConsoleAccess: true,
        lastLoginDate: 1717977600000,
        lastLoginIP: '10.0.0.1',
        failedLoginAttempts: 0
    },
    {
        userId: 'dotcms.org.2',
        id: 'dotcms.org.2',
        firstName: 'Dave',
        lastName: 'Smith',
        fullName: 'Dave Smith',
        name: 'Dave Smith',
        emailAddress: 'dave@dotcms.com',
        gravitar: 'def',
        active: true,
        admin: false,
        backendUser: true,
        frontendUser: true,
        hasConsoleAccess: true,
        lastLoginDate: 1717891200000,
        lastLoginIP: '10.0.0.2',
        failedLoginAttempts: 0
    }
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

    const createService = createServiceFactory({
        service: DotUsersListStore,
        providers: [
            mockProvider(DotUsersService, {
                getUsersPaginated: jest.fn().mockReturnValue(of(MOCK_RESPONSE)),
                deleteUser: jest.fn().mockReturnValue(of({})),
                createUser: jest.fn().mockReturnValue(of(MOCK_USERS[0])),
                updateUser: jest.fn().mockReturnValue(of(MOCK_USERS[0]))
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
        // Clear call history from other tests (mockProvider's jest.fn() is shared).
        // Implementations set via mockReturnValue are preserved.
        jest.clearAllMocks();
        usersService.getUsersPaginated.mockReturnValue(of(MOCK_RESPONSE));
        usersService.deleteUser.mockReturnValue(of({}));
        usersService.createUser.mockReturnValue(of(MOCK_USERS[0]));
        usersService.updateUser.mockReturnValue(of(MOCK_USERS[0]));
    });

    it('loadUsers passes the current state as query params', () => {
        store.loadUsers();

        expect(usersService.getUsersPaginated).toHaveBeenCalledWith({
            filter: undefined,
            roleKey: undefined,
            page: 1,
            perPage: 20,
            orderBy: 'lastLoginDate',
            direction: 'DESC'
        });
        expect(store.users()).toEqual(MOCK_USERS);
        expect(store.totalRecords()).toBe(2);
        expect(store.status()).toBe('loaded');
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
            firstName: 'Ada',
            lastName: 'Lovelace',
            email: 'ada@dotcms.com',
            active: true,
            password: 'Xy7#abcdef'
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
            firstName: 'Ada',
            lastName: 'Lovelace',
            email: 'ada@dotcms.com',
            active: true
        });

        expect(errorManager.handle).toHaveBeenCalled();
        expect(store.status()).toBe('loaded');
    });

    it('updateUser should call the service, push a success toast, and reload the list', () => {
        const messageDisplay = spectator.inject(DotMessageDisplayService);
        usersService.getUsersPaginated.mockClear();

        store.updateUser({
            userId: 'dotcms.org.1',
            firstName: 'Admin',
            lastName: 'User',
            email: 'admin@dotcms.com',
            active: true
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

        store.deleteSingleUser('dotcms.org.1', 'dotcms.org.42');

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
