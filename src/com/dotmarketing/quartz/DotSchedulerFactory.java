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
	
	private StdSchedulerFactory sequentialSchedulerFactory;
	private StdSchedulerFactory standardSchedulerFactory;
	private Scheduler sequentialScheduler;
	private Scheduler standardScheduler;
	
	private DotSchedulerFactory () throws SchedulerException {
		
			if(sequentialSchedulerFactory == null) {
				Properties sequentialProperties = new Properties();
				try {
					ClassLoader cl = Thread.currentThread().getContextClassLoader();
					sequentialProperties.load(cl.getResourceAsStream("quartz_sequential.properties"));
				} catch (IOException e) {
					Logger.error(DotSchedulerFactory.class, e.getMessage(), e);
					throw new SchedulerException(e.getMessage(), e);
				}
				sequentialSchedulerFactory = new StdSchedulerFactory(sequentialProperties);
				sequentialScheduler = sequentialSchedulerFactory.getScheduler();
			}

			if(standardSchedulerFactory == null) {
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
		list.add(sequentialScheduler);
		return list;
	}

	public Scheduler getScheduler() throws SchedulerException {
		return standardScheduler;
	}

	public Scheduler getScheduler(String arg0) throws SchedulerException {
		if(arg0.equals("sequential"))
			return sequentialScheduler;
		else if(arg0.equals("standard"))
			return standardScheduler;
		throw new SchedulerException("Invalid scheduler " + arg0 + " requested");
	}
	
	public Scheduler getSequentialScheduler() throws SchedulerException {
		return sequentialScheduler;
	}

	public static DotSchedulerFactory getInstance() throws SchedulerException {
		synchronized (DotSchedulerFactory.class) {
			if(factory == null) {
				factory = new DotSchedulerFactory();
			}
		}
		return factory; 
	}
	

}
