import {
    ChangeDetectionStrategy,
    Component,
    OnDestroy,
    computed,
    effect,
    inject,
    input,
    output
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationService, type TooltipOptions } from 'primeng/api';
import { Select } from 'primeng/select';
import { Tooltip } from 'primeng/tooltip';

import { Editor } from '@tiptap/core';
import { DOMSerializer } from '@tiptap/pm/model';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { EditorToolbarStore } from './editor-toolbar.store';

import { BLOCK_TARGET_KEY } from '../../extensions/selection-preserve.extension';
import { ContentletEditUrlService } from '../../services/contentlet-edit-url.service';
import { EditorModalService } from '../../services/editor-modal.service';
import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorStore } from '../../store/editor.store';
import { writeRelationshipReturnBreadcrumb } from '../../utils/breadcrumb.utils';
import { htmlToMarkdown, markdownToHtml } from '../../utils/markdown.utils';

import type { ContentletEditEvent } from '../../extensions/nodes/contentlet/contentlet.extension';

@Component({
    selector: 'dot-toolbar',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [FormsModule, Select, Tooltip, DotMessagePipe],
    host: {
        role: 'toolbar',
        '[attr.aria-label]': 'toolbarAriaLabel',
        'aria-orientation': 'horizontal',
        class: 'flex flex-wrap items-center gap-0.5 p-1.5 bg-indigo-50 border border-b-0 border-indigo-100 rounded-t-lg',
        '(keydown)': 'onToolbarKeyDown($event)'
    },
    templateUrl: './toolbar.component.html'
})
export class ToolbarComponent implements OnDestroy {
    protected readonly state = inject(EditorToolbarStore);
    protected readonly store = inject(EditorStore);
    private readonly popovers = inject(EditorPopoverService);
    private readonly editorModal = inject(EditorModalService);
    private readonly contentletEditUrl = inject(ContentletEditUrlService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);

    /** Resolved at construction so the host's `[attr.aria-label]` reads a static string. */
    protected readonly toolbarAriaLabel = this.dotMessageService.get(
        'dot.block.editor.toolbar.aria-label'
    );

    readonly editor = input.required<Editor>();
    readonly isFullscreen = input<boolean>(false);

    /**
     * Fullscreen editor shell uses `z-[9998]` on its backdrop; PrimeNG tooltips append to `document.body`
     * with a much lower default z-index, so they render under the overlay. Bump only while fullscreen.
     */
    protected readonly overlayTooltipOptions = computed(
        (): TooltipOptions =>
            this.isFullscreen() ? { tooltipZIndex: '10050' } : { tooltipZIndex: 'auto' }
    );

    readonly fullscreenToggle = output<void>();
    readonly contentletEdit = output<ContentletEditEvent>();

    private cleanupFn: (() => void) | null = null;

    constructor() {
        effect(() => {
            this.cleanupFn?.();
            this.cleanupFn = this.state.connect(this.editor());
        });
    }

    ngOnDestroy(): void {
        this.cleanupFn?.();
    }

    /**
     * Per-PrimeNG-Tooltip delay before any toolbar tooltip appears. Centralized here so
     * the 44 toolbar buttons stay in sync — change the timing in one place.
     */
    protected readonly TOOLTIP_SHOW_DELAY = 350;

    /** Position used by every toolbar tooltip. */
    protected readonly TOOLTIP_POSITION = 'bottom' as const;

    /**
     * Visual divider between toolbar groups. Centralized so every separator stays in lock-step
     * — adjust the spacing/color/height in one place. Used by `<span [class]="DIVIDER_CLASS">`.
     */
    protected readonly DIVIDER_CLASS = 'mx-1 h-6 w-px shrink-0 bg-indigo-200';

    private readonly BTN_BASE =
        'inline-flex items-center justify-center w-8 h-8 rounded-md border border-transparent transition-all cursor-pointer disabled:cursor-not-allowed';

