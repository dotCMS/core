package com.dotmarketing.db.commands;

import com.dotcms.system.SimpleMapAppContext;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;

/**
 * Database Command Interface to Execute database queries
 * @author Andre Curione
 */
public interface DatabaseCommand {

    enum QueryReplacements {
        TABLE,
        ID_COLUMN,
        ID_VALUE,
        CONDITIONAL_COLUMN,
        CONDITIONAL_VALUE,
        EXTRA_COLUMNS,
        DO_NOTHING
    }

    /**
     * Generates the Native SQL Query to be executed
     * @return
     */
    String generateSQLQuery (SimpleMapAppContext replacements);

    /**
     * Execute the query
     * @throws DotDataException
     */
    void execute (DotConnect dotConnect, SimpleMapAppContext queryReplacements, Object... parameters) throws DotDataException;
}
