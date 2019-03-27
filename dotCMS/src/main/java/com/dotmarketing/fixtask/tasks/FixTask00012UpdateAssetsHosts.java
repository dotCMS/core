package com.dotmarketing.fixtask.tasks;

import com.dotmarketing.beans.FixAudit;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.InodeUtils;
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
import java.util.List;
import java.util.Map;

public class FixTask00012UpdateAssetsHosts implements FixTask {
	private HostAPI hostAPI = APILocator.getHostAPI();
	
	private final static String selectInodesSQL = "select id as inode from identifier where host_inode is null or host_inode = '' or host_inode = '0'";
	
	private final static String selectContentletInodeByIdentifierSQL = "select contentlet.inode from contentlet, inode contentlet_1_ where contentlet_1_.type = 'contentlet' and working = " + DbConnectionFactory.getDBTrue() + " and identifier = ? and contentlet.inode = contentlet_1_.inode";
	
	private final static String selectTemplateInodeByIdentifierSQL = "select template.inode from template, inode template_1_ where template.identifier = ? and template.inode = template_1_.inode and template.working = " + DbConnectionFactory.getDBTrue();
	
	private final static String selectContainerInodeByIdentifierSQL = "select " + Inode.Type.CONTAINERS.getTableName() + ".inode from " + Inode.Type.CONTAINERS.getTableName() + ", inode dot_containers_1_ where " + Inode.Type.CONTAINERS.getTableName() + ".inode = dot_containers_1_.inode and " + Inode.Type.CONTAINERS.getTableName() + ".identifier = ? and working = " + DbConnectionFactory.getDBTrue();
	
	private final static String selectLinkInodeByIdentifierSQL = "select links.inode from links, inode links_1_ where links.identifier = ? and links_1_.inode = links.inode and links.working = " + DbConnectionFactory.getDBTrue();

	private final static String getHostIdQuery = "select identifier.id as identifier " +
												 "from tree, " +
												 "     identifier, " +
												 "     contentlet, " +
												 "     structure, " +
												 "     inode " +
												 "where tree.child = ? and " +
												 "      tree.parent=identifier.id and " +
												 "      identifier.id=contentlet.identifier and " +
												 "      contentlet.working=true and " +
												 "      contentlet.structure_inode=structure.inode and " +
												 "      structure.velocity_var_name='Host' and " +
												 "      contentlet.inode=inode.inode";
	
	private final static String updateIdentifierHost = "update identifier set host_inode=? where id =?";
	
	private List<Map<String, String>> modifiedData = new ArrayList<Map<String, String>>();
	
