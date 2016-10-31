import {BaseComponent} from '../_base/base-component';
import {Component, Output, EventEmitter} from '@angular/core';
import {LoginService, User} from '../../../../api/services/login-service';
import {MessageService} from '../../../../api/services/messages-service';
import {DotRouterService} from '../../../../api/services/dot-router-service';

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

    constructor(private loginService: LoginService, private router: DotRouterService, private messageService: MessageService) {
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

    dolLoginAs(options: any): void {
        this.loginService.loginAs(options).subscribe(data => {
            if (data) {
                this.router.goToMain();
                this.close();
            }
        });
    }

    userSelectedHandler($event): void {
        this.needPassword = this.loginService.getLoginAsUser($event.value).requestPassword || false;
    }

}