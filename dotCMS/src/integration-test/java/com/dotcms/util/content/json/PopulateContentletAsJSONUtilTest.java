package com.dotcms.util.content.json;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import org.apache.felix.framework.OSGIUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class PopulateContentletAsJSONUtilTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        if (!OSGIUtil.getInstance().isInitialized()) {
            OSGIUtil.getInstance().initializeFramework();
        }
    }

    private void removeContentletAsJSONColumn() throws DotDataException {

        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        final DotConnect dotConnect = new DotConnect();
        try {
            dotConnect.setSQL("alter table contentlet drop column contentlet_as_json");
            dotConnect.loadResult();
        } catch (DotDataException e) {
            Logger.error(this, "Error removing contentlet_as_json column", e);
        }
    }

    private void createContentletAsJSONColumn() throws DotDataException {

        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        final String type;
        if (DbConnectionFactory.isPostgres()) {
            type = "JSONB";
        } else {
            type = "NVARCHAR(MAX)";
        }

        final DotConnect dotConnect = new DotConnect();
        try {
            dotConnect.setSQL(String.format("ALTER TABLE contentlet ADD contentlet_as_json %s", type));
            dotConnect.loadResult();
        } catch (DotDataException e) {
            Logger.error(this, "Error creating contentlet_as_json column", e);
        }
    }

    /**
     * Method to test: {@link PopulateContentletAsJSONUtil#populateForAssetSubType(String)}
     *
     * @throws SQLException
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void Test_populate_host() throws SQLException, DotDataException, IOException {

        Collection<Host> hosts = new ArrayList<>();

        try {

            // First we need to create some hosts
            for (int i = 0; i < 10; i++) {
                hosts.add(new SiteDataGen().nextPersisted(true));
            }

            // We drop the contentlet_as_json column
            removeContentletAsJSONColumn();

            // And we add it again
            createContentletAsJSONColumn();

            // Make sure we have the column but with not content
            final DotConnect dotConnect = new DotConnect();
            var results = dotConnect.setSQL("select c.* " +
                            "from contentlet c " +
                            "         JOIN identifier i ON i.id = c.identifier " +
                            "         JOIN contentlet_version_info cv ON i.id = cv.identifier AND " +
                            "                                            (c.inode = cv.working_inode OR c.inode = cv.live_inode) " +
                            "WHERE i.asset_subtype = 'Host'")
                    .loadObjectResults();

            // Make sure we have the right number of hosts
            assertTrue(results.size() >= 10);

            results.forEach(rowMap -> {
                assertTrue(rowMap.containsKey("contentlet_as_json"));
                assertNull(rowMap.get("contentlet_as_json"));
            });

            // Now we execute the task
            new PopulateContentletAsJSONUtil().populateForAssetSubType("Host");

            results = dotConnect.setSQL("select c.* " +
                            "from contentlet c " +
                            "         JOIN identifier i ON i.id = c.identifier " +
                            "         JOIN contentlet_version_info cv ON i.id = cv.identifier AND " +
                            "                                            (c.inode = cv.working_inode OR c.inode = cv.live_inode) " +
                            "WHERE i.asset_subtype = 'Host'")
                    .loadObjectResults();

            // Make sure we have the right number of hosts again
            assertTrue(results.size() >= 10);

            // This time contentlet_as_json can not be null
            results.forEach(rowMap -> {
                assertTrue(rowMap.containsKey("contentlet_as_json"));
                assertNotNull(rowMap.get("contentlet_as_json"));
            });
        } finally {
            // Clean up
            hosts.forEach(ContentletDataGen::destroy);
        }
    }

    /**
     * Method to test: {@link PopulateContentletAsJSONUtil#populateExcludingAssetSubType(String)}
     *
     * @throws SQLException
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void Test_populate_All_excluding_host() throws SQLException, DotDataException, IOException {

        Collection<Contentlet> contents = new ArrayList<>();

        try {

            var defaultLanguageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

            // First we need to create some contentlets
            for (int i = 0; i < 10; i++) {
                contents.add(TestDataUtils.getGenericContentContent(true, defaultLanguageId));
            }

            // We drop the contentlet_as_json column
            removeContentletAsJSONColumn();

            // And we add it again
            createContentletAsJSONColumn();

            // Make sure we have the column but with not content
            final DotConnect dotConnect = new DotConnect();
            var results = dotConnect.setSQL("select c.* " +
                            "from contentlet c " +
                            "         JOIN identifier i ON i.id = c.identifier " +
                            "         JOIN contentlet_version_info cv ON i.id = cv.identifier AND " +
                            "                                            (c.inode = cv.working_inode OR c.inode = cv.live_inode) " +
                            "WHERE i.asset_subtype <> 'Host' AND asset_type = 'contentlet'")
                    .loadObjectResults();

            // Make sure we have the right number of hosts
            assertTrue(results.size() >= 10);

            results.forEach(rowMap -> {
                assertTrue(rowMap.containsKey("contentlet_as_json"));
                assertNull(rowMap.get("contentlet_as_json"));
            });

            // Now we execute the task
            new PopulateContentletAsJSONUtil().populateExcludingAssetSubType("Host");

            results = dotConnect.setSQL("select c.* " +
                            "from contentlet c " +
                            "         JOIN identifier i ON i.id = c.identifier " +
                            "         JOIN contentlet_version_info cv ON i.id = cv.identifier AND " +
                            "                                            (c.inode = cv.working_inode OR c.inode = cv.live_inode) " +
                            "WHERE i.asset_subtype <> 'Host' AND asset_type = 'contentlet'")
                    .loadObjectResults();

            // Make sure we have the right number of hosts again
            assertTrue(results.size() >= 10);

            // This time contentlet_as_json can not be null
            results.forEach(rowMap -> {
                assertTrue(rowMap.containsKey("contentlet_as_json"));
                assertNotNull(rowMap.get("contentlet_as_json"));
            });
        } finally {
            // Clean up
            contents.forEach(ContentletDataGen::destroy);
        }
    }

}
