import {Component, Inject} from "@angular/core";

// Angular Material
import {MdToolbar} from '@angular2-material/toolbar/toolbar';
import {MD_SIDENAV_DIRECTIVES} from '@angular2-material/sidenav/sidenav';
import {MD_LIST_DIRECTIVES} from '@angular2-material/list/list';

@Component({
    selector: 'app',
    styles: ['md-sidenav-layout[fullscreen] {top: 64px}'],
    template: `
    <md-toolbar color="primary">
        <div class="main-toolbar">
            <h1>DotCMS</h1>
            <div class="user">freddy@dotcms.com</div>
        </div>
    </md-toolbar>
    <md-sidenav-layout fullscreen>
        <md-sidenav #start mode="side" opened="true">
            <md-sidenav #start mode="side" opened="true">
                <md-nav-list>
                    <template ngFor let-menu [ngForOf]="menuItems.navigationItems">
                        <h3>{{menu.tabName}}</h3>
                        <template ngFor let-menuItem [ngForOf]="menu.menuItems">
                            <a md-list-item class="item" linkTo="/portlet/{{menuItem.id}}" *ngIf="menuItem.url && !menuItem.angular && !menuItem.ajax">
                                {{menuItem.name}}
                            </a>
                            <a md-list-item class="item" linkTo="{{menuItem.url}}" *ngIf="menuItem.angular && !menuItem.ajax">
                                {{menuItem.name}}
                            </a>
                            <a md-list-item class="item" linkTo="{{menuItem.url}}"  *ngIf="menuItem.ajax">
                                {{menuItem.name}}
                            </a>
                        </template>
                        <hr />
                    </template>
                </md-nav-list>
            </md-sidenav>
        </md-sidenav>
        <route-view></route-view>
    </md-sidenav-layout>
    `,
    providers: [],
    directives: [MdToolbar, MD_SIDENAV_DIRECTIVES, MD_LIST_DIRECTIVES]
})
export class AppComponent {
    constructor(@Inject('menuItems') private menuItems:any[]) {}
}