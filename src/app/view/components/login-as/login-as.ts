import {
    Component,
    Output,
    EventEmitter,
    Input,
    OnInit,
    ElementRef,
    ViewChild,
    OnDestroy
} from '@angular/core';

import { LoginService, User } from 'dotcms-js';

import { FormGroup, FormBuilder, FormControl, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { take, takeUntil } from 'rxjs/operators';

import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotMessageService } from '@services/dot-messages-service';
import { DotNavigationService } from '../dot-navigation/services/dot-navigation.service';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { PaginatorService } from '@services/paginator';
import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';

@Component({
    providers: [],
    selector: 'dot-login-as',
    styleUrls: ['./login-as.scss'],
    templateUrl: 'login-as.html'
})
export class LoginAsComponent implements OnInit, OnDestroy {
    @Output()
    cancel = new EventEmitter<boolean>();

    @Input()
    visible: boolean;

    @ViewChild('password')
    passwordElem: ElementRef;

    @ViewChild('formEl')
    formEl: HTMLFormElement;

    form: FormGroup;
    needPassword = false;
    userCurrentPage: User[];
    errorMessage: string;
    dialogActions: DotDialogActions;

    i18nMessages: {
        [key: string]: string;
    } = {};

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotMessageService: DotMessageService,
        private dotEventsService: DotEventsService,
        private fb: FormBuilder,
        private loginService: LoginService,
        public paginationService: PaginatorService,
        private iframeOverlayService: IframeOverlayService,
        private dotNavigationService: DotNavigationService
    ) {}

    ngOnInit(): void {
        this.paginationService.url = 'v2/users/loginAsData';
        this.getUsersList();

        this.form = this.fb.group({
            loginAsUser: new FormControl('', Validators.required),
            password: ''
        });

        this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.dialogActions = {
                ...this.dialogActions,
                accept: {
                    ...this.dialogActions.accept,
                    disabled: !this.form.valid
                }
            };
        });

        this.dotMessageService
            .getMessages([
                'Change',
                'cancel',
                'loginas.select.loginas.user',
                'loginas.input.loginas.password',
                'loginas.error.wrong-credentials',
                'login-as'
            ])
            .pipe(take(1))
            .subscribe((res) => {
                this.i18nMessages = res;

                this.dialogActions = {
                    accept: {
                        label: this.i18nMessages['Change'],
                        action: () => {
                            this.formEl.ngSubmit.emit();
                        },
                        disabled: true
                    },
                    cancel: {
                        label: this.i18nMessages['cancel']
                    },
                };
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Emit cancel
     *
     * @memberof LoginAsComponent
     */
    close(): void {
        this.cancel.emit(true);
    }

    /**
     * Do request to login as specfied user
     *
     * @memberof LoginAsComponent
     */
    doLoginAs(): void {
        this.errorMessage = '';
        const password: string = this.form.value.password;
        const user: User = this.form.value.loginAsUser;
        this.loginService
            .loginAs({ user: user, password: password })
            .pipe(take(1))
            .subscribe(
                (data) => {
                    if (data) {
                        this.close();
                        this.iframeOverlayService.hide();
                        this.dotNavigationService.goToFirstPortlet();
                        this.dotEventsService.notify('login-as');
                    }
                },
                (response) => {
                    if (response.entity) {
                        this.errorMessage = response.errorsMessages;
                    } else {
                        this.errorMessage = this.i18nMessages['loginas.error.wrong-credentials'];
                        this.passwordElem.nativeElement.focus();
                    }
                }
            );
    }

    /**
     * Set need password
     *
     * @param {User} user
     * @memberof LoginAsComponent
     */
    userSelectedHandler(user: User): void {
        this.needPassword = user.requestPassword || false;
    }

    /**
     * Call to load a new page of user.
     *
     * @param string [filter='']
     * @param number [page=1]
     * @memberof LoginAsComponent
     */
    getUsersList(filter = '', offset = 0): void {
        this.paginationService.filter = filter;
        this.paginationService
            .getWithOffset(offset)
            .pipe(take(1))
            .subscribe((items) => {
                // items.splice(0) is used to return a new object and trigger the change detection in angular
                this.userCurrentPage = items.splice(0);
            });
    }

    /**
     * Call when the user global serach changed
     * @param any filter
     * @memberof SiteSelectorComponent
     */
    handleFilterChange(filter): void {
        this.getUsersList(filter);
    }

    /**
     * Call when the current page changed
     * @param any event
     * @memberof SiteSelectorComponent
     */
    handlePageChange(event): void {
        this.getUsersList(event.filter, event.first);
    }
}
