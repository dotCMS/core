package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.RelationshipBundler;
import com.dotcms.publisher.pusher.wrapper.RelationshipWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;

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
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this");
		Collection<File> relationships = FileUtil.listFilesRecursively(bundleFolder, new RelationshipBundler().getFileFilter());
		handleRelationships(relationships);
	}

	private void handleRelationships(Collection<File> relationships) throws DotPublishingException {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this");
		try{
			RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
			XStream xstream=new XStream(new DomDriver());
			for(File relationship : relationships){
				if(relationship.isDirectory()) continue;
				RelationshipWrapper relationshipWrapper;
				try(final InputStream input = Files.newInputStream(relationship.toPath())){
					relationshipWrapper = (RelationshipWrapper)xstream.fromXML(input);
				}

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
		}catch(Exception e){
			throw new DotPublishingException(e.getMessage(),e);
		}
	}


}
