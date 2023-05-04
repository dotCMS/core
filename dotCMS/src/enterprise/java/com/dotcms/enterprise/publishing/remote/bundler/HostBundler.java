/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.publishing.remote.bundler;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.*;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publisher.pusher.wrapper.HostWrapper;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

/**
 * This bundler will take the list of {@link Contentlet} (Host) objects that are
 * being pushed and will write them in the file system in the form of an XML
 * file. This information will be part of the bundle that will be pushed to the
 * destination server.
 * 
 * @author Jorge Urdaneta
 * @version 1.0
 * @since Mar 7, 2013
 *
 */
public class HostBundler implements IBundler {

	private PushPublisherConfig config;
	private User systemUser;
	private ContentletAPI conAPI = null;
	private UserAPI uAPI = null;
	private PublisherAPI pubAPI = null;
	private PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();

	public final static String HOST_EXTENSION = ".host.xml" ;

	@Override
	public String getName() {
		return "Host bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		config = (PushPublisherConfig) pc;
		conAPI = APILocator.getContentletAPI();
		uAPI = APILocator.getUserAPI();
		pubAPI = PublisherAPI.getInstance();

		try {
			systemUser = uAPI.getSystemUser();
		} catch (DotDataException e) {
			Logger.fatal(HostBundler.class,e.getMessage(),e);
		}
	}

    @Override
    public void setPublisher(IPublisher publisher) {
    }

	@Override
	public void generate(BundleOutput output, BundlerStatus status)
			throws DotBundleException {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
	        throw new RuntimeException("need an enterprise pro license to run this bundler");
	    }
		try {
			Set<String> contents = config.getHostSet();

			//Updating audit table
			PublishAuditHistory currentStatusHistory = null;
			if (!config.isDownloading()) {
				currentStatusHistory = this.pubAuditAPI.getPublishAuditStatus(this.config.getId()).getStatusPojo();
				if (currentStatusHistory == null) {
					currentStatusHistory = new PublishAuditHistory();
				}
				currentStatusHistory.setBundleStart(new Date());
				PushPublishLogger.log(this.getClass(), "Status Update: Bundling.");
				this.pubAuditAPI.updatePublishAuditStatus(this.config.getId(), PublishAuditStatus.Status.BUNDLING,
						currentStatusHistory);
			}
            
			if(UtilMethods.isSet(contents) && !contents.isEmpty()) { // this content set is a dependency of other assets, like htmlpages
				List<Contentlet> contentList = new ArrayList<>();
				for (String contentIdentifier : contents) {
					try{
						Contentlet workingContentlet;
						List<Contentlet> results = APILocator.getContentletAPI()
								.search("+identifier:" + contentIdentifier + " +working:true",
									1, 0, null, systemUser, false);
						if (UtilMethods.isSet(results)) {
							workingContentlet = results.get(0);
						} else {
							workingContentlet = APILocator.getContentletAPI()
									.findContentletByIdentifier(contentIdentifier, false,
									APILocator.getLanguageAPI().getDefaultLanguage().getId(), systemUser, false);
						}

						Contentlet liveContentlet = null;
						try {
							List<Contentlet> returnedContent = APILocator.getContentletAPI()
									.search("+identifier:" + contentIdentifier + " +live:true",
										1, 0, null, systemUser, false);

							if(UtilMethods.isSet(returnedContent)) {
								liveContentlet = returnedContent.get(0);
							} else {
								liveContentlet = APILocator.getContentletAPI()
										.findContentletByIdentifier(contentIdentifier, true,
										APILocator.getLanguageAPI().getDefaultLanguage().getId(), systemUser, false);
							}

							if (liveContentlet == null) {
								Logger.info(HostBundler.class, "Unable to find live version of contentlet with"
										+ " identifier '"+ contentIdentifier);
							}
						} catch (DotDataException | DotSecurityException | DotContentletStateException e) {
							// the process can work with only the working version, unpublished
							Logger.info(HostBundler.class, "Error retrieving live contentlet with identifier '"
								+ contentIdentifier +"' ("+ e.getMessage() +")");
						}

						// there should always be a working version
						if(workingContentlet != null)
							contentList.add(workingContentlet);
						else
							throw new DotBundleException("No working version of host " + contentIdentifier);
						// the process can work with only the working version, unpublished
						if(liveContentlet != null)
							contentList.add(liveContentlet);

					}catch(DotDataException de){
						throw new DotBundleException("Data error on host content " + contentIdentifier, de);
					}catch(DotSecurityException ds){
						throw new DotBundleException("Security error on host content " + contentIdentifier, ds);
					}catch(DotContentletStateException dc){
						throw new DotBundleException("Content error on host " + contentIdentifier, dc);
					}
				}
				Set<Contentlet> contentsToProcessWithFiles = getRelatedFilesAndContent(contentList);

				for (Contentlet con : contentsToProcessWithFiles) {
					writeFileToDisk(output, con);
					status.addCount();
				}
			}
			if (currentStatusHistory != null && !this.config.isDownloading()) {
				// Updating audit table
				currentStatusHistory = pubAuditAPI.getPublishAuditStatus(this.config.getId()).getStatusPojo();
				currentStatusHistory.setBundleEnd(new Date());
				PushPublishLogger.log(this.getClass(), "Status Update: Bundling.");
				this.pubAuditAPI.updatePublishAuditStatus(this.config.getId(), PublishAuditStatus.Status.BUNDLING,
						currentStatusHistory);
			}
		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}
	}

	/**
	 * 
	 * @param cs
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private Set<Contentlet> getRelatedFilesAndContent(List<Contentlet> cs) throws DotDataException,
			DotSecurityException {

		Set<Contentlet> contentsToProcess = new HashSet<>();

		//Getting all related content
		for (Contentlet con : cs) {
			Map<Relationship, List<Contentlet>> contentRel =
					conAPI.findContentRelationships(con, systemUser);

			for (Relationship rel : contentRel.keySet()) {
				contentsToProcess.addAll(contentRel.get(rel));
			}

			contentsToProcess.add(con);
		}

		Set<Contentlet> contentsToProcessWithFiles = new HashSet<>();
		//Getting all linked files
		for(Contentlet con: contentsToProcess) {
			List<Field> fields=FieldsCache.getFieldsByStructureInode(con.getStructureInode());
			for(Field ff : fields) {
				if(ff.getFieldType().toString().equals(Field.FieldType.FILE.toString())) {
					String identifier = (String) con.get(ff.getVelocityVarName());
                                        if(UtilMethods.isSet(identifier)) {
					    contentsToProcessWithFiles.addAll(conAPI.search("+identifier:"+identifier, 0, -1, null, systemUser, false));
			                }
                                }
			}
			contentsToProcessWithFiles.add(con);
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
	 * A simple file filter that looks for contentlet data files inside a
	 * bundle.
	 * 
	 * @author Jorge Urdaneta
	 * @version 1.0
	 * @since Mar 7, 2013
	 *
	 */
	public class HostBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

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
