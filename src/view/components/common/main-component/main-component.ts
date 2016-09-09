import {Component, ViewEncapsulation} from '@angular/core';
import {FORM_DIRECTIVES} from '@angular/common';

// Custom Components
import {DropdownComponent} from "../dropdown-component/dropdown-component";
import {GlobalSearch} from '../global-search/global-search';
import {MainNavigation} from '../main-navigation/main-navigation';
import {SiteSelectorComponent} from '../../site-selector/dot-site-selector-component';
import {ToolbarNotifications} from '../toolbar-notifications/toolbar-notifications';
import {ToolbarUserComponent} from "../toolbar-user/toolbar-user";

// Angular Material
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {MD_SIDENAV_DIRECTIVES} from '@angular2-material/sidenav/sidenav';
import {MdButton} from '@angular2-material/button/button';
import {MdIcon} from '@angular2-material/icon/icon';
import {MdToolbar} from '@angular2-material/toolbar/toolbar';
import {ToolbarAddContenletComponent} from '../../toolbar-add-contentlet/toolbar-add-contentlet';

@Component({
    directives: [MdToolbar, MD_SIDENAV_DIRECTIVES, MD_INPUT_DIRECTIVES, FORM_DIRECTIVES, MdButton, MdIcon,
        GlobalSearch, MainNavigation, ToolbarNotifications, SiteSelectorComponent, ToolbarUserComponent,
        DropdownComponent, ToolbarAddContenletComponent],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [],
    selector: 'dot-main-component',
    styleUrls: ['main-component.css'],
    templateUrl: ['main-component.html'],
})
export class MainComponent {
    private messages:any = {};
    private label:string = '';
    private subs;

    constructor() {
    }

    ngOnInit() {
        document.body.style.backgroundColor = '';
        document.body.style.backgroundImage = '';
    }

    ngOnDestroy() {
        this.messages = null;
        this.label = null;
    }
}
