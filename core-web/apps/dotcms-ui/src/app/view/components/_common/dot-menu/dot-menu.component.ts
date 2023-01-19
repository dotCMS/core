/* eslint-disable no-debugger */
/* eslint-disable no-console */
import { fromEvent as observableFromEvent } from 'rxjs';

import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    ViewChild
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { Menu } from 'primeng/menu';

import { take } from 'rxjs/operators';

/**
 * Custom Menu to display options as a pop-up.
 *
 * @export
 * @class DotMenuComponent
 */
@Component({
    selector: 'dot-menu',
    templateUrl: './dot-menu.component.html',
    styleUrls: ['./dot-menu.component.scss']
})
export class DotMenuComponent implements OnChanges {
    @Input()
    icon: string;

    @Input()
    model: MenuItem[];

    @Input()
    float: boolean;

    @Input()
    lazy = false;

    @Output() menuVisible: EventEmitter<boolean> = new EventEmitter();

    @ViewChild('menu', { static: true })
    menu: Menu;

    private _mouseEvent: MouseEvent;

    ngOnChanges(changes: SimpleChanges): void {
        debugger;
        if (changes.model.currentValue.length && this.lazy) {
            this.toggleMenu(this._mouseEvent);
        }
    }

    /**
     * Toogle the visibility of the menu options & track
     * a document click when is open to eventually hide the menu
     *
     * @param {MouseEvent} $event
     *
     * @memberof DotMenuComponent
     */
    toggle(event: MouseEvent): void {
        this._mouseEvent = event;
        debugger;
        event.stopPropagation();
        console.log('**$event', event, this.lazy);
        console.log('==this._mouseEvent', this._mouseEvent);

        if (!this.menu.visible) {
            this.menuVisible.emit(true);
        } else {
            this.menuVisible.emit(false);
        }

        if (!this.lazy || this.menu.visible) {
            this.toggleMenu(this._mouseEvent);
        }
    }

    private toggleMenu(event: MouseEvent) {
        console.log('**$toggleMenu', event, this.lazy);
        this.menu.toggle(event);

        if (this.menu.visible) {
            // Skip 1 because the event bubbling capture the document.click
            observableFromEvent(document, 'click')
                .pipe(
                    // skip(1), // not needed anymore
                    take(1)
                )
                .subscribe(() => {
                    this.menu.hide();
                });
        }
    }
}
