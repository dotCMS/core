import {Component, ViewEncapsulation, ElementRef, ViewChild} from '@angular/core';
import {RoutingService, Menu, MenuItem} from '../../../../api/services/routing-service';

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

    private menuItemIdActive: string;
    private menuActiveTabName: string;
    private open: boolean = true;

    constructor(private routingService: RoutingService) {
        if (routingService.menus) {
            this.menuItems = routingService.menus;
        }

        routingService.menusChange$.subscribe(menu => {
            this.menuItems = menu;
        });

        routingService.currentPortlet$.subscribe(id => {
            this.open = !this.open;
            this.menuItemIdActive = id;
            this.menuActiveTabName = this.getMenuSelected(id).tabName;
        });
    }

    /**
     * Change or refresh the portlets from the main menu
     * @param menuItem portlet url
     */
    public gotToPage(menuItem: MenuItem): void {
        let link = menuItem.menuLink;
        this.routingService.changeRefreshPortlet(link);
    }

    private getMenuSelected(menuItemSelectedId: string): Menu {
        return this.menuItems.filter(menu => menu.menuItems.filter( menuItem => menuItem.id === menuItemSelectedId).length > 0)[0];
    }
}
