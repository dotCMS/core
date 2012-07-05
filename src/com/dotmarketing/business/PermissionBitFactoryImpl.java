package com.dotmarketing.business;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.bulk.BulkRequestBuilder;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.ESContentletIndexAPI;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.PermissionReference;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * This class upgrades the old permissionsfactoryimpl to handle the storage and retrieval of bit permissions from the database
 * a big storage improvement that will let us reduces the amount of rows in the permissions table
 * as much as three times.
 *
 * @author David Torres (2009)
*/
public class PermissionBitFactoryImpl extends PermissionFactory {

	private PermissionCache permissionCache;
	private static final Map<String, Integer> PERMISION_TYPES = new HashMap<String, Integer>();

	//SQL Queries used to maintain permissions

	/*
	 * To load permissions either individual permissions or inherited permissions as well as inheritable permissions for a
	 * given permissionable id
	 *
	 * Parameters
	 * 1. The permisionable id
	 * 2. The permisionable id
	 */

	private final String loadPermissionHSQL =
		"select {permission.*} from permission where permission.id in (" +
		"	select permission.id from permission where inode_id = ?" +
		"	union" +
		"	select permission.id from permission where exists (" +
		"		select * from permission_reference where asset_id = ? " +
		"		and inode_id = reference_id and permission.permission_type = permission_reference.permission_type" +
		"	)" +
		")";

	/*
	 * To load permission references objects based on the reference they are pointing to
	 * Parameters
	 * 1. The reference id the references are pointing to
	 */
	private final String loadPermissionReferencesByReferenceIdHSQL = "from " + PermissionReference.class.getCanonicalName() +
		" permission_reference where reference_id = ?";

	/*
	 * To insert a single permission reference for an asset
	 * Parameters
	 * 1. Asset id
	 * 2. Reference id
	 * 3. Type
	 */
//	private final String insertPermissionReferenceSQL =
//		(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL) || DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)?
//		"insert into permission_reference (asset_id, reference_id, permission_type) " +
//		"	SELECT ?,?,? FROM permission_reference ":
//		DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)?
//		"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
//		"	SELECT (SELECT permission_reference_seq.NEXTVAL), ?, ?, ? FROM permission_reference ":
//		"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
//		"	SELECT (SELECT nextval('permission_reference_seq')), ?, ?, ? FROM permission_reference ") +
//		" WHERE not exists (SELECT asset_id from permission_reference where asset_id = ?)";

