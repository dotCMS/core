import { Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    inject,
    input,
    OnDestroy,
    OnInit,
    output,
    signal,
    viewChild
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
import { PasswordModule } from 'primeng/password';
import { Select, SelectModule } from 'primeng/select';

import { take } from 'rxjs/operators';

import { DotMessageService, PaginatorService } from '@dotcms/data-access';
import { LoginService, User } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';

import { LOCATION_TOKEN } from '../../../../../providers';
import { DotNavigationService } from '../../../dot-navigation/services/dot-navigation.service';

@Component({
    selector: 'dot-login-as',
    styleUrls: ['./dot-login-as.component.scss'],
    templateUrl: 'dot-login-as.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        ReactiveFormsModule,
        DialogModule,
        ButtonModule,
        PasswordModule,
        SelectModule,
        DotMessagePipe
    ]
})
export class DotLoginAsComponent implements OnInit, OnDestroy {
    visible = input<boolean>(false);
    visibleChange = output<boolean>();
    cancel = output<boolean>();

    passwordElem = viewChild<ElementRef>('password');
    dropdown = viewChild<Select>('dropdown');
    formEl = viewChild<HTMLFormElement>('formEl');

    form: FormGroup;
    needPassword = signal<boolean>(false);
    userCurrentPage = signal<User[]>([]);
    errorMessage = signal<string>('');
    loading = signal<boolean>(false);

    #destroy$ = new Subject<boolean>();
    #location = inject(LOCATION_TOKEN);
    #dotMessageService = inject(DotMessageService);
    #dotNavigationService = inject(DotNavigationService);
    #fb = inject(FormBuilder);
    #loginService = inject(LoginService);
    #paginationService = inject(PaginatorService);

    ngOnInit(): void {
        this.#paginationService.url = 'v1/users/loginAsData';
        this.getUsersList();

        this.form = this.#fb.group({
            loginAsUser: new FormControl('', Validators.required),
            password: ''
        });
    }

    ngOnDestroy(): void {
        this.#destroy$.next(true);
        this.#destroy$.complete();
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
            this.loading.set(true);
            this.errorMessage.set('');
            const password: string = this.form.value.password;
            const user: User = this.form.value.loginAsUser;
            this.#loginService
                .loginAs({ user: user, password: password })
                .pipe(take(1))
                .subscribe({
                    next: (data) => {
                        if (data) {
                            this.#dotNavigationService.goToFirstPortlet().then(() => {
                                this.#location.reload();
                            });
                        }

                        this.loading.set(false);
                    },
                    error: (response) => {
                        this.loading.set(false);
                        if (response.errorsMessages) {
                            this.errorMessage.set(response.errorsMessages);
                        } else {
                            this.errorMessage.set(
                                this.#dotMessageService.get('loginas.error.wrong-credentials')
                            );
                            if (this.passwordElem()) {
                                this.passwordElem().nativeElement.focus();
                            }
                        }
                    }
                });
        }
    }

    /**
     * Set need password
     */
    userSelectedHandler(user: User): void {
        this.errorMessage.set('');
        this.needPassword.set(user?.requestPassword || false);
    }

    /**
     * Call to load a new page of user.
     */
    getUsersList(filter = '', offset = 0): void {
        this.#paginationService.filter = filter;
        this.#paginationService
            .getWithOffset<User[]>(offset)
            .pipe(take(1))
            .subscribe((items: User[]) => {
                this.userCurrentPage.set(items.slice());
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
        this.needPassword.set(false);
        this.errorMessage.set('');
    }
}
