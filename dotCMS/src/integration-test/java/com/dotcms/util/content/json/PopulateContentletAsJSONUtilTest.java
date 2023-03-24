package com.dotcms.util.content.json;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import org.apache.felix.framework.OSGIUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;

public class PopulateContentletAsJSONUtilTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        if (!OSGIUtil.getInstance().isInitialized()) {
            OSGIUtil.getInstance().initializeFramework();
        }
    }

    @Test
    public void Test_populate_host() throws SQLException, DotDataException, IOException {
        PopulateContentletAsJSONUtil.getInstance().populateForAssetSubType("Host");
    }

    @Test
    public void Test_populate_All_excluding_host() throws SQLException, DotDataException, IOException {
        PopulateContentletAsJSONUtil.getInstance().populateExcludingAssetSubType("Host");
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup() {

    }

}
