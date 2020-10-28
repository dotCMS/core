package com.dotcms.browser;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates the logic to interact with the Browser App
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
    public Map<String, Object> getFolderContent(final BrowserQuery browserQuery) throws DotSecurityException, DotDataException;
}

