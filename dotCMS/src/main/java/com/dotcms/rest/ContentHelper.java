package com.dotcms.rest;

import com.dotcms.repackage.javax.ws.rs.DefaultValue;
import com.dotcms.repackage.javax.ws.rs.QueryParam;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.viewtools.content.util.ContentUtils;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.liferay.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;
import static com.dotmarketing.util.WebKeys.DOTCMS_PAGINATION_LINKS;
import static com.dotmarketing.util.WebKeys.DOTCMS_PAGINATION_ROWS;

/**
 * Content Helper for {@link ContentResource}
 *
 * @author jsanca
 */
// TODDO: the PaginationUtil needs an design change in order to support other implementations.
// so by the moment this class has copy paste some methods that will be removed as soon as the Paginator can deal
// with more generic cases
class ContentHelper {

    public static final ContentHelper INSTANCE = new ContentHelper();

    public static final int FIRST_PAGE_INDEX = 1; // TODO: remove me
    private static final String TM_DATE = "tm_date";
    private final int perPageDefault; // TODO: remove me
    private final int nLinks;  // TODO: remove me
    private final static int DEFAULT_PAGINATION_SIZE = 10;
    private final static int DEFAULT_DOTCMS_PAGINATION_LINKS = 5;
    private static final String LINK_TEMPLATE = "<{URL}>;rel=\"{relValue}\"";
    private static final String FIRST_REL_VALUE = "first";
    private static final String LAST_REL_VALUE = "last";
    private static final String PREV_REL_VALUE = "prev";
    private static final String NEXT_REL_VALUE = "next";
    private static final String PAGE_REL_VALUE = "x-page";
    public static  final String FILTER = "filter";
    public static  final String PAGE = "page";
    public static  final String PER_PAGE = "per_page";
    public static  final String ORDER_BY = "orderby";
    public static  final String DIRECTION = "direction";
    private static final String LINK_HEADER_NAME = "Link";
    private static final String PAGINATION_PER_PAGE_HEADER_NAME = "X-Pagination-Per-Page";
    private static final String PAGINATION_CURRENT_PAGE_HEADER_NAME = "X-Pagination-Current-Page";
    private static final String PAGINATION_MAX_LINK_PAGES_HEADER_NAME = "X-Pagination-Link-Pages";
    private static final String PAGINATION_TOTAL_ENTRIES_HEADER_NAME = "X-Pagination-Total-Entries";

    private static final String URL_TEMPLATE;

    static{
        URL_TEMPLATE = StringUtil.format(
                "{baseURL}?{filter}={filterValue}&{page}={pageValue}&{perPage}={perPageValue}&{direction}={directionValue}&{orderBy}={orderByValue}",
                map(
                        "filter", FILTER,
                        "page", PAGE,
                        "perPage", PER_PAGE,
                        "direction", DIRECTION,
                        "orderBy", ORDER_BY
                )
        );
    }

    ContentHelper () {

        this.perPageDefault = Config.getIntProperty(DOTCMS_PAGINATION_ROWS, DEFAULT_PAGINATION_SIZE);
        this.nLinks = Config.getIntProperty(DOTCMS_PAGINATION_LINKS, DEFAULT_DOTCMS_PAGINATION_LINKS);
    }

    ContentRelatedResult getContentRelated(
            final InitDataObject initData,
            final HttpServletRequest request,
            final String relationshipName,
            final String contentletIdentifier,
            final String filter,
            final boolean pullParents,
            final int page,
            final int perPage,
            final String orderbyParam,
            final String direction) throws DotSecurityException, JSONException, DotDataException {

        final User        user    = initData.getUser();
        final HttpSession session = request.getSession();
        final boolean adminMode   = session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null;
        final boolean previewMode = session.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null && adminMode;
        final boolean editMode    = session.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null && adminMode;
        final boolean editOrPreviewMode = previewMode || editMode;
        final int pageValue       = page == 0 ? FIRST_PAGE_INDEX : page;
        final int perPageValue    = perPage == 0 ? perPageDefault : perPage;
        final int offset          = getMinIndex (pageValue, perPageValue);
        final String sort            = getSort (orderbyParam, direction);
        final PaginatedArrayList<Contentlet> relatedContents = ContentUtils.pullRelated(relationshipName,
                contentletIdentifier, addDefaultsToQuery(filter, request, editOrPreviewMode),
                pullParents, offset, perPage, sort, user, (String) session.getAttribute(TM_DATE));
        final long totalRecords   =(null != relatedContents)?relatedContents.getTotalResults():0;
        final String linkHeaderValue = getHeaderValue(request.getRequestURL().toString(), filter, pageValue, perPageValue,
                totalRecords, orderbyParam, OrderDirection.valueOf(direction.toUpperCase()));

        return new ContentRelatedResult (relatedContents,
                map(
                        LINK_HEADER_NAME, linkHeaderValue,
                        PAGINATION_PER_PAGE_HEADER_NAME, perPageValue,
                        PAGINATION_CURRENT_PAGE_HEADER_NAME, pageValue,
                        PAGINATION_MAX_LINK_PAGES_HEADER_NAME, nLinks,
                        PAGINATION_TOTAL_ENTRIES_HEADER_NAME, totalRecords
                )
        );
    }

