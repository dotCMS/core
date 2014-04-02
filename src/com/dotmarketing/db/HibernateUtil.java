package com.dotmarketing.db;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.sf.hibernate.CallbackException;
import net.sf.hibernate.FlushMode;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Interceptor;
import net.sf.hibernate.MappingException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.cfg.Configuration;
import net.sf.hibernate.cfg.Mappings;
import net.sf.hibernate.type.Type;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIImpl;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;

/**
 * 
 * @author will & david (2005)
 */
public class HibernateUtil {
	private static String dialect;
	private static SessionFactory sessionFactory;

	private static ThreadLocal sessionHolder = new ThreadLocal();

	private Class thisClass;

	private Query query;

	private int maxResults;

	private int firstResult;

	private int t;

	private static Mappings mappings;

	private static final boolean useCache = true;
	
	private static final ThreadLocal< Map<String,Runnable> > commitListeners=new ThreadLocal<Map<String,Runnable>>() {
	    protected java.util.Map<String,Runnable> initialValue() {
	        return new HashMap<String,Runnable>();
	    }
	};
	
	private static final ThreadLocal< List<Runnable> > rollbackListeners=new ThreadLocal<List<Runnable>>() {
        protected java.util.List<Runnable> initialValue() {
            return new ArrayList<Runnable>();
        }
    };

	public HibernateUtil(Class c) {
		setClass(c);
	}

	public HibernateUtil() {
	}

	public void setClass(Class c) {
		thisClass = c;
	}

	public static String getTableName(Class c) {

		return mappings.getClass(c).getTable().getName();
	}

	public static String getDialect() throws DotHibernateException{
		if (sessionFactory == null) {
			buildSessionFactory();
		}
		return dialect;	
	}
	public int getCount() throws DotHibernateException {
		try{
			getSession();
			int i = 0;
			if (maxResults > 0) {
				query.setMaxResults(maxResults);
			}
			if (firstResult > 0) {
				query.setFirstResult(firstResult);
			}
			i = ((Integer) query.list().iterator().next()).intValue();
			return i;
		}catch (Exception e) {
			throw new DotHibernateException("Unable to get count ", e);
		}
	}

	public void setFirstResult(int firstResult) {
		this.firstResult = firstResult;
	}

	public void setMaxResults(int g) {
		this.maxResults = g;
	}

	public void setParam(long g) {
		query.setLong(t, g);
		t++;
	}
	
	public void setParam(Long g) {
		query.setLong(t, g);
		t++;
	}

	public void setParam(String g) {
		query.setString(t, g);
		t++;
	}

	public void setParam(int g) {
		query.setInteger(t, g);
		t++;
	}

	public void setParam(Integer g) {
		query.setInteger(t, g);
		t++;
	}
	
	public void setParam(java.util.Date g) {
		query.setTimestamp(t, g);
		t++;
	}

	public void setParam(boolean g) {
		query.setBoolean(t, g);
		t++;
	}
	
	public void setParam(Boolean g) {
		query.setBoolean(t, g);
		t++;
	}

	public void setParam(double g) {
		query.setDouble(t, g);
		t++;
	}
	
	public void setParam(Double g) {
		query.setDouble(t, g);
		t++;
	}

	public void setParam(float g) {
		query.setFloat(t, g);
		t++;
	}
	
	public void setParam(Float g) {
		query.setFloat(t, g);
		t++;
	}

	public void setParam(Object g) {
		query.setEntity(t, g);
		t++;
	}

	public void setQuery(String x) throws DotHibernateException{
		try{
			Session session = getSession();
			query = session.createQuery(x);
			query.setCacheable(useCache);
		}catch(Exception ex){
			throw new DotHibernateException("Error setting Query",ex);
		}
	}

	public void setSQLQuery(String x) throws DotHibernateException{
		try{
		Session session = getSession();
			query = session.createSQLQuery(x, getTableName(thisClass), thisClass);
			query.setCacheable(useCache);
		}catch (Exception e) {
			throw new DotHibernateException("Error setting SQLQuery ", e);
		}
	}

	/*
	 * hibernate delete object
	 */
	public static void delete(Object obj) throws DotHibernateException {
		try{
			Session session = getSession();
			session.delete(obj);
			session.flush();
		}catch (Exception e) {
			throw new DotHibernateException("Error deleting object ", e);
		}
	}

