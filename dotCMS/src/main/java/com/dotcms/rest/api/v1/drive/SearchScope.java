package com.dotcms.rest.api.v1.drive;

/**
 * Which fields of a document a Content Drive search term is matched against.
 *
 * <p>Not to be confused with a <i>browse</i> scope, which says <b>where</b> you are browsing. The
 * two are independent: a browse scope says where you are, a search scope says how a search reads
 * what is there. Both travel on the same request, which is why neither is called simply
 * "scope".</p>
 *
 * <p>{@link #ALL_FIELDS} is the default, so a request that omits the scope is processed exactly as
 * it was before this type existed.</p>
 *
 * @see AbstractQueryFilters#searchScope()
 */
public enum SearchScope {

    /**
     * The term is matched against every indexed field of the document — body copy, Story Block
     * content and metadata included. The historical behavior of the Content Drive search box.
     */
    ALL_FIELDS,

    /**
     * The term is matched against the contentlet title only. A contentlet whose term occurrence is
     * confined to body copy, Story Block content or metadata is not returned.
     *
     * <p>Folder and link name matching is unaffected: those never reach the search index and are
     * narrowed on their own name in both scopes.</p>
     */
    TITLE

}
