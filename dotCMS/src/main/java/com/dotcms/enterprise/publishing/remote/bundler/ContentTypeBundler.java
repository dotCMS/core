package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.contenttype.transform.field.FieldVariableTransformer;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publisher.pusher.wrapper.ContentTypeWrapper;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.liferay.portal.model.User;

public class ContentTypeBundler implements IBundler {
	private PushPublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	PublisherAPI pubAPI = null;
	PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	FolderAPI fAPI = APILocator.getFolderAPI();

	public final static String CONTENT_TYPE_EXTENSION = ".contentType.json" ;

	@Override
	public String getName() {
		return "ContentType bundler";
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
			Logger.fatal(FolderBundler.class,e.getMessage(),e);
		}
	}

	@Override
	public void generate(File bundleRoot, BundlerStatus status) throws DotBundleException {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level)
	        throw new RuntimeException("need an enterprise pro license to run this bundler");

		Set<String> structures = config.getStructures();

		try {
			for (String str : structures) {
				Structure s = CacheLocator.getContentTypeCache().getStructureByInode(str);

				writeContentType(bundleRoot, s, config.getOperation());
			}
		} catch (Exception e) {
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}

	}



	private void writeContentType(File bundleRoot, Structure structure, Operation op)
			throws IOException, DotBundleException, DotDataException,
			DotSecurityException, DotPublisherException
	{
		ContentType contentType = new StructureTransformer(structure).from();

		List<Field> fields = new LegacyFieldTransformer(FieldsCache.getFieldsByStructureInode(structure.getInode())).asList();

		List<FieldVariable> fieldVariables=new ArrayList<FieldVariable>();
        for(Field ff : fields) {
            fieldVariables.addAll(
                new FieldVariableTransformer(
                	APILocator.getFieldAPI().getFieldVariablesForField(ff.inode(), APILocator.getUserAPI().getSystemUser(), false)
                ).newFieldList()
            );
        }

		ContentTypeWrapper wrapper = new ContentTypeWrapper(contentType,fields,fieldVariables);
		
		wrapper.setOperation(op);
		List<WorkflowScheme> schemes = APILocator.getWorkflowAPI().findSchemesForStruct(structure);
		final ImmutableList.Builder<String> schemeIds = new ImmutableList.Builder<>();
		final ImmutableList.Builder<String> schemeNames = new ImmutableList.Builder<>();
        for(WorkflowScheme scheme : schemes){
			schemeIds.add(scheme.getId());
			schemeNames.add(scheme.getName());
		}
		wrapper.setWorkflowSchemaIds(schemeIds.build());
		wrapper.setWorkflowSchemaNames(schemeNames.build());
		
		String liveworking = structure.isLive() ? "live" :  "working";

		String uri = structure.getInode();
		if(!uri.endsWith(CONTENT_TYPE_EXTENSION)){
			uri.replace(CONTENT_TYPE_EXTENSION, "");
			uri.trim();
			uri += CONTENT_TYPE_EXTENSION;
		}

		Host h = APILocator.getHostAPI().find(structure.getHost(), systemUser, false);

		String myFileUrl = bundleRoot.getPath() + File.separator
				+liveworking + File.separator
				+ h.getHostname() +File.separator + uri;

		File strFile = new File(myFileUrl);
		strFile.mkdirs();

		BundlerUtil.objectToJSON(wrapper, strFile);
		strFile.setLastModified(Calendar.getInstance().getTimeInMillis());

		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Content Type bundled for pushing. Operation: "+config.getOperation()+", Id: "+ structure.getInode(), config.getId());
		}
	}

	@Override
	public FileFilter getFileFilter(){
		return new FolderBundlerFilter();
	}

	public class FolderBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(CONTENT_TYPE_EXTENSION));
		}

	}

	@Override
	public void setPublisher(IPublisher publisher) {
		// TODO Auto-generated method stub
		
	}
}
