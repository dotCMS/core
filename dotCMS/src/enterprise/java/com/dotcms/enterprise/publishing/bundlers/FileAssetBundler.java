/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.bundlers;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publisher.util.dependencies.DependencyModDateUtil;
import com.dotcms.publisher.util.dependencies.PushedAssetUtil;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * This bundler will take the list of File Assets and export them as XML files for:
 * <ul>
 *     <li>Site Search.</li>
 *     <li>Static Publishing, such as AWS S3.</li>
 * </ul>
 * <p>Notice that this Bundler is used by Site Search feature as well, but it's <b>NOT USED</b> by the Dynamic Push
 * Publishing mechanism. In the latter, File Assets are treated as Contentlets and have their own generic Bundler.</p>
 *
 * @author Jorge Urdaneta
 * @since Jun 11, 2012
 */
public class FileAssetBundler implements IBundler {

	private PublisherConfig config;
	private IPublisher publisher;
	private LanguageAPI langAPI = null;
	private ContentletAPI conAPI = null;
	private UserAPI uAPI = null;
	private FileAssetAPI fAPI = null;
	private User systemUser = null;
	
	public final static String  FILE_ASSET_EXTENSION = ".fileAsset.xml" ;

	@Override
	public String getName() {
		return "File Asset Bundler";
	}
	
	@Override
	public void setConfig(final PublisherConfig pc) {
		config = pc;
		langAPI = APILocator.getLanguageAPI();
		conAPI = APILocator.getContentletAPI();
		uAPI = APILocator.getUserAPI();
		fAPI = APILocator.getFileAssetAPI();
		try {
			systemUser = uAPI.getSystemUser();
		} catch (final DotDataException e) {
            Logger.fatal(FileAssetBundler.class, String.format("An error occurred when initializing the " +
                    "FileAssetBundler: %s", e.getMessage()), e);
		}
	}

    @Override
    public void setPublisher(final IPublisher publisher) {
    	this.publisher = publisher;
    }

	@Override
	public void generate(final BundleOutput output, final BundlerStatus status) throws DotBundleException {
	    if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            throw new RuntimeException("Need an enterprise license to run this bundler");
        }
	    
        final StringBuilder luceneQuery = new StringBuilder("+baseType:" + BaseContentType.FILEASSET.getType() + " ");
        if (UtilMethods.isSet(this.config.getExcludePatterns())) {
			luceneQuery.append(" -(" );
			for (final String pattern : this.config.getExcludePatterns()) {
				if (UtilMethods.isSet(pattern)) {
					//if we detect blank spaces wil try enclosing in double quotes
					if(StringUtils.hasWhiteSpaces(pattern)){
						// Adding double quotes for the query to work with File asset paths containing blank spaces
						luceneQuery.append("path:\"").append(pattern).append("\" ");
					} else {
						luceneQuery.append("path:").append(pattern).append(" ");
					}
				}
			}
			luceneQuery.append(")" );
		} else if (UtilMethods.isSet(this.config.getIncludePatterns())) {
			luceneQuery.append(" +(" );
			for (final String pattern : this.config.getIncludePatterns()) {
				if (UtilMethods.isSet(pattern)) {
					//if we detect blank spaces wil try enclosing in double quotes
					if(StringUtils.hasWhiteSpaces(pattern)){
						// Adding double quotes for the query to work with File asset paths containing blank spaces
						luceneQuery.append("path:\"").append(pattern).append("\" ");
					} else {
						luceneQuery.append("path:").append(pattern).append(" ");
					}
				}
			}
			luceneQuery.append(")" );
		} else {
        	// Ignore static-publishing over file-assets that are not part of include patterns (https://github.com/dotCMS/core/issues/10504)
        	if (this.config.isStatic()) {
        		return;
        	}
		}
		
		if (this.config.isIncremental()) {
			final Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, 1900);
			
			Date start;
		    Date end;
		    
		    if (this.config.getStartDate() != null) {
		        start = this.config.getStartDate();
		    } else {
                start = cal.getTime();                
	        }
		    