    /**
     * Tailwind compiles utility rules into the stylesheet by utility identity, not by the
     * order they appear in the className attribute. Mixing `hover:text-indigo-700` and
     * `hover:text-white` in the same list lets whichever Tailwind emits last in CSS win,
     * which made the active button's white icon flip to indigo-on-indigo on hover (the
     * icon visibly disappeared). Splitting the inactive and active branches into mutually
     * exclusive class strings — no shared `hover:text-*` — avoids the collision entirely.
     */
    protected btnClass(active: boolean): string {
        return [
            this.BTN_BASE,
            'disabled:text-indigo-300 disabled:hover:bg-transparent disabled:hover:shadow-none',
            active
                ? 'bg-indigo-600 text-white hover:bg-indigo-700 hover:shadow-none'
                : 'hover:bg-white/85 hover:shadow-sm'
        ].join(' ');
    }

    protected readonly blockTypeValue = computed(() => {
        // Order matters: lists / blockquote / codeBlock can contain a paragraph,
        // so we resolve the wrapping block before falling back to paragraph/heading.
        if (this.state.isBulletList()) return 'bulletList';
        if (this.state.isOrderedList()) return 'orderedList';
        if (this.state.isBlockquote()) return 'blockquote';
        if (this.state.isCodeBlock()) return 'codeBlock';
        const level = this.state.headingLevel();
        return level === null ? 'paragraph' : `h${level}`;
    });

    protected readonly selectPt = {
        root: 'bg-white border border-indigo-200 rounded-md text-sm text-indigo-900 hover:border-indigo-300 transition-colors',
        label: '!text-indigo-900',
        dropdown: 'w-7 text-indigo-500',
        panel: 'bg-white border border-indigo-200 rounded-md shadow-lg mt-1',
        list: 'p-1',
        item: 'px-3 py-1.5 text-sm text-slate-700 rounded hover:bg-indigo-50 hover:text-indigo-700 aria-selected:bg-indigo-600 aria-selected:text-white'
    };

    protected readonly blockTypeOptions = computed(() => {
        const msg = (key: string) => this.dotMessageService.get(key);
        const opts: { label: string; value: string }[] = [
            { label: msg('dot.block.editor.toolbar.paragraph'), value: 'paragraph' }
        ];
        for (const level of [1, 2, 3, 4, 5, 6] as const) {
            if (this.store.isAllowed(`heading${level}`)) {
                opts.push({
                    label: msg(`dot.block.editor.toolbar.heading-${level}`),
                    value: `h${level}`
                });
            }
        }
        if (this.store.isAllowed('bulletList')) {
            opts.push({
                label: msg('dot.block.editor.toolbar.bullet-list'),
                value: 'bulletList'
            });
        }
        if (this.store.isAllowed('orderedList')) {
            opts.push({
                label: msg('dot.block.editor.toolbar.ordered-list'),
                value: 'orderedList'
            });
        }
        if (this.store.isAllowed('blockquote')) {
            opts.push({
                label: msg('dot.block.editor.toolbar.blockquote'),
                value: 'blockquote'
            });
        }
        if (this.store.isAllowed('codeBlock')) {
            opts.push({
                label: msg('dot.block.editor.toolbar.code-block'),
                value: 'codeBlock'
            });
        }
        return opts;
    });

    // ── allowedBlocks helpers ────────────────────────────────────────────────

    protected isAllowed(block: string): boolean {
        return this.store.isAllowed(block);
    }

    protected readonly showBlockFormatsGroup = computed(
        () =>
            this.store.isAllowed('bulletList') ||
            this.store.isAllowed('orderedList') ||
            this.store.isAllowed('blockquote') ||
            this.store.isAllowed('codeBlock')
    );

    protected readonly showInsertGroup = computed(
        () =>
            this.store.isAllowed('link') ||
            this.store.isAllowed('image') ||
            this.store.isAllowed('video') ||
            this.store.isAllowed('table') ||
            this.store.isAllowed('emoji') ||
            this.showAssetByUrl()
    );

    /**
     * The "Add asset by URL" popover inserts an image, plain video, or YouTube embed —
     * show the trigger only when at least one of those node types is permitted by
     * the field's allowedBlocks configuration.
     */
    protected readonly showAssetByUrl = computed(
        () =>
            this.store.isAllowed('image') ||
            this.store.isAllowed('video') ||
            this.store.isAllowed('youtube')
    );

