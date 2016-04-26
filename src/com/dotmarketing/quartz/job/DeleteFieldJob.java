package com.dotmarketing.quartz.job;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.services.ContentletMapServices;
import com.dotmarketing.services.ContentletServices;
import com.dotmarketing.services.StructureServices;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.quartz.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

public class DeleteFieldJob implements Job {

	public static void triggerJobImmediately (Structure structure, Field field, User user) {
        Preconditions.checkNotNull(structure, "Structure can't be null");
        Preconditions.checkArgument(Strings.isNullOrEmpty(structure.getInode()), "Structure Id can't be null or empty");
        Preconditions.checkNotNull(field, "Field can't be null");
        Preconditions.checkNotNull(user, "User can't be null");

		JobDataMap dataMap = new JobDataMap();
		dataMap.put("structure", structure);
		dataMap.put("field", field);
		dataMap.put("user", user);

        String randomID = UUID.randomUUID().toString();

		JobDetail jd = new JobDetail("DeleteFieldJob-" + randomID, "delete_field_jobs", DeleteFieldJob.class);
		jd.setJobDataMap(dataMap);
		jd.setDurability(false);
		jd.setVolatility(false);
		jd.setRequestsRecovery(true);

		long startTime = System.currentTimeMillis();
		SimpleTrigger trigger = new SimpleTrigger("deleteFieldTrigger-"+randomID, "delete_field_triggers",
                new Date(startTime));

		try {
			Scheduler sched = QuartzUtils.getSequentialScheduler();
			sched.scheduleJob(jd, trigger);
		} catch (SchedulerException e) {
			Logger.error(DeleteFieldJob.class, "Error scheduling DeleteFieldJob", e);
			throw new DotRuntimeException("Error scheduling DeleteFieldJob", e);
		}

		AdminLogger.log(DeleteFieldJob.class, "triggerJobImmediately",
                String.format("Deleting Field '%s' for Structure with id: %s",
                        field.getVelocityVarName(), structure.getInode()));

	}

	public void execute(JobExecutionContext jobContext) throws JobExecutionException {
		JobDataMap map = jobContext.getJobDetail().getJobDataMap();
		Structure structure = (Structure) map.get("structure");
		Field field = (Field) map.get("field");
		User user = (User) map.get("user");

        Preconditions.checkNotNull(structure, "Structure can't be null");
        Preconditions.checkArgument(Strings.isNullOrEmpty(structure.getInode()), "Structure Id can't be null or empty");
        Preconditions.checkNotNull(field, "Field can't be null");
        Preconditions.checkNotNull(user, "User can't be null");

        try {
            String type = field.getFieldType();

            HibernateUtil.startTransaction();

            if (!APILocator.getFieldAPI().isElementConstant(field)
                    && !Field.FieldType.LINE_DIVIDER.toString().equals(type)
                    && !Field.FieldType.TAB_DIVIDER.toString().equals(type)
                    && !Field.FieldType.RELATIONSHIPS_TAB.toString().equals(type)
                    && !Field.FieldType.CATEGORIES_TAB.toString().equals(type)
                    && !Field.FieldType.PERMISSIONS_TAB.toString().equals(type)
                    && !Field.FieldType.HOST_OR_FOLDER.toString().equals(type)) {

                clearField(structure.getInode(), field);
            }
            FieldFactory.deleteField(field);
            // Call the commit method to avoid a deadlock
            HibernateUtil.commitTransaction();

            APILocator.getNotificationAPI().info(
                    String.format("Field '%s' was deleted succesfully. Field Inode: %s, Structure Inode: %s",
                            field.getVelocityVarName(), field.getInode(), structure.getInode()), user.getUserId());

            ActivityLogger.logInfo(ActivityLogger.class, "Delete Field Action", "User " + user.getUserId() + "/"
                    + user.getFirstName() + " deleted field " + field.getFieldName() + " to " + structure.getName()
                    + " Structure.");

            FieldsCache.removeFields(structure);

            CacheLocator.getContentTypeCache().remove(structure);
            StructureServices.removeStructureFile(structure);

            //Refreshing permissions
            PermissionAPI perAPI = APILocator.getPermissionAPI();
            if(field.getFieldType().equals("host or folder")) {
                APILocator.getContentletAPI().cleanHostField(structure, APILocator.getUserAPI().getSystemUser(), false);
                perAPI.resetChildrenPermissionReferences(structure);
            }
            StructureFactory.saveStructure(structure);
            // rebuild contentlets indexes
            APILocator.getContentletAPI().reindex(structure);
            // remove the file from the cache
            ContentletServices.removeContentletFile(structure);
            ContentletMapServices.removeContentletMapFile(structure);
        } catch (Exception e) {
            Logger.error(this, String.format("Unable to delete field '%s'. Field Inode: %s, Structure Inode: %s",
                    field.getVelocityVarName(), field.getInode(), structure.getInode()), e);
            APILocator.getNotificationAPI().error(
                    String.format("Unable to delete field '%s'. Field Inode: %s, Structure Inode: %s",
                    field.getVelocityVarName(), field.getInode(), structure.getInode()), user.getUserId());
        }

	}

