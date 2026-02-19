/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.handler;

import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME;
import static com.dotcms.contenttype.model.type.PageContentType.PAGE_FRIENDLY_NAME_FIELD_VAR;
import static com.dotcms.publishing.FilterDescriptor.RELATIONSHIPS_KEY;
import static com.liferay.util.StringPool.BLANK;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.elasticsearch.util.ESUtils;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.ContentBundler;
import com.dotcms.enterprise.publishing.remote.bundler.HostBundler;
import com.dotcms.enterprise.publishing.remote.handler.HandlerUtil.HandlerType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.ContentWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.rendering.velocity.services.PageLoader;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.model.Metadata;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.MultiTreeCache;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.common.reindex.ReindexQueueFactory.Priority;
import com.dotmarketing.db.FlushCacheRunnable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAssetValidationException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import io.vavr.Lazy;
import io.vavr.Tuple3;
import io.vavr.control.Try;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

/**
 * This handler deals with Contentlet-related information inside a bundle and
 * saves it in the destination server. This class will read only the
 * {@link Contentlet} data files.
 * <p>
 * As different types of objects are Contentlets, such as Sites, Files, Content
 * Pages, etc., this handler must be particularly careful with the information
 * it processes.
 * 
 * @author Jorge Urdaneta
 * @version 1.0
 * @since Mar 7, 2013
 *
 */
public class ContentHandler implements IHandler {

    private final ContentletAPI  contentletAPI  = APILocator.getContentletAPI();
	private final IdentifierAPI  identifierAPI  = APILocator.getIdentifierAPI();
	private final FolderAPI      folderAPI 	    = APILocator.getFolderAPI();
	private final CategoryAPI    categoryAPI    = APILocator.getCategoryAPI();
	private final PermissionAPI  permissionAPI  = APILocator.getPermissionAPI();
	private final HostAPI 		 hostAPI        = APILocator.getHostAPI();
	private final UserAPI 		 userAPI 	    = APILocator.getUserAPI();
	private final TagAPI 		 tagAPI 	    = APILocator.getTagAPI();
	private final VersionableAPI versionableAPI = APILocator.getVersionableAPI();
	private final FileMetadataAPI fileMetadataAPI = APILocator.getFileMetadataAPI();
	private final Lazy<MultiTreeAPI> multiTreeAPI = Lazy.of(APILocator::getMultiTreeAPI);
	private final Lazy<BundleAPI> bundleAPI = Lazy.of(APILocator::getBundleAPI);

    // tuple3(identifier, lang, shouldReindex)
    private final HashSet<Tuple3<String, Long, Boolean>> contentToRefresh = new HashSet<>();
	private final ExistingContentMapping existingContentMap = new ExistingContentMapping();

	private static final boolean RESPECT_FRONTEND_ROLES = true;

	private final PublisherConfig config;

	/**
	 * Default class constructor. Initializes the handler with the configuration
	 * of the Publisher selected to bundle the data.
	 * 
	 * @param config
	 *            - The {@link PublisherConfig} object that has the main
	 *            configuration values for the bundle that is being published.
	 */
	public ContentHandler(final PublisherConfig config) {
		this.config = config;
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public void handle(final File bundleFolder) throws Exception {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
			throw new RuntimeException("need an enterprise pro license to run this");
		}
		handle(bundleFolder, false);
	}

	/**
	 * Reads the information of the contentlets contained in the bundle and
	 * saves them in the destination server.
	 * 
	 * @param bundleFolder
	 *            - The location of the bundle in the file system.
	 * @param isHost
	 *            - If <code>true</code> this handler will read and process the
	 *            information belonging to a Site. Otherwise, <code>false</code>
	 *            .
	 * @throws Exception
	 *             An error occurred when saving the new content.
	 */
	public void handle(final File bundleFolder, final Boolean isHost) throws Exception {

        List<File> contents = isHost ? FileUtil.listFilesRecursively(bundleFolder, new HostBundler().getFileFilter()) :
				FileUtil.listFilesRecursively(bundleFolder, new ContentBundler().getFileFilter());
		Collections.sort(contents);
		contents = contents.stream().filter(File::isFile).collect(Collectors.toList());
		handleContents(contents, bundleFolder, isHost);
		HandlerUtil.setExistingContent(config.getId(), existingContentMap);

		String identToRemove = null;
		Contentlet contentlet = null;
		try{
            // tuple3(identifier, lang, shouldReindex)
            for (final Tuple3<String, Long, Boolean> cvinfo : contentToRefresh) {

                APILocator.getVersionableAPI()
                        .removeContentletVersionInfoFromCache(cvinfo._1, cvinfo._2);
                contentlet = contentletAPI.findContentletByIdentifier(cvinfo._1, false, cvinfo._2,
                        APILocator.getUserAPI().getSystemUser(), true);
				invalidateRelationshipsCache(contentlet);

                if (cvinfo._3) {
                    // reindex background value, after new content but before any full reindexing
                    APILocator.getReindexQueueAPI().addIdentifierReindex(cvinfo._1, Priority.STRUCTURE.dbValue());
                } else {

                    Logger.info(this,
                            "Skipping reindex on related child content {title:" + (contentlet != null
                                    ? contentlet.getTitle() : "ukn content")
                                    + "},{id:"
                                    + cvinfo._1 + "}");
                }
            }


        }catch (Exception e) {
			HandlerUtil.cleanupExistingContentByBundleId(config.getId());
			throw new DotPublishingException("Unable to update Cache or Reindex Content: identToRemove=[" +
					identToRemove + "], contentId=[" + (null != contentlet ? contentlet.getIdentifier() : "null id") +
					"], contentInode=[" + (null != contentlet ? contentlet.getInode() : "null inode") + "], lang=[" +
                    (null != contentlet ? contentlet.getLanguageId() : "null lang") + "]:" + e.getMessage(), e);
		}
	}

	private boolean ignoreContent(Contentlet contentlet){

		// if a host does not exist on target, skip content
		Host localHost = Try.of(()->APILocator.getHostAPI().find(contentlet.getHost(), APILocator.systemUser(), false)).getOrNull();
		return UtilMethods.isEmpty(localHost::getIdentifier);

	}

	/**
	 * Reads the information of the contentlets contained in the bundle and
	 * saves them in the destination server.
	 * 
	 * @param contentsIn
	 *            The list of data files containing the contentlet information.
	 * @param folderOut
	 *            - The location of the bundle in the file system.
	 * @param isHost
	 *            - If <code>true</code> this handler will read and process the
	 *            information belonging to a Site. Otherwise, <code>false</code>
	 * @throws DotPublishingException
	 *             An error occurred when saving the new content.
	 * @throws DotDataException
	 *             An error occurred when interacting with the database.
	 */
	private void handleContents(final Collection<File> contentsIn, final File folderOut, final Boolean isHost) throws DotPublishingException, DotDataException{
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
			throw new RuntimeException("need an enterprise pro license to run this");
		}
	    final User systemUser = userAPI.getSystemUser();
        File workingOn=null;
        Contentlet content;
		ContentWrapper wrapper = null;
		final Collection<File> contents = contentsIn.stream().filter(File::isFile).collect(Collectors.toList());
		final Collection<String> alreadyDeleted = new HashSet<>();

