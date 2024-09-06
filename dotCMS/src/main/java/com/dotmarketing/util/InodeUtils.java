package com.dotmarketing.util;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.business.CategoryTransformer;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InodeUtils {

	/**
	 * Checks whether the inodeStr isSet or not.
	 * 
	 * @param inodeStr
	 *            Inode.inode which is a String.
	 * 
	 * @return the value <code>True</code> if the inodeStr is a in valid
	 *         UUID.toString() format or inodeStr is a positive long. else the
	 *         value <code>False</code>.
	 * 
	 */
	public static final boolean isSet(String inodeStr) {

		if (inodeStr == null)
			return false;

		inodeStr = inodeStr.toLowerCase();
		inodeStr = inodeStr.trim();

		if (inodeStr.equals("null") || inodeStr.equals("0") ||
			inodeStr.equals("") || inodeStr.equals("''")) {
			return false;
		}

		return (inodeStr.length() > 0);
	}

	/**
	 * Compares two inodes for ordering.
	 * 
	 * @param inodeStr1
	 *            Inode.inode which is a String.
	 * @param inodeStr2
	 *            Inode.inode which is a String.
	 * 
	 * @return the value <code>0</code> if the Inode corresponding to inodeStr1
	 *         is created at the same time as Inode corresponding to inodeStr2.
	 *         a value less than <code>0</code> if the Inode corresponding to
	 *         inodeStr1 is created before the Inode corresponding to inodeStr2.
	 *         and a value greater than <code>0</code> if the Inode
	 *         corresponding to inodeStr1 is created after the Inode
	 *         corresponding to inodeStr2.
	 * 
	 */
	public static final int compareInodes(String inodeStr1, String inodeStr2) {

		int result = 0;

		if (isSet(inodeStr1) && isSet(inodeStr2)) {
			Inode inodeObj1 = InodeFactory.getInode(inodeStr1, Inode.class);
			Inode inodeObj2 = InodeFactory.getInode(inodeStr2, Inode.class);
			result = inodeObj1.getiDate().compareTo(inodeObj2.getiDate());
		}

		return result;
	}

	public static Class getClassByDBType(String type) {

		if (type == null) {
			return Inode.class;
		}

		if (type.equals("identifier")) {
			return Identifier.class;
		}

		else if (type.equals("template")) {
			return Template.class;
		} else if (type.equals("structure")) {
			return Structure.class;
		} else if (type.equals("workflow_task")) {
			return WorkflowTask.class;
		} else if (type.equals("relationship")) {
			return Relationship.class;

		} else if (type.equals("workflow_comment")) {
			return WorkflowComment.class;
		}		

		else if (type.equals("workflow_history")) {
			return WorkflowHistory.class;
		}

		else if (type.equals("folder")) {
			return Folder.class;
		}

		else if (type.equals("category")) {
			return Category.class;

		} else if (type.equals("template")) {
			return Template.class;
		} else if (type.equals("containers")) {
			return Container.class;
		} else if (type.equals("links")) {
			return Link.class;
		} else  {
			return Inode.class;
		}

	}

	/**
	 * This method hits the DB, table inode to get the Type of the Asset.
	 *
	 * @param inode of the Asset we want to find out the Asset Type.
	 * @return String with the Type of the Asset if found. This method hist the DB.
	 * @throws DotDataException
	 */
	@CloseDBIfOpened
	public static String getAssetTypeFromDB(String inode) throws DotDataException {
		String assetType = null;

		try{
			DotConnect dotConnect = new DotConnect();
			List<Map<String, Object>> results = new ArrayList<>();

			dotConnect.setSQL("SELECT type FROM inode WHERE inode = ?");
			dotConnect.addParam(inode);

			Connection connection = DbConnectionFactory.getConnection();
			results = dotConnect.loadObjectResults(connection);

			if(!results.isEmpty()){
				assetType = results.get(0).get("type").toString();
			}

		} catch (DotDataException e) {
			Logger.error(InodeUtils.class, "Error trying find the Asset Type " + e.getMessage());
			throw new DotDataException("Error trying find the Asset Type ", e);

		}

		return assetType;
	}
	
	/**
	 * This method intents to replace any call to the deprecated InodeFactory.getInode,
	 * since the Structure mapping was deleted from the Hibernate files, now you need to 
	 * get it via the ContentTypeAPI.
	 * 
	 * @param inode
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@CloseDBIfOpened
	public static Inode getInode(final String inode) throws DotDataException, DotSecurityException{
	    Inode inodeObj = null;
        //Using the ShortyAPI to identify the nature of this inode
        final Optional<ShortyId> shortOpt = APILocator.getShortyAPI().getShorty(inode);
        
      //Hibernate won't handle structures, thats why we need a special case here
        if ( shortOpt.isPresent() && ShortType.STRUCTURE == shortOpt.get().subType ) {

            //Search for the given ContentType inode
            final ContentType foundContentType = APILocator.getContentTypeAPI(APILocator.systemUser()).find(inode);
            if ( null != foundContentType ) {
                //Transform the found content type to a Structure
                inodeObj = new StructureTransformer(foundContentType).asStructure();
            }
		}else if ( shortOpt.isPresent() && ShortType.FOLDER == shortOpt.get().subType ) {
			//Folder no longer inherit from inode, returning an empty inode
			inodeObj = new Inode();
		} else if ( shortOpt.isPresent() && ShortType.CATEGORY == shortOpt.get().subType ) {
			inodeObj = APILocator.getCategoryAPI().find(inode, APILocator.systemUser(), false);
		} else {
            inodeObj = InodeFactory.getInode(inode, Inode.class);
        }
        return inodeObj;
	}
}
