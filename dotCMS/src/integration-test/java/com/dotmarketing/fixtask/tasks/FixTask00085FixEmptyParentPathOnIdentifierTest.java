package com.dotmarketing.fixtask.tasks;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.fixtask.tasks.FixTask00085FixEmptyParentPathOnIdentifier;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class FixTask00085FixEmptyParentPathOnIdentifierTest extends IntegrationTestBase {

    DotConnect dc = new DotConnect();
    private static String DUMMY_IDENTIFIER;
    private static IdentifierAPI identifierAPI;
    final String ASSET_NAME = "test" + System.currentTimeMillis() + ".jpg";

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        DUMMY_IDENTIFIER = UUIDGenerator.generateUuid();
        identifierAPI = APILocator.getIdentifierAPI();
    }


    private void removeTrigger() throws DotDataException {
        String sqlDropTrigger = "";
        if (DbConnectionFactory.isPostgres()) {
            sqlDropTrigger = "drop trigger if exists identifier_parent_path_trigger on identifier";
        } else if (DbConnectionFactory.isMySql()) {
            sqlDropTrigger = "DROP TRIGGER IF EXISTS check_parent_path_when_insert";
        } else if (DbConnectionFactory.isOracle()) {
            sqlDropTrigger = "drop trigger identifier_parent_path_check";
        } else if (DbConnectionFactory.isMsSql()) {
            sqlDropTrigger = "drop trigger check_identifier_parent_path";
        }
        dc.setSQL(sqlDropTrigger);
        dc.loadResult();
    }

    private void insertRecordOnIndentifier() throws DotDataException {
        dc.setSQL("insert into identifier (id,asset_name,host_inode,asset_type) values ('"
                + DUMMY_IDENTIFIER + "','" + ASSET_NAME + "','SYSTEM_HOST','contentlet')");
        dc.loadResult();
    }

    private void recreateTrigger() throws DotDataException {
        String sqlCreateTrigger = "";
        if (DbConnectionFactory.isPostgres()) {
            sqlCreateTrigger = "CREATE TRIGGER identifier_parent_path_trigger BEFORE INSERT OR UPDATE ON identifier FOR EACH ROW EXECUTE PROCEDURE identifier_parent_path_check()";
        } else if (DbConnectionFactory.isMySql()) {
            sqlCreateTrigger = "CREATE TRIGGER check_parent_path_when_insert  BEFORE INSERT\n"
                    + "on identifier\n"
                    + "FOR EACH ROW\n"
                    + "BEGIN\n"
                    + "DECLARE idCount INT;\n"
                    + "DECLARE canInsert boolean default false;\n"
                    + " select count(id)into idCount from identifier where asset_type='folder' and CONCAT(parent_path,asset_name,'/')= NEW.parent_path and host_inode = NEW.host_inode and id <> NEW.id;\n"
                    + " IF(idCount > 0 OR NEW.parent_path = '/' OR NEW.parent_path = '/System folder') THEN\n"
                    + "   SET canInsert := TRUE;\n"
                    + " END IF;\n"
                    + " IF(canInsert = FALSE) THEN\n"
                    + "  delete from Cannot_insert_for_this_path_does_not_exist_for_the_given_host;\n"
                    + " END IF;\n"
                    + "END\n"
                    + "#";
        } else if (DbConnectionFactory.isOracle()) {
            sqlCreateTrigger = "CREATE OR REPLACE TRIGGER identifier_parent_path_check\n"
                    + "AFTER INSERT OR UPDATE ON identifier\n"
                    + "DECLARE\n"
                    + "  rowcount varchar2(100);\n"
                    + "  assetIdentifier varchar2(100);\n"
                    + "  parentPath varchar2(255);\n"
                    + "  hostInode varchar2(100);\n"
                    + "BEGIN\n"
                    + "   for i in 1 .. check_parent_path_pkg.newRows.count LOOP\n"
                    + "      select id,parent_path,host_inode into assetIdentifier,parentPath,hostInode from identifier where rowid = check_parent_path_pkg.newRows(i);\n"
                    + "      IF(parentPath='/' OR parentPath='/System folder') THEN\n"
                    + "        return;\n"
                    + "      ELSE\n"
                    + "        select count(*) into rowcount from identifier where asset_type='folder' and host_inode = hostInode and parent_path||asset_name||'/' = parentPath and id <> assetIdentifier;\n"
                    + "        IF (rowcount = 0) THEN\n"
                    + "           RAISE_APPLICATION_ERROR(-20000, 'Cannot insert/update for this path does not exist for the given host');\n"
                    + "        END IF;\n"
                    + "      END IF;\n"
                    + "END LOOP;\n"
                    + "END;\n"
                    + "/";
        } else if (DbConnectionFactory.isMsSql()) {
            sqlCreateTrigger = "CREATE Trigger check_identifier_parent_path\n"
                    + " ON identifier\n"
                    + " FOR INSERT,UPDATE AS\n"
                    + " DECLARE @folderId NVARCHAR(100)\n"
                    + " DECLARE @id NVARCHAR(100)\n"
                    + " DECLARE @assetType NVARCHAR(100)\n"
                    + " DECLARE @parentPath NVARCHAR(255)\n"
                    + " DECLARE @hostInode NVARCHAR(100)\n"
                    + " DECLARE cur_Inserted cursor LOCAL FAST_FORWARD for\n"
                    + " Select id,asset_type,parent_path,host_inode\n"
                    + "  from inserted\n"
                    + "  for Read Only\n"
                    + " open cur_Inserted\n"
                    + " fetch next from cur_Inserted into @id,@assetType,@parentPath,@hostInode\n"
                    + " while @@FETCH_STATUS <> -1\n"
                    + " BEGIN\n"
                    + "  IF(@parentPath <>'/' AND @parentPath <>'/System folder')\n"
                    + "  BEGIN\n"
                    + "    select @folderId = id from identifier where asset_type='folder' and host_inode = @hostInode and parent_path+asset_name+'/' = @parentPath and id <>@id\n"
                    + "    IF (@folderId IS NULL)\n"
                    + "     BEGIN\n"
                    + "       RAISERROR (N'Cannot insert/update for this path does not exist for the given host', 10, 1)\n"
                    + "       ROLLBACK WORK\n"
                    + "     END\n"
                    + "  END\n"
                    + " fetch next from cur_Inserted into @id,@assetType,@parentPath,@hostInode\n"
                    + "END;";
        }
        dc.setSQL(SQLUtil.tokenize(sqlCreateTrigger).get(0));
        dc.loadResult();
    }

    @Test
    public void testExecuteFix() throws DotDataException {
        try {
            removeTrigger();
            insertRecordOnIndentifier();
            Identifier identifier = identifierAPI.find(DUMMY_IDENTIFIER);
            Assert.assertTrue(identifier.getParentPath() == null || identifier.getParentPath().isEmpty());
            FixTask00085FixEmptyParentPathOnIdentifier fixTask = new FixTask00085FixEmptyParentPathOnIdentifier();
            fixTask.executeFix();
            identifier = identifierAPI.find(DUMMY_IDENTIFIER);
            Assert.assertEquals("/", identifier.getParentPath());
        } finally {
            identifierAPI.delete(identifierAPI.find(DUMMY_IDENTIFIER));
            recreateTrigger();
        }
    }

}
