import {
    Component,
    Output,
    EventEmitter,
    Input,
    HostListener,
    OnInit,
    HostBinding,
    OnDestroy
} from '@angular/core';
import { DotMenu, DotMenuItem } from '@models/navigation';
import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { merge, Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { DotEventsService } from '@services/dot-events/dot-events.service';

@Component({
    selector: 'dot-nav-item',
    templateUrl: './dot-nav-item.component.html',
    styleUrls: ['./dot-nav-item.component.scss']
})
export class DotNavItemComponent implements OnInit, OnDestroy {
    @Input() data: DotMenu;
    @Output()
    menuClick: EventEmitter<{ originalEvent: MouseEvent; data: DotMenu }> = new EventEmitter();
    @Output()
    itemClick: EventEmitter<{ originalEvent: MouseEvent; data: DotMenuItem }> = new EventEmitter();
    @HostBinding('class.collapsed')
    @Input()
    collapsed: boolean;
    @HostBinding('class.contextmenu') contextmenu = false;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        public iframeOverlayService: IframeOverlayService,
        private dotEventsService: DotEventsService
    ) {}

    ngOnInit() {
        this.setHideFlyOutSubscription();
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Handle click on menu section title
     *
     * @param MouseEvent $event
     * @param DotMenu data
     * @memberof DotNavItemComponent
     */
    clickHandler($event: MouseEvent, data: DotMenu): void {
        this.menuClick.emit({
            originalEvent: $event,
            data: data
        });
        this.dotEventsService.notify('hide-sub-nav-fly-out');
    }

    /**
     * Handle right-click on menu section title
     *
     * @param MouseEvent $event
     * @memberof DotNavItemComponent
     */
    @HostListener('contextmenu', ['$event'])
    showSubMenuPanel(event: MouseEvent) {
        if (this.collapsed) {
            event.preventDefault();
            this.dotEventsService.notify('hide-sub-nav-fly-out');
            this.iframeOverlayService.show();
            this.contextmenu = true;
        }
    }

    /**
     * Handle click on document to hide the fly-out menu
     *
     * @memberof DotNavItemComponent
     */
    @HostListener('document:click')
    handleDocumentClick(): void {
        this.contextmenu = false;
    }

    /**
     * Handle click in dot-sub-nav items
     *
     * @param { originalEvent: MouseEvent; data: DotMenuItem } $event
     * @memberof DotNavItemComponent
     */
    handleItemClick(event: { originalEvent: MouseEvent; data: DotMenuItem }) {
        this.itemClick.emit(event);
        this.dotEventsService.notify('hide-sub-nav-fly-out');
    }

    private setHideFlyOutSubscription(): void {
        const hideFlyOut$ = merge(
            this.iframeOverlayService.overlay.pipe(filter((val: boolean) => !val)),
            this.dotEventsService.listen('hide-sub-nav-fly-out')
        ).pipe(takeUntil(this.destroy$), filter(() => this.contextmenu));

        hideFlyOut$.subscribe(() => {
            this.contextmenu = false;
            this.iframeOverlayService.hide();
        });
    }
}
