import { Props } from 'tippy.js';

// Assets
import {
    codeIcon,
    headerIcons,
    lineIcon,
    olIcon,
    pIcon,
    quoteIcon,
    ulIcon,
    listStarsIcon,
    mountsStarsIcon
} from '../components/suggestions/suggestion-icons';
import { DotMenuItem } from '../components/suggestions/suggestions.component';

const headings: DotMenuItem[] = [...Array(6).keys()].map((level) => {
    const size = level + 1;

    return {
        label: `Heading ${size}`,
        icon: headerIcons[level] || '',
        id: `heading${size}`,
        attributes: { level: size }
    };
});

const dotContentet: DotMenuItem = {
    label: 'Contentlet',
    icon: 'receipt',
    id: 'dotContent'
};

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
    },
    {
        label: 'Audio',
        icon: 'audiotrack',
        id: 'audio'
    }
];

const table: DotMenuItem[] = [
    {
        label: 'Table',
        icon: 'table_view',
        id: 'table'
    }
];

const grid: DotMenuItem[] = [
    {
        label: 'Grid (2 columns)',
        icon: 'grid_view',
        id: 'gridBlock'
    }
];

const paragraph: DotMenuItem = {
    label: 'Paragraph',
    icon: pIcon,
    id: 'paragraph'
};

const list: DotMenuItem[] = [
    {
        label: 'List Ordered',
        icon: olIcon,
        id: 'orderedList'
    },
    {
        label: 'List Unordered',
        icon: ulIcon,
        id: 'bulletList'
    }
];

const block: DotMenuItem[] = [
    {
        label: 'AI Content',
        icon: listStarsIcon,
        id: 'aiContentPrompt'
    },
    {
        label: 'AI Image',
        icon: mountsStarsIcon,
        id: 'aiImagePrompt'
    },
    {
        label: 'Blockquote',
        icon: quoteIcon,
        id: 'blockquote'
    },
    {
        label: 'Code Block',
        icon: codeIcon,
        id: 'codeBlock'
    },
    {
        label: 'Horizontal Line',
        icon: lineIcon,
        id: 'horizontalRule'
    }
];

export const getEditorBlockOptions = () => {
    return (
        [...suggestionOptions, dotContentet]
            // get all blocks except the Paragraph
            .filter(({ id }) => id != paragraph.id)
            .map(({ label, id }) => ({ label, code: id }))
            .sort((a, b) => a.label.localeCompare(b.label))
    );
};

export const suggestionOptions: DotMenuItem[] = [
    ...image,
    ...headings,
    ...table,
    ...grid,
    ...list,
    ...block,
    paragraph
];

export const tableChangeToItems: DotMenuItem[] = [...headings, paragraph, ...list];

export const SuggestionPopperModifiers = [
    {
        name: 'animate-flip',
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
    image: true,
    video: true,
    audio: true,
    gridBlock: true
};

export const changeToItems: DotMenuItem[] = [
    ...suggestionOptions.filter((item) => !FORBIDDEN_CHANGE_TO_BLOCKS[item.id])
];

export const clearFilter = function ({ type, editor, range, suggestionKey, ItemsType }) {
    const queryRange = {
        to: range.to + suggestionKey.getState(editor.view.state).query?.length,
        from: type === ItemsType.BLOCK ? range.from : range.from + 1
    };
    editor.chain().deleteRange(queryRange).run();
};

export const BASIC_TIPPY_OPTIONS: Partial<Props> = {
    duration: [250, 0],
    interactive: true,
    maxWidth: 'none',
    trigger: 'manual',
    placement: 'bottom-start',
    hideOnClick: 'toggle',
    popperOptions: {
        modifiers: [
            {
                name: 'animate-flip',
                options: { fallbackPlacements: ['top-start'] }
            }
        ]
    }
};
