import { EditorState, NodeSelection, Plugin, PluginKey } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import tippy, { Instance } from 'tippy.js';

import { ComponentRef } from '@angular/core';

import { filter, take } from 'rxjs/operators';

import { posToDOMRect } from '@tiptap/core';
import { BubbleMenuView } from '@tiptap/extension-bubble-menu';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';
import { ImageNode } from '../../../nodes';
import {
    changeToItems,
    deleteByNode,
    deleteByRange,
    findParentNode,
    SuggestionsComponent,
    tableChangeToItems
} from '../../../shared';
import { BUBBLE_FORM_PLUGIN_KEY } from '../../bubble-form/bubble-form.extension';
import { LINK_FORM_PLUGIN_KEY } from '../../bubble-link-form/bubble-link-form.extension';
import {
    BubbleMenuComponentProps,
    BubbleMenuItem,
    DotBubbleMenuPluginProps,
    DotBubbleMenuViewProps
} from '../models';
import { getBubbleMenuItem, getNodeCoords, popperModifiers, setBubbleMenuCoords } from '../utils';

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
    dotContentTypeService: DotContentTypeService;
    dotMessageService: DotMessageService;

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

        //Services
        this.dotContentTypeService = props.dotContentTypeService;
        this.dotMessageService = props.messageService;

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
    override update(view: EditorView, oldState?: EditorState) {
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
            element: this.element,
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

        this.show();
        this.setMenuItems(doc, from);
        this.updateComponent();
    }

    /* @Overrrider */
    override destroy() {
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
        const activeMarks = this.getActiveMarks(['justify', 'left', 'center', 'right']);

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

        this.selectionNode = node || null;
        this.component.instance.items = getBubbleMenuItem(type);
        this.component.changeDetectorRef.detectChanges();
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
                          this.editor.commands.updateAttributes(ImageNode.name, { ...data });
                          this.editor.commands.closeForm();
                      });
                  });
    }

    /* Run commands */
    exeCommand(item: BubbleMenuItem) {
        const { markAction: action, active } = item;
        const { data = {} } = this.selectionNode.attrs;
        const { inode, languageId } = data;
        const currentInode = this.getQueryParam('inode');

        switch (action) {
            case 'bold':
                this.editor.commands.toggleBold?.();
                break;

            case 'italic':
                this.editor.commands.toggleItalic?.();
                break;

            case 'strike':
                this.editor.commands.toggleStrike?.();
                break;

            case 'underline':
                this.editor.commands.toggleUnderline?.();
                break;

            case 'justify':
                this.toggleTextAlign(action, active);
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
                this.editor.commands.toggleBulletList?.();
                break;

            case 'orderedList':
                this.editor.commands.toggleOrderedList?.();
                break;

            case 'indent':
                // Handle lists with native commands directly
                if (this.editor.isActive('listItem')) {
                    // Try sinkListItem first, if it fails, manually indent with wrapIn
                    if (this.editor.can().sinkListItem('listItem')) {
                        this.editor.commands.sinkListItem('listItem');
                    } else {
                        // Alternative: wrap in a new list of the same type
                        const currentList = this.editor.isActive('bulletList')
                            ? 'bulletList'
                            : 'orderedList';
                        this.editor.chain().wrapIn(currentList).focus().run();
                    }
                } else {
                    // Use IndentExtension for paragraphs, headings, blockquotes
                    this.editor.commands.indent();
                }
                break;

            case 'outdent':
                // Handle lists with native commands directly
                if (this.editor.isActive('listItem')) {
                    this.editor.commands.liftListItem('listItem');
                } else {
                    // Use IndentExtension for paragraphs, headings, blockquotes
                    this.editor.commands.outdent();
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
                    : this.selectionNode &&
                      deleteByNode({
                          editor: this.editor,
                          nodeType: this.selectionNode.type.name,
                          selectionRange: this.selectionRange
                      });
                break;

            case 'clearAll':
                this.editor.commands?.unsetAllMarks();
                this.editor.commands?.clearNodes();
                break;

            case 'superscript':
                this.editor.commands?.toggleSuperscript?.();
                break;

            case 'subscript':
                this.editor.commands?.toggleSubscript?.();
                break;

            case 'goToContentlet':
                this.goToContentlet(inode, languageId);

                break;
        }
    }

    /**
     * Navigates from the Block Editor to edit a contentlet in either the legacy or new content editor.
     *
     * This method is triggered from the bubble menu when a user selects to edit embedded content.
     * It performs the following operations:
     * 1. Validates that there's a selected node with the required data
     * 2. Shows a confirmation dialog to warn about potential data loss
     * 3. Fetches content type information to determine which editor to use
     * 4. Saves navigation data to localStorage for returning to the Block Editor later
     * 5. Redirects to the appropriate editor based on feature flags
     *
     * @param {string} contentletInode - The unique identifier (inode) of the contentlet to edit
     * @param {number} languageId - The language ID of the contentlet version to edit
     *
     * @throws {console.warn} Logs a warning if the selection node is undefined
     *
     * @see {@link FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED} - Controls which editor is used
     * @see {@link extractInodeFromUrl} - Helper method to extract the current inode for return navigation
     */
    goToContentlet(contentletInode: string, languageId: number) {
        // Validate selection exists before proceeding
        if (!this.selectionNode) {
            console.warn('Selection node is undefined, cannot navigate to contentlet');
            return;
        }

        // Extract content type information from the selected node
        const { data = {} } = this.selectionNode.attrs;
        const { contentType } = data;

        // Confirm navigation with user to prevent accidental data loss
        if (!confirm(this.dotMessageService.get('message.contentlet.lose.unsaved.changes'))) {
            return;
        }

        // Query content type service to determine editor capabilities and preferences
        this.dotContentTypeService
            .getContentType(contentType)
            .pipe(take(1))
            .subscribe((contentTypeInfo) => {
                // Determine which editor to use based on feature flag in content type metadata
                const shouldUseOldEditor =
                    !contentTypeInfo?.metadata?.[
                        FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
                    ];

                // Prepare return navigation data
                // Extract current page title removing any suffix after the dash
                const title = window.parent
                    ? window.parent.document.title.split(' - ')[0]
                    : document.title.split(' - ')[0] ||
                      this.dotMessageService.get('message.contentlet.back.to.content');

                // Store navigation state in localStorage for returning to block editor
                const relationshipReturnValue = {
                    title,
                    blockEditorBackUrl: shouldUseOldEditor
                        ? this.generateBackUrl(contentletInode)
                        : window.location.href,
                    inode: this.extractInodeFromUrl(shouldUseOldEditor) // is not needed but I am seeing it, to follow the logic edit_contentlet_basic_properties.jsp
                };

                localStorage.setItem(
                    'dotcms.relationships.relationshipReturnValue',
                    JSON.stringify(relationshipReturnValue)
                );

                // Navigate to the appropriate editor based on feature flag
                if (shouldUseOldEditor) {
                    // Legacy approach - direct page navigation to old editor
                    window.parent.location.href = `/dotAdmin/#/c/content/${contentletInode}`;
                } else {
                    window.parent.location.href = `/dotAdmin/#/content/${contentletInode}`;
                }
            });
    }

    toggleTextAlign(alignment, active) {
        active
            ? this.editor.commands?.unsetTextAlign?.()
            : this.editor.commands?.setTextAlign?.(alignment);
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
            heading4: () => {
                this.editor.chain().focus().clearNodes().setHeading({ level: 4 }).run();
            },
            heading5: () => {
                this.editor.chain().focus().clearNodes().setHeading({ level: 5 }).run();
            },
            heading6: () => {
                this.editor.chain().focus().clearNodes().setHeading({ level: 6 }).run();
            },
            paragraph: () => {
                this.editor.chain().focus().clearNodes().run();
            },
            orderedList: () => {
                this.editor.chain().focus().clearNodes().toggleOrderedList?.().run();
            },
            bulletList: () => {
                this.editor.chain().focus().clearNodes().toggleBulletList?.().run();
            },
            blockquote: () => {
                this.editor.chain().focus().clearNodes().toggleBlockquote?.().run();
            },
            codeBlock: () => {
                this.editor.chain().focus().clearNodes().toggleCodeBlock?.().run();
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
                this.editor.commands.freezeScroll(false);
            },
            onShow: () => {
                this.editor.storage.bubbleMenu.changeToIsOpen = true;
                this.editor.commands.freezeScroll(true);
                this.updateChangeTo();
            }
        });
    }

    toggleChangeTo() {
        this.tippyChangeTo?.state.isVisible
            ? this.tippyChangeTo?.hide()
            : this.tippyChangeTo?.show();
    }

    private hanlderScroll(e: Event) {
        const element = e.target as HTMLElement;
        const suggestionElement = this.changeTo.instance.listElement?.nativeElement;

        if (!this.tippy?.state.isMounted || element === suggestionElement) {
            return;
        }

        this.tippyChangeTo?.hide();
    }

    /**
     * Retrieves a query parameter value from the current URL.
     *
     * This helper method safely extracts query parameters from the current page URL
     * using the native URLSearchParams API, which handles proper decoding of values.
     *
     * @param {string} param - The name of the query parameter to extract
     * @returns {string|null} - The value of the specified parameter, or null if not present
     *
     * @example
     * // For URL: https://example.com?inode=123abc&language=1
     * getQueryParam('inode') // returns "123abc"
     * getQueryParam('missing') // returns null
     */
    private getQueryParam(param: string): string | null {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get(param);
    }

    /**
     * Extracts the inode from the current URL based on the editor type.
     *
     * This helper method parses the current URL to extract the inode identifier,
     * handling different URL formats between the legacy and new editor systems.
     * It supports two common DotCMS URL patterns for content:
     *
     * @param {boolean} isOldEditor - Flag indicating whether to extract from parent window (legacy editor)
     *                                or current window (new editor)
     *
     * @returns {string|null} The extracted inode string if found, or null if not found
     *
     * @example
     * // For URL: /dotAdmin/#/content/123abc456def
     * extractInodeFromUrl(false) // returns "123abc456def"
     *
     * @example
     * // For URL: /dotAdmin/#/c/content/789xyz123abc
     * extractInodeFromUrl(true) // returns "789xyz123abc"
     */
    private extractInodeFromUrl(isOldEditor: boolean): string | null {
        // Determine which window location to use based on editor type
        const url = isOldEditor ? window.parent.location.href : window.location.href;

        // Define regex patterns for both URL formats
        // Pattern 1: /dotAdmin/#/content/[inode] - Used by new editor
        const contentPattern = /\/content\/([a-f0-9-]+)/i;
        // Pattern 2: /dotAdmin/#/c/content/[inode] - Used by legacy editor
        const legacyPattern = /\/c\/content\/([a-f0-9-]+)/i;

        // Try matching the new editor pattern first
        const contentMatch = url.match(contentPattern);
        if (contentMatch && contentMatch[1]) {
            return contentMatch[1];
        }

        // Fall back to legacy pattern if new pattern didn't match
        const legacyMatch = url.match(legacyPattern);
        if (legacyMatch && legacyMatch[1]) {
            return legacyMatch[1];
        }

        // Return null if no pattern matched
        return null;
    }

    /**
     * Generates the back URL for the Block Editor by replacing the inode in the parent URL.
     *
     * This method takes the current parent window location and replaces the inode portion
     * of the URL with the inode from the current window's query parameters.
     *
     * This is needed because the iframe refresh the inode when switch between languages,
     * and needs to be updated to generete the correct back url.
     *
     * @param {string} contentletInode - The inode to use if no query parameter is found
     * @returns {string} The modified URL with the replaced inode
     */
    private generateBackUrl(contentletInode: string): string {
        const currentUrl = window.parent.location.href;
        // Get inode from query params
        const params = new URLSearchParams(window.location.search);
        const inode = params.get('inode') || contentletInode;

        // Pattern to match the inode in the URL
        const inodePattern = /\/c\/content\/([a-f0-9-]+)/i;

        // Replace the inode in the URL with the inode from query params
        return currentUrl.replace(inodePattern, `/c/content/${inode}`);
    }
}
