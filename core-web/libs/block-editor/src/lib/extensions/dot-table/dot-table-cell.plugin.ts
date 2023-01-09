import { Plugin, PluginKey } from 'prosemirror-state';
import { Decoration, DecorationSet } from 'prosemirror-view';

import tippy, { Instance, Props } from 'tippy.js';

import { popperModifiers, SuggestionsComponent } from '@dotcms/block-editor';
import { getCellsOptions } from './utils';

class DotTableCellPluginView {
    public tippy: Instance | undefined;

    constructor(view, tippy) {
        this.tippy = tippy;
    }

    // eslint-disable-next-line
    init(): void {}

    // eslint-disable-next-line
    update(): void {}

    destroy() {
        this.tippy.destroy();
    }
}

export const DotTableCellPlugin = (options) => {
    let tippyCellOptions;

    function setFocusDecoration(selection): Decoration {
        // get the before and after position of the parent cell where the selection is.
        return Decoration.node(selection.$to.before(3), selection.$to.after(3), {
            class: 'focus'
        });
    }

    function displayTableOptions(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        tippyCellOptions?.setProps({
            getReferenceClientRect: () => (event.target as HTMLElement).getBoundingClientRect()
        });
        tippyCellOptions.show();
    }

    function isArrowClicked(element: HTMLButtonElement): boolean {
        return element?.classList.contains('dot-cell-arrow');
    }

    function isNodeRelatedToTable(node): boolean {
        return (
            node?.type.name === 'tableCell' ||
            node?.type.name === 'tableHeader' ||
            node?.type.name === 'tableRow'
        );
    }

    return new Plugin({
        key: new PluginKey('dotTableCell'),
        state: {
            // eslint-disable-next-line
            apply: () => {},
            init: () => {
                const component = options.viewContainerRef.createComponent(SuggestionsComponent);
                const element = component.location.nativeElement;
                component.instance.currentLanguage = options.editor.storage.dotConfig.lang;

                const defaultTippyOptions: Partial<Props> = {
                    duration: 500,
                    maxWidth: 'none',
                    placement: 'top-start',
                    trigger: 'manual',
                    interactive: true
                };

                const { element: editorElement } = options.editor.options;
                tippyCellOptions = tippy(editorElement, {
                    ...defaultTippyOptions,
                    appendTo: document.body,
                    getReferenceClientRect: null,
                    content: element,
                    placement: 'bottom-start',
                    duration: 0,
                    hideOnClick: true,
                    popperOptions: {
                        modifiers: popperModifiers
                    },
                    onShow: () => {
                        const mergeCellsOption = component.instance.items.find(
                            (item) => item.id == 'mergeCells'
                        );
                        const splitCellsOption = component.instance.items.find(
                            (item) => item.id == 'splitCells'
                        );

                        mergeCellsOption.disabled = !options.editor.can().mergeCells();
                        splitCellsOption.disabled = !options.editor.can().splitCell();
                        setTimeout(() => {
                            component.changeDetectorRef.detectChanges();
                        });
                    }
                });

                component.instance.items = getCellsOptions(options.editor, tippyCellOptions);
                component.instance.title = '';
                component.changeDetectorRef.detectChanges();
            }
        },

        view: (view) => new DotTableCellPluginView(view, tippyCellOptions),
        props: {
            decorations(state) {
                // Table cells deep is 3, this approach will work while we don't allow nested tables.
                const parentCell =
                    state.selection.$from.depth > 3 ? state.selection.$from.node(3) : null;

                if (
                    parentCell?.type?.name == 'tableCell' ||
                    parentCell?.type?.name == 'tableHeader'
                ) {
                    return DecorationSet.create(state.doc, [setFocusDecoration(state.selection)]);
                }

                return null;
            },

            handleDOMEvents: {
                contextmenu: (view, event) => {
                    const grandpaSelectedNode = view.state.selection.$from.node(
                        view.state.selection.$from.depth - 1
                    );

                    if (isNodeRelatedToTable(grandpaSelectedNode)) {
                        displayTableOptions(event);
                    }
                },

                mousedown: (view, event) => {
                    const grandpaSelectedNode = view.state.selection.$from.node(
                        view.state.selection.$from.depth - 1
                    );

                    if (isArrowClicked(event.target as HTMLButtonElement)) {
                        displayTableOptions(event);
                    } else if (event.button === 2 && isNodeRelatedToTable(grandpaSelectedNode)) {
                        event.preventDefault();
                    }
                }
            }
        }
    });
};
