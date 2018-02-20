import { BaseComponent } from '../_common/_base/base-component';
import { Component, ViewChild, OnInit } from '@angular/core';
import { DotDropdownComponent } from '../_common/dropdown-component/dot-dropdown.component';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { LoginService, Auth, LoggerService } from 'dotcms-js/dotcms-js';
import { DotMessageService } from '../../../api/services/dot-messages-service';

@Component({
    selector: 'dot-toolbar-user',
    styleUrls: ['./toolbar-user.scss'],
    templateUrl: 'toolbar-user.html'
})
export class ToolbarUserComponent extends BaseComponent implements OnInit {
    @ViewChild(DotDropdownComponent) dropdown: DotDropdownComponent;
    auth: Auth;

    showLoginAs = false;
    showMyAccount = false;

    constructor(
        dotMessageService: DotMessageService,
        private loggerService: LoggerService,
        private loginService: LoginService,
        public iframeOverlayService: IframeOverlayService
    ) {
        super(['my-account'], dotMessageService);
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
        this.loginService.logOutUser().subscribe(
            () => {},
            (error) => {
                this.loggerService.error(error);
            }
        );
        return false;
    }

    logoutAs($event): void {
        $event.preventDefault();
        this.loginService.logoutAs().subscribe(
            () => {
                this.dropdown.closeIt();
                this.iframeOverlayService.hide();
            },
            (error) => {
                this.loggerService.error(error);
            }
        );
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
