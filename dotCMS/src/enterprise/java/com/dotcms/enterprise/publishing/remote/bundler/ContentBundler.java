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


import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.PushContentWorkflowWrapper;
import com.dotcms.publisher.pusher.wrapper.PushContentWrapper;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.PublisherFilter;
import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.model.Metadata;

import com.dotcms.publishing.*;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

import io.vavr.control.Try;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import java.util.function.Function;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * This bundler will take the list of {@link Contentlet} objects that are being
 * pushed and will write them in the file system in the form of an XML file.
 * This information will be part of the bundle that will be pushed to the
 * destination server. Please notice that a Contentlet encompasses a number of 
 * different entities, such as a Content Page, a Site, etc.
 * 
 * @author Jorge Urdaneta
 * @version 1.0
 * @since Mar 7, 2013
 *
 */
public class ContentBundler implements IBundler {

	private final StringBuilder LIVE_QUERY = new StringBuilder();
	private final StringBuilder WORKING_QUERY = new StringBuilder();
	private final StringBuilder ARCHIVED_QUERY = new StringBuilder();

	private PushPublisherConfig config;
	private User systemUser;
	private IdentifierAPI identifierAPI = null;
	private ContentletAPI conAPI = null;
	private CategoryAPI categoryAPI = null;
	private UserAPI uAPI = null;
	private PublisherAPI pubAPI = null;
	private PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();

	public final static String CONTENT_EXTENSION = ".content.xml" ;
	public final static String CONTENT_WORKFLOW_EXTENSION = ".contentworkflow.xml";
	public static final String CATEGORY_SEPARATOR = "sep_sep";

	@Override
	public String getName() {
		return "Content bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		config = (PushPublisherConfig) pc;
		this.identifierAPI = APILocator.getIdentifierAPI();
		conAPI = APILocator.getContentletAPI();
		categoryAPI = APILocator.getCategoryAPI();
		uAPI = APILocator.getUserAPI();
		pubAPI = PublisherAPI.getInstance();

		try {
			systemUser = uAPI.getSystemUser();
		} catch (DotDataException e) {
			Logger.fatal(ContentBundler.class,e.getMessage(),e);
		}
	}

    @Override
    public void setPublisher(IPublisher publisher) {
    }

