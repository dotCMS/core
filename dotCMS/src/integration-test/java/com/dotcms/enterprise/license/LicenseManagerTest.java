package com.dotcms.enterprise.license;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class LicenseManagerTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void test_checkServerDuplicity_community() throws Exception {
        try {
            LicenseManager.getInstance().freeLicenseOnRepo();
            //assertEquals(LicenseLevel.COMMUNITY.level, LicenseManager.getInstance().license.level);
            assertFalse(LicenseManager.getInstance().checkServerDuplicity());
        } finally {
            LicenseManager.getInstance().takeLicenseFromRepoIfNeeded();
        }
    }

    @Test
    public void test_checkServerDuplicity_noDups() throws Exception {
        LicenseManager.getInstance().updateServerStartTime();
        //assertNotEquals(LicenseLevel.COMMUNITY.level, LicenseManager.getInstance().license.level);
        assertFalse(LicenseManager.getInstance().checkServerDuplicity());
    }

    @Test
    public void test_checkServerDuplicity_dups() throws Exception {
        final String serverId = APILocator.getServerAPI().readServerId();
        final DotConnect dotConnect = new DotConnect();
        final String id = UUID.randomUUID().toString();
        boolean added = false;
        try {
            dotConnect
                    .setSQL("SELECT * FROM sitelic WHERE serverid = ? AND license = ?")
                    .addParam(serverId)
              //      .addParam(LicenseManager.getInstance().license.raw)
                    ;

            final List<Map<String, Object>> results = dotConnect.loadResults();
            final Map<String, Object> first = results.get(0);
            dotConnect.setSQL("INSERT INTO sitelic (id, license, serverid, lastping, startup_time) VALUES (?, ?, ?, ?, ?)")
                    .addParam(id)
                    .addParam(first.get("license"))
                    .addParam(serverId)
                    .addParam(new Date())
                    .addParam(System.currentTimeMillis())
                    .loadResult();
            added = true;

          //  assertNotEquals(LicenseLevel.COMMUNITY.level, LicenseManager.getInstance().license.level);
            assertTrue(LicenseManager.getInstance().checkServerDuplicity());
        } finally {
            if (added) {
                dotConnect.setSQL("DELETE FROM sitelic WHERE id = ?").addParam(id).loadResult();
            }
        }
    }

    @Test
    public void test_updateServerStartTime() throws Exception {
        final String serverId = APILocator.getServerAPI().readServerId();
        final DotConnect dotConnect = new DotConnect();
        Map<String, Object> row = dotConnect
                .setSQL("SELECT startup_time FROM sitelic WHERE serverid = ?")
                .addParam(serverId)
                .loadObjectResults()
                .get(0);
        final Long backThen = (Long) row.get("startup_time");
        LicenseManager.getInstance().updateServerStartTime();
        row = dotConnect
                .setSQL("SELECT startup_time FROM sitelic WHERE serverid = ?")
                .addParam(serverId)
                .loadObjectResults()
                .get(0);
        assertNotEquals(backThen, row.get("startup_time"));
    }

}
