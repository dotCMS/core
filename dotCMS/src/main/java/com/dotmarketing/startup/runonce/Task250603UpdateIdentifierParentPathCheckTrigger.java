package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.sql.SQLException;

/**
 * This task updates the identifier_parent_path_check trigger to validate
 * existing folders ignoring upper/lower casing in names and paths
 */
public class Task250603UpdateIdentifierParentPathCheckTrigger implements StartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DotConnect dotConnect = new DotConnect();

            //Creates a composite index to improve performance in queries filtering by LOWER(parent_path) and LOWER(asset_name)
            //Helpful for queries in FixTask00090RecreateMissingFoldersInParentPath
            dotConnect.executeStatement("CREATE INDEX IF NOT EXISTS idx_identifier_composite_lower ON identifier \n"
                    + "(host_inode, asset_type, LOWER(parent_path), LOWER(asset_name))");

            //Updates function trigger to support case-insensitive folder lookup
            final String trigger = "CREATE OR REPLACE FUNCTION identifier_parent_path_check()  RETURNS trigger AS '\n"
                    + "DECLARE\n"
                    + "    folderId varchar(100);\n"
                    + "  BEGIN\n"
                    + "     IF (tg_op = ''INSERT'' OR tg_op = ''UPDATE'') THEN\n"
                    + "      IF(NEW.parent_path=''/'') OR (NEW.parent_path=''/System folder'') THEN\n"
                    + "        RETURN NEW;\n"
                    + "     ELSE\n"
                    + "      select id into folderId from identifier where asset_type=''folder'' and host_inode = NEW.host_inode and lower(parent_path||asset_name||''/'') = lower(NEW.parent_path) and id <> NEW.id;\n"
                    + "      IF FOUND THEN\n"
                    + "        RETURN NEW;\n"
                    + "      ELSE\n"
                    + "        RAISE EXCEPTION ''Cannot % folder % [%] in path % as one or more parent folders do not exist in Site %'', tg_op, NEW.asset_name, NEW.id, NEW.parent_path, NEW.host_inode;\n"
                    + "        RETURN NULL;\n"
                    + "      END IF;\n"
                    + "     END IF;\n"
                    + "    END IF;\n"
                    + "RETURN NULL;\n"
                    + "END\n" +
                    "' LANGUAGE plpgsql;\n";

                dotConnect.executeStatement(trigger);
        } catch (SQLException e) {
            throw new DotDataException(e);
        }
    }
}
