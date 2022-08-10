package com.dotmarketing.util;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class ContentletPaginated implements Iterable<Contentlet> {

    private static int NOT_LOAD = -1;
    private User user;
    private ContentletAPI contentletAPI;
    private String luceneQuery;
    private boolean respectFrontendRoles;

    private String SORT_BY = "title desc";
    private Lazy<Integer> perPage = Lazy.of(() -> Config.getIntProperty("PER_PAGE", 1000));;
    private int currentOffset = 0;
    private long totalHits = NOT_LOAD;
    private List<String> currentPageContentletInodes = null;

    @VisibleForTesting
    public ContentletPaginated(final String luceneQuery, final User user, final boolean respectFrontendRoles,
            final ContentletAPI contentletAPI) {
        this.user = user;
        this.luceneQuery = luceneQuery;
        this.contentletAPI = contentletAPI;
        this.respectFrontendRoles = respectFrontendRoles;


        try {
            currentPageContentletInodes = loadNextPage();
        } catch (DotSecurityException | DotDataException e) {
            Logger.error(ContentletIterator.class, e.getMessage());
        }
    }

    public ContentletPaginated(final String luceneQuery, final User user, final boolean respectFrontendRoles) {
        this(luceneQuery, user, respectFrontendRoles, APILocator.getContentletAPI());
    }

    @NotNull
    @Override
    public Iterator<Contentlet> iterator() {
        return new ContentletIterator();
    }

    private List<String> loadNextPage() throws DotSecurityException, DotDataException {
         final PaginatedArrayList<ContentletSearch> paginatedArrayList = (PaginatedArrayList) this.contentletAPI
                .searchIndex(this.luceneQuery,
                        perPage.get(),
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
                return ContentletPaginated.this.contentletAPI.find(inode, user, respectFrontendRoles);
            } catch (DotSecurityException | DotDataException e) {
                Logger.error(ContentletIterator.class, e.getMessage());
                throw new NoSuchElementException(e.getMessage());
            }
        }
    }
}
