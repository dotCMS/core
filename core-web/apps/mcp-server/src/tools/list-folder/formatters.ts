import type { Contentlet } from '../../types/search';

function getDotcmsBaseUrl(): string | undefined {
    const url = process.env.DOTCMS_URL;
    try {
        if (url) {
            // Validate URL format
            // eslint-disable-next-line no-new
            new URL(url);
            return url.replace(/\/+$/, '');
        }
        // eslint-disable-next-line no-empty
    } catch {}
    return undefined;
}

export function formatListFolderResponse(
    folderPath: string,
    items: Contentlet[],
    meta: { limit: number; offset: number; total: number }
): string {
    const baseUrl = getDotcmsBaseUrl();
    const lines: string[] = [];

    if (items.length === 0) {
        lines.push(`No items found in folder "${folderPath}".`);
        lines.push('');
        lines.push('Tips:');
        lines.push('- Check the folder path (e.g., "/", "/images", "/docs").');
        lines.push('- Try adjusting limit/offset for pagination.');
        return lines.join('\n');
    }

    lines.push(
        `Found ${items.length} item(s) in "${folderPath}" (showing ${meta.offset}–${
            meta.offset + items.length - 1
        } of ${meta.total}).`
    );

    lines.push('');
    lines.push('Items:');
    for (const c of items) {
        const titleOrUrl = c.title || c.url || c.identifier;
        const url = baseUrl && c.url ? `${baseUrl}${c.url}` : c.url || '';
        const entry = [
            `- ${titleOrUrl}`,
            c.contentType ? `type=${c.contentType}` : '',
            c.hostName ? `site=${c.hostName}` : '',
            url ? `url=${url}` : ''
        ]
            .filter(Boolean)
            .join(' | ');
        lines.push(entry);
    }

    lines.push('');
    lines.push('Next steps:');
    lines.push(`- Use offset=${meta.offset + items.length} to view the next page.`);
    lines.push('- Use the content_search tool for advanced filters.');

    return lines.join('\n');
}
