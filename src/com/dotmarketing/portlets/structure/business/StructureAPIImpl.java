package com.dotmarketing.portlets.structure.business;


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
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.services.StructureServices;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portlet.PortletURLImpl;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;

import static com.dotcms.util.CollectionsUtils.map;

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

	private final ContentTypeCache contentTypeCache = CacheLocator.getContentTypeCache();

	private static String RECENTS_STRUCTURE_QUERY = "select s.name,s.structuretype as type,structure_inode as inode,c.mod_date " +
			"from contentlet as c,structure as s\n" +
			"where c.structure_inode = s.inode and mod_user = ? and s.structuretype = ? " +
			"order by c.mod_date desc;";

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
            for(Contentlet contentlet : contentlets){
            	contentlet.getMap().put(Contentlet.DONT_VALIDATE_ME, true);
            }
            conAPI.delete(contentlets, user, false);
        } while(contentlets.size()>0);

        //delete bad data contents
        deleteStructureContentlets(st.getInode());
        
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
        CacheLocator.getContentTypeCache().remove(st);
        StructureServices.removeStructureFile(st);
    }

	@Override
	public Structure find(String inode, User user) throws DotSecurityException, DotDataException, DotStateException {
		Structure s = StructureFactory.getStructureByInode(inode);
		if(!APILocator.getPermissionAPI().doesUserHavePermission(s, PermissionAPI.PERMISSION_READ, user)){
			throw new DotSecurityException("User " + user + " does not have permission to struct " +inode);
		}
		return s;
	}

	@Override
	public List<Structure> find(User user, boolean respectFrontendRoles, boolean allowedStructsOnly) throws DotDataException {
		return StructureFactory.getStructures(user, respectFrontendRoles, allowedStructsOnly);
	}

	@Override
	public List<Structure> find(User user, boolean respectFrontendRoles, boolean allowedStructsOnly, String condition,
			String orderBy, int limit, int offset, String direction) throws DotDataException {
		return StructureFactory.getStructures(user, respectFrontendRoles, allowedStructsOnly, condition, orderBy, limit,
				offset, direction);
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
	
	@Override
	public Structure findByVarName(String varName, User user) throws DotSecurityException, DotDataException{
		Structure s = StructureFactory.getStructureByVelocityVarName(varName);
		if(!APILocator.getPermissionAPI().doesUserHavePermission(s, PermissionAPI.PERMISSION_READ, user)){
			throw new DotSecurityException("User " + user + " does not have permission to struct " +varName);
		}
		return s;
	}
	
    /*
     * (non-Javadoc)
     * 
     * @see com.dotmarketing.portlets.structure.business.StructureAPI#countStructures(java.lang.String)
     */
    @Override
    public int countStructures(String condition) {
        return StructureFactory.getStructuresCount(condition);
    }

	public Collection<Map<String, Object>> getRecentContentType(Structure.Type type, User user, int nRecents) throws DotDataException {
		Collection<Map<String, Object>> recents = contentTypeCache.getRecents(type, user, nRecents);

		if (recents == null) {
			recents = getRecentsFromDataBase(type, user, nRecents);
			contentTypeCache.addRecents(type, user, nRecents, recents);
		}

		return recents;
	}

	private Collection<Map<String, Object>> getRecentsFromDataBase(Structure.Type type, User user, int nRecents) throws DotDataException {
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
