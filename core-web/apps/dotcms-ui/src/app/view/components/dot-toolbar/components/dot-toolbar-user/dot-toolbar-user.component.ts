import { Observable } from 'rxjs';

import { Component, Inject, OnInit, ViewChild } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotDropdownComponent } from '@components/_common/dot-dropdown-component/dot-dropdown.component';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { LOCATION_TOKEN } from '@dotcms/app/providers';
import { Auth, CurrentUser, LoggerService, LoginService, LOGOUT_URL } from '@dotcms/dotcms-js';

import { IframeOverlayService } from '../../../_common/iframe/service/iframe-overlay.service';

@Component({
    selector: 'dot-toolbar-user',
    styleUrls: ['./dot-toolbar-user.component.scss'],
    templateUrl: 'dot-toolbar-user.component.html'
})
export class DotToolbarUserComponent implements OnInit {
    @ViewChild(DotDropdownComponent) dropdown: DotDropdownComponent;
    auth: Auth;

    showLoginAs = false;
    showMyAccount = false;

    logoutUrl = `${LOGOUT_URL}?r=${new Date().getTime()}`;

    haveLoginAsPermission$: Observable<boolean>;

    constructor(
        @Inject(LOCATION_TOKEN) private location: Location,
        private loggerService: LoggerService,
        private loginService: LoginService,
        public iframeOverlayService: IframeOverlayService,
        private dotNavigationService: DotNavigationService,
        private dotRouterService: DotRouterService
    ) {}

    ngOnInit(): void {
        this.loginService.watchUser((auth: Auth) => {
            this.auth = auth;
        });

        this.haveLoginAsPermission$ = this.loginService
            .getCurrentUser()
            .pipe(map((res: CurrentUser) => res.loginAs));
    }

    /**
     * Call the logout service and clear the user session
     *
     * @returns boolean
     * @memberof ToolbarUserComponent
     */
    logout(): boolean {
        this.dotRouterService.doLogOut();

        return false;
    }

    /**
     * Call the logout as service and clear the user login as
     *
     * @param any $event
     * @memberof To/olbarUserComponent
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
