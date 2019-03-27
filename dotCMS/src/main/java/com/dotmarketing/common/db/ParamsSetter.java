package com.dotmarketing.common.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * When you need to override the parameters set for the batch
 * @author jsanca
 */
public interface ParamsSetter {

    /**
     * Sets the custom for the parameters set.
     * @param preparedStatement PreparedStatement
     * @param params  Params
     * @throws SQLException
     */
    void setParams(final PreparedStatement preparedStatement,
                   final Params params) throws SQLException;
}
