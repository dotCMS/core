import {Accordion, AccordionGroup} from '../accordion/accordion';
import {Component} from '@angular/core';
import {RoutingService, Menu} from '../../../../api/services/routing-service';

// Angular Material
import {MD_LIST_DIRECTIVES} from '@angular2-material/list/list';

@Component({
    directives: [MD_LIST_DIRECTIVES, Accordion, AccordionGroup],
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [],
    selector: 'dot-main-nav',
    styleUrls: ['main-navigation.css'],
    templateUrl: ['main-navigation.html'],
})

export class MainNavigation {

    private menuItems: Menu[];

    constructor(routingService: RoutingService) {
        routingService.menusChange$.subscribe(menu => {
            this.menuItems = menu;
        });
    }
}
