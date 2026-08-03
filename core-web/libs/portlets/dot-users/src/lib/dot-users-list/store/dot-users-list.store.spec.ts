import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

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
    'users.delete.success': 'Deleted {0} user(s).',
    'users.delete.partial-success': 'Deleted {0} of {1} users. {2} failed.'
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
                deleteUser: jest.fn().mockReturnValue(of({}))
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
        // The onInit effect fires loadUsers automatically
        spectator.flushEffects();
    });

    it('should load users on init with default params', () => {
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

    it('setSort should update sort state and trigger a reload', () => {
        usersService.getUsersPaginated.mockClear();

        store.setSort('emailAddress', 'ASC');
        spectator.flushEffects();

        expect(store.sortField()).toBe('emailAddress');
        expect(store.sortOrder()).toBe('ASC');
        expect(usersService.getUsersPaginated).toHaveBeenCalledWith(
            expect.objectContaining({ orderBy: 'emailAddress', direction: 'ASC' })
        );
    });

    it('setPagination should update page/rows', () => {
        store.setPagination(3, 40);

        expect(store.page()).toBe(3);
        expect(store.rows()).toBe(40);
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

        expect(usersService.deleteUser).toHaveBeenCalledWith('dotcms.org.1');
        expect(usersService.deleteUser).toHaveBeenCalledWith('dotcms.org.2');
        expect(store.selectedUsers()).toEqual([]);
        expect(usersService.getUsersPaginated).toHaveBeenCalled();
        expect(messageDisplay.push).toHaveBeenCalledWith(
            expect.objectContaining({
                severity: DotMessageSeverity.SUCCESS,
                type: DotMessageType.SIMPLE_MESSAGE,
                message: 'Deleted 2 user(s).'
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

    it('should set status to error and delegate to httpErrorManager when load fails', () => {
        const errorManager = spectator.inject(DotHttpErrorManagerService);
        usersService.getUsersPaginated.mockReturnValueOnce(throwError(() => new Error('boom')));

        store.setFilter('will-error');
        spectator.flushEffects();

        expect(store.status()).toBe('error');
        expect(errorManager.handle).toHaveBeenCalled();
    });
});
