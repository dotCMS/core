import { Observable } from 'rxjs';

import { AsyncPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, Inject, OnInit, ViewChild } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { Menu, MenuModule } from 'primeng/menu';

import { map, take } from 'rxjs/operators';

import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { DotGravatarDirective } from '@directives/dot-gravatar/dot-gravatar.directive';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { LOCATION_TOKEN } from '@dotcms/app/providers';
import { DotPipesModule } from '@dotcms/app/view/pipes/dot-pipes.module';
import { DotMessageService } from '@dotcms/data-access';
import { Auth, CurrentUser, LoggerService, LoginService, LOGOUT_URL } from '@dotcms/dotcms-js';

import { DotLoginAsModule } from '../dot-login-as/dot-login-as.module';
import { DotMyAccountModule } from '../dot-my-account/dot-my-account.module';

@Component({
    selector: 'dot-toolbar-user',
    styleUrls: ['./dot-toolbar-user.component.scss'],
    templateUrl: 'dot-toolbar-user.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [
        DotGravatarDirective,
        AvatarModule,
        DotLoginAsModule,
        DotMyAccountModule,
        DotPipesModule,
        MenuModule,
        AsyncPipe,
        NgIf
    ]
})
export class DotToolbarUserComponent implements OnInit {
    @ViewChild(Menu) menu: Menu;

    items$: Observable<MenuItem[]>;

    auth: Auth;

    userData: { email: string; name: string };

    showLoginAs = false;
    showMyAccount = false;

    logoutUrl = `${LOGOUT_URL}?r=${new Date().getTime()}`;

    constructor(
        @Inject(LOCATION_TOKEN) private location: Location,
        private loggerService: LoggerService,
        private loginService: LoginService,
        private dotNavigationService: DotNavigationService,
        private dotRouterService: DotRouterService,
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit(): void {
        this.loginService.watchUser((auth: Auth) => {
            this.auth = auth;

            this.userData = {
                email: this.getEmailFromAuthState(),
                name: this.getNameFromAuthState()
            };
        });

        this.items$ = this.loginService.getCurrentUser().pipe(
            map(
                ({ loginAs }: CurrentUser) =>
                    [
                        {
                            id: 'toolbar-header',
                            label: `
                              <div class="toolbar-user__header">
                                  <p class="toolbar-user__user-name" id="dot-toolbar-user-name">
                                    ${this.userData.name}
                                  </p>
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

        this.loginService
            .logoutAs()
            .pipe(take(1))
            .subscribe(
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

    private getEmailFromAuthState() {
        return this.auth.loginAsUser
            ? this.auth.loginAsUser.emailAddress
            : this.auth.user.emailAddress;
    }

    private getNameFromAuthState() {
        return this.auth.loginAsUser
            ? this.auth.loginAsUser.name || this.auth.loginAsUser.fullName
            : this.auth.user.name || this.auth.user.fullName;
    }
}
