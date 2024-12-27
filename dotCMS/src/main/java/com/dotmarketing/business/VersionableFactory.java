package com.dotmarketing.business;

import com.dotcms.variant.model.Variant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;

/**
 * Provides access to information regarding the different versions (working,
 * live, and version info) of a dotCMS content object -i.e., contents,
 * containers, templates, link, and so on.
 * <p>
 * dotCMS keeps track of changes made to its content objects every time they are
 * changed. This also allows content authors to go back to a previous version of
 * a content object. This class allows developers to create the different types
 * of versions for them.
 *
 * @author Will Ezell
 * @version 1.0
 * @since Mar 22, 2012
 * 
 */
public abstract class VersionableFactory {

	/**
	 * Returns the working version of the versionable object represented by the
	 * specified identifier. A versionable is a dotCMS object that can have
	 * several versions of its content. A working version represents an object
	 * that cannot yet be seen from the front-end as it is not published.
	 * 
	 * @param id
	 *            - The identifier of the versionable.
	 * @return The working version of the specified dotCMS object.
	 * @throws DotDataException
	 *             An error occurred when retrieving information from the
	 *             database.
	 * @throws DotStateException
	 *             The version info of the specified object could not be
	 *             retrieved.
	 */
	protected abstract Versionable findWorkingVersion(String id) throws DotDataException, DotStateException;

	/**
	 * Returns the live version of the versionable object represented by the
	 * specified identifier. A versionable is a dotCMS object that can have
	 * several versions of its content. The live version represents an object
	 * that can be seen from the front-end as it is published.
	 * 
	 * @param id
	 *            - The identifier of the versionable.
	 * @return The working version of the specified dotCMS object.
	 * @throws DotDataException
	 *             An error occurred when retrieving information from the
	 *             database.
	 * @throws DotStateException
	 *             The version info of the specified object could not be
	 *             retrieved.
	 */
	protected abstract Versionable findLiveVersion(String id) throws DotDataException, DotStateException;

	/**
	 * 
	 * @param id
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	protected abstract Versionable findDeletedVersion(String id) throws DotDataException, DotStateException;

	/**
	 * 
	 * @param id
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	protected abstract List<Versionable> findAllVersions(String id) throws DotDataException, DotStateException;

	/**
	 * Find all versions limitated by the maxResults (if maxResults is present)
	 * @param identifier {@link String}
	 * @param maxResults {@link Integer}
	 * @return List of Versionable
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	protected abstract List<Versionable> findAllVersions(String identifier, Optional<Integer> maxResults) throws DotDataException, DotStateException;

	/**
	 * 
	 * @param info
	 * @param updateVersionTS
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	protected abstract void saveVersionInfo(VersionInfo info, boolean updateVersionTS) throws DotDataException, DotStateException;

	/**
	 * 
	 * @param identifier
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	protected abstract VersionInfo getVersionInfo(String identifier) throws DotDataException, DotStateException;

	/**
	 * Get a list of all the working versions of a contentlet excluding the one with the specified language id.
	 * @param identifier
	 * @param lang
	 * @return List of the rows (working_inode + lang)
	 * @throws DotDataException
	 */
	protected abstract List<Map<String, Object>> getWorkingVersionsExcludingLanguage(String identifier, long lang) throws DotDataException;

	/**
	 * 
	 * @param identifier
	 * @param lang
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	protected abstract Optional<ContentletVersionInfo> getContentletVersionInfo(String identifier, long lang) throws DotDataException, DotStateException;

	protected abstract Optional<ContentletVersionInfo> getContentletVersionInfo(
			final String identifier, final long lang, final String variantId) throws DotDataException, DotStateException;

	/**
	 * The method will load from Hibernate and NOT use cache
	 * 
	 * @param cvInfo
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	protected abstract Optional<ContentletVersionInfo> findContentletVersionInfoInDB(String identifier, long lang) throws DotDataException, DotStateException;

	/**
	 * Return a {@link ContentletVersionInfo}
	 *
	 * @param identifier
	 * @param lang
	 * @param variantId
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	protected abstract Optional<ContentletVersionInfo> findContentletVersionInfoInDB(
			String identifier, long lang, String variantId) throws DotDataException, DotStateException;


	/**
	 * 
	 * @param cvInfo
	 * @param updateVersionTS
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	protected abstract void saveContentletVersionInfo(ContentletVersionInfo cvInfo, boolean updateVersionTS) throws DotDataException, DotStateException;

	/**
	 * 
	 * @param identifier
	 * @param workingInode
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	protected abstract VersionInfo createVersionInfo(Identifier identifier, String workingInode) throws DotStateException, DotDataException;

	/**
	 * Create a new {@link ContentletVersionInfo} with the {@link com.dotcms.variant.VariantAPI#DEFAULT_VARIANT}
	 * and with de live inode equals to null.
	 *
	 * @param identifier {@link ContentletVersionInfo}'s ID
	 * @param lang {@link ContentletVersionInfo}'s language
	 * @param workingInode {@link ContentletVersionInfo}'s working inode
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	protected abstract ContentletVersionInfo createContentletVersionInfo(Identifier identifier, long lang, String workingInode) throws DotStateException, DotDataException;

	/**
	 * Create a new {@link ContentletVersionInfo} with with de live inode equals to null.
	 *
	 * @param identifier {@link ContentletVersionInfo}'s ID
	 * @param lang {@link ContentletVersionInfo}'s language
	 * @param workingInode {@link ContentletVersionInfo}'s working inode
	 * @param variantId {@link ContentletVersionInfo}'s variant
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	protected abstract ContentletVersionInfo createContentletVersionInfo(final Identifier identifier,
			final long lang, final String workingInode, final String variantId)
			throws DotStateException, DotDataException;


	/**
	 * 
	 * @param identifier
	 * @throws DotDataException
	 */
	protected abstract void deleteVersionInfo(String identifier) throws DotDataException;

