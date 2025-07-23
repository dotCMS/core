import { TiptapBubbleMenuDirective } from 'ngx-tiptap';
import { Placement } from 'tippy.js';

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
    ViewChild,
    OnInit,
    DestroyRef
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';

import { DropdownModule } from 'primeng/dropdown';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { Editor } from '@tiptap/core';

import { DotAiService } from '@dotcms/data-access';

import { DotImageEditorPopoverComponent } from './components/dot-image-editor-popover/dot-image-editor-popover.component';
import { DotLinkEditorPopoverComponent } from './components/dot-link-editor-popover/dot-link-editor-popover.component';

import { EditorModalDirective } from '../../directive/editor-modal.directive';
import { AI_IMAGE_PROMPT_EXTENSION_NAME } from '../../extensions/ai-image-prompt/ai-image-prompt.extension';
import {
    codeIcon,
    headerIcons,
    listStarsIcon,
    mountsStarsIcon,
    olIcon,
    pIcon,
    quoteIcon,
    ulIcon
} from '../../utils/icons';
import { getCurrentNodeType } from '../../utils/prosemirror';

interface NodeTypeOption {
    name: string;
    value: string;
    icon?: string;
    command: () => boolean;
}

const BUBBLE_MENU_HIDDEN_NODES = {
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
export class DotBubbleMenuComponent implements OnInit {
    @ViewChild('editorModal') editorModal: EditorModalDirective;
    @ViewChild('bubbleMenu', { read: ElementRef }) bubbleMenuRef: ElementRef<HTMLElement>;
    @ViewChild('linkModal') linkModal: DotLinkEditorPopoverComponent;
    @ViewChild('imageModal') imageModal: DotImageEditorPopoverComponent;

    readonly editor = input.required<Editor>();
    protected readonly cd = inject(ChangeDetectorRef);
    protected readonly domSanitizer = inject(DomSanitizer);
    protected readonly dotAiService = inject(DotAiService);

    protected readonly dropdownItem = signal<NodeTypeOption | null>(null);
    protected readonly placeholder = signal<string>('Paragraph');
    protected readonly showShould = signal<boolean>(true);
    protected readonly showImageMenu = signal<boolean>(false);

    protected nodeTypeOptions = [
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
        placement: 'top-start' as Placement,
        trigger: 'manual'
    };

    destroyRef = inject(DestroyRef);

    ngOnInit() {
        this.dotAiService
            .checkPluginInstallation()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((isInstalled) => {
                if (isInstalled) {
                    this.nodeTypeOptions = [
                        {
                            name: 'AI Content',
                            value: 'aiContent',
                            icon: listStarsIcon,
                            command: () =>
                                this.editor().chain().focus().clearNodes().openAIPrompt().run()
                        },
                        {
                            name: 'AI Image',
                            value: AI_IMAGE_PROMPT_EXTENSION_NAME,
                            icon: mountsStarsIcon,
                            command: () =>
                                this.editor().chain().focus().clearNodes().openImagePrompt().run()
                        },
                        ...this.nodeTypeOptions
                    ];
                }
            });
    }

    protected runConvertToCommand(option: NodeTypeOption) {
        option.command();
    }

    protected sanitizeHtml(html: string) {
        return this.domSanitizer.sanitize(SecurityContext.HTML, html);
    }

    protected preventLostEditorSelection(event: MouseEvent) {
        event.preventDefault();
    }

    /**
     * Toggles the link editor popover and prevents event bubbling
     */
    protected toggleLinkModal(event: MouseEvent) {
        event.stopPropagation();
        this.linkModal?.toggle();
        this.imageModal?.hide();
    }

    /**
     * Toggles the image editor popover and prevents event bubbling
     */
    protected toggleImageModal(event: MouseEvent) {
        event.stopPropagation();
        this.imageModal?.toggle();
        this.linkModal?.hide();
    }

    /**
     * Closes any open popover components (link and image editors)
     */
    protected closePopups() {
        this.linkModal?.hide();
        this.imageModal?.hide();
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

        // CRITICAL: Always use the exact reference from nodeTypeOptions
        const foundOption = this.nodeTypeOptions.find((option) => option.value === currentNodeType);

        // Use the found reference or the first item's reference
        this.dropdownItem.set(foundOption ?? this.nodeTypeOptions[0]);
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
