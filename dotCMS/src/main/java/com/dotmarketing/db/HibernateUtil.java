package com.dotmarketing.db;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.repackage.net.sf.hibernate.*;
import com.dotcms.repackage.net.sf.hibernate.cfg.Configuration;
import com.dotcms.repackage.net.sf.hibernate.cfg.Mappings;
import com.dotcms.repackage.net.sf.hibernate.dialect.Dialect;
import com.dotcms.repackage.net.sf.hibernate.impl.SessionFactoryImpl;
import com.dotcms.repackage.net.sf.hibernate.type.Type;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.util.*;
import com.google.common.annotations.VisibleForTesting;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * This class provides a great number of utility methods that allow developers to interact with the
 * underlying data source in dotCMS. Without regards to using the legacy Hibernate queries or the
 * new {@link com.dotmarketing.common.db.DotConnect} class, developers can manage database
 * transactions, connections, commit listeners, and many other configuration settings.
 *
 * @author will & david (2005)
 */
public class HibernateUtil {

    private static final String LISTENER_SUBMITTER = "dotListenerSubmitter";
    private static final String NETWORK_CACHE_FLUSH_DELAY = "NETWORK_CACHE_FLUSH_DELAY";

    private static Dialect dialect;
    private static SessionFactory sessionFactory;

    private static ThreadLocal<Session> sessionHolder = new ThreadLocal<>();

    private Class thisClass;

    private Query query;

    private int maxResults;

    private int firstResult;

    private int t;

    private static Mappings mappings;

    private static final boolean useCache = true;

    public HibernateUtil(SessionFactory sessionFac) {
        this.sessionFactory = sessionFac;
    }

    public enum TransactionListenerStatus {
        ENABLED, DISABLED;
    }

    public static final String addToIndex = "-add-to-index";
    public static final String removeFromIndex = "-remove-from-index";

    /**
     * Status for listeners of thread-local -based transactions. This allows to control whether
     * listeners are appended or not (ENABLED by default)
     */
    private static final ThreadLocal<TransactionListenerStatus> listenersStatus = new ThreadLocal<TransactionListenerStatus>() {
        protected TransactionListenerStatus initialValue() {
            return TransactionListenerStatus.ENABLED;
        }
    };

    private static final ThreadLocal<Boolean> asyncCommitListenersFinalization = ThreadLocal.withInitial(
            () -> true);

    @VisibleForTesting
    static final ThreadLocal<Map<String, Runnable>> asyncCommitListeners = ThreadLocal
            .withInitial(LinkedHashMap::new);

    @VisibleForTesting
    static final ThreadLocal<Map<String, Runnable>> syncCommitListeners = ThreadLocal
            .withInitial(LinkedHashMap::new);

    static final ThreadLocal<Map<String, Runnable>> rollbackListeners = ThreadLocal
            .withInitial(LinkedHashMap::new);

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

    public static Dialect getDialect() {
        if (sessionFactory == null) {
            buildSessionFactory();
        }
        return dialect;
    }

