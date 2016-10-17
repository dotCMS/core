package com.dotmarketing.fixtask.tasks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.FixAudit;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotcms.repackage.com.thoughtworks.xstream.XStream;
import com.dotcms.repackage.com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * This task removes records from identifier and tree when they do not have a mathing identifier in the
 * table matching its asset type.
 * It applies to contentlets, containers, file assets, html pages, links, and templates.
 *
 *   @author jasontesser
 *   @author jgambarios
 *   @author Jose Castro
 *   @author oarrietadotcms
 *
 *   @author gabbydotCMS
 *   @version 2.0
 *   @since June 18, 2016
 */
public class FixTask00020DeleteOrphanedIdentifiers implements FixTask{

	private List <Map<String, String>>  modifiedData = new  ArrayList <Map<String,String>>();
	private HashMap<String, Integer> badDataCount = new HashMap<String, Integer>();
	private int total = 0;
	private String assetNames[] = { "contentlet", "containers", "file_asset", "htmlpage", "links", "template" };



	/**
	 * This method executes the deletes from tree and identifier in case any inconsistencies are found
	 *
	 * @return Status of the excecution of the fixes
	 * @throws DotDataException
	 * @throws DotRuntimeException
	 */
	public List<Map<String, Object>> executeFix() throws DotDataException,
		DotRuntimeException {

		List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>();

		Logger.info(FixTask00020DeleteOrphanedIdentifiers.class,"Beginning DeleteOrphanedIdentifiers");

		if (!FixAssetsProcessStatus.getRunning()) {

			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("Task 20: Deleting Orphan Identifiers");
			FixAssetsProcessStatus.setTotal(total);

			try{
				HibernateUtil.startTransaction();
				DotConnect dc = new DotConnect();

				for (String asset : assetNames) {

					Inode.Type assetType = Inode.Type.valueOf(asset.toUpperCase());
					final String tableName = assetType.getTableName();

					if (badDataCount.get("tree_child_"+asset).intValue() > 0) {
						//Delete orphan tree entries where identifier is child
						final String deleteTreesToDelete_child = "delete from tree where exists (select * from identifier i where tree.child=i.id and i.asset_type='" +
							asset + "' and not exists (select * from " + tableName + " where i.id=identifier))";

						Logger.debug(MaintenanceUtil.class,"Task 20: Deleting from tree(child) type " + asset + " : " + deleteTreesToDelete_child);

						dc.setSQL(deleteTreesToDelete_child);

						dc.loadResult();
					}

					if (badDataCount.get("tree_parent_"+asset).intValue() > 0) {
						//Delete orphan tree entries where identifier is parent
						final String deleteTreesToDelete_parent = "delete from tree where exists (select * from identifier i where tree.parent=i.id and i.asset_type='" +
							asset + "' and not exists (select * from " + tableName + " where i.id=identifier))";

						Logger.debug(MaintenanceUtil.class,"Task 20: Deleting from tree(parent) type " + asset + " : " + deleteTreesToDelete_parent);

						dc.setSQL(deleteTreesToDelete_parent);

						dc.loadResult();
					}

					if (badDataCount.get("identifier_"+asset).intValue() > 0) {
						//Delete orphan entries from identifier
						final String indentifiersToDelete = "delete from identifier where (identifier.asset_type='" +
							asset + "' and not exists (select * from " + tableName + " where identifier.id=identifier))";

						Logger.debug(MaintenanceUtil.class,"Task 20: Deleting from identifier type " + asset + " : " + indentifiersToDelete);

						dc.setSQL(indentifiersToDelete);

						dc.loadResult();
					}
				}

				FixAssetsProcessStatus.setErrorsFixed(total);

				FixAudit Audit = new FixAudit();
				Audit.setTableName("identifier");
				Audit.setDatetime(new Date());
				Audit.setRecordsAltered(total);
				Audit.setAction("task 20: Fixed DeleteOrphanedIdentifiers");
				HibernateUtil.save(Audit);
				HibernateUtil.commitTransaction();
				MaintenanceUtil.flushCache();

				returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
				FixAssetsProcessStatus.stopProgress();
				Logger.debug(FixTask00020DeleteOrphanedIdentifiers.class,"Ending DeleteOrphanedIdentifiers");
			} catch (Exception e) {
				Logger.error(this,e.getMessage(), e);
				HibernateUtil.rollbackTransaction();
				FixAssetsProcessStatus.stopProgress();
				FixAssetsProcessStatus.setActual(-1);
			}
		}
		return returnValue;
	}

