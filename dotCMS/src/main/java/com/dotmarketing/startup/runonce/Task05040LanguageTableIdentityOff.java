package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

import java.sql.SQLException;
import java.util.Map;

public class Task05040LanguageTableIdentityOff implements StartupTask  {


    //MySQL Upgrade
    private static final String MY_SQL_FOREIGN_CHECKS_OFF = "SET foreign_key_checks = 0;";
    private static final String MY_SQL_FOREIGN_CHECKS_ON = "SET foreign_key_checks = 1;";
    private static final String MY_SQL_ALTER_TABLE_LANGUAGE_DROP_AUTO_INCREMENT =
             " ALTER TABLE language MODIFY id BIGINT, DROP PRIMARY KEY, ADD PRIMARY KEY (id); ";

    //MsSQL Upgrade
    private static final String MS_SQL_GET_IDENTITY_COLUMN = " SELECT $IDENTITY FROM language; ";
    private static final String MS_SQL_ADD_TEMP_NON_IDENTITY_ID_COLUMN = " ALTER TABLE language ADD non_idenity_id numeric(19,0); ";
    private static final String MS_SQL_BACKUP_ID_COLUMN = "UPDATE language SET non_idenity_id = id; ";

    private static final String LANGUAGE_CONTENTLET_CONSTRAINT_NAME = "fk_contentlet_lang";
    private static final String LANGUAGE_WORKFLOW_TASK_CONSTRAINT_NAME = "fk_workflow_task_language";
    private static final String VERSION_INFO_CONSTRAINT_NAME = "fk_contentlet_version_info_lang";

    private static final String MS_SQL_GET_PK_CONSTRAIN_NAME = "SELECT name FROM sys.key_constraints WHERE [type] = 'PK' AND [parent_object_id] = Object_id('language');";
    private static final String MS_SQL_DROP_IDENTITY_ID_COLUMN = "ALTER TABLE language DROP COLUMN id;";
    private static final String MS_SQL_RENAME_TEMP_COLUMN_TO_ID_COLUMN = "EXEC sp_rename 'language.non_idenity_id', 'id', 'COLUMN'; ";

    private static final String MS_SQL_VERIFY_CONSTRAINT_EXITS = " SELECT * FROM sys.objects WHERE name = '%s' ";

    private static final String MS_SQL_DROP_LANGUAGE_PK_CONSTRAINT = "ALTER TABLE language DROP CONSTRAINT %s; ";
    private static final String MS_SQL_DROP_CONTENTLET_CONSTRAINT = "ALTER TABLE contentlet DROP CONSTRAINT fk_contentlet_lang; ";
    private static final String MS_SQL_DROP_WORKFLOW_CONSTRAINT = " ALTER TABLE workflow_task DROP CONSTRAINT fk_workflow_task_language; ";
    private static final String MS_SQL_DROP_VERSION_INFO_CONSTRAINT = " ALTER TABLE contentlet_version_info DROP CONSTRAINT fk_contentlet_version_info_lang;";

    private static final String MS_SQL_ADD_ID_NOT_NULL_CONSTRAINT = "ALTER TABLE language ALTER COLUMN id numeric(19,0) NOT NULL";
    private static final String MS_SQL_ADD_PK_CONSTRAINT = " ALTER TABLE language ADD CONSTRAINT pk_language PRIMARY KEY (id); ";
    private static final String MS_SQL_ADD_CONTENTLET_CONSTRAINT = " ALTER TABLE contentlet ADD CONSTRAINT fk_contentlet_lang FOREIGN KEY (language_id) REFERENCES language(id); ";
    private static final String MS_SQL_ADD_WORKFLOW_CONSTRAINT = " ALTER TABLE workflow_task ADD CONSTRAINT FK_workflow_task_language FOREIGN KEY (language_id) REFERENCES language(id); ";
    private static final String MS_SQL_ADD_VERSION_INFO_CONSTRAINT = " ALTER TABLE contentlet_version_info ADD CONSTRAINT fk_contentlet_version_info_lang FOREIGN KEY (lang) REFERENCES language(id); ";

    //POSTGRES Upgrade
    private static final String POSTGRES_DROP_SEQUENCE_IF_EXISTS = " DROP SEQUENCE IF EXISTS language_seq CASCADE; ";
    private static final String POSTGRES_ALTER_TABLE_LANG = " ALTER TABLE language ALTER COLUMN id DROP default; ";

    //Oracle Upgrade
    private static final String ORACLE_CHECK_SEQUENCE_EXIST = "SELECT sequence_name FROM all_sequences WHERE lower(sequence_name) = 'language_seq'";
    private static final String ORACLE_DROP_SEQUENCE = " DROP SEQUENCE language_seq ";

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    private boolean checkIdentityExistsMSSQL(final DotConnect dotConnect) throws DotRuntimeException {
       try {
          dotConnect.setSQL(MS_SQL_GET_IDENTITY_COLUMN);
          dotConnect.loadResult();
          return true;
       }catch(Exception e){
          Logger.error(getClass(),"failed getting identity column from table ",e);
          return false;
       }
    }

