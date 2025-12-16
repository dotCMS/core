import { Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    OnDestroy,
    OnInit,
    Output,
    computed,
    inject,
    model,
    signal
} from '@angular/core';
import {
    AbstractControl,
    FormBuilder,
    FormGroup,
    FormsModule,
    ReactiveFormsModule,
    ValidationErrors,
    ValidatorFn,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

import { map, take, takeUntil } from 'rxjs/operators';

import {
    DotAlertConfirmService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRouterService
} from '@dotcms/data-access';
import { Auth, DotcmsConfigService, LoginService, User } from '@dotcms/dotcms-js';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import { DotAccountService, DotAccountUser } from '../../../../../api/services/dot-account-service';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';

enum FormStatus {
    INIT = 'init',
    SAVING = 'saving',
    ERROR = 'error'
}

@Component({
    selector: 'dot-my-account',
    styleUrls: ['./dot-my-account.component.scss'],
    templateUrl: 'dot-my-account.component.html',
    imports: [
        ReactiveFormsModule,
        FormsModule,
        ButtonModule,
        PasswordModule,
        InputTextModule,
        DialogModule,
        CheckboxModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    providers: [DotAlertConfirmService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotMyAccountComponent implements OnInit, OnDestroy {
    @Output() shutdown = new EventEmitter<void>();

    // Signals
    readonly visible = model(false);
    readonly formStatus = signal<FormStatus>(FormStatus.INIT);
    readonly showStarter = signal(false);
    readonly changePasswordOption = model(false);
    readonly confirmPasswordFailedMsg = signal('');
    readonly newPasswordFailedMsg = signal('');
    readonly isSaving = computed(() => this.formStatus() === FormStatus.SAVING);

    // Error messages
    readonly errorMessages = computed(() => {
        return {
            firstName: this.dotMessageService.get(
                'error.form.mandatory',
                this.dotMessageService.get('First-Name')
            ),
            lastName: this.dotMessageService.get(
                'error.form.mandatory',
                this.dotMessageService.get('Last-Name')
            ),
            email: {
                required: this.dotMessageService.get(
                    'error.form.mandatory',
                    this.dotMessageService.get('email-address')
                ),
                pattern: this.dotMessageService.get(
                    'errors.email',
                    this.dotMessageService.get('email-address')
                )
            },
            passwordsDontMatch: this.dotMessageService.get(
                'error.forgot.password.passwords.dont.match'
            )
        };
    });

    // Form
    form: FormGroup;
    emailRegex = '';

    // Private fields
    private readonly destroy$ = new Subject<boolean>();
    private readonly currentUser = signal<User | null>(null);

    // Dependency injection
    private readonly fb = inject(FormBuilder);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly dotAccountService = inject(DotAccountService);
    private readonly dotcmsConfigService = inject(DotcmsConfigService);
    private readonly loginService = inject(LoginService);
    private readonly dotRouterService = inject(DotRouterService);
    private readonly dotMenuService = inject(DotMenuService);
    private readonly httpErrorManagerService = inject(DotHttpErrorManagerService);
    private readonly dotAlertConfirmService = inject(DotAlertConfirmService);

    constructor() {
        // Create the form as early as possible
        this.initForm();

        // Get regex for email validation
        this.dotcmsConfigService
            .getConfig()
            .pipe(takeUntil(this.destroy$))
            .subscribe((res) => {
                this.emailRegex = res.emailRegex;
                // Update email validator with regex
                this.updateEmailValidator();
            });

        // Save current user to use when opening the dialog
        this.loginService.watchUser(this.handleUserChange.bind(this));
    }

    ngOnInit(): void {
        this.dotMenuService
            .isPortletInMenu('starter')
            .pipe(take(1))
            .subscribe((showStarter: boolean) => {
                this.showStarter.set(showStarter);
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Sets the visibility of the dialog
     * @param value Whether the dialog should be visible
     */
    setVisible(value: boolean): void {
        this.visible.set(value);

        // Reset form state when dialog is opened or closed
        if (value === false) {
            this.formStatus.set(FormStatus.INIT);
        } else if (value === true) {
            this.resetFormState();

            // Load user data when dialog is opened
            if (this.currentUser()) {
                this.updateFormWithUserData(this.currentUser());
            }
        }
    }

    /**
     * Toggles the change password option and updates form controls accordingly
     */
    toggleChangePasswordOption(): void {
        // Update checkbox state
        this.changePasswordOption.update((value) => !value);

        // Update form controls according to new state
        if (this.changePasswordOption()) {
            this.form.get('newPassword')?.enable();
            this.form.get('confirmPassword')?.enable();
            // Apply password match validator
            this.form.get('confirmPassword')?.updateValueAndValidity();
        } else {
            this.form.get('newPassword')?.disable();
            this.form.get('confirmPassword')?.disable();
            this.form.get('newPassword')?.setValue('');
            this.form.get('confirmPassword')?.setValue('');
            this.newPasswordFailedMsg.set('');
        }
    }

    /**
     * Calls Api based on checked input to add/remove starter portlet from menu
     */
    setShowStarter(): void {
        if (this.showStarter()) {
            this.dotAccountService.addStarterPage().subscribe();
        } else {
            this.dotAccountService.removeStarterPage().subscribe();
        }
    }

    /**
     * Handles dialog close action
     * Resets loading state and emits shutdown event
     */
    handleClose(): void {
        this.formStatus.set(FormStatus.INIT);
        // TODO: The 'emit' function requires a mandatory void argument
        this.shutdown.emit();
    }

    /**
     * Saves the user account information
     */
    save(): void {
        if (this.form.invalid) {
            return;
        }

        this.formStatus.set(FormStatus.SAVING);

        const formValue = this.form.getRawValue();
        const userToUpdate: DotAccountUser = {
            userId: formValue.userId,
            givenName: formValue.givenName,
            surname: formValue.surname,
            email: formValue.email,
            currentPassword: formValue.currentPassword
        };

        if (this.changePasswordOption() && formValue.newPassword) {
            userToUpdate.newPassword = formValue.newPassword;
        }

        this.dotAccountService
            .updateUser(userToUpdate)
            .pipe(take(1))
            .subscribe(
                (response) => {
                    this.dotAlertConfirmService.alert({
                        header: this.dotMessageService.get('my-account'),
                        message: this.dotMessageService.get('message.createaccount.success')
                    });

                    this.setShowStarter();

                    // Reset form state before closing
                    this.resetFormState();
                    this.handleClose();

                    if (response.entity.reauthenticate) {
                        this.dotRouterService.doLogOut();
                    } else {
                        this.loginService.setAuth({
                            loginAsUser: null,
                            user: response.entity.user
                        });
                    }
                },
                (response) => {
                    this.formStatus.set(FormStatus.ERROR);
                    const { errorCode, message } = response.error.errors[0];

                    switch (errorCode) {
                        case 'User-Info-Confirm-Current-Password-Failed':
                            this.confirmPasswordFailedMsg.set(message);
                            break;

                        case 'User-Info-Save-Password-Failed':
                            this.newPasswordFailedMsg.set(message);
                            break;

                        default:
                            this.httpErrorManagerService
                                .handle(response)
                                .pipe(
                                    take(1),
                                    map(() => null)
                                )
                                .subscribe();
                            break;
                    }
                }
            );
    }

    /**
     * Handles changes in the current user
     * @param auth Authentication object containing user information
     */
    private handleUserChange(auth: Auth): void {
        if (!auth || !auth.user) return;

        this.currentUser.set(auth.user);

        this.updateFormWithUserData(auth.user);
    }

    /**
     * Actualiza el validador del email con el regex obtenido
     */
    private updateEmailValidator(): void {
        if (!this.form) return;

        const emailControl = this.form.get('email');
        if (emailControl) {
            emailControl.setValidators([
                Validators.required,
                Validators.pattern(this.emailRegex || '')
            ]);
            emailControl.updateValueAndValidity();
        }
    }

    /**
     * Custom validator to verify that passwords match
     */
    private passwordMatchValidator(): ValidatorFn {
        return (control: AbstractControl): ValidationErrors | null => {
            // If password change is not enabled or any of the fields is empty, don't validate
            if (
                !this.changePasswordOption() ||
                !control.parent ||
                !control.parent.get('newPassword')?.value ||
                !control.value
            ) {
                return null;
            }

            const newPassword = control.parent.get('newPassword')?.value;
            const confirmPassword = control.value;

            return newPassword === confirmPassword ? null : { passwordMismatch: true };
        };
    }

    /**
     * Initializes the form with validators
     */
    private initForm(): void {
        this.form = this.fb.group({
            userId: [''],
            givenName: ['', Validators.required],
            surname: ['', Validators.required],
            email: ['', [Validators.required]],
            currentPassword: ['', Validators.required],
            newPassword: [{ value: '', disabled: true }],
            confirmPassword: [{ value: '', disabled: true }]
        });

        // Add custom validator for confirm password
        this.form.get('confirmPassword')?.setValidators([this.passwordMatchValidator().bind(this)]);

        // Update validation of confirmation field when password changes
        this.form.get('newPassword')?.valueChanges.subscribe(() => {
            this.form.get('confirmPassword')?.updateValueAndValidity();
        });
    }

    /**
     * Updates the form with user data
     * @param user User object with the information to display in the form
     */
    private updateFormWithUserData(user: User): void {
        if (!this.form || !user) return;

        this.form.patchValue({
            userId: user.userId,
            givenName: user.firstName,
            surname: user.lastName,
            email: user.emailAddress
        });

        // Important: Do not mark as touched to avoid immediate validations
        Object.keys(this.form.controls).forEach((key) => {
            this.form.get(key)?.markAsPristine();
            this.form.get(key)?.markAsUntouched();
        });
    }

    /**
     * Resets the form state
     */
    private resetFormState(): void {
        this.formStatus.set(FormStatus.INIT);
        this.changePasswordOption.set(false);
        this.confirmPasswordFailedMsg.set('');
        this.newPasswordFailedMsg.set('');

        // Reset only password fields
        this.form.get('currentPassword')?.setValue('');
        this.form.get('newPassword')?.setValue('');
        this.form.get('confirmPassword')?.setValue('');
        this.form.get('newPassword')?.disable();
        this.form.get('confirmPassword')?.disable();

        Object.keys(this.form.controls).forEach((key) => {
            this.form.get(key)?.markAsPristine();
            this.form.get(key)?.markAsUntouched();
        });
    }
}
