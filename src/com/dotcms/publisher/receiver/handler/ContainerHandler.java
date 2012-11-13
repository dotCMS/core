package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;

import com.dotcms.publisher.myTest.bundler.ContainerBundler;
import com.dotcms.publisher.myTest.wrapper.ContainerWrapper;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class ContainerHandler implements IHandler {
	private IdentifierAPI iAPI = APILocator.getIdentifierAPI();
	private UserAPI uAPI = APILocator.getUserAPI();
	private ContainerAPI cAPI = APILocator.getContainerAPI();

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
	        	
	        	if(!UtilMethods.isSet(iAPI.find(container))) {
	        		Identifier id = iAPI.find(container.getIdentifier());
	        		Host localHost = APILocator.getHostAPI().find(containerId.getHostId(), systemUser, false);
        			if(id ==null || !UtilMethods.isSet(id.getId())){
        				Identifier containerIdNew = null;
        				
        				containerIdNew = iAPI.createNew(container, 
        						localHost, 
        						containerId.getId());
	            			
        				
        				container.setIdentifier(containerIdNew.getId());
            		}
        			
        			cAPI.save(container, 
        					StructureCache.getStructureByInode(container.getStructureInode()),
        					localHost, systemUser, false);
	        	}        			
	        }
        	
    	}
    	catch(Exception e){
    		throw new DotPublishingException(e.getMessage(),e);
    	}    	
    }
}
