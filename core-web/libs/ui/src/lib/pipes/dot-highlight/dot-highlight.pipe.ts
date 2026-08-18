import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

/**
 * Pipe that adds highlighting to text by wrapping matching search terms with a 'highlight' CSS class.
 * This is primarily used to visually emphasize search matches, making it easy for users to identify
 * where their search terms appear in the text.
 *
 * @param text The original text to search within
 * @param search The search term to highlight
 * @returns HTML string with matching text wrapped in highlight spans
 *
 * @example
 * <!-- Highlights all occurrences of 'search' in 'text' -->
 * <div [innerHTML]="text | dotHighlight:search"></div>
 *
 * <!-- Example with actual values -->
 * <div [innerHTML]="'Hello World' | dotHighlight:'World'"></div>
 * <!-- Output: Hello <span class="highlight">World</span> -->
 */
const HTML_ESCAPES: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
};

/** `&` first is implicit in the character class: each character is replaced exactly once. */
function escapeHtml(value: string): string {
    return value.replace(/[&<>"']/g, (character) => HTML_ESCAPES[character]);
}

@Pipe({
    name: 'dotHighlight'
})
export class DotHighlightPipe implements PipeTransform {
    private sanitizer = inject(DomSanitizer);

    transform(text: string, search: string | null): SafeHtml {
        if (!text) return '';
        if (!search) return text;

        try {
            // Ensure we're working with strings
            const textStr = String(text);
            const searchStr = String(search);

            // Escape special characters in the search string
            const searchRegex = searchStr.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, '\\$&');

            // Split text into parts that match and don't match the search term
            const parts = textStr.split(new RegExp(`(${searchRegex})`, 'gi'));

            // Wrap matching parts with highlight span. Every part is escaped first: the text is
            // author-supplied (content names, keys, descriptions) and the result below is handed
            // to `bypassSecurityTrustHtml`, so anything not escaped here would be injected as
            // live markup. Escaping leaves the `<span>` we add as the only real HTML.
            const highlighted = parts
                .map((part) => {
                    const escaped = escapeHtml(part);

                    if (part.toLowerCase() === searchStr.toLowerCase()) {
                        return `<span class="highlight">${escaped}</span>`;
                    }

                    return escaped;
                })
                .join('');

            return this.sanitizer.bypassSecurityTrustHtml(highlighted);
        } catch (error) {
            console.error('Error in highlight pipe:', error);

            return text;
        }
    }
}
