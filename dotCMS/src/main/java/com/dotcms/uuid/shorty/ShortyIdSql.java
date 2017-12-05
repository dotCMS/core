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

  protected static final String SELECT_SHORTY_SQL_EQUALS =
      "select inode as id, 'inode' as type, type as subtype from inode where inode = ? union select id,'identifier', asset_type from identifier where id = ?";

}
