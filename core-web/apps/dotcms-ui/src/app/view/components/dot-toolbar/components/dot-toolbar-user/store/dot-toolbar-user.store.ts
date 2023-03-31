import { ComponentStore } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { Inject, Injectable } from '@angular/core';

import { MenuItem } from 'primeng/api';

import { filter, map, take } from 'rxjs/operators';

import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { LOCATION_TOKEN } from '@dotcms/app/providers';
import { DotMessageService } from '@dotcms/data-access';
import { Auth, LoggerService, LoginService, LOGOUT_URL } from '@dotcms/dotcms-js';

interface DotToolbarUserState {
    items: MenuItem[];
    userData: {
        email: string;
        name: string;
    };
    showMyAccount: boolean;
    showLoginAs: boolean;
}

const INITIAL_STATE: DotToolbarUserState = {
    items: [],
    userData: {
        email: '',
        name: ''
    },
    showMyAccount: false,
    showLoginAs: false
};

@Injectable()
export class DotToolbarUserStore extends ComponentStore<DotToolbarUserState> {
    private readonly FINAL_LOGOUT_URL = `${LOGOUT_URL}?r=${new Date().getTime()}`;

    readonly vm$: Observable<DotToolbarUserState> = this.select((state) => state).pipe(
        filter((vm) => !!vm.userData.email)
    );

    constructor(
        private loginService: LoginService,
        private dotMessageService: DotMessageService,
        private dotNavigationService: DotNavigationService,
        private loggerService: LoggerService,
        @Inject(LOCATION_TOKEN) private location: Location
    ) {
        super(INITIAL_STATE);
    }

    readonly showMyAccount = this.updater((state, value: boolean) => ({
        ...state,
        showMyAccount: value
    }));

    readonly showLoginAs = this.updater((state, value: boolean) => ({
        ...state,
        showLoginAs: value
    }));

    /**
     * Initialize the store with the user data
     *
     * @memberof DotToolbarUserStore
     */
    init() {
        this.loginService
            .loadAuth()
            .pipe(
                map((auth: Auth) => {
                    const userData = auth.loginAsUser || auth.user;

                    return {
                        items: this.getItems(auth),
                        userData: {
                            email: userData.emailAddress,
                            name: userData.name || userData.fullName
                        }
                    };
                }),
                take(1)
            )
            .subscribe((state) => {
                this.patchState(state);
            });
    }

    /**
     * Logout the user, redirect to first portlet and reload the page
     *
     * @memberof DotToolbarUserStore
     */
    logoutAs() {
        this.loginService
            .logoutAs()
            .pipe(take(1))
            .subscribe(
                () => {
                    this.dotNavigationService.goToFirstPortlet().then(() => {
                        this.location.reload();
                    });
                },
                (error) => {
                    this.loggerService.error(error);
                }
            );
    }

    private getItems(auth: Auth): MenuItem[] {
        const userData = auth.loginAsUser || auth.user;

        return [
            {
                id: 'toolbar-header',
                label: `
                    <div class="toolbar-user__header">
                        <p class="toolbar-user__user-name" id="dot-toolbar-user-name">
                            ${userData.name}
                        </p>
                    </div>`,
                escape: false
            },
            { separator: true },
            {
                id: 'dot-toolbar-user-link-my-account',
                label: this.dotMessageService.get('my-account'),
                icon: 'pi pi-user-edit',
                visible: !auth.isLoginAs,
                command: () => {
                    this.showMyAccount(true);
                }
            },
            {
                id: 'dot-toolbar-user-link-login-as',
                label: this.dotMessageService.get('login-as'),
                icon: 'pi pi-sort-alt',
                visible: !auth.isLoginAs,
                command: () => this.showLoginAs(true)
            },
            { separator: true, visible: !auth.isLoginAs },
            {
                id: 'dot-toolbar-user-link-logout',
                label: this.dotMessageService.get('Logout'),
                icon: 'pi pi-sign-out',
                visible: !auth.isLoginAs,
                url: this.FINAL_LOGOUT_URL
            },
            {
                id: 'dot-toolbar-user-link-logout-as',
                label: this.dotMessageService.get('logout-as'),
                icon: 'pi pi-sign-out',
                visible: !!auth.isLoginAs,
                command: () => this.logoutAs()
            }
        ] as MenuItem[];
    }
}
