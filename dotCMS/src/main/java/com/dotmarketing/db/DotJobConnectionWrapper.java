package com.dotmarketing.db;

import java.sql.Connection;

/**
 * Wrapper to extend functionalities of a {@Link Connection}
 * @author nollymar
 */
public class DotJobConnectionWrapper extends DotConnectionWrapper {

    private final boolean newConnection;

    public DotJobConnectionWrapper(final boolean newConnection, Connection conn) {
        super(conn);
        this.newConnection = newConnection;
    }

    public boolean isNewConnection() {
        return newConnection;
    }
}
