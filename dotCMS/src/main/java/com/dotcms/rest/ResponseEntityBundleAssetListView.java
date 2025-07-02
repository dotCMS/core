package com.dotcms.rest;

import java.util.List;
import java.util.Map;

/**
 * Entity View for bundle asset list responses.
 * Contains list of publish queue element assets with their metadata.
 * 
 * Each asset map contains:
 * - type: Asset type (content, language, etc.)
 * - title: Asset title or display name
 * - inode: Asset inode identifier  
 * - content_type_name: Content type name (for contentlets)
 * - language_code: Language code (for languages)
 * - country_code: Country code (for languages)
 * - operation: Publish operation type
 * - asset: Asset identifier
 * - isHtmlPage: Boolean indicating if content is an HTML page
 * 
 * @author Steve Bolton
 */
public class ResponseEntityBundleAssetListView extends ResponseEntityView<List<Map<String, Object>>> {
    public ResponseEntityBundleAssetListView(final List<Map<String, Object>> entity) {
        super(entity);
    }
}