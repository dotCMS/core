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
import com.dotcms.enterprise.publishing.remote.bundler.ExtensionFileFilter;
import com.dotcms.enterprise.publishing.staticpublishing.StaticDependencyBundler;
import com.dotcms.publishing.*;
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
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This bundler will take the list of HTML Pages and export them as XML files for:
 * <ul>
 *     <li>Site Search.</li>
 *     <li>Static Publishing, such as AWS S3.</li>
 * </ul>
 * <p>Notice that this Bundler is used by Site Search feature as well, but it's <b>NOT USED</b> by the Dynamic Push
 * Publishing mechanism. In the latter, HTML Pages are treated as Contentlets and have their own generic Bundler.</p>
 *
 * @author Jorge Urdaneta
 * @since Jun 11, 2012
 */
public class HTMLPageAsContentBundler implements IBundler {

	private PublisherConfig config;
	private LanguageAPI langAPI = null;
	private ContentletAPI conAPI = null;
	private UserAPI uAPI = null;
	private User systemUser = null;

    public final static String[] HTMLPAGE_ASSET_EXTENSIONS = {".html.xml", ".html.json"};

	@Override
	public String getName() {
		return "HTMLPage as Content Bundler";
	}
	
	@Override
	public void setConfig(final PublisherConfig pc) {
		config = pc;
		langAPI = APILocator.getLanguageAPI();
		conAPI = APILocator.getContentletAPI();
		uAPI = APILocator.getUserAPI();
		try {
			systemUser = uAPI.getSystemUser();
		} catch (final DotDataException e) {
            Logger.fatal(FileAssetBundler.class, String.format("An error occurred when initializing the " +
                    "HTMLPageAsContentBundler: %s", e.getMessage()), e);
		}
	}

    @Override
    public void setPublisher(final IPublisher publisher) {
    }

	@Override
	public void generate(final BundleOutput output, final BundlerStatus status) throws DotBundleException {
	    if(LicenseUtil.getLevel()< LicenseLevel.STANDARD.level) {
            throw new RuntimeException("Need an enterprise license to run this bundler");
        }

        final StringBuilder luceneQuery = new StringBuilder("+baseType:" + BaseContentType.HTMLPAGE.getType() + " ");
		
		if (UtilMethods.isSet(this.config.getExcludePatterns())) {
			luceneQuery.append("-(" );
			for (final String pattern : config.getExcludePatterns()) {
				if (UtilMethods.isSet(pattern)) {
                    //if we detect blank spaces wil try enclosing in double quotes
					if(StringUtils.hasWhiteSpaces(pattern)){
                      // Adding double quotes for the query to work with HTML Page paths containing blank spaces
                      luceneQuery.append("path:\"").append(pattern).append("\" ");
                    } else {
						luceneQuery.append("path:").append(pattern).append(" ");
                    }
				}
			}
			luceneQuery.append(")" );
		}else if (UtilMethods.isSet(this.config.getIncludePatterns())) {
			luceneQuery.append("+(" );
			for (final String pattern : config.getIncludePatterns()) {
				if (UtilMethods.isSet(pattern)) {
					//if we detect blank spaces wil try enclosing in double quotes
					if(StringUtils.hasWhiteSpaces(pattern)){
						// Adding double quotes for the query to work with HTML Page paths containing blank spaces
						luceneQuery.append("path:\"").append(pattern).append("\" ");
					} else {
						luceneQuery.append("path:").append(pattern).append(" ");
					}
				}
			}
			luceneQuery.append(")" );
		} else {
        	// Ignore static-publishing over html-pages that are not part of include patterns (https://github.com/dotCMS/core/issues/10504)
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
	        luceneQuery.append(" +versionTs:[").append(ESMappingAPIImpl.datetimeFormat.format(start))
					.append(" TO ").append(ESMappingAPIImpl.datetimeFormat.format(end))
					.append("] ");
		}

		if (UtilMethods.isSet(this.config.getHosts())) {
			luceneQuery.append(" +(" );
			for (final Host site : this.config.getHosts()) {
				luceneQuery.append("conhost:").append(site.getIdentifier()).append(" ");
			}
			luceneQuery.append(" ) ");
		}

		Logger.info(HTMLPageAsContentBundler.class,luceneQuery.toString());

		final long defaultLanguage = this.langAPI.getDefaultLanguage().getId();
		for (final Long languageId : getSortedConfigLanguages(this.config, defaultLanguage)) {
			String queryLanguage = " +languageid:" + languageId + " ";
			/*
			If we are defaulting to default language we need to unify the current language
			with the default language pages.
			 */
			if (this.langAPI.canDefaultPageToDefaultLanguage() && !languageId.equals(defaultLanguage)) {
				queryLanguage =
						"+(languageid:" + languageId + " languageid:" + defaultLanguage + ") ";
			}
			processHTMLPages(output, luceneQuery.toString() + queryLanguage, status, languageId);
		}
	}

