/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.RelationshipBundler;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.publisher.pusher.wrapper.RelationshipWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;

/**
 * This handler class is part of the Push Publishing mechanism that deals with Content Relationship-related information
 * inside a bundle and saves it in the receiving instance. This class will read and process only the {@link
 * Relationship} data files.
 * <p>
 * Relationships define a pre-defined hierarchical relationship among different Content Types. Individual pieces of
 * Content may then be related to other content in a parent-child relationship. When certain types of content need to be
 * found by related content in a hierarchy (e.g. you need to find children of an item, or the parent or siblings of an
 * item), then Relationships are usually the best solution.
 *
 * @author root
 * @since Mar 7, 2013
 */
public class RelationshipHandler implements IHandler {

	private final PublisherConfig config;

	public RelationshipHandler(PublisherConfig config) {
		this.config = config;
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public void handle(File bundleFolder) throws Exception {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
			throw new RuntimeException("need an enterprise pro license to run this");
		}
		Collection<File> relationships = FileUtil.listFilesRecursively(bundleFolder, new RelationshipBundler().getFileFilter());
		handleRelationships(relationships);
	}

	private void handleRelationships(Collection<File> relationships) throws DotPublishingException {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }
        File workingOn = null;
	    Relationship relationshipToPublish = null;
		try{
			RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
			XStream xstream = XStreamHandler.newXStreamInstance();
			for(File relationshipFile : relationships){
                workingOn = relationshipFile;
				if(relationshipFile.isDirectory()) continue;
				RelationshipWrapper relationshipWrapper;
				try(final InputStream input = Files.newInputStream(relationshipFile.toPath())){
					relationshipWrapper = (RelationshipWrapper)xstream.fromXML(input);
				}
                relationshipToPublish = relationshipWrapper.getRelationship();
				Relationship aRel = relationshipAPI.byTypeValue(relationshipWrapper.getRelationship().getRelationTypeValue());
				
				boolean relationshipAlreadyExists = aRel!=null && UtilMethods.isSet(aRel.getInode());				

				// we are unpublishing...
				if(relationshipWrapper.getOperation().equals(Operation.UNPUBLISH)){
					if(relationshipAlreadyExists) {
						String relIden = aRel.getIdentifier();
						relationshipAPI.delete(aRel);
						PushPublishLogger.log(getClass(), PushPublishHandler.RELATIONSHIP, PushPublishAction.UNPUBLISH,
								relIden, aRel.getInode(), aRel.getParentRelationName() + " " + aRel.getChildRelationName(), config.getId());
					}
				} else {
					if(!relationshipAlreadyExists){
						Relationship rel = relationshipWrapper.getRelationship();
						relationshipAPI.create(rel);

						PushPublishLogger.log(getClass(), PushPublishHandler.RELATIONSHIP, PushPublishAction.PUBLISH_CREATE,
								rel.getIdentifier(), rel.getInode(), rel.getParentRelationName() + " " + rel.getChildRelationName(), config.getId());
					}else{// delete if exists and recreate

						Relationship relationshipSent = relationshipWrapper.getRelationship();

						//Deletes the current relationship keeping intact the existing tree records
						relationshipAPI.deleteKeepTrees(aRel);

						//Saves the new relationship
						relationshipAPI.save(relationshipSent, aRel.getInode());

						PushPublishLogger.log(getClass(), PushPublishHandler.RELATIONSHIP, PushPublishAction.PUBLISH_UPDATE,
										aRel.getIdentifier(), aRel.getInode(), relationshipSent.getParentRelationName() + " " +
										relationshipSent.getChildRelationName(), config.getId());
					}
				}
			}
		} catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when processing Relationship in '%s' with type value " +
                    "'%s' [%s]: %s", workingOn, (null == relationshipToPublish ? "(empty)" : relationshipToPublish
                    .getRelationTypeValue()), (null == relationshipToPublish ? "(empty)" : relationshipToPublish
                    .getInode()), ExceptionUtil.getErrorMessage(e));
            Logger.error(this.getClass(), errorMsg, e);
            throw new DotPublishingException(errorMsg, e);
		}
	}

}
