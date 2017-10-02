import {
    Component,
    ElementRef,
    ViewEncapsulation,
    OnInit,
    Input,
    ViewChild,
    SimpleChanges
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SafeResourceUrl, DomSanitizer } from '@angular/platform-browser';
import { LoginService, LoggerService } from 'dotcms-js/dotcms-js';
import { IframeOverlayService } from '../../../../../api/services/iframe-overlay-service';
import { Observable } from 'rxjs/Observable';
import { DotLoadingIndicatorService } from '../dot-loading-indicator/dot-loading-indicator.service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-iframe',
    styleUrls: ['./iframe.component.scss'],
    templateUrl: 'iframe.component.html'
})
export class IframeComponent implements OnInit  {
    @ViewChild('iframeElement') iframeElement: ElementRef;
    @Input() src: string;

    iframeURL: SafeResourceUrl;
    showOverlay = false;

    private readonly DEFAULT_LOCATION = {
        pathname: '',
        href: ''
    };

    constructor(
        private element: ElementRef,
        private loggerService: LoggerService,
        private loginService: LoginService,
        private route: ActivatedRoute,
        private sanitizer: DomSanitizer,
        public dotLoadingIndicatorService: DotLoadingIndicatorService,
        public iframeOverlayService: IframeOverlayService
    ) {}

    ngOnInit(): void {
        this.iframeOverlayService.overlay.subscribe(val => (this.showOverlay = val));
        this.element.nativeElement.style.height = this.getIframeHeight(window.innerHeight);

        Observable.fromEvent(window, 'resize')
            .debounceTime(250)
            .subscribe(($event: any) => {
                this.element.nativeElement.style.height = this.getIframeHeight($event.target.innerHeight);
            });
    }

    /**
     * Validate if the iframe window is send to the login page after jsessionid expired
     * then logout the user from angular session
     *
     * @memberof IframeComponent
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

    get location(): any {
        return this.iframeElement && this.iframeElement.nativeElement.contentWindow
            ? this.iframeElement.nativeElement.contentWindow.location
            : this.DEFAULT_LOCATION;
    }

    private getIframeHeight(height: number): string {
        // TODO there is a weird 4px bug here that make unnecessary scroll, need to look into it.
        return height - 64 + 'px';
    }
}
