import { BundleAssetView } from '@dotcms/dotcms-models';

/**
 * Groups contentlet assets by content type. Non-contentlet rows (templates,
 * languages, containers, etc.) and rows without an inode are skipped — those
 * are not linkable to the content editor.
 *
 * The result is consumed by both `dot-publishing-queue-select-bundle-dialog`
 * and `dot-publishing-queue-asset-list-dialog` to fan out edit-URL resolution
 * per distinct content type instead of per asset.
 */
export function groupContentletAssetsByType(
    assets: BundleAssetView[]
): Map<string, BundleAssetView[]> {
    const groups = new Map<string, BundleAssetView[]>();
    for (const asset of assets) {
        if (asset.type !== 'contentlet' || !asset.inode) {
            continue;
        }
        const key = asset.content_type_name ?? '';
        groups.set(key, [...(groups.get(key) ?? []), asset]);
    }
    return groups;
}
