import { COMMENT_TINYMCE } from './dot-edit-content-wysiwyg-field.constant';

const escapeRegExp = (string: string) => {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
};

export const CountOccurrences = (str: string, searchStr: string) => {
    const escapedSearchStr = escapeRegExp(searchStr);

    return (str.match(new RegExp(escapedSearchStr, 'gi')) || []).length;
};

export const shouldUseDefaultEditor = (content: unknown): boolean => {
    return (
        !content ||
        typeof content !== 'string' ||
        content.trim() === '' ||
        content.trim() === COMMENT_TINYMCE
    );
};
