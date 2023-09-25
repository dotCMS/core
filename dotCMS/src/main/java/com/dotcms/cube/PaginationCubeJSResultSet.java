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
    private int pageSize;

    public PaginationCubeJSResultSet(final CubeJSClient cubeJSClient,
            final CubeJSQuery query,
            int pageSize) {
        this.cubeJSClient = cubeJSClient;
        this.query = query;
        this.pageSize = pageSize;
    }


    @Override
    public long size() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Iterator<ResultSetItem> iterator() {
        return new CubeIterator();
    }

    private class CubeIterator implements java.util.Iterator<ResultSetItem> {

        private long nextItem = 0;
        private Iterator<ResultSetItem> currentIterator;
        private boolean lastPage;

        public CubeIterator() {}

        @Override
        public boolean hasNext() {
            if ((currentIterator == null || !currentIterator.hasNext()) && !lastPage) {
                final CubeJSQuery cubeJSQuery = query.builder()
                    .limit(pageSize)
                    .offset(nextItem)
                    .build();

                final CubeJSResultSet cubeJSResultSet = cubeJSClient.send(cubeJSQuery);

                if (cubeJSResultSet.size() < pageSize) {
                    lastPage = true;
                }

                currentIterator = cubeJSResultSet.iterator();
            }

            return currentIterator.hasNext();
        }

        @Override
        public ResultSetItem next() {
            if (!hasNext()) {
                throw new PaginationException("There are no more items to iterate");
            }

            nextItem++;
            return currentIterator.next();
        }
    }

    /**
     * Exception to be thrown when it is not possible to get all the items from the CubeJS Server.
     */
    static class PaginationException extends RuntimeException {
        public PaginationException(String message) {
            super(message);
        }
    }

}
