import { Component, ViewChild, OnInit } from '@angular/core';
import { DotDropdownComponent } from '../_common/dropdown-component/dot-dropdown.component';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { LoginService, Auth, LoggerService } from 'dotcms-js/dotcms-js';
import { DotMessageService } from '@services/dot-messages-service';
import { DotNavigationService } from '../dot-navigation/services/dot-navigation.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';

@Component({
    selector: 'dot-toolbar-user',
    styleUrls: ['./toolbar-user.scss'],
    templateUrl: 'toolbar-user.html'
})
export class ToolbarUserComponent implements OnInit {
    @ViewChild(DotDropdownComponent)
    dropdown: DotDropdownComponent;
    auth: Auth;

    showLoginAs = false;
    showMyAccount = false;

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(
        private dotMessageService: DotMessageService,
        private dotEventsService: DotEventsService,
        private loggerService: LoggerService,
        private loginService: LoginService,
        public iframeOverlayService: IframeOverlayService,
        private dotNavigationService: DotNavigationService
    ) {}

    ngOnInit(): void {
        this.dotMessageService.getMessages(['my-account']).subscribe((res) => {
            this.i18nMessages = res;
        });

        this.loginService.watchUser((auth: Auth) => {
            this.auth = auth;
        });
    }

    /**
     * Call the logout service and clear the user session
     *
     * @returns boolean
     * @memberof ToolbarUserComponent
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

    /**
     * Call the logout as service and clear the user login as
     *
     * @param any $event
     * @memberof ToolbarUserComponent
     */
    logoutAs($event): void {
        $event.preventDefault();

        this.loginService.logoutAs().subscribe(
            () => {
                this.dropdown.closeIt();
                this.iframeOverlayService.hide();
                this.dotNavigationService.goToFirstPortlet();
                this.dotEventsService.notify('logout-as');
            },
            (error) => {
                this.loggerService.error(error);
            }
        );
    }

    /**
     * Toggle show/hide login as dialog
     *
     * @returns boolean
     * @memberof ToolbarUserComponent
     */
    tooggleLoginAs(): boolean {
        this.dropdown.closeIt();
        this.showLoginAs = !this.showLoginAs;
        return false;
    }

    /**
     * Toggle show/hide my acccont menu
     *
     * @returns boolean
     * @memberof ToolbarUserComponent
     */
    toggleMyAccount(): boolean {
        this.showMyAccount = !this.showMyAccount;
        return false;
    }
}
