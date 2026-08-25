import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { EMPTY, of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotRoleUsersTabComponent } from './dot-role-users-tab.component';

import { DotRolesPortletService } from '../../../services/dot-roles-portlet.service';
import { DotRolesStore } from '../../store/dot-roles.store';

const MESSAGES = {
    'roles.users.grant': 'Grant to User',
    'roles.users.remove': 'Remove',
    'roles.users.confirm.remove.header': 'Remove user',
    'roles.users.confirm.remove.message': 'Remove {0}?',
    'roles.action.cancel': 'Cancel',
    'roles.users.empty.title': 'No users',
    'roles.users.empty.copy': 'Grant a user',
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
                members: jest.fn().mockReturnValue([]),
                membersStatus: jest.fn().mockReturnValue('LOADED'),
                selectedRole: jest.fn().mockReturnValue({
                    id: 'r-eco',
                    name: 'Eco Role',
                    roleKey: 'eco',
                    editUsers: true
                }),
                selectedRoleId: jest.fn().mockReturnValue('r-eco'),
                selectedRoleStatus: jest.fn().mockReturnValue('LOADED'),
                canGrantUsers: jest.fn().mockReturnValue(true),
                loadMembers: jest.fn(),
                grantUserToRole: jest.fn().mockResolvedValue(null),
                removeUsersFromRole: jest.fn().mockResolvedValue(null)
            }),
            mockProvider(ConfirmationService, {
                confirm: jest.fn().mockImplementation((cfg) => cfg.accept?.()),
                requireConfirmation$: EMPTY,
                accept: EMPTY,
                reject: EMPTY
            }),
            mockProvider(DotRolesPortletService, {
                searchUsers: jest.fn().mockReturnValue(of([]))
            })
        ],
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) },
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() })
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render the empty state when there are no members', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('members-empty'))).toBeTruthy();
        expect(spectator.query(byTestId('members-table'))).toBeNull();
    });

    it('should render the members table when members are loaded', () => {
        const store = spectator.inject(DotRolesStore, true);
        (store.members as jest.Mock).mockReturnValue([
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
        (store.members as jest.Mock).mockReturnValue([
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
        (store.members as jest.Mock).mockReturnValue([member]);
        spectator.detectChanges();

        (
            spectator.component as unknown as {
                onRemoveMember: (m: typeof member) => void;
            }
        ).onRemoveMember(member);
        await Promise.resolve();

        expect(store.removeUsersFromRole).toHaveBeenCalledWith(['u-1']);
    });

    it('should NOT render the bulk-remove button (removed with design update)', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('bulk-remove-btn'))).toBeNull();
    });

    it('should render the Grant to User button', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('grant-user-btn'))).toBeTruthy();
    });
});
