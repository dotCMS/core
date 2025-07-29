import { Extension, isNodeEmpty } from '@tiptap/core';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import { Decoration, DecorationSet } from '@tiptap/pm/view';

export interface DotPlusButtonOptions {
    showOnlyWhenEditable: boolean;
    showOnlyCurrent: boolean;
    includeChildren: boolean;
}

export enum PositionHeadings {
    TOP_INITIAL = '40px',
    TOP_CURRENT = '26px'
}

// Constants for button styling
const BUTTON_STYLES = {
    position: 'absolute',
    left: '-45px',
    top: '-2px'
} as const;

const CONTAINER_STYLES = {
    position: 'relative'
} as const;

/**
 * Creates the plus button element with basic styling
 */
const createPlusButton = (): HTMLButtonElement => {
    const button = document.createElement('button');
    button.classList.add('add-button');
    button.innerHTML = '<i class="pi pi-plus"></i>';
    button.setAttribute('draggable', 'false');
    
    // Apply basic styles
    Object.assign(button.style, BUTTON_STYLES);
    
    return button;
};

/**
 * Creates the container div for the plus button
 */
const createButtonContainer = (): HTMLDivElement => {
    const div = document.createElement('div');
    Object.assign(div.style, CONTAINER_STYLES);
    div.setAttribute('draggable', 'false');
    
    return div;
};

/**
 * Applies position-specific styling based on node type and position
 */
const applyPositionStyling = (_button: HTMLButtonElement, _nodeDOM: HTMLElement): void => {
    // WIP
};

/**
 * Adds click event listener to the button
 */
const addButtonEventListener = (button: HTMLButtonElement): void => {
    button.addEventListener(
        'mousedown',
        (e) => {
            e.preventDefault();
        },
        { once: true }
    );
};

/**
 * Creates a complete plus button widget with container
 */
const createPlusButtonWidget = ({ nodeDOM }: { nodeDOM: HTMLElement }): HTMLDivElement => {
    const button = createPlusButton();
    const container = createButtonContainer();
    
    applyPositionStyling(button, nodeDOM);
    addButtonEventListener(button);
    
    container.appendChild(button);
    
    return container;
};

export const DotCMSPlusButton = Extension.create<DotPlusButtonOptions>({
    name: 'dotCMSPlusButton',

    addOptions() {
        return {
            showOnlyWhenEditable: true,
            showOnlyCurrent: true,
            includeChildren: false
        };
    },

    addProseMirrorPlugins() {
        return [
            new Plugin({
                key: new PluginKey('dotCMSPlusButton'),
                props: {
                    decorations: ({ doc, selection }) => {
                        const active = this.editor.isEditable || !this.options.showOnlyWhenEditable
                        const { anchor } = selection
                        const decorations: Decoration[] = []
            
                        if (!active) {
                          return null
                        }
            
                        doc.descendants((node, pos) => {
                          const hasAnchor = anchor >= pos && anchor <= pos + node.nodeSize
                          const isEmpty = !node.isLeaf && isNodeEmpty(node) && !node.childCount

                          if ( (hasAnchor || !this.options.showOnlyCurrent) && isEmpty) {

                            const nodeDOM = this.editor.view.nodeDOM(pos) as HTMLElement;

                            const widget = createPlusButtonWidget({ nodeDOM })
                            const decoration = Decoration.widget(pos, widget);
                            decorations.push(decoration);
            
                          }
            
                          return this.options.includeChildren
                        })
            
                        return DecorationSet.create(doc, decorations)
                      },
                }
            })
        ];
    }
});