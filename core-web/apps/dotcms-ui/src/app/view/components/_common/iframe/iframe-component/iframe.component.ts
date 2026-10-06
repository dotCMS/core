import { merge, Subject } from 'rxjs';

import {
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    inject,
    input,
    NgZone,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
    ChangeDetectionStrategy
} from '@angular/core';

import { debounceTime, filter, map, takeUntil } from 'rxjs/operators';

import {
    DotEventsSocket,
    DotIframeService,
    DotRouterService,
    DotUiColorsService
} from '@dotcms/data-access';
import { DotEventTypeWrapper, LoggerService } from '@dotcms/dotcms-js';
import { DotFunctionInfo } from '@dotcms/dotcms-models';
import { DotLoadingIndicatorService } from '@dotcms/utils';

import { DotOverlayMaskComponent } from '../../dot-overlay-mask/dot-overlay-mask.component';
import { DotLoadingIndicatorComponent } from '../dot-loading-indicator/dot-loading-indicator.component';
import { DotSafeUrlPipe } from '../pipes/dot-safe-url/dot-safe-url.pipe';
import { IframeOverlayService } from '../service/iframe-overlay.service';

@Component({
    selector: 'dot-iframe',
    templateUrl: 'iframe.component.html',
    styles: [
        `
            :host {
                display: block;
                height: 100%;
                position: relative;
                overflow: hidden;
            }
        `
    ],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [DotLoadingIndicatorComponent, DotOverlayMaskComponent, DotSafeUrlPipe]
})
export class IframeComponent implements OnInit, OnDestroy {
    private dotIframeService = inject(DotIframeService);
    private dotRouterService = inject(DotRouterService);
    private dotUiColorsService = inject(DotUiColorsService);
    private dotEventsSocket = inject(DotEventsSocket);
    private ngZone = inject(NgZone);
    private cdr = inject(ChangeDetectorRef);
    dotLoadingIndicatorService = inject(DotLoadingIndicatorService);
    iframeOverlayService = inject(IframeOverlayService);
    loggerService = inject(LoggerService);

    @ViewChild('iframeElement') iframeElement!: ElementRef;

    readonly src = input<string>();

    $isLoading = input(false, { alias: 'isLoading' });

    @Output() charge: EventEmitter<Event> = new EventEmitter();

    @Output() keyWasDown: EventEmitter<KeyboardEvent> = new EventEmitter();

    @Output() custom: EventEmitter<CustomEvent> = new EventEmitter();

    showOverlay = false;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit(): void {
        this.iframeOverlayService.overlay
            .pipe(takeUntil(this.destroy$))
            .subscribe((val: boolean) => {
                queueMicrotask(() => {
                    this.showOverlay = val;
                    this.cdr.markForCheck();
                });
            });

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
                const iframeWindow = this.getIframeWindow() as unknown as
                    | Record<string, (...args: unknown[]) => void>
                    | undefined;

                if (typeof iframeWindow?.[func.name] === 'function') {
                    iframeWindow[func.name](...this.setArgs(func.args));
                }
            });

        this.dotIframeService
            .reloadedColors()
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                const html = this.getIframeDocument()?.querySelector('html');

                if (html) {
                    this.dotUiColorsService.setColors(html);
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
    onLoad($event: Event): void {
        // The template binds the DOM `load` event, whose `target` is `EventTarget | null`, and a
        // cross-origin or not-yet-ready frame has no `contentDocument`. `parseInt('')` is `NaN`,
        // which fails the `> 400` test below exactly as a missing title always did.
        const iframe = $event.target as HTMLIFrameElement | null;
        // JSP is setting the error number in the title
        const errorCode = parseInt(iframe?.contentDocument?.title ?? '', 10);
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

        const webSocketEvents$ = merge(
            ...events.map((eventType) =>
                this.dotEventsSocket
                    .on<unknown>(eventType)
                    .pipe(
                        map((data) => ({ data, name: eventType }) as DotEventTypeWrapper<unknown>)
                    )
            )
        ).pipe(takeUntil(this.destroy$));

        webSocketEvents$
            .pipe(filter(() => this.dotRouterService.currentPortlet.id === 'site-browser'))
            .subscribe((event) => {
                this.loggerService.debug('Capturing Site Browser event', event.name, event.data);
            });

        webSocketEvents$
            .pipe(
                filter(
                    (event) =>
                        (this.iframeElement.nativeElement.contentWindow &&
                            event.name === 'DELETE_BUNDLE') ||
                        event.name === 'PAGE_RELOAD' // Providing this event so backend devs can reload the jsp easily
                )
            )
            .subscribe(() => {
                this.iframeElement.nativeElement.contentWindow.postMessage('reload');
            });

        /**
         * The debouncetime is required because when the websocket event is received,
         * the list of plugins still cannot be updated, this is because the framework (OSGI)
         * needs to restart before the list can be refreshed.
         * Currently, an event cannot be emitted when the framework finishes restarting.
         */
        this.dotEventsSocket
            .on<void>('OSGI_BUNDLES_LOADED')
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
        const errorMapHandler: Record<number, () => void> = {
            401: () => {
                this.dotRouterService.doLogOut();
            }
        };

        if (errorMapHandler[error]) {
            errorMapHandler[error]();
        }
    }

    private handleIframeEvents($event: Event): void {
        // `'ng-event'` is a custom event name, so `addEventListener` resolves to its `Event`
        // overload and will not take a `CustomEvent` handler directly.
        //
        // NOTE: each `.bind(this)` below produces a fresh function, so neither
        // `removeEventListener` call has ever removed anything — every iframe load adds another
        // pair of listeners. Left alone here: changing listener identity changes what this
        // component emits, which is more than a strict-mode pass should do.
        this.getIframeWindow().removeEventListener('keydown', this.emitKeyDown.bind(this));
        this.getIframeWindow().document.removeEventListener(
            'ng-event',
            this.emitCustonEvent.bind(this) as EventListener
        );

        this.getIframeWindow().addEventListener('keydown', this.emitKeyDown.bind(this));
        this.getIframeWindow().document.addEventListener(
            'ng-event',
            this.emitCustonEvent.bind(this) as EventListener
        );
        this.charge.emit($event);

        const html = this.getIframeDocument()?.querySelector('html');

        if (html) {
            this.dotUiColorsService.setColors(html);
        }
    }

    private isIframeHaveContent(): boolean {
        return !!this.iframeElement?.nativeElement?.contentWindow?.document?.body?.innerHTML
            ?.length;
    }

    private setArgs(args?: unknown[]): unknown[] {
        return args ? args : [];
    }
}
