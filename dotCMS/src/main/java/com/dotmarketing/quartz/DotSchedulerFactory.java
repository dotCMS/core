package com.dotmarketing.quartz;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import com.dotmarketing.util.Logger;

public class DotSchedulerFactory implements SchedulerFactory {

    private static DotSchedulerFactory factory;


    private StdSchedulerFactory standardSchedulerFactory;

    private Scheduler standardScheduler;


    private DotSchedulerFactory() throws SchedulerException {


        if (standardSchedulerFactory == null) {
            Properties standardProperties = new Properties();
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                standardProperties.load(cl.getResourceAsStream("quartz.properties"));
            } catch (IOException e) {
                Logger.error(DotSchedulerFactory.class, e.getMessage(), e);
                throw new SchedulerException(e.getMessage(), e);
            }
            standardSchedulerFactory = new StdSchedulerFactory(standardProperties);
            standardScheduler = standardSchedulerFactory.getScheduler();
        }


    }

    public Collection<Scheduler> getAllSchedulers() throws SchedulerException {
        List<Scheduler> list = new ArrayList<Scheduler>();
        list.add(standardScheduler);

        return list;
    }

    public Scheduler getScheduler() throws SchedulerException {
        return standardScheduler;
    }

    public Scheduler getScheduler(String arg0) throws SchedulerException {

        return standardScheduler;

    }


    public static DotSchedulerFactory getInstance() throws SchedulerException {
        synchronized (DotSchedulerFactory.class) {
            if (factory == null) {
                factory = new DotSchedulerFactory();
            }
        }
        return factory;
    }


}
