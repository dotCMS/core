import { Component, Input, EventEmitter, Output } from '@angular/core';

@Component({
    selector: 'dot-reorder-menu',
    templateUrl: './dot-reorder-menu.component.html'
})
export class DotReorderMenuComponent {
    @Input() url: string;
    @Output() close: EventEmitter<any> = new EventEmitter();

    constructor() {}

    /**
     * Handle close event from the iframe
     *
     * @memberof DotContentletWrapperComponent
     */
    onClose(): void {
        this.close.emit();
    }
}
