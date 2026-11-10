package com.dotmarketing.startup.runonce;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;

/**
 * Moves the admin menu onto the promoted Angular Users, Roles and Tools portlets: rows that still
 * point at the {@code users-beta}, {@code roles-beta} or {@code tools-beta} ids are converted to
 * {@code users}, {@code roles} and {@code tools}, and the Tools portlet is inserted right after
 * Roles in every layout that has Roles but no Tools.
 * <p>
 * Not implemented yet: this is the empty shell the integration test compiles against.
 */
public class Task261110PromoteUsersRolesToolsPortlets implements StartupTask {

    /**
     * Tells whether any layout still needs the conversion or the Tools insert.
     *
     * @return {@code false} until the task is implemented
     */
    @Override
    public boolean forceRun() {
        return false;
    }

    /**
     * Converts the beta rows and inserts the Tools portlet after Roles. Does nothing yet.
     *
     * @throws DotDataException never, until the task is implemented
     */
    @Override
    public void executeUpgrade() throws DotDataException {
        // intentionally empty until the implementation checkpoint
    }
}
