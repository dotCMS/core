import {
    codeIcon,
    headerIcons,
    lineIcon,
    olIcon,
    pIcon,
    quoteIcon,
    ulIcon
} from '../extensions/components/suggestions/suggestion-icons';
import { SafeUrl, ɵDomSanitizerImpl } from '@angular/platform-browser';
import { DotMenuItem } from '../extensions/components/suggestions/suggestions.component';

const domSanitizer = new ɵDomSanitizerImpl(document);

const headings = [...Array(3).keys()].map((level) => {
    const size = level + 1;
    return {
        label: `Heading ${size}`,
        icon: sanitizeUrl(headerIcons[level]),
        id: `heading${size}`,
        attributes: { level: size }
    };
});

const paragraph = [
    {
        label: 'Paragraph',
        icon: sanitizeUrl(pIcon),
        id: 'paragraph'
    }
];

const list = [
    {
        label: 'List Ordered',
        icon: sanitizeUrl(olIcon),
        id: 'orderedList'
    },
    {
        label: 'List Unordered',
        icon: sanitizeUrl(ulIcon),
        id: 'bulletList'
    }
];

const block = [
    {
        label: 'Blockquote',
        icon: sanitizeUrl(quoteIcon),
        id: 'blockquote'
    },
    {
        label: 'Code Block',
        icon: sanitizeUrl(codeIcon),
        id: 'codeBlock'
    },
    {
        label: 'Horizontal Line',
        icon: sanitizeUrl(lineIcon),
        id: 'horizontalLine'
    }
];

function sanitizeUrl(url: string): SafeUrl {
    return domSanitizer.bypassSecurityTrustUrl(url);
}

export const suggestionOptions: DotMenuItem[] = [...headings, ...paragraph, ...list, ...block];
