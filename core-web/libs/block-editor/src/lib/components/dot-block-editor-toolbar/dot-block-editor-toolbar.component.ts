import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { Editor } from '@tiptap/core';

import { DotSafeHtmlPipe } from '@dotcms/ui';

export interface DotEditorToolBarItem {
    key: string;
    icon: string;
    command: () => void;
}

interface DotDividerItem {
    divider: true;
}

@Component({
    selector: 'dot-block-editor-toolbar',
    standalone: true,
    imports: [CommonModule, ButtonModule, DotSafeHtmlPipe],
    templateUrl: './dot-block-editor-toolbar.component.html',
    styleUrl: './dot-block-editor-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBlockEditorToolbarComponent {
    @Input({ required: true }) editor: Editor;

    protected items: Array<DotEditorToolBarItem | DotDividerItem> = [
        {
            key: 'bold',
            icon: 'format_bold',
            command: () => {
                this.editor.chain().toggleBold().focus().run();
            }
        },
        {
            key: 'italic',
            icon: 'format_italic',
            command: () => {
                this.editor.chain().toggleItalic().focus().run();
            }
        },
        {
            key: 'strike',
            icon: 'strikethrough_s',
            command: () => {
                this.editor.chain().toggleStrike().focus().run();
            }
        },
        {
            key: 'code',
            icon: 'code',
            command: () => {
                this.editor.chain().toggleCode().focus().run();
            }
        },
        {
            key: 'highlight',
            icon: '<svg xmlns="http://www.w3.org/2000/svg" height="18" viewBox="0 -960 960 960" width="18" fill="white"><path d="M544-400 440-504 240-304l104 104 200-200Zm-47-161 104 104 199-199-104-104-199 199Zm-84-28 216 216-229 229q-24 24-56 24t-56-24l-2-2-26 26H60l126-126-2-2q-24-24-24-56t24-56l229-229Zm0 0 227-227q24-24 56-24t56 24l104 104q24 24 24 56t-24 56L629-373 413-589Z"/></svg>',
            command: () => {
                this.editor.chain().toggleHighlight({ color: '#ffcc00' }).focus().run();
            }
        },
        {
            divider: true
        },
        {
            key: 'h1',
            icon: '<svg xmlns="http://www.w3.org/2000/svg" height="20" viewBox="0 -960 960 960" width="20" fill="white"><path d="M200-280v-400h80v160h160v-160h80v400h-80v-160H280v160h-80Zm480 0v-320h-80v-80h160v400h-80Z"/></svg>',
            command: () => {
                this.editor.chain().setHeading({ level: 1 }).focus().run();
            }
        },
        {
            key: 'h2',
            icon: '<svg xmlns="http://www.w3.org/2000/svg" height="20" viewBox="0 -960 960 960" width="20" fill="white"><path d="M120-280v-400h80v160h160v-160h80v400h-80v-160H200v160h-80Zm400 0v-160q0-33 23.5-56.5T600-520h160v-80H520v-80h240q33 0 56.5 23.5T840-600v80q0 33-23.5 56.5T760-440H600v80h240v80H520Z"/></svg>',
            command: () => {
                this.editor.chain().setHeading({ level: 2 }).focus().run();
            }
        },
        {
            key: 'paragraph',
            icon: '<svg xmlns="http://www.w3.org/2000/svg" height="20" viewBox="0 -960 960 960" width="20" fill="white"><path d="M360-160v-240q-83 0-141.5-58.5T160-600q0-83 58.5-141.5T360-800h360v80h-80v560h-80v-560H440v560h-80Z"/></svg>',
            command: () => {
                this.editor.chain().setParagraph().focus().run();
            }
        },
        {
            key: 'Bullet List',
            icon: 'format_list_bulleted',
            command: () => {
                this.editor.chain().toggleBulletList().focus().run();
            }
        },
        {
            key: 'Bullet List',
            icon: 'format_list_numbered',
            command: () => {
                this.editor.chain().toggleOrderedList().focus().run();
            }
        },
        {
            key: 'Code Block',
            icon: '<svg xmlns="http://www.w3.org/2000/svg" height="24" viewBox="0 -960 960 960" width="24" fill="white"><path d="m384-336 56-57-87-87 87-87-56-57-144 144 144 144Zm192 0 144-144-144-144-56 57 87 87-87 87 56 57ZM200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-80h560v-560H200v560Zm0-560v560-560Z"/></svg>',
            command: () => {
                this.editor.chain().toggleCodeBlock().focus().run();
            }
        },
        {
            divider: true
        },
        {
            key: 'fortmat clear',
            icon: 'format_clear',
            command: () => {
                this.editor.chain().unsetAllMarks().clearNodes().focus().run();
            }
        }
    ];
}
