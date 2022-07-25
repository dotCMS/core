package com.dotmarketing.fixtask.tasks;


import com.dotmarketing.beans.FixAudit;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
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
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.dotcms.repackage.net.sf.hibernate.HibernateException;


public class FixTask00001CheckAssetsMissingIdentifiers  implements FixTask {

	private List <Map<String, String>> modifiedData= new  ArrayList <Map<String, String>>();
	
	
	public List <Map <String,Object>> executeFix() throws DotDataException, DotRuntimeException {

		Logger.info(CMSMaintenanceFactory.class,
				"Beginning fixAssetsInconsistencies");
		int total = 0;
    	List <Map <String,Object>> returnValue= new ArrayList <Map <String,Object>> ();


		
		if (!FixAssetsProcessStatus.getRunning()) {
			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("task 1: Deleting all assets with no identifier");
			HibernateUtil.startTransaction();
			Logger.info(CMSMaintenanceFactory.class,
					"Deleting all assets with no identifier");
			DotConnect db = new DotConnect();

			String tableNameOfAsset[] = { "contentlet", Inode.Type.CONTAINERS.getTableName(), "links", "template" };

			for (String asset : tableNameOfAsset) {

				final String countSQL = "select count(*) as count from "
						+ asset + " t";
				final String selectTreeIdentsSQl = "select at.inode as inode, i.inode as ident from inode ie, identifier i, "
						+ asset
						+ " at, tree t "
						+ "where at.inode = t.child and i.inode = t.parent and at.inode = ie.inode and ie.identifier is null";
				final String updateIdentsSQL = "update inode set identifier = ? where inode = ?";
				final String selectNullIdentsSQL = "select i.inode as inode from inode i, "
						+ asset
						+ " at where at.inode = i.inode and at.identifier is null";

				
				DotConnect dc = new DotConnect();
				dc.setSQL(countSQL);
				List<HashMap<String, String>> result = dc.getResults();
				
				int before = Integer.parseInt(result.get(0).get("count"));
				/*dc.setSQL(selectTreeIdentsSQl);
				List<HashMap<String, String>> results = dc.getResults();
				modifiedData.addAll(results);
				total =total + dc.getResults().size();
				for (HashMap<String, String> r : results) {
					dc.setSQL(updateIdentsSQL);
					dc.addParam(r.get("ident"));
					dc.addParam(r.get("inode"));
					dc.getResult();
					FixAssetsProcessStatus.addAError();
				}*/
				dc.setSQL(selectNullIdentsSQL);
				List<HashMap<String, String>> results = dc.getResults();
				modifiedData.addAll(results);
				getModifiedData();
				total =total + dc.getResults().size();
				FixAssetsProcessStatus.setTotal(total);
				List<String> inodesToClean = new ArrayList<String>();
				boolean runDelete = false;
				for (HashMap<String, String> r : results) {
					inodesToClean.add(r.get("inode"));
					FixAssetsProcessStatus.addAErrorFixed();
					runDelete = true;
				}
				if (runDelete) {
					
					MaintenanceUtil.deleteAssets(inodesToClean, asset,1000);
				}
				dc.setSQL(countSQL);
				result = dc.getResults();
				int after = Integer.parseInt(result.get(0).get("count"));
				// return before - after;
			}
			Map map = new HashMap();
			//Including Identifier.class because it is not mapped with Hibernate anymore
			map.put(Identifier.class, null);
			try {
				map.putAll(HibernateUtil.getSession().getSessionFactory().getAllClassMetadata());
			} catch (HibernateException e) {
				throw new DotDataException(e.getMessage(),e);
			}


			Iterator it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();
				Class x = (Class) pairs.getKey();
				if (!x.equals(Inode.class)){
					Object o;
					try {
						o = x.newInstance();
					} catch (Exception e) {
						Logger.info(MaintenanceUtil.class, "Unable to instaniate object");
						Logger.debug(MaintenanceUtil.class,"Unable to instaniate object", e);
						continue;
					}
					if(o instanceof Inode){
						Inode i = (Inode)o;
						String type = i.getType();
						String tableName = ((com.dotcms.repackage.net.sf.hibernate.persister.AbstractEntityPersister)map.get(x)).getTableName();
						MaintenanceUtil.cleanInodeTableData(tableName, type);
						//FixAssetsProcessStatus.addAError();
					}
				}
			}
			it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();
				Class x = (Class) pairs.getKey();
				if (!x.equals(Inode.class)){
					Object o;
					try {
						o = x.newInstance();
					} catch (Exception e) {
						Logger.info(MaintenanceUtil.class,"Unable to instaniate object");
						Logger.debug(MaintenanceUtil.class,"Unable to instaniate object", e);
						continue;
					}
					if(o instanceof Inode){
						Inode i = (Inode)o;
						String type = i.getType();
						String tableName = ((com.dotcms.repackage.net.sf.hibernate.persister.AbstractEntityPersister)map.get(x)).getTableName();
						MaintenanceUtil.removeOphanedInodes(tableName, type);
						
					}
				}
			}
			
			FixAudit Audit = new FixAudit();
			Audit.setTableName("contentlet");
			Audit.setDatetime(new Date());
			Audit.setRecordsAltered(total);
			Audit.setAction("delete assets with missing identifiers");
			HibernateUtil.save(Audit);
			
				try {
					returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		
			FixAssetsProcessStatus.stopProgress();
			FixAssetsProcessStatus.setActual(-1);
            
		}
		return returnValue;
        
	}



	public List <Map<String, String>> getModifiedData() {

		if (modifiedData.size() > 0) {
			XStream _xstream = new XStream(new DomDriver());
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
			String lastmoddate = sdf.format(date);
			File _writing = null;

			if (!new File(ConfigUtils.getBackupPath()+File.separator+"fixes").exists()) {
				new File(ConfigUtils.getBackupPath()+File.separator+"fixes").mkdirs();
			}
			_writing = new File(ConfigUtils.getBackupPath()+File.separator+"fixes" + java.io.File.separator  + lastmoddate + "_"
					+ "FixTask00001CheckAssetsMissingIdentifiers" + ".xml");

			try (BufferedOutputStream _bout = new BufferedOutputStream(Files.newOutputStream(_writing.toPath()))){
				_xstream.toXML(modifiedData, _bout);
			} catch (IOException e) {
				Logger.error(this, "Error trying to get modified data from XML.", e);
			}
		}
		return modifiedData;

	}


	public boolean shouldRun() {

		String tableNameOfAsset[] = { "contentlet", Inode.Type.CONTAINERS.getTableName(), "links", "template" };
		int total = 0;
		DotConnect dc = new DotConnect();
		for (String asset : tableNameOfAsset) {

			final String selectNullIdentsSQL = "select i.inode as inode from inode i, "
					+ asset
					+ " at where at.inode = i.inode and at.identifier is null";

			dc.setSQL(selectNullIdentsSQL);
			List<HashMap<String, String>> result = null ; 
			try {
				result = dc.getResults();
			} catch (DotDataException e) {
				Logger.error(this,e.getMessage(), e);
			}

			total += result.size();
		}

		if (total > 0)
			return true;

		else
			return false;
	}

}
