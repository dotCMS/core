package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.UUIDGenerator;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class Task04385UpdateCategoryKey implements StartupTask {

    private final String UPDATE_CATEGORY_KEY_SQL = "UPDATE category SET category_key = ? WHERE inode = ?";
    private final String SELECT_CATEGORY_KEY_EMPTY_SQL = "SELECT inode FROM category WHERE category_key is NULL or category_key = ''";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
            DotConnect dc = new DotConnect();

            dc.setSQL(SELECT_CATEGORY_KEY_EMPTY_SQL);
            List<Map<String, String>> results = dc.loadResults();

            for (Map<String, String> result : results) {
                dc.setSQL(UPDATE_CATEGORY_KEY_SQL);
                dc.addParam(UUIDGenerator.generateUuid());
                dc.addParam(result.get("inode"));
                dc.loadResult();
            }
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }
}
