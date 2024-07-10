package com.dotcms.content.elasticsearch.util;

import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.PaginatedContentList;

/**
 * The PaginationUtil class provides utility methods for creating paginated content lists.
 */
public class PaginationUtil {

    private PaginationUtil() {
    }

    /**
     * Converts a PaginatedArrayList of Contentlet objects to a PaginatedContentList, implementing
     * pagination based on the provided limit and offset parameters.
     *
     * @param searchResults The original PaginatedArrayList of Contentlet objects.
     * @param limit         The maximum number of Contentlet objects per page. If set to 0 or a
     *                      negative number, the default maximum limit will be used.
     * @param offset        The offset value to determine the starting index for pagination. If set
     *                      to 0 or a negative number, the starting index will be set to 0.
     * @return The converted PaginatedContentList with pagination information set. If the
     * totalResults of the searchResults is 0 or the limit is 0, an empty PaginatedContentList is
     * returned.
     */
    public static PaginatedContentList<Contentlet> paginatedArrayListToPaginatedContentList(
            final PaginatedArrayList<Contentlet> searchResults, final int limit, int offset) {

        // Calculate the pagination
        var totalResults = searchResults.getTotalResults();
        int contentsPerPage = limit;
        if (limit <= 0) {
            contentsPerPage = ESContentletAPIImpl.MAX_LIMIT;
        }

        int currentPage = 1;
        if (offset > 0) {
            currentPage = (offset / contentsPerPage) + 1;
        } else {
            offset = 0;
        }

        var paginatedContentList = new PaginatedContentList<Contentlet>();
        paginatedContentList.setLimit(contentsPerPage);
        paginatedContentList.setOffset(offset);

        if (totalResults > 0) {

            paginatedContentList.setCurrentPage(currentPage);

            // Calculate the pagination
            long minIndex = (currentPage - 1L) * contentsPerPage;
            long maxIndex = (long) contentsPerPage * currentPage;
            if ((minIndex + contentsPerPage) >= totalResults) {
                maxIndex = totalResults;
            }
            paginatedContentList.setTotalResults(totalResults);
            paginatedContentList.setTotalPages(
                    (int) Math.ceil(((double) totalResults) / ((double) contentsPerPage))
            );
            paginatedContentList.setNextPage(maxIndex < totalResults);
            paginatedContentList.setPreviousPage(minIndex > 0);

            // Add the content to the paginated list
            paginatedContentList.addAll(searchResults);
        }

        return paginatedContentList;
    }

}
