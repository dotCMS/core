import {Component, Inject, EventEmitter, Output, ViewEncapsulation} from '@angular/core';
import {FORM_DIRECTIVES} from '@angular/common';
import {LoginService} from '../../../../api/services/login-service';

// Custom Components
import {GlobalSearch} from '../global-search/global-search';
import {MainNavigation} from '../main-navigation/main-navigation';
import {ToolbarNotifications} from '../toolbar-notifications/toolbar-notifications';

// Angular Material
import {MdToolbar} from '@angular2-material/toolbar/toolbar';
import {MD_SIDENAV_DIRECTIVES} from '@angular2-material/sidenav/sidenav';
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {MdButton} from '@angular2-material/button/button';
import {MdIcon, MdAnchor} from '@angular2-material/icon/icon';
import { Router } from '@ngrx/router';
import {SiteSelectorContainer} from "../../site-selector/dot-site-selector-container";

@Component({
    directives: [MdToolbar, MD_SIDENAV_DIRECTIVES, MD_INPUT_DIRECTIVES, FORM_DIRECTIVES, MdButton, MdIcon,
        GlobalSearch, MainNavigation, ToolbarNotifications, SiteSelectorContainer],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [],
    selector: 'dot-main-component',
    styleUrls: ['main-component.css'],
    templateUrl: ['main-component.html'],
})
export class MainComponent {

    logoutLabel: string;

    constructor(private loginService: LoginService, private router: Router) {
        this.logoutLabel = 'Logout'; // TODO need to use internationalization
    }

    ngOnInit(){
        document.body.style.backgroundColor = '';
        document.body.style.backgroundImage = '';
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

}
