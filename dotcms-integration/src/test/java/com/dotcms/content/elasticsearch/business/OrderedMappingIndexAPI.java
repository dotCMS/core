package com.dotcms.content.elasticsearch.business;

import com.dotmarketing.common.reindex.ReindexEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Test-only {@link ContentletIndexAPIImpl} that fixes the order in which the documents of one
 * reindex entry are handed to the bulk processor.
 *
 * <p>The versions of an identifier are loaded into a {@code HashMap}, so whether a rejected
 * document comes before or after its healthy siblings depends on inode hashes. #37269 requires
 * the outcome to be the same in both orders; this subclass lets a test pin each order instead of
 * hoping to hit both by chance.</p>
 */
public class OrderedMappingIndexAPI extends ContentletIndexAPIImpl {

    private final boolean rejectedFirst;

    /**
     * Creates the index API with a fixed document order.
     *
     * @param rejectedFirst {@code true} to put rejected documents before the healthy ones,
     *                      {@code false} to put them after
     */
    public OrderedMappingIndexAPI(final boolean rejectedFirst) {
        super();
        this.rejectedFirst = rejectedFirst;
    }

    /**
     * Maps the entry as the production code does, then reorders the documents so the rejected
     * ones come first or last as requested. The sort is stable, so healthy documents keep their
     * relative order.
     */
    @Override
    List<MappedDocument> mapEntry(final ReindexEntry idx) throws Exception {
        final List<MappedDocument> documents = new ArrayList<>(super.mapEntry(idx));
        documents.sort(Comparator.comparing(document -> document.isRejected() != rejectedFirst));
        return documents;
    }
}
