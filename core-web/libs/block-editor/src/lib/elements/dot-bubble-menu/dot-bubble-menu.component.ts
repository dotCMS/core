import { TiptapBubbleMenuDirective } from 'ngx-tiptap';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    inject,
    input,
    SecurityContext,
    signal,
    ViewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';

import { DropdownModule } from 'primeng/dropdown';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { Editor } from '@tiptap/core';

import { DotImageEditorPopoverComponent } from './components/dot-image-editor-popover/dot-image-editor-popover.component';
import { DotLinkEditorPopoverComponent } from './components/dot-link-editor-popover/dot-link-editor-popover.component';

import { EditorModalDirective } from '../../directive/editor-modal.directive';
import { codeIcon, headerIcons, olIcon, pIcon, quoteIcon, ulIcon } from '../../utils/icons';
import { getCurrentNodeType } from '../../utils/prosemirror';

interface NodeTypeOption {
    name: string;
    value: string;
    icon?: string;
    command: () => boolean;
}

const BUBBLE_MENU_HIDDEN_NODES = {
    tableCell: true,
    table: true,
    youtube: true,
    dotVideo: true,
    aiContent: true,
    loader: true
};

@Component({
    selector: 'dot-bubble-menu',
    templateUrl: './dot-bubble-menu.component.html',
    styleUrls: ['./dot-bubble-menu.component.scss'],
    imports: [
        CommonModule,
        TiptapBubbleMenuDirective,
        FormsModule,
        DropdownModule,
        DotLinkEditorPopoverComponent,
        DotImageEditorPopoverComponent,
        OverlayPanelModule
    ],
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBubbleMenuComponent {
    @ViewChild('editorModal') editorModal: EditorModalDirective;
    @ViewChild('bubbleMenu', { read: ElementRef }) bubbleMenuRef: ElementRef<HTMLElement>;

    protected readonly editor = input.required<Editor>();
    protected readonly cd = inject(ChangeDetectorRef);
    protected readonly domSanitizer = inject(DomSanitizer);

    protected readonly dropdownItem = signal<NodeTypeOption | null>(null);
    protected readonly placeholder = signal<string>('Paragraph');
    protected readonly showShould = signal<boolean>(true);
    protected readonly showImageMenu = signal<boolean>(false);

    protected readonly nodeTypeOptions: NodeTypeOption[] = [
        {
            name: 'Paragraph',
            value: 'paragraph',
            icon: pIcon,
            command: () => this.editor().chain().focus().clearNodes().focus().run()
        },
        {
            name: 'Heading 1',
            value: 'heading-1',
            icon: headerIcons[0],
            command: () =>
                this.editor().chain().focus().clearNodes().toggleHeading({ level: 1 }).run()
        },
        {
            name: 'Heading 2',
            value: 'heading-2',
            icon: headerIcons[1],
            command: () =>
                this.editor().chain().focus().clearNodes().toggleHeading({ level: 2 }).run()
        },
        {
            name: 'Heading 3',
            value: 'heading-3',
            icon: headerIcons[2],
            command: () =>
                this.editor().chain().focus().clearNodes().toggleHeading({ level: 3 }).run()
        },
        {
            name: 'Heading 4',
            value: 'heading-4',
            icon: headerIcons[3],
            command: () =>
                this.editor().chain().focus().clearNodes().toggleHeading({ level: 4 }).run()
        },
        {
            name: 'Heading 5',
            value: 'heading-5',
            icon: headerIcons[4],
            command: () =>
                this.editor().chain().focus().clearNodes().toggleHeading({ level: 5 }).run()
        },
        {
            name: 'Heading 6',
            value: 'heading-6',
            icon: headerIcons[5],
            command: () =>
                this.editor().chain().focus().clearNodes().toggleHeading({ level: 6 }).run()
        },
        {
            name: 'Ordered List',
            value: 'orderedList',
            icon: olIcon,
            command: () => this.editor().chain().focus().clearNodes().toggleOrderedList().run()
        },
        {
            name: 'Bullet List',
            value: 'bulletList',
            icon: ulIcon,
            command: () => this.editor().chain().focus().clearNodes().toggleBulletList().run()
        },
        {
            name: 'Blockquote',
            value: 'blockquote',
            icon: quoteIcon,
            command: () => this.editor().chain().focus().clearNodes().toggleBlockquote().run()
        },
        {
            name: 'Code Block',
            value: 'codeBlock',
            icon: codeIcon,
            command: () => this.editor().chain().focus().clearNodes().toggleCodeBlock().run()
        }
    ];

    protected readonly tippyOptions = {
        maxWidth: '100%',
        onBeforeUpdate: this.onBeforeUpdate.bind(this),
        placement: 'top-start',
        trigger: 'manual'
    };

    protected runConvertToCommand(option: NodeTypeOption) {
        option.command();
    }

    protected sanitizeHtml(html: string) {
        return this.domSanitizer.sanitize(SecurityContext.HTML, html);
    }

    protected preventLostEditorSelection(event: MouseEvent) {
        event.preventDefault();
    }

    protected imageHasLink() {
        const image = this.editor().getAttributes('dotImage');

        return !!image?.href;
    }

    private onBeforeUpdate() {
        this.checkIfShowBubbleMenu();
        this.checkIfShowImageMenu();
        this.setCurrentSelectedNode();
        this.detectChanges();
    }

    private detectChanges() {
        this.cd.markForCheck();
        this.cd.detectChanges();
    }

    private setCurrentSelectedNode() {
        const currentNodeType = getCurrentNodeType(this.editor());
        const option = this.nodeTypeOptions.find((option) => option.value === currentNodeType);
        this.dropdownItem.set(option || this.nodeTypeOptions[0]);
    }

    private checkIfShowBubbleMenu() {
        const currentNodeType = getCurrentNodeType(this.editor());
        this.showShould.set(!BUBBLE_MENU_HIDDEN_NODES[currentNodeType]);
    }

    private checkIfShowImageMenu() {
        const currentNodeType = getCurrentNodeType(this.editor());
        this.showImageMenu.set(currentNodeType === 'dotImage');
    }
}
