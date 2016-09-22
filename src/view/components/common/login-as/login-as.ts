import {BaseComponent} from '../_base/base-component';
import {Component, Output, EventEmitter} from '@angular/core';
import {DotSelect, DotOption} from '../dot-select/dot-select';
import {LoginService, User} from '../../../../api/services/login-service';
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {MdButton} from '@angular2-material/button';
import {MessageService} from '../../../../api/services/messages-service';
import {Router} from '@ngrx/router';

@Component({
    directives: [DotSelect, DotOption, MD_INPUT_DIRECTIVES, MdButton],
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

    dolLoginAs(options: any): void {
        this.loginService.loginAs(options).subscribe(data => {
            if (data) {
                this.router.go('/dotCMS');
                this.close();
            }
        });
    }

    userSelectedHandler($event): void {
        this.needPassword = this.loginService.getLoginAsUser($event.value).requestPassword || false;
    }

}