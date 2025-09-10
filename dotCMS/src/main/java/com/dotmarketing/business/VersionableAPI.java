package com.dotmarketing.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.variant.model.Variant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

public interface VersionableAPI {

	/**
	 * Finds the working version based on any version of the object being passed in
	 * @param inode
	 * @param user
	 * @param respectAnonPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	Versionable findWorkingVersion(Versionable inode, User user, boolean respectAnonPermissions) throws DotDataException, DotStateException,DotSecurityException;
	/**
	 * Finds the working version based on the identifier
	 * @param id
	 * @param user
	 * @param respectAnonPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	Versionable findWorkingVersion(Identifier id, User user, boolean respectAnonPermissions)  throws DotDataException, DotStateException,DotSecurityException;
	/**
	 * Finds the working version based on the identifier's id
	 * @param id
	 * @param user
	 * @param respectAnonPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	Versionable findWorkingVersion(String id, User user, boolean respectAnonPermissions) throws DotDataException, DotStateException,DotSecurityException;
	/**
	 * Finds the live version based on any version of the object being passed in
	 * @param inode
	 * @param user
	 * @param respectAnonPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	Versionable findLiveVersion(Versionable inode, User user, boolean respectAnonPermissions)  throws DotDataException, DotStateException,DotSecurityException;
	
	/**
	 * Finds the live version based on the identifier
	 * @param id
	 * @param user
	 * @param respectAnonPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	Versionable findLiveVersion(Identifier id, User user, boolean respectAnonPermissions)  throws DotDataException, DotStateException,DotSecurityException;
	/**
	 * Finds the live version based on the identifier's id
	 * @param id
	 * @param user
	 * @param respectAnonPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	Versionable findLiveVersion(String id, User user, boolean respectAnonPermissions)  throws DotDataException, DotStateException,DotSecurityException;
	/**
	 * Finds all versions based on an versionable
	 * @param inode
	 * @param user
	 * @param respectAnonPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	List<Versionable>  findAllVersions(Versionable inode)  throws DotDataException, DotStateException,DotSecurityException;

	/**
	 * Finds all versions based on an id
	 * @param identifier {@link Identifier}
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	List<Versionable>  findAllVersions(Identifier identifier)  throws DotDataException, DotStateException,DotSecurityException;

	/**
	 * Finds previous versions based on an identifier (if exists)
	 * @param identifier {@link Identifier}
	 * @return Optional of Versionable
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	 Optional<Versionable> findPreviousVersion(Identifier identifier)  throws DotDataException, DotStateException,DotSecurityException;

	/**
	 * Finds previous versions based on an identifier (if exists)
	 * @param identifier {@link String}
	 * @return Optional of Versionable
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	Optional<Versionable> findPreviousVersion(String identifier)  throws DotDataException, DotStateException,DotSecurityException;

	/**
	 * Finds previous versions based on an identifier (if exists)
	 * @param identifier {@link String}
	 * @param user {@link User}
	 * @param respectAnonPermissions {@link Boolean}
	 * @return Optional of Versionable
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	 Optional<Versionable>  findPreviousVersion(String identifier, User user, boolean respectAnonPermissions)  throws DotDataException, DotStateException,DotSecurityException;

	/**
	 * Finds all versions based on an id
	 * @param inode
	 * @param user
	 * @param respectAnonPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	List<Versionable>  findAllVersions(String id)  throws DotDataException, DotStateException,DotSecurityException;
	
	/**
	 * Finds all versions based on any version of the object being passed in
	 * @param inode
	 * @param user
	 * @param respectAnonPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	List<Versionable>  findAllVersions(Versionable inode, User user, boolean respectAnonPermissions)  throws DotDataException, DotStateException,DotSecurityException;
	/**
	 * Finds all versions based on an identifier
	 * @param inode
	 * @param user
	 * @param respectAnonPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */

	List<Versionable>  findAllVersions(Identifier id, User user, boolean respectAnonPermissions)  throws DotDataException, DotStateException,DotSecurityException;
	/**
	 * Finds all versions based on an identifier.id
	 * @param inode
	 * @param user
	 * @param respectAnonPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	List<Versionable>  findAllVersions(String id, User user, boolean respectAnonPermissions)  throws DotDataException, DotStateException,DotSecurityException;
	/**
	 * Finds the deleted version based on any version of the object being passed in
	 * @param inode
	 * @param user
	 * @param respectAnonPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	Versionable findDeletedVersion(Versionable inode, User user, boolean respectAnonPermissions) throws DotDataException, DotStateException,DotSecurityException;
	/**
	 * Finds the deleted version based on the identifier
	 * @param id
	 * @param user
	 * @param respectAnonPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	Versionable findDeletedVersion(Identifier id, User user, boolean respectAnonPermissions)  throws DotDataException, DotStateException,DotSecurityException;
	/**
	 * Finds the deleted version based on the identifier's id
	 * @param id
	 * @param user
	 * @param respectAnonPermissions
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	Versionable findDeletedVersion(String id, User user, boolean respectAnonPermissions) throws DotDataException, DotStateException,DotSecurityException;
	
	/**
	 * Tells if the versionable is live
	 * 
	 * @param versionable
	 * @return true if it is live. false if not
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	boolean isLive(Versionable versionable) throws DotDataException, DotStateException,DotSecurityException;
	
	/**
	 * Set this versionable as the live version for its identifier
	 * 
	 * @param versionable versionable to be set as the live version
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	void setLive(Versionable versionable) throws DotDataException, DotStateException,DotSecurityException;
	
	/**
	 * Remove the reference to the live version for this identifier. This is useful to unpublish 
	 * an asset.
	 * 
	 * @param identifier identifier of the asset to be left without live version
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	void removeLive(String identifier) throws DotDataException, DotStateException,DotSecurityException;
	
	/**
     * Remove the reference to the live version for this identifier. This is useful to unpublish 
     * an asset. This is the contentlet specific method as we need the language_id for those cases 
     * 
     * @param contentlet 
     * @throws DotDataException
     * @throws DotStateException
     * @throws DotSecurityException
     */
    void removeLive(Contentlet contentlet) throws DotDataException, DotStateException,DotSecurityException;
	
