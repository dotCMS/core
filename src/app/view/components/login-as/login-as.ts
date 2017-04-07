import {BaseComponent} from '../_common/_base/base-component';
import {Component, Output, EventEmitter, Input, ViewEncapsulation, ViewChild} from '@angular/core';
import {LoginService, User} from '../../../api/services/login-service';
import {MessageService} from '../../../api/services/messages-service';
import {DotRouterService} from '../../../api/services/dot-router-service';
import {AutoComplete} from 'primeng/primeng';

@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [],
    selector: 'dot-login-as',
    styles: [require('./login-as.scss')],
    templateUrl: 'login-as.html'
})
export class LoginAsComponent extends BaseComponent {
    @Output() cancel = new EventEmitter<boolean>();
    @Input() visible: boolean;

    private needPassword = false;
    private userLists: Array<User>;
    private filteredLoginAsUsersResults: Array<any>;

    @ViewChild(AutoComplete) private autoCompleteComponent: AutoComplete;

    constructor(private loginService: LoginService, private router: DotRouterService, messageService: MessageService) {
        super(['Change', 'cancel', 'password', 'loginas.select.loginas.user', 'login-as'], messageService);
    }

    ngOnInit(): void {
        this.loginService.getLoginAsUsersList().subscribe(data => {
            this.userLists = data;
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
    dolLoginAs(options: any): void {
        let parameters = {password: options.password, userId: options.user.value};
        this.loginService.loginAs(parameters).subscribe(data => {
            if (data) {
                this.router.goToMain();
                this.close();
            }
            // TODO: Replace the alert below with a modal error message.
        }, response => {
            if (response.entity) {
                alert(response.errorsMessages);
            } else {
                alert(response);
            }
        });
    }

    userSelectedHandler($event): void {
        this.needPassword = this.loginService.getLoginAsUser($event.value).requestPassword || false;
    }

    /**
     * Filter the users displayed in the dropdown by comparing if
     * the user name characters set on the drowpdown search box matches
     * some on the user names set on the userlist variable loaded on the
     * ngOnInit method
     *
     * @param event - The event with the query parameter to filter the users
     */
    filterUsers(event): void {
        this.filteredLoginAsUsersResults = this.userLists.
        filter(user => user.fullName.toLowerCase().indexOf(event.query.toLowerCase()) >= 0)
            .map(user => {
                return {
                    label: user.fullName,
                    value: user.userId
                };
            });
    }

    /**
     * Display all the existing login as users availables loaded on the
     * userList variable initialized on the ngOnInit method
     *
     * @param event - The click event to display the dropdown options
     */
    handleLoginAsUsersDropdownClick(event: {originalEvent: Event, query: string}): void {
        // TODO: get rid of this lines when this is fixed: https://github.com/primefaces/primeng/issues/745
        event.originalEvent.preventDefault();
        event.originalEvent.stopPropagation();
        if (this.autoCompleteComponent.panelVisible) {
            this.autoCompleteComponent.onDropdownBlur();
            this.autoCompleteComponent.hide();
        } else {
            this.autoCompleteComponent.onDropdownFocus();
            this.autoCompleteComponent.show();
        }

        this.filteredLoginAsUsersResults = [];

        /**
         * This time out is included to imitate a remote call and
         * avoid that the suggestion box is not displayed, because
         * the autocomplete hide method is execute after the the show
         * method.
         *
         * TODO - remove the setTimeout when we add the pagination option
         * making a call to the login service to get a subset of login as users
         * paginated to display on the dropdown sugestions pannel.
         *
         */
        setTimeout(() => {

            this.filteredLoginAsUsersResults = this.userLists.map(user => {
                return {
                    label: user.fullName,
                    value: user.userId,
                };
            });
        }, 100);
    }

}