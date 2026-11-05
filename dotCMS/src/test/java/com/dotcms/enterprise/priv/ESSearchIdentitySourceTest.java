package com.dotcms.enterprise.priv;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.Test;

/** Keeps the native ES API's identifier-only source shape without fetching actual source. */
public class ESSearchIdentitySourceTest {

    /** Both storage formats must retain the native source accessor without exposing extra fields. */
    @Test
    public void restoresOnlyIdentityForNativeCallers() {
        for (final String suffix : List.of("", "_dotraw")) {
            final String identifier = "identifier" + suffix;
            final String inode = "inode" + suffix;
            final SearchHit hit = new SearchHit(1, "doc", null, Map.of(
                    identifier, new DocumentField(identifier, List.of("id-1")),
                    inode, new DocumentField(inode, List.of("inode-1")),
                    "unrelated", new DocumentField("unrelated", List.of("must not become source"))), Map.of());
            final SearchHits hits = new SearchHits(new SearchHit[]{hit},
                    new TotalHits(1, TotalHits.Relation.EQUAL_TO), 1);

            ESSearchAPIImpl.restoreIdentitySource(hits);

            assertEquals(Map.of("identifier", "id-1", "inode", "inode-1"), hit.getSourceAsMap());
        }
    }
}
