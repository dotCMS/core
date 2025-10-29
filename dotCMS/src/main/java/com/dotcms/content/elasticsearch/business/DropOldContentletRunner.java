package com.dotcms.content.elasticsearch.business;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;
import io.vavr.control.Try;
import java.io.File;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


public class DropOldContentletRunner implements Runnable {

    final Date finalEndDate;

    public DropOldContentletRunner(Date finalEndDate) {
        this.finalEndDate = finalEndDate;
    }

    public void run() {
        deleteOldContent();
    }

    final String SELECT_OLD_CONTENT_INODES = "SELECT c.inode FROM contentlet c"
            + " WHERE "
            + " (c.identifier <> 'SYSTEM_HOST' AND c.identifier IS NOT NULL) "
            + " AND c.mod_date >= ? "
            + " AND c.mod_date <= ? "
            + " AND NOT EXISTS "
            + " ("
            + "    SELECT 1 FROM contentlet_version_info vi"
            + "    WHERE "
            + "    vi.working_inode = c.inode "
            + "    OR vi.live_inode = c.inode"
            + " )"
            + " ORDER BY c.mod_date ASC"
            + " LIMIT ?";

    final String DELETE_CONTENT_DATA = "DELETE FROM contentlet WHERE inode IN (%s)";
    final String DELETE_CONTENT_INODE = "DELETE FROM  inode where inode IN (%s)";
    final String DELETE_TAG_INODES = "DELETE FROM tag_inode where inode IN (%s)";
    final String CREATE_INDEX_CONTENTLET_MOD_DATE = "CREATE INDEX if not exists idx_contentlet_mod_date on contentlet(mod_date)";

    final String EARLIEST_CONTENTLET_DATE = "SELECT min(mod_date) as start_date from contentlet where identifier <> 'SYSTEM_HOST' and identifier is not null";

    final boolean CLEAN_DEAD_INODE_FROM_FS = Config.getBooleanProperty("DROP_OLD_ASSET_CLEAN_DEAD_INODE_FROM_FS", true);
    final boolean DROP_OLD_ASSET_DRY_RUN = Config.getBooleanProperty("DROP_OLD_ASSET_DRY_RUN", false);
    final long SLEEP_BETWEEN_RUNS_MS = Config.getLongProperty("DROP_OLD_ASSET_SLEEP_BETWEEN_RUNS_MS", 100);

    final int DROP_OLD_ASSET_BATCH_SIZE = Config.getIntProperty("DROP_OLD_ASSET_BATCH_SIZE", 100);
    final int DAYS_TO_ITERATE = Config.getIntProperty("DROP_OLD_ASSET_ITERATE_BY_DAYS", 7);

    final int MAX_RUNTIME_SECONDS = Config.getIntProperty("DROP_OLD_ASSET_MAX_RUNTIME_SECONDS", 0);

    final Date STOP_RUNNING_AT =
            MAX_RUNTIME_SECONDS > 0 ? Date.from(Instant.now().plus(MAX_RUNTIME_SECONDS, ChronoUnit.SECONDS))
                    : Date.from(Instant.now().plus(100, ChronoUnit.DAYS));


