import { Component, Output, EventEmitter, Input, HostBinding } from '@angular/core';
import { DotMenu, DotMenuItem } from '@models/navigation';

@Component({
    selector: 'dot-nav-item',
    templateUrl: './dot-nav-item.component.html',
    styleUrls: ['./dot-nav-item.component.scss']
})
export class DotNavItemComponent {
    @Input() data: DotMenu;

    @Output()
    menuClick: EventEmitter<{ originalEvent: MouseEvent; data: DotMenu }> = new EventEmitter();

    @Output()
    itemClick: EventEmitter<{ originalEvent: MouseEvent; data: DotMenuItem }> = new EventEmitter();

    @HostBinding('class.dot-nav-item__collapsed')
    @Input()
    collapsed: boolean;

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
     * Handle click in dot-sub-nav items
     *
     * @param { originalEvent: MouseEvent; data: DotMenuItem } $event
     * @memberof DotNavItemComponent
     */
    handleItemClick(event: { originalEvent: MouseEvent; data: DotMenuItem }) {
        this.itemClick.emit(event);
    }
}
