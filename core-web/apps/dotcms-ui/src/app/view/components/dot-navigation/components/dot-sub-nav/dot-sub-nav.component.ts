import { animate, state, style, transition, trigger } from '@angular/animations';
import {
    Component,
    ElementRef,
    EventEmitter,
    HostBinding,
    Input,
    Output,
    ViewChild
} from '@angular/core';
import { RouterModule } from '@angular/router';

import { DotMenu, DotMenuItem } from '@dotcms/dotcms-models';

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
    styleUrls: ['./dot-sub-nav.component.scss'],
    imports: [RouterModule]
})
export class DotSubNavComponent {
    @ViewChild('ul', { static: true }) ul: ElementRef;

    @Input() data: DotMenu;

    @Output()
    itemClick: EventEmitter<{ originalEvent: MouseEvent; data: DotMenuItem }> = new EventEmitter();

    @Input() collapsed: boolean;

    @HostBinding('@expandAnimation') get getAnimation(): string {
        return !this.collapsed && this.data.isOpen ? 'expanded' : 'collapsed';
    }

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
