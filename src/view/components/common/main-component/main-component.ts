import {Component, Inject, EventEmitter, Output, ViewEncapsulation} from '@angular/core';
import {FORM_DIRECTIVES} from '@angular/common';
import {LoginService} from '../../../../api/services/login-service';
import {FormatDate} from '../../../../api/services/format-date-service';

// Custom Components
import {GlobalSearch} from '../global-search/global-search';
import {MainNavigation} from '../main-navigation/main-navigation';
import {ToolbarNotifications} from '../toolbar-notifications/toolbar-notifications';

// Angular Material
import {MdToolbar} from '@angular2-material/toolbar/toolbar';
import {MD_SIDENAV_DIRECTIVES} from '@angular2-material/sidenav/sidenav';
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {MdButton} from '@angular2-material/button/button';
import {MdIcon} from '@angular2-material/icon/icon';
import { Router } from '@ngrx/router';
import {SiteSelectorComponent} from '../../site-selector/dot-site-selector-component';
import {MyAccountComponent} from '../../my-account-component/dot-my-account-component';

@Component({
    directives: [MdToolbar, MD_SIDENAV_DIRECTIVES, MD_INPUT_DIRECTIVES, FORM_DIRECTIVES, MdButton, MdIcon,
        GlobalSearch, MainNavigation, ToolbarNotifications, SiteSelectorComponent, MyAccountComponent],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [FormatDate],
    selector: 'dot-main-component',
    styleUrls: ['main-component.css'],
    templateUrl: ['main-component.html'],
})
export class MainComponent {
    private logoutLabel: string;
    private showMyAccount: boolean = false;

    constructor(private loginService: LoginService, private router: Router, private formatDate: FormatDate) {
        // TODO need to use internationalization
        this.logoutLabel = 'Logout';
        // TODO: probably this will be initialize some place else, for now it's good here.
        this.setGlobalLang();
    }

    ngOnInit(): void {
        document.body.style.backgroundColor = '';
        document.body.style.backgroundImage = '';
    }

    setGlobalLang(): void {
        // TODO: this strings of date information will come from the rest API base on user language.
        this.formatDate.setLang('es', {
            relativeTime : {
                d : 'un jour',
                dd : '%d jours',
                future : 'dans %s',
                h : 'une heure',
                hh : '%d heures',
                M : 'un mois',
                m : 'une minute',
                mm : '%d minutes',
                MM : '%d mois',
                past : 'il y a %s',
                s : 'quelques secondes',
                y : 'une année',
                yy : '%d années'
            },
        })
    }

    /**
     * Call the logout service
     */
    logout(): void {
        this.loginService.logOutUser().subscribe(data => {
            this.router.go('/public/login');
        }, (error) => {
            console.log(error);
        });

    }

    toggleMyAccount(): void {
        this.showMyAccount = !this.showMyAccount;
    }
}
