package com.dotcms.util.content.json;

import com.dotcms.IntegrationTestBase;
import com.dotmarketing.exception.DotDataException;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;

public class PopulateContentletAsJSONUtilTest extends IntegrationTestBase {

    @Test
    public void Test_Run() throws SQLException, DotDataException, IOException {
        PopulateContentletAsJSONUtil.getInstance().populate("Host");
    }

}
