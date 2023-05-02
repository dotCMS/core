import { DOMOutputSpec } from 'prosemirror-model';

import { mergeAttributes } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

const LANGUAGE_ID = 'language_id';

export const imageLinkElement = (attrs, newAttrs): DOMOutputSpec => {

    const { href = null } = newAttrs;

    return ['a', { href }, imageElement(attrs, newAttrs)];
};

export const imageElement = (attrs, newAttrs): DOMOutputSpec => {
    return ['img', mergeAttributes(attrs, newAttrs)];
};

export const addImageLanguageId = (src: string, languageId: number) =>
    src.includes(LANGUAGE_ID) ? src : `${src}?${LANGUAGE_ID}=${languageId}`;

export const getImageAttr = (attrs: DotCMSContentlet | string) => {
    if (typeof attrs === 'string') {
        return { src: attrs };
    }

    const { fileAsset, asset, title, languageId } = attrs;

    return {
        data: attrs,
        src: addImageLanguageId(fileAsset || asset, languageId),
        title: title,
        alt: title
    };
};
