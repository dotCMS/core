import { mergeAttributes } from '@tiptap/core';

export const imageLinkElement = (attrs, newAttrs) => {
    const { href = null } = newAttrs;

    return ['a', { href }, imageElement(attrs, newAttrs)];
};

export const imageElement = (attrs, newAttrs) => {
    return ['img', mergeAttributes(attrs, newAttrs)];
};