    private void clearField(String structureInode, Field field) throws DotDataException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Connection conn = DbConnectionFactory.getConnection();

        try {
            Queries queries = getQueries(field);
            ps = conn.prepareStatement(queries.getSelect());
            ps.setObject(1, structureInode);
            rs = ps.executeQuery();
            final int BATCH_SIZE = 200;
            PreparedStatement  ps2 = conn.prepareCall(queries.getUpdate());

            for(int i=1; rs.next(); i++) {
                String contentInode = rs.getString("inode");
                ps2.setObject(1, contentInode);
                ps2.addBatch();

                if(i % BATCH_SIZE == 0) {
                    ps2.executeBatch();
                }
            }

            conn.commit();

        } catch(SQLException e) {
            throw new DotDataException(String.format("Error Clearing Field '%s' for Structure with id: %s",
                    field.getVelocityVarName(), structureInode), e);

        } finally {
            try { if (conn != null) conn.close(); } catch (Exception e) { Logger.error(this, "Error closing connection", e); }
            try { if (rs != null) rs.close(); } catch (Exception e) {  Logger.error(this, "Error closing result set", e); }
            try { if (ps != null) ps.close(); } catch (Exception e) { Logger.error(this, "Error closing statement", e);  }
        }
    }

    private Queries getQueries(Field field) {

        StringBuilder select = new StringBuilder("SELECT inode FROM contentlet " );
        StringBuilder update = new StringBuilder("UPDATE contentlet SET " );
        StringBuilder whereField = new StringBuilder();

        if(field.getFieldContentlet().contains("float")){
            if ( DbConnectionFactory.isMySql() ) {
                select.append(field.getFieldContentlet()).append(" = ");
                whereField.append(field.getFieldContentlet()).append(" IS NOT NULL AND ").append(field.getFieldContentlet())
                        .append(" != ");
            } else {
                select.append("\"").append(field.getFieldContentlet()).append("\"").append(" = ");
                whereField.append("\"").append(field.getFieldContentlet()).append("\" IS NOT NULL AND \"")
                        .append(field.getFieldContentlet()).append("\" != ");
            }
        }else{
            update.append(field.getFieldContentlet()).append(" = ");
            whereField.append(field.getFieldContentlet()).append(" IS NOT NULL AND ").append(field.getFieldContentlet())
                    .append(" != ");
        }

        if(field.getFieldContentlet().contains("bool")){
            update.append(DbConnectionFactory.getDBFalse());
            whereField.append(DbConnectionFactory.getDBFalse());
        }else if(field.getFieldContentlet().contains("date")){
            update.append(DbConnectionFactory.getDBDateTimeFunction());
            whereField.append(DbConnectionFactory.getDBDateTimeFunction());
        }else if(field.getFieldContentlet().contains("float")){
            update.append(0.0);
            whereField.append(0.0);
        }else if(field.getFieldContentlet().contains("integer")){
            update.append(0);
            whereField.append(0);
        }else{
            update.append("''");
            whereField.append("''");
        }

        select.append(" WHERE structure_inode = ?").append(" AND (").append(whereField).append(")");
        update.append(" WHERE inode = ?");

        return new Queries().setSelect(select.toString()).setUpdate(update.toString());

    }

    private final class Queries {
        private String select;
        private String update;

        private Queries setSelect(String select) {
            this.select = select;
            return this;
        }

        private Queries setUpdate(String update) {
            this.update = update;
            return this;
        }

        public String getSelect() {
            return select;
        }

        public String getUpdate() {
            return update;
        }
    }

}
