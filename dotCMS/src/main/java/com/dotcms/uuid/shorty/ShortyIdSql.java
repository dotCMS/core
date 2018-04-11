package com.dotcms.uuid.shorty;

import com.dotmarketing.db.DbConnectionFactory;

public class ShortyIdSql {

  static protected ShortyIdSql getInstance() {
    if (DbConnectionFactory.isMsSql()) {
      return new ShortyIdMSSql();
    } else {
      return new ShortyIdSql();
    }
  }

  protected static final String SELECT_SHORTY_SQL_LIKE =
      "select inode as id, 'inode' as type, type as subtype from inode where inode like ? union select id,'identifier', asset_type from identifier where id like ?";

  protected static final String SELECT_WF_SCHEME_SHORTY_SQL_LIKE =
          "select id, 'workflow_scheme' as type, 'workflow_scheme' as subtype from workflow_scheme where id like ?";

  protected static final String SELECT_WF_STEP_SHORTY_SQL_LIKE =
          "select id, 'workflow_step' as type, 'workflow_step' as subtype from workflow_step where id like ?";

  protected static final String SELECT_WF_ACTION_SHORTY_SQL_LIKE =
          "select id, 'workflow_action' as type, 'workflow_action' as subtype from workflow_action where id like ?";

  // EQUALS

  protected static final String SELECT_SHORTY_SQL_EQUALS =
      "select inode as id, 'inode' as type, type as subtype from inode where inode = ? union select id,'identifier', asset_type from identifier where id = ?";

  protected static final String SELECT_WF_SCHEME_SHORTY_SQL_EQUALS =
          "select id, 'workflow_scheme' as type, 'workflow_scheme' as subtype from workflow_scheme where id = ?";

  protected static final String SELECT_WF_STEP_SHORTY_SQL_EQUALS =
          "select id, 'workflow_step' as type, 'workflow_step' as subtype from workflow_step where id = ?";

  protected static final String SELECT_WF_ACTION_SHORTY_SQL_EQUALS =
          "select id, 'workflow_action' as type, 'workflow_action' as subtype from workflow_action where id = ?";
}
