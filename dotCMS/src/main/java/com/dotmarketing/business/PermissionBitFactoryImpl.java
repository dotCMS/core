package com.dotmarketing.business;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotConcurrentFactory.SubmitterConfigBuilder;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.concurrent.lock.IdentifierStripedLock;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImpl;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.rendering.velocity.viewtools.navigation.NavResult;
import com.dotcms.repackage.com.google.common.primitives.Ints;
import com.dotcms.system.SimpleMapAppContext;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.PermissionReference;
import com.dotmarketing.beans.PermissionType;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.commands.DatabaseCommand.QueryReplacements;
import com.dotmarketing.db.commands.UpsertCommand;
import com.dotmarketing.db.commands.UpsertCommandFactory;
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
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.client.RequestOptions;
import java.util.stream.Collectors;
import org.elasticsearch.common.unit.TimeValue;


/**
 * This class upgrades the old permissionsfactoryimpl to handle the storage and retrieval of bit permissions from the database
 * a big storage improvement that will let us reduces the amount of rows in the permissions table
 * as much as three times.
 *
 * @author David Torres (2009)
*/
public class PermissionBitFactoryImpl extends PermissionFactory {

	private PermissionCache permissionCache;
	private static final Map<String, Integer> PERMISION_TYPES = new HashMap<>();

	private final IdentifierStripedLock lockManager = DotConcurrentFactory.getInstance().getIdentifierStripedLock();

	private static final String LOCK_PREFIX = "PermissionID:";


	//SQL Queries used to maintain permissions
	/*
	 * To load permissions either individual permissions or inherited permissions as well as inheritable permissions for a
	 * given permissionable id
	 *
	 * Parameters
	 * 1. The permisionable id
	 * 2. The permisionable id
	 */
	private static final String LOAD_PERMISSION_SQL =
		" select {permission.*} from permission where inode_id = ? "+
        " union all "+
        " select {permission.*} from permission join permission_reference "+
        "    on (inode_id = reference_id and permission.permission_type = permission_reference.permission_type) "+
        "    where asset_id = ?";

	/*
	 * To load permission references objects based on the reference they are pointing to
	 * Parameters
	 * 1. The reference id the references are pointing to
	 */
	private static final String LOAD_PERMISSION_REFERENCES_BY_REFERENCEID_HSQL = "from " + PermissionReference.class.getCanonicalName() +
		" permission_reference where reference_id = ?";



	/*
	 * To update a permission reference by the owner asset
	 * Parameters
	 * 1. New reference id to set
	 * 2. Permission type
	 * 3. asset id
	 */
	private static final String UPDATE_PERMISSION_REFERENCE_BY_ASSETID_SQL = "update permission_reference set reference_id = ? where permission_type = ? and asset_id = ?";

	/*
	 * Select permission references based on how are referencing the type of reference
	 * Parameters
	 * 1. Reference id
	 * 2. Permission type
	 */
	private static final String SELECT_PERMISSION_REFERENCE_SQL = "select asset_id from permission_reference where reference_id = ? and permission_type = ?";

	/*
	 * To remove a permission reference of an specific asset or referencing an asset
	 *
	 */
	private static final String DELETE_PERMISSION_REFERENCE_SQL = "delete from permission_reference where asset_id = ? or reference_id = ?";

	/*
	 * To remove all permission references
	 *
	 */
	private static final String DELETE_ALL_PERMISSION_REFERENCES_SQL = "delete from permission_reference";

	/*
	 * To remove a permission reference of an specific asset
	 *
	 */
	private static final String DELETE_PERMISSIONABLE_REFERENCE_SQL = "delete from permission_reference where asset_id = ?";

	/*
	 * To load template identifiers that are children of a host
	 * Parameters
	 * 1. The id of the host
	 */
	private static final String SELECT_CHILD_TEMPLATE_SQL =
		"select id from identifier where identifier.host_inode = ? and asset_type='template' ";

	/*
	 * To load template identifiers that are children of a host and have inheritable permissions
	 * Parameters
	 * 1. The id of the host
	 */
	private static final String SELECT_CHILD_TEMPLATE_WITH_INDIVIDUAL_PERMISSIONS_SQL =
        "select distinct identifier.id from identifier join permission on (inode_id = identifier.id) " +
        "where asset_type='template' and permission_type='" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "' " +
        "and host_inode = ? ";

	/*
	 * To remove all permission of templates attached to an specific host
	 * Parameters
	 * 1. The host id the templates belong to
	 */
	private final String deleteTemplatePermissionsSQL =
		"delete from permission where inode_id in " +
		"	(" + SELECT_CHILD_TEMPLATE_SQL + ")";

	/*
	 * To remove all permission references of templates attached to an specific host
	 * Parameters
	 * 1. The host id the templates belong to
	 */
	private final String deleteTemplateReferencesSQL =
		"delete from permission_reference where asset_id in " +
		"	(" + SELECT_CHILD_TEMPLATE_SQL + ")";


	/*
	 * To load container identifiers that are children of a host
	 * Parameters
	 * 1. The host id
	 */
    private static final String SELECT_CHILD_CONTAINER_SQL =
		"select distinct identifier.id from identifier where " +
		"identifier.host_inode = ? and asset_type='containers' ";

	/*
	 * To load container identifiers that are children of a host and have inheritable permissions
	 * Parameters
	 * 1. The host id
	 */
	private final static String SELECT_CHILD_CONTAINER_WITH_INDIVIDUAL_PERMISSIONS_SQL =
        "select distinct identifier.id from identifier join permission on (inode_id = identifier.id) " +
        "where asset_type='containers' and permission_type='" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "' " +
        "and host_inode = ? ";

	/*
	 * To remove all permissions of containers attached to an specific host
	 * Parameters
	 * 1. The host id the containers belong to
	 */
	private final String deleteContainerPermissionsSQL =
		"delete from permission where inode_id in " +
		"	(" + SELECT_CHILD_CONTAINER_SQL + ")";

	/*
	 * To remove all permission references of containers attached to an specific host
	 * Parameters
	 * 1. The host id the containers belong to
	 */
	private final String deleteContainerReferencesSQL =
		"delete from permission_reference where asset_id in " +
		"	(" + SELECT_CHILD_CONTAINER_SQL + ")";

	/**
	 * Function name to get the folder path. MSSql need owner prefix dbo
	 */
	private static final String DOT_FOLDER_PATH=(DbConnectionFactory.isMsSql() ? "dbo.":"")+"dotFolderPath";

	/*
	 * To load folder inodes that are in the same tree/hierarchy of a parent host/folder
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 * 3. Parent folder exact path E.G. '/about/' pass '' if you want all from the host
	 */
	private static final String SELECT_CHILD_FOLDER_SQL =
		"select distinct folder.inode from folder join identifier on (folder.identifier = identifier.id) where " +
		"identifier.host_inode = ? and "+DOT_FOLDER_PATH+"(parent_path,asset_name) like ? and "+DOT_FOLDER_PATH+"(parent_path,asset_name) <> ? ";

	/*
	 * To load folder identifiers that are children of a host and have either individual and/or inheritable permissions
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 * 3. Parent folder exact path E.G. '/about/' pass '' if you want all from the host
	 */
	private static final String SELECT_CHILD_FOLDER_WITH_DIRECT_PERMISSIONS_SQL =
	     "select distinct folder.inode from folder join identifier on (folder.identifier = identifier.id) join permission on (inode_id=folder.inode) where " +
	     "identifier.host_inode = ? and "+DOT_FOLDER_PATH+"(parent_path,asset_name) like ? and "+DOT_FOLDER_PATH+"(parent_path,asset_name) <> ?";

	/*
	 * To remove all permissions of sub-folders of a given parent folder
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 * 3. Parent folder exact path E.G. '/about/' pass '' if you want all from the host
	 */
	private final String deleteSubfolderPermissionsSQL =
		"delete from permission where inode_id in " +
		"	(" + SELECT_CHILD_FOLDER_SQL + ")";

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
			" " + SELECT_CHILD_FOLDER_SQL + " and " +
			"	permission_type = '" + Folder.class.getCanonicalName() + "' and asset_id = folder.inode)";

	private final String deleteSubfolderReferencesSQLOnAdd =
		"delete from permission_reference where exists(" +
		" " + SELECT_CHILD_FOLDER_SQL + " and " +
		"	permission_type = '" + Folder.class.getCanonicalName() + "' and asset_id = folder.inode) " +
		"and (reference_id in ( " +
			"select distinct folder.inode " +
			"from folder join identifier on (folder.identifier = identifier.id) " +
			"where " +
			"	identifier.host_inode = ? " +
			"	and ("+DOT_FOLDER_PATH+"(parent_path,asset_name) not like ? OR "+DOT_FOLDER_PATH+"(parent_path,asset_name) = ?) " +
			"	and permission_type = 'com.dotmarketing.portlets.folders.model.Folder' " +
			"	and reference_id = folder.inode" +
			")" +
			" OR EXISTS(SELECT c.inode " +
			" FROM contentlet c  " +
			" WHERE c.identifier = reference_id)" +
			")";


	/*
	 * To load html page identifiers that are in the same tree/hierarchy of a parent host/folder
	 *
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
	private static final String SELECT_CHILD_HTMLPAGE_SQL =
            "select distinct li.id from identifier li where" +
                " li.asset_type='htmlpage' and li.host_inode = ? and li.parent_path like ?" +
            " UNION ALL" +
                " SELECT distinct li.id FROM identifier li" +
                    " INNER JOIN contentlet lc ON (lc.identifier = li.id and li.asset_type = 'contentlet')" +
                    " INNER JOIN structure ls ON (lc.structure_inode = ls.inode and ls.structuretype = " + BaseContentType.HTMLPAGE.getType() + ")" +
                    " AND li.host_inode = ? and li.parent_path like ?";

	private static final String SELECT_CHILD_HTMLPAGE_ON_PERMISSIONS_SQL =
            "select distinct li.id from identifier li" +
                    " JOIN permission_reference ON permission_type = '" + IHTMLPage.class.getCanonicalName() + "' and asset_id = li.id" +
                    " AND li.asset_type='htmlpage' and li.host_inode = ? and li.parent_path like ?" +
            " UNION ALL" +
                    " SELECT distinct li.id FROM identifier li" +
                    " INNER JOIN contentlet lc ON (lc.identifier = li.id and li.asset_type = 'contentlet')" +
                    " INNER JOIN structure ls ON (lc.structure_inode = ls.inode and ls.structuretype = " + BaseContentType.HTMLPAGE.getType() + ")" +
                    " JOIN permission_reference ON permission_type = '" + IHTMLPage.class.getCanonicalName() + "' and asset_id = li.id" +
                    " AND li.host_inode = ? and li.parent_path like ?";

	/*
	 * To load html pages identifiers that are children of a host and have inheritable permissions
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
    private static final String SELECT_CHILD_HTMLPAGE_WITH_INDIVIDUAL_PERMISSIONS_SQL =
            "select distinct li.id from identifier li join permission on (inode_id = li.id) where " +
                    " li.asset_type='htmlpage' and li.host_inode = ? and li.parent_path like ? " +
                    " and permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
            " UNION ALL" +
                    " SELECT distinct li.id from identifier li" +
                        " INNER JOIN contentlet lc ON (lc.identifier = li.id and li.asset_type = 'contentlet')" +
                        " INNER JOIN structure ls ON (lc.structure_inode = ls.inode and ls.structuretype = " + BaseContentType.HTMLPAGE.getType() + ")" +
                        " JOIN permission lp ON (lp.inode_id = li.id) " +
                        " AND li.host_inode = ? and li.parent_path like ?" +
                        " AND lp.permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'";

	/*
	 * To remove all permissions of html pages of a given parent folder
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
	private final String deleteHTMLPagePermissionsSQL =
		"delete from permission where inode_id in " +
		"	(" + SELECT_CHILD_HTMLPAGE_SQL + ")";


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
			(DbConnectionFactory.isMySql() ?
					"delete from permission_reference where exists ( select id FROM (" + SELECT_CHILD_HTMLPAGE_ON_PERMISSIONS_SQL + ") AS C )" :
					"delete from permission_reference where exists (" + SELECT_CHILD_HTMLPAGE_ON_PERMISSIONS_SQL + ")");

	private final String deleteHTMLPageReferencesOnAddSQL =
		(DbConnectionFactory.isMySql() ?
				"delete from permission_reference where exists ( select id FROM (" + SELECT_CHILD_HTMLPAGE_ON_PERMISSIONS_SQL + ") AS C ) " :
				"delete from permission_reference where exists (" + SELECT_CHILD_HTMLPAGE_ON_PERMISSIONS_SQL + ") ") +
		"and (reference_id in (" +
			"select distinct folder.inode " +
			" from folder join identifier on (folder.identifier = identifier.id) " +
			" where identifier.host_inode = ? " +
			" and ("+DOT_FOLDER_PATH+"(parent_path,asset_name) not like ? OR "+DOT_FOLDER_PATH+"(parent_path,asset_name) = ?) " +
			" and reference_id = folder.inode" +
			") " +
			" OR EXISTS(SELECT c.inode " +
			"  FROM contentlet c " +
			"  WHERE c.identifier = reference_id)	" +
			")";

	/**
	 * Delete permission by Inode
	 * Parameter
	 * 1. inode
	 */
	private static final String DELETE_PERMISSION_BY_INODE = "delete from permission where inode_id=?";


