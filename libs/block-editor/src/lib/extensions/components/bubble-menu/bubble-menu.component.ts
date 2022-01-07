import { Component, Input, OnInit } from '@angular/core';
import { Editor } from '@tiptap/core';

export interface BubbleMenuItem {
    icon: string;
    markAction: string;
    active: boolean;
    divider?: boolean;
}

@Component({
    selector: 'dotcms-bubble-menu',
    templateUrl: './bubble-menu.component.html',
    styleUrls: ['./bubble-menu.component.scss']
})
export class BubbleMenuComponent implements OnInit {
    @Input() editor: Editor;

    public enabledMarks: string[] = [];
    public textAlings: string[] = ['left', 'center', 'right'];
    public activeMarks: string[] = [];

    public items: BubbleMenuItem[] = [
        {
            icon: 'format_bold',
            markAction: 'bold',
            active: false
        },
        {
            icon: 'format_underlined',
            markAction: 'underline',
            active: false
        },
        {
            icon: 'format_italic',
            markAction: 'italic',
            active: false
        },
        {
            icon: 'strikethrough_s',
            markAction: 'strike',
            active: false,
            divider: true
        },
        {
            icon: 'format_align_left',
            markAction: 'left',
            active: false
        },
        {
            icon: 'format_align_center',
            markAction: 'center',
            active: false
        },
        {
            icon: 'format_align_right',
            markAction: 'right',
            active: false,
            divider: true
        },
        {
            icon: 'format_list_bulleted',
            markAction: 'bulletList',
            active: false
        },
        {
            icon: 'format_list_numbered',
            markAction: 'orderedList',
            active: false
        },
        {
            icon: 'format_indent_decrease',
            markAction: 'outdent',
            active: false
        },
        {
            icon: 'format_indent_increase',
            markAction: 'indent',
            active: false,
            divider: true
        },
        {
            icon: 'link',
            markAction: 'link',
            active: false,
            divider: true
        },
        {
            icon: 'format_clear',
            markAction: 'clearAll',
            active: false
        }
    ];

    ngOnInit() {
        this.setEnabledMarks();

        /**
         * Every time the selection is updated, the active state of the buttons must be updated.
         */
        this.editor.on('transaction', () => {
            this.setActiveMarks();
            this.updateActiveItems();
        });
    }

    command(item: BubbleMenuItem): void {
        this.menuActions(item);
        this.setActiveMarks();
        this.updateActiveItems();
    }

    preventDeSelection(event: MouseEvent): void {
        event.preventDefault();
    }

    private menuActions(item: BubbleMenuItem): void {
        const markActions = {
            bold: () => {
                this.editor.commands.toggleBold();
            },
            italic: () => {
                this.editor.commands.toggleItalic();
            },
            strike: () => {
                this.editor.commands.toggleStrike();
            },
            underline: () => {
                this.editor.commands.toggleUnderline();
            },
            left: () => {
                this.toggleTextAlign('left', item.active);
            },
            center: () => {
                this.toggleTextAlign('center', item.active);
            },
            right: () => {
                this.toggleTextAlign('right', item.active);
            },
            bulletList: () => {
                this.editor.commands.toggleBulletList();
            },
            orderedList: () => {
                this.editor.commands.toggleOrderedList();
            },
            indent: () => {
                if (this.isListNode()) {
                    this.editor.commands.sinkListItem('listItem');
                }
            },
            outdent: () => {
                if (this.isListNode()) {
                    this.editor.commands.liftListItem('listItem');
                }
            },
            link: () => {
                this.editor.commands.toogleLinkForm();
            },
            clearAll: () => {
                this.editor.commands.unsetAllMarks();
                this.editor.commands.clearNodes();
            }
        };

        markActions[item.markAction] ? markActions[item.markAction]() : null;
    }

    private updateActiveItems(): void {
        this.items.forEach((item) => {
            if (this.activeMarks.includes(item.markAction)) {
                item.active = true;
            } else {
                item.active = false;
            }
        });
    }

    private toggleTextAlign(aligment: string, active: boolean): void {
        if (active) {
            this.editor.commands.unsetTextAlign();
        } else {
            this.editor.commands.setTextAlign(aligment);
        }
    }

    private isListNode(): boolean {
        return this.editor.isActive('bulletList') || this.editor.isActive('orderedList');
    }

    private setEnabledMarks(): void {
        this.enabledMarks = [...Object.keys(this.editor.schema.marks), ...Object.keys(this.editor.schema.nodes)];
    }

    private setActiveMarks(): void {
        this.activeMarks = [
            ...this.enabledMarks.filter((mark) => this.editor.isActive(mark)),
            ...this.textAlings.filter((alignment) => this.editor.isActive({ textAlign: alignment }))
        ];
    }
}