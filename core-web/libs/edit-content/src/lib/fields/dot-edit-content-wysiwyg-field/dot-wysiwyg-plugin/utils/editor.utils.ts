import { DotCMSContentlet } from '@dotcms/dotcms-models';

/**
 * Take it from https://github.com/dotCMS/core/blob/792f5c1e62dacd404ddcf9172eaee7ba3524c4c8/dotCMS/src/main/webapp/html/portlet/ext/contentlet/field/edit_field_js.jsp#L754
 */
export const DEFAULT_IMAGE_URL_PATTERN = '/dA/{shortyId}/{name}?language_id={languageId}';

/**
 * Format the image node to be inserted in the WYSIWYG editor
 *
 * @param {string} pattern
 * @param {DotCMSContentlet} asset
 * @return {*}
 */
export const formatDotImageNode = (pattern: string, asset: DotCMSContentlet) => {
    const src = replaceURLPattern(pattern, asset);

    return (
        `<img src="${src}"\n` +
        `alt="${asset.title}"\n` +
        `data-field-name="${asset.titleImage}"\n` +
        `data-inode="${asset.inode}"\n` +
        `data-identifier="${asset.identifier}"\n` +
        `data-saveas="${asset.title}" />`
    );
};

/**
 * Take it from: https://github.com/dotCMS/core/blob/792f5c1e62dacd404ddcf9172eaee7ba3524c4c8/dotCMS/src/main/webapp/html/portlet/ext/contentlet/field/edit_field_js.jsp#L760
 * @param pattern
 * @param asset
 * @returns
 */
export const replaceURLPattern = (pattern: string, asset: DotCMSContentlet) => {
    const replacements = {
        '{name}': asset.fileName || asset.name,
        '{fileName}': asset.fileName || asset.name,
        '{path}': asset.path,
        '{extension}': asset.extension,
        '{languageId}': asset.languageId.toString(),
        '{hostname}': asset.hostName,
        '{inode}': asset.inode,
        '{hostId}': asset.host,
        '{identifier}': asset.identifier,
        '{id}': asset.identifier,
        '{shortyInode}': asset.inode.replace('-', '').substring(0, 10),
        '{shortyId}': asset.identifier.replace('-', '').substring(0, 10)
    };

    return pattern.replace(/{\w+}/g, (placeholder) => replacements[placeholder] || placeholder);
};
