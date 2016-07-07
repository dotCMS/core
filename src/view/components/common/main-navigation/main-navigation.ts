import {Component, Inject} from '@angular/core';
import {Accordion, AccordionGroup} from '../accordion/accordion'

// Angular Material
import {MD_LIST_DIRECTIVES} from '@angular2-material/list/list';

@Component({
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    selector: 'dot-main-nav',
    styleUrls: ['main-navigation.css'],
    template: `
    <md-nav-list>
        <accordion>
            <nav>
                <template ngFor let-menu [ngForOf]="menuItems.navigationItems">
                    <accordion-group [heading]="menu.tabName">
                        <div class="md-nav-list__submenu">
                            <template ngFor let-menuItem [ngForOf]="menu.menuItems">
                                <a linkTo="/portlet/{{menuItem.id}}" *ngIf="menuItem.url && !menuItem.angular && !menuItem.ajax">
                                    {{menuItem.name}}
                                </a>
                                <a linkTo="{{menuItem.url}}" *ngIf="menuItem.angular && !menuItem.ajax">
                                    {{menuItem.name}}
                                </a>
                                <a linkTo="{{menuItem.url}}"  *ngIf="menuItem.ajax">
                                    {{menuItem.name}}
                                </a>
                            </template>
                        </div>
                    </accordion-group>
                </template>
            </nav>
        </accordion>
    </md-nav-list>
    `,
    providers: [],
    directives: [MD_LIST_DIRECTIVES, Accordion, AccordionGroup],
})
export class MainNavigation {
    constructor(@Inject('menuItems') private menuItems:any[]) {
    }
}