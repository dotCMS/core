import { Directive, HostBinding } from '@angular/core';

@Directive({
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: '[tiptapNodeViewContent]'
})
export class NodeViewContentDirective {
    @HostBinding('attr.data-node-view-content') handle = '';
}
