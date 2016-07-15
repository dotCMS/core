import {Component, Inject} from '@angular/core';
import {Accordion, AccordionGroup} from '../accordion/accordion';
import {RoutingService} from '../../../../api/services/routing-service';
import {provideRouter, ROUTES} from '@ngrx/router';
import {provide} from '@angular/core';

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
    _routingService: RoutingService;

    constructor( @Inject('menuItems') private menuItems: Array<any>, private _routingserv: RoutingService) {
        this._routingService = _routingserv;
        // this.updateRoutes();

    }

    public updateRoutes(): void {
        this._routingService.getRoutes().subscribe(menu => {
            this.menuItems = menu.menuItems;
            provide('menuItems', {useValue: menu.menuItems});
            provideRouter(menu.routers);
            provide(ROUTES, { useValue: menu.routes });
        }, (error) => {
            console.log( error);
        });
    }
}
