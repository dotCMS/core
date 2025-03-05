import { BehaviorSubject, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';
import { FormGroupDirective, FormsModule, NgForm } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { map, take } from 'rxjs/operators';

import { DotAccountService, DotAccountUser } from '@dotcms/app/api/services/dot-account-service';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import {
    DotAlertConfirmService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRouterService
} from '@dotcms/data-access';
import { Auth, DotcmsConfigService, LoginService, User } from '@dotcms/dotcms-js';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-my-account',
    styleUrls: ['./dot-my-account.component.scss'],
    templateUrl: 'dot-my-account.component.html',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        ButtonModule,
        PasswordModule,
        InputTextModule,
        DialogModule,
        CheckboxModule,
        ProgressSpinnerModule,
        DotFieldRequiredDirective,
        DotMessagePipe
    ],
    providers: [DotAlertConfirmService, FormGroupDirective],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotMyAccountComponent implements OnInit, OnDestroy {
    @ViewChild('myAccountForm', { static: true }) form: NgForm;

    @Output() shutdown = new EventEmitter<void>();

    @Input()
    set visible(value: boolean) {
        this._visible = value;
        // Reset loading state when dialog is opened or closed
        if (value === false) {
            this.isSaving$.next(false);
        } else if (value === true) {
            // Reset form state when dialog is opened
            this.resetFormState();
        }
    }
    get visible(): boolean {
        return this._visible;
    }
    private _visible: boolean;

    emailRegex: string;
    passwordMatch: boolean;

    dotAccountUser: DotAccountUser = {
        currentPassword: '',
        email: '',
        givenName: '',
        surname: '',
        userId: ''
    };

    passwordConfirm: string;

    changePasswordOption = false;
    showStarter: boolean;

    newPasswordFailedMsg = '';
    confirmPasswordFailedMsg = '';

    isSaving$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotMessageService: DotMessageService,
        private dotAccountService: DotAccountService,
        private dotcmsConfigService: DotcmsConfigService,
        private loginService: LoginService,
        private dotRouterService: DotRouterService,
        private dotMenuService: DotMenuService,
        private httpErrorManagerService: DotHttpErrorManagerService,
        private dotAlertConfirmService: DotAlertConfirmService
    ) {
        this.passwordMatch = false;
        this.changePasswordOption = false;
        this.loginService.watchUser(this.loadUser.bind(this));
        this.dotcmsConfigService.getConfig().subscribe((res) => {
            this.emailRegex = res.emailRegex;
        });
    }

    ngOnInit() {
        this.dotMenuService
            .isPortletInMenu('starter')
            .pipe(take(1))
            .subscribe((showStarter: boolean) => {
                this.showStarter = showStarter;
            });

        // Initialize form state
        this.resetFormState();
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    checkPasswords(): void {
        this.newPasswordFailedMsg = '';
        this.confirmPasswordFailedMsg = '';

        this.passwordMatch =
            this.dotAccountUser.newPassword !== '' &&
            this.passwordConfirm !== '' &&
            this.dotAccountUser.newPassword === this.passwordConfirm;
    }

    toggleChangePasswordOption(): void {
        this.changePasswordOption = !this.changePasswordOption;
    }

    /**
     * Calls Api based on checked input to add/remove starter portlet from menu
     *
     * @memberof DotMyAccountComponent
     */
    setShowStarter(): void {
        if (this.showStarter) {
            this.dotAccountService.addStarterPage().subscribe();
        } else {
            this.dotAccountService.removeStarterPage().subscribe();
        }
    }

    getRequiredMessage(...args: string[]): string {
        return this.dotMessageService.get('error.form.mandatory', ...args);
    }

    /**
     * Handles dialog close action
     * Resets loading state and emits shutdown event
     */
    handleClose(): void {
        this.isSaving$.next(false);
        this.shutdown.emit();
    }

    save(): void {
        this.isSaving$.next(true);
        this.dotAccountService
            .updateUser(this.dotAccountUser)
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
                    this.isSaving$.next(false);
                    const { errorCode, message } = response.error.errors[0];

                    switch (errorCode) {
                        case 'User-Info-Confirm-Current-Password-Failed':
                            this.confirmPasswordFailedMsg = message;
                            break;

                        case 'User-Info-Save-Password-Failed':
                            this.newPasswordFailedMsg = message;
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

    private loadUser(auth: Auth): void {
        const user: User = auth.user;
        // Update user data without affecting form state
        this.dotAccountUser.email = user.emailAddress;
        this.dotAccountUser.givenName = user.firstName;
        this.dotAccountUser.surname = user.lastName;
        this.dotAccountUser.userId = user.userId;

        // Clear password fields but don't affect form state
        this.dotAccountUser.newPassword = '';
        this.dotAccountUser.currentPassword = '';
        this.passwordConfirm = '';
    }

    /**
     * Resets form state without clearing values
     */
    private resetFormState(): void {
        this.isSaving$.next(false);
        this.confirmPasswordFailedMsg = '';
        this.newPasswordFailedMsg = '';
        this.changePasswordOption = false;

        // Clear password fields
        this.passwordConfirm = '';
        this.dotAccountUser.newPassword = '';
        this.dotAccountUser.currentPassword = '';

        // Reload user data
        this.loginService.auth$.pipe(take(1)).subscribe(this.loadUser.bind(this));

        // Mark form as pristine after view is initialized
        setTimeout(() => {
            if (this.form) {
                // Don't reset the form values, just mark it as pristine and untouched
                Object.keys(this.form.controls).forEach((key) => {
                    const control = this.form.controls[key];
                    control.markAsPristine();
                    control.markAsUntouched();
                });

                // Mark the form itself as pristine
                this.form.form.markAsPristine();
                this.form.form.markAsUntouched();
            }
        });
    }
}
