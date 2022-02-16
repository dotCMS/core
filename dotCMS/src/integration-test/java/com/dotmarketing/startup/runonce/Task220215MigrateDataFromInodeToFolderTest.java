package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.InodeUtils;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;


public class Task220215MigrateDataFromInodeToFolderTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private Inode getInode() throws DotDataException, DotSecurityException {

        final String inode = UUID.randomUUID().toString();
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL("insert into inode (inode, idate, owner, type) values (?,?,?,'folder')");
        dotConnect.addParam(inode);
        dotConnect.addParam(new java.util.Date());
        dotConnect.addParam("test");
        dotConnect.loadResult();

        return InodeUtils.getInode(inode);
    }

    private boolean areColumnsPopulated(final Inode inode)
            throws DotDataException {

            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL("select owner, idate from folder where inode=?");
            dotConnect.addParam(inode.getInode());
            List<Map<String, Object>> results = dotConnect.loadObjectResults();

            if (!areValuesEqual(inode, results.get(0))){
                return false;
            }

        return true;
    }

    private boolean areValuesEqual(final Inode expectedResult, final Map<String, Object> result){
        final LocalDateTime expectedDate = new Timestamp(
                expectedResult.getIDate().getTime()).toLocalDateTime().truncatedTo(
                ChronoUnit.SECONDS);
        final LocalDateTime resultDate = new Timestamp(
                ((Date)result.get("idate")).getTime()).toLocalDateTime().truncatedTo(
                ChronoUnit.SECONDS);

        return doesOwnerMatch(expectedResult, result) && resultDate.equals(expectedDate);
    }

    private boolean doesOwnerMatch(final Inode expectedResult, final Map<String, Object> result) {
        return (expectedResult.getOwner() == null && result.get("owner") == null) || result
                .get("owner").equals(expectedResult.getOwner());
    }

    /**
     * Method to Test: {@link Task220215MigrateDataFromInodeToFolder#executeUpgrade()}
     * When: Run the Upgrade Task
     * Should: Populate columns owner and idate of the folder table
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void test_upgradeTask_success() throws DotDataException, DotSecurityException {
        final Inode inode = getInode();
        final Task220215MigrateDataFromInodeToFolder task = new Task220215MigrateDataFromInodeToFolder();
        task.executeUpgrade();
        assertTrue(areColumnsPopulated(inode));
    }

}
