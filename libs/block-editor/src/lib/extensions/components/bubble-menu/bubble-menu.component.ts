import { Component, EventEmitter, Input, Output } from '@angular/core';
import { BubbleMenuItem } from '@dotcms/block-editor';

@Component({
    selector: 'dotcms-bubble-menu',
    templateUrl: './bubble-menu.component.html',
    styleUrls: ['./bubble-menu.component.scss']
})
export class BubbleMenuComponent {
    @Input() items: BubbleMenuItem[] = [];
    @Output() command: EventEmitter<BubbleMenuItem> = new EventEmitter();

    preventDeSelection(event: MouseEvent): void {
        event.preventDefault();
    }
}
