package com.dotmarketing.fixtask.tasks;

import org.junit.BeforeClass;
import com.dotcms.UnitTestBase;
import com.dotcms.util.IntegrationTestInitService;

public class FixTask00100DeleteUnlinkedContentletAssetsTest extends UnitTestBase {

    private static FixTask00100DeleteUnlinkedContentletAssets fixTask;


    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        fixTask= new FixTask00100DeleteUnlinkedContentletAssets();
    }

    
    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        fixTask= new FixTask00100DeleteUnlinkedContentletAssets();
    }

}
