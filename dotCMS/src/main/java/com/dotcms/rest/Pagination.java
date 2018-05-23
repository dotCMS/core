package com.dotcms.rest;

public class Pagination {

    private final String link;
    private final int perPage;
    private final int currentPage;
    private final int linkPages;
    private final long totalRecords;

    public Pagination(final String link, final int perPage, final int currentPage,
            final int linkPages, final long totalRecords) {
        this.link = link;
        this.perPage = perPage;
        this.currentPage = currentPage;
        this.linkPages = linkPages;
        this.totalRecords = totalRecords;
    }

    public String getLink() {
        return link;
    }

    public int getPerPage() {
        return perPage;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getLinkPages() {
        return linkPages;
    }

    public long getTotalRecords() {
        return totalRecords;
    }
}