	/*
	 * hibernate delete object
	 */
	public static void delete(String sql) throws DotHibernateException {
		try{	
			Session session = getSession();
			session.delete(sql);
		}catch (Exception e) {
			throw new DotHibernateException("Error deleteing SQL ", e);
		}
	}

	public static java.util.List find(String x)  throws DotHibernateException{
		try{
			Session session = getSession();
			return (ArrayList) session.find(x);
		}catch (Exception e) {
			throw new DotHibernateException("Error executing a find on Hibernate Session ", e);
		}
	}

    public static Object load(Class c, Serializable key)  throws DotHibernateException{
    	Session session = getSession();
    	try{        	    		
            return (Object) session.load(c, key);
		}catch (Exception e) {
			try
			{
				/*
				 * DOTCMS-1398
				 * when we try to find an object that doesn't exist the session become "dirty" cause:
				 * 
				 * "Like all Hibernate exceptions, this exception is considered unrecoverable."
				 *  http://hibernate.bluemars.net/hib_docs/v3/api/org/hibernate/ObjectNotFoundException.html
				 *  
				 *  and we have to close the session, cause it can't be used anymore.
				 */
				session.close();
			}
			catch(Exception ex)
			{
				Logger.debug(HibernateUtil.class,ex.toString());
			}
			throw new DotHibernateException("Error loading object from Hibernate Session ", e);
		}
    }

	/*
	 * hibernate RecipientList object
	 */
	public List list() throws DotHibernateException{
		try{
			getSession();
			if (maxResults > 0) {
				query.setMaxResults(maxResults);
			}
			if (firstResult > 0) {
				query.setFirstResult(firstResult);
			}
			long before = System.currentTimeMillis();
			java.util.List l = query.list();
			long after = System.currentTimeMillis();
			if(((after - before) / 1000) > 20) {
				String[] paramsA = query.getNamedParameters();
				String params = "";
				for(String s : paramsA)
					params = s + ", ";
				Logger.warn(this, "Too slow query sql: " + query.getQueryString() + " " + params);
			}
			return l;
		}catch (Exception e) {
			Logger.warn(this, "---------- DotHibernate: error on list ---------------", e);
			/*Ozzy i comment this because see DOTCMS-206. it have nonsence to make a rollback 
			 * when we are doing a search and the object is not found. this make some other operation
			 * to rollback when this is not required
			 **/
			//handleSessionException();
			// throw new DotRuntimeException(e.toString());
			return new java.util.ArrayList();
		}
	}

	public Object load(long id)throws DotHibernateException {
		Session session = getSession();

		if (id == 0) {
			try {
				return thisClass.newInstance();
			} catch (Exception e) {
				throw new DotRuntimeException(e.toString());
			}
		}

		try {
			return session.load(thisClass, new Long(id));
		} catch (Exception e) {
			Logger.debug(this, "---------- DotHibernate: error on load ---------------", e);
			/*Ozzy i comment this because see DOTCMS-206. it have nonsence to make a rollback 
			 * when we are doing a search and the object is not found. this make some other operation
			 * to rollback when this is not required
			 **/
			//handleSessionException();

			// if no object is found in db, return an new Object
			try {
				return thisClass.newInstance();
			} catch (Exception ex) {
				throw new DotRuntimeException(e.toString());
			}
		}
	}

	/**
	 * Will return null if object not found
	 * @param id
	 * @return
	 * @throws DotHibernateException
	 */
	public Object load(String id) throws DotHibernateException{
		Session session = getSession();

		if (id == null) {
			try {
				return thisClass.newInstance();
			} catch (Exception e) {
				throw new DotRuntimeException(e.toString());
			}
		}

		try {
			return session.load(thisClass, id);
		} catch (Exception e) {
			Logger.debug(this, "---------- DotHibernate: error on load ---------------", e);
			
			/*Ozzy i comment this because see DOTCMS-206. it have nonsence to make a rollback 
			 * when we are doing a search and the object is not found. this make some other operation
			 * to rollback when this is not required
			 **/
			//handleSessionException();

			// if no object is found in db, return an new Object
			try {
				return thisClass.newInstance();
			} catch (Exception ex) {
				throw new DotRuntimeException(e.toString());
			}
		}
	}

