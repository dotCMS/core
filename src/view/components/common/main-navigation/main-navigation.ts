import {Component, Inject} from '@angular/core';
import {Accordion, AccordionGroup} from '../accordion/accordion';
import {AppConfigurationService} from '../../../../api/services/system/app-configuration-service';
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

    constructor(@Inject('menuItems') private menuItems: Array<any>, private _appConfigurationService: AppConfigurationService) {
        // TODO update dinamically menuItems and routes calling
        // this.updateRoutes();

    }

    /**
     * This method should allow to update the menuItems and routes dinamically
     * TODO the menu is displayed but the changes in the route and menuItem
     * are not visible in the other component
     */
    public updateRoutes(): void {
        this._appConfigurationService.getConfigProperties().subscribe(configData => {
            this.menuItems = configData.menuItems;
            provide('menuItems', {useValue: configData.menuItems});
            provideRouter(configData.routers);
            provide(ROUTES, { useValue: configData.routes });
        }, (error) => {
            console.log( error);
        });
    }
}