    /**
     * Takes the HTML Pages that will be added to the bundle and process them in order to determine which specific
     * files will be added to the actual bundle.
     *
     * @param bundleOutput The location where bundle data will be added to the bundle.
     * @param luceneQuery  The Lucene query that allows dotCMS to find and filter the HTML Pages that will be added to
     *                     the bundle.
     * @param status       The object that provides status information for the bundle.
     *
     * @throws DotBundleException An error occurred with the bundle creation process.
     */
    private void processHTMLPages(final BundleOutput bundleOutput, final String luceneQuery, final BundlerStatus status,
								  long languageToProcess) {
		final int limit = 200;
		int page = 0;
		status.setTotal(0);
        while (true) {
			final List<ContentletSearch> contentletSearchList = new ArrayList<>();
			try {
				contentletSearchList.addAll(this.conAPI
						.searchIndex(luceneQuery + " +live:true", limit, page * limit, "inode",
                                this.systemUser, true));
			} catch (final Exception e) {
				Logger.debug(HTMLPageAsContentBundler.class,e.getMessage(),e);
			}
			try {
				if (!this.config.liveOnly() || this.config.isIncremental()) {
					contentletSearchList.addAll(this.conAPI
							.searchIndex(luceneQuery + " +working:true", limit, page * limit,
									"inode", this.systemUser, true));
				}
			} catch (final Exception e) {
				Logger.debug(HTMLPageAsContentBundler.class,e.getMessage(),e);
			}

			/*
			Depending if we are defaulting to default language we could have duplicated identifiers,
			meaning, same page in the current and default language.
			 */
			final Set<String> identifiersToProcess = contentletSearchList.stream()
					.map(ContentletSearch::getIdentifier)
					.collect(Collectors.toSet());
			page++;
			// no more content
			if (contentletSearchList.isEmpty() || page > 100000000) {
				break;
			}

			status.setTotal(status.getTotal() + contentletSearchList.size());

			for (final String identifier : identifiersToProcess) {
                try {
					// Get the page taking into account the DEFAULT_PAGE_TO_DEFAULT_LANGUAGE property
					final IHTMLPage htmlPage = APILocator.getHTMLPageAssetAPI().findByIdLanguageFallback
							(identifier, languageToProcess, this.config.liveOnly(), this.systemUser, false);
					try {
                        final Host site = APILocator.getHostAPI().find(((Contentlet) htmlPage).getHost(), this
                                .systemUser, true);
						final Optional<ContentletVersionInfo> info = APILocator.getVersionableAPI()
								.getContentletVersionInfo(htmlPage.getIdentifier(),
										((Contentlet) htmlPage).getLanguageId());

						if (info.isEmpty()) {
							throw new DotDataException("Can't find ContentletVersionInfo for Identifier: "
									+ htmlPage.getIdentifier() + ". Lang: " + htmlPage.getLanguageId());
						}

						final HTMLPageAsContentWrapper wrap = new HTMLPageAsContentWrapper();
						wrap.setAsset(htmlPage);
						wrap.setInfo(info.get());
						wrap.setId(APILocator.getIdentifierAPI().find(htmlPage.getIdentifier()));
						//Write the page to disk
						writeFileToDisk(site, languageToProcess, bundleOutput, wrap);
					} catch (final Exception e) {
                        throw new DotBundleException(String.format("An error occurred when writing HTML Page with ID " +
                                "'%s' to disk: %s", htmlPage.getIdentifier(), e.getMessage()), e);
					}

                    /*///////////////////////////////////////////////
                    //TODO: Fow now we will always force push the HTML Pages. Avoid the case when content on the page
                    //TODO: is changed but not the page itself.
                    //We will check is already pushed only for static.
                    if (contents.isPresent()){
                        //If we are able to add to DependencySet means that page will be pushed and we need to write file.
                        //Send htmlPage.getIdentifier() because Push Publish standard.
                        if ( contents.get().addOrClean(htmlPage.getIdentifier(), htmlPage.getModDate()) ) {
                            writeHTMLPage(bundleRoot, htmlPage);
                        } else {
                            //If we are not able to add to DependencySet means that the file will not be generated and
                            //the status total will be decreased.
                            status.setTotal(status.getTotal() - 1);
                        }
                    } else {
                        //In case is it not static we will always write to disk.
                        writeHTMLPage(bundleRoot, htmlPage);
                    }
                    status.addCount();
                    //End of TODO.
                    ///////////////////////////////////////////////*/
                	status.addCount();
                } catch (final Exception e) {
                    Logger.error(HTMLPageAsContentBundler.class, String.format("An error occurred when processing writing of " +
                                    "HTML Page with ID '%s': %s", identifier, e.getMessage()), e);
                    status.addFailure();
                }
			}
		}
	}

