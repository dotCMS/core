import {BaseComponent} from '../../common/_base/base-component';
import {Component, ViewChild} from '@angular/core';
import {DotRouterService} from '../../../../api/services/dot-router-service';
import {DropdownComponent} from '../dropdown-component/dropdown-component';
import {LoginService, Auth} from '../../../../api/services/login-service';
import {MessageService} from '../../../../api/services/messages-service';
import {LoggerService} from '../../../../api/services/logger.service';
import {IframeOverlayService} from "../../../../api/services/iframe-overlay-service";

@Component({
    selector: 'toolbar-user',
    styleUrls: ['toolbar-user.css'],
    templateUrl: 'toolbar-user.html',

})
export class ToolbarUserComponent extends BaseComponent {
    @ViewChild(DropdownComponent) dropdown: DropdownComponent;


    private showLoginAs: boolean = false;
    private auth: Auth;
    private showMyAccount: boolean = false;

    constructor(private router: DotRouterService, private loginService: LoginService,
                private messageService: MessageService, private loggerService: LoggerService,
                private iframeOverlayService: IframeOverlayService) {
        super(['my-account'], messageService);
    }

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
        }, (error) => {
            this.loggerService.error(error);
        });
        return false;
    }

    logoutAs($event): void {
        $event.preventDefault();
        this.loginService.logoutAs().subscribe(data => {
            this.router.goToMain();
            this.dropdown.closeIt();
        }, (error) => {
            this.loggerService.error(error);
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