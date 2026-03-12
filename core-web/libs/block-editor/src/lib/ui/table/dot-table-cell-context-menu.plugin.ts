import { Plugin, PluginKey } from 'prosemirror-state';
import { Decoration, DecorationSet, EditorView } from 'prosemirror-view';

import { ComponentRef, Injector, ViewContainerRef } from '@angular/core';

import { Editor, Extension } from '@tiptap/core';

import { getCellsOptions } from './utils';

import { SuggestionsComponent } from '../../shared';
import { createFloatingUI, type FloatingUIInstance } from '../../shared/utils/floating-ui.utils';

// Types and Interfaces
interface TableContextMenuOptions {
    editor: Editor;
    viewContainerRef: ViewContainerRef;
    /** Injector that provides SuggestionsService (e.g. from the block editor). Required for DeferBlock injector. */
    injector: Injector;
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

// Helper Functions
function setFocusDecoration(selection: Selection): Decoration {
    // Get the before and after position of the parent cell where the selection is
    return Decoration.node(selection.$to.before(CELL_DEPTH), selection.$to.after(CELL_DEPTH), {
        class: CSS_CLASSES.FOCUS
    });
}

function displayTableOptions(event: MouseEvent, floatingInstance: FloatingUIInstance | null): void {
    if (!floatingInstance || !event.target) {
        return;
    }

    event.preventDefault();
    event.stopPropagation();

    const target = event.target as HTMLElement;

    if (floatingInstance.isVisible) {
        floatingInstance.hide();
        return;
    }

    floatingInstance.setReferenceRect(() => target.getBoundingClientRect());
    requestAnimationFrame(() => floatingInstance.show());
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
    floatingInstance: FloatingUIInstance;
} {
    const { editor, viewContainerRef, injector } = options;
    const { element: editorElement } = editor.options;

    if (!editorElement || !viewContainerRef || !injector) {
        throw new Error('Editor element, ViewContainerRef, or Injector is not available');
    }

    const component = viewContainerRef.createComponent(SuggestionsComponent, { injector });
    const element = component.location.nativeElement as HTMLElement;

    const floatingInstance = createFloatingUI(
        () => (editorElement as HTMLElement).getBoundingClientRect(),
        element,
        {
            placement: 'bottom-start',
            offset: 8,
            zIndex: 10,
            appendTo: document.body,
            onShow: () => handleComponentSetup(editor, component),
            onHide: () => {
                editor.commands.freezeScroll(false);
            },
            onClickOutside: (e) => {
                if ((e?.target as HTMLElement)?.classList?.contains(CSS_CLASSES.CELL_ARROW)) return;
                floatingInstance.hide();
            }
        }
    );

    component.instance.title = '';
    component.instance.items = getCellsOptions(editor, floatingInstance);
    component.instance.currentLanguage = editor.storage.dotConfig?.lang || 'en';
    component.changeDetectorRef.detectChanges();

    return { component, floatingInstance };
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
    let floatingCellOptions: FloatingUIInstance | null = null;
    let componentRef: ComponentRef<SuggestionsComponent> | null = null;

    return new Plugin({
        key: new PluginKey('dotTableCell'),
        state: {
            apply: () => {
                /* empty - no state changes needed */
            },
            init: () => {
                try {
                    const { floatingInstance, component } = initializeComponent(options);
                    componentRef = component;
                    floatingCellOptions = floatingInstance;
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
                const isVisible = floatingCellOptions?.isVisible;

                if (!isVisible) {
                    return false;
                }

                if (key === 'Escape') {
                    event.stopImmediatePropagation();
                    floatingCellOptions?.hide();

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
                        displayTableOptions(event, floatingCellOptions);
                    }
                },
                mousedown: (view, event) => {
                    const target = event.target as HTMLElement;

                    if (isArrowClicked(target)) {
                        displayTableOptions(event, floatingCellOptions);
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
export const DotTableCellContextMenu = (viewContainerRef: ViewContainerRef, injector: Injector) => {
    return Extension.create({
        name: 'dotTableCellContextMenu',
        priority: 800,
        addProseMirrorPlugins() {
            return [
                TableCellContextMenuPlugin({
                    editor: this.editor,
                    viewContainerRef,
                    injector
                })
            ];
        }
    });
};
