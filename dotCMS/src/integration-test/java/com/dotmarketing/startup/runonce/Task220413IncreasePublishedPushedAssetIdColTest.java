package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class Task220413IncreasePublishedPushedAssetIdColTest {

    private static final String ASSET_ID = "this-is-an-assetid-which-is-longer-than-expected";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Tests Task220413IncreasePublishedPushedAssetIdCol by inserting to published_pushed_assets with an assetId with
     * greater capacity that it can hold.
     * An exception is expected to be thrown.
     *
     * @throws DotDataException
     */
    @Test(expected = DotDataException.class)
    public void test_upgradeTask_noCapacity() throws DotDataException {
        rollbackToOriginal();
        insertPublishedAsset(ASSET_ID);
    }

    /**
     * Tests Task220413IncreasePublishedPushedAssetIdCol by inserting to published_pushed_assets with an assetId with
     * greater capacity that it used to hold.
     *
     * @throws DotDataException
     */
    @Test
    public void test_upgradeTask() throws DotDataException {
        final Task220413IncreasePublishedPushedAssetIdCol task = new Task220413IncreasePublishedPushedAssetIdCol();
        assertTrue(task.forceRun());

        task.executeUpgrade();

        try {
            insertPublishedAsset(ASSET_ID);
        } catch (DotDataException e) {
            Assert.fail();
        }
    }

    private void rollbackToOriginal() throws DotDataException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        final DotConnect dotConnect = new DotConnect();
        try {
            dotConnect
                    .setSQL("DELETE FROM publishing_pushed_assets")
                    .loadResult();
            dotConnect
                    .setSQL(Task220413IncreasePublishedPushedAssetIdCol.resolveAlterCommand(36))
                    .loadResult();
        } catch (DotDataException e){
            //Nah.
        }
    }

    private void insertPublishedAsset(final String assetId) throws DotDataException {
        new DotConnect().setSQL("INSERT INTO public.publishing_pushed_assets(" +
                        "bundle_id, asset_id, asset_type, push_date, environment_id, endpoint_ids, publisher)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)")
                .addParam(UUID.randomUUID())
                .addParam(assetId)
                .addParam("OSGI")
                .addParam(new Date())
                .addParam(UUID.randomUUID())
                .addParam(UUID.randomUUID())
                .addParam("com.dotcms.publisher.pusher.PushPublisher")
                .loadResult();
    }

}