		    if (this.config.getEndDate() != null) {
		    	end = this.config.getEndDate();
		    } else {
		    	end = cal.getTime();
		    }
            luceneQuery.append(" +versionTs:[" + ESMappingAPIImpl.datetimeFormat.format(start.toInstant())
                    + " TO " + ESMappingAPIImpl.datetimeFormat.format(end.toInstant()) + "] ");
		}
		
		if (UtilMethods.isSet(this.config.getHosts())) {
			luceneQuery.append(" +(" );
			for (final Host site : this.config.getHosts()){
				luceneQuery.append("conhost:" + site.getIdentifier() + " ");
			}
			luceneQuery.append(" ) ");
		}
		
		Logger.info(FileAssetBundler.class,luceneQuery.toString());
		for (final Long languageId : getSortedConfigLanguages(this.config, this.langAPI.getDefaultLanguage().getId())){
			processFileAssets(output, luceneQuery.toString() + " +languageid:" + languageId + " ", status);
		}
	}

    /**
     * Takes the File Assets that will be added to the bundle and process them in order to determine which specific
     * files will be added to the actual bundle.
     *
     * @param output      The location where bundle data will be added to the bundle.
     * @param luceneQuery The Lucene query that allows dotCMS to find and filter the files that will be added to the
     *                    bundle.
     * @param status      The object that provides status information for the bundle.
     *
     * @throws DotBundleException An error occurred with the bundle creation process.
     */
	protected void processFileAssets(final BundleOutput output, final String luceneQuery,
									 final BundlerStatus status) throws DotBundleException {
		final int limit = 200;
		int page = 0;
		status.setTotal(0);
		
		while (true) {
			final List<ContentletSearch> searchResults = new ArrayList<>();
			try {
				searchResults.addAll(this.conAPI.searchIndex(luceneQuery + " +live:true", limit, page * limit, "inode", this.systemUser, true));
			} catch (final Exception e) {
				Logger.debug(FileAssetBundler.class,e.getMessage(),e);
			}
			try {
			    if (!this.config.liveOnly() || this.config.isIncremental()) {
                    searchResults.addAll(conAPI.searchIndex(luceneQuery + " +working:true", limit, page * limit, "inode", this.systemUser, true));
                }
			} catch (final Exception e) {
				Logger.debug(FileAssetBundler.class,e.getMessage(),e);
			}

			page++;
			
			// no more content
			if (searchResults.isEmpty() || page > 100000000) {
				break;
			}
			
			status.setTotal(status.getTotal() + searchResults.size());
			final List<String> inodes = new ArrayList<>();
			for (final ContentletSearch searchResult : searchResults) {
				inodes.add(searchResult.getInode());
			}
			List<FileAsset> assets;
			try {
				final List<Contentlet> cons = this.conAPI.findContentlets(inodes);
				assets = this.fAPI.fromContentlets(cons);
			} catch (final Exception e) {
			    final String errorMsg = String.format("An error occurred when processing File Assets based on query [ %s ]: %s", luceneQuery, e.getMessage());
				Logger.error(FileAssetBundler.class, errorMsg, e);
				throw new DotBundleException(errorMsg, e);
			}

			final DependencyModDateUtil dependencyModDateUtil = new DependencyModDateUtil(this.config);
			final PushedAssetUtil pushedAssetUtil = new PushedAssetUtil(config);

			final Set<String> contents = new HashSet<>();
			
			for (final FileAsset fileAsset : assets) {
				try {
					//We will check is already pushed only for static.
					if (config.shouldManageDependencies()){
						//If publisher reports that a file should be pushed (issue https://github.com/dotCMS/core/issues/10560)
						//For instance, in case bucket does not exist in a static push scenario
						if (this.publisher != null && this.publisher.shouldForcePush(fileAsset.getHost(), fileAsset.getLanguageId())) {
							writeFileAsset(output, fileAsset);
						}
						//If we are able to add to DependencySet means that page will be pushed and we need to write file.
						//Send fileAsset.getIdentifier() because Push Publish standard.
						else if ( !contents.contains(fileAsset.getIdentifier()) &&
								(this.config.getOperation() == Operation.UNPUBLISH || dependencyModDateUtil.excludeByModDate(fileAsset, PusheableAsset.CONTENTLET))) {

							contents.add(fileAsset.getIdentifier());
							writeFileAsset(output, fileAsset);
							pushedAssetUtil.savePushedAssetForAllEnv(fileAsset, PusheableAsset.CONTENTLET);
						} else {
							//If we are not able to add to DependencySet means that the file will not be generated and
							//the status total will be decreased.
                            Logger.warn(FileAssetBundler.class, String.format("File Asset with ID '%s' was not added " +
                                    "to the bundle as it hasn't been modified lately.", fileAsset.getIdentifier()));
                            status.setTotal(status.getTotal() - 1);
						}
					} else {
						writeFileAsset(output, fileAsset);
						status.addCount();
					}

				} catch (final Exception e) {
                    Logger.error(FileAssetBundler.class, String.format("An error occurred when processing writing of " +
                            "File Asset '%s%s' with ID '%s': %s", fileAsset.getPath(), fileAsset.getFileName(),
                            fileAsset.getIdentifier(), e.getMessage()), e);
					status.addFailure();
				}


			}
		}
	}

    /**
     * Writes the specified {@link FileAsset} object to the bundle contents pointed by the {@link BundleOutput}.
     *
     * @param output    The location where bundle data is being written.
     * @param fileAsset The File Asset that will be written to the bundle.
     *
     * @throws IOException        An error occurred when writing the File Asset to file system.
     * @throws DotBundleException An error occurred with the writing process.
     */
	private void writeFileAsset(final BundleOutput output, final FileAsset fileAsset) throws IOException, DotBundleException{
		try{
			final Host site = APILocator.getHostAPI().find(fileAsset.getHost(), this.systemUser, true);
			final Optional<ContentletVersionInfo> info = APILocator.getVersionableAPI()
					.getContentletVersionInfo(fileAsset.getIdentifier(), fileAsset.getLanguageId());

			if (info.isEmpty()) {
				throw new DotDataException("Can't find ContentletVersionInfo for Identifier: "
						+ fileAsset.getIdentifier() + ". Lang: " + fileAsset.getLanguageId());
			}

			final FileAssetWrapper wrap = new FileAssetWrapper();
			wrap.setAsset(fileAsset);
			wrap.setInfo(info.get());
			wrap.setId(APILocator.getIdentifierAPI().find(fileAsset.getIdentifier()));

			// Replicate file-assets from default language across all languages (https://github.com/dotCMS/core/issues/10573)
			if (com.dotmarketing.util.Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", true) &&
				fileAsset.getLanguageId() == this.langAPI.getDefaultLanguage().getId() && !this.config.getLanguages().isEmpty()) {
				for (final String languageId : this.config.getLanguages()) {
					writeFileToDisk(site, languageId, output, wrap);
				}
			} else {
				writeFileToDisk(site, String.valueOf(fileAsset.getLanguageId()), output, wrap);
			}
		} catch (final Exception e) {
            throw new DotBundleException(String.format("An error occurred when writing File Asset '%s%s' with ID '%s'" +
                    " to disk: %s", fileAsset.getPath(), fileAsset.getFileName(), fileAsset.getIdentifier(), e
                    .getMessage()), e);
		}
	}
	
	private void writeFileToDisk(Host host, String languageId, BundleOutput output, FileAssetWrapper fileAssetWrapper)
			throws IOException, DotDataException, DotSecurityException {

		String liveworking = (fileAssetWrapper.getAsset().getInode().equals(fileAssetWrapper.getInfo().getLiveInode() )) ? "live" : "working";
		String myFile = File.separator
				+liveworking + File.separator 
				+ host.getHostname() + File.separator + languageId
				+ fileAssetWrapper.getAsset().getURI().replace("/", File.separator) + FILE_ASSET_EXTENSION;

		
		// Should we write or is the file already there:
		Calendar cal = Calendar.getInstance();
		cal.setTime(fileAssetWrapper.getInfo().getVersionTs());
		cal.set(Calendar.MILLISECOND, 0);

    	/*
    	 * Inhibited xml-file generation for static-publishing scenario
    	 * Performance enhancement due to https://github.com/dotCMS/core/issues/12291
    	 */
		if( !config.isStatic() && (!output.exists(myFile) ||
				output.lastModified(myFile)  != cal.getTimeInMillis()) ){
		    if(output.exists(myFile)) {
				output.delete(myFile); // unlink possible existing hard link
			}

			try(final OutputStream outputStream = output.addFile(myFile)) {

				BundlerUtil.objectToXML(fileAssetWrapper, outputStream);
				output.setLastModified(myFile, cal.getTimeInMillis());
			}
		}

		final boolean deleteFile = config.liveOnly() && config.isIncremental() && !fileAssetWrapper.getAsset().isLive();

		String filePath = myFile.replaceAll(FILE_ASSET_EXTENSION, "");
		if(deleteFile) {
			output.delete(filePath);
			filePath = File.separator
                    +"live" + File.separator 
                    + host.getHostname() + File.separator + languageId
                    + fileAssetWrapper.getAsset().getURI().replace("/", File.separator);
		    if(output.exists(filePath)) {
				output.delete(filePath);
            }
		}
		else {
		    //only write if changed
			if(!output.exists(filePath) || output.lastModified(filePath) != cal.getTimeInMillis()){
				File oldAsset = new File(APILocator.getFileAssetAPI().getRealAssetPathIgnoreExtensionCase(fileAssetWrapper.getAsset().getInode(), fileAssetWrapper.getAsset().getUnderlyingFileName()));
				if(output.exists(filePath)) {
					output.delete(filePath);
				}

				FileUtil.copyFile(oldAsset, output.getFile(filePath), true);
				output.setLastModified(filePath, cal.getTimeInMillis());
			}
		}		
	}
	
	@Override
	public FileFilter getFileFilter(){
		return new FileObjectBundlerFilter();
		
	}

	public class FileObjectBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(FILE_ASSET_EXTENSION));
		}

	}
	
}
