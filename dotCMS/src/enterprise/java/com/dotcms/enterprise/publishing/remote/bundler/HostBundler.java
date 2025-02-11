/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.HostWrapper;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.util.EnterpriseFeature;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.dotmarketing.util.Constants.DONT_RESPECT_FRONT_END_ROLES;

/**
 * This bundler will take the list of {@link Contentlet} (Host) objects that are being pushed and
 * will write them in the file system in the form of an XML file. This information will be part of
 * the bundle that will be pushed to the destination server.
 *
 * @author Jorge Urdaneta
 * @version 1.0
 * @since Mar 7, 2013
 */
public class HostBundler implements IBundler {

	private PushPublisherConfig config;
	private User systemUser;
	private ContentletAPI conAPI = null;
    private PublisherAPI pubAPI = null;
	private LanguageAPI langAPI = null;
	private final PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();

	public final static String HOST_EXTENSION = ".host.xml" ;

	@Override
	public String getName() {
		return "Host bundler";
	}

	@Override
	public void setConfig(final PublisherConfig pc) {
		config = (PushPublisherConfig) pc;
		conAPI = APILocator.getContentletAPI();
		pubAPI = PublisherAPI.getInstance();
		this.langAPI = APILocator.getLanguageAPI();
		try {
			this.systemUser = APILocator.getUserAPI().getSystemUser();
		} catch (final DotDataException e) {
			Logger.fatal(HostBundler.class,e.getMessage(),e);
		}
	}

    @Override
    public void setPublisher(IPublisher publisher) {
    }

	@Override
	@EnterpriseFeature
	public void generate(final BundleOutput output, final BundlerStatus status) throws DotBundleException {
		final Set<String> siteIds = config.getHostSet();
		try {
			final PublishAuditHistory currentAuditHistory = this.getCurrentPublishAuditHistory();
			if (UtilMethods.isSet(siteIds)) {
				// This content set is a dependency of other assets, like html pages
				final List<Contentlet> siteAsContentList = this.getSitesAsContentlets(siteIds);
				final Set<Contentlet> contentsToProcessWithFiles = this.getRelatedFilesAndContent(siteAsContentList);
				for (final Contentlet con : contentsToProcessWithFiles) {
					this.writeFileToDisk(output, con);
					status.addCount();
				}
			}
			this.updateAuditHistory(currentAuditHistory);
		} catch (final Exception e) {
			status.addFailure();
			Logger.error(this, String.format("Failed to pull Sites with IDs: %s", siteIds), e);
			throw new DotBundleException(String.format("Failed to pull content for Host Bundler: " +
					"%s", ExceptionUtil.getErrorMessage(e)), e);
		}
	}

	/**
	 * Returns the Publishing Audit History for the current bundle. Keep in mind that, depending on
	 * the system load or the available resources of the dotCMS instance, parts of the audit
	 * history may not be ready yet, which is expected.
	 *
	 * @return The current {@link PublishAuditHistory} object for the current bundle.
	 *
	 * @throws DotPublisherException An error occurred when retrieving or updating the bundle
	 *                               status.
	 */
	private PublishAuditHistory getCurrentPublishAuditHistory() throws DotPublisherException {
		PublishAuditHistory currentAuditHistory = null;
		if (!config.isDownloading()) {
			final PublishAuditStatus publishAuditStatus = this.pubAuditAPI.getPublishAuditStatus(this.config.getId());
			if (null != publishAuditStatus) {
				currentAuditHistory = this.pubAuditAPI.getPublishAuditStatus(this.config.getId()).getStatusPojo();
			}
			currentAuditHistory = null == currentAuditHistory ? new PublishAuditHistory() : currentAuditHistory;
			currentAuditHistory.setBundleStart(new Date());
			PushPublishLogger.log(this.getClass(), "Status Update: Bundling.");
			this.pubAuditAPI.updatePublishAuditStatus(this.config.getId(), PublishAuditStatus.Status.BUNDLING,
					currentAuditHistory);
		}
		return currentAuditHistory;
	}

