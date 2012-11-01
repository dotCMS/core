package com.dotcms.publisher.myTest;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.*;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publishing.*;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

public class PushPublisherBundler implements IBundler {
	private PushPublisherConfig config;
	private User systemUser;
	ContentletAPI conAPI = null;
	UserAPI uAPI = null;
	PublisherAPI pubAPI = null;
	PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();

	@Override
	public String getName() {
		return "Push publisher bundler";
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
			Logger.fatal(PushPublisherBundler.class,e.getMessage(),e);
		}
	}

	@Override
	public void generate(File bundleRoot, BundlerStatus status)
			throws DotBundleException {
		if(LicenseUtil.getLevel()<200)
	        throw new RuntimeException("need an enterprise license to run this bundler");



		List<Contentlet> cs = new ArrayList<Contentlet>();

		PublishAuditHistory currentStatusHistory = null;
		try {
			//Updating audit table
			currentStatusHistory =
					PublishAuditHistory.getObjectFromString(
							(String)pubAuditAPI.getPublishAuditStatus(
									config.getId()).get("status_pojo"));
			currentStatusHistory.setBundleStart(new Date());
			pubAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.BUNDLING, currentStatusHistory);


			for(String luceneQuery: config.getLuceneQueries()) {
				cs = conAPI.search(luceneQuery, 0, 0, "moddate", systemUser, false);


				for (Contentlet con : cs) {
					writeFileToDisk(bundleRoot, con);
					status.addCount();
				}
			}

			//Updating audit table
			currentStatusHistory =
					PublishAuditHistory.getObjectFromString(
							(String)pubAuditAPI.getPublishAuditStatus(
									config.getId()).get("status_pojo"));
			currentStatusHistory.setBundleEnd(new Date());
			pubAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.BUNDLING, currentStatusHistory);

		} catch (Exception e) {
			try {
				pubAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.FAILED_TO_BUNDLE, currentStatusHistory);
			} catch (DotPublisherException e1) { }
			status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}
	}

	private void writeFileToDisk(File bundleRoot, Contentlet con)
			throws IOException, DotBundleException, DotDataException,
				DotSecurityException, DotPublisherException
	{
		Calendar cal = Calendar.getInstance();
		File pushContentFile = null;
		Host h = null;

		//Populate wrapper
		ContentletVersionInfo info = APILocator.getVersionableAPI().getContentletVersionInfo(con.getIdentifier(), con.getLanguageId());
		h = APILocator.getHostAPI().find(con.getHost(), APILocator.getUserAPI().getSystemUser(), true);

		PushContentWrapper wrapper=new PushContentWrapper();
	    wrapper.setContent(con);
		wrapper.setInfo(info);
		wrapper.setId(APILocator.getIdentifierAPI().find(con.getIdentifier()));
		wrapper.setTags(APILocator.getTagAPI().getTagsByInode(con.getInode()));
		wrapper.setOperation(config.getOperation());

		//Find MultiTree
		wrapper.setMultiTree(pubAPI.getContentMultiTreeMatrix(con.getIdentifier()));

        //Find Tree
        List<Map<String, Object>> contentTreeMatrix = pubAPI.getContentTreeMatrix( con.getIdentifier() );
        //Now add the categories, we will find categories by inode NOT by identifier
        contentTreeMatrix.addAll( pubAPI.getContentTreeMatrix( con.getInode() ) );
        wrapper.setTree( contentTreeMatrix );

		//Copy asset files to bundle folder keeping original folders structure
		List<Field> fields=FieldsCache.getFieldsByStructureInode(con.getStructureInode());
		File assetFolder = new File(bundleRoot.getPath()+File.separator+"assets");
		String inode=con.getInode();
		for(Field ff : fields) {
			if(ff.getFieldType().toString().equals(Field.FieldType.BINARY.toString())) {
				File sourceFile = con.getBinary( ff.getVelocityVarName());

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


		String liveworking = con.isLive() ? "live" :  "working";

		String assetName = APILocator.getFileAssetAPI().isFileAsset(con)?(File.separator + con.getIdentifier() + "." + "content"):APILocator.getIdentifierAPI().find(con).getURI().replace("/", File.separator);

		String myFileUrl = bundleRoot.getPath() + File.separator
				+liveworking + File.separator
				+ h.getHostname() + File.separator
				+ config.getLanguage() + assetName;

		pushContentFile = new File(myFileUrl);
		pushContentFile.mkdirs();

		BundlerUtil.objectToXML(wrapper, pushContentFile, true);
		pushContentFile.setLastModified(cal.getTimeInMillis());
	}

	@Override
	public FileFilter getFileFilter() {
		// TODO Auto-generated method stub
		return null;
	}

}
