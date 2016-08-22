import { Component, Inject, ElementRef,ViewEncapsulation } from '@angular/core';
import { SafeResourceUrl, DomSanitizationService } from '@angular/platform-browser';

import { RouteParams } from '@ngrx/router';
import { Observable } from 'rxjs/Rx';

import {RoutingService} from '../../../../api/services/routing-service';
import {MD_PROGRESS_CIRCLE_DIRECTIVES} from '@angular2-material/progress-circle';
import {SiteService} from '../../../../api/services/site-service';
import {SiteChangeListener} from '../../../../api/util/site-change-listener';


@Component({
    directives: [MD_PROGRESS_CIRCLE_DIRECTIVES],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    pipes: [],
    providers: [],
    selector: 'dot-iframe',
    styleUrls: ['iframe-legacy-component.css'],
    templateUrl: ['iframe-legacy-component.html']
})
export class IframeLegacyComponent extends SiteChangeListener{
    iframe: Observable<SafeResourceUrl>;
    iframeElement;
    private menuIdUrlMatch: Map<string, string>;
    private loadingInProgress: boolean = true;

    constructor(private params$: RouteParams, private routingService: RoutingService,
                private sanitizer: DomSanitizationService, private element: ElementRef, private siteService: SiteService) {
        super(siteService);
    }

    ngOnInit(): void {
        this.iframeElement = this.element.nativeElement.querySelector('iframe');

        this.iframe = this.params$.pluck<string>('id')
            .distinctUntilChanged()
            .map(id => {
                return this.sanitizer.bypassSecurityTrustResourceUrl( this.menuIdUrlMatch.get( id ) );
            });

        this.routingService.subscribeMenusChange().subscribe( menus => {
            this.menuIdUrlMatch = new Map();

            menus.forEach(menu => menu.menuItems.forEach(
                menuItem => {
                      this.menuIdUrlMatch.set( menuItem.id, menuItem.url );
                }
            ));
        });

        this.iframeElement.onload = () => {
            this.loadingInProgress = false;
        };
    }

    changeSiteReload(): void {
        if (this.iframeElement && this.iframeElement.contentWindow) {
            this.loadingInProgress = true;
            this.iframeElement.contentWindow.location.reload();
        }
    }
}