	private void writeFileToDisk(final Host site, final long tryingLang, final BundleOutput bundleOutput, HTMLPageAsContentWrapper htmlPageWrapper) throws IOException, DotDataException, DotSecurityException {
		
		final boolean live = (htmlPageWrapper.getAsset().getInode().equals(htmlPageWrapper.getInfo().getLiveInode())) ;

		String liveworking = (htmlPageWrapper.getAsset().getInode()
				.equals(htmlPageWrapper.getInfo().getLiveInode())) ? "live" : "working";

        for (String htmlPageAssetExtension : HTMLPAGE_ASSET_EXTENSIONS) {
            String myFile = File.separator
                    + liveworking + File.separator
                    + site.getHostname() + File.separator + tryingLang
                    + htmlPageWrapper.getAsset().getURI().replace("/", File.separator)
                    + htmlPageAssetExtension;

            // Should we write or is the file already there:
            Calendar cal = Calendar.getInstance();
            cal.setTime(htmlPageWrapper.getInfo().getVersionTs());
            cal.set(Calendar.MILLISECOND, 0);

            //only write if changed
            String pageFilePath = myFile;

            if (!bundleOutput.exists(pageFilePath)
                    || bundleOutput.lastModified(pageFilePath) != cal.getTimeInMillis()) {
                if (bundleOutput.exists(pageFilePath)) {
                    bundleOutput.delete(pageFilePath); // unlink possible existing hard link
                }

                try (final OutputStream outputStream = bundleOutput.addFile(myFile)) {
                    BundlerUtil.writeObject(htmlPageWrapper, outputStream, myFile);
                    bundleOutput.setLastModified(pageFilePath, cal.getTimeInMillis());
                }
            }

            boolean deleteFile = config.liveOnly() && config.isIncremental() && !htmlPageWrapper.getAsset().isLive();

            pageFilePath = myFile.replaceAll(htmlPageAssetExtension, "");
            if (deleteFile) {
                if (bundleOutput.exists(pageFilePath)) {
                    bundleOutput.delete(pageFilePath);
                }
                pageFilePath = File.separator
                        + "live" + File.separator
                        + site.getHostname() + File.separator + tryingLang
                        + htmlPageWrapper.getAsset().getURI().replace("/", File.separator);
                if (bundleOutput.exists(pageFilePath)) {
                    bundleOutput.delete(pageFilePath);
                }
            } else {

                try (final OutputStream outputStream = bundleOutput.addFile(pageFilePath)) {
                    final String html = APILocator.getHTMLPageAssetAPI()
                            .getHTML(htmlPageWrapper.getAsset().getURI(),
                                    site, live, htmlPageWrapper.getAsset().getInode(),
                                    uAPI.getSystemUser(), tryingLang,
                                    getUserAgent(config));
                    if (UtilMethods.isSet(html)) {
                        outputStream.write(html.getBytes());
                    } else {
                        Logger.warn(HTMLPageAsContentBundler.class, String.format("HTML Page with ID '%s' has no " +
                                        "contents, or does not have the required CMS Anonymous permissions.",
                                htmlPageWrapper.getId().getId()));
                    }

                } catch (final Exception e) {
                    Logger.error(HTMLPageAsContentBundler.class, String.format("An error occurred when retrieving " +
                            "contents from page '%s' under Site '%s': %s", htmlPageWrapper.getAsset().getURI(), site
                            .getHostname(), e.getMessage()), e);
                }
            }
        }
	}
	
	@Override
	public FileFilter getFileFilter(){
        return new ExtensionFileFilter(HTMLPAGE_ASSET_EXTENSIONS);
	}


	
}
