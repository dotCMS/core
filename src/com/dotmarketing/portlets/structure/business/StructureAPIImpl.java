package com.dotmarketing.portlets.structure.business;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.services.StructureServices;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class StructureAPIImpl implements StructureAPI {
    
    @Override
    public void delete(Structure st, User user) throws DotSecurityException, DotDataException, DotStateException {        
        // check for write permissions
        PermissionAPI perAPI=APILocator.getPermissionAPI();
        if(!perAPI.doesUserHavePermission(st, PermissionAPI.PERMISSION_WRITE, user)) 
            throw new DotSecurityException("User doesn't have permission to delete the structure");
        
     // checking if there is containers using this structure
     		List<Container> containers=APILocator.getContainerAPI().findContainersForStructure(st.getInode());
     		Map<String, String> containersInUse = new HashMap<String, String>();
     		StringBuilder names=new StringBuilder();
     		for(Container c : containers) {
     			try {
     				String hostTitle = APILocator.getHostAPI().findParentHost(c, user, false).getTitle();
     				containersInUse.put(c.getIdentifier(), hostTitle + " : " + c.getTitle() + "</br>");
     			} catch (Exception e) {
     			}
      		}
     		for(String title : containersInUse.values()){
     			names.append(title).append("</br>");
     		}
     		if(UtilMethods.isSet(names.toString()))
     			throw new DotStateException("Structure " + st.getName() + 
                         " can't be deleted because the following containers are using it: " + names);
        
        // default structure can't be deleted
        if(st.isDefaultStructure())
            throw new DotStateException("Can't delete default structure");
        
        // deleting fields 
        for(Field field : FieldFactory.getFieldsByStructure(st.getInode()))
            FieldFactory.deleteField(field);
        
        // delete related contentlets
        int limit = 200;
        int offset = 0;
        ContentletAPI conAPI=APILocator.getContentletAPI();
        List<Contentlet> contentlets=null;
        do {
            contentlets = conAPI.findByStructure(st, user, false, limit, offset);
            conAPI.delete(contentlets, user, false);
        } while(contentlets.size()>0);

        //delete bad data contents
        deleteStructureContentlets(st.getInode());
        
        // delete Forms entry if it is a form structure
        if (st.getStructureType() == Structure.STRUCTURE_TYPE_FORM) {
            Structure sf = StructureCache.getStructureByVelocityVarName(
                    FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME);
            if (UtilMethods.isSet(sf) && UtilMethods.isSet(sf.getInode())) {
                Field field = st.getFieldVar(FormAPI.FORM_WIDGET_FORM_ID_FIELD_VELOCITY_VAR_NAME);
                conAPI.delete( conAPI.search(
                        "+structureInode:" + sf.getInode() + 
                        " +structureInode:" + st.getInode(), 0, 0,
                        "", user, false), user, false);
            }
        }
        
        // make sure folders don't refer to this structure as default fileasset structure
        if (st.getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET)
            StructureFactory.updateFolderFileAssetReferences(st);
        
        // delete relationships where the structure is child or parent
        List<Relationship> relationships = RelationshipFactory.getRelationshipsByParent(st);
        for (Relationship rel : relationships)
            RelationshipFactory.deleteRelationship(rel);
        relationships = RelationshipFactory.getRelationshipsByChild(st);
        for (Relationship rel : relationships)
            RelationshipFactory.deleteRelationship(rel);
        
        // remove structure permissions
        perAPI.removePermissions(st);

        HibernateUtil.getSession().clear();
        
        // remove structure itself
        StructureFactory.deleteStructure(st);
        
        // flushing cache
        FieldsCache.removeFields(st);
        StructureCache.removeStructure(st);
        StructureServices.removeStructureFile(st);
        
        try {
			PublisherAPI.getInstance().deleteElementFromPublishQueueTable(st.getInode());
		} catch (DotPublisherException e) {
			Logger.error(getClass(), "Error deleting Contentlet from Publishing Queue. Identifier:  " + st.getIdentifier());
		}
    }

	@Override
	public Structure find(String inode, User user) throws DotSecurityException, DotDataException, DotStateException {
		Structure s = StructureFactory.getStructureByInode(inode);
		if(!APILocator.getPermissionAPI().doesUserHavePermission(s, PermissionAPI.PERMISSION_READ, user)){
			throw new DotSecurityException("User " + user + " does not have permission to struct " +inode);
		}
		return s;
				
		
	}

	/**
	 * Delete broken contentlet that use the structure specified
	 * @param structureInode
	 */
	private void deleteStructureContentlets(String structureInode){
		 DotConnect db = new DotConnect();
	     db.setSQL("delete from contentlet where structure_inode = '" + structureInode + "'");
	     db.getResult();
	}



}
