package com.dotmarketing.fixtask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.TaskLocatorUtil;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotcms.repackage.edu.emory.mathcs.backport.java.util.Collections;

public class FixTasksExecutor  implements StatefulJob {

	private final String FixtasksPackage = "com.dotmarketing.fixtask.tasks";
	
	private static FixTasksExecutor executor;



	private FixTasksExecutor() {

	}

	public static FixTasksExecutor getInstance() {
		if (executor == null)
			executor = new FixTasksExecutor();
		return executor;
	}

	/**
	 * Check which database we're using, and select the apropiate SQL. In a
	 * different method to avoid further clutter
	 */
	List <Map>  returnValue =  new ArrayList <Map>();
	
	public void execute(JobExecutionContext arg0) {
		try {
    		returnValue= new ArrayList <Map>();
    		List<Class<?>> runOnce;
    
    		Comparator<Class<?>> comparator = new Comparator<Class<?>>() {
    			public int compare(Class<?> o1, Class<?> o2) {
    			    String name = o1.getName();
    			    String name2 = o2.getName();
    			    if(!name.matches("(\\D+)(\\d{5})(.*)")) {
    			        return 1;
    			    }
    			    
    				return o1.getName().compareTo(o2.getName());
    			}
    		};
    		try {
    			runOnce = TaskLocatorUtil.getFixTaskClasses();
    
    		} catch (Exception e) {
    			throw new DotRuntimeException(e.getMessage(), e);
    		}
    		Collections.sort(runOnce, comparator);
    
    		//PreparedStatement update = null;
    		int taskId = 0;
    		try {
    			for (Class<?> c : runOnce) {
    				String name = c.getCanonicalName();
    				name = name.substring(name.lastIndexOf(".") + 1);
    				String id = name.substring(7, 12);

    				    try{
    				        taskId = Integer.parseInt(id);
    				    } catch (NumberFormatException e) {
    				        taskId++;
    				    }
    				    
    					if (FixTask.class.isAssignableFrom(c)) {
    						FixTask task;
    						try {
    							task = (FixTask) c.newInstance();
    						} catch (Exception e) {
    							throw new DotRuntimeException(e.getMessage(), e);
    						}
    						HibernateUtil.startTransaction();
                            Boolean shouldrun=task.shouldRun();
    						if (shouldrun) {
    							Logger.info(this, "Running: " + name);
    							returnValue.addAll(task.executeFix());
    						}
    			
    					String executed="";
    					if (shouldrun)
    						executed="was Executed";
    					else executed="was not Executed";
    						
    						Logger.info(this,
    								"fix assets and inconsistencies task: "
    										+ name+" "+executed);
    					}
    				

    			}
    		} catch (Exception e) {
    			Logger
    					.fatal(
    							this,
    							"Unable to execute the fix assets and inconsistencies tasks",
    							e);
    			try {
    				throw new DotDataException(
    						"Unable to execute fix assets inconsistencies task : ",
    						e);
    			} catch (DotDataException e1) {
    				// TODO Auto-generated catch block
    			}
    		}
		}
		finally {
		    try {
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.warn(this, e.getMessage(), e);
            }
		    finally {
		        DbConnectionFactory.closeConnection();
		    }
		}
		Logger.info(this, "Finishing tasks.");

	}

	public List getTasksresults (){
		
		return returnValue;
	} 


}
