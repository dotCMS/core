package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch.core.SearchRequest;

public class ContentFactoryIndexOperationsOSTest {

    @Test
    public void addBuilderSort_acceptsCanonicalAndDotrawFieldNames() {
        final SearchRequest.Builder builder = new SearchRequest.Builder();

        ContentFactoryIndexOperationsOS.addBuilderSort(
                "Book.title asc,Book.author_dotraw desc", builder);

        final SearchRequest request = builder.build();
        assertEquals("book.title_dotraw", request.sort().get(0).field().field());
        assertEquals(SortOrder.Asc, request.sort().get(0).field().order());
        assertEquals("book.author_dotraw", request.sort().get(1).field().field());
        assertEquals(SortOrder.Desc, request.sort().get(1).field().order());
    }
}
