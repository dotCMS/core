import { marked } from 'marked';
import { DOMSerializer } from 'prosemirror-model';

import { CommonModule } from '@angular/common';
import { Component, computed, input, signal } from '@angular/core';

import { ContextMenuModule } from 'primeng/contextmenu';
import { RippleModule } from 'primeng/ripple';

import { Editor } from '@tiptap/core';

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
    imports: [CommonModule, ContextMenuModule, RippleModule]
})
export class DotContextMenuComponent {
    // === INPUTS ===
    editor = input.required<Editor>();

    // === COMPUTED PROPERTIES ===
    protected readonly target = computed(() => this.editor().view.dom.parentElement);

    protected readonly items = computed(() => this.buildMenuItems());

    // === SIGNALS ===
    private readonly hasSelection = signal(false);

    // === PLATFORM DETECTION ===
    private get platform(): Platform {
        return PLATFORM_PATTERNS.MAC.test(navigator.platform) ? 'mac' : 'pc';
    }

    // === MENU BUILDING ===
    /**
     * Builds the complete menu items array with selection and paste items
     * @returns Array of context menu items
     */
    private buildMenuItems(): ContextMenuItem[] {
        return [
            ...this.buildSelectionMenuItems(),
            { separator: true },
            ...this.buildPasteMenuItems()
        ];
    }

    /**
     * Builds menu items that require text selection (cut, copy, copy as markdown)
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
     * Builds paste-related menu items (paste, paste from markdown)
     * @returns Array of paste menu items
     */
    private buildPasteMenuItems(): ContextMenuItem[] {
        return [
            {
                label: MENU_LABELS.PASTE,
                command: () => this.pasteCommand(),
                shortcut: this.getShortcut(SHORTCUTS.PASTE)
            },
            {
                label: MENU_LABELS.PASTE_MARKDOWN,
                command: () => this.pasteFromMarkdownCommand(),
                shortcut: this.getShortcut(SHORTCUTS.PASTE_MARKDOWN)
            }
        ];
    }

    // === CLIPBOARD COMMANDS ===
    /**
     * Cuts the selected text to clipboard using the browser's cut command
     */
    private cutCommand(): void {
        this.focusEditor();
        document.execCommand('cut');
    }

    /**
     * Copies the selected text to clipboard using the browser's copy command
     */
    private copyCommand(): void {
        this.focusEditor();
        document.execCommand('copy');
    }

    /**
     * Converts selected HTML content to Markdown and copies it to clipboard
     * @returns Promise that resolves when copy operation completes
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

    private async pasteCommand(): Promise<void> {
        try {
            const text = await navigator.clipboard.readText();
            this.editor().commands.insertContent(text);
            this.focusEditor();
        } catch (err) {
            console.warn('Failed to paste content:', err);
        }
    }

    private async pasteFromMarkdownCommand(): Promise<void> {
        try {
            const text = await navigator.clipboard.readText();
            const html = marked.parse(text);
            this.editor().commands.insertContent(html);
            this.focusEditor();
        } catch (err) {
            console.warn('Failed to paste markdown content:', err);
        }
    }

    // === SELECTION UTILITIES ===
    private getSelectedHtml(): string {
        const { view, state } = this.editor();
        const { from, to, empty } = view.state.selection;

        if (empty) {
            return '';
        }

        const selectedDoc = state.doc.cut(from, to);
        const serializer = DOMSerializer.fromSchema(this.editor().schema);
        const fragment = serializer.serializeFragment(selectedDoc.content);
        const tempDiv = document.createElement('div');
        tempDiv.appendChild(fragment);

        return tempDiv.innerHTML;
    }

    // === UTILITY METHODS ===
    private getShortcut(shortcutConfig: { mac: string; pc: string }): string {
        return shortcutConfig[this.platform];
    }

    private focusEditor(): void {
        this.editor().commands.focus();
    }

    // === EVENT HANDLERS ===
    onContextMenuShow(): void {
        const hasSelection = !this.editor().view.state.selection.empty;
        this.hasSelection.set(hasSelection);
    }
}
