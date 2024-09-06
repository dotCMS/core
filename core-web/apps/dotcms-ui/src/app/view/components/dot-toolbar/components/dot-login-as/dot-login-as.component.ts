import { Subject } from 'rxjs';

import {
    Component,
    ElementRef,
    EventEmitter,
    Inject,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';
import {
    UntypedFormBuilder,
    UntypedFormControl,
    UntypedFormGroup,
    Validators
} from '@angular/forms';

import { take, takeUntil } from 'rxjs/operators';

import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { LOCATION_TOKEN } from '@dotcms/app/providers';
import { DotMessageService, PaginatorService } from '@dotcms/data-access';
import { LoginService, User } from '@dotcms/dotcms-js';
import { DotDialogActions } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-login-as',
    styleUrls: ['./dot-login-as.component.scss'],
    templateUrl: 'dot-login-as.component.html'
})
export class DotLoginAsComponent implements OnInit, OnDestroy {
    @Output()
    cancel = new EventEmitter<boolean>();

    @Input()
    visible: boolean;

    @ViewChild('password')
    passwordElem: ElementRef;

    @ViewChild('formEl', { static: true })
    formEl: HTMLFormElement;

    form: UntypedFormGroup;
    needPassword = false;
    userCurrentPage: User[];
    errorMessage: string;
    dialogActions: DotDialogActions;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        @Inject(LOCATION_TOKEN) private location: Location,
        private dotMessageService: DotMessageService,
        private dotNavigationService: DotNavigationService,
        private fb: UntypedFormBuilder,
        private loginService: LoginService,
        public paginationService: PaginatorService
    ) {}

    ngOnInit(): void {
        this.paginationService.url = 'v1/users/loginAsData';
        this.getUsersList();

        this.form = this.fb.group({
            loginAsUser: new UntypedFormControl('', Validators.required),
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

        this.dialogActions = {
            accept: {
                label: this.dotMessageService.get('Change'),
                action: () => {
                    this.formEl.ngSubmit.emit();
                },
                disabled: true
            },
            cancel: {
                label: this.dotMessageService.get('cancel')
            }
        };
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
                        this.dotNavigationService.goToFirstPortlet().then(() => {
                            this.location.reload();
                        });
                    }
                },
                (response) => {
                    if (response.errorsMessages) {
                        this.errorMessage = response.errorsMessages;
                    } else {
                        this.errorMessage = this.dotMessageService.get(
                            'loginas.error.wrong-credentials'
                        );
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
        this.errorMessage = '';
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
            .getWithOffset<User[]>(offset)
            .pipe(take(1))
            .subscribe((items: User[]) => {
                // items.splice(0) to return a new object and trigger the change detection
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
