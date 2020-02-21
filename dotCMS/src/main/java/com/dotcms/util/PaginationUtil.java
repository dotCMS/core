package com.dotcms.util;

import static com.dotcms.util.CollectionsUtils.map;
import static com.dotmarketing.util.WebKeys.DOTCMS_PAGINATION_LINKS;
import static com.dotmarketing.util.WebKeys.DOTCMS_PAGINATION_ROWS;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.Paginator;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.liferay.util.StringUtil;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;

/**
 * Utility class to the pagination elements and filter
 *
 * @author oswaldogallango
 *
 */
public class PaginationUtil {

	public static final int FIRST_PAGE_INDEX = 1;

	public static final String FILTER = "filter";
	public static final String PAGE = "page";
	public static final String PER_PAGE = "per_page";
	public static final String ORDER_BY = "orderby";
	public static final String DIRECTION = "direction";

	private static final String LINK_HEADER_NAME = "Link";
	private static final String PAGINATION_PER_PAGE_HEADER_NAME = "X-Pagination-Per-Page";
	private static final String PAGINATION_CURRENT_PAGE_HEADER_NAME = "X-Pagination-Current-Page";
	private static final String PAGINATION_MAX_LINK_PAGES_HEADER_NAME = "X-Pagination-Link-Pages";
	private static final String PAGINATION_TOTAL_ENTRIES_HEADER_NAME = "X-Pagination-Total-Entries";
	public static final String PAGE_VALUE_TEMPLATE = "pageValue";

	private static final String LINK_TEMPLATE = "<{URL}>;rel=\"{relValue}\"";

	private static final String  FIRST_REL_VALUE = "first";
	private static final String  LAST_REL_VALUE = "last";
	private static final String  PREV_REL_VALUE = "prev";
	private static final String  NEXT_REL_VALUE = "next";
	private static final String  PAGE_REL_VALUE = "x-page";

	private final Paginator paginator;
	private final int perPageDefault;
	private final int nLinks;

	public PaginationUtil(final Paginator paginator){
		this.paginator = paginator;
		perPageDefault = Config.getIntProperty(DOTCMS_PAGINATION_ROWS, 10);
		nLinks = Config.getIntProperty(DOTCMS_PAGINATION_LINKS, 5);
	}

	/**
	 * Get the minimum pagination element index
	 * @param currentPage The current page
	 * @param perPage The max amount of element per page
	 * @return minimum pagination element index
	 */
	private int getMinIndex(int currentPage, int perPage){
		return (currentPage - 1) * perPage;

	}

	/**
	 * Get the maximum pagination element index
	 * @param currentPage The current page
	 * @param perPage The max amount of element per page
	 * @return maximum pagination element index
	 */
	private int getMaxIndex(int currentPage, int perPage){
		return perPage * currentPage;
	}

	public Response getPage(final HttpServletRequest req, final User user, final String filter, final int pageParam, final int perPageParam) {
		return getPage(req, user, filter, pageParam, perPageParam, StringUtils.EMPTY, (OrderDirection) null, null);
	}

	public Response getPage(final HttpServletRequest req, final User user, final String filter, final int pageParam,
							final int perPageParam, final String orderBy, final String direction) {
		return getPage(req, user, filter, pageParam, perPageParam, orderBy,
				OrderDirection.valueOf(direction), null);
	}

	public Response getPage(final HttpServletRequest req, final User user, final String filter, final int pageParam,
							final int perPageParam, final Map<String, Object>  extraParams) {
		return getPage(req, user, filter, pageParam, perPageParam, null, null, extraParams);
	}

