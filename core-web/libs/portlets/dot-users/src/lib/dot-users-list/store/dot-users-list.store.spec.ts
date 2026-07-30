import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

import { DotUsersListStore } from './dot-users-list.store';

import { DotUserListItem, DotUsersService } from '../../services/dot-users.service';

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
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        usersService = spectator.inject(DotUsersService) as jest.Mocked<DotUsersService>;
        // The onInit effect fires loadUsers automatically
        spectator.flushEffects();
    });

    it('should load users on init with default params', () => {
        expect(usersService.getUsersPaginated).toHaveBeenCalledWith({
            filter: undefined,
            page: 1,
            perPage: 20,
            orderBy: 'lastLoginDate',
            direction: 'DESC'
        });
        expect(store.users()).toEqual(MOCK_USERS);
        expect(store.totalRecords()).toBe(2);
        expect(store.status()).toBe('loaded');
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

    it('deleteSelectedUsers should delete each selected user and reload', () => {
        store.setSelectedUsers([MOCK_USERS[0], MOCK_USERS[1]]);
        usersService.getUsersPaginated.mockClear();

        store.deleteSelectedUsers();

        expect(usersService.deleteUser).toHaveBeenCalledWith('dotcms.org.1');
        expect(usersService.deleteUser).toHaveBeenCalledWith('dotcms.org.2');
        expect(store.selectedUsers()).toEqual([]);
        expect(usersService.getUsersPaginated).toHaveBeenCalled();
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
