import {
    Component,
    ElementRef,
    ViewEncapsulation,
    OnInit,
    OnChanges,
    Input,
    ViewChild
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SafeResourceUrl, DomSanitizer } from '@angular/platform-browser';
import { LoginService, LoggerService } from 'dotcms-js/dotcms-js';
import { IframeOverlayService } from '../../../../../api/services/iframe-overlay-service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-iframe',
    styleUrls: ['./iframe.component.scss'],
    templateUrl: 'iframe.component.html'
})
export class IframeComponent implements OnInit, OnChanges {
    private readonly DEFAULT_LOCATION = {
        pathname: '',
        href: ''
    };

    @Input() src: string;
    @ViewChild('iframeElement') iframeElement: ElementRef;

    iframeURL: SafeResourceUrl;
    loadingInProgress = true;
    showOverlay = false;

    constructor(
        private element: ElementRef,
        private route: ActivatedRoute,
        private sanitizer: DomSanitizer,
        public iframeOverlayService: IframeOverlayService,
        private loginService: LoginService,
        private loggerService: LoggerService
    ) {}

    ngOnInit(): void {
        this.iframeOverlayService.overlay.subscribe(val => (this.showOverlay = val));

        // TODO there is a weird 4px bug here that make unnecessary scroll, need to look into it.
        this.element.nativeElement.style.height = window.innerHeight - 64 + 'px';
    }

    ngOnChanges(changes): void {
        if (changes.src && changes.src.currentValue) {
            this.iframeURL = this.loadURL(changes.src.currentValue);
        }
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

    private loadURL(url: string): SafeResourceUrl {
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
        if (this.iframeElement && this.iframeElement.nativeElement.contentWindow) {
            const currentPath = this.iframeElement.nativeElement.contentWindow.location.pathname;

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
     * Reload the iframe with the current URL
     * @memberof IframeComponent
     */
    reload(): void {
        if (this.iframeElement && this.iframeElement.nativeElement.contentWindow) {
            this.loadingInProgress = true;
            this.iframeElement.nativeElement.contentWindow.location.reload();
        }
    }

    get location(): any {
        return this.iframeElement && this.iframeElement.nativeElement.contentWindow ?
                    this.iframeElement.nativeElement.contentWindow.location : this.DEFAULT_LOCATION;
    }
}
