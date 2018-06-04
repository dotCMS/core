import {
    Component,
    ElementRef,
    OnInit,
    Input,
    ViewChild,
    Output,
    EventEmitter,
    NgZone
} from '@angular/core';
import { LoginService, LoggerService } from 'dotcms-js/dotcms-js';
import { DotLoadingIndicatorService } from '../dot-loading-indicator/dot-loading-indicator.service';
import { IframeOverlayService } from '../service/iframe-overlay.service';
import { DotIframeService } from '../service/dot-iframe/dot-iframe.service';

@Component({
    selector: 'dot-iframe',
    styleUrls: ['./iframe.component.scss'],
    templateUrl: 'iframe.component.html'
})
export class IframeComponent implements OnInit {
    @ViewChild('iframeElement') iframeElement: ElementRef;
    @Input() src: string;
    @Input() isLoading = false;
    @Output() load: EventEmitter<any> = new EventEmitter();
    @Output() keydown: EventEmitter<KeyboardEvent> = new EventEmitter();
    @Output() custom: EventEmitter<CustomEvent> = new EventEmitter();

    showOverlay = false;

    constructor(
        private dotIframeService: DotIframeService,
        private loggerService: LoggerService,
        private loginService: LoginService,
        private ngZone: NgZone,
        public dotLoadingIndicatorService: DotLoadingIndicatorService,
        public iframeOverlayService: IframeOverlayService,
    ) {}

    ngOnInit(): void {
        this.iframeOverlayService.overlay.subscribe((val) => (this.showOverlay = val));

        this.dotIframeService.reloaded().subscribe(() => {
            if (this.getIframeWindow()) {
                this.getIframeLocation().reload();
            }
        });
    }

    /**
     * Validate if the iframe window is send to the login page after jsessionid expired
     * then logout the user from angular session
     *
     * @memberof IframeComponent
     */
    checkSessionExpired(): void {
        if (!!this.getIframeWindow() && this.getIframeLocation().pathname.indexOf('/c/portal_public/login') !== -1) {
            this.loginService.logOutUser().subscribe(
                (_data) => {},
                (error) => {
                    this.loggerService.error(error);
                }
            );
        }
    }

    /**
     * Called when iframe load event happen.
     *
     * @param {any} $event
     * @memberof IframeComponent
     */
    onLoad($event): void {
        this.dotLoadingIndicatorService.hide();

        if (this.isIframeHaveContent()) {
            this.getIframeWindow().removeEventListener('keydown', this.emitKeyDown.bind(this));
            this.getIframeWindow().document.removeEventListener('ng-event', this.emitCustonEvent.bind(this));

            this.getIframeWindow().addEventListener('keydown', this.emitKeyDown.bind(this));
            this.getIframeWindow().document.addEventListener('ng-event', this.emitCustonEvent.bind(this));
            this.load.emit($event);
        }
    }

    private emitKeyDown($event: KeyboardEvent): void {
        this.ngZone.run(() => {
            this.keydown.emit($event);
        });
    }

    private emitCustonEvent($event: CustomEvent): void {
        this.ngZone.run(() => {
            this.custom.emit($event);
        });
    }

    private getIframeWindow(): Window {
        return this.iframeElement && this.iframeElement.nativeElement.contentWindow;
    }

    private getIframeLocation(): any {
        return this.iframeElement.nativeElement.contentWindow.location;
    }

    private isIframeHaveContent(): boolean {
        return this.iframeElement && this.iframeElement.nativeElement.contentWindow.document.body.innerHTML.length;
    }
}
