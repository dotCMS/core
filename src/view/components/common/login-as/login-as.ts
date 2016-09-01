import {Component, Output, EventEmitter, Inject} from '@angular/core';
import {DotcmsConfig} from '../../../../api/services/system/dotcms-config';
import {DotSelect, DotOption} from '../dot-select/dot-select';
import {LoginService, User} from '../../../../api/services/login-service';
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {MdButton} from '@angular2-material/button';
import {Router} from '@ngrx/router';

@Component({
    directives: [DotSelect, DotOption, MD_INPUT_DIRECTIVES, MdButton],
    moduleId: __moduleName,
    providers: [],
    selector: 'dot-login-as',
    styleUrls: ['login-as.css'],
    templateUrl: ['login-as.html']
})
export class LoginAsComponent {
    @Output() cancel = new EventEmitter<>();

    private userLists: Array<User>;
    private needPassword: boolean = false;

    constructor(private loginService: LoginService, private router: Router,
                @Inject('dotcmsConfig') private dotcmsConfig: DotcmsConfig) {
    }

    ngOnInit(): void {
        this.loginService.loadLoginAsUsers();
        this.loginService.loginAsUsers$.subscribe(data => {
            this.userLists = data;
        });
    }

    close(): void {
        this.cancel.emit(true);
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