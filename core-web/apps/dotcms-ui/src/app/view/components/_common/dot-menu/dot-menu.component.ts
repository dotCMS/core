import { fromEvent as observableFromEvent } from 'rxjs';

import { take, skip } from 'rxjs/operators';
import { Component, Input, ViewChild } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { Menu } from 'primeng/menu';

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
export class DotMenuComponent {
    @Input()
    icon: string;

    @Input()
    model: MenuItem[];

    @Input()
    float: boolean;

    @ViewChild('menu', { static: true })
    menu: Menu;

    /**
     * Toogle the visibility of the menu options & track
     * a document click when is open to eventually hide the menu
     *
     * @param {MouseEvent} $event
     *
     * @memberof DotMenuComponent
     */
    toggle($event: MouseEvent): void {
        $event.stopPropagation();
        this.menu.toggle($event);

        if (this.menu.visible) {
            // Skip 1 because the event bubbling capture the document.click
            observableFromEvent(document, 'click')
                .pipe(skip(1), take(1))
                .subscribe(() => {
                    this.menu.hide();
                });
        }
    }
}
