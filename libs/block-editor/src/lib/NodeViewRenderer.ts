import { Component, Injector, Input, Type } from '@angular/core';
import {
    Editor,
    NodeView,
    NodeViewProps,
    NodeViewRenderer,
    NodeViewRendererProps
} from '@tiptap/core';
import { Decoration, NodeView as ProseMirrorNodeView } from 'prosemirror-view';
import { Node as ProseMirrorNode } from 'prosemirror-model';
import { AngularRenderer } from './AngularRenderer';

@Component({ template: '' })
export class AngularNodeViewComponent {
    @Input() props!: NodeViewProps;
}

interface AngularNodeViewRendererOptions {
    stopEvent?: ((event: Event) => boolean) | null;
    update?: ((node: ProseMirrorNode, decorations: Decoration[]) => boolean) | null;
    injector: Injector;
}

class AngularNodeView
    extends NodeView<Type<AngularNodeViewComponent>, Editor>
    implements ProseMirrorNodeView {
    renderer!: AngularRenderer<AngularNodeViewComponent>;
    contentDOMElement!: HTMLElement | null;

    mount() {
        const injector = (this.options as AngularNodeViewRendererOptions).injector as Injector;

        const props: NodeViewProps = {
            editor: this.editor,
            node: this.node,
            decorations: this.decorations,
            deleteNode: () => {},
            selected: false,
            extension: this.extension,
            getPos: () => this.getPos(),
            updateAttributes: (attributes = {}) => this.updateAttributes(attributes)
        };

        // create renderer
        this.renderer = new AngularRenderer(this.component, injector);

        // Pass input props to the component
        this.renderer.instance.props = props;

        if (this.extension.config.draggable) {
            // Register drag handler
            this.renderer.elementRef.nativeElement.ondragstart = (e: DragEvent) => {
                this.onDragStart(e);
            };
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

        // attach stopEvent
        if (this.options.stopEvent) {
            this.stopEvent = this.options.stopEvent;
        }
    }

    private updateProps(props: Partial<NodeViewProps>) {
        this.renderer.instance.props = {
            ...this.renderer.instance.props,
            ...props
        };
    }

    get dom() {
        return this.renderer.dom;
    }

    get contentDOM() {
        if (this.node.isLeaf) {
            return null;
        }

        const contentElement = this.dom.querySelector('[data-node-view-content]');

        if (
            this.contentDOMElement &&
            contentElement &&
            !contentElement.contains(this.contentDOMElement)
        ) {
            contentElement.appendChild(this.contentDOMElement);
        }

        return this.contentDOMElement;
    }

    update(node: ProseMirrorNode, decorations: Decoration[]): boolean {
        if (this.options.update) {
            return this.options.update(node, decorations);
        }

        if (node.type !== this.node.type) {
            return false;
        }

        if (node === this.node && this.decorations === decorations) {
            return true;
        }

        this.node = node;
        this.decorations = decorations;
        this.updateProps({ node, decorations });

        return true;
    }

    selectNode() {
        this.updateProps({ selected: true });
    }

    deselectNode() {
        this.updateProps({ selected: false });
    }

    destroy() {
        this.renderer.destroy();
    }
}

export const AngularNodeViewRenderer = (
    component: Type<AngularNodeViewComponent>,
    options: AngularNodeViewRendererOptions
): NodeViewRenderer => {
    return (props: NodeViewRendererProps) => {
        return new AngularNodeView(component, props, options) as ProseMirrorNodeView;
    };
};
