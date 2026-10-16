import { animate, state, style, transition, trigger } from '@angular/animations';
import {
    Component,
    ElementRef,
    EventEmitter,
    HostBinding,
    inject,
    Input,
    Output,
    ViewChild,
    ChangeDetectionStrategy
} from '@angular/core';
import { RouterModule } from '@angular/router';

import { DotMenu, DotMenuItem, getMenuItemKey } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

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
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [RouterModule]
})
export class DotSubNavComponent {
    readonly #globalStore = inject(GlobalStore);

    @ViewChild('ul', { static: true }) ul: ElementRef;

    @Input() data: DotMenu;

    @Output()
    itemClick: EventEmitter<{ originalEvent: MouseEvent; data: DotMenuItem }> = new EventEmitter();

    @Input() collapsed: boolean;

    @HostBinding('@expandAnimation') get getAnimation(): string {
        return !this.collapsed && this.data.isOpen ? 'expanded' : 'collapsed';
    }

    /**
     * Whether a menu item is the highlighted one. The highlight is the store's `activeKey`, not a
     * flag on the item, so replacing the menu does not erase it.
     *
     * @param item the menu item to check
     * @returns true when the item is the highlighted one
     */
    isActive(item: DotMenuItem): boolean {
        return getMenuItemKey(item) === this.#globalStore.activeKey();
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
