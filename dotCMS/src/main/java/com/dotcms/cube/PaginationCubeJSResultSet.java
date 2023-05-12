package com.dotcms.cube;

import com.dotcms.util.DotPreconditions;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * It represents a {@link CubeJSResultSet} with pagination, it is going to get the result by a limit
 * of 1000 items for each request.
 */
public class PaginationCubeJSResultSet implements CubeJSResultSet {

    private CubeJSClient cubeJSClient;
    private CubeJSQuery query;
    private Long totalItems;
    private int pageSize;

    public PaginationCubeJSResultSet(final CubeJSClient cubeJSClient,
            final CubeJSQuery query,
            final Long totalItems, int pageSize) {
        this.cubeJSClient = cubeJSClient;
        this.query = query;
        this.totalItems = totalItems;
        this.pageSize = pageSize;
    }


    @Override
    public long size() {
        return totalItems;
    }

    @Override
    public Iterator<ResultSetItem> iterator() {
        return new CubeIterator();
    }

    private class CubeIterator implements java.util.Iterator<ResultSetItem> {

        private long nextItem = 0;
        private Iterator<ResultSetItem> currentIterator;

        public CubeIterator() {

        }

        @Override
        public boolean hasNext() {
            return nextItem < totalItems;
        }

        @Override
        public ResultSetItem next() {

            DotPreconditions.isTrue(hasNext(),
                    NoSuchElementException.class, () -> "There are no more items to iterate");

            if (currentIterator == null || !currentIterator.hasNext()) {
                final CubeJSQuery cubeJSQuery = query.builder()
                        .limit(pageSize)
                        .offset(nextItem)
                        .build();
                currentIterator = cubeJSClient.send(cubeJSQuery).iterator();
            }

            nextItem++;

            try {
                return currentIterator.next();

            } catch (NoSuchElementException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
