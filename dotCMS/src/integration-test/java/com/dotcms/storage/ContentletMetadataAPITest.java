package com.dotcms.storage;

import com.dotcms.util.IntegrationTestInitService;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContentletMetadataAPITest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();


    }


    @Test
    public void Test_put_Secret_Then_Verify_Get_From_Cache(){
    }

}
