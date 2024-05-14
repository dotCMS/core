package com.dotcms.rest;


/**
 * This class encapsulates Pagination general params.
 * Allows to centralize the pagination parameters in one single bean
 * And also allows for customization of the pagination parameters
 * This is also here to avoid duplication code warnings from Sonar
 * No Builder pattern here, as this is a simple managed by the rest layer
 */
public class AbstractPaginationContext {

    final String filter;

    final int page;

    final int perPage;

    final String orderBy;

    final String direction;

    public AbstractPaginationContext(final String filter, final int page, final int perPage, final String orderBy, final String direction) {
        this.filter = filter;
        this.page = page;
        this.perPage = perPage;
        this.orderBy = orderBy;
        this.direction = direction;
    }

    /**
     * Any name used to filter by
     * @return the filter
     */
    public String getFilter() {
        return filter;
    }

    /**
     * Page number you want to look at
     * @return the page
     */
    public int getPage() {
        return page;
    }

    /**
     * Number of items that conform the page.
     * @return the perPage
     */
    public int getPerPage() {
        return perPage;
    }

    /**
     * Field to order by in this case (name,id,integrations)
     * @return the orderBy
     */
    public String getOrderBy() {
        return orderBy;
    }

    /**
     * Ascendant or Descendant
     * @return the direction
     */
    public String getDirection() {
        return direction;
    }

}