	/**
	 * Takes the list of Site IDs that have been included in this bundle, and returns them as a
	 * list of their working and live versions in the form of {@link Contentlet} objects. Please
	 * take into consideration that, even though not having a live version of a Site is permitted,
	 * <b>there must ALWAYS be a working version of it</b>.
	 *
	 * @param siteIds The list of Site IDs to retrieve.
	 *
	 * @return A list of {@link Contentlet} objects representing the Sites to be bundled.
	 *
	 * @throws DotBundleException An error occurred when retrieving the Sites, or thw working
	 *                            version of a Site was not found.
	 */
	private List<Contentlet> getSitesAsContentlets(final Set<String> siteIds) throws DotBundleException {
		final List<Contentlet> siteAsContentList = new ArrayList<>();
		for (final String siteIdentifier : siteIds) {
			try {
				final Contentlet workingSite = this.getSiteAsContentlet(siteIdentifier, false);
				// There must always be a working version
				if (null == workingSite) {
					throw new DotBundleException(String.format("No working version of Site with ID '%s'" +
							" was found", siteIdentifier));
				}
				siteAsContentList.add(workingSite);
				try {
					final Contentlet liveSite = this.getSiteAsContentlet(siteIdentifier, true);
					if (liveSite != null) {
						siteAsContentList.add(liveSite);
					} else {
						// The bundling process can work with only the working version, unpublished
						Logger.warn(this, String.format("Unable to find live version of contentlet with identifier " +
								"'%s'. The process will continue", siteIdentifier));
					}
				} catch (final DotDataException | DotSecurityException | DotContentletStateException e) {
					// the process can work with only the working version, unpublished
					Logger.warn(this, String.format("Could not retrieve live version of Site with ID " +
							"'%s' (the process can continue): %s", siteIdentifier, ExceptionUtil.getErrorMessage(e)));
				}
			} catch (final DotDataException de) {
				throw new DotBundleException(String.format("Data error on Site with ID '%s'", siteIdentifier), de);
			} catch (final DotSecurityException ds) {
				throw new DotBundleException(String.format("Security error on Site with ID '%s'", siteIdentifier), ds);
			} catch (final DotContentletStateException dc) {
				throw new DotBundleException(String.format("Content error on Site with ID '%s'", siteIdentifier), dc);
			}
		}
		return siteAsContentList;
	}

	/**
	 * Returns the specified Site ID as a Contentlet object. The Elasticsearch API will be used as
	 * the first data source. If not present, the database will be used as a fallback.
	 *
	 * @param siteIdentifier The identifier of the Site to retrieve.
	 * @param live           If the live version of the Site is required, set this to {@code true}.
	 *
	 * @return The Site as a {@link Contentlet} object.
	 *
	 * @throws DotDataException     An error occurred when accessing the data source.
	 * @throws DotSecurityException A User permission error has occurred.
	 */
	private Contentlet getSiteAsContentlet(final String siteIdentifier, final boolean live) throws DotDataException, DotSecurityException {
		final List<Contentlet> results = this.conAPI
				.search("+identifier:" + siteIdentifier + (live ? " +live:true" : " +working:true"),
						1, 0, null, this.systemUser, DONT_RESPECT_FRONT_END_ROLES);
		if (UtilMethods.isSet(results)) {
			return results.get(0);
		}
		return this.conAPI.findContentletByIdentifier(siteIdentifier, live,
				this.langAPI.getDefaultLanguage().getId(), this.systemUser, DONT_RESPECT_FRONT_END_ROLES);
	}

	/**
	 * Updates the current {@link PublishAuditHistory} object with a new status indicating that the
	 * bundle is being built right now.
	 *
	 * @param currentAuditHistory The current {@link PublishAuditHistory} object.
	 *
	 * @throws DotPublisherException An error occurred when updating the bundle status.
	 */
	private void updateAuditHistory(PublishAuditHistory currentAuditHistory) throws DotPublisherException {
		if (currentAuditHistory != null && !this.config.isDownloading()) {
			// Updating audit table
			currentAuditHistory = pubAuditAPI.getPublishAuditStatus(this.config.getId()).getStatusPojo();
			currentAuditHistory.setBundleEnd(new Date());
			PushPublishLogger.log(this.getClass(), "Status Update: Bundling.");
			this.pubAuditAPI.updatePublishAuditStatus(this.config.getId(), PublishAuditStatus.Status.BUNDLING,
					currentAuditHistory);
		}
	}