	/**
	 * Tells if the versionable is the working version for its identifier
	 * 
	 * @param versionable
	 * @return true if it is the working version. False if not
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	boolean isWorking(Versionable versionable) throws DotDataException, DotStateException,DotSecurityException;

	/**
	 * Tells if has working version in any language.
	 *
	 * @param versionable
	 * @return true if it has the working version. False if not
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	boolean hasWorkingVersionInAnyOtherLanguage(Versionable versionable, long versionableLanguageId) throws DotDataException, DotStateException,DotSecurityException;

	/**
	 * Sets the versionable as the working version for its identifier
	 * 
	 * @param versionable
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	void setWorking(Versionable versionable) throws DotDataException, DotStateException,DotSecurityException;
	
	/**
	 * Tells if the versionable is locked
	 * 
	 * @param ver
	 * @return true if it is locked, false if not
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	boolean isLocked(Versionable ver) throws DotDataException, DotStateException,DotSecurityException;
	
	/**
	 * Returns the userId of the owner of the asset's lock
	 * 
	 * @param ver
	 * @return
	 */
	Optional<String> getLockedBy(Versionable ver) throws DotDataException;
	
	/**
	 * Returns the date when the asset were locked
	 * 
	 * @param ver
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	Optional<Date> getLockedOn(Versionable ver) throws DotDataException, DotStateException,DotSecurityException;
	
	/**
	 * Allows to change locked status for the versionable 
	 * 
	 * @param ver
	 * @param locked status to be set
	 * @param user lock owner
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	void setLocked(Versionable ver, boolean locked, User user) throws DotDataException, DotStateException,DotSecurityException;
	
	/**
	 * Tells if the versionable is deleted
	 * 
	 * @param ver
	 * @return true if it is deleted, false if not
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	boolean isDeleted(Versionable ver) throws DotDataException, DotStateException;
	
	/**
	 * Allows to delete (when true) of undelete (when false) a versionable 
	 * 
	 * @param ver
	 * @param deleted true to delete, false to undelete
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	void setDeleted(Versionable ver, boolean deleted) throws DotDataException, DotStateException,DotSecurityException;
	
	
	/**
	 * Will return the @ContentletLangVersionInfo holder for the given identifier
	 * @param identifier
	 * @param lang
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	Optional<ContentletVersionInfo> getContentletVersionInfo(String identifier, long lang);

	/**
	 * Will return the @ContentletLangVersionInfo holder for the given identifier
	 *
	 * @param identifier
	 * @param lang
	 * @param variantId
	 * @return
	 */
	Optional<ContentletVersionInfo> getContentletVersionInfo(String identifier, long lang, String variantId);

	/**
	 * Will save the VersionInfo Record. For normal operations you should use the setLive, setWorking etc... but there are cases like
	 * PushPublishing where you want to say the entire record. 
	 * 
	 * @param cvInfo
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	void saveVersionInfo(VersionInfo vInfo) throws DotDataException, DotStateException;
	
	/**
	 * Will save the contentletVersionInfo Record. For normal operations you should use the setLive, setWorking etc... but there are cases like
	 * PushPublishing where you want to say the entire record. 
	 * 
	 * @param cvInfo
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	void saveContentletVersionInfo(ContentletVersionInfo cvInfo) throws DotDataException, DotStateException;
	
	/**
	 * Will return the @VersionInfo holder for the given identifier
	 * @param identifier
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	VersionInfo getVersionInfo(String identifier) throws DotDataException, DotStateException;
	
	void deleteVersionInfo(String identifier) throws DotDataException;
	
	void deleteContentletVersionInfoByLanguage(final Contentlet contentlet) throws DotDataException;
	void deleteContentletVersionInfoByVariant(final Contentlet contentlet) throws DotDataException;

	boolean hasLiveVersion(Versionable identifier)  throws DotDataException, DotStateException;
	
	/**
	 * 
	 * @param identifier
	 * @param lang
	 */
	void removeContentletVersionInfoFromCache(String identifier, long lang);
	
	/**
	 * 
	 * @param identifier
	 * @param lang
	 */
	void removeVersionInfoFromCache(String identifier);
	
	/**
	 * Will return a list of all ContentletVersionInfo for a given piece of content (if there are multiple languages) 
	 * @param identifier
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 */
    List<ContentletVersionInfo> findContentletVersionInfos(String identifier) throws DotDataException, DotStateException;

	/**
	 * Will return a list of all ContentletVersionInfo for a given piece of content and {@link com.dotcms.variant.model.Variant}
	 * (if there are multiple languages)
	 *
	 * @param identifier
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	List<ContentletVersionInfo> findContentletVersionInfos(String identifier, String variantName)
			throws DotDataException, DotStateException;

	/**
	 * Will return a list of all {@link ContentletVersionInfo} for a given {@link com.dotcms.variant.model.Variant},
	 * if there are multiple languages it is going to return all the languages versions.
	 *
	 * @param variant
	 * @return
	 * @throws DotDataException
	 */
	List<ContentletVersionInfo> findAllByVariant(final Variant variant) throws DotDataException;
}
