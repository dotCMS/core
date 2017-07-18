package com.dotmarketing.util;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.report.model.Report;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.dotcms.util.CollectionsUtils.entry;
import static com.dotcms.util.CollectionsUtils.imap;
import static com.dotcms.util.CollectionsUtils.mapEntries;

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

	private final static Map<String, Class> typeClassMap = imap(
			entry("contentlet",    Contentlet.class),
			entry("identifier",    Identifier.class),
			entry("template",      Template.class),
			entry("virtual_link",  VirtualLink.class),
			entry("user_proxy",    UserProxy.class),
			entry("structure",     Structure.class),
			entry("workflow_task",    WorkflowTask.class),
			entry("relationship",     Relationship.class),
			entry("workflow_comment", WorkflowComment.class),
			entry("workflow_history", WorkflowHistory.class),
			entry("folder",       Folder.class),
			entry("category",     Category.class),
			entry("containers",   Container.class),
			entry("links", 		  Link.class),
			entry("report_asset", Report.class),
			entry("field",		  Field.class)
	);

	public static Class getClassByDBType(final String type) {

		return (type != null && typeClassMap.containsKey(type))? typeClassMap.get(type): Inode.class;
	} // getClassByDBType.


	/**
	 * This method hits the DB, table inode to get the Type of the Asset.
	 *
	 * @param inode of the Asset we want to find out the Asset Type.
	 * @return String with the Type of the Asset if found. This method hist the DB.
	 * @throws DotDataException
	 */
	public static String getAssetTypeFromDB(String inode) throws DotDataException {
		String assetType = null;

		try{
			DotConnect dotConnect = new DotConnect();
			List<Map<String, Object>> results = new ArrayList<Map<String,Object>>();

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

		} finally{
			DbConnectionFactory.closeConnection();
		}

		return assetType;
	}
}
