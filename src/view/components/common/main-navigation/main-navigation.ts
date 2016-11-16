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
    private menuIcons = {
        'Home': 'home',
        'Site Browser': 'sitemap',
        'Content': 'folder-open',
        'Marketing': 'shopping-cart',
        'Content Types': 'file-text',
        'System': 'cog',
    };

    constructor(routingService: RoutingService) {
        if (routingService.menus) {
            this.menuItems = routingService.menus;
        }

        routingService.menusChange$.subscribe(menu => {
            console.log(menu);
            this.menuItems = menu;
        });
    }
}
