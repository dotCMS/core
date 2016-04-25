package com.dotmarketing.quartz.job;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import org.quartz.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

public class CleanFieldJob implements Job {

	public static void triggerJobImmediately (String structureInode, Field field) {
		String randomID = UUID.randomUUID().toString();
		JobDataMap dataMap = new JobDataMap();

		dataMap.put("structureInode", structureInode);
		dataMap.put("field", field);

		JobDetail jd = new JobDetail("CleanFieldJob-" + randomID, "clean_field_jobs", CleanFieldJob.class);
		jd.setJobDataMap(dataMap);
		jd.setDurability(false);
		jd.setVolatility(false);
		jd.setRequestsRecovery(true);

		long startTime = System.currentTimeMillis();
		SimpleTrigger trigger = new SimpleTrigger("cleanFieldTrigger-"+randomID, "clean_field_triggers",  new Date(startTime));

		try {
			Scheduler sched = QuartzUtils.getSequentialScheduler();
			sched.scheduleJob(jd, trigger);
		} catch (SchedulerException e) {
			Logger.error(CleanFieldJob.class, "Error scheduling CleanFieldJob", e);
			throw new DotRuntimeException("Error scheduling CleanFieldJob", e);
		}
		AdminLogger.log(CleanFieldJob.class, "triggerJobImmediately", String.format("Clearing Field '%s' for Structure with id: %s", field.getVelocityVarName(), structureInode));

	}

	public void execute(JobExecutionContext jobContext) throws JobExecutionException {
		
		JobDataMap map = jobContext.getJobDetail().getJobDataMap();

		String structureInode = (String) map.get("structureInode");
		Field field = (Field) map.get("field");

        Preconditions.checkArgument(Strings.isNullOrEmpty(structureInode), "Bundle Id can't be null or empty");
        Preconditions.checkNotNull(field, "field can't be null");

        PreparedStatement statement = null;
        ResultSet rs = null;
        Connection conn = DbConnectionFactory.getConnection();

        try {

            statement = conn.prepareStatement(getSelectQuery(field));

            rs = statement.executeQuery();

            PreparedStatement s2 = conn.prepareCall("UPDATE contentlet SET " + field.getFieldContentlet() + ");

            while(rs.next()) {

                s2.setObject(1, null);
                s2.setObject(2, asset.getAssetId());
                s2.setObject(3, environmentId);
                s2.addBatch();

            }




            for (HistoricalPushedAsset asset : assets) {

            }

            statement.executeBatch();

            // it all good let's remove the entries from cache
            for (HistoricalPushedAsset asset : assets) {
                cache.removePushedItemById(asset.getAssetId(), environmentId);
            }

        } catch(SQLException e) {
            throw new DotDataException(String.format("Error reseting push date for assets in bundle %s and environment &s", bundleId, environmentId), e);
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception e) { Logger.error(this, "Error closing connection", e); }
            try { if (rs != null) rs.close(); } catch (Exception e) {  Logger.error(this, "Error closing result set", e); }
            try { if (statement != null) statement.close(); } catch (Exception e) { Logger.error(this, "Error closing statement", e);  }
        }
	}

    private String getSelectQuery(Field field) {

        StringBuilder sql = new StringBuilder("update contentlet set " );
        StringBuilder whereField = new StringBuilder();

        if(field.getFieldContentlet().contains("float")){
            if ( DbConnectionFactory.isMySql() ) {
                sql.append(field.getFieldContentlet()).append(" = ");
                whereField.append(field.getFieldContentlet()).append(" IS NOT NULL AND ")
                        .append(field.getFieldContentlet()).append(" != ");
            } else {
                sql.append("\"").append(field.getFieldContentlet()).append("\"").append(" = ");
                whereField.append("\"").append(field.getFieldContentlet())
                        .append("\" IS NOT NULL AND \"").append(field.getFieldContentlet()).append("\" != ");
            }
        }else{
            sql.append(field.getFieldContentlet()).append(" = ");
            whereField.append(field.getFieldContentlet()).append(" IS NOT NULL AND ").append(field.getFieldContentlet()).append(" != ");
        }
        if(field.getFieldContentlet().contains("bool")){
            sql.append(DbConnectionFactory.getDBFalse());
            whereField.append(DbConnectionFactory.getDBFalse());
        }else if(field.getFieldContentlet().contains("date")){
            sql.append(DbConnectionFactory.getDBDateTimeFunction());
            whereField.append(DbConnectionFactory.getDBDateTimeFunction());
        }else if(field.getFieldContentlet().contains("float")){
            sql.append(0.0);
            whereField.append(0.0);
        }else if(field.getFieldContentlet().contains("integer")){
            sql.append(0);
            whereField.append(0);
        }else{
            sql.append("''");
            whereField.append("''");
        }

        sql.append(" where structure_inode = ?").append(" AND (").append(whereField).append(")");

        return sql.toString();

    }

}
