import TurndownService from 'turndown';

import { MARKDOWN_CONFIG } from './context-menu.constants';

/**
 * Processes an HTML table element and converts it to Markdown format
 * @param table - The HTML table element to process
 * @returns Markdown representation of the table
 */
export function processTable(table: HTMLTableElement): string {
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

/**
 * Cleans HTML content for better Markdown conversion
 * @param html - The HTML string to clean
 * @returns Cleaned HTML string
 */
export function cleanHtmlForMarkdown(html: string): string {
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = html;

    // Remove button elements (like dot-cell-arrow)
    const buttons = tempDiv.querySelectorAll('button');
    buttons.forEach((button) => button.remove());

    // Remove elements with data-dotCMS-contentlet attribute (custom contentlet blocks)
    const contentletBlocks = tempDiv.querySelectorAll('[data-dotCMS-contentlet]');
    contentletBlocks.forEach((block) => block.remove());

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

/**
 * Creates and configures a TurndownService instance for HTML to Markdown conversion
 * @returns Configured TurndownService instance
 */
export function createTurndownService(): TurndownService {
    const turndownService = new TurndownService(MARKDOWN_CONFIG);

    // Add custom rule for table processing
    // NOTE: We override TurndownService's default table handling because rich text editors
    // like TipTap/ProseMirror generate complex HTML with nested <p> tags, <colgroup> elements,
    // and other structures that the default converter doesn't handle cleanly. Our custom
    // processTable function ensures proper header detection and clean text extraction.
    turndownService.addRule('tables', {
        filter: 'table',
        replacement: function (_content, node) {
            const table = node as HTMLTableElement;

            return '\n\n' + processTable(table) + '\n\n';
        }
    });

    return turndownService;
}

/**
 * Converts HTML content to Markdown
 * @param html - The HTML string to convert
 * @returns Markdown representation of the HTML
 */
export function htmlToMarkdown(html: string): string {
    const cleanHtml = cleanHtmlForMarkdown(html);
    const turndownService = createTurndownService();

    return turndownService.turndown(cleanHtml);
}