	@Override
	public void generate(final BundleOutput output, final BundlerStatus status)
			throws DotBundleException {

	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
	        throw new RuntimeException("need an enterprise pro license to run this bundler");
	    }
		PublishAuditHistory currentStatusHistory = null;
		try {
            //Updating audit table
            if ( !config.isDownloading() ) {
                currentStatusHistory = pubAuditAPI.getPublishAuditStatus( config.getId() ).getStatusPojo();
                if ( currentStatusHistory == null ) {
                    currentStatusHistory = new PublishAuditHistory();
                }
                currentStatusHistory.setBundleStart( new Date() );
				PushPublishLogger.log(this.getClass(), "Status Update: Bundling.");
                pubAuditAPI.updatePublishAuditStatus( config.getId(), PublishAuditStatus.Status.BUNDLING, currentStatusHistory );
            }

            Set<String> contentsIds = config.getContentlets();
            
			if(UtilMethods.isSet(contentsIds) && !contentsIds.isEmpty()) { // this content set is a dependency of other assets, like htmlpages
				final DotSubmitter submitter = DotConcurrentFactory.getInstance().getSubmitter("ContentBundlerSubmitter_" + System.currentTimeMillis(),
						new DotConcurrentFactory.SubmitterConfigBuilder()
								.poolSize(Config.getIntProperty("MIN_NUMBER_THREAD_TO_EXECUTE_BUNDLER", 10))
								.maxPoolSize(Config.getIntProperty("MAX_NUMBER_THREAD_TO_EXECUTE_BUNDLER", 40))
								.queueCapacity(Config.getIntProperty("QUEUE_CAPACITY_TO_EXECUTE_BUNDLER", Integer.MAX_VALUE))
								.build()
				);

				final Collection<Future<Void>> tasks = new HashSet<>();
				try {
					int index = 0;

					for (final String contentIdentifier : contentsIds) {
						final Collection<Contentlet> contents = getContents(contentIdentifier);

						for (Contentlet contentlet : contents) {
							tasks.add(
									submitter.submit(
											new ContentBundlerCallable(output, contentlet, index, status)
									));
							index++;
						}
					}
				} finally {
					submitter.shutdown();
				}

				submitter.waitForAll(tasks);
			}

            if ( currentStatusHistory != null && !config.isDownloading() ) {
                //Updating audit table
                currentStatusHistory = pubAuditAPI.getPublishAuditStatus( config.getId() ).getStatusPojo();

                currentStatusHistory.setBundleEnd( new Date() );
				PushPublishLogger.log(this.getClass(), "Status Update: Bundling.");
                pubAuditAPI.updatePublishAuditStatus( config.getId(), PublishAuditStatus.Status.BUNDLING, currentStatusHistory );
            }

        } catch (Exception e) {

            try {
                if ( currentStatusHistory != null && !config.isDownloading() ) {
					PushPublishLogger.log(this.getClass(), "Status Update: Failed to bundle");
                    pubAuditAPI.updatePublishAuditStatus( config.getId(), PublishAuditStatus.Status.FAILED_TO_BUNDLE, currentStatusHistory );
                }
            } catch ( DotPublisherException e1 ) {
                Logger.warn( this.getClass(), "Unable to update Publish Audit Status for failed bundle: " + config.getId(), e1 );
            }

            status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}
	}

	private Collection<Contentlet> getContents(final String contentIdentifier)
			throws DotDataException, DotSecurityException {

		final ContentletAPI contentletAPI = APILocator.getContentletAPI();

		final List<ContentletVersionInfo> contentletVersionInfos = APILocator.getVersionableAPI()
				.findContentletVersionInfos(contentIdentifier);

		Map<String, Contentlet> contents = new HashMap<String, Contentlet>();

		for (ContentletVersionInfo contentletVersionInfo : contentletVersionInfos) {
				if (UtilMethods.isSet(contentletVersionInfo.getLiveInode())) {
					final Optional<Contentlet> optionalContentlet = contentletAPI.findInDb(
							contentletVersionInfo.getLiveInode());

					if (optionalContentlet.isPresent()) {
						contents.put(contentletVersionInfo.getLiveInode(), optionalContentlet.get());
					}
				}

			if (UtilMethods.isSet(contentletVersionInfo.getWorkingInode())) {
				final Optional<Contentlet> optionalContentlet = contentletAPI.findInDb(
						contentletVersionInfo.getWorkingInode());

				if (optionalContentlet.isPresent()) {
					contents.put(contentletVersionInfo.getWorkingInode(), optionalContentlet.get());
				}
			}
		}

		if(config.isSameIndexNotIncremental()){
			final String archivedQuery = getArchivedQuery(contentIdentifier);

			conAPI.search(archivedQuery, 0, -1, null, systemUser, false)
					.forEach(content -> contents.put(content.getInode(), content));
		}
		return contents.values();
	}

	private String getArchivedQuery(String contentIdentifier) {
		ARCHIVED_QUERY.setLength(0);
		ARCHIVED_QUERY.append("+identifier:");
		ARCHIVED_QUERY.append(contentIdentifier)
				.append(" +deleted:true");
		return ARCHIVED_QUERY.toString();
	}

	private String gtWorkingQuery(String contentIdentifier) {
		WORKING_QUERY.setLength(0);
		WORKING_QUERY.append("+identifier:");
		WORKING_QUERY.append(contentIdentifier)
				.append(" +working:true");
		return WORKING_QUERY.toString();
	}

	private String getLiveQuery(String contentIdentifier) {
		LIVE_QUERY.setLength(0);
		LIVE_QUERY.append("+identifier:");
		LIVE_QUERY.append(contentIdentifier)
				.append(" +live:true");
		return LIVE_QUERY.toString();
	}

	/**
	 * Writes the properties of a {@link Contentlet} object to the file system,
	 * so that it can be bundled and pushed to the destination server.
	 * 
	 * @param output
	 *            - The root location of the bundle in the file system.
	 * @param con
	 *            - The {@link Rule} object to write.
	 * @param countOrder
	 *            - The order in which a Contentlet must be processed.
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
	private void writeFileToDisk(final BundleOutput output, final Contentlet con, final int countOrder)
			throws IOException, DotDataException,
				DotSecurityException, DotPublisherException
	{

		Calendar cal = Calendar.getInstance();
		File pushContentFile = null;
		Host h = null;

		//Populate wrapper
		Optional<ContentletVersionInfo> info = APILocator.getVersionableAPI()
				.getContentletVersionInfo(con.getIdentifier(), con.getLanguageId());

		if(!info.isPresent()) {
			throw new DotDataException("Can't find ContentletVersionInfo. Identifier: "
					+ con.getIdentifier() + ". Lang: " + con.getLanguageId());
		}

		h = APILocator.getHostAPI().find(con.getHost(), APILocator.getUserAPI().getSystemUser(), true);

		PushContentWrapper wrapper=new PushContentWrapper();
	    wrapper.setContent(con);
		wrapper.setInfo(info.get());
		wrapper.setId(APILocator.getIdentifierAPI().find(con.getIdentifier()));
		wrapper.setTags(APILocator.getTagAPI().getTagsByInode(con.getInode()));
		wrapper.setOperation(config.getOperation());
		wrapper.setLanguage(APILocator.getLanguageAPI().getLanguage(con.getLanguageId()));


        //Find Tree
        List<Map<String, Object>> contentTreeMatrix = pubAPI.getContentTreeMatrix( con.getIdentifier() );
        wrapper.setTree( contentTreeMatrix );

        //Now add the categories, we will find categories by inode NOT by identifier
        List<Map<String, Object>> categoryTrees = pubAPI.getContentTreeMatrix( con.getInode() );
        Collections.sort(categoryTrees, (lhs, rhs) ->
        	Integer.parseInt(lhs.get("tree_order").toString()) - Integer.parseInt(rhs.get("tree_order").toString())
        );

        List<String> categories = new ArrayList<String>();
        for(Map<String, Object> categoryTree : categoryTrees) {
        	Category cat = categoryAPI.find((String) categoryTree.get("parent"), systemUser, true);

        	if (cat == null) continue;

    		List<Category> categoryPath = categoryAPI.getCategoryTreeUp(cat, systemUser, true);
    		categoryPath.remove(0);

    		final String[] categoryPathNames = categoryPath.stream().map( category -> category.getKey() ).toArray(String[]::new);
        	final String categoryQualifiedName = StringUtils.join(categoryPathNames, CATEGORY_SEPARATOR);

        	categories.add(categoryQualifiedName);
        }
        wrapper.setCategories(categories);

        //Verify if this contentlet is a HTMLPage
        if ( con.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE ) {

            List<Map<String, Object>> multiTreesList = new ArrayList<Map<String, Object>>();
            //Find the MultiTree records for this html page and add them to the wrapper
			final HTMLPageAsset htmlPageAsset = APILocator.getHTMLPageAssetAPI().fromContentlet(con);
			final Table<String, String, Set<PersonalizedContentlet>> pageContents =
					APILocator.getMultiTreeAPI().getPageMultiTrees(htmlPageAsset, false);

			for (final String containerId : pageContents.rowKeySet()) {
				for (final String uniqueId : pageContents.row(containerId).keySet()) {

					final Collection<PersonalizedContentlet> personalizedContentletSet = pageContents.get(containerId, uniqueId);

					for (final PersonalizedContentlet personalizedContentlet : personalizedContentletSet) {
						Map<String, Object> multiTreeMap = new HashMap<String, Object>();
						multiTreeMap.put("parent1", con.getIdentifier());
						multiTreeMap.put("parent2", containerId);
						multiTreeMap.put("child", personalizedContentlet.getContentletId());
						multiTreeMap.put("relation_type", uniqueId);
						multiTreeMap.put("tree_order", personalizedContentlet.getTreeOrder());
						multiTreeMap.put("personalization", personalizedContentlet.getPersonalization());
						multiTreesList.add( multiTreeMap );
					}
				}
			}

            wrapper.setMultiTree( multiTreesList );
        }

		//Copy asset files to bundle folder keeping original folders structure
		List<Field> fields=FieldsCache.getFieldsByStructureInode(con.getStructureInode());
		final String assetFolderPath = File.separator + "assets";
		String inode=con.getInode();
		Map<String, List<Tag>> contentTags = new HashMap<>();

		for(Field ff : fields) {
			if(ff.getFieldType().equals(Field.FieldType.BINARY.toString())) {
				File sourceFile = con.getBinary( ff.getVelocityVarName());

				if(sourceFile != null && sourceFile.exists()) {

					String folderTree = inode.charAt(0)+File.separator+inode.charAt(1)+File.separator+
					        inode+File.separator+ff.getVelocityVarName()+File.separator+sourceFile.getName();

					String destFile = assetFolderPath + File.separator + folderTree;
					output.copyFile(sourceFile, destFile);

				}
			} else if ( ff.getFieldType().equals(Field.FieldType.TAG.toString()) ) {
				List<Tag> tagsByFieldVarName = APILocator.getTagAPI().getTagsByInodeAndFieldVarName(con.getInode(), ff.getVelocityVarName());
				contentTags.put(ff.getVelocityVarName(), tagsByFieldVarName);
			}

		}

		wrapper.setContentTags(contentTags);

		final FileMetadataAPI fileMetadataAPI = APILocator.getFileMetadataAPI();
		final Map<String,Metadata> binariesMetadata =
		con.getContentType().fields(BinaryField.class).stream()
				.filter(field -> con.get(field.variable()) != null)
				.map(field -> Try.of(() -> fileMetadataAPI.getFullMetadataNoCache(con,field.variable())).getOrNull()).filter(Objects::nonNull)
				.collect(Collectors.toMap(Metadata::getFieldName, Function.identity()));

		wrapper.setBinariesMetadata(binariesMetadata);

		String liveworking = con.isLive() ? "live" :  "working";

		String uri = APILocator.getIdentifierAPI().find(con).getURI().replace("/", File.separator);
		if(!uri.endsWith(CONTENT_EXTENSION)){
			uri.replace(CONTENT_EXTENSION, "");
			uri.trim();
			uri += CONTENT_EXTENSION;
		}
		uri = uri.replace(uri.substring(uri.lastIndexOf(File.separator)+1, uri.length()), countOrder +"-"+ uri.substring(uri.lastIndexOf(File.separator)+1, uri.length()));

		String assetName = APILocator.getFileAssetAPI().isFileAsset(con)?(File.separator + countOrder +"-" +con.getInode() + CONTENT_EXTENSION):uri;

		String myFileUrl = File.separator
				+liveworking + File.separator
				+ h.getHostname() + File.separator +
				+ con.getLanguageId() + assetName;

		try(final OutputStream outputStream = output.addFile(myFileUrl)) {

			BundlerUtil.objectToXML(wrapper, outputStream);
		}

		output.setLastModified(myFileUrl, cal.getTimeInMillis());

		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Content bundled for pushing. Operation: "+config.getOperation()+", Identifier: "+ con.getIdentifier(), config.getId());
		}

		WorkflowStep step = APILocator.getWorkflowAPI().findStepByContentlet(con);
		WorkflowScheme scheme = null;
		if( null != step) {
			scheme = APILocator.getWorkflowAPI().findScheme(step.getSchemeId());
		}

		if(config.getOperation().equals(Operation.PUBLISH) && null != scheme) {
		    // send step information if we're publishing and the structure has a scheme other than the default
            WorkflowTask task = APILocator.getWorkflowAPI().findTaskByContentlet( con );
            if ( task != null &&  null != task.getId()) {

                Role assignedToRole = APILocator.getRoleAPI().loadRoleById( task.getAssignedTo() );
                if ( (assignedToRole != null && UtilMethods.isSet( assignedToRole.getRoleKey() ))
                        && assignedToRole.getRoleKey().equalsIgnoreCase( "system" ) ) {

                    // user role for system user might be different.
                    HibernateUtil.evict( task );
                    WorkflowTask orig = task;
                    task = new WorkflowTask();
                    try {
                        BeanUtils.copyProperties( task, orig );
                    } catch ( Exception e ) {
                        Logger.error( this, "can't copy properties from original task to a new with system user role mark", e );
                    }
                    task.setAssignedTo( "__SYSTEM_USER_ROLE__" );
                }

                PushContentWorkflowWrapper w=new PushContentWorkflowWrapper();
    		    w.setTask(task);

				//w.setHistory(APILocator.getWorkflowAPI().findWorkflowHistory(task));
				w.setHistory(new ImmutableList.Builder<WorkflowHistory>().build());
				//w.setComments(APILocator.getWorkflowAPI().findWorkFlowComments(task));
				w.setComments(new ImmutableList.Builder<WorkflowComment>().build());

    		    final String stepFilePath = File.separator + liveworking + File.separator + h.getHostname() + File.separator +
    		            con.getLanguageId() + File.separator + con.getIdentifier() + CONTENT_WORKFLOW_EXTENSION;

				try (final OutputStream outputStream = output.addFile(stepFilePath)) {
					BundlerUtil.objectToXML(w, outputStream);
				}

    		    output.setLastModified(stepFilePath, cal.getTimeInMillis());
		    }
		}
		// If content is a Content Page, add dependent Rules, if any
		if (con.isHTMLPage()) {
            final PublisherFilter publisherFilter = APILocator.getPublisherAPI().createPublisherFilter(config.getId());
            if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.RULE.getType())) {
                this.config.getRules().addAll(
                        APILocator.getRulesAPI()
                                .getAllRulesByParent(con, systemUser, false)
                                .stream()
                                .map(Rule::getId)
                                .collect(Collectors.toSet()));
            }
		}
	}

	@Override
	public FileFilter getFileFilter(){
		return new ContentBundlerFilter();
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
	public class ContentBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(CONTENT_EXTENSION));
		}

	}

	private class ContentBundlerCallable implements Callable<Void> {
		private BundleOutput output;
		private Contentlet contentlet;
		private int index;
		private BundlerStatus status;

		public ContentBundlerCallable(
				final BundleOutput output,
				final Contentlet contentlet,
				final int index,
				final BundlerStatus status) {

			this.output = output;
			this.contentlet = contentlet;
			this.index = index;
			this.status = status;
		}

		@Override
		public Void call() throws Exception {
			if (contentlet.isHTMLPage()) {
				// Temporarily add the URI to the contentlet properties
				// The URI is now stored ONLY in the page Identifier
				String pageUri = APILocator.getIdentifierAPI().find(
						contentlet.getIdentifier()).getAssetName();
				contentlet.getMap().put(HTMLPageAssetAPI.URL_FIELD, pageUri);
			}
			writeFileToDisk(output, contentlet, index);
			status.addCountThreadSafe();
			return null;
		}
	}
}
