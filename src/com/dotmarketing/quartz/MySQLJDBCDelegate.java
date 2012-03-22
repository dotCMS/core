package com.dotmarketing.quartz;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.quartz.Calendar;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.jdbcjobstore.FiredTriggerRecord;
import org.quartz.impl.jdbcjobstore.SchedulerStateRecord;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.utils.Key;
import org.quartz.utils.TriggerStatus;

import com.dotmarketing.util.Logger;

/**
 * <p>
 * This is meant to be an abstract base class for most, if not all, <code>{@link org.quartz.impl.jdbcjobstore.DriverDelegate}</code>
 * implementations. Subclasses should override only those methods that need
 * special handling for the DBMS driver in question.
 * </p>
 * 
 * @author <a href="mailto:jeff@binaryfeed.org">Jeffrey Wescott</a>
 * @author James House
 * @author Eric Mueller
 */
public class MySQLJDBCDelegate extends StdJDBCDelegate{

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    protected Log logger = null;

    protected String tablePrefix = DEFAULT_TABLE_PREFIX.toLowerCase();

    protected String instanceId;

    protected boolean useProperties;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Create new StdJDBCDelegate instance.
     * </p>
     * 
     * @param logger
     *          the logger to use during execution
     * @param tablePrefix
     *          the prefix of all table names
     */
    public MySQLJDBCDelegate(Log logger, String tablePrefix, String instanceId) {
        super(logger,tablePrefix,instanceId);
    }

