package com.dotmarketing.fixtask.tasks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.repackage.com.thoughtworks.xstream.XStream;
import com.dotcms.repackage.com.thoughtworks.xstream.io.xml.DomDriver;
import com.dotmarketing.beans.FixAudit;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactory;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;

/**
 * This task will locate the Content Type fields that are pointing to Content
 * Types that no longer exist in the database and removes them. Specifically,
 * the {@code default_file_type} column of the {@code field} table references an
 * Inode that does not exist neither in the {@code inode} column of the
 * {@code structure} table nor in the {@code inode} column of the {@code inode}
 * table.
 * <p>
 * Not running this fix task might cause problems when solving conflicts via the
 * Integrity Checker. For example, Server A has a Content Type whose inode is
 * "1", and Server B has a field that is referencing the inode "1" as well,
 * <b>even though the Content Type "1" DOES NOT exist in server B</b>. This
 * field is actually an orphan record that is not supposed to exist.
 * <p>
 * Therefore, when solving integrity conflicts in Server B, a duplicate record
 * will be inserted and a SQL exception will be thrown.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since Aug 28, 2015
 *
 */
public class FixTask00080DeleteOrphanedContentTypeFields implements FixTask {

	private List<Map<String, String>> modifiedData = new ArrayList<Map<String, String>>();

	/** Lookup invalid inodes in Field table referencing the Structure table */
	private static final String VERIFICATION_QUERY = "SELECT inode FROM field WHERE structure_inode NOT IN (SELECT inode FROM structure)";

	@Override
	public List<Map<String, Object>> executeFix() throws DotDataException,
			DotRuntimeException {
		Logger.info(FixTask00080DeleteOrphanedContentTypeFields.class,
				"Beginning DeleteOrphanedContentTypeFields");
		List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>();

		if (!FixAssetsProcessStatus.getRunning()) {
			HibernateUtil.startTransaction();
			int total = 0;
			try {
				FixAssetsProcessStatus.startProgress();
				FixAssetsProcessStatus
						.setDescription("task 80: DeleteOrphanedContentTypeFields");
				DotConnect dc = new DotConnect();
				dc.setSQL(VERIFICATION_QUERY);
				modifiedData = dc.loadResults();
				total = modifiedData != null ? modifiedData.size() : 0;
				FixAssetsProcessStatus.setTotal(total);
				getModifiedData();
				if (total > 0) {
					try {
						HibernateUtil.startTransaction();
						MaintenanceUtil.deleteOrphanContentTypeFields();
						HibernateUtil.commitTransaction();
						// Set the number of records that were fixed
						FixAssetsProcessStatus.setErrorsFixed(modifiedData.size());
					} catch (Exception e) {
						Logger.error(
								this,
								"Unable to clean orphaned content type fields.",
								e);
						HibernateUtil.rollbackTransaction();
						modifiedData.clear();
					}
					FixAudit Audit = new FixAudit();
					Audit.setTableName("field");
					Audit.setDatetime(new Date());
					Audit.setRecordsAltered(total);
					Audit.setAction("task 80: DeleteOrphanedContentTypeFields");
					HibernateUtil.save(Audit);
					HibernateUtil.commitTransaction();
					MaintenanceUtil.flushCache();
					returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
					Logger.debug(
							FixTask00080DeleteOrphanedContentTypeFields.class,
							"Ending DeleteOrphanedContentTypeFields");
				}
			} catch (Exception e) {
				Logger.debug(
						FixTask00080DeleteOrphanedContentTypeFields.class,
						"There was a problem during DeleteOrphanedContentTypeFields",
						e);
				HibernateUtil.rollbackTransaction();
				FixAssetsProcessStatus.setActual(-1);
			} finally {
				FixAssetsProcessStatus.stopProgress();
			}
		}
		return returnValue;
	}

	@Override
	public List<Map<String, String>> getModifiedData() {
		if (modifiedData.size() > 0) {
			XStream _xstream = new XStream(new DomDriver());
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
			String lastmoddate = sdf.format(date);
			File _writing = null;
			if (!new File(ConfigUtils.getBackupPath() + File.separator
					+ "fixes").exists()) {
				new File(ConfigUtils.getBackupPath() + File.separator + "fixes")
						.mkdirs();
			}
			_writing = new File(ConfigUtils.getBackupPath() + File.separator
					+ "fixes" + java.io.File.separator + lastmoddate + "_"
					+ "FixTask00080DeleteOrphanedContentTypeFields" + ".xml");

			BufferedOutputStream _bout = null;
			try {
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
			} catch (FileNotFoundException e) {
				Logger.error(this, "Could not write to Fix Task status file.");
			}
			_xstream.toXML(modifiedData, _bout);
		}
		return modifiedData;
	}

	@Override
	public boolean shouldRun() {
		Logger.debug(CMSMaintenanceFactory.class, "Running query for fields: "
				+ VERIFICATION_QUERY);
		DotConnect dc = new DotConnect();
		dc.setSQL(VERIFICATION_QUERY);
		List<Map<String, Object>> inodesInStructure = null;
		try {
			inodesInStructure = dc.loadObjectResults();
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
		}
		Logger.debug(CMSMaintenanceFactory.class,
				"Found " + inodesInStructure.size() + " invalid inodes.");
		int total = inodesInStructure.size();
		FixAssetsProcessStatus.setTotal(total);
		return total > 0 ? true : false;
	}

}
