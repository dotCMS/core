import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnInit,
    inject
} from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { Editor } from '@tiptap/core';
import Blockquote from '@tiptap/extension-blockquote';
import Bold from '@tiptap/extension-bold';
import BulletList from '@tiptap/extension-bullet-list';
import Code from '@tiptap/extension-code';
import CodeBlock from '@tiptap/extension-code-block';
import HardBreak from '@tiptap/extension-hard-break';
import Heading from '@tiptap/extension-heading';
import Highlight from '@tiptap/extension-highlight';
import HorizontalRule from '@tiptap/extension-horizontal-rule';
import Italic from '@tiptap/extension-italic';
import OrderedList from '@tiptap/extension-ordered-list';
import Paragraph from '@tiptap/extension-paragraph';
import Strike from '@tiptap/extension-strike';

import { DotSafeHtmlPipe } from '@dotcms/ui';

export interface DotEditorToolBarItem {
    key: string;
    icon: string;
    command: () => void;
    attr?: Record<string, string>;
}

@Component({
    selector: 'dot-block-editor-toolbar',
    standalone: true,
    imports: [CommonModule, ButtonModule, DotSafeHtmlPipe],
    templateUrl: './dot-block-editor-toolbar.component.html',
    styleUrl: './dot-block-editor-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBlockEditorToolbarComponent implements OnInit {
    @Input({ required: true }) editor: Editor;

    private readonly textMarks = [
        {
            key: Bold.name,
            icon: 'format_bold',
            command: () => {
                this.editor.chain().toggleBold().focus().run();
            }
        },
        {
            key: Italic.name,
            icon: 'format_italic',
            command: () => {
                this.editor.chain().toggleItalic().focus().run();
            }
        },
        {
            key: Strike.name,
            icon: 'strikethrough_s',
            command: () => {
                this.editor.chain().toggleStrike().focus().run();
            }
        },
        {
            key: Code.name,
            icon: 'code',
            command: () => {
                this.editor.chain().toggleCode().focus().run();
            }
        },
        {
            key: Highlight.name,
            icon: '<svg xmlns="http://www.w3.org/2000/svg" height="18" viewBox="0 -960 960 960" width="18" fill="white"><path d="M544-400 440-504 240-304l104 104 200-200Zm-47-161 104 104 199-199-104-104-199 199Zm-84-28 216 216-229 229q-24 24-56 24t-56-24l-2-2-26 26H60l126-126-2-2q-24-24-24-56t24-56l229-229Zm0 0 227-227q24-24 56-24t56 24l104 104q24 24 24 56t-24 56L629-373 413-589Z"/></svg>',
            command: () => {
                this.editor.chain().toggleHighlight({ color: '#ffcc00' }).focus().run();
            }
        }
    ];

    private readonly nodeMarks = [
        {
            key: `${Heading.name}1`,
            icon: '<svg xmlns="http://www.w3.org/2000/svg" height="20" viewBox="0 -960 960 960" width="20" fill="white"><path d="M200-280v-400h80v160h160v-160h80v400h-80v-160H280v160h-80Zm480 0v-320h-80v-80h160v400h-80Z"/></svg>',
            attr: {
                level: 1
            },
            command: () => {
                this.editor.chain().setHeading({ level: 1 }).focus().run();
            }
        },
        {
            key: `${Heading.name}2`,
            icon: '<svg xmlns="http://www.w3.org/2000/svg" height="20" viewBox="0 -960 960 960" width="20" fill="white"><path d="M120-280v-400h80v160h160v-160h80v400h-80v-160H200v160h-80Zm400 0v-160q0-33 23.5-56.5T600-520h160v-80H520v-80h240q33 0 56.5 23.5T840-600v80q0 33-23.5 56.5T760-440H600v80h240v80H520Z"/></svg>',
            attr: {
                level: 2
            },
            command: () => {
                this.editor.chain().setHeading({ level: 2 }).focus().run();
            }
        },
        {
            key: Paragraph.name,
            icon: '<svg xmlns="http://www.w3.org/2000/svg" height="20" viewBox="0 -960 960 960" width="20" fill="white"><path d="M360-160v-240q-83 0-141.5-58.5T160-600q0-83 58.5-141.5T360-800h360v80h-80v560h-80v-560H440v560h-80Z"/></svg>',
            command: () => {
                this.editor.chain().setParagraph().focus().run();
            }
        },
        {
            key: BulletList.name,
            icon: 'format_list_bulleted',
            command: () => {
                this.editor.chain().toggleBulletList().focus().run();
            }
        },
        {
            key: OrderedList.name,
            icon: 'format_list_numbered',
            command: () => {
                this.editor.chain().toggleOrderedList().focus().run();
            }
        },
        {
            key: CodeBlock.name,
            icon: '<svg xmlns="http://www.w3.org/2000/svg" height="20" viewBox="0 -960 960 960" width="20" fill="white"><path d="m384-336 56-57-87-87 87-87-56-57-144 144 144 144Zm192 0 144-144-144-144-56 57 87 87-87 87 56 57ZM200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-80h560v-560H200v560Zm0-560v560-560Z"/></svg>',
            command: () => {
                this.editor.chain().toggleCodeBlock().focus().run();
            }
        }
    ];

    private readonly styleMarks = [
        {
            key: HardBreak.name,
            icon: '<svg xmlns="http://www.w3.org/2000/svg" height="20" viewBox="0 -960 960 960" width="20" fill="white"><path d="M588-132 440-280l148-148 56 58-50 50h96q29 0 49.5-20.5T760-390q0-29-20.5-49.5T690-460H160v-80h530q63 0 106.5 43.5T840-390q0 63-43.5 106.5T690-240h-96l50 50-56 58ZM160-240v-80h200v80H160Zm0-440v-80h640v80H160Z"/></svg>',
            command: () => {
                this.editor.chain().setHardBreak().focus().run();
            }
        },
        {
            key: 'fortmat clear',
            icon: '<svg xmlns="http://www.w3.org/2000/svg" height="20" viewBox="0 -960 960 960" width="20" fill="white"><path d="m528-546-93-93-121-121h486v120H568l-40 94ZM792-56 460-388l-80 188H249l119-280L56-792l56-56 736 736-56 56Z"/></svg>',
            command: () => {
                this.editor.chain().unsetAllMarks().clearNodes().focus().run();
            }
        }
    ];

    private readonly styleNodes = [
        {
            key: Blockquote.name,
            icon: '<svg xmlns="http://www.w3.org/2000/svg"  height="20" viewBox="0 0 24 24" width="20" fill="white"><path d="M4.583 17.321C3.553 16.227 3 15 3 13.011c0-3.5 2.457-6.637 6.03-8.188l.893 1.378c-3.335 1.804-3.987 4.145-4.247 5.621.537-.278 1.24-.375 1.929-.311 1.804.167 3.226 1.648 3.226 3.489a3.5 3.5 0 0 1-3.5 3.5c-1.073 0-2.099-.49-2.748-1.179zm10 0C13.553 16.227 13 15 13 13.011c0-3.5 2.457-6.637 6.03-8.188l.893 1.378c-3.335 1.804-3.987 4.145-4.247 5.621.537-.278 1.24-.375 1.929-.311 1.804.167 3.226 1.648 3.226 3.489a3.5 3.5 0 0 1-3.5 3.5c-1.073 0-2.099-.49-2.748-1.179z"></path></svg>',
            command: () => {
                this.editor.chain().toggleBlockquote().focus().run();
            }
        },
        {
            key: HorizontalRule.name,
            icon: 'horizontal_rule',
            command: () => {
                this.editor.chain().setHorizontalRule().focus().run();
            }
        }
    ];

    private readonly historyActions = [
        {
            key: 'undo',
            icon: '<svg xmlns="http://www.w3.org/2000/svg" height="20" viewBox="0 -960 960 960" width="20" fill="white"><path d="M280-200v-80h284q63 0 109.5-40T720-420q0-60-46.5-100T564-560H312l104 104-56 56-200-200 200-200 56 56-104 104h252q97 0 166.5 63T800-420q0 94-69.5 157T564-200H280Z"/></svg>',
            command: () => {
                this.editor.chain().undo().focus().run();
            }
        },
        {
            key: 'redo',
            icon: '<svg xmlns="http://www.w3.org/2000/svg" height="20" viewBox="0 -960 960 960" width="20" fill="white"><path d="M396-200q-97 0-166.5-63T160-420q0-94 69.5-157T396-640h252L544-744l56-56 200 200-200 200-56-56 104-104H396q-63 0-109.5 40T240-420q0 60 46.5 100T396-280h284v80H396Z"/></svg>',
            command: () => {
                this.editor.chain().redo().focus().run();
            }
        }
    ];

    protected groups: Array<DotEditorToolBarItem[]> = [
        this.textMarks,
        this.nodeMarks,
        this.styleMarks,
        this.styleNodes,
        this.historyActions
    ];

    protected activeMarks: Record<string, boolean> = {};

    private readonly cd: ChangeDetectorRef = inject(ChangeDetectorRef);

    ngOnInit(): void {
        const marks = this.groups.reduce((acc, group) => {
            return [...acc, ...group];
        }, []);

        this.editor.on('transaction', () => {
            marks.forEach(({ key, attr }) => {
                const markKey = key.includes(Heading.name) ? Heading.name : key;
                this.activeMarks[markKey] = this.editor?.isActive(markKey, attr);
            });
            this.cd.detectChanges();
        });
    }
}
