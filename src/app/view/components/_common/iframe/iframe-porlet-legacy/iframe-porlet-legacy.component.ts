import { Component, ViewEncapsulation, OnInit, ViewChild } from '@angular/core';
import { SiteService, DotcmsEventsService, LoggerService } from 'dotcms-js/dotcms-js';
import { ActivatedRoute, UrlSegment } from '@angular/router';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { DotRouterService } from '../../../../../api/services/dot-router-service';
import { DotContentletService } from '../../../../../api/services/dot-contentlet.service';
import { DotLoadingIndicatorService } from '../dot-loading-indicator/dot-loading-indicator.service';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Location } from '@angular/common';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-iframe-porlet',
    templateUrl: 'iframe-porlet-legacy.component.html'
})
export class IframePortletLegacyComponent implements OnInit {
    @ViewChild('iframe') iframe;
    url: BehaviorSubject<string> = new BehaviorSubject('');

    constructor(
        private contentletService: DotContentletService,
        private dotcmsEventsService: DotcmsEventsService,
        private dotLoadingIndicatorService: DotLoadingIndicatorService,
        private dotNavigationService: DotMenuService,
        private dotRouterService: DotRouterService,
        private route: ActivatedRoute,
        public loggerService: LoggerService,
        public siteService: SiteService,
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
            'COPY_PAGE_ASSET'
        ];

        this.dotcmsEventsService.subscribeToEvents(events).subscribe(eventTypeWrapper => {
            if (this.dotRouterService.currentPortlet.id === 'site-browser') {
                this.loggerService.debug(
                    'Capturing Site Browser event',
                    eventTypeWrapper.eventType,
                    eventTypeWrapper.data
                );
                // TODO: When we finish the migration of the site browser this event will be handle.....
            }
        });
    }

    private setIframeSrc(): void {
        this.route.params
            .pluck('id')
            .map((id: string) => id)
            .withLatestFrom(this.route.parent.url.map((urlSegment: UrlSegment[]) => urlSegment[0].path))
            .flatMap(([id, url]) => {
                this.dotLoadingIndicatorService.show();
                return url === 'add'
                    ? this.contentletService.getUrlById(id)
                    : this.dotNavigationService.getUrlById(id);
            })
            .subscribe((url: string) => {
                this.setUrl(url);
            });
    }
    private setUrl(nextUrl: string): void {
        const currentUrl = this.url.getValue();
        if (currentUrl === nextUrl) {
            /*
                When user navigates deeper in the jsp (Like: Sites > Templates > Create/Edit a template)
                the iframe src remains the same, so in order to reload the "original" iframe page we
                need to first change the url to "something else" an empty string in this case to
                trigger the angular change detection and then set the original url again to the <iframe>
                and this will trigger the iframe load/reload.
            */
            this.url.next('');
            setTimeout(() => {
                this.url.next(nextUrl);
            }, 0);
        } else {
            this.url.next(nextUrl);
        }
    }
}