	/**
	 * Return a pagination's response
	 *
	 * @param req
	 * @param user Login User
	 * @param filter
	 * @param page Page to return
	 * @param perPage Number of items by page
	 * @param orderBy Field name to order by
	 * @param direction Order direction (ASC, DESC)
	 * @return
	 */
	public Response getPage(final HttpServletRequest req, final User user, final String filter, final int page,
							final int perPage, final String orderBy, final OrderDirection direction,
							final Map<String, Object> extraParams) {

		final int pageValue = page == 0 ? FIRST_PAGE_INDEX : page;
		final int perPageValue = perPage == 0 ? perPageDefault : perPage;
		final int minIndex = getMinIndex(pageValue, perPageValue);

		final String sanitizefilter = filter != null ? SQLUtil.sanitizeParameter(filter) : "";

		final Map<String, Object> params = getParameters(sanitizefilter, orderBy, direction, extraParams);

		PaginatedArrayList items = paginator.getItems(user, perPageValue, minIndex, params);

		if (!UtilMethods.isSet(items)){
			items = new PaginatedArrayList();
		}

		final long totalRecords = items.getTotalResults();

		final String linkHeaderValue = getHeaderValue(req.getRequestURI(), sanitizefilter, pageValue, perPageValue,
				totalRecords, orderBy, direction, extraParams);

		return Response.
				ok(new ResponseEntityView((Object) items))
				.header(LINK_HEADER_NAME, linkHeaderValue)
				.header(PAGINATION_PER_PAGE_HEADER_NAME, perPageValue)
				.header(PAGINATION_CURRENT_PAGE_HEADER_NAME, pageValue)
				.header(PAGINATION_MAX_LINK_PAGES_HEADER_NAME, nLinks)
				.header(PAGINATION_TOTAL_ENTRIES_HEADER_NAME, totalRecords)
				.build();
	}

	/**
	 * Typically our Pagination is directly wrapped into a ResponseEntityView
	 * But if we had a more complex situation where the paginated item are meant to live within a nested level in our json object?
	 * This Method comes to solve that problem.
	 * Return a pagination's response
	 *
	 * @param req
	 * @param user Login User
	 * @param filter
	 * @param page Page to return
	 * @param perPage Number of items by page
	 * @param orderBy Field name to order by
	 * @param direction Order direction (ASC, DESC)
	 * @param function This is function must feed the items into whatever json we want to use.
	 * @return
	 */
	public <T,R> Response getPage(final HttpServletRequest req, final User user, final String filter, final int page,
			final int perPage, final String orderBy, final OrderDirection direction,
			final Map<String, Object> extraParams,
			final Function<PaginatedArrayList<T>, R> function) {

		final int pageValue = page == 0 ? FIRST_PAGE_INDEX : page;
		final int perPageValue = perPage == 0 ? perPageDefault : perPage;
		final int minIndex = getMinIndex(pageValue, perPageValue);

		final String sanitizeFilter = filter != null ? SQLUtil.sanitizeParameter(filter) : StringPool.BLANK;

		final Map<String, Object> params = getParameters(sanitizeFilter, orderBy, direction, extraParams);

		PaginatedArrayList items = paginator.getItems(user, perPageValue, minIndex, params);

		if (!UtilMethods.isSet(items)){
			items = new PaginatedArrayList();
		}

		final long totalRecords = items.getTotalResults();

		final String linkHeaderValue = getHeaderValue(req.getRequestURI(), sanitizeFilter, pageValue, perPageValue,
				totalRecords, orderBy, direction, extraParams);

		return Response.
				ok(new ResponseEntityView(function.apply(items)))
				.header(LINK_HEADER_NAME, linkHeaderValue)
				.header(PAGINATION_PER_PAGE_HEADER_NAME, perPageValue)
				.header(PAGINATION_CURRENT_PAGE_HEADER_NAME, pageValue)
				.header(PAGINATION_MAX_LINK_PAGES_HEADER_NAME, nLinks)
				.header(PAGINATION_TOTAL_ENTRIES_HEADER_NAME, totalRecords)
				.build();
	}

