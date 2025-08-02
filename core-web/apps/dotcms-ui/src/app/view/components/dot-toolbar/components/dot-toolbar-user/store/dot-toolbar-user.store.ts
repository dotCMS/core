import { ComponentStore } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { MenuItem } from 'primeng/api';

import { filter, take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { Auth, LoggerService, LoginService, LOGOUT_URL } from '@dotcms/dotcms-js';

import { LOCATION_TOKEN } from '../../../../../../providers';
import { DotNavigationService } from '../../../../dot-navigation/services/dot-navigation.service';

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
    readonly #loginService = inject(LoginService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotNavigationService = inject(DotNavigationService);
    readonly #loggerService = inject(LoggerService);
    readonly #location = inject<Location>(LOCATION_TOKEN);
    readonly #FINAL_LOGOUT_URL = `${LOGOUT_URL}?r=${new Date().getTime()}`;

    readonly vm$: Observable<DotToolbarUserState> = this.select((state) => state).pipe(
        filter((vm) => !!vm.userData.email)
    );

    constructor() {
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
        // There's an error were you always get redirected to first portlet
        // this patches that error, I tried different options to avoid the use of this method and use a regular observable
        // but all the options I tried didn't work, the redirect kept happening
        this.#loginService.watchUser((auth: Auth) => {
            const userData = auth.loginAsUser || auth.user;

            this.patchState({
                items: this.getItems(auth),
                userData: {
                    email: userData.emailAddress,
                    name: userData.name || userData.fullName
                }
            });
        });
    }

    /**
     * Logout the user, redirect to first portlet and reload the page
     *
     * @memberof DotToolbarUserStore
     */
    logoutAs() {
        this.#loginService
            .logoutAs()
            .pipe(take(1))
            .subscribe(
                () => {
                    this.#dotNavigationService.goToFirstPortlet().then(() => {
                        this.#location.reload();
                    });
                },
                (error) => {
                    this.#loggerService.error(error);
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
                label: this.#dotMessageService.get('my-account'),
                icon: 'pi pi-user',
                visible: !auth.isLoginAs,
                command: () => this.showMyAccount(true)
            },
            {
                id: 'dot-toolbar-user-link-login-as',
                label: this.#dotMessageService.get('login-as'),
                icon: 'pi pi-users',
                visible: !auth.isLoginAs,
                command: () => this.showLoginAs(true)
            },
            { separator: true, visible: !auth.isLoginAs },
            {
                id: 'dot-toolbar-user-link-logout',
                label: this.#dotMessageService.get('Logout'),
                icon: 'pi pi-sign-out',
                visible: !auth.isLoginAs,
                url: this.#FINAL_LOGOUT_URL,
                target: '_self',
                styleClass: 'toolbar-user__logout'
            },
            {
                id: 'dot-toolbar-user-link-logout-as',
                label: this.#dotMessageService.get('logout-as'),
                icon: 'pi pi-sign-out',
                visible: !!auth.isLoginAs,
                command: () => this.logoutAs(),
                styleClass: 'toolbar-user__logout'
            }
        ] as MenuItem[];
    }
}