	private final String insertPermissionReferenceSQL =
		DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL) || DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)?
		"insert into permission_reference (asset_id, reference_id, permission_type) " +
		"	values (?, ?, ?)":
		DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)?
		"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
		"	values (permission_reference_seq.NEXTVAL, ?, ?, ?)":
		"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
		"	values (nextval('permission_reference_seq'), ?, ?, ?)";

	/*
	 * To update a permission reference by who it is currently pointing to
	 * Parameters
	 * 1. New reference id to set
	 * 2. Permission type
	 * 3. Reference id to be updated
	 */
	private final String updatePermissionReferenceByReferenceIdSQL = "update permission_reference set reference_id = ? where permission_type = ? and reference_id = ?";

	/*
	 * To update a permission reference by the owner asset
	 * Parameters
	 * 1. New reference id to set
	 * 2. Permission type
	 * 3. asset id
	 */
	private final String updatePermissionReferenceByAssetIdSQL = "update permission_reference set reference_id = ? where permission_type = ? and asset_id = ?";

	/*
	 * Select permission references based on how are referencing the type of reference
	 * Parameters
	 * 1. Reference id
	 * 2. Permission type
	 */
	private final String selectPermissionReferenceSQL = "select asset_id from permission_reference where reference_id = ? and permission_type = ?";

	/*
	 * To remove a permission reference of an specific asset or referencing an asset
	 *
	 */
	private final String deletePermissionReferenceSQL = "delete from permission_reference where asset_id = ? or reference_id = ?";

	/*
	 * To remove all permission references
	 *
	 */
	private final String deleteAllPermissionReferencesSQL = "delete from permission_reference";


	/*
	 * To remove a permission reference of an specific asset
	 *
	 */
	private final String deletePermissionableReferenceSQL = "delete from permission_reference where asset_id = ?";

	/*
	 * To load template identifiers that are children of a host
	 * Parameters
	 * 1. The id of the host
	 */
	private final String selectChildrenTemplateSQL =
		"select distinct identifier.id from template, inode, identifier where " +
		"template.inode = inode.inode and template.identifier = identifier.id and " +
		"identifier.host_inode = ?";

	/*
	 * To load template identifiers that are children of a host and have inheritable permissions
	 * Parameters
	 * 1. The id of the host
	 */
	private final String selectChildrenTemplateWithIndividualPermissionsSQL =
		selectChildrenTemplateSQL + " and exists (select * from permission where inode_id = identifier.id and " +
				"permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "')";

	/*
	 * To remove all permission of templates attached to an specific host
	 * Parameters
	 * 1. The host id the templates belong to
	 */
	private final String deleteTemplatePermissionsSQL =
		"delete from permission where inode_id in " +
		"	(" + selectChildrenTemplateSQL + ")";

	/*
	 * To remove all permission references of templates attached to an specific host
	 * Parameters
	 * 1. The host id the templates belong to
	 */
	private final String deleteTemplateReferencesSQL =
		"delete from permission_reference where asset_id in " +
		"	(" + selectChildrenTemplateSQL + ")";

	/*
	 * To insert permission references to all templates attached to a host, it only inserts the references if the template does not have
	 * a reference already and does not have individual permissions
	 *
	 * Parameters
	 * 1. The host id you want the new reference to point to
	 * 2. The host id the templates belong to
	 */
	private final String insertTemplateReferencesToAHostSQL =
		(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL) || DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)?
		"insert into permission_reference (asset_id, reference_id, permission_type) " +
		"select ":
		 DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)?
		"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
		"select permission_reference_seq.NEXTVAL, ":
		"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
		"select nextval('permission_reference_seq'), ") +
		"identifier.id, ?, '" + Template.class.getCanonicalName() + "'" +
		"	from identifier where identifier.id in " +
		"		(" + selectChildrenTemplateSQL + " and " +
		"		 template.identifier not in (select inode_id from permission " +
		"			where permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "') and " +
		"		 template.identifier not in (select asset_id from permission_reference where " +
		"			permission_type = '" + Template.class.getCanonicalName() + "'))";

	/*
	 * To load container identifiers that are children of a host
	 * Parameters
	 * 1. The host id
	 */
	private final String selectChildrenContainerSQL =
		"select distinct identifier.id from containers, inode, identifier where " +
		"containers.inode = inode.inode and containers.identifier = identifier.id and " +
		"identifier.host_inode = ?";

	/*
	 * To load container identifiers that are children of a host and have inheritable permissions
	 * Parameters
	 * 1. The host id
	 */
	private final String selectChildrenContainerWithIndividualPermissionsSQL =
		selectChildrenContainerSQL + " and exists (select * from permission where inode_id = identifier.id and " +
		"permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "')";

	/*
	 * To remove all permissions of containers attached to an specific host
	 * Parameters
	 * 1. The host id the containers belong to
	 */
	private final String deleteContainerPermissionsSQL =
		"delete from permission where inode_id in " +
		"	(" + selectChildrenContainerSQL + ")";

	/*
	 * To remove all permission references of containers attached to an specific host
	 * Parameters
	 * 1. The host id the containers belong to
	 */
	private final String deleteContainerReferencesSQL =
		"delete from permission_reference where asset_id in " +
		"	(" + selectChildrenContainerSQL + ")";
	/*
	 * To insert permission references to all containers attached to a host, it only inserts the reference if the container does not have
	 * a reference already and does not have individual permissions
	 *
	 * Parameters
	 * 1. The host id you want the new reference to point to
	 * 2. The host id the templates belong to
	 */
	private final String insertContainerReferencesToAHostSQL =
		(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL) || DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)?
			"insert into permission_reference (asset_id, reference_id, permission_type) " +
			"select ":
		 DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)?
			"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
			"select permission_reference_seq.NEXTVAL, ":
			"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
			"select nextval('permission_reference_seq'), ") +
		" identifier.id, ?, '" + Container.class.getCanonicalName() + "'" +
		"	from identifier where identifier.id in " +
		"		(" + selectChildrenContainerSQL + " and " +
		"		 containers.identifier not in (select inode_id from permission " +
		"			where permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "') and " +
		"		 containers.identifier not in (select asset_id from permission_reference where " +
		"			permission_type = '" + Container.class.getCanonicalName() + "'))";

	/**
	 * Function name to get the folder path. MSSql need owner prefix dbo
	 */
	private final String dotFolderPath=(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL) ? "dbo.":"")+"dotFolderPath";

	/*
	 * To load folder inodes that are in the same tree/hierarchy of a parent host/folder
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 * 3. Parent folder exact path E.G. '/about/' pass '' if you want all from the host
	 */
	private final String selectChildrenFolderSQL =
		"select distinct folder.inode from folder, inode,identifier where " +
		"folder.inode = inode.inode and	folder.identifier = identifier.id and identifier.host_inode = ? and "+dotFolderPath+"(parent_path,asset_name) like ? and "+dotFolderPath+"(parent_path,asset_name) <> ?";

	/*
	 * To load folder identifiers that are children of a host and have either individual and/or inheritable permissions
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 * 3. Parent folder exact path E.G. '/about/' pass '' if you want all from the host
	 */
	private final String selectChildrenFolderWithDirectPermissionsSQL =
		selectChildrenFolderSQL + " and exists (select * from permission where inode_id = inode.inode)";

	/*
	 * To remove all permissions of sub-folders of a given parent folder
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 * 3. Parent folder exact path E.G. '/about/' pass '' if you want all from the host
	 */
	private final String deleteSubfolderPermissionsSQL =
		"delete from permission where inode_id in " +
		"	(" + selectChildrenFolderSQL + ")";

	/*
	 * To delete all permission references on sub-folders of a given parent folder
	 *
	 * Parameters
	 * 1. host the sub-folders belong to
	 * 2. path like for sub-folder E.G /about/% (everything under /about/)
	 * 3. exact path E.G. /about/ (notice end / that is the way dotCMS saves paths for folders in the identifier table)
	 * 4. host the sub-folders belong to
	 * 5. same as 2
	 */
	private final String deleteSubfolderReferencesSQL =
			"delete from permission_reference where exists (" +
			" " + selectChildrenFolderSQL + " and " +
			"	permission_type = '" + Folder.class.getCanonicalName() + "' and asset_id = folder.inode)";

	private final String deleteSubfolderReferencesSQLOnAdd =
		"delete from permission_reference where exists(" +
		" " + selectChildrenFolderSQL + " and " +
		"	permission_type = '" + Folder.class.getCanonicalName() + "' and asset_id = folder.inode) " +
		"and (reference_id in ( " +
			"select distinct folder.inode " +
			"from folder, inode,identifier " +
			"where folder.inode = inode.inode " +
			"	and folder.identifier = identifier.id and identifier.host_inode = ? " +
			"	and ("+dotFolderPath+"(parent_path,asset_name) not like ? OR "+dotFolderPath+"(parent_path,asset_name) = ?) " +
			"	and permission_type = 'com.dotmarketing.portlets.folders.model.Folder' " +
			"	and reference_id = folder.inode" +
			")" +
			"OR EXISTS(SELECT c.inode " +
			"FROM contentlet c JOIN inode i " +
			"ON  " +
			"  i.type = 'contentlet' " +
			"  AND i.inode = c.inode" +
			"  WHERE c.identifier = reference_id)	" +
			")";

	/*
	 * To insert permission references to sub-folders of a given parent folder, it only insert the references if the folder does not have already a reference or
	 * individual permissions
	 *
	 * Parameters
	 * 1. folder/host id the new references are going to point to
	 * 2. host the sub-folders belong to
	 * 3. path like for sub-folder E.G /about/% (everything under /about/)
	 * 4. exact path E.G. /about/ (notice end / that is the way dotCMS saves paths for folders in the identifier table)
	 * 5. same as 3
	 */
	private final String insertSubfolderReferencesSQL =
		(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL) || DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)?
				"insert into permission_reference (asset_id, reference_id, permission_type) " +
				"select ":
		 DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)?
				"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				"select permission_reference_seq.NEXTVAL, ":
				"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				"select nextval('permission_reference_seq'), ") +
		" folder.inode, ?, '" + Folder.class.getCanonicalName() + "'" +
		"	from folder where folder.inode in (" +
		"		" + selectChildrenFolderSQL + " and " +
		"		folder.inode not in (" +
		"			select asset_id from permission_reference, folder ref_folder where " +
		"			reference_id = ref_folder.inode and " +
		"			"+dotFolderPath+"(parent_path,asset_name) like ? and permission_type = '" + Folder.class.getCanonicalName() + "'" +
		"		) and " +
		"		folder.inode not in (" +
		"			select inode_id from permission where " +
		"			permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		"		) " +
		"	) and not exists (SELECT asset_id from permission_reference where asset_id = folder.inode)";

	/*
	 * To load html page identifiers that are in the same tree/hierarchy of a parent host/folder
	 *
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
	private final String selectChildrenHTMLPageSQL =
		"select distinct identifier.id from htmlpage join identifier " +
		" on (htmlpage.identifier = identifier.id) where " +
		" identifier.host_inode = ? and identifier.parent_path like ?";

	/*
	 * To load html pages identifiers that are children of a host and have inheritable permissions
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
	private final String selectChildrenHTMLPageWithIndividualPermissionsSQL =
		selectChildrenHTMLPageSQL + " and exists (select * from permission where inode_id = identifier.id and " +
		"permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "')";

	/*
	 * To remove all permissions of html pages of a given parent folder
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
	private final String deleteHTMLPagePermissionsSQL =
		"delete from permission where inode_id in " +
		"	(" + selectChildrenHTMLPageSQL + ")";


	/*
	 * To delete all permission references on HTML pages under a given folder hierarchy
	 *
	 * Parameters
	 * 1. host the pages belong to
	 * 2. path like to the folder hierarchy the pages live under E.G /about/% (pages under /about/)
	 * 3. host the pages belong to
	 * 4. same as 2
	 */
	private final String deleteHTMLPageReferencesSQL =
			"delete from permission_reference where exists (" +
			"	" + selectChildrenHTMLPageSQL + " and" +
			"	permission_type = '" + HTMLPage.class.getCanonicalName() + "' and asset_id = identifier.id)";

	private final String deleteHTMLPageReferencesOnAddSQL =
		"delete from permission_reference where exists (" +
		selectChildrenHTMLPageSQL + " and " +
		" permission_type = '" + HTMLPage.class.getCanonicalName() + "' and asset_id = identifier.id) " +
		"and (reference_id in (" +
			"select distinct folder.inode " +
			" from folder join identifier on (folder.identifier = identifier.id) " +
			" where identifier.host_inode = ? " +
			" and ("+dotFolderPath+"(parent_path,asset_name) not like ? OR "+dotFolderPath+"(parent_path,asset_name) = ?) " +
			" and reference_id = folder.inode" +
			") " +
			" OR EXISTS(SELECT c.inode " +
			"  FROM contentlet c " +
			"  WHERE c.identifier = reference_id)	" +
			")";

	/*
	 * To insert permission references to HTML pages under a parent folder hierarchy, it only insert the references if the page
	 * does not have already a reference or individual permissions assigned
	 *
	 * Parameters
	 * 1. folder/host id the new references are going to point to
	 * 2. host the pages belong to
	 * 3. path like to the folder hierarchy the pages live under E.G /about/% (pages under /about/)
	 * 4. same as 3
	 */
	private final String insertHTMLPageReferencesSQL =
		(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL) || DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)?
				"insert into permission_reference (asset_id, reference_id, permission_type) " +
				"select ":
		 DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)?
				"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				"select permission_reference_seq.NEXTVAL, ":
				"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				"select nextval('permission_reference_seq'), ") +
				"	identifier.id, ?, '" + HTMLPage.class.getCanonicalName() + "' " +
				"	from identifier where identifier.id in (" +
				"		" + selectChildrenHTMLPageSQL + " and" +
				"		identifier.id not in (" +
		        "			select asset_id from permission_reference, folder ref_folder, identifier where ref_folder.identifier=identifier.id and " +
		        "			reference_id = ref_folder.inode and	"+dotFolderPath+"(parent_path,asset_name) like ? and permission_type = '" + HTMLPage.class.getCanonicalName() + "'" +
				"		) and " +
				"		identifier.id not in (" +
				"			select inode_id from permission where permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
				"		) " +
				"	) " +
				"and not exists (SELECT asset_id from permission_reference where asset_id = identifier.id)";

	/*
	 * To load file identifiers that are in the same tree/hierarchy of a parent host/folder
	 *
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
	private final String selectChildrenFileSQL =
		"select distinct identifier.id from file_asset, inode, identifier where " +
		"file_asset.inode = inode.inode and file_asset.identifier = identifier.id and " +
		"identifier.host_inode = ? and identifier.parent_path like ?";

	/*
	 * To load file identifiers that are children of a host and have inheritable permissions
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
	private final String selectChildrenFileWithIndividualPermissionsSQL =
		selectChildrenFileSQL + " and exists (select * from permission where inode_id = identifier.id and " +
		"permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "')";

	/*
	 * To remove all permissions of files of a given parent folder
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 * 3. Parent folder exact path E.G. '/about/' pass '' if you want all from the host
	 */
	private final String deleteFilePermissionsSQL =
		"delete from permission where inode_id in " +
		"	(" + selectChildrenFileSQL + ")";

	/*
	 * To delete all permission references on files under a given folder hierarchy
	 *
	 * Parameters
	 * 1. host the files belong to
	 * 2. path like to the folder hierarchy the files live under E.G /about/% (files under /about/)
	 * 3. host the files belong to
	 * 4. same as 2
	 */
	private final String deleteFileReferencesSQL =
			"delete from permission_reference where exists (" +
			"	" + selectChildrenFileSQL + " and" +
			"	permission_type = '" + File.class.getCanonicalName() + "' and asset_id = identifier.id)";

	private final String deleteFileReferencesOnAddSQL =
		"delete from permission_reference where exists (" +
		"	" + selectChildrenFileSQL + " and" +
		"	permission_type = '" + File.class.getCanonicalName() + "' and asset_id = identifier.id) " +
		"and (reference_id in (" +
		"select distinct folder.inode " +
		"from folder, inode,identifier " +
		"where folder.inode = inode.inode " +
		"and folder.identifier = identifier.id and identifier.host_inode = ? " +
		"and ("+dotFolderPath+"(parent_path,asset_name) not like ? OR "+dotFolderPath+"(parent_path,asset_name) = ?) " +
		"and permission_type = 'com.dotmarketing.portlets.folders.model.Folder' " +
		"and reference_id = folder.inode" +
		") " +
		"OR EXISTS(SELECT c.inode " +
		"FROM contentlet c JOIN inode i " +
		"ON  " +
		"  i.type = 'contentlet' " +
		"  AND i.inode = c.inode" +
		"  WHERE c.identifier = reference_id)	" +
		")";
	/*
	 * To insert permission references to files under a parent folder hierarchy, it only insert the references if the file
	 * does not have already a reference or individual permissions assigned
	 *
	 * Parameters
	 * 1. folder/host id the new references are going to point to
	 * 2. host the files belong to
	 * 3. path like to the folder hierarchy the files live under E.G /about/% (files under /about/)
	 * 4. same as 3
	 */
	private final String insertFileReferencesSQL =
		(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL) || DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)?
				"insert into permission_reference (asset_id, reference_id, permission_type) " +
				"select ":
		 DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)?
				"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				"select permission_reference_seq.NEXTVAL, ":
				"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				"select nextval('permission_reference_seq'), ") +
		"	identifier.id, ?, '" + File.class.getCanonicalName() + "' " +
		"	from identifier where identifier.id in (" +
		"		" + selectChildrenFileSQL + " and" +
		"		identifier.id not in (" +
		"			select asset_id from permission_reference, folder ref_folder, identifier ii where" +
		"			reference_id = ref_folder.inode and ii.id=ref_folder.identifier and" +
		"			"+dotFolderPath+"(ii.parent_path,ii.asset_name) like ? and permission_type = '" + File.class.getCanonicalName() + "'" +
		"		) and " +
		"		identifier.id not in (" +
		"			select inode_id from permission where " +
		"			permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		"		) " +
		"	) " +
		"and not exists (SELECT asset_id from permission_reference where asset_id = identifier.id)";

	/*
	 * To load link identifiers that are in the same tree/hierarchy of a parent host/folder
	 *
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
	private final String selectChildrenLinkSQL =
		"select distinct identifier.id from links, inode, identifier where " +
		"links.inode = inode.inode and links.identifier = identifier.id and " +
		"identifier.host_inode = ? and identifier.parent_path like ?";

	/*
	 * To load link identifiers that are children of a host and have inheritable permissions
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
	private final String selectChildrenLinkWithIndividualPermissionsSQL =
		selectChildrenLinkSQL + " and exists (select * from permission where inode_id = identifier.id and " +
		"permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "')";

	/*
	 * To remove all permissions of links of a given parent folder
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
	private final String deleteLinkPermissionsSQL =
		"delete from permission where inode_id in " +
		"	(" + selectChildrenLinkSQL + ")";

	/*
	 * To delete all permission references on menu links under a given folder hierarchy
	 *
	 * Parameters
	 * 1. host the links belong to
	 * 2. path like to the folder hierarchy the links live under E.G /about/% (files under /about/)
	 * 3. host the links belong to
	 * 4. same as 2
	 */
	private final String deleteLinkReferencesSQL =
			"delete from permission_reference where exists (" +
			"	" + selectChildrenLinkSQL + " and" +
			"	permission_type = '" + Link.class.getCanonicalName() + "' and asset_id = identifier.id)";

	private final String deleteLinkReferencesOnAddSQL =
		"delete from permission_reference where exists (" +
		"	" + selectChildrenLinkSQL + " and" +
		"	permission_type = '" + Link.class.getCanonicalName() + "' and asset_id = identifier.id) " +
		"and (reference_id in (" +
		"select distinct folder.inode " +
		"from folder, inode,identifier " +
		"where folder.inode = inode.inode " +
		"and folder.identifier = identifier.id and identifier.host_inode = ? " +
		"and ("+dotFolderPath+"(parent_path,asset_name) not like ? OR "+dotFolderPath+"(parent_path,asset_name) = ?) " +
		"and permission_type = 'com.dotmarketing.portlets.folders.model.Folder' " +
		"and reference_id = folder.inode" +
		") " +
		"OR EXISTS(SELECT c.inode " +
		"FROM contentlet c JOIN inode i " +
		"ON  " +
		"  i.type = 'contentlet' " +
		"  AND i.inode = c.inode" +
		"  WHERE c.identifier = reference_id)	" +
		")";

	/*
	 * To insert permission references to menu links under a parent folder hierarchy, it only insert the references if the link
	 * does not have already a reference or individual permissions assigned
	 *
	 * Parameters
	 * 1. folder/host id the new references are going to point to
	 * 2. host the files belong to
	 * 3. path like to the folder hierarchy the files live under E.G /about/% (files under /about/)
	 * 4. same as 3
	 */
	private final String insertLinkReferencesSQL =
		(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL) || DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)?
				"insert into permission_reference (asset_id, reference_id, permission_type) " +
				"select ":
		 DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)?
				"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				"select permission_reference_seq.NEXTVAL, ":
				"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				"select nextval('permission_reference_seq'), ") +
		"   identifier.id, ?, '" + Link.class.getCanonicalName() + "' " +
		"	from identifier where identifier.id in (" +
		"		" + selectChildrenLinkSQL + " and" +
		"		identifier.id not in (" +
		"			select asset_id from permission_reference, folder ref_folder, identifier ii where" +
		"			reference_id = ref_folder.inode and ii.id=ref_folder.identifier and" +
		"			"+dotFolderPath+"(ii.parent_path,ii.asset_name) like ? and permission_type = '" + Link.class.getCanonicalName() + "'" +
		"		) and " +
		"		identifier.id not in (" +
		"			select inode_id from permission where " +
		"			permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		"		) " +
		"	) " +
		"and not exists (SELECT asset_id from permission_reference where asset_id = identifier.id)";

	/*
	 * To load content identifiers that are in the same tree/hierarchy of a parent host/folder
	 *
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
	private final String selectChildrenContentByPathSQL =
		"select distinct identifier.id from contentlet, inode, identifier, folder, identifier ffi where " +
		"contentlet.inode = inode.inode and identifier.id = contentlet.identifier and folder.identifier=ffi.id and  " +
		"identifier.host_inode = ? and contentlet.identifier <> identifier.host_inode and identifier.parent_path = "+dotFolderPath+"(ffi.parent_path,ffi.asset_name) and "+dotFolderPath+"(ffi.parent_path,ffi.asset_name) like ?";

	/*
	 * To load content identifiers that are children of a host and have inheritable permissions
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
	private final String selectChildrenContentWithIndividualPermissionsByPathSQL =
		selectChildrenContentByPathSQL + " and exists (select * from permission where inode_id = identifier.id and " +
		"permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "')";

	/*
	 * To load content identifiers that are of the type of a structure
	 *
	 * Parameters
	 * 1. The structure inode
	 */
	private final String selectChildrenContentByStructureSQL =
			" select distinct contentlet.identifier as id from contentlet " +
			" where contentlet.structure_inode = ?";

	/*
	 * To remove all permissions of content under a given parent folder
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
	private final String deleteContentPermissionsByPathSQL =
		"delete from permission where inode_id in " +
		"	(" + selectChildrenContentByPathSQL + ")";

	/*
	 * To delete all permission references on content under a given host/folder hierarchy
	 *
	 * Parameters
	 * 1. host the content belong to
	 * 2. path like to the folder hierarchy the content live under E.G /about/% (files under /about/)
	 * 3. host the content belong to
	 * 4. same as 2
	 */
	private final String deleteContentReferencesByPathSQL =
			"delete from permission_reference where exists (" +
			"	" + selectChildrenContentByPathSQL + " and " +
			"permission_type = '" + Contentlet.class.getCanonicalName() + "' and asset_id = identifier.id)";

	private final String deleteContentReferencesByPathOnAddSQL =
		"delete from permission_reference where exists (" +
		"	" + selectChildrenContentByPathSQL + " and " +
		"permission_type = '" + Contentlet.class.getCanonicalName() + "' and asset_id = contentlet.identifier) " +
		"and (reference_id in (" +
		"select distinct folder.inode " +
		"from folder, inode,identifier " +
		"where folder.inode = inode.inode " +
		"and folder.identifier = identifier.id and identifier.host_inode = ? " +
		"and ("+dotFolderPath+"(parent_path,asset_name) not like ? OR "+dotFolderPath+"(parent_path,asset_name) = ?) " +
		"and permission_type = 'com.dotmarketing.portlets.folders.model.Folder' " +
		"and reference_id = folder.inode" +
		") " +
		"OR EXISTS(SELECT c.inode " +
		"FROM contentlet c JOIN inode i " +
		"ON  " +
		"  i.type = 'contentlet' " +
		"  AND i.inode = c.inode" +
		"  WHERE c.identifier = reference_id)	" +
		")";

	/*
	 * To remove all permissions of content under a given parent folder
	 * Parameters
	 * 1. structure inode
	 */
	private final String deleteContentPermissionsByStructureSQL =
		"delete from permission where inode_id in " +
		"	(" + selectChildrenContentByStructureSQL + ")";

	/*
	 * To delete all permission references on content under a given structure
	 *
	 * Parameters
	 * 1. structure inode
	 */
	private final String deleteContentReferencesByStructureSQL =
		"delete from permission_reference where exists (" +
		" select contentlet.identifier from contentlet " +
		" where contentlet.structure_inode = ? " +
		" and permission_reference.asset_id = contentlet.identifier " +
		" and permission_reference.permission_type = 'com.dotmarketing.portlets.contentlet.model.Contentlet' " +
		" group by contentlet.identifier)";

	/*
	 * To insert permission references for content under a parent folder hierarchy, it only inserts the references if the content
	 * does not already have a reference or individual permissions assigned
	 *
	 * Parameters
	 * 1. folder/host id the new references are going to point to
	 * 2. host the files belong to
	 * 3. path like to the folder hierarchy the files live under E.G /about/% (files under /about/)
	 * 4. same as 3
	 */
	private final String insertContentReferencesByPathSQL =
		(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL) || DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)?
				"insert into permission_reference (asset_id, reference_id, permission_type) " +
				"select ":
		 DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)?
				"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				"select permission_reference_seq.NEXTVAL, ":
				"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				"select nextval('permission_reference_seq'), ") +
		"	identifier.id, ?, '" + Contentlet.class.getCanonicalName() + "' " +
		"	from identifier where identifier.id in (" +
		"		" + selectChildrenContentByPathSQL + " and" +
		"		identifier.id not in (" +
		"			select asset_id from permission_reference, folder ref_folder, identifier where" +
		"			reference_id = ref_folder.inode and identifier.id=folder.identifier and " +
		"			"+dotFolderPath+"(parent_path,asset_name) like ? and permission_type = '" + Contentlet.class.getCanonicalName() + "'" +
		"		) and " +
		"		identifier.id not in (" +
		"			select inode_id from permission where " +
		"			permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		"		) " +
		"	) " +
		"and not exists (SELECT asset_id from permission_reference where asset_id = identifier.id)";

	/*
	 * To insert permission references for content under a parent folder hierarchy, it only inserts the references if the content
	 * does not already have a reference or individual permissions assigned
	 *
	 * Parameters
	 * 1. structure id
	 */
	private final String insertContentReferencesByStructureSQL =
		(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL) || DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)?
				"insert into permission_reference (asset_id, reference_id, permission_type) " +
				"select ":
		 DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)?
				"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				"select permission_reference_seq.NEXTVAL, ":
				"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				"select nextval('permission_reference_seq'), ") +
		"	identifier.id, ?, '" + Contentlet.class.getCanonicalName() + "' " +
		"	from identifier where identifier.id in (" +
		"		" + selectChildrenContentByStructureSQL + " and " +
		"		identifier.id not in (" +
		"			select asset_id from permission_reference " +
		"		) and " +
		"		identifier.id not in (" +
		"			select inode_id from permission where " +
		"			permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		"		) " +
		"	) " +
		"and not exists (SELECT asset_id from permission_reference where asset_id = identifier.id)";



	/*
	 * To load structure identifiers that are in the same tree/hierarchy of a parent host/folder
	 *
	 * Parameters
	 * 1. path like to the folder hierarchy the structure lives under E.G /about/% (files under /about/)
	 * 2. The host id
	 * 3. The host id
	 */
	private final String selectChildrenStructureByPathSQL =
		"select distinct structure.inode from structure, inode where structure.inode = inode.inode  and ( " +
		"(structure.folder <> 'SYSTEM_FOLDER' AND exists(select folder.inode from folder,identifier where identifier.id=folder.identifier and structure.folder = folder.inode and "+dotFolderPath+"(parent_path,asset_name) like ?)) OR " +
		"(structure.host <> 'SYSTEM_HOST' AND structure.host = ?) OR " +
		"(structure.host = 'SYSTEM_HOST' AND exists (select inode from contentlet where title = 'System Host' AND inode = ?)))";


	/*
	 * To load structure identifiers that are in the same tree/hierarchy of a parent host/folder
	 *
	 * Parameters
	 * 1. path like to the folder hierarchy the structure lives under E.G /about/% (files under /about/)
	 * 2. The host id
	 * 3. The host id
	 */
	private final String selectChildrenStructureByPathSQLFolder =
		"select distinct structure.inode from structure, inode where structure.inode = inode.inode  and ( " +
		"(structure.folder <> 'SYSTEM_FOLDER' AND exists(select folder.inode from folder,identifier where identifier.id=folder.identifier and structure.folder = folder.inode and "+dotFolderPath+"(parent_path,asset_name) like ?)) OR " +
		"(structure.host = 'SYSTEM_HOST' AND exists (select inode from contentlet where title = 'System Host' AND inode = ?)))";

	/*
	 * To delete all permission references on a structure under a given host/folder hierarchy
	 *
	 * Parameters
	 * 1. path like to the folder hierarchy the structure lives under E.G /about/% (files under /about/)
	 * 2. host the structure belongs to
	 * 3. host the structure belongs to
	 * 4. host the structure belongs to
	 * 5. same as 1
	 */
	private final String deleteStructureReferencesByPathSQL =
			"delete from permission_reference where exists (" +
			"	" + selectChildrenStructureByPathSQL + " and asset_id = structure.inode and " +
			"permission_type = '" + Structure.class.getCanonicalName() + "' and reference_id not in (" +
			"select ref_folder.inode from folder ref_folder,identifier ref_ident where ref_folder.identifier = ref_ident.id " +
			"and ref_ident.host_inode = ? and "+dotFolderPath+"(ref_ident.parent_path,ref_ident.asset_name) like ?))";


	/*
	 * To delete all permission references on a structure under a given host/folder hierarchy
	 *
	 * Parameters
	 * 1. path like to the folder hierarchy the structure lives under E.G /about/% (files under /about/)
	 * 2. host the structure belongs to
	 * 3. host the structure belongs to
	 * 4. host the structure belongs to
	 * 5. same as 1
	 */
	private final String deleteStructureReferencesByPathSQLFolder =
			"delete from permission_reference where exists (" +
			"	" + selectChildrenStructureByPathSQLFolder + " and asset_id = structure.inode and " +
			"permission_type = '" + Structure.class.getCanonicalName() + "' and reference_id not in (" +
			"select ref_folder.inode from folder ref_folder,identifier ref_ident where ref_folder.identifier = ref_ident.id " +
			"and ref_ident.host_inode = ? and "+dotFolderPath+"(ref_ident.parent_path,ref_ident.asset_name) like ?))";


	private final String deleteStructureReferencesByPathOnAddSQL =
		"delete from permission_reference where exists(" +
		"	" + selectChildrenStructureByPathSQL + " and asset_id = structure.inode and " +
		"permission_type = '" + Structure.class.getCanonicalName() + "' and reference_id not in (" +
		"select ref_folder.inode from folder ref_folder,identifier ref_ident where ref_folder.identifier = ref_ident.id " +
		"and ref_ident.host_inode = ? and "+dotFolderPath+"(ref_ident.parent_path,ref_ident.asset_name) like ?)) " +
		"and (reference_id in (" +
		"select distinct folder.inode " +
		"from folder, inode,identifier " +
		"where folder.inode = inode.inode " +
		"and folder.identifier = identifier.id and identifier.host_inode = ? " +
		"and ("+dotFolderPath+"(parent_path,asset_name) not like ? OR "+dotFolderPath+"(parent_path,asset_name) = ?) " +
		"and permission_type = 'com.dotmarketing.portlets.folders.model.Folder' " +
		"and reference_id = folder.inode" +
		") " +
		"OR EXISTS(SELECT c.inode " +
		"FROM contentlet c JOIN inode i " +
		"ON  " +
		"  i.type = 'contentlet' " +
		"  AND i.inode = c.inode" +
		"  WHERE c.identifier = reference_id)	" +
		")";


	private final String deleteStructureReferencesByPathOnAddSQLFolder =
			"delete from permission_reference where exists(" +
			"	" + selectChildrenStructureByPathSQLFolder + " and asset_id = structure.inode and " +
			"permission_type = '" + Structure.class.getCanonicalName() + "' and reference_id not in (" +
			"select ref_folder.inode from folder ref_folder,identifier ref_ident where ref_folder.identifier = ref_ident.id " +
			"and ref_ident.host_inode = ? and "+dotFolderPath+"(ref_ident.parent_path,ref_ident.asset_name) like ?)) " +
			"and (reference_id in (" +
			"select distinct folder.inode " +
			"from folder, inode,identifier " +
			"where folder.inode = inode.inode " +
			"and folder.identifier = identifier.id and identifier.host_inode = ? " +
			"and ("+dotFolderPath+"(parent_path,asset_name) not like ? OR "+dotFolderPath+"(parent_path,asset_name) = ?) " +
			"and permission_type = 'com.dotmarketing.portlets.folders.model.Folder' " +
			"and reference_id = folder.inode" +
			") " +
			"OR EXISTS(SELECT c.inode " +
			"FROM contentlet c JOIN inode i " +
			"ON  " +
			"  i.type = 'contentlet' " +
			"  AND i.inode = c.inode" +
			"  WHERE c.identifier = reference_id)	" +
			")";


	/*
	 * To insert permission references for structure under a parent folder hierarchy, it only inserts the references if the structure
	 * does not already have a reference or individual permissions assigned
	 *
	 * Parameters
	 * 1. folder/host id the new references are going to point to
	 * 2. path like to the folder hierarchy the structure lives under E.G /about/% (files under /about/)
	 * 3. host the structure belongs to
	 * 4. host the structure belongs to
	 * 5. same as 2
	 */
	private final String insertStructureReferencesByPathSQL =
		(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL) || DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)?
				"insert into permission_reference (asset_id, reference_id, permission_type) " +
				"select ":
		 DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)?
				"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				"select permission_reference_seq.NEXTVAL, ":
				"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				"select nextval('permission_reference_seq'), ") +
		"	structure.inode, ?, '" + Structure.class.getCanonicalName() + "' " +
		"	from structure where structure.inode in (" +
		"		" + selectChildrenStructureByPathSQL + " and" +
		"		structure.inode not in (" +
		"			select asset_id from permission_reference, folder ref_folder, identifier where" +
		"			reference_id = ref_folder.inode and ref_folder.identifier=identifier.id and" +
		"			"+dotFolderPath+"(parent_path,asset_name) like ? and permission_type = '" + Structure.class.getCanonicalName() + "'" +
		"		) and " +
		"		structure.inode not in (" +
		"			select inode_id from permission where " +
		"			permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		"		) " +
		"	) " +
		"and not exists (SELECT asset_id from permission_reference where asset_id = structure.inode)";


	/*
	 * To insert permission references for structure under a parent folder hierarchy, it only inserts the references if the structure
	 * does not already have a reference or individual permissions assigned
	 *
	 * Parameters
	 * 1. folder/host id the new references are going to point to
	 * 2. path like to the folder hierarchy the structure lives under E.G /about/% (files under /about/)
	 * 3. host the structure belongs to
	 * 4. host the structure belongs to
	 * 5. same as 2
	 */
	private final String insertStructureReferencesByPathSQLFolder =
		(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL) || DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)?
				"insert into permission_reference (asset_id, reference_id, permission_type) " +
				"select ":
		 DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)?
				"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				"select permission_reference_seq.NEXTVAL, ":
				"insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				"select nextval('permission_reference_seq'), ") +
		"	structure.inode, ?, '" + Structure.class.getCanonicalName() + "' " +
		"	from structure where structure.inode in (" +
		"		" + selectChildrenStructureByPathSQLFolder + " and" +
		"		structure.inode not in (" +
		"			select asset_id from permission_reference, folder ref_folder, identifier where" +
		"			reference_id = ref_folder.inode and ref_folder.identifier=identifier.id and" +
		"			"+dotFolderPath+"(parent_path,asset_name) like ? and permission_type = '" + Structure.class.getCanonicalName() + "'" +
		"		) and " +
		"		structure.inode not in (" +
		"			select inode_id from permission where " +
		"			permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		"		) " +
		"	) " +
		"and not exists (SELECT asset_id from permission_reference where asset_id = structure.inode)";

	/*
	 * To remove all permissions of structures under a given parent folder
	 * Parameters
	 * 1. path like to the folder hierarchy the structure lives under E.G /about/% (files under /about/)
	 * 2. The host id
	 * 3. The host id
	 */
	private final String deleteStructurePermissionsByPathSQL =
		"delete from permission where inode_id in " +
		"	(" + selectChildrenStructureByPathSQL + ")";


	/*
	 * To remove all permissions of structures under a given parent folder
	 * Parameters
	 * 1. path like to the folder hierarchy the structure lives under E.G /about/% (files under /about/)
	 * 2. The host id
	 * 3. The host id
	 */
	private final String deleteStructurePermissionsByPathSQLFolder =
		"delete from permission where inode_id in " +
		"	(" + selectChildrenStructureByPathSQLFolder + ")";

	/*
	 * To load structure identifiers that are children of a host and have inheritable permissions
	 * Parameters
	 * 1. path like to the folder hierarchy the structure lives under E.G /about/% (files under /about/)
	 * 2. The host id
	 * 3. The host id
	 */
	private final String selectChildrenStructureWithIndividualPermissionsByPathSQL =
		selectChildrenStructureByPathSQL + " and exists (select * from permission where inode_id = inode.inode and " +
		"permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "')";


	/*
	 * To load structure identifiers that are children of a host and have inheritable permissions
	 * Parameters
	 * 1. path like to the folder hierarchy the structure lives under E.G /about/% (files under /about/)
	 * 2. The host id
	 * 3. The host id
	 */
	private final String selectChildrenStructureWithIndividualPermissionsByPathSQLFolder =
		selectChildrenStructureByPathSQLFolder + " and exists (select * from permission where inode_id = inode.inode and " +
		"permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "')";

	static {
		String[] listOfMasks = PermissionAPI.PERMISSION_TYPES;
		for(String mask : listOfMasks) {
	        for (Field f : PermissionAPI.class.getDeclaredFields()) {
	        	if(f.getName().equals(mask)){
	        		try {
						PERMISION_TYPES.put(mask, f.getInt(null));
					} catch (Exception e) {
						Logger.error(PermissionBitFactoryImpl.class,e.getMessage(),e);
					} 
	        	}
	        	
	        }
				
		}
	}

	/**
	 * @param permissionCache
	 */
	public PermissionBitFactoryImpl(PermissionCache permissionCache) {
		super();
		this.permissionCache = permissionCache;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionFactory#getPermission(java.lang.String)
	 */
	@Override
	protected Permission getPermission(String x) {
		try {
			Permission p = (Permission) new HibernateUtil(Permission.class).load(Long.parseLong(x));
			p.setBitPermission(true);
			return p;
		} catch (Exception e) {
			try {
				return (Permission) new HibernateUtil(Permission.class).load(x);
			} catch (DotHibernateException e1) {
				Logger.error(this, e1.getMessage(), e1);
				throw new DataAccessException(e1.getMessage(), e1);
			}
		}
	}

	@Override
	protected List<Permission> getInheritablePermissions(Permissionable permissionable, String type) throws DotDataException {
		List<Permission> permissions = getInheritablePermissions(permissionable, true);
		List<Permission> toReturn = new ArrayList<Permission>();
		for(Permission p : permissions) {
			if(p.getType().equals(type))
				toReturn.add(p);
		}
		return toReturn;
	}

	@Override
	protected List<Permission> getInheritablePermissions(Permissionable permissionable) throws DotDataException {
		return getInheritablePermissions(permissionable, true);
	}

	@Override
	protected List<Permission> getInheritablePermissions(Permissionable permissionable, boolean bitPermissions) throws DotDataException {
		List<Permission> bitPermissionsList = permissionCache.getPermissionsFromCache(permissionable.getPermissionId());
		if(bitPermissionsList == null || bitPermissionsList.size() == 0) {
			bitPermissionsList = loadPermissions(permissionable);
		}

		if(!bitPermissions)
			return convertToNonBitPermissions(filterOnlyInheritablePermissions(bitPermissionsList, permissionable.getPermissionId()));
		else
			return filterOnlyInheritablePermissions(bitPermissionsList, permissionable.getPermissionId());

	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionFactory#getPermissions(com.dotmarketing.beans.Inode)
	 */
	@Override
	protected List<Permission> getPermissions(Permissionable permissionable) throws DotDataException {
		return getPermissions(permissionable, true);
	}


	@Override
	protected List<Permission> getPermissions(Permissionable permissionable, boolean bitPermissions) throws DotDataException {
		return getPermissions(permissionable, bitPermissions, false);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionFactory#getPermissions(com.dotmarketing.beans.Inode)
	 */
	@Override
	protected List<Permission> getPermissions(Permissionable permissionable, boolean bitPermissions, boolean onlyIndividualPermissions) throws DotDataException {


		if (!InodeUtils.isSet(permissionable.getPermissionId())) return new ArrayList<Permission>();

		List<Permission> bitPermissionsList = permissionCache.getPermissionsFromCache(permissionable.getPermissionId());

		//No permissions in cache have to look for individual permissions or inherited permissions
		if(bitPermissionsList == null) {
			synchronized(permissionable.getPermissionId().intern()){
				//Checking individual permissions
				bitPermissionsList = permissionCache.getPermissionsFromCache(permissionable.getPermissionId());
				if(bitPermissionsList == null) {
					bitPermissionsList = loadPermissions(permissionable);
					permissionCache.addToPermissionCache(permissionable.getPermissionId(), bitPermissionsList);
				}
			}
		}

		bitPermissionsList = filterOnlyNonInheritablePermissions(bitPermissionsList, permissionable.getPermissionId());

		if(!bitPermissions)
			bitPermissionsList = convertToNonBitPermissions(bitPermissionsList);

		return getPermissions(permissionable, bitPermissions, onlyIndividualPermissions, false);

	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionFactory#getPermissions(com.dotmarketing.beans.Inode)
	 */
	@Override
	protected List<Permission> getPermissions(Permissionable permissionable, boolean bitPermissions, boolean onlyIndividualPermissions, boolean forceLoadFromDB) throws DotDataException {


		if (!InodeUtils.isSet(permissionable.getPermissionId())) return new ArrayList<Permission>();

		List<Permission> bitPermissionsList = null;

		if(!forceLoadFromDB)
			bitPermissionsList = permissionCache.getPermissionsFromCache(permissionable.getPermissionId());

		//No permissions in cache have to look for individual permissions or inherited permissions
		if(bitPermissionsList == null) {
			synchronized(permissionable.getPermissionId().intern()){

				if(!forceLoadFromDB)
					bitPermissionsList = permissionCache.getPermissionsFromCache(permissionable.getPermissionId());
				//Checking individual permissions
				if(bitPermissionsList == null) {
					bitPermissionsList = loadPermissions(permissionable);
					permissionCache.addToPermissionCache(permissionable.getPermissionId(), bitPermissionsList);
				}
			}
		}

		bitPermissionsList = filterOnlyNonInheritablePermissions(bitPermissionsList, permissionable.getPermissionId());

		if(!bitPermissions)
			bitPermissionsList = convertToNonBitPermissions(bitPermissionsList);

		return onlyIndividualPermissions?filterOnlyIndividualPermissions(bitPermissionsList, permissionable.getPermissionId()):bitPermissionsList;
	}

	@Override
	protected void removePermissions(Permissionable permissionable) throws DotDataException {
		removePermissions(permissionable, true);
	}

	@Override
	protected void removePermissions(Permissionable permissionable, boolean includeInheritablePermissions) throws DotDataException {

		boolean updatePermissionReferences = false;

		String permissionableId = permissionable.getPermissionId();
		List<Permission> permissions = null;
		if(includeInheritablePermissions && permissionable.isParentPermissionable())
			permissions = filterAssetOnlyPermissions(loadPermissions(permissionable), permissionableId) ;
		else {
			permissions = filterOnlyIndividualPermissions(loadPermissions(permissionable), permissionableId) ;
		}
		for(Permission p : permissions) {
			deletePermission(p);
			if(!p.getType().equals(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE))
				updatePermissionReferences = true;
		}
		if(updatePermissionReferences) {
			updatePermissionReferencesOnRemove(permissionable);
		}
		if(includeInheritablePermissions){
			 removePermissionsReference(permissionable);
		 }
		permissionCache.remove(permissionable.getPermissionId());

	}

	/*
	 * updates all permission references that are not pointing to the given permissionable but should,
	 * this happens when a new inheritable permission is added
	 *
	 */
	@SuppressWarnings("unchecked")
	private void updatePermissionReferencesOnAdd(Permissionable permissionable) throws DotDataException {

		String parentPermissionableId = permissionable.getPermissionId();

		boolean isHost = permissionable instanceof Host ||
		(permissionable instanceof Contentlet && ((Contentlet)permissionable).getStructure().getVelocityVarName().equals("Host"));
		boolean isFolder = permissionable instanceof Folder;
		boolean isCategory = permissionable instanceof Category;
		boolean isStructure = permissionable instanceof Structure;

		if(!isHost && !isFolder && !isCategory && !isStructure)
			return;

		HostAPI hostAPI = APILocator.getHostAPI();
		Host systemHost = hostAPI.findSystemHost();
		DotConnect dc = new DotConnect();


		List<Map<String, Object>> idsToClear = new ArrayList<Map<String, Object>>();
		List<Permission> permissions = filterOnlyInheritablePermissions(loadPermissions(permissionable), parentPermissionableId);
		for(Permission p : permissions) {

			if (isHost || isFolder) {

				Host parentHost = null;
				if(isFolder)
					try {
						parentHost = hostAPI.findParentHost((Folder)permissionable, APILocator.getUserAPI().getSystemUser(), false);
					} catch (DotSecurityException e) {
						Logger.error(this, e.getMessage(), e);
					}
				else
					parentHost = (Host)permissionable;

				String path = isFolder ? APILocator.getIdentifierAPI().find((Folder) permissionable).getPath() : "";

				// Only if permissions were updated to a host != to the system
				// host
				if (!permissionable.getPermissionId().equals(systemHost.getPermissionId())) {

					if (isHost && p.getType().equals(Template.class.getCanonicalName())) {
						// Find all host templates pointing to the system host
						// and update their references

						// Removing all references to the system host
						dc.setSQL(this.deleteTemplateReferencesSQL);
						dc.addParam(permissionable.getPermissionId());
						dc.loadResult();

						// Adding new references to the new host
						dc.setSQL(this.insertTemplateReferencesToAHostSQL);
						dc.addParam(permissionable.getPermissionId());
						dc.addParam(permissionable.getPermissionId());
						dc.loadResult();

						// Retrieving the list of templates changed to clear
						// their caches
						dc.setSQL(selectChildrenTemplateSQL);
						dc.addParam(permissionable.getPermissionId());
						idsToClear.addAll(dc.loadResults());

					} else if (isHost && p.getType().equals(Container.class.getCanonicalName())) {
						// Find all host containers pointing to the system host
						// and update their references

						// Removing all references to the system host
						dc.setSQL(this.deleteContainerReferencesSQL);
						dc.addParam(permissionable.getPermissionId());
						dc.loadResult();

						// Adding new references to the new host
						dc.setSQL(this.insertContainerReferencesToAHostSQL);
						dc.addParam(permissionable.getPermissionId());
						dc.addParam(permissionable.getPermissionId());
						dc.loadResult();

						// Retrieving the list of container changed to clear
						// their caches
						dc.setSQL(selectChildrenContainerSQL);
						dc.addParam(permissionable.getPermissionId());
						idsToClear.addAll(dc.loadResults());

					}else if (p.getType().equals(Folder.class.getCanonicalName())) {
						// Find all subfolders
						// Removing all references to the system host
						dc.setSQL(this.deleteSubfolderReferencesSQLOnAdd);
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						dc.addParam(path);
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						dc.addParam(path);
						dc.loadResult();

						// Adding new references to the new host
						dc.setSQL(this.insertSubfolderReferencesSQL);
						dc.addParam(permissionable.getPermissionId());
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						dc.addParam(path);
						dc.addParam(path + "%");
						dc.loadResult();

						// Retrieving the list of container changed to clear
						// their caches
						dc.setSQL(selectChildrenFolderSQL);
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						dc.addParam(path);
						idsToClear.addAll(dc.loadResults());

					} else if (p.getType().equals(HTMLPage.class.getCanonicalName())) {

						// Update html page references

						// Removing all references to the system host
						dc.setSQL(this.deleteHTMLPageReferencesOnAddSQL);
						// All the pages that belongs to the host
						dc.addParam(parentHost.getPermissionId());
						// Under any folder
						dc.addParam(path + "%");
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						dc.addParam(path);
						dc.loadResult();

						// Adding new references to the new host
						dc.setSQL(this.insertHTMLPageReferencesSQL);
						// Insert new references pointing to the host
						dc.addParam(permissionable.getPermissionId());
						// Under the same host
						dc.addParam(parentHost.getPermissionId());
						// Under any folder
						dc.addParam(path + "%");
						dc.addParam(path + "%");
						dc.loadResult();

						// Retrieving the list of pages changed to clear their
						// caches
						dc.setSQL(selectChildrenHTMLPageSQL);
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						idsToClear.addAll(dc.loadResults());

					} else if (p.getType().equals(File.class.getCanonicalName())) {

						// Find all files to update their references

						// Removing all references to the system host
						dc.setSQL(this.deleteFileReferencesOnAddSQL);
						// All the files that belongs to the host
						dc.addParam(parentHost.getPermissionId());
						// Which references are currently pointing to the system
						// host
						dc.addParam(path + "%");
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						dc.addParam(path);
						dc.loadResult();

						// Adding new references to the new host
						dc.setSQL(this.insertFileReferencesSQL);
						// Insert new references pointing to the host
						dc.addParam(permissionable.getPermissionId());
						// For all the pages that belong to the host
						dc.addParam(parentHost.getPermissionId());
						// Under any folder
						dc.addParam(path + "%");
						dc.addParam(path + "%");
						dc.loadResult();

						// Retrieving the list of files changed to clear their
						// caches
						dc.setSQL(selectChildrenFileSQL);
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						idsToClear.addAll(dc.loadResults());

					} else if (p.getType().equals(Link.class.getCanonicalName())) {
						// Find all files to update their references

						// Removing all references to the system host
						dc.setSQL(this.deleteLinkReferencesOnAddSQL);
						// All the links that belongs to the host
						dc.addParam(parentHost.getPermissionId());
						// Under any folder
						dc.addParam(path + "%");
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						dc.addParam(path);
						dc.loadResult();

						// Adding new references to the new host
						dc.setSQL(this.insertLinkReferencesSQL);
						// Insert new references pointing to the host
						dc.addParam(permissionable.getPermissionId());
						// For all the pages that belong to the host
						dc.addParam(parentHost.getPermissionId());
						// Under any folder
						dc.addParam(path + "%");
						dc.addParam(path + "%");
						dc.loadResult();

						// Retrieving the list of links changed to clear their
						// caches
						dc.setSQL(selectChildrenLinkSQL);
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						idsToClear.addAll(dc.loadResults());

					} else if (p.getType().equals(Contentlet.class.getCanonicalName())) {
						// Find all content

						// Removing all references to the system host
						dc.setSQL(this.deleteContentReferencesByPathOnAddSQL);
						// All the content that belongs to the host
						dc.addParam(parentHost.getPermissionId());
						// Under any folder
						dc.addParam(path + "%");
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						dc.addParam(path);
						dc.loadResult();

						// Adding new references to the new host
						dc.setSQL(this.insertContentReferencesByPathSQL);
						// Insert new references pointing to the host
						dc.addParam(permissionable.getPermissionId());
						// For all the content that belong to the host
						dc.addParam(parentHost.getPermissionId());
						// Under any folder
						dc.addParam(path + "%");
						dc.addParam(path + "%");
						dc.loadResult();

						// Retrieving the list of links changed to clear their
						// caches
						dc.setSQL(selectChildrenContentByPathSQL);
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						idsToClear.addAll(dc.loadResults());

					} else if (p.getType().equals(Structure.class.getCanonicalName())) {

						if(isHost){
							dc.setSQL(this.deleteStructureReferencesByPathOnAddSQL);
							dc.addParam(path + "%");
							dc.addParam(parentHost.getPermissionId());
							dc.addParam(parentHost.getPermissionId());
							dc.addParam(parentHost.getPermissionId());
							dc.addParam(path + "%");
							dc.addParam(parentHost.getPermissionId());
							dc.addParam(path + "%");
							dc.addParam(path);
							dc.loadResult();

							// Adding new references to the new host
							// Insert new references pointing to the host
							dc.setSQL(this.insertStructureReferencesByPathSQL);
							dc.addParam(permissionable.getPermissionId());
							dc.addParam(path + "%");
							dc.addParam(parentHost.getPermissionId());
							dc.addParam(parentHost.getPermissionId());
							dc.addParam(path + "%");
							dc.loadResult();

							// Retrieving the list of structures changed to clear
							// their caches
							dc.setSQL(this.selectChildrenStructureByPathSQL);
							dc.addParam(path + "%");
							dc.addParam(parentHost.getPermissionId());
							dc.addParam(parentHost.getPermissionId());
							idsToClear.addAll(dc.loadResults());
						}else{
							dc.setSQL(this.deleteStructureReferencesByPathOnAddSQLFolder);
							dc.addParam(path + "%");
							dc.addParam(parentHost.getPermissionId());
							dc.addParam(parentHost.getPermissionId());
							dc.addParam(path + "%");
							dc.addParam(parentHost.getPermissionId());
							dc.addParam(path + "%");
							dc.addParam(path);
							dc.loadResult();

							// Adding new references to the new host
							// Insert new references pointing to the host
							dc.setSQL(this.insertStructureReferencesByPathSQLFolder);
							dc.addParam(permissionable.getPermissionId());
							dc.addParam(path + "%");
							dc.addParam(parentHost.getPermissionId());
							dc.addParam(path + "%");
							dc.loadResult();

							// Retrieving the list of structures changed to clear
							// their caches
							dc.setSQL(this.selectChildrenStructureByPathSQLFolder);
							dc.addParam(path + "%");
							dc.addParam(parentHost.getPermissionId());
							idsToClear.addAll(dc.loadResults());

						}

					}
				} else {
					// If the system host we need to force all references of the
					// type of the permissionable
					dc.setSQL(selectPermissionReferenceSQL);
					dc.addParam(permissionable.getPermissionId());
					dc.addParam(p.getType());
					idsToClear.addAll(dc.loadResults());
				}
			} else if(isCategory) {
				Category cat = (Category) permissionable;
				CategoryAPI catAPI = APILocator.getCategoryAPI();
				User systemUser = APILocator.getUserAPI().getSystemUser();
				try {
					List<Category> children = catAPI.getCategoryTreeDown(cat, cat, systemUser, false);
					for(Category child : children) {
						dc.setSQL(updatePermissionReferenceByAssetIdSQL);
						dc.addParam(cat.getInode());
						dc.addParam(Category.class.getCanonicalName());
						dc.addParam(child.getInode());
						dc.loadResult();
						idsToClear.add(child.getMap());
					}
				} catch (DotSecurityException e) {
					Logger.error(PermissionBitFactoryImpl.class, e.getMessage(), e);
					throw new DotRuntimeException(e.getMessage(), e);
				}
			} else if (isStructure) {

				// Removing all references to the system host
				dc.setSQL(this.deleteContentReferencesByStructureSQL);
				// All the content that belongs to the host
				dc.addParam(permissionable.getPermissionId());
				dc.loadResult();

				dc.setSQL(selectChildrenContentByStructureSQL);
				dc.addParam(permissionable.getPermissionId());
				idsToClear.addAll(dc.loadResults());

			}


		}

		//Clearing the caches
		if(idsToClear.size() < 30) {
			for(Map<String, Object> idToClear: idsToClear) {
				String inode = (String)(idToClear.get("inode") != null?idToClear.get("inode"):idToClear.get("asset_id"));
				if(inode==null) inode=(String)idToClear.get("id");
				permissionCache.remove(inode);
			}
		} else {
			permissionCache.clearCache();
		}

	}

	/*
	 * updates all permission references that are pointing to the given permissionable if this
	 * permissionable no longer provides the inheritable permissions that the children require
	 * this happens when inheritable permissions have being removed from the given permissionable
	 */
	@SuppressWarnings("unchecked")
	private void updatePermissionReferencesOnRemove(Permissionable permissionable) throws DotDataException {

		DotConnect dc = new DotConnect();
		String query = loadPermissionReferencesByReferenceIdHSQL;
		HibernateUtil hu = new HibernateUtil(PermissionReference.class);
		hu.setQuery(query);
		hu.setParam(permissionable.getPermissionId());
		List<PermissionReference> permissionReferences = hu.list();
		Set<String> typesToLookFor = new HashSet<String>();

		for(PermissionReference ref: permissionReferences) {
			typesToLookFor.add(ref.getType());
		}

		Map<String, String> referenceReplacement = new HashMap<String, String>();

		Permissionable defaultReplacement = null;
		Permissionable parentPermissionable = permissionable;

		whileLoop: while(parentPermissionable != null) {
			defaultReplacement = parentPermissionable;
			String parentPermissionableId = parentPermissionable.getPermissionId();
			List<Permission> permissions = filterOnlyInheritablePermissions(loadPermissions(parentPermissionable), parentPermissionableId);
			for(Permission p : permissions) {
				if(typesToLookFor.contains(p.getType())) {
					referenceReplacement.put(p.getType(), p.getInode());
					typesToLookFor.remove(p.getType());
					if(typesToLookFor.size() == 0)
						break whileLoop;
				}
			}
			parentPermissionable = parentPermissionable.getParentPermissionable();
		}

		if(defaultReplacement != null)
			for(String type: typesToLookFor) {
				referenceReplacement.put(type, defaultReplacement.getPermissionId());
			}

		List<Map<String, String>> toClear = new ArrayList<Map<String,String>>();
		for(String type: referenceReplacement.keySet()) {

			dc.setSQL(selectPermissionReferenceSQL);
			dc.addParam(permissionable.getPermissionId());
			dc.addParam(type);
			for(Map<String, String> entry : (ArrayList<Map<String, String>>)dc.loadResults()) {
				toClear.add(entry);
			}

			String replacement = referenceReplacement.get(type);
			if(!replacement.equals(permissionable.getPermissionId())) {

				dc.setSQL(updatePermissionReferenceByReferenceIdSQL);
				dc.addParam(replacement);
				dc.addParam(type);
				dc.addParam(permissionable.getPermissionId());
				dc.loadResult();

			}

		}

		if(toClear.size() < 30) {
			for(Map<String, String> entry : toClear) {
				permissionCache.remove(entry.get("asset_id"));
			}
		} else {
			permissionCache.clearCache();
		}


	}

	/*
	 * updates all permission references that are pointing to the given permissionable if this
	 * permissionable no longer provides the inheritable permissions that the children require
	 * this happens when inheritable permissions have being removed from the given permissionable
	 */
	@SuppressWarnings("unchecked")
	private void clearReferencesCache(Permissionable permissionable) throws DotDataException {

		String query = loadPermissionReferencesByReferenceIdHSQL;
		HibernateUtil hu = new HibernateUtil(PermissionReference.class);
		hu.setQuery(query);
		hu.setParam(permissionable.getPermissionId());
		List<PermissionReference> permissionReferences = hu.list();

		if(permissionReferences.size() < 30) {
			for(PermissionReference reference : permissionReferences) {
				permissionCache.remove(reference.getAssetId());
			}
		} else {
			permissionCache.clearCache();
		}

	}

	private enum PersistResult {
		NEW, UPDATED, REMOVED, NOTHING;
	};

	private PersistResult persistPermission(Permission p) throws DotDataException {
		// Persisting changes
		Permission toPersist;
		boolean newPermission = false;
		boolean persist = true;
		if (p.isBitPermission()) {
			toPersist = findPermissionByInodeAndRole(p.getInode(), p.getRoleId(), p.getType());
			if (toPersist == null || toPersist.getId()== 0 ) {
				toPersist = p;
				newPermission = true;
			}

			if(toPersist.getPermission() == p.getPermission() && !newPermission) {
				persist = false;
			}

			toPersist.setPermission(p.getPermission());
		} else {
			toPersist = findPermissionByInodeAndRole(p.getInode(), p.getRoleId(), p.getType());
			if (toPersist == null || toPersist.getId()== 0 ) {
				toPersist = new Permission(p.getInode(), p.getRoleId(), 0);
				newPermission = true;
			}
			if((toPersist.getPermission() | p.getPermission()) == toPersist.getPermission() && !newPermission)
				persist = false;
			toPersist.setPermission(toPersist.getPermission() | p.getPermission());
		}

		if(toPersist.getPermission() == 0 && toPersist.getId() > 0) {
			deletePermission(toPersist);
			return PersistResult.REMOVED;
		} else if(toPersist.getPermission() != 0 && persist) {
			if(newPermission)
				HibernateUtil.save(toPersist);
			else
				HibernateUtil.saveOrUpdate(toPersist);
			return newPermission?PersistResult.NEW:PersistResult.UPDATED;
		}
		return PersistResult.NOTHING;
	}

	private void removePermissionsReference(Permissionable permissionable) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(this.deletePermissionReferenceSQL);
		dc.addParam(permissionable.getPermissionId());
		dc.addParam(permissionable.getPermissionId());
		dc.loadResult();
	}

	private void removePermissionableReference(Permissionable permissionable) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(this.deletePermissionableReferenceSQL);
		dc.addParam(permissionable.getPermissionId());
		dc.loadResult();
	}

	private boolean containsPermission(List<Permission> permissions, Permission permission) {
        for(Permission p : permissions) {
                if(p.getInode().equals(permission.getInode()) && p.getRoleId().equals(permission.getRoleId())
                                && p.getType().equals(permission.getType()) && p.getPermission()!=0)
                        return true;
        }
        return false;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionFactory#assignPermissions
	 */
	@Override
	protected void assignPermissions(List<Permission> permissions, Permissionable permissionable) throws DotDataException {

		boolean updateReferencesOnDelete = false;
		boolean updateReferencesOnAdd = false;
		boolean removePermissionableReference = false;
		boolean clearReferencesCache = false;

		List<Permission> currentPermissions = filterAssetOnlyPermissions(loadPermissions(permissionable), permissionable.getPermissionId());

		for(Permission cp : currentPermissions) {
            if(containsPermission(permissions, cp)) {
            	deletePermission(cp);
            	if(!cp.isIndividualPermission())
            		updateReferencesOnDelete = true;
            }
		}

		for(Permission p: permissions) {
			PersistResult result = persistPermission(p);
			if (!p.isIndividualPermission()) {
				switch(result) {
				case NEW:
					updateReferencesOnAdd = true;
					break;
				case REMOVED:
					updateReferencesOnDelete = true;
					break;
				case UPDATED:
					clearReferencesCache = true;
					break;
				}
			} else {
				removePermissionableReference = true;
			}
		}
		if(updateReferencesOnDelete)
			updatePermissionReferencesOnRemove(permissionable);
		if(updateReferencesOnAdd)
			updatePermissionReferencesOnAdd(permissionable);
		if(removePermissionableReference)
			removePermissionableReference(permissionable);
		if(clearReferencesCache)
			clearReferencesCache(permissionable);

		permissionCache.remove(permissionable.getPermissionId());

		if(permissionable instanceof Structure) {
			ContentletAPI contAPI = APILocator.getContentletAPI();
			contAPI.refresh((Structure)permissionable);
		} else if(permissionable instanceof Contentlet) {
			ContentletAPI contAPI = APILocator.getContentletAPI();
			contAPI.refresh((Contentlet)permissionable);
		}
		//DOTCMS-4959
		if(permissionable instanceof Host && ((Host)permissionable).isSystemHost()){
			ContentletAPI contAPI = APILocator.getContentletAPI();
			contAPI.refresh(((Host)permissionable).getStructure());
			//http://jira.dotmarketing.net/browse/DOTCMS-5768
			permissionCache.clearCache();
		}
	}


	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionFactory#savePermission(com.dotmarketing.beans.Permission)
	 */
	@Override
	protected Permission savePermission(Permission p, Permissionable permissionable) throws DotDataException {

		if(!p.getInode().equals(permissionable.getPermissionId()))
			throw new DotDataException("You cannot update permissions of a different permissionable id than the one you are passing to the method");

		PersistResult result = persistPermission(p);
		if (!p.isIndividualPermission()) {
			switch(result) {
			case NEW:
				updatePermissionReferencesOnAdd(permissionable);
				break;
			case REMOVED:
				updatePermissionReferencesOnRemove(permissionable);
				break;
			case UPDATED:
				clearReferencesCache(permissionable);
				break;
			}
		} else {
			if(result == PersistResult.NEW) {
				removePermissionsReference(permissionable);
			}
		}

		permissionCache.remove(permissionable.getPermissionId());

		if(permissionable instanceof Structure) {
			ContentletAPI contAPI = APILocator.getContentletAPI();
			contAPI.refresh((Structure)permissionable);
		} else if(permissionable instanceof Contentlet) {
			ContentletAPI contAPI = APILocator.getContentletAPI();
			((Contentlet)permissionable).setLowIndexPriority(true);
			contAPI.refresh((Contentlet)permissionable);
		}

		return findPermissionByInodeAndRole(p.getInode(), p.getRoleId(), p.getType());
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionFactory#getUsers(com.dotmarketing.beans.Inode, int, java.lang.String, int, int)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected List<User> getUsers(Permissionable permissionable, int permissionType, String filter, int start, int limit) {
    	try
    	{

    		RoleAPI roleAPI = APILocator.getRoleAPI();

    		List<Permission> allPermissions = getPermissions(permissionable);
    		List<String> roleIds = new ArrayList<String>();
    		for(Permission p : allPermissions) {
    			if(p.matchesPermission(permissionType)) {
    				roleIds.add(p.getRoleId());
    			}
    		}
    		roleIds.add(roleAPI.loadCMSAdminRole().getId());

    		StringBuilder roleIdsSB = new StringBuilder();
    		boolean first = true;
    		for(String roleId: roleIds) {
    			if(!first)
    				roleIdsSB.append(",");
    			roleIdsSB.append("'" + roleId + "'");
    			first=false;

    		}

			ArrayList<User> users = new ArrayList<User>();

			DotConnect dotConnect = new DotConnect();
			String userFullName = DotConnect.concat(new String[] { "user_.firstName", "' '", "user_.lastName" });

			StringBuffer baseSql = new StringBuffer("select distinct (user_.userid), user_.firstName || ' ' || user_.lastName ");
			baseSql.append(" from user_, users_cms_roles where");
			baseSql.append(" user_.companyid = ? and user_.userid <> 'system' ");
			baseSql.append(" and users_cms_roles.role_id in (" + roleIdsSB.toString() + ")");
			baseSql.append(" and user_.userId = users_cms_roles.user_id ");

			boolean isFilteredByName = UtilMethods.isSet(filter);
			if (isFilteredByName) {
				baseSql.append(" and lower(");
				baseSql.append(userFullName);
				baseSql.append(") like ?");
			}
			baseSql.append(" order by ");
			baseSql.append(userFullName);

			String sql = baseSql.toString();
			dotConnect.setSQL(sql);
			Logger.debug(PermissionBitFactoryImpl.class, "::getUsers -> query: " + dotConnect.getSQL());

			dotConnect.addParam(PublicCompanyFactory.getDefaultCompanyId());
			if (isFilteredByName) {
				dotConnect.addParam("%" + filter.toLowerCase() + "%");
			}

			if (start > -1)
				dotConnect.setStartRow(start);
			if (limit > -1)
				dotConnect.setMaxRows(limit);

			ArrayList<Map<String, Object>> results = dotConnect.loadResults();

			for (int i = 0; i < results.size(); i++) {
				Map<String, Object> hash = (Map<String, Object>) results.get(i);
				String userId = (String) hash.get("userid");
				users.add(APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false));
			}

			return users;
		}
    	catch(Exception ex)
    	{
    		Logger.error(PermissionBitFactoryImpl.class, ex.toString(), ex);
    		throw new DotRuntimeException(ex.getMessage(), ex);
    	}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionFactory#getUserCount(com.dotmarketing.beans.Inode, int, java.lang.String)
	 */
	@Override
	protected int getUserCount(Permissionable permissionable, int permissionType, String filter) {
    	try
    	{
    		RoleAPI roleAPI = APILocator.getRoleAPI();

    		List<Permission> allPermissions = getPermissions(permissionable);
    		List<String> roleIds = new ArrayList<String>();
    		for(Permission p : allPermissions) {
    			if(p.matchesPermission(permissionType)) {
    				roleIds.add(p.getRoleId());
    			}
    		}
    		roleIds.add(roleAPI.loadCMSAdminRole().getId());

    		StringBuilder roleIdsSB = new StringBuilder();
    		boolean first = true;
    		for(String roleId: roleIds) {
    			if(!first)
    				roleIdsSB.append(",");
    			roleIdsSB.append("'" + roleId + "'");
    			first=false;

    		}

			DotConnect dotConnect = new DotConnect();
			String userFullName = DotConnect.concat(new String[] { "user_.firstName", "' '", "user_.lastName" });

			StringBuffer baseSql = new StringBuffer("select count(distinct user_.userid) as total ");
			baseSql.append(" from user_, users_cms_roles where");
			baseSql.append(" user_.companyid = ? and user_.userid <> 'system' ");
			baseSql.append(" and users_cms_roles.role_id in (" + roleIdsSB.toString() + ") ");
			baseSql.append(" and user_.userId = users_cms_roles.user_id ");

			boolean isFilteredByName = UtilMethods.isSet(filter);
			if (isFilteredByName) {
				baseSql.append(" and lower(");
				baseSql.append(userFullName);
				baseSql.append(") like ?");
			}

			String sql = baseSql.toString();
			dotConnect.setSQL(sql);
			Logger.debug(PermissionBitFactoryImpl.class, "::getUsers -> query: " + dotConnect.getSQL());

			dotConnect.addParam(PublicCompanyFactory.getDefaultCompanyId());
			if (isFilteredByName) {
				dotConnect.addParam("%" + filter.toLowerCase() + "%");
			}

    		return dotConnect.getInt("total");
    	}
    	catch(Exception ex)
    	{
    		Logger.error(PermissionBitFactoryImpl.class, ex.toString(), ex);
    		throw new DotRuntimeException(ex.getMessage(), ex);
    	}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionFactory#getPermissionsFromCache(com.dotmarketing.beans.Permissionable)
	 */
	@Override
	protected List<Permission> getPermissionsFromCache(String permissionableId) {
		List<Permission> l = null;
		l = permissionCache.getPermissionsFromCache(permissionableId);
		return l;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionFactory#getPermissions(java.util.List<P>)
	 */
	@Override
	protected Map<Permissionable, List<Permission>> getPermissions(List<Permissionable> permissionables) throws DotDataException, DotSecurityException {

		return getPermissions(permissionables, true);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionFactory#getPermissions(java.util.List<P>, boolean)
	 */
	@Override
	protected Map<Permissionable, List<Permission>> getPermissions(List<Permissionable> permissionables, boolean bitPermission)
		throws DotDataException, DotSecurityException {

		Map<Permissionable, List<Permission>> result = new HashMap<Permissionable, List<Permission>>();

		for(Permissionable p : permissionables) {
			List<Permission> permission = getPermissions(p, bitPermission);
			result.put(p, permission);
		}

		return result;

	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionFactory#removePermissionsByRole(java.lang.String)
	 */
	public void removePermissionsByRole(String roleId) {

		try {
			DotConnect db = new DotConnect();
			db.setSQL("select * from permission where roleid='"+roleId+"'");

			/*removing from the bd*/
			db.setSQL("delete from permission where roleid='"+roleId+"'");
			db.loadResult();

			permissionCache.clearCache();
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DataAccessException (e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionFactory#getPermissionTypes()
	 */
	@Override
	protected Map<String, Integer> getPermissionTypes() {
		return PERMISION_TYPES;
	}


	@SuppressWarnings("unchecked")
	@Override
	void updateOwner(Permissionable asset, String ownerId)
			throws DotDataException {

		String permissionId = asset.getPermissionId();

		DotConnect dc = new DotConnect();
		if (ownerId != null && ownerId.startsWith("user-")) {
			ownerId = ownerId.substring(5, ownerId.length());
		}
		String updateIdentifierSql = "update inode set owner = ? "
				+ "where inode = ? ";

		dc.setSQL(updateIdentifierSql);
		dc.addParam(ownerId);
		dc.addParam(permissionId);

		dc.loadResult();

		asset.setOwner(ownerId);

		/*dc.setSQL("select inode from inode where identifier = ?");
		dc.addParam(permissionId);*/


		List<HashMap<String, String>> inodes = new ArrayList<HashMap<String, String>>();
		String assetType ="";
		dc.setSQL("Select asset_type from identifier where id =?");
		dc.addParam(permissionId);
		ArrayList assetResult = dc.loadResults();
		if(assetResult.size()>0){
			assetType = (String) ((Map)assetResult.get(0)).get("asset_type");
		}
		if(UtilMethods.isSet(assetType)){
			dc.setSQL("select i.inode, type from inode i,"+assetType+" a where i.inode = a.inode and a.identifier = ?");
			dc.addParam(permissionId);
			inodes= dc.loadResults();
		}

		StringBuilder inodeCondition = new StringBuilder(128);
		inodeCondition.ensureCapacity(32);
		inodeCondition.append("");

		for (HashMap<String, String> inode : inodes) {
			if (0 < inodeCondition.length())
				inodeCondition.append(", " + inode.get("inode"));
			else
				inodeCondition.append(inode.get("inode"));
		}

		String updateVersionsSql = "update inode set owner = ? where inode in ('"
				+ inodeCondition + "')";
		dc.setSQL(updateVersionsSql);
		dc.addParam(ownerId);
		dc.loadResult();

		if (InodeUtils.isSet(permissionId) && asset instanceof Versionable) {
			CacheLocator.getIdentifierCache().removeFromCacheByVersionable((Versionable)asset);
		}

		if(asset instanceof Contentlet) {
			ContentletAPI contAPI = APILocator.getContentletAPI();
			contAPI.refresh((Contentlet)asset);
		}

	}

	@Override
	protected int maskOfAllPermissions () {
		int result = 0;
		for(Integer mask : PERMISION_TYPES.values()) {
			result = result | mask;
		}
		return result;
	}

	//Private utility methods

	/**
	 * This method returns a bit permission object based on the given inode and roleId
	 * @param p permission
	 * @return boolean
	 * @version 1.7
	 * @since 1.0
	 */
	private Permission findPermissionByInodeAndRole (String inode, String roleId, String permissionType) {
		try {
			HibernateUtil persistanceService = new HibernateUtil(Permission.class);

			persistanceService.setQuery("from inode in class com.dotmarketing.beans.Permission where inode_id = ? and roleid = ? " +
					"and permission_type = ?");
			persistanceService.setParam(inode);
			persistanceService.setParam(roleId);
			persistanceService.setParam(permissionType);
			return (Permission) persistanceService.load();
		} catch (DotHibernateException e) {
			throw new DataAccessException(e.getMessage(), e);
		}

	}

	/**
	 * This method return true if exists in db that permission object
	 * @param p permission
	 * @return boolean
	 * @version 1.7
	 * @since 1.0
	 */
	private boolean permissionExists(Permission p) {
		HibernateUtil persistanceService = new HibernateUtil(Permission.class);
		try {
			if (p.isBitPermission()) {
				Permission permission = (Permission) persistanceService.load(p.getId());
				if (permission != null) {
					return true;
				}
			} else {
				Permission permission = findPermissionByInodeAndRole(p.getInode(), p.getRoleId(), p.getType());
				if (permission != null && permission.getId() > 0 && ((permission.getPermission() & p.getPermission()) > 0)) {
					return true;
				}
			}
			return false;
		} catch (DotHibernateException e) {
			throw new DataAccessException(e.getMessage(), e);
		}
	}

	/**
	 * This method let you convert a list of bit permission to the old non bit kind of permission, so you
	 * end up with a longer list
	 * @param p permission
	 * @return boolean
	 * @version 1.7
	 * @since 1.7
	 */
	private List<Permission> convertToNonBitPermissions (List<Permission> bitPermissionsList) {
		Set<Permission> permissionsSet = new LinkedHashSet<Permission>();

		for(Permission p : bitPermissionsList) {
			if(p.isBitPermission()) {
				for(String mask : PERMISION_TYPES.keySet()) {
					if((p.getPermission() & PERMISION_TYPES.get(mask)) > 0){
						permissionsSet.add(new Permission(p.getType(), p.getInode(), p.getRoleId(), PERMISION_TYPES.get(mask)));
					}
				}
			} else {
				permissionsSet.add(p);
			}
		}
		return new ArrayList<Permission> (permissionsSet);

	}

	/**
	 * This method let you convert a list of non bit permission to the new  bit kind of permission, so you should
	 * end up with a compressed list
	 * @param p permission
	 * @return boolean
	 * @version 1.7
	 * @since 1.7
	 */
	@SuppressWarnings("unused")
	private List<Permission> convertToBitPermissions (List<Permission> nonbitPermissionsList) {

		Map<String, Permission> tempList = new HashMap<String, Permission>();

		for(Permission p : nonbitPermissionsList) {
			if(!p.isBitPermission()) {
				Permission pt = tempList.get(p.getInode() + "-" + p.getRoleId());
				if(pt == null)
					pt = new Permission(p.getInode(), p.getRoleId(), p.getPermission());
				else
					pt = new Permission(p.getInode(), p.getRoleId(), pt.getPermission() | p.getPermission());
				tempList.put(pt.getInode() + "-" + pt.getRoleId(), pt);
			} else {
				tempList.put(p.getInode() + "-" + p.getRoleId(), p);
			}
		}
		return new ArrayList<Permission> (tempList.values());

	}


	private void deletePermission(Permission p) {
		if (p != null && permissionExists(p)) {

			try{

				HibernateUtil persistenceService = new HibernateUtil();

				if(p.isBitPermission()) {
					Permission pToDel = null;

					pToDel = findPermissionByInodeAndRole(p.getInode(), p.getRoleId(), p.getType());
					if( pToDel != null )
					{
						HibernateUtil.delete(pToDel);
						Logger.debug(this.getClass(), String.format("deletePermission: %s deleted successful!", p.toString()));
						permissionCache.remove(pToDel.getInode());
					}
					else
					{
						// This should not happen unless it's with the cms admin role which we synthetically generate, but just in case... log it
						Logger.debug(this.getClass(), String.format("deletePermission: Trying to load a non-existent permission (%s)", p.toString()));
					}
				} else {
					persistenceService.setQuery("from inode in class com.dotmarketing.beans.Permission where inode_id = ? and roleid = ? and " +
							"permission_type = ?");
					persistenceService.setParam(p.getInode());
					persistenceService.setParam(p.getRoleId());
					persistenceService.setParam(p.getType());
					Permission bitPermission = (Permission) persistenceService.load();
					if (bitPermission != null) {
						bitPermission.setPermission((bitPermission.getPermission() ^ p.getPermission()) & bitPermission.getPermission());
						if (bitPermission.getPermission() == 0)
							HibernateUtil.delete(bitPermission);
						else
							HibernateUtil.save(bitPermission);
					}
				}

			}catch (DotHibernateException dhe) {
				String cause = String.format("deletePermission: Unable to delete %s in database", p.toString());
				Logger.error(this.getClass(), cause, dhe);
				throw new DataAccessException(cause, dhe);
			}
		}
		else {
			String cause = String.format("deletePermission: %s not found", p.toString());
			Logger.debug(this.getClass(), cause);
		}
	}

	@SuppressWarnings("unchecked")
	private List<Permission> loadPermissions(Permissionable permissionable) throws DotDataException {

		if(permissionable == null || ! UtilMethods.isSet(permissionable.getPermissionId())){
			throw new DotDataException("Invalid Permissionable passed in. permissionable:" + permissionable.getPermissionId());
		}

		HibernateUtil persistenceService = new HibernateUtil(Permission.class);
		persistenceService.setSQLQuery(loadPermissionHSQL);
		persistenceService.setParam(permissionable.getPermissionId());
		persistenceService.setParam(permissionable.getPermissionId());
		List<Permission> bitPermissionsList = (List<Permission>) persistenceService.list();

		for(Permission p : bitPermissionsList) {
			p.setBitPermission(true);
		}
		//Check permission reference
		if(bitPermissionsList.size() == 0) {
			synchronized(permissionable.getPermissionId().intern()) {
				//Need to determine who this asset should inherit from
				String type = permissionable.getClass().getCanonicalName();
				if(permissionable instanceof Host ||
						(permissionable instanceof Contentlet &&
								((Contentlet)permissionable).getStructure() != null &&
								((Contentlet)permissionable).getStructure().getVelocityVarName() != null &&
								((Contentlet)permissionable).getStructure().getVelocityVarName().equals("Host"))){
					type = Host.class.getCanonicalName();
				}else if(permissionable instanceof FileAsset ||
				        (permissionable instanceof Contentlet &&
				         ((Contentlet)permissionable).getStructure().getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET)){
				    type = File.class.getCanonicalName();
				}else if(permissionable instanceof Event){
					type = Contentlet.class.getCanonicalName();
				}else if(permissionable instanceof Identifier){
					Permissionable perm = InodeFactory.getInode(permissionable.getPermissionId(), Inode.class);
					Logger.error(this, "PermissionBitFactoryImpl :  loadPermissions Method : was passed an identifier. This is a problem. We will get inode as a fallback but this should be reported");
					if(perm!=null){
						if(perm instanceof HTMLPage){
							type = HTMLPage.class.getCanonicalName();
						}else if(perm instanceof Container){
							type = Container.class.getCanonicalName();
						}else if(perm instanceof File){
							type = File.class.getCanonicalName();
						}else if(perm instanceof Folder){
							type = Folder.class.getCanonicalName();
						}else if(perm instanceof Link){
							type = Link.class.getCanonicalName();
						}else if(perm instanceof Template){
							type = Template.class.getCanonicalName();
						}else if(perm instanceof Structure){
							type = Structure.class.getCanonicalName();
						}else if(perm instanceof Contentlet || perm instanceof Event){
							type = Contentlet.class.getCanonicalName();
						}
					}
				}
				Permissionable parentPermissionable = permissionable.getParentPermissionable();
				Permissionable newReference = null;
				List<Permission> inheritedPermissions = new ArrayList<Permission>();
				while(parentPermissionable != null) {
					newReference = parentPermissionable;
					inheritedPermissions = getInheritablePermissions(parentPermissionable, type);
					if(inheritedPermissions.size() > 0) {
						break;
					}
					parentPermissionable = parentPermissionable.getParentPermissionable();
				}
				HostAPI hostAPI = APILocator.getHostAPI();
				if(newReference == null)
					newReference = hostAPI.findSystemHost();
				boolean localTransaction = false;
				try{
					try{
						localTransaction =	 DbConnectionFactory.getConnection().getAutoCommit();
					}
					catch(Exception e){
						throw new DotDataException(e.getMessage());
					}
					if(localTransaction){
						HibernateUtil.startTransaction();
					}
					DotConnect dc1 = new DotConnect();
					dc1.setSQL("SELECT inode FROM inode WHERE inode = ?");
					dc1.addParam(permissionable.getPermissionId());
					List<Map<String, Object>> l = dc1.loadObjectResults();
					if(l != null && l.size()>0){
						dc1.setSQL(deletePermissionableReferenceSQL);
						dc1.addParam(permissionable.getPermissionId());
						dc1.loadResult();
						dc1.setSQL(insertPermissionReferenceSQL);
						dc1.addParam(permissionable.getPermissionId());
						dc1.addParam(newReference.getPermissionId());
						dc1.addParam(type);
	//					dc1.addParam(permissionable.getPermissionId());
						dc1.loadResult();
					}
				} catch(Exception exception){
					if(permissionable != null && newReference != null){
						Logger.warn(this.getClass(), "Failed to insert Permission Ref. Usually not a problem. Permissionable:" + permissionable.getPermissionId() + " Parent : " + newReference.getPermissionId() + " Type: " + type);
					}
					else{
						Logger.warn(this.getClass(), "Failed to insert Permission Ref. Usually not a problem. Setting Parent Permissions to null value: Permissionable:" + permissionable + " Parent:" + newReference + " Type: " + type);
					}
					Logger.debug(this.getClass(), "Failed to insert Permission Ref. : " + exception.toString(), exception);
					if(localTransaction){
						HibernateUtil.rollbackTransaction();
					}else{
						throw new DotDataException(exception.getMessage(), exception);
					}
				}
				if(localTransaction){
					HibernateUtil.commitTransaction();
				}
				bitPermissionsList = inheritedPermissions;
			}
		}

		return bitPermissionsList;

	}

	private List<Permission> filterOnlyNonInheritablePermissions(List<Permission> permissions, String permissionableId) {
		List<Permission> filteredList = new ArrayList<Permission>();
		for(Permission p: permissions) {
			if((p.isIndividualPermission() && p.getInode().equals(permissionableId)) || !p.getInode().equals(permissionableId))
				filteredList.add(p);
		}
		return filteredList;
	}

	private List<Permission> filterOnlyInheritablePermissions(List<Permission> permissions, String permissionableId) {
		List<Permission> filteredList = new ArrayList<Permission>();
		for(Permission p: permissions) {
			if(!p.isIndividualPermission() && p.getInode().equals(permissionableId))
				filteredList.add(p);
		}
		return filteredList;
	}

	private List<Permission> filterOnlyInheritablePermissions(List<Permission> permissions, String permissionableId, String type) {
		List<Permission> filteredList = new ArrayList<Permission>();
		for(Permission p: permissions) {
			if(!p.isIndividualPermission() && p.getInode().equals(permissionableId) && p.getType().equals(type))
				filteredList.add(p);
		}
		return filteredList;
	}

	private Permission filterInheritablePermission(List<Permission> permissions, String permissionableId, String type, String roleId) {
		for(Permission p: permissions) {
			if(!p.isIndividualPermission() && p.getInode().equals(permissionableId) && p.getType().equals(type) && p.getRoleId().equals(roleId))
				return p;
		}
		return null;
	}

	private List<Permission> filterOnlyIndividualPermissions(List<Permission> permissions, String permissionableId) {
		List<Permission> filteredList = new ArrayList<Permission>();
		for(Permission p: permissions) {
			if(p.isIndividualPermission() && p.getInode().equals(permissionableId))
				filteredList.add(p);
		}
		return filteredList;
	}

	private List<Permission> filterAssetOnlyPermissions(List<Permission> permissions, String permissionableId) {
		List<Permission> filteredList = new ArrayList<Permission>();
		for(Permission p: permissions) {
			if(p.getInode().equals(permissionableId))
				filteredList.add(p);
		}
		return filteredList;
	}

	@SuppressWarnings("unchecked")
	@Override
	List<Permission> getPermissionsByRole(Role role, boolean onlyFoldersAndHosts, boolean bitPermissions) throws DotDataException {


		StringBuilder query = new StringBuilder();
		query.append("select {permission.*} from permission ");
		query.append(" where permission.roleid = ? ");
		if(onlyFoldersAndHosts) {
			query.append(" and (permission.inode_id in " +
					"(select contentlet.identifier from contentlet, inode cont_inode where cont_inode.inode = contentlet.inode and " +
					"contentlet.structure_inode = ?) " +
					"or permission.inode_id in (select inode from folder)) ");
		}

		HibernateUtil persistenceService = new HibernateUtil(Permission.class);
		persistenceService.setSQLQuery(query.toString());
		persistenceService.setParam(role.getId());
		if(onlyFoldersAndHosts) {
			Structure hostStructure = StructureCache.getStructureByVelocityVarName("Host");
			persistenceService.setParam(hostStructure.getInode());
		}


		List<Permission> bitPermissionsList = (List<Permission>) persistenceService.list();

		for(Permission p : bitPermissionsList) {
			p.setBitPermission(true);
		}

		if(bitPermissions)
			return bitPermissionsList;
		else
			return convertToNonBitPermissions(bitPermissionsList);
	}

	@SuppressWarnings("unchecked")
	@Override
	void resetPermissionsUnder(Permissionable permissionable) throws DotDataException {

		if(!permissionable.isParentPermissionable())
			return;

		boolean isHost = permissionable instanceof Host ||
			(permissionable instanceof Contentlet && ((Contentlet)permissionable).getStructure().getVelocityVarName().equals("Host"));
		boolean isFolder = permissionable instanceof Folder;
		boolean isStructure = permissionable instanceof Structure;
		boolean isCategory = permissionable instanceof Category;

		DotConnect dc = new DotConnect();
		HostAPI hostAPI = APILocator.getHostAPI();
		User systemUser = APILocator.getUserAPI().getSystemUser();

		//Search all children remove individual permissions and permission references and make them point to this permissionable
		List<Map<String, String>> idsToClear = new ArrayList<Map<String,String>>();
		if(isHost || isFolder) {

			Permissionable host = null;
			try {
				host = isHost?permissionable:hostAPI.findParentHost((Folder)permissionable, systemUser, false);
			} catch (DotSecurityException e) {
				Logger.error(PermissionBitFactoryImpl.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
			Folder folder = isFolder?(Folder)permissionable:null;


			if(isHost) {
				//Removing permissions and permission references for all children templates
				dc.setSQL(deleteTemplateReferencesSQL);
				dc.addParam(host.getPermissionId());
				dc.loadResult();
				dc.setSQL(deleteTemplatePermissionsSQL);
				dc.addParam(host.getPermissionId());
				dc.loadResult();
				//Pointing the children templates to reference the current host
				dc.setSQL(insertTemplateReferencesToAHostSQL);
				dc.addParam(host.getPermissionId());
				dc.addParam(host.getPermissionId());
				dc.loadResult();
				//Retrieving the list of templates to clear their caches later
				dc.setSQL(selectChildrenTemplateSQL);
				dc.addParam(host.getPermissionId());
				idsToClear.addAll(dc.loadResults());

				//Removing permissions and permission references for all children containers
				dc.setSQL(deleteContainerReferencesSQL);
				dc.addParam(host.getPermissionId());
				dc.loadResult();
				dc.setSQL(deleteContainerPermissionsSQL);
				dc.addParam(host.getPermissionId());
				dc.loadResult();
				//Pointing the children containers to reference the current host
				dc.setSQL(insertContainerReferencesToAHostSQL);
				dc.addParam(host.getPermissionId());
				dc.addParam(host.getPermissionId());
				dc.loadResult();
				//Retrieving the list of containers to clear their caches later
				dc.setSQL(selectChildrenContainerSQL);
				dc.addParam(host.getPermissionId());
				idsToClear.addAll(dc.loadResults());

			}
			String folderPath = "";
			if(!isHost) {
				folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
			}
			//Removing permissions and permission references for all children subfolders
			dc.setSQL(deleteSubfolderReferencesSQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.addParam(isHost?" ":folderPath+"");
			dc.loadResult();
			dc.setSQL(deleteSubfolderPermissionsSQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.addParam(isHost?" ":folderPath+"");
			dc.loadResult();
			//Pointing the children subfolders to reference the current host
			dc.setSQL(insertSubfolderReferencesSQL);
			dc.addParam(permissionable.getPermissionId());
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.addParam(isHost?" ":folderPath+"");
			dc.addParam(isHost?"%":folderPath+"%");
			dc.loadResult();
			//Retrieving the list of sub folders changed to clear their caches
			dc.setSQL(selectChildrenFolderSQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.addParam(isHost?" ":folderPath+"");
			idsToClear.addAll(dc.loadResults());


			//Removing permissions and permission references for all children containers
			dc.setSQL(deleteHTMLPageReferencesSQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.loadResult();
			dc.setSQL(deleteHTMLPagePermissionsSQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.loadResult();
			//Pointing the children containers to reference the current host
			dc.setSQL(insertHTMLPageReferencesSQL);
			dc.addParam(permissionable.getPermissionId());
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.addParam(isHost?"%":folderPath+"%");
			dc.loadResult();
			//Retrieving the list of htmlpages changed to clear their caches
			dc.setSQL(selectChildrenHTMLPageSQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			idsToClear.addAll(dc.loadResults());

			//Removing permissions and permission references for all children containers
			dc.setSQL(deleteFileReferencesSQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.loadResult();
			dc.setSQL(deleteFilePermissionsSQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.loadResult();
			//Pointing the children containers to reference the current host
			dc.setSQL(insertFileReferencesSQL);
			dc.addParam(permissionable.getPermissionId());
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.addParam(isHost?"%":folderPath+"%");
			dc.loadResult();
			//Retrieving the list of files changed to clear their caches
			dc.setSQL(selectChildrenFileSQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			idsToClear.addAll(dc.loadResults());

			//Removing permissions and permission references for all children containers
			dc.setSQL(deleteLinkReferencesSQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.loadResult();
			dc.setSQL(deleteLinkPermissionsSQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.loadResult();
			//Pointing the children containers to reference the current host
			dc.setSQL(insertLinkReferencesSQL);
			dc.addParam(permissionable.getPermissionId());
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.addParam(isHost?"%":folderPath+"%");
			dc.loadResult();
			//Retrieving the list of links changed to clear their caches
			dc.setSQL(selectChildrenLinkSQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			idsToClear.addAll(dc.loadResults());

			//Removing permissions and permission references for all children content
			dc.setSQL(deleteContentReferencesByPathSQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.loadResult();
			dc.setSQL(deleteContentPermissionsByPathSQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.loadResult();
			//Pointing the children containers to reference the current host
			dc.setSQL(insertContentReferencesByPathSQL);
			dc.addParam(permissionable.getPermissionId());
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.addParam(isHost?"%":folderPath+"%");
			dc.loadResult();
			//Retrieving the list of content changed to clear their caches
			dc.setSQL(selectChildrenContentByPathSQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			idsToClear.addAll(dc.loadResults());

			if(isHost){
				//Removing permissions and permission references for all children structures
				dc.setSQL(this.deleteStructureReferencesByPathSQL);
				dc.addParam(isHost?"%":folderPath+"%");
				dc.addParam(host.getPermissionId());
				dc.addParam(host.getPermissionId());
				dc.addParam(host.getPermissionId());
				dc.addParam(isHost?"%":folderPath+"%");
				dc.loadResult();
				dc.setSQL(this.deleteStructurePermissionsByPathSQL);
				dc.addParam(isHost?"%":folderPath+"%");
				dc.addParam(host.getPermissionId());
				dc.addParam(host.getPermissionId());
				dc.loadResult();
				//Pointing the children structures to reference the current host
				dc.setSQL(this.insertStructureReferencesByPathSQL);
				dc.addParam(permissionable.getPermissionId());
				dc.addParam(isHost?"%":folderPath+"%");
				dc.addParam(host.getPermissionId());
				dc.addParam(host.getPermissionId());
				dc.addParam(isHost?"%":folderPath+"%");
				dc.loadResult();
				// Retrieving the list of structures changed to clear their caches
				dc.setSQL(this.selectChildrenStructureByPathSQL);
				dc.addParam(isHost?"%":folderPath+"%");
				dc.addParam(host.getPermissionId());
				dc.addParam(host.getPermissionId());
				idsToClear.addAll(dc.loadResults());
			}else if(isFolder){
				//Removing permissions and permission references for all children structures
				dc.setSQL(this.deleteStructureReferencesByPathSQLFolder);
				dc.addParam(isHost?"%":folderPath+"%");
				dc.addParam(host.getPermissionId());
				dc.addParam(host.getPermissionId());
				dc.addParam(isHost?"%":folderPath+"%");
				dc.loadResult();
				dc.setSQL(this.deleteStructurePermissionsByPathSQLFolder);
				dc.addParam(isHost?"%":folderPath+"%");
				dc.addParam(host.getPermissionId());
				dc.loadResult();
				//Pointing the children structures to reference the current host
				dc.setSQL(this.insertStructureReferencesByPathSQLFolder);
				dc.addParam(permissionable.getPermissionId());
				dc.addParam(isHost?"%":folderPath+"%");
				dc.addParam(host.getPermissionId());
				dc.addParam(isHost?"%":folderPath+"%");
				dc.loadResult();
				// Retrieving the list of structures changed to clear their caches
				dc.setSQL(this.selectChildrenStructureByPathSQLFolder);
				dc.addParam(isHost?"%":folderPath+"%");
				dc.addParam(host.getPermissionId());
				idsToClear.addAll(dc.loadResults());
			}


		} else if(isStructure) {

			//Removing permissions and permission references for all children containers
			dc.setSQL(deleteContentReferencesByStructureSQL);
			dc.addParam(permissionable.getPermissionId());
			dc.loadResult();
			dc.setSQL(deleteContentPermissionsByStructureSQL);
			dc.addParam(permissionable.getPermissionId());
			dc.loadResult();
			//Pointing the children containers to reference the current host
			dc.setSQL(insertContentReferencesByStructureSQL);
			dc.addParam(permissionable.getPermissionId());
			dc.addParam(permissionable.getPermissionId());
			dc.loadResult();
			//Retrieving the list of content changed to clear their caches
			dc.setSQL(selectChildrenContentByStructureSQL);
			dc.addParam(permissionable.getPermissionId());
			idsToClear.addAll(dc.loadResults());

		} else if(isCategory) {

			CategoryAPI catAPI = APILocator.getCategoryAPI();
			Category cat = (Category) permissionable;
			UserAPI userAPI = APILocator.getUserAPI();
			List<Category> children;
			try {
				children = catAPI.getCategoryTreeDown(cat, cat, userAPI.getSystemUser(), false);
			} catch (DotSecurityException e) {
				Logger.error(PermissionBitFactoryImpl.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
			for(Category child : children) {
				removePermissions(child);
			}
		}

		if(isFolder || isHost || isStructure) {
			//Ensure every reference that was moved to point to this permissionable has its permissions fulfilled if not
			//look up in the hierarchy
			updatePermissionReferencesOnRemove(permissionable);

			//Clearing the caches
			if(idsToClear.size() < 1000) {
				for(Map<String, String> idToClear: idsToClear) {
					permissionCache.remove(idToClear.get("inode"));
				}
			} else {
				permissionCache.clearCache();
			}

			if(isHost) {
				ContentletAPI contentAPI = APILocator.getContentletAPI();
				contentAPI.refreshContentUnderHost((Host)permissionable);
			}

			if(isStructure) {
				ContentletAPI contentAPI = APILocator.getContentletAPI();
				Structure st = StructureCache.getStructureByInode(permissionable.getPermissionId());
				if(st != null)
					contentAPI.refresh(st);
			}
			// http://jira.dotmarketing.net/browse/DOTCMS-6114
			if(isFolder) {
				ContentletAPI contAPI = APILocator.getContentletAPI();
				contAPI.refreshContentUnderFolder((Folder)permissionable);
			}
		}

	}

	@Override
	void cascadePermissionUnder(Permissionable permissionable, Role role) throws DotDataException {

		Logger.info(this, "Starting cascade role permissions for permissionable " + permissionable.getPermissionId() + " for role " + role.getId());
		if(!permissionable.isParentPermissionable()) {
			Logger.info(this, "Ending cascade role permissions (nothing to do is not parent permissionable) for permissionable " +
					permissionable.getPermissionId() + " for role " + role.getId());
			return;
		}

		boolean isHost = permissionable instanceof Host ||
			(permissionable instanceof Contentlet && ((Contentlet)permissionable).getStructure().getVelocityVarName().equals("Host"));
		boolean isFolder = permissionable instanceof Folder;

		if(!isHost && !isFolder) {
			Logger.info(this, "Ending cascade role permissions (not a folder or a host) for permissionable " + permissionable.getPermissionId() + " for role " + role.getId());
			return;
		}

		HostAPI hostAPI = APILocator.getHostAPI();
		User systemUser = APILocator.getUserAPI().getSystemUser();
		Host systemHost = hostAPI.findSystemHost();

		List<Permission> allPermissions = filterOnlyInheritablePermissions(loadPermissions(permissionable), permissionable.getPermissionId());

		if(isHost && permissionable.getPermissionId().equals(systemHost.getPermissionId())) {
			List<Host> allHosts;
			try {
				allHosts = hostAPI.findAll(systemUser, false);
			} catch (DotSecurityException e) {
				Logger.error(PermissionBitFactoryImpl.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
			for(Host host : allHosts) {
				if(!host.isSystemHost()) {

					if(filterOnlyIndividualPermissions(loadPermissions(host), host.getPermissionId()).size() > 0) {
						Permission inheritablePermission = filterInheritablePermission(allPermissions, permissionable
								.getPermissionId(), Host.class.getCanonicalName(), role.getId());
						int permission = 0;
						if (inheritablePermission != null) {
							permission = inheritablePermission.getPermission();
						}
						savePermission(new Permission(host.getPermissionId(), role.getId(), permission, true), host);
					}
					cascadePermissionUnder(host, role, permissionable, allPermissions);
				}
			}
		} else if(isHost || isFolder) {
			cascadePermissionUnder(permissionable, role, permissionable, allPermissions);
		}
		Logger.info(this, "Ending cascade role permissions for permissionable " + permissionable.getPermissionId() + " for role " + role.getId());

	}

	@SuppressWarnings("unchecked")
	private void cascadePermissionUnder(Permissionable permissionable, Role role, Permissionable permissionsPermissionable, List<Permission> allPermissions) throws DotDataException {

		boolean isHost = permissionable instanceof Host ||
			(permissionable instanceof Contentlet && ((Contentlet)permissionable).getStructure().getVelocityVarName().equals("Host"));
		boolean isFolder = permissionable instanceof Folder;

		DotConnect dc = new DotConnect();
		HostAPI hostAPI = APILocator.getHostAPI();
		User systemUser = APILocator.getUserAPI().getSystemUser();

		Permissionable host = null;
		try {
			host = isHost ? permissionable : hostAPI.findParentHost((Folder) permissionable, systemUser, false);
		} catch (DotSecurityException e) {
			Logger.error(PermissionBitFactoryImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		Folder folder = isFolder ? (Folder) permissionable : null;
		String folderPath = folder!=null?APILocator.getIdentifierAPI().find(folder).getPath():"";

		List<Permission> permissionablePermissions = loadPermissions(permissionable);

		if (isHost) {

			//Templates
			Permission inheritablePermission = filterInheritablePermission(allPermissions, permissionsPermissionable
					.getPermissionId(), Template.class.getCanonicalName(), role.getId());

			//Assigning inheritable permissions to the permissionable if needed
			List<Permission> permissionableTemplatePermissions = filterOnlyInheritablePermissions(permissionablePermissions, permissionable.getPermissionId(),
					Template.class.getCanonicalName());
			if(permissionableTemplatePermissions.size() > 0) {
				Permission permissionToUpdate = filterInheritablePermission(permissionablePermissions, permissionsPermissionable.getPermissionId(),
						Template.class.getCanonicalName(), role.getId());
				if(permissionToUpdate == null) {
					permissionToUpdate = new Permission(Template.class.getCanonicalName(), permissionable.getPermissionId(), role.getId(), 0, true);
				}
				if(inheritablePermission != null)
					permissionToUpdate.setPermission(inheritablePermission.getPermission());
				savePermission(permissionToUpdate, permissionable);
			}

			//Looking for children templates overriding inheritance to also apply the cascade changes
			dc.setSQL(selectChildrenTemplateWithIndividualPermissionsSQL);
			dc.addParam(host.getPermissionId());
			List<Map<String, String>> idsToUpdate = dc.loadResults();
			TemplateAPI templateAPI = APILocator.getTemplateAPI();
			int permission = 0;
			if (inheritablePermission != null) {
				permission = inheritablePermission.getPermission();
			}
			for (Map<String, String> idMap : idsToUpdate) {
				String id = idMap.get("id");
				Permissionable childPermissionable;
				try {
					childPermissionable = templateAPI.findWorkingTemplate(id, systemUser, false);
				} catch (DotSecurityException e) {
					Logger.error(PermissionBitFactoryImpl.class, e.getMessage(), e);
					throw new DotRuntimeException(e.getMessage(), e);
				}
				savePermission(new Permission(id, role.getId(), permission, true), childPermissionable);
			}

			//Containers
			inheritablePermission = filterInheritablePermission(allPermissions, permissionsPermissionable.getPermissionId(),
					Container.class.getCanonicalName(), role.getId());

			//Assigning inheritable permissions to the permissionable if needed
			List<Permission> permissionableContainerPermissions = filterOnlyInheritablePermissions(permissionablePermissions, permissionable.getPermissionId(),
					Container.class.getCanonicalName());
			if(permissionableContainerPermissions.size() > 0) {
				Permission permissionToUpdate = filterInheritablePermission(permissionablePermissions, permissionsPermissionable.getPermissionId(),
						Container.class.getCanonicalName(), role.getId());
				if(permissionToUpdate == null) {
					permissionToUpdate = new Permission(Container.class.getCanonicalName(), permissionable.getPermissionId(), role.getId(), 0, true);
				}
				if(inheritablePermission != null)
					permissionToUpdate.setPermission(inheritablePermission.getPermission());
				savePermission(permissionToUpdate, permissionable);
			}

			//Looking for children containers overriding inheritance to also apply the cascade changes
			dc.setSQL(selectChildrenContainerWithIndividualPermissionsSQL);
			dc.addParam(host.getPermissionId());
			idsToUpdate = dc.loadResults();
			ContainerAPI containerAPI = APILocator.getContainerAPI();
			permission = 0;
			if (inheritablePermission != null) {
				permission = inheritablePermission.getPermission();
			}
			for (Map<String, String> idMap : idsToUpdate) {
				String id = idMap.get("id");
				Permissionable childPermissionable;
				try {
					childPermissionable = containerAPI.getWorkingContainerById(id, systemUser, false);
				} catch (DotSecurityException e) {
					Logger.error(PermissionBitFactoryImpl.class, e.getMessage(), e);
					throw new DotRuntimeException(e.getMessage(), e);
				}
				savePermission(new Permission(id, role.getId(), permission, true), childPermissionable);
			}

		}

		//Folders
		Permission inheritablePermission = filterInheritablePermission(allPermissions,
				permissionsPermissionable.getPermissionId(), Folder.class.getCanonicalName(), role.getId());

		//Assigning inheritable permissions to the permissionable if needed
		List<Permission> permissionableFolderPermissions = filterOnlyInheritablePermissions(permissionablePermissions, permissionable.getPermissionId(),
				Folder.class.getCanonicalName());
		if(permissionableFolderPermissions.size() > 0) {
			Permission permissionToUpdate = filterInheritablePermission(permissionablePermissions, permissionsPermissionable.getPermissionId(),
					Folder.class.getCanonicalName(), role.getId());
			if(permissionToUpdate == null) {
				permissionToUpdate = new Permission(Folder.class.getCanonicalName(), permissionable.getPermissionId(), role.getId(), 0, true);
			}
			if(inheritablePermission != null)
				permissionToUpdate.setPermission(inheritablePermission.getPermission());
			savePermission(permissionToUpdate, permissionable);
		}

		// Selecting folders which are children and need individual permission
		// changes
		dc.setSQL(selectChildrenFolderWithDirectPermissionsSQL);
		dc.addParam(host.getPermissionId());
		dc.addParam(isHost ? "%" : folderPath + "%");
		dc.addParam(isHost ? " " : folderPath + "");
		List<Map<String, String>> idsToUpdate = dc.loadResults();
		FolderAPI folderAPI = APILocator.getFolderAPI();
		int permission = 0;
		if (inheritablePermission != null) {
			permission = inheritablePermission.getPermission();
		}
		for (Map<String, String> idMap : idsToUpdate) {
			String id = idMap.get("inode");
			Permissionable childPermissionable;
			try {
				childPermissionable = folderAPI.find(id, systemUser, false);
				savePermission(new Permission(id, role.getId(), permission, true), childPermissionable);
			} catch (DotSecurityException e) {
				Logger.error(this.getClass(), "Should not be getting a Permission Error with system user", e);
			}

		}

		//HTML pages

		inheritablePermission = filterInheritablePermission(allPermissions, permissionsPermissionable.getPermissionId(),
				HTMLPage.class.getCanonicalName(), role.getId());

		//Assigning inheritable permissions to the permissionable if needed
		List<Permission> permissionablePagesPermissions = filterOnlyInheritablePermissions(permissionablePermissions, permissionable.getPermissionId(),
				HTMLPage.class.getCanonicalName());
		if(permissionablePagesPermissions.size() > 0) {
			Permission permissionToUpdate = filterInheritablePermission(permissionablePermissions, permissionsPermissionable.getPermissionId(),
					HTMLPage.class.getCanonicalName(), role.getId());
			if(permissionToUpdate == null) {
				permissionToUpdate = new Permission(HTMLPage.class.getCanonicalName(), permissionable.getPermissionId(), role.getId(), 0, true);
			}
			if(inheritablePermission != null)
				permissionToUpdate.setPermission(inheritablePermission.getPermission());
			savePermission(permissionToUpdate, permissionable);
		}

		// Selecting html pages which are children and need individual
		// permission changes
		dc.setSQL(selectChildrenHTMLPageWithIndividualPermissionsSQL);
		dc.addParam(host.getPermissionId());
		dc.addParam(isHost ? "%" : folderPath + "%");
		idsToUpdate = dc.loadResults();
		HTMLPageAPI pageAPI = APILocator.getHTMLPageAPI();
		permission = 0;
		if (inheritablePermission != null) {
			permission = inheritablePermission.getPermission();
		}
		for (Map<String, String> idMap : idsToUpdate) {
			String id = idMap.get("id");
			Permissionable childPermissionable;
			try {
				childPermissionable = pageAPI.loadWorkingPageById(id, systemUser, false);
			} catch (DotSecurityException e) {
				Logger.error(PermissionBitFactoryImpl.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
			savePermission(new Permission(id, role.getId(), permission, true), childPermissionable);
		}

		// File

		inheritablePermission = filterInheritablePermission(allPermissions, permissionsPermissionable.getPermissionId(),
				File.class.getCanonicalName(), role.getId());

		//Assigning inheritable permissions to the permissionable if needed
		List<Permission> permissionableFilesPermissions = filterOnlyInheritablePermissions(permissionablePermissions, permissionable.getPermissionId(),
				File.class.getCanonicalName());
		if(permissionableFilesPermissions.size() > 0) {
			Permission permissionToUpdate = filterInheritablePermission(permissionablePermissions, permissionsPermissionable.getPermissionId(),
					File.class.getCanonicalName(), role.getId());
			if(permissionToUpdate == null) {
				permissionToUpdate = new Permission(File.class.getCanonicalName(), permissionable.getPermissionId(), role.getId(), 0, true);
			}
			if(inheritablePermission != null)
				permissionToUpdate.setPermission(inheritablePermission.getPermission());
			savePermission(permissionToUpdate, permissionable);
		}

		// Selecting files which are children and need individual permission
		// changes
		dc.setSQL(selectChildrenFileWithIndividualPermissionsSQL);
		dc.addParam(host.getPermissionId());
		dc.addParam(isHost ? "%" : folderPath + "%");
		idsToUpdate = dc.loadResults();
		FileAPI fileAPI = APILocator.getFileAPI();
		permission = 0;
		if (inheritablePermission != null) {
			permission = inheritablePermission.getPermission();
		}
		for (Map<String, String> idMap : idsToUpdate) {
			String id = idMap.get("id");
			Permissionable childPermissionable;
			try {
				childPermissionable = fileAPI.getWorkingFileById(id, systemUser, false);
			} catch (DotSecurityException e) {
				Logger.error(PermissionBitFactoryImpl.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
			savePermission(new Permission(id, role.getId(), permission, true), childPermissionable);
		}

		// Links

		inheritablePermission = filterInheritablePermission(allPermissions, permissionsPermissionable.getPermissionId(),
				Link.class.getCanonicalName(), role.getId());

		//Assigning inheritable permissions to the permissionable if needed
		List<Permission> permissionableLinksPermissions = filterOnlyInheritablePermissions(permissionablePermissions, permissionable.getPermissionId(),
				Link.class.getCanonicalName());
		if(permissionableLinksPermissions.size() > 0) {
			Permission permissionToUpdate = filterInheritablePermission(permissionablePermissions, permissionsPermissionable.getPermissionId(),
					Link.class.getCanonicalName(), role.getId());
			if(permissionToUpdate == null) {
				permissionToUpdate = new Permission(Link.class.getCanonicalName(), permissionable.getPermissionId(), role.getId(), 0, true);
			}
			if(inheritablePermission != null)
				permissionToUpdate.setPermission(inheritablePermission.getPermission());
			savePermission(permissionToUpdate, permissionable);
		}

		// Selecting links which are children and need individual permission
		// changes
		dc.setSQL(selectChildrenLinkWithIndividualPermissionsSQL);
		dc.addParam(host.getPermissionId());
		dc.addParam(isHost ? "%" : folderPath + "%");
		idsToUpdate = dc.loadResults();
		MenuLinkAPI linkAPI = APILocator.getMenuLinkAPI();
		permission = 0;
		if (inheritablePermission != null) {
			permission = inheritablePermission.getPermission();
		}
		for (Map<String, String> idMap : idsToUpdate) {
			String id = idMap.get("id");
			Permissionable childPermissionable;
			try {
				childPermissionable = linkAPI.findWorkingLinkById(id, systemUser, false);
			} catch (DotSecurityException e) {
				Logger.error(PermissionBitFactoryImpl.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
			savePermission(new Permission(id, role.getId(), permission, true), childPermissionable);
		}

		// Contentlets

		inheritablePermission = filterInheritablePermission(allPermissions, permissionsPermissionable.getPermissionId(),
				Contentlet.class.getCanonicalName(), role.getId());

		//Assigning inheritable permissions to the permissionable if needed
		List<Permission> permissionableContentPermissions = filterOnlyInheritablePermissions(permissionablePermissions, permissionable.getPermissionId(),
				Contentlet.class.getCanonicalName());
		if(permissionableContentPermissions.size() > 0) {
			Permission permissionToUpdate = filterInheritablePermission(permissionablePermissions, permissionsPermissionable.getPermissionId(),
					Contentlet.class.getCanonicalName(), role.getId());
			if(permissionToUpdate == null) {
				permissionToUpdate = new Permission(Contentlet.class.getCanonicalName(), permissionable.getPermissionId(), role.getId(), 0, true);
			}
			if(inheritablePermission != null)
				permissionToUpdate.setPermission(inheritablePermission.getPermission());
			savePermission(permissionToUpdate, permissionable);
		}

		// Selecting content which are children and need individual permission
		// changes
		dc.setSQL(selectChildrenContentWithIndividualPermissionsByPathSQL);
		dc.addParam(host.getPermissionId());
		dc.addParam(isHost ? "%" : folderPath + "%");
		idsToUpdate = dc.loadResults();
		ContentletAPI contentAPI = APILocator.getContentletAPI();
		permission = 0;
		if (inheritablePermission != null) {
			permission = inheritablePermission.getPermission();
		}
		for (Map<String, String> idMap : idsToUpdate) {
			String id = idMap.get("id");
			Permissionable childPermissionable;
			try {
				childPermissionable = contentAPI.findContentletByIdentifier(id, false, 0, systemUser, false);
			} catch (DotSecurityException e) {
				Logger.error(PermissionBitFactoryImpl.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
			savePermission(new Permission(id, role.getId(), permission, true), childPermissionable);
		}

		// Structures

		inheritablePermission = filterInheritablePermission(allPermissions, permissionsPermissionable.getPermissionId(),
				Structure.class.getCanonicalName(), role.getId());

		//Assigning inheritable permissions to the permissionable if needed
		List<Permission> permissionableStructurePermissions = filterOnlyInheritablePermissions(permissionablePermissions, permissionable.getPermissionId(),
				Structure.class.getCanonicalName());
		if(permissionableStructurePermissions.size() > 0) {
			Permission permissionToUpdate = filterInheritablePermission(permissionablePermissions, permissionsPermissionable.getPermissionId(),
					Structure.class.getCanonicalName(), role.getId());
			if(permissionToUpdate == null) {
				permissionToUpdate = new Permission(Structure.class.getCanonicalName(), permissionable.getPermissionId(), role.getId(), 0, true);
			}
			if(inheritablePermission != null)
				permissionToUpdate.setPermission(inheritablePermission.getPermission());
			savePermission(permissionToUpdate, permissionable);
		}


		// Selecting structures which are children and need individual permission
		// changes
		dc.setSQL(selectChildrenStructureWithIndividualPermissionsByPathSQL);
		dc.addParam(isHost ? "%" : folderPath + "%");
		dc.addParam(host.getPermissionId());
		dc.addParam(host.getPermissionId());
		idsToUpdate = dc.loadResults();
		permission = 0;
		if (inheritablePermission != null) {
			permission = inheritablePermission.getPermission();
		}

		for (Map<String, String> idMap : idsToUpdate) {
			String id = idMap.get("inode");
			Permissionable childPermissionable = StructureCache.getStructureByInode(id);
			savePermission(new Permission(id, role.getId(), permission, true), childPermissionable);
			//http://jira.dotmarketing.net/browse/DOTCMS-6090
			//If a structure we need to save permissions inheritable by children content
			savePermission(new Permission(Contentlet.class.getCanonicalName(), id, role.getId(), permission, true),childPermissionable);
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	void resetChildrenPermissionReferences(Structure structure) throws DotDataException {
	    ContentletAPI contAPI = APILocator.getContentletAPI();
	    ContentletIndexAPI indexAPI=new ESContentletIndexAPI();
	    
	    DotConnect dc = new DotConnect();
		dc.setSQL(deleteContentReferencesByStructureSQL);
		dc.addParam(structure.getPermissionId());
		dc.loadResult();

		final int limit=500;
		int offset=0;
		List<Contentlet> contentlets;
		do {
			String query="structurename:"+structure.getName();
			try {
			    contentlets=contAPI.search(query, limit, offset, "identifier", APILocator.getUserAPI().getSystemUser(), false);
            } catch (DotSecurityException e) {
                throw new RuntimeException(e);
            }
			
			BulkRequestBuilder bulk=new ESClient().getClient().prepareBulk();
			for(Contentlet cont : contentlets) {
			    permissionCache.remove(cont.getPermissionId());
			    indexAPI.addContentToIndex(cont, false, true, true, bulk);
			}
			if(bulk.numberOfActions()>0)
			    bulk.execute().actionGet();
			
			offset=offset+limit;
		} while(contentlets.size()>0);
	}

	@Override
	void resetPermissionReferences(Permissionable permissionable) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(this.deletePermissionReferenceSQL);
		dc.addParam(permissionable.getPermissionId());
		dc.addParam(permissionable.getPermissionId());
		dc.loadResult();

		permissionCache.remove(permissionable.getPermissionId());
	}

	@Override
	void resetAllPermissionReferences() throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(this.deleteAllPermissionReferencesSQL);
		dc.loadResult();
		permissionCache.clearCache();

	}


	<P extends Permissionable> List<P> filterCollectionByDBPermissionReference(
			List<P> permissionables, int requiredTypePermission,
			boolean respectFrontendRoles, User user) throws DotDataException,
			DotSecurityException {


		Role adminRole;
		Role anonRole;
		Role frontEndUserRole;
		try {
			adminRole = APILocator.getRoleAPI().loadCMSAdminRole();
			anonRole = APILocator.getRoleAPI().loadCMSAnonymousRole();
			frontEndUserRole = APILocator.getRoleAPI().loadLoggedinSiteRole();
		} catch (DotDataException e1) {
			Logger.error(this, e1.getMessage(), e1);
			throw new DotRuntimeException(e1.getMessage(), e1);
		}

		List<String> roleIds = new ArrayList<String>();
		if(respectFrontendRoles){
		// add anonRole and frontEndUser roles
			roleIds.add(anonRole.getId());
			if(user != null ){
			 roleIds.add("'"+frontEndUserRole.getId()+"'");
			}
		}

		//If user is null and roleIds are empty return empty list
		if(roleIds.isEmpty() && user==null){
			return new ArrayList<P>();
		}

		List<Role> roles;
		try {
			roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
		} catch (DotDataException e1) {
			Logger.error(this, e1.getMessage(), e1);
			throw new DotRuntimeException(e1.getMessage(), e1);
		}
		for (Role role : roles) {
			try{
				String roleId = role.getId();
				roleIds.add("'"+roleId+"'");
				if(roleId.equals(adminRole.getId())){
					// if CMS Admin return all permissionables
					return permissionables;
				}
			}catch (Exception e) {
				Logger.error(this, "Roleid should be a long : ",e);
			}
		}

		Map<String, P> permissionableMap = new HashMap<String, P>();
		StringBuilder permissionRefSQL = new StringBuilder();
		permissionRefSQL.append("select asset_id from permission_reference, permission WHERE permission_reference.reference_id = permission.inode_id ");
		permissionRefSQL.append(" and permission.permission_type = permission_reference.permission_type ");
		permissionRefSQL.append("and permission.roleid in( ");
		StringBuilder individualPermissionSQL = new StringBuilder();
		individualPermissionSQL.append("select inode_id from permission where permission_type = 'individual' ");
		individualPermissionSQL.append("and roleid in( ");
		int roleIdCount = 0;
		for(String roleId : roleIds){
			permissionRefSQL.append(roleId);
			individualPermissionSQL.append(roleId);
			if(roleIdCount<roleIds.size()-1){
				permissionRefSQL.append(", ");
				individualPermissionSQL.append(", ");
			}
			roleIdCount++;
		}
		if(DbConnectionFactory.isOracle()){
			permissionRefSQL.append(") and bitand(permission.permission, "+ requiredTypePermission +") > 0 and permission_reference.asset_id in( ");
			individualPermissionSQL.append(") and bitand(permission, "+ requiredTypePermission +") > 0 and inode_id in( ");
		}else{
			permissionRefSQL.append(") and (permission.permission & "+ requiredTypePermission +") > 0 and permission_reference.asset_id in( ");
			individualPermissionSQL.append(") and (permission & "+ requiredTypePermission +") > 0 and inode_id in( ");
		}
		StringBuilder permIds = new StringBuilder();
		//Iterate over 500 at a time to build the SQL
		int permIdCount = 0;
		List<P> permsToReturn = new ArrayList<P>();
		for(P perm : permissionables){
			Inode inode = (Inode)perm;
			permissionableMap.put(inode.getIdentifier(), perm);
			permIds.append("'"+inode.getIdentifier()+"'");
			if((permIdCount>0 && permIdCount%500==0) || (permIdCount==permissionables.size()-1)){
				permIds.append(") ");
				DotConnect dc = new DotConnect();
				dc.setSQL(permissionRefSQL.toString()+permIds.toString() + " UNION " +individualPermissionSQL.toString() + permIds.toString());
				List<Map<String, Object>> results = (ArrayList<Map<String, Object>>)dc.loadResults();
	    		for (int i = 0; i < results.size(); i++) {
	    			Map<String, Object> hash = (Map<String, Object>) results.get(i);
	    			if(!hash.isEmpty()){
	    				String assetId = (String) hash.get("asset_id");
	    				permsToReturn.add(permissionableMap.get(assetId));
	    			}
	    		}
	    		permissionableMap = new HashMap<String, P>();
				permIds = new StringBuilder();
			}else{
				permIds.append(", ");
			}
			permIdCount++;
		}
		return permsToReturn;
	}

	@Override
	boolean isInheritingPermissions(Permissionable permissionable) throws DotDataException {
		// http://jira.dotmarketing.net/browse/DOTCMS-6393
		// if it haven't a parent to inherit then don't bother looking for individual permissions
		if(permissionable.getParentPermissionable()==null) return false;

		DotConnect dc = new DotConnect();
		dc.setSQL("SELECT COUNT(*) AS cc FROM permission where inode_id=?");
		dc.addParam(permissionable.getPermissionId());
		dc.loadResult();
		return dc.getInt("cc")==0;
	}
}
