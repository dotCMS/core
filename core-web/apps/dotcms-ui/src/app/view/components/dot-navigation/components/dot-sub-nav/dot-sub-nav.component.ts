import { animate, state, style, transition, trigger } from '@angular/animations';
import {
    Component,
    ElementRef,
    EventEmitter,
    HostBinding,
    Output,
    ViewChild,
    ChangeDetectionStrategy,
    input
} from '@angular/core';
import { RouterModule } from '@angular/router';

import { DotMenuItem, MenuGroup } from '@dotcms/dotcms-models';

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
    @ViewChild('ul', { static: true }) ul!: ElementRef;

    readonly data = input.required<MenuGroup>();

    @Output()
    itemClick: EventEmitter<{ originalEvent: MouseEvent; data: DotMenuItem }> = new EventEmitter();

    readonly collapsed = input.required<boolean>();

    @HostBinding('@expandAnimation') get getAnimation(): string {
        return !this.collapsed() && this.data().isOpen ? 'expanded' : 'collapsed';
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
