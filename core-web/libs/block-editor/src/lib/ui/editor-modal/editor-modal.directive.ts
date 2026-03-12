import { Directive, ElementRef, OnDestroy, OnInit, inject, input } from '@angular/core';

import { Editor, isNodeSelection, posToDOMRect } from '@tiptap/core';

import { createFloatingUI, type FloatingUIInstance } from '../../shared/utils/floating-ui.utils';

@Directive({
    selector: 'dot-editor-modal[editor], [dotEditorModal][editor]',
    exportAs: 'dotEditorModal'
})
export class EditorModalDirective implements OnInit, OnDestroy {
    readonly editor = input.required<Editor>();

    private elRef = inject<ElementRef<HTMLElement>>(ElementRef);
    private floating: FloatingUIInstance | null = null;

    private editorElement: HTMLElement;

    /**
     * The native element of the floating modal.
     */
    get nativeElement() {
        return this.elRef.nativeElement;
    }

    get isVisible() {
        return this.floating?.isVisible ?? false;
    }

    ngOnInit(): void {
        const editorElement = this.editor().options.element as Element;
        const editorIsAttached = !!editorElement.parentElement;

        if (!editorIsAttached) {
            return;
        }

        this.editorElement = editorElement as HTMLElement;
        const el = this.elRef.nativeElement;
        this.floating = createFloatingUI(() => this.getReferenceClientRect(), el, {
            placement: 'bottom-start',
            offset: 0,
            zIndex: 10,
            onClickOutside: () => this.hide()
        });

        editorElement.addEventListener('mousedown', () => this.hide());
    }

    ngOnDestroy(): void {
        this.floating?.destroy();
        this.floating = null;
        this.editorElement?.removeEventListener('mousedown', () => this.hide());
    }

    show() {
        this.floating?.show();
    }

    hide() {
        this.floating?.hide();
    }

    toggle() {
        this.floating?.isVisible ? this.hide() : this.show();
    }

    private getReferenceClientRect() {
        const { state, view } = this.editor();
        const { from, to } = state.selection;

        // Handle node selections (like images, tables, etc.)
        if (isNodeSelection(state.selection)) {
            const node = this.getNodeElement(view, from);
            if (node) {
                // If the node has a bubble menu, return its bounding client rect
                const bubbleMenu = document.querySelector('[tiptapbubblemenu]');

                if (bubbleMenu) {
                    return bubbleMenu.getBoundingClientRect();
                }

                // Otherwise, return the node's bounding client rect
                return node.getBoundingClientRect();
            }
        }

        // Handle text selections
        return posToDOMRect(view, from, to);
    }

    private getNodeElement(view: Editor['view'], pos: number): HTMLElement | null {
        const node = view.nodeDOM(pos) as HTMLElement;

        if (!node) return null;

        // Look for node view wrapper and get its first child
        const nodeViewWrapper = node.dataset.nodeViewWrapper
            ? node
            : node.querySelector('[data-node-view-wrapper]');

        return (nodeViewWrapper?.firstChild as HTMLElement) || node;
    }
}
