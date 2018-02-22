package com.dotcms.enterprise.priv.util;

import java.io.IOException;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Assert;
import org.junit.Test;

public class SearchSourceBuilderUtilTest {


    @Test
    public void testGetSearchSourceBuilder() throws IOException {
        String query = "{" + "  \"aggs\" : {" + "    \"entries\" : {"
                + "       \"terms\" : { \"field\" : \"contenttype._keyword\",  \"size\" : "
                + Integer.MAX_VALUE + "}"
                + "     }" + "   }," + "   \"size\":0}";

        SearchSourceBuilder builder = SearchSourceBuilderUtil.getSearchSourceBuilder(query);

        Assert.assertNotNull(builder);
        Assert.assertTrue(builder.aggregations().getAggregatorFactories().size() == 1);
        Assert.assertTrue(
                builder.aggregations().getAggregatorFactories().get(0).getName().equals("entries"));
    }

}
