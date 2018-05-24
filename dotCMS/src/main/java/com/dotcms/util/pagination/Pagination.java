package com.dotcms.util.pagination;

public class Pagination {

    public static final String LINK = "link";
    public static final String LINK_PAGES = "linkPages";
    public static final String PER_PAGE = "perSize";
    public static final String CURRENT_PAGE = "currentPage";
    public static final String TOTAL_RECORDS = "totalRecords";

    private final String link;
    private final Number perPage;
    private final Number currentPage;
    private final Number linkPages;
    private final Number totalRecords;

    Pagination(final String link, final Number perPage, final Number currentPage,
            final Number linkPages, final Number totalRecords) {
        this.link = link;
        this.perPage = perPage;
        this.currentPage = currentPage;
        this.linkPages = linkPages;
        this.totalRecords = totalRecords;
    }

    public String getLink() {
        return link;
    }

    public Number getPerPage() {
        return perPage;
    }

    public Number getCurrentPage() {
        return currentPage;
    }

    public Number getLinkPages() {
        return linkPages;
    }

    public Number getTotalRecords() {
        return totalRecords;
    }
}
