package com.dotcms.enterprise.publishing.remote.bundler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publisher.pusher.wrapper.RelationshipWrapper;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PushPublishLogger;

public class RelationshipBundler implements IBundler {

	private PushPublisherConfig config;
	public final static String RELATIONSHIP_EXTENSION = ".relationship.xml" ;

	@Override
	public String getName() {
		return "Relationship Bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		config = (PushPublisherConfig)pc;

	}

    @Override
    public void setPublisher(IPublisher publisher) {
    }

	@Override
	public void generate(File bundleRoot, BundlerStatus status) throws DotBundleException {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this bundler");

		Set<String> relationships = config.getRelationships();

		try {
			for(String relInode : relationships){
				PushPublishLogger.log(getClass(), "Relationship INODE: " + relInode);
				Relationship rel = rel = APILocator.getRelationshipAPI().byInode(relInode);
				writeRelationship(bundleRoot, rel, config.getOperation());
			}
		}catch(Exception e){
			status.addFailure();
			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}

	}


	private void writeRelationship(File bundleRoot, Relationship relationship, Operation op)
			throws IOException, DotBundleException, DotDataException, DotSecurityException, DotPublisherException {
		RelationshipWrapper wrapper = new RelationshipWrapper(relationship, op);
		String liveworking = relationship.isLive() ? "live" :  "working";

		String uri = relationship.getInode();
		if(!uri.endsWith(RELATIONSHIP_EXTENSION)){
			uri.replace(RELATIONSHIP_EXTENSION, "");
			uri.trim();
			uri += RELATIONSHIP_EXTENSION;
		}

		Structure parent = CacheLocator.getContentTypeCache().getStructureByInode(relationship.getParentStructureInode());

		Host h = APILocator.getHostAPI().find(parent.getHost(), APILocator.getUserAPI().getSystemUser(), false);

		String myFileUrl = bundleRoot.getPath() + File.separator
				+liveworking + File.separator
				+ h.getHostname() +File.separator + uri;

		File strFile = new File(myFileUrl);
		strFile.mkdirs();

		BundlerUtil.objectToXML(wrapper, strFile, true);
		strFile.setLastModified(Calendar.getInstance().getTimeInMillis());

		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Relationship bundled for pushing. Operation: "+config.getOperation()+", Id: "+ relationship.getInode(), config.getId());
		}
	}

	@Override
	public FileFilter getFileFilter(){
		return new RelationshipBundlerFilter();
	}

	public class RelationshipBundlerFilter implements FileFilter {

		@Override
		public boolean accept(File pathname) {
			return (pathname.isDirectory() || pathname.getName().endsWith(RELATIONSHIP_EXTENSION));
		}

	}

}
