package com.dotmarketing.startup;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;

import java.util.List;

/**
 * API to interact with the start up tasks.
 *
 * @author jsanca
 */
public interface StartupAPI {

    /**
     * Returns the start up (run always) tasks
     * @return List of start up classes
     */
    List<Class<?>> getStartupRunAlwaysTaskClasses();

    /**
     * Returns the start up (run once) tasks
     * @return List of start up classes
     */
    List<Class<?>> getStartupRunOnceTaskClasses();

    /**
     * Returns all start up tasks (run always and once)
     * @return List of start up classes
     */
    List<Class<?>> getAllStartupTaskClasses();

    /**
     * Runs a start up class.
     * @param startupClass {@link Class}
     * @throws DotDataException
     * @throws DotRuntimeException
     */
    void runStartup (Class<?> startupClass) throws DotDataException, DotRuntimeException;

} // E:O:F:StartupAPI.