	public Object get(long id)throws DotHibernateException {
		try{
			Session session = getSession();
			if (id == 0) {
				return thisClass.newInstance();
			}
				return session.get(thisClass, new Long(id));
		}catch (Exception e) {
			throw new DotHibernateException("Unable to get Object with id " + id + " from Hibernate Session ", e);
		}
	}

	public Object get(String id) throws DotHibernateException{
		try{
			Session session = getSession();
			if (id == null)
				return thisClass.newInstance();
			return session.get(thisClass, id);
		}catch (Exception e) {
			throw new DotHibernateException("Unable to get Object with id " + id + " from Hibernate Session ", e);
		}
	}

	/**
	 * 
	 * @return The object loaded from the query or null if no object matches the query
	 * @throws DotHibernateException
	 */
	public Object load() throws DotHibernateException{
		getSession();
		ArrayList l = new java.util.ArrayList();
		Object obj = new Object();

		try {
			if (maxResults > 0) {
				query.setMaxResults(maxResults);
			}

			l = (java.util.ArrayList) query.list();
			obj = l.get(0);
			query = null;
		} catch (java.lang.IndexOutOfBoundsException iob) {
			// if no object is found in db, return an new Object
			try {
				obj = thisClass.newInstance();
			} catch (Exception ex) {
				Logger.error(this, query.getQueryString(), ex);
				throw new DotRuntimeException(ex.toString());
			}
		} catch (Exception e) {
			Logger.warn(this, "---------- DotHibernate: can't load- no results from query---------------", e);
			/*Ozzy i comment this because see DOTCMS-206. it have nonsence to make a rollback 
			 * when we are doing a search and the object is not found. this make some other operation
			 * to rollback when this is not required
			 **/
			//handleSessionException();

			try {
				obj = thisClass.newInstance();
			} catch (Exception ee) {
				Logger.error(this, "---------- DotHibernate: can't load- thisClass.newInstance()---------------", e);
				throw new DotRuntimeException(e.toString());
			}
		}

		return obj;
	}

	public String getQuery() throws DotHibernateException {
		try{
		StringBuffer sb = new StringBuffer(this.query.getQueryString() + "\n");
			for (int i = 0; i < this.query.getNamedParameters().length; i++) {
				sb.append("param " + i + " = " + query.getNamedParameters()[i]);
			}
			
		return sb.toString();
		}catch (Exception e) {
			throw new DotHibernateException("Unable to set Query ", e);
		}
	}

	public static void save(Object obj)  throws DotHibernateException{
		try{
			Session session = getSession();
			session.save(obj);
			session.flush();
		}catch (Exception e) {
			throw new DotHibernateException("Unable to save Object to Hibernate Session ", e);
		}
	}

	public static void saveOrUpdate(Object obj)  throws DotHibernateException{
		try{
		    forceDirtyObject.set(obj);
			Session session = getSession();
			session.saveOrUpdate(obj);
			session.flush();
		}catch (Exception e) {
			throw new DotHibernateException("Unable to save/update Object to Hibernate Session ", e);
		}
		finally {
		    forceDirtyObject.remove();
		}
	}

	public static void update(Object obj)  throws DotHibernateException{
		try{
			Session session = getSession();
			session.update(obj);
		}catch (Exception e) {
			throw new DotHibernateException("Unable to update Object to Hibernate Session ", e);
		}
	}

	// Session management methods

	protected static ThreadLocal forceDirtyObject=new ThreadLocal();
	
	protected static class NoDirtyFlushInterceptor implements Interceptor {
        
        protected final static int[] EMPTY=new int[0];
        
        public int[] findDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames,Type[] types) {
            if(forceDirtyObject.get() == entity)
                return null;
            else
                return EMPTY;
        }
        
