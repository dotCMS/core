import {Component, ViewEncapsulation, ElementRef, ViewChild} from '@angular/core';
import {RoutingService, Menu, MenuItem} from '../../../api/services/routing-service';

@Component({
    encapsulation: ViewEncapsulation.None,

    providers: [],
    selector: 'dot-main-nav',
    styles: [require('./main-navigation.scss')],
    templateUrl: 'main-navigation.html',
})
export class MainNavigation {

    private menuItems: Menu[];

    private menuItemIdActive: string;
    private menuActiveTabName: string;
    private open = true;

    constructor(private routingService: RoutingService) {
        if (routingService.currentMenu) {
            this.menuItems = routingService.currentMenu;
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
    public gotToPage(link: string): void {
        this.routingService.changeRefreshPortlet(link);
    }

    private getMenuSelected(menuItemSelectedId: string): Menu {
        return this.menuItems.filter(menu => menu.menuItems.filter( menuItem => menuItem.id === menuItemSelectedId).length > 0)[0];
    }
}