	/**
	 * Takes the list of Sites that are being bundled and generates a list of Contentlets that are
	 * related to them. Such relationships may exist because of the following reasons:
	 * <ul>
	 *     <li>The Site has at least one field of type {@code Relationship}.</li>
	 *     <li>The Site has at least one field of type {@code File}</li>
	 * </ul>
	 *
	 * @param siteAsContentList The list of {@link Contentlet} objects representing a Site being
	 *                          bundled.
	 *
	 * @return A set of {@link Contentlet} objects that are related to the Sites being bundled,
	 * including the Sites themselves.
	 *
	 * @throws DotDataException     An error occurred when accessing the data source.
	 * @throws DotSecurityException Related data could not be retrieved.
	 */
	private Set<Contentlet> getRelatedFilesAndContent(final List<Contentlet> siteAsContentList) throws DotDataException,
			DotSecurityException {
		final Set<Contentlet> contentsToProcess = new HashSet<>();
		// Getting all contents related to every Site in the list
		for (final Contentlet siteAsContent : siteAsContentList) {
			final Map<Relationship, List<Contentlet>> contentRelationships =
					conAPI.findContentRelationships(siteAsContent, systemUser);
			contentRelationships.forEach((key, value) -> contentsToProcess.addAll(value));
			contentsToProcess.add(siteAsContent);
		}

		final Set<Contentlet> contentsToProcessWithFiles = new HashSet<>();
		//Getting all linked files
		for (final Contentlet contentlet : contentsToProcess) {
			final List<Field> contentTypeFields = FieldsCache.getFieldsByStructureInode(contentlet.getContentTypeId());
			for (final Field field : contentTypeFields) {
				if (field.getFieldType().equals(Field.FieldType.FILE.toString())) {
					final String identifier = (String) contentlet.get(field.getVelocityVarName());
					if (UtilMethods.isSet(identifier)) {
					    contentsToProcessWithFiles.addAll(
								this.conAPI.search("+identifier:" + identifier, 0, -1, null, this.systemUser, DONT_RESPECT_FRONT_END_ROLES));
			        }
				}
			}
			contentsToProcessWithFiles.add(contentlet);
		}
		return contentsToProcessWithFiles;
	}

