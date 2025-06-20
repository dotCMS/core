import { TiptapBubbleMenuDirective } from 'ngx-tiptap';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { Editor } from '@tiptap/core';

@Component({
    selector: 'dot-bubble-menu',
    templateUrl: './bubble-menu.component.html',
    styleUrls: ['./bubble-menu.component.scss'],
    imports: [CommonModule, TiptapBubbleMenuDirective],
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class BubbleMenuComponent {
    editor = input.required<Editor>();
}
