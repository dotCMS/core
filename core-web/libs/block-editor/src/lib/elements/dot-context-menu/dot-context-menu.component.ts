import { marked } from 'marked';
import { DOMSerializer } from 'prosemirror-model';
import TurndownService from 'turndown';

import { CommonModule } from '@angular/common';
import { Component, computed, input, signal } from '@angular/core';

import { ContextMenuModule } from 'primeng/contextmenu';
import { RippleModule } from 'primeng/ripple';

import { Editor } from '@tiptap/core';

@Component({
    selector: 'dot-editor-context-menu',
    templateUrl: './dot-context-menu.component.html',
    styleUrls: ['./dot-context-menu.component.scss'],
    standalone: true,
    imports: [CommonModule, ContextMenuModule, RippleModule]
})
export class DotContextMenuComponent {
    editor = input.required<Editor>();

    protected readonly target = computed(() => this.editor().view.dom.parentElement);

    private get isMac(): boolean {
        return navigator.platform.toUpperCase().indexOf('MAC') >= 0;
    }

    private readonly hasSelection = signal(false);

    protected readonly items = computed(() => [
        {
            label: 'Cut',
            command: () => this.cutCommand(),
            shortcut: this.getShortcut('⌘X', 'Ctrl+X'),
            disabled: !this.hasSelection()
        },
        {
            label: 'Copy',
            command: () => this.copyCommand(),
            shortcut: this.getShortcut('⌘C', 'Ctrl+C'),
            disabled: !this.hasSelection()
        },
        {
            label: 'Copy as Markdown',
            command: () => this.copyAsMarkdownCommand(),
            disabled: !this.hasSelection()
        },
        { separator: true },
        {
            label: 'Paste',
            command: () => this.pasteCommand(),
            shortcut: this.getShortcut('⌘V', 'Ctrl+V')
        },
        {
            label: 'Paste from Markdown',
            command: () => this.pasteFromMarkdownCommand(),
            shortcut: this.getShortcut('⌘⇧V', 'Ctrl+Shift+V')
        }
    ]);

    private cutCommand() {
        this.editor().commands.focus();
        document.execCommand('cut');
    }

    private copyCommand() {
        this.editor().commands.focus();
        document.execCommand('copy');
    }

    private async copyAsMarkdownCommand() {
        const html = this.getSelectedHtml();
        if (html) {
            const cleanHtml = this.cleanHtmlForMarkdown(html);
            const turndownService = new TurndownService({
                headingStyle: 'atx',
                hr: '---',
                bulletListMarker: '-',
                codeBlockStyle: 'fenced',
                emDelimiter: '_'
            });

            turndownService.addRule('tables', {
                filter: 'table',
                replacement: function (content, node) {
                    const table = node as HTMLTableElement;

                    return '\n\n' + processTable(table) + '\n\n';
                }
            });

            const markdown = turndownService.turndown(cleanHtml);

            try {
                await navigator.clipboard.writeText(markdown);
            } catch (err) {
                console.warn('Failed to copy markdown to clipboard:', err);
            } finally {
                this.editor().commands.focus();
            }
        }
    }

    private pasteCommand() {
        navigator.clipboard.readText().then((text) => {
            this.editor().commands.insertContent(text);
            this.editor().commands.focus();
        });
    }

    private pasteFromMarkdownCommand() {
        navigator.clipboard.readText().then((text) => {
            const html = marked.parse(text);
            this.editor().commands.insertContent(html);
            this.editor().commands.focus();
        });
    }

    private cleanHtmlForMarkdown(html: string): string {
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = html;

        // Remove button elements (like dot-cell-arrow)
        const buttons = tempDiv.querySelectorAll('button');
        buttons.forEach((button) => button.remove());

        // Remove colgroup and col elements
        const colgroups = tempDiv.querySelectorAll('colgroup');
        colgroups.forEach((colgroup) => colgroup.remove());

        // Clean up nested paragraphs in table cells
        const cells = tempDiv.querySelectorAll('td, th');
        cells.forEach((cell) => {
            // If cell only contains one paragraph, replace with its content
            const paragraphs = cell.querySelectorAll('p');
            if (paragraphs.length === 1) {
                const p = paragraphs[0];
                if (p.parentElement === cell) {
                    cell.innerHTML = p.innerHTML;
                }
            }
        });

        // Remove style attributes that might interfere
        const elementsWithStyle = tempDiv.querySelectorAll('[style]');
        elementsWithStyle.forEach((el) => el.removeAttribute('style'));

        return tempDiv.innerHTML;
    }

    private getSelectedHtml() {
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

    private getShortcut(mac: string, pc: string): string {
        return this.isMac ? mac : pc;
    }

    onContextMenuShow() {
        // Update selection signal which will trigger items computed to update
        const hasSelection = !this.editor().view.state.selection.empty;
        this.hasSelection.set(hasSelection);
    }
}

function processTable(table: HTMLTableElement): string {
    const rows = Array.from(table.querySelectorAll('tr'));

    if (rows.length === 0) {
        return '';
    }

    const markdownRows: string[] = [];

    rows.forEach((row, index) => {
        const cells = Array.from(row.querySelectorAll('td, th'));
        const cellContents = cells.map((cell) => {
            return cell.textContent?.trim() || '';
        });

        // Create markdown table row
        const markdownRow = '| ' + cellContents.join(' | ') + ' |';
        markdownRows.push(markdownRow);

        // Add separator after header row (first row with th elements)
        if (index === 0 && row.querySelector('th')) {
            const separator = '| ' + cellContents.map(() => '---').join(' | ') + ' |';
            markdownRows.push(separator);
        }
    });

    return markdownRows.join('\n');
}
