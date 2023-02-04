import { Component, Input } from '@angular/core';

// Interface
import { BubbleMenuItem } from '../../models';

@Component({
    selector: 'dot-bubble-menu-button',
    templateUrl: './bubble-menu-button.component.html',
    styleUrls: ['./bubble-menu-button.component.scss']
})
export class BubbleMenuButtonComponent {
    @Input() item: BubbleMenuItem;
    @Input() active = false;
}
