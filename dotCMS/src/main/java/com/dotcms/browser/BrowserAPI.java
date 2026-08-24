package com.dotcms.browser;

import com.dotcms.browser.BrowserAPIImpl.PaginatedContents;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates the logic to interact with the {@code Site Browser} portlet in dotCMS.
 * <p>From the Site Browser you can easily find, create, and modify folders, File Assets, Pages and Menu Links, and
 * upload File Assets to any folder.</p>
 *
 * @author Jonathan Sanchez
 * @since Apr 28th, 2020
 */
public interface BrowserAPI {

    /**
     * @deprecated see {@link #getFolderContent(BrowserQuery)}
     * @param usr
     * @param folderId
     * @param offset
     * @param maxResults
     * @param filter
     * @param mimeTypes
     * @param extensions
     * @param showArchived
     * @param noFolders
     * @param onlyFiles
     * @param sortBy
     * @param sortByDesc
     * @param excludeLinks
     * @param languageId
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Deprecated
    public default Map<String, Object> getFolderContent (final User usr, final String folderId, final int offset, final int maxResults, final String filter, final List<String> mimeTypes,
                                                 final List<String> extensions, final boolean showArchived, final boolean noFolders, final boolean onlyFiles, final String sortBy,
                                                 final boolean sortByDesc, final boolean excludeLinks, final long languageId) throws DotSecurityException, DotDataException {
    	return getFolderContent(usr, folderId, offset, maxResults, filter, mimeTypes, extensions, true, showArchived, noFolders, onlyFiles, sortBy, sortByDesc, excludeLinks, languageId);
    }

    /**
     * @deprecated see {@link #getFolderContent(BrowserQuery)}
     * @param usr
     * @param folderId
     * @param offset
     * @param maxResults
     * @param filter
     * @param mimeTypes
     * @param extensions
     * @param showArchived
     * @param noFolders
     * @param onlyFiles
     * @param sortBy
     * @param sortByDesc
     * @param languageId
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Deprecated
    public default Map<String, Object> getFolderContent (final  User usr, final String folderId, final int offset, final int maxResults, final String filter, final List<String> mimeTypes,
                                                 final List<String> extensions, final boolean showArchived, final boolean noFolders, final boolean onlyFiles, final String sortBy, final boolean sortByDesc, final long languageId ) throws DotSecurityException, DotDataException {
    	return getFolderContent(usr, folderId, offset, maxResults, filter, mimeTypes, extensions, true, showArchived, noFolders, onlyFiles, sortBy, sortByDesc, languageId);
    }

    /**
     * @deprecated see {@link #getFolderContent(BrowserQuery)}
     * @param user
     * @param folderId
     * @param offset
     * @param maxResults
     * @param filter
     * @param mimeTypes
     * @param extensions
     * @param showWorking
     * @param showArchived
     * @param noFolders
     * @param onlyFiles
     * @param sortBy
     * @param sortByDesc
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Deprecated
    public default Map<String, Object> getFolderContent(final User user, final String folderId,
                                                final int offset, final int maxResults, final String filter, final List<String> mimeTypes,
                                                final List<String> extensions, final boolean showWorking, final boolean showArchived, final boolean noFolders,
                                                final boolean onlyFiles, final String sortBy, final boolean sortByDesc)
			throws DotSecurityException, DotDataException {

    	return getFolderContent(user, folderId, offset, maxResults, filter, mimeTypes, extensions, showWorking, showArchived, noFolders, onlyFiles, sortBy, sortByDesc, false, 0);
    }

    /**
     * @deprecated see {@link #getFolderContent(BrowserQuery)}
     * @param user
     * @param folderId
     * @param offset
     * @param maxResults
     * @param filter
     * @param mimeTypes
     * @param extensions
     * @param showWorking
     * @param showArchived
     * @param noFolders
     * @param onlyFiles
     * @param sortBy
     * @param sortByDesc
     * @param languageId
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Deprecated
    public default Map<String, Object> getFolderContent(final User user, final String folderId,
                                                final int offset, final int maxResults, final String filter, final List<String> mimeTypes,
                                                final List<String> extensions, final boolean showWorking, final boolean showArchived, final boolean noFolders,
                                                final boolean onlyFiles, final String sortBy, final boolean sortByDesc, final long languageId)
			throws DotSecurityException, DotDataException {

    	return getFolderContent(user, folderId, offset, maxResults, filter, mimeTypes, extensions, showWorking, showArchived, noFolders, onlyFiles, sortBy, sortByDesc, false, languageId);
    }

    /**
     * @deprecated see {@link #getFolderContent(BrowserQuery)}
     * @param user
     * @param folderId
     * @param offset
     * @param maxResults
     * @param filter
     * @param mimeTypes
     * @param extensions
     * @param showWorking
     * @param showArchived
     * @param noFolders
     * @param onlyFiles
     * @param sortBy
     * @param sortByDesc
     * @param excludeLinks
     * @param languageId
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Deprecated
    public default Map<String, Object> getFolderContent(final User user, final String folderId,
                                                final int offset, final int maxResults, final String filter, final List<String> mimeTypes,
                                                final List<String> extensions, final boolean showWorking, final boolean showArchived, final boolean noFolders,
                                                final boolean onlyFiles, final String sortBy, final boolean sortByDesc, final boolean excludeLinks, final long languageId)
                    throws DotSecurityException, DotDataException {
        
        return getFolderContent( user,  folderId,
                         offset,  maxResults,  filter,  mimeTypes,
                        extensions,  showWorking,  showArchived,  noFolders,
                         onlyFiles,  sortBy,  sortByDesc,  excludeLinks,  languageId, false);
    }
    
	/**
	 * Gets the Folders, HTMLPages, Links, FileAssets under the specified folderId.
	 *
	 * @param user
	 * @param folderId
	 * @param offset
	 * @param maxResults
	 * @param filter
	 * @param mimeTypes
	 * @param extensions
	 *
	 * @param showWorking   If true, returns the working version of HTMLPages, Links and FileAssets in the folder.
	 * 						If false, returns the live version of HTMLPages, Links and FileAssets in the folder.
	 *
	 * @param showArchived  If true, includes the archived version of HTMLPages, Links and FileAssets in the folder.
	 *
	 * @param noFolders
	 * @param onlyFiles
	 * @param sortBy
	 * @param sortByDesc
	 * @param excludeLinks
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
    @Deprecated
	public default Map<String, Object> getFolderContent(final User user, final String folderId,
                                                final int offset, final int maxResults, final String filter, final List<String> mimeTypes,
                                                final List<String> extensions, final boolean showWorking, final boolean showArchived, final boolean noFolders,
                                                final boolean onlyFiles, final String sortBy, final boolean sortByDesc, final boolean excludeLinks, final long languageId, final boolean dotAssets)
			throws DotSecurityException, DotDataException {

	    final boolean showPages =! onlyFiles;

	    final BrowserQuery browserQuery = BrowserQuery.builder()
	                    .showDotAssets(dotAssets)
	                    .showLinks(!excludeLinks)
	                    .showExtensions(extensions)
	                    .withFilter(filter)
	                    .withHostOrFolderId(folderId)
	                    .withLanguageId(languageId)
	                    .offset(offset)
	                    .showFiles(true)
	                    .showPages(showPages)
	                    .showFolders(!noFolders)
	                    .showArchived(showArchived)
	                    .showWorking(showWorking)
                        .showMimeTypes(mimeTypes)
                        .maxResults(maxResults)
	                    .sortBy(sortBy)
	                    .sortByDesc(sortByDesc)
	                    .withUser(user).build();

	    return getFolderContent(browserQuery);
	}

    /**
     * Returns a map with two elements:
     *     1 - "total": size of the total elements available
     *     2 - "list":  collection of contentlets as a map to be showed in the browsers
     * @param browserQuery {@link BrowserQuery}
     * @return Map result
     * @throws DotSecurityException
     * @throws DotDataException
     */

