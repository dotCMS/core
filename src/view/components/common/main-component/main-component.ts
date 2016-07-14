import {Component, Inject, EventEmitter, Output, ViewEncapsulation} from '@angular/core';
import {FORM_DIRECTIVES} from '@angular/common';
import {LoginService} from '../../../../api/services/login-service';

// Custom Components
import {GlobalSearch} from '../global-search/global-search';
import {MainNavigation} from '../main-navigation/main-navigation';

// Angular Material
import {MdToolbar} from '@angular2-material/toolbar/toolbar';
import {MD_SIDENAV_DIRECTIVES} from '@angular2-material/sidenav/sidenav';
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {MdButton, MdAnchor} from '@angular2-material/button/button';
import {MdIcon, MdIconRegistry} from '@angular2-material/icon/icon';

@Component({
    directives: [MdToolbar, MD_SIDENAV_DIRECTIVES, MD_INPUT_DIRECTIVES, FORM_DIRECTIVES, MdAnchor, MdButton, MdIcon, GlobalSearch, MainNavigation],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [MdIconRegistry, LoginService],
    selector: 'dot-main-component',
    styleUrls: ['main-component.css'],
    templateUrl: ['main-component.html']
})
export class MainComponent {

    @Output() toggleMain = new EventEmitter<boolean>();
    _loginService: LoginService;
    logoutLabel: string;

    constructor( @Inject('menuItems') private menuItems: Array<any>, private _service: LoginService) {
        this._loginService = _service;
        this.logoutLabel = 'Logout'; // need to use internationalization
    }

    /**
     * Call the logout service
     */
    logout(): void {
        this._loginService.logOutUser().subscribe( data => {
            this.toggleMain.emit(true);
        }, (error) => {
            console.log(error);
        });

    }

}
