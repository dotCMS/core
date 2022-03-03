import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Editor } from '@tiptap/core';
import { bubbleMenuItems, bubbleMenuImageItems } from '@dotcms/block-editor';

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
export class BubbleMenuComponent implements OnInit, OnDestroy {
    @Input() editor: Editor;

    public enabledMarks: string[] = [];
    public textAlings: string[] = ['left', 'center', 'right'];
    public activeMarks: string[] = [];

    public items: BubbleMenuItem[] = [];

    ngOnInit() {
        this.setEnabledMarks();

        /**
         * Every time the editor is updated, the active state of the buttons must be updated.
         */
        this.editor.on('transaction', this.onUpdate.bind(this));

        /**
         * Every time the selection is updated, check if it's a dotImage
         */
        this.editor.on('selectionUpdate', this.setMenuItems.bind(this));
    }

    ngOnDestroy(): void {
        this.editor.off('transaction', this.onUpdate.bind(this));
        this.editor.off('selectionUpdate', this.setMenuItems.bind(this));
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

    private onUpdate(): void {
        this.setActiveMarks();
        this.updateActiveItems();
    }

    private setMenuItems({ editor: { state } }: { editor: Editor }): void {
        const { doc, selection } = state;
        const { empty } = selection;

        if (empty) {
            return;
        }

        const node = doc.nodeAt(selection.from);
        const isDotImage = node.type.name == 'dotImage';

        this.items = isDotImage ? bubbleMenuImageItems : bubbleMenuItems;
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
        this.enabledMarks = [
            ...Object.keys(this.editor.schema.marks),
            ...Object.keys(this.editor.schema.nodes)
        ];
    }

    private setActiveMarks(): void {
        this.activeMarks = [
            ...this.enabledMarks.filter((mark) => this.editor.isActive(mark)),
            ...this.textAlings.filter((alignment) => this.editor.isActive({ textAlign: alignment }))
        ];
    }
}
