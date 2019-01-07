package com.dotmarketing.startup;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.TaskLocatorUtil;
import com.google.common.collect.ImmutableList;

import java.util.Comparator;
import java.util.List;

/**
 * Default implementation.
 * @author jsanca
 */
public class StartupAPIImpl implements StartupAPI {
    @Override
    public List<Class<?>> getStartupRunAlwaysTaskClasses() {

        return TaskLocatorUtil.getStartupRunAlwaysTaskClasses().stream()
                .sorted(Comparator.comparing(Class::getName)).collect(CollectionsUtils.toImmutableList());
    }

    @Override
    public List<Class<?>> getStartupRunOnceTaskClasses() {
        return TaskLocatorUtil.getStartupRunOnceTaskClasses().stream()
                .sorted(Comparator.comparing(Class::getName)).collect(CollectionsUtils.toImmutableList());
    }

    @Override
    public List<Class<?>> getAllStartupTaskClasses() {

        final ImmutableList.Builder<Class<?>> allStartupTasksClasses =
                new ImmutableList.Builder<>();

        allStartupTasksClasses.addAll(this.getStartupRunOnceTaskClasses());
        allStartupTasksClasses.addAll(this.getStartupRunAlwaysTaskClasses());

        return allStartupTasksClasses.build();
    }

    @WrapInTransaction
    @Override
    public void runStartup(final Class<?> startupClass) throws DotDataException, DotRuntimeException {

        final StartupTask startupTask = (StartupTask) ReflectionUtils.newInstance(startupClass);
        if (null != startupTask) {

            Logger.debug(this, ()-> "Running the start up class: " + startupClass.getCanonicalName());
            startupTask.executeUpgrade();
            Logger.debug(this, ()-> "Ran the start up class: "     + startupClass.getCanonicalName());
        } else {

            throw new DoesNotExistException("The start up class: "       + startupClass.getCanonicalName() +
                    ", does not exists");
        }
    }
} // E:O:F:StartupAPIImpl.
