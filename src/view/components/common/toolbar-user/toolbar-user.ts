import {Component, ViewChild, Inject} from '@angular/core';
import {Router} from '@ngrx/router';
import {DropdownComponent} from '../dropdown-component/dropdown-component';
import {LoginAsComponent} from '../login-as/login-as';
import {LoginService, User} from '../../../../api/services/login-service';
import {MyAccountComponent} from '../../my-account-component/dot-my-account-component';
import {DotcmsConfig} from '../../../../api/services/system/dotcms-config';



@Component({
    directives: [DropdownComponent, LoginAsComponent, MyAccountComponent],
    moduleId: __moduleName,
    selector: 'toolbar-user',
    styleUrls: ['toolbar-user.css'],
    templateUrl: ['toolbar-user.html'],

})
export class ToolbarUserComponent {
    @ViewChild(DropdownComponent) dropdown: DropdownComponent;
    private isLoggedAs: boolean = false;
    private showLoginAs: boolean = false;
    private user: User;
    private showMyAccount: boolean = false;

    constructor(private router: Router, private loginService: LoginService,
                @Inject('dotcmsConfig') private dotcmsConfig: DotcmsConfig) {}

    ngOnInit(): void {
        this.user = this.loginService.loginUser;
        this.loginService.loginUser$.subscribe((user) => {
            this.user = user;
        });
        this.isLoggedAs = this.dotcmsConfig.configParams.loginAsUser ? true : false;
        this.loginService.isLoginAs$.subscribe(data => {
            this.isLoggedAs = data;
        });
    }

    /**
     * Call the logout service
     */
    logout(): void {
        this.loginService.logOutUser().subscribe(data => {
            this.router.go('/public/login');
            return;
        }, (error) => {
            console.log(error);
        });

    }

    logoutAs($event): void {
        $event.preventDefault();
        this.loginService.logoutAs().subscribe(data => {
            this.router.go('/dotCMS');
            this.dropdown.closeIt();
        }, (error) => {
            console.log(error);
        });

    }

    tooggleLoginAs(): boolean {
        this.dropdown.closeIt();
        this.showLoginAs = !this.showLoginAs;
        return false;
    }

    toggleMyAccount(): void {
        this.showMyAccount = !this.showMyAccount;
        return false;
    }
}