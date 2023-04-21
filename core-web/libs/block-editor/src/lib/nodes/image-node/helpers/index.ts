import { mergeAttributes } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

export const imageLinkElement = (attrs, newAttrs) => {
    const { href = null } = newAttrs;

    return ['a', { href }, imageElement(attrs, newAttrs)];
};

export const imageElement = (attrs, newAttrs) => {
    return ['div', { style: newAttrs.style }, ['img', mergeAttributes(attrs, newAttrs)]];
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