	/*
	 * To load link identifiers that are in the same tree/hierarchy of a parent host/folder
	 *
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
    private static final String SELECT_CHILD_LINK_SQL =
		"select distinct identifier.id from identifier where " +
		"asset_type='links' and identifier.host_inode = ? and identifier.parent_path like ?";

	/*
	 * To load link identifiers that are children of a host and have inheritable permissions
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
	private static final String SELECT_CHILD_LINK_WITH_INDIVIDUAL_PERMISSIONS_SQL =
        "select distinct identifier.id from identifier join permission on (inode_id = identifier.id) where " +
        "asset_type='links' and identifier.host_inode = ? and identifier.parent_path like ? " +
        "and permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'";

	/*
	 * To remove all permissions of links of a given parent folder
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
	private final String deleteLinkPermissionsSQL =
		"delete from permission where inode_id in " +
		"	(" + SELECT_CHILD_LINK_SQL + ")";

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
			"	" + SELECT_CHILD_LINK_SQL + " and" +
			"	permission_type = '" + Link.class.getCanonicalName() + "' and asset_id = identifier.id)";

	private final String deleteLinkReferencesOnAddSQL =
		"delete from permission_reference where exists (" +
		"	" + SELECT_CHILD_LINK_SQL + " and" +
		"	permission_type = '" + Link.class.getCanonicalName() + "' and asset_id = identifier.id) " +
		"and (reference_id in (" +
		"select distinct folder.inode " +
		"from folder join identifier on (folder.identifier = identifier.id) " +
		"where " +
		" identifier.host_inode = ? " +
		" and ("+DOT_FOLDER_PATH+"(parent_path,asset_name) not like ? OR "+DOT_FOLDER_PATH+"(parent_path,asset_name) = ?) " +
		" and permission_type = 'com.dotmarketing.portlets.folders.model.Folder' " +
		" and reference_id = folder.inode" +
		") " +
		"OR EXISTS(SELECT c.inode " +
		"FROM contentlet c " +
		"  WHERE c.identifier = reference_id)	" +
		")";


	/*
	 * To load content identifiers that are in the same tree/hierarchy of a parent host/folder
	 *
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
    private static final String SELECT_CHILD_CONTENT_BY_PATH_SQL =
        "select distinct identifier.id from identifier where asset_type='contentlet' " +
        " and identifier.id <> identifier.host_inode and identifier.host_inode = ? " +
        " and identifier.parent_path like ?";

	/*
	 * To load content identifiers that are children of a host and have inheritable permissions
	 * Parameters
	 * 1. The host id
	 * 2. Parent folder like path E.G. '/about/%' pass '%' if you want all from the host
	 */
    private static final String SELECT_CHILD_CONTENT_WITH_INDIVIDUAL_PERMISSIONS_BY_PATH_SQL =
            "select distinct li.id from identifier li" +
                " join permission lp on (lp.inode_id = li.id) " +
                " INNER JOIN contentlet lc ON (lc.identifier = li.id and li.asset_type = 'contentlet')" +
                " INNER JOIN structure ls ON (lc.structure_inode = ls.inode)" +
                " where li.asset_type='contentlet' and lp.permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "' " +
                " AND ls.structuretype <> " + BaseContentType.HTMLPAGE.getType() +
                " and li.id <> li.host_inode and li.host_inode = ? " +
                " and li.parent_path like ?";

	/*
	 * To load content identifiers that are of the type of a structure
	 *
	 * Parameters
	 * 1. The content type inode
	 */
	private static final String SELECT_CHILD_CONTENT_BY_CONTENTTYPE_SQL =
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
		"	(" + SELECT_CHILD_CONTENT_BY_PATH_SQL + ")";

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
			"	" + SELECT_CHILD_CONTENT_BY_PATH_SQL + " and " +
			"permission_type = '" + Contentlet.class.getCanonicalName() + "' and asset_id = identifier.id)";

	private final String deleteContentReferencesByPathOnAddSQL =
		"delete from permission_reference where exists (" +
		"	" + SELECT_CHILD_CONTENT_BY_PATH_SQL + " and " +
		"permission_type = '" + Contentlet.class.getCanonicalName() + "' and asset_id = identifier.id) " +
		"and (reference_id in (" +
		"select distinct folder.inode " +
		"from folder join identifier on (folder.identifier = identifier.id) " +
		"where identifier.host_inode = ? " +
		"and ("+DOT_FOLDER_PATH+"(parent_path,asset_name) not like ? OR "+DOT_FOLDER_PATH+"(parent_path,asset_name) = ?) " +
		"and permission_type = '"+Contentlet.class.getCanonicalName()+"' " +
		"and reference_id = folder.inode" +
		") " +
		"OR EXISTS(SELECT c.inode " +
		"FROM contentlet c WHERE c.identifier = reference_id)	" +
		")";

	/*
	 * To remove all permissions of content under a given parent folder
	 * Parameters
	 * 1. content type inode
	 */
	private final String deleteContentPermissionsByStructureSQL =
		"delete from permission where inode_id in " +
		"	(" + SELECT_CHILD_CONTENT_BY_CONTENTTYPE_SQL + ")";

	/*
	 * To delete all permission references on content under a given content type
	 *
	 * Parameters
	 * 1. content type inode
	 */
	private static final String DELETE_CONTENT_REFERENCES_BY_CONTENTTYPE_SQL =
		"delete from permission_reference where exists (" +
		" select contentlet.identifier from contentlet " +
		" where contentlet.structure_inode = ? " +
		" and permission_reference.asset_id = contentlet.identifier " +
		" and permission_reference.permission_type = 'com.dotmarketing.portlets.contentlet.model.Contentlet' " +
		" group by contentlet.identifier)";




	/*
	 * To load content type identifiers that are in the same tree/hierarchy of a parent host/folder
	 *
	 * Parameters
	 * 1. path like to the folder hierarchy the content type lives under E.G /about/% (files under /about/)
	 * 2. The host id
	 * 3. The host id
	 */
	private static final String SELECT_CHILD_CONTENTTYPE_BY_PATH_SQL =
		"select distinct structure.inode from structure where ( " +
		"(structure.folder <> 'SYSTEM_FOLDER' AND exists(" +
		"         select folder.inode from folder join identifier on (identifier.id=folder.identifier) " +
		"         where structure.folder = folder.inode and "+DOT_FOLDER_PATH+"(parent_path,asset_name) like ?)) OR " +
		"(structure.host <> 'SYSTEM_HOST' AND structure.host = ?) OR " +
		"(structure.host = 'SYSTEM_HOST' AND exists (select inode from contentlet where title = 'System Host' AND inode = ?)))";


	/*
	 * To load content type identifiers that are in the same tree/hierarchy of a parent host/folder
	 *
	 * Parameters
	 * 1. path like to the folder hierarchy the content type lives under E.G /about/% (files under /about/)
	 * 2. The host id
	 * 3. The host id
	 */
	private static final String SELECT_CHILD_CONTENTTYPE_BY_PATH_SQL_FOLDER =
		"select distinct structure.inode from structure where ( " +
		"(structure.folder <> 'SYSTEM_FOLDER' AND exists(" +
		"            select folder.inode from folder join identifier on(identifier.id=folder.identifier) " +
		"            where structure.folder = folder.inode and "+DOT_FOLDER_PATH+"(parent_path,asset_name) like ?)) OR " +
		"(structure.host = 'SYSTEM_HOST' AND exists (select inode from contentlet where title = 'System Host' AND inode = ?)))";

	/*
	 * To delete all permission references on a content type under a given host/folder hierarchy
	 *
	 * Parameters
	 * 1. path like to the folder hierarchy the content type lives under E.G /about/% (files under /about/)
	 * 2. host the content type belongs to
	 * 3. host the content type belongs to
	 * 4. host the content type belongs to
	 * 5. same as 1
	 */
	private static final String DELETE_CONTENTTYPE_REFERENCES_BY_PATH_SQL =
			"delete from permission_reference where exists (" +
			"	" + SELECT_CHILD_CONTENTTYPE_BY_PATH_SQL + " and asset_id = structure.inode and " +
			"permission_type = '" + Structure.class.getCanonicalName() + "' and reference_id not in (" +
			"select ref_folder.inode from folder ref_folder join identifier ref_ident on (ref_folder.identifier = ref_ident.id) where " +
			"ref_ident.host_inode = ? and "+DOT_FOLDER_PATH+"(ref_ident.parent_path,ref_ident.asset_name) like ?))";


	/*
	 * To delete all permission references on a content type under a given host/folder hierarchy
	 *
	 * Parameters
	 * 1. path like to the folder hierarchy the content type lives under E.G /about/% (files under /about/)
	 * 2. host the content type belongs to
	 * 3. host the content type belongs to
	 * 4. host the content type belongs to
	 * 5. same as 1
	 */
	private final String deleteStructureReferencesByPathSQLFolder =
			"delete from permission_reference where exists (" +
			"	" + SELECT_CHILD_CONTENTTYPE_BY_PATH_SQL_FOLDER + " and asset_id = structure.inode and " +
			"permission_type = '" + Structure.class.getCanonicalName() + "' and reference_id not in (" +
			"select ref_folder.inode from folder ref_folder join identifier ref_ident on (ref_folder.identifier = ref_ident.id) where " +
			"ref_ident.host_inode = ? and "+DOT_FOLDER_PATH+"(ref_ident.parent_path,ref_ident.asset_name) like ?))";


	private static final String DELETE_CONTENTTYPE_REFERENCES_BY_PATH_ON_ADD_SQL =
		"delete from permission_reference where exists(" +
		"	" + SELECT_CHILD_CONTENTTYPE_BY_PATH_SQL + " and asset_id = structure.inode and " +
		"permission_type = '" + Structure.class.getCanonicalName() + "' and reference_id not in (" +
		"select ref_folder.inode from folder ref_folder join identifier ref_ident on (ref_folder.identifier = ref_ident.id) where " +
		"ref_ident.host_inode = ? and "+DOT_FOLDER_PATH+"(ref_ident.parent_path,ref_ident.asset_name) like ?)) " +
		"and (reference_id in (" +
		"select distinct folder.inode " +
		"from folder join identifier on(folder.identifier = identifier.id) " +
		"where " +
		"identifier.host_inode = ? " +
		"and ("+DOT_FOLDER_PATH+"(parent_path,asset_name) not like ? OR "+DOT_FOLDER_PATH+"(parent_path,asset_name) = ?) " +
		"and permission_type = 'com.dotmarketing.portlets.folders.model.Folder' " +
		"and reference_id = folder.inode" +
		") " +
		"OR EXISTS(SELECT c.inode " +
		"FROM contentlet c " +
		"  WHERE c.identifier = reference_id)	" +
		")";


	private final String deleteStructureReferencesByPathOnAddSQLFolder =
			"delete from permission_reference where exists(" +
			"	" + SELECT_CHILD_CONTENTTYPE_BY_PATH_SQL_FOLDER + " and asset_id = structure.inode and " +
			"permission_type = '" + Structure.class.getCanonicalName() + "' and reference_id not in (" +
			"select ref_folder.inode from folder ref_folder join identifier ref_ident on(ref_folder.identifier = ref_ident.id) where  " +
			"ref_ident.host_inode = ? and "+DOT_FOLDER_PATH+"(ref_ident.parent_path,ref_ident.asset_name) like ?)) " +
			"and (reference_id in (" +
			"select distinct folder.inode " +
			"from folder join identifier on (folder.identifier = identifier.id) " +
			"where " +
			"identifier.host_inode = ? " +
			"and ("+DOT_FOLDER_PATH+"(parent_path,asset_name) not like ? OR "+DOT_FOLDER_PATH+"(parent_path,asset_name) = ?) " +
			"and permission_type = 'com.dotmarketing.portlets.folders.model.Folder' " +
			"and reference_id = folder.inode" +
			") " +
			"OR EXISTS(SELECT c.inode " +
			"FROM contentlet c " +
			"  WHERE c.identifier = reference_id)	" +
			")";





	/*
	 * To remove all permissions of structures under a given parent folder
	 * Parameters
	 * 1. path like to the folder hierarchy the content type lives under E.G /about/% (files under /about/)
	 * 2. The host id
	 * 3. The host id
	 */
	private static final String DELETE_CONTENTTYPE_PERMISSIONS_BY_PATH_SQL =
		"delete from permission where inode_id in " +
		"	(" + SELECT_CHILD_CONTENTTYPE_BY_PATH_SQL + ")";


