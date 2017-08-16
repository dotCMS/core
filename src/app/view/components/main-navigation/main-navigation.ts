import {Component, ViewEncapsulation} from '@angular/core';
import {RoutingService, Menu} from '../../../api/services/routing-service';

@Component({
    encapsulation: ViewEncapsulation.None,

    providers: [],
    selector: 'dot-main-nav',
    styleUrls: ['./main-navigation.scss'],
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
            const menuSelected = this.getMenuSelected(id);
            this.menuActiveTabName = menuSelected ? menuSelected.tabName : null;
        });
    }

    /**
     * Change or refresh the portlets from the main menu
     * @param menuItem portlet url
     */
    public gotToPage(event: any, link: string): void {
        if (!event.ctrlKey && !event.metaKey) {
            this.routingService.changeRefreshPortlet(link);
        }
    }

    private getMenuSelected(menuItemSelectedId: string): Menu {
        return this.menuItems.filter(menu => menu.menuItems.filter( menuItem => menuItem.id === menuItemSelectedId).length > 0)[0];
    }
}
