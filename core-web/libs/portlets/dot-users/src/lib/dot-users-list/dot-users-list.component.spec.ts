import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { ActivatedRoute } from '@angular/router';

import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUsersListComponent } from './dot-users-list.component';
import { DotUsersListStore } from './store/dot-users-list.store';

import { DotUsersService } from '../services/dot-users.service';
import { createFakeUser } from '../testing/dot-user.mock';

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
        frontendUser: true,
        lastLoginDate: 1717977600000,
        lastLoginIP: '10.0.0.1'
    }),
    createFakeUser({
        userId: 'dotcms.org.9',
        id: 'dotcms.org.9',
        firstName: 'Snow',
        lastName: 'User',
        fullName: 'Snow User',
        name: 'Snow User',
        emailAddress: 'snow@dotcms.com',
        gravitar: 'def',
        active: false,
        backendUser: false,
        frontendUser: true,
        hasConsoleAccess: false
    })
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
    'users.filter.all-access': 'All access',
    'users.actions.more.aria': 'More actions',
    'users.actions.edit': 'Edit',
    'users.actions.push-publish': 'Push to Publish',
    'users.actions.add-to-bundle': 'Add to Bundle',
    'contenttypes.content.push_publish': 'Remote Publish'
};

/**
 * Feeds the resolver-shaped `route.snapshot.data` the component reads on
 * construction. Tests that need to flip enterprise/env state override
 * `useValue` in their own `createComponent(...)` overrides block.
 */
