import { EditorView } from 'prosemirror-view';
import { isNodeSelection, posToDOMRect } from '@tiptap/core';
import { PluginKey, Plugin, EditorState } from 'prosemirror-state';
import { BubbleMenuView } from '@tiptap/extension-bubble-menu';
import tippy, { Instance } from 'tippy.js';

// Utils
import { getNodePosition, suggestionOptions } from '@dotcms/block-editor';
import { ComponentRef } from '@angular/core';
import { SuggestionsComponent } from '../extensions/components/suggestions/suggestions.component';
import {
    bubbleMenuItems,
    bubbleMenuImageItems,
    isListNode,
    popperModifiers
} from '../utils/bubble-menu.utils';

// Model
import {
    BubbleMenuItem,
    BubbleMenuComponentProps,
    DotBubbleMenuPluginProps,
    DotBubbleMenuViewProps
} from '@dotcms/block-editor';

export const DotBubbleMenuPlugin = (options: DotBubbleMenuPluginProps) => {
    const component = options.component.instance;
    const changeTo = options.changeToComponent.instance;

    return new Plugin({
        key:
            typeof options.pluginKey === 'string'
                ? new PluginKey(options.pluginKey)
                : options.pluginKey,
        view: (view) => new DotBubbleMenuPluginView({ view, ...options }),
        props: {
            /**
             * Catch and handle the keydown in the plugin
             *
             * @param {EditorView} view
             * @param {KeyboardEvent} event
             * @return {*}
             */
            handleKeyDown(view: EditorView, event: KeyboardEvent) {
                const { key } = event;
                if (changeTo.isOpen) {
                    if (key === 'Escape') {
                        component.toggleChangeTo.emit();
                        return true;
                    }
                    if (key === 'Enter') {
                        changeTo.execCommand();
                        return true;
                    }
                    if (key === 'ArrowDown' || key === 'ArrowUp') {
                        changeTo.updateSelection(event);
                        return true;
                    }
                }
                return false;
            }
        }
    });
};

export class DotBubbleMenuPluginView extends BubbleMenuView {
    public component: ComponentRef<BubbleMenuComponentProps>;
    public changeTo: ComponentRef<SuggestionsComponent>;
    public changeToElement: HTMLElement;

    public tippyChangeTo: Instance | undefined;

    /* @Overrrider */
    constructor(props: DotBubbleMenuViewProps) {
        // Inherit the parent class
        super(props);

        const { component, changeToComponent } = props;

        // New Properties
        this.component = component;
        this.changeTo = changeToComponent;
        this.changeToElement = this.changeTo.location.nativeElement;

        // Subscriptions
        this.component.instance.command.subscribe(this.exeCommand.bind(this));
        this.component.instance.toggleChangeTo.subscribe(this.toggleChangeTo.bind(this));

        // Load ChangeTo Options
        this.changeTo.instance.items = this.setChangeToOptions();
        this.changeTo.instance.title = 'Change To';
        this.changeToElement.remove();
        this.changeTo.changeDetectorRef.detectChanges();

        // We need to also react to page scrolling.
        document.body.addEventListener('scroll', this.hanlderScroll.bind(this), true);
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
        this.createChangeToTooltip();

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
            this.tippyChangeTo?.hide();
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

        this.tippyChangeTo?.setProps({
            getReferenceClientRect: () => this.tippy?.popper.getBoundingClientRect()
        });

        this.setMenuItems(doc, from);
        this.updateComponent();
        this.show();
    }

    /* @Overrrider */
    destroy() {
        this.tippy?.destroy();
        this.tippyChangeTo?.destroy();

        this.element.removeEventListener('mousedown', this.mousedownHandler, { capture: true });
        this.view.dom.removeEventListener('dragstart', this.dragstartHandler);

        this.editor.off('focus', this.focusHandler);
        this.editor.off('blur', this.blurHandler);

        this.component.instance.command.unsubscribe();
        this.component.instance.toggleChangeTo.unsubscribe();
        this.component.destroy();
        this.changeTo.destroy();

        document.body.removeEventListener('scroll', this.hanlderScroll.bind(this), true);
    }

    /* Update Component */
    updateComponent() {
        const { items } = this.component.instance;
        const aligment: string[] = ['left', 'center', 'right'];
        const activeMarks = this.setActiveMarks(aligment);
        this.setSelectedNodeItem();
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
        const isDotImage = node?.type.name == 'dotImage';

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

    setChangeToOptions() {
        const changeToOptions = suggestionOptions.filter((item) => item.id != 'horizontalLine');
        const changeTopCommands = {
            heading1: () => {
                this.editor.chain().focus().clearNodes().setHeading({ level: 1 }).run();
            },
            heading2: () => {
                this.editor.chain().focus().clearNodes().setHeading({ level: 2 }).run();
            },
            heading3: () => {
                this.editor.chain().focus().clearNodes().setHeading({ level: 3 }).run();
            },
            paragraph: () => {
                this.editor.chain().focus().clearNodes().setParagraph().run();
            },
            orderedList: () => {
                this.editor.chain().focus().clearNodes().toggleOrderedList().run();
            },
            bulletList: () => {
                this.editor.chain().focus().clearNodes().toggleBulletList().run();
            },
            blockquote: () => {
                this.editor.chain().focus().clearNodes().toggleBlockquote().run();
            },
            codeBlock: () => {
                this.editor.chain().focus().clearNodes().toggleCodeBlock().run();
            }
        };

        changeToOptions.forEach((option) => {
            option.isActive = () => {
                return option.id.includes('heading')
                    ? this.editor.isActive('heading', option.attributes)
                    : this.editor.isActive(option.id);
            };
            option.command = () => {
                changeTopCommands[option.id]();
                this.tippyChangeTo.hide();
                this.setSelectedNodeItem();
            };
        });
        return changeToOptions;
    }

    setSelectedNodeItem() {
        const items = this.changeTo.instance.items;
        const activeMarks = items.filter((option) => option?.isActive());
        // Needed because in some scenarios, paragraph and other mark (ex: blockquote)
        // can be active at the same time
        const activeNode = activeMarks.length > 1 ? activeMarks[1] : activeMarks[0];
        // Set Active on Change To List
        this.changeTo.instance.updateActiveItem(items.findIndex((item) => item === activeNode));
        // Set button label on Bubble Menu
        this.component.instance.selected = activeNode?.label || '';
    }

    // Tippy Change To
    createChangeToTooltip() {
        const { element: editorElement } = this.editor.options;

        if (this.tippyChangeTo || !editorElement) {
            return;
        }

        this.tippyChangeTo = tippy(editorElement, {
            ...this.tippyOptions,
            appendTo: document.body,
            getReferenceClientRect: null,
            content: this.changeToElement,
            placement: 'bottom-start',
            duration: 0,
            hideOnClick: false,
            popperOptions: {
                modifiers: popperModifiers
            },
            onHide: () => {
                this.changeTo.instance.isOpen = false;
            },
            onShow: () => {
                this.changeTo.instance.isOpen = true;
                this.setSelectedNodeItem();
            }
        });
    }

    toggleChangeTo() {
        if (this.tippyChangeTo.state.isVisible) {
            this.tippyChangeTo?.hide();
        } else {
            this.tippyChangeTo?.show();
        }
    }

    hanlderScroll() {
        if (this.tippyChangeTo?.state.isVisible) {
            this.tippyChangeTo?.hide();
        }
    }
}
