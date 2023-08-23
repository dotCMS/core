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
      "(SELECT inode AS id, 'inode' AS type, type AS subtype FROM inode WHERE inode LIKE ? UNION ALL SELECT id,'identifier', asset_type FROM identifier WHERE id LIKE ?)";

  protected static final String SELECT_WF_SCHEME_SHORTY_SQL_LIKE =
          "SELECT id, 'workflow_scheme' AS type, 'workflow_scheme' AS subtype FROM workflow_scheme WHERE id LIKE ?";

  protected static final String SELECT_WF_STEP_SHORTY_SQL_LIKE =
          "SELECT id, 'workflow_step' AS type, 'workflow_step' AS subtype FROM workflow_step WHERE id LIKE ?";

  protected static final String SELECT_WF_ACTION_SHORTY_SQL_LIKE =
          "SELECT id, 'workflow_action' AS type, 'workflow_action' AS subtype FROM workflow_action WHERE id LIKE ?";

  // EQUALS

  protected static final String SELECT_SHORTY_SQL_EQUALS =
      "(SELECT inode AS id, 'inode' AS type, type AS subtype FROM inode WHERE inode = ? UNION ALL SELECT id,'identifier', asset_type FROM identifier WHERE id = ?) limit 1";

  protected static final String SELECT_WF_SCHEME_SHORTY_SQL_EQUALS =
          "SELECT id, 'workflow_scheme' AS type, 'workflow_scheme' AS subtype FROM workflow_scheme WHERE id = ?";

  protected static final String SELECT_WF_STEP_SHORTY_SQL_EQUALS =
          "SELECT id, 'workflow_step' AS type, 'workflow_step' AS subtype FROM workflow_step WHERE id = ?";

  protected static final String SELECT_WF_ACTION_SHORTY_SQL_EQUALS =
          "SELECT id, 'workflow_action' AS type, 'workflow_action' AS subtype FROM workflow_action WHERE id = ?";




}
