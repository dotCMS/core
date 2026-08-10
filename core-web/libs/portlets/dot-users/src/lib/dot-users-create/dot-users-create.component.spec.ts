import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUsersCreateComponent } from './dot-users-create.component';

import { DotUserDetail, DotUserListItem, DotUsersService } from '../services/dot-users.service';

const MOCK_USER: DotUserListItem = {
    userId: 'user-42',
    id: 'user-42',
    firstName: 'Jane',
    lastName: 'Doe',
    fullName: 'Jane Doe',
    name: 'Jane Doe',
    emailAddress: 'jane@dotcms.com',
    gravitar: '',
    active: true,
    admin: false,
    backendUser: true,
    frontendUser: false,
    hasConsoleAccess: true,
    lastLoginDate: null,
    lastLoginIP: null,
    failedLoginAttempts: 0
};

const MOCK_USER_DETAIL: DotUserDetail = {
    ...MOCK_USER,
    birthday: null,
    middleName: null,
    nickname: null,
    languageId: 'en-US',
    timeZoneId: null,
    male: null,
    female: null,
    additionalInfo: null,
    createDate: null,
    modificationDate: null
};

const MESSAGES = {
    'users.dialog.tabs.profile': 'Profile',
    'users.dialog.tabs.roles': 'Roles',
    'users.dialog.tabs.permissions': 'Permissions',
    'users.dialog.tabs.api-tokens': 'API Tokens',
    'users.dialog.new-user': 'New User',
    'users.dialog.untitled-user': 'Untitled User',
    'users.dialog.status.active': 'Active',
    'users.dialog.status.inactive': 'Inactive',
    'users.dialog.create.subtitle': 'Fill in the details below to create this user',
    'users.dialog.save': 'Save Changes',
    'users.dialog.create': 'Create User',
    'users.dialog.delete.button': 'Delete User',
    'users.dialog.delete-confirm.header': 'Delete user',
    'users.cancel': 'Cancel'
};