	/**
	 * 
	 * @param id
	 * @param lang
	 * @throws DotDataException
	 */
    protected abstract void deleteContentletVersionInfo(String id, long lang) throws DotDataException;

	/**
	 *
	 * @param id
	 * @param lang
	 * @param variantId
	 * @throws DotDataException
	 */
	protected abstract void deleteContentletVersionInfo(String id, final String variantId)
			throws DotDataException;

    /**
     * 
     * @param identifier
     * @return
     * @throws DotDataException
     * @throws DotStateException
     */
    protected abstract VersionInfo findVersionInfoFromDb(Identifier identifier) throws DotDataException, DotStateException;

	/**
	 * Return any version of the {@link com.dotmarketing.portlets.contentlet.model.Contentlet} no matter the
	 * {@link com.dotmarketing.portlets.languagesmanager.model.Language} or the {@link com.dotcms.variant.model.Variant}
	 */
    protected abstract Optional<ContentletVersionInfo> findAnyContentletVersionInfo(String identifier) throws DotDataException;

	/**
	 * Return any version of the {@link com.dotmarketing.portlets.contentlet.model.Contentlet} in
	 * the {@link com.dotcms.variant.VariantAPI#DEFAULT_VARIANT} no matter the
	 * {@link com.dotmarketing.portlets.languagesmanager.model.Language}.
	 *
	 * @param identifier {@link com.dotmarketing.portlets.contentlet.model.Contentlet}'s Identifier that you are looking for
	 * @param deleted If it is true then return just archived {@link com.dotmarketing.portlets.contentlet.model.Contentlet}.
	 * @return
	 * @throws DotDataException
	 */
	public abstract Optional<ContentletVersionInfo> findAnyContentletVersionInfo(final String identifier, final boolean deleted)
			throws DotDataException;

	/**
	 * Return any version of the {@link com.dotmarketing.portlets.contentlet.model.Contentlet} no matter the
	 * {@link com.dotmarketing.portlets.languagesmanager.model.Language} or the {@link com.dotcms.variant.model.Variant}
	 *
	 * @param identifier {@link com.dotmarketing.portlets.contentlet.model.Contentlet}'s Identifier that you are looking for
	 * @param deleted If it is true then return just archived {@link com.dotmarketing.portlets.contentlet.model.Contentlet}.
	 * @return
	 * @throws DotDataException
	 */
	public abstract Optional<ContentletVersionInfo> findAnyContentletVersionInfoAnyVariant(final String identifier, final boolean deleted)
			throws DotDataException;

	public abstract Optional<ContentletVersionInfo> findAnyContentletVersionInfo(final String identifier, final String variant,
			final boolean deleted)
			throws DotDataException;

    protected abstract List<ContentletVersionInfo> findAllContentletVersionInfos(String identifier)
        throws DotDataException, DotStateException ;

	protected abstract List<ContentletVersionInfo> findAllContentletVersionInfos(String identifier, String variantName)
			throws DotDataException, DotStateException ;

	/**
	 * Return all versions of the {@link com.dotmarketing.portlets.contentlet.model.Contentlet}
	 * for the specific {@link com.dotcms.variant.model.Variant} no matter the {@Link Language}
	 *
	 * @param variant
	 * @return
	 */
    public abstract List<ContentletVersionInfo> findAllByVariant(Variant variant)
			throws DotDataException;
}
