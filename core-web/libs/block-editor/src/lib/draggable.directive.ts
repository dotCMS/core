import { Directive, HostBinding } from '@angular/core';

@Directive({
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: '[tiptapDraggable]'
})
export class DraggableDirective {
    @HostBinding('attr.draggable') draggable = true;
    @HostBinding('attr.data-drag-handle') handle = '';
}