    public int getCount() throws DotHibernateException {
        try {
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
        } catch (Exception e) {
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

    public void setQuery(String x) throws DotHibernateException {
        try {
            Session session = getSession();
            query = session.createQuery(x);
            query.setCacheable(useCache);
        } catch (Exception ex) {
            throw new DotHibernateException("Error setting Query", ex);
        }
    }

    public void setSQLQuery(String x) throws DotHibernateException {
        try {
            Session session = getSession();
            query = session.createSQLQuery(x, getTableName(thisClass), thisClass);
            query.setCacheable(useCache);
        } catch (Exception e) {
            throw new DotHibernateException("Error setting SQLQuery ", e);
        }
    }

    /*
     * hibernate delete object
     */
    public static void delete(Object obj) throws DotHibernateException {
        try {
            Session session = getSession();
            session.delete(obj);
            session.flush();
        } catch (Exception e) {
            throw new DotHibernateException("Error deleting object " + e.getMessage(), e);
        }
    }

    /**
     * Do a query, settings the parameters (if exists) and return a unique result
     *
     * @param clazz
     * @param query
     * @param parameters
     * @param <T>
     * @return
     * @throws DotHibernateException
     * @throws HibernateException
     */
    public static <T> T load(final Class<T> clazz, final String query,
            final Object... parameters) throws DotHibernateException {

        T result = null;
        int index = 0;
        Query hibernateQuery = null;

        try {

            hibernateQuery = getSession().createQuery(query).setCacheable(useCache);

            for (Object parameter : parameters) {
                hibernateQuery.setParameter(index++, parameter);
            }

            result = (T) hibernateQuery.list().get(0);
            hibernateQuery = null;
        } catch (java.lang.IndexOutOfBoundsException iob) {
            // if no object is found in db, return an new Object
            try {
                result = clazz.newInstance();
            } catch (Exception ex) {
                Logger.error(HibernateUtil.class, hibernateQuery.getQueryString(), ex);
                throw new DotRuntimeException(ex.toString());
            }
        } catch (ObjectNotFoundException e) {
            Logger.warn(HibernateUtil.class,
                    "---------- DotHibernate: can't load- no results from query---------------", e);
            try {
                result = clazz.newInstance();
            } catch (Exception ee) {
                Logger.error(HibernateUtil.class,
                        "---------- DotHibernate: can't load- thisClass.newInstance()---------------",
                        e);
                throw new DotRuntimeException(e.toString());
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }

        return result;
    }

    /*
     * hibernate delete object
     */
    public static void delete(String sql) throws DotHibernateException {
        try {
            Session session = getSession();
            session.delete(sql);
        } catch (Exception e) {
            throw new DotHibernateException("Error deleteing SQL " + e.getMessage(), e);
        }
    }

    public static java.util.List find(String x) throws DotHibernateException {
        try {
            Session session = getSession();
            return session.find(x);
        } catch (Exception e) {
            throw new DotHibernateException(
                    "Error executing a find on Hibernate Session " + e.getMessage(), e);
        }
    }

    public static Object load(Class c, Serializable key) throws DotHibernateException {
        Session session = getSession();

        try {
            return (Object) session.load(c, key);
        } catch (Exception e) {
            try {
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
            } catch (Exception ex) {
                Logger.debug(HibernateUtil.class, ex.toString());
            }
            throw new DotHibernateException("Error loading object from Hibernate Session ", e);
        }
    }

    /*
     * hibernate RecipientList object
     */
    public List list() throws DotHibernateException {
        try {
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
            if (((after - before) / 1000) > 20) {
                String[] paramsA = query.getNamedParameters();
                String params = "";
				for (String s : paramsA) {
					params = s + ", ";
				}
                Logger.warn(this, "Too slow query sql: " + query.getQueryString() + " " + params);
            }
            return l;
        } catch (ObjectNotFoundException e) {
            Logger.warn(this, "---------- DotHibernate: error on list ---------------", e);
            /*Ozzy i comment this because see DOTCMS-206. it have nonsence to make a rollback
             * when we are doing a search and the object is not found. this make some other operation
             * to rollback when this is not required
             **/
            //handleSessionException();
            // throw new DotRuntimeException(e.toString());
            return new java.util.ArrayList();
        } catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    public Object load(long id) throws DotHibernateException {
        Session session = getSession();

        if (id == 0) {
            try {
                return thisClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new DotRuntimeException(e.toString());
            }
        }

        try {
            return session.load(thisClass, Long.valueOf(id));
        } catch (ObjectNotFoundException e) {
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
        } catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Will return null if object not found
     *
     * @param id
     * @return
     * @throws DotHibernateException
     */
    public Object load(String id) throws DotHibernateException {
        Session session = getSession();

        if (id == null) {
            try {
                return thisClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new DotRuntimeException(e.toString());
            }
        }

        try {
            return session.load(thisClass, id);
        } catch (ObjectNotFoundException e) {
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
        } catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    public Object get(long id) throws DotHibernateException {
        try {
            Session session = getSession();
            if (id == 0) {
                return thisClass.getDeclaredConstructor().newInstance();
            }
            return session.get(thisClass, id);
        } catch (Exception e) {
            throw new DotHibernateException(
                    "Unable to get Object with id " + id + " from Hibernate Session ", e);
        }
    }

    public Object get(String id) throws DotHibernateException {
        try {
            Session session = getSession();
			if (id == null) {
				return thisClass.getDeclaredConstructor().newInstance();
			}
            return session.get(thisClass, id);
        } catch (Exception e) {
            throw new DotHibernateException(
                    "Unable to get Object with id " + id + " from Hibernate Session ", e);
        }
    }

    /**
     * @return The object loaded from the query or null if no object matches the query
     * @throws DotHibernateException
     */
    public Object load() throws DotHibernateException {
        getSession();
        Object obj;

        try {
            if (maxResults > 0) {
                query.setMaxResults(maxResults);
            }

            List l = (java.util.List) query.list();
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
        } catch (ObjectNotFoundException e) {
            Logger.warn(this,
                    "---------- DotHibernate: can't load- no results from query---------------", e);
            /*Ozzy i comment this because see DOTCMS-206. it have nonsence to make a rollback
             * when we are doing a search and the object is not found. this make some other operation
             * to rollback when this is not required
             **/
            //handleSessionException();

            try {
                obj = thisClass.newInstance();
            } catch (Exception ee) {
                Logger.error(this,
                        "---------- DotHibernate: can't load- thisClass.newInstance()---------------",
                        e);
                throw new DotRuntimeException(e.toString());
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }

        return obj;
    }

    public String getQuery() throws DotHibernateException {
        try {
            final StringBuilder sb = new StringBuilder(this.query.getQueryString() + "\n");
            for (int i = 0; i < this.query.getNamedParameters().length; i++) {
                sb.append("param " + i + " = " + query.getNamedParameters()[i]);
            }

            return sb.toString();
        } catch (Exception e) {
            throw new DotHibernateException("Unable to set Query ", e);
        }
    }

    public static void save(Object obj) throws DotHibernateException {
        try {
            forceDirtyObject.set(obj);
            Session session = getSession();
            session.save(obj);
            session.flush();
        } catch (Exception e) {
            throw new DotHibernateException("Unable to save Object to Hibernate Session ", e);
        } finally {
            forceDirtyObject.remove();
        }
    }

    public static void saveOrUpdate(Object obj) throws DotHibernateException {
        try {
            forceDirtyObject.set(obj);
            Session session = getSession();
            session.saveOrUpdate(obj);
            session.flush();
        } catch (Exception e) {
            throw new DotHibernateException("Unable to save/update Object to Hibernate Session ",
                    e);
        } finally {
            forceDirtyObject.remove();
        }
    }

    /**
     * Merge is pretty similar to saveOrUpdate method, but specially util for add/update detached
     * objects (objects that are not longer in the current session or objects that were loaded from
     * JDBC)
     *
     * @param obj Object
     * @throws DotHibernateException
     */
    public static void merge(final Object obj) throws DotHibernateException {
        try {
            forceDirtyObject.set(obj);
            Session session = getSession();
            session.saveOrUpdateCopy(obj);
            session.flush();
        } catch (Exception e) {
            throw new DotHibernateException("Unable to merge Object to Hibernate Session ", e);
        } finally {
            forceDirtyObject.remove();
        }
    }

    public static void update(Object obj) throws DotHibernateException {
        try {
            forceDirtyObject.set(obj);
            Session session = getSession();
            session.update(obj);
            session.flush();
        } catch (Exception e) {
            throw new DotHibernateException("Unable to update Object to Hibernate Session ", e);
        } finally {
            forceDirtyObject.remove();
        }
    }

    // Session management methods

    private static ThreadLocal<Object> forceDirtyObject = new ThreadLocal<>();

    protected static class NoDirtyFlushInterceptor implements Interceptor {

        protected final static int[] EMPTY = new int[0];

        public int[] findDirty(Object entity, Serializable id, Object[] currentState,
                Object[] previousState, String[] propertyNames, Type[] types) {
			if (forceDirtyObject.get() == entity) {
				return null;
			} else {
				return EMPTY;
			}
        }

        public Object instantiate(Class entityClass, Serializable id) throws CallbackException {
            return null;
        }

        public Boolean isUnsaved(Object arg0) {
            return null;
        }

        public void onDelete(Object arg0, Serializable arg1, Object[] arg2, String[] arg3,
                Type[] arg4) throws CallbackException {
        }

        public boolean onFlushDirty(Object arg0, Serializable arg1, Object[] arg2, Object[] arg3,
                String[] arg4, Type[] arg5) throws CallbackException {
            return false;
        }

        public boolean onLoad(Object arg0, Serializable arg1, Object[] arg2, String[] arg3,
                Type[] arg4) throws CallbackException {
            return false;
        }

        public boolean onSave(Object arg0, Serializable arg1, Object[] arg2, String[] arg3,
                Type[] arg4) throws CallbackException {
            return false;
        }

        public void postFlush(Iterator arg0) throws CallbackException {
        }

        public void preFlush(Iterator arg0) throws CallbackException {
        }
    }

    private static synchronized void buildSessionFactory() {
        long start = System.currentTimeMillis();
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
            cfg.setProperty("hibernate.cache.provider_class",
                    "com.dotmarketing.db.NoCacheProvider");
            cfg.setProperty("hibernate.jdbc.use_scrollable_resultset", "true");
            cfg.addResource("META-INF/portal-hbm.xml");
            final String[] additionalConfigs = Config.getStringArrayProperty(
                    "additional.hibernate.configs", new String[]{});
            for (String config : additionalConfigs) {
                cfg.addResource(config);
            }

            if (DbConnectionFactory.isMySql()) {
                //http://jira.dotmarketing.net/browse/DOTCMS-4937
                cfg.setNamingStrategy(new LowercaseNamingStrategy());
                cfg.addResource("com/dotmarketing/beans/DotCMSId.hbm.xml");
                cfg.addResource("com/dotmarketing/beans/DotCMSId_NOSQLGEN.hbm.xml");
                getPluginsHBM("Id", cfg);
                cfg.setProperty("hibernate.dialect",
                        "com.dotcms.repackage.net.sf.hibernate.dialect.MySQLDialect");
            } else if (DbConnectionFactory.isPostgres()) {
                cfg.addResource("com/dotmarketing/beans/DotCMSSeq.hbm.xml");
                cfg.addResource("com/dotmarketing/beans/DotCMSSeq_NOSQLGEN.hbm.xml");
                getPluginsHBM("Seq", cfg);
                cfg.setProperty("hibernate.dialect",
                        "com.dotcms.repackage.net.sf.hibernate.dialect.PostgreSQLDialect");
            } else if (DbConnectionFactory.isMsSql()) {
                cfg.addResource("com/dotmarketing/beans/DotCMSId.hbm.xml");
                cfg.addResource("com/dotmarketing/beans/DotCMSId_NOSQLGEN.hbm.xml");
                getPluginsHBM("Id", cfg);
                cfg.setProperty("hibernate.dialect",
                        "com.dotcms.repackage.net.sf.hibernate.dialect.SQLServerDialect");
            } else if (DbConnectionFactory.isOracle()) {
                cfg.addResource("com/dotmarketing/beans/DotCMSSeq.hbm.xml");
                cfg.addResource("com/dotmarketing/beans/DotCMSSeq_NOSQLGEN.hbm.xml");
                getPluginsHBM("Seq", cfg);
                cfg.setProperty("hibernate.dialect",
                        "com.dotcms.repackage.net.sf.hibernate.dialect.OracleDialect");
            }

            cfg.setInterceptor(new NoDirtyFlushInterceptor());

            mappings = cfg.createMappings();

            sessionFactory = cfg.buildSessionFactory();
            dialect = ((SessionFactoryImpl) sessionFactory).getDialect();
            System.setProperty(WebKeys.DOTCMS_STARTUP_TIME_DB,
                    String.valueOf(System.currentTimeMillis() - start));

        } catch (Exception e) {
            throw new DotStateException("Unable to build Session Factory ", e);
        }
    }


    private static void getPluginsHBM(String type, Configuration cfg) {
        Logger.debug(HibernateUtil.class, "Loading Hibernate Mappings from plugins ");
        PluginAPI pAPI = APILocator.getPluginAPI();

        File pluginDir = pAPI.getPluginJarDir();
        if (pluginDir == null) {
            return;
        }
        File[] plugins = pluginDir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                if (name.startsWith("plugin-") && name.endsWith(".jar")) {
                    return true;
                }
                return false;
            }

        });
        for (File plugin : plugins) {
            try {
                JarFile jar = new JarFile(plugin);
                JarEntry entry = jar.getJarEntry("conf/DotCMS" + type + ".hbm.xml");
                if (entry != null) {
                    InputStream in = new BufferedInputStream(jar.getInputStream(entry));
                    StringBuffer out = new StringBuffer();
                    byte[] b = new byte[4096];
                    for (int n; (n = in.read(b)) != -1; ) {
                        out.append(new String(b, 0, n));
                    }
                    Logger.debug(HibernateUtil.class,
                            "Loading Hibernate Mapping from: " + plugin.getName());
                    cfg.addXML(out.toString());
                }
            } catch (IOException e) {
                Logger.debug(HibernateUtil.class, "IOException: " + e.getMessage(), e);
            } catch (MappingException e) {
                Logger.debug(HibernateUtil.class, "MappingException: " + e.getMessage(), e);
            }
        }
        Logger.debug(HibernateUtil.class, "Done loading Hibernate Mappings from plugins ");
    }


    public static Optional<Session> getSessionIfOpened() {
        if (sessionFactory == null) {
            buildSessionFactory();
        }
        return Optional.ofNullable(sessionHolder.get());
    }

    /**
     * Creates a new Hibernate Session based on the conn on the parameter Also
     *
     * @param newTransactionConnection {@link Connection}
     * @return Session
     */
    public static Session createNewSession(final Connection newTransactionConnection) {

        try {

            // just to create the initial if are not set
            getSessionIfOpened();
            final Session session = sessionFactory.openSession(newTransactionConnection);
            if (null != session) {
                session.setFlushMode(FlushMode.NEVER);
            }
            return session;
        } catch (Exception e) {
            throw new DotStateException("Unable to get Hibernate Session ", e);
        }
    }

    /**
     * Set a session on the parameter as the new session to use on all next hibernate calls
     *
     * @param newSession
     */
    public static void setSession(final Session newSession) {

        try {
            if (null != newSession && null != newSession.connection()
                    && !newSession.connection().isClosed()) {
                sessionHolder.set(newSession);
            }
        } catch (Exception e) {
            Logger.error(HibernateUtil.class, "---------- HibernateUtil: error : " + e);
            Logger.debug(HibernateUtil.class, "---------- HibernateUtil: error ", e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Attempts to find a session associated with the Thread. If there isn't a session, it will
     * create one.
     */
    public static Session getSession() {
        try {
            final Optional<Session> sessionOptional = getSessionIfOpened();
            Session session = sessionOptional.isPresent() ? sessionOptional.get() : null;

            if (session == null) {
                session = sessionFactory.openSession(DbConnectionFactory.getConnection());
            } else {
                try {
                    if (session.connection().isClosed()) {
                        try {
                            session.close();
                        } catch (HibernateException e1) {
                            Logger.error(HibernateUtil.class, e1.getMessage(), e1);
                        }
                        session = null;
                        session = sessionFactory.openSession(DbConnectionFactory.getConnection());
                    }
                } catch (Exception e) {
                    try {
                        if (null != session) {
                            session.close();
                        }
                    } catch (Exception ex) {
                        Logger.error(HibernateUtil.class, e.getMessage());
                        Logger.debug(HibernateUtil.class, e.getMessage(), e);
                    }
                    session = null;
                    try {
                        session = sessionFactory.openSession(DbConnectionFactory.getConnection());
                    } catch (Exception ex) {
                        Logger.error(HibernateUtil.class, ex.getMessage());
                        Logger.debug(HibernateUtil.class, ex.getMessage(), ex);
                    }
                }
            }
            sessionHolder.set(session);
            if (null != session) {
                session.setFlushMode(FlushMode.NEVER);
            }
            return session;
        } catch (Exception e) {
            throw new DotStateException("Unable to get Hibernate Session ", e);
        }
    }

    /**
     * Returns the listeners status currently associated to the thread-local -based transaction
     * (ENABLED by default)
     */
    public static TransactionListenerStatus getTransactionListenersStatus() {
        return listenersStatus.get();
    }

    /**
     * Allows to override the status of the listeners associated to the current thread-local -based
     * transaction (DISABLED if overriden) When using TransactionListenerStatus.DISABLED, client
     * code should be aware of controlling the operations that are suppossed to be done by
     * listeners
     *
     * @param status TransactionListenerStatus
     */
    public static void setTransactionListenersStatus(TransactionListenerStatus status) {
        listenersStatus.set(status);
    }

    public static boolean getAsyncCommitListenersFinalization() {
        return asyncCommitListenersFinalization.get();
    }

    public static void setAsyncCommitListenersFinalization(boolean finalizeAsync) {
        asyncCommitListenersFinalization.set(finalizeAsync);
    }

    /**
     * Allows you to add an asynchronous commit listener to the current database
     * session/transaction. This means that the current flow of the application will continue its
     * way and the specified commit listener will be spawned subsequently after the transaction has
     * been committed or the session has been closed. <p>By default, asynchronous commit listeners
     * will be spawned by a new thread. This means that they will not share the same database
     * connection information with the main thread of the dotCMS application.</p>
     *
     * @param runnable The commit listener wrapped as a {@link Runnable} object.
     * @param order    in case you want to add an order
     * @throws DotHibernateException An error occurred when registering the commit listener.
     */
    public static void addCommitListener(final Runnable runnable, final int order) throws
            DotHibernateException {
        addCommitListener(new DotOrderedRunnable(runnable, order));
    }

    /**
     * Adds a commit listener to the current database transaction/session. There are several types
     * of commit listeners, namely:
     * <ul>
     * <li>{@link DotSyncRunnable}</li>
     * <li>{@link DotOrderedRunnable}</li>
     * <li>{@link FlushCacheRunnable}</li>
     * <li>{@link ReindexRunnable}</li>
     * <li>Among others.</li>
     * </ul>
     * Commit listeners allow developers to execute code after a transaction has been committed
     * or the session has ended.
     *
     * @param listener The commit listener wrapped as a {@link Runnable} object.
     * @throws DotHibernateException An error occurred when registering the commit listener.
     */
    public static void addCommitListener(final Runnable listener) throws DotHibernateException {
        addCommitListener(UUIDGenerator.generateUuid(), listener);
    }

    /**
     * Adds a commit listener to the current database transaction/session. There are several types
     * of commit listeners, namely:
     * <ul>
     * <li>{@link DotSyncRunnable}</li>
     * <li>{@link DotOrderedRunnable}</li>
     * <li>{@link FlushCacheRunnable}</li>
     * <li>{@link ReindexRunnable}</li>
     * <li>Among others.</li>
     * </ul>
     * Commit listeners allow developers to execute code after a transaction has been committed
     * or the session has ended.
     *
     * @param listener The commit listener wrapped as a {@link Runnable} object.
     */
    public static void addCommitListenerNoThrow(final Runnable listener) {
        addCommitListener(UUIDGenerator.generateUuid(), listener);
    }

    /**
     * Allows you to add an asynchronous commit listener to the current database
     * session/transaction. This means that the current flow of the application will take care of
     * running the specified commit listener. This is particularly useful when you need to keep
     * database objects that were created during the database transaction.
     * <p>For example, if you created a temporary table that your commit listener needs to access,
     * you will definitely need to create a synchronous listener for it to be able to access the
     * such a table as temporary tables are only available for the duration of the transaction or
     * the session. With synchronous commit listeners, dotCMS will use the its main execution thread
     * to call the listener; therefore, it can access the same database connection and see the
     * temporary table.</p>
     *
     * @param runnable The commit listener wrapped as a {@link Runnable} object.
     * @throws DotHibernateException An error occurred when registering the commit listener.
     */
    public static void addSyncCommitListener(final Runnable runnable) throws
            DotHibernateException {
        addCommitListener(new DotSyncRunnable(runnable));
    }

    /**
     * In case you need to execute the Runnable in an specific order, you can use this class.
     */
    public static class DotOrderedRunnable implements Runnable {

        private final Runnable runnable;
        private final int order;

        public DotOrderedRunnable(final Runnable runnable) {
            this(runnable, 0);
        }

        public DotOrderedRunnable(final Runnable runnable, final int order) {
            this.runnable = runnable;
            this.order = order;
        }

        @Override
        public void run() {
            runnable.run();
        }

        public Runnable getRunnable() {
            return runnable;
        }

        public int getOrder() {
            return order;
        }
    }

    /**
     * Allows you to create a synchronous commit listener, which represents code that will be called
     * after a database transaction has been committed or the session has been closed. Synchronous
     * listeners will NOT RUN ON A NEW THREAD, they will use the main dotCMS thread.
     */
    public static class DotSyncRunnable implements Runnable {

        private final Runnable runnable;
        private final int order;

        public DotSyncRunnable(final Runnable runnable) {
            this(runnable, 0);
        }

        public DotSyncRunnable(final Runnable runnable, final int order) {

            this.runnable = runnable;
            this.order = order;
        }

        @Override
        public void run() {
            runnable.run();
        }

        public Runnable getRunnable() {
            return runnable;
        }

        public int getOrder() {
            return order;
        }
    }


    /**
     * Adds a commit listener to the current database transaction/session. There are several types
     * of commit listeners, namely:
     * <ul>
     * <li>{@link DotSyncRunnable}</li>
     * <li>{@link DotOrderedRunnable}</li>
     * <li>{@link FlushCacheRunnable}</li>
     * <li>{@link ReindexRunnable}</li>
     * <li>Among others.</li>
     * </ul>
     * Commit listeners allow developers to execute code after a transaction has been committed or the
     * session has ended. For listeners that are not instances of {@link DotSyncRunnable} or
     * {@link DotOrderedRunnable} a configuration property called {@code
     * REINDEX_ON_SAVE_IN_SEPARATE_THREAD} determines whether listeners are executed in a separate
     * thread, or in the same dotCMS thread. By default, they run in a brand new thread.
     *
     * @param tag      A unique ID for the specified listener.
     * @param listener The commit listener wrapped as a {@link Runnable} object.
     * @throws DotHibernateException An error occurred when registering the commit listener.
     */
    public static void addCommitListener(final String tag, final Runnable listener) {
        if (DbConnectionFactory.inTransaction()
                && getTransactionListenersStatus() != TransactionListenerStatus.DISABLED) {
            if (listener instanceof DotSyncRunnable) {
                syncCommitListeners.get().put(tag, listener);
            } else if (listener instanceof ReindexRunnable && asyncReindexCommitListeners()) {
                asyncCommitListeners.get().put(tag, listener);
            } else if (listener instanceof ReindexRunnable) {
                syncCommitListeners.get().put(tag, listener);
            } else if (getAsyncCommitListenersFinalization() && asyncCommitListeners()) {
                asyncCommitListeners.get().put(tag, listener);
            } else {
                syncCommitListeners.get().put(tag, listener);
            }
        } else {
            listener.run();
        }
    }

    public static void addRollbackListener(Runnable listener) {
        addRollbackListener(UUIDGenerator.generateUuid(), listener);
    }


    public static void addRollbackListener(final String key, Runnable listener) {
        if (getTransactionListenersStatus() != TransactionListenerStatus.DISABLED
                && DbConnectionFactory.inTransaction()) {
            rollbackListeners.get().put(key, listener);
        }
    }

    public static void closeSessionSilently() {

        try {
            closeSession();
        } catch (DotHibernateException e) {

        }
    }

    public static void closeSession() throws DotHibernateException {
        try {
            // if there is nothing to close
            if (null == sessionHolder.get()) {
                return;
            }
            Optional<Session> sessionOptional = getSessionIfOpened();

            if (sessionOptional.isPresent()) {
                Session session = sessionOptional.get();
                if (session.isOpen()) {
                    session.flush();
                    Connection connection = session.connection();
                    if (connection != null && !connection.isClosed()) {
                        if (!connection.getAutoCommit()) {
                            connection.commit();
                            connection.setAutoCommit(true);
                        }
                        if (!syncCommitListeners.get().isEmpty() || !asyncCommitListeners.get()
                                .isEmpty()) {
                            finalizeCommitListeners();
                        }
                    }
                }

                DbConnectionFactory.closeConnection();
                if (session.isOpen()) {
                    session.close();
                }
                sessionHolder.set(session);
            }
        } catch (Exception e) {
            Logger.error(HibernateUtil.class, e.getMessage(), e);
            throw new DotHibernateException("Unable to close Hibernate Session ", e);
        } finally {
            syncCommitListeners.get().clear();
            asyncCommitListeners.get().clear();
            rollbackListeners.get().clear();
        }
    }

    /**
     * Traverses the lists of both synchronous and asynchronous commit listeners and starts them up.
     * The asynchronous listeners are executed as separate threads, whereas synchronous listeners
     * use the main dotCMS thread to run.
     */
    private static void finalizeCommitListeners() {
        final List<Runnable> asyncListeners = new ArrayList<>(asyncCommitListeners.get().values());
        final List<Runnable> syncListeners = new ArrayList<>(syncCommitListeners.get().values());
        asyncCommitListeners.get().clear();
        syncCommitListeners.get().clear();

        if (!asyncListeners.isEmpty()) {

            final List<Runnable> listeners = getListeners(asyncListeners);
            final List<Runnable> flushers = getFlushers(asyncListeners);
            final DotSubmitter submitter = DotConcurrentFactory.getInstance()
                    .getSubmitter(LISTENER_SUBMITTER);

            if (!listeners.isEmpty()) {
                submitter.submit(new DotRunnableThread(listeners, true));
            }

            if (!flushers.isEmpty()) {
                submitter.submit(new DotRunnableFlusherThread(flushers, true));
                submitter.delay(new DotRunnableFlusherThread(flushers, true),
                        Config.getLongProperty(NETWORK_CACHE_FLUSH_DELAY, 3000),
                        TimeUnit.MILLISECONDS);
            }
        }

        if (!syncListeners.isEmpty()) {

            final List<Runnable> listeners = getListeners(syncListeners);
            final List<Runnable> flushers = getFlushers(syncListeners);
            if (!listeners.isEmpty()) {
                new DotRunnableThread(listeners).run();
            }

            if (!flushers.isEmpty()) {
                new DotRunnableFlusherThread(flushers).run();
                DateUtil.sleep(Config.getLongProperty(NETWORK_CACHE_FLUSH_DELAY, 3000));
                new DotRunnableFlusherThread(flushers,
                        false).run(); // todo: double check this if we still want a thread for the flushers
            }
        }
    }

    private static List<Runnable> getListeners(final List<Runnable> allListeners) {
        return allListeners.stream().filter(HibernateUtil::isNotFlushCacheRunnable)
                .sorted(HibernateUtil::compare).collect(Collectors.toList());
    }

    private static int compare(final Runnable runnable, final Runnable runnable1) {
        return getOrder(runnable).compareTo(getOrder(runnable1));
    }

    private static Integer getOrder(final Runnable runnable) {

        final int order = (runnable instanceof HibernateUtil.DotSyncRunnable) ?
                HibernateUtil.DotSyncRunnable.class.cast(runnable).getOrder() : 0;

        return (runnable instanceof HibernateUtil.DotOrderedRunnable) ?
                HibernateUtil.DotOrderedRunnable.class.cast(runnable).getOrder() : order;
    }

    private static boolean isNotFlushCacheRunnable(final Runnable listener) {

        return !isFlushCacheRunnable(listener);
    }

    private static List<Runnable> getFlushers(final List<Runnable> allListeners) {
        return allListeners.stream().filter(HibernateUtil::isFlushCacheRunnable)
                .collect(Collectors.toList());
    }

    private static boolean isFlushCacheRunnable(final Runnable listener) {

        return (
                listener instanceof FlushCacheRunnable ||
                        (listener instanceof HibernateUtil.DotOrderedRunnable
                                && HibernateUtil.DotOrderedRunnable.class.cast(listener)
                                .getRunnable() instanceof FlushCacheRunnable) ||
                        (listener instanceof HibernateUtil.DotSyncRunnable
                                && HibernateUtil.DotSyncRunnable.class.cast(listener)
                                .getRunnable() instanceof FlushCacheRunnable)
        );
    }

    public static void startTransaction() throws DotHibernateException {
        try {
            /*
             * Transactions are now used by default
             *
             */
            getSession().connection().setAutoCommit(false);
            rollbackListeners.get().clear();
            syncCommitListeners.get().clear();
            asyncCommitListeners.get().clear();
            Logger.debug(HibernateUtil.class, "Starting Transaction!");
        } catch (Exception e) {
            throw new DotHibernateException(
                    "Unable to set AutoCommit to false on Hibernate Session ", e);
        }
    }

    /**
     * This just commit the transaction and process the listeners.
     *
     * @throws DotHibernateException
     */
    public static void commitTransaction() throws DotHibernateException {

        try {
            // if there is nothing to close
            if (null == sessionHolder.get()) {
                return;
            }
            Session session = getSession();

            if (null != session) {
                session.flush();
                if (!session.connection().getAutoCommit()) {
                    Logger.debug(HibernateUtil.class, "Closing session. Commiting changes!");
                    session.connection().commit();
                    session.connection().setAutoCommit(true);
                    if (!asyncCommitListeners.get().isEmpty() || !syncCommitListeners.get()
                            .isEmpty()) {
                        finalizeCommitListeners();
                    }
                }
            }
        } catch (Exception e) {
            Logger.error(HibernateUtil.class, e.getMessage(), e);
            throw new DotHibernateException("Unable to close Hibernate Session ", e);
        } finally {
            syncCommitListeners.get().clear();
            asyncCommitListeners.get().clear();
            rollbackListeners.get().clear();
        }
    }

    /**
     * This commit and close the current session, connection
     *
     * @return
     * @throws DotHibernateException
     */
    public static boolean closeAndCommitTransaction() throws DotHibernateException {
        closeSession();
        return true;
    }

    public static void rollbackTransaction() throws DotHibernateException {

        sessionCleanupAndRollback();

    }

    public static boolean startLocalTransactionIfNeeded() throws DotDataException {
        boolean startTransaction = false;

        try {
            startTransaction = DbConnectionFactory.getConnection().getAutoCommit();
            if (startTransaction) {
                HibernateUtil.startTransaction();
            }
        } catch (SQLException e) {
            Logger.error(HibernateUtil.class, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        }
        return startTransaction;
    }

    public static void flush() throws DotHibernateException {
        try {
            Session session = getSession();
            session.flush();
        } catch (Exception e) {
            throw new DotHibernateException("Unable to flush Hibernate Session ", e);
        }
    }

    public static void sessionCleanupAndRollback() throws DotHibernateException {
        syncCommitListeners.get().clear();
        asyncCommitListeners.get().clear();
        Logger.debug(HibernateUtil.class, "sessionCleanupAndRollback");
        Session session = getSession();
        session.clear();

        try {
            session.connection().rollback();
            session.connection().setAutoCommit(true);
        } catch (Exception ex) {
            Logger.debug(HibernateUtil.class,
                    "---------- DotHibernate: error on rollbackTransaction ---------------",
                    ex);
            Logger.error(HibernateUtil.class,
                    "---------- DotHibernate: error on rollbackTransaction ---------------\n" + ex);
            // throw new DotRuntimeException(ex.toString());
        }

        try {
            DbConnectionFactory.closeConnection();
            session.close();
            session = null;
            sessionHolder.set(null);
        } catch (Exception ex) {
            Logger.debug(HibernateUtil.class,
                    "---------- DotHibernate: error on rollbackTransaction ---------------",
                    ex);
            Logger.error(HibernateUtil.class,
                    "---------- DotHibernate: error on rollbackTransaction ---------------\n" + ex);
            // throw new DotRuntimeException(ex.toString());
        }

        if (rollbackListeners.get().size() > 0) {
            List<Runnable> r = new ArrayList<>(rollbackListeners.get().values());
            rollbackListeners.get().clear();
            for (Runnable runnable : r) {
                runnable.run();
            }
        }
    }

    public static Savepoint setSavepoint() throws DotHibernateException {
        Connection conn;
        try {
            conn = getSession().connection();
			if (!conn.getAutoCommit()) {
				return conn.setSavepoint();
			}
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

    public static void saveWithPrimaryKey(Object obj, Serializable id)
            throws DotHibernateException {
        try {
            Session session = getSession();
            session.save(obj, id);
        } catch (Exception e) {
            throw new DotHibernateException(
                    "Unable to save Object with primary key " + id + " to Hibernate Session ", e);
        }
        try {
            Session session = getSession();
            session.flush();
        } catch (Exception e) {
            throw new DotHibernateException("Unable to flush Hibernate Session ", e);
        }
    }

    public void setDate(java.util.Date g) {
        query.setDate(t, g);
        t++;
    }

    public static void evict(Object obj) throws DotHibernateException {
        final Optional<Session> sessionOptional = getSessionIfOpened();
        try {
            if (sessionOptional.isPresent()) {
                sessionOptional.get().evict(obj);
            }
        } catch (HibernateException e) {
            throw new DotHibernateException("Unable to evict from Hibernate Session ", e);
        }
    }

    private static boolean asyncReindexCommitListeners() {
        return Config.getBooleanProperty("ASYNC_REINDEX_COMMIT_LISTENERS", true);
    }

    private static boolean asyncCommitListeners() {
        return Config.getBooleanProperty("ASYNC_COMMIT_LISTENERS", true);
    }

}