        public Object instantiate(Class entityClass, Serializable id) throws CallbackException { return null; }
        public Boolean isUnsaved(Object arg0) { return null; }
        public void onDelete(Object arg0, Serializable arg1, Object[] arg2, String[] arg3, Type[] arg4) throws CallbackException { }
        public boolean onFlushDirty(Object arg0, Serializable arg1,Object[] arg2, Object[] arg3, String[] arg4, Type[] arg5) throws CallbackException { return false; }
        public boolean onLoad(Object arg0, Serializable arg1, Object[] arg2,String[] arg3, Type[] arg4) throws CallbackException { return false; }
        public boolean onSave(Object arg0, Serializable arg1, Object[] arg2, String[] arg3, Type[] arg4) throws CallbackException { return false; }
        public void postFlush(Iterator arg0) throws CallbackException { }
        public void preFlush(Iterator arg0) throws CallbackException { }
    }
	
	private static void buildSessionFactory() throws DotHibernateException{
		try {
			// Initialize the Hibernate environment
			/*
			################################
			##
			##	USE THE FILE hibernate.cfg.xml to point to mapping files
			##
			#################################
			*/
			Configuration cfg = new Configuration().configure();
			String _dbType = DbConnectionFactory.getDBType();
			if(_dbType == null){
				throw new Exception("DbConnectionFactory.getDBType() is null.  Cannot build Hibernate DB Connection without a dbType.");
			}
			if (DbConnectionFactory.MYSQL.equals(_dbType)) {
				//http://jira.dotmarketing.net/browse/DOTCMS-4937
				cfg.setNamingStrategy(new LowercaseNamingStrategy());
				cfg.addResource("com/dotmarketing/beans/DotCMSId.hbm.xml");
				cfg.addResource("com/dotmarketing/beans/DotCMSId_NOSQLGEN.hbm.xml");
				getPluginsHBM("Id",cfg);
				cfg.setProperty("hibernate.dialect", "net.sf.hibernate.dialect.MySQLDialect");
			} else if (DbConnectionFactory.POSTGRESQL.equals(_dbType)) {
				cfg.addResource("com/dotmarketing/beans/DotCMSSeq.hbm.xml");
				cfg.addResource("com/dotmarketing/beans/DotCMSSeq_NOSQLGEN.hbm.xml");
				getPluginsHBM("Seq",cfg);
				cfg.setProperty("hibernate.dialect", "net.sf.hibernate.dialect.PostgreSQLDialect");
			} else if (DbConnectionFactory.MSSQL.equals(_dbType)) {
				cfg.addResource("com/dotmarketing/beans/DotCMSId.hbm.xml");
				cfg.addResource("com/dotmarketing/beans/DotCMSId_NOSQLGEN.hbm.xml");
				getPluginsHBM("Id",cfg);
				cfg.setProperty("hibernate.dialect", "net.sf.hibernate.dialect.SQLServerDialect");
			} else if (DbConnectionFactory.ORACLE.equals(_dbType)) {
				cfg.addResource("com/dotmarketing/beans/DotCMSSeq.hbm.xml");
				cfg.addResource("com/dotmarketing/beans/DotCMSSeq_NOSQLGEN.hbm.xml");
				getPluginsHBM("Seq",cfg);
				cfg.setProperty("hibernate.dialect", "net.sf.hibernate.dialect.OracleDialect");
			}
			
			cfg.setInterceptor(new NoDirtyFlushInterceptor());
			
			mappings = cfg.createMappings();
			sessionFactory = cfg.buildSessionFactory();
			dialect = cfg.getProperty("hibernate.dialect");
			
		}catch (Exception e) {
			throw new DotHibernateException("Unable to build Session Factory ", e);
		}
	}
	
	
	
	private static void getPluginsHBM(String type,Configuration cfg) {
		Logger.debug(HibernateUtil.class, "Loading Hibernate Mappings from plugins ");
		PluginAPI pAPI=APILocator.getPluginAPI();
		
		File pluginDir=pAPI.getPluginJarDir();
		if (pluginDir==null) {
		return;
		}		
		File[] plugins=pluginDir.listFiles(new FilenameFilter(){

			public boolean accept(File dir, String name) {
				if (name.startsWith("plugin-") && name.endsWith(".jar")) {
					return true;
				}
				return false;
			}
			
		});
		for (File plugin:plugins) {
			try {
				JarFile jar = new JarFile(plugin);
				JarEntry entry=jar.getJarEntry("conf/DotCMS"+type+".hbm.xml");
				if (entry!=null) {
					InputStream in = new BufferedInputStream(jar.getInputStream(entry));
					StringBuffer out = new StringBuffer();
					byte[] b = new byte[4096];
					for (int n; (n = in.read(b)) != -1;) {
				        out.append(new String(b, 0, n));
				    }
					Logger.debug(HibernateUtil.class, "Loading Hibernate Mapping from: " + plugin.getName());
					cfg.addXML(out.toString());
				}
			} catch (IOException e) {
				Logger.debug(HibernateUtil.class,"IOException: " + e.getMessage(),e);
			} catch (MappingException e) {
				Logger.debug(HibernateUtil.class,"MappingException: " + e.getMessage(),e);
			}
		}
		Logger.debug(HibernateUtil.class, "Done loading Hibernate Mappings from plugins ");
	}
	

	/**
	 * Attempts to find a session associated with the Thread. If there isn't a
	 * session, it will create one.
	 */
	public static Session getSession() throws DotHibernateException{
		try{
			if (sessionFactory == null) {
				buildSessionFactory();
			}
			Session session = (Session) sessionHolder.get();
	
			if (session == null) {
					session = sessionFactory.openSession(DbConnectionFactory.getConnection());
			} else {
				try {
					if (session.connection().isClosed()) {
                        try {
                            session.close();
                        } catch (HibernateException e1) {
                            Logger.error(HibernateUtil.class,e1.getMessage(),e1);
                        }
                        session = null;
						session = sessionFactory.openSession(DbConnectionFactory.getConnection());
					}
    			} catch (Exception e) {
    	        	try {
    	        		session.close();
    				}
    				catch (Exception ex) {
    					Logger.error(HibernateUtil.class,e.getMessage() );
    		        	Logger.debug(HibernateUtil.class,e.getMessage(),e);
    				}
    				session = null;
    				try{
    					session = sessionFactory.openSession(DbConnectionFactory.getConnection());
    					
    				}
    				catch (Exception ex) {
    					Logger.error(HibernateUtil.class,ex.getMessage() );
    		        	Logger.debug(HibernateUtil.class,ex.getMessage(),ex);
    				}
            	}
			}
			sessionHolder.set(session);
			session.setFlushMode(FlushMode.NEVER);
			return session;
		}catch (Exception e) {
			throw new DotHibernateException("Unable to get Hibernate Session ", e);
		}
	}
	
	public static void addCommitListener(Runnable listener) throws DotHibernateException {
	    addCommitListener(UUIDGenerator.generateUuid(),listener);
	}
	
	public static void addCommitListener(String tag, Runnable listener) throws DotHibernateException { 
	    try {
    	    if(getSession().connection().getAutoCommit())
    	        listener.run();
    	    else {
    	        if(!commitListeners.get().containsKey(tag))
    	            commitListeners.get().put(tag,listener);
    	    }
	    }
	    catch(Exception ex) {
	        throw new DotHibernateException(ex.getMessage(),ex);
	    }
	}
	
	public static void addRollbackListener(Runnable listener) throws DotHibernateException{
        try {
            if(getSession().connection().getAutoCommit())
                listener.run();
            else
                rollbackListeners.get().add(listener);
        }
        catch(Exception ex) {
            throw new DotHibernateException(ex.getMessage(),ex);
        }
    }

	static class RunnablesExecutor extends Thread {
		private List<Runnable> runnables;
		
		public RunnablesExecutor(List<Runnable> runnables) {
			this.runnables = runnables;
		}
		public void run(){
			for(Runnable listeners : runnables){
			    listeners.run();
			}
		}
	}
	
	public static void closeSession()  throws DotHibernateException{
		try{
			// if there is nothing to close
			if (sessionHolder.get() == null)
				return;
			Session session = getSession();
	
			if (session != null) {
					session.flush();
					if (!session.connection().getAutoCommit()) {
						Logger.debug(HibernateUtil.class, "Closing session. Commiting changes!");
						session.connection().commit();
						session.connection().setAutoCommit(true);
						if(commitListeners.get().size()>0) {
    						List<Runnable> r = new ArrayList<Runnable>(commitListeners.get().values());
    						commitListeners.get().clear();
    						RunnablesExecutor t = new RunnablesExecutor(r);
    						t.run();
						}
					}
					DbConnectionFactory.closeConnection();
					session.close();
					session = null;
					sessionHolder.set(null);
			}
		}catch (Exception e) {
			throw new DotHibernateException("Unable to close Hibernate Session ", e);
		}
		finally {
		    commitListeners.get().clear();
		    rollbackListeners.get().clear();
		}
	}

	public static void startTransaction()  throws DotHibernateException{
		try{

		/*
		 * Transactions are now used by default
		 * 
		 */
			getSession().connection().setAutoCommit(false);
			rollbackListeners.get().clear();
			commitListeners.get().clear();
			Logger.debug(HibernateUtil.class, "Starting Transaction!");
		}catch (Exception e) {
			throw new DotHibernateException("Unable to set AutoCommit to false on Hibernate Session ", e);
		}
	}

	public static boolean commitTransaction()  throws DotHibernateException{
		closeSession();
		return true;
	}

	public static void rollbackTransaction() throws DotHibernateException {

		sessionCleanupAndRollback();

	}
	
	public static boolean startLocalTransactionIfNeeded() throws DotDataException{
    	boolean startTransaction = false;

    	try {
    		startTransaction = DbConnectionFactory.getConnection().getAutoCommit();
			if(startTransaction){
				HibernateUtil.startTransaction();
			}
		} catch (SQLException e) {
			Logger.error(WorkflowAPIImpl.class,e.getMessage(),e);
			throw new DotDataException(e.getMessage());
		}
		return startTransaction;
    }

	public static void flush()  throws DotHibernateException{
		try{
			Session session = getSession();
			session.flush();
		}catch (Exception e) {
			throw new DotHibernateException("Unable to flush Hibernate Session ", e);
		}
	}

	public static void sessionCleanupAndRollback()  throws DotHibernateException{
	    commitListeners.get().clear();
		Logger.debug(HibernateUtil.class, "sessionCleanupAndRollback");
		Session session = getSession();
		session.clear();

		try {
			session.connection().rollback();
			session.connection().setAutoCommit(true);
		} catch (Exception ex) {
			Logger.debug(HibernateUtil.class, "---------- DotHibernate: error on rollbackTransaction ---------------",
					ex);
			Logger.error(HibernateUtil.class, "---------- DotHibernate: error on rollbackTransaction ---------------\n"+ ex);
			// throw new DotRuntimeException(ex.toString());
		}

		try {
			DbConnectionFactory.closeConnection();
			session.close();
			session = null;
			sessionHolder.set(null);
		} catch (Exception ex) {
			Logger.debug(HibernateUtil.class, "---------- DotHibernate: error on rollbackTransaction ---------------",
					ex);
			Logger.error(HibernateUtil.class, "---------- DotHibernate: error on rollbackTransaction ---------------\n"+ ex);
			// throw new DotRuntimeException(ex.toString());
		}
		
		if(rollbackListeners.get().size()>0) {
            List<Runnable> r = new ArrayList<Runnable>(rollbackListeners.get());
            rollbackListeners.get().clear();
            RunnablesExecutor t = new RunnablesExecutor(r);
            t.run();
        }
	}
	
    public static Savepoint setSavepoint() throws DotHibernateException {
    	Connection conn;
		try {
			conn = getSession().connection();
			if(!conn.getAutoCommit())
				return conn.setSavepoint();
			return null;
		} catch (HibernateException e) {
			throw new DotHibernateException(e.getMessage(), e);
		} catch (SQLException e) {
			throw new DotHibernateException(e.getMessage(), e);
		}
    }

	public static void rollbackSavepoint(Savepoint savepoint) throws DotHibernateException {

		try {
			getSession().connection().rollback(savepoint);
		} catch (HibernateException e) {
			throw new DotHibernateException(e.getMessage(), e);
		} catch (SQLException e) {
			throw new DotHibernateException(e.getMessage(), e);
		}

	}
    
	public static void saveWithPrimaryKey(Object obj, Serializable id)  throws DotHibernateException{
		try{
			Session session = getSession();
			session.save(obj, id);
		}catch (Exception e) {
			throw new DotHibernateException("Unable to save Object with primary key " + id + " to Hibernate Session ", e);
		}try{
			Session session = getSession();
			session.flush();
		}catch (Exception e) {
			throw new DotHibernateException("Unable to flush Hibernate Session ", e);
		}
	}
	
    public void setDate(java.util.Date g) {
        query.setDate(t, g);
        t++;
    }
	
    public static void evict(Object obj) throws DotHibernateException{
        Session session = getSession();
        try {
            session.evict(obj);
        } catch (HibernateException e) {
        	throw new DotHibernateException("Unable to evict from Hibernate Session ", e);
        }
    }

}
