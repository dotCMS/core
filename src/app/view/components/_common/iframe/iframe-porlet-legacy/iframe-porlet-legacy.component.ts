import { pluck, map, withLatestFrom, mergeMap } from 'rxjs/operators';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, UrlSegment } from '@angular/router';

import { BehaviorSubject } from 'rxjs';

import { SiteService, LoggerService } from 'dotcms-js';

import { DotLoadingIndicatorService } from '../dot-loading-indicator/dot-loading-indicator.service';
import { DotMenuService } from '@services/dot-menu.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotIframeEventsHandler } from './services/iframe-events-handler.service';
import { DotContentTypeService } from '@services/dot-content-type/dot-content-type.service';

@Component({
    selector: 'dot-iframe-porlet',
    styleUrls: ['./iframe-porlet-legacy.component.scss'],
    templateUrl: 'iframe-porlet-legacy.component.html'
})
export class IframePortletLegacyComponent implements OnInit {
    url: BehaviorSubject<string> = new BehaviorSubject('');
    isLoading = false;

    constructor(
        private contentletService: DotContentTypeService,
        private dotLoadingIndicatorService: DotLoadingIndicatorService,
        private dotMenuService: DotMenuService,
        private dotRouterService: DotRouterService,
        private route: ActivatedRoute,
        private dotIframeEventsHandler: DotIframeEventsHandler,
        public loggerService: LoggerService,
        public siteService: SiteService
    ) {}

    ngOnInit(): void {
        this.dotRouterService.portletReload$.subscribe((portletId: string) => {
            if (this.dotRouterService.isJSPPortlet()) {
                this.reloadIframePortlet(portletId);
            }
        });
        this.siteService.switchSite$.subscribe(() => {
            // prevent set empty URL - when the page first loads.
            if (this.url.getValue() !== '') {
                this.reloadIframePortlet();
            }
        });
        this.setIframeSrc();
    }

    /**
     * Handle the custom events emmited by the iframe
     *
     * @param any $event
     * @memberof IframePortletLegacyComponent
     */
    onCustomEvent($event: CustomEvent): void {
        this.dotIframeEventsHandler.handle($event);
    }

    /**
     * Tigger when the current site is changed, this method reload the iframe if is neccesary
     * @memberof IframePortletLegacyComponent
     */
    reloadIframePortlet(portletId?: string): void {
        this.dotLoadingIndicatorService.show();
        if (portletId) {
            this.dotMenuService.getUrlById(portletId).subscribe((url: string) => {
                this.setUrl(url);
            });
        } else {
            this.setUrl(this.url.getValue());
        }
    }

    private setIframeSrc(): void {
        // We use the query param to load a page in edit mode in the iframe
        const queryUrl$ = this.route.queryParams.pipe(
            pluck('url'),
            map((url: string) => url)
        );

        queryUrl$.subscribe((queryUrl: string) => {
            if (queryUrl) {
                this.setUrl(queryUrl);
            } else {
                this.setPortletUrl();
            }
        });
    }

    private setPortletUrl(): void {
        const portletId$ = this.route.params.pipe(
            pluck('id'),
            map((id: string) => id)
        );

        portletId$
            .pipe(
                withLatestFrom(
                    this.route.parent.url.pipe(
                        map((urlSegment: UrlSegment[]) => urlSegment[0].path)
                    )
                ),
                mergeMap(([id, url]) =>
                    url === 'add'
                        ? this.contentletService.getUrlById(id)
                        : this.dotMenuService.getUrlById(id)
                )
            )
            .subscribe((url: string) => {
                this.setUrl(url);
            });
    }

    /**
     * This function set isLoading to true, to remove the Legacy Iframe from the DOM while the src attribute is updated.
     * @param string nextUrl
     */
    private setUrl(nextUrl: string): void {
        this.dotLoadingIndicatorService.show();
        this.isLoading = true;
        this.url.next(nextUrl);
        // Need's this time to update the iFrame src.
        setTimeout(() => {
            this.isLoading = false;
        }, 0);
    }
}
