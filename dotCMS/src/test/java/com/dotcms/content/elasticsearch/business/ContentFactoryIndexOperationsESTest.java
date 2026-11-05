package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.content.elasticsearch.ESQueryCache;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.mockito.ArgumentCaptor;

import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;

public class ContentFactoryIndexOperationsESTest {

    /** Identity comes from stored fields or legacy keyword doc values, without fetching source. */
    @Test
    public void searchUsesStoredIdentityAndRequestsBothStorageFormats() {
        final ESQueryCache cache = mock(ESQueryCache.class);
        final SearchHit hit = new SearchHit(1, "doc", null, Map.of(
                "identifier", new DocumentField("identifier", List.of("id-1")),
                "inode", new DocumentField("inode", List.of("inode-1"))), Map.of());
        when(cache.get(any(SearchRequest.class))).thenReturn(Optional.of(
                new SearchHits(new SearchHit[]{hit}, new TotalHits(1, TotalHits.Relation.EQUAL_TO), 1)));

        assertEquals(List.of("inode-1"),
                new ContentFactoryIndexOperationsES(cache).search("+contenttype:blog", 10, 0));

        final ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
        verify(cache).get(request.capture());
        assertEquals(List.of("inode", "identifier"),
                request.getValue().source().storedFields().fieldNames());
        assertFalse(request.getValue().source().fetchSource().fetchSource());
        assertEquals(List.of("inode_dotraw", "identifier_dotraw"), request.getValue().source()
                .docValueFields().stream().map(field -> field.field).toList());
    }

    @Test
    public void addBuilderSort_acceptsCanonicalAndDotrawFieldNames() {
        final SearchSourceBuilder builder = new SearchSourceBuilder();

        ContentFactoryIndexOperationsES.addBuilderSort(
                "Book.title asc,Book.author_dotraw desc", builder);

        final FieldSortBuilder title = (FieldSortBuilder) builder.sorts().get(0);
        final FieldSortBuilder author = (FieldSortBuilder) builder.sorts().get(1);
        assertEquals("book.title_dotraw", title.getFieldName());
        assertEquals(SortOrder.ASC, title.order());
        assertEquals("book.author_dotraw", author.getFieldName());
        assertEquals(SortOrder.DESC, author.order());
    }
}
