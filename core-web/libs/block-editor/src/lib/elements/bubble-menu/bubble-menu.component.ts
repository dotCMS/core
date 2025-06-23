import { TiptapBubbleMenuDirective } from 'ngx-tiptap';
import { Node } from 'prosemirror-model';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    inject,
    input,
    signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { Editor } from '@tiptap/core';

interface NodeTypeOption {
    name: string;
    value: string;
    command: () => boolean;
}

@Component({
    selector: 'dot-bubble-menu',
    templateUrl: './bubble-menu.component.html',
    styleUrls: ['./bubble-menu.component.scss'],
    imports: [CommonModule, TiptapBubbleMenuDirective, FormsModule, DropdownModule],
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class BubbleMenuComponent {
    protected readonly editor = input.required<Editor>();
    protected readonly cd = inject(ChangeDetectorRef);

    protected readonly dropdownItem = signal<NodeTypeOption | null>(null);
    protected readonly placeholder = signal<string>('Paragraph');
    protected readonly onBeforeUpdateFn = this.onBeforeUpdate.bind(this);

    protected readonly nodeTypeOptions: NodeTypeOption[] = [
        {
            name: 'Paragraph',
            value: 'paragraph',
            command: () => this.editor().chain().focus().clearNodes().run()
        },
        {
            name: 'Heading 1',
            value: 'heading-1',
            command: () => this.editor().chain().focus().clearNodes().setHeading({ level: 1 }).run()
        },
        {
            name: 'Heading 2',
            value: 'heading-2',
            command: () => this.editor().chain().focus().clearNodes().setHeading({ level: 2 }).run()
        },
        {
            name: 'Heading 3',
            value: 'heading-3',
            command: () => this.editor().chain().focus().clearNodes().setHeading({ level: 3 }).run()
        },
        {
            name: 'Heading 4',
            value: 'heading-4',
            command: () => this.editor().chain().focus().clearNodes().setHeading({ level: 4 }).run()
        },
        {
            name: 'Heading 5',
            value: 'heading-5',
            command: () => this.editor().chain().focus().clearNodes().setHeading({ level: 5 }).run()
        },
        {
            name: 'Heading 6',
            value: 'heading-6',
            command: () => this.editor().chain().focus().clearNodes().setHeading({ level: 6 }).run()
        },
        {
            name: 'Ordered List',
            value: 'orderedList',
            command: () => this.editor().chain().focus().clearNodes().toggleOrderedList?.().run()
        },
        {
            name: 'Bullet List',
            value: 'bulletList',
            command: () => this.editor().chain().focus().clearNodes().toggleBulletList?.().run()
        },
        {
            name: 'Blockquote',
            value: 'blockquote',
            command: () => this.editor().chain().focus().clearNodes().toggleBlockquote?.().run()
        },
        {
            name: 'Code Block',
            value: 'codeBlock',
            command: () => this.editor().chain().focus().clearNodes().toggleCodeBlock?.().run()
        }
    ];

    protected changeToBlock(option: NodeTypeOption) {
        option.command();
    }

    private onBeforeUpdate() {
        this.setCurrentSelectedNode();
        this.detectChanges();
    }

    private detectChanges() {
        this.cd.markForCheck();
        this.cd.detectChanges();
    }

    private setCurrentSelectedNode() {
        const currentNodeType = this.getCurrentNodeType();
        const option = this.nodeTypeOptions.find((option) => option.value === currentNodeType);

        this.dropdownItem.set(option || this.nodeTypeOptions[0]);
    }

    private getCurrentNodeType(): string {
        const state = this.editor().view.state;
        const from = state.selection.from;
        const pos = state.doc.resolve(from);
        const currentNode = pos.node(pos.depth);
        const parentNode = pos.node(pos.depth - 1);

        if (!parentNode) {
            return 'paragraph';
        }

        const parentType = parentNode.type.name;
        const currentNodeType = this.getNodeTypeWithLevel(currentNode);

        return parentType === 'doc' ? currentNodeType : parentType;
    }

    private getNodeTypeWithLevel(node: Node): string {
        const hasLevelAttribute = node.attrs.level;
        const baseNodeType = node.type.name;

        return hasLevelAttribute ? `${baseNodeType}-${node.attrs.level}` : baseNodeType;
    }
}
