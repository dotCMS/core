package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;

public class HandlerUtil {

    protected enum HandlerType {
        CONTENTLET,
        CONTAINERS,
        TEMPLATE,
        LINKS
    }

    @WrapInTransaction
	protected static void setModUser(String inode, String modUserId, HandlerType type) {
		try {
			User userToUse = APILocator.getUserAPI().loadUserById(modUserId);

            String tableName = Inode.Type.valueOf(type.name()).getTableName();

			if(UtilMethods.isSet(userToUse)) {
				DotConnect dc = new DotConnect();
				dc.setSQL("update " +tableName+ " set mod_user = ? where inode = ?");
				dc.addParam(modUserId);
				dc.addParam(inode);
				dc.loadResult();
			}
		} catch (NoSuchUserException e) {
			Logger.info(HandlerUtil.class, "User with ID: " + modUserId + " does not exist. Leaving system user as mod user");
		} catch (DotDataException | DotSecurityException e) {
			Logger.error(HandlerUtil.class, "Error trying to get User with ID: " + modUserId + ". Leaving system user as mod user");
		}


	}

    /**
     * Save a given multi-tree list after removing the old multi-tree records associated to a given
     * html page identifier and cleaning the html page cache.
     *
     * @param pageIdentifier
     * @param pageInode
     * @param wrapperMultiTree
     * @param modUser
     * @throws DotDataException
     */
    @WrapInTransaction
    protected static void setMultiTree(String pageIdentifier, String pageInode, List<Map<String, Object>> wrapperMultiTree, String modUser)
            throws DotDataException, DotSecurityException {
        setMultiTree(pageIdentifier, pageInode, null, wrapperMultiTree, modUser);
    }

    /**
     * Save a given multi-tree list after removing the old multi-tree records associated to a given
     * html page identifier and cleaning the html page cache.
     *
     * @param pageIdentifier
     * @param pageInode
     * @param pageLanguage
     * @param wrapperMultiTree
     * @param modUser
     * @throws DotDataException
     */
    @WrapInTransaction
    protected static void setMultiTree(String pageIdentifier, String pageInode, Long pageLanguage, List<Map<String, Object>> wrapperMultiTree, String modUser)
            throws DotDataException, DotSecurityException {

        //Remove the current records
        DotConnect dc = new DotConnect();
        dc.setSQL( "delete from multi_tree where parent1=?" );
        dc.addParam( pageIdentifier );
        dc.loadResult();
        HibernateUtil.getSession().clear();

        //Adding the content to the html page saving the multi tree entries
        for ( Map<String, Object> multiTreeData : wrapperMultiTree ) {
            MultiTree multiTree = new MultiTree();
            multiTree.setChild( (String) multiTreeData.get( "child" ) );
            multiTree.setParent1( (String) multiTreeData.get( "parent1" ) );
            multiTree.setParent2( (String) multiTreeData.get( "parent2" ) );
            multiTree.setRelationType( (String) multiTreeData.get( "relation_type" ) );
            multiTree.setTreeOrder( Integer.parseInt( multiTreeData.get( "tree_order" ).toString() ) );

            APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        }

        //Cleaning up the cache
        CacheLocator.getHTMLPageCache().remove( pageInode );
    }

    /**
     * Transform a given contentlet of type HTMLPage into a HTMLPageAsset object
     *
     * @param con
     * @param includeFolder False to avoid the setting the folder to the HTMLPageAsset, that implies some validations and when calling
     *                      from some handlers the folder could be not been created yet throwing exceptions.
     * @return
     */
    protected static HTMLPageAsset fromContentlet ( Contentlet con, Boolean includeFolder ) {

        if ( con == null || con.getStructure().getStructureType() != Structure.STRUCTURE_TYPE_HTMLPAGE ) {
            throw new DotStateException( "Contentlet : " + con.getInode() + " is not a pageAsset" );
        }

        HTMLPageAsset pa = new HTMLPageAsset();
        pa.setStructureInode( con.getStructureInode() );
        try {
            APILocator.getContentletAPI().copyProperties( (Contentlet) pa, con.getMap() );
        } catch ( Exception e ) {
            throw new DotStateException( "Page Copy Failed", e );
        }
        pa.setHost( con.getHost() );
        if ( UtilMethods.isSet( con.getFolder() ) && includeFolder ) {
            try {
                Identifier ident = APILocator.getIdentifierAPI().find( con );
                User systemUser = APILocator.getUserAPI().getSystemUser();
                Host host = APILocator.getHostAPI().find( con.getHost(), systemUser, false );
                Folder folder = APILocator.getFolderAPI().findFolderByPath( ident.getParentPath(), host, systemUser, false );
                pa.setFolder( folder.getInode() );
            } catch ( Exception e ) {
                Logger.warn( HandlerUtil.class, "Unable to convert contentlet to page asset " + con, e );
            }
        }
        return pa;
    }

}
