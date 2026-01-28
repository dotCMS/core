import { TiptapBubbleMenuDirective } from 'ngx-tiptap';
import { of } from 'rxjs';
import { Instance, Props } from 'tippy.js';

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
    SecurityContext,
    signal,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';

import { PopoverModule } from 'primeng/popover';
import { Select, SelectModule } from 'primeng/select';

import { catchError, take } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { DotAiService, DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentType, FeaturedFlags } from '@dotcms/dotcms-models';
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
    styleUrls: ['./dot-bubble-menu.component.scss'],
    imports: [
        TiptapBubbleMenuDirective,
        FormsModule,
        SelectModule,
        DotLinkEditorPopoverComponent,
        DotImageEditorPopoverComponent,
        PopoverModule,
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
    protected readonly cd = inject(ChangeDetectorRef);
    protected readonly domSanitizer = inject(DomSanitizer);
    protected readonly dotMessageService = inject(DotMessageService);
    protected readonly dotContentTypeService = inject(DotContentTypeService);
    protected readonly dotAiService = inject(DotAiService);
    private readonly destroyRef = inject(DestroyRef);

    protected readonly dropdownItem = signal<NodeTypeOption | null>(null);
    protected readonly placeholder = signal<string>('Paragraph');
    protected readonly currentNodeType = signal<string>('paragraph');
    protected readonly showShould = signal<boolean>(true);
    protected readonly showImageMenu = computed(() => this.currentNodeType() === 'dotImage');
    protected readonly showContentMenu = computed(() => this.currentNodeType() === 'dotContent');

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

    protected readonly tippyOptions: Partial<Props> = {
        maxWidth: '100%',
        placement: 'top-start',
        trigger: 'manual',
        onBeforeUpdate: this.onBeforeUpdate.bind(this),
        onClickOutside: this.onClickOutside.bind(this),
        appendTo: (element) => {
            // Append it to the block editor host, so it is outside of the editor container
            const blockEditorHost = element?.parentElement?.parentElement ?? document.body;

            return blockEditorHost;
        },
        popperOptions: {
            modifiers: [
                // This modifier is needed to flip the bubble menu when it is too close to the edge of the screen
                {
                    name: 'animate-flip',
                    options: {
                        fallbackPlacements: ['top', 'bottom']
                    }
                },
                // This modifier adds an attribute to the tippy element to hide it when the reference is hidden
                {
                    name: 'hide',
                    phase: 'main'
                }
            ]
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
    protected goToContentlet() {
        // Validate selection exists before proceeding

        const { contentType, inode } = getContentletDataFromSelection(this.editor());

        if (!contentType) {
            console.warn('contentType is undefined, cannot navigate to contentlet');

            return;
        }

        if (!inode) {
            console.warn('inode is undefined, cannot navigate to contentlet');

            return;
        }

        // Confirm navigation with user to prevent accidental data loss
        if (!confirm(this.dotMessageService.get('message.contentlet.lose.unsaved.changes'))) {
            return;
        }

        // Query content type service to determine editor capabilities and preferences
        this.dotContentTypeService
            .getContentType(contentType)
            .pipe(
                take(1),
                catchError(() => of(null))
            )
            .subscribe((contentTypeInfo: DotCMSContentType | null) => {
                // Determine which editor to use based on feature flag in content type metadata
                const shouldUseOldEditor =
                    !contentTypeInfo?.metadata?.[
                        FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
                    ];

                const titleFallback = this.dotMessageService.get(
                    'message.contentlet.back.to.content'
                );

                // Prepare return navigation data

                // Extract current page title removing any suffix after the dash
                const title = window.parent
                    ? window.parent.document?.title?.split(' - ')?.[0]
                    : document?.title?.split(' - ')?.[0];

                // Store navigation state in localStorage for returning to first editor
                const relationshipReturnValue = {
                    title: title || titleFallback,
                    blockEditorBackUrl: shouldUseOldEditor
                        ? this.generateBackUrl(inode)
                        : window.location.href,
                    inode: this.extractInodeFromUrl(shouldUseOldEditor) // is not needed but I am seeing it, to follow the logic edit_contentlet_basic_properties.jsp
                };

                localStorage.setItem(
                    'dotcms.relationships.relationshipReturnValue',
                    JSON.stringify(relationshipReturnValue)
                );

                // Navigate to the appropriate editor based on feature flag
                if (shouldUseOldEditor) {
                    // Legacy approach - direct page navigation to old editor
                    window.parent.location.href = `/dotAdmin/#/c/content/${inode}`;
                } else {
                    window.parent.location.href = `/dotAdmin/#/content/${inode}`;
                }
            });
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

    /**
     * This is needed because the iframe refresh the inode when switch between languages,
     * and needs to be updated to generete the correct back url.
     *
     * @param {string} contentletInode - The inode to use if no query parameter is found
     * @returns {string} The modified URL with the replaced inode
     */
    private generateBackUrl(contentletInode: string): string {
        const currentUrl = window.parent.location.href;

        // Get inode from query params

        const params = new URLSearchParams(window.location.search);

        const inode = params.get('inode') || contentletInode;

        // Pattern to match the inode in the URL

        const inodePattern = /\/c\/content\/([a-f0-9-]+)/i;

        // Replace the inode in the URL with the inode from query params

        return currentUrl.replace(inodePattern, `/c/content/${inode}`);
    }

    /**
     * Extracts the inode from the current URL based on the editor type.
     *
     * This helper method parses the current URL to extract the inode identifier,
     * handling different URL formats between the legacy and new editor systems.
     * It supports two common DotCMS URL patterns for content:
     *
     * @param {boolean} isOldEditor - Flag indicating whether to extract from parent window (legacy editor)
     *                                or current window (new editor)
     *
     * @returns {string|null} The extracted inode string if found, or null if not found
     *
     * @example
     * // For URL: /dotAdmin/#/content/123abc456def
     * extractInodeFromUrl(false) // returns "123abc456def"
     *
     * @example
     * // For URL: /dotAdmin/#/c/content/789xyz123abc
     * extractInodeFromUrl(true) // returns "789xyz123abc"
     */
    private extractInodeFromUrl(isOldEditor: boolean): string | null {
        // Determine which window location to use based on editor type
        const url = isOldEditor ? window.parent.location.href : window.location.href;

        // Define regex patterns for both URL formats
        // Pattern 1: /dotAdmin/#/content/[inode] - Used by new editor
        const contentPattern = /\/content\/([a-f0-9-]+)/i;
        // Pattern 2: /dotAdmin/#/c/content/[inode] - Used by legacy editor
        const legacyPattern = /\/c\/content\/([a-f0-9-]+)/i;

        // Try matching the new editor pattern first
        const contentMatch = url.match(contentPattern);
        if (contentMatch && contentMatch[1]) {
            return contentMatch[1];
        }

        // Fall back to legacy pattern if new pattern didn't match
        const legacyMatch = url.match(legacyPattern);
        if (legacyMatch && legacyMatch[1]) {
            return legacyMatch[1];
        }

        // Return null if no pattern matched
        return null;
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
