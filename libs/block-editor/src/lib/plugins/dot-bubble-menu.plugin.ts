import { EditorView } from 'prosemirror-view';
import { isNodeSelection, posToDOMRect } from '@tiptap/core';
import { PluginKey, Plugin, EditorState } from 'prosemirror-state';
import { BubbleMenuView } from '@tiptap/extension-bubble-menu';

// Utils
import { getNodePosition } from '@dotcms/block-editor';
import { ComponentRef } from '@angular/core';
import { bubbleMenuItems, bubbleMenuImageItems, isListNode } from '../utils/bubble-menu.utils';

// Model
import {
    BubbleMenuItem,
    BubbleMenuComponentProps,
    DotBubbleMenuPluginProps,
    DotBubbleMenuViewProps
} from '@dotcms/block-editor';

export const DotBubbleMenuPlugin = (options: DotBubbleMenuPluginProps) => {
    return new Plugin({
        key:
            typeof options.pluginKey === 'string'
                ? new PluginKey(options.pluginKey)
                : options.pluginKey,
        view: (view) => new DotBubbleMenuPluginView({ view, ...options })
    });
};

export class DotBubbleMenuPluginView extends BubbleMenuView {
    public component: ComponentRef<BubbleMenuComponentProps>;

    /* @Overrrider */
    constructor(props: DotBubbleMenuViewProps) {
        // Inherit the parent class
        super(props);

        // New Properties
        this.component = props.component;
        this.component.instance.command.subscribe(this.exeCommand.bind(this));
    }

    /* @Overrrider */
    update(view: EditorView, oldState?: EditorState) {
        const { state, composing } = view;
        const { doc, selection } = state;
        const isSame = oldState && oldState.doc.eq(doc) && oldState.selection.eq(selection);

        if (composing || isSame) {
            return;
        }

        this.createTooltip();

        // support for CellSelections
        const { ranges } = selection;
        const from = Math.min(...ranges.map((range) => range.$from.pos));
        const to = Math.max(...ranges.map((range) => range.$to.pos));

        const shouldShow = this.shouldShow?.({
            editor: this.editor,
            view,
            state,
            oldState,
            from,
            to
        });

        if (!shouldShow) {
            this.hide();

            return;
        }

        this.tippy?.setProps({
            getReferenceClientRect: () => {
                if (isNodeSelection(selection)) {
                    const node = view.nodeDOM(from) as HTMLElement;

                    if (node) {
                        const type = doc.nodeAt(from).type.name;
                        return getNodePosition(node, type);
                    }
                }

                return posToDOMRect(view, from, to);
            }
        });
        this.setMenuItems(doc, from);
        this.updateComponent();
        this.show();
    }

    /* @Overrrider */
    destroy() {
        this.tippy?.destroy();
        this.element.removeEventListener('mousedown', this.mousedownHandler, { capture: true });
        this.view.dom.removeEventListener('dragstart', this.dragstartHandler);
        this.editor.off('focus', this.focusHandler);
        this.editor.off('blur', this.blurHandler);
        this.component.instance.command.unsubscribe();
    }

    /* Update Component */
    updateComponent() {
        const { items } = this.component.instance;
        const aligment: string[] = ['left', 'center', 'right'];
        const activeMarks = this.setActiveMarks(aligment);
        this.component.instance.items = this.updateActiveItems(items, activeMarks);
        this.component.changeDetectorRef.detectChanges();
    }

    updateActiveItems = (items: BubbleMenuItem[] = [], activeMarks: string[]): BubbleMenuItem[] => {
        return items.map((item) => {
            item.active = activeMarks.includes(item.markAction);
            return item;
        });
    };

    enabledMarks = (): string[] => {
        return [...Object.keys(this.editor.schema.marks), ...Object.keys(this.editor.schema.nodes)];
    };

    setActiveMarks = (aligment = []): string[] => {
        return [
            ...this.enabledMarks().filter((mark) => this.editor.isActive(mark)),
            ...aligment.filter((alignment) => this.editor.isActive({ textAlign: alignment }))
        ];
    };

    setMenuItems(doc, from) {
        const node = doc.nodeAt(from);
        const isDotImage = node.type.name == 'dotImage';

        this.component.instance.items = isDotImage ? bubbleMenuImageItems : bubbleMenuItems;
    }

    /* Run commands */
    exeCommand(item: BubbleMenuItem) {
        const { markAction: action, active } = item;
        switch (action) {
            case 'bold':
                this.editor.commands.toggleBold();
                break;
            case 'italic':
                this.editor.commands.toggleItalic();
                break;
            case 'strike':
                this.editor.commands.toggleStrike();
                break;
            case 'underline':
                this.editor.commands.toggleUnderline();
                break;
            case 'left':
                this.toggleTextAlign(action, active);
                break;
            case 'center':
                this.toggleTextAlign(action, active);
                break;
            case 'right':
                this.toggleTextAlign(action, active);
                break;
            case 'bulletList':
                this.editor.commands.toggleBulletList();
                break;
            case 'orderedList':
                this.editor.commands.toggleOrderedList();
                break;
            case 'indent':
                if (isListNode(this.editor)) {
                    this.editor.commands.sinkListItem('listItem');
                }
                break;
            case 'outdent':
                if (isListNode(this.editor)) {
                    this.editor.commands.liftListItem('listItem');
                }
                break;
            case 'link':
                this.editor.commands.toogleLinkForm();
                break;
            case 'clearAll':
                this.editor.commands.unsetAllMarks();
                this.editor.commands.clearNodes();
                break;
        }
    }

    toggleTextAlign(alignment, active) {
        if (active) {
            this.editor.commands.unsetTextAlign();
        } else {
            this.editor.commands.setTextAlign(alignment);
        }
    }
}