function fakeRoute(
    isEnterprise = true,
    pushPublishEnvironments: Array<{ id: string; name: string }> = [
        { id: 'env-1', name: 'Production' }
    ]
): Partial<ActivatedRoute> {
    return {
        snapshot: {
            data: { isEnterprise, pushPublishEnvironments }
        } as ActivatedRoute['snapshot']
    };
}

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
                userRoles: jest.fn().mockReturnValue({}),
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
                applyLazyLoad: jest.fn(),
                setSelectedUsers: jest.fn(),
                deleteSelectedUsers: jest.fn(),
                loadUsers: jest.fn()
            }),
            mockProvider(DialogService, {
                open: jest
                    .fn()
                    .mockReturnValue({ onClose: { pipe: () => ({ subscribe: jest.fn() }) } })
            })
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService(MESSAGES)
            },
            {
                provide: ActivatedRoute,
                useValue: fakeRoute()
            },
            mockProvider(DotPushPublishDialogService, {
                open: jest.fn()
            }),
            mockProvider(DotUsersService, {
                getUsersPaginated: jest.fn().mockReturnValue(
                    of({
                        entity: [],
                        errors: [],
                        messages: [],
                        permissions: [],
                        i18nMessagesMap: {},
                        pagination: { currentPage: 1, perPage: 10, totalEntries: 0 }
                    })
                )
            })
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.detectChanges();
    });

    it('should render the search input', () => {
        expect(spectator.query(byTestId('users-search-input'))).toBeTruthy();
    });

    it('should render the New button visible when nothing is selected', () => {
        const wrapper = spectator.query(byTestId('users-new-btn-wrapper'));

        expect(spectator.query(byTestId('users-new-btn'))).toBeTruthy();
        expect(wrapper?.className).toContain('opacity-100');
    });

    it('should fade the New button out while a selection is active', () => {
        const store = spectator.inject(DotUsersListStore, true);
        (store.selectedUsers as jest.Mock).mockReturnValue([MOCK_USERS[0]]);
        spectator.detectChanges();

        const wrapper = spectator.query(byTestId('users-new-btn-wrapper'));

        expect(wrapper?.className).toContain('opacity-0');
        expect(wrapper?.className).toContain('pointer-events-none');

        // Mocks live at the factory level, so mutations persist across sibling
        // tests. Reset before yielding so the next "no selection" test starts
        // fresh instead of inheriting our non-empty selection.
        (store.selectedUsers as jest.Mock).mockReturnValue([]);
    });

    it('should keep the selection toolbar mounted but faded when nothing is selected', () => {
        // The toolbar always renders — the fade / max-w-0 transition needs
        // both mount states to keep animation on enter and exit, so we
        // check the collapsed classes instead of a mount check.
        const toolbar = spectator.query(byTestId('users-selection-toolbar'));

        expect(toolbar).toBeTruthy();
        expect(toolbar?.className).toContain('opacity-0');
        expect(toolbar?.className).toContain('pointer-events-none');
    });

    it('should reveal the selection toolbar and count when there is a selection', () => {
        const store = spectator.inject(DotUsersListStore, true);
        (store.selectedUsers as jest.Mock).mockReturnValue([MOCK_USERS[0], MOCK_USERS[1]]);
        spectator.detectChanges();

        const toolbar = spectator.query(byTestId('users-selection-toolbar'));
        const countLabel = spectator.query(byTestId('users-selected-count'));

        expect(toolbar?.className).toContain('opacity-100');
        expect(toolbar?.className).not.toContain('pointer-events-none');
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

    it('should render a trailing more button on every visible row when enterprise', () => {
        const buttons = spectator.queryAll(byTestId('users-row-more-btn'));

        expect(buttons.length).toBe(MOCK_USERS.length);
    });

    it('should render the more button hover-hidden by default', () => {
        const btn = spectator.query(byTestId('users-row-more-btn'));

        // Row-level `group` + `group-hover:opacity-100` reveal the button on
        // hover (and focus-within for keyboard). At rest it stays opacity-0.
        expect(btn?.className).toContain('opacity-0');
        expect(btn?.className).toContain('group-hover:opacity-100');
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

    describe('row kebab menu', () => {
        it('should expose only Push Publish and Add to Bundle when enterprise', () => {
            const items = spectator.component['getRowMenuItems'](MOCK_USERS[0]);
            const labels = items.filter((i) => i.label).map((i) => i.label);

            expect(labels).toEqual(['Push to Publish', 'Add to Bundle']);
        });

        it('should not carry icons on the menu items', () => {
            const items = spectator.component['getRowMenuItems'](MOCK_USERS[0]);

            expect(items.every((i) => i.icon === undefined)).toBe(true);
        });

        it('openRowPushPublish should send the user_<id> asset identifier', () => {
            const svc = spectator.inject(DotPushPublishDialogService, true);
            spectator.component.openRowPushPublish(MOCK_USERS[0]);

            expect(svc.open).toHaveBeenCalledWith(
                expect.objectContaining({
                    assetIdentifier: 'user_dotcms.org.1',
                    title: 'Remote Publish'
                })
            );
        });

        it('openRowAddToBundle should stage the user_<id> asset identifier', () => {
            spectator.component.openRowAddToBundle(MOCK_USERS[0]);

            expect(spectator.component['$addToBundleAssetId']()).toBe('user_dotcms.org.1');
        });
    });

    describe('bulk push publish + bundle', () => {
        beforeEach(() => {
            const store = spectator.inject(DotUsersListStore, true);
            (store.selectedUsers as jest.Mock).mockReturnValue([MOCK_USERS[0], MOCK_USERS[1]]);
            spectator.detectChanges();
        });

        it('openBulkPushPublish should send a comma-joined user_<id> list', () => {
            const svc = spectator.inject(DotPushPublishDialogService, true);
            spectator.component.openBulkPushPublish();

            expect(svc.open).toHaveBeenCalledWith(
                expect.objectContaining({
                    assetIdentifier: 'user_dotcms.org.1,user_dotcms.org.9'
                })
            );
        });

        it('openBulkAddToBundle should stage the comma-joined selection', () => {
            spectator.component.openBulkAddToBundle();

            expect(spectator.component['$addToBundleAssetId']()).toBe(
                'user_dotcms.org.1,user_dotcms.org.9'
            );
        });

        it('should render both bulk action buttons in the selection toolbar', () => {
            expect(spectator.query(byTestId('users-bulk-push-publish-btn'))).toBeTruthy();
            expect(spectator.query(byTestId('users-bulk-add-to-bundle-btn'))).toBeTruthy();
        });
    });

    describe('bulk delete', () => {
        const REPLACEMENT = createFakeUser({
            ...MOCK_USERS[0],
            userId: 'dotcms.org.42',
            id: 'dotcms.org.42',
            emailAddress: 'ops@dotcms.com'
        });

        it('confirmDelete should open the bulk delete dialog and reset the replacement', () => {
            spectator.component['$bulkReplacementUser'].set(REPLACEMENT);

            spectator.component.confirmDelete();

            expect(spectator.component['$bulkDeleteVisible']()).toBe(true);
            expect(spectator.component['$bulkReplacementUser']()).toBeNull();
        });

        it('canConfirmBulkDelete should require a replacement not in the selection', () => {
            const store = spectator.inject(DotUsersListStore, true);
            (store.selectedUsers as jest.Mock).mockReturnValue([MOCK_USERS[0]]);
            spectator.detectChanges();

            expect(spectator.component['$canConfirmBulkDelete']()).toBe(false);

            spectator.component.onBulkReplacementSelect(MOCK_USERS[0]);
            expect(spectator.component['$canConfirmBulkDelete']()).toBe(false);

            spectator.component.onBulkReplacementSelect(REPLACEMENT);
            expect(spectator.component['$canConfirmBulkDelete']()).toBe(true);
        });

        it('confirmBulkDelete should forward the replacement id to the store and close', () => {
            const store = spectator.inject(DotUsersListStore, true);
            (store.selectedUsers as jest.Mock).mockReturnValue([MOCK_USERS[0]]);
            spectator.detectChanges();

            spectator.component.onBulkReplacementSelect(REPLACEMENT);
            spectator.component.confirmBulkDelete();

            expect(store.deleteSelectedUsers).toHaveBeenCalledWith('dotcms.org.42');
            expect(spectator.component['$bulkDeleteVisible']()).toBe(false);
        });

        it('confirmBulkDelete should no-op when no replacement was picked', () => {
            const store = spectator.inject(DotUsersListStore, true);
            (store.deleteSelectedUsers as jest.Mock).mockClear();

            spectator.component.confirmBulkDelete();

            expect(store.deleteSelectedUsers).not.toHaveBeenCalled();
        });

        it('should surface the footer warning after an invalid confirmBulkDelete click', () => {
            const store = spectator.inject(DotUsersListStore, true);
            (store.selectedUsers as jest.Mock).mockReturnValue([MOCK_USERS[0]]);
            spectator.component['$bulkDeleteVisible'].set(true);
            spectator.detectChanges();

            // No replacement chosen — first Delete click reveals the warning
            // banner instead of dispatching to the store.
            spectator.component.confirmBulkDelete();
            spectator.detectChanges();

            expect(spectator.query(byTestId('users-bulk-delete-warning'))).toBeTruthy();
            expect(spectator.component['$bulkDeleteWarning']()).toBe(
                'users.dialog.warning.form-errors'
            );
        });

        it('should hide the footer warning once a valid replacement is picked', () => {
            const store = spectator.inject(DotUsersListStore, true);
            (store.selectedUsers as jest.Mock).mockReturnValue([MOCK_USERS[0]]);

            spectator.component.confirmBulkDelete();
            expect(spectator.component['$bulkDeleteWarning']()).toBe(
                'users.dialog.warning.form-errors'
            );

            spectator.component.onBulkReplacementSelect(REPLACEMENT);

            expect(spectator.component['$bulkDeleteWarning']()).toBeNull();
        });
    });
});

/**
 * Separate top-level describe so the ActivatedRoute mock is swapped
 * before TestBed instantiates — Spectator's per-test provider override
 * throws once the module is initialized by the enterprise describe's
 * beforeEach. Everything else stays identical.
 */
describe('DotUsersListComponent — non-enterprise instance', () => {
    let spectator: Spectator<DotUsersListComponent>;

    const createComponent = createComponentFactory({
        component: DotUsersListComponent,
        detectChanges: false,
        componentProviders: [
            mockProvider(DotUsersListStore, {
                users: jest.fn().mockReturnValue(MOCK_USERS),
                userRoles: jest.fn().mockReturnValue({}),
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
                applyLazyLoad: jest.fn(),
                setSelectedUsers: jest.fn(),
                deleteSelectedUsers: jest.fn(),
                loadUsers: jest.fn()
            }),
            mockProvider(DialogService, {
                open: jest
                    .fn()
                    .mockReturnValue({ onClose: { pipe: () => ({ subscribe: jest.fn() }) } })
            })
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService(MESSAGES)
            },
            {
                provide: ActivatedRoute,
                useValue: fakeRoute(false, [])
            },
            mockProvider(DotPushPublishDialogService, { open: jest.fn() }),
            mockProvider(DotUsersService, {
                getUsersPaginated: jest.fn().mockReturnValue(
                    of({
                        entity: [],
                        errors: [],
                        messages: [],
                        permissions: [],
                        i18nMessagesMap: {},
                        pagination: { currentPage: 1, perPage: 10, totalEntries: 0 }
                    })
                )
            })
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.detectChanges();
    });

    it('should return no menu items — kebab drops off the row entirely', () => {
        const items = spectator.component['getRowMenuItems'](MOCK_USERS[0]);

        expect(items).toEqual([]);
        expect(spectator.query(byTestId('users-row-more-btn'))).toBeNull();
    });

    it('should hide the Push Publish bulk action but keep Add to Bundle promoted', () => {
        const store = spectator.inject(DotUsersListStore, true);
        (store.selectedUsers as jest.Mock).mockReturnValue([MOCK_USERS[0]]);
        spectator.detectChanges();

        expect(spectator.query(byTestId('users-bulk-push-publish-btn'))).toBeNull();
        expect(spectator.query(byTestId('users-bulk-add-to-bundle-btn'))).toBeTruthy();
    });
});