	/*
	 * To remove all permissions of structures under a given parent folder
	 * Parameters
	 * 1. path like to the folder hierarchy the content type lives under E.G /about/% (files under /about/)
	 * 2. The host id
	 * 3. The host id
	 */
	private final String deleteStructurePermissionsByPathSQLFolder =
		"delete from permission where inode_id in " +
		"	(" + SELECT_CHILD_CONTENTTYPE_BY_PATH_SQL_FOLDER + ")";

	/*
	 * To load content type identifiers that are children of a host and have inheritable permissions
	 * Parameters
	 * 1. path like to the folder hierarchy the content type lives under E.G /about/% (files under /about/)
	 * 2. The host id
	 * 3. The host id
	 */
	private static final String SELECT_CHILD_CONTENTTYPE_WITH_INDIVIDUAL_PERMISSIONS_BY_PATH_SQL =
		SELECT_CHILD_CONTENTTYPE_BY_PATH_SQL + " and exists (select * from permission where inode_id = structure.inode and " +
		"permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "')";

	private static final Map<PermissionType, String> SELECT_CHILDREN_WITH_INDIVIDUAL_PERMISSIONS_SQLS = new HashMap<>();

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

		SELECT_CHILDREN_WITH_INDIVIDUAL_PERMISSIONS_SQLS.put(PermissionType.TEMPLATE, SELECT_CHILD_TEMPLATE_WITH_INDIVIDUAL_PERMISSIONS_SQL);
		SELECT_CHILDREN_WITH_INDIVIDUAL_PERMISSIONS_SQLS.put(PermissionType.CONTAINER, SELECT_CHILD_CONTAINER_WITH_INDIVIDUAL_PERMISSIONS_SQL);
		SELECT_CHILDREN_WITH_INDIVIDUAL_PERMISSIONS_SQLS.put(PermissionType.FOLDER, SELECT_CHILD_FOLDER_WITH_DIRECT_PERMISSIONS_SQL);
		SELECT_CHILDREN_WITH_INDIVIDUAL_PERMISSIONS_SQLS.put(PermissionType.IHTMLPAGE, SELECT_CHILD_HTMLPAGE_WITH_INDIVIDUAL_PERMISSIONS_SQL);
		SELECT_CHILDREN_WITH_INDIVIDUAL_PERMISSIONS_SQLS.put(PermissionType.LINK, SELECT_CHILD_LINK_WITH_INDIVIDUAL_PERMISSIONS_SQL);
		SELECT_CHILDREN_WITH_INDIVIDUAL_PERMISSIONS_SQLS.put(PermissionType.CONTENTLET, SELECT_CHILD_CONTENT_WITH_INDIVIDUAL_PERMISSIONS_BY_PATH_SQL);
		SELECT_CHILDREN_WITH_INDIVIDUAL_PERMISSIONS_SQLS.put(PermissionType.STRUCTURE, SELECT_CHILD_CONTENTTYPE_WITH_INDIVIDUAL_PERMISSIONS_BY_PATH_SQL);
	}

	/**
	 * @param permissionCache
	 */
	public PermissionBitFactoryImpl(PermissionCache permissionCache) {
		super();
		this.permissionCache = permissionCache;
	}

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
		final List<Permission> permissions = getInheritablePermissions(permissionable, true);
		return permissions.stream().filter(permission -> permission.getType().equals(type)).collect(Collectors.toList());
	}

	@Override
	protected List<Permission> getInheritablePermissions(Permissionable permissionable) throws DotDataException {
		return getInheritablePermissions(permissionable, true);
	}

	@Override
	protected List<Permission> getInheritablePermissions(Permissionable permissionable, boolean bitPermissions) throws DotDataException {
		List<Permission> bitPermissionsList = permissionCache.getPermissionsFromCache(permissionable.getPermissionId());
		if (bitPermissionsList == null) {
			bitPermissionsList = loadPermissions(permissionable);
		}

		if(!bitPermissions)
			return convertToNonBitPermissions(filterOnlyInheritablePermissions(bitPermissionsList, permissionable.getPermissionId()));
		else
			return filterOnlyInheritablePermissions(bitPermissionsList, permissionable.getPermissionId());

	}

	@Override
	protected List<Permission> getPermissions(Permissionable permissionable) throws DotDataException {
		return getPermissions(permissionable, true);
	}


	@Override
	protected List<Permission> getPermissions(Permissionable permissionable, boolean bitPermissions) throws DotDataException {
		return getPermissions(permissionable, bitPermissions, false);
	}

    @Override
    protected void addPermissionsToCache ( Permissionable permissionable ) throws DotDataException {

        //Checking individual permissions
        List<Permission> bitPermissionsList = permissionCache.getPermissionsFromCache( permissionable.getPermissionId() );
        if (bitPermissionsList == null) {//Already in cache
            bitPermissionsList = loadPermissions( permissionable );
			Logger.debug(this, "addPermissionsToCache - Adding permissions to cache for permissionable: " + permissionable.getPermissionId() + " with permissions: " + bitPermissionsList);
            permissionCache.addToPermissionCache( permissionable.getPermissionId(), bitPermissionsList );
        }
    }

	@Override
	protected List<Permission> getPermissions(Permissionable permissionable, boolean bitPermissions, boolean onlyIndividualPermissions) throws DotDataException {

		return getPermissions(permissionable, bitPermissions, onlyIndividualPermissions, false);

	}

	@Override
	protected List<Permission> getPermissions(Permissionable permissionable, boolean bitPermissions,
			boolean onlyIndividualPermissions, boolean forceLoadFromDB) throws DotDataException {

		if (!InodeUtils.isSet(permissionable.getPermissionId())) {
			return new ArrayList<>();
		}

		List<Permission> bitPermissionsList = null;

		bitPermissionsList = loadPermissions(permissionable, forceLoadFromDB);
		bitPermissionsList = filterOnlyNonInheritablePermissions(bitPermissionsList,
				permissionable.getPermissionId());

		if (!bitPermissions) {
			bitPermissionsList = convertToNonBitPermissions(bitPermissionsList);
		}
		return onlyIndividualPermissions ? filterOnlyIndividualPermissions(bitPermissionsList,
				permissionable.getPermissionId()) : bitPermissionsList;
	}

	@Override
	protected void removePermissions(final Permissionable permissionable) throws DotDataException {
		removePermissions(Collections.singletonList(permissionable));
	}


	protected void removePermissions(final List<Permissionable> permissionables) throws DotDataException{

		final List<Params> paramsList = permissionables.stream()
				.map(permissionable -> new Params(
						permissionable.getPermissionId())
				).collect(Collectors.toList());

		final List<Integer> batchResult =
				Ints.asList(new DotConnect().executeBatch(DELETE_PERMISSION_BY_INODE, paramsList));

		Logger.debug(PermissionBitFactoryImpl.class,
				() -> "removePermissions batch results: " + batchResult.stream().map(Object::toString)
						.collect(Collectors.joining(",")));

        for(final Permissionable permissionable:permissionables){
		   resetPermissionReferences(permissionable);
		   permissionCache.remove(permissionable.getPermissionId());
		}
	}

	/*
	 * updates all permission references that are not pointing to the given permissionable but should,
	 * this happens when a new inheritable permission is added
	 *
	 */
	@SuppressWarnings("unchecked")
	private void updatePermissionReferencesOnAdd(final Permissionable permissionable) throws DotDataException {

		String parentPermissionableId = permissionable.getPermissionId();

		final boolean isHost = permissionable instanceof Host ||
		(permissionable instanceof Contentlet && ((Contentlet)permissionable).isHost());
		final boolean isFolder = permissionable instanceof Folder;
		final boolean isCategory = permissionable instanceof Category;
		final boolean isContentType = permissionable instanceof Structure || permissionable instanceof ContentType;

		if(!isHost && !isFolder && !isCategory && !isContentType) {
			return;
		}
		final HostAPI hostAPI = APILocator.getHostAPI();
		final Host systemHost = hostAPI.findSystemHost();
		final DotConnect dc = new DotConnect();

		boolean ran01=false,ran02=false,ran03=false,ran04=false,
		        ran06=false,ran07=false,ran08=false,ran09=false,ran10=false;

		final List<Map<String, Object>> idsToClear = new ArrayList<>();
		final List<Permission> permissions = filterOnlyInheritablePermissions(loadPermissions(permissionable, true), parentPermissionableId);
		for(final Permission p : permissions) {

			if (isHost || isFolder) {

				Contentlet parentHost = null;
				if(isFolder)
					try {
						parentHost = hostAPI.findParentHost((Folder)permissionable, APILocator.getUserAPI().getSystemUser(), false);
					} catch (DotSecurityException e) {
						Logger.error(this, e.getMessage(), e);
					}
				else {
					parentHost = (Contentlet) permissionable;
				}
				final String path = isFolder ? APILocator.getIdentifierAPI().find(((Folder) permissionable).getIdentifier()).getPath() : StringPool.BLANK;

				// Only if permissions were updated to a host != to the system
				// host
				if (!permissionable.getPermissionId().equals(systemHost.getPermissionId())) {

					if (isHost && p.getType().equals(Template.class.getCanonicalName()) && !ran01) {
						// Find all host templates pointing to the system host
						// and update their references

						// Removing all references to the system host
						dc.setSQL(this.deleteTemplateReferencesSQL);
						dc.addParam(permissionable.getPermissionId());
						dc.loadResult();


						// Retrieving the list of templates changed to clear
						// their caches
						dc.setSQL(SELECT_CHILD_TEMPLATE_SQL);
						dc.addParam(permissionable.getPermissionId());
						idsToClear.addAll(dc.loadResults());

						ran01=true;
					} else if (isHost && p.getType().equals(Container.class.getCanonicalName()) && !ran02) {
						// Find all host containers pointing to the system host
						// and update their references

						// Removing all references to the system host
						dc.setSQL(this.deleteContainerReferencesSQL);
						dc.addParam(permissionable.getPermissionId());
						dc.loadResult();

						// Retrieving the list of container changed to clear
						// their caches
						dc.setSQL(SELECT_CHILD_CONTAINER_SQL);
						dc.addParam(permissionable.getPermissionId());
						idsToClear.addAll(dc.loadResults());

						ran02=true;

					}else if (p.getType().equals(Folder.class.getCanonicalName()) && !ran03) {
						// Find all subfolders
						// Removing all references to the system host
						dc.setSQL(this.deleteSubfolderReferencesSQLOnAdd);
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						dc.addParam(isHost ? " " : path);
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						dc.addParam(path);
						dc.loadResult();


						// Retrieving the list of container changed to clear
						// their caches
						dc.setSQL(SELECT_CHILD_FOLDER_SQL);
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						dc.addParam(isHost ? " " : path);
						idsToClear.addAll(dc.loadResults());

						ran03=true;
					} else if (p.getType().equals(IHTMLPage.class.getCanonicalName()) && !ran04) {

						// Update html page references

						// Removing all references to the system host
						dc.setSQL(this.deleteHTMLPageReferencesOnAddSQL);
						// All the pages that belongs to the host
						dc.addParam(parentHost.getPermissionId());
						// Under any folder
						dc.addParam(path + "%");
                        dc.addParam(parentHost.getPermissionId());
                        dc.addParam(path + "%");
						dc.addParam(parentHost.getPermissionId());
						dc.addParam( path + "%" );
						dc.addParam(path);
						dc.loadResult();


						// Retrieving the list of pages changed to clear their
						// caches
						dc.setSQL(SELECT_CHILD_HTMLPAGE_SQL);
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						idsToClear.addAll(dc.loadResults());

						ran04=true;
					} else if (p.getType().equals(Link.class.getCanonicalName()) && !ran06) {
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



						// Retrieving the list of links changed to clear their
						// caches
						dc.setSQL(SELECT_CHILD_LINK_SQL);
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						idsToClear.addAll(dc.loadResults());

						ran06=true;

					} else if (p.getType().equals(Contentlet.class.getCanonicalName()) && !ran07) {
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



						// Retrieving the list of links changed to clear their
						// caches
						dc.setSQL(SELECT_CHILD_CONTENT_BY_PATH_SQL);
						dc.addParam(parentHost.getPermissionId());
						dc.addParam(path + "%");
						idsToClear.addAll(dc.loadResults());

						ran07=true;

					} else if (p.getType().equals(Structure.class.getCanonicalName()) && !ran08) {

						if(isHost){
							dc.setSQL(DELETE_CONTENTTYPE_REFERENCES_BY_PATH_ON_ADD_SQL);
							dc.addParam(path + "%");
							dc.addParam(parentHost.getPermissionId());
							dc.addParam(parentHost.getPermissionId());
							dc.addParam(parentHost.getPermissionId());
							dc.addParam(path + "%");
							dc.addParam(parentHost.getPermissionId());
							dc.addParam(path + "%");
							dc.addParam(path);
							dc.loadResult();



							// Retrieving the list of structures changed to clear
							// their caches

							dc.setSQL(SELECT_CHILD_CONTENTTYPE_BY_PATH_SQL);
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


							// Retrieving the list of structures changed to clear
							// their caches
							dc.setSQL(SELECT_CHILD_CONTENTTYPE_BY_PATH_SQL_FOLDER);
							dc.addParam(path + "%");
							dc.addParam(parentHost.getPermissionId());
							List<Map<String,Object>> sts=dc.loadResults();
							idsToClear.addAll(sts);

							Iterator<Map<String,Object>> it = sts.iterator();
							while(it.hasNext()) {
							    dc.setSQL(SELECT_CHILD_CONTENT_BY_CONTENTTYPE_SQL);
							    dc.addParam(it.next().get("inode"));
							    idsToClear.addAll(dc.loadResults());
							}
						}
						ran08=true;
					}
				} else {
					// If the system host we need to force all references of the
					// type of the permissionable
				    dc.setSQL(SELECT_PERMISSION_REFERENCE_SQL);
				    dc.addParam(permissionable.getPermissionId());
				    dc.addParam(p.getType());
				    idsToClear.addAll(dc.loadResults());
				}
			} else if(isCategory) {
			    if(!ran09) {
    				Category cat = (Category) permissionable;
    				CategoryAPI catAPI = APILocator.getCategoryAPI();
    				User systemUser = APILocator.getUserAPI().getSystemUser();
    				try {
    					List<Category> children = catAPI.getCategoryTreeDown(cat, cat, systemUser, false);
    					for(Category child : children) {
    						dc.setSQL(UPDATE_PERMISSION_REFERENCE_BY_ASSETID_SQL);
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
    				ran09=true;
			    }
			} else if (isContentType) {
			    if(!ran10) {
    				// Removing all references to the system host
    				dc.setSQL(DELETE_CONTENT_REFERENCES_BY_CONTENTTYPE_SQL);
    				// All the content that belongs to the host
    				dc.addParam(permissionable.getPermissionId());
    				dc.loadResult();

    				dc.setSQL(SELECT_CHILD_CONTENT_BY_CONTENTTYPE_SQL);
    				dc.addParam(permissionable.getPermissionId());
    				idsToClear.addAll(dc.loadResults());
        				
    				ran10=true;
			    }
			}
		}

		//Clearing the caches
		for(Map<String, Object> idToClear: idsToClear) {
		    String inode = (String)(idToClear.get("inode") != null?idToClear.get("inode"):idToClear.get("asset_id"));
		    if(inode==null) inode=(String)idToClear.get("id");
		    permissionCache.remove(inode);
		}

        if(isFolder) {
            ContentletAPI contAPI = APILocator.getContentletAPI();
            contAPI.refreshContentUnderFolder((Folder)permissionable);
        }
	}

	
	/**
	 * returns all permission references that reference 
	 * a given permissionable
	 * @param permissionable
	 * @return
	 * @throws DotDataException
	 */
	private List<PermissionReference> loadAllPermissionReferencesTo(Permissionable permissionable) throws DotDataException{
	    HibernateUtil hu = new HibernateUtil(PermissionReference.class);
	    hu.setQuery(LOAD_PERMISSION_REFERENCES_BY_REFERENCEID_HSQL);
	    hu.setParam(permissionable.getPermissionId());
	    return  hu.list();
	}
	
	
	
	/*
	 * updates all permission references that are pointing to the given permissionable if this
	 * permissionable no longer provides the inheritable permissions that the children require
	 * this happens when inheritable permissions have being removed from the given permissionable
	 */
	private void dbDeletePermissionReferences(final List<Permissionable> permissionables)
			throws DotDataException {
		final DotConnect dotConnect = new DotConnect();
		final List<Params> paramsList = permissionables.stream()
				.map(permissionable -> new Params(
				       permissionable.getPermissionId(), permissionable.getPermissionId())
				).collect(Collectors.toList());
		final List<Integer> batchResult = Ints
				.asList(dotConnect.executeBatch(DELETE_PERMISSION_REFERENCE_SQL, paramsList));
		for (final Permissionable permissionable : permissionables) {
			permissionCache.remove(permissionable.getPermissionId());
		}
		Logger.debug(PermissionBitFactoryImpl.class,
				() -> "dbDeletePermissionReferences batch results: " + batchResult.stream().map(Object::toString)
						.collect(Collectors.joining(",")));
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
				toPersist = new Permission(p.getType(), p.getInode(), p.getRoleId(), 0);
				newPermission = true;
			}
			if((toPersist.getPermission() | p.getPermission()) == toPersist.getPermission() && !newPermission)
				persist = false;
			toPersist.setPermission(toPersist.getPermission() | p.getPermission());
		}

		if(toPersist.getPermission() == 0 && toPersist.getId() > 0) {
			dbDeletePermission(toPersist);
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




	/**
	 * @deprecated Use {@link PermissionBitFactoryImpl#savePermission(Permission, Permissionable)} instead.
	 */
	@Override
    @Deprecated
	protected void assignPermissions(List<Permission> permissions, Permissionable permissionable) throws DotDataException {
		boolean anyNew = false;

		for(Permission permission:permissions) {
			final PermissionSaveResult permissionSaveResult = savePermission(permission,
					permissionable, false);

			if (permissionSaveResult.result == PersistResult.NEW) {
				anyNew = true;
			}
		}

		if(anyNew) {
			resetPermissionReferences(permissionable);
		} else {
			refreshPermissionable(permissionable);
		}
	}

	@Override
	protected Permission savePermission(Permission permission, Permissionable permissionable) throws DotDataException {
		return savePermission(permission, permissionable, true).permission;
	}

	private PermissionSaveResult savePermission(final  Permission permission, final  Permissionable permissionable, final boolean refreshPermissionable) throws DotDataException {


		if(!permission.getInode().equals(permissionable.getPermissionId()))
			throw new DotDataException("You cannot update permissions of a different permissionable id than the one you are passing to the method");

		PersistResult result = persistPermission(permission);

		if (!permission.isIndividualPermission()) {
			switch(result) {
				case NEW:
					updatePermissionReferencesOnAdd(permissionable);
					break;
				case REMOVED:
				  resetPermissionReferences(permissionable);
					break;
				case UPDATED:
				  resetPermissionReferences(permissionable);
					break;
				}
		} else {
			if(result == PersistResult.NEW && refreshPermissionable) {
			  resetPermissionReferences(permissionable);
			}
		}

		permissionCache.remove(permissionable.getPermissionId());

		if (refreshPermissionable) {
			refreshPermissionable(permissionable);
		}

		return new PermissionSaveResult(findPermissionByInodeAndRole(permission.getInode(), permission.getRoleId(), permission.getType()), result);
	}

	private void refreshPermissionable(Permissionable permissionable) throws DotDataException {
		if(permissionable instanceof Structure) {
			ContentletAPI contAPI = APILocator.getContentletAPI();
			contAPI.refresh((Structure) permissionable);
		} else if (permissionable instanceof ContentType) {
            final Structure contentType = new StructureTransformer(ContentType.class.cast(
					permissionable)).asStructure();
            APILocator.getContentletAPI().refresh(contentType);
        } else if(permissionable instanceof Contentlet) {
			ContentletAPI contAPI = APILocator.getContentletAPI();
			((Contentlet) permissionable).setLowIndexPriority(true);
			contAPI.refresh((Contentlet) permissionable);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<User> getUsers(Permissionable permissionable, int permissionType, String filter, int start, int limit) {
    	try
    	{

    		RoleAPI roleAPI = APILocator.getRoleAPI();

    		List<Permission> allPermissions = getPermissions(permissionable);
    		List<String> roleIds = new ArrayList<>();
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

			ArrayList<User> users = new ArrayList<>();

			DotConnect dotConnect = new DotConnect();
			String userFullName = DotConnect.concat(new String[] { "user_.firstName", "' '", "user_.lastName" });

			StringBuffer baseSql = new StringBuffer("select distinct (user_.userid), ");
			baseSql.append(userFullName);
			baseSql.append(" from user_, users_cms_roles where");
			baseSql.append(" user_.companyid = ? and user_.userid <> 'system' ");
			baseSql.append(" and users_cms_roles.role_id in (" + roleIdsSB.toString() + ")");
			baseSql.append(" and user_.userId = users_cms_roles.user_id ");

			baseSql.append(" and user_.delete_in_progress = ");
			baseSql.append(DbConnectionFactory.getDBFalse());

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

	@Override
	protected int getUserCount(Permissionable permissionable, int permissionType, String filter) {
    	try
    	{
    		RoleAPI roleAPI = APILocator.getRoleAPI();

    		List<Permission> allPermissions = getPermissions(permissionable);
    		List<String> roleIds = new ArrayList<>();
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
			baseSql.append(" and user_.delete_in_progress = ");
			baseSql.append(DbConnectionFactory.getDBFalse());

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

	@Override
	protected List<Permission> getPermissionsFromCache(String permissionableId) {
		List<Permission> l = null;
		l = permissionCache.getPermissionsFromCache(permissionableId);
		return l;
	}

	@Override
	protected Map<Permissionable, List<Permission>> getPermissions(List<Permissionable> permissionables) throws DotDataException, DotSecurityException {

		return getPermissions(permissionables, true);
	}

	@Override
	protected Map<Permissionable, List<Permission>> getPermissions(List<Permissionable> permissionables, boolean bitPermission)
		throws DotDataException, DotSecurityException {

		Map<Permissionable, List<Permission>> result = new HashMap<>();

		for(Permissionable p : permissionables) {
			List<Permission> permission = getPermissions(p, bitPermission);
			result.put(p, permission);
		}

		return result;

	}

	@Override
	public void removePermissionsByRole(String roleId) {

		try {
			DotConnect db = new DotConnect();
			db.setSQL("delete from permission where roleid='"+roleId+"'");
			db.loadResult();

			permissionCache.clearCache();
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DataAccessException (e.getMessage(), e);
		}
	}

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

		List<HashMap<String, String>> inodes = new ArrayList<>();
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
	 * @param inode
	 * @param roleId
	 * @param permissionType
	 * @return
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
	 * This method let you convert a list of bit permission to the old non bit kind of permission, so you
	 * end up with a longer list
	 * @param bitPermissionsList
	 * @return
	 */
	private List<Permission> convertToNonBitPermissions (List<Permission> bitPermissionsList) {
		Set<Permission> permissionsSet = new LinkedHashSet<>();

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
		return new ArrayList<> (permissionsSet);

	}




  private void dbDeletePermission(Permission p) {

    try {
      new DotConnect().setSQL("delete from permission where id=?").addParam(p.getId()).loadResult();

      if(p.isBitPermission()) {
        new DotConnect().setSQL("delete from permission where inode_id=? and roleid=? and permission=?").addParam(p.getInode()).addParam(p.getRoleId()).addParam(p.getPermission()).loadResult();
      }
      permissionCache.remove(p.getInode());
      new HibernateUtil().evict(p);

    } catch (DotDataException dhe) {
      String cause = String.format("deletePermission: Unable to delete %s in database", p.toString());
      Logger.error(this.getClass(), cause, dhe);
      throw new DataAccessException(cause, dhe);
    }

  }

  @CloseDBIfOpened
  private List<Permission> loadPermissions(final Permissionable permissionable, final boolean forceDB) throws DotDataException {
     	if(forceDB){
     	   permissionCache.remove(permissionable.getPermissionId());
     	}
     	return loadPermissions(permissionable);
  }

  
  private Lazy<DotSubmitter> submitter = Lazy.of(()-> DotConcurrentFactory.getInstance().getSubmitter("permissionreferences", new SubmitterConfigBuilder().poolSize(1).maxPoolSize(5).queueCapacity(10000).build()));
  
  
  @CloseDBIfOpened
  @SuppressWarnings("unchecked")
  private List<Permission> loadPermissions(final Permissionable permissionable) throws DotDataException {

    final String permissionKey = Try.of(() -> permissionable.getPermissionId())
        .getOrElseThrow(() -> new DotDataException("Invalid Permissionable passed in. permissionable:" + permissionable));

    
    // Step 1. cache lookup first
    List<Permission> permissionList = permissionCache.getPermissionsFromCache(permissionKey);
    if (permissionList != null) {
      return permissionList;
    }

    /*
     * Step 2. check the permission_reference table 
     * In this step, we try to find our entries in the
     * permission_reference table We lock to lookup, check if cache has been loaded while we have been
     * waiting, and if so, use it, otherwise we query the permission_reference table
     */
    permissionList = _loadPermissionsFromDb(permissionable,permissionKey);


    // if we've found permissions, return'em
    if (!permissionList.isEmpty()) {
      return permissionList;
    }
	Logger.debug(this.getClass(), ()-> "Permissionable:" + permissionable + " has no permissions in the cache or in the permission_reference table, looking for parent permissionable");

    /*
     * Step 3. Recursive calls to find our "parent permissionable" 
     * If we don't have any permissions in
     * cache or entries in the permission_reference table, we need to find our "parent permissionable"
     * and store that in the permission_reference table for a faster lookup in Step 2.
     */
    final String type = resolvePermissionType(permissionable);
    final Tuple2<Permissionable,List<Permission>> parentPerms =_loadParentPermissions(permissionable,permissionKey);
    final Permissionable finalNewReference = parentPerms._1;
    permissionList = parentPerms._2;
	Logger.debug(this.getClass(), "loadPermissions - Permissionable:" + permissionable + " found parent permissionable:" + finalNewReference + " with permissions:" + permissionList);
    permissionCache.addToPermissionCache(permissionKey, permissionList);
    /*
     * Step 4. Upsert into the permission_reference table 
     * We have found our "parent permissionable", now
     * we have to lock again in order to UPSERT our parent permissionable in the permission_reference
     * table
     */
    
    if(Config.getBooleanProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", true)) {
        submitter.get().submit( () -> {
            try {
                upsertPermissionReferences(permissionable, type, finalNewReference);
            }
            catch(Exception e) {
                Logger.warnAndDebug(this.getClass(), "Permission References failed to update for permissionable:" + permissionable + " TYPE:" + type + " finalNewReference:" + finalNewReference, e);
            }
        });
    }
    else {
        upsertPermissionReferences(permissionable, type, finalNewReference);
    }
    
    Logger.debug(this.getClass(), ()-> "Permission inherited for permissionable:" + permissionable + " TYPE:" + type + " finalNewReference:" + finalNewReference);
	
    return permissionList;

  }

  
  private class ReadPermissionSupplier implements Supplier<List<Permission>> {

      final String permissionKey, permissionId;

      ReadPermissionSupplier(final String permissionKey, final String permissionId) {
          this.permissionId = permissionId;
          this.permissionKey = permissionKey;
      }

      @Override
      public List<Permission> get() {

          List<Permission> bitPermissionsList = permissionCache.getPermissionsFromCache(permissionKey);
          if (bitPermissionsList != null) {
              return bitPermissionsList;
          }
          HibernateUtil persistenceService = new HibernateUtil(Permission.class);
          Try.run(() -> persistenceService.setSQLQuery(LOAD_PERMISSION_SQL));
          persistenceService.setParam(permissionId);
          persistenceService.setParam(permissionId);
          bitPermissionsList = (List<Permission>) Try.of(() -> persistenceService.list())
                          .getOrElseThrow(e -> new DotRuntimeException(e));
          bitPermissionsList.forEach(p -> p.setBitPermission(true));

		  Logger.debug(this.getClass(), "get() - Permissionable:" + permissionKey + " found permissions:" + bitPermissionsList);
          // adding to cache if found
          if (!bitPermissionsList.isEmpty()) {
              permissionCache.addToPermissionCache(permissionKey, bitPermissionsList);
          }
          return bitPermissionsList;

      }

  }
  

  private List<Permission> _loadPermissionsFromDb(final Permissionable permissionable, final String permissionKey) throws DotDataException {
      
      final Supplier<List<Permission>> readPermissions = new ReadPermissionSupplier(permissionKey,permissionable.getPermissionId());
      
      if(Config.getBooleanProperty("PERMISSION_LOCK_ON_READ", false)) {
          return Try.of(() -> lockManager.tryLock(LOCK_PREFIX + permissionKey, () -> {
              return readPermissions.get();
          })).getOrElseThrow(e -> new DotRuntimeException(e));
      }
      
      return readPermissions.get();
      

  }
  
  private Tuple2<Permissionable,List<Permission>> _loadParentPermissions(final Permissionable permissionable, final String permissionKey) throws DotDataException {

      List<Permission> permissionList = new ArrayList<>();
      final String type = resolvePermissionType(permissionable);

      Permissionable parentPermissionable = permissionable.getParentPermissionable();

      while (parentPermissionable != null) {
        permissionList = getInheritablePermissions(parentPermissionable, type);
        if (!permissionList.isEmpty() || Host.SYSTEM_HOST.equals(parentPermissionable.getPermissionId())) {
           break;
        }
        parentPermissionable = parentPermissionable.getParentPermissionable();
      }

      final Permissionable finalNewReference = (parentPermissionable == null) ? APILocator.systemHost() : parentPermissionable;
      return Tuple.of(finalNewReference,permissionList);

  }
  
  private String resolvePermissionType(final Permissionable permissionable) {
    // Need to determine who this asset should inherit from
    String type = permissionable.getPermissionType();
    if (permissionable instanceof Host || (permissionable instanceof Contentlet && ((Contentlet) permissionable).getStructure() != null
        && ((Contentlet) permissionable).getStructure().getVelocityVarName() != null
        && ((Contentlet) permissionable).getStructure().getVelocityVarName().equals("Host"))) {
      type = Host.class.getCanonicalName();
    } else if (permissionable instanceof Contentlet
        && BaseContentType.FILEASSET.getType() == ((Contentlet) permissionable).getStructure().getStructureType()) {
      type = Contentlet.class.getCanonicalName();
    } else if (permissionable instanceof IHTMLPage || (permissionable instanceof Contentlet
        && BaseContentType.HTMLPAGE.getType() == ((Contentlet) permissionable).getStructure().getStructureType())) {
      type = IHTMLPage.class.getCanonicalName();
    } else if (permissionable instanceof Event) {
      type = Contentlet.class.getCanonicalName();
    } else if (permissionable instanceof Identifier) {
      Permissionable perm = InodeFactory.getInode(permissionable.getPermissionId(), Inode.class);
      Logger.error(this,
          "PermissionBitFactoryImpl :  loadPermissions Method : was passed an identifier. This is a problem. We will get inode as a fallback but this should be reported");
      if (perm != null) {
        if (perm instanceof IHTMLPage || (perm instanceof Contentlet
            && BaseContentType.HTMLPAGE.getType() == ((Contentlet) perm).getStructure().getStructureType())) {
          type = IHTMLPage.class.getCanonicalName();
        } else if (perm instanceof Container) {
          type = Container.class.getCanonicalName();
        } else if (perm instanceof Folder) {
          type = Folder.class.getCanonicalName();
        } else if (perm instanceof Link) {
          type = Link.class.getCanonicalName();
        } else if (perm instanceof Template) {
          type = Template.class.getCanonicalName();
        } else if (perm instanceof Structure || perm instanceof ContentType) {
          type = Structure.class.getCanonicalName();
        } else if (perm instanceof Contentlet || perm instanceof Event) {
          type = Contentlet.class.getCanonicalName();
        }
      }
    }

    if (permissionable instanceof Template && UtilMethods.isSet(((Template) permissionable).isDrawed())
        && ((Template) permissionable).isDrawed()) {
      type = TemplateLayout.class.getCanonicalName();
    }

    if (permissionable instanceof NavResult) {
      type = ((NavResult) permissionable).getEnclosingPermissionClassName();
    }
    return type;
  }




	protected static final String PERMISSION_REFERENCE = "permission_reference";
	protected static final String ASSET_ID = "asset_id";
	protected static final String REFERENCE_ID = "reference_id";
	protected static final String PERMISSION_TYPE = "permission_type";
	protected static final String ID = "id";

    @WrapInTransaction
    private void upsertPermissionReferences(Permissionable permissionable, final String type,
                    final Permissionable newReference) throws DotDataException {

        if (permissionable==null || permissionable.getPermissionId() == null || newReference == null || newReference.getPermissionId() == null) {
            throw new DotRuntimeException("Failed to insert Permission Ref.  Permissionable:" + permissionable
                            + " Parent : " + newReference + " Type: " + type);
        }
        
        final String permissionId = permissionable.getPermissionId();

        Logger.debug(this.getClass(),
                        () -> "PERMDEBUG: " + Thread.currentThread().getName() + " - " + permissionId + " - started");

        DotConnect dc1 = new DotConnect();

        Logger.debug(this.getClass(), () -> "UPSERTING permission_reference = assetId:" + permissionId + " type:" + type
                        + " reference:" + newReference.getPermissionId());

        upsertPermission(dc1, permissionId, newReference, type);


    }

	/**
	 * Method to Insert or Update a Permission Reference
	 * @param dc DotConnect
	 * @param permissionId the asset id to be inserted in the permission reference
	 * @param newReference the reference
	 * @param type the asset type
	 * @throws DotDataException
	 */
    private void upsertPermission(DotConnect dc, String permissionId, Permissionable newReference, String type)
									throws DotDataException {
		UpsertCommand upsertCommand = UpsertCommandFactory.getUpsertCommand();

		SimpleMapAppContext replacements = new SimpleMapAppContext();
		replacements.setAttribute(QueryReplacements.TABLE, PERMISSION_REFERENCE);
		replacements.setAttribute(QueryReplacements.CONDITIONAL_COLUMN, ASSET_ID);
		replacements.setAttribute(QueryReplacements.CONDITIONAL_VALUE, permissionId);
		replacements.setAttribute(QueryReplacements.EXTRA_COLUMNS, new String[]{REFERENCE_ID, PERMISSION_TYPE});

		if (DbConnectionFactory.isPostgres()) {
			replacements.setAttribute(QueryReplacements.ID_COLUMN, ID);
			replacements.setAttribute(QueryReplacements.ID_VALUE, "nextval('permission_reference_seq')");
		}
		if (DbConnectionFactory.isOracle()) {
			replacements.setAttribute(QueryReplacements.ID_COLUMN, ID);
			replacements.setAttribute(QueryReplacements.ID_VALUE, "permission_reference_seq.NEXTVAL");
		}

		upsertCommand.execute(dc, replacements, permissionId, newReference.getPermissionId(), type);
	}

    private List<Permission> filterOnlyNonInheritablePermissions(List<Permission> permissions, String permissionableId) {
		List<Permission> filteredList = new ArrayList<>();
		for(Permission p: permissions) {
			if((p.isIndividualPermission() && p.getInode().equals(permissionableId)) || !p.getInode().equals(permissionableId))
				filteredList.add(p);
		}
		return filteredList;
	}

	private List<Permission> filterOnlyInheritablePermissions(List<Permission> permissions, String permissionableId) {
		List<Permission> filteredList = new ArrayList<>();
		for(Permission p: permissions) {
			if(!p.isIndividualPermission() && p.getInode().equals(permissionableId))
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
		List<Permission> filteredList = new ArrayList<>();
		for(Permission p: permissions) {
			if(p.isIndividualPermission() && p.getInode().equals(permissionableId))
				filteredList.add(p);
		}
		return filteredList;
	}



	@SuppressWarnings("unchecked")
	@Override
	List<Permission> getPermissionsByRole(Role role, boolean onlyFoldersAndHosts, boolean bitPermissions) throws DotDataException {


		StringBuilder query = new StringBuilder();
		query.append("select distinct {permission.*} from permission ");
		if(onlyFoldersAndHosts) {
		    query.append("  join contentlet on (contentlet.identifier=permission.inode_id and structure_inode=?) ")
		         .append("  where permission.roleid =? ")
		         .append("  union all ")
		         .append("  select {permission.*} from permission join folder on (permission.inode_id=folder.inode) ");
		}
		query.append(" where permission.roleid = ? ");

		HibernateUtil persistenceService = new HibernateUtil(Permission.class);
		persistenceService.setSQLQuery(query.toString());
		if(onlyFoldersAndHosts) {
            Structure hostStructure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
            persistenceService.setParam(hostStructure.getInode());
            persistenceService.setParam(role.getId());
        }
		persistenceService.setParam(role.getId());

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
	void resetPermissionsUnder(final Permissionable permissionable) throws DotDataException {

		if(!permissionable.isParentPermissionable())
			return;

		final boolean isHost = permissionable instanceof Host ||
			(permissionable instanceof Contentlet && ((Contentlet)permissionable).getStructure().getVelocityVarName().equals("Host"));
		final boolean isFolder = permissionable instanceof Folder;
		final boolean isContentType = permissionable instanceof Structure || permissionable instanceof ContentType;
		final boolean isCategory = permissionable instanceof Category;

		DotConnect dc = new DotConnect();
		HostAPI hostAPI = APILocator.getHostAPI();
		User systemUser = APILocator.getUserAPI().getSystemUser();

		//Search all children remove individual permissions and permission references and make them point to this permissionable
		List<Map<String, String>> idsToClear = new ArrayList<>();
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

				//Retrieving the list of templates to clear their caches later
				dc.setSQL(SELECT_CHILD_TEMPLATE_SQL);
				dc.addParam(host.getPermissionId());
				idsToClear.addAll(dc.loadResults());

				//Removing permissions and permission references for all children containers
				dc.setSQL(deleteContainerReferencesSQL);
				dc.addParam(host.getPermissionId());
				dc.loadResult();
				dc.setSQL(deleteContainerPermissionsSQL);
				dc.addParam(host.getPermissionId());
				dc.loadResult();

				//Retrieving the list of containers to clear their caches later
				dc.setSQL(SELECT_CHILD_CONTAINER_SQL);
				dc.addParam(host.getPermissionId());
				idsToClear.addAll(dc.loadResults());

			}
			String folderPath = "";
			if(!isHost) {
				folderPath = APILocator.getIdentifierAPI().find(folder.getIdentifier()).getPath();
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

			//Retrieving the list of sub folders changed to clear their caches
			dc.setSQL(SELECT_CHILD_FOLDER_SQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			dc.addParam(isHost?" ":folderPath+"");
			idsToClear.addAll(dc.loadResults());

			//Removing permissions and permission references for all children containers
			dc.setSQL(deleteHTMLPageReferencesSQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
            dc.addParam( host.getPermissionId() );
            dc.addParam( isHost ? "%" : folderPath + "%" );
			dc.loadResult();
			dc.setSQL( deleteHTMLPagePermissionsSQL );
			dc.addParam( host.getPermissionId() );
			dc.addParam(isHost?"%":folderPath+"%");
            dc.addParam( host.getPermissionId() );
            dc.addParam(isHost?"%":folderPath+"%");
			dc.loadResult();

			//Retrieving the list of htmlpages changed to clear their caches
			dc.setSQL(SELECT_CHILD_HTMLPAGE_SQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
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

			//Retrieving the list of links changed to clear their caches
			dc.setSQL(SELECT_CHILD_LINK_SQL);
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

			//Retrieving the list of content changed to clear their caches
			dc.setSQL(SELECT_CHILD_CONTENT_BY_PATH_SQL);
			dc.addParam(host.getPermissionId());
			dc.addParam(isHost?"%":folderPath+"%");
			idsToClear.addAll(dc.loadResults());


			if(isHost){
				//Removing permissions and permission references for all children structures
				dc.setSQL(DELETE_CONTENTTYPE_REFERENCES_BY_PATH_SQL);
				dc.addParam(isHost?"%":folderPath+"%");
				dc.addParam(host.getPermissionId());
				dc.addParam(host.getPermissionId());
				dc.addParam(host.getPermissionId());
				dc.addParam(isHost?"%":folderPath+"%");
				dc.loadResult();
				dc.setSQL(DELETE_CONTENTTYPE_PERMISSIONS_BY_PATH_SQL);
				dc.addParam(isHost?"%":folderPath+"%");
				dc.addParam(host.getPermissionId());
				dc.addParam(host.getPermissionId());
				dc.loadResult();

				// Retrieving the list of structures changed to clear their caches

				dc.setSQL(SELECT_CHILD_CONTENTTYPE_BY_PATH_SQL);
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

				// Retrieving the list of structures changed to clear their caches
				dc.setSQL(SELECT_CHILD_CONTENTTYPE_BY_PATH_SQL_FOLDER);
				dc.addParam(isHost?"%":folderPath+"%");
				dc.addParam(host.getPermissionId());
				idsToClear.addAll(dc.loadResults());

			}


		} else if(isContentType) {

			//Removing permissions and permission references for all children containers
			dc.setSQL(DELETE_CONTENT_REFERENCES_BY_CONTENTTYPE_SQL);
			dc.addParam(permissionable.getPermissionId());
			dc.loadResult();
			dc.setSQL(deleteContentPermissionsByStructureSQL);
			dc.addParam(permissionable.getPermissionId());
			dc.loadResult();

			//Retrieving the list of content changed to clear their caches
			dc.setSQL(SELECT_CHILD_CONTENT_BY_CONTENTTYPE_SQL);
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
			removePermissions(new ArrayList<>(children));
		}

		if(isFolder || isHost || isContentType) {
			//Ensure every reference that was moved to point to this permissionable has its permissions fulfilled if not
			//look up in the hierarchy
		  resetPermissionReferences(permissionable);

			//Clearing the caches
			for(Map<String, String> idToClear: idsToClear) {
			    String ii=idToClear.get("inode");
			    if(ii==null) ii=idToClear.get("id");
			    permissionCache.remove(ii);
			}
 

			if(isHost) {
				ContentletAPI contentAPI = APILocator.getContentletAPI();
				contentAPI.refreshContentUnderHost((Host)permissionable);
			}

			if(isContentType) {
				ContentletAPI contentAPI = APILocator.getContentletAPI();
				Structure st = CacheLocator.getContentTypeCache().getStructureByInode(permissionable.getPermissionId());
				if(st != null)
					contentAPI.refresh(st);
			}
			if(isFolder) {
				ContentletAPI contAPI = APILocator.getContentletAPI();
				contAPI.refreshContentUnderFolder((Folder)permissionable);
			}
		}

	}

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

		 if(isHost) {
			 ContentletAPI contentAPI = APILocator.getContentletAPI();
			 contentAPI.refreshContentUnderHost((Host)permissionable);
		 }
		 Logger.info(this, "Ending cascade role permissions for permissionable " + permissionable.getPermissionId() + " for role " + role.getId());

	}

	/**
	 * Assign to  a Permissionable the same permission that it's parent
	 *
	 * @param permissionable permissionable link with the permission to update or save
	 * @param role role link with the permission to update or save
	 * @param permissionsPermissionable permissionable's parent
	 * @param allPermissions parent permission
	 * @throws DotDataException
     */
	private void cascadePermissionUnder(Permissionable permissionable, Role role, Permissionable permissionsPermissionable, List<Permission> allPermissions) throws DotDataException {
		boolean isFolder = isFolder(permissionable);

		User systemUser = APILocator.getUserAPI().getSystemUser();

		List<Permission> permissionablePermissions = loadPermissions(permissionable);

		PermissionType[] values = PermissionType.values();

		for (PermissionType permissionType : values) {

			if(isFolder && permissionType.getApplyTo() == PermissionType.ApplyTo.ONLY_HOST){
				continue;
			}

			Permission inheritablePermission = filterInheritablePermission(allPermissions, permissionsPermissionable
					.getPermissionId(), permissionType.getKey(), role.getId());

			Permission permissionToUpdate = filterInheritablePermission(permissionablePermissions, permissionsPermissionable.getPermissionId(),
					Template.class.getCanonicalName(), role.getId());
			if(permissionToUpdate == null) {
				permissionToUpdate = new Permission(permissionType.getKey(), permissionable.getPermissionId(), role.getId(), 0, true);
			}
			if(inheritablePermission != null)
				permissionToUpdate.setPermission(inheritablePermission.getPermission());
			savePermission(permissionToUpdate, permissionable);

			//Looking for children  overriding inheritance to also apply the cascade changes
			List<String> idsToUpdate = getChildrenOverridingInheritancePermission(permissionable, permissionType);

			int permission = 0;
			if (inheritablePermission != null) {
				permission = inheritablePermission.getPermission();
			}
			for (String id : idsToUpdate) {
				List<Permissionable> childPermissionables;
				try {
					childPermissionables = getPermissionable(id, systemUser, permissionType);
				} catch (DotSecurityException e) {
					Logger.error(PermissionBitFactoryImpl.class, e.getMessage(), e);
					throw new DotRuntimeException(e.getMessage(), e);
				}

				for (Permissionable childPermissionable : childPermissionables) {
					savePermission(new Permission(id, role.getId(), permission, true), childPermissionable);
				}

			}
		}

	}

	/**
	 * Return {@link Permissionable}
	 *
	 * @param id {@link Permissionable}'s id
	 * @param user
	 * @param permissionType
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
     */
	private List<Permissionable> getPermissionable(String id, User user, PermissionType permissionType) throws DotSecurityException, DotDataException {

		List<Permissionable> result = new ArrayList<>();

		switch (permissionType){
			case TEMPLATE:
				TemplateAPI templateAPI = APILocator.getTemplateAPI();
				result.add(templateAPI.findWorkingTemplate(id, user, false));
				break;
			case CONTAINER:
				ContainerAPI containerAPI = APILocator.getContainerAPI();
				result.add(containerAPI.getWorkingContainerById(id, user, false));
				break;
			case FOLDER:
				FolderAPI folderAPI = APILocator.getFolderAPI();
				result.add(folderAPI.find(id, user, false));
				break;
			case IHTMLPAGE:
				Identifier identifier = APILocator.getIdentifierAPI().find( id );
				if ( identifier != null ) {
					if ( identifier.getAssetType().equals( Identifier.ASSET_TYPE_CONTENTLET ) ) {
						HTMLPageAssetAPI htmlPageAssetAPI = APILocator.getHTMLPageAssetAPI();
						//Get the contentlet and the HTMLPage asset object related to the given permissionable id
						Contentlet pageWorkingVersion = APILocator.getContentletAPI().findContentletByIdentifier( id, false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, false );
						result.add(htmlPageAssetAPI.fromContentlet( pageWorkingVersion ));
					}
				}
				break;
			case LINK:
				MenuLinkAPI linkAPI = APILocator.getMenuLinkAPI();
				result.add(linkAPI.findWorkingLinkById(id, user, false));
				break;
			case CONTENTLET:
				ContentletAPI contentAPI = APILocator.getContentletAPI();
				String luceneQuery = "+identifier:"+id+" +working:true";
				result.addAll(contentAPI.search(luceneQuery, 1, 0, null, user, false));
				break;
			case STRUCTURE:
				result.add(CacheLocator.getContentTypeCache().getStructureByInode(id));
				break;
		}


		return result;
	}


	/**
	 * Returns the permissionable's children that have individual permission.
	 * @return a list of {@link Permissionable} 's id
	 */
	public List<String> getChildrenOverridingInheritancePermission(Permissionable permissionable, PermissionType permissionType) throws DotDataException {
		HostAPI hostAPI = APILocator.getHostAPI();
		User systemUser = APILocator.getUserAPI().getSystemUser();

		String fieldNameFromQueryToreturn = "id";
		DotConnect dc = new DotConnect();

		boolean isHost = isHost(permissionable);
		boolean isFolder = isFolder(permissionable);
		Permissionable host;
		try {
			host = isHost ? permissionable : hostAPI.findParentHost((Folder) permissionable, systemUser, false);
		} catch (DotSecurityException e) {
			Logger.error(PermissionBitFactoryImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		Folder folder = isFolder ? (Folder) permissionable : null;
		String folderPath = folder != null ? APILocator.getIdentifierAPI().find(folder.getIdentifier()).getPath() : "";
		String query = SELECT_CHILDREN_WITH_INDIVIDUAL_PERMISSIONS_SQLS.get(permissionType);

		List<String> result = new ArrayList<>();

		if (query != null) {
			dc.setSQL(query);

			switch (permissionType) {
				case TEMPLATE:
					dc.addParam(host.getPermissionId());
					break;
				case CONTAINER:
					dc.addParam(host.getPermissionId());
					break;
				case FOLDER:
					dc.addParam(host.getPermissionId());
					dc.addParam(isHost ? "%" : folderPath + "%");
					dc.addParam(isHost ? " " : folderPath + "");
					fieldNameFromQueryToreturn = "inode";
					break;
				case IHTMLPAGE:
					dc.addParam(host.getPermissionId());
					dc.addParam(isHost ? "%" : folderPath + "%");
					dc.addParam(host.getPermissionId());
					dc.addParam(isHost ? "%" : folderPath + "%");
					break;
				case LINK:
					dc.addParam(host.getPermissionId());
					dc.addParam(isHost ? "%" : folderPath + "%");
					break;
				case CONTENTLET:
					dc.addParam(host.getPermissionId());
					dc.addParam(isHost ? "%" : folderPath + "%");
					break;
				case STRUCTURE:
					dc.addParam(isHost ? "%" : folderPath + "%");
					dc.addParam(host.getPermissionId());
					dc.addParam(host.getPermissionId());
					fieldNameFromQueryToreturn = "inode";
					break;
				default:
					//rules and template layput dont have individual permission
			}


			List<Map<String, String>> idsToUpdate = dc.loadResults();
			for (Map<String, String> permissionableInfo : idsToUpdate) {
				result.add( permissionableInfo.get(fieldNameFromQueryToreturn) );
			}
		}

		return result;
	}

	private boolean isFolder(Permissionable permissionable) {
		return permissionable instanceof Folder;
	}

	private boolean isHost(Permissionable permissionable) {
		return permissionable instanceof Host ||
			(permissionable instanceof Contentlet && ((Contentlet)permissionable).getStructure().getVelocityVarName().equals("Host"));
	}

	@Override
	void resetChildrenPermissionReferences(Structure structure) throws DotDataException {
	    ContentletAPI contAPI = APILocator.getContentletAPI();
	    ContentletIndexAPI indexAPI=new ContentletIndexAPIImpl();

	    DotConnect dc = new DotConnect();
		dc.setSQL(DELETE_CONTENT_REFERENCES_BY_CONTENTTYPE_SQL);
		dc.addParam(structure.getPermissionId());
		dc.loadResult();

		final int limit=500;
		int offset=0;
		List<Contentlet> contentlets;
		do {
			String query="structurename:"+structure.getVelocityVarName();
			try {
			    contentlets=contAPI.search(query, limit, offset, "identifier", APILocator.getUserAPI().getSystemUser(), false);
            } catch (DotSecurityException e) {
                throw new RuntimeException(e);
            }

			BulkRequest bulkRequest=indexAPI.createBulkRequest();
			bulkRequest.timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));

			for(Contentlet cont : contentlets) {
			    permissionCache.remove(cont.getPermissionId());
			    cont.setIndexPolicy(IndexPolicy.DEFER);
			    indexAPI.addContentToIndex(cont, false);
			}
			if(bulkRequest.numberOfActions()>0) {
				Sneaky.sneak(()-> RestHighLevelClientProvider.getInstance().getClient()
						.bulk(bulkRequest, RequestOptions.DEFAULT));
			}

			offset=offset+limit;
		} while(contentlets.size()>0);
	}

	@Override
	void resetPermissionReferences(final Permissionable permissionable) throws DotDataException {
		permissionCache.remove(permissionable.getPermissionId());
		final List<PermissionReference> references = loadAllPermissionReferencesTo(permissionable);
		final List<Permissionable> proxies = references
				.stream().map(reference -> {
					PermissionableProxy proxy = new PermissionableProxy();
					proxy.setIdentifier(reference.getAssetId());
					return proxy;
				}).collect(Collectors.toList());

		dbDeletePermissionReferences(proxies);

		for (final PermissionReference reference : references) {
			APILocator.getReindexQueueAPI().addIdentifierReindex(reference.getAssetId());
		}

		dbDeletePermissionReferences(Collections.singletonList(permissionable));

		if (permissionable instanceof ContentType) {
			APILocator.getReindexQueueAPI()
					.addStructureReindexEntries((ContentType) permissionable);
		} else if (permissionable instanceof Contentlet) {
			APILocator.getReindexQueueAPI().addIdentifierReindex(permissionable.getPermissionId());
		}
	}

	@Override
	void resetAllPermissionReferences() throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(DELETE_ALL_PERMISSION_REFERENCES_SQL);
		dc.loadResult();
		permissionCache.clearCache();

		// at least we need to regenerate for template and structure
		HibernateUtil hu=new HibernateUtil(Template.class);
		int offset=0;
		int max=100;
		List<Template> list=null;
		do {
    		hu.setQuery("from "+Template.class.getCanonicalName());
    		hu.setFirstResult(offset);
    		hu.setMaxResults(max);
    		list = hu.list();
    		for(Template t : list) {
    		    getPermissions(t);
    		}
    		offset=offset+max;
		} while(list.size()>0);

		hu=new HibernateUtil(Structure.class);
        offset=0;

        List<Structure> listSt=null;
        do {
            hu.setQuery("from "+Structure.class.getCanonicalName());
            hu.setFirstResult(offset);
            hu.setMaxResults(max);
            listSt = hu.list();
            for(Structure st : listSt) {
                getPermissions(st);
            }
            offset=offset+max;
        } while(listSt.size()>0);
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

		List<String> roleIds = new ArrayList<>();
		if(respectFrontendRoles){
		// add anonRole and frontEndUser roles
			roleIds.add(anonRole.getId());
			if(user != null ){
			 roleIds.add("'"+frontEndUserRole.getId()+"'");
			}
		}

		//If user is null and roleIds are empty return empty list
		if(roleIds.isEmpty() && user==null){
			return new ArrayList<>();
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

		Map<String, P> permissionableMap = new HashMap<>();
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
		List<P> permsToReturn = new ArrayList<>();
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
	    		permissionableMap = new HashMap<>();
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
		// if it haven't a parent to inherit then don't bother looking for individual permissions
		if(permissionable.getParentPermissionable()==null) return false;

		DotConnect dc = new DotConnect();
		dc.setSQL("SELECT COUNT(*) AS cc FROM permission where inode_id=?");
		dc.addParam(permissionable.getPermissionId());
		dc.loadResult();
		return dc.getInt("cc")==0;
	}


	private interface AssetPermissionReferencesSQLProvider {
		String getInsertContainerReferencesToAHostSQL();
		String getInsertContentReferencesByPathSQL();
		String getInsertHTMLPageReferencesSQL();
		String getInsertLinkReferencesSQL();
		String getInsertTemplateReferencesToAHostSQL();
	}

	private class MsSqlAssetPermissionReferencesSQLProvider implements AssetPermissionReferencesSQLProvider {

		@Override
		public String getInsertContainerReferencesToAHostSQL() {
			return
		            "insert into permission_reference (asset_id, reference_id, permission_type) " +
		            "select ident.id, ?, '" + Container.class.getCanonicalName() + "'" +
		            "	from identifier ident, " +
		            "		(" + SELECT_CHILD_CONTAINER_SQL +
		            "			and identifier.id not in (" + 
		            "				select inode_id from permission " +
		            "					where permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "') " + 
		            "					and identifier.id not in (" +
		            "						select asset_id " + 
		            "							from permission_reference " + 
		            "							where permission_type = '" + Container.class.getCanonicalName() + "')) ids " + 
		            "	where ident.id = ids.id"
			;
		}

		@Override
		public String getInsertContentReferencesByPathSQL() {
			return
		            "insert into permission_reference (asset_id, reference_id, permission_type) " +
		            "select identifier.id, ?, '" + Contentlet.class.getCanonicalName() + "' " +
		            "	from identifier, (" +
		            "		" + SELECT_CHILD_CONTENT_BY_PATH_SQL + " and" +
		            "		identifier.id not in (" +
		            "			select asset_id from permission_reference join folder ref_folder on (reference_id = ref_folder.inode)" +
		            "                                join identifier on (identifier.id=ref_folder.identifier) " +
		            "			where "+DOT_FOLDER_PATH+"(parent_path,asset_name) like ? and permission_type = '" + Contentlet.class.getCanonicalName() + "'" +
		            "		) and " +
		            "		identifier.id not in (" +
		            "			select inode_id from permission where " +
		            "			permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		            "		) " +
		            "	) ids " +
		            "where identifier.id = ids.id " +
		            "and not exists (SELECT asset_id from permission_reference where asset_id = identifier.id)"
			;
		}

		@Override
		public String getInsertHTMLPageReferencesSQL() {
			return
		            "insert into permission_reference (asset_id, reference_id, permission_type) " +
		            "select identifier.id, ?, '" + IHTMLPage.class.getCanonicalName() + "' " +
		            "	from identifier, " +
		            "		(" + SELECT_CHILD_HTMLPAGE_SQL + " and" +
		            "		not exists (" +
		            "			select asset_id from " + 
		            "				permission_reference " +
		            "				join folder ref_folder on (reference_id = ref_folder.inode)" +
		            "               join identifier on (ref_folder.identifier=identifier.id) " +
		            "				where asset_id = li.id and "+DOT_FOLDER_PATH+"(parent_path,asset_name) like ? " + 
		            "				and permission_type = '" + IHTMLPage.class.getCanonicalName() + "'" +
		            "		) and " +
		            "		not exists (" +
		            "			select inode_id " + 
		            "				from permission " + 
		            "				where inode_id = li.id and permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		            "		) " +
		            "	) ids " +
		            "	where identifier.id = ids.id " +
		            "	and not exists (SELECT asset_id " + 
		            "		from permission_reference " + 
		            "		where asset_id = identifier.id)"
			;
		}

		@Override
		public String getInsertLinkReferencesSQL() {
			return
		            "insert into permission_reference (asset_id, reference_id, permission_type) " +
		            "select identifier.id, ?, '" + Link.class.getCanonicalName() + "' " +
		            "	from identifier, (" +
		            "		" + SELECT_CHILD_LINK_SQL + " and" +
		            "		not exists (" +
		            "			select asset_id from permission_reference join folder ref_folder on (reference_id = ref_folder.inode)" +
		            "            join identifier ii on (ii.id=ref_folder.identifier) where asset_id = identifier.id and " +
		            "			"+DOT_FOLDER_PATH+"(ii.parent_path,ii.asset_name) like ? and permission_type = '" + Link.class.getCanonicalName() + "'" +
		            "		) and " +
		            "		not exists (" +
		            "			select inode_id from permission where inode_id = identifier.id and " +
		            "			permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		            "		) " +
		            "	) ids  " +
		            "where identifier.id = ids.id "+
		            "and not exists (SELECT asset_id from permission_reference where asset_id = identifier.id)"
			;
		}

		@Override
		public String getInsertTemplateReferencesToAHostSQL() {
			return
				    "insert into permission_reference (asset_id, reference_id, permission_type) " +
				    "select ident.id, ?, '" + Template.class.getCanonicalName() + "'" +
				    "	from identifier ident, " + 
				    "		(" + SELECT_CHILD_TEMPLATE_SQL + 
				    "		and identifier.id not in (" + 
				    "			select inode_id " + 
				    "				from permission " +
				    "				where permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "') " + 
				    "				and identifier.id not in (" + 
				    "					select asset_id " + 
				    "						from permission_reference " + 
				    "						where permission_type = '" + Template.class.getCanonicalName() + "')) ids" +
				    "	where ident.id = ids.id"
			;
		}
	}

	private class MySqlAssetPermissionReferencesSQLProvider implements AssetPermissionReferencesSQLProvider {

		@Override
		public String getInsertContainerReferencesToAHostSQL() {
			return
		            "insert into permission_reference (asset_id, reference_id, permission_type) " +
		            "select ident.id, ?, '" + Container.class.getCanonicalName() + "'" +
		            "	from identifier ident, " +
		            "		(" + SELECT_CHILD_CONTAINER_SQL + " and " +
		            "		 identifier.id not in (select inode_id from permission " +
		            "			where permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "') and " +
		            "		 identifier.id not in (select asset_id from permission_reference where " +
		            "			permission_type = '" + Container.class.getCanonicalName() + "')) x where ident.id = x.id"
			;
		}

		@Override
		public String getInsertContentReferencesByPathSQL() {
			return
		            "insert into permission_reference (asset_id, reference_id, permission_type) " +
		            "select identifier.id, ?, '" + Contentlet.class.getCanonicalName() + "' " +
		            "	from identifier, (" +
		            "		" + SELECT_CHILD_CONTENT_BY_PATH_SQL + " and" +
		            "		identifier.id not in (" +
		            "			select asset_id from permission_reference join folder ref_folder on (reference_id = ref_folder.inode)" +
		            "                                join identifier on (identifier.id=ref_folder.identifier) " +
		            "			where "+DOT_FOLDER_PATH+"(parent_path,asset_name) like ? and permission_type = '" + Contentlet.class.getCanonicalName() + "'" +
		            "		) and " +
		            "		identifier.id not in (" +
		            "			select inode_id from permission where " +
		            "			permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		            "		) " +
		            "	) x " +
		            "WHERE identifier.id = x.id " +
		            "and not exists (SELECT asset_id from permission_reference where asset_id = identifier.id)"
			;
		}

		@Override
		public String getInsertHTMLPageReferencesSQL() {
			return
		            "insert into permission_reference (asset_id, reference_id, permission_type) " +
		            "select identifier.id, ?, '" + IHTMLPage.class.getCanonicalName() + "' " +
		            "	from identifier, (" +
		            "	select distinct li.id as li_id from identifier li where" +
		            " 	li.asset_type='htmlpage' and li.host_inode = ? and li.parent_path like ?" +
		            " UNION ALL" +
		            " SELECT distinct li.id as li_id FROM identifier li" +
		                " INNER JOIN contentlet lc ON (lc.identifier = li.id and li.asset_type = 'contentlet')" +
		                " INNER JOIN structure ls ON (lc.structure_inode = ls.inode and ls.structuretype = " + BaseContentType.HTMLPAGE.getType() + ")" +
		                " AND li.host_inode = ? and li.parent_path like ?" + 
		                " and" +
		            "		li.id not in (" +
		            "			select asset_id from permission_reference join folder ref_folder on (reference_id = ref_folder.inode)" +
		            "                                join identifier on (ref_folder.identifier=identifier.id) " +
		            "			where "+DOT_FOLDER_PATH+"(parent_path,asset_name) like ? and permission_type = '" + IHTMLPage.class.getCanonicalName() + "'" +
		            "		) and " +
		            "		li.id not in (" +
		            "			select inode_id from permission where permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		            "		) " +
		            "	) all_ids where identifier.id = all_ids.li_id " +
		            "and not exists (SELECT asset_id from permission_reference where asset_id = identifier.id)"
			;
		}

		@Override
		public String getInsertLinkReferencesSQL() {
			return
		            "insert into permission_reference (asset_id, reference_id, permission_type) " +
		            "select identifier.id, ?, '" + Link.class.getCanonicalName() + "' " +
		            "	from identifier, (" +
		            "		" + SELECT_CHILD_LINK_SQL + " and" +
		            "		identifier.id not in (" +
		            "			select asset_id from permission_reference join folder ref_folder on (reference_id = ref_folder.inode)" +
		            "            join identifier ii on (ii.id=ref_folder.identifier) where " +
		            "			"+DOT_FOLDER_PATH+"(ii.parent_path,ii.asset_name) like ? and permission_type = '" + Link.class.getCanonicalName() + "'" +
		            "		) and " +
		            "		identifier.id not in (" +
		            "			select inode_id from permission where " +
		            "			permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		            "		) " +
		            "	) x where identifier.id = x.id " +
		            "and not exists (SELECT asset_id from permission_reference where asset_id = identifier.id)"
			;
		}

		@Override
		public String getInsertTemplateReferencesToAHostSQL() {
			return
				    "insert into permission_reference (asset_id, reference_id, permission_type) " +
				    "select ident.id, ?, '" + Template.class.getCanonicalName() + "'" +
				    "	from identifier ident, " +
				    "		(" + SELECT_CHILD_TEMPLATE_SQL + " and " +
				    "		 identifier.id not in (select inode_id from permission " +
				    "			where permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "') and " +
				    "		 identifier.id not in (select asset_id from permission_reference where " +
				    "			permission_type = '" + Template.class.getCanonicalName() + "')) x where ident.id = x.id"
			;
		}
	}

	private class OracleAssetPermissionReferencesSQLProvider implements AssetPermissionReferencesSQLProvider {

		@Override
		public String getInsertContainerReferencesToAHostSQL() {
			return
		            "insert into permission_reference (id, asset_id, reference_id, permission_type) " +
		            "select permission_reference_seq.NEXTVAL, ident.id, ?, '" + Container.class.getCanonicalName() + "'" +
		            "	from identifier ident, " + 
		            "		(" + SELECT_CHILD_CONTAINER_SQL +
		            " 			and not exists (" +
		            "				select inode_id " + 
		            "					from permission " +
		            "					where inode_id = identifier.id " +
		            "					permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "') " +
		            "			and not exists (" + 
		            "				select asset_id " + 
		            "					from permission_reference " + 
		            "					where asset_id = identifier.id " + 
		            "					and permission_type = '" + Container.class.getCanonicalName() + "')) ids " +
		            "	where ident.id = ids.id"
			;
		}

		@Override
		public String getInsertContentReferencesByPathSQL() {
			return
		            "insert into permission_reference (id, asset_id, reference_id, permission_type) " +
		            "select permission_reference_seq.NEXTVAL, identifier.id, ?, '" + Contentlet.class.getCanonicalName() + "' " +
		            "		from identifier " +
		            "		where asset_type='contentlet' " +
		            "		and identifier.id <> identifier.host_inode " +
		            "		and identifier.host_inode = ? " + 
		            "		and identifier.parent_path like ? and" +
		            "		identifier.id not in (" +
		            "			select asset_id " + 
		            "				from permission_reference " + 
		            "				join folder ref_folder on (reference_id = ref_folder.inode)" +
		            "           	join identifier on (identifier.id=ref_folder.identifier) " +
		            "				where "+DOT_FOLDER_PATH+"(parent_path,asset_name) like ? " +
		            "				and permission_type = '" + Contentlet.class.getCanonicalName() + "'" +
		            "		) and " +
		            "		identifier.id not in (" +
		            "			select inode_id " + 
		            "			from permission where " +
		            "			permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		            "		) " +
		            "and not exists (SELECT asset_id from permission_reference where asset_id = identifier.id)"
			;
		}

		@Override
		public String getInsertHTMLPageReferencesSQL() {
			return
		            "insert into permission_reference (id, asset_id, reference_id, permission_type) " +
		            "select permission_reference_seq.NEXTVAL, identifier.id, ?, '" + IHTMLPage.class.getCanonicalName() + "' " +
		            "	from identifier, " +
		            "	(" + SELECT_CHILD_HTMLPAGE_SQL + " and " +
		            "		not exists (" +
		            "			select asset_id " +
		            "				from permission_reference " +
		            "				join folder ref_folder on (reference_id = ref_folder.inode) " +
		            "           	join identifier on (ref_folder.identifier=identifier.id) " +
		            "				where asset_id = li.id " +
		            "				and " + DOT_FOLDER_PATH + "(parent_path,asset_name) like ? " + 
		            "				and permission_type = '" + IHTMLPage.class.getCanonicalName() + "'" +
		            "		) and " +
		            "		not exists (" +
		            "			select inode_id " + 
		            "				from permission " +
		            "				where inode_id = li.id and permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		            "		) " +
		            "	) ids" +
		            "	where identifier.id = ids.id " +
		            "	and not exists (" +
		            "		SELECT asset_id " +
		            "			from permission_reference " +
		            "			where asset_id = identifier.id)"
			;
		}

		@Override
		public String getInsertLinkReferencesSQL() {
			return
		            "insert into permission_reference (id, asset_id, reference_id, permission_type) " +
		            "select permission_reference_seq.NEXTVAL, identifier.id, ?, '" + Link.class.getCanonicalName() + "' " +
		            "	from identifier, " + 
		            "		(" + SELECT_CHILD_LINK_SQL + " and" +
		            "		not exists (" +
		            "			select asset_id " + 
		            "				from permission_reference " + 
		            "				join folder ref_folder on (reference_id = ref_folder.inode)" +
		            "            	join identifier ii on (ii.id=ref_folder.identifier) " + 
		            "				where asset_id = identifier.id " +
		            "				and "+DOT_FOLDER_PATH+"(ii.parent_path,ii.asset_name) like ? " + 
		            "				and permission_type = '" + Link.class.getCanonicalName() + "'" +
		            "		) and " +
		            "		not exists (" +
		            "			select inode_id " + 
		            "			from permission " + 
		            "			where inode_id = identifier.id " +
		            "			and permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		            "		) " +
		            "	) ids " +
		            "	where identifier.id = ids.id " +
		            "	and not exists (" + 
		            "		SELECT asset_id " + 
		            "			from permission_reference " + 
		            "			where asset_id = identifier.id)"	
			;
		}

		@Override
		public String getInsertTemplateReferencesToAHostSQL() {
			return
				    "insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				    "select permission_reference_seq.NEXTVAL, ident.id, ?, '" + Template.class.getCanonicalName() + "'" +
				    "	from identifier ident, " + 
				    "		(" + SELECT_CHILD_TEMPLATE_SQL + " and " +
				    "		 	not exists (" + 
				    "				select inode_id " +
				    "				from permission " +
				    "				where inode_id = identidier.id " + 
				    "				and permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "') " + 
				    "			and not exists (" +
				    "				select asset_id " + 
				    "				from permission_reference "  + 
				    "				where asset_id = identifier.id " +
				    "				and permission_type = '" + Template.class.getCanonicalName() + "')" + 
				    "	) ids " +
				    " where ident.id = ids.id"
			;
		}
	}

	private class PostgresAssetPermissionReferencesSQLProvider implements AssetPermissionReferencesSQLProvider {

		@Override
		public String getInsertContainerReferencesToAHostSQL() {
			return
		            "insert into permission_reference (id, asset_id, reference_id, permission_type) " +
		            "select nextval('permission_reference_seq'), ident.id, ?, '" + Container.class.getCanonicalName() + "'" +
		            "	from identifier ident, " +
		            "		(" + SELECT_CHILD_CONTAINER_SQL + " and " +
		            "			identifier.id not in (" + 
		            "				select inode_id from permission " +
		            "				where permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "') " +
		            "			and identifier.id not in (" +
		            "				select asset_id from permission_reference where " +
		            "				permission_type = '" + Container.class.getCanonicalName() + "')" +
		            ") ids " + 
		            "where ident.id = ids.id"
			;
		}

		@Override
		public String getInsertContentReferencesByPathSQL() {
			return
		            "insert into permission_reference (id, asset_id, reference_id, permission_type) " +
		            "select nextval('permission_reference_seq'), identifier.id, ?, '" + Contentlet.class.getCanonicalName() + "' " +
		            "	from identifier, " +
		            "		(" + SELECT_CHILD_CONTENT_BY_PATH_SQL + " and" +
		            "			identifier.id not in (" +
		            "				select asset_id " + 
		            "					from permission_reference " + 
		            "					join folder ref_folder on (reference_id = ref_folder.inode)" +
		            "               	join identifier on (identifier.id=ref_folder.identifier) " +
		            "					where "+DOT_FOLDER_PATH+"(parent_path,asset_name) like ? " +
		            "					and permission_type = '" + Contentlet.class.getCanonicalName() + "'" +
		            "		) and " +
		            "		identifier.id not in (" +
		            "			select inode_id from permission where " +
		            "			permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		            "		) " +
		            "	) ids " +
		            "where identifier.id = ids.id and not exists (SELECT asset_id from permission_reference where asset_id = identifier.id)"
			;
		}

		@Override
		public String getInsertHTMLPageReferencesSQL() {
			return
		            "insert into permission_reference (id, asset_id, reference_id, permission_type) " +
		            "select nextval('permission_reference_seq'), identifier.id, ?, '" + IHTMLPage.class.getCanonicalName() + "' " +
		            "	from identifier, " +
		            "   (" + SELECT_CHILD_HTMLPAGE_SQL + " and" +
		            "		li.id not in (" +
		            "			select asset_id " +
		            "				from permission_reference "+
		            "				join folder ref_folder on (reference_id = ref_folder.inode)" +
		            "           	join identifier on (ref_folder.identifier=identifier.id) " +
		            "				where "+DOT_FOLDER_PATH+"(parent_path,asset_name) like ? " +
		            "				and permission_type = '" + IHTMLPage.class.getCanonicalName() + "'" +
		            "		) and " +
		            "		li.id not in (" +
		            "			select inode_id " +
		            "				from permission " + 
		            "				where permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		            "		) " +
		            "	) ids " +
		            "	where identifier.id = ids.id " +
		            "	and not exists (" +
		            "		SELECT asset_id " + 
		            "			from permission_reference " + 
		            "			where asset_id = identifier.id)"
			;
		}

		@Override
		public String getInsertLinkReferencesSQL() {
			return
		            "insert into permission_reference (id, asset_id, reference_id, permission_type) " +
		            "select nextval('permission_reference_seq'), identifier.id, ?, '" + Link.class.getCanonicalName() + "' " +
		            "	from identifier, " +
		            "		(" + SELECT_CHILD_LINK_SQL + " and" +
		            "		identifier.id not in (" +
		            "			select asset_id " +
		            "				from permission_reference " +
		            "				join folder ref_folder on (reference_id = ref_folder.inode)" +
		            "            	join identifier ii on (ii.id=ref_folder.identifier) "  +
		            "				where " + DOT_FOLDER_PATH+"(ii.parent_path,ii.asset_name) like ? " + 
		            "				and permission_type = '" + Link.class.getCanonicalName() + "'" +
		            "		) and " +
		            "		identifier.id not in (" +
		            "			select inode_id " + 
		            "				from permission where " +
		            "				permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "'" +
		            "		) " +
		            "	) ids " +
		            "	where identifier.id = ids.id " + 
		            "	and not exists (" + 
		            "		SELECT asset_id " + 
		            "			from permission_reference " +
		            "			where asset_id = identifier.id)"
			;
		}

		@Override
		public String getInsertTemplateReferencesToAHostSQL() {
			return
				    "insert into permission_reference (id, asset_id, reference_id, permission_type) " +
				    "select nextval('permission_reference_seq'), ident.id, ?, '" + Template.class.getCanonicalName() + "'" +
				    "	from identifier ident, " +
				    "		(" + SELECT_CHILD_TEMPLATE_SQL + " and " +
				    "		identifier.id not in (" +
				    "			select inode_id from permission " +
				    "			where permission_type = '" + PermissionAPI.INDIVIDUAL_PERMISSION_TYPE + "') " + 
				    "		and " +
				    "		identifier.id not in (" +
				    "			select asset_id from permission_reference where " +
				    "			permission_type = '" + Template.class.getCanonicalName() + "')" +
				    "	) ids " + 
				    "	where ident.id = ids.id"

			;
		}
	}

	private static class PermissionSaveResult {
		Permission permission;
		PersistResult result;

		public PermissionSaveResult(Permission permission,
				PersistResult result) {
			this.permission = permission;
			this.result = result;
		}
	}

}
