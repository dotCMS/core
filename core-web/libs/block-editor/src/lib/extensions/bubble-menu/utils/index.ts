import { isTextSelection } from '@tiptap/core';

import { ImageNode } from '../../../nodes';
import { findParentNode } from '../../../shared';
import { LINK_FORM_PLUGIN_KEY } from '../../bubble-link-form/bubble-link-form.extension';
import { BubbleMenuItem, HideBubbleMenuExtensions, ShouldShowProps } from '../models';

const hideBubbleMenuOn: HideBubbleMenuExtensions = {
    tableCell: true,
    table: true,
    youtube: true,
    dotVideo: true,
    aiContent: true,
    loader: true
};

/**
 * Determine when the bubble menu can or cannot be displayed.
 *
 * @param {ShouldShowProps} { editor, state, from, to }
 * @return {*}
 */
export const shouldShowBubbleMenu = ({ editor, state, from, to }: ShouldShowProps) => {
    const { doc, selection } = state;
    const { view } = editor;
    const { empty } = selection;

    const { isOpen, openOnClick } = LINK_FORM_PLUGIN_KEY.getState(state);

    // Current selected node and parent.
    const node = editor.state.doc.nodeAt(editor.state.selection.from);
    const parentNode = findParentNode(editor.state.selection.$from);

    // Sometime check for `empty` is not enough.
    // Doubleclick an empty paragraph returns a node size of 2.
    // So we check also for an empty text size.
    const isEmptyTextBlock = !doc.textBetween(from, to).length && isTextSelection(state.selection);
    const isTextInsideTable = node?.type.name === 'text' && parentNode?.type.name === 'table';

    // Is a text node inside the table
    if (isTextInsideTable && !isEmptyTextBlock) {
        return true;
    }

    // If it's empty or the parent and node itself is part of the hideBubbleMenuOn , it will not open.
    if (
        !isOpen &&
        (!view.hasFocus() || empty || isEmptyTextBlock || hideBubbleMenuOn[parentNode?.type.name]
            ? true
            : hideBubbleMenuOn[node?.type.name])
    ) {
        return false;
    }

    if (isOpen && openOnClick) {
        return false;
    }

    return true;
};

/**
 *  Check if a text is a valid url
 *
 * @param {string} nodeText
 * @return {*}
 */
export const isValidURL = (nodeText: string) => {
    const pattern = new RegExp(
        '^(https?:\\/\\/)?' + // protocol
            '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|' + // domain name
            '((\\d{1,3}\\.){3}\\d{1,3}))' + // OR ip (v4) address
            '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*' + // port and path
            '(\\?[;&a-z\\d%_.~+=-]*)?' + // query string
            '(\\#[-a-z\\d_]*)?$',
        'i'
    ); // fragment locator

    return !!pattern.test(nodeText);
};

export const getNodePosition = (node: HTMLElement, type: string): DOMRect => {
    if (type === ImageNode.name) {
        const img = node.getElementsByTagName('img')[0];

        // If is a image Node, get the image position
        return img?.getBoundingClientRect() || node.getBoundingClientRect();
    }

    return node.getBoundingClientRect();
};

export const getNodeCoords = (node: HTMLElement, type: string): DOMRect => {
    if (type === ImageNode.name && node?.firstElementChild) {
        return node.firstElementChild.getBoundingClientRect();
    }

    return node.getBoundingClientRect();
};

export const setBubbleMenuCoords = ({ viewCoords, nodeCoords, padding }): DOMRect => {
    const { top: nodeTop, bottom: nodeBottom } = nodeCoords;
    const { top: viewTop, bottom: viewBottom } = viewCoords;

    const isTopOverflow = Math.ceil(nodeTop - viewTop) < padding;
    const isBottomOverflow = Math.ceil(viewBottom - nodeBottom) < padding;

    return {
        ...nodeCoords.toJSON(),
        top: isTopOverflow && isBottomOverflow ? viewTop + padding : nodeTop
    };
};

export const isListNode = (editor): boolean => {
    return editor.isActive('bulletList') || editor.isActive('orderedList');
};

