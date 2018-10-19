import { takeUntil } from 'rxjs/operators';
import {
    Component,
    ElementRef,
    OnInit,
    Input,
    ViewChild,
    Output,
    EventEmitter,
    NgZone,
    OnDestroy
} from '@angular/core';
import { LoginService, LoggerService } from 'dotcms-js';
import { DotLoadingIndicatorService } from '../dot-loading-indicator/dot-loading-indicator.service';
import { IframeOverlayService } from '../service/iframe-overlay.service';
import { DotIframeService } from '../service/dot-iframe/dot-iframe.service';
import { Subject } from 'rxjs';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';

@Component({
    selector: 'dot-iframe',
    styleUrls: ['./iframe.component.scss'],
    templateUrl: 'iframe.component.html'
})
export class IframeComponent implements OnInit, OnDestroy {
    @ViewChild('iframeElement')
    iframeElement: ElementRef;

    @Input()
    src: string;

    @Input()
    isLoading = false;

    @Output()
    load: EventEmitter<any> = new EventEmitter();

    @Output()
    keydown: EventEmitter<KeyboardEvent> = new EventEmitter();

    @Output()
    custom: EventEmitter<CustomEvent> = new EventEmitter();

    showOverlay = false;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotIframeService: DotIframeService,
        private loggerService: LoggerService,
        private loginService: LoginService,
        private ngZone: NgZone,
        private dotUiColorsService: DotUiColorsService,
        public dotLoadingIndicatorService: DotLoadingIndicatorService,
        public iframeOverlayService: IframeOverlayService
    ) {}

    ngOnInit(): void {
        this.iframeOverlayService.overlay.subscribe((val) => (this.showOverlay = val));

        this.dotIframeService
            .reloaded()
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                if (this.getIframeWindow()) {
                    this.getIframeLocation().reload();
                }
            });

        this.dotIframeService
            .ran()
            .pipe(takeUntil(this.destroy$))
            .subscribe((func: string) => {
                if (this.getIframeWindow() && typeof this.getIframeWindow()[func] === 'function') {
                    this.getIframeWindow()[func]();
                }
            });

        this.dotIframeService
            .reloadedColors()
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                const doc = this.getIframeDocument();

                if (doc) {
                    this.dotUiColorsService.setColors(doc.querySelector('html'));
                }
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Validate if the iframe window is send to the login page after jsessionid expired
     * then logout the user from angular session
     *
     * @memberof IframeComponent
     */
    checkSessionExpired(): void {
        if (
            !!this.getIframeWindow() &&
            this.getIframeLocation().pathname.indexOf('/c/portal_public/login') !== -1
        ) {
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
     * @param any $event
     * @memberof IframeComponent
     */
    onLoad($event): void {
        this.dotLoadingIndicatorService.hide();

        if (this.isIframeHaveContent()) {
            this.getIframeWindow().removeEventListener('keydown', this.emitKeyDown.bind(this));
            this.getIframeWindow().document.removeEventListener(
                'ng-event',
                this.emitCustonEvent.bind(this)
            );

            this.getIframeWindow().addEventListener('keydown', this.emitKeyDown.bind(this));
            this.getIframeWindow().document.addEventListener(
                'ng-event',
                this.emitCustonEvent.bind(this)
            );
            this.load.emit($event);

            const doc = this.getIframeDocument();

            if (doc) {
                this.dotUiColorsService.setColors(doc.querySelector('html'));
            }
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

    private getIframeDocument(): Document {
        return this.getIframeWindow().document;
    }

    private getIframeLocation(): any {
        return this.iframeElement.nativeElement.contentWindow.location;
    }

    private isIframeHaveContent(): boolean {
        return (
            this.iframeElement &&
            this.iframeElement.nativeElement.contentWindow.document.body.innerHTML.length
        );
    }
}
