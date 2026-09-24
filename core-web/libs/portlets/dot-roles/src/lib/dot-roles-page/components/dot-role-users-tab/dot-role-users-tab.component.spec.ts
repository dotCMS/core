import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { EMPTY, of } from 'rxjs';
import { Mock, vi } from 'vitest';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { Popover } from 'primeng/popover';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotRoleUsersTabComponent } from './dot-role-users-tab.component';

import { DotRolesPortletService } from '../../../services/dot-roles-portlet.service';
import { DotRolesStore } from '../../store/dot-roles.store';

const MESSAGES = {
    'roles.users.add': 'Add User',
    'roles.users.filter.placeholder': 'Search user',
    'roles.users.filter.empty.title': 'No users match your search',
    'roles.users.remove': 'Remove',
    'roles.users.confirm.remove.header': 'Remove user',
    'roles.users.confirm.remove.message': 'Remove {0}?',
    'roles.action.cancel': 'Cancel',
    'roles.users.empty.title': 'No users',
    'roles.users.empty.copy': 'Add a user',
    'roles.users.search.placeholder': 'Search',
    'roles.users.column.name': 'Name',
    'roles.users.column.email': 'Email',
    'roles.users.column.granted-from': 'Granted From',
    loading: 'Loading',
    'roles.error.load-failed': 'Failed'
};

