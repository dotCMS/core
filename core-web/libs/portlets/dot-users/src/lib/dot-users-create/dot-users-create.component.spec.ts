import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUsersCreateComponent } from './dot-users-create.component';

import { DotUsersService } from '../services/dot-users.service';
import { createFakeUser, createFakeUserDetail } from '../testing/dot-user.mock';

const MOCK_USER = createFakeUser();
const MOCK_USER_DETAIL = createFakeUserDetail();

/** p-button doesn't forward clicks itself — reach the inner native button. */
function saveButton(spectator: Spectator<DotUsersCreateComponent>): HTMLButtonElement {
    return spectator
        .query(byTestId('users-dialog-save-btn'))!
        .querySelector('button') as HTMLButtonElement;
}

const MESSAGES = {
    'users.dialog.tabs.profile': 'Profile',
    'users.dialog.tabs.roles': 'Roles',
    'users.dialog.tabs.permissions': 'Permissions',
    'users.dialog.tabs.api-tokens': 'API Tokens',
    'users.dialog.new-user': 'New User',
    'users.dialog.untitled-user': 'Untitled User',
    'users.dialog.status.active': 'Active',
    'users.dialog.status.inactive': 'Inactive',
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
                getUserRoles: jest.fn().mockReturnValue(
                    of([
                        { id: 'role-back', roleKey: 'DOTCMS_BACK_END_USER' },
                        { id: 'role-personal', roleKey: 'user-42' }
                    ])
                ),
                getAllRoles: jest.fn().mockReturnValue(of([])),
                getGettingStartedState: jest.fn().mockReturnValue(of(false)),
                setGettingStarted: jest.fn().mockReturnValue(of({})),
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

        it('should NOT render the status chip in the header when no user is provided', () => {
            expect(spectator.query(byTestId('users-dialog-header-status'))).toBeFalsy();
        });

        it('should show NU as the initials placeholder', () => {
            expect(spectator.component.$initials()).toBe('NU');
        });

        it('should mark password as required in create mode', () => {
            const password = spectator.component.form.controls.account.controls.password;
            expect(password.hasError('required')).toBe(true);
        });

        it('should not close on save when the form is invalid', () => {
            spectator.click(saveButton(spectator));

            expect(dialogRef.close).not.toHaveBeenCalled();
            // markAllAsTouched runs on invalid Save so field-level
            // errors surface immediately — verify by looking at the
            // required first-name control's touched state.
            expect(spectator.component.form.controls.account.controls.firstName.touched).toBe(true);
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
            spectator.detectChanges();

            spectator.click(saveButton(spectator));

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
            expect(spectator.component['$canConfirmDelete']()).toBe(false);
        });

        it('should render Save enabled in create mode (dataReady is true immediately)', () => {
            const button = saveButton(spectator);
            expect(button.disabled).toBe(false);
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

        it('should hydrate access toggles from the loaded role keys', () => {
            const access = spectator.component.form.controls.access.getRawValue();
            // The mock returns DOTCMS_BACK_END_USER + the user's personal role.
            expect(access.backend).toBe(true);
            expect(access.cmsAdmin).toBe(false);
            expect(access.frontend).toBe(false);
        });

        it('should derive canLoginToAdmin from CMS Admin or Back-end toggles', () => {
            const access = spectator.component.form.controls.access.controls;

            // Loaded user has DOTCMS_BACK_END_USER → chip shows.
            expect(spectator.component.$canLoginToAdmin()).toBe(true);

            access.backend.setValue(false);
            expect(spectator.component.$canLoginToAdmin()).toBe(false);

            access.cmsAdmin.setValue(true);
            expect(spectator.component.$canLoginToAdmin()).toBe(true);

            access.cmsAdmin.setValue(false);
            expect(spectator.component.$canLoginToAdmin()).toBe(false);
        });

        it('should merge access toggles into `roles` and drop the personal role on save', () => {
            spectator.component.form.controls.access.patchValue({
                cmsAdmin: true,
                backend: true,
                frontend: false
            });

            spectator.detectChanges();
            spectator.click(saveButton(spectator));

            expect(dialogRef.close).toHaveBeenCalledWith(
                expect.objectContaining({
                    action: 'save',
                    mode: 'update',
                    payload: expect.objectContaining({
                        userId: 'user-42',
                        // Personal role (roleKey === userId) is filtered
                        // out to avoid the backend `Cannot alter users
                        // on this role` guard on editUsers=false roles.
                        roles: expect.arrayContaining(['DOTCMS_BACK_END_USER', 'CMS Administrator'])
                    })
                })
            );

            const call = (dialogRef.close as jest.Mock).mock.calls[0][0] as {
                payload: { roles: string[] };
            };
            expect(call.payload.roles).not.toContain('user-42');
        });

        it('should emit `gettingStartedChange: add` when the toggle flips ON', () => {
            spectator.component.form.controls.access.patchValue({ showGettingStarted: true });

            spectator.detectChanges();
            spectator.click(saveButton(spectator));

            expect(dialogRef.close).toHaveBeenCalledWith(
                expect.objectContaining({ gettingStartedChange: 'add' })
            );
        });

        const REPLACEMENT_USER = createFakeUser({
            userId: 'user-99',
            id: 'user-99',
            emailAddress: 'nobody@dotcms.com',
            firstName: 'Nobody',
            lastName: 'Else',
            fullName: 'Nobody Else',
            name: 'Nobody Else'
        });

        it('should require both email match AND a replacement user to enable delete confirmation', () => {
            spectator.component['openDeleteConfirm']();
            expect(spectator.component['$canConfirmDelete']()).toBe(false);

            spectator.component['onDeleteInputChange']('jane@dotcms.com');
            expect(spectator.component['$canConfirmDelete']()).toBe(false);

            spectator.component['onReplacementSelect'](REPLACEMENT_USER);
            expect(spectator.component['$canConfirmDelete']()).toBe(true);
        });

        it('should keep delete confirmation disabled when the picked replacement is the same user', () => {
            spectator.component['openDeleteConfirm']();
            spectator.component['onDeleteInputChange']('jane@dotcms.com');
            spectator.component['onReplacementSelect'](MOCK_USER);

            expect(spectator.component['$canConfirmDelete']()).toBe(false);
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

            expect(spectator.component['$replacementUser']()).toBeNull();
        });
    });
});
