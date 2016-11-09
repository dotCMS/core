package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

public class Task03715AddFKForPublishingBundleTable implements StartupTask {

    private void createMissingFKForPublishingBundleTables(DotConnect dc) throws SQLException, DotDataException {
        if(DbConnectionFactory.isMsSql()) {
        	//Fix Inconsistent Data
        	dc.executeStatement("UPDATE publishing_bundle SET owner = 'system' WHERE owner NOT IN ( SELECT userid FROM user_ );");

        	//Add Foreign Key
            dc.executeStatement("ALTER TABLE publishing_bundle ADD CONSTRAINT FK_publishing_bundle_owner FOREIGN KEY (owner) REFERENCES user_(userid);");
        }else if(DbConnectionFactory.isOracle()) {
        	//Fix Inconsistent Data
        	dc.executeStatement("UPDATE publishing_bundle SET owner = 'system' WHERE owner NOT IN ( SELECT userid FROM user_ );");

        	//Add Foreign Key
            dc.executeStatement("ALTER TABLE publishing_bundle ADD CONSTRAINT FK_publishing_bundle_owner FOREIGN KEY (owner) REFERENCES user_(userid);");
        }else if(DbConnectionFactory.isMySql()) {
        	//Fix Inconsistent Data
        	dc.executeStatement("UPDATE publishing_bundle SET owner = 'system' WHERE owner NOT IN ( SELECT userid FROM user_ );");

        	//Add Foreign Key
            dc.executeStatement("ALTER TABLE publishing_bundle ADD CONSTRAINT FK_publishing_bundle_owner FOREIGN KEY (owner) REFERENCES user_(userid);");
        }else if(DbConnectionFactory.isPostgres()) {
        	//Fix Inconsistent Data
        	dc.executeStatement("UPDATE publishing_bundle SET owner = 'system' WHERE owner NOT IN ( SELECT userid FROM user_ );");

        	//Add Foreign Key
            dc.executeStatement("ALTER TABLE publishing_bundle ADD CONSTRAINT FK_publishing_bundle_owner FOREIGN KEY (owner) REFERENCES user_(userid);");
        }else if(DbConnectionFactory.isH2()) {
        	//Fix Inconsistent Data
        	dc.executeStatement("UPDATE publishing_bundle SET owner = 'system' WHERE owner NOT IN ( SELECT userid FROM user_ );");

        	//Add Foreign Key
            dc.executeStatement("ALTER TABLE publishing_bundle ADD CONSTRAINT FK_publishing_bundle_owner FOREIGN KEY (owner) REFERENCES user_(userid);");
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        try {
            DotConnect dc=new DotConnect();
            createMissingFKForPublishingBundleTables(dc);
        } catch (SQLException e) {
            throw new DotRuntimeException(e.getMessage(),e);
        }

    }

    @Override
    public boolean forceRun() {
        return true;
    }

}
