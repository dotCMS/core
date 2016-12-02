package com.dotmarketing.portlets.structure.business;


import java.util.List;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.enterprise.ESSeachAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

import java.util.*;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.portlet.PortletURL;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.business.*;
import com.dotmarketing.cache.ContentTypeCache;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;

import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;


/**
 * This API exposes useful methods to access and modify information related to
 * Content Types in dotCMS. Please note that the term Structure is deprecated,
 * and it's referred to as Content Type now.
 * 
 * @author Jorge Urdaneta
 * @version 1.0
 * @since Feb 11, 2013
 *
 */
public class StructureAPIImpl implements StructureAPI {


	@Override
	public void delete(Structure st, User user) throws DotSecurityException, DotDataException, DotStateException {
		APILocator.getContentTypeAPI2(user).delete(new StructureTransformer(st).from());
	}
	
	@Override
	public void save(Structure st, User user) throws DotSecurityException, DotDataException, DotStateException {
		APILocator.getContentTypeAPI2(user).save(new StructureTransformer(st).from());
	}

	private final ContentTypeCache contentTypeCache = CacheLocator.getContentTypeCache();

	private static String RECENTS_STRUCTURE_QUERY = "select s.name,s.structuretype as type,structure_inode as inode,c.mod_date " +
			"from contentlet as c,structure as s\n" +
			"where c.structure_inode = s.inode and mod_user = ? and s.structuretype = ? " +
			"order by c.mod_date desc;";

    public void olddelete(Structure st, User user) throws DotSecurityException, DotDataException, DotStateException {        
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
        ContentletAPI conAPI=APILocator.getContentletAPI();
        // delete related contentlets
        /*
        int limit = 200;
        int offset = 0;
       
        List<Contentlet> contentlets=null;
        do {
            contentlets = conAPI.findByStructure(st, user, false, limit, offset);
            for(Contentlet contentlet : contentlets){
            	contentlet.getMap().put(Contentlet.DONT_VALIDATE_ME, true);
            }
            conAPI.delete(contentlets, user, false);
        } while(contentlets.size()>0);

        */
        
        //delete bad data contents
       // deleteStructureContentlets(st.getInode());
        
        // delete Forms entry if it is a form structure
        if (st.getStructureType() == Structure.STRUCTURE_TYPE_FORM) {
            Structure sf = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(
                    FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME);
            if (UtilMethods.isSet(sf) && UtilMethods.isSet(sf.getInode())) {
                conAPI.delete( conAPI.search(
                        "+structureInode:" + sf.getInode() +
                        " +structureInode:" + st.getInode(), 0, 0,
                        "", user, false), user, false);
            }
        }
/*
        // delete relationships where the structure is child or parent
        List<Relationship> relationships = FactoryLocator.getRelationshipFactory().getRelationshipsByParent(st);
        for (Relationship rel : relationships)
          FactoryLocator.getRelationshipFactory().deleteRelationship(rel);
        relationships = FactoryLocator.getRelationshipFactory().getRelationshipsByChild(st);
        for (Relationship rel : relationships)
          FactoryLocator.getRelationshipFactory().deleteRelationship(rel);
*/
        // remove structure permissions
        perAPI.removePermissions(st);

        HibernateUtil.getSession().clear();
        
        // remove structure itself
        StructureFactory.deleteStructure(st);

        // flushing cache
        FieldsCache.removeFields(st);
        CacheLocator.getContentTypeCache().remove(st);

    


	}
	@Override
	public Structure find(String inode, User user) throws DotSecurityException, DotDataException, DotStateException {

		return new StructureTransformer(APILocator.getContentTypeAPI2(user).find(inode)).asStructure();

	}

	@Override
	public List<Structure> find(User user, boolean respectFrontendRoles, boolean allowedStructsOnly) throws DotDataException {

		return new StructureTransformer(APILocator.getContentTypeAPI2(user, respectFrontendRoles).findAll()).asStructureList();

	}

	@Override
	public List<Structure> find(User user, boolean respectFrontendRoles, boolean allowedStructsOnly, String condition, String orderBy,
			int limit, int offset, String direction) throws DotDataException {

		return new StructureTransformer(APILocator.getContentTypeAPI2(user, respectFrontendRoles).search(condition, orderBy + " " + direction, limit,
				offset)).asStructureList();

	}

	@Override
	public Structure findByVarName(String varName, User user) throws DotSecurityException, DotDataException {

		return new StructureTransformer(APILocator.getContentTypeAPI2(user).find(varName)).asStructure();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.dotmarketing.portlets.structure.business.StructureAPI#countStructures
	 * (java.lang.String)
	 */
	@Override
	public int countStructures(String condition) {
		try {
			return APILocator.getContentTypeAPI2(APILocator.systemUser()).count(condition);
		} catch (DotDataException dde) {
			throw new DotStateException(dde.getMessage(), dde);
		}
	}

	public Collection<Map<String, Object>> getRecentContentType(BaseContentType type, User user, int nRecents) throws DotDataException {

	    
	    return getRecentsFromDataBase(type, user, nRecents);
	}

	private Collection<Map<String, Object>> getRecentsFromDataBase(BaseContentType type, User user, int nRecents) throws DotDataException {
	
		final DotConnect dc = new DotConnect();
		dc.setSQL(RECENTS_STRUCTURE_QUERY);
		dc.addParam(user.getUserId());
		dc.addParam(type.getType());

		List<Map<String, Object>> queryResults = dc.loadObjectResults();

		Map result = new LinkedHashMap();

		for (Map<String, Object> queryResult : queryResults) {
			String inode = (String) queryResult.get("inode");

			if (!result.containsKey(inode)){
				result.put(inode, queryResult);
			}

			if (nRecents != -1 && result.size() == nRecents){
				break;
			}
		}

		return result.values();
	

	}
}
