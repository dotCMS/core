import { DOMOutputSpec } from 'prosemirror-model';

import { mergeAttributes } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

export const imageLinkElement = (attrs, newAttrs): DOMOutputSpec => {
    const { href = null } = newAttrs;

    return ['a', { href }, imageElement(attrs, newAttrs)];
};

export const imageElement = (attrs, newAttrs): DOMOutputSpec => {
    return ['img', mergeAttributes(attrs, newAttrs)];
};

export const getImageAttr = (attrs: DotCMSContentlet | string) => {
    if (typeof attrs === 'string') {
        return { src: attrs };
    }

    const { fileAsset, asset, title } = attrs;

    return {
        data: attrs,
        src: fileAsset || asset,
        title: title,
        alt: title
    };
};
