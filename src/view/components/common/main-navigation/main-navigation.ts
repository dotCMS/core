import {Component, ViewEncapsulation} from '@angular/core';
import {RoutingService, Menu} from '../../../../api/services/routing-service';

@Component({
    encapsulation: ViewEncapsulation.None,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [],
    selector: 'dot-main-nav',
    styleUrls: ['main-navigation.css'],
    templateUrl: ['main-navigation.html'],
})

export class MainNavigation {

    private menuItems: Menu[];

    constructor(private routingService: RoutingService) {
        if (routingService.menus) {
            this.menuItems = routingService.menus;
        }

        routingService.menusChange$.subscribe(menu => {
            this.menuItems = menu;
        });
    }

    /**
     * Change or refresh the portlets from the main menu
     * @param link portlet url
     */
    public gotToPage(link: string): void {
        this.routingService.changeRefreshPortlet(link);
    }
}
