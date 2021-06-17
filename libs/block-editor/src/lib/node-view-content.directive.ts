import { Directive, HostBinding } from '@angular/core';

@Directive({
  selector: '[tiptapNodeViewContent]'
})
export class NodeViewContentDirective {
  @HostBinding('attr.data-node-view-content') handle = ''
}
