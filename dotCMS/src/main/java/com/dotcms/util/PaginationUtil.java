package com.dotcms.util;

import com.dotcms.rest.Pagination;
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
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.dotmarketing.util.WebKeys.DOTCMS_PAGINATION_LINKS;
import static com.dotmarketing.util.WebKeys.DOTCMS_PAGINATION_ROWS;

/**
 * This class provides the base information and structure for any {@link com.dotcms.util.pagination.PaginatorOrdered}
 * inheriting class in dotCMS. Any common-use functionality or features can be implemented at this level.
 *
 * @author oswaldogallango
 * @since Dec 5th, 2016
 */
public class PaginationUtil {

	public static final int FIRST_PAGE_INDEX = 1;

	public static final String FILTER = "filter";
	public static final String PAGE = "page";
	public static final String PER_PAGE = "per_page";
	public static final String ORDER_BY = "orderby";
	public static final String DIRECTION = "direction";
	private static final String LINK_HEADER_NAME = "Link";
	public static final String PAGINATION_PER_PAGE_HEADER_NAME = "X-Pagination-Per-Page";
	public static final String PAGINATION_CURRENT_PAGE_HEADER_NAME = "X-Pagination-Current-Page";
	private static final String PAGINATION_MAX_LINK_PAGES_HEADER_NAME = "X-Pagination-Link-Pages";
	public static final String PAGINATION_TOTAL_ENTRIES_HEADER_NAME = "X-Pagination-Total-Entries";
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
	 * Returns the minimum index for a specific page.
	 *
	 * @param currentPage The current page.
	 * @param perPage     The max amount of items per page.
	 *
	 * @return The minimum index.
	 */
	private int getMinIndex(int currentPage, int perPage){
		return (currentPage - 1) * perPage;

	}

	/**
	 * Returns a paginated response of elements to the User based on a specific set of parameters. Any class inheriting
	 * the {@link Paginator} interface will be able to provide paginated items.
	 *
	 * @param req     The current instance of the {@link HttpServletRequest}.
	 * @param user    The {@link User} calling this pagination.
	 * @param filter  A multipurpose String filtering parameter.
	 * @param page    The page offset value, for pagination purposes.
	 * @param perPage The number of items per page, for pagination purposes.
	 *
	 * @return The {@link Response} object including the paginated items and the respective pagination headers.
	 */
	public Response getPage(final HttpServletRequest req, final User user, final String filter, final int page, final int perPage) {
		return getPage(req, user, filter, page, perPage, StringUtils.EMPTY, null, null);
	}

	/**
	 * Returns a paginated response of elements to the User based on a specific set of parameters. Any class inheriting
	 * the {@link Paginator} interface will be able to provide paginated items.
	 *
	 * @param req       The current instance of the {@link HttpServletRequest}.
	 * @param user      The {@link User} calling this pagination.
	 * @param filter    A multipurpose String filtering parameter.
	 * @param page      The page offset value, for pagination purposes.
	 * @param perPage   The number of items per page, for pagination purposes.
	 * @param orderBy   The column name that will be used to sort the paginated results. For reference, please
	 *                  check {@link SQLUtil#ORDERBY_WHITELIST}.
	 * @param direction The sorting direction for the results: {@code "ASC"} or {@code "DESC"}
	 *
	 * @return The {@link Response} object including the paginated items and the respective pagination headers.
	 */
	public Response getPage(final HttpServletRequest req, final User user, final String filter, final int page,
							final int perPage, final String orderBy, final String direction) {
		return getPage(req, user, filter, page, perPage, orderBy,
				OrderDirection.valueOf(direction), null);
	}

	/**
	 * Returns a paginated response of elements to the User based on a specific set of parameters. Any class inheriting
	 * the {@link Paginator} interface will be able to provide paginated items.
	 *
	 * @param req         The current instance of the {@link HttpServletRequest}.
	 * @param user        The {@link User} calling this pagination.
	 * @param filter      A multipurpose String filtering parameter.
	 * @param page        The page offset value, for pagination purposes.
	 * @param perPage     The number of items per page, for pagination purposes.
	 * @param extraParams A flexible map with custom or very specific parameters that may be required by the Paginator
	 *                    class to return the expected items.
	 *
	 * @return The {@link Response} object including the paginated items and the respective pagination headers.
	 */
	public Response getPage(final HttpServletRequest req, final User user, final String filter, final int page,
							final int perPage, final Map<String, Object>  extraParams) {
		return getPage(req, user, filter, page, perPage, null, null, extraParams);
	}

