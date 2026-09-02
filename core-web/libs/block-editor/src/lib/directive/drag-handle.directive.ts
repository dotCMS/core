import { Props as TippyProps } from 'tippy.js';

import {
    AfterViewInit,
    Directive,
    ElementRef,
    inject,
    input,
    OnDestroy,
    signal
} from '@angular/core';

import { Editor, isNodeEmpty } from '@tiptap/core';
import {
    DragHandlePlugin,
    dragHandlePluginDefaultKey,
    DragHandlePluginProps
} from '@tiptap/extension-drag-handle';
import { Node } from '@tiptap/pm/model';
import { Plugin, PluginKey } from '@tiptap/pm/state';

/**
 * Directive that adds drag handle functionality to an element.
 * This allows for dragging and repositioning of nodes in the editor.
 *
 * @example
 * <div dotDragHandle [editor]="editor" [pluginKey]="customKey"></div>
 */
@Directive({
    selector: '[dotDragHandle]'
})
export class DragHandleDirective implements AfterViewInit, OnDestroy {
    /**
     * The Tiptap editor instance
     */
    editor = input.required<Editor>();

    /**
     * Optional plugin key for the drag handle
     *
     * @default dragHandlePluginDefaultKey
     * @type {PluginKey<any>} - The plugin key for the drag handle, it's an any type because the plugin key is not typed
     */
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    pluginKey = input<PluginKey<any>>(dragHandlePluginDefaultKey);

    /**
     * Optional callback when node changes
     */
    onNodeChange = input<
        ((data: { node: Node | null; editor: Editor; pos: number }) => void) | undefined
    >(undefined);

    /**
     * Optional Tippy.js options for the drag handle tooltip.
     * v3 dropped `tippyOptions` from DragHandlePluginProps; type against tippy directly.
     */
    tippyOptions = input<Partial<TippyProps> | undefined>(undefined);

    private plugin = signal<Plugin | null>(null);
    private elementRef = inject(ElementRef<HTMLElement>);

    ngAfterViewInit(): void {
        const editor = this.editor();

        if (!editor) {
            throw new Error('Required: Input `editor`');
        }

        if (editor.isDestroyed) {
            return;
        }

        // v3 changes: `DragHandlePlugin(...)` now returns `{ unbind, plugin }`,
        // and `tippyOptions` was replaced by floating-ui-shaped `computePositionConfig`.
        // Translate the legacy `tippyOptions.placement` so callers don't have to change
        // their inputs. Tippy-only fields (`zIndex`, `duration`) have no floating-ui
        // equivalent and are dropped — z-index belongs in CSS, animation duration is gone.
        const tippyOpts = this.tippyOptions();
        const { plugin } = DragHandlePlugin({
            editor: editor,
            element: this.elementRef.nativeElement,
            pluginKey: this.pluginKey(),
            onNodeChange: (data) => {
                const onNodeChange = this.onNodeChange();
                if (onNodeChange) {
                    onNodeChange(data);
                } else {
                    this.handleNodeChange(data.node);
                }
            },
            computePositionConfig: tippyOpts?.placement
                ? {
                      placement:
                          tippyOpts.placement as DragHandlePluginProps['computePositionConfig']['placement']
                  }
                : undefined
        } as unknown as DragHandlePluginProps);

        this.plugin.set(plugin);

        editor.registerPlugin(this.plugin());
    }

    ngOnDestroy(): void {
        this.cleanupPlugin();
    }

    private handleNodeChange(node: Node | null): void {
        if (!node) {
            return;
        }

        const element = this.elementRef.nativeElement;
        const isEmptyNode = this.isEmptyNode(node);
        element.style.display = isEmptyNode ? 'none' : '';
    }

    /**
     * Cleanup the plugin when the directive is destroyed
     */
    private cleanupPlugin(): void {
        const editor = this.editor();
        if (editor && !editor.isDestroyed && this.plugin()) {
            editor.unregisterPlugin(this.pluginKey());
            this.plugin.set(null);
        }
    }

    /**
     * Check if the node is an empty node
     * @param node - The node to check
     * @returns True if the node is an empty node, false otherwise
     */
    private isEmptyNode(node: Node): boolean {
        const isEmpty = !node.isLeaf && isNodeEmpty(node) && !node.childCount;

        return isEmpty;
    }
}