    // When an image is selected, the alignment buttons reflect the image's textAlign
    // (defaulting to 'left' when unset, matching paragraph behavior). Otherwise they
    // reflect the standard text-align state from the TextAlign extension.
    protected readonly effectiveAlign = computed(() =>
        this.state.isImageSelected()
            ? (this.state.imageTextAlign() ?? 'left')
            : this.state.textAlign()
    );

    // ── History ──────────────────────────────────────────────────────────────

    protected undo(): void {
        this.editor().chain().focus().undo().run();
    }

    protected redo(): void {
        this.editor().chain().focus().redo().run();
    }

    // ── Block type ───────────────────────────────────────────────────────────

    protected setBlockType(value: string): void {
        const editor = this.editor();
        const chain = editor.chain().focus();
        switch (value) {
            case 'paragraph':
                chain.setParagraph().run();
                return;
            case 'bulletList':
                // `clearNodes()` first so the dropdown converts the active block (e.g. heading)
                // into a list, matching legacy behaviour. Without it, toggleBulletList on a
                // heading is a no-op.
                chain.clearNodes().toggleBulletList().run();
                return;
            case 'orderedList':
                chain.clearNodes().toggleOrderedList().run();
                return;
            case 'blockquote':
                chain.clearNodes().toggleBlockquote().run();
                return;
            case 'codeBlock':
                chain.clearNodes().toggleCodeBlock().run();
                return;
            default: {
                const level = Number(value.replace('h', '')) as 1 | 2 | 3 | 4 | 5 | 6;
                chain.setHeading({ level }).run();
            }
        }
    }

    /** Highlights the cursor's block while the block-type select is open. */
    protected setBlockTargetActive(active: boolean): void {
        const editor = this.editor();
        editor.view.dispatch(editor.state.tr.setMeta(BLOCK_TARGET_KEY, { active }));
    }

    // ── Markdown copy / paste ────────────────────────────────────────────────

    /** Copies the selection (or whole doc if no selection) as Markdown. */
    protected async copyAsMarkdown(): Promise<void> {
        const editor = this.editor();
        const html = this.getSelectedHtmlOrAll(editor);
        if (!html) return;
        try {
            await navigator.clipboard.writeText(htmlToMarkdown(html));
        } catch (err) {
            console.warn('Copy as Markdown failed', err);
        } finally {
            editor.view.focus();
        }
    }

    /** Reads Markdown from the clipboard and inserts it as rich content at the cursor. */
    protected async pasteFromMarkdown(): Promise<void> {
        const editor = this.editor();
        try {
            const text = await navigator.clipboard.readText();
            if (!text) return;
            editor.chain().focus().insertContent(markdownToHtml(text)).run();
        } catch (err) {
            console.warn('Paste from Markdown failed', err);
        }
    }

    /** Returns the selection's HTML, or the entire document's HTML when the selection is empty. */
    private getSelectedHtmlOrAll(editor: Editor): string {
        const { from, to, empty } = editor.state.selection;
        if (empty) return editor.getHTML();
        const slice = editor.state.doc.cut(from, to);
        const fragment = DOMSerializer.fromSchema(editor.schema).serializeFragment(slice.content);
        const div = document.createElement('div');
        div.appendChild(fragment);
        return div.innerHTML;
    }

    // ── Inline marks ─────────────────────────────────────────────────────────

    protected toggleBold(): void {
        this.editor().chain().focus().toggleBold().run();
    }

    protected toggleItalic(): void {
        this.editor().chain().focus().toggleItalic().run();
    }

    protected toggleStrike(): void {
        this.editor().chain().focus().toggleStrike().run();
    }

    protected toggleCode(): void {
        this.editor().chain().focus().toggleCode().run();
    }

    // ── Block formats ────────────────────────────────────────────────────────

    protected toggleBulletList(): void {
        this.editor().chain().focus().toggleBulletList().run();
    }

