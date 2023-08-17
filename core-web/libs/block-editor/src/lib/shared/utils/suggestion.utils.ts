import { SafeUrl, ɵDomSanitizerImpl } from '@angular/platform-browser';

import { DotMenuItem } from '@dotcms/block-editor';

// Assets
import {
    codeIcon,
    headerIcons,
    lineIcon,
    olIcon,
    pIcon,
    quoteIcon,
    ulIcon
} from '../components/suggestions/suggestion-icons';

const domSanitizer = new ɵDomSanitizerImpl(document);

const headings: DotMenuItem[] = [...Array(6).keys()].map((level) => {
    const size = level + 1;

    return {
        label: `Heading ${size}`,
        icon: sanitizeUrl(headerIcons[level] || ''),
        id: `heading${size}`,
        attributes: { level: size }
    };
});

const image: DotMenuItem[] = [
    {
        label: 'Image',
        icon: 'image',
        id: 'image'
    },
    {
        label: 'Video',
        icon: 'movie',
        id: 'video'
    }
];

const table: DotMenuItem[] = [
    {
        label: 'Table',
        icon: 'table_view',
        id: 'table'
    }
];

const paragraph: DotMenuItem = {
    label: 'Paragraph',
    icon: sanitizeUrl(pIcon),
    id: 'paragraph'
};

const list: DotMenuItem[] = [
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

const block: DotMenuItem[] = [
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
        id: 'horizontalRule'
    }
];

export const getEditorBlockOptions = () => {
    return (
        suggestionOptions
            // get all blocks except the Paragraph
            .filter(({ id }) => id != paragraph.id)
            .map(({ label, id }) => ({ label, code: id }))
            .sort((a, b) => a.label.localeCompare(b.label))
    );
};

export function sanitizeUrl(url: string): SafeUrl {
    return domSanitizer.bypassSecurityTrustUrl(url);
}

export const suggestionOptions: DotMenuItem[] = [
    ...image,
    ...headings,
    ...table,
    ...list,
    ...block,
    paragraph
];

export const tableChangeToItems: DotMenuItem[] = [...headings, paragraph, ...list];

export const SuggestionPopperModifiers = [
    {
        name: 'flip',
        options: {
            fallbackPlacements: ['top']
        }
    },
    {
        name: 'preventOverflow',
        options: {
            altAxis: false,
            tether: false
        }
    }
];

export const CONTENT_SUGGESTION_ID = 'dotContent';

const FORBIDDEN_CHANGE_TO_BLOCKS = {
    horizontalRule: true,
    table: true,
    image: true
};

export const changeToItems: DotMenuItem[] = [
    ...suggestionOptions.filter((item) => !FORBIDDEN_CHANGE_TO_BLOCKS[item.id])
];
