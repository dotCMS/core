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

export const getImageAttr = (
    attrs: DotCMSContentlet | string | { url: string; base64: string }
) => {
    if (typeof attrs === 'string') {
        return { src: attrs, data: 'null' };
    }

    if ('url' in attrs) {
        return { src: (attrs as { url: string }).url, data: 'null' };
    }

    if ('base64' in attrs) {
        return {
            src: `data:image/png;base64,${(attrs as { base64: string }).base64}`,
            data: 'null'
        };
    }

    const { fileAsset, asset, title, languageId } = attrs as DotCMSContentlet;

    return {
        data: attrs,
        src: addImageLanguageId(fileAsset || asset, languageId),
        title: title,
        alt: title
    };
};
