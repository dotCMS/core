import { Component, Output, EventEmitter, Input, HostListener, HostBinding } from '@angular/core';
import { DotMenu, DotMenuItem } from '@models/navigation';

@Component({
    selector: 'dot-nav-item',
    templateUrl: './dot-nav-item.component.html',
    styleUrls: ['./dot-nav-item.component.scss']
})
export class DotNavItemComponent {
    @Input() data: DotMenu;
    @Output()
    menuRightClick: EventEmitter<{ originalEvent: MouseEvent; data: DotMenu }> = new EventEmitter();
    @Output()
    menuClick: EventEmitter<{ originalEvent: MouseEvent; data: DotMenu }> = new EventEmitter();
    @Output()
    itemClick: EventEmitter<{ originalEvent: MouseEvent; data: DotMenuItem }> = new EventEmitter();
    @HostBinding('class.collapsed')
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
     * Handle right-click on menu section title
     *
     * @param MouseEvent $event
     * @memberof DotNavItemComponent
     */
    @HostListener('contextmenu', ['$event'])
    showSubMenuPanel(event: MouseEvent) {
        if (this.collapsed) {
            event.preventDefault();
            this.menuRightClick.emit({
                originalEvent: event,
                data: this.data
            });
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
