package com.dotcms.util;

import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.TaskLocatorUtil;
import java.util.Comparator;
import java.util.List;

/**
 * @author Jonathan Gamba 2019-03-27
 */
public class StartupTasksUtil {

    private static StartupTasksUtil instance;

    public static StartupTasksUtil getInstance() {
        if (instance == null) {
            synchronized (StartupTasksUtil.class) {
                if (instance == null) {
                    instance = new StartupTasksUtil();
                }
            }
        }
        return instance;
    }

    public void init() throws Exception {

        /*
        Reading the always run startup tasks in order to create the db schema if the
        database is empty and to load the starter.zip
         */
        Comparator<Class<?>> comparator = Comparator.comparing(Class::getName);
        List<Class<?>> runAlways = TaskLocatorUtil.getStartupRunAlwaysTaskClasses();
        runAlways.sort(comparator);

        String name;

        try {
            Logger.info(this, "Starting startup tasks.");
            HibernateUtil.startTransaction();

            for (Class<?> c : runAlways) {

                name = c.getCanonicalName();
                name = name.substring(name.lastIndexOf(".") + 1);
                if (StartupTask.class.isAssignableFrom(c)) {

                    StartupTask task = (StartupTask) c.newInstance();

                    HibernateUtil.startTransaction();
                    if (task.forceRun()) {
                        HibernateUtil.closeAndCommitTransaction();
                        HibernateUtil.startTransaction();
                        Logger.info(this, "Running: " + name);
                        task.executeUpgrade();
                    } else {
                        Logger.info(this, "Not running: " + name);
                    }
                    HibernateUtil.closeAndCommitTransaction();

                }
            }
            Logger.info(this, "Finishing startup tasks.");
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            throw e;
        } finally {
            // This will commit the changes and close the connection
            HibernateUtil.closeSession();
        }

    }

}