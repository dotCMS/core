import { Plugin, PluginKey } from 'prosemirror-state';
import { Decoration, DecorationSet, EditorView } from 'prosemirror-view';
import tippy, { Instance, Props } from 'tippy.js';

import { ComponentRef, ViewContainerRef } from '@angular/core';

import { Editor, Extension } from '@tiptap/core';

import { getCellsOptions } from './utils';

import { SuggestionsComponent, popperModifiers } from '../../shared';

// Types and Interfaces
interface TableContextMenuOptions {
    editor: Editor;
    viewContainerRef: ViewContainerRef;
}

interface TableNode {
    type: {
        name: string;
    };
}

interface Selection {
    $to: {
        before: (depth: number) => number;
        after: (depth: number) => number;
    };
    $from: {
        depth: number;
        node: (depth: number) => TableNode | null;
    };
}

// Constants
const CELL_DEPTH = 3;
const GRANDPARENT_DEPTH_OFFSET = 1;
const RIGHT_MOUSE_BUTTON = 2;

const TABLE_NODE_TYPES = {
    CELL: 'tableCell',
    HEADER: 'tableHeader',
    ROW: 'tableRow'
} as const;

const CSS_CLASSES = {
    FOCUS: 'focus',
    CELL_ARROW: 'dot-cell-arrow'
} as const;

const CELL_OPTION_IDS = {
    MERGE_CELLS: 'mergeCells',
    SPLIT_CELLS: 'splitCells'
} as const;

const TIPPY_OPTIONS: Partial<Props> = {
    trigger: 'manual',
    interactive: true,
    appendTo: document.body,
    placement: 'auto-start',
    duration: 0,
    hideOnClick: false,
    maxWidth: 'none',
    popperOptions: {
        modifiers: popperModifiers
    },
    onClickOutside: (instance: Instance<Props>, event: Event) => {
        const target = event.target as HTMLElement;

        if (target.classList.contains(CSS_CLASSES.CELL_ARROW)) {
            return;
        }

        instance.hide();
    }
};

// Helper Functions
function setFocusDecoration(selection: Selection): Decoration {
    // Get the before and after position of the parent cell where the selection is
    return Decoration.node(selection.$to.before(CELL_DEPTH), selection.$to.after(CELL_DEPTH), {
        class: CSS_CLASSES.FOCUS
    });
}

function displayTableOptions(event: MouseEvent, tippyInstance: Instance | null): void {
    if (!tippyInstance || !event.target) {
        return;
    }

    event.preventDefault();
    event.stopPropagation();

    const target = event.target as HTMLElement;

    // Check if tippy is already visible - toggle behavior
    if (tippyInstance.state.isVisible) {
        // If already visible, hide it (toggle behavior)
        tippyInstance.hide();

        return;
    }

    // Set position and show
    tippyInstance.setProps({
        getReferenceClientRect: () => target.getBoundingClientRect()
    });

    // Use requestAnimationFrame to ensure DOM is ready
    requestAnimationFrame(() => {
        tippyInstance.show();
    });
}

function isArrowClicked(element: HTMLElement | null): boolean {
    return element?.classList.contains(CSS_CLASSES.CELL_ARROW) ?? false;
}

function isNodeRelatedToTable(node: TableNode | null): boolean {
    if (!node?.type?.name) {
        return false;
    }

    const validTypes = Object.values(TABLE_NODE_TYPES) as string[];

    return validTypes.includes(node.type.name);
}

function handleComponentSetup(editor: Editor, component: ComponentRef<SuggestionsComponent>): void {
    if (!component?.instance) {
        return;
    }

    editor.commands.freezeScroll(true);

    const { items } = component.instance;
    const mergeCellsOption = items.find((item) => item.id === CELL_OPTION_IDS.MERGE_CELLS);
    const splitCellsOption = items.find((item) => item.id === CELL_OPTION_IDS.SPLIT_CELLS);

    if (mergeCellsOption) {
        mergeCellsOption.disabled = !editor.can().mergeCells();
    }

    if (splitCellsOption) {
        splitCellsOption.disabled = !editor.can().splitCell();
    }

    // Use setTimeout to ensure change detection runs after the current execution cycle
    setTimeout(() => {
        component.changeDetectorRef.detectChanges();
    });
}