    Date earliestContentlet() {
        try (Connection conn = DbConnectionFactory.getConnection()) {

            return Try.of(() -> {
                        return (Date) new DotConnect()
                                .setSQL(EARLIEST_CONTENTLET_DATE)
                                .loadObjectResults(conn)
                                .get(0)
                                .get("start_date");
                    }
            ).getOrElse(Date.from(Instant.parse("2010-01-01T00:00:00Z")));
        } catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    List<String> loadInodes(Date startDate, Date endDate, Connection conn) {
        try {
            return new DotConnect()
                    .setSQL(SELECT_OLD_CONTENT_INODES)
                    .addParam(startDate)
                    .addParam(endDate)
                    .addParam(DROP_OLD_ASSET_BATCH_SIZE)
                    .loadObjectResults(conn)
                    .stream()
                    .map(row -> (String) row.get("inode"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    void createIndexIfNeeded() {
        try (Connection conn = DbConnectionFactory.getConnection()) {
            DotConnect dc = new DotConnect();
            Logger.info(this, "Creating index on contentlet.mod_date");
            dc.setSQL(CREATE_INDEX_CONTENTLET_MOD_DATE);
            dc.loadResult(conn);
        } catch (Exception e) {
            Logger.error(this, "Error creating index on contentlet.mod_date", e);
            throw new DotRuntimeException(e);
        }

    }


    public int deleteOldContent() {
        int deleted = 0;
        Logger.info(this, "------- Delete Old Assets Started ------- ");
        Logger.info(this, "Configuration:");
        Logger.info(this, "  DROP_OLD_ASSET_BATCH_SIZE: " + DROP_OLD_ASSET_BATCH_SIZE);
        Logger.info(this, "  DAYS_TO_ITERATE: " + DAYS_TO_ITERATE);
        Logger.info(this, "  FINAL_END_DATE: " + finalEndDate);
        Logger.info(this, "  CLEAN_DEAD_INODE_FROM_FS: " + CLEAN_DEAD_INODE_FROM_FS);
        Logger.info(this, "  SLEEP_BETWEEN_RUNS_MS: " + SLEEP_BETWEEN_RUNS_MS);
        Logger.info(this, "  MAX_RUNTIME_SECONDS: " + MAX_RUNTIME_SECONDS);
        Logger.info(this, "----------------------------------------------------");

        createIndexIfNeeded();

        if (isInterrupted()) {
            return deleted;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date startIterationDate = earliestContentlet();
        Date endInterationDate = Date.from(startIterationDate.toInstant().plus(DAYS_TO_ITERATE, ChronoUnit.DAYS));
        Logger.info(this, "START: Deleting contentlet older than " + sdf.format(finalEndDate));
        Logger.info(this, "     : earliest contentlet " + sdf.format(startIterationDate));

        int i = 0;
        final List<String> inodeList = new ArrayList<>();
        while (endInterationDate.getTime() < finalEndDate.getTime()) {
            if (isInterrupted()) {
                return deleted;
            }
            Logger.info(this,
                    "Looking for old contentlet from " + sdf.format(startIterationDate) + " to " + sdf.format(
                            endInterationDate));

            try (Connection conn = DbConnectionFactory.getConnection()) {
                inodeList.addAll(loadInodes(startIterationDate, endInterationDate, conn));
                while (!inodeList.isEmpty()) {
                    if (isInterrupted()) {
                        return deleted;
                    }
                    if (DROP_OLD_ASSET_DRY_RUN) {
                        Logger.info(this, " --- FOUND  " + inodeList.size() + " old contentlets");
                        deleted += inodeList.size();
                        break;
                    }
                    Logger.info(this, " --- DELETING " + inodeList.size() + " old contentlets");
                    conn.setAutoCommit(false);
                    String inodes = String.join("','", inodeList);

                    inodes = "'" + inodes + "'";
                    DotConnect dc = new DotConnect();
                    deleted += dc.executeUpdate(conn, String.format(DELETE_CONTENT_DATA, inodes));

                    dc.setSQL(String.format(DELETE_CONTENT_INODE, inodes));
                    dc.loadResult(conn);

                    dc.setSQL(String.format(DELETE_TAG_INODES, inodes));
                    dc.loadResult(conn);

                    conn.commit();
                    conn.setAutoCommit(true);

                    deleteFromAssetsDir(inodeList);

                    inodeList.clear();
                    if (isInterrupted()) {
                        return deleted;
                    }
                    Thread.sleep(SLEEP_BETWEEN_RUNS_MS);
                    inodeList.addAll(loadInodes(startIterationDate, endInterationDate, conn));

                }

            } catch (Exception e) {
                Logger.error(this,
                        "Error deleting old content from " + sdf.format(startIterationDate) + " to " + sdf.format(
                                endInterationDate),
                        e);

                throw new DotRuntimeException(e);
            }

            inodeList.clear();
            startIterationDate = endInterationDate;
            endInterationDate = Date.from(startIterationDate.toInstant().plus(DAYS_TO_ITERATE, ChronoUnit.DAYS));
            endInterationDate = endInterationDate.before(finalEndDate) ? endInterationDate : finalEndDate;

        }
        Logger.info(this, "END: Deleted " + deleted + " old contentlet(s)");
        return deleted;
    }

    boolean deleteFromAssetsDir(List<String> inodes) {
        if (!CLEAN_DEAD_INODE_FROM_FS) {
            return true;
        }
        for (String inode : inodes) {
            String path = APILocator.getFileAssetAPI().getRealAssetPath(inode)
                    .replace(FileAssetAPI.BINARY_FIELD + File.separator, "");

            Logger.info(this, "Deleting file asset " + path);
            try {
                FileUtil.deltree(path);
            } catch (Exception e) {
                Logger.error(this, "Error deleting file asset " + path, e);
                return false;
            }


        }
        return true;
    }


    boolean isInterrupted() {
        boolean stopping =
                STOP_RUNNING_AT.getTime() < System.currentTimeMillis() || Thread.currentThread().isInterrupted();
        if (stopping) {
            Logger.info(this, "DropOldAssetsRunner is stopping");
        }
        return stopping;
    }


}
