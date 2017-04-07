package com.dotcms.business;

import com.dotmarketing.beans.Inode;
import com.liferay.portal.model.User;

import java.io.Serializable;

/**
 * This component basically encapsulates operation to retrieve type and Inode from the datastorage.
 * @author
 */
public interface TypeDAO extends Serializable {

    /**
     * Based on the inodeId returns the associated Inode type, null if it does not exists.
     * @param inodeId {@link String}
     * @return Class
     */
    Class getInodeType (String inodeId);

    /**
     * Based on the identifier returns the associated to the Identifier type, null if it does not exists.
     * @param identifier {@link String}
     * @return Class
     */
    Class getIdentifierType (String identifier);


    /**
     * Based on the identifier returns the associated asset type, null if it does not exists.
     * @param identifier {@link String}
     * @return  String
     */
    String getIdentifierAssetType (String identifier);

    /**
     * Find the first Inode by identifier returning the right subclass of {@link Inode} based on the inode type
     * This method returns whatever first inode is in the list, regardless version etc. Usually useful just from the permission perspective since all version share the same set of permissions for instance.
     * @param identifier {@link String}
     * @return Inode
     */
    Inode findFirstInodeByIdentifier (String identifier);


    /**
     * Find by INode and returns a subclass depending on the clazz.
     * If clazz parameters is an {@link Inode} the method will try to figure the type in order to get the right subclas but keep in mind it is more expensive.
     * Pre: {@link com.dotmarketing.beans.Identifier} is not supported
     * @see #findFirstInodeByIdentifier(String)
     * Pre: {@link com.dotmarketing.portlets.folders.model.Folder} is not supported
     * @see com.dotmarketing.portlets.folders.business.FolderAPI#find(String, User, boolean)
     * Pre: {@link com.dotmarketing.portlets.structure.model.Structure} is not supported
     * @see com.dotcms.contenttype.business.ContentTypeAPI#find(String)
     * Pre: {@link com.dotcms.contenttype.model.type.ContentType} is not supported
     * @see com.dotcms.contenttype.business.ContentTypeAPI#find(String)
     *
     * @param inodeId {@link String} inode id
     * @param clazz {@link Class}  clazz you want to get, if it is an INode will try to figure out the type, but it will be more expensive.
     * @return Inode
     */
    public <T extends Inode> T findByInode(final String inodeId, final Class<T> clazz);

    /**
     * This method is pretty similar to {@link #findByInode(String, Class)} but it will tries to figure out the type,
     * if it couldn't determine the type, will return null.
     * @param inodeId {@link String}
     * @return Inode.
     */
    public Inode findByInode(final String inodeId);
} // E:O:F:TypeDAO.
