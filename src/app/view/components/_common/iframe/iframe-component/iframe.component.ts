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

import { takeUntil, filter } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { LoginService, DotcmsEventsService, DotEventTypeWrapper, LoggerService } from 'dotcms-js';

import { DotLoadingIndicatorService } from '../dot-loading-indicator/dot-loading-indicator.service';
import { IframeOverlayService } from '../service/iframe-overlay.service';
import { DotIframeService } from '../service/dot-iframe/dot-iframe.service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotFunctionInfo } from '@models/dot-function-info/dot-function-info.model';

@Component({
    selector: 'dot-iframe',
    styleUrls: ['./iframe.component.scss'],
    templateUrl: 'iframe.component.html'
})
export class IframeComponent implements OnInit, OnDestroy {
    @ViewChild('iframeElement') iframeElement: ElementRef;

    @Input() src: string;

    @Input() isLoading = false;

    @Output() load: EventEmitter<any> = new EventEmitter();

    @Output() keydown: EventEmitter<KeyboardEvent> = new EventEmitter();

    @Output() custom: EventEmitter<CustomEvent> = new EventEmitter();

    showOverlay = false;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotIframeService: DotIframeService,
        private dotRouterService: DotRouterService,
        private dotUiColorsService: DotUiColorsService,
        private dotcmsEventsService: DotcmsEventsService,
        private loginService: LoginService,
        private ngZone: NgZone,
        public dotLoadingIndicatorService: DotLoadingIndicatorService,
        public iframeOverlayService: IframeOverlayService,
        public loggerService: LoggerService
    ) {}

    ngOnInit(): void {
        this.iframeOverlayService.overlay
            .pipe(takeUntil(this.destroy$))
            .subscribe((val: boolean) => (this.showOverlay = val));

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
            .subscribe((func: DotFunctionInfo) => {
                if (
                    this.getIframeWindow() &&
                    typeof this.getIframeWindow()[func.name] === 'function'
                ) {
                    this.getIframeWindow()[func.name](...this.setArgs(func.args));
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

        this.bindGlobalEvents();
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Called when iframe load event happen.
     *
     * @param any $event
     * @memberof IframeComponent
     */
    onLoad($event): void {
        // JSP is setting the error number in the title
        const errorCode = parseInt($event.target.contentDocument.title, 10);
        if (errorCode > 400) {
            this.handleErrors(errorCode);
        }

        this.dotLoadingIndicatorService.hide();

        if (this.isIframeHaveContent()) {
            this.handleIframeEvents($event);
        }
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
            'DELETE_BUNDLE',
            'PAGE_RELOAD'
        ];

        const webSocketEvents$ = this.dotcmsEventsService
            .subscribeToEvents<any>(events)
            .pipe(takeUntil(this.destroy$));

        webSocketEvents$
            .pipe(filter(() => this.dotRouterService.currentPortlet.id === 'site-browser'))
            .subscribe((event: DotEventTypeWrapper<any>) => {
                this.loggerService.debug('Capturing Site Browser event', event.name, event.data);
            });

        webSocketEvents$
            .pipe(
                filter(
                    (event: DotEventTypeWrapper<any>) =>
                        (this.iframeElement.nativeElement.contentWindow &&
                            event.name === 'DELETE_BUNDLE') ||
                        event.name === 'PAGE_RELOAD' // Provinding this event so backend devs can reload the jsp easily
                )
            )
            .subscribe(() => {
                this.iframeElement.nativeElement.contentWindow.postMessage('reload');
            });
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

    private handleErrors(error: number): void {
        const errorMapHandler = {
            401: () => {
                this.loginService.logOutUser().subscribe(_data => {});
            }
        };

        if (errorMapHandler[error]) {
            errorMapHandler[error]();
        }
    }

    private handleIframeEvents($event): void {
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

    private isIframeHaveContent(): boolean {
        return (
            this.iframeElement &&
            this.iframeElement.nativeElement.contentWindow.document.body.innerHTML.length
        );
    }

    private setArgs(args: any[]): any[] {
        return args ? args : [];
    }
}
