package com.dotcms.util;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.Paginator;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;
import static com.dotmarketing.util.WebKeys.DOTCMS_PAGINATION_LINKS;
import static com.dotmarketing.util.WebKeys.DOTCMS_PAGINATION_ROWS;

/**
 * Utility class to the pagination elements and filter
 * 
 * @author oswaldogallango
 *
 */
public class PaginationUtil {

	public static final int FIRST_PAGE_INDEX = 1;

	public static final String FILTER = "filter";
	public static final String ARCHIVED = "archived";
	public static final String PAGE = "page";
	public static final String PER_PAGE = "per_page";
	public static final String ORDER_BY = "orderby";
	public static final String DIRECTION = "direction";

	private static final String LINK_HEADER_NAME = "Link";
	private static final String PAGINATION_PER_PAGE_HEADER_NAME = "X-Pagination-Per-Page";
	private static final String PAGINATION_CURRENT_PAGE_HEADER_NAME = "X-Pagination-Current-Page";
	private static final String PAGINATION_MAX_LINK_PAGES_HEADER_NAME = "X-Pagination-Link-Pages";
	private static final String PAGINATION_TOTAL_ENTRIES_HEADER_NAME = "X-Pagination-Total-Entries";

	private Paginator paginator;

	private static final String URL_TEMPLATE;
	private static final String LINK_TEMPLATE = "<{URL}>;rel=\"{relValue}\"";

	private static final String  FIRST_REL_VALUE = "first";
	private static final String  LAST_REL_VALUE = "last";
	private static final String  PREV_REL_VALUE = "prev";
	private static final String  NEXT_REL_VALUE = "next";
	private static final String  PAGE_REL_VALUE = "x-page";

	private int perPageDefault;
	private int nLinks;

	static{
		URL_TEMPLATE = StringUtil.format(
				"{baseURL}?{filter}={filterValue}&{archived}={archivedValue}&{page}={pageValue}" +
							"&{perPage}={perPageValue}&{direction}={directionValue}&{orderBy}={orderByValue}",
			map(
					"filter", FILTER,
					"archived", ARCHIVED,
					"page", PAGE,
					"perPage", PER_PAGE,
					"direction", DIRECTION,
					"orderBy", ORDER_BY
			)
		);
	}

	public PaginationUtil(Paginator paginator){
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

	public Response getPage(HttpServletRequest req, User user, String filter, boolean showArchived, int pageParam,
							int perPageParam) {
		return getPage(req, user, filter, showArchived, pageParam, perPageParam, "", (OrderDirection) null);
	}

	public Response getPage(HttpServletRequest req, User user, String filter, boolean showArchived, int pageParam,
							int perPageParam, String orderBy, String direction) {
		return getPage(req, user, filter, showArchived, pageParam, perPageParam, orderBy, OrderDirection.valueOf(direction));
	}

	/**
	 * Return a pagination's response
	 *
	 * @param req
	 * @param user Login User
	 * @param filter
	 * @param showArchived If is true return items archived when apply
	 * @param page Page to return
	 * @param perPage Number of items by page
	 * @param orderBy Field name to order by
	 * @param direction Order direction (ASC, DESC)
	 * @return
	 */
	public Response getPage(HttpServletRequest req, User user, String filter, boolean showArchived, int page,
							int perPage, String orderBy, OrderDirection direction) {

		int pageValue = page == 0 ? FIRST_PAGE_INDEX : page;
		int perPageValue = perPage == 0 ? perPageDefault : perPage;
		int minIndex = getMinIndex(pageValue, perPage);
		String sanitizefilter = SQLUtil.sanitizeParameter(filter);

		Collection items = paginator.getItems(user, sanitizefilter, showArchived, perPageValue, minIndex, orderBy, direction);
		long totalRecords = paginator.getTotalRecords(filter);
		String linkHeaderValue = getHeaderValue(req.getRequestURL().toString(), sanitizefilter, pageValue, perPageValue, showArchived,
				totalRecords, orderBy, direction);

		return Response.
				ok(new ResponseEntityView(items))
				.header(LINK_HEADER_NAME, linkHeaderValue)
				.header(PAGINATION_PER_PAGE_HEADER_NAME, perPageValue)
				.header(PAGINATION_CURRENT_PAGE_HEADER_NAME, pageValue)
				.header(PAGINATION_MAX_LINK_PAGES_HEADER_NAME, nLinks)
				.header(PAGINATION_TOTAL_ENTRIES_HEADER_NAME, totalRecords)
				.build();
	}

	private static String getHeaderValue(String urlBase, String filter, int page, int perPage, boolean showArchived,
											long totalRecords, String orderBy, OrderDirection direction) {
		List<String> links = new ArrayList<>();

		links.add(StringUtil.format(LINK_TEMPLATE, map(
				"URL", getUrl(urlBase, filter, FIRST_PAGE_INDEX, perPage, showArchived, orderBy, direction),
				"relValue", FIRST_REL_VALUE
		)));

		int lastPage = (int) (Math.ceil((double) totalRecords/perPage));
		links.add(StringUtil.format(LINK_TEMPLATE, map(
				"URL", getUrl(urlBase, filter, lastPage, perPage, showArchived, orderBy, direction),
				"relValue", LAST_REL_VALUE
		)));

		links.add(StringUtil.format(LINK_TEMPLATE, map(
				"URL", getUrl(urlBase, filter, -1, perPage, showArchived, orderBy, direction),
				"relValue", PAGE_REL_VALUE
		)));

		int next = page + 1;
		if (next <= lastPage){
			links.add(StringUtil.format(LINK_TEMPLATE, map(
					"URL", getUrl(urlBase, filter, next, perPage, showArchived, orderBy, direction),
					"relValue", NEXT_REL_VALUE
			)));
		}

		int prev = page - 1;
		if (prev > 0){
			links.add(StringUtil.format(LINK_TEMPLATE, map(
					"URL", getUrl(urlBase, filter, prev, perPage, showArchived, orderBy, direction),
					"relValue", PREV_REL_VALUE
			)));
		}

		return String.join(",", links);
}

	private static String getUrl(String urlBase, String filter, int page, int perPage, boolean showArchived,
								 String orderBy, OrderDirection direction){

		Map<String, String> params = map(
				"baseURL",
				urlBase,
				"filterValue",
				filter,
				"archivedValue",
				String.valueOf(showArchived),
				"perPageValue",
				String.valueOf(perPage)
		);

		if (page != -1){
			params.put("pageValue", String.valueOf(page));
		}

		if (direction != null){
			params.put("directionValue", direction.toString());
		}

		if (UtilMethods.isSet(orderBy)){
			params.put("orderByValue", orderBy.toString());
		}

		return StringUtil.format(URL_TEMPLATE, params);
	}
}
