import { BundleAssetView } from '@dotcms/dotcms-models';

/** A bundle asset that opens in the content editor: a contentlet naming its inode and its type. */
export type EditableContentletAsset = BundleAssetView & {
    inode: string;
    content_type_name: string;
};

/**
 * Whether a bundle asset opens in the side panel's content editor (#38303). Only contentlets do,
 * and only with an inode and a content type; templates, languages, containers and the like stay
 * plain rows.
 *
 * @param asset A row of `/api/bundle/{id}/assets`.
 */
export function isEditableContentlet(asset: BundleAssetView): asset is EditableContentletAsset {
    return asset.type === 'contentlet' && !!asset.inode && !!asset.content_type_name;
}
