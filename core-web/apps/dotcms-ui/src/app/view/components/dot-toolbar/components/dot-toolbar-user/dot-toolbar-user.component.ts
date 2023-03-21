import * as md5 from 'md5';
import { Observable } from 'rxjs';

import { Component, Inject, OnInit, ViewChild } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { Menu } from 'primeng/menu';

import { map, switchMap } from 'rxjs/operators';

import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { DotGravatarService } from '@dotcms/app/api/services/dot-gravatar-service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { LOCATION_TOKEN } from '@dotcms/app/providers';
import { DotMessageService } from '@dotcms/data-access';
import { Auth, CurrentUser, LoggerService, LoginService, LOGOUT_URL } from '@dotcms/dotcms-js';

@Component({
    selector: 'dot-toolbar-user',
    styleUrls: ['./dot-toolbar-user.component.scss'],
    templateUrl: 'dot-toolbar-user.component.html'
})
export class DotToolbarUserComponent implements OnInit {
    @ViewChild(Menu) menu: Menu;

    items$: Observable<MenuItem[]>;

    auth: Auth;

    showLoginAs = false;
    showMyAccount = false;

    logoutUrl = `${LOGOUT_URL}?r=${new Date().getTime()}`;

    constructor(
        @Inject(LOCATION_TOKEN) private location: Location,
        private loggerService: LoggerService,
        private loginService: LoginService,
        private dotNavigationService: DotNavigationService,
        private dotRouterService: DotRouterService,
        private dotMessageService: DotMessageService,
        private dotGravatarService: DotGravatarService
    ) {}

    ngOnInit(): void {
        this.loginService.watchUser((auth: Auth) => {
            this.auth = auth;
        });

        this.items$ = this.dotGravatarService.getPhoto(md5(this.getEmailFromAuthState())).pipe(
            switchMap((photo) =>
                this.loginService.getCurrentUser().pipe(
                    map(
                        ({ loginAs }: CurrentUser) =>
                            [
                                {
                                    id: 'toolbar-header',
                                    label: `<div class="toolbar-user__header">
                                    ${this.getTemplateFromPhoto(photo)}
                                    </div>`,
                                    escape: false
                                },
                                { separator: true },
                                {
                                    id: 'dot-toolbar-user-link-my-account',
                                    label: this.dotMessageService.get('my-account'),
                                    icon: 'pi pi-user-edit',
                                    visible: !this.auth.loginAsUser,
                                    command: () => this.toggleMyAccount()
                                },
                                {
                                    id: 'dot-toolbar-user-link-login-as',
                                    label: this.dotMessageService.get('login-as'),
                                    icon: 'pi pi-sort-alt',
                                    visible: !this.auth.loginAsUser && loginAs,
                                    command: () => this.tooggleLoginAs()
                                },
                                { separator: true, visible: !this.auth.loginAsUser },
                                {
                                    id: 'dot-toolbar-user-link-logout',
                                    label: this.dotMessageService.get('Logout'),
                                    icon: 'pi pi-sign-out',
                                    visible: !this.auth.loginAsUser,
                                    url: this.logoutUrl
                                },
                                {
                                    id: 'dot-toolbar-user-link-logout-as',
                                    label: this.dotMessageService.get('logout-as'),
                                    icon: 'pi pi-sign-out',
                                    visible: !!this.auth.loginAsUser,
                                    command: (event) => this.logoutAs(event.originalEvent)
                                }
                            ] as MenuItem[]
                    )
                )
            )
        );
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
    logoutAs($event: Event): void {
        $event.preventDefault();

        this.loginService.logoutAs().subscribe(
            () => {
                this.menu.hide();
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
        this.menu.hide();
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

    getEmailFromAuthState() {
        return this.auth.loginAsUser
            ? this.auth.loginAsUser.emailAddress
            : this.auth.user.emailAddress;
    }

    private getNameFromAuthState() {
        return this.auth.loginAsUser
            ? this.auth.loginAsUser.name || this.auth.loginAsUser.fullName
            : this.auth.user.name || this.auth.user.fullName;
    }

    private getTemplateFromPhoto(photo: string | null) {
        const name = this.getNameFromAuthState();

        const photoTemplate = photo
            ? `<img src=${photo} alt="${name} gravatar" title="${name} gravatar" class="gravatar" />`
            : `<div class="gravatar-placeholder">${name.charAt(0)}</div>`;

        const nameTemplate = `<p class="toolbar-user__user-name" id="dot-toolbar-user-name">
            ${name}
        </p>`;

        return photoTemplate + nameTemplate;
    }
}
