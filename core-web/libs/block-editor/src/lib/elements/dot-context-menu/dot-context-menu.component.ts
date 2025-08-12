import { marked } from 'marked';
import { DOMSerializer } from 'prosemirror-model';

import { CommonModule } from '@angular/common';
import { Component, computed, input, signal, viewChild } from '@angular/core';

import { ContextMenu, ContextMenuModule } from 'primeng/contextmenu';
import { RippleModule } from 'primeng/ripple';

import { Editor } from '@tiptap/core';

import { DotMessagePipe } from '@dotcms/ui';

import { MENU_LABELS, PLATFORM_PATTERNS, SHORTCUTS } from './context-menu.constants';
import { ContextMenuItem, Platform } from './context-menu.interfaces';
import { htmlToMarkdown } from './markdown.utils';

/**
 * Context menu component for the dot editor that provides clipboard operations
 * and markdown conversion functionality.
 *
 * @example
 * ```html
 * <dot-editor-context-menu [editor]="editorInstance" />
 * ```
 */
@Component({
    selector: 'dot-editor-context-menu',
    templateUrl: './dot-context-menu.component.html',
    styleUrls: ['./dot-context-menu.component.scss'],
    standalone: true,
    imports: [CommonModule, ContextMenuModule, RippleModule, DotMessagePipe]
})
export class DotContextMenuComponent {
    $editor = input.required<Editor>({ alias: 'editor' });
    $contextMenu = viewChild(ContextMenu);

    protected readonly target = computed(() => this.$editor().view.dom.parentElement);
    protected readonly items = computed(() => this.buildMenuItems());
    private readonly hasSelection = signal(false);

    private get platform(): Platform {
        return PLATFORM_PATTERNS.MAC.test(navigator.platform) ? 'mac' : 'pc';
    }

    /**
     * Event handler called when the context menu is about to be shown
     * Updates the selection state to enable/disable selection-dependent menu items
     */
    protected onContextMenuShow(): void {
        const hasSelection = !this.$editor().view.state.selection.empty;
        this.hasSelection.set(hasSelection);
    }

    /**
     * Keep the context menu open while executing item commands (e.g., Firefox paste prompt).
     * Manually hides the menu after the action completes.
     */
    protected async onItemClick(event: MouseEvent, item: ContextMenuItem): Promise<void> {
        if (item.disabled || item.separator) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();
        this.stopImmediateIfSupported(event);

        try {
            await item.command?.();
        } finally {
            this.$contextMenu()?.hide();
        }
    }

    private stopImmediateIfSupported(event: MouseEvent): void {
        const extendedEvent = event as MouseEvent & { stopImmediatePropagation?: () => void };
        extendedEvent.stopImmediatePropagation?.();
    }

    /**
     * Builds the complete menu items array combining selection and paste items
     * @returns Array of all available context menu items
     */
    private buildMenuItems(): ContextMenuItem[] {
        if (this.hasSelection()) {
            return [...this.buildSelectionMenuItems(), ...this.buildPasteMenuItems()];
        }

        return this.buildPasteMenuItems();
    }

    /**
     * Builds menu items that require text selection (cut, copy, copy as markdown)
     * These items are disabled when no text is selected
     * @returns Array of selection-based menu items
     */
    private buildSelectionMenuItems(): ContextMenuItem[] {
        const hasSelection = this.hasSelection();

        return [
            {
                label: MENU_LABELS.CUT,
                command: () => this.cutCommand(),
                shortcut: this.getShortcut(SHORTCUTS.CUT),
                disabled: !hasSelection
            },
            {
                label: MENU_LABELS.COPY,
                command: () => this.copyCommand(),
                shortcut: this.getShortcut(SHORTCUTS.COPY),
                disabled: !hasSelection
            },
            {
                label: MENU_LABELS.COPY_MARKDOWN,
                command: () => this.copyAsMarkdownCommand(),
                disabled: !hasSelection
            }
        ];
    }

    /**
     * Builds paste-related menu items with different formatting options
     * @returns Array of paste menu items including:
     *   - Paste (with formatting preserved)
     *   - Paste without format (plain text only)
     *   - Paste from Markdown (converts markdown to HTML)
     */
    private buildPasteMenuItems(): ContextMenuItem[] {
        return [
            {
                label: MENU_LABELS.PASTE,
                command: () => this.pasteCommand(),
                shortcut: this.getShortcut(SHORTCUTS.PASTE)
            },
            {
                label: MENU_LABELS.PASTE_WITHOUT_FORMAT,
                command: () => this.pasteWithoutFormatCommand(),
                shortcut: this.getShortcut(SHORTCUTS.PASTE_WITHOUT_FORMAT)
            },
            {
                label: MENU_LABELS.PASTE_MARKDOWN,
                command: () => this.pasteFromMarkdownCommand()
            }
        ];
    }

    /**
     * Cuts the selected text to clipboard using the browser's native cut command
     * Focuses the editor first to ensure the selection is active
     */
    private cutCommand(): void {
        this.focusEditor();
        document.execCommand('cut');
    }