function initializeComponent(options: TableContextMenuOptions): {
    component: ComponentRef<SuggestionsComponent>;
    tippyInstance: Instance;
} {
    const { editor, viewContainerRef } = options;
    const { element: editorElement } = editor.options;

    if (!editorElement || !viewContainerRef) {
        throw new Error('Editor element or ViewContainerRef is not available');
    }

    const component = viewContainerRef.createComponent(SuggestionsComponent);
    const element = component.location.nativeElement;

    const tippyInstance = tippy(editorElement as HTMLElement, {
        ...TIPPY_OPTIONS,
        content: element,
        onShow: () => handleComponentSetup(editor, component),
        onHide: () => {
            editor.commands.freezeScroll(false);
        }
    });

    // Configure component instance
    component.instance.title = '';
    component.instance.items = getCellsOptions(editor, tippyInstance);
    component.instance.currentLanguage = editor.storage.dotConfig?.lang || 'en';
    component.changeDetectorRef.detectChanges();

    return { component, tippyInstance };
}

function getGrandparentNode(view: EditorView): TableNode | null {
    const { selection } = view.state;

    return selection.$from.node(selection.$from.depth - GRANDPARENT_DEPTH_OFFSET) || null;
}

function shouldShowTableOptions(view: EditorView): boolean {
    const grandpaSelectedNode = getGrandparentNode(view);

    return isNodeRelatedToTable(grandpaSelectedNode);
}

// Main Plugin
const TableCellContextMenuPlugin = (options: TableContextMenuOptions) => {
    let tippyCellOptions: Instance | null = null;
    let componentRef: ComponentRef<SuggestionsComponent> | null = null;

    return new Plugin({
        key: new PluginKey('dotTableCell'),
        state: {
            apply: () => {
                /* empty - no state changes needed */
            },
            init: () => {
                try {
                    const { tippyInstance, component } = initializeComponent(options);
                    componentRef = component;
                    tippyCellOptions = tippyInstance;
                } catch (error) {
                    console.error('Failed to initialize table cell context menu:', error);
                }
            }
        },
        props: {
            decorations(state) {
                const { selection } = state;
                const parentCell =
                    selection.$from.depth > CELL_DEPTH ? selection.$from.node(CELL_DEPTH) : null;
                const nodeName = parentCell?.type?.name;

                if (nodeName === TABLE_NODE_TYPES.CELL || nodeName === TABLE_NODE_TYPES.HEADER) {
                    return DecorationSet.create(state.doc, [setFocusDecoration(state.selection)]);
                }

                return null;
            },
            handleKeyDown(_, event) {
                const { key } = event;
                const isVisible = tippyCellOptions?.state.isVisible;

                if (!isVisible) {
                    return false;
                }

                if (key === 'Escape') {
                    event.stopImmediatePropagation();
                    tippyCellOptions?.hide();

                    return true;
                }

                if (key === 'Enter') {
                    componentRef.instance.execCommand();

                    return true;
                }

                if (key === 'ArrowDown' || key === 'ArrowUp') {
                    componentRef.instance.updateSelection(event);

                    return true;
                }

                if (key === 'ArrowRight' || key === 'ArrowLeft') {
                    return true;
                }

                return false;
            },
            handleDOMEvents: {
                contextmenu: (view: EditorView, event) => {
                    if (shouldShowTableOptions(view)) {
                        displayTableOptions(event, tippyCellOptions);
                    }
                },
                mousedown: (view, event) => {
                    const target = event.target as HTMLElement;

                    if (isArrowClicked(target)) {
                        displayTableOptions(event, tippyCellOptions);
                    } else if (
                        event.button === RIGHT_MOUSE_BUTTON &&
                        shouldShowTableOptions(view)
                    ) {
                        event.preventDefault();
                    }
                }
            }
        }
    });
};

// Extension Export
export const DotTableCellContextMenu = (viewContainerRef: ViewContainerRef) => {
    return Extension.create({
        name: 'dotTableCellContextMenu',
        priority: 800,
        addProseMirrorPlugins() {
            return [
                TableCellContextMenuPlugin({
                    editor: this.editor,
                    viewContainerRef: viewContainerRef
                })
            ];
        }
    });
};
