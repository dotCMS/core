package com.dotmarketing.portlets.cmsmaintenance.factories;

import com.dotcms.content.elasticsearch.business.DropOldContentletRunner;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This tool allows CMS users to delete entries of versionable dotCMS objects
 * that are older than a specified date. These versions can be accessed through
 * the <b>History</b> tab of such objects. The list of elements that are
 * versionable in the system are:
 * <ul>
 * <li>Contentlets - including HTML Pages as Content, Files as Content, Sites,
 * etc.</li>
 * <li>Containers.</li>
 * <li>Templates.</li>
 * <li>Links.</li>
 * <li>Workflow History.</li>
 * </ul>
 *
 * @author root
 * @since Mar 22, 2012
 */
public class CMSMaintenanceFactory {

    /**
     * Deletes dotCMS object versions that are older than the specified date. Such a
     * date is compared to the date in which a version of an object was created. The
     * list of objects may include:
     * <ul>
     * <li>Contentlets - including HTML Pages as Content, Files as Content, Sites,
     * etc.</li>
     * <li>Containers.</li>
     * <li>Templates.</li>
     * <li>Links.</li>
     * <li>Workflow History.</li>
     * </ul>
     *
     * @param assetsOlderThan
     *            The user-specified date up to which object versions will be
     *            deleted.
     * @return The total number of versions that were deleted.
     */
    public static int deleteOldAssetVersions(final Date assetsOlderThan) {
        Logger.info(CMSMaintenanceFactory.class, "------------------------------------------------------------");
        Logger.info(CMSMaintenanceFactory.class, " Executing the Drop Old Assets Versions tool");

        int totalRecords = new DropOldContentletRunner(assetsOlderThan).deleteOldContent();






        Calendar startDate = Calendar.getInstance();
        startDate.setTime(assetsOlderThan);
        startDate.add(Calendar.YEAR, -2);
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);
        final Calendar oldestInodeDate = getOldestInodeDate();
        startDate = null != oldestInodeDate ? oldestInodeDate : startDate;
        Logger.info(CMSMaintenanceFactory.class, String.format("- Deleting versions older than '%s'",
                UtilMethods.dateToHTMLDate(assetsOlderThan, "yyyy-MM-dd")));
        Logger.info(CMSMaintenanceFactory.class, " ");

        while (startDate.getTime().before(assetsOlderThan) || startDate.getTime().equals(assetsOlderThan)) {
            try {
                HibernateUtil.startTransaction();
                final int deletedRecords = removeOldVersions(startDate);
                // Run the drop tasks iteratively, moving forward in time
                // DROP_OLD_ASSET_ITERATE_BY_SECONDS controls how many seconds to move forward
                // in time for each iteration - default is to iterate by 30 days
                startDate.add(Calendar.SECOND,
                        Config.getIntProperty("DROP_OLD_ASSET_ITERATE_BY_SECONDS", 60 * 60 * 24 * 30));
                // We should never go past the date the user entered
                totalRecords += deletedRecords;
                HibernateUtil.commitTransaction();
                if (startDate.getTime().after(assetsOlderThan)) {
                    break;
                }
            } catch (final Exception ex) {
                Logger.error(CMSMaintenanceFactory.class,
                        String.format("An error occurred when deleting old asset versions: %s", ex.getMessage()), ex);
                try {
                    HibernateUtil.rollbackTransaction();
                } catch (final DotHibernateException e) {
                    Logger.error(CMSMaintenanceFactory.class,
                            String.format("An error occurred when rolling the transaction back: %s", e.getMessage()),
                            e);
                }
                return -1;
            }
        }
        if (totalRecords > 0) {
            CacheLocator.getCacheAdministrator().flushAll();
        }
        Logger.info(CMSMaintenanceFactory.class, String.format(
                " The Drop Old Assets Versions tool has run. A total of %d records were deleted!", totalRecords));
        return totalRecords;
    }

    /**
     * Looks for the oldest last modified date of versionable objects in the system.
     * Such a date will allow the tool to start deleting from the oldest to the
     * newest version. If no date is found, the current date will be returned.
     *
     * @return The oldest modification date from a versionable object.
     */
    private static Calendar getOldestInodeDate() {
        Calendar startDate = null;
        final DotConnect dc = new DotConnect();
        dc.setSQL(
                "SELECT MIN(idate) AS min_date FROM inode WHERE type IN ('contentlet', 'containers', 'template')");
        dc.setMaxRows(1);
        try {
            final List<Map<String, Object>> results = dc.loadObjectResults();
            final Date minDate = (Date) results.get(0).get("min_date");
            if (null != minDate) {
                startDate = Calendar.getInstance();
                startDate.setTime(minDate);
            }
        } catch (final DotDataException e) {
            Logger.error(CMSMaintenanceFactory.class, String.format("An error occurred when finding the minimum date." +
                    " Using date specified by user instead: '%s'", e.getMessage()), e);
        }
        return startDate;
    }

    /**
     * Deletes all the versions of dotCMS objects that are older than the specified
     * date.
     *
     * @param assetsOlderThan The date up to which object versions will be deleted.
     *
     * @return The total The total number of versions that were deleted for the specified date.
     *
     * @throws DotDataException An error occurred when deleting the data.
     */
    private static int removeOldVersions(final Calendar assetsOlderThan) throws DotDataException {
        int deletedRecords = 0;
        int totalRecords = 0;
        final String statusMsg = " Removed %d old %s";
        Logger.info(CMSMaintenanceFactory.class, String.format("-> Dropping versions older than '%s':",
                UtilMethods.dateToHTMLDate(assetsOlderThan.getTime(), "yyyy-MM-dd")));


        deletedRecords = APILocator.getContainerAPI().deleteOldVersions(assetsOlderThan.getTime());
        if (deletedRecords > 0) {
            Logger.info(CMSMaintenanceFactory.class, String.format(statusMsg, deletedRecords, "Containers"));
            totalRecords += deletedRecords;
        }

        deletedRecords = APILocator.getTemplateAPI().deleteOldVersions(assetsOlderThan.getTime());
        if (deletedRecords > 0) {
            Logger.info(CMSMaintenanceFactory.class, String.format(statusMsg, deletedRecords, "Templates"));
            totalRecords += deletedRecords;
        }

        deletedRecords = APILocator.getMenuLinkAPI().deleteOldVersions(assetsOlderThan.getTime());
        if (deletedRecords > 0) {
            Logger.info(CMSMaintenanceFactory.class, String.format(statusMsg, deletedRecords, "Links"));
            totalRecords += deletedRecords;
        }

        deletedRecords = APILocator.getWorkflowAPI().deleteWorkflowHistoryOldVersions(assetsOlderThan.getTime());
        if (deletedRecords > 0) {
            Logger.info(CMSMaintenanceFactory.class, String.format(statusMsg, deletedRecords, "Workflow History entries"));
            totalRecords += deletedRecords;
        }

        if (totalRecords > 0) {
            Logger.info(CMSMaintenanceFactory.class,
                    "-> Finished removing old versions. Total objects deleted: " + totalRecords);
        } else {
            Logger.info(CMSMaintenanceFactory.class, "-> No records were found!");
        }
        Logger.info(CMSMaintenanceFactory.class, " ");
        return totalRecords;
    }

}
