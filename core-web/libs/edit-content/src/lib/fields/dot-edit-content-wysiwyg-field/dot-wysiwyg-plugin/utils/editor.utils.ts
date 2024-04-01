import { DotCMSContentlet } from '@dotcms/dotcms-models';

export const formatDotImageNode = (pattern: string, asset: DotCMSContentlet) => {
    const src = replaceURLPattern(pattern, asset);

    return `<img src="${src}"
    alt="${asset.title}"
    data-field-name="${asset.titleImage}"
    data-inode="${asset.inode}"
    data-identifier="${asset.identifier}"
    data-saveas="${asset.title}" />`;
};

/**
 * Take it from: https://github.com/dotCMS/core/blob/792f5c1e62dacd404ddcf9172eaee7ba3524c4c8/dotCMS/src/main/webapp/html/portlet/ext/contentlet/field/edit_field_js.jsp#L760
 * @param pattern
 * @param asset
 * @returns
 */
export const replaceURLPattern = (pattern: string, asset: DotCMSContentlet) => {
    return pattern
        .replace(/{name}/g, asset.fileName || asset.name)
        .replace(/{fileName}/g, asset.fileName || asset.name)
        .replace(/{path}/g, asset.path)
        .replace(/{extension}/g, asset.extension)
        .replace(/{languageId}/g, asset.languageId.toString())
        .replace(/{hostname}/g, asset.hostName)
        .replace(/{hostName}/g, asset.hostName)
        .replace(/{inode}/g, asset.inode)
        .replace(/{hostId}/g, asset.host)
        .replace(/{identifier}/g, asset.identifier)
        .replace(/{id}/g, asset.identifier)
        .replace(/{shortyInode}/g, asset.inode.replace('-', '').substring(0, 10))
        .replace(/{shortyId}/g, asset.identifier.replace('-', '').substring(0, 10));
};
