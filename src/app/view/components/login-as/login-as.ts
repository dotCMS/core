import { BaseComponent } from '../_common/_base/base-component';
import {
    Component,
    Output,
    EventEmitter,
    Input,
    ViewEncapsulation
} from '@angular/core';
import { LoginService, User } from 'dotcms-js/dotcms-js';
import { MessageService } from '../../../api/services/messages-service';
import { DotRouterService } from '../../../api/services/dot-router-service';
import { PaginatorService } from '../../../api/services/paginator';
import { FormGroup, FormBuilder, FormControl } from '@angular/forms';

@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [],
    selector: 'dot-login-as',
    styleUrls: ['./login-as.scss'],
    templateUrl: 'login-as.html'
})
export class LoginAsComponent extends BaseComponent {
    @Output() cancel = new EventEmitter<boolean>();
    @Input() visible: boolean;

    userCurrentPage: User[];
    private needPassword = false;
    private form: FormGroup;

    constructor(
        private loginService: LoginService,
        private router: DotRouterService,
        messageService: MessageService,
        public paginationService: PaginatorService,
        private fb: FormBuilder
    ) {
        super(
            ['Change', 'cancel', 'password', 'loginas.select.loginas.user', 'login-as'],
            messageService
        );
    }

    ngOnInit(): void {
        this.paginationService.url = 'v2/users/loginAsData';
        this.getUsersList();

        this.form = this.fb.group({
            loginAsUser: new FormControl(),
            password: ''
        });
    }

    close(): boolean {
        this.cancel.emit(true);
        return false;
    }

    /**
     * Calls the back-end service that will change the appropriate request and session
     * attributes in order to impersonate the specified user. If an error occurs, a
     * message will be displayed to the user indicating so.
     *
     * @param options - The parameters required by the back-end service.
     */
    doLoginAs(): void {
        const password: string = this.form.value.password;
        const user: User = this.form.value.loginAsUser;

        this.loginService.loginAs({ user: user, password: password }).subscribe(
            data => {
                if (data) {
                    this.router.goToMain();
                    this.close();
                }
                // TODO: Replace the alert below with a modal error message.
            },
            response => {
                if (response.entity) {
                    alert(response.errorsMessages);
                } else {
                    alert(response);
                }
            }
        );
    }

    userSelectedHandler(user: User): void {
        this.needPassword = user.requestPassword || false;
    }

    /**
     * Call to load a new page of user.
     * @param {string} [filter='']
     * @param {number} [page=1]
     * @memberof SiteSelectorComponent
     */
    getUsersList(filter = '', offset = 0): void {
        this.paginationService.filter = filter;
        this.paginationService.getWithOffset(offset).subscribe(items => {
            // items.splice(0) is used to return a new object and trigger the change detection in angular
            this.userCurrentPage = items.splice(0);
        });
    }

    /**
     * Call when the user global serach changed
     * @param {any} filter
     * @memberof SiteSelectorComponent
     */
    handleFilterChange(filter): void {
        this.getUsersList(filter);
    }

    /**
     * Call when the current page changed
     * @param {any} event
     * @memberof SiteSelectorComponent
     */
    handlePageChange(event): void {
        this.getUsersList(event.filter, event.first);
    }
}