    // this could be adapt to the Paginator.
    private String getSort(final String orderbyParam, final String direction) {

        if (orderbyParam == null) {

            return null;
        }

        final StringBuilder stringBuilder = new StringBuilder();
        final String [] querySortFields = orderbyParam.split(",");
        for (int i = 0; i < querySortFields.length; ++i) {

            stringBuilder.append(querySortFields[i]).append(" ").append(direction.toLowerCase());
            if (i < querySortFields.length -1) {

                stringBuilder.append(",");
            }
        }

        return stringBuilder.toString();
    }

    /**
     * Get the minimum pagination element index
     * @param currentPage The current page
     * @param perPage The max amount of element per page
     * @return minimum pagination element index
     */
    private int getMinIndex(final int currentPage, final int perPage) { // TODO: remove me
        return (currentPage - 1) * perPage;

    }

    /**
     * Return the valur for the Link header according tis RFC:
     *
     * https://tools.ietf.org/html/rfc5988#page-6
     *
     * The sintax to build each URL is the follow:
     *
     * [urlBase]?filter=[filter]&page=[page]&perPage=[perPage]&archived=[showArchived]&direction=[direction]&orderBy=[orderBy]
     *
     * The real parameter could hve the follow values:  next, prev, last, fisrt and x-page.
     *
     * For more information, you can see:
     *
     * https://developer.github.com/v3/#pagination
     *
     * @param urlBase
     * @param filter
     * @param page
     * @param perPage
     * @param totalRecords
     * @param orderBy
     * @param direction
     * @return
     */ // TODO: remove me
    private static String getHeaderValue(String urlBase, String filter, int page, int perPage,
                                         long totalRecords, String orderBy, OrderDirection direction) {
        final List<String> links = new ArrayList<>();

        links.add(StringUtil.format(LINK_TEMPLATE, map(
                "URL", getUrl(urlBase, filter, FIRST_PAGE_INDEX, perPage, orderBy, direction),
                "relValue", FIRST_REL_VALUE
        )));

        int lastPage = (int) (Math.ceil((double) totalRecords/perPage));
        links.add(StringUtil.format(LINK_TEMPLATE, map(
                "URL", getUrl(urlBase, filter, lastPage, perPage, orderBy, direction),
                "relValue", LAST_REL_VALUE
        )));

        links.add(StringUtil.format(LINK_TEMPLATE, map(
                "URL", getUrl(urlBase, filter, -1, perPage, orderBy, direction),
                "relValue", PAGE_REL_VALUE
        )));

        int next = page + 1;
        if (next <= lastPage){
            links.add(StringUtil.format(LINK_TEMPLATE, map(
                    "URL", getUrl(urlBase, filter, next, perPage, orderBy, direction),
                    "relValue", NEXT_REL_VALUE
            )));
        }

        int prev = page - 1;
        if (prev > 0){
            links.add(StringUtil.format(LINK_TEMPLATE, map(
                    "URL", getUrl(urlBase, filter, prev, perPage, orderBy, direction),
                    "relValue", PREV_REL_VALUE
            )));
        }

        return String.join(",", links);
    }

    /**
     *  Build each URL for the Link header.
     *  The sintax to build each URL is the follow:
     *
     * [urlBase]?filter=[filter]&page=[page]&perPage=[perPage]&direction=[direction]&orderBy=[orderBy]
     *
     * @param urlBase
     * @param page
     * @param perPage
     * @param direction
     * @return
     */ // TODO: remove me
    private static String getUrl(String urlBase, String filter, int page, int perPage,
                                 String orderBy, OrderDirection direction){

        Map<String, String> params = map(
                "baseURL",
                urlBase,
                "perPageValue",
                String.valueOf(perPage)
        );

        params.put("pageValue", (page != -1)?String.valueOf(page): StringPool.BLANK);
        params.put("filterValue", (null != filter)?filter: StringPool.BLANK);
        params.put("directionValue", UtilMethods.isSet(direction) ? direction.toString() : StringUtils.EMPTY);
        params.put("orderByValue", UtilMethods.isSet(orderBy) ? orderBy.toString()  : StringUtils.EMPTY);

        return StringUtil.format(URL_TEMPLATE, params);
    }

    private String addDefaultsToQuery(final String query, final HttpServletRequest request,
                                      final boolean editOrPreviewMode) {

        final StringBuilder queryBuilder = new StringBuilder();

        if (query != null) {

            queryBuilder.append(query);

            if (!query.contains("languageId")) {

                queryBuilder.append(" +languageId:" +
                        WebAPILocator.getLanguageWebAPI().getLanguage(request).getId());
            }

            if (!(query.contains("live:") || query.contains("working:"))) {

                queryBuilder.append((editOrPreviewMode) ? " +working:true " : " +live:true ");
            }

            if (!UtilMethods.contains(query, "deleted:")) {

                queryBuilder.append(" +deleted:false ");
            }
        }

        return queryBuilder.toString();
    } // addDefaultsToQuery.


    protected class ContentRelatedResult {

        private final List<Contentlet> relatedContents;
        private final Map<String, Object> headersMap;

        public ContentRelatedResult(final List<Contentlet> relatedContents, final Map<String, Object> headersMap) {

            this.relatedContents = relatedContents;
            this.headersMap      = headersMap;
        }

        public List<Contentlet> getRelatedContents() {
            return relatedContents;
        }

        public Map<String, Object> getHeadersMap() {
            return headersMap;
        }
    }
} // E:O:F:ContentHelper.
