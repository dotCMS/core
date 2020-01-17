import { Component, ViewChild, OnInit, Inject } from '@angular/core';
import { DotDropdownComponent } from '@components/_common/dot-dropdown-component/dot-dropdown.component';
import { IframeOverlayService } from '../../../_common/iframe/service/iframe-overlay.service';
import { LoginService, Auth, LoggerService } from 'dotcms-js';
import { DotMessageService } from '@services/dot-messages-service';
import { LOCATION_TOKEN } from 'src/app/providers';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { retryWhen, take, tap } from 'rxjs/operators';

@Component({
    selector: 'dot-toolbar-user',
    styleUrls: ['./dot-toolbar-user.component.scss'],
    templateUrl: 'dot-toolbar-user.component.html'
})
export class DotToolbarUserComponent implements OnInit {
    @ViewChild(DotDropdownComponent)
    dropdown: DotDropdownComponent;
    auth: Auth;

    showLoginAs = false;
    showMyAccount = false;

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(
        @Inject(LOCATION_TOKEN) private location: Location,
        private dotMessageService: DotMessageService,
        private loggerService: LoggerService,
        private loginService: LoginService,
        public iframeOverlayService: IframeOverlayService,
        private dotNavigationService: DotNavigationService
    ) {}

    ngOnInit(): void {
        this.dotMessageService
            .getMessages(['my-account', 'login-as', 'Logout', 'logout-as'])
            .pipe(
                tap((res: {
                    [key: string]: string;
                }) => {
                    if (!Object.keys(res).length) {
                        throw new Error('No message keys');
                    }
                }),
                retryWhen((errors) => errors),
                take(1)
            )
            .subscribe((res) => {
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
                this.dotNavigationService.goToFirstPortlet().then(() => {
                    this.location.reload();
                });
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