// TODO: Remove JSPRedirectFn when Edit Content JSP is removed.
/**
 * JSPRedirectFn
 *
 * A variable that represents the function to redirect to go to a related content page.
 *
 * @global
 */
const JSPRedirectFn = (window as any).rel_BlogblogComment_PeditRelatedContent;

const textMarks: Array<BubbleMenuItem> = [
    {
        icon: 'format_bold',
        markAction: 'bold',
        active: false
    },
    {
        icon: 'format_underlined',
        markAction: 'underline',
        active: false
    },
    {
        icon: 'format_italic',
        markAction: 'italic',
        active: false
    },
    {
        icon: 'strikethrough_s',
        markAction: 'strike',
        active: false
    },
    {
        icon: 'superscript',
        markAction: 'superscript',
        active: false
    },
    {
        icon: 'subscript',
        markAction: 'subscript',
        active: false,
        divider: true
    }
];

const alignmentMarks: Array<BubbleMenuItem> = [
    {
        icon: 'format_align_left',
        markAction: 'left',
        active: false
    },
    {
        icon: 'format_align_center',
        markAction: 'center',
        active: false
    },
    {
        icon: 'format_align_right',
        markAction: 'right',
        active: false
    },
    {
        icon: 'format_align_justify',
        markAction: 'justify',
        active: false,
        divider: true
    }
];

/* Bubble Menu Items*/
const bubbleMenuDefaultItems: Array<BubbleMenuItem> = [
    ...textMarks,
    ...alignmentMarks,
    {
        icon: 'format_list_bulleted',
        markAction: 'bulletList',
        active: false
    },
    {
        icon: 'format_list_numbered',
        markAction: 'orderedList',
        active: false
    },
    {
        icon: 'format_indent_decrease',
        markAction: 'outdent',
        active: false
    },
    {
        icon: 'format_indent_increase',
        markAction: 'indent',
        active: false,
        divider: true
    },
    {
        icon: 'link',
        markAction: 'link',
        active: false,
        divider: true
    },
    {
        icon: 'format_clear',
        markAction: 'clearAll',
        active: false,
        divider: true
    },
    {
        icon: 'delete',
        markAction: 'deleteNode',
        active: false
    }
];

const imageOptions: Array<BubbleMenuItem> = [
    ...alignmentMarks,
    {
        icon: 'link',
        markAction: 'link',
        active: false,
        divider: true
    },
    {
        text: 'Properties',
        markAction: 'properties',
        active: false,
        divider: true
    },
    {
        icon: 'delete',
        markAction: 'deleteNode',
        active: false
    }
];

/* Table text node Items*/
const tableOptions: Array<BubbleMenuItem> = [
    ...textMarks,
    ...alignmentMarks,
    {
        icon: 'link',
        markAction: 'link',
        active: false,
        divider: true
    },
    {
        icon: 'format_clear',
        markAction: 'clearAll',
        active: false,
        divider: true
    },
    {
        icon: 'delete',
        markAction: 'deleteNode',
        active: false
    }
];

const dotContentOptions: Array<BubbleMenuItem> = [
    {
        icon: 'delete',
        markAction: 'deleteNode',
        active: false
    }
];

export const getBubbleMenuItem = (nodeType = ''): Array<BubbleMenuItem> => {
    switch (nodeType) {
        case 'dotImage':
            return imageOptions;

        case 'dotContent':
            // TODO: Remove JSPRedirectFn when Edit Content JSP is removed.

            if (JSPRedirectFn) {
                return [
                    ...dotContentOptions,
                    {
                        icon: 'edit',
                        markAction: 'goToContentlet',
                        active: false
                    }
                ];
            }

            return dotContentOptions;

        case 'table':
            return tableOptions;

        default:
            return bubbleMenuDefaultItems;
    }
};

// Tippy Modifiers
export const popperModifiers = [
    {
        name: 'offset',
        options: {
            offset: [0, 5]
        }
    },
    {
        name: 'flip',
        options: {
            fallbackPlacements: ['bottom-start', 'top-start']
        }
    },
    {
        name: 'preventOverflow',
        options: {
            altAxis: true,
            tether: true
        }
    }
];
