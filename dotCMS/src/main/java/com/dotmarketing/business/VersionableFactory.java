package com.dotmarketing.business;

import java.util.List;
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
	 * 
	 * @param identifier
	 * @param lang
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	protected abstract Optional<ContentletVersionInfo> getContentletVersionInfo(String identifier, long lang) throws DotDataException, DotStateException;

	/**
	 * The method will load from Hibernate and NOT use cache
	 * 
	 * @param cvInfo
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	protected abstract Optional<ContentletVersionInfo> findContentletVersionInfoInDB(String identifier, long lang) throws DotDataException, DotStateException;

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
	 * 
	 * @param identifier
	 * @param lang
	 * @param workingInode
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	protected abstract ContentletVersionInfo createContentletVersionInfo(Identifier identifier, long lang, String workingInode) throws DotStateException, DotDataException;

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
     * @param identifier
     * @return
     * @throws DotDataException
     * @throws DotStateException
     */
    protected abstract VersionInfo findVersionInfoFromDb(Identifier identifier) throws DotDataException, DotStateException;

    protected abstract List<ContentletVersionInfo> findAllContentletVersionInfos(String identifier)
        throws DotDataException, DotStateException ;

}