    protected toggleOrderedList(): void {
        this.editor().chain().focus().toggleOrderedList().run();
    }

    protected toggleBlockquote(): void {
        this.editor().chain().focus().toggleBlockquote().run();
    }

    protected toggleCodeBlock(): void {
        this.editor().chain().focus().toggleCodeBlock().run();
    }

    protected insertHR(): void {
        this.editor().chain().focus().setHorizontalRule().run();
    }

    protected indent(): void {
        this.editor().chain().focus().sinkListItem('listItem').run();
    }

    protected outdent(): void {
        this.editor().chain().focus().liftListItem('listItem').run();
    }

    protected clearFormat(): void {
        this.editor().chain().focus().unsetAllMarks().clearNodes().run();
    }

    // ── Dialog openers ────────────────────────────────────────────────────────

    protected openLinkDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();

        if (this.popovers.isOpen('link')) {
            this.popovers.close();
            return;
        }

        const editor = this.editor();
        const { from, to, empty } = editor.state.selection;
        const btn = event.currentTarget as HTMLElement;

        // Check if cursor/selection is inside an existing link
        const linkMark = editor.state.doc
            .resolve(from)
            .marks()
            .find((m) => m.type.name === 'link');
        const linkEl = linkMark
            ? ((editor.view.domAtPos(from).node as HTMLElement).closest?.(
                  'a[href]'
              ) as HTMLElement | null)
            : null;

