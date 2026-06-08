/**
 * Shape of a single row in the Asset list modal.
 *
 * Backed by `GET /api/bundle/{bundleId}/assets` which returns a `List<Map<String,Object>>`
 * (`com.dotcms.rest.BundleResource#getPublishQueueElements`) produced by
 * `PublishQueueElementTransformer`. The transformer emits keys like `type`, `title`,
 * `inode`, `content_type_name`, `language_code`, `country_code`, `operation`, `asset`.
 *
 * This interface narrows that loose map to the fields the UI actually renders.
 */
export interface BundleAssetView {
    id: string;
    title: string;
    type: string;
    state?: string;
    inode?: string;
    contentTypeName?: string;
    languageCode?: string;
    countryCode?: string;
    operation?: number;
}
