package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.List;

import com.dotcms.publisher.myTest.bundler.StructureBundler;
import com.dotcms.publisher.myTest.wrapper.StructureWrapper;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class StructureHandler implements IHandler {
	private UserAPI uAPI = APILocator.getUserAPI();
	
	@Override
	public String getName() {
		return this.getClass().getName();
	}
	
	@Override
	public void handle(File bundleFolder) throws Exception {
		Collection<File> structures = FileUtil.listFilesRecursively(bundleFolder, new StructureBundler().getFileFilter());
		
        handleStructures(structures);
	}
	
	private void handleStructures(Collection<File> structures) throws DotPublishingException, DotDataException{
		User systemUser = uAPI.getSystemUser();
		
		try{
	        XStream xstream=new XStream(new DomDriver());
	        //Handle folders
	        for(File structureFile: structures) {
	        	if(structureFile.isDirectory()) continue;
	        	
	        	StructureWrapper structureWrapper = (StructureWrapper)  xstream.fromXML(new FileInputStream(structureFile));
	        	
	        	Structure structure = structureWrapper.getStructure();
	        	List<Field> fields = structureWrapper.getFields();
	        	
	        	//StructureFactory.saveStructure(structure);
	        	
	        }
        	
    	}
    	catch(Exception e){
    		throw new DotPublishingException(e.getMessage(),e);
    	}    	
    }
}