	/**
	 * Returns the contents of a specific Folder in the form of a Map with the following data structure:
	 * <ul>
	 *     <li>{@code "total"}: The total count of items living under the Folder.</li>
	 *     <li>{@code "list"}: The Folder items represented as Maps so that their data can be processed in the UI.</li>
	 * </ul>
	 *
	 * @param browserQuery The {@link BrowserQuery} object with the appropriate query and filtering criteria.
	 *
	 * @return The Folder contents in the form of a Map.
	 *
	 * @throws DotSecurityException The {@link User} calling this operation does not have the required permissions to do
	 *                              so.
	 * @throws DotDataException     An error occurred when interacting with the data source.
	 */
	Map<String, Object> getFolderContent(final BrowserQuery browserQuery) throws DotSecurityException, DotDataException;


    /**
     * Retrieves a paginated collection of the assets that reside under the specified parent
     * ({@code browserQuery.directParent}).
     * <p>
     * Three sources may contribute to a page — folders, menu links and contentlets — each gated
     * by its own flag ({@code showFolders}, {@code showLinks}, {@code showContent}) and each
     * paged by its own independent cursor:
     * <ul>
     *   <li>Folders and links are read in full from the database and sliced in memory by
     *       {@code folderCursor} / {@code linkCursor}. Both are strictly the direct children of
     *       the parent.</li>
     *   <li>Contentlets are paged at the database level from {@code contentCursor}, and may be
     *       gathered recursively when {@code skipFolder} is enabled.</li>
     *   <li>The three sources consume {@code maxResults} in that order, so a page is filled with
     *       folders first, then links, then contentlets.</li>
     * </ul>
     * <p>
     * When implementing pagination, feed each {@code next*Cursor} from the response back as the
     * matching {@code *Cursor} on the following request and keep {@code offset} at 0. Once a
     * source reports {@code hasMore* == false} its flag can be switched off to skip that query
     * entirely. See {@link com.dotcms.browser.BrowserAPIImpl.PaginatedContents} for the full
     * contract.
     *
     * @param browserQuery the query parameters defining parent, pagination, and flags for which
     *                     elements (content, folders, links) to include
     * @return a {@link com.dotcms.browser.BrowserAPIImpl.PaginatedContents} holding this page's
     * merged, sorted list plus the per-source counts and cursors
     * @throws DotSecurityException if the user does not have permission to access the parent or
     *                              contents
     * @throws DotDataException     if a data retrieval error occurs at the database level
     */
    PaginatedContents getPaginatedContents(final BrowserQuery browserQuery)
            throws DotSecurityException, DotDataException;

	/**
	 * Returns a collection of contentlets that live inside the parent(browserQuery.directParent)
	 * @param browserQuery {@link BrowserQuery}
	 * @return list of contentlets
	 */
	List<Contentlet> getContentUnderParentFromDB(BrowserQuery browserQuery);

	/**
	 * Returns a collection of contentlets, folders, links that live inside the parent(browserQuery.directParent)
	 * The underlying flag respectFrontEndRoles passed by this method is set to true by default
	 * @param browserQuery {@link BrowserQuery}
	 * @return list of treeable (folders, content, links)
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	List<Treeable> getFolderContentList(BrowserQuery browserQuery) throws DotSecurityException, DotDataException;

	/**
	 * Returns a collection of contentlets, folders, links that live inside the parent(browserQuery.directParent)
	 * @param browserQuery {@link BrowserQuery}
	 * @param respectFrontEndRoles if true, the method will respect the front end roles
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	List<Treeable> getFolderContentList(BrowserQuery browserQuery, boolean respectFrontEndRoles) throws DotSecurityException, DotDataException;


}
