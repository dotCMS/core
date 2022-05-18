import {
    Component,
    EventEmitter,
    Output,
    Input,
    OnInit,
    ViewChild,
    OnDestroy
} from '@angular/core';
import { NgForm } from '@angular/forms';
import { take, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotcmsConfigService, LoginService, User, Auth } from '@dotcms/dotcms-js';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotMenuService } from '@services/dot-menu.service';
import { DotAccountUser, DotAccountService } from '@services/dot-account-service';

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
    message = null;
    changePasswordOption = false;
    dialogActions: DotDialogActions;
    showStarter: boolean;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotMessageService: DotMessageService,
        private dotAccountService: DotAccountService,
        private dotcmsConfigService: DotcmsConfigService,
        private loginService: LoginService,
        private dotRouterService: DotRouterService,
        private dotMenuService: DotMenuService
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
        if (this.message) {
            this.message = null;
        }
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
        this.dotAccountService.updateUser(this.dotAccountUser).subscribe(
            (response) => {
                // TODO: replace the alert with a Angular components
                alert(this.dotMessageService.get('message.createaccount.success'));
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
                // TODO: We have to define how must be the user feedback in case of error
                this.message = response.errorsMessages;
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
