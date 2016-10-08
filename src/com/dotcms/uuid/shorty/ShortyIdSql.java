package com.dotcms.uuid.shorty;

import com.dotmarketing.db.DbConnectionFactory;

public class ShortyIdSql {

    static protected ShortyIdSql getInstance() {
        if (DbConnectionFactory.isMsSql()) {
            return new ShortyIdMSSql();
        } 
        else{
            return new ShortyIdSql();
        }
    }


    static String SELECT_SHORTY_SQL="select inode as id, 'inode' as type, type as subtype from inode where inode like ? union select id,'identifier', asset_type from identifier where id like ?";
    
    

}
