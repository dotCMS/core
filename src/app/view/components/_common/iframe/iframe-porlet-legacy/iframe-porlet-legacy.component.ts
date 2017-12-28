import { Component, ViewEncapsulation, OnInit, ViewChild, NgZone } from '@angular/core';
import { ActivatedRoute, UrlSegment } from '@angular/router';

import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Observable } from 'rxjs/Observable';

import { SiteService, DotcmsEventsService, LoggerService } from 'dotcms-js/dotcms-js';

import { DotContentletService } from '../../../../../api/services/dot-contentlet.service';
import { DotLoadingIndicatorService } from '../dot-loading-indicator/dot-loading-indicator.service';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { DotRouterService } from '../../../../../api/services/dot-router-service';
import { IframeComponent } from '../iframe-component';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-iframe-porlet',
    templateUrl: 'iframe-porlet-legacy.component.html',
})
export class IframePortletLegacyComponent implements OnInit {
    url: BehaviorSubject<string> = new BehaviorSubject('');
    isLoading = false;

    constructor(
        private contentletService: DotContentletService,
        private dotLoadingIndicatorService: DotLoadingIndicatorService,
        private dotMenuService: DotMenuService,
        private dotRouterService: DotRouterService,
        private dotcmsEventsService: DotcmsEventsService,
        private ngZone: NgZone,
        private route: ActivatedRoute,
        public loggerService: LoggerService,
        public siteService: SiteService
    ) {}

    ngOnInit(): void {
        this.dotRouterService.portletReload$.subscribe(() => {
            this.reloadIframePortlet();
        });
        this.siteService.switchSite$.subscribe(() => {
            // prevent set empty URL - when the page first loads.
            if (this.url.getValue() !== '') {
                this.reloadIframePortlet();
            }
        });
        this.setIframeSrc();
        this.bindGlobalEvents();
    }

    /**
     * Handle the iframe load
     *
     * @param {any} $event
     * @memberof IframePortletLegacyComponent
     */
    onLoad($event): void {
        Observable.fromEvent($event.target.contentWindow.document, 'ng-event').subscribe((event: any) => {
            if (event.detail.name === 'edit-page') {
                this.ngZone.run(() => {
                    this.dotRouterService.goToEditPage(event.detail.data.url);
                });
            }
        });
    }

    /**
     * Tigger when the current site is changed, this method reload the iframe if is neccesary
     * @memberof IframePortletLegacyComponent
     */
    reloadIframePortlet(): void {
        this.dotLoadingIndicatorService.show();
        this.setUrl(this.url.getValue());
    }

    private bindGlobalEvents(): void {
        const events: string[] = [
            'SAVE_FOLDER',
            'UPDATE_FOLDER',
            'DELETE_FOLDER',
            'SAVE_PAGE_ASSET',
            'UPDATE_PAGE_ASSET',
            'ARCHIVE_PAGE_ASSET',
            'UN_ARCHIVE_PAGE_ASSET',
            'DELETE_PAGE_ASSET',
            'PUBLISH_PAGE_ASSET',
            'UN_PUBLISH_PAGE_ASSET',
            'SAVE_FILE_ASSET',
            'UPDATE_FILE_ASSET',
            'ARCHIVE_FILE_ASSET',
            'UN_ARCHIVE_FILE_ASSET',
            'DELETE_FILE_ASSET',
            'PUBLISH_FILE_ASSET',
            'UN_PUBLISH_FILE_ASSET',
            'SAVE_LINK',
            'UPDATE_LINK',
            'ARCHIVE_LINK',
            'UN_ARCHIVE_LINK',
            'MOVE_LINK',
            'COPY_LINK',
            'DELETE_LINK',
            'PUBLISH_LINK',
            'UN_PUBLISH_LINK',
            'MOVE_FOLDER',
            'COPY_FOLDER',
            'MOVE_FILE_ASSET',
            'COPY_FILE_ASSET',
            'MOVE_PAGE_ASSET',
            'COPY_PAGE_ASSET',
        ];

        this.dotcmsEventsService.subscribeToEvents(events).subscribe((eventTypeWrapper) => {
            if (this.dotRouterService.currentPortlet.id === 'site-browser') {
                this.loggerService.debug(
                    'Capturing Site Browser event',
                    eventTypeWrapper.eventType,
                    eventTypeWrapper.data,
                );
                // TODO: When we finish the migration of the site browser this event will be handle.....
            }
        });
    }

    private setIframeSrc(): void {
        // We use the query param to load a page in edit mode in the iframe
        const queryUrl$ = this.route.queryParams.pluck('url').map((url: string) => url);

        queryUrl$.subscribe((queryUrl: string) => {
            if (queryUrl) {
                this.setUrl(queryUrl);
            } else {
                this.setPortletUrl();
            }
        });
    }

    private setPortletUrl(): void {
        const portletId$ = this.route.params.pluck('id').map((id: string) => id);

        portletId$
            .withLatestFrom(this.route.parent.url.map((urlSegment: UrlSegment[]) => urlSegment[0].path))
            .flatMap(
                ([id, url]) =>
                    url === 'add' ? this.contentletService.getUrlById(id) : this.dotMenuService.getUrlById(id),
            )
            .subscribe((url: string) => {
                this.setUrl(url);
            });
    }

    /**
     * This function set isLoading to true, to remove the Legacy Iframe from the DOM while the src attribute is updated.
     * @param {string} nextUrl
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
