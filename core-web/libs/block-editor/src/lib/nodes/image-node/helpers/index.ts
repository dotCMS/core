import { mergeAttributes } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

const LANGUAGE_ID = 'language_id';

export const imageLinkElement = (attrs, newAttrs) => {
    const { href = null } = newAttrs;

    return ['a', { href }, imageElement(attrs, newAttrs)];
};

export const imageElement = (attrs, newAttrs) => {
    return ['div', { style: newAttrs.style }, ['img', mergeAttributes(attrs, newAttrs)]];
};

export const addImageLenguageId = (src: string, languageId: number) =>
    src.includes(LANGUAGE_ID) ? src : `${src}?${LANGUAGE_ID}=${languageId}`;

export const getImageAttr = (attrs: DotCMSContentlet | string) => {
    if (typeof attrs === 'string') {
        return { src: attrs };
    }

    const { fileAsset, asset, title, languageId } = attrs;

    return {
        data: attrs,
        src: addImageLenguageId(fileAsset || asset, languageId),
        title: title,
        alt: title
    };
};
