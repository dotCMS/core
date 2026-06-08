import tippy, { Instance, Props as TippyProps } from 'tippy.js';

import { Directive, ElementRef, OnDestroy, OnInit, inject, input } from '@angular/core';

import { Editor, isNodeSelection, posToDOMRect } from '@tiptap/core';

import { getEditorElement } from '../shared/utils';

@Directive({
    selector: 'dot-editor-modal[editor], [dotEditorModal][editor]',
    exportAs: 'dotEditorModal'
})
export class EditorModalDirective implements OnInit, OnDestroy {
    readonly editor = input.required<Editor>();
    // v3 dropped `tippyOptions` from BubbleMenuPluginProps; type against tippy directly.
    readonly tippyOptions = input<Partial<TippyProps>>({});

    private elRef = inject<ElementRef<HTMLElement>>(ElementRef);
    private tippy: Instance;

    private editorElement: HTMLElement;

    private readonly PROPER_MODIFIERS = {
        modifiers: [
            {
                name: 'animate-flip',
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
        const editorElement = getEditorElement(this.editor());
        const editorIsAttached = !!editorElement?.parentElement;

        if (!editorElement || !editorIsAttached) {
            return;
        }

        this.editorElement = editorElement;
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
        }) as Instance;

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
                // Anchor to THIS editor's bubble menu. The directive lives inside the owning
                // `<dot-bubble-menu>` (alongside the `[tiptapbubblemenu]` element), so scope the
                // lookup to that host instead of the whole document.
                //
                // A global `document.querySelector('[tiptapbubblemenu]')` returns the FIRST bubble
                // menu on the page, so when a content type has multiple Block Editor fields the
                // image/link popover anchored to the wrong editor instance (#35908).
                const bubbleMenu = this.elRef.nativeElement
                    .closest('dot-bubble-menu')
                    ?.querySelector('[tiptapbubblemenu]');

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
