package com.dotmarketing.util.contentet.pagination;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class ContentletsPaginated implements Iterable<Contentlet> {

    private static int NOT_LOAD = -1;
    private User user;
    private ContentletAPI contentletAPI;
    private String luceneQuery;
    private boolean respectFrontendRoles;

    private String SORT_BY = "title desc";
    private int perPage;
    private int currentOffset = 0;
    private long totalHits = NOT_LOAD;
    private List<String> currentPageContentletInodes = null;

    ContentletsPaginated(final String luceneQuery, final User user, final boolean respectFrontendRoles,
            final int perPage, final ContentletAPI contentletAPI) {
        this.user = user;
        this.luceneQuery = luceneQuery;
        this.contentletAPI = contentletAPI;
        this.respectFrontendRoles = respectFrontendRoles;
        this.perPage = perPage;

        try {
            currentPageContentletInodes = loadNextPage();
        } catch (DotSecurityException | DotDataException e) {
            Logger.error(ContentletIterator.class, e.getMessage());
        }
    }

    ContentletsPaginated(final String luceneQuery, final User user, final boolean respectFrontendRoles, final int perPage) {
        this(luceneQuery, user, respectFrontendRoles, perPage, APILocator.getContentletAPI());
    }

    @NotNull
    @Override
    public Iterator<Contentlet> iterator() {
        return new ContentletIterator();
    }

    private List<String> loadNextPage() throws DotSecurityException, DotDataException {
         final PaginatedArrayList<ContentletSearch> paginatedArrayList = (PaginatedArrayList) this.contentletAPI
                .searchIndex(this.luceneQuery,
                        perPage,
                        currentOffset,
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

        private int currentIndex = 0;
        private int currentTotalIndex = 0;

        @Override
        public boolean hasNext() {
            return currentTotalIndex < totalHits;
        }

        @Override
        public Contentlet next() {
            try {
                if (currentIndex >= currentPageContentletInodes.size()) {
                    currentPageContentletInodes = loadNextPage();
                    currentIndex = 0;
                }

                final String inode = currentPageContentletInodes.get(currentIndex);
                currentIndex++;
                currentTotalIndex++;
                return ContentletsPaginated.this.contentletAPI.find(inode, user, respectFrontendRoles);
            } catch (DotSecurityException | DotDataException e) {
                Logger.error(ContentletIterator.class, e.getMessage());
                throw new NoSuchElementException(e.getMessage());
            }
        }
    }
}
