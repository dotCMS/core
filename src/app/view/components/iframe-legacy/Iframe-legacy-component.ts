import { Component, ElementRef, ViewEncapsulation, OnInit } from '@angular/core';
import { LoginService, SiteService, DotcmsEventsService, LoggerService } from 'dotcms-js/dotcms-js';
import { ActivatedRoute } from '@angular/router';
import { RoutingService } from '../../../api/services/routing-service';
import { SafeResourceUrl, DomSanitizer } from '@angular/platform-browser';
import { MessageService } from '../../../api/services/messages-service';
import { IframeOverlayService } from '../../../api/services/iframe-overlay-service';
import { BaseComponent } from '../_common/_base/base-component';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-iframe',
    styleUrls: ['./iframe-legacy-component.scss'],
    templateUrl: 'iframe-legacy-component.html'
})
export class IframeLegacyComponent extends BaseComponent implements OnInit {
    iframe: SafeResourceUrl;
    iframeElement;
    loadingInProgress = true;
    showOverlay = false;

    constructor(
        messageService: MessageService,
        private dotcmsEventsService: DotcmsEventsService,
        private element: ElementRef,
        private loginService: LoginService,
        private route: ActivatedRoute,
        private routingService: RoutingService,
        private sanitizer: DomSanitizer,
        public iframeOverlayService: IframeOverlayService,
        public loggerService: LoggerService,
        public siteService: SiteService
    ) {
        super(['ask-reload-page-message'], messageService);

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

        this.iframeOverlayService.overlay.subscribe(val => (this.showOverlay = val));

        // TODO there is a weird 4px bug here that make unnecessary scroll, need to look into it.
        this.element.nativeElement.style.height = window.innerHeight - 64 + 'px';
        this.iframeElement = this.element.nativeElement.querySelector('iframe');

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

    /**
     * Hide the loading indicator
     * @param $event
     */
    hideLoadingIndicator($event): void {
        setTimeout(() => {
            this.loadingInProgress = false;
        }, 0);
    }

    initComponent(): void {
        this.route.params.pluck('id').subscribe((id: string) => {
            this.iframe = this.loadURL(this.routingService.getPortletURL(id));
        });

        this.route.queryParams.pluck('url').subscribe((url: string) => {
            if (url) {
                this.iframe = this.loadURL(url);
            }
        });
    }

    changeSiteReload(): void {
        if (!this.iframeElement) {
            this.iframeElement = this.element.nativeElement.querySelector('iframe');
        }

        if (
            this.iframeElement &&
            this.iframeElement.contentWindow &&
            this.iframeElement.contentWindow.location.href !== 'about:blank' && // For IE11
            this.iframeElement.contentWindow.location.pathname !== 'blank' &&
            this.routingService.currentPortletId !== 'sites'
        ) {
            this.loadingInProgress = true;
            this.iframeElement.contentWindow.location.reload();
        }
    }

    loadURL(url: string): SafeResourceUrl {
        let urlWithParameters = url;

        this.loadingInProgress = true;

        urlWithParameters += urlWithParameters.indexOf('?') === -1 ? '?' : '&';
        urlWithParameters +=
            urlWithParameters.indexOf('in_frame') === -1
                ? 'in_frame=true&frame=detailFrame&container=true'
                : '';

        return this.sanitizer.bypassSecurityTrustResourceUrl(urlWithParameters);
    }

    /**
     * Validate if the iframe window is send to the login page after jsessionid expired
     * then logout the user from angular session
     */
    checkSessionExpired(): void {
        if (this.iframeElement && this.iframeElement.contentWindow) {
            const currentPath = this.iframeElement.contentWindow.location.pathname;

            if (currentPath.indexOf('/c/portal_public/login') !== -1) {
                this.loginService.logOutUser().subscribe(
                    data => {},
                    error => {
                        this.loggerService.error(error);
                    }
                );
            }
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
            this.iframe = this.loadURL(this.routingService.getPortletURL(id));
        }
    }
}
