import { DecorationSet, type DecorationSource } from 'prosemirror-view';

import { Component, Injector, Input, Type } from '@angular/core';

import {
    DecorationWithType,
    Editor,
    NodeView,
    NodeViewProps,
    NodeViewRenderer,
    NodeViewRendererOptions,
    NodeViewRendererProps
} from '@tiptap/core';

import { AngularRenderer } from './AngularRenderer';

import type { Node as ProseMirrorNode } from 'prosemirror-model';

export type toJSONFn = (this: { node: ProseMirrorNode }) => Record<string, unknown>;

@Component({
    template: '',
    standalone: false
})
export class AngularNodeViewComponent implements NodeViewProps {
    @Input() editor!: NodeViewProps['editor'];
    @Input() node!: NodeViewProps['node'];
    @Input() decorations!: readonly DecorationWithType[];
    @Input() selected!: NodeViewProps['selected'];
    @Input() extension!: NodeViewProps['extension'];
    @Input() getPos!: NodeViewProps['getPos'];
    @Input() updateAttributes!: NodeViewProps['updateAttributes'];
    @Input() deleteNode!: NodeViewProps['deleteNode'];
    @Input() view!: NodeViewProps['view'];
    @Input() innerDecorations!: DecorationSource;
    @Input() HTMLAttributes!: NodeViewProps['HTMLAttributes'];
}

interface AngularNodeViewRendererOptions extends NodeViewRendererOptions {
    update?: ((node: ProseMirrorNode, decorations: DecorationWithType[]) => boolean) | null;
    toJSON?: toJSONFn;
    injector: Injector;
}

class AngularNodeView extends NodeView<
    Type<AngularNodeViewComponent>,
    Editor,
    AngularNodeViewRendererOptions
> {
    renderer!: AngularRenderer<AngularNodeViewComponent, NodeViewProps>;
    contentDOMElement!: HTMLElement | null;
    override decorations!: readonly DecorationWithType[];

    override mount() {
        const injector = this.options.injector as Injector;

        const props: NodeViewProps = {
            editor: this.editor,
            node: this.node,
            decorations: this.decorations as readonly DecorationWithType[],
            selected: false,
            extension: this.extension,
            getPos: () => this.getPos(),
            updateAttributes: (attributes = {}) => this.updateAttributes(attributes),
            deleteNode: () => this.deleteNode(),
            view: this.editor.view,
            innerDecorations: DecorationSet.empty,
            HTMLAttributes: {}
        };

        // create renderer
        this.renderer = new AngularRenderer(this.component, injector, props);

        // Register drag handler
        if (this.extension.config.draggable) {
            this.renderer.elementRef.nativeElement.ondragstart = (e: DragEvent) => {
                this.onDragStart(e);
            };
        }

        //
        if (this.options.toJSON) {
            this.node.toJSON = this.options.toJSON.bind(this);
        }

        this.contentDOMElement = this.node.isLeaf
            ? null
            : document.createElement(this.node.isInline ? 'span' : 'div');

        if (this.contentDOMElement) {
            // For some reason the whiteSpace prop is not inherited properly in Chrome and Safari
            // With this fix it seems to work fine
            // See: https://github.com/ueberdosis/tiptap/issues/1197
            this.contentDOMElement.style.whiteSpace = 'inherit';
            this.renderer.detectChanges();
        }
    }

    override get dom() {
        return this.renderer.dom;
    }

    override get contentDOM() {
        if (this.node.isLeaf) {
            return null;
        }

        this.maybeMoveContentDOM();

        return this.contentDOMElement;
    }

    private maybeMoveContentDOM(): void {
        const contentElement = this.dom.querySelector('[data-node-view-content]');

        if (
            this.contentDOMElement &&
            contentElement &&
            !contentElement.contains(this.contentDOMElement)
        ) {
            contentElement.appendChild(this.contentDOMElement);
        }
    }

    update(node: ProseMirrorNode, decorations: DecorationWithType[]): boolean {
        if (this.options.update) {
            return this.options.update(node, decorations);
        }

        if (this.options.toJSON) {
            this.node.toJSON = this.options.toJSON.bind(this);
        }

        if (node.type !== this.node.type) {
            return false;
        }

        if (node === this.node && this.decorations === decorations) {
            return true;
        }

        this.node = node;
        this.decorations = decorations;
        this.renderer.updateProps({ node, decorations });
        this.maybeMoveContentDOM();

        return true;
    }

    selectNode() {
        this.renderer.updateProps({ selected: true });

        this.renderer.elementRef.nativeElement.classList.add('ProseMirror-selectednode');
    }

    deselectNode() {
        this.renderer.updateProps({ selected: false });
        this.renderer.elementRef.nativeElement.classList.remove('ProseMirror-selectednode');
    }

    destroy() {
        this.renderer.destroy();
    }
}

export const AngularNodeViewRenderer = (
    component: Type<AngularNodeViewComponent>,
    options: Partial<AngularNodeViewRendererOptions>
): NodeViewRenderer => {
    return (props: NodeViewRendererProps) => {
        return new AngularNodeView(component, props, options);
    };
};
