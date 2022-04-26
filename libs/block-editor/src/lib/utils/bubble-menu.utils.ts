import { isTextSelection } from '@tiptap/core';
import { ShouldShowProps } from '@dotcms/block-editor';

/**
 * Determine when the bubble menu can or cannot be displayed.
 *
 * @param {ShouldShowProps} { editor, state, from, to }
 * @return {*}
 */
export const shouldShowBubbleMenu = ({ editor, state, from, to }: ShouldShowProps) => {
    const { doc, selection } = state;
    const { empty } = selection;

    // Current selected node
    const node = editor.state.doc.nodeAt(editor.state.selection.from);

    // Sometime check for `empty` is not enough.
    // Doubleclick an empty paragraph returns a node size of 2.
    // So we check also for an empty text size.
    const isEmptyTextBlock = !doc.textBetween(from, to).length && isTextSelection(state.selection);

    // If it's empty or the current node is type dotContent, it will not open.
    if (!editor.isFocused || empty || isEmptyTextBlock || node?.type.name == 'dotContent') {
        return false;
    }

    return true;
};

export const getNodePosition = (node: HTMLElement, type: string): DOMRect => {
    const img = node.getElementsByTagName('img')[0];
    // If is a dotImage Node, get the image position
    if (type === 'dotImage' && img) {
        return img.getBoundingClientRect();
    }
    return node.getBoundingClientRect();
};

export const isListNode = (editor): boolean => {
    return editor.isActive('bulletList') || editor.isActive('orderedList');
};

/* Bubble Menu Items*/
export const bubbleMenuItems = [
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
        active: false
    }
];

export const bubbleMenuImageItems = [
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
