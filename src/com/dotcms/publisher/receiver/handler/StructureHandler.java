package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.List;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotcms.publisher.pusher.bundler.StructureBundler;
import com.dotcms.publisher.pusher.wrapper.StructureWrapper;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class StructureHandler implements IHandler {

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
		try{
	        XStream xstream=new XStream(new DomDriver());
	        //Handle folders
	        for(File structureFile: structures) {
	        	if(structureFile.isDirectory()) continue;
	        	
	        	StructureWrapper structureWrapper = (StructureWrapper)  xstream.fromXML(new FileInputStream(structureFile));
	        	
	        	Structure structure = structureWrapper.getStructure();
	        	
	        	Structure localSt=StructureCache.getStructureByInode(structure.getInode());
	        	boolean localExists = localSt!=null && UtilMethods.isSet(localSt.getInode());
	        	
	        	if(structureWrapper.getOperation().equals(Operation.UNPUBLISH)) {
	        	    // delete operation
	        	    if(localExists)
	        	        APILocator.getStructureAPI().delete(localSt, APILocator.getUserAPI().getSystemUser());
	        	}
	        	else {
	        		// create/update the structure
	        	    
	        	    if(!localExists)
	        	        StructureFactory.saveStructure(structure, structure.getInode());
	        	    
	        	    List<Field> fields = structureWrapper.getFields();
	                
	                for (Field field : fields) {
	                        Field localField = FieldsCache.getField(field.getInode());
	                        if(localField == null || !UtilMethods.isSet(localField.getInode()))
	                            FieldFactory.saveField(field, field.getInode());
	                        else {
	                            FieldFactory.deleteField(localField);
	                            FieldFactory.saveField(field, field.getInode());
	                        }
	                }
	                FieldsCache.removeFields(structure);
	        	}	        	
	        }
        	
    	}
    	catch(Exception e){
    		throw new DotPublishingException(e.getMessage(),e);
    	}    	
    }
}
