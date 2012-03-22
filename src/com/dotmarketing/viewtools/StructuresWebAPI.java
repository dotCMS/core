package com.dotmarketing.viewtools;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.widget.business.WidgetAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class StructuresWebAPI implements ViewTool {

	private HttpServletRequest request;
	private User user = null;
	private User backuser = null;
	private PermissionAPI perAPI;
	private UserWebAPI userAPI;
	private WidgetAPI wAPI;
	private FormAPI fAPI;

	// private HttpServletRequest request;
	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		userAPI = WebAPILocator.getUserWebAPI();
		perAPI = APILocator.getPermissionAPI();
		wAPI = APILocator.getWidgetAPI();
		fAPI = APILocator.getFormAPI();
		try {
			user = userAPI.getLoggedInFrontendUser(request);
			backuser = userAPI.getLoggedInUser(request);
		} catch (Exception e) {
			Logger.error(this, "Error finding the logged in user", e);
		}
	}

	/**
	 * Use this viewtool method to find all structures.
	 * It will use the backend user or front end user depending on the backendPermissions boolean.
	 * @param backendPermissions
	 * @return
	 */
	public List<Structure> findStructures(boolean backendPermissions){
		try{
			List<Structure> structures = StructureFactory.getStructures();
			if(backendPermissions){
				return perAPI.filterCollection(structures, PermissionAPI.PERMISSION_READ, false, backuser);
			}else{
				return perAPI.filterCollection(structures, PermissionAPI.PERMISSION_READ, true, user);
			}
		}catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
			return new ArrayList<Structure>();
		}
	}

	/**
	 * Use this viewtool method to find all widgets.
	 * It will use the backend user or front end user depending on the backendPermissions boolean.
	 * @param backendPermissions
	 * @return
	 */
	public List<Structure> findWidgets(boolean backendPermissions){
		try{
			if(backendPermissions){
				return wAPI.findAll(backuser, false);
			}else{
				return wAPI.findAll(user, true);
			}
		}catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
			return new ArrayList<Structure>();
		}
	}

	/**
	 * Use this viewtool method to find all forms.
	 * It will use the backend user or front end user depending on the backendPermissions boolean.
	 * @param backendPermissions
	 * @return
	 */
	public List<Structure> findForms(boolean backendPermissions){
		try{
			if(backendPermissions){
				return fAPI.findAll(backuser, false);
			}else{
				return fAPI.findAll(user, true);
			}
		}catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
			return new ArrayList<Structure>();
		}
	}

	/**
	 * Retrieves a structure by its id/inode
	 * @param inode
	 * @return
	 */
	@Deprecated
	public Structure findStructure(long inode) {
		return findStructure(String.valueOf(inode));
	}

	/**
	 * Retrieves a structure by its id/inode
	 * @param inode
	 * @return
	 */
	public Structure findStructure(String inode) {
		return StructureCache.getStructureByInode(inode);
	}

	/**
	 * Retrieves a structure by its structureName/type
	 * @param structureName
	 * @return
	 * @deprecated getting the structure by its name might not be safe, since the
     * structure name can be changed by the user, use getStructureByVelocityVarName
	 */
	public Structure findStructureByName(String structureName) {
		Structure structure =StructureCache.getStructureByType(structureName);
		//http://jira.dotmarketing.net/browse/DOTCMS-6282
		if(!UtilMethods.isSet(structure.getInode())){
			return findStructureByVelocityVarName(structureName);
		}
		return structure;

	}

	/**
	 * Retrieves a structure by its velocityvarname
	 * @param velocityVarName
	 * @return
	 */
	public Structure findStructureByVelocityVarName(String velocityVarName) {
		return StructureCache.getStructureByVelocityVarName(velocityVarName);
	}

	/**
	 * Retrieves a field from a structure based on its field inode
	 * @param st The parent structure
	 * @param fieldInode the inode
	 * @return
	 */
	@Deprecated
	public Field findField(Structure st, long fieldInode) {
		return findField(st,String.valueOf(fieldInode));
	}

	public Field findField(Structure st, String fieldInode) {
		List<Field> fields = FieldsCache.getFieldsByStructureInode(st.getInode());
		for(Field f : fields) {
			if(f.getInode().equalsIgnoreCase(fieldInode))
				return f;
		}
		return null;
	}

	/**
	 * Retrieves a field from a structure based on its field inode
	 * @param st The structure
	 * @param fieldVariableName The variable name
	 * @return
	 */
	public Field findFieldByVarName(Structure st, String fieldVariableName) {
		List<Field> fields = FieldsCache.getFieldsByStructureInode(st.getInode());
		for(Field f : fields) {
			if(f.getVelocityVarName().equals(fieldVariableName))
				return f;
		}
		return null;
	}

	/**
	 * Retrieves all the fields associated to a structure
	 * @return
	 */
	public List<Field> getFields(Structure st) {
		return FieldsCache.getFieldsByStructureInode(st.getInode());
	}

	/**
	 * Retrieves if the user have permission to publish on the structure
	 * @param st The Structure
	 * @param user The user loggedIn
	 * @return boolean
	 * @throws DotDataException
	 */
	public boolean havePublishPermision(Structure st, User user) throws DotDataException{
		boolean ret = APILocator.getPermissionAPI().doesUserHavePermission(st, PermissionAPI.PERMISSION_PUBLISH, user, true);
		return ret;
	}

	/**
	 * Retrieves if the user have permission to edit on the structure
	 * @param st The Structure
	 * @param user The user loggedIn
	 * @return boolean
	 * @throws DotDataException
	 */
	public boolean haveEditPermision(Structure st, User user) throws DotDataException{
		boolean ret = APILocator.getPermissionAPI().doesUserHavePermission(st, PermissionAPI.PERMISSION_EDIT, user, true);
		return ret;
	}

	/**
	 * Retrieves if the user have permission to view on the structure
	 * @param st The Structure
	 * @param user The user loggedIn
	 * @return boolean
	 * @throws DotDataException
	 */
	public boolean haveViewPermision(Structure st, User user) throws DotDataException{
		boolean ret = APILocator.getPermissionAPI().doesUserHavePermission(st, PermissionAPI.PERMISSION_READ, user, true);
		return ret;
	}

	/**
	 * Retrieve the list of relationships associated to the structure
	 * @param st The Structure
	 * @return List<Relationship>
	 */
	public List<Relationship> getStructureRelationShips(Structure st){
		return RelationshipFactory.getAllRelationshipsByStructure(st);
	}

	/**
	 * This gets the list of all the relationship objects associated to the
	 * structure of the contentlet
	 *
	 * @param cont
	 *            The contentlet
	 * @param hasParent
	 *            true If you find the relations where the contentlet is parent,
	 *            false If you find the relations where the contentlet is child
	 * @return A list of relationship objects
	 */
	public List<Relationship> getRelationshipsOfStructure(Structure st, boolean hasParent) {
		return getRelationshipsOfStructure(st.getInode(), hasParent);
	}

	@Deprecated
	public List<Relationship> getRelationshipsOfStructure(long structureInode, boolean hasParent) {
		return getRelationshipsOfStructure(((Long) structureInode).toString(), hasParent);
	}

	public List<Relationship> getRelationshipsOfStructure(String structureInode, boolean hasParent) {
		Structure st = (Structure) InodeFactory.getInode(structureInode, Structure.class);
		return RelationshipFactory.getAllRelationshipsByStructure(st, hasParent);
	}

	public boolean isFieldConstant(Field field){
		return APILocator.getFieldAPI().isElementConstant(field);
	}
}