package com.dotmarketing.fixtask.tasks;

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
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;

/**
 * This task updates invalid asset-types values on identifier records by trying to match identifiers against
 * records from corresponding asset-types and "guessing" that their existence implies correspondence to given asset-type
 *
 * It applies to contentlets, containers, file assets, html pages, links, and templates.
 *
 * @author Anibal Gomez
 */
public class FixTask00015FixAssetTypesInIdentifiers implements FixTask {

	private HashMap<String, Integer> badDataCount = new HashMap<String, Integer>();
	private int total = 0;
	private String assetNames[] = { "contentlet", "containers", "file_asset", "htmlpage", "links", "template" };

	public List<Map<String, Object>> executeFix() throws DotDataException,
		DotRuntimeException {

		List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>();

		Logger.info(FixTask00015FixAssetTypesInIdentifiers.class, "Beginning FixAssetTypesInIdentifiers");

		if (!FixAssetsProcessStatus.getRunning()) {

			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("Task 15: Updating Invalid Identifier Asset-Types");
			FixAssetsProcessStatus.setTotal(total);

			try{
				HibernateUtil.startTransaction();
				DotConnect dc = new DotConnect();

				for (String asset : assetNames) {

					Inode.Type assetType = Inode.Type.valueOf(asset.toUpperCase());
					final String tableName = assetType.getTableName();

					Integer assetDataCount = badDataCount.get("identifier_"+asset);
					if (assetDataCount != null && assetDataCount.intValue() > 0) {

						//Update invalid entries from identifier
						final String identifiersToUpdate = "UPDATE identifier SET asset_type = '"+ asset +"' "+
							"WHERE asset_type <> '"+asset+"' AND EXISTS ("+
								"SELECT * FROM "+ tableName +" WHERE identifier.id = identifier"+
							")";

						Logger.debug(MaintenanceUtil.class,"Task 15: Updating from identifier type " + asset + " : " + identifiersToUpdate);

						dc.setSQL(identifiersToUpdate);

						dc.loadResult();
					}
				}

				FixAssetsProcessStatus.setErrorsFixed(total);

				FixAudit Audit = new FixAudit();
				Audit.setTableName("identifier");
				Audit.setDatetime(new Date());
				Audit.setRecordsAltered(total);
				Audit.setAction("task 15: Fixed FixAssetTypesInIdentifiers");
				HibernateUtil.save(Audit);
				HibernateUtil.commitTransaction();
				MaintenanceUtil.flushCache();

				returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
				FixAssetsProcessStatus.stopProgress();

				Logger.debug(FixTask00015FixAssetTypesInIdentifiers.class, "Ending FixAssetTypesInIdentifiers");

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
		return new ArrayList<Map<String,String>>();
	}

	public boolean shouldRun() {
		total=0;
		badDataCount.clear();

		DotConnect dc = new DotConnect();

		Logger.debug(MaintenanceUtil.class,"Task 15: Checking for invalid identifier asset-types");

		for (String asset : assetNames) {

			Inode.Type assetType = Inode.Type.valueOf(asset.toUpperCase());
			final String tableName = assetType.getTableName();

			//Check for invalid identifier asset-type entries needing to be updated
			final String indentifiersToDelete = "SELECT COUNT(*) AS count FROM identifier i WHERE EXISTS(" +
				"SELECT * FROM "+tableName+" WHERE i.id = identifier"+
			") AND asset_type <> '"+ asset +"'";

			try {
				dc.setSQL(indentifiersToDelete);
				Logger.debug(MaintenanceUtil.class, "Task 15: Checking for invalid identifier asset-types for" + asset + ": " + indentifiersToDelete);

				List<Map<String, String>> result = dc.loadResults();
				Logger.debug(MaintenanceUtil.class, "Task 15: Checking for invalid identifier asset-types for" + asset + ": " + result.get(0).get("count") + " entries");

				badDataCount.put("identifier_" + asset, Integer.valueOf(result.get(0).get("count")));
				total += Integer.parseInt(result.get(0).get("count"));
			} catch (DotDataException e) {
				Logger.error(this,e.getMessage(), e);
			}
		}

		if (total > 0) {
			Logger.info(MaintenanceUtil.class,"Task 15: " + total + " identifier entries to update" );
			return true;
		} else
			return false;
	}
}
