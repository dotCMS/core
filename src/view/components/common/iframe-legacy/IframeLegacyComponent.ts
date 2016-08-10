import { Component, Inject } from '@angular/core';
import { SafeResourceUrl, DomSanitizationService } from '@angular/platform-browser';

import { RouteParams } from '@ngrx/router';
import { Observable } from 'rxjs/Rx';

import {RoutingService} from "../../../../api/services/routing-service";

@Component({
    providers: [],
    selector: 'dot-iframe',
    template: ` 
        <iframe width="100%" height="100%" id="detailFrame" [src]="iframe | async" frameborder="0"></iframe>
    `,
})

export class IframeLegacyComponent {
    iframe: Observable<SafeResourceUrl>;
    private menuIdUrlMatch:Map<string, string>;

    constructor(params$: RouteParams, private routingService: RoutingService, sanitizer: DomSanitizationService) {

        this.iframe = params$.pluck<string>('id')
            .distinctUntilChanged()
            .map(id => {
                console.log('this.menuIdUrlMatch.get( id )', this.menuIdUrlMatch.get( id ));
                return sanitizer.bypassSecurityTrustResourceUrl( this.menuIdUrlMatch.get( id ) );
            });

        routingService.subscribeMenusChange().subscribe( menus => {
            this.menuIdUrlMatch = new Map();

            menus.forEach(menu => menu.menuItems.forEach(
                menuItem => {
                    this.menuIdUrlMatch.set( menuItem.id, menuItem.url );
                }
            ));
        });
    }
}