    	try{
	        final XStream xstream = XStreamHandler.newXStreamInstance();
			final Set<Pair<String,Long>> pushedIdsToIgnore = new HashSet<>();
            for (final File contentFile : contents) {
                workingOn=contentFile;

                try(final InputStream input = Files.newInputStream(contentFile.toPath())){
                    wrapper = (ContentWrapper) xstream.fromXML(input);
                }
                //This is to check if the contentType exists in the receiver, to improve logs
				//If the ContentType does not exist, this method will throw a NotFoundInDBException
				APILocator.getContentTypeAPI(systemUser).find(wrapper.getContent().getContentTypeId());

				if (Host.SYSTEM_HOST.equalsIgnoreCase(wrapper.getContent().getIdentifier())) {
					return;
				}

				content = new Contentlet(wrapper.getContent());

				addRelatedContentsToInfoToRemove(content, wrapper.getInfo());

				if (content.getContentType().baseType().equals(BaseContentType.HTMLPAGE)) {
					checkContentPageConflicts(content);
				}
                content.setProperty(Contentlet.DONT_VALIDATE_ME, true);
                content.setProperty(Contentlet.DISABLE_WORKFLOW, true);
                if ( content.getModDate() != null ) {
                    //We want to respect the modification date for the content we received.
                    content.setProperty( "_use_mod_date", content.getModDate() );
                }
                content.setProperty(Contentlet.WORKFLOW_ASSIGN_KEY, null);
                content.setProperty(Contentlet.WORKFLOW_ACTION_KEY, null);
                content.setProperty(Contentlet.WORKFLOW_COMMENTS_KEY, null);

                //TODO: Remove this condition for future releases.
                // It has been done to keep backward compatibility -> Issue: https://github.com/dotCMS/core/issues/17239
                if (content.getMap().containsKey(PAGE_FRIENDLY_NAME_FIELD_VAR.toLowerCase())) {
                    //Saving the value with the proper key
                    content.setProperty(PAGE_FRIENDLY_NAME_FIELD_VAR,
                            content.get(PAGE_FRIENDLY_NAME_FIELD_VAR.toLowerCase()));
                    content.getMap().remove(PAGE_FRIENDLY_NAME_FIELD_VAR.toLowerCase());
                }

				// if a Site does not exist on target, skip content
				if(ignoreContent(content)){
                    Logger.warn(this.getClass(), "Ignoring contentlet: " + content.getIdentifier() + " | " + Try.of(
                            content::getTitle).getOrElse("unknown")  + " . Unable to find referenced Site: " + content.getHost());
					continue;
				}

				content.setVariantId(wrapper.getContent().getVariantId());

                // get the local language and assign it to the version info, and content, since the id's might be different
                final Language remoteLang = wrapper.getLanguage();
				final Pair<Long,Long> remoteLocalLanguages = this.existingContentMap.getRemoteLocalLanguages(wrapper);
				if(UtilMethods.isSet(remoteLang) && remoteLang.getId() > 0) {
					// This should take care of solving any existing conflicts. Previously solved by the Language Handler.
					final Language mappedRemoteLanguage = config.getMappedRemoteLanguage(remoteLang.getId());
					if (UtilMethods.isSet(mappedRemoteLanguage) && mappedRemoteLanguage.getId() > 0) {
						// We trust this mapped value is always accurate. LanguageHandler took the time to map it for us.
						content.setLanguageId(mappedRemoteLanguage.getId());
					} else {
						//fall-back
						final Language localLang = APILocator.getLanguageAPI().getLanguage(remoteLang.getLanguageCode(), remoteLang.getCountryCode());
						if(localLang != null && localLang.getId() > 0){
						   content.setLanguageId(localLang.getId());
						}
					}
				}

				content.setIdentifier(this.findLocalIdentifier(content));




                try{
					final boolean isPushedContentArchived = wrapper.getInfo().isDeleted();
					final String contentId = content.getIdentifier();
					final String contentInode = content.getInode();
					final long languageId = content.getLanguageId();
					if(wrapper.getOperation().equals(PushPublisherConfig.Operation.PUBLISH) && !isPushedContentArchived) {
						// This operation (PUBLISH) publishes/un-publishes a Content that is NOT archived. The un-publishing
						// is taken care of at the end of this handleContents() method
						saveContent(content, folderOut, systemUser, wrapper, isHost,remoteLocalLanguages);
						Logger.debug(this, () -> "Content saved: " + contentId
								+ " , inode: " + contentInode + " , language: " + languageId);
					} else if (wrapper.getOperation().equals(PushPublisherConfig.Operation.PUBLISH) && isPushedContentArchived) {
						// This operation (PUBLISH) flags the pushed Content as Archived, WITHOUT deleting it
						boolean localContentAlreadyExists = APILocator.getContentletAPI().isContentlet(content.getInode());
						if (!localContentAlreadyExists) {
							// If the content doesn't exist in the local instance yet, we need to save it first
							saveContent(content, folderOut, systemUser, wrapper, isHost, remoteLocalLanguages);
							Logger.debug(this, () -> "Content saved before archive: " + contentId
									+ " , inode: " + contentInode + " , language: " + languageId);
						}
						archiveOrDeleteContent(content, systemUser, isHost, remoteLocalLanguages, true);
						Logger.debug(this, () -> "Content archived: " + contentId
								+ " , inode: " + contentInode + " , language: " + languageId);
						// Store the IDs that will be archived in the receiver
						final Pair<String,Long> idAndLanguageKey = Pair.of(content.getIdentifier(),
								remoteLocalLanguages.getRight());
						pushedIdsToIgnore.add(idAndLanguageKey);
					} else {
						// Finally, this operation (UNPUBLISH) deletes a Content altogether

						if (!alreadyDeleted.contains(contentId)) {
							archiveOrDeleteContent(content, systemUser, isHost, remoteLocalLanguages, false);
						}

						alreadyDeleted.add(contentId);
						Logger.info(this, () -> "Content deleted: " + contentId
								+ " , inode: " + contentInode + " , language: " + languageId);
					}
				} catch (final FileAssetValidationException e1){
                    Logger.error(ContentHandler.class, "Content id ["+content.getIdentifier()+"] could not be processed because of missing binary file. Error: "+ ExceptionUtil.getErrorMessage(e1), e1);
				}
            }
			workingOn = null;
			for (final File contentFile : contents) {
				if (contentFile.isDirectory()) {
				    continue;
                }
				workingOn = contentFile;
				try (final InputStream input = Files.newInputStream(contentFile.toPath())){
					wrapper = (ContentWrapper)xstream.fromXML(input);
				}
				content = null;

                if(wrapper.getOperation().equals(PushPublisherConfig.Operation.PUBLISH)) {
	                ContentletVersionInfo info = wrapper.getInfo();
	                content = wrapper.getContent();
	                boolean updateExisting = Boolean.FALSE;

					// get the local language and assign it to the version info, and the content, since the id's might be different
					final Language remoteLang = wrapper.getLanguage();
					final Pair<Long,Long> remoteLocalLanguages = this.existingContentMap.getRemoteLocalLanguages(wrapper);


					// if a host does not exist on target, skip content
					if(ignoreContent(content)){
						Contentlet finalContent = content;
						Logger.warn(this.getClass(), "Ignoring contentlet: " + content.getIdentifier() + " | " + Try.of(
                                finalContent::getTitle).getOrElse("unknown")  + " . Unable to find referenced Site: " + content.getHost());
						continue;
					}


					if(UtilMethods.isSet(remoteLang) && remoteLang.getId() > 0) {
						// This should take care of solving any existing conflicts. Previously solved by the Language Handler.
						final Language mappedRemoteLanguage = config.getMappedRemoteLanguage(remoteLang.getId());
						if (UtilMethods.isSet(mappedRemoteLanguage) && mappedRemoteLanguage.getId() > 0) {
							// We trust this mapped value is always accurate. LanguageHandler took the time to map it for us.
							content.setLanguageId(mappedRemoteLanguage.getId());
							info.setLang(mappedRemoteLanguage.getId());
						} else {
						    //fall-back
							final Language localLang = APILocator.getLanguageAPI().getLanguage(remoteLang.getLanguageCode(), remoteLang.getCountryCode());
							if(localLang != null && localLang.getId() > 0){
								content.setLanguageId(localLang.getId());
								info.setLang(localLang.getId());
							}
						}
					}

					final Pair<String,Long> idAndLanguageKey = Pair.of(content.getIdentifier(),
							remoteLocalLanguages.getRight());
					final String contentId = content.getIdentifier();
					final String contentInode = content.getInode();
					final long languageId = content.getLanguageId();
					final String workingOnFile = workingOn.toString();
					if (pushedIdsToIgnore.contains(idAndLanguageKey)) {
						// The specified Content has been pushed as Archived to the receiver. So, DO NOT execute
						// any more code, just continue
						Logger.debug(this, () -> "Content skipped because it's archived, id: " + contentId
								+ " , inode: " + contentInode+ " , language: " + languageId);
						continue;
					}

					final Pair<String, Long> contentIdAndLang = Pair.of(
							content.getIdentifier(), remoteLocalLanguages.getLeft());
					if (this.existingContentMap.hasExistingContent(contentIdAndLang)) {
                        // Updating an existing content with different Identifier. Therefore, get
                        // the info and content from the local server (receiver), not the bundle (sender)
                        updateExisting = Boolean.TRUE;
						final String existingIdentifier = this.existingContentMap.getExistingContentIdentifier(contentIdAndLang);
						Logger.debug(this,() -> "Contentlet in file " + workingOnFile
								+ " with id " + contentId + ", language " + remoteLocalLanguages.getLeft()
								+ " has been mapped to existing content id " + existingIdentifier
								+ ", language " + remoteLocalLanguages.getRight());
                        boolean isLive = info.getWorkingInode().equals(info.getLiveInode());

                        Optional<ContentletVersionInfo> infoOptional = this.versionableAPI
								.getContentletVersionInfo(existingIdentifier,
										remoteLocalLanguages.getRight());

						if(infoOptional.isEmpty()) {
							throw new DotDataException("Can't find Local ContentletVersionInfo. Identifier: "
									+ existingIdentifier + ". Lang: " + remoteLocalLanguages.getRight());
						}

						info = infoOptional.get();
                        // isLive param is set based on the contentlet status in sender in order to
                        // update the local copy. HOWEVER, if local contentlet is unpublished,
                        // always set isLive as false as the contentlet wouldn't be live
                        if (!UtilMethods.isSet(infoOptional.get().getLiveInode())) {
                            isLive = Boolean.FALSE;
                        }
                        content = this.contentletAPI.findContentletByIdentifier(existingIdentifier, isLive, remoteLocalLanguages.getRight(),
                                        systemUser, !RESPECT_FRONTEND_ROLES);
                    }

                    if ( info.isLocked() && info.getLockedBy() != null ) {
                        if ( !info.getLockedBy().equals( systemUser.getUserId() ) ) {//Verify in order to avoid unnecessary queries
                            try {
                                //Verify if the user who locked the content exist
                                final User tempUser = APILocator.getUserAPI().loadUserById( info.getLockedBy() );
                                info.setLockedBy( tempUser.getUserId() );
                            } catch (final Exception e) {
                                info.setLockedBy( systemUser.getUserId() );
                            }
                        }
                    }

                    contentToRefresh.add(new Tuple3(info.getIdentifier(), info.getLang(), true));
					addRelatedContentsToInfoToRemove(content, wrapper.getInfo());

                    // saving a contentletVersionInfo might do an implicit publish. Need to know in order to clean up properly
                    boolean implicitPublish;
                    if (updateExisting) {
                        // Updating an existing content. Just read the local content version info to
                        // publish or not publish the content
                        implicitPublish = wrapper.getInfo().getWorkingInode().equals(wrapper.getInfo().getLiveInode());
                    } else {
                        final Optional<ContentletVersionInfo> local = APILocator.getVersionableAPI().getContentletVersionInfo(info.getIdentifier(), info.getLang());
                        implicitPublish = local.isPresent() && InodeUtils.isSet(local.get().getIdentifier()) &&
                                ((!InodeUtils.isSet(local.get().getLiveInode()) && InodeUtils.isSet(info.getLiveInode())) ||
                                        (InodeUtils.isSet(local.get().getLiveInode()) && InodeUtils.isSet(info.getLiveInode())
                                                && !local.get().getLiveInode().equals(info.getLiveInode())));
                    }

                    /*
                    When the current content is sent only as a working version of the contentlet we don't
                    want to force a Publish, we need to force only live versions.
                     */
                    if ( implicitPublish &&
                            (InodeUtils.isSet(info.getLiveInode()) && InodeUtils.isSet(info.getWorkingInode())) &&
                            (!info.getLiveInode().equals(info.getWorkingInode())) && //The Live version is a different Contentlet than the Working version
                            content.getInode().equals(info.getWorkingInode()) ) {
                        implicitPublish = false;
                    }
                    
                    // I need to know if the current contentlet has a live version that is different by current working (the inodes).
                    // In that case I can't unpublish only because it is in working status but I must check if the current
                    // working match with the current live.
                    final boolean workingWithALiveVersion = !info.getWorkingInode().equals(info.getLiveInode()) && info.getLiveInode()!=null;
                    final boolean onlyWorking = info.getLiveInode()==null;
					final boolean isLiveContentlet = content.isLive();
					final String workingInode = info.getWorkingInode();
					final String liveInode = info.getLiveInode();
                    Logger.debug(this, () -> "*********************** identifier: " + contentId);
                    Logger.debug(this, () -> "*********************** working inode: " + workingInode);
                    Logger.debug(this, () -> "*********************** live inode: " + liveInode);
                    Logger.debug(this, () -> "*********************** Is a working with a live version? " + workingWithALiveVersion);
                    if(info.getLiveInode() == null) {
                        Logger.debug(this, ()-> "*********************** live inode is null");
                    } else {
						if(Logger.isDebugEnabled(getClass())){
                          //This might generate a DotStateException if we're copying a brand-new instance that doesn't have a version on the receiver
						  Logger.debug(this,
								  () -> "*********************** content " + contentId
										+ " is live? " + isLiveContentlet);
						}
					}
                    if (updateExisting && null == wrapper.getInfo().getLiveInode()) {
                        // The content from the sender is unpublished. Therefore, content in the
                        // receiver must be unpublished as well
                        Logger.debug(this, () -> "*********************** identifier ready to unpublish: " + contentId);
                        try {
                            contentletAPI.unpublish(content, systemUser, false);
                            // Setting live Inode to null equals unpublishing the content
                            info.setLiveInode(null);
                        } catch (final DotStateException e) {
                            Logger.debug(this, e,
									() -> "Content cannot unpublish while remote publishing which is probably because it didn't exist. Moving On");
                        }
                    } else if((!UtilMethods.isSet(info.getLiveInode()) && !workingWithALiveVersion) || (!UtilMethods.isSet(info.getLiveInode()) && onlyWorking)){

                        Logger.debug(this, () -> "*********************** identifier ready to unpublish: " + contentId);
                        try{
                            contentletAPI.unpublish(content, systemUser, false);
                        } catch (final DotStateException dpe) {
                            Logger.debug(this, "Content cannot unpublish while remote publishing which is probably because it didn't exist. Moving On", dpe);
                        }
                    }

					final ContentletVersionInfo versionInfoToSave = info;
					Logger.debug(this, () -> "*********************** Saving content info: " + versionInfoToSave
							+ ", language: " + versionInfoToSave.getLang());

					String localIdentifier = findLocalIdentifier(content);


					info.setIdentifier(localIdentifier);
                    APILocator.getVersionableAPI().saveContentletVersionInfo(info);

	                if(isHost) {
                    	final String hostIdentifier = content.getIdentifier();
                    	final Host host = hostAPI.find(hostIdentifier, systemUser, false);
						host.setProperty(Contentlet.DONT_VALIDATE_ME, true);
						host.setProperty(Contentlet.DISABLE_WORKFLOW, true);
                    	if(host.isDefault()) {
                    		APILocator.getHostAPI().updateDefaultHost(host, systemUser, false);
                    	}
                    	APILocator.getHostAPI().updateCache(host);
                    }

	                // pushing live content requires some cleanup we have on contentletAPI.publish
	                if(implicitPublish) {
	                    content = contentletAPI.findContentletByIdentifier(content.getIdentifier(), false, content.getLanguageId(), systemUser, false);
						content.setProperty(Contentlet.DISABLE_WORKFLOW, true);
	                    contentletAPI.publish(content, systemUser, false);
						Logger.debug(this, () -> "Content published: " + contentId
								+ ", inode: " + contentInode + ", language: " + languageId);
	                }
                } //end PUBLISH
            } //end for contentWrapper
		} catch (final NotFoundInDbException e){
			final String errorMsg = String.format("Error processing content in '%s' with ID '%s'. ContentType '%s' does not exist.",
					workingOn,
					(UtilMethods.isSet(wrapper) && UtilMethods.isSet(wrapper.getContent()) ? wrapper.getContent().getIdentifier() : "(empty)"),
					(UtilMethods.isSet(wrapper) && UtilMethods.isSet(wrapper.getContent()) ? wrapper.getContent().getContentTypeId() : "(empty)"));
			Logger.error(this.getClass(), errorMsg, e);
			throw new DotPublishingException(errorMsg, e);
    	} catch (final Exception e){
			final String errorMsg = String.format("An error occurred when processing Contentlet in '%s' with ID '%s': '%s'",
					workingOn,
					(UtilMethods.isSet(wrapper) && UtilMethods.isSet(wrapper.getContent()) ? wrapper.getContent().getIdentifier() : "(empty)"),
					ExceptionUtil.getErrorMessage(e));
			Logger.error(this.getClass(), errorMsg, e);
			throw new DotPublishingException(errorMsg, e);
		}
    }

	private String findLocalIdentifier(final Contentlet content) throws DotDataException {

		String localIdentifier = Try.of(()->APILocator.getIdentifierAPI().find(content.getIdentifier()).getId()).getOrNull();

		if(UtilMethods.isSet(localIdentifier)){
			return localIdentifier;
		}


		if(content.isHTMLPage() || content.isFileAsset()){
			Host host = Try.of(()->APILocator.getHostAPI().find(content.getHost(), APILocator.systemUser(), false)).getOrNull();

			String uri = Try.of(()->content.isHTMLPage() ? APILocator.getHTMLPageAssetAPI().fromContentlet(content).getURI() : APILocator.getFileAssetAPI().fromContentlet(content).getURI()).getOrNull();

			if(!UtilMethods.isSet(uri) || !UtilMethods.isSet(host) || !UtilMethods.isSet(host.getIdentifier())){
				return content.getIdentifier();
			}

			Identifier identifier = identifierAPI.find(host,uri);
			if(UtilMethods.isSet(identifier) && UtilMethods.isSet(identifier.getId())){
				return identifier.getId();
			}

		}
		return content.getIdentifier();

	}




	
	private void addRelatedContentsToInfoToRemove(Contentlet content, ContentletVersionInfo info) {
		try {
			final RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
			final ContentType contentType = content.getContentType();
			final List<Relationship> relationships = relationshipAPI.byContentType(contentType);
            final boolean onlyParents = Config.getBooleanProperty("PUSH_PUBLISHING_REINDEX_RELATIONSHIP_PARENTS_ONLY",
                    false);
			if (!relationships.isEmpty()) {

                for (final Relationship relationship : relationships) {
					final List<Contentlet> relatedContents = new ArrayList<>();

					if (relationshipAPI.sameParentAndChild(relationship)) {
						relatedContents.addAll(APILocator.getRelationshipAPI()
								.dbRelatedContent(relationship, content, true));

						relatedContents.addAll(APILocator.getRelationshipAPI()
								.dbRelatedContent(relationship, content, false));
					} else {
						if (relationship.getParentStructureInode().equals(contentType.inode())){
							relatedContents.addAll(APILocator.getRelationshipAPI()
									.dbRelatedContent(relationship, content, true));
						} else {
							relatedContents.addAll(APILocator.getRelationshipAPI()
									.dbRelatedContent(relationship, content, false));
						}
					}

					for (final Contentlet relatedContent : relatedContents) {
                        boolean shouldReindex = onlyParents
                                ? relationship.getChildStructureInode().equals(content.getContentTypeId())
                                : true;   // always reindex if config is not set


                        contentToRefresh.add(
                                new Tuple3<>(relatedContent.getIdentifier(), info.getLang(), shouldReindex));


					}
				}
			}
		} catch (DotDataException e) {
			Logger.error(ContentHandler.class,
					String.format("Error trying to push content with ID '%s' / version info '%s': %s",
							content.getIdentifier(),
							info,
							ExceptionUtil.getErrorMessage(e))
			);
			Logger.debug(ContentHandler.class, e, () -> ExceptionUtil.getErrorMessage(e));
		}
	}

	/**
	 * Takes the information of the content page coming from the sender and
	 * verifies that the pushed page:
	 * <ol>
	 * <li>Does not match the URL of an existing page with a different
	 * identifier.</li>
	 * <li>Does not match the URL of an existing folder.</li>
	 * <li>Does not match the URL of an existing file asset.</li>
	 * </ol>
	 * <p>
	 * If the page already exists, it <b>MUST</b> have the same identifier in
	 * both the sender and receiver servers for the push to work correctly.
	 * </p>
	 *
	 * @param contentPage
	 *            - The content page data as a {@link Contentlet} object.
	 * @throws DotDataException
	 *             An error occurred when accessing the database.
	 * @throws DotSecurityException
	 *             The user does not have the required permissions to access
	 *             some database information.
	 */
	private void checkContentPageConflicts(Contentlet contentPage) throws DotDataException, DotSecurityException {

		User systemUser = userAPI.getSystemUser();
		String parentFolderId = contentPage.getFolder();
		String pageUrl = contentPage.getMap().get(HTMLPageAssetAPI.URL_FIELD)
				.toString();
		Folder parentFolder = folderAPI.find( parentFolderId, systemUser, false );

		if (parentFolder != null && InodeUtils.isSet(parentFolder.getInode())) {
			Host h = hostAPI.find(parentFolder.getHostId(), systemUser, true);
			String parentFolderPath = parentFolder.getPath();
			if (UtilMethods.isSet(parentFolderPath)) {
				if (!parentFolderPath.startsWith("/")) {
					parentFolderPath = "/" + parentFolderPath;
				}
				if (!parentFolderPath.endsWith("/")) {
					parentFolderPath = parentFolderPath + "/";
				}
				String fullPageUrl = parentFolderPath + pageUrl;
				if (!pageUrl.endsWith(".html")) {
					// Check for folders with same path
					List<Identifier> folders = identifierAPI.findByURIPattern(
							"folder", fullPageUrl, true, h);
					if (!folders.isEmpty()) {
						throw new DotDataException(
								"Conflict between HTML page and Folder. Page with identifier : '"
										+ contentPage.getIdentifier() + "' "
										+ "has the same path as the folder '"
										+ fullPageUrl + "' in the receiver.");
					}
				}
				Identifier i = identifierAPI.find(h, fullPageUrl);
				if (i != null && InodeUtils.isSet(i.getId())) {

                    Contentlet contentInReceiver = null;
                    try {
                        // Check for file assets with same path
                        contentInReceiver = contentletAPI.findContentletByIdentifier( i.getId(), true, contentPage.getLanguageId(), systemUser, false );
                    } catch ( DotContentletStateException e ) {//Just for the record, I don't like to do validations using exceptions....
                        //Do nothing, is safe to continue, we found the identifier but not a record for the given language
                        Logger.debug( this, e.getMessage() );
                    }

                    if ( contentInReceiver != null && contentInReceiver.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET ) {
                        throw new DotDataException(
								"Conflict between HTML page and File Asset. Page with identifier : '"
										+ contentPage.getIdentifier() + "' "
										+ "has the same path as the file '"
										+ fullPageUrl + "' in the receiver.");
					} else if (!contentPage.getIdentifier().equals(i.getId())) {
						// There's already a page with different identifier
						throw new DotDataException(
								"Conflict between HTML pages. Page with identifier : '"
										+ contentPage.getIdentifier()
										+ "' "
										+ "has different Inodes at sender and receiver. Please run the Integrity Checker at sender.");
					}
				}
			}
		}
	}

	/**
	 * Adds the {@link Contentlet} object coming from the pushed bundle. It's
	 * worth noting that different actions are performed on the content based on
	 * its type: Contentlet, Content Page, Host, etc.
	 * 
	 * @param content
	 *            - The content being saved.
	 * @param folderOut
	 *            - The location of the bundle in the file system.
	 * @param userToUse
	 *            - The {@link User} performing this action.
	 * @param wrapper
	 *            - The data {@link ContentWrapper} containing the most relevant
	 *            information of the asset being saved.
	 * @param isHost - Whether the content being published is of type Host or not
	 * @throws Exception
	 *             An error occurred when processing the content.
	 */
	private void saveContent(Contentlet content, final File folderOut, final User userToUse, final ContentWrapper wrapper, final boolean isHost, final Pair<Long,Long> remoteLocalLanguages)
            throws Exception
    {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
	        throw new RuntimeException("need an enterprise pro license to run this");
	    }
        final List<Field> fields = FieldsCache.getFieldsByStructureInode(content.getContentTypeId());
        final String inode = content.getInode();
        final String identifier = content.getIdentifier();
        boolean updateExisting = Boolean.FALSE;
        content = findUniqueContentMatch(content, fields, remoteLocalLanguages);
        if (!identifier.equals(content.getIdentifier())) {
            updateExisting = Boolean.TRUE;
        }
	    
	    //Copy asset files to bundle folder keeping original folders structure
        for (final Field ff : fields) {
            if(ff.getFieldType().equals(Field.FieldType.BINARY.toString())) {
                final String folderTree = inode.charAt(0)+File.separator+inode.charAt(1)+File.separator+
                        inode+File.separator+ff.getVelocityVarName();

                final File binaryFolder = new File(folderOut+File.separator+"assets"+File.separator+folderTree);
				final File[] filesInFolder = binaryFolder.listFiles();

                if(binaryFolder.exists() && filesInFolder!=null && filesInFolder.length > 0) {
					content.setBinary(ff.getVelocityVarName(), filesInFolder[0]);
				}
			} else if ( ff.getFieldType().equals(Field.FieldType.TAG.toString()) ) {
				// We don't want to rely on this value since it is not supposed to exist. So set it empty always
				content.setStringProperty(ff.getVelocityVarName(), "");
			}
		}

		//Search for the correct modUser to use
		final String modUserId = identifyModUser(content);

		// Verify if this content is the default host in the target server
		if (isHost) {
			holdDefaultHostConfiguration(content, userToUse);
		}

      // Wiping out the thumbnails and resized versions (if a fileAsset)
      APILocator.getFileAssetAPI().cleanThumbnailsFromContentlet(content);

      //Verify if this contentlet is a HTMLPage
      if (BaseContentType.HTMLPAGE.equals(content.getContentType().baseType())) {
        //Adding to the page a listener in order to invalidate the page after being save
        final IHTMLPage hp = HandlerUtil.fromContentlet(content, false);
        HibernateUtil.addCommitListener(new FlushCacheRunnable() {
          public void run() {
            new PageLoader().invalidate(hp);
          }
        });
      }

        //Saving the content
        if (content.isArchived()){
            this.contentletAPI.unarchive(content, userToUse, !RESPECT_FRONTEND_ROLES);
        }
        content = this.contentletAPI.checkin(content, userToUse, !RESPECT_FRONTEND_ROLES);

		final String filterKey = this.getFilterKeyFromBundle();
		Logger.debug(this, () -> "Filter Key: " + filterKey);
		final FilterDescriptor filterDescriptor = APILocator.getPublisherAPI().getFilterDescriptorByKey(filterKey);
		boolean isRelationshipsFilter = filterDescriptor.getFilters().containsKey(RELATIONSHIPS_KEY) ? Boolean.class.cast(filterDescriptor.getFilters()
				.get(FilterDescriptor.RELATIONSHIPS_KEY)) : true;
		Logger.debug(this, () -> "Relationships Filter: " + isRelationshipsFilter);
        if (isRelationshipsFilter) {
			// Depending on the selected Push Publishing Filter, we need to remove the "old" trees
			// in order to add the new ones, if the relationships filter is set to false, we shouldn't remove the trees
			this.cleanTrees(content);
			this.regenerateTree(wrapper, remoteLocalLanguages.getLeft());
		}
        // Categories
        if (UtilMethods.isSet(wrapper.getCategories())) {
            handleContentCategories(content.getInode(), wrapper.getCategories());
        }
        CacheLocator.getCategoryCache().removeParents(content);

        //Verify if this contentlet is an HTMLPage
        if (BaseContentType.HTMLPAGE.equals(content.getContentType().baseType())) {
            //Applying the multi-tree changes to the html page
            if (updateExisting) {
                // Updating an existing content. So, use local Identifier instead of the one in the
                // bundle.
                final String existingIdentifier = content.getIdentifier();
                wrapper.getMultiTree().forEach(multiTree -> {
                    multiTree.put("parent1", existingIdentifier);
                });
            }
			HandlerUtil.setMultiTree(content.getIdentifier(), content.getInode(),
					content.getLanguageId(), wrapper.getMultiTree(), modUserId);
		}
		// Check if associated rules should be overwritten
		if (Config.getBooleanProperty("PUSH_PUBLISHING_RULES_OVERWRITE", true)) {
			if ((content.isHost() || content.isHTMLPage())) {
				// Delete all associated Rules
    			APILocator.getRulesAPI().deleteRulesByParent(content, userToUse, !RESPECT_FRONTEND_ROLES);
        	}
        }
		Map<String, List<Tag>> tagsFromSender = wrapper.getContentTags();

		relateTagsToContent(content, tagsFromSender);

        persistMetadata(content, wrapper.getBinariesMetadata());

		HandlerUtil.setModUser(content.getInode(), modUserId, HandlerType.CONTENTLET);
		CacheLocator.getContentletCache().remove(content.getInode());

		final String contentId = content.getIdentifier();
		HibernateUtil.addCommitListener( new FlushCacheRunnable() {
			public void run () {
				try {
					invalidateMultiTreeCache(contentId);
				} catch (final DotDataException e) {
					Logger.debug(this, () -> String.format("Failed to flush cache for Contentlet '%s': %s", contentId,
							ExceptionUtil.getErrorMessage(e)));
				}
			}
		} );
		APILocator.getContentletAPI().find(content.getInode(), userToUse, !RESPECT_FRONTEND_ROLES);
		APILocator.getContentletIndexAPI().addContentToIndex(content, false);

		PushPublishLogger.log(getClass(),
				isHost ? PushPublishHandler.HOST : PushPublishHandler.CONTENT,
				PushPublishAction.PUBLISH, content.getIdentifier(), content.getInode(), content.getName(), config.getId());
    }

	/**
	 * Retrieves the Push Publishing Filter that was selected to generate the current Bundle.
	 *
	 * @return The Push Publishing Filter key. But, if the bundle doesn't exist, returns an empty
	 * String.
	 *
	 * @throws DotDataException An error occurred when interacting with the data source.
	 */
	private String getFilterKeyFromBundle() throws DotDataException {
		final Bundle bundle =
				this.bundleAPI.get().getBundleById(com.dotmarketing.util.FileUtil.removeExtension(this.config.getId()));
		return null != bundle ? bundle.getFilterKey() : BLANK;
	}

	/**
	 * Invalidates the respective MultiTree cache entry when the pushed Contentlet is the child of an existing record
	 * but wasn't pushed back when such a record was created.
	 * <p>There are situations in which an HTML Page is shallow-pushed, i.e., none of its child contents are pushed
	 * along. If a User opens up that page in the receiving instance, this can result in the MultiTree cache storing an
	 * entry with an empty list of child contents. So, the next time any of such contents are pushed, they will not show
	 * up in the HTML Page, requiring the MultiTree cache to be flushed. This method checks this situation, and flushes
	 * the respective cache entry when appropriate.</p>
	 *
	 * @param contentletId The ID of the {@link Contentlet} that is being pushed.
	 *
	 * @throws DotDataException An error occurred when interacting with the data source.
	 */
	private void invalidateMultiTreeCache(final String contentletId) throws DotDataException {
		if (UtilMethods.isNotSet(contentletId)) {
			return;
		}
		final List<MultiTree> multiTreesByChild = this.multiTreeAPI.get().getMultiTreesByChild(contentletId);
		if (UtilMethods.isSet(multiTreesByChild)) {
			final MultiTreeCache multiTreeCache = CacheLocator.getMultiTreeCache();
			multiTreesByChild.forEach(multiTree -> multiTreeCache.removePageMultiTrees(multiTree.getHtmlPage()));
		}
	}

    /**
     * @param content
     * @throws DotDataException
     */
    private void invalidateRelationshipsCache(final Contentlet content) throws DotDataException {
    	if(content==null) return;
        final RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        final ContentType contentType = content.getContentType();
        final List<Relationship> relationships = relationshipAPI.byContentType(contentType);
        relationships.forEach(relationship -> {
            this.contentletAPI.invalidateRelatedContentCache(content, relationship,
                    relationshipAPI.isParent(relationship,
                            contentType));
        });

    }

    /**
	 * If this content is the default host in the receiver, hold it as the default host, since a
	 * dotCMS instance shouldn't end without a default host
	 *
	 * @param content The {@link Contentlet} object coming from the bundle (sender environment).
	 * @param userToUse The {@link User} performing this action.
	 * @throws Exception An error occurred when processing the content.
	 */
	private void holdDefaultHostConfiguration(final Contentlet content, final User userToUse)
			throws Exception {

		final Identifier targetExistingIdentifier = this.identifierAPI.find(content);
		if (null != targetExistingIdentifier && UtilMethods
				.isSet(targetExistingIdentifier.getId())) {
			final String hostIdentifier = content.getIdentifier();
			final Host targetHost = hostAPI.find(hostIdentifier, userToUse, false);
			if (targetHost != null && UtilMethods.isSet(targetHost.getIdentifier())) {
				if (targetHost.isDefault()) {
					final Host sourceHost = new Host(content);
					sourceHost.setDefault(true);
					Logger.debug(this, () ->
							"Keep host as default in the target env: " + targetHost.getHostname());
				}
			}
		}

	}

	/**
	 * After removing the old references to the contentlet specified in the {@code tree} table, the
	 * new updated entries will be provided in the {@link ContentWrapper} object, which contain
	 * the most recent changes.
	 *
	 * @param wrapper The wrapped Contentlet coming from the bundle.
	 */
	@WrapInTransaction
	private void regenerateTree(ContentWrapper wrapper, final long languageId) {
		for (final Map<String, Object> tRow : wrapper.getTree()) {
			final Tree tree = new Tree();
			tree.setChild((String) tRow.get("child"));
			tree.setParent((String) tRow.get("parent"));
			Pair<String, Long> contentIdAndKey = Pair.of((String) tRow.get("parent"), languageId);
			if (this.existingContentMap.hasExistingContent(contentIdAndKey)) {
				// Updating an existing content. So, use local Identifier instead of the one in the
				// bundle.
				tree.setParent(this.existingContentMap.getExistingContentIdentifier(contentIdAndKey));
			}
			tree.setRelationType((String) tRow.get("relation_type"));
			tree.setTreeOrder(Integer.parseInt(tRow.get("tree_order").toString()));
			final Tree temp = TreeFactory.getTree(tree);
			try {
				// Keep the control for safety
				if (null != temp && UtilMethods.isSet(temp.getParent())) {
					TreeFactory.deleteTree(temp);
					HibernateUtil.flush();
					HibernateUtil.evict(temp);
				}
			} catch (final Exception e) {
				Logger.error(this, "Error deleting tree for contentlet '" + wrapper.getContent().getIdentifier() + "'." +
						" Parent: " + temp.getParent(), e);
			}
			TreeFactory.saveTree(tree);
		}
	}

    /**
     * Based on the content coming from the sender environment, finds the correspondent content in the receiver
     * environment based on a unique field instead of using the Identifier equality approach. This will allow content
     * creators to update or delete a given piece of content based on the value of a unique field, instead of using the
     * Identifier value.
     *
     * @param content              The {@link Contentlet} object coming from the bundle (sender environment).
     * @param fields               The list of fields in the content, based on its Content Type.
     * @param remoteLocalLanguages Internal map that stores the association between language IDs from the sending and
     *                             the receiving dotCMS instance.
     *
     * @return If the "correspondent" content is found such a content will be returned. Otherwise, the original content
     * in the bundle will be returned.
     *
     * @throws DotDataException     An error occurred when retrieving the data.
     * @throws DotSecurityException A user permission error has occurred.
     */
    private Contentlet findUniqueContentMatch(final Contentlet content, final List<Field> fields, final Pair<Long,Long> remoteLocalLanguages)
			throws DotDataException, DotSecurityException {
		final Identifier contentIdentifier = this.identifierAPI.find(content);
		if (null != contentIdentifier && !UtilMethods.isSet(contentIdentifier.getId())) {
			// If the Identifier doesn't exist, needs to figure out if contentlet has a unique field
			// Could be that the contentlet has more than one unique field, we need to evaluate all
			//If at least one unique field is the same, the local contentlet must be updated
			final List<Field> uniqueFields = fields.stream().filter(Field::isUnique).collect(
					CollectionsUtils.toImmutableList());
			if (!uniqueFields.isEmpty()) {
				// If it does, maybe the contentlet is referencing another one with different
				// Identifier but same value for the unique field
				final User systemUser = this.userAPI.getSystemUser();
				final StringBuilder luceneQuery = new StringBuilder();
				luceneQuery.append("+structureInode:").append(content.getContentTypeId());
				luceneQuery.append(" +languageId:").append(remoteLocalLanguages.getRight());
				luceneQuery.append(" +(");
				for (final Field uniqueField : uniqueFields) {
					if (getUniquePerSiteConfig(uniqueField)){
						luceneQuery.append(" +conHost:" + content.getHost());
					}
					final String fieldValue = content.get(uniqueField.getVelocityVarName()).toString();
					luceneQuery.append(content.getContentType().variable()).append(".")
							.append(uniqueField.getVelocityVarName()).append(ESUtils.SHA_256)
							.append(":")
							.append(ESUtils.sha256(content.getContentType().variable()
											+ "." + uniqueField.getVelocityVarName(), fieldValue,
									content.getLanguageId()))
							.append(" ");
				}
				luceneQuery.append(")");
				final int limit = 0;
				final int offset = -1;
				final String sortBy = "score";
				final List<ContentletSearch> contentlets = this.contentletAPI
						.searchIndex(luceneQuery.toString(), limit, offset, sortBy,
								systemUser, !RESPECT_FRONTEND_ROLES);

				if (null != contentlets && !contentlets.isEmpty()) {
					// A contentlet with different Identifier but same unique value has been found. Update the local
                    // one WITHOUT CHANGING the local Identifier and Inode
					final Contentlet matchingContent =
							this.contentletAPI.find(contentlets.get(0).getInode(), systemUser,
									!RESPECT_FRONTEND_ROLES);

                    if (null == matchingContent || !UtilMethods.isSet(matchingContent.getIdentifier())) {
                        // It might be that the matching content doesn't exist in the DB anymore, or is archived
                        throw new DotDataException(getUniqueMatchErrorMsg(uniqueFields, luceneQuery.toString(),
                                contentlets.get(0)));
                    }
                    existingContentMap.addExistingContent(
							Pair.of(content.getIdentifier(), remoteLocalLanguages.getLeft()),
							Pair.of(matchingContent.getIdentifier(), remoteLocalLanguages.getRight()));
					final String identifier = matchingContent.getIdentifier();
					this.contentletAPI.copyProperties(matchingContent, content.getMap());
					matchingContent.setIdentifier(identifier);
					matchingContent.setInode(UUIDUtil.uuid());
					matchingContent.setProperty(Contentlet.DONT_VALIDATE_ME, true);
					matchingContent.setProperty(Contentlet.DISABLE_WORKFLOW, true);
					return matchingContent;
				}
			}
		}
		return content;
	}

    /**
     * Utility method to generate the appropriate error message when {@link #findUniqueContentMatch(Contentlet, List,
     * Pair)} method fails to retrieve a result.
     *
     * @param uniqueFields   The list of unique {@link Field} objects in the contentlet that is being pushed.
     * @param luceneQuery    The Lucene query used to find the contentlet that matches the unique value.
     * @param matchedContent The first {@link ContentletSearch} result matched by the Lucene query.
     *
     * @return The error message to display in the log.
     */
    private String getUniqueMatchErrorMsg(final List<Field> uniqueFields, final String luceneQuery, final
	ContentletSearch matchedContent) {
		final StringBuilder fieldsInfo = new StringBuilder();
		for (final Field field : uniqueFields) {
			fieldsInfo.append(field.getVelocityVarName()).append(" [").append(field.getInode()).append("], ");
		}
        return String.format("Lucene query [ %s ] matched existing content with ID '%s' / inode '%s' in ES Index, but" +
                " it was not found via API. Unique fields: %s", luceneQuery, matchedContent.getIdentifier(),
                matchedContent.getInode(), fieldsInfo);
    }

	/**
	 * Associates a list of tags coming from the bundle to the specified local content.
	 *
	 * @param content - The {@link Contentlet} that will have the updated tags from the bundle.
	 * @param tagsFromSender - The list of {@link Tag} objects coming from the sender,
	 * @throws DotDataException Tags could not be read or saved to the data source.
	 */
	@VisibleForTesting
	void relateTagsToContent(Contentlet content, Map<String, List<Tag>> tagsFromSender) throws DotDataException {
		if(tagsFromSender==null || tagsFromSender.isEmpty()) {
			return;
		}

		for (Map.Entry<String, List<Tag>> fieldTags : tagsFromSender.entrySet()) {
			String fieldVarName = fieldTags.getKey();

			for (Tag remoteTag : fieldTags.getValue()) {
				Tag localTag = tagAPI.getTagByNameAndHost(remoteTag.getTagName(), remoteTag.getHostId());

				String localUserId = Try.of(()->APILocator.getUserAPI().loadUserById(remoteTag.getUserId()).getUserId()).getOrElse(APILocator.systemUser().getUserId());

				Host tagSite = Try.of(()->APILocator.getHostAPI().find(remoteTag.getHostId(), APILocator.systemUser(), false)).getOrNull();
				Host contentSite = Try.of(()->APILocator.getHostAPI().find(content.getIdentifier(), APILocator.systemUser(), false)).getOrNull();

				final String localSiteId = UtilMethods.isSet(()->tagSite.getTagStorage())
						? tagSite.getTagStorage()
						: UtilMethods.isSet(()->contentSite.getTagStorage())
								? contentSite.getTagStorage()
								: Host.SYSTEM_HOST;



				// if there is NO local tag, save the one coming from remote, otherwise use local
				if (localTag == null || Strings.isNullOrEmpty(localTag.getTagId())) {
					localTag = tagAPI.saveTag(remoteTag.getTagName(), localUserId, localSiteId);
				}

				TagInode localTagInode = tagAPI.getTagInode(localTag.getTagId(), content.getInode(), fieldVarName);

				// avoid relating tags twice
				if(UtilMethods.isEmpty(()->localTagInode.getTagId())) {
					tagAPI.addContentletTagInode(localTag, content.getInode(), fieldVarName);
				}
			}
		}
	}

    /**
     * Handles categories included in the bundle by creating/ensuring a tree for each category is associated to the content inode
     *
     * @param inode corresponding to the content
     * @param categories list of category-qualified-names included in the bundle
     * @throws DotSecurityException
     * @throws DotDataException
     */
	@WrapInTransaction
	private void handleContentCategories(String inode, List<String> categories) throws DotSecurityException, DotDataException {
		for(int i = 0; i < categories.size(); i++) {
			Category category = findCategory(categories.get(i));

			if (category != null) {
		        Tree tree = new Tree();
		        tree.setChild(inode);
		        tree.setParent(category.getInode());
		        tree.setRelationType("child");
		        tree.setTreeOrder(i);

		        Tree temp = TreeFactory.getTree(tree);
		        try {
		        	//Keep the control for safety
		            if(temp != null && UtilMethods.isSet(temp.getParent())) {
		            	TreeFactory.deleteTree(temp);
			            HibernateUtil.flush();
			        	HibernateUtil.evict(temp);
		            }
		        } catch (Exception e) {
					Logger.error(this, "Error deleting Tree for inode '" + inode + "'. Parent: " + temp.getParent(), e);
				}

		        TreeFactory.saveTree(tree);
			}
		}
	}

    /**
     * Returns the Category object corresponding to a category-qualified-name by traversing its implicit hierarchy
     *
     * @param categoryQualifiedName corresponding to the category object
     * @throws DotSecurityException
     * @throws DotDataException
     */
	private Category findCategory(String categoryQualifiedName) throws DotSecurityException, DotDataException {
		final String[] categoryQualifiedNameParts = categoryQualifiedName.split(ContentBundler.CATEGORY_SEPARATOR);
		
		Category cat = categoryAPI.findByKey(categoryQualifiedNameParts[0], userAPI.getSystemUser(), true);

		for(int i = 1; cat != null && i < categoryQualifiedNameParts.length; i++) {
			List<Category> children = categoryAPI.findChildren(userAPI.getSystemUser(), cat.getInode(), true, null);

			final int iFinal = i;
			cat = children.stream().filter(
				c -> categoryQualifiedNameParts[iFinal].equals(c.getKey())
			).findFirst().orElse(null);
		}

		return cat;
	}

	/**
	 * Deletes the {@link Contentlet} object specified in the pushed bundle.
	 * 
	 * @param content
	 *            - The content being deleted.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param isHost - Whether the content to unpublish is of type Host or not
	 * @param isPushedContentArchived If the Content being pushed is flagged as {@code archived}, set to {@code true} so
	 *                                that it is archived in the receiving environment WITHOUT being deleted. If a full
	 *                                delete is required, set to {@code false}.
	 * @throws Exception
	 *             An error occurred when deleting the specified asset.
	 */
	private void archiveOrDeleteContent(Contentlet content, final User user, final boolean isHost,
										final Pair<Long, Long> remoteLocalLanguages, final boolean isPushedContentArchived)
            throws Exception
    {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
	        throw new RuntimeException("need an enterprise pro license to run this");
	    }
	    if(content.getContentTypeId().equals(APILocator.getHostAPI().findSystemHost().getStructureInode())){
	    	final Host site = APILocator.getHostAPI().find(content.getIdentifier(), APILocator.getUserAPI().getSystemUser(), !RESPECT_FRONTEND_ROLES);
	    	if(null != site && UtilMethods.isSet(site.getInode())){
	    		APILocator.getHostAPI().archive(site,user,!RESPECT_FRONTEND_ROLES);
	    		APILocator.getHostAPI().delete(site, user, !RESPECT_FRONTEND_ROLES);
	    	}
	    }else{
			// if the content is not live, then we don't need to unpublish it
			Optional<ContentletVersionInfo> existingInfoOptional = Optional.empty();
            final List<Contentlet> contents = findContents(content.getIdentifier(), user);

            if (UtilMethods.isSet(contents)) {
				final Optional<Contentlet> contentletOptional = contents.stream()
						.filter(c -> c.getLanguageId() == remoteLocalLanguages.getRight()).findFirst();

				if (contentletOptional.isPresent()) {
					final Contentlet existingContent = contentletOptional.get();
					existingInfoOptional = this.versionableAPI.getContentletVersionInfo(
							existingContent.getIdentifier(), remoteLocalLanguages.getRight());
				}
			} else {
                final List<Field> fields = FieldsCache.getFieldsByStructureInode(content.getContentTypeId());
                content = findUniqueContentMatch(content, fields,remoteLocalLanguages);
				final Pair<String, Long> contentIdAndLang = Pair.of(
						content.getIdentifier(), remoteLocalLanguages.getLeft());
				if (this.existingContentMap.hasExistingContent(contentIdAndLang)) {
					final String existingIdentifier = this.existingContentMap
							.getExistingContentIdentifier(contentIdAndLang);
					existingInfoOptional = this.versionableAPI.getContentletVersionInfo(
							existingIdentifier, remoteLocalLanguages.getRight());
				}
            }

			boolean isExistingContentLive = false;
			if (existingInfoOptional.isPresent()) {
				ContentletVersionInfo existingVersionInfo = existingInfoOptional.get();
				isExistingContentLive = UtilMethods.isSet(existingVersionInfo.getLiveInode());
			}

			if (isExistingContentLive) {
				try {
					contentletAPI.unpublish(content, user, !RESPECT_FRONTEND_ROLES);
				} catch (DotStateException dse) {
					final String contentId = content.getIdentifier();
					final String contentInode = content.getInode();
					if (isPushedContentArchived) {
						Logger.debug(getClass(), () -> String.format(
								"No live version, not able to archive, contentlet: id -> %s, inode-> %s",
								contentId, contentInode));
					} else {
						Logger.debug(getClass(), () -> String.format("No live version, not able to unpublish, contentlet: id -> %s, inode-> %s",
								contentId, contentInode));
					}
				}
			}

			if (isPushedContentArchived) {
				this.contentletAPI.archive(content, user, !RESPECT_FRONTEND_ROLES);
			} else {
				this.contentletAPI.destroy(content, user, !RESPECT_FRONTEND_ROLES);
			}

			PushPublishLogger.log(getClass(),
					isHost ? PushPublishHandler.HOST : PushPublishHandler.CONTENT,
					PushPublishAction.UNPUBLISH, content.getIdentifier(), content.getInode(), content.getName(), config.getId());
	    }
	    if (content.isHost() || content.isHTMLPage()) {
	    	// Delete all associated Rules
			APILocator.getRulesAPI().deleteRulesByParent(content, user, !RESPECT_FRONTEND_ROLES);
		}
    }

    /**
     * Returns the list of {@link Contentlet} objects that match the specified Identifier. A lucene
     * search will be performed on "live" contents. If not found, "working" contents will be
     * retrieved. Otherwise, an empty list will be returned.
     * 
     * @param identifier - The content's Identifier.
     * @param user - The user performing this action.
     * @return The list of {@link Contentlet} objects.
     * @throws DotDataException An error occurred when retrieving data from the index.
     * @throws DotSecurityException The specified user does not have the required permissions to
     *         perform this action.
     */
    private List<Contentlet> findContents(final String identifier, final User user)
                    throws DotDataException, DotSecurityException {
        final int limit = 0;
        final int offset = -1;
        final String sortBy = null;
        String luceneQuery = "+identifier:" + identifier + " +live:true";
        List<Contentlet> contents = contentletAPI.search(luceneQuery, limit, offset, sortBy, user, !RESPECT_FRONTEND_ROLES);

        if (contents.isEmpty()) {
            luceneQuery = "+identifier:" + identifier + " +working:true";
            contents = contentletAPI.search(luceneQuery, limit, offset, sortBy, user, !RESPECT_FRONTEND_ROLES);
        }
        return contents;
    }

    /**
     * Delete the Trees related to this given contentlet, this is in order to add the new published Trees (Relationships and categories)
     *
     * @param contentlet whose tree will be deleted
     */
	@WrapInTransaction
	private void cleanTrees ( Contentlet contentlet ) {
		if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
			throw new RuntimeException("need an enterprise pro license to run this");
		try{
			TreeFactory.deleteTreesByChildId(contentlet.getInode());
			TreeFactory.deleteTreesByChildId(contentlet.getIdentifier());
			TreeFactory.deleteTreesByParentById(contentlet.getInode());
			TreeFactory.deleteTreesByParentById(contentlet.getIdentifier());
			HibernateUtil.flush();
		}catch (Exception e) {
			Logger.error(this, "Cleaning trees for Contentlet '" + contentlet.getIdentifier() + "' has failed: " +
					ExceptionUtil.getErrorMessage(e), e);
		}
	}

	/**
	 * If the <strong>HEADLESS_USER_CONTENT_DELIVERY</strong> is set to <strong>TRUE</strong> we
	 * need to use the System User if the modUser does not exist or does not have publish
	 * permissions on the current contentlet, running jobs for publish and expire content
	 * (Publish/Expired attributes on the Content Type) uses the modUser as the user to execute
	 * those processes.
	 *
	 * @param content Contentlet to analyze
	 */
	private String identifyModUser(Contentlet content) throws DotDataException {

		String modUserId = content.getModUser();

		//Based on the HEADLESS_USER_CONTENT_DELIVERY lets find out the correct modUser to use
		if (Config.getBooleanProperty("HEADLESS_USER_CONTENT_DELIVERY", Boolean.TRUE)) {

			try {
				//First load the modUser
				User modUser = APILocator.getUserAPI().loadUserById(modUserId);
				//Verify if the modUser has publish permissions on this content
				if (!permissionAPI
						.doesUserHavePermission(content, PermissionAPI.PERMISSION_PUBLISH, modUser,
								false)) {
					//If no permissions lets use the System User
					modUserId = APILocator.getUserAPI().getSystemUser().getUserId();
				}
			} catch (final Exception e) {
				Logger.debug(this,
						"Unable to use received user from sender. Using 'system' User instead. "
							+ "UserId [" + modUserId + "]. Error message: " + ExceptionUtil.getErrorMessage(e));
				//On errors also lets use the System User and allow the process to continue
				modUserId = APILocator.getUserAPI().getSystemUser().getUserId();
			}
		}

		return modUserId;
	}

	/**
	 *
	 * @param contentlet
	 * @param binariesMetadata
	 * @throws DotDataException
	 */
    private void persistMetadata(final Contentlet contentlet, final Map<String, Metadata> binariesMetadata)
			throws DotDataException {
        if(null != binariesMetadata){
           fileMetadataAPI.setMetadata(contentlet, binariesMetadata);
        }
    }

	private boolean getUniquePerSiteConfig(final Field field) {
		return getUniquePerSiteConfig(LegacyFieldTransformer.from(field));
	}

	private boolean getUniquePerSiteConfig(final com.dotcms.contenttype.model.field.Field field) {
		return field.fieldVariableValue(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
				.map(value -> Boolean.valueOf(value)).orElse(false);
	}

}
