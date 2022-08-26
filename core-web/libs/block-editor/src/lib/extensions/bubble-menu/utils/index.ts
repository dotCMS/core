import { isTextSelection } from '@tiptap/core';
import { BubbleMenuItem, ShouldShowProps, LINK_FORM_PLUGIN_KEY } from '@dotcms/block-editor';

const hideBubbleMenuOn = {
    dotContent: true
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

    // Current selected node
    const node = editor.state.doc.nodeAt(editor.state.selection.from);

    // Sometime check for `empty` is not enough.
    // Doubleclick an empty paragraph returns a node size of 2.
    // So we check also for an empty text size.
    const isEmptyTextBlock = !doc.textBetween(from, to).length && isTextSelection(state.selection);

    // If it's empty or the current node is type dotContent, it will not open.
    if (
        !isOpen &&
        (!view.hasFocus() || empty || isEmptyTextBlock || hideBubbleMenuOn[node?.type.name])
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
    const img = node?.getElementsByTagName('img')[0];
    // console.log("HOLA")
    // If is a image Node, get the image position
    if (type === 'image' && img) {
        return img.getBoundingClientRect();
    }

    return node.getBoundingClientRect();
};

export const isListNode = (editor): boolean => {
    return editor.isActive('bulletList') || editor.isActive('orderedList');
};

/* Bubble Menu Items*/
export const bubbleMenuItems: Array<BubbleMenuItem> = [
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
        active: false,
        divider: true
    },
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
        active: false,
        divider: true
    },
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

export const bubbleMenuImageItems: Array<BubbleMenuItem> = [
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
        text: 'Properties',
        markAction: 'properties',
        active: false
    }
];

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
