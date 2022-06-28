import { Component, Input, EventEmitter, Output } from '@angular/core';

@Component({
    selector: 'dot-reorder-menu',
    templateUrl: './dot-reorder-menu.component.html'
})
export class DotReorderMenuComponent {
    @Input() url: string;
    @Output() shutdown: EventEmitter<unknown> = new EventEmitter();

    /**
     * Handle close event from the iframe
     *
     * @memberof DotContentletWrapperComponent
     */
    onClose(): void {
        this.shutdown.emit();
    }
}
