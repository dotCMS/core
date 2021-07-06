package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fixes and inconsistency on Vanity URLs where the host_inode on the Identifier table is different
 * from the site field of the Vanity URL contentlet.
 * @author Jonathan Gamba 11/14/17
 */
public class Task04230FixVanityURLInconsistencies extends AbstractJDBCStartupTask {

    private static final String FIND_INCONSISTENCIES_QUERY =
            "SELECT contentlet.text2 as siteid, identifier.id as identifierid"
                    + " FROM contentlet, identifier"
                    + " WHERE contentlet.identifier = identifier.id"
                    + " AND EXISTS (select 1 from structure where structuretype=7 AND structure.inode = contentlet.structure_inode)"
                    + " AND contentlet.text2 != identifier.host_inode";

    private static final String UPDATE_IDENTIFIER_QUERY = "UPDATE identifier SET host_inode = ? WHERE id = ?";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    @CloseDBIfOpened
    public void executeUpgrade() throws DotDataException {

        final DotConnect dc = new DotConnect();

        //Verify if we have inconsistencies to process
        dc.setSQL(FIND_INCONSISTENCIES_QUERY);
        final List<Map<String, Object>> sqlResults = dc.loadObjectResults();
        if (null != sqlResults) {

            Logger.info(this,
                    String.format("Found [%s] inconsistencies on Vanity URLs executing query [%s]",
                            sqlResults.size(),
                            dc.getSQL()));

            for (final Map<String, Object> sqlResult : sqlResults) {

                final String identifierId = sqlResult.get("identifierid").toString();
                final String siteId = sqlResult.get("siteid").toString();

                //Update the host id of the identifier with the inconsistency
                updateIdentifierHostInode(identifierId, siteId);
            }
        }

    }

    @WrapInTransaction
    private void updateIdentifierHostInode(final String identifierId, final String hostInode)
            throws DotDataException {

        //Update identifier table
        final DotConnect dc = new DotConnect();
        dc.setSQL(UPDATE_IDENTIFIER_QUERY);
        dc.addParam(hostInode);
        dc.addParam(identifierId);
        Logger.info(this, String.format("Executing Update on Identifier table - "
                        + "identifier [%s] - Host inode [%s] - Query [%s]", identifierId, hostInode,
                dc.getSQL()));
        dc.loadResult();
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
    protected List<String> getTablesToDropConstraints() {
        return Collections.emptyList();
    }

}