	/**
	 * Returns a paginated response of elements to the User based on a specific set of parameters. Any class inheriting
	 * the {@link Paginator} interface will be able to provide paginated items.
	 *
	 * @param req          The current instance of the {@link HttpServletRequest}.
	 * @param user         The {@link User} calling this pagination.
	 * @param filter       A multipurpose String filtering parameter.
	 * @param page		   The page offset value, for pagination purposes.
	 * @param perPage	   The number of items per page, for pagination purposes.
	 * @param orderBy      The column name that will be used to sort the paginated results. For reference, please check
	 *                     {@link SQLUtil#ORDERBY_WHITELIST}.
	 * @param direction    The sorting direction for the results: {@code "ASC"} or {@code "DESC"}
	 * @param extraParams  A flexible map with custom or very specific parameters that may be required by the Paginator
	 *                     class to return the expected items.
	 *
	 * @return The {@link Response} object including the paginated items and the respective pagination headers.
	 */
	public Response getPage(final HttpServletRequest req, final User user, final String filter, final int page,
							final int perPage, final String orderBy, final OrderDirection direction,
							final Map<String, Object> extraParams) {
		return getPage(req, user, filter, page, perPage, orderBy, direction, extraParams, null);
	}

	/**
	 * Typically, our pagination is directly wrapped into a ResponseEntityView object. But, what if we had a more
	 * complex situation where the paginated items are meant to live within a nested level in our JSON object? This
	 * method comes to solve that problem.
	 *
	 * @param req          The current instance of the {@link HttpServletRequest}.
	 * @param user         The {@link User} calling this pagination.
	 * @param filter       A multipurpose String filtering parameter.
	 * @param page		   The page offset value, for pagination purposes.
	 * @param perPage	   The number of items per page, for pagination purposes.
	 * @param orderBy      The column name that will be used to sort the paginated results. For reference, please
	 *                     check {@link SQLUtil#ORDERBY_WHITELIST}.
	 * @param direction    The sorting direction for the results: {@code "ASC"} or {@code "DESC"}
	 * @param function     This is function must feed the items into whatever json we want to use.
	 *
	 * @return The {@link Response} object including the paginated items and the respective pagination headers.
	 */
	@SuppressWarnings("unchecked")
	public <T, R> Response getPage(final HttpServletRequest req, final User user, final String filter, final int page,
								   final int perPage, final String orderBy, final OrderDirection direction,
								   final Map<String, Object> extraParams,
								   final Function<PaginatedArrayList<T>, R> function) {
		final int pageValue = page <= 0 ? FIRST_PAGE_INDEX : page;
		final int perPageValue = perPage <= 0 ? perPageDefault : perPage;
		final int minIndex = this.getMinIndex(pageValue, perPageValue);
		final String sanitizeFilter = filter != null ? SQLUtil.sanitizeParameter(filter) : StringPool.BLANK;
		final Map<String, Object> params = this.getParameters(sanitizeFilter, orderBy, direction, extraParams);
		PaginatedArrayList items = minIndex >= 0 ? paginator.getItems(user, perPageValue, minIndex, params) : null;
		if (UtilMethods.isNotSet(items)) {
			items = new PaginatedArrayList<>();
		}
		final long totalRecords = items.getTotalResults();
		final LinkHeader linkHeader =
				new LinkHeader.Builder().baseUrl(req.getRequestURI()).filter(sanitizeFilter).page(pageValue)
						.perPage(perPageValue).totalRecords(totalRecords).orderBy(orderBy).direction(direction)
						.extraParams(extraParams).build();
		final Object paginatedItems = null != function ? function.apply(items) : items;
		return this.createResponse(paginatedItems, linkHeader, pageValue, perPageValue, totalRecords);
	}

