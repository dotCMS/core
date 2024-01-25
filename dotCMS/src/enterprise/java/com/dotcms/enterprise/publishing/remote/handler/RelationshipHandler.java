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

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.RelationshipBundler;
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

	private PublisherConfig config;

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
            final String errorMsg = String.format("An error occurred when processing Relationship in '%s' with ID " +
                    "'%s': %s", workingOn, (null == relationshipToPublish ? "(empty)" : relationshipToPublish
                    .getRelationTypeValue()), (null == relationshipToPublish ? "(empty)" : relationshipToPublish
                    .getInode()), e.getMessage());
            Logger.error(this.getClass(), errorMsg, e);
            throw new DotPublishingException(errorMsg, e);
		}
	}

}
