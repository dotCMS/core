package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;

import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;

public class ContentFactoryIndexOperationsESTest {

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