	@SuppressWarnings({ "deprecation", "unchecked" })
	public List<Map<String, Object>> executeFix() throws DotDataException, DotRuntimeException {
		List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>();
		Logger.info(FixTask00012UpdateAssetsHosts.class,"Beginning UpdateAssetsHosts");
		
		if (!FixAssetsProcessStatus.getRunning()) {
			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("task 12: Update Assets Hosts");
			HibernateUtil.startTransaction();
			
			try {
				DotConnect dc = new DotConnect();
				dc.setSQL(selectInodesSQL);
				List<Map<String, String>> identifiers = dc.getResults();
				
				String inode;
				List<Map<String, String>> result;
				
				List<Map<String,String>> inodes;
				Map<String,String> host;
				
				HashMap <String,String> data;
				int counter = 0;
				
				Host systemHost = hostAPI.findSystemHost();
				for (Map<String, String> identifier: identifiers) {
					inode = "";
					
					try {
						dc = new DotConnect();
						dc.setSQL(selectContentletInodeByIdentifierSQL);
						dc.addParam(identifier.get("inode"));
						result = dc.getResults();
						if ((result != null) && (0 < result.size()) && (result.get(0) != null) && InodeUtils.isSet(result.get(0).get("inode")))
							inode = result.get(0).get("inode");
					} catch (Exception e) {
					}
					if (!InodeUtils.isSet(inode)) {
						try {
							dc = new DotConnect();
							dc.setSQL(selectTemplateInodeByIdentifierSQL);
							dc.addParam(identifier.get("inode"));
							result = dc.getResults();
							if ((result != null) && (0 < result.size()) && (result.get(0) != null) && InodeUtils.isSet(result.get(0).get("inode")))
								inode = result.get(0).get("inode");
						} catch (Exception e) {
						}
					}
					
					if (!InodeUtils.isSet(inode)) {
						try {
							dc = new DotConnect();
							dc.setSQL(selectContainerInodeByIdentifierSQL);
							dc.addParam(identifier.get("inode"));
							result = dc.getResults();
							if ((result != null) && (0 < result.size()) && (result.get(0) != null) && InodeUtils.isSet(result.get(0).get("inode")))
								inode = result.get(0).get("inode");
						} catch (Exception e) {
						}
					}
					
					if (!InodeUtils.isSet(inode)) {
						try {
							dc = new DotConnect();
							dc.setSQL(selectLinkInodeByIdentifierSQL);
							dc.addParam(identifier.get("inode"));
							result = dc.getResults();
							if ((result != null) && (0 < result.size()) && (result.get(0) != null) && InodeUtils.isSet(result.get(0).get("inode")))
								inode = result.get(0).get("inode");
						} catch (Exception e) {
						}
					}

					if (InodeUtils.isSet(inode)) {
						dc = new DotConnect();
						dc.setSQL(getHostIdQuery);
						dc.addParam(inode);
						inodes = dc.getResults();
						if ((inodes != null) && (0 < inodes.size())) {
							host = inodes.get(0);
							
							dc = new DotConnect();
							dc.setSQL(updateIdentifierHost);
							dc.addParam(host.get("identifier"));
							dc.addParam(identifier.get("inode"));
							dc.getResult();
						} else {
							dc = new DotConnect();
							dc.setSQL(updateIdentifierHost);
							dc.addParam(systemHost.getIdentifier());
							dc.addParam(identifier.get("inode"));
							dc.getResult();
						}
					} else {
						dc = new DotConnect();
						dc.setSQL(updateIdentifierHost);
						dc.addParam(systemHost.getIdentifier());
						dc.addParam(identifier.get("inode"));
						dc.getResult();
					}
					
					data = new HashMap<String, String>();
					data.put("" + (++counter), "identifier with inode=" + identifier.get("inode"));
					modifiedData.add(data);
				}
				
				FixAssetsProcessStatus.setTotal(counter);
				
				getModifiedData();
				FixAudit Audit = new FixAudit();
				Audit.setTableName("identifier");
				Audit.setDatetime(new Date());
				Audit.setRecordsAltered(counter);
				Audit.setAction("task 12: Update Assets Hosts");
				HibernateUtil.save(Audit);
				HibernateUtil.closeAndCommitTransaction();
				MaintenanceUtil.flushCache();
				MaintenanceUtil.deleteStaticFileStore();
				returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
				FixAssetsProcessStatus.stopProgress();
				Logger.debug(FixTask00012UpdateAssetsHosts.class, "Ending UpdateAssetsHosts");
			} catch (Exception e) {
				Logger.debug(FixTask00012UpdateAssetsHosts.class,"There was a problem updating assets host", e);
				Logger.warn(FixTask00012UpdateAssetsHosts.class,"There was a problem updating assets host", e);
				HibernateUtil.rollbackTransaction();
				FixAssetsProcessStatus.stopProgress();
				FixAssetsProcessStatus.setActual(-1);
			}
		}
		
		return returnValue;
	}
	
	public List<Map<String, String>> getModifiedData() {
		if (0 < modifiedData.size()) {
			XStream _xstream = new XStream(new DomDriver());
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
			String lastmoddate = sdf.format(date);
			java.io.File _writing = null;
			
			if (!new java.io.File(ConfigUtils.getBackupPath()+File.separator+"fixes").exists()) {
				new java.io.File(ConfigUtils.getBackupPath()+File.separator+"fixes").mkdirs();
			}
			
			_writing = new java.io.File(ConfigUtils.getBackupPath()+File.separator+"fixes");
			
			if (!_writing.exists()) {
				_writing.mkdirs();
			}
			
			_writing = new java.io.File(ConfigUtils.getBackupPath()+File.separator+"fixes" + java.io.File.separator + lastmoddate + "_" + "FixTask00012UpdateAssetsHosts" + ".xml");

            try (BufferedOutputStream _bout = new BufferedOutputStream(Files.newOutputStream(_writing.toPath()))){
                _xstream.toXML(modifiedData, _bout);
            } catch (IOException e) {
                Logger.error(this, "Error trying to get modified data from XML.", e);
            }
		}
		return modifiedData;
	}
	
	@SuppressWarnings({ "deprecation", "unchecked" })
	public boolean shouldRun() {
		DotConnect dc = new DotConnect();
		dc.setSQL(selectInodesSQL);
		List<HashMap<String, String>> results =null;
		try {
			results = dc.getResults();
		} catch (DotDataException e) {
			Logger.error(this,e.getMessage(), e);
		}
		
		if (0 < results.size())
			return true;
		else
			return false;
	}
}