package com.dotcms.publisher.receiver.handler;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotcms.publisher.pusher.bundler.RelationshipBundler;
import com.dotcms.publisher.pusher.wrapper.RelationshipWrapper;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class RelationshipHandler implements IHandler {

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public void handle(File bundleFolder) throws Exception {
		Collection<File> relationships = FileUtil.listFilesRecursively(bundleFolder, new RelationshipBundler().getFileFilter());
		handleRelationships(relationships);
	}

	private void handleRelationships(Collection<File> relationships) throws DotPublishingException {
		try{
			XStream xstream=new XStream(new DomDriver());
			for(File relationship : relationships){
				if(relationship.isDirectory()) continue;
				RelationshipWrapper relationshipWrapper = (RelationshipWrapper)  xstream.fromXML(new FileInputStream(relationship));
				
				Relationship aRel = RelationshipFactory.getRelationshipByInode(relationshipWrapper.getRelationship().getInode());
				
				boolean relationshipAlreadyExists = aRel!=null && UtilMethods.isSet(aRel.getInode());
				
				// we are unpublishing...
				if(relationshipWrapper.getOperation().equals(Operation.UNPUBLISH)){
					if(relationshipAlreadyExists)
						RelationshipFactory.deleteRelationship(aRel);					
				} else {
					if(!relationshipAlreadyExists){
						Relationship rel = relationshipWrapper.getRelationship();
						RelationshipFactory.saveRelationship(rel,rel.getInode());
					}else{// delete if exists and recreate
						RelationshipFactory.deleteRelationship(aRel);
						Relationship rel = relationshipWrapper.getRelationship();
						RelationshipFactory.saveRelationship(rel,rel.getInode());
					}
				}
			}
		}catch(Exception e){
			throw new DotPublishingException(e.getMessage(),e);
		}
	}

	
}
