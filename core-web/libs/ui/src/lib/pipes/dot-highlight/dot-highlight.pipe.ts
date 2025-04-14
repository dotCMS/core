import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

/**
 * Pipe to highlight text in a string.
 *
 * @example
 * <div [innerHTML]="text | dotHighlight:search"></div>
 */
@Pipe({
    name: 'dotHighlight',
    standalone: true
})
export class DotHighlightPipe implements PipeTransform {
    constructor(private sanitizer: DomSanitizer) {}

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

            // Wrap matching parts with highlight span
            const highlighted = parts
                .map((part) => {
                    if (part.toLowerCase() === searchStr.toLowerCase()) {
                        return `<span class="highlight">${part}</span>`;
                    }

                    return part;
                })
                .join('');

            return this.sanitizer.bypassSecurityTrustHtml(highlighted);
        } catch (error) {
            console.error('Error in highlight pipe:', error);

            return text;
        }
    }
}
