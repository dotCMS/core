import { TiptapBubbleMenuDirective } from 'ngx-tiptap';
import { Instance } from 'tippy.js';

import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
    DestroyRef,
    ElementRef,
    inject,
    input,
    OnInit,
    output,
    SecurityContext,
    signal,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';

import { Button } from 'primeng/button';
import { Select } from 'primeng/select';

import { Editor } from '@tiptap/core';

import { DotAiConfigService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotImageEditorPopoverComponent } from './components/dot-image-editor-popover/dot-image-editor-popover.component';
import { DotLinkEditorPopoverComponent } from './components/dot-link-editor-popover/dot-link-editor-popover.component';
import { getContentletDataFromSelection } from './utils';

import { AI_CONTENT_PROMPT_EXTENSION_NAME } from '../../extensions/ai-content-prompt/ai-content-prompt.extension';
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
import { getCurrentLeafBlock } from '../../utils/prosemirror';

interface NodeTypeOption {
    name: string;
    value: string;
    icon?: string;
    command: () => boolean;
}

const BUBBLE_MENU_VISIBLE_NODES = {
    text: true,
    heading: true,
    dotImage: true,
    paragraph: true,
    dotContent: true
};

@Component({
    selector: 'dot-bubble-menu',
    templateUrl: './dot-bubble-menu.component.html',
    styleUrls: ['./dot-bubble-menu.component.css'],
    imports: [
        TiptapBubbleMenuDirective,
        FormsModule,
        Button,
        Select,
        DotLinkEditorPopoverComponent,
        DotImageEditorPopoverComponent,
        DotMessagePipe
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBubbleMenuComponent implements OnInit {
    dropdown = viewChild<Select>('dropdown');
    linkModal = viewChild.required<DotLinkEditorPopoverComponent>('linkModal');
    imageModal = viewChild.required<DotImageEditorPopoverComponent>('imageModal');
    bubbleMenuRef = viewChild.required<ElementRef<HTMLElement>>('bubbleMenu');

    readonly editor = input.required<Editor>();

    /** The author asked to edit the selected contentlet (see {@link goToContentlet}). */
    readonly contentletEdit = output<{ inode: string; contentType: string }>();

    protected readonly cd = inject(ChangeDetectorRef);
    protected readonly domSanitizer = inject(DomSanitizer);
    protected readonly dotAiService = inject(DotAiConfigService);
    private readonly destroyRef = inject(DestroyRef);

    protected readonly dropdownItem = signal<NodeTypeOption | null>(null);
    protected readonly placeholder = signal<string>('Paragraph');
    protected readonly currentNodeType = signal<string>('paragraph');
    protected readonly showShould = signal<boolean>(true);
    protected readonly showImageMenu = computed(() => this.currentNodeType() === 'dotImage');
    protected readonly showContentMenu = computed(() => this.currentNodeType() === 'dotContent');
    protected readonly imageTextWrap = signal<string | null>(null);
    protected readonly imageTextAlign = signal<string | null>(null);

    protected nodeTypeOptions: NodeTypeOption[] = [
        {
            name: 'Paragraph',
            value: 'paragraph',
            icon: pIcon,
            command: () => this.editor().chain().focus().clearNodes().focus().run()
        },
        {
            name: 'Heading 1',
            value: 'heading1',
            icon: headerIcons[0],
            command: () =>
                this.editor().chain().focus().clearNodes().toggleHeading({ level: 1 }).run()
        },
        {
            name: 'Heading 2',
            value: 'heading2',
            icon: headerIcons[1],
            command: () =>
                this.editor().chain().focus().clearNodes().toggleHeading({ level: 2 }).run()
        },
        {
            name: 'Heading 3',
            value: 'heading3',
            icon: headerIcons[2],
            command: () =>
                this.editor().chain().focus().clearNodes().toggleHeading({ level: 3 }).run()
        },
        {
            name: 'Heading 4',
            value: 'heading4',
            icon: headerIcons[3],
            command: () =>
                this.editor().chain().focus().clearNodes().toggleHeading({ level: 4 }).run()
        },
        {
            name: 'Heading 5',
            value: 'heading5',
            icon: headerIcons[4],
            command: () =>
                this.editor().chain().focus().clearNodes().toggleHeading({ level: 5 }).run()
        },
        {
            name: 'Heading 6',
            value: 'heading6',
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

    // ngx-tiptap v14 dropped tippy for floating-ui. Map the relevant options:
    //   - `placement` and `flip` map directly.
    //   - Default `strategy: 'absolute'` (floating-ui default) keeps the menu anchored to its
    //      reference text and scrolling with the page; `'fixed'` mis-positioned on first show.
    //   - `onUpdate` replaces tippy's `onBeforeUpdate` for refreshing the dropdown's selected node
    //      whenever the bubble menu re-positions; without it the dropdown shows no value.
    //   - tippy-only callbacks (`onClickOutside`, `trigger`, `maxWidth`) have no floating-ui equivalent
    //      and are dropped — outside-click dismissal is handled by the bubble menu plugin lifecycle.
    protected readonly bubbleOptions = {
        placement: 'top-start' as const,
        flip: { fallbackPlacements: ['top' as const, 'bottom' as const] },
        onUpdate: () => {
            this.onBeforeUpdate();
        }
    };

    ngOnInit() {
        // Initial filter without AI entries
        this.nodeTypeOptions = this.filterByAllowed(this.nodeTypeOptions);

        this.dotAiService
            .checkPluginInstallation()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((isInstalled) => {
                if (isInstalled) {
                    const withAI = [
                        {
                            name: 'AI Content',
                            value: AI_CONTENT_PROMPT_EXTENSION_NAME,
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

                    this.nodeTypeOptions = this.filterByAllowed(withAI);
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
        this.linkModal()?.toggle();
        this.imageModal()?.hide();
        this.dropdown()?.hide();
    }

    /**
     * Toggles the image editor popover and prevents event bubbling
     */
    protected toggleImageModal(event: MouseEvent) {
        event.stopPropagation();
        this.imageModal()?.toggle();
        this.linkModal()?.hide();
    }

    /**
     * Closes any open popover components (link and image editors)
     */
    protected closePopups() {
        this.linkModal()?.hide();
        this.imageModal()?.hide();
    }

    protected imageHasLink() {
        const image = this.editor().getAttributes('dotImage');

        return !!image?.href;
    }

    protected setImageTextWrap(value: 'left' | 'right') {
        this.editor().chain().focus().setImageTextWrap(value).run();
        const currentWrap = this.imageTextWrap();
        this.imageTextWrap.set(currentWrap === value ? null : value);
        this.imageTextAlign.set(null);
    }

    protected setImageTextAlign(align: string) {
        const isToggleOff = this.imageTextAlign() === align;
        const resolvedAlign = isToggleOff ? null : align;

        this.editor()
            .chain()
            .focus()
            .updateAttributes('dotImage', { textAlign: resolvedAlign, textWrap: null })
            .run();
        this.imageTextAlign.set(resolvedAlign);
        this.imageTextWrap.set(null);
    }
    /**
     * Toggles superscript on the selected text, removing subscript first to
     * ensure the two marks are mutually exclusive.
     */
    protected toggleSuperscript() {
        this.editor().chain().focus().unsetSubscript().toggleSuperscript().run();
    }

    /**
     * Toggles subscript on the selected text, removing superscript first to
     * ensure the two marks are mutually exclusive.
     */
    protected toggleSubscript() {
        this.editor().chain().focus().unsetSuperscript().toggleSubscript().run();
    }

    /**
     * Asks to edit the selected contentlet, and nothing else: whoever mounts the editor decides
     * where it opens (#38303). The Edit Content form stacks a panel over the content being edited;
     * the legacy JSP editor forwards it to the panel it sits in. There is no unsaved-changes
     * prompt, no "return to" breadcrumb and no navigation of the parent window, all of which threw
     * the author out of the content they were editing.
     */
    protected goToContentlet() {
        const { contentType, inode } = getContentletDataFromSelection(this.editor());

        if (!contentType || !inode) {
            return;
        }

        this.contentletEdit.emit({ inode, contentType });
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
        const currentNodeType = getCurrentLeafBlock(this.editor());
        const baseNodeType = currentNodeType.startsWith('heading') ? 'heading' : currentNodeType;

        // For the dropdown, we need the heading with the level. E.g. heading1, heading2, etc.
        const foundOption = this.nodeTypeOptions.find((option) => option.value === currentNodeType);

        // Use the found reference or the first item's reference
        this.dropdownItem.set(foundOption ?? this.nodeTypeOptions[0]);
        this.currentNodeType.set(baseNodeType);
        this.showShould.set(BUBBLE_MENU_VISIBLE_NODES[baseNodeType]);

        if (baseNodeType === 'dotImage') {
            const attrs = this.editor().getAttributes('dotImage');
            this.imageTextWrap.set(attrs?.textWrap ?? null);
            this.imageTextAlign.set(attrs?.textAlign ?? null);
        }
    }

    /**
     * Filters node type options against the allowedBlocks list from editor config.
     * If allowedBlocks is not provided or empty, returns the options unchanged.
     */
    private filterByAllowed(options: NodeTypeOption[]): NodeTypeOption[] {
        const allowedBlocks = this.editor().storage.dotConfig?.allowedBlocks;

        // If allowedBlocks is not provided, empty, or only contains the default 'paragraph',
        // treat it as "no restrictions" and return all options.
        if (
            !allowedBlocks?.length ||
            (allowedBlocks.length === 1 && allowedBlocks[0] === 'paragraph')
        ) {
            return options;
        }

        return options.filter((opt) => allowedBlocks.includes(opt.value));
    }

    private onClickOutside(instance: Instance, event: MouseEvent) {
        const target = event.target as HTMLElement;
        const isImageElement = this.imageModal().tippyElement?.contains(target);
        const isLinkElement = this.linkModal().tippyElement?.contains(target);

        if (isImageElement || isLinkElement) {
            return;
        }

        instance.hide();
        this.closePopups();
    }
}
