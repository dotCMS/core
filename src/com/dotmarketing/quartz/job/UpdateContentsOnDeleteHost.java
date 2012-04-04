package com.dotmarketing.quartz.job;

import java.util.List;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.business.FolderFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class UpdateContentsOnDeleteHost extends DotStatefulJob {
	private UserAPI userAPI;
	private ContentletAPI contentAPI;
	private HostAPI hostAPI;

	public UpdateContentsOnDeleteHost() {
		userAPI = APILocator.getUserAPI();
		contentAPI = APILocator.getContentletAPI();
		hostAPI = APILocator.getHostAPI();
	}
	@Override
	public void run(JobExecutionContext jobContext) throws JobExecutionException {
		JobDataMap dataMap = jobContext.getJobDetail().getJobDataMap();
		Host host = (Host) dataMap.get("host");

		try {
			DotConnect dc = new DotConnect();
			dc.setSQL("SELECT contentlet.inode " +
					  "FROM identifier, " +
						   "inode, " +
						   "contentlet, " +
						   "contentlet_lang_version_info cl  " +
					  "WHERE identifier.host_inode='" + host.getIdentifier() + "' AND " +
					  		"identifier.id = contentlet.identifier AND " +
					  		"inode.inode=contentlet.inode AND " +
					  		"cl.working_inode= contentlet.inode ");
			List<Map<String, String>> inodes = dc.getResults();
			boolean hostIsRequided;
			User systemUser = userAPI.getSystemUser();
			Host defaultHost = hostAPI.findDefaultHost(systemUser, false);
			Host systemHost = hostAPI.findSystemHost(systemUser, false);
			Contentlet contentlet;
			List<Field> fields;
			Field field = null;
			for(Map<String, String> inode: inodes) {
				contentlet = contentAPI.checkout(inode.get("inode"), systemUser, false);
				fields = FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode());
				hostIsRequided = false;
				for (int i = 0; i < fields.size(); ++i) {
					field = fields.get(i);
					if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
						if (field.isRequired())
							hostIsRequided = true;
						break;
					}
				}

				if (hostIsRequided) {
					contentlet.setHost(defaultHost.getIdentifier());
					contentlet.setStringProperty(field.getVelocityVarName(), defaultHost.getIdentifier());
				} else {
					contentlet.setHost(systemHost.getIdentifier());
					contentlet.setStringProperty(field.getVelocityVarName(), systemHost.getIdentifier());
				}

				contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);

				contentAPI.checkin(contentlet, systemUser, false);
			}
		} catch (Exception e) {
			Logger.error(UpdateContentsOnDeleteHost.class, e.getMessage(), e);
			throw new JobExecutionException(e.getMessage(), e);
		}
	}
}