    /**
     * Copies the selected text to clipboard using the browser's native copy command
     * Focuses the editor first to ensure the selection is active
     */
    private copyCommand(): void {
        this.focusEditor();
        document.execCommand('copy');
    }

    /**
     * Converts selected HTML content to Markdown format and copies it to clipboard
     * This allows users to copy rich content as markdown syntax
     * @returns Promise that resolves when the copy operation completes
     * @throws Logs warning if clipboard operation fails
     */
    private async copyAsMarkdownCommand(): Promise<void> {
        const html = this.getSelectedHtml();
        if (!html) return;

        try {
            const markdown = htmlToMarkdown(html);
            await navigator.clipboard.writeText(markdown);
        } catch (err) {
            console.warn('Failed to copy markdown to clipboard:', err);
        } finally {
            this.focusEditor();
        }
    }

    /**
     * Pastes clipboard content with formatting preserved
     * Attempts to read HTML content first, falls back to plain text if unavailable
     * @returns Promise that resolves when the paste operation completes
     * @throws Logs warning and falls back to plain text paste if HTML paste fails
     */
    private async pasteCommand(): Promise<void> {
        try {
            const { html, text } = await this.getHtmlOrPlainFromClipboard();

            if (html) {
                this.$editor().commands.insertContent(html);
                this.focusEditor();
                return;
            }

            if (text) {
                this.$editor().commands.insertContent(text, {
                    parseOptions: { preserveWhitespace: 'full' }
                });
                this.focusEditor();
                return;
            }
        } catch {
            console.warn(
                '[Block Editor] Paste failed using clipboard.read(); attempting plain text as fallback'
            );
        }
    }

    /**
     * Pastes clipboard content as plain text, stripping all formatting
     * Preserves whitespace structure but removes HTML tags and styling
     * @returns Promise that resolves when the paste operation completes
     * @throws Logs warning if clipboard read operation fails
     */
    private async pasteWithoutFormatCommand(): Promise<void> {
        try {
            const text = await navigator.clipboard.readText();
            this.$editor().commands.insertContent(text, {
                parseOptions: { preserveWhitespace: 'full' }
            });
            this.focusEditor();
        } catch (err) {
            console.warn(
                '[Block Editor] Paste failed using clipboard.readText(); attempting plain text as fallback',
                err
            );
        }
    }

    /**
     * Pastes clipboard content treating it as Markdown and converting to HTML
     * Reads plain text from clipboard, parses as markdown, then inserts as HTML
     * @returns Promise that resolves when the paste operation completes
     * @throws Logs warning if clipboard read or markdown parsing fails
     */
    private async pasteFromMarkdownCommand(): Promise<void> {
        try {
            const text = await navigator.clipboard.readText();
            const html = marked.parse(text);
            this.$editor().commands.insertContent(html);
            this.focusEditor();
        } catch (err) {
            console.warn(
                '[Block Editor] Paste failed using clipboard.readText(); attempting markdown as fallback',
                err
            );
        }
    }

    /**
     * Extracts the currently selected content as HTML string
     * Uses ProseMirror's DOMSerializer to convert the selection to HTML
     * @returns HTML string of selected content, or empty string if no selection
     */
    private getSelectedHtml(): string {
        const { view, state } = this.$editor();
        const { from, to, empty } = view.state.selection;

        if (empty) {
            return '';
        }

        const selectedDoc = state.doc.cut(from, to);
        const serializer = DOMSerializer.fromSchema(this.$editor().schema);
        const fragment = serializer.serializeFragment(selectedDoc.content);
        const tempDiv = document.createElement('div');
        tempDiv.appendChild(fragment);

        return tempDiv.innerHTML;
    }

    /**
     * Gets the appropriate keyboard shortcut string for the current platform
     * @param shortcutConfig Configuration object with mac and pc shortcut strings
     * @returns Platform-specific shortcut string (e.g., "⌘C" on Mac, "Ctrl+C" on PC)
     */
    private getShortcut(shortcutConfig: { mac: string; pc: string }): string {
        return shortcutConfig[this.platform];
    }

    /**
     * Focuses the editor to ensure it's ready for operations
     * Used after clipboard operations to maintain editor focus
     */
    private focusEditor(): void {
        this.$editor().commands.focus();
    }

    /**
     * Reads the clipboard once and returns either HTML (preferred) or plain text if available.
     * This avoids triggering multiple browser confirmation prompts.
     */
    private async getHtmlOrPlainFromClipboard(): Promise<{ html?: string; text?: string }> {
        const clipboardItems = await navigator.clipboard.read();
        let fallbackText: string | undefined;

        for (const clipboardItem of clipboardItems) {
            if (clipboardItem.types.includes('text/html')) {
                const htmlBlob = await clipboardItem.getType('text/html');
                const html = await htmlBlob.text();
                return { html };
            }

            if (clipboardItem.types.includes('text/plain') && fallbackText === undefined) {
                const textBlob = await clipboardItem.getType('text/plain');
                fallbackText = await textBlob.text();
            }
        }

        return fallbackText ? { text: fallbackText } : {};
    }
}
