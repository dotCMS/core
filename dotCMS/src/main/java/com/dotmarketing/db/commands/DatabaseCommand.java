package com.dotmarketing.db.commands;

import com.dotmarketing.exception.DotDataException;

/**
 * Database Command to Execute a database query
 * @author Andre Curione
 */
public interface DatabaseCommand {

    /**
     * Execute the query
     * @throws DotDataException
     */
    void execute () throws DotDataException;

}
