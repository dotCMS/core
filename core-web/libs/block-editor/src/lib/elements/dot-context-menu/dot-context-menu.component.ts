import { marked } from 'marked';
import { DOMSerializer } from 'prosemirror-model';
import TurndownService from 'turndown';

import { Component, computed, input, OnInit, signal } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ContextMenuModule } from 'primeng/contextmenu';

import { Editor } from '@tiptap/core';

@Component({
    selector: 'dot-editor-context-menu',
    templateUrl: './dot-context-menu.component.html',
    styleUrls: ['./dot-context-menu.component.scss'],
    standalone: true,
    imports: [ContextMenuModule]
})
export class DotContextMenuComponent implements OnInit {
    editor = input.required<Editor>();

    protected readonly items = signal<MenuItem[]>([]);
    protected readonly target = computed(() => this.editor().view.dom.parentElement);

    ngOnInit() {
        this.items.set([
            { label: 'Cut', icon: '', command: () => this.cutCommand() },
            { label: 'Copy', icon: '', command: () => this.copyCommand() },
            { label: 'Copy as Markdown', icon: '', command: () => this.copyAsMarkdownCommand() },
            { label: 'Paste', icon: '', command: () => this.pasteCommand() },
            {
                label: 'Paste from Markdown',
                icon: '',
                command: () => this.pasteFromMarkdownCommand()
            }
        ]);
    }

    private cutCommand() {
        const hasSelection = this.editor().state.selection.content().size > 0;
        if (hasSelection) {
            document.execCommand('cut');
        }
    }

    private copyCommand() {
        const hasSelection = this.editor().state.selection.content().size > 0;
        if (hasSelection) {
            document.execCommand('copy');
        }
    }

    private pasteCommand() {
        navigator.clipboard.readText().then((text) => {
            // console.log(text);
            this.editor().commands.insertContent(text);
        });
    }

    private async copyAsMarkdownCommand() {
        const html = this.getSelectedHtml();
        if (html) {
            const cleanHtml = this.cleanHtmlForMarkdown(html);
            const turndownService = new TurndownService({
                headingStyle: 'atx', // Use # for headers instead of underlines
                hr: '---',
                bulletListMarker: '-',
                codeBlockStyle: 'fenced',
                emDelimiter: '_'
            });

            // Ensure tables are properly converted
            turndownService.addRule('tables', {
                filter: 'table',
                replacement: function (content, node) {
                    const table = node as HTMLTableElement;

                    return '\n\n' + processTable(table) + '\n\n';
                }
            });

            const markdown = turndownService.turndown(cleanHtml);
            // console.log('Markdown:', markdown);
            // console.log('Clean HTML:', cleanHtml);

            // Copy to clipboard
            try {
                await navigator.clipboard.writeText(markdown);
            } catch (err) {
                console.warn('Failed to copy markdown to clipboard:', err);
            }
        }
    }

    private pasteFromMarkdownCommand() {
        navigator.clipboard.readText().then((text) => {
            const html = marked.parse(text);
            this.editor().commands.insertContent(html);
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