	protected Map<String, Object> getParameters(final String filter,
												final String orderBy,
												final OrderDirection direction,
												final Map<String, Object> extraParams) {
		final Map<String, Object> params = map(
				Paginator.DEFAULT_FILTER_PARAM_NAME, filter,
				Paginator.ORDER_BY_PARAM_NAME, orderBy,
				Paginator.ORDER_DIRECTION_PARAM_NAME, direction != null ? direction : OrderDirection.ASC
		);

		if (extraParams != null) {
			for (final Map.Entry<String, Object> paramEntry : extraParams.entrySet()) {
				final Object value = paramEntry.getValue() instanceof String ?
						SQLUtil.sanitizeParameter((String) paramEntry.getValue()) :
						paramEntry.getValue();

				params.put(paramEntry.getKey(), value);
			}
		}
		return params;
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
	 */
	private static String getHeaderValue(final String urlBase, final String filter, final int page, final int perPage,
										 final long totalRecords, final String orderBy, final OrderDirection direction,
										 final Map<String, Object> extraParams) {
		final List<String> links = new ArrayList<>();

		links.add(StringUtil.format(LINK_TEMPLATE, map(
				"URL", getUrl(urlBase, filter, FIRST_PAGE_INDEX, perPage, orderBy, direction, extraParams),
				"relValue", FIRST_REL_VALUE
		)));

		int lastPage = (int) (Math.ceil((double) totalRecords/perPage));
		links.add(StringUtil.format(LINK_TEMPLATE, map(
				"URL", getUrl(urlBase, filter, lastPage, perPage, orderBy, direction, extraParams),
				"relValue", LAST_REL_VALUE
		)));

		links.add(StringUtil.format(LINK_TEMPLATE, map(
				"URL", getUrl(urlBase, filter, -1, perPage, orderBy, direction, extraParams),
				"relValue", PAGE_REL_VALUE
		)));

		int next = page + 1;
		if (next <= lastPage){
			links.add(StringUtil.format(LINK_TEMPLATE, map(
					"URL", getUrl(urlBase, filter, next, perPage, orderBy, direction, extraParams),
					"relValue", NEXT_REL_VALUE
			)));
		}

		int prev = page - 1;
		if (prev > 0){
			links.add(StringUtil.format(LINK_TEMPLATE, map(
					"URL", getUrl(urlBase, filter, prev, perPage, orderBy, direction, extraParams),
					"relValue", PREV_REL_VALUE
			)));
		}

		return String.join(",", links);
	}

	/**
	 *  Build each URL for the Link header.
	 *  The sintax to build each URL is the follow:
	 *
	 * [urlBase]?filter=[filter]&page=[page]&perPage=[perPage]&archived=[showArchived]&direction=[direction]&orderBy=[orderBy]
	 *
	 * @param urlBase
	 * @param filter
	 * @param page
	 * @param perPage
	 * @param orderBy
	 * @param direction
	 * @return
	 */
	private static String getUrl(final String urlBase, final String filter, final int page, final int perPage,
								 final String orderBy, final OrderDirection direction, final Map<String, Object> extraParams){

		final Map<String, String> params = new HashMap<>();

		if (UtilMethods.isSet(filter)){
			params.put(FILTER, String.valueOf(filter));
		}

		params.put(PER_PAGE, String.valueOf(perPage));
		params.put (PAGE, (-1 != page) ? String.valueOf(page) : PAGE_VALUE_TEMPLATE);

		if (UtilMethods.isSet(direction)) {
			params.put(DIRECTION, direction.toString());
		}

		if (UtilMethods.isSet(orderBy)) {
			params.put(ORDER_BY, orderBy);
		}

		if (extraParams != null) {
			for (final Map.Entry<String, Object> extraParamsEntry : extraParams.entrySet()) {
				final Object value = extraParamsEntry.getValue();

				if (value != null) {
					if (value instanceof Collection) {
						final Collection valueCollection = (Collection) value;
						final StringBuilder buffer = new StringBuilder();

						for (final Object valueItem : valueCollection) {
							if (buffer.length() != 0) {
								buffer.append(",");
							}

							buffer.append(valueItem.toString());
						}

						params.put(extraParamsEntry.getKey(), buffer.toString());
					} else {
						params.put(extraParamsEntry.getKey(), value.toString());
					}
				}
			}
		}

		final StringBuilder buffer = new StringBuilder(urlBase);

		boolean firstParam = true;

		for (final Map.Entry<String, String> paramsEntry : params.entrySet()) {

			if (firstParam){
				buffer.append('?');
				firstParam = false;
			}else{
				buffer.append('&');
			}


			try {
				final String encode = URLEncoder.encode(paramsEntry.getValue(), "UTF-8");
				buffer.append(paramsEntry.getKey())
						.append('=')
						.append(encode);
			} catch (UnsupportedEncodingException e) {
				continue;
			}
		}

		return buffer.toString();
	}
}