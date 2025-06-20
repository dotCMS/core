import { TiptapBubbleMenuDirective } from 'ngx-tiptap';

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { Editor } from '@tiptap/core';

@Component({
    selector: 'dot-bubble-menu',
    templateUrl: './bubble-menu.component.html',
    styleUrls: ['./bubble-menu.component.scss'],
    imports: [TiptapBubbleMenuDirective],
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class BubbleMenuComponent {
    @Input() editor: Editor;
}
