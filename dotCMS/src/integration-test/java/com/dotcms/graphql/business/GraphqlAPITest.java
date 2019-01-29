package com.dotcms.graphql.business;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;

import org.junit.BeforeClass;
import org.junit.Test;

public class GraphqlAPITest {

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }



    @Test
    public void testGenerateSchema() throws DotDataException {
        GraphqlAPI api = APILocator.getGraphqlAPI();
        api.getSchema();
    }

}
