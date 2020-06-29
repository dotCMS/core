import { Component, Output, EventEmitter, Input, HostBinding, ViewChild } from '@angular/core';
import { DotMenu, DotMenuItem } from '@models/navigation';
import { DotSubNavComponent } from '../dot-sub-nav/dot-sub-nav.component';

@Component({
    selector: 'dot-nav-item',
    templateUrl: './dot-nav-item.component.html',
    styleUrls: ['./dot-nav-item.component.scss']
})
export class DotNavItemComponent {
    @ViewChild('subnav') subnav: DotSubNavComponent;

    @Input() data: DotMenu;

    @Output()
    menuClick: EventEmitter<{ originalEvent: MouseEvent; data: DotMenu }> = new EventEmitter();

    @Output()
    itemClick: EventEmitter<{ originalEvent: MouseEvent; data: DotMenuItem }> = new EventEmitter();

    @HostBinding('class.dot-nav-item__collapsed')
    @Input()
    collapsed: boolean;

    customStyles = {};

    private windowHeight = window.innerHeight;

    constructor() {}

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
    }

    /**
     * Align the submenu top or bottom depending of the browser window
     *
     * @memberof DotNavItemComponent
     */
    setSubMenuPosition(): void {
        if (this.collapsed) {
            const [rects] = this.subnav.ul.nativeElement.getClientRects();

            if (window.innerHeight !== this.windowHeight) {
                this.customStyles = {};
                this.windowHeight = window.innerHeight;
            }

            if (rects.bottom > this.windowHeight) {
                this.customStyles = {
                    top: 'auto',
                    bottom: '0'
                };
            }
        }
    }

    /**
     * Handle click in dot-sub-nav items
     *
     * @param { originalEvent: MouseEvent; data: DotMenuItem } $event
     * @memberof DotNavItemComponent
     */
    handleItemClick(event: { originalEvent: MouseEvent; data: DotMenuItem }) {
        this.itemClick.emit(event);
    }
}
