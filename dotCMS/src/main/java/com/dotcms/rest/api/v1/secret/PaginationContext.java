package com.dotcms.rest.api.v1.secret;

import com.dotcms.util.PaginationUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

/**
 * This class encapsulates Pagination general params.
 */
public class PaginationContext {

    private final String filter;

    private final int page;

    private final int perPage;

    private final String orderBy;

    private final String direction;

    @JsonCreator
    public PaginationContext(
            @QueryParam(PaginationUtil.FILTER) final String filter,
            @QueryParam(PaginationUtil.PAGE) final int page,
            @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
            @DefaultValue("integrations")
            @QueryParam(PaginationUtil.ORDER_BY) final String orderBy,
            @DefaultValue("ASC")
            @QueryParam(PaginationUtil.DIRECTION) final String direction) {
        this.filter = filter;
        this.page = page;
        this.perPage = perPage;
        this.orderBy = orderBy;
        this.direction = direction;
    }

    /**
     * Any name used to filter by
     * @return
     */
    public String getFilter() {
        return filter;
    }

    /**
     * Page number you want to look at
     * @return
     */
    public int getPage() {
        return page;
    }

    /**
     * Number of items that conform the page.
     * @return
     */
    public int getPerPage() {
        return perPage;
    }

    /**
     * Field to order by in this case (name,id,integrations)
     * @return
     */
    public String getOrderBy() {
        return orderBy;
    }

    /**
     * Ascendant or Descendant
     * @return
     */
    public String getDirection() {
        return direction;
    }

}
