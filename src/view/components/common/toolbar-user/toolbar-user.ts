import {Component, ViewChild} from '@angular/core';
import {Router} from '@angular/router';
import {DropdownComponent} from '../dropdown-component/dropdown-component';
import {LoginService, Auth} from '../../../../api/services/login-service';
import {MyAccountComponent} from '../../my-account-component/dot-my-account-component';

@Component({
    directives: [],
    moduleId: __moduleName,
    selector: 'toolbar-user',
    styleUrls: ['toolbar-user.css'],
    templateUrl: ['toolbar-user.html'],

})
export class ToolbarUserComponent {
    @ViewChild(DropdownComponent) dropdown: DropdownComponent;
    private showLoginAs: boolean = false;
    private auth: Auth;
    private showMyAccount: boolean = false;

    constructor(private router: Router, private loginService: LoginService) {}

    ngOnInit(): void {
        this.loginService.watchUser((auth: Auth) => {
            this.auth = auth;
        });
    }

    /**
     * Call the logout service
     */
    logout(): boolean {
        this.loginService.logOutUser().subscribe(data => {
            this.router.navigate(['public/login']);
        }, (error) => {
            console.log(error);
        });
        return false;
    }

    logoutAs($event): void {
        $event.preventDefault();
        this.loginService.logoutAs().subscribe(data => {
            this.router.navigate(['dotCMS']);
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

    toggleMyAccount(): boolean {
        this.showMyAccount = !this.showMyAccount;
        return false;
    }
}