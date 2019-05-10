package com.dotcms.rest.api.v1.tag;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.UUIDGenerator;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;

@RunWith(DataProviderRunner.class)
public class TagResourceIntegrationTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    // todo generate known tag names to check in the tests 
    List<String> testTagNames = list(UUIDGenerator.generateUuid())

    @DataProvider
    public static Object[] listTestCases() {
        return
    }


    @Test
    @UseDataProvider("listTestCases")
    public void testList() {

    }

}
