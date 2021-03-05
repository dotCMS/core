import { Component, Input } from '@angular/core';

/**
 * The DotIconButtonComponent is a round button which
 * reuses the dot-icon component
 * @export
 * @class DotIconButtonComponent
 */
@Component({
    selector: 'dot-icon-button',
    styleUrls: ['./dot-icon-button.component.scss'],
    templateUrl: './dot-icon-button.component.html'
})
export class DotIconButtonComponent {
    @Input()
    disabled?: boolean;

    @Input()
    icon: string;

    @Input()
    size: number;

    constructor() {}
}
