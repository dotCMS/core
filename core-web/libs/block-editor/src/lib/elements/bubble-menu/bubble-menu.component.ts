import { TiptapBubbleMenuDirective } from 'ngx-tiptap';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { DropdownModule } from 'primeng/dropdown';

import { Editor } from '@tiptap/core';

interface NodeTypeOption {
    name: string;
    value: string;
    command: () => boolean;
}

@Component({
    selector: 'dot-bubble-menu',
    templateUrl: './bubble-menu.component.html',
    styleUrls: ['./bubble-menu.component.scss'],
    imports: [CommonModule, TiptapBubbleMenuDirective, DropdownModule],
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class BubbleMenuComponent {
    protected readonly editor = input.required<Editor>();
    protected readonly nodeTypeOptions: NodeTypeOption[] = [
        {
            name: 'Paragraph',
            value: 'paragraph',
            command: () => this.editor().chain().focus().clearNodes().run()
        },
        {
            name: 'Heading 1',
            value: 'heading-1',
            command: () => this.editor().chain().focus().clearNodes().setHeading({ level: 1 }).run()
        },
        {
            name: 'Heading 2',
            value: 'heading-2',
            command: () => this.editor().chain().focus().clearNodes().setHeading({ level: 2 }).run()
        },
        {
            name: 'Heading 3',
            value: 'heading-3',
            command: () => this.editor().chain().focus().clearNodes().setHeading({ level: 3 }).run()
        },
        {
            name: 'Heading 4',
            value: 'heading-4',
            command: () => this.editor().chain().focus().clearNodes().setHeading({ level: 4 }).run()
        },
        {
            name: 'Heading 5',
            value: 'heading-5',
            command: () => this.editor().chain().focus().clearNodes().setHeading({ level: 5 }).run()
        },
        {
            name: 'Heading 6',
            value: 'heading-6',
            command: () => this.editor().chain().focus().clearNodes().setHeading({ level: 6 }).run()
        },
        {
            name: 'Ordered List',
            value: 'ordered-list',
            command: () => this.editor().chain().focus().clearNodes().toggleOrderedList?.().run()
        },
        {
            name: 'Bullet List',
            value: 'bullet-list',
            command: () => this.editor().chain().focus().clearNodes().toggleBulletList?.().run()
        },
        {
            name: 'Blockquote',
            value: 'blockquote',
            command: () => this.editor().chain().focus().clearNodes().toggleBlockquote?.().run()
        },
        {
            name: 'Code Block',
            value: 'code-block',
            command: () => this.editor().chain().focus().clearNodes().toggleCodeBlock?.().run()
        }
    ];

    changeToBlock(option: NodeTypeOption) {
        option.command();
    }
}
