import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    Component,
    ElementRef,
    EventEmitter,
    inject,
    Input,
    NgZone,
    OnDestroy,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';

import { debounceTime, filter, takeUntil } from 'rxjs/operators';

import { DotIframeService, DotRouterService, DotUiColorsService } from '@dotcms/data-access';
import { DotcmsEventsService, DotEventTypeWrapper, LoggerService } from '@dotcms/dotcms-js';
import { DotFunctionInfo } from '@dotcms/dotcms-models';
import { DotLoadingIndicatorService } from '@dotcms/utils';

import { DotOverlayMaskModule } from '../../dot-overlay-mask/dot-overlay-mask.module';
import { DotLoadingIndicatorModule } from '../dot-loading-indicator/dot-loading-indicator.module';
import { DotSafeUrlPipe } from '../pipes/dot-safe-url/dot-safe-url.pipe';
import { IframeOverlayService } from '../service/iframe-overlay.service';

@Component({
    selector: 'dot-iframe',
    styleUrls: ['./iframe.component.scss'],
    templateUrl: 'iframe.component.html',
    imports: [CommonModule, DotLoadingIndicatorModule, DotOverlayMaskModule, DotSafeUrlPipe]
})
export class IframeComponent implements OnInit, OnDestroy {
    private dotIframeService = inject(DotIframeService);
    private dotRouterService = inject(DotRouterService);
    private dotUiColorsService = inject(DotUiColorsService);
    private dotcmsEventsService = inject(DotcmsEventsService);
    private ngZone = inject(NgZone);
    dotLoadingIndicatorService = inject(DotLoadingIndicatorService);
    iframeOverlayService = inject(IframeOverlayService);
    loggerService = inject(LoggerService);

    @ViewChild('iframeElement') iframeElement: ElementRef;

    @Input() src: string;

    @Input() isLoading = false;

    @Output() charge: EventEmitter<unknown> = new EventEmitter();

    @Output() keyWasDown: EventEmitter<KeyboardEvent> = new EventEmitter();

    @Output() custom: EventEmitter<CustomEvent> = new EventEmitter();

    showOverlay = false;

    private destroy$: Subject<boolean> = new Subject<boolean>();

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
            .subscribeToEvents<unknown>(events)
            .pipe(takeUntil(this.destroy$));

        webSocketEvents$
            .pipe(filter(() => this.dotRouterService.currentPortlet.id === 'site-browser'))
            .subscribe((event: DotEventTypeWrapper<unknown>) => {
                this.loggerService.debug('Capturing Site Browser event', event.name, event.data);
            });

        webSocketEvents$
            .pipe(
                filter(
                    (event: DotEventTypeWrapper<unknown>) =>
                        (this.iframeElement.nativeElement.contentWindow &&
                            event.name === 'DELETE_BUNDLE') ||
                        event.name === 'PAGE_RELOAD' // Provinding this event so backend devs can reload the jsp easily
                )
            )
            .subscribe(() => {
                this.iframeElement.nativeElement.contentWindow.postMessage('reload');
            });

        /**
         * The debouncetime is required because when the websocket event is received,
         * the list of plugins still cannot be updated, thi is because the framework (OSGI)
         * needs to restart before the list can be refreshed.
         * Currently, an event cannot be emitted when the framework finishes restarting.
         */
        this.dotcmsEventsService
            .subscribeTo('OSGI_BUNDLES_LOADED')
            .pipe(takeUntil(this.destroy$), debounceTime(4000))
            .subscribe(() => {
                this.dotIframeService.run({ name: 'getBundlesData' });
            });
    }

    private emitKeyDown($event: KeyboardEvent): void {
        this.ngZone.run(() => {
            this.keyWasDown.emit($event);
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

    private getIframeLocation(): Location {
        return this.iframeElement.nativeElement.contentWindow.location;
    }

    private handleErrors(error: number): void {
        const errorMapHandler = {
            401: () => {
                this.dotRouterService.doLogOut();
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
        this.charge.emit($event);

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

    private setArgs(args: unknown[]): unknown[] {
        return args ? args : [];
    }
}
