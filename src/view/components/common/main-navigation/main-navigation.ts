import {Component, Inject} from '@angular/core';
import {Accordion, AccordionGroup} from '../accordion/accordion';
import {AppConfigurationService} from '../../../../api/services/system/app-configuration-service';
import {RoutingService} from '../../../../api/services/routing-service';
import {provideRouter, ROUTES} from '@ngrx/router';
import {provide} from '@angular/core';

// Angular Material
import {MD_LIST_DIRECTIVES} from '@angular2-material/list/list';
import Observable =

@Component({
    directives: [MD_LIST_DIRECTIVES, Accordion, AccordionGroup],
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [],
    selector: 'dot-main-nav',
    styleUrls: ['main-navigation.css'],
    templateUrl: ['main-navigation.html'],
})

export class MainNavigation {

    private menuItems:any[];
    private message:string = 'error';

    constructor(routingService:RoutingService) {
        console.log('MainNavigation');
        routingService.subscribeMenusChange().subscribe( menu => {
            console.log('MENU', menu);
            this.menuItems = menu;
            this.message = 'success';
        });
    }


}
