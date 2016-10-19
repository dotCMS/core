package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Jonathan Gamba
 *         Date: 1/27/16
 */
public class Task03525LowerTagsTagname extends AbstractJDBCStartupTask {

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);

            DotConnect dc = new DotConnect();

            dc.setSQL("SELECT lower(tagname) as tagname, host_id, COUNT(*) FROM tag GROUP BY lower(tagname), host_id HAVING COUNT(*) > 1");
            List<Map<String, Object>> duplicatedTagNameRows = dc.loadObjectResults();

            for (Map<String, Object> duplicatedTagNameRow : duplicatedTagNameRows) {
                String duplicatedLowerTagName = (String)duplicatedTagNameRow.get("tagname");
                String duplicatedHostId = (String)duplicatedTagNameRow.get("host_id");

                Logger.info(Task03525LowerTagsTagname.class, "Found duplicated tag/host: " + duplicatedLowerTagName + "/" + duplicatedHostId);

                dc.setSQL("SELECT tag_id FROM tag WHERE lower(tagname) = ? AND host_id = ?");
                dc.addParam(duplicatedLowerTagName);
                dc.addParam(duplicatedHostId);

                List<Map<String, Object>> duplicatedTagRows = dc.loadObjectResults();

                dc.setSQL("SELECT tag_id FROM tag WHERE tagname = ? AND host_id = ?", 1);
                dc.addParam(duplicatedLowerTagName);
                dc.addParam(duplicatedHostId);

                List<Map<String, Object>> lowerMatchRows = dc.loadObjectResults();

                String newTagId;

                if(lowerMatchRows != null && !lowerMatchRows.isEmpty()){
                    newTagId = lowerMatchRows.get(0).get("tag_id").toString();

                    Logger.info(Task03525LowerTagsTagname.class, "Found lower case tag_name: " + duplicatedLowerTagName + " with tag_id: " + newTagId);
                } else {
                    newTagId = UUIDGenerator.generateUuid();

                    Logger.info(Task03525LowerTagsTagname.class, "Creating lower case tag_name: " + duplicatedLowerTagName + " with tag_id: " + newTagId);

                    dc.setSQL("INSERT INTO tag(tag_id, tagname, host_id, user_id, persona, mod_date) VALUES (?, ?, ?, ?, ?, ?)");
                    dc.addParam(newTagId);
                    dc.addParam(duplicatedLowerTagName);
                    dc.addParam(duplicatedHostId);
                    dc.addParam("");
                    dc.addParam(false);
                    dc.addParam(new Date());

                    dc.loadResult();
                }

                for (Map<String, Object> duplicatedTagRow : duplicatedTagRows) {
                    String oldTagId = duplicatedTagRow.get("tag_id").toString();

                    if(!oldTagId.equals(newTagId)){
                        Logger.info(Task03525LowerTagsTagname.class, "Updating tag_inode set tag_id: " + newTagId + " old tag_id: " + oldTagId);

                        dc.setSQL("UPDATE tag_inode SET tag_id = ? WHERE tag_id = ?");
                        dc.addParam(newTagId);
                        dc.addParam(oldTagId);
                        dc.loadResult();

                        Logger.info(Task03525LowerTagsTagname.class, "Deleting tag WHERE tag_id: " + oldTagId);

                        dc.setSQL("DELETE FROM tag WHERE tag_id = ?");
                        dc.addParam(oldTagId);
                        dc.loadResult();
                    }
                }
            }

            Logger.info(Task03525LowerTagsTagname.class, "Updating tag, lower all tagnames");

            dc.setSQL("UPDATE tag SET tagname=LOWER(tagname)");
            dc.loadResult();
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public String getPostgresScript() {
        return null;
    }

    @Override
    public String getMySQLScript() {
        return null;
    }

    @Override
    public String getOracleScript() {
        return null;
    }

    @Override
    public String getMSSQLScript() {
        return null;
    }

    @Override
    public String getH2Script() {
        return null;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