        if (linkMark && linkEl) {
            // Edit mode — anchor to the link element itself
            const href = linkMark.attrs['href'] ?? '';
            const displayText = linkEl.textContent?.trim() ?? '';
            const anchorPos = editor.view.posAtDOM(linkEl, 0);

            this.popovers.openLink(() => linkEl.getBoundingClientRect(), {
                initialValues: {
                    href,
                    displayText,
                    target: linkMark.attrs['target'] ?? null,
                    title: linkMark.attrs['title'] ?? null,
                    ariaLabel: linkMark.attrs['aria-label'] ?? null,
                    rel: linkMark.attrs['rel'] ?? null
                },
                linkEl,
                anchorPos
            });
        } else {
            // Insert mode — anchor to the toolbar button
            const selectedText = empty ? '' : editor.state.doc.textBetween(from, to);
            this.popovers.openLink(
                () => btn.getBoundingClientRect(),
                selectedText
                    ? { initialValues: { href: '', displayText: selectedText } }
                    : undefined
            );
        }
    }

    protected openImageDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        this.editorModal.openImagePicker(this.editor());
    }

    protected openVideoDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        this.editorModal.openVideoPicker(this.editor());
    }

    protected openTableDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        if (this.popovers.isOpen('table')) {
            this.popovers.close();
            return;
        }
        const btn = event.currentTarget as HTMLElement;
        this.popovers.open('table', () => btn.getBoundingClientRect());
    }

    protected openAssetByUrlDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        if (this.popovers.isOpen('asset-by-url')) {
            this.popovers.close();
            return;
        }
        const btn = event.currentTarget as HTMLElement;
        this.popovers.open('asset-by-url', () => btn.getBoundingClientRect());
    }

    protected openEmojiPicker(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        const btn = event.currentTarget as HTMLElement;
        this.popovers.toggle('emoji', () => btn.getBoundingClientRect());
    }

    // ── Text alignment ───────────────────────────────────────────────────────

    protected setTextAlign(align: 'left' | 'center' | 'right' | 'justify'): void {
        const editor = this.editor();
        if (this.state.isImageSelected()) {
            // Justify isn't meaningful for an image; mirror the old node's behavior
            if (align === 'justify') return;
            editor.chain().focus().setImageTextAlign(align).run();
            return;
        }
        editor.chain().focus().setTextAlign(align).run();
    }

    // ── Superscript / Subscript ──────────────────────────────────────────────

    protected toggleSuperscript(): void {
        this.editor().chain().focus().unsetSubscript().toggleSuperscript().run();
    }

    protected toggleSubscript(): void {
        this.editor().chain().focus().unsetSuperscript().toggleSubscript().run();
    }

    // ── Edit contentlet ──────────────────────────────────────────────────────

    /**
     * Mirrors the legacy bubble-menu behaviour: warn the user about unsaved changes,
     * resolve the destination URL via {@link ContentletEditUrlService} (handles the
     * legacy vs new content-editor flag per content type), drop a `relationshipReturnValue`
     * breadcrumb in localStorage so the destination editor can navigate back, then push
     * the parent window to the resolved URL. The `contentletEdit` output still fires for
     * hosts that want to observe (analytics, custom logging) — it does not gate navigation.
     */
    protected editContentlet(): void {
        const data = this.state.selectedContentlet();
        if (!data) return;

        this.contentletEdit.emit(data);

        this.confirmationService.confirm({
            message: this.dotMessageService.get('message.contentlet.lose.unsaved.changes'),
            header: this.dotMessageService.get('dot.block.editor.contentlet.edit.confirm.header'),
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: this.dotMessageService.get('dot.common.continue'),
            rejectLabel: this.dotMessageService.get('dot.common.cancel'),
            accept: () => this.navigateToContentEditor(data)
        });
    }

    private navigateToContentEditor(data: ContentletEditEvent): void {
        this.contentletEditUrl
            .resolveEditUrl({ inode: data.inode, contentType: data.contentType })
            .subscribe((url) => {
                writeRelationshipReturnBreadcrumb(data.inode);
                if (window.parent) {
                    window.parent.location.href = url;
                }
            });
    }

    // ── Image text wrap ──────────────────────────────────────────────────────

    protected setImageWrap(value: 'left' | 'right'): void {
        this.editor().chain().focus().setImageTextWrap(value).run();
    }

    // ── Edit image properties ────────────────────────────────────────────────

    protected openImagePropertiesDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        const editor = this.editor();
        if (!editor) return;

        const { from } = editor.state.selection;
        const node = editor.state.doc.nodeAt(from);
        if (!node || node.type.name !== 'dotImage') return;

        const btn = event.currentTarget as HTMLElement;
        this.popovers.openImageProperties(() => btn.getBoundingClientRect(), {
            initialValues: {
                src: node.attrs['src'],
                title: node.attrs['title'] ?? '',
                alt: node.attrs['alt'] ?? ''
            }
        });
    }

    // ── Table actions ────────────────────────────────────────────────────────

    protected tableInsertRowAbove(): void {
        this.editor().chain().focus().addRowBefore().run();
    }

    protected tableInsertRowBelow(): void {
        this.editor().chain().focus().addRowAfter().run();
    }

    protected tableInsertColLeft(): void {
        this.editor().chain().focus().addColumnBefore().run();
    }

    protected tableInsertColRight(): void {
        this.editor().chain().focus().addColumnAfter().run();
    }

    protected tableMerge(): void {
        this.editor().chain().focus().mergeCells().run();
    }

    protected tableSplit(): void {
        this.editor().chain().focus().splitCell().run();
    }

    protected tableToggleRowHeader(): void {
        this.editor().chain().focus().toggleHeaderRow().run();
    }

    protected tableToggleColHeader(): void {
        this.editor().chain().focus().toggleHeaderColumn().run();
    }

    protected tableDeleteRow(): void {
        this.editor().chain().focus().deleteRow().run();
    }

    protected tableDeleteCol(): void {
        this.editor().chain().focus().deleteColumn().run();
    }

    protected tableDeleteTable(): void {
        this.editor().chain().focus().deleteTable().run();
    }

    // ── Keyboard navigation (roving tabindex) ────────────────────────────────

    protected onToolbarKeyDown(event: KeyboardEvent): void {
        if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') return;
        const els = Array.from(
            (event.currentTarget as HTMLElement).querySelectorAll<HTMLElement>(
                'button:not([disabled]), select'
            )
        );
        const idx = els.indexOf(document.activeElement as HTMLElement);
        if (idx === -1) return;
        event.preventDefault();
        const next =
            event.key === 'ArrowRight'
                ? (idx + 1) % els.length
                : (idx - 1 + els.length) % els.length;
        els[next]?.focus();
    }
}
