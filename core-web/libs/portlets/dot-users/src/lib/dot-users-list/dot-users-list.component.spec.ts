import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUsersListComponent } from './dot-users-list.component';
import { DotUsersListStore } from './store/dot-users-list.store';

import { DotUserListItem } from '../services/dot-users.service';

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
        frontendUser: true,
        hasConsoleAccess: true,
        lastLoginDate: 1717977600000,
        lastLoginIP: '10.0.0.1',
        failedLoginAttempts: 0
    },
    {
        userId: 'dotcms.org.9',
        id: 'dotcms.org.9',
        firstName: 'Snow',
        lastName: 'User',
        fullName: 'Snow User',
        name: 'Snow User',
        emailAddress: 'snow@dotcms.com',
        gravitar: 'def',
        active: false,
        admin: false,
        backendUser: false,
        frontendUser: true,
        hasConsoleAccess: false,
        lastLoginDate: null,
        lastLoginIP: null,
        failedLoginAttempts: 0
    }
];

const MESSAGES = {
    'users.new': 'New',
    'users.delete': 'Delete',
    'users.cancel': 'Cancel',
    'users.close': 'Close',
    'users.inactive': 'Inactive',
    'users.search.placeholder': 'Search users',
    'users.access.backend': 'Back-end',
    'users.access.frontend': 'Front-end',
    'users.table.header.user': 'User',
    'users.table.header.email': 'Email',
    'users.table.header.roles': 'Roles',
    'users.table.header.access': 'Access',
    'users.table.header.last-login': 'Last login',
    'users.empty.state.title': 'No users yet',
    'users.empty.state.description': 'Create a user to get started.',
    'users.create.header': 'Create User',
    'users.edit.header': 'Edit User',
    'users.confirm.delete.header': 'Delete users',
    'users.confirm.delete.message': 'Delete {0} users?',
    'users.selected.count': '{0} selected',
    'users.filter.by': 'Filter by',
    'users.filter.all-access': 'All access'
};

describe('DotUsersListComponent', () => {
    let spectator: Spectator<DotUsersListComponent>;

    // Mock window.matchMedia for PrimeNG components that query it
    beforeAll(() => {
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation((query) => ({
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
    });

    const createComponent = createComponentFactory({
        component: DotUsersListComponent,
        detectChanges: false,
        componentProviders: [
            mockProvider(DotUsersListStore, {
                users: jest.fn().mockReturnValue(MOCK_USERS),
                selectedUsers: jest.fn().mockReturnValue([]),
                filter: jest.fn().mockReturnValue(''),
                roleFilter: jest.fn().mockReturnValue(''),
                page: jest.fn().mockReturnValue(1),
                rows: jest.fn().mockReturnValue(20),
                totalRecords: jest.fn().mockReturnValue(2),
                sortField: jest.fn().mockReturnValue('lastLoginDate'),
                sortOrder: jest.fn().mockReturnValue('DESC'),
                status: jest.fn().mockReturnValue('loaded'),
                setFilter: jest.fn(),
                setRoleFilter: jest.fn(),
                setPagination: jest.fn(),
                setSort: jest.fn(),
                setSelectedUsers: jest.fn(),
                deleteSelectedUsers: jest.fn(),
                loadUsers: jest.fn()
            }),
            mockProvider(DialogService, {
                open: jest
                    .fn()
                    .mockReturnValue({ onClose: { pipe: () => ({ subscribe: jest.fn() }) } })
            }),
            ConfirmationService
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService(MESSAGES)
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.detectChanges();
    });

    it('should render the search input', () => {
        expect(spectator.query(byTestId('users-search-input'))).toBeTruthy();
    });

    it('should render the New button', () => {
        expect(spectator.query(byTestId('users-new-btn'))).toBeTruthy();
    });

    it('should not render selected-count or Delete when nothing is selected', () => {
        expect(spectator.query(byTestId('users-selected-count'))).toBeNull();
        expect(spectator.query(byTestId('users-delete-btn'))).toBeNull();
    });

    it('should render "N selected" and the Delete button when there is a selection', () => {
        const store = spectator.inject(DotUsersListStore, true);
        (store.selectedUsers as jest.Mock).mockReturnValue([MOCK_USERS[0], MOCK_USERS[1]]);
        spectator.detectChanges();

        const countLabel = spectator.query(byTestId('users-selected-count'));

        expect(countLabel?.textContent?.trim()).toBe('2 selected');
        expect(spectator.query(byTestId('users-delete-btn'))).toBeTruthy();
    });

    it('should render an Inactive chip only for inactive users', () => {
        const chips = spectator.queryAll(byTestId('users-inactive-chip'));

        expect(chips.length).toBe(1);
    });

    it('should render Back-end chip when user is a backend user', () => {
        expect(spectator.queryAll(byTestId('users-access-backend-chip')).length).toBe(1);
    });

    it('should render Front-end chip when user is a frontend user', () => {
        expect(spectator.queryAll(byTestId('users-access-frontend-chip')).length).toBe(2);
    });

    it('should open the create dialog when openCreateDialog is called', () => {
        const dialogService = spectator.inject(DialogService, true);
        spectator.component.openCreateDialog();

        expect(dialogService.open).toHaveBeenCalled();
    });

    it('should open the edit dialog when a row is opened via openEditDialog', () => {
        const dialogService = spectator.inject(DialogService, true);
        spectator.component.openEditDialog(MOCK_USERS[0]);

        expect(dialogService.open).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({ data: { user: MOCK_USERS[0] } })
        );
    });
});
