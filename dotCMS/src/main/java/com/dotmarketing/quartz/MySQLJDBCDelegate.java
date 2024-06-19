package com.dotmarketing.quartz;

import java.math.BigDecimal;
import org.quartz.JobDetail;
import org.quartz.impl.jdbcjobstore.NoSuchDelegateException;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;
import org.quartz.impl.jdbcjobstore.TriggerPersistenceDelegate;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.OperableTrigger;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * MySQLDelegate extends StdJDBCDelegate to provide specific functionality for MySQL.
 */
public class MySQLJDBCDelegate extends StdJDBCDelegate {

    public MySQLJDBCDelegate() {
        super();
    }

    @Override
    public int insertTrigger(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail) throws SQLException, IOException {
        ByteArrayOutputStream baos = null;
        if (trigger.getJobDataMap().size() > 0) {
            baos = serializeJobData(trigger.getJobDataMap());
        }

        try (PreparedStatement ps = conn.prepareStatement(rtp(INSERT_TRIGGER))) {
            ps.setString(1, trigger.getKey().getName());
            ps.setString(2, trigger.getKey().getGroup());
            ps.setString(3, trigger.getJobKey().getName());
            ps.setString(4, trigger.getJobKey().getGroup());
            ps.setString(5, trigger.getDescription());
            if (trigger.getNextFireTime() != null)
                ps.setBigDecimal(6, new BigDecimal(String.valueOf(trigger.getNextFireTime().getTime())));
            else
                ps.setBigDecimal(6, null);
            long prevFireTime = -1;
            if (trigger.getPreviousFireTime() != null) {
                prevFireTime = trigger.getPreviousFireTime().getTime();
            }
            ps.setBigDecimal(7, new BigDecimal(String.valueOf(prevFireTime)));
            ps.setString(8, state);

            TriggerPersistenceDelegate tDel = findTriggerPersistenceDelegate(trigger);
            String type = TTYPE_BLOB;
            if (tDel != null) {
                type = tDel.getHandledTriggerTypeDiscriminator();
            }
            ps.setString(9, type);

            ps.setBigDecimal(10, new BigDecimal(String.valueOf(trigger.getStartTime().getTime())));
            long endTime = 0;
            if (trigger.getEndTime() != null) {
                endTime = trigger.getEndTime().getTime();
            }
            ps.setBigDecimal(11, new BigDecimal(String.valueOf(endTime)));
            ps.setString(12, trigger.getCalendarName());
            ps.setInt(13, trigger.getMisfireInstruction());
            setBytes(ps, 14, baos);
            ps.setInt(15, trigger.getPriority());

            int insertResult = ps.executeUpdate();

            if (tDel == null) {
                insertBlobTrigger(conn, trigger);
            } else {
                tDel.insertExtendedTriggerProperties(conn, trigger, state, jobDetail);
            }

            return insertResult;
        }
    }

    @Override
    public int updateTrigger(Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail) throws SQLException, IOException {
        boolean updateJobData = trigger.getJobDataMap().isDirty();
        ByteArrayOutputStream baos = null;
        if (updateJobData) {
            baos = serializeJobData(trigger.getJobDataMap());
        }

        try (PreparedStatement ps = conn.prepareStatement(updateJobData ? rtp(UPDATE_TRIGGER) : rtp(UPDATE_TRIGGER_SKIP_DATA))) {
            ps.setString(1, trigger.getJobKey().getName());
            ps.setString(2, trigger.getJobKey().getGroup());
            ps.setString(3, trigger.getDescription());
            long nextFireTime = -1;
            if (trigger.getNextFireTime() != null) {
                nextFireTime = trigger.getNextFireTime().getTime();
            }
            ps.setBigDecimal(4, new BigDecimal(String.valueOf(nextFireTime)));
            long prevFireTime = -1;
            if (trigger.getPreviousFireTime() != null) {
                prevFireTime = trigger.getPreviousFireTime().getTime();
            }
            ps.setBigDecimal(5, new BigDecimal(String.valueOf(prevFireTime)));
            ps.setString(6, state);

            TriggerPersistenceDelegate tDel = findTriggerPersistenceDelegate(trigger);
            String type = TTYPE_BLOB;
            if (tDel != null) {
                type = tDel.getHandledTriggerTypeDiscriminator();
            }
            ps.setString(7, type);

            ps.setBigDecimal(8, new BigDecimal(String.valueOf(trigger.getStartTime().getTime())));
            long endTime = 0;
            if (trigger.getEndTime() != null) {
                endTime = trigger.getEndTime().getTime();
            }
            ps.setBigDecimal(9, new BigDecimal(String.valueOf(endTime)));
            ps.setString(10, trigger.getCalendarName());
            ps.setInt(11, trigger.getMisfireInstruction());
            ps.setInt(12, trigger.getPriority());

            if (updateJobData) {
                setBytes(ps, 13, baos);
                ps.setString(14, trigger.getKey().getName());
                ps.setString(15, trigger.getKey().getGroup());
            } else {
                ps.setString(13, trigger.getKey().getName());
                ps.setString(14, trigger.getKey().getGroup());
            }

            int insertResult = ps.executeUpdate();

            if (tDel == null) {
                updateBlobTrigger(conn, trigger);
            } else {
                tDel.updateExtendedTriggerProperties(conn, trigger, state, jobDetail);
            }

            return insertResult;
        }
    }

    @Override
    public void initialize(Logger logger, String tablePrefix, String schedName, String instanceId, ClassLoadHelper classLoadHelper, boolean useProperties, String initString) throws NoSuchDelegateException {
        this.logger = logger;
        this.tablePrefix = tablePrefix;
        this.schedName = schedName;
        this.instanceId = instanceId;
        this.useProperties = useProperties;
        this.classLoadHelper = classLoadHelper;
        addDefaultTriggerPersistenceDelegates();

        if (initString == null) {
            return;
        }

        String[] settings = initString.split("\\|");

        for (String setting : settings) {
            String[] parts = setting.split("=");
            String name = parts[0];
            if (parts.length == 1 || parts[1] == null || parts[1].equals("")) {
                continue;
            }

            if (name.equals("triggerPersistenceDelegateClasses")) {
                String[] trigDelegates = parts[1].split(",");

                for (String trigDelClassName : trigDelegates) {
                    try {
                        Class<?> trigDelClass = classLoadHelper.loadClass(trigDelClassName);
                        addTriggerPersistenceDelegate((TriggerPersistenceDelegate) trigDelClass.newInstance());
                    } catch (Exception e) {
                        throw new NoSuchDelegateException("Error instantiating TriggerPersistenceDelegate of type: " + trigDelClassName, e);
                    }
                }
            } else {
                throw new NoSuchDelegateException("Unknown setting: '" + name + "'");
            }
        }
    }
}