package com.dotmarketing.common.db;


import java.sql.PreparedStatement;

/**
 *  Used to override default Statement object setting used in DotConnect
 */

public interface StatementObjectSetter {

    void execute(PreparedStatement statement, int parameterIndex, Object parameter);
}
