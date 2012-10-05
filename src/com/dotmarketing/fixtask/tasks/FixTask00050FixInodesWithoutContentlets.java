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
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Fix database inodes of type contentlet without a contentlet in the content table
 * @author Oswaldo
 *
 */

public class FixTask00050FixInodesWithoutContentlets implements FixTask {

	private List <Map<String, String>> modifiedData= new  ArrayList <Map<String, String>>();
	/* Queries */
	private static final String selectInodeContentletWithoutContent = "select inode from inode where type = 'contentlet' and inode not in (select inode from contentlet)";
	private static final String selectIdentifierContentletWithoutContent = "select id from identifier where asset_type = 'contentlet' and id not in (select identifier from contentlet)";
	//private static final String cleanInodeContentletWithoutContentTree="delete from tree where parent=? or child=?";
	private static final String cleanInodeContentletWithoutContentPermission="delete from permission where inode_id=?";
	private static final String cleanInodeContentletWithoutContentPermissionReference="delete from permission_reference where asset_id=?";
	private static final String cleanInodeContentletWithoutContent = "delete from inode where type = 'contentlet' and inode = ?";
	private static final String cleanIdentifierInodeContentletWithoutContent = "delete from identifier where id = ?";
	//private static final String cleanInodeIdentifierContentletWithoutContent = "delete from inode where type = 'identifier' and inode = ?";

	@SuppressWarnings({ "unchecked", "deprecation" })
	public List<Map<String, Object>> executeFix() throws DotDataException,
	DotRuntimeException {
		List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>();
		Logger.info(FixTask00050FixInodesWithoutContentlets.class,"Beginning FixInodesWithoutContentlets");

		if (!FixAssetsProcessStatus.getRunning()) {
			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("task 50: FixInodesWithoutContentlets");
			HibernateUtil.startTransaction();
			int counter=0;
			try {
				DotConnect dc = new DotConnect();
				dc.setSQL(selectInodeContentletWithoutContent);
				List<Map<String, String>> inodes = dc.getResults();
				dc.setSQL(selectIdentifierContentletWithoutContent);
				List<Map<String, String>> identifiers = dc.getResults();	
				List<Map<String, String>> result;
				String inodeS="";
				String identifierS="";	


				for (Map<String, String> inode: inodes) {
					inodeS = inode.get("inode");
					//identifierS = inode.get("identifier");
					try {							
						/*dc = new DotConnect();
						dc.setSQL(cleanInodeContentletWithoutContentTree);
						dc.addParam(identifierS);
						dc.addParam(inodeS);
						result = dc.getResults();*/
						
						dc = new DotConnect();
						dc.setSQL(cleanInodeContentletWithoutContent);
						dc.addParam(inodeS);
						result = dc.getResults();

						/*dc = new DotConnect();
						dc.setSQL(cleanInodeIdentifierContentletWithoutContent);
						dc.addParam(identifierS);
						result = dc.getResults();*/

						counter=counter++;
					} catch (Exception e) {
						Logger.error(FixTask00050FixInodesWithoutContentlets.class,e.getMessage(),e);
					}					
				}	
				for (Map<String, String> ident: identifiers) {
					identifierS = ident.get("id");
					try{
						dc = new DotConnect();
						dc.setSQL(cleanInodeContentletWithoutContentPermissionReference);
						dc.addParam(identifierS);
						result = dc.getResults();
					
						dc = new DotConnect();
						dc.setSQL(cleanInodeContentletWithoutContentPermission);
						dc.addParam(identifierS);
						result = dc.getResults();
					
						dc = new DotConnect();
						dc.setSQL(cleanIdentifierInodeContentletWithoutContent);
						dc.addParam(identifierS);
						result = dc.getResults();
					} catch (Exception e) {
						Logger.error(FixTask00050FixInodesWithoutContentlets.class,e.getMessage(),e);
					}		
					
				}

				FixAssetsProcessStatus.setTotal(counter);

				//getModifiedData();
				FixAudit Audit = new FixAudit();
				Audit.setTableName("inode");
				Audit.setDatetime(new Date());
				Audit.setRecordsAltered(counter);
				Audit.setAction("task 50: Fixed FixInodesWithoutContentlets");
				HibernateUtil.save(Audit);
				HibernateUtil.commitTransaction();
				MaintenanceUtil.flushCache();

				returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
				FixAssetsProcessStatus.stopProgress();
				Logger.debug(FixTask00050FixInodesWithoutContentlets.class, "Ending FixInodesWithoutContentlets");
			} catch (Exception e1) {
				Logger.debug(FixTask00050FixInodesWithoutContentlets.class,"There was a problem during FixInodesWithoutContentlets", e1);
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
				new File(ConfigUtils.getBackupPath()+File.separator+"fixes").mkdir();
			}
			_writing = new File(ConfigUtils.getBackupPath()+File.separator+"fixes" + java.io.File.separator  + lastmoddate + "_"
					+ "FixTask00060FixInodesWithoutContentlets" + ".xml");

			BufferedOutputStream _bout = null;
			try {
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
			} catch (FileNotFoundException e) {

			}
			_xstream.toXML(modifiedData, _bout);
		}
		return modifiedData;
	}

	/**
	 * Validated if exist inodes without content
	 */
	public boolean shouldRun() {
		int total = 0;
		DotConnect dc = new DotConnect();
		dc.setSQL(selectInodeContentletWithoutContent);
		List<HashMap<String, String>> result =null ;
		try {
			result = dc.getResults();
		} catch (DotDataException e) {
			Logger.error(this,e.getMessage(), e);
		}
		total = total + result.size();

		if (total > 0)
			return true;
		else
			return false;
	}	

}