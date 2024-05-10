import { BehaviorSubject, Subject } from 'rxjs';

import {
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';
import { NgForm } from '@angular/forms';

import { map, take, takeUntil } from 'rxjs/operators';

import { DotAccountService, DotAccountUser } from '@dotcms/app/api/services/dot-account-service';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import {
    DotAlertConfirmService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRouterService
} from '@dotcms/data-access';
import { Auth, DotcmsConfigService, LoginService, User } from '@dotcms/dotcms-js';
import { DotDialogActions } from '@dotcms/dotcms-models';

interface AccountUserForm extends DotAccountUser {
    confirmPassword?: string;
}

@Component({
    selector: 'dot-my-account',
    styleUrls: ['./dot-my-account.component.scss'],
    templateUrl: 'dot-my-account.component.html'
})
export class DotMyAccountComponent implements OnInit, OnDestroy {
    @ViewChild('myAccountForm', { static: true }) form: NgForm;

    @Output() shutdown = new EventEmitter<void>();

    @Input() visible: boolean;

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
    dialogActions: DotDialogActions;
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
        this.dialogActions = {
            accept: {
                label: this.dotMessageService.get('save'),
                action: () => {
                    this.save();
                },
                disabled: true
            },
            cancel: {
                label: this.dotMessageService.get('modes.Close')
            }
        };

        this.dotMenuService
            .isPortletInMenu('starter')
            .pipe(take(1))
            .subscribe((showStarter: boolean) => {
                this.showStarter = showStarter;
            });

        this.form.valueChanges
            .pipe(takeUntil(this.destroy$))
            .subscribe((valueChange: AccountUserForm) => {
                this.dialogActions = {
                    ...this.dialogActions,
                    accept: {
                        ...this.dialogActions.accept,
                        disabled:
                            (this.changePasswordOption &&
                                valueChange.newPassword !== valueChange.confirmPassword) ||
                            !this.form.valid
                    }
                };
            });
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
                    this.shutdown.emit();

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
        this.dotAccountUser.email = user.emailAddress;
        this.dotAccountUser.givenName = user.firstName;
        this.dotAccountUser.surname = user.lastName;
        this.dotAccountUser.userId = user.userId;
        this.dotAccountUser.newPassword = null;
        this.passwordConfirm = null;
    }
}