	public List<Map<String, String>> getModifiedData() {
		if (modifiedData.size() > 0) {
			XStream _xstream = new XStream(new DomDriver());
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
			String lastmoddate = sdf.format(date);
			File _writing = null;

			if (!new File(ConfigUtils.getBackupPath()+File.separator+"fixes").exists()) {
				new File(ConfigUtils.getBackupPath()+File.separator+"fixes").mkdirs();
			}
			_writing = new File(ConfigUtils.getBackupPath()+File.separator+"fixes" + java.io.File.separator + lastmoddate + "_"
				+ "FixTask00020DeleteOrphanedIdentifiers" + ".xml");

			BufferedOutputStream _bout = null;
			try {
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
			} catch (FileNotFoundException e) {

			}
			_xstream.toXML(modifiedData, _bout);
		}
		return modifiedData;
	}

	public boolean shouldRun() {

		total=0;
		badDataCount.clear();

		DotConnect dc = new DotConnect();
		List<Map<String, String>> result;

		Logger.debug(MaintenanceUtil.class,"Task 20: Checking for orphan entries");

		for (String asset : assetNames) {

			Inode.Type assetType = Inode.Type.valueOf(asset.toUpperCase());
			final String tableName = assetType.getTableName();

			//Check for orphan tree entries (child) needing to be deleted
			final String treesToDeleteChild = "select count(*) as count from tree t where exists (select * from identifier i where t.child=i.id and i.asset_type='" +
				asset + "' and not exists (select * from " + tableName + " where i.id=identifier))";

			//Check for orphan tree entries (parent) needing to be deleted
			final String treesToDeleteParent = "select count(*) as count from tree t where exists (select * from identifier i where t.parent=i.id and i.asset_type='" +
				asset + "' and not exists (select * from " + tableName + " where i.id=identifier))";

			//Check for orphan identifier entries needing to be deleted
			final String indentifiersToDelete = "select count(*) as count from identifier i where (i.asset_type='" +
				asset + "' and not exists (select * from " + tableName + " where i.id=identifier))";


			try {

				dc.setSQL(treesToDeleteChild);
				Logger.debug(MaintenanceUtil.class,"Task 20: Checking orphan tree entries (child) for " + asset + ": " + treesToDeleteChild);
				result = dc.loadResults();
				Logger.debug(MaintenanceUtil.class,"Task 20: Checking orphan tree entries (child) for " + asset + ": " + result.get(0).get("count") + " entries");
				badDataCount.put("tree_child_" + asset, Integer.valueOf(result.get(0).get("count")));
				total += Integer.parseInt(result.get(0).get("count"));


				dc.setSQL(treesToDeleteParent);
				Logger.debug(MaintenanceUtil.class,"Task 20: Checking orphan tree entries (parent) for " + asset + ": " + treesToDeleteParent);
				result = dc.loadResults();
				Logger.debug(MaintenanceUtil.class,"Task 20: Checking orphan tree entries (parent) for " + asset + ": " + result.get(0).get("count") + " entries");
				badDataCount.put("tree_parent_" + asset, Integer.valueOf(result.get(0).get("count")));
				total += Integer.parseInt(result.get(0).get("count"));

				dc.setSQL(indentifiersToDelete);
				Logger.debug(MaintenanceUtil.class,"Task 20: Checking orphan identifier entries for " + asset + ": " + indentifiersToDelete);
				result = dc.loadResults();
				Logger.debug(MaintenanceUtil.class,"Task 20: Checking orphan identifier entries for " + asset + ": " + result.get(0).get("count") + " entries");
				badDataCount.put("identifier_" + asset, Integer.valueOf(result.get(0).get("count")));
				total += Integer.parseInt(result.get(0).get("count"));

			} catch (DotDataException e) {
				Logger.error(this,e.getMessage(), e);
			}
		}

		if (total > 0) {
			Logger.info(MaintenanceUtil.class,"Task 20: " + total + " orphan entries to delete" );
			return true;
		} else
			return false;

	}

}
