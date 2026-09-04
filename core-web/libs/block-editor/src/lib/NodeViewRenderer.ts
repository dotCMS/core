import { type Decoration, DecorationSet, type DecorationSource } from 'prosemirror-view';

import { Component, Injector, Input, Type, ChangeDetectionStrategy } from '@angular/core';

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
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class AngularNodeViewComponent implements NodeViewProps {
    // TODO: Skipped for migration because:
    //  This input overrides a field from a superclass, while the superclass field
    //  is not migrated.
    @Input() editor!: NodeViewProps['editor'];
    // TODO: Skipped for migration because:
    //  This input overrides a field from a superclass, while the superclass field
    //  is not migrated.
    @Input() node!: NodeViewProps['node'];
    // TODO: Skipped for migration because:
    //  This input overrides a field from a superclass, while the superclass field
    //  is not migrated.
    @Input() decorations!: readonly DecorationWithType[];
    // TODO: Skipped for migration because:
    //  This input overrides a field from a superclass, while the superclass field
    //  is not migrated.
    @Input() selected!: NodeViewProps['selected'];
    // TODO: Skipped for migration because:
    //  This input overrides a field from a superclass, while the superclass field
    //  is not migrated.
    @Input() extension!: NodeViewProps['extension'];
    // TODO: Skipped for migration because:
    //  This input overrides a field from a superclass, while the superclass field
    //  is not migrated.
    @Input() getPos!: NodeViewProps['getPos'];
    // TODO: Skipped for migration because:
    //  This input overrides a field from a superclass, while the superclass field
    //  is not migrated.
    @Input() updateAttributes!: NodeViewProps['updateAttributes'];
    // TODO: Skipped for migration because:
    //  This input overrides a field from a superclass, while the superclass field
    //  is not migrated.
    @Input() deleteNode!: NodeViewProps['deleteNode'];
    // TODO: Skipped for migration because:
    //  This input overrides a field from a superclass, while the superclass field
    //  is not migrated.
    @Input() view!: NodeViewProps['view'];
    // TODO: Skipped for migration because:
    //  This input overrides a field from a superclass, while the superclass field
    //  is not migrated.
    @Input() innerDecorations!: DecorationSource;
    // TODO: Skipped for migration because:
    //  This input overrides a field from a superclass, while the superclass field
    //  is not migrated.
    @Input() HTMLAttributes!: NodeViewProps['HTMLAttributes'];
}

interface AngularNodeViewRendererOptions extends NodeViewRendererOptions {
    update?:
        | ((node: ProseMirrorNode, decorations: readonly DecorationWithType[]) => boolean)
        | null;
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
    // `declare` (and so no `override`, which TypeScript forbids alongside it): this only
    // restates the base class's property for the type-checker and emits nothing. `libs/block-editor`
    // targets es2015, where a plain field is an assignment — but `apps/dotcms-ui` targets ES2022,
    // where `useDefineForClassFields` is on by default and the same field would emit a
    // `defineProperty` that shadows the base value with `undefined` (TS2612).
    declare decorations: readonly DecorationWithType[];

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

    // Signature mirrors ProseMirror's `NodeView.update`, which hands over a readonly
    // `Decoration[]`. TipTap narrows those to `DecorationWithType` for node views, which is
    // what every consumer below expects.
    update(node: ProseMirrorNode, nodeDecorations: readonly Decoration[]): boolean {
        const decorations = nodeDecorations as readonly DecorationWithType[];

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
