import { marked } from 'marked';
import TurndownService from 'turndown';

/**
 * Turndown options. Matches the legacy block-editor's MARKDOWN_CONFIG so output is consistent
 * across editors when both are in use during the migration.
 */
const MARKDOWN_CONFIG: TurndownService.Options = {
    headingStyle: 'atx',
    hr: '---',
    bulletListMarker: '-',
    codeBlockStyle: 'fenced',
    emDelimiter: '_'
};

/**
 * Renders an HTML table as a Markdown pipe-table.
 *
 * We override turndown's default table handling because TipTap/ProseMirror generates HTML with
 * nested `<p>` inside cells, `<colgroup>` elements, and other structures the default converter
 * doesn't handle cleanly. This walks rows directly and extracts `textContent`.
 */
function processTable(table: HTMLTableElement): string {
    const rows = Array.from(table.querySelectorAll('tr'));
    if (rows.length === 0) return '';

    const markdownRows: string[] = [];
    rows.forEach((row, index) => {
        const cells = Array.from(row.querySelectorAll('td, th'));
        const cellContents = cells.map((cell) => cell.textContent?.trim() ?? '');
        markdownRows.push('| ' + cellContents.join(' | ') + ' |');

        if (index === 0 && row.querySelector('th')) {
            markdownRows.push('| ' + cellContents.map(() => '---').join(' | ') + ' |');
        }
    });

    return markdownRows.join('\n');
}

/**
 * Strips editor chrome that shouldn't appear in copied Markdown:
 * - Buttons (e.g. table cell arrows)
 * - Embedded contentlet blocks (no clean Markdown representation)
 * - `<colgroup>` / `<col>` (purely structural)
 * - Inline `style` attributes
 *
 * Also unwraps single-paragraph table cells so turndown sees flat text instead of `<p>` nesting.
 */
function cleanHtmlForMarkdown(html: string): string {
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = html;

    tempDiv.querySelectorAll('button').forEach((el) => el.remove());
    // dotContent node uses `data-type="dot-content"` (legacy used `data-dotCMS-contentlet`).
    tempDiv.querySelectorAll('[data-type="dot-content"]').forEach((el) => el.remove());
    tempDiv.querySelectorAll('colgroup').forEach((el) => el.remove());

    tempDiv.querySelectorAll('td, th').forEach((cell) => {
        const paragraphs = cell.querySelectorAll('p');
        if (paragraphs.length === 1 && paragraphs[0].parentElement === cell) {
            cell.innerHTML = paragraphs[0].innerHTML;
        }
    });

    tempDiv.querySelectorAll('[style]').forEach((el) => el.removeAttribute('style'));

    return tempDiv.innerHTML;
}

function createTurndownService(): TurndownService {
    const service = new TurndownService(MARKDOWN_CONFIG);
    service.addRule('tables', {
        filter: 'table',
        replacement: (_content, node) => '\n\n' + processTable(node as HTMLTableElement) + '\n\n'
    });
    return service;
}

/** Converts editor HTML to Markdown. */
export function htmlToMarkdown(html: string): string {
    return createTurndownService().turndown(cleanHtmlForMarkdown(html));
}

/** Converts Markdown to HTML for insertion via TipTap's `insertContent`. */
export function markdownToHtml(md: string): string {
    // marked.parse returns string in sync mode (default). Cast to satisfy `string | Promise<string>` union.
    return marked.parse(md, { async: false }) as string;
}
