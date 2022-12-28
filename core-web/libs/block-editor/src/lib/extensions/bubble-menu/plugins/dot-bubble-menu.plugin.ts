import { ComponentRef } from '@angular/core';
import { EditorView } from 'prosemirror-view';
import { posToDOMRect } from '@tiptap/core';
import { EditorState, Plugin, PluginKey, NodeSelection } from 'prosemirror-state';
import { BubbleMenuView } from '@tiptap/extension-bubble-menu';
import tippy, { Instance } from 'tippy.js';

import {
    // Bubble Menu
    BubbleMenuComponentProps,
    BubbleMenuItem,
    DotBubbleMenuPluginProps,
    DotBubbleMenuViewProps,
    // Suggestions
    changeToItems,
    SuggestionsComponent,
    // Utils
    setBubbleMenuCoords,
    getNodeCoords,
    deleteByRange,
    deleteByNode,
    ImageNode,
    findParentNode,
    tableChangeToItems
} from '@dotcms/block-editor';

import { LINK_FORM_PLUGIN_KEY, BUBBLE_FORM_PLUGIN_KEY } from '@dotcms/block-editor';

import { getBubbleMenuItem, isListNode, popperModifiers } from '../utils';
import { filter, take } from 'rxjs/operators';

export const DotBubbleMenuPlugin = (options: DotBubbleMenuPluginProps) => {
    const component = options.component.instance;
    const changeTo = options.changeToComponent.instance;

    return new Plugin<DotBubbleMenuPluginProps>({
        key: options.pluginKey as PluginKey,
        view: (view) => new DotBubbleMenuPluginView({ view, ...options }),
        props: {
            /**
             * Catch and handle the keydown in the plugin
             *
             * @param {EditorView} view
             * @param {KeyboardEvent} event
             * @return {*}
             */
            handleKeyDown(_view: EditorView, event: KeyboardEvent) {
                const { key } = event;
                const { changeToIsOpen } = options.editor?.storage.bubbleMenu || {};

                if (changeToIsOpen) {
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
    component: ComponentRef<BubbleMenuComponentProps>;
    changeTo: ComponentRef<SuggestionsComponent>;
    changeToElement: HTMLElement;
    tippyChangeTo: Instance | undefined;

    private shouldShowProp = false;

    private selectionRange;
    private selectionNodesCount;
    private selectionNode;

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
        this.changeTo.instance.items = this.changeToItems();
        this.changeTo.instance.title = 'Change To';
        this.changeToElement.remove();
        this.changeTo.changeDetectorRef.detectChanges();

        // We need to also react to page scrolling.
        document.body.addEventListener('scroll', this.hanlderScroll.bind(this), true);
        document.body.addEventListener('mouseup', this.showMenu.bind(this), true);
        document.body.addEventListener('keyup', this.showMenu.bind(this), true);

        this.editor.off('blur', this.blurHandler);
    }

    showMenu() {
        if (this.shouldShowProp) {
            this.tippyChangeTo?.setProps({
                getReferenceClientRect: () => this.tippy?.popper.getBoundingClientRect()
            });
            this.show();
        }
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
        this.selectionRange = ranges[0];
        this.selectionNodesCount = 0;

        doc.nodesBetween(this.selectionRange.$from.pos, this.selectionRange.$to.pos, (node) => {
            if (node.isBlock) {
                this.selectionNodesCount++;
            }
        });

        const from = Math.min(...ranges.map((range) => range.$from.pos));
        const to = Math.max(...ranges.map((range) => range.$to.pos));

        this.shouldShowProp = this.shouldShow?.({
            editor: this.editor,
            view,
            state,
            oldState,
            from,
            to
        });

        if (!this.shouldShowProp) {
            this.hide();
            this.tippyChangeTo?.hide();

            return;
        }

        this.tippy?.setProps({
            getReferenceClientRect: () => {
                const node = view.nodeDOM(from) as HTMLElement;
                const type = doc.nodeAt(from)?.type.name;
                const viewCoords = view.dom.parentElement.getBoundingClientRect();
                const nodeCoords =
                    selection instanceof NodeSelection
                        ? getNodeCoords(node, type)
                        : posToDOMRect(view, from, to);

                return setBubbleMenuCoords({ viewCoords, nodeCoords, padding: 60 });
            }
        });

        this.updateComponent();
        this.setMenuItems(doc, from);
        this.show();
    }

    /* @Overrrider */
    destroy() {
        this.tippy?.destroy();
        this.tippyChangeTo?.destroy();

        this.element.removeEventListener('mousedown', this.mousedownHandler, { capture: true });
        this.view.dom.removeEventListener('dragstart', this.dragstartHandler);

        this.component.instance.command.unsubscribe();
        this.component.instance.toggleChangeTo.unsubscribe();

        this.component.destroy();
        this.changeTo.destroy();

        document.body.removeEventListener('scroll', this.hanlderScroll.bind(this), true);
        document.body.removeEventListener('mouseup', this.showMenu.bind(this), true);
        document.body.removeEventListener('keyup', this.showMenu.bind(this), true);
    }

    /* Update Component */
    updateComponent() {
        const { items } = this.component.instance;
        const { activeItem } = this.getActiveNode();
        const activeMarks = this.getActiveMarks(['left', 'center', 'right']);

        // Update
        this.component.instance.selected = activeItem?.label;
        this.component.instance.items = this.updateActiveItems(items, activeMarks);
        this.component.changeDetectorRef.detectChanges();
    }

    /**
     * Update Change To Component before showing the component
     *
     * @memberof DotBubbleMenuPluginView
     */
    updateChangeTo() {
        this.changeTo.instance.items = this.changeToItems();
        this.changeTo.changeDetectorRef.detectChanges();
        this.updateListActiveItem();
    }

    /**
     * Update the current Active Item in the Change To List.
     *
     * @memberof DotBubbleMenuPluginView
     */
    updateListActiveItem() {
        const { index } = this.getActiveNode();
        requestAnimationFrame(() => {
            this.changeTo.instance.list.updateActiveItem(index);
            this.changeTo.changeDetectorRef.detectChanges();
        });
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

    getActiveMarks = (aligment = []): string[] => {
        return [
            ...this.enabledMarks().filter((mark) => this.editor.isActive(mark)),
            ...aligment.filter((alignment) => this.editor.isActive({ textAlign: alignment }))
        ];
    };

    setMenuItems(doc, from) {
        const node = doc.nodeAt(from);
        const parentNode = findParentNode(this.editor.state.selection.$from);
        const type = parentNode.type.name === 'table' ? 'table' : node?.type.name;

        this.selectionNode = node;
        this.component.instance.items = getBubbleMenuItem(type);
    }

    openImageProperties() {
        const { open } = BUBBLE_FORM_PLUGIN_KEY.getState(this.editor.state);
        const { alt, src, title, data } = this.editor.getAttributes(ImageNode.name);
        const { title: dotTitle = '', asset } = data || {};

        open
            ? this.editor.commands.closeForm()
            : this.editor.commands
                  .openForm([
                      {
                          value: src || asset,
                          key: 'src',
                          label: 'path',
                          required: true,
                          controlType: 'text',
                          type: 'text'
                      },
                      {
                          value: alt || dotTitle,
                          key: 'alt',
                          label: 'alt',
                          controlType: 'text',
                          type: 'text'
                      },
                      {
                          value: title || dotTitle,
                          key: 'title',
                          label: 'caption',
                          controlType: 'text',
                          type: 'text'
                      }
                  ])
                  .pipe(
                      take(1),
                      filter((data) => data != null)
                  )
                  .subscribe((data) => {
                      requestAnimationFrame(() => {
                          this.editor.commands.setImage({ ...data });
                          this.editor.commands.closeForm();
                      });
                  });
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
                // eslint-disable-next-line
                const { isOpen } = LINK_FORM_PLUGIN_KEY.getState(this.editor.state);
                isOpen
                    ? this.editor.view.focus()
                    : this.editor.commands.openLinkForm({ openOnClick: false });
                break;

            case 'properties':
                this.openImageProperties();
                break;

            case 'deleteNode':
                this.selectionNodesCount > 1
                    ? deleteByRange(this.editor, this.selectionRange)
                    : deleteByNode({
                          editor: this.editor,
                          nodeType: this.selectionNode.type.name,
                          selectionRange: this.selectionRange
                      });
                break;

            case 'clearAll':
                this.editor.commands.unsetAllMarks();
                this.editor.commands.clearNodes();
                break;
        }
    }

    toggleTextAlign(alignment, active) {
        active
            ? this.editor.commands.unsetTextAlign()
            : this.editor.commands.setTextAlign(alignment);
    }

    changeToItems() {
        const allowedBlocks: string[] = this.editor.storage.dotConfig.allowedBlocks;
        const parentNode = findParentNode(this.editor.state.selection.$from);

        let changeToOptions = parentNode.type.name === 'table' ? tableChangeToItems : changeToItems;

        // means the user restrict the allowed blocks with the prop "allowedBlocks"
        if (allowedBlocks.length > 1) {
            changeToOptions = changeToOptions.filter((item) => allowedBlocks.includes(item.id));
        }

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
                this.editor.chain().focus().clearNodes().run();
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
                this.getActiveNode();
            };
        });

        return changeToOptions;
    }

    getActiveNode() {
        const items = this.changeToItems();
        const activeMarks = items.filter((option) => option?.isActive());
        // Needed because in some scenarios, paragraph and other mark (ex: blockquote)
        // can be active at the same time
        const activeItem = activeMarks.length > 1 ? activeMarks[1] : activeMarks[0];
        const index = items.findIndex((item) => item === activeItem);

        return { activeItem, index };
    }

    // Tippy Change To
    createChangeToTooltip() {
        const { element: editorElement } = this.editor.options;

        if (this.tippyChangeTo) {
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
                this.editor.storage.bubbleMenu.changeToIsOpen = false;
                this.changeTo.instance.items = [];
                this.changeTo.changeDetectorRef.detectChanges();
            },
            onShow: () => {
                this.editor.storage.bubbleMenu.changeToIsOpen = true;
                this.updateChangeTo();
            }
        });
    }

    toggleChangeTo() {
        const { changeToIsOpen } = this.editor?.storage.bubbleMenu || {};
        changeToIsOpen ? this.tippyChangeTo?.hide() : this.tippyChangeTo?.show();
    }

    hanlderScroll() {
        if (this.tippyChangeTo?.state.isVisible) {
            this.tippyChangeTo?.hide();
        }
    }
}
