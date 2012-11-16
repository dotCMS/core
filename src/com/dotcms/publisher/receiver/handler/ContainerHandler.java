package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.dotcms.publisher.myTest.bundler.ContainerBundler;
import com.dotcms.publisher.myTest.wrapper.ContainerWrapper;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class ContainerHandler implements IHandler {
	private UserAPI uAPI = APILocator.getUserAPI();
	private ContainerAPI cAPI = APILocator.getContainerAPI();
	private List<String> infoToRemove = new ArrayList<String>();

	@Override
	public String getName() {
		return this.getClass().getName();
	}
	
	@Override
	public void handle(File bundleFolder) throws Exception {
		Collection<File> containers = FileUtil.listFilesRecursively(bundleFolder, new ContainerBundler().getFileFilter());
		
        handleContainers(containers);
	}
	
	private void handleContainers(Collection<File> containers) throws DotPublishingException, DotDataException{
		User systemUser = uAPI.getSystemUser();
		
		try{
	        XStream xstream=new XStream(new DomDriver());
	        //Handle folders
	        for(File containerFile: containers) {
	        	if(containerFile.isDirectory()) continue;
	        	
	        	ContainerWrapper containerWrapper = (ContainerWrapper)  xstream.fromXML(new FileInputStream(containerFile));
	        	
	        	Container container = containerWrapper.getContainer();
	        	Identifier containerId = containerWrapper.getContainerId();
	        	
        		Host localHost = APILocator.getHostAPI().find(containerId.getHostId(), systemUser, false);
        		
        		//Set defaul type
    			Structure defaultStr = StructureCache.getStructureByVelocityVarName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME);
    			container.setStructureInode(defaultStr.getInode());
        		
        		
    			cAPI.save(container, 
    					StructureCache.getStructureByInode(container.getStructureInode()),
    					localHost, systemUser, false);
	        }
	        for(File containerFile: containers) {
	        	if(containerFile.isDirectory()) continue;
	        	ContainerWrapper containerWrapper = (ContainerWrapper)  xstream.fromXML(new FileInputStream(containerFile));
	        	
	        	VersionInfo info = containerWrapper.getCvi();
                infoToRemove.add(info.getIdentifier());
                APILocator.getVersionableAPI().saveVersionInfo(info);
	        }
	        try{
	            for (String ident : infoToRemove) {
	                APILocator.getVersionableAPI().removeVersionInfoFromCache(ident);
	            }
	        }catch (Exception e) {
	            throw new DotPublishingException("Unable to remove from cache version info", e);
	        }
    	}
    	catch(Exception e){
    		throw new DotPublishingException(e.getMessage(),e);
    	}    	
    }
}