	/**
	 * Utility method that condenses a list of provided filtering parameters into a single Object Map.
	 *
	 * @param filter      The multipurpose filtering parameter.
	 * @param orderBy     The result sorting criterion.
	 * @param direction   The sorting direction.
	 * @param extraParams The initially-provided Map with more filtering parameters.
	 *
	 * @return The Map with all the filtering parameters.
	 */
	protected Map<String, Object> getParameters(final String filter,
												final String orderBy,
												final OrderDirection direction,
												final Map<String, Object> extraParams) {
		final Map<String, Object> params = new HashMap<>();
		params.put(Paginator.DEFAULT_FILTER_PARAM_NAME, filter);
		params.put(Paginator.ORDER_BY_PARAM_NAME, orderBy);
		params.put(Paginator.ORDER_DIRECTION_PARAM_NAME, direction != null ? direction : OrderDirection.ASC);

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
	 * Returns the value of the Link header according this RFC:
	 * <a href="https://tools.ietf.org/html/rfc5988#page-6">https://tools.ietf.org/html/rfc5988#page-6</a> . The syntax
	 * to build each URL is the following:
	 * <pre>
	 *     [urlBase]?filter=[filter]&page=[page]&perPage=[perPage]&archived=[showArchived]&direction=[direction]&orderBy=[orderBy]
	 * </pre>
	 * <p>
	 * The real parameter can have the follow values: next, prev, last, first and x-page. For more information, please
	 * see: https://developer.github.com/v3/#pagination
	 *
	 * @return The value of the {@code Link} header.
	 */
	private String getHeaderValue(final LinkHeader linkHeader) {
		final List<String> links = new ArrayList<>();
		final String URL_KEY = "URL";
		final String REL_VALUE_KEY = "relValue";
		links.add(StringUtil.format(LINK_TEMPLATE, Map.of(
				URL_KEY, getUrl(linkHeader.baseUrl(), linkHeader.filter(), FIRST_PAGE_INDEX, linkHeader.perPage(),
						linkHeader.orderBy(), linkHeader.direction(), linkHeader.extraParams()),
				REL_VALUE_KEY, FIRST_REL_VALUE
		)));

		int lastPage = (int) (Math.ceil((double) linkHeader.totalRecords() / linkHeader.perPage()));
		links.add(StringUtil.format(LINK_TEMPLATE, Map.of(
				URL_KEY, getUrl(linkHeader.baseUrl(), linkHeader.filter(), lastPage, linkHeader.perPage(),
						linkHeader.orderBy(), linkHeader.direction(), linkHeader.extraParams()),
				REL_VALUE_KEY, LAST_REL_VALUE
		)));

		links.add(StringUtil.format(LINK_TEMPLATE, Map.of(
				URL_KEY, getUrl(linkHeader.baseUrl(), linkHeader.filter(), -1, linkHeader.perPage(),
						linkHeader.orderBy(), linkHeader.direction(), linkHeader.extraParams()),
				REL_VALUE_KEY, PAGE_REL_VALUE
		)));

		int next = linkHeader.page() + 1;
		if (next <= lastPage){
			links.add(StringUtil.format(LINK_TEMPLATE, Map.of(
					URL_KEY, getUrl(linkHeader.baseUrl(), linkHeader.filter(), next, linkHeader.perPage(),
							linkHeader.orderBy(), linkHeader.direction(), linkHeader.extraParams()),
					REL_VALUE_KEY, NEXT_REL_VALUE
			)));
		}

		int prev = linkHeader.page() - 1;
		if (prev > 0){
			links.add(StringUtil.format(LINK_TEMPLATE, Map.of(
					URL_KEY, getUrl(linkHeader.baseUrl(), linkHeader.filter(), prev, linkHeader.perPage(),
							linkHeader.orderBy(), linkHeader.direction(), linkHeader.extraParams()),
					REL_VALUE_KEY, PREV_REL_VALUE
			)));
		}

		return String.join(StringPool.COMMA, links);
	}

	/**
	 * Builds the expected URL for the {@code Link} header. The syntax to build each URL is the following:
	 * <pre>
	 *     [urlBase]?filter=[filter]&page=[page]&perPage=[perPage]&archived=[showArchived]&direction=[direction]&orderBy=[orderBy]
	 * </pre>
	 *
	 * @param baseUrl     The URI for the current HTTP Request.
	 * @param filter      The specified filter parameter.
	 * @param page        The value of the page parameter, for result pagination purposes.
	 * @param perPage     The value of the per-page parameter, for result pagination purposes.
	 * @param orderBy     The criterion used to sort the returned values.
	 * @param direction   The sort ordering: Ascending or descending.
	 * @param extraParams A map with additional/uncommon extra parameters.
	 *
	 * @return The resulting value of the {@code Link} header.
	 */
	private static String getUrl(final String baseUrl, final String filter, final int page, final int perPage,
								 final String orderBy, final OrderDirection direction, final Map<String, Object> extraParams){

		final Map<String, String> params = new HashMap<>();

		if (UtilMethods.isSet(filter)){
			params.put(FILTER, filter);
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
								buffer.append(StringPool.COMMA);
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

		final StringBuilder buffer = new StringBuilder(baseUrl);

		boolean firstParam = true;

		for (final Map.Entry<String, String> paramsEntry : params.entrySet()) {

			if (firstParam){
				buffer.append(StringPool.QUESTION);
				firstParam = false;
			}else{
				buffer.append(StringPool.AMPERSAND);
			}


			try {
				final String encode = URLEncoder.encode(paramsEntry.getValue(), "UTF-8");
				buffer.append(paramsEntry.getKey())
						.append('=')
						.append(encode);
			} catch (UnsupportedEncodingException e) {
				// Ignore error
			}
		}

		return buffer.toString();
	}

	/**
	 * Creates an instance of the {@link Response} class including both the Entity -- i.e., the list of queried
	 * objects -- and an additional attribute containing the pagination attributes for the UI layer to handle it. This
	 * is meant to solve a problem in which certain proxies may remove the already existing pagination headers.
	 *
	 * @param paginatedItems The list of results that are being returned based on the specified pagination parameters.
	 * @param linkHeader     The {@link LinkHeader} object with the information for generating the main navigation
	 *                       links.
	 * @param pageValue      The currently selected page.
	 * @param perPageValue   The maximum number of items returned in a given page.
	 * @param totalRecords   The total number of unfiltered items returned by the query.
	 *
	 * @return The expected {@link Response} object.
	 */
	private Response createResponse(final Object paginatedItems, final LinkHeader linkHeader, final int pageValue,
									final int perPageValue, final long totalRecords) {
		final String linkHeaderValue = this.getHeaderValue(linkHeader);
		final Pagination pagination =
				new Pagination.Builder()
						.currentPage(pageValue)
						.perPage(perPageValue)
						.totalEntries(totalRecords).build();
		return Response.ok(new ResponseEntityView<>(paginatedItems, pagination))
					   .header(LINK_HEADER_NAME, linkHeaderValue)
					   .header(PAGINATION_PER_PAGE_HEADER_NAME, perPageValue)
					   .header(PAGINATION_CURRENT_PAGE_HEADER_NAME, pageValue)
					   .header(PAGINATION_MAX_LINK_PAGES_HEADER_NAME, nLinks)
					   .header(PAGINATION_TOTAL_ENTRIES_HEADER_NAME, totalRecords).build();
	}

	/**
	 * Contains the required information for generating the value of the {@code Link} HTTP Header used by the UI layer.
	 * There are three values that can be generated:
	 * <ul>
	 *     <li>The link to get the <b>first</b> results page.</li>
	 *     <li>The link to get the <b>last</b> results page.</li>
	 *     <li>The link to get the <b>a specific</b> results page.</li>
	 * </ul>
	 * For instance, the value of this header may look like this:
	 * <br/><br/>{@code
	 *     </api/v1/templates?per_page=40&host={SITE-ID}&orderby=modDate&archive=false&page=1&direction=DESC>;rel="first",
	 *     </api/v1/templates?per_page=40&host={SITE-ID}&orderby=modDate&archive=false&page=1&direction=DESC>;rel="last",
	 *     </api/v1/templates?per_page=40&host={SITE-ID}&orderby=modDate&archive=false&page=pageValue&direction=DESC>;rel="x-page"
	 * }
	 */
	protected static class LinkHeader {

		private final String baseUrl;
		private final String filter;
		private final int page;
		private final int perPage;
		private final long totalRecords;
		private final String orderBy;
		private final OrderDirection direction;
		private final Map<String, Object> extraParams;

		/**
		 *
		 * @param builder
		 */
		private LinkHeader(final Builder builder) {
			this.baseUrl = builder.baseUrl;
			this.filter = builder.filter;
			this.page = builder.page;
			this.perPage = builder.perPage;
			this.totalRecords = builder.totalRecords;
			this.orderBy = builder.orderBy;
			this.direction = builder.direction;
			this.extraParams = builder.extraParams;
		}

		public String baseUrl() {
			return baseUrl;
		}

		public String filter() {
			return filter;
		}

		public int page() {
			return page;
		}

		public int perPage() {
			return perPage;
		}

		public long totalRecords() {
			return totalRecords;
		}

		public String orderBy() {
			return orderBy;
		}

		public OrderDirection direction() {
			return direction;
		}

		public Map<String, Object> extraParams() {
			return extraParams;
		}

		protected static final class Builder {

			private String baseUrl;
			private String filter;
			private int page;
			private int perPage;
			private long totalRecords;
			private String orderBy;
			private OrderDirection direction;
			private Map<String, Object> extraParams;

			public Builder baseUrl(final String urlBase) {
				this.baseUrl = urlBase;
				return this;
			}

			public Builder filter(final String filter) {
				this.filter = filter;
				return this;
			}

			public Builder page(final int page) {
				this.page = page;
				return this;
			}

			public Builder perPage(final int perPage) {
				this.perPage = perPage;
				return this;
			}

			public Builder totalRecords(final long totalRecords) {
				this.totalRecords = totalRecords;
				return this;
			}

			public Builder orderBy(final String orderBy) {
				this.orderBy = orderBy;
				return this;
			}

			public Builder direction(final OrderDirection direction) {
				this.direction = direction;
				return this;
			}

			public Builder extraParams(final Map<String, Object> extraParams) {
				this.extraParams = extraParams;
				return this;
			}

			public LinkHeader build () {
				return new LinkHeader(this);
			}

		}

	}

}
