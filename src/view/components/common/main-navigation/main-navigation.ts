import {Component, Inject} from '@angular/core';

// Angular Material
import {MD_LIST_DIRECTIVES} from '@angular2-material/list/list';

@Component({
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    selector: 'dot-main-nav',
    styleUrls: ['main-navigation.css'],
    template: `
    <md-nav-list>
        <template ngFor let-menu [ngForOf]="menuItems.navigationItems">
            <h3>{{menu.tabName}}</h3>
            <template ngFor let-menuItem [ngForOf]="menu.menuItems">
                <a md-list-item linkTo="/portlet/{{menuItem.id}}" *ngIf="menuItem.url && !menuItem.angular && !menuItem.ajax">
                    {{menuItem.name}}
                </a>
                <a md-list-item linkTo="{{menuItem.url}}" *ngIf="menuItem.angular && !menuItem.ajax">
                    {{menuItem.name}}
                </a>
                <a md-list-item linkTo="{{menuItem.url}}"  *ngIf="menuItem.ajax">
                    {{menuItem.name}}
                </a>
            </template>
            <hr />
        </template>
    </md-nav-list>
    `,
    providers: [],
    directives: [MD_LIST_DIRECTIVES],
})
export class MainNavigation {
    constructor(@Inject('menuItems') private menuItems:any[]) {
    }
}