    private boolean checkConstraintExistsMSSQL(final DotConnect dotConnect, final String constraintName)
            throws DotRuntimeException, DotDataException {
            final String sql = String.format(MS_SQL_VERIFY_CONSTRAINT_EXITS, constraintName);
            dotConnect.setSQL(sql);
            return !dotConnect.loadResults().isEmpty();
    }

    private String getLanguageTablePKConstraintNameMSSQL(final DotConnect dotConnect) throws DotDataException, DotRuntimeException {
        dotConnect.setSQL(MS_SQL_GET_PK_CONSTRAIN_NAME);
        final Map<String, Object> meta = dotConnect.loadObjectResults().get(0);
        return (String)meta.get("name");
    }

    private boolean checkSequenceExistsOracle(final DotConnect dotConnect) throws DotDataException, DotRuntimeException {
        dotConnect.setSQL(ORACLE_CHECK_SEQUENCE_EXIST);
        return !dotConnect.loadObjectResults().isEmpty();
    }

    @Override
    @CloseDBIfOpened
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        Logger.debug(this, "Drop Auto-increment/Identity from `Language` Table definition.");
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        final DotConnect dotConnect = new DotConnect();

        if (DbConnectionFactory.isMsSql()) {
            if (!checkIdentityExistsMSSQL(dotConnect)) {
                return;
            }

            dotConnect.setSQL(MS_SQL_ADD_TEMP_NON_IDENTITY_ID_COLUMN);
            dotConnect.loadResult();

            dotConnect.setSQL(MS_SQL_BACKUP_ID_COLUMN);
            dotConnect.loadResult();

            if(checkConstraintExistsMSSQL(dotConnect,LANGUAGE_CONTENTLET_CONSTRAINT_NAME)){
                dotConnect.setSQL(MS_SQL_DROP_CONTENTLET_CONSTRAINT);
                dotConnect.loadResult();
            }
            if(checkConstraintExistsMSSQL(dotConnect,LANGUAGE_WORKFLOW_TASK_CONSTRAINT_NAME)){
                dotConnect.setSQL(MS_SQL_DROP_WORKFLOW_CONSTRAINT);
                dotConnect.loadResult();
            }
            if(checkConstraintExistsMSSQL(dotConnect,VERSION_INFO_CONSTRAINT_NAME)){
                dotConnect.setSQL(MS_SQL_DROP_VERSION_INFO_CONSTRAINT);
                dotConnect.loadResult();
            }

            final String constraint = getLanguageTablePKConstraintNameMSSQL(dotConnect);

            dotConnect.setSQL( String.format(MS_SQL_DROP_LANGUAGE_PK_CONSTRAINT, constraint) );
            dotConnect.loadResult();

            dotConnect.setSQL(MS_SQL_DROP_IDENTITY_ID_COLUMN);
            dotConnect.loadResult();

            dotConnect.setSQL(MS_SQL_RENAME_TEMP_COLUMN_TO_ID_COLUMN);
            dotConnect.loadResult();

            dotConnect.setSQL(MS_SQL_ADD_ID_NOT_NULL_CONSTRAINT);
            dotConnect.loadResult();

            dotConnect.setSQL(MS_SQL_ADD_PK_CONSTRAINT);
            dotConnect.loadResult();

            dotConnect.setSQL(MS_SQL_ADD_CONTENTLET_CONSTRAINT);
            dotConnect.loadResult();

            dotConnect.setSQL(MS_SQL_ADD_WORKFLOW_CONSTRAINT);
            dotConnect.loadResult();

            dotConnect.setSQL(MS_SQL_ADD_VERSION_INFO_CONSTRAINT);
            dotConnect.loadResult();


        } else if (DbConnectionFactory.isMySql()) {
            dotConnect.setSQL(MY_SQL_FOREIGN_CHECKS_OFF);
            dotConnect.loadResult();
            dotConnect.setSQL(MY_SQL_ALTER_TABLE_LANGUAGE_DROP_AUTO_INCREMENT);
            dotConnect.loadResult();
            dotConnect.setSQL(MY_SQL_FOREIGN_CHECKS_ON);
            dotConnect.loadResult();
        } else if (DbConnectionFactory.isPostgres()) {
            dotConnect.setSQL(POSTGRES_DROP_SEQUENCE_IF_EXISTS);
            dotConnect.loadResult();
            dotConnect.setSQL(POSTGRES_ALTER_TABLE_LANG);
            dotConnect.loadResult();
        } else if (DbConnectionFactory.isOracle()) {
            if (!checkSequenceExistsOracle(dotConnect)) {
                return;
            }
            dotConnect.setSQL(ORACLE_DROP_SEQUENCE);
            dotConnect.loadResult();
        }

    }

}
