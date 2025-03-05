import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    inject,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';
import {
    FormBuilder,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { Dropdown, DropdownModule } from 'primeng/dropdown';
import { PasswordModule } from 'primeng/password';

import { take } from 'rxjs/operators';

import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { LOCATION_TOKEN } from '@dotcms/app/providers';
import { DotMessageService, PaginatorService } from '@dotcms/data-access';
import { LoginService, User } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-login-as',
    styleUrls: ['./dot-login-as.component.scss'],
    templateUrl: 'dot-login-as.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        DialogModule,
        ButtonModule,
        PasswordModule,
        SearchableDropDownModule,
        DropdownModule,
        DotMessagePipe
    ]
})
export class DotLoginAsComponent implements OnInit, OnDestroy {
    @Input() visible = false;
    @Output() visibleChange = new EventEmitter<boolean>();
    @Output() cancel = new EventEmitter<boolean>();

    @ViewChild('password')
    passwordElem: ElementRef;

    @ViewChild('dropdown')
    dropdown: Dropdown;

    @ViewChild('formEl', { static: true })
    formEl: HTMLFormElement;

    form: FormGroup;
    needPassword = false;
    userCurrentPage: User[];
    errorMessage: string;

    private readonly destroy$ = new Subject<boolean>();
    private readonly location = inject(LOCATION_TOKEN);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly dotNavigationService = inject(DotNavigationService);
    private readonly fb = inject(FormBuilder);
    private readonly loginService = inject(LoginService);
    readonly paginationService = inject(PaginatorService);

    ngOnInit(): void {
        this.paginationService.url = 'v1/users/loginAsData';
        this.getUsersList();

        this.form = this.fb.group({
            loginAsUser: new FormControl('', Validators.required),
            password: ''
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Emit cancel
     */
    close(): void {
        this.cancel.emit(true);
    }

    /**
     * Do request to login as specified user
     */
    doLoginAs(): void {
        if (this.form.valid) {
            this.errorMessage = '';
            const password: string = this.form.value.password;
            const user: User = this.form.value.loginAsUser;
            this.loginService
                .loginAs({ user: user, password: password })
                .pipe(take(1))
                .subscribe({
                    next: (data) => {
                        if (data) {
                            this.dotNavigationService.goToFirstPortlet().then(() => {
                                this.location.reload();
                            });
                        }
                    },
                    error: (response) => {
                        if (response.errorsMessages) {
                            this.errorMessage = response.errorsMessages;
                        } else {
                            this.errorMessage = this.dotMessageService.get(
                                'loginas.error.wrong-credentials'
                            );
                            this.passwordElem.nativeElement.focus();
                        }
                    }
                });
        }
    }

    /**
     * Set need password
     */
    userSelectedHandler(user: User): void {
        this.errorMessage = '';
        this.needPassword = user?.requestPassword || false;
    }

    /**
     * Call to load a new page of user.
     */
    getUsersList(filter = '', offset = 0): void {
        this.paginationService.filter = filter;
        this.paginationService
            .getWithOffset<User[]>(offset)
            .pipe(take(1))
            .subscribe((items: User[]) => {
                this.userCurrentPage = items.slice();
            });
    }

    /**
     * Call when the user global search changed
     */
    handleFilterChange(event: { filter: string }): void {
        this.getUsersList(event.filter);
    }

    /**
     * Call when the current page changed
     */
    handlePageChange(event: { filter: string; first: number }): void {
        this.getUsersList(event.filter, event.first);
    }

    /**
     * Clear user selection
     */
    clearSelection(): void {
        this.needPassword = false;
        this.errorMessage = '';
    }
}