    /**
     * <p>
     * Create new StdJDBCDelegate instance.
     * </p>
     * 
     * @param logger
     *          the logger to use during execution
     * @param tablePrefix
     *          the prefix of all table names
     */
    public MySQLJDBCDelegate(Log logger, String tablePrefix, String instanceId,
            Boolean useProperties) {
        super(logger,tablePrefix,instanceId,useProperties);
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    protected boolean canUseProperties() {
        return useProperties;
    }

    //---------------------------------------------------------------------------
    // startup / recovery
    //---------------------------------------------------------------------------

    /**
     * <p>
     * Insert the job detail record.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param newState
     *          the new state for the triggers
     * @param oldState1
     *          the first old state to update
     * @param oldState2
     *          the second old state to update
     * @return number of rows updated
     */
    public int updateTriggerStatesFromOtherStates(Connection conn,
            String newState, String oldState1, String oldState2)
        throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn
                    .prepareStatement(rtp(UPDATE_TRIGGER_STATES_FROM_OTHER_STATES.toLowerCase()));
            ps.setString(1, newState);
            ps.setString(2, oldState1);
            ps.setString(3, oldState2);
            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Get the names of all of the triggers that have misfired.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @return an array of <code>{@link
     * org.quartz.utils.Key}</code> objects
     */
    public Key[] selectMisfiredTriggers(Connection conn, long ts)
        throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_MISFIRED_TRIGGERS.toLowerCase()));
            ps.setBigDecimal(1, new BigDecimal(String.valueOf(ts)));
            rs = ps.executeQuery();

            ArrayList list = new ArrayList();
            while (rs.next()) {
                String triggerName = rs.getString(COL_TRIGGER_NAME.toLowerCase());
                String groupName = rs.getString(COL_TRIGGER_GROUP.toLowerCase());
                list.add(new Key(triggerName, groupName));
            }
            Object[] oArr = list.toArray();
            Key[] kArr = new Key[oArr.length];
            System.arraycopy(oArr, 0, kArr, 0, oArr.length);
            return kArr;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select all of the triggers in a given state.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param state
     *          the state the triggers must be in
     * @return an array of trigger <code>Key</code> s
     */
    public Key[] selectTriggersInState(Connection conn, String state)
        throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_TRIGGERS_IN_STATE.toLowerCase()));
            ps.setString(1, state);
            rs = ps.executeQuery();

            ArrayList list = new ArrayList();
            while (rs.next()) {
                list.add(new Key(rs.getString(1), rs.getString(2)));
            }

            Key[] sArr = (Key[]) list.toArray(new Key[list.size()]);
            return sArr;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    public Key[] selectMisfiredTriggersInState(Connection conn, String state,
            long ts) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_MISFIRED_TRIGGERS_IN_STATE.toLowerCase()));
            ps.setBigDecimal(1, new BigDecimal(String.valueOf(ts)));
            ps.setString(2, state);
            rs = ps.executeQuery();

            ArrayList list = new ArrayList();
            while (rs.next()) {
                String triggerName = rs.getString(COL_TRIGGER_NAME.toLowerCase());
                String groupName = rs.getString(COL_TRIGGER_GROUP.toLowerCase());
                list.add(new Key(triggerName, groupName));
            }
            Object[] oArr = list.toArray();
            Key[] kArr = new Key[oArr.length];
            System.arraycopy(oArr, 0, kArr, 0, oArr.length);
            return kArr;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Get the names of all of the triggers in the given states that have
     * misfired - according to the given timestamp.  No more than count will
     * be returned.
     * </p>
     * 
     * @param conn The DB Connection
     * @param count The most misfired triggers to return, negative for all
     * @param resultList Output parameter.  A List of 
     *      <code>{@link org.quartz.utils.Key}</code> objects.  Must not be null.
     *          
     * @return Whether there are more misfired triggers left to find beyond
     *         the given count.
     */
    public boolean selectMisfiredTriggersInStates(Connection conn, String state1, String state2,
        long ts, int count, List resultList) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_MISFIRED_TRIGGERS_IN_STATES.toLowerCase()));
            ps.setBigDecimal(1, new BigDecimal(String.valueOf(ts)));
            ps.setString(2, state1);
            ps.setString(3, state2);
            rs = ps.executeQuery();

            boolean hasReachedLimit = false;
            while (rs.next() && (hasReachedLimit == false)) {
                if (resultList.size() == count) {
                    hasReachedLimit = true;
                } else {
                    String triggerName = rs.getString(COL_TRIGGER_NAME.toLowerCase());
                    String groupName = rs.getString(COL_TRIGGER_GROUP.toLowerCase());
                    resultList.add(new Key(triggerName, groupName));
                }
            }
            
            return hasReachedLimit;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }
    
    /**
     * <p>
     * Get the number of triggers in the given states that have
     * misfired - according to the given timestamp.
     * </p>
     * 
     * @param conn the DB Connection
     */
    public int countMisfiredTriggersInStates(
            Connection conn, String state1, String state2, long ts) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(COUNT_MISFIRED_TRIGGERS_IN_STATES.toLowerCase()));
            ps.setBigDecimal(1, new BigDecimal(String.valueOf(ts)));
            ps.setString(2, state1);
            ps.setString(3, state2);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

            throw new SQLException("No misfired trigger count returned.");
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Get the names of all of the triggers in the given group and state that
     * have misfired.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @return an array of <code>{@link
     * org.quartz.utils.Key}</code> objects
     */
    public Key[] selectMisfiredTriggersInGroupInState(Connection conn,
            String groupName, String state, long ts) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn
                    .prepareStatement(rtp(SELECT_MISFIRED_TRIGGERS_IN_GROUP_IN_STATE.toLowerCase()));
            ps.setBigDecimal(1, new BigDecimal(String.valueOf(ts)));
            ps.setString(2, groupName);
            ps.setString(3, state);
            rs = ps.executeQuery();

            ArrayList list = new ArrayList();
            while (rs.next()) {
                String triggerName = rs.getString(COL_TRIGGER_NAME.toLowerCase());
                list.add(new Key(triggerName, groupName));
            }
            Object[] oArr = list.toArray();
            Key[] kArr = new Key[oArr.length];
            System.arraycopy(oArr, 0, kArr, 0, oArr.length);
            return kArr;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select all of the triggers for jobs that are requesting recovery. The
     * returned trigger objects will have unique "recoverXXX" trigger names and
     * will be in the <code>{@link
     * org.quartz.Scheduler}.DEFAULT_RECOVERY_GROUP</code>
     * trigger group.
     * </p>
     * 
     * <p>
     * In order to preserve the ordering of the triggers, the fire time will be
     * set from the <code>COL_FIRED_TIME</code> column in the <code>TABLE_FIRED_TRIGGERS</code>
     * table. The caller is responsible for calling <code>computeFirstFireTime</code>
     * on each returned trigger. It is also up to the caller to insert the
     * returned triggers to ensure that they are fired.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @return an array of <code>{@link org.quartz.Trigger}</code> objects
     */
    public Trigger[] selectTriggersForRecoveringJobs(Connection conn)
        throws SQLException, IOException, ClassNotFoundException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn
                    .prepareStatement(rtp(SELECT_INSTANCES_RECOVERABLE_FIRED_TRIGGERS.toLowerCase()));
            ps.setString(1, instanceId);
            setBoolean(ps, 2, true);
            rs = ps.executeQuery();

            long dumId = System.currentTimeMillis();
            ArrayList list = new ArrayList();
            while (rs.next()) {
                String jobName = rs.getString(COL_JOB_NAME.toLowerCase());
                String jobGroup = rs.getString(COL_JOB_GROUP.toLowerCase());
                String trigName = rs.getString(COL_TRIGGER_NAME.toLowerCase());
                String trigGroup = rs.getString(COL_TRIGGER_GROUP.toLowerCase());
                long firedTime = rs.getLong(COL_FIRED_TIME.toLowerCase());
                int priority = rs.getInt(COL_PRIORITY.toLowerCase());
                SimpleTrigger rcvryTrig = new SimpleTrigger("recover_"
                        + instanceId + "_" + String.valueOf(dumId++),
                        Scheduler.DEFAULT_RECOVERY_GROUP, new Date(firedTime));
                rcvryTrig.setJobName(jobName);
                rcvryTrig.setJobGroup(jobGroup);
                rcvryTrig.setPriority(priority);
                rcvryTrig
                        .setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

                JobDataMap jd = selectTriggerJobDataMap(conn, trigName, trigGroup);
                jd.put(Scheduler.FAILED_JOB_ORIGINAL_TRIGGER_NAME, trigName);
                jd.put(Scheduler.FAILED_JOB_ORIGINAL_TRIGGER_GROUP, trigGroup);
                jd.put(Scheduler.FAILED_JOB_ORIGINAL_TRIGGER_FIRETIME_IN_MILLISECONDS, String.valueOf(firedTime));
                rcvryTrig.setJobDataMap(jd);
                
                list.add(rcvryTrig);
            }
            Object[] oArr = list.toArray();
            Trigger[] tArr = new Trigger[oArr.length];
            System.arraycopy(oArr, 0, tArr, 0, oArr.length);
            return tArr;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Delete all fired triggers.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @return the number of rows deleted
     */
    public int deleteFiredTriggers(Connection conn) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(DELETE_FIRED_TRIGGERS.toLowerCase()));

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    public int deleteFiredTriggers(Connection conn, String instanceId)
        throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(DELETE_INSTANCES_FIRED_TRIGGERS.toLowerCase()));
            ps.setString(1, instanceId);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    //---------------------------------------------------------------------------
    // jobs
    //---------------------------------------------------------------------------

    /**
     * <p>
     * Insert the job detail record.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param job
     *          the job to insert
     * @return number of rows inserted
     * @throws IOException
     *           if there were problems serializing the JobDataMap
     */
    public int insertJobDetail(Connection conn, JobDetail job)
        throws IOException, SQLException {
        ByteArrayOutputStream baos = serializeJobData(job.getJobDataMap());

        PreparedStatement ps = null;

        int insertResult = 0;

        try {
            ps = conn.prepareStatement(rtp(INSERT_JOB_DETAIL.toLowerCase()));
            ps.setString(1, job.getName());
            ps.setString(2, job.getGroup());
            ps.setString(3, job.getDescription());
            ps.setString(4, job.getJobClass().getName());
            setBoolean(ps, 5, job.isDurable());
            setBoolean(ps, 6, job.isVolatile());
            setBoolean(ps, 7, job.isStateful());
            setBoolean(ps, 8, job.requestsRecovery());
            setBytes(ps, 9, baos);

            insertResult = ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }

        if (insertResult > 0) {
            String[] jobListeners = job.getJobListenerNames();
            for (int i = 0; jobListeners != null && i < jobListeners.length; i++) {
                insertJobListener(conn, job, jobListeners[i]);
            }
        }

        return insertResult;
    }

    /**
     * <p>
     * Update the job detail record.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param job
     *          the job to update
     * @return number of rows updated
     * @throws IOException
     *           if there were problems serializing the JobDataMap
     */
    public int updateJobDetail(Connection conn, JobDetail job)
        throws IOException, SQLException {
        ByteArrayOutputStream baos = serializeJobData(job.getJobDataMap());

        PreparedStatement ps = null;

        int insertResult = 0;

        try {
            ps = conn.prepareStatement(rtp(UPDATE_JOB_DETAIL.toLowerCase()));
            ps.setString(1, job.getDescription());
            ps.setString(2, job.getJobClass().getName());
            setBoolean(ps, 3, job.isDurable());
            setBoolean(ps, 4, job.isVolatile());
            setBoolean(ps, 5, job.isStateful());
            setBoolean(ps, 6, job.requestsRecovery());
            setBytes(ps, 7, baos);
            ps.setString(8, job.getName());
            ps.setString(9, job.getGroup());

            insertResult = ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }

        if (insertResult > 0) {
            deleteJobListeners(conn, job.getName(), job.getGroup());

            String[] jobListeners = job.getJobListenerNames();
            for (int i = 0; jobListeners != null && i < jobListeners.length; i++) {
                insertJobListener(conn, job, jobListeners[i]);
            }
        }

        return insertResult;
    }

    /**
     * <p>
     * Get the names of all of the triggers associated with the given job.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param jobName
     *          the name of the job
     * @param groupName
     *          the group containing the job
     * @return an array of <code>{@link
     * org.quartz.utils.Key}</code> objects
     */
    public Key[] selectTriggerNamesForJob(Connection conn, String jobName,
            String groupName) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_TRIGGERS_FOR_JOB.toLowerCase()));
            ps.setString(1, jobName);
            ps.setString(2, groupName);
            rs = ps.executeQuery();

            ArrayList list = new ArrayList(10);
            while (rs.next()) {
                String trigName = rs.getString(COL_TRIGGER_NAME.toLowerCase());
                String trigGroup = rs.getString(COL_TRIGGER_GROUP.toLowerCase());
                list.add(new Key(trigName, trigGroup));
            }
            Object[] oArr = list.toArray();
            Key[] kArr = new Key[oArr.length];
            System.arraycopy(oArr, 0, kArr, 0, oArr.length);
            return kArr;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Delete all job listeners for the given job.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param jobName
     *          the name of the job
     * @param groupName
     *          the group containing the job
     * @return the number of rows deleted
     */
    public int deleteJobListeners(Connection conn, String jobName,
            String groupName) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(DELETE_JOB_LISTENERS.toLowerCase()));
            ps.setString(1, jobName);
            ps.setString(2, groupName);
            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Delete the job detail record for the given job.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param jobName
     *          the name of the job
     * @param groupName
     *          the group containing the job
     * @return the number of rows deleted
     */
    public int deleteJobDetail(Connection conn, String jobName, String groupName)
        throws SQLException {
        PreparedStatement ps = null;

        try {
            Logger.debug(this, "Deleting job: " + groupName + "." + jobName);
            
            ps = conn.prepareStatement(rtp(DELETE_JOB_DETAIL.toLowerCase()));
            ps.setString(1, jobName);
            ps.setString(2, groupName);
            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Check whether or not the given job is stateful.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param jobName
     *          the name of the job
     * @param groupName
     *          the group containing the job
     * @return true if the job exists and is stateful, false otherwise
     */
    public boolean isJobStateful(Connection conn, String jobName,
            String groupName) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_JOB_STATEFUL.toLowerCase()));
            ps.setString(1, jobName);
            ps.setString(2, groupName);
            rs = ps.executeQuery();
            if (!rs.next()) { return false; }
            return getBoolean(rs, COL_IS_STATEFUL.toLowerCase());
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Check whether or not the given job exists.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param jobName
     *          the name of the job
     * @param groupName
     *          the group containing the job
     * @return true if the job exists, false otherwise
     */
    public boolean jobExists(Connection conn, String jobName, String groupName)
        throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_JOB_EXISTENCE.toLowerCase()));
            ps.setString(1, jobName);
            ps.setString(2, groupName);
            rs = ps.executeQuery();
            if (rs.next()) {
                return true;
            } else {
                return false;
            }
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }

    }

    /**
     * <p>
     * Update the job data map for the given job.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param job
     *          the job to update
     * @return the number of rows updated
     */
    public int updateJobData(Connection conn, JobDetail job)
        throws IOException, SQLException {
        ByteArrayOutputStream baos = serializeJobData(job.getJobDataMap());

        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(UPDATE_JOB_DATA.toLowerCase()));
            setBytes(ps, 1, baos);
            ps.setString(2, job.getName());
            ps.setString(3, job.getGroup());

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Associate a listener with a job.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param job
     *          the job to associate with the listener
     * @param listener
     *          the listener to insert
     * @return the number of rows inserted
     */
    public int insertJobListener(Connection conn, JobDetail job, String listener)
        throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(INSERT_JOB_LISTENER.toLowerCase()));
            ps.setString(1, job.getName());
            ps.setString(2, job.getGroup());
            ps.setString(3, listener);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Get all of the listeners for a given job.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param jobName
     *          the job name whose listeners are wanted
     * @param groupName
     *          the group containing the job
     * @return array of <code>String</code> listener names
     */
    public String[] selectJobListeners(Connection conn, String jobName,
            String groupName) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ArrayList list = new ArrayList();
            ps = conn.prepareStatement(rtp(SELECT_JOB_LISTENERS.toLowerCase()));
            ps.setString(1, jobName);
            ps.setString(2, groupName);
            rs = ps.executeQuery();

            while (rs.next()) {
                list.add(rs.getString(1));
            }

            Object[] oArr = list.toArray();
            String[] sArr = new String[oArr.length];
            System.arraycopy(oArr, 0, sArr, 0, oArr.length);
            return sArr;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select the JobDetail object for a given job name / group name.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param jobName
     *          the job name whose listeners are wanted
     * @param groupName
     *          the group containing the job
     * @return the populated JobDetail object
     * @throws ClassNotFoundException
     *           if a class found during deserialization cannot be found or if
     *           the job class could not be found
     * @throws IOException
     *           if deserialization causes an error
     */
    public JobDetail selectJobDetail(Connection conn, String jobName,
            String groupName, ClassLoadHelper loadHelper)
        throws ClassNotFoundException, IOException, SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_JOB_DETAIL.toLowerCase()));
            ps.setString(1, jobName);
            ps.setString(2, groupName);
            rs = ps.executeQuery();

            JobDetail job = null;

            if (rs.next()) {
                job = new JobDetail();

                job.setName(rs.getString(COL_JOB_NAME.toLowerCase()));
                job.setGroup(rs.getString(COL_JOB_GROUP.toLowerCase()));
                job.setDescription(rs.getString(COL_DESCRIPTION.toLowerCase()));
                job.setJobClass(loadHelper.loadClass(rs
                        .getString(COL_JOB_CLASS.toLowerCase())));
                job.setDurability(getBoolean(rs, COL_IS_DURABLE.toLowerCase()));
                job.setVolatility(getBoolean(rs, COL_IS_VOLATILE.toLowerCase()));
                job.setRequestsRecovery(getBoolean(rs, COL_REQUESTS_RECOVERY.toLowerCase()));

                Map map = null;
                if (canUseProperties()) {
                    map = getMapFromProperties(rs);
                } else {
                    map = (Map) getObjectFromBlob(rs, COL_JOB_DATAMAP.toLowerCase());
                }

                if (null != map) {
                    job.setJobDataMap(new JobDataMap(map));
                }
            }

            return job;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * build Map from java.util.Properties encoding.
     */
    private Map getMapFromProperties(ResultSet rs)
        throws ClassNotFoundException, IOException, SQLException {
        Map map;
        InputStream is = (InputStream) getJobDetailFromBlob(rs, COL_JOB_DATAMAP.toLowerCase());
        if(is == null) {
            return null;
        }
        Properties properties = new Properties();
        if (is != null) {
            try {
                properties.load(is);
            } finally {
                is.close();
            }
        }
        map = convertFromProperty(properties);
        return map;
    }

    /**
     * <p>
     * Select the total number of jobs stored.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @return the total number of jobs stored
     */
    public int selectNumJobs(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            int count = 0;
            ps = conn.prepareStatement(rtp(SELECT_NUM_JOBS.toLowerCase()));
            rs = ps.executeQuery();

            if (rs.next()) {
                count = rs.getInt(1);
            }

            return count;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select all of the job group names that are stored.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @return an array of <code>String</code> group names
     */
    public String[] selectJobGroups(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_JOB_GROUPS.toLowerCase()));
            rs = ps.executeQuery();

            ArrayList list = new ArrayList();
            while (rs.next()) {
                list.add(rs.getString(1));
            }

            Object[] oArr = list.toArray();
            String[] sArr = new String[oArr.length];
            System.arraycopy(oArr, 0, sArr, 0, oArr.length);
            return sArr;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select all of the jobs contained in a given group.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param groupName
     *          the group containing the jobs
     * @return an array of <code>String</code> job names
     */
    public String[] selectJobsInGroup(Connection conn, String groupName)
        throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_JOBS_IN_GROUP.toLowerCase()));
            ps.setString(1, groupName);
            rs = ps.executeQuery();

            ArrayList list = new ArrayList();
            while (rs.next()) {
                list.add(rs.getString(1));
            }

            Object[] oArr = list.toArray();
            String[] sArr = new String[oArr.length];
            System.arraycopy(oArr, 0, sArr, 0, oArr.length);
            return sArr;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    //---------------------------------------------------------------------------
    // triggers
    //---------------------------------------------------------------------------

    /**
     * <p>
     * Insert the base trigger data.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param trigger
     *          the trigger to insert
     * @param state
     *          the state that the trigger should be stored in
     * @return the number of rows inserted
     */
    public int insertTrigger(Connection conn, Trigger trigger, String state,
            JobDetail jobDetail) throws SQLException, IOException {

        ByteArrayOutputStream baos = null;
        if(trigger.getJobDataMap().size() > 0) {
            baos = serializeJobData(trigger.getJobDataMap());
        }
        
        PreparedStatement ps = null;

        int insertResult = 0;

        try {
            ps = conn.prepareStatement(rtp(INSERT_TRIGGER.toLowerCase()));
            ps.setString(1, trigger.getName());
            ps.setString(2, trigger.getGroup());
            ps.setString(3, trigger.getJobName());
            ps.setString(4, trigger.getJobGroup());
            setBoolean(ps, 5, trigger.isVolatile());
            ps.setString(6, trigger.getDescription());
            ps.setBigDecimal(7, new BigDecimal(String.valueOf(trigger
                    .getNextFireTime().getTime())));
            long prevFireTime = -1;
            if (trigger.getPreviousFireTime() != null) {
                prevFireTime = trigger.getPreviousFireTime().getTime();
            }
            ps.setBigDecimal(8, new BigDecimal(String.valueOf(prevFireTime)));
            ps.setString(9, state);
            if (trigger.getClass() == SimpleTrigger.class) {
                ps.setString(10, TTYPE_SIMPLE);
            } else if (trigger.getClass() == CronTrigger.class) {
                ps.setString(10, TTYPE_CRON);
            } else {
                ps.setString(10, TTYPE_BLOB);
            }
            ps.setBigDecimal(11, new BigDecimal(String.valueOf(trigger
                    .getStartTime().getTime())));
            long endTime = 0;
            if (trigger.getEndTime() != null) {
                endTime = trigger.getEndTime().getTime();
            }
            ps.setBigDecimal(12, new BigDecimal(String.valueOf(endTime)));
            ps.setString(13, trigger.getCalendarName());
            ps.setInt(14, trigger.getMisfireInstruction());
            setBytes(ps, 15, baos);
            ps.setInt(16, trigger.getPriority());
            
            insertResult = ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }

        if (insertResult > 0) {
            String[] trigListeners = trigger.getTriggerListenerNames();
            for (int i = 0; trigListeners != null && i < trigListeners.length; i++) {
                insertTriggerListener(conn, trigger, trigListeners[i]);
            }
        }

        return insertResult;
    }

    /**
     * <p>
     * Insert the simple trigger data.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param trigger
     *          the trigger to insert
     * @return the number of rows inserted
     */
    public int insertSimpleTrigger(Connection conn, SimpleTrigger trigger)
        throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(INSERT_SIMPLE_TRIGGER.toLowerCase()));
            ps.setString(1, trigger.getName());
            ps.setString(2, trigger.getGroup());
            ps.setInt(3, trigger.getRepeatCount());
            ps.setBigDecimal(4, new BigDecimal(String.valueOf(trigger
                    .getRepeatInterval())));
            ps.setInt(5, trigger.getTimesTriggered());

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Insert the cron trigger data.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param trigger
     *          the trigger to insert
     * @return the number of rows inserted
     */
    public int insertCronTrigger(Connection conn, CronTrigger trigger)
        throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(INSERT_CRON_TRIGGER.toLowerCase()));
            ps.setString(1, trigger.getName());
            ps.setString(2, trigger.getGroup());
            ps.setString(3, trigger.getCronExpression());
            ps.setString(4, trigger.getTimeZone().getID());

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Insert the blob trigger data.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param trigger
     *          the trigger to insert
     * @return the number of rows inserted
     */
    public int insertBlobTrigger(Connection conn, Trigger trigger)
        throws SQLException, IOException {
        PreparedStatement ps = null;
        ByteArrayOutputStream os = null;

        try {
            // update the blob
            os = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(trigger);
            oos.close();

            byte[] buf = os.toByteArray();
            ByteArrayInputStream is = new ByteArrayInputStream(buf);

            ps = conn.prepareStatement(rtp(INSERT_BLOB_TRIGGER.toLowerCase()));
            ps.setString(1, trigger.getName());
            ps.setString(2, trigger.getGroup());
            ps.setBinaryStream(3, is, buf.length);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Update the base trigger data.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param trigger
     *          the trigger to insert
     * @param state
     *          the state that the trigger should be stored in
     * @return the number of rows updated
     */
    public int updateTrigger(Connection conn, Trigger trigger, String state,
            JobDetail jobDetail) throws SQLException, IOException {

        // save some clock cycles by unnecessarily writing job data blob ...
        boolean updateJobData = trigger.getJobDataMap().isDirty();
        ByteArrayOutputStream baos = null;
        if(updateJobData && trigger.getJobDataMap().size() > 0) {
            baos = serializeJobData(trigger.getJobDataMap());
        }
                
        PreparedStatement ps = null;

        int insertResult = 0;


        try {
            if(updateJobData) {
                ps = conn.prepareStatement(rtp(UPDATE_TRIGGER.toLowerCase()));
            } else {
                ps = conn.prepareStatement(rtp(UPDATE_TRIGGER_SKIP_DATA.toLowerCase()));
            }
                
            ps.setString(1, trigger.getJobName());
            ps.setString(2, trigger.getJobGroup());
            setBoolean(ps, 3, trigger.isVolatile());
            ps.setString(4, trigger.getDescription());
            long nextFireTime = -1;
            if (trigger.getNextFireTime() != null) {
                nextFireTime = trigger.getNextFireTime().getTime();
            }
            ps.setBigDecimal(5, new BigDecimal(String.valueOf(nextFireTime)));
            long prevFireTime = -1;
            if (trigger.getPreviousFireTime() != null) {
                prevFireTime = trigger.getPreviousFireTime().getTime();
            }
            ps.setBigDecimal(6, new BigDecimal(String.valueOf(prevFireTime)));
            ps.setString(7, state);
            if (trigger.getClass() == SimpleTrigger.class) {
                //                updateSimpleTrigger(conn, (SimpleTrigger)trigger);
                ps.setString(8, TTYPE_SIMPLE);
            } else if (trigger.getClass() == CronTrigger.class) {
                //                updateCronTrigger(conn, (CronTrigger)trigger);
                ps.setString(8, TTYPE_CRON);
            } else {
                //                updateBlobTrigger(conn, trigger);
                ps.setString(8, TTYPE_BLOB);
            }
            ps.setBigDecimal(9, new BigDecimal(String.valueOf(trigger
                    .getStartTime().getTime())));
            long endTime = 0;
            if (trigger.getEndTime() != null) {
                endTime = trigger.getEndTime().getTime();
            }
            ps.setBigDecimal(10, new BigDecimal(String.valueOf(endTime)));
            ps.setString(11, trigger.getCalendarName());
            ps.setInt(12, trigger.getMisfireInstruction());
            ps.setInt(13, trigger.getPriority());

            if(updateJobData) {
                setBytes(ps, 14, baos);
                ps.setString(15, trigger.getName());
                ps.setString(16, trigger.getGroup());
            } else {
                ps.setString(14, trigger.getName());
                ps.setString(15, trigger.getGroup());
            }

            insertResult = ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }

        if (insertResult > 0) {
            deleteTriggerListeners(conn, trigger.getName(), trigger.getGroup());

            String[] trigListeners = trigger.getTriggerListenerNames();
            for (int i = 0; trigListeners != null && i < trigListeners.length; i++) {
                insertTriggerListener(conn, trigger, trigListeners[i]);
            }
        }

        return insertResult;
    }

    /**
     * <p>
     * Update the simple trigger data.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param trigger
     *          the trigger to insert
     * @return the number of rows updated
     */
    public int updateSimpleTrigger(Connection conn, SimpleTrigger trigger)
        throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(UPDATE_SIMPLE_TRIGGER.toLowerCase()));

            ps.setInt(1, trigger.getRepeatCount());
            ps.setBigDecimal(2, new BigDecimal(String.valueOf(trigger
                    .getRepeatInterval())));
            ps.setInt(3, trigger.getTimesTriggered());
            ps.setString(4, trigger.getName());
            ps.setString(5, trigger.getGroup());

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Update the cron trigger data.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param trigger
     *          the trigger to insert
     * @return the number of rows updated
     */
    public int updateCronTrigger(Connection conn, CronTrigger trigger)
        throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(UPDATE_CRON_TRIGGER.toLowerCase()));
            ps.setString(1, trigger.getCronExpression());
            ps.setString(2, trigger.getName());
            ps.setString(3, trigger.getGroup());

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Update the blob trigger data.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param trigger
     *          the trigger to insert
     * @return the number of rows updated
     */
    public int updateBlobTrigger(Connection conn, Trigger trigger)
        throws SQLException, IOException {
        PreparedStatement ps = null;
        ByteArrayOutputStream os = null;

        try {
            // update the blob
            os = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(trigger);
            oos.close();

            byte[] buf = os.toByteArray();
            ByteArrayInputStream is = new ByteArrayInputStream(buf);

            ps = conn.prepareStatement(rtp(UPDATE_BLOB_TRIGGER.toLowerCase()));
            ps.setBinaryStream(1, is, buf.length);
            ps.setString(2, trigger.getName());
            ps.setString(3, trigger.getGroup());

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
            if (os != null) {
                os.close();
            }
        }
    }

    /**
     * <p>
     * Check whether or not a trigger exists.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param triggerName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @return true if the trigger exists, false otherwise
     */
    public boolean triggerExists(Connection conn, String triggerName,
            String groupName) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_TRIGGER_EXISTENCE.toLowerCase()));
            ps.setString(1, triggerName);
            ps.setString(2, groupName);
            rs = ps.executeQuery();

            if (rs.next()) {
                return true;
            } else {
                return false;
            }
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Update the state for a given trigger.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param triggerName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @param state
     *          the new state for the trigger
     * @return the number of rows updated
     */
    public int updateTriggerState(Connection conn, String triggerName,
            String groupName, String state) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(UPDATE_TRIGGER_STATE.toLowerCase()));
            ps.setString(1, state);
            ps.setString(2, triggerName);
            ps.setString(3, groupName);
            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Update the given trigger to the given new state, if it is one of the
     * given old states.
     * </p>
     * 
     * @param conn
     *          the DB connection
     * @param triggerName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @param newState
     *          the new state for the trigger
     * @param oldState1
     *          one of the old state the trigger must be in
     * @param oldState2
     *          one of the old state the trigger must be in
     * @param oldState3
     *          one of the old state the trigger must be in
     * @return int the number of rows updated
     * @throws SQLException
     */
    public int updateTriggerStateFromOtherStates(Connection conn,
            String triggerName, String groupName, String newState,
            String oldState1, String oldState2, String oldState3)
        throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(UPDATE_TRIGGER_STATE_FROM_STATES.toLowerCase()));
            ps.setString(1, newState);
            ps.setString(2, triggerName);
            ps.setString(3, groupName);
            ps.setString(4, oldState1);
            ps.setString(5, oldState2);
            ps.setString(6, oldState3);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    public int updateTriggerStateFromOtherStatesBeforeTime(Connection conn,
            String newState, String oldState1, String oldState2, long time)
        throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn
                    .prepareStatement(rtp(UPDATE_TRIGGER_STATE_FROM_OTHER_STATES_BEFORE_TIME.toLowerCase()));
            ps.setString(1, newState);
            ps.setString(2, oldState1);
            ps.setString(3, oldState2);
            ps.setLong(4, time);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Update all triggers in the given group to the given new state, if they
     * are in one of the given old states.
     * </p>
     * 
     * @param conn
     *          the DB connection
     * @param groupName
     *          the group containing the trigger
     * @param newState
     *          the new state for the trigger
     * @param oldState1
     *          one of the old state the trigger must be in
     * @param oldState2
     *          one of the old state the trigger must be in
     * @param oldState3
     *          one of the old state the trigger must be in
     * @return int the number of rows updated
     * @throws SQLException
     */
    public int updateTriggerGroupStateFromOtherStates(Connection conn,
            String groupName, String newState, String oldState1,
            String oldState2, String oldState3) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn
                    .prepareStatement(rtp(UPDATE_TRIGGER_GROUP_STATE_FROM_STATES.toLowerCase()));
            ps.setString(1, newState);
            ps.setString(2, groupName);
            ps.setString(3, oldState1);
            ps.setString(4, oldState2);
            ps.setString(5, oldState3);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Update the given trigger to the given new state, if it is in the given
     * old state.
     * </p>
     * 
     * @param conn
     *          the DB connection
     * @param triggerName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @param newState
     *          the new state for the trigger
     * @param oldState
     *          the old state the trigger must be in
     * @return int the number of rows updated
     * @throws SQLException
     */
    public int updateTriggerStateFromOtherState(Connection conn,
            String triggerName, String groupName, String newState,
            String oldState) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(UPDATE_TRIGGER_STATE_FROM_STATE.toLowerCase()));
            ps.setString(1, newState);
            ps.setString(2, triggerName);
            ps.setString(3, groupName);
            ps.setString(4, oldState);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Update all of the triggers of the given group to the given new state, if
     * they are in the given old state.
     * </p>
     * 
     * @param conn
     *          the DB connection
     * @param groupName
     *          the group containing the triggers
     * @param newState
     *          the new state for the trigger group
     * @param oldState
     *          the old state the triggers must be in
     * @return int the number of rows updated
     * @throws SQLException
     */
    public int updateTriggerGroupStateFromOtherState(Connection conn,
            String groupName, String newState, String oldState)
        throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn
                    .prepareStatement(rtp(UPDATE_TRIGGER_GROUP_STATE_FROM_STATE.toLowerCase()));
            ps.setString(1, newState);
            ps.setString(2, groupName);
            ps.setString(3, oldState);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Update the states of all triggers associated with the given job.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param jobName
     *          the name of the job
     * @param groupName
     *          the group containing the job
     * @param state
     *          the new state for the triggers
     * @return the number of rows updated
     */
    public int updateTriggerStatesForJob(Connection conn, String jobName,
            String groupName, String state) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(UPDATE_JOB_TRIGGER_STATES.toLowerCase()));
            ps.setString(1, state);
            ps.setString(2, jobName);
            ps.setString(3, groupName);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    public int updateTriggerStatesForJobFromOtherState(Connection conn,
            String jobName, String groupName, String state, String oldState)
        throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn
                    .prepareStatement(rtp(UPDATE_JOB_TRIGGER_STATES_FROM_OTHER_STATE.toLowerCase()));
            ps.setString(1, state);
            ps.setString(2, jobName);
            ps.setString(3, groupName);
            ps.setString(4, oldState);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Delete all of the listeners associated with a given trigger.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param triggerName
     *          the name of the trigger whose listeners will be deleted
     * @param groupName
     *          the name of the group containing the trigger
     * @return the number of rows deleted
     */
    public int deleteTriggerListeners(Connection conn, String triggerName,
            String groupName) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(DELETE_TRIGGER_LISTENERS.toLowerCase()));
            ps.setString(1, triggerName);
            ps.setString(2, groupName);
            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Associate a listener with the given trigger.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param trigger
     *          the trigger
     * @param listener
     *          the name of the listener to associate with the trigger
     * @return the number of rows inserted
     */
    public int insertTriggerListener(Connection conn, Trigger trigger,
            String listener) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(INSERT_TRIGGER_LISTENER.toLowerCase()));
            ps.setString(1, trigger.getName());
            ps.setString(2, trigger.getGroup());
            ps.setString(3, listener);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select the listeners associated with a given trigger.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param triggerName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @return array of <code>String</code> trigger listener names
     */
    public String[] selectTriggerListeners(Connection conn, String triggerName,
            String groupName) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_TRIGGER_LISTENERS.toLowerCase()));
            ps.setString(1, triggerName);
            ps.setString(2, groupName);
            rs = ps.executeQuery();

            ArrayList list = new ArrayList();
            while (rs.next()) {
                list.add(rs.getString(1));
            }
            Object[] oArr = list.toArray();
            String[] sArr = new String[oArr.length];
            System.arraycopy(oArr, 0, sArr, 0, oArr.length);
            return sArr;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Delete the simple trigger data for a trigger.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param triggerName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @return the number of rows deleted
     */
    public int deleteSimpleTrigger(Connection conn, String triggerName,
            String groupName) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(DELETE_SIMPLE_TRIGGER.toLowerCase()));
            ps.setString(1, triggerName);
            ps.setString(2, groupName);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Delete the cron trigger data for a trigger.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param triggerName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @return the number of rows deleted
     */
    public int deleteCronTrigger(Connection conn, String triggerName,
            String groupName) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(DELETE_CRON_TRIGGER.toLowerCase()));
            ps.setString(1, triggerName);
            ps.setString(2, groupName);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Delete the cron trigger data for a trigger.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param triggerName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @return the number of rows deleted
     */
    public int deleteBlobTrigger(Connection conn, String triggerName,
            String groupName) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(DELETE_BLOB_TRIGGER.toLowerCase()));
            ps.setString(1, triggerName);
            ps.setString(2, groupName);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Delete the base trigger data for a trigger.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param triggerName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @return the number of rows deleted
     */
    public int deleteTrigger(Connection conn, String triggerName,
            String groupName) throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(DELETE_TRIGGER.toLowerCase()));
            ps.setString(1, triggerName);
            ps.setString(2, groupName);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select the number of triggers associated with a given job.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param jobName
     *          the name of the job
     * @param groupName
     *          the group containing the job
     * @return the number of triggers for the given job
     */
    public int selectNumTriggersForJob(Connection conn, String jobName,
            String groupName) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_NUM_TRIGGERS_FOR_JOB.toLowerCase()));
            ps.setString(1, jobName);
            ps.setString(2, groupName);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0;
            }
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select the job to which the trigger is associated.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param triggerName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @return the <code>{@link org.quartz.JobDetail}</code> object
     *         associated with the given trigger
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public JobDetail selectJobForTrigger(Connection conn, String triggerName,
            String groupName, ClassLoadHelper loadHelper) throws ClassNotFoundException, SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_JOB_FOR_TRIGGER.toLowerCase()));
            ps.setString(1, triggerName);
            ps.setString(2, groupName);
            rs = ps.executeQuery();

            if (rs.next()) {
                JobDetail job = new JobDetail();
                job.setName(rs.getString(1));
                job.setGroup(rs.getString(2));
                job.setDurability(getBoolean(rs, 3));
                job.setJobClass(loadHelper.loadClass(rs
                        .getString(4)));
                job.setRequestsRecovery(getBoolean(rs, 5));
                
                return job;
            } else {
            	Logger.debug(this, "No job for trigger '" + groupName + "."
                            + triggerName + "'.");
                
                return null;
            }
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select the triggers for a job
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param jobName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @return an array of <code>(@link org.quartz.Trigger)</code> objects
     *         associated with a given job.
     * @throws SQLException
     */
    public Trigger[] selectTriggersForJob(Connection conn, String jobName,
            String groupName) throws SQLException, ClassNotFoundException,
            IOException {

        ArrayList trigList = new ArrayList();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_TRIGGERS_FOR_JOB.toLowerCase()));
            ps.setString(1, jobName);
            ps.setString(2, groupName);
            rs = ps.executeQuery();

            while (rs.next()) {
                Trigger t = selectTrigger(conn,
                        rs.getString(COL_TRIGGER_NAME.toLowerCase()), 
                        rs.getString(COL_TRIGGER_GROUP.toLowerCase()));
                if(t != null) {
                    trigList.add(t);
                }
            }
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }

        return (Trigger[]) trigList.toArray(new Trigger[trigList.size()]);
    }

    public Trigger[] selectTriggersForCalendar(Connection conn, String calName)
        throws SQLException, ClassNotFoundException, IOException {

        ArrayList trigList = new ArrayList();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_TRIGGERS_FOR_CALENDAR.toLowerCase()));
            ps.setString(1, calName);
            rs = ps.executeQuery();

            while (rs.next()) {
                trigList.add(selectTrigger(conn,
                        rs.getString(COL_TRIGGER_NAME.toLowerCase()), rs
                                .getString(COL_TRIGGER_GROUP.toLowerCase())));
            }
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }

        return (Trigger[]) trigList.toArray(new Trigger[trigList.size()]);
    }
    
    public List selectStatefulJobsOfTriggerGroup(Connection conn,
            String groupName) throws SQLException {
        ArrayList jobList = new ArrayList();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn
                    .prepareStatement(rtp(SELECT_STATEFUL_JOBS_OF_TRIGGER_GROUP.toLowerCase()));
            ps.setString(1, groupName);
            setBoolean(ps, 2, true);
            rs = ps.executeQuery();

            while (rs.next()) {
                jobList.add(new Key(rs.getString(COL_JOB_NAME.toLowerCase()), rs
                        .getString(COL_JOB_GROUP.toLowerCase())));
            }
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }

        return jobList;
    }

    /**
     * <p>
     * Select a trigger.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param triggerName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @return the <code>{@link org.quartz.Trigger}</code> object
     */
    public Trigger selectTrigger(Connection conn, String triggerName,
            String groupName) throws SQLException, ClassNotFoundException,
            IOException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            Trigger trigger = null;

            ps = conn.prepareStatement(rtp(SELECT_TRIGGER.toLowerCase()));
            ps.setString(1, triggerName);
            ps.setString(2, groupName);
            rs = ps.executeQuery();

            if (rs.next()) {
                String jobName = rs.getString(COL_JOB_NAME.toLowerCase());
                String jobGroup = rs.getString(COL_JOB_GROUP.toLowerCase());
                boolean volatility = getBoolean(rs, COL_IS_VOLATILE.toLowerCase());
                String description = rs.getString(COL_DESCRIPTION.toLowerCase());
                long nextFireTime = rs.getLong(COL_NEXT_FIRE_TIME.toLowerCase());
                long prevFireTime = rs.getLong(COL_PREV_FIRE_TIME.toLowerCase());
                String triggerType = rs.getString(COL_TRIGGER_TYPE.toLowerCase());
                long startTime = rs.getLong(COL_START_TIME.toLowerCase());
                long endTime = rs.getLong(COL_END_TIME.toLowerCase());
                String calendarName = rs.getString(COL_CALENDAR_NAME.toLowerCase());
                int misFireInstr = rs.getInt(COL_MISFIRE_INSTRUCTION.toLowerCase());
                int priority = rs.getInt(COL_PRIORITY.toLowerCase());

                Map map = null;
                if (canUseProperties()) {
                    map = getMapFromProperties(rs);
                } else {
                    map = (Map) getObjectFromBlob(rs, COL_JOB_DATAMAP.toLowerCase());
                }
                
                Date nft = null;
                if (nextFireTime > 0) {
                    nft = new Date(nextFireTime);
                }

                Date pft = null;
                if (prevFireTime > 0) {
                    pft = new Date(prevFireTime);
                }
                Date startTimeD = new Date(startTime);
                Date endTimeD = null;
                if (endTime > 0) {
                    endTimeD = new Date(endTime);
                }

                rs.close();
                ps.close();

                if (triggerType.equals(TTYPE_SIMPLE)) {
                    ps = conn.prepareStatement(rtp(SELECT_SIMPLE_TRIGGER.toLowerCase()));
                    ps.setString(1, triggerName);
                    ps.setString(2, groupName);
                    rs = ps.executeQuery();

                    if (rs.next()) {
                        int repeatCount = rs.getInt(COL_REPEAT_COUNT.toLowerCase());
                        long repeatInterval = rs.getLong(COL_REPEAT_INTERVAL.toLowerCase());
                        int timesTriggered = rs.getInt(COL_TIMES_TRIGGERED.toLowerCase());

                        SimpleTrigger st = new SimpleTrigger(triggerName,
                                groupName, jobName, jobGroup, startTimeD,
                                endTimeD, repeatCount, repeatInterval);
                        st.setCalendarName(calendarName);
                        st.setMisfireInstruction(misFireInstr);
                        st.setTimesTriggered(timesTriggered);
                        st.setVolatility(volatility);
                        st.setNextFireTime(nft);
                        st.setPreviousFireTime(pft);
                        st.setDescription(description);
                        st.setPriority(priority);
                        if (null != map) {
                            st.setJobDataMap(new JobDataMap(map));
                        }
                        trigger = st;
                    }
                } else if (triggerType.equals(TTYPE_CRON)) {
                    ps = conn.prepareStatement(rtp(SELECT_CRON_TRIGGER.toLowerCase()));
                    ps.setString(1, triggerName);
                    ps.setString(2, groupName);
                    rs = ps.executeQuery();

                    if (rs.next()) {
                        String cronExpr = rs.getString(COL_CRON_EXPRESSION.toLowerCase());
                        String timeZoneId = rs.getString(COL_TIME_ZONE_ID.toLowerCase());

                        CronTrigger ct = null;
                        try {
                            TimeZone timeZone = null;
                            if (timeZoneId != null) {
                                timeZone = TimeZone.getTimeZone(timeZoneId);
                            }
                            ct = new CronTrigger(triggerName, groupName,
                                    jobName, jobGroup, startTimeD, endTimeD,
                                    cronExpr, timeZone);
                        } catch (Exception neverHappens) {
                            // expr must be valid, or it never would have
                            // gotten to the store...
                        }
                        if (null != ct) {
                            ct.setCalendarName(calendarName);
                            ct.setMisfireInstruction(misFireInstr);
                            ct.setVolatility(volatility);
                            ct.setNextFireTime(nft);
                            ct.setPreviousFireTime(pft);
                            ct.setDescription(description);
                            ct.setPriority(priority);
                            if (null != map) {
                                ct.setJobDataMap(new JobDataMap(map));
                            }
                            trigger = ct;
                        }
                    }
                } else if (triggerType.equals(TTYPE_BLOB)) {
                    ps = conn.prepareStatement(rtp(SELECT_BLOB_TRIGGER.toLowerCase()));
                    ps.setString(1, triggerName);
                    ps.setString(2, groupName);
                    rs = ps.executeQuery();

                    if (rs.next()) {
                        trigger = (Trigger) getObjectFromBlob(rs, COL_BLOB.toLowerCase());
                    }
                } else {
                    throw new ClassNotFoundException("class for trigger type '"
                            + triggerType + "' not found.");
                }
            }

            return trigger;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select a trigger's JobDataMap.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param triggerName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @return the <code>{@link org.quartz.JobDataMap}</code> of the Trigger,
     * never null, but possibly empty.
     */
    public JobDataMap selectTriggerJobDataMap(Connection conn, String triggerName,
            String groupName) throws SQLException, ClassNotFoundException,
            IOException {
        
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            Trigger trigger = null;

            ps = conn.prepareStatement(rtp(SELECT_TRIGGER_DATA.toLowerCase()));
            ps.setString(1, triggerName);
            ps.setString(2, groupName);
            rs = ps.executeQuery();

            if (rs.next()) {

                Map map = null;
                if (canUseProperties()) { 
                    map = getMapFromProperties(rs);
                } else {
                    map = (Map) getObjectFromBlob(rs, COL_JOB_DATAMAP.toLowerCase());
                }
                
                rs.close();
                ps.close();

                if (null != map) {
                    return new JobDataMap(map);
                }
            }
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
        
        return new JobDataMap();
    }
            

    /**
     * <p>
     * Select a trigger' state value.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param triggerName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @return the <code>{@link org.quartz.Trigger}</code> object
     */
    public String selectTriggerState(Connection conn, String triggerName,
            String groupName) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            String state = null;

            ps = conn.prepareStatement(rtp(SELECT_TRIGGER_STATE.toLowerCase()));
            ps.setString(1, triggerName);
            ps.setString(2, groupName);
            rs = ps.executeQuery();

            if (rs.next()) {
                state = rs.getString(COL_TRIGGER_STATE.toLowerCase());
            } else {
                state = STATE_DELETED;
            }

            return state.intern();
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }

    }

    /**
     * <p>
     * Select a trigger' status (state & next fire time).
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param triggerName
     *          the name of the trigger
     * @param groupName
     *          the group containing the trigger
     * @return a <code>TriggerStatus</code> object, or null
     */
    public TriggerStatus selectTriggerStatus(Connection conn,
            String triggerName, String groupName) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            TriggerStatus status = null;

            ps = conn.prepareStatement(rtp(SELECT_TRIGGER_STATUS.toLowerCase()));
            ps.setString(1, triggerName);
            ps.setString(2, groupName);
            rs = ps.executeQuery();

            if (rs.next()) {
                String state = rs.getString(COL_TRIGGER_STATE.toLowerCase());
                long nextFireTime = rs.getLong(COL_NEXT_FIRE_TIME.toLowerCase());
                String jobName = rs.getString(COL_JOB_NAME.toLowerCase());
                String jobGroup = rs.getString(COL_JOB_GROUP.toLowerCase());

                Date nft = null;
                if (nextFireTime > 0) {
                    nft = new Date(nextFireTime);
                }

                status = new TriggerStatus(state, nft);
                status.setKey(new Key(triggerName, groupName));
                status.setJobKey(new Key(jobName, jobGroup));
            }

            return status;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }

    }

    /**
     * <p>
     * Select the total number of triggers stored.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @return the total number of triggers stored
     */
    public int selectNumTriggers(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            int count = 0;
            ps = conn.prepareStatement(rtp(SELECT_NUM_TRIGGERS.toLowerCase()));
            rs = ps.executeQuery();

            if (rs.next()) {
                count = rs.getInt(1);
            }

            return count;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select all of the trigger group names that are stored.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @return an array of <code>String</code> group names
     */
    public String[] selectTriggerGroups(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_TRIGGER_GROUPS.toLowerCase()));
            rs = ps.executeQuery();

            ArrayList list = new ArrayList();
            while (rs.next()) {
                list.add(rs.getString(1));
            }

            Object[] oArr = list.toArray();
            String[] sArr = new String[oArr.length];
            System.arraycopy(oArr, 0, sArr, 0, oArr.length);
            return sArr;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select all of the triggers contained in a given group.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param groupName
     *          the group containing the triggers
     * @return an array of <code>String</code> trigger names
     */
    public String[] selectTriggersInGroup(Connection conn, String groupName)
        throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_TRIGGERS_IN_GROUP.toLowerCase()));
            ps.setString(1, groupName);
            rs = ps.executeQuery();

            ArrayList list = new ArrayList();
            while (rs.next()) {
                list.add(rs.getString(1));
            }

            Object[] oArr = list.toArray();
            String[] sArr = new String[oArr.length];
            System.arraycopy(oArr, 0, sArr, 0, oArr.length);
            return sArr;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    public int insertPausedTriggerGroup(Connection conn, String groupName)
        throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(INSERT_PAUSED_TRIGGER_GROUP.toLowerCase()));
            ps.setString(1, groupName);
            int rows = ps.executeUpdate();

            return rows;
        } finally {
            closeStatement(ps);
        }
    }

    public int deletePausedTriggerGroup(Connection conn, String groupName)
        throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(DELETE_PAUSED_TRIGGER_GROUP.toLowerCase()));
            ps.setString(1, groupName);
            int rows = ps.executeUpdate();

            return rows;
        } finally {
            closeStatement(ps);
        }
    }

    public int deleteAllPausedTriggerGroups(Connection conn)
        throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(DELETE_PAUSED_TRIGGER_GROUPS.toLowerCase()));
            int rows = ps.executeUpdate();

            return rows;
        } finally {
            closeStatement(ps);
        }
    }

    public boolean isTriggerGroupPaused(Connection conn, String groupName)
        throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_PAUSED_TRIGGER_GROUP.toLowerCase()));
            ps.setString(1, groupName);
            rs = ps.executeQuery();

            return rs.next();
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    public boolean isExistingTriggerGroup(Connection conn, String groupName)
        throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_NUM_TRIGGERS_IN_GROUP.toLowerCase()));
            ps.setString(1, groupName);
            rs = ps.executeQuery();

            if (!rs.next()) {
                return false;
            }

            return (rs.getInt(1) > 0);
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    //---------------------------------------------------------------------------
    // calendars
    //---------------------------------------------------------------------------

    /**
     * <p>
     * Insert a new calendar.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param calendarName
     *          the name for the new calendar
     * @param calendar
     *          the calendar
     * @return the number of rows inserted
     * @throws IOException
     *           if there were problems serializing the calendar
     */
    public int insertCalendar(Connection conn, String calendarName,
            Calendar calendar) throws IOException, SQLException {
        ByteArrayOutputStream baos = serializeObject(calendar);

        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(INSERT_CALENDAR.toLowerCase()));
            ps.setString(1, calendarName);
            setBytes(ps, 2, baos);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Update a calendar.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param calendarName
     *          the name for the new calendar
     * @param calendar
     *          the calendar
     * @return the number of rows updated
     * @throws IOException
     *           if there were problems serializing the calendar
     */
    public int updateCalendar(Connection conn, String calendarName,
            Calendar calendar) throws IOException, SQLException {
        ByteArrayOutputStream baos = serializeObject(calendar);

        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(UPDATE_CALENDAR.toLowerCase()));
            setBytes(ps, 1, baos);
            ps.setString(2, calendarName);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Check whether or not a calendar exists.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param calendarName
     *          the name of the calendar
     * @return true if the trigger exists, false otherwise
     */
    public boolean calendarExists(Connection conn, String calendarName)
        throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_CALENDAR_EXISTENCE.toLowerCase()));
            ps.setString(1, calendarName);
            rs = ps.executeQuery();

            if (rs.next()) {
                return true;
            } else {
                return false;
            }
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select a calendar.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param calendarName
     *          the name of the calendar
     * @return the Calendar
     * @throws ClassNotFoundException
     *           if a class found during deserialization cannot be found be
     *           found
     * @throws IOException
     *           if there were problems deserializing the calendar
     */
    public Calendar selectCalendar(Connection conn, String calendarName)
        throws ClassNotFoundException, IOException, SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String selCal = rtp(SELECT_CALENDAR.toLowerCase());
            ps = conn.prepareStatement(selCal);
            ps.setString(1, calendarName);
            rs = ps.executeQuery();

            Calendar cal = null;
            if (rs.next()) {
                cal = (Calendar) getObjectFromBlob(rs, COL_CALENDAR.toLowerCase());
            }
            if (null == cal) {
                logger.warn("Couldn't find calendar with name '" + calendarName
                        + "'.");
            }
            return cal;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Check whether or not a calendar is referenced by any triggers.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param calendarName
     *          the name of the calendar
     * @return true if any triggers reference the calendar, false otherwise
     */
    public boolean calendarIsReferenced(Connection conn, String calendarName)
        throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(rtp(SELECT_REFERENCED_CALENDAR.toLowerCase()));
            ps.setString(1, calendarName);
            rs = ps.executeQuery();

            if (rs.next()) {
                return true;
            } else {
                return false;
            }
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Delete a calendar.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param calendarName
     *          the name of the trigger
     * @return the number of rows deleted
     */
    public int deleteCalendar(Connection conn, String calendarName)
        throws SQLException {
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(DELETE_CALENDAR.toLowerCase()));
            ps.setString(1, calendarName);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select the total number of calendars stored.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @return the total number of calendars stored
     */
    public int selectNumCalendars(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            int count = 0;
            ps = conn.prepareStatement(rtp(SELECT_NUM_CALENDARS.toLowerCase()));

            rs = ps.executeQuery();

            if (rs.next()) {
                count = rs.getInt(1);
            }

            return count;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select all of the stored calendars.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @return an array of <code>String</code> calendar names
     */
    public String[] selectCalendars(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_CALENDARS.toLowerCase()));
            rs = ps.executeQuery();

            ArrayList list = new ArrayList();
            while (rs.next()) {
                list.add(rs.getString(1));
            }

            Object[] oArr = list.toArray();
            String[] sArr = new String[oArr.length];
            System.arraycopy(oArr, 0, sArr, 0, oArr.length);
            return sArr;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    //---------------------------------------------------------------------------
    // trigger firing
    //---------------------------------------------------------------------------

    /**
     * <p>
     * Select the next time that a trigger will be fired.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @return the next fire time, or 0 if no trigger will be fired
     * 
     * @deprecated Does not account for misfires.
     */
    public long selectNextFireTime(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(rtp(SELECT_NEXT_FIRE_TIME.toLowerCase()));
            ps.setString(1, STATE_WAITING);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getLong(ALIAS_COL_NEXT_FIRE_TIME.toLowerCase());
            } else {
                return 0l;
            }
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select the trigger that will be fired at the given fire time.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param fireTime
     *          the time that the trigger will be fired
     * @return a <code>{@link org.quartz.utils.Key}</code> representing the
     *         trigger that will be fired at the given fire time, or null if no
     *         trigger will be fired at that time
     */
    public Key selectTriggerForFireTime(Connection conn, long fireTime)
        throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(rtp(SELECT_TRIGGER_FOR_FIRE_TIME.toLowerCase()));
            ps.setString(1, STATE_WAITING);
            ps.setBigDecimal(2, new BigDecimal(String.valueOf(fireTime)));
            rs = ps.executeQuery();

            if (rs.next()) {
                return new Key(rs.getString(COL_TRIGGER_NAME.toLowerCase()), rs
                        .getString(COL_TRIGGER_GROUP.toLowerCase()));
            } else {
                return null;
            }
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select the next trigger which will fire to fire between the two given timestamps 
     * in ascending order of fire time, and then descending by priority.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param noLaterThan
     *          highest value of <code>getNextFireTime()</code> of the triggers (exclusive)
     * @param noEarlierThan 
     *          highest value of <code>getNextFireTime()</code> of the triggers (inclusive)
     *          
     * @return The next identifier of the next trigger to be fired.
     */
    public Key selectTriggerToAcquire(Connection conn, long noLaterThan, long noEarlierThan)
        throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(rtp(SELECT_NEXT_TRIGGER_TO_ACQUIRE.toLowerCase()));
            
            // Try to give jdbc driver a hint to hopefully not pull over 
            // more than the one row we actually need.
            ps.setFetchSize(1);
            ps.setMaxRows(1);
            
            ps.setString(1, STATE_WAITING);
            ps.setBigDecimal(2, new BigDecimal(String.valueOf(noLaterThan)));
            ps.setBigDecimal(3, new BigDecimal(String.valueOf(noEarlierThan)));
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return new Key(
                        rs.getString(COL_TRIGGER_NAME.toLowerCase()),
                        rs.getString(COL_TRIGGER_GROUP.toLowerCase()));
            }
            
            return null;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }      
    }

    /**
     * <p>
     * Insert a fired trigger.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param trigger
     *          the trigger
     * @param state
     *          the state that the trigger should be stored in
     * @return the number of rows inserted
     */
    public int insertFiredTrigger(Connection conn, Trigger trigger,
            String state, JobDetail job) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp(INSERT_FIRED_TRIGGER.toLowerCase()));
            ps.setString(1, trigger.getFireInstanceId());
            ps.setString(2, trigger.getName());
            ps.setString(3, trigger.getGroup());
            setBoolean(ps, 4, trigger.isVolatile());
            ps.setString(5, super.instanceId);
            ps.setBigDecimal(6, new BigDecimal(String.valueOf(trigger
                    .getNextFireTime().getTime())));
            ps.setString(7, state);
            if (job != null) {
                ps.setString(8, trigger.getJobName());
                ps.setString(9, trigger.getJobGroup());
                setBoolean(ps, 10, job.isStateful());
                setBoolean(ps, 11, job.requestsRecovery());
            } else {
                ps.setString(8, null);
                ps.setString(9, null);
                setBoolean(ps, 10, false);
                setBoolean(ps, 11, false);
            }
            ps.setInt(12, trigger.getPriority());
            

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select the states of all fired-trigger records for a given trigger, or
     * trigger group if trigger name is <code>null</code>.
     * </p>
     * 
     * @return a List of FiredTriggerRecord objects.
     */
    public List selectFiredTriggerRecords(Connection conn, String triggerName,
            String groupName) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            List lst = new LinkedList();

            if (triggerName != null) {
                ps = conn.prepareStatement(rtp(SELECT_FIRED_TRIGGER.toLowerCase()));
                ps.setString(1, triggerName);
                ps.setString(2, groupName);
            } else {
                ps = conn.prepareStatement(rtp(SELECT_FIRED_TRIGGER_GROUP.toLowerCase()));
                ps.setString(1, groupName);
            }
            rs = ps.executeQuery();

            while (rs.next()) {
                FiredTriggerRecord rec = new FiredTriggerRecord();

                rec.setFireInstanceId(rs.getString(COL_ENTRY_ID.toLowerCase()));
                rec.setFireInstanceState(rs.getString(COL_ENTRY_STATE.toLowerCase()));
                rec.setFireTimestamp(rs.getLong(COL_FIRED_TIME.toLowerCase()));
                rec.setPriority(rs.getInt(COL_PRIORITY.toLowerCase()));
                rec.setSchedulerInstanceId(rs.getString(COL_INSTANCE_NAME.toLowerCase()));
                rec.setTriggerIsVolatile(getBoolean(rs, COL_IS_VOLATILE.toLowerCase()));
                rec.setTriggerKey(new Key(rs.getString(COL_TRIGGER_NAME.toLowerCase()), rs
                        .getString(COL_TRIGGER_GROUP.toLowerCase())));
                if (!rec.getFireInstanceState().equals(STATE_ACQUIRED)) {
                    rec.setJobIsStateful(getBoolean(rs, COL_IS_STATEFUL.toLowerCase()));
                    rec.setJobRequestsRecovery(rs
                            .getBoolean(COL_REQUESTS_RECOVERY.toLowerCase()));
                    rec.setJobKey(new Key(rs.getString(COL_JOB_NAME.toLowerCase()), rs
                            .getString(COL_JOB_GROUP.toLowerCase())));
                }
                lst.add(rec);
            }

            return lst;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select the states of all fired-trigger records for a given job, or job
     * group if job name is <code>null</code>.
     * </p>
     * 
     * @return a List of FiredTriggerRecord objects.
     */
    public List selectFiredTriggerRecordsByJob(Connection conn, String jobName,
            String groupName) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            List lst = new LinkedList();

            if (jobName != null) {
                ps = conn.prepareStatement(rtp(SELECT_FIRED_TRIGGERS_OF_JOB.toLowerCase()));
                ps.setString(1, jobName);
                ps.setString(2, groupName);
            } else {
                ps = conn
                        .prepareStatement(rtp(SELECT_FIRED_TRIGGERS_OF_JOB_GROUP.toLowerCase()));
                ps.setString(1, groupName);
            }
            rs = ps.executeQuery();

            while (rs.next()) {
                FiredTriggerRecord rec = new FiredTriggerRecord();

                rec.setFireInstanceId(rs.getString(COL_ENTRY_ID.toLowerCase()));
                rec.setFireInstanceState(rs.getString(COL_ENTRY_STATE.toLowerCase()));
                rec.setFireTimestamp(rs.getLong(COL_FIRED_TIME.toLowerCase()));
                rec.setPriority(rs.getInt(COL_PRIORITY.toLowerCase()));
                rec.setSchedulerInstanceId(rs.getString(COL_INSTANCE_NAME.toLowerCase()));
                rec.setTriggerIsVolatile(getBoolean(rs, COL_IS_VOLATILE.toLowerCase()));
                rec.setTriggerKey(new Key(rs.getString(COL_TRIGGER_NAME.toLowerCase()), rs
                        .getString(COL_TRIGGER_GROUP.toLowerCase())));
                if (!rec.getFireInstanceState().equals(STATE_ACQUIRED)) {
                    rec.setJobIsStateful(getBoolean(rs, COL_IS_STATEFUL.toLowerCase()));
                    rec.setJobRequestsRecovery(rs
                            .getBoolean(COL_REQUESTS_RECOVERY.toLowerCase()));
                    rec.setJobKey(new Key(rs.getString(COL_JOB_NAME.toLowerCase()), rs
                            .getString(COL_JOB_GROUP.toLowerCase())));
                }
                lst.add(rec);
            }

            return lst;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }

    }

    public List selectInstancesFiredTriggerRecords(Connection conn,
            String instanceName) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            List lst = new LinkedList();

            ps = conn.prepareStatement(rtp(SELECT_INSTANCES_FIRED_TRIGGERS.toLowerCase()));
            ps.setString(1, instanceName);
            rs = ps.executeQuery();

            while (rs.next()) {
                FiredTriggerRecord rec = new FiredTriggerRecord();

                rec.setFireInstanceId(rs.getString(COL_ENTRY_ID.toLowerCase()));
                rec.setFireInstanceState(rs.getString(COL_ENTRY_STATE.toLowerCase()));
                rec.setFireTimestamp(rs.getLong(COL_FIRED_TIME.toLowerCase()));
                rec.setSchedulerInstanceId(rs.getString(COL_INSTANCE_NAME.toLowerCase()));
                rec.setTriggerIsVolatile(getBoolean(rs, COL_IS_VOLATILE.toLowerCase()));
                rec.setTriggerKey(new Key(rs.getString(COL_TRIGGER_NAME.toLowerCase()), rs
                        .getString(COL_TRIGGER_GROUP.toLowerCase())));
                if (!rec.getFireInstanceState().equals(STATE_ACQUIRED)) {
                    rec.setJobIsStateful(getBoolean(rs, COL_IS_STATEFUL.toLowerCase()));
                    rec.setJobRequestsRecovery(rs
                            .getBoolean(COL_REQUESTS_RECOVERY.toLowerCase()));
                    rec.setJobKey(new Key(rs.getString(COL_JOB_NAME.toLowerCase()), rs
                            .getString(COL_JOB_GROUP.toLowerCase())));
                }
                rec.setPriority(rs.getInt(COL_PRIORITY.toLowerCase()));
                lst.add(rec);
            }

            return lst;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    /**
     * <p>
     * Select the distinct instance names of all fired-trigger records.
     * </p>
     * 
     * <p>
     * This is useful when trying to identify orphaned fired triggers (a 
     * fired trigger without a scheduler state record.) 
     * </p>
     * 
     * @return a Set of String objects.
     */
    public Set selectFiredTriggerInstanceNames(Connection conn) 
        throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Set instanceNames = new HashSet();

            ps = conn.prepareStatement(rtp(SELECT_FIRED_TRIGGER_INSTANCE_NAMES.toLowerCase()));
            rs = ps.executeQuery();

            while (rs.next()) {
                instanceNames.add(rs.getString(COL_INSTANCE_NAME.toLowerCase()));
            }

            return instanceNames;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }
    
    /**
     * <p>
     * Delete a fired trigger.
     * </p>
     * 
     * @param conn
     *          the DB Connection
     * @param entryId
     *          the fired trigger entry to delete
     * @return the number of rows deleted
     */
    public int deleteFiredTrigger(Connection conn, String entryId)
        throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp(DELETE_FIRED_TRIGGER.toLowerCase()));
            ps.setString(1, entryId);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    public int selectJobExecutionCount(Connection conn, String jobName,
            String jobGroup) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_JOB_EXECUTION_COUNT.toLowerCase()));
            ps.setString(1, jobName);
            ps.setString(2, jobGroup);

            rs = ps.executeQuery();

            return (rs.next()) ? rs.getInt(1) : 0;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    public int deleteVolatileFiredTriggers(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp(DELETE_VOLATILE_FIRED_TRIGGERS.toLowerCase()));
            setBoolean(ps, 1, true);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }
    
    public int insertSchedulerState(Connection conn, String instanceId,
            long checkInTime, long interval)
        throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp(INSERT_SCHEDULER_STATE.toLowerCase()));
            ps.setString(1, instanceId);
            ps.setLong(2, checkInTime);
            ps.setLong(3, interval);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    public int deleteSchedulerState(Connection conn, String instanceId)
        throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp(DELETE_SCHEDULER_STATE.toLowerCase()));
            ps.setString(1, instanceId);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    public int updateSchedulerState(Connection conn, String instanceId, long checkInTime)
        throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(rtp(UPDATE_SCHEDULER_STATE.toLowerCase()));
            ps.setLong(1, checkInTime);
            ps.setString(2, instanceId);
        
            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }
        
    public List selectSchedulerStateRecords(Connection conn, String instanceId)
        throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            List lst = new LinkedList();

            if (instanceId != null) {
                ps = conn.prepareStatement(rtp(SELECT_SCHEDULER_STATE.toLowerCase()));
                ps.setString(1, instanceId);
            } else {
                ps = conn.prepareStatement(rtp(SELECT_SCHEDULER_STATES.toLowerCase()));
            }
            rs = ps.executeQuery();

            while (rs.next()) {
                SchedulerStateRecord rec = new SchedulerStateRecord();

                rec.setSchedulerInstanceId(rs.getString(COL_INSTANCE_NAME.toLowerCase()));
                rec.setCheckinTimestamp(rs.getLong(COL_LAST_CHECKIN_TIME.toLowerCase()));
                rec.setCheckinInterval(rs.getLong(COL_CHECKIN_INTERVAL.toLowerCase()));

                lst.add(rec);
            }

            return lst;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }

    }




    public Key[] selectVolatileTriggers(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_VOLATILE_TRIGGERS.toLowerCase()));
            setBoolean(ps, 1, true);
            rs = ps.executeQuery();

            ArrayList list = new ArrayList();
            while (rs.next()) {
                String triggerName = rs.getString(COL_TRIGGER_NAME.toLowerCase());
                String groupName = rs.getString(COL_TRIGGER_GROUP.toLowerCase());
                list.add(new Key(triggerName, groupName));
            }
            Object[] oArr = list.toArray();
            Key[] kArr = new Key[oArr.length];
            System.arraycopy(oArr, 0, kArr, 0, oArr.length);
            return kArr;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

    public Key[] selectVolatileJobs(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_VOLATILE_JOBS.toLowerCase()));
            setBoolean(ps, 1, true);
            rs = ps.executeQuery();

            ArrayList list = new ArrayList();
            while (rs.next()) {
                String triggerName = rs.getString(COL_JOB_NAME.toLowerCase());
                String groupName = rs.getString(COL_JOB_GROUP.toLowerCase());
                list.add(new Key(triggerName, groupName));
            }
            Object[] oArr = list.toArray();
            Key[] kArr = new Key[oArr.length];
            System.arraycopy(oArr, 0, kArr, 0, oArr.length);
            return kArr;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }


    /** 
     * @see org.quartz.impl.jdbcjobstore.DriverDelegate#selectPausedTriggerGroups(java.sql.Connection)
     */
    public Set selectPausedTriggerGroups(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        HashSet set = new HashSet();
        try {
            ps = conn.prepareStatement(rtp(SELECT_PAUSED_TRIGGER_GROUPS.toLowerCase()));
            rs = ps.executeQuery();

            while (rs.next()) {
                String groupName = rs.getString(COL_TRIGGER_GROUP.toLowerCase());
                set.add(groupName);
            }
            return set;
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
        }
    }

}

// EOF