	/**
	 * Writes the properties of a {@link Contentlet} (Host) object to the file
	 * system, so that it can be bundled and pushed to the destination server.
	 * 
	 * @param output
	 *            - The root location of the bundle in the file system.
	 * @param hostContentlet
	 *            - The host object to write.
	 * @throws IOException
	 *             An error occurred when writing the rule to the file system.
	 * @throws DotDataException
	 *             An error occurred reading information from the database.
	 * @throws DotSecurityException
	 *             The current user does not have the required permissions to
	 *             perform this action.
	 * @throws DotPublisherException
	 *             An error occurred when retrieving the content matrix.
	 */
	private void writeFileToDisk(final BundleOutput output, final Contentlet hostContentlet)
			throws IOException, DotDataException,
				DotSecurityException, DotPublisherException
	{
		if (Host.SYSTEM_HOST.equalsIgnoreCase(hostContentlet.getIdentifier())) {
			return;
		}

		Calendar cal = Calendar.getInstance();

		//Populate wrapper
		Logger.info(this, "Searching for ContentletVersionInfo. Identifier: " + hostContentlet.getIdentifier() +
				". Language:" + hostContentlet.getLanguageId());
		Optional<ContentletVersionInfo> info = APILocator.getVersionableAPI().getContentletVersionInfo(hostContentlet.getIdentifier(), hostContentlet.getLanguageId());
		final Host host = APILocator.getHostAPI().find(hostContentlet.getHost(), APILocator.getUserAPI().getSystemUser(), true);

        //We need to be careful because APILocator.getHostAPI().find() could return null.
        if (!UtilMethods.isSet(host) || !UtilMethods.isSet(host.getIdentifier())){
            final String contentHost = UtilMethods.isSet(hostContentlet.getHost()) ? hostContentlet.getHost() : "Unknown";
            Logger.warn(this, "Skipping file, can't find Host with id: " + contentHost);
            return;
        }

        if(info.isEmpty()) {
			Logger.warn(this, "Can't find ContentletVersionInfo. Identifier: "
					+ hostContentlet.getIdentifier() + ". Lang: " + hostContentlet.getLanguageId());
			return;
		}

		HostWrapper wrapper=new HostWrapper();
	    wrapper.setContent(hostContentlet);
		wrapper.setInfo(info.get());
		wrapper.setId(APILocator.getIdentifierAPI().find(hostContentlet.getIdentifier()));
		wrapper.setTags(APILocator.getTagAPI().getTagsByInode(hostContentlet.getInode()));
		wrapper.setOperation(config.getOperation());

		//Find MultiTree
		wrapper.setMultiTree(pubAPI.getContentMultiTreeMatrix(hostContentlet.getIdentifier()));

        //Find Tree
        List<Map<String, Object>> contentTreeMatrix = pubAPI.getContentTreeMatrix( hostContentlet.getIdentifier() );
        //Now add the categories, we will find categories by inode NOT by identifier
        contentTreeMatrix.addAll( pubAPI.getContentTreeMatrix( hostContentlet.getInode() ) );
        wrapper.setTree( contentTreeMatrix );

		//Copy asset files to bundle folder keeping original folders structure

		List<Field> fields=FieldsCache.getFieldsByStructureInode(hostContentlet.getStructureInode());
		String assetFolderPath = File.separator + "assets";

		String inode=hostContentlet.getInode();

		for(Field ff : fields) {
			if(ff.getFieldType().equals(Field.FieldType.BINARY.toString())) {
				File sourceFile = hostContentlet.getBinary( ff.getVelocityVarName());

				if(sourceFile != null && sourceFile.exists()) {

					String folderTree = inode.charAt(0)+File.separator+inode.charAt(1)+File.separator+
					        inode+File.separator+ff.getVelocityVarName()+File.separator+sourceFile.getName();

					String destFilePath = assetFolderPath + File.separator + folderTree;

					output.copyFile(sourceFile, destFilePath);
				}
		    }

		}

		String liveworking = hostContentlet.isLive() ? "live" :  "working";

		String uri = APILocator.getIdentifierAPI().find(hostContentlet).getURI().replace("/", File.separator);
		if(!uri.endsWith(HOST_EXTENSION)){
			uri.replace(HOST_EXTENSION, "");
			uri.trim();
			uri += HOST_EXTENSION;
		}
		String assetName = APILocator.getFileAssetAPI().isFileAsset(hostContentlet)?(File.separator + hostContentlet.getInode() + HOST_EXTENSION):uri;

		String myFileUrl = File.separator
				+liveworking + File.separator
				+ host.getHostname() + File.separator
				+ hostContentlet.getLanguageId() + assetName;


		try (final OutputStream outputStream = output.addFile(myFileUrl)) {

			BundlerUtil.objectToXML(wrapper, outputStream);
		}

		output.setLastModified(myFileUrl, cal.getTimeInMillis());

		Set<String> htmlIds = PublisherUtil.getPropertiesSet(wrapper.getMultiTree(), "parent1");
		Set<String> containerIds = PublisherUtil.getPropertiesSet(wrapper.getMultiTree(), "parent2");

		// adding content dependencies only if pushing content explicitly, not when it is a dependency of another asset
		if(!UtilMethods.isSet(config.getContentlets()) || config.getContentlets().isEmpty()) {
			addToConfig(hostContentlet.getFolder(), htmlIds, containerIds, hostContentlet.getStructureInode());
		}
		// Adding dependent Rules, if any
		List<Rule> ruleList = APILocator.getRulesAPI().getAllRulesByParent(hostContentlet, systemUser, false);
		Set<String> ruleIds = new HashSet<>();
		if (!ruleList.isEmpty()) {
			for (Rule rule : ruleList) {
				ruleIds.add(rule.getId());
			}
			this.config.getRules().addAll(ruleIds);
		}
		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Host bundled for pushing. Identifier: "+ hostContentlet.getIdentifier(), config.getId());
		}

	}

	@Override
	public FileFilter getFileFilter(){
		return new HostBundlerFilter();
	}

	/**
	 * A simple file filter that looks for Site data files inside a bundle.
	 * 
	 * @author Jorge Urdaneta
	 * @version 1.0
	 * @since Mar 7, 2013
	 *
	 */
	public static class HostBundlerFilter implements FileFilter {

		@Override
		public boolean accept(final File pathname) {
			return (pathname.isDirectory() || pathname.getName().endsWith(HOST_EXTENSION));
		}

	}

	/**
	 * 
	 * @param folder
	 * @param htmlPages
	 * @param containers
	 * @param structure
	 * @throws DotStateException
	 * @throws DotHibernateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private void addToConfig(String folder, Set<String> htmlPages, Set<String> containers, String structure)
			throws DotStateException, DotHibernateException, DotDataException, DotSecurityException
	{
		//Get Id from folder
		if(Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_ALL_FOLDER_PAGES", false)) {
			List<IHTMLPage> folderHtmlPages = APILocator.getHTMLPageAssetAPI().getLiveHTMLPages(APILocator.getFolderAPI().find(folder, systemUser, false), systemUser, false);
			folderHtmlPages.addAll(APILocator.getHTMLPageAssetAPI().getWorkingHTMLPages(APILocator.getFolderAPI().find(folder, systemUser, false), systemUser, false));
			for(IHTMLPage htmlPage: folderHtmlPages) {
				config.getHTMLPages().add(htmlPage.getIdentifier());
			}
		}

		config.getHTMLPages().addAll(htmlPages);

		config.getFolders().add(folder);

		config.getContainers().addAll(containers);

		if(Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_STRUCTURES", true) && (config.getOperation().equals(Operation.PUBLISH))) {
			config.getStructures().add(structure);
		}

	}

}
