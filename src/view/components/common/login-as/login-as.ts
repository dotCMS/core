import {BaseComponent} from '../_base/base-component';
import {Component, Output, EventEmitter} from '@angular/core';
import {LoginService, User} from '../../../../api/services/login-service';
import {MessageService} from '../../../../api/services/messages-service';
import {Router} from '@angular/router';

@Component({
    directives: [],
    moduleId: __moduleName,
    providers: [],
    selector: 'dot-login-as',
    styleUrls: ['login-as.css'],
    templateUrl: ['login-as.html']
})
export class LoginAsComponent extends BaseComponent {
    @Output() cancel = new EventEmitter<>();

    private needPassword: boolean = false;
    private userLists: Array<User>;

    constructor(private loginService: LoginService, private router: Router, private messageService: MessageService) {
        super(['change', 'cancel', 'password'], messageService);
    }

    ngOnInit(): void {
        this.loginService.getLoginAsUsersList().subscribe(data => {
            this.userLists = data;
        });
    }

    close(): void {
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
        this.loginService.loginAs(options).subscribe(data => {
            if (data) {
                this.router.navigate(['dotCMS']);
                this.close();
            }
            // TODO: Replace the alert below with a modal error message.
        }, message => alert(message));
    }

    userSelectedHandler($event): void {
        this.needPassword = this.loginService.getLoginAsUser($event.value).requestPassword || false;
    }

}