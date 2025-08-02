import tippy, { Instance } from 'tippy.js';

import { Directive, ElementRef, OnDestroy, OnInit, inject, input } from '@angular/core';

import { Editor, isNodeSelection, posToDOMRect } from '@tiptap/core';
import { BubbleMenuPluginProps } from '@tiptap/extension-bubble-menu';

@Directive({
    selector: 'dot-editor-modal[editor], [dotEditorModal][editor]',
    standalone: true,
    exportAs: 'dotEditorModal'
})
export class EditorModalDirective implements OnInit, OnDestroy {
    readonly editor = input.required<Editor>();
    readonly tippyOptions = input<BubbleMenuPluginProps['tippyOptions']>({});

    private elRef = inject<ElementRef<HTMLElement>>(ElementRef);
    private tippy: Instance;

    private editorElement: HTMLElement;

    private readonly PROPER_MODIFIERS = {
        modifiers: [
            {
                name: 'flip',
                options: { fallbackPlacements: ['top-start'] }
            }
        ]
    };

    /**
     * The native element of the Tippy instance.
     */
    get nativeElement() {
        return this.elRef.nativeElement;
    }

    ngOnInit(): void {
        const { element: editorElement } = this.editor().options;
        const editorIsAttached = !!editorElement.parentElement;

        if (!editorIsAttached) {
            return;
        }

        this.editorElement = editorElement as HTMLElement;
        this.tippy = tippy(editorElement, {
            duration: 0,
            content: this.elRef.nativeElement,
            interactive: true,
            trigger: 'manual',
            placement: 'bottom-start',
            popperOptions: this.PROPER_MODIFIERS,
            hideOnClick: 'toggle',
            getReferenceClientRect: this.getReferenceClientRect.bind(this),
            ...this.tippyOptions()
        });

        editorElement.addEventListener('mousedown', () => this.hide());
    }

    ngOnDestroy(): void {
        this.tippy?.destroy();
        this.editorElement.removeEventListener('mousedown', () => this.hide());
    }

    show() {
        this.tippy.show();
    }

    hide() {
        this.tippy.hide();
    }

    toggle() {
        this.tippy.state.isVisible ? this.hide() : this.show();
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
