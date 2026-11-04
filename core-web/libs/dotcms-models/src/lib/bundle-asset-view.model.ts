/**
 * Shape of a single row in the Asset list / Bundle Details modal.
 *
 * Backed by `GET /api/bundle/{bundleId}/assets` which returns a `List<Map<String,Object>>`
 * (`com.dotcms.rest.BundleResource#getPublishQueueElements`) produced by
 * `PublishQueueElementTransformer`. The transformer emits keys exactly as named below
 * (snake_case for the multi-word ones — these field names mirror the wire format).
 *
 * `asset` is the universal identifier — always set by the transformer for every row
 * type (contentlet, language, template, container, etc.). This is the id we pass to
 * `DELETE /v1/bundles/{bundleId}/assets` when removing an asset from a bundle.
 *
 * This interface narrows that loose map to the fields the UI actually renders.
 */
export interface BundleAssetView {
    asset: string;
    title: string;
    type: string;
    inode?: string;
    /** The content type's display name, for showing. */
    content_type_name?: string;
    /** The content type's variable, the name the content type API knows it by. */
    content_type_variable?: string;
    language_code?: string;
    country_code?: string;
    operation?: number;
}
