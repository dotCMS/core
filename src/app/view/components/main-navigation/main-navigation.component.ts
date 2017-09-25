import { Component, ViewEncapsulation, OnInit } from '@angular/core';
import { RoutingService, Menu } from '../../../api/services/routing-service';

@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [],
    selector: 'dot-main-nav',
    styleUrls: ['./main-navigation.component.scss'],
    templateUrl: 'main-navigation.component.html'
})

export class MainNavigationComponent implements OnInit {
    private menuItems: Menu[];
    private menuItemIdActive: string;
    private menuActiveTabName: string;
    private open = true;

    constructor(private routingService: RoutingService) {}

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
        return this.menuItems.filter(
            menu => menu.menuItems.filter(menuItem => menuItem.id === menuItemSelectedId).length > 0
        )[0];
    }

    public  setMenuActiveTabName(id?: string) {
        this.open = !this.open;
        this.menuItemIdActive  = id || this.routingService.currentPortletId;
        const menuSelected = this.getMenuSelected(this.menuItemIdActive);
        this.menuActiveTabName = menuSelected ? menuSelected.tabName : null;
        if (menuSelected) {
            setTimeout(() => {
                menuSelected.isOpen = true;
            }, 0);
        }
    }

    ngOnInit() {
        if (this.routingService.currentMenu) {
            this.menuItems = this.routingService.currentMenu;
        }

        this.routingService.menusChange$.subscribe(menu => {
            this.menuItems = menu;
        });

        this.routingService.currentPortlet$.subscribe(id => {
            this.setMenuActiveTabName(id);
        });

        // Set the Menu Active Tab when the page first loads.
        this.setMenuActiveTabName();
    }
}
