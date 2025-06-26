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
    private elRef = inject<ElementRef<HTMLElement>>(ElementRef);

    readonly editor = input.required<Editor>();
    readonly tippyOptions = input<BubbleMenuPluginProps['tippyOptions']>({});

    private tippy: Instance;

    ngOnInit(): void {
        const { element: editorElement } = this.editor().options;
        const editorIsAttached = !!editorElement.parentElement;

        if (!editorIsAttached) {
            return;
        }

        this.tippy = tippy(editorElement, {
            duration: 0,
            content: this.elRef.nativeElement,
            interactive: true,
            trigger: 'manual',
            placement: 'bottom',
            hideOnClick: 'toggle',
            getReferenceClientRect: () => {
                const { state, view } = this.editor();
                const { from, to } = state.selection;

                if (isNodeSelection(state.selection)) {
                    let node = view.nodeDOM(from) as HTMLElement;

                    if (node) {
                        const nodeViewWrapper = node.dataset.nodeViewWrapper
                            ? node
                            : node.querySelector('[data-node-view-wrapper]');

                        if (nodeViewWrapper) {
                            node = nodeViewWrapper.firstChild as HTMLElement;
                        }

                        if (node) {
                            return node.getBoundingClientRect();
                        }
                    }
                }

                return posToDOMRect(view, from, to);
            },
            ...this.tippyOptions
        });
    }

    ngOnDestroy(): void {
        this.tippy.destroy();
    }

    show() {
        this.tippy.show();
    }

    hide() {
        this.tippy.hide();
    }
}
