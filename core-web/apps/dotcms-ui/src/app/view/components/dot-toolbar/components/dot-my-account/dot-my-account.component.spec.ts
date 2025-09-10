/* eslint-disable @typescript-eslint/no-empty-function */

import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { fakeAsync, tick } from '@angular/core/testing';

import { ConfirmationService } from 'primeng/api';

import {
    DotAlertConfirmService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRouterService
} from '@dotcms/data-access';
import { Auth, DotcmsConfigService, LoginService, User } from '@dotcms/dotcms-js';

import { DotMyAccountComponent } from './dot-my-account.component';

import { DotAccountService } from '../../../../../api/services/dot-account-service';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';

describe('DotMyAccountComponent', () => {
    let spectator: Spectator<DotMyAccountComponent>;
    let component: DotMyAccountComponent;

    const mockUser: User = {
        emailAddress: 'admin@dotcms.com',
        firstName: 'Admin',
        lastName: 'User',
        userId: '1'
    } as User;

    const mockAuth: Auth = {
        user: mockUser,
        loginAsUser: null
    };

    const createComponent = createComponentFactory({
        component: DotMyAccountComponent,
        providers: [
            mockProvider(DotAccountService, {
                updateUser: () => of({ entity: { user: mockUser, reauthenticate: false } }),
                addStarterPage: () => of({}),
                removeStarterPage: () => of({})
            }),
            mockProvider(DotMessageService, {
                get: (key) => key
            }),
            mockProvider(DotMenuService, {
                isPortletInMenu: () => of(true)
            }),
            mockProvider(LoginService, {
                auth$: of(mockAuth),
                watchUser: (callback) => callback(mockAuth),
                setAuth: () => {}
            }),
            mockProvider(DotRouterService, {
                doLogOut: () => {}
            }),
            mockProvider(DotAlertConfirmService, {
                alert: () => {}
            }),
            mockProvider(DotHttpErrorManagerService, {
                handle: () => of({})
            }),
            mockProvider(DotcmsConfigService, {
                getConfig: () =>
                    of({ emailRegex: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$' })
            }),
            mockProvider(ConfirmationService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
    });

    describe('Dialog Visibility', () => {
        it('should be hidden by default', () => {
            // Check that the dialog has visible=false
            expect(component.visible()).toBe(false);
            const dialog = spectator.query(byTestId('dot-my-account-dialog'));
            expect(dialog.getAttribute('ng-reflect-visible')).toBe('false');
        });

        it('should show dialog when visible is set to true', () => {
            // Use setVisible method to update the signal
            component.setVisible(true);
            spectator.detectChanges();

            expect(component.visible()).toBe(true);
            const dialog = spectator.query(byTestId('dot-my-account-dialog'));
            expect(dialog.getAttribute('ng-reflect-visible')).toBe('true');
        });

        it('should emit shutdown when dialog is closed', () => {
            jest.spyOn(component.shutdown, 'emit');
            component.setVisible(true);
            spectator.detectChanges();

            component.handleClose();
            expect(component.shutdown.emit).toHaveBeenCalled();
        });
    });

    describe('Form Fields', () => {
        beforeEach(() => {
            component.setVisible(true);
            spectator.detectChanges();
        });

        it('should have all required fields', () => {
            const requiredFields = [
                'dot-my-account-first-name-input',
                'dot-my-account-last-name-input',
                'dot-my-account-email-input',
                'dot-my-account-current-password-input'
            ];

            requiredFields.forEach((fieldId) => {
                const field = spectator.query(byTestId(fieldId)) as HTMLInputElement;
                expect(field).toBeTruthy();
            });
        });

        it('should show validation messages when form is submitted with empty fields', () => {
            // Clear form values
            const form = component.form;
            form.reset();

            // Force form validation
            Object.keys(form.controls).forEach((key) => {
                const control = form.get(key);
                if (control) {
                    control.markAsDirty();
                    control.markAsTouched();
                    control.updateValueAndValidity();
                }
            });
            spectator.detectChanges();

            // Check that validation messages are shown
            const errorMessages = spectator.queryAll('small.p-invalid:not([hidden])');
            expect(errorMessages.length).toBeGreaterThan(0);
        });

        it('should validate email format', () => {
            // Get the email control
            const emailControl = component.form.get('email');

            // Set invalid email
            emailControl?.setValue('invalid-email');
            emailControl?.markAsDirty();
            emailControl?.markAsTouched();
            emailControl?.updateValueAndValidity();
            spectator.detectChanges();

            // Verify error message is shown
            expect(emailControl?.valid).toBeFalsy();
            const errorMessage = spectator.query(byTestId('dot-my-account-email-error'));
            expect(errorMessage).toBeTruthy();

            // Set valid email
            emailControl?.setValue('valid@email.com');
            emailControl?.updateValueAndValidity();
            spectator.detectChanges();

            // Verify error message is hidden
            expect(emailControl?.valid).toBeTruthy();
        });
    });

    describe('Password Change', () => {
        beforeEach(() => {
            component.setVisible(true);
            spectator.detectChanges();
        });

        it('should enable password fields when change password is checked', () => {
            // Check initial state of checkbox and fields
            expect(component.changePasswordOption()).toBe(false);

            // Verify that password fields are initially disabled
            const newPasswordField = component.form.get('newPassword');
            const confirmPasswordField = component.form.get('confirmPassword');
            expect(newPasswordField?.disabled).toBe(true);
            expect(confirmPasswordField?.disabled).toBe(true);

            // Simulate click on checkbox using the method directly
            // since clicking the checkbox may not work correctly in tests
            component.toggleChangePasswordOption();
            spectator.detectChanges();

            // Verify that checkbox is checked and fields are enabled after the change
            expect(component.changePasswordOption()).toBe(true);
            expect(component.form.get('newPassword')?.disabled).toBe(false);
            expect(component.form.get('confirmPassword')?.disabled).toBe(false);
        });

        it('should validate password match', () => {
            // Enable password change
            component.toggleChangePasswordOption();
            spectator.detectChanges();

            // Verify that checkbox is checked and fields are enabled
            expect(component.changePasswordOption()).toBe(true);
            expect(component.form.get('newPassword')?.disabled).toBe(false);
            expect(component.form.get('confirmPassword')?.disabled).toBe(false);

            // Set different passwords
            component.form.get('newPassword')?.setValue('password1');
            component.form.get('confirmPassword')?.setValue('password2');
            component.form.get('confirmPassword')?.markAsTouched();
            component.form.get('confirmPassword')?.updateValueAndValidity();
            spectator.detectChanges();

            // Verify that there is a validation error
            const confirmPasswordControl = component.form.get('confirmPassword');
            expect(confirmPasswordControl?.hasError('passwordMismatch')).toBe(true);

            // Verify that error message is displayed
            const errorMessage = spectator.query(byTestId('dot-my-account-confirm-password-error'));
            expect(errorMessage).toBeTruthy();

            // Set matching passwords
            component.form.get('confirmPassword')?.setValue('password1');
            component.form.get('confirmPassword')?.updateValueAndValidity();
            spectator.detectChanges();

            // Verify that there is no validation error
            expect(confirmPasswordControl?.hasError('passwordMismatch')).toBe(false);

            // Verify that error message is not displayed
            const errorMessageAfterFix = spectator.query(
                byTestId('dot-my-account-confirm-password-error')
            );
            expect(errorMessageAfterFix).toBeFalsy();
        });
    });

    describe('Starter Checkbox', () => {
        beforeEach(() => {
            component.setVisible(true);
            spectator.detectChanges();
        });

        it('should call addStarterPage when checkbox is checked', () => {
            const accountService = spectator.inject(DotAccountService);
            jest.spyOn(accountService, 'addStarterPage').mockReturnValue(of({}));

            // First set showStarter to false
            component.showStarter.set(false);
            spectator.detectChanges();

            // Then set it to true and trigger setShowStarter
            component.showStarter.set(true);
            component.setShowStarter();
            spectator.detectChanges();

            expect(accountService.addStarterPage).toHaveBeenCalled();
        });

        it('should call removeStarterPage when checkbox is unchecked', () => {
            const accountService = spectator.inject(DotAccountService);
            jest.spyOn(accountService, 'removeStarterPage').mockReturnValue(of({}));

            // First set showStarter to true
            component.showStarter.set(true);
            spectator.detectChanges();

            // Then set it to false and trigger setShowStarter
            component.showStarter.set(false);
            component.setShowStarter();
            spectator.detectChanges();

            expect(accountService.removeStarterPage).toHaveBeenCalled();
        });
    });

    describe('Form Submission', () => {
        beforeEach(() => {
            component.setVisible(true);
            spectator.detectChanges();
        });

        it('should update user successfully', fakeAsync(() => {
            // Arrange
            const accountService = spectator.inject(DotAccountService);
            const loginService = spectator.inject(LoginService);

            // Spy on updateUser and setAuth methods
            const updateUserSpy = jest.jest
                .spyOn(accountService, 'updateUser')
                .mockReturnValue(of({ entity: { user: mockUser, reauthenticate: false } }));
            const setAuthSpy = jest.spyOn(loginService, 'setAuth');

            // Fill form with valid data
            component.form.patchValue({
                userId: '1',
                givenName: 'John',
                surname: 'Doe',
                email: 'john@doe.com',
                currentPassword: 'currentPass'
            });

            // Force form validation
            Object.keys(component.form.controls).forEach((key) => {
                const control = component.form.get(key);
                if (control) {
                    control.markAsDirty();
                    control.markAsTouched();
                    control.updateValueAndValidity();
                }
            });
            spectator.detectChanges();

            // Act - Call save method directly
            component.save();

            // Use tick to complete all async operations
            tick(100);
            spectator.detectChanges();

            // Assert
            // Verify updateUser was called with correct data
            expect(updateUserSpy).toHaveBeenCalledWith({
                userId: '1',
                givenName: 'John',
                surname: 'Doe',
                email: 'john@doe.com',
                currentPassword: 'currentPass'
            });

            // Verify setAuth was called with correct data
            expect(setAuthSpy).toHaveBeenCalledWith({
                loginAsUser: null,
                user: mockUser
            });

            // Verify form status was reset
            expect(component.formStatus()).not.toBe('saving');
        }));

        it('should handle current password error', fakeAsync(() => {
            const accountService = spectator.inject(DotAccountService);
            jest.spyOn(accountService, 'updateUser').mockReturnValue(
                throwError({
                    status: 400,
                    error: {
                        errors: [
                            {
                                errorCode: 'User-Info-Confirm-Current-Password-Failed',
                                message: 'Invalid current password'
                            }
                        ]
                    }
                })
            );

            // Fill form with valid data
            component.form.patchValue({
                userId: '1',
                givenName: 'John',
                surname: 'Doe',
                email: 'john@doe.com',
                currentPassword: 'currentPass'
            });

            // Force form validation
            Object.keys(component.form.controls).forEach((key) => {
                const control = component.form.get(key);
                if (control) {
                    control.markAsDirty();
                    control.markAsTouched();
                    control.updateValueAndValidity();
                }
            });
            spectator.detectChanges();

            // Call save method directly instead of clicking the button
            component.save();

            // Use tick to complete all async operations
            tick();
            spectator.detectChanges();

            expect(component.confirmPasswordFailedMsg()).toBe('Invalid current password');
            expect(spectator.query(byTestId('dot-my-account-current-password-error'))).toBeTruthy();
        }));

        it('should handle new password error', fakeAsync(() => {
            const accountService = spectator.inject(DotAccountService);
            jest.spyOn(accountService, 'updateUser').mockReturnValue(
                throwError({
                    status: 400,
                    error: {
                        errors: [
                            {
                                errorCode: 'User-Info-Save-Password-Failed',
                                message: 'Invalid new password'
                            }
                        ]
                    }
                })
            );

            // Enable password change
            component.toggleChangePasswordOption();
            spectator.detectChanges();

            // Fill form with valid data
            component.form.patchValue({
                userId: '1',
                givenName: 'John',
                surname: 'Doe',
                email: 'john@doe.com',
                currentPassword: 'currentPass',
                newPassword: 'newPass',
                confirmPassword: 'newPass'
            });

            // Force form validation
            Object.keys(component.form.controls).forEach((key) => {
                const control = component.form.get(key);
                if (control && !control.disabled) {
                    control.markAsDirty();
                    control.markAsTouched();
                    control.updateValueAndValidity();
                }
            });
            spectator.detectChanges();

            // Call save method directly instead of clicking the button
            component.save();

            // Use tick to complete all async operations
            tick();
            spectator.detectChanges();

            // Check that the error message is displayed
            expect(component.newPasswordFailedMsg()).toBe('Invalid new password');
            expect(spectator.query(byTestId('dot-my-account-new-password-error'))).toBeTruthy();
        }));

        it('should handle generic error', fakeAsync(() => {
            const accountService = spectator.inject(DotAccountService);
            const errorService = spectator.inject(DotHttpErrorManagerService);
            jest.spyOn(accountService, 'updateUser').mockReturnValue(
                throwError({
                    status: 500,
                    error: {
                        errors: [
                            {
                                errorCode: 'Generic-Error',
                                message: 'Something went wrong'
                            }
                        ]
                    }
                })
            );
            jest.spyOn(errorService, 'handle').mockReturnValue(of({}));

            // Fill form with valid data
            component.form.patchValue({
                userId: '1',
                givenName: 'John',
                surname: 'Doe',
                email: 'john@doe.com',
                currentPassword: 'currentPass'
            });

            // Force form validation
            Object.keys(component.form.controls).forEach((key) => {
                const control = component.form.get(key);
                if (control) {
                    control.markAsDirty();
                    control.markAsTouched();
                    control.updateValueAndValidity();
                }
            });
            spectator.detectChanges();

            // Call save method directly instead of clicking the button
            component.save();

            // Use tick to complete all async operations
            tick();
            spectator.detectChanges();

            expect(errorService.handle).toHaveBeenCalled();
        }));

        it('should handle reauthentication requirement', fakeAsync(() => {
            const accountService = spectator.inject(DotAccountService);
            const routerService = spectator.inject(DotRouterService);
            jest.spyOn(accountService, 'updateUser').mockReturnValue(
                of({ entity: { user: mockUser, reauthenticate: true } })
            );
            jest.spyOn(routerService, 'doLogOut');

            // Fill form with valid data
            component.form.patchValue({
                userId: '1',
                givenName: 'John',
                surname: 'Doe',
                email: 'john@doe.com',
                currentPassword: 'currentPass'
            });

            // Force form validation
            Object.keys(component.form.controls).forEach((key) => {
                const control = component.form.get(key);
                if (control) {
                    control.markAsDirty();
                    control.markAsTouched();
                    control.updateValueAndValidity();
                }
            });
            spectator.detectChanges();

            // Call save method directly instead of clicking the button
            component.save();

            // Use tick to complete all async operations
            tick();
            spectator.detectChanges();

            expect(routerService.doLogOut).toHaveBeenCalled();
        }));
    });
});
