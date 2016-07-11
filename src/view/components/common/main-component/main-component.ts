import {Component, Inject, EventEmitter, Output, ViewEncapsulation} from '@angular/core';
import {FORM_DIRECTIVES} from '@angular/common';
import {LoginService} from '../../../../api/services/login-service';

// Custom Components
import {GlobalSearch} from '../global-search/global-search';
import {MainNavigation} from '../main-navigation/main-navigation';
import {ToolbarNotifications} from '../toolbar-notifications/toolbar-notifications'

// Angular Material
import {MdToolbar} from '@angular2-material/toolbar/toolbar';
import {MD_SIDENAV_DIRECTIVES} from '@angular2-material/sidenav/sidenav';
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {MdButton, MdAnchor} from '@angular2-material/button/button';
import {MdIcon, MdAnchor} from '@angular2-material/icon/icon';

@Component({
    directives: [MdToolbar, MD_SIDENAV_DIRECTIVES, MD_INPUT_DIRECTIVES, FORM_DIRECTIVES, MdAnchor, MdButton, MdIcon, GlobalSearch, MainNavigation, ToolbarNotifications],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [LoginService],
    selector: 'dot-main-component',
    styleUrls: ['main-component.css'],
    templateUrl: ['main-component.html']
})
export class MainComponent {

    @Output() toggleMain = new EventEmitter<boolean>();
    logoutLabel: string;

    constructor(@Inject('menuItems') private menuItems: Array<any>, private _loginService: LoginService) {
        this.logoutLabel = 'Logout'; // TODO need to use internationalization
    }

    /**
     * Call the logout service
     */
    logout(): void {
        this._loginService.logOutUser().subscribe( data => {
            // This line update the browser url page without reloading the page
            window.history.replaceState('index.html', 'index', '/html/ng/index.html');
            this.toggleMain.emit(true);
        }, (error) => {
            console.log(error);
        });

    }

}
