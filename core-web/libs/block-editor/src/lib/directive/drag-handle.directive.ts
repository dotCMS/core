import { AfterViewInit, Directive, ElementRef, Input, OnDestroy } from '@angular/core';

import { Editor } from '@tiptap/core';
import {
    DragHandlePlugin,
    dragHandlePluginDefaultKey,
    DragHandlePluginProps
} from '@tiptap/extension-drag-handle';
import { Node } from '@tiptap/pm/model';
import { Plugin } from '@tiptap/pm/state';

/**
 * Directive that adds drag handle functionality to an element.
 * This allows for dragging and repositioning of nodes in the editor.
 *
 * @example
 * <div dotDragHandle [editor]="editor" [pluginKey]="customKey"></div>
 */
@Directive({
    selector: '[dotDragHandle]',
    standalone: true
})
export class DragHandleDirective implements AfterViewInit, OnDestroy {
    /**
     * The Tiptap editor instance
     */
    @Input({ required: true }) editor!: Editor;

    /**
     * Optional plugin key for the drag handle
     */
    @Input() pluginKey = dragHandlePluginDefaultKey;

    /**
     * Optional callback when node changes
     */
    @Input() onNodeChange?: (data: { node: Node | null; editor: Editor; pos: number }) => void;

    /**
     * Optional Tippy.js options for the drag handle tooltip
     */
    @Input() tippyOptions?: DragHandlePluginProps['tippyOptions'];

    private plugin: Plugin | null = null;

    constructor(private elementRef: ElementRef<HTMLElement>) {}

    ngAfterViewInit(): void {
        if (!this.editor) {
            throw new Error('Required: Input `editor`');
        }

        if (this.editor.isDestroyed) {
            return;
        }

        this.plugin = DragHandlePlugin({
            editor: this.editor,
            element: this.elementRef.nativeElement,
            pluginKey: this.pluginKey,
            tippyOptions: this.tippyOptions,
            onNodeChange: this.onNodeChange
        });

        this.editor.registerPlugin(this.plugin);
    }

    ngOnDestroy(): void {
        if (this.editor && !this.editor.isDestroyed && this.plugin) {
            this.editor.unregisterPlugin(this.pluginKey);
            this.plugin = null;
        }
    }
}
