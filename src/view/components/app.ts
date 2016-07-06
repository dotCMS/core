import {Component, Inject, ViewEncapsulation} from '@angular/core';
import {FORM_DIRECTIVES} from '@angular/common';

// Custom Components
import {GlobalSearch} from './common/global-search/global-search';
import {MainNavigation} from './common/main-navigation/main-navigation';

// Angular Material
import {MdToolbar} from '@angular2-material/toolbar/toolbar';
import {MD_SIDENAV_DIRECTIVES} from '@angular2-material/sidenav/sidenav';
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {MdButton, MdAnchor} from '@angular2-material/button/button';
import {MdIcon, MdIconRegistry} from '@angular2-material/icon/icon';

@Component({
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    selector: 'app',
    styleUrls: ['app.css'],
    template: `
    <md-toolbar color="primary">
        <div class="main-toolbar">
            <div class="host-selector">
                <button md-icon-button (click)="sideNav.toggle()">
                    <md-icon class="md-24">menu</md-icon>
                </button>
                <h1>DotCMS</h1>
            </div>
            <dot-global-search></dot-global-search>
            <div class="user">freddy@dotcms.com</div>
        </div>
    </md-toolbar>
    <md-sidenav-layout fullscreen>
        <md-sidenav #sideNav mode="side" opened="true">  
            <dot-main-nav></dot-main-nav>
        </md-sidenav>
        <route-view></route-view>
    </md-sidenav-layout>
    `,
    providers: [MdIconRegistry],
    directives: [MdToolbar, MD_SIDENAV_DIRECTIVES, MD_INPUT_DIRECTIVES, FORM_DIRECTIVES, MdAnchor, MdButton, MdIcon, GlobalSearch, MainNavigation],
    encapsulation: ViewEncapsulation.Emulated
})
export class AppComponent {
    constructor(@Inject('menuItems') private menuItems:any[]) {}
}