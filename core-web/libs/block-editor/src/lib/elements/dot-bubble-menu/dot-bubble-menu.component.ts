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
    ViewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';

import { DropdownModule } from 'primeng/dropdown';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { take } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

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
        OverlayPanelModule,
        DotMessagePipe
    ],
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBubbleMenuComponent {
    @ViewChild('editorModal') editorModal: EditorModalDirective;
    @ViewChild('bubbleMenu', { read: ElementRef }) bubbleMenuRef: ElementRef<HTMLElement>;
    @ViewChild('linkModal') linkModal: DotLinkEditorPopoverComponent;
    @ViewChild('imageModal') imageModal: DotImageEditorPopoverComponent;

    readonly editor = input.required<Editor>();
    protected readonly cd = inject(ChangeDetectorRef);
    protected readonly domSanitizer = inject(DomSanitizer);
    protected readonly dotMessageService = inject(DotMessageService);
    protected readonly dotContentTypeService = inject(DotContentTypeService);

    protected readonly dropdownItem = signal<NodeTypeOption | null>(null);
    protected readonly placeholder = signal<string>('Paragraph');
    protected readonly showShould = signal<boolean>(true);
    protected readonly showImageMenu = signal<boolean>(false);
    protected readonly showContentMenu = signal<boolean>(false);

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
        placement: 'top-start' as Placement,
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
    protected goToContentlet() {
        // Validate selection exists before proceeding

        // Get the selection
        const selection = this.editor()?.state?.selection;

        // Get the slice of the selection
        const slice = selection?.content?.();

        // Get the fragment of the slice
        const fragment = slice?.content;

        // Get the first node of the fragment
        const selectionNode = fragment?.content?.[0];

        if (!selectionNode) {
            console.warn('Selection node is undefined, cannot navigate to contentlet');

            return;
        }

        // Extract content type information from the selected node

        const { data = {} } = selectionNode?.attrs || {};

        const { contentType, inode } = data || {};

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
            .pipe(take(1))
            .subscribe((contentTypeInfo) => {
                // Determine which editor to use based on feature flag in content type metadata

                const shouldUseOldEditor =
                    !contentTypeInfo?.metadata?.[
                        FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
                    ];

                // Prepare return navigation data

                // Extract current page title removing any suffix after the dash

                const title = window.parent
                    ? window.parent.document.title.split(' - ')[0]
                    : document.title.split(' - ')[0] ||
                      this.dotMessageService.get('message.contentlet.back.to.content');

                // Store navigation state in localStorage for returning to first editor
                const relationshipReturnValue = {
                    title,
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
        this.checkIfShowBubbleMenu();
        this.checkIfShowImageMenu();
        this.checkIfIsContentlet();
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

    private checkIfIsContentlet() {
        const currentNodeType = getCurrentNodeType(this.editor());
        this.showContentMenu.set(currentNodeType === 'dotContent');
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
}
