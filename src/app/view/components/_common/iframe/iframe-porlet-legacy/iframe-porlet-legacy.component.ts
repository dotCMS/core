import { Component, ElementRef, ViewEncapsulation, OnInit, ViewChild } from '@angular/core';
import { LoginService, SiteService, DotcmsEventsService, LoggerService } from 'dotcms-js/dotcms-js';
import { ActivatedRoute } from '@angular/router';
import { SafeResourceUrl, DomSanitizer } from '@angular/platform-browser';
import { MessageService } from '../../../../../api/services/messages-service';
import { RoutingService } from '../../../../../api/services/routing-service';
import { IframeOverlayService } from '../service/iframe-overlay-service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-iframe-porlet',
    templateUrl: 'iframe-porlet-legacy.component.html'
})
export class IFramePortletLegacyComponent  implements OnInit {
    @ViewChild ('iframe') iframe;
    url: SafeResourceUrl;

    constructor(
        private dotcmsEventsService: DotcmsEventsService,
        private route: ActivatedRoute,
        private routingService: RoutingService,
        public loggerService: LoggerService,
        public siteService: SiteService
    ) {
        /**
         * Subscribe to the portletUrl$ changes to force the
         * reload of a portlet in the iframe legacy component
         * when the user click the subnav link and the user is on the
         * same portlet or other
         */
        routingService.portletUrl$.subscribe(url => {
            this.reloadIframePortlet(url);
        });
    }

    ngOnInit(): void {
        this.siteService.switchSite$.subscribe(site => {
            this.changeSiteReload();
        });

        this.initComponent();

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
            if (this.routingService.currentPortletId === 'site-browser') {
                this.loggerService.debug(
                    'Capturing Site Browser event',
                    eventTypeWrapper.eventType,
                    eventTypeWrapper.data
                );
                // TODO: When we finish the migration of the site browser this event will be handle.....
            }
        });
    }

    private initComponent(): void {
        this.route.params.pluck('id').subscribe((id: string) => {
            this.url = this.routingService.getPortletURL(id);
        });

        this.route.queryParams.pluck('url').subscribe((url: string) => {
            if (url) {
                this.url = url;
            }
        });
    }

    /**
     * Tigger when the current site is changed, this method reload the iframe if is neccesary
     * @memberof IFramePortletLegacyComponent
     */
    changeSiteReload(): void {

        if (this.iframe.location.href !== 'about:blank' && // For IE11
            this.iframe.location.pathname !== 'blank' &&
            this.routingService.currentPortletId !== 'sites'
        ) {
            this.iframe.reload();
        }
    }

    /**
     * Force to reload the current iframe component portlet,
     * with the specified portlet url
     * @param url Url of the portlet to display
     */
    reloadIframePortlet(url: string): void {
        if (url !== undefined && url !== '') {
            const urlSplit = url.split('/');
            const id = urlSplit[urlSplit.length - 1];
            this.url = this.routingService.getPortletURL(id);
        }
    }
}