describe('DotUsersCreateComponent', () => {
    let spectator: Spectator<DotUsersCreateComponent>;
    let dialogRef: DynamicDialogRef;

    const createComponent = createComponentFactory({
        component: DotUsersCreateComponent,
        imports: [NoopAnimationsModule],
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) },
            mockProvider(DotUsersService, {
                getUser: jest.fn().mockReturnValue(of(MOCK_USER_DETAIL)),
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
            }),
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() })
        ]
    });

    describe('create mode', () => {
        beforeEach(() => {
            dialogRef = { close: jest.fn() } as unknown as DynamicDialogRef;
            spectator = createComponent({
                providers: [
                    { provide: DynamicDialogRef, useValue: dialogRef },
                    { provide: DynamicDialogConfig, useValue: { data: {} } }
                ]
            });
        });

        it('should render the four tabs', () => {
            expect(spectator.query(byTestId('users-dialog-tab-profile'))).toBeTruthy();
            expect(spectator.query(byTestId('users-dialog-tab-roles'))).toBeTruthy();
            expect(spectator.query(byTestId('users-dialog-tab-permissions'))).toBeTruthy();
            expect(spectator.query(byTestId('users-dialog-tab-api-tokens'))).toBeTruthy();
        });

        it('should render the create subtitle in the header when no user is provided', () => {
            expect(spectator.query(byTestId('users-dialog-header-subtitle'))).toBeTruthy();
            expect(spectator.query(byTestId('users-dialog-header-status'))).toBeFalsy();
        });

        it('should show NU as the initials placeholder', () => {
            expect(spectator.component.initials()).toBe('NU');
        });

        it('should mark password as required in create mode', () => {
            const password = spectator.component.form.controls.account.controls.password;
            expect(password.hasError('required')).toBe(true);
        });

        it('should not close on save when the form is invalid', () => {
            spectator.component['save']();

            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('should close with the form value on save when valid', () => {
            spectator.component.form.patchValue({
                account: {
                    firstName: 'Ada',
                    lastName: 'Lovelace',
                    email: 'ada@dotcms.com',
                    password: 'Xy7#abcdef',
                    confirmPassword: 'Xy7#abcdef'
                }
            });

            spectator.component['save']();

            expect(dialogRef.close).toHaveBeenCalledWith(
                expect.objectContaining({ action: 'save' })
            );
        });

        it('should surface passwordMismatch when passwords do not match', () => {
            spectator.component.form.patchValue({
                account: {
                    password: 'abc123',
                    confirmPassword: 'different'
                }
            });

            expect(spectator.component.form.controls.account.errors?.['passwordMismatch']).toBe(
                true
            );
        });

        it('should keep canConfirmDelete false when the user is missing', () => {
            expect(spectator.component['canConfirmDelete']()).toBe(false);
        });
    });

    describe('edit mode', () => {
        beforeEach(() => {
            dialogRef = { close: jest.fn() } as unknown as DynamicDialogRef;
            spectator = createComponent({
                providers: [
                    { provide: DynamicDialogRef, useValue: dialogRef },
                    { provide: DynamicDialogConfig, useValue: { data: { user: MOCK_USER } } }
                ]
            });
        });

        it('should hydrate the form from the user data', () => {
            const account = spectator.component.form.controls.account.value;
            expect(account.firstName).toBe('Jane');
            expect(account.lastName).toBe('Doe');
            expect(account.email).toBe('jane@dotcms.com');
            expect(account.active).toBe(true);
        });

        it('should render the status chip in the header', () => {
            expect(spectator.query(byTestId('users-dialog-header-status'))).toBeTruthy();
        });

        it('should NOT mark password as required in edit mode', () => {
            const password = spectator.component.form.controls.account.controls.password;
            expect(password.hasError('required')).toBe(false);
        });

        const REPLACEMENT_USER: DotUserListItem = {
            ...MOCK_USER,
            userId: 'user-99',
            id: 'user-99',
            emailAddress: 'nobody@dotcms.com',
            firstName: 'Nobody',
            lastName: 'Else',
            fullName: 'Nobody Else',
            name: 'Nobody Else'
        };

        it('should require both email match AND a replacement user to enable delete confirmation', () => {
            spectator.component['openDeleteConfirm']();
            expect(spectator.component['canConfirmDelete']()).toBe(false);

            spectator.component['onDeleteInputChange']('jane@dotcms.com');
            expect(spectator.component['canConfirmDelete']()).toBe(false);

            spectator.component['onReplacementSelect'](REPLACEMENT_USER);
            expect(spectator.component['canConfirmDelete']()).toBe(true);
        });

        it('should keep delete confirmation disabled when the picked replacement is the same user', () => {
            spectator.component['openDeleteConfirm']();
            spectator.component['onDeleteInputChange']('jane@dotcms.com');
            spectator.component['onReplacementSelect'](MOCK_USER);

            expect(spectator.component['canConfirmDelete']()).toBe(false);
        });

        it('should close with delete action carrying the replacement userId when confirmed', () => {
            spectator.component['openDeleteConfirm']();
            spectator.component['onDeleteInputChange']('jane@dotcms.com');
            spectator.component['onReplacementSelect'](REPLACEMENT_USER);
            spectator.component['confirmDelete']();

            expect(dialogRef.close).toHaveBeenCalledWith(
                expect.objectContaining({
                    action: 'delete',
                    userId: 'user-42',
                    replacementUserId: 'user-99'
                })
            );
        });

        it('should not close when confirmDelete is called without matching input', () => {
            spectator.component['openDeleteConfirm']();
            spectator.component['confirmDelete']();

            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('should reset the replacement user when the delete dialog re-opens', () => {
            spectator.component['openDeleteConfirm']();
            spectator.component['onReplacementSelect'](REPLACEMENT_USER);
            spectator.component['closeDeleteConfirm']();

            spectator.component['openDeleteConfirm']();

            expect(spectator.component['replacementUser']()).toBeNull();
        });
    });
});
