import { Component, Input, Output, EventEmitter } from '@angular/core';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { DotMenu, DotMenuItem } from '@models/navigation';

@Component({
    animations: [
        trigger('expandAnimation', [
            state(
                'expanded',
                style({
                    height: '!',
                    overflow: 'hidden'
                })
            ),
            state(
                'collapsed',
                style({
                    height: '0px',
                    overflow: 'hidden'
                })
            ),
            transition('expanded <=> collapsed', animate('250ms ease-in-out'))
        ])
    ],
    selector: 'dot-sub-nav',
    templateUrl: './dot-sub-nav.component.html',
    styleUrls: ['./dot-sub-nav.component.scss']
})
export class DotSubNavComponent {
    @Input()
    data: DotMenu;
    @Input()
    contextmenu: boolean;
    @Output()
    itemClick: EventEmitter<{ originalEvent: MouseEvent; data: DotMenuItem }> = new EventEmitter();

    constructor() {}

    /**
     * Handle click event in a menu sub item
     *
     * @param MouseEvent $event
     * @param DotMenuItem item
     * @memberof DotSubNavComponent
     */
    onItemClick($event: MouseEvent, item: DotMenuItem): void {
        this.itemClick.emit({
            originalEvent: $event,
            data: item
        });
    }
}
