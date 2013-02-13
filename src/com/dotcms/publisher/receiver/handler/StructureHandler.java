package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotcms.publisher.pusher.bundler.StructureBundler;
import com.dotcms.publisher.pusher.wrapper.StructureWrapper;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
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
	        	    
	        	    if(!localExists) {
	        	        StructureFactory.saveStructure(structure, structure.getInode());
	        	    }
	        	    else {
	        	        // lets update the attributes
	        	        BeanUtils.copyProperties(localSt, structure);
	        	        StructureFactory.saveStructure(localSt);
	        	    }
	        	    
	        	    // set the workflow scheme
	        	    try {
	        	        WorkflowScheme scheme = APILocator.getWorkflowAPI().findScheme(
	                            structureWrapper.getWorkflowSchemaId());
	        	        APILocator.getWorkflowAPI().saveSchemeForStruct(
	        	                localExists ? localSt : structure, scheme);
	        	    }
	        	    catch(Exception ex) {
	        	        // well we don't have that schema here. What a shame
	        	        Logger.warn(StructureHandler.class, 
	        	                "target schema id "+structureWrapper.getWorkflowSchemaId()+
	        	                " for structure "+structure.getName()+" doesn't exists");
	        	    }
	        	    
	        	    List<Field> fields = structureWrapper.getFields();
	                List<Field> localFields = FieldsCache.getFieldsByStructureInode(localSt.getInode());
	        	    
	                // for each field in the pushed structure lets create it if doesn't exists
	                // and update its properties if it do
	                HibernateUtil.getSession().clear();
	                for (Field field : fields) {
	                    Field localField = FieldsCache.getField(field.getInode());
	                    if(localField == null || !UtilMethods.isSet(localField.getInode()))
	                        FieldFactory.saveField(field, field.getInode());
	                    else {
	                        BeanUtils.copyProperties(localField, field);
	                        HibernateUtil.saveOrUpdate(localField);
	                    }
	                    localFields.remove(localField);
	                }
	                
	                if(localFields.size()>0) {
	                    // we have local fields that didn't came 
	                    // in the pushed structure. lets remove them
	                    for(Field ff : localFields) 
	                        FieldFactory.deleteField(ff);
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
