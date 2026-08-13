import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotRoleUsersTabComponent } from './dot-role-users-tab.component';

import { DotRolesStore } from '../../store/dot-roles.store';

const MESSAGES = {
    'roles.users.grant': 'Grant to User',
    'roles.users.remove': 'Remove',
    'roles.users.remove.blocked': 'blocked',
    'roles.users.empty.title': 'No users',
    'roles.users.empty.copy': 'Grant a user',
    'roles.users.search.placeholder': 'Search',
    'roles.users.grant.blocked': 'grant blocked',
    'roles.users.column.name': 'Name',
    'roles.users.column.email': 'Email',
    'roles.users.column.granted-from': 'Granted From',
    loading: 'Loading',
    'error.load-failed': 'Failed'
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
                membersStatus: jest.fn().mockReturnValue('loaded'),
                selectedMembers: jest.fn().mockReturnValue([]),
                selectedRoleId: jest.fn().mockReturnValue('r-eco'),
                setSelectedMembers: jest.fn()
            })
        ],
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }]
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

    it('should disable the bulk-remove button when nothing is selected', () => {
        spectator.detectChanges();

        const btn = spectator.query(byTestId('bulk-remove-btn')) as HTMLButtonElement;

        expect(btn.disabled).toBe(true);
    });

    it('should render the Grant to User button', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('grant-user-btn'))).toBeTruthy();
    });
});
