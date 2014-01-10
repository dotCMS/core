package com.dotcms.h2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.h2.tools.TriggerAdapter;

import com.dotmarketing.util.UtilMethods;

public class IdentifierHostInodeCheckTrigger extends TriggerAdapter {

    @Override
    public void fire(Connection conn, ResultSet oldIdent, ResultSet newIdent)  throws SQLException {        
        String newIdentHost=newIdent.getString("host_inode");
        if(!newIdent.getString("asset_type").equals("contentlet") && !UtilMethods.isSet(newIdentHost)) {
            throw new SQLException("Cannot insert/update a null or empty host inode for this kind of identifier");
        }

    }

}
