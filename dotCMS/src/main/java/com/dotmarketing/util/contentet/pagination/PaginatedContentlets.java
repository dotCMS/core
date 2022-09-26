package com.dotmarketing.util.contentet.pagination;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;;
import com.liferay.portal.model.User;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * It is a {@link Iterable} of {@link Contentlet}.
 * The {@link Contentlet} are got from Elastic Search using a lucene query using pagination,
 * the size of each page can be set by {@link PaginatedContentlets#perPage} attribute.
 * Just the {@link Contentlet}'s Inode are storage into memory and before return each of them the
 * {@link Contentlet} object is load from cache or database.
 */
public class PaginatedContentlets implements Iterable<Contentlet> {

    private static int NOT_LOAD = -1;
    private User user;
    private ContentletAPI contentletAPI;
    private String luceneQuery;
    private boolean respectFrontendRoles;

    private String SORT_BY = "title asc";
    private int perPage;
    private long totalHits = NOT_LOAD;
    private List<String> currentPageContentletInodes = null;

    /**
     * Create a PaginatedContentlet
     *
     * @param luceneQuery lucene query to get the contentlets
     * @param user User to check permission
     * @param respectFrontendRoles true if you want to respect Front end roles
     * @param perPage Page size limit
     * @param contentletAPI
     */
    PaginatedContentlets(final String luceneQuery, final User user, final boolean respectFrontendRoles,
            final int perPage, final ContentletAPI contentletAPI) {
        this.user = user;
        this.luceneQuery = luceneQuery;
        this.contentletAPI = contentletAPI;
        this.respectFrontendRoles = respectFrontendRoles;
        this.perPage = perPage;

        try {
            currentPageContentletInodes = loadNextPage(0);
        } catch (DotSecurityException | DotDataException e) {
            Logger.error(ContentletIterator.class, e.getMessage());
        }
    }

    PaginatedContentlets(final String luceneQuery, final User user, final boolean respectFrontendRoles, final int perPage) {
        this(luceneQuery, user, respectFrontendRoles, perPage, APILocator.getContentletAPI());
    }

    @Override
    public Iterator<Contentlet> iterator() {
        return new ContentletIterator();
    }

    private List<String> loadNextPage(final int offset) throws DotSecurityException, DotDataException {
         final PaginatedArrayList<ContentletSearch> paginatedArrayList = (PaginatedArrayList) this.contentletAPI
                .searchIndex(this.luceneQuery,
                        perPage,
                        offset,
                        SORT_BY,
                        this.user,
                        this.respectFrontendRoles);

         if (totalHits == NOT_LOAD) {
             totalHits  = paginatedArrayList.getTotalResults();
         }

         return paginatedArrayList.stream()
                 .map(contentletSearch -> contentletSearch.getInode())
                 .collect(Collectors.toList());
    }

    public long size(){
        return totalHits;
    }

    private class ContentletIterator implements Iterator<Contentlet> {

        private int currentIndex = -1;
        private int totalIndex = 0;

        @Override
        public boolean hasNext() {
            return totalIndex < totalHits;
        }

        @Override
        public Contentlet next() {
            try {

                currentIndex++;
                if (currentIndex >= currentPageContentletInodes.size()) {
                    currentPageContentletInodes = loadNextPage(totalIndex);
                    currentIndex = 0;
                }

                final String inode = currentPageContentletInodes.get(currentIndex);
                totalIndex++;
                return PaginatedContentlets.this.contentletAPI.find(inode, user, respectFrontendRoles);
            } catch (DotSecurityException | DotDataException e) {
                Logger.error(ContentletIterator.class, e.getMessage());
                throw new NoSuchElementException(e.getMessage());
            }
        }

        @Override
        public void remove() {
            currentPageContentletInodes.remove(currentIndex);
            currentIndex--;
        }
    }
}
