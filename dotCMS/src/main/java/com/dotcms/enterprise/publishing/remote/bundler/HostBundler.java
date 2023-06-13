package com.dotcms.enterprise.publishing.remote.bundler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publisher.pusher.wrapper.HostWrapper;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
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
	public void generate(File bundleRoot, BundlerStatus status)
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
				List<Contentlet> contentList = new ArrayList<Contentlet>();
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
					writeFileToDisk(bundleRoot, con);
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

		Set<Contentlet> contentsToProcess = new HashSet<Contentlet>();

		//Getting all related content
		for (Contentlet con : cs) {
			Map<Relationship, List<Contentlet>> contentRel =
					conAPI.findContentRelationships(con, systemUser);

			for (Relationship rel : contentRel.keySet()) {
				contentsToProcess.addAll(contentRel.get(rel));
			}

			contentsToProcess.add(con);
		}

		Set<Contentlet> contentsToProcessWithFiles = new HashSet<Contentlet>();
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
	 * @param bundleRoot
	 *            - The root location of the bundle in the file system.
	 * @param host
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
	private void writeFileToDisk(File bundleRoot, Contentlet host)
			throws IOException, DotDataException,
				DotSecurityException, DotPublisherException
	{

		Calendar cal = Calendar.getInstance();
		File pushContentFile = null;
		Host h = null;

		//Populate wrapper
		ContentletVersionInfo info = APILocator.getVersionableAPI().getContentletVersionInfo(host.getIdentifier(), host.getLanguageId());
		h = APILocator.getHostAPI().find(host.getHost(), APILocator.getUserAPI().getSystemUser(), true);

        //We need to be careful because APILocator.getHostAPI().find() could return null.
        if (!UtilMethods.isSet(h) || !UtilMethods.isSet(h.getIdentifier())){
            final String contentHost = UtilMethods.isSet(host.getHost()) ? host.getHost() : "Unknown";
            Logger.warn(this, "Skipping file, can't find Host with id: " + contentHost);
            return;
        }

		HostWrapper wrapper=new HostWrapper();
	    wrapper.setContent(host);
		wrapper.setInfo(info);
		wrapper.setId(APILocator.getIdentifierAPI().find(host.getIdentifier()));
		wrapper.setTags(APILocator.getTagAPI().getTagsByInode(host.getInode()));
		wrapper.setOperation(config.getOperation());

		//Find MultiTree
		wrapper.setMultiTree(pubAPI.getContentMultiTreeMatrix(host.getIdentifier()));

        //Find Tree
        List<Map<String, Object>> contentTreeMatrix = pubAPI.getContentTreeMatrix( host.getIdentifier() );
        //Now add the categories, we will find categories by inode NOT by identifier
        contentTreeMatrix.addAll( pubAPI.getContentTreeMatrix( host.getInode() ) );
        wrapper.setTree( contentTreeMatrix );

		//Copy asset files to bundle folder keeping original folders structure
		List<Field> fields=FieldsCache.getFieldsByStructureInode(host.getStructureInode());
		File assetFolder = new File(bundleRoot.getPath()+File.separator+"assets");
		String inode=host.getInode();
		for(Field ff : fields) {
			if(ff.getFieldType().toString().equals(Field.FieldType.BINARY.toString())) {
				File sourceFile = host.getBinary( ff.getVelocityVarName());

				if(sourceFile != null && sourceFile.exists()) {
					if(!assetFolder.exists())
						assetFolder.mkdir();

					String folderTree = inode.charAt(0)+File.separator+inode.charAt(1)+File.separator+
					        inode+File.separator+ff.getVelocityVarName()+File.separator+sourceFile.getName();

					File destFile = new File(assetFolder, folderTree);
		            destFile.getParentFile().mkdirs();
		            FileUtil.copyFile(sourceFile, destFile);
				}
		    }

		}

		String liveworking = host.isLive() ? "live" :  "working";

		String uri = APILocator.getIdentifierAPI().find(host).getURI().replace("/", File.separator);
		if(!uri.endsWith(HOST_EXTENSION)){
			uri.replace(HOST_EXTENSION, "");
			uri.trim();
			uri += HOST_EXTENSION;
		}
		String assetName = APILocator.getFileAssetAPI().isFileAsset(host)?(File.separator + host.getInode() + HOST_EXTENSION):uri;

		String myFileUrl = bundleRoot.getPath() + File.separator
				+liveworking + File.separator
				+ h.getHostname() + File.separator
				+ host.getLanguageId() + assetName;

		pushContentFile = new File(myFileUrl);
		pushContentFile.mkdirs();

		BundlerUtil.objectToXML(wrapper, pushContentFile, true);
		pushContentFile.setLastModified(cal.getTimeInMillis());

		Set<String> htmlIds = PublisherUtil.getPropertiesSet(wrapper.getMultiTree(), "parent1");
		Set<String> containerIds = PublisherUtil.getPropertiesSet(wrapper.getMultiTree(), "parent2");

		// adding content dependencies only if pushing content explicitly, not when it is a dependency of another asset
		if(!UtilMethods.isSet(config.getContentlets()) || config.getContentlets().isEmpty()) {
			addToConfig(host.getFolder(), htmlIds, containerIds, host.getStructureInode());
		}
		// Adding dependent Rules, if any
		List<Rule> ruleList = APILocator.getRulesAPI().getAllRulesByParent(host, systemUser, false);
		Set<String> ruleIds = new HashSet<>();
		if (!ruleList.isEmpty()) {
			for (Rule rule : ruleList) {
				ruleIds.add(rule.getId());
			}
			this.config.getRules().addAll(ruleIds);
		}
		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Host bundled for pushing. Identifier: "+ host.getIdentifier(), config.getId());
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
