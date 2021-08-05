import { Editor, posToDOMRect, Range } from '@tiptap/core';
import { EditorState, Plugin, PluginKey } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import tippy, { Instance, Props } from 'tippy.js';
export interface FloatingActionsPluginProps {
    editor: Editor;
    element: HTMLElement;
    tippyOptions?: Partial<Props>;
    on: {
        command: (props: { rect: DOMRect, range: Range, editor: Editor }) => void;
        keydown: (view: EditorView, event: KeyboardEvent) => void;
    }

}

export type FloatingActionsViewProps = FloatingActionsPluginProps & {
    view: EditorView;
};

export class FloatingActionsView {
    public editor: Editor;

    public element: HTMLElement;

    public view: EditorView;

    public preventHide = false;

    public tippy!: Instance;

    public command: (props: { rect: DOMRect, range: Range, editor: Editor }) => void;

    constructor({ editor, element, view, tippyOptions, on: { command } }: FloatingActionsViewProps) {
        console.log('constructor');
        this.editor = editor;
        this.element = element;
        this.view = view;
        this.element.addEventListener('mousedown', this.mousedownHandler, { capture: true });
        this.editor.on('focus', this.focusHandler);
        this.editor.on('blur', this.blurHandler);
        this.createTooltip(tippyOptions);
        this.element.style.visibility = 'visible';
        this.command = command;
    }

    mousedownHandler = () => {
        console.log('mousedownHandler');
        this.preventHide = true;

        const { selection } = this.editor.state;
        const { from, to } = selection;
        const rect = posToDOMRect(this.view, from, to);
        this.command({ rect, range: { from, to }, editor: this.editor });
    };

    focusHandler = () => {
        console.log('focusHandler');
        // we use `setTimeout` to make sure `selection` is already updated
        setTimeout(() => this.update(this.editor.view));
    };

    blurHandler = () => {
        console.log('blurHandler');
        this.view.focus()
    };


    createTooltip(options: Partial<Props> = {}) {
        this.tippy = tippy(this.view.dom, {
            duration: 0,
            getReferenceClientRect: null,
            content: this.element,
            interactive: true,
            trigger: 'manual',
            placement: 'left',
            hideOnClick: 'toggle',
            ...options
        });
    }

    update(view: EditorView, oldState?: EditorState) {
        const { state, composing } = view;
        const { doc, selection } = state;
        const isSame = oldState && oldState.doc.eq(doc) && oldState.selection.eq(selection);

        if (composing || isSame) {
            return;
        }

        const { $anchor, empty, from, to } = selection;
        const isRootDepth = $anchor.depth === 1;
        const isNodeEmpty =
            !selection.$anchor.parent.isLeaf && !selection.$anchor.parent.textContent;
        const isActive = isRootDepth && isNodeEmpty;

        if (!empty || !isActive) {
            this.hide();

            return;
        }

        this.tippy.setProps({
            getReferenceClientRect: () => posToDOMRect(view, from, to)
        });

        this.show();
    }

    show() {
        this.tippy.show();
    }

    hide() {
        this.tippy.hide();
    }

    destroy() {
        this.tippy.destroy();
        this.element.removeEventListener('mousedown', this.mousedownHandler);
        this.editor.off('focus', this.focusHandler);
    }
}

export const FloatingActionsPluginKey = new PluginKey('menuFloating');

export const FloatingActionsPlugin = (options: FloatingActionsPluginProps) => {
    return new Plugin({
        key: FloatingActionsPluginKey,
        view: (view) => new FloatingActionsView({ view, ...options }),
        props: {
            handleKeyDown(view: EditorView, event: KeyboardEvent) {
                console.log('handleKeyDown')
                options.on.keydown(view, event);

                return false
            },
        }
    });
};