describe('DotRoleUsersTabComponent', () => {
    let spectator: Spectator<DotRoleUsersTabComponent>;

    const createComponent = createComponentFactory({
        component: DotRoleUsersTabComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        detectChanges: false,
        imports: [HttpClientTestingModule],
        componentProviders: [
            mockProvider(DotRolesStore, {
                members: vi.fn().mockReturnValue([]),
                membersStatus: vi.fn().mockReturnValue('LOADED'),
                selectedRole: vi.fn().mockReturnValue({
                    id: 'r-eco',
                    name: 'Eco Role',
                    roleKey: 'eco',
                    editUsers: true
                }),
                selectedRoleId: vi.fn().mockReturnValue('r-eco'),
                selectedRoleStatus: vi.fn().mockReturnValue('LOADED'),
                membersFilter: vi.fn().mockReturnValue(''),
                canGrantUsers: vi.fn().mockReturnValue(true),
                loadMembers: vi.fn(),
                setMembersFilter: vi.fn(),
                grantUserToRole: vi.fn().mockResolvedValue(null),
                removeUsersFromRole: vi.fn().mockResolvedValue(null)
            }),
            mockProvider(ConfirmationService, {
                confirm: vi.fn().mockImplementation((cfg) => cfg.accept?.()),
                requireConfirmation$: EMPTY,
                accept: EMPTY,
                reject: EMPTY
            }),
            mockProvider(DotRolesPortletService, {
                searchUsers: vi.fn().mockReturnValue(of([]))
            })
        ],
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) },
            mockProvider(DotHttpErrorManagerService, { handle: vi.fn() })
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        // `mockProvider` builds its vi.fn()s once at factory scope, so a return
        // value set by one test would otherwise become every later test's.
        const store = spectator.inject(DotRolesStore, true);
        (store.members as Mock).mockReturnValue([]);
        (store.membersFilter as Mock).mockReturnValue('');
        (store.canGrantUsers as Mock).mockReturnValue(true);
        vi.clearAllMocks();
    });

    it('should render the empty state when there are no members', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('members-empty'))).toBeTruthy();
        expect(spectator.query(byTestId('members-table'))).toBeNull();
    });

    it('should render the members table when members are loaded', () => {
        const store = spectator.inject(DotRolesStore, true);
        (store.members as Mock).mockReturnValue([
            {
                userId: 'u-1',
                firstName: 'Alan',
                lastName: 'Cruz',
                emailAddress: 'alan.cruz@dotcms.com',
                grantedFromRoleId: 'r-eco',
                grantedFromRoleName: 'Eco Role'
            }
        ]);
        spectator.detectChanges();

        expect(spectator.query(byTestId('members-table'))).toBeTruthy();
        expect(spectator.query(byTestId('member-row-u-1'))).toBeTruthy();
    });

    it('should render a per-row Remove button ONLY for direct-grant members', () => {
        const store = spectator.inject(DotRolesStore, true);
        (store.members as Mock).mockReturnValue([
            {
                userId: 'u-1',
                firstName: 'Alan',
                lastName: 'Cruz',
                emailAddress: 'alan.cruz@dotcms.com',
                grantedFromRoleId: 'r-eco',
                grantedFromRoleName: 'Eco Role'
            },
            {
                userId: 'u-2',
                firstName: 'Elena',
                lastName: 'Petrov',
                emailAddress: 'elena.p@dotcms.com',
                grantedFromRoleId: 'r-ancestor',
                grantedFromRoleName: 'Ancestor'
            }
        ]);
        spectator.detectChanges();

        expect(spectator.query(byTestId('member-remove-u-1'))).toBeTruthy();
        expect(spectator.query(byTestId('member-remove-u-2'))).toBeNull();
    });

    it('should confirm + call removeUsersFromRole with the row user id', async () => {
        const store = spectator.inject(DotRolesStore, true);
        const member = {
            userId: 'u-1',
            firstName: 'Alan',
            lastName: 'Cruz',
            emailAddress: 'alan.cruz@dotcms.com',
            grantedFromRoleId: 'r-eco',
            grantedFromRoleName: 'Eco Role'
        };
        (store.members as Mock).mockReturnValue([member]);
        spectator.detectChanges();

        // Click the row-level trash — PrimeNG wraps the button, so we
        // reach through to the inner native <button>.
        const removeBtn = spectator.query(byTestId('member-remove-u-1'))?.querySelector('button');
        expect(removeBtn).toBeTruthy();
        spectator.click(removeBtn as HTMLElement);
        await Promise.resolve();

        expect(store.removeUsersFromRole).toHaveBeenCalledWith(['u-1']);
    });

    it('should NOT render the bulk-remove button (removed with design update)', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('bulk-remove-btn'))).toBeNull();
    });

    describe('toolbar', () => {
        it('should replace Grant to User with a search box and an outlined Add User', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('grant-user-btn'))).toBeNull();
            expect(spectator.query(byTestId('members-search-input'))).toBeTruthy();

            const addBtn = spectator.query(byTestId('add-user-btn'))?.querySelector('button');
            expect(addBtn?.textContent).toContain('Add User');
            expect(addBtn?.classList).toContain('p-button-outlined');
            expect(addBtn?.disabled).toBe(false);
        });

        it('should disable Add User and keep the notice when the role cannot be granted', () => {
            const store = spectator.inject(DotRolesStore, true);
            (store.canGrantUsers as Mock).mockReturnValue(false);
            spectator.detectChanges();

            const addBtn = spectator.query(byTestId('add-user-btn'))?.querySelector('button');
            expect(addBtn?.disabled).toBe(true);
            expect(spectator.query(byTestId('cannot-grant-notice'))).toBeTruthy();
        });

        it('should open the user picker from Add User', () => {
            spectator.detectChanges();
            const popover = spectator.query(Popover);
            expect(popover).toBeTruthy();
            const toggle = vi.spyOn(popover as Popover, 'toggle').mockReturnValue(undefined);

            const addBtn = spectator.query(byTestId('add-user-btn'))?.querySelector('button');
            spectator.click(addBtn as HTMLElement);

            expect(toggle).toHaveBeenCalled();
        });
    });

    describe('member search', () => {
        beforeEach(() => vi.useFakeTimers());
        afterEach(() => vi.useRealTimers());

        it('should send the typed term to the store once typing pauses', () => {
            const store = spectator.inject(DotRolesStore, true);
            spectator.detectChanges();

            spectator.typeInElement('jan', byTestId('members-search-input'));
            spectator.typeInElement('jane', byTestId('members-search-input'));
            vi.advanceTimersByTime(299);
            expect(store.setMembersFilter).not.toHaveBeenCalled();

            vi.advanceTimersByTime(1);
            expect(store.setMembersFilter).toHaveBeenCalledTimes(1);
            expect(store.setMembersFilter).toHaveBeenCalledWith('jane');
        });

        it('should send an emptied box so the full list comes back', () => {
            const store = spectator.inject(DotRolesStore, true);
            spectator.detectChanges();

            spectator.typeInElement('jane', byTestId('members-search-input'));
            vi.advanceTimersByTime(300);
            spectator.typeInElement('', byTestId('members-search-input'));
            vi.advanceTimersByTime(300);

            expect(store.setMembersFilter).toHaveBeenLastCalledWith('');
        });
    });

    it('should show a no-results state, not the no-members one, when a search matches nobody', () => {
        const store = spectator.inject(DotRolesStore, true);
        (store.membersFilter as Mock).mockReturnValue('zzz');
        spectator.detectChanges();

        expect(spectator.query(byTestId('members-search-empty'))).toBeTruthy();
        expect(spectator.query(byTestId('members-empty'))).toBeNull();
        // The toolbar stays, so the admin can change or clear the search.
        expect(spectator.query(byTestId('members-search-input'))).toBeTruthy();
    });
});
