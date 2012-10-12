package com.dotcms.publisher.myTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.publishing.bundlers.FileAssetWrapper;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResult;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Lists;
import com.liferay.util.FileUtil;

public class PushPublisher extends Publisher {

	public static final String SITE_SEARCH_INDEX = "SITE_SEARCH_INDEX";

	@Override
	public PublisherConfig init(PublisherConfig config) throws DotPublishingException {
	    if(LicenseUtil.getLevel()<200)
            throw new RuntimeException("need an enterprise licence to run this");
	    
		this.config = super.init(config);
		PushPublisherConfig myConf = (PushPublisherConfig) config;

		// if we don't specify an index, use the current one
		if (myConf.getIndexName() == null) {
			try {
				String index = APILocator.getIndiciesAPI().loadIndicies().working;
				if (index != null) {
					myConf.setIndexName(index);
					this.config = myConf;
				} else {
					throw new DotPublishingException("Active Site Search Index:null");
				}
			} catch (Exception e) {
				throw new DotPublishingException(
						"You must either specify a valid site search index in your PublishingConfig or have current active site search index.  " +
						"Make sure you have a current active site search index by going to the site search admin portlet and creating one");
			}

		}

		return this.config;

	}

	@Override
	public PublisherConfig process(final PublishStatus status) throws DotPublishingException {
	    if(LicenseUtil.getLevel()<200)
            throw new RuntimeException("need an enterprise licence to run this");
	    
		try {
			File bundleRoot = BundlerUtil.getBundleRoot(config);
			// int numThreads = config.getAdditionalThreads() + 1;
			// ExecutorService executor =
			// Executors.newFixedThreadPool(numThreads);
			for (final IBundler b : config.getBundlers()) {

				List<File> files = FileUtil.listFilesRecursively(bundleRoot, b.getFileFilter());
				List<File> filteredFiles = new ArrayList<File>();
				for (File f : files) {
					
					
//					if (shouldProcess(f)) {
//						
//						Logger.debug(this, "######### processing: "  + f);
//						
//						filteredFiles.add(f);
//					}else {
//						Logger.debug(this, "######### skipping  : "  + f);
//					}

				}
				files = filteredFiles;
				filteredFiles = null;

				Collections.sort(files, new FileDateSortComparator());

				List<List<File>> listsOfFiles = Lists.partition(files, 10);

				for (final List<File> list : listsOfFiles) {

					// Runnable worker = new Runnable() {
					// @Override
					// public void run() {
					if (b instanceof PushPublisherBundler) {
						processFileObjects(list, status);
					}
				}
				// };
				// executor.execute(worker);
				// }
			}

			return config;

		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotPublishingException(e.getMessage());

		}
	}

	private void processFileObjects(List<File> files, PublishStatus status) {
		for (File f : files) {
//			try {
//
//				processFileObject(f);
//				status.addCurrentProgress();
//
//			} catch (IOException e) {
//				Logger.info(this.getClass(), "failed: " + f + " : " + e.getMessage());
//
//			}
		}

	}


	private void processFileObject(File file) throws IOException {

		// Logger.info(this.getClass(), "processing: " +
		// file.getAbsolutePath());

		FileAssetWrapper wrap = (FileAssetWrapper) BundlerUtil.xmlToObject(file);
		if (wrap == null) {
			return;
		}
		FileAsset asset = wrap.getAsset();
		String docId = wrap.getId().getId();

		// is the live guy
		if (UtilMethods.isSet(wrap.getInfo().getLiveInode()) && wrap.getInfo().getLiveInode().equals(wrap.getAsset().getInode())) {
			try {
				try {

					SiteSearchResult res = new SiteSearchResult();
					res.setContentLength(asset.getFileSize());
					res.setHost(asset.getHost());

					Host h;

					h = APILocator.getHostAPI().find(asset.getHost(), APILocator.getUserAPI().getSystemUser(), true);
					res.setUri(asset.getPath() + asset.getFileName());
					res.setUrl(h.getHostname() + res.getUri());
					res.setFileName(asset.getFileName());

					res.setMimeType(APILocator.getFileAPI().getMimeType(asset.getFileName()));
					res.setModified(asset.getModDate());
					res.setModified(wrap.getInfo().getVersionTs());

					res.setLanguage(config.getLanguage());
					
					res.setId(docId);

					String x = asset.getMetaData();
					if (x != null) {

						Map<String, Object> m = com.dotmarketing.portlets.structure.model.KeyValueFieldUtil.JSONValueToHashMap(x);

						res.setAuthor((String) m.get("author"));
						res.setDescription((String) m.get("description"));
						res.setContent((String) m.get("content"));

					}

					APILocator.getSiteSearchAPI().putToIndex(((PushPublisherConfig) config).getIndexName(), res);

				} catch (Exception e) {
					throw new DotPublishingException(e.getMessage());

				}
			} catch (Exception e) {
				Logger.error(this.getClass(), "site search indexPut failed: " + e.getMessage());
			}

			// if we need to delete
		} else if (!UtilMethods.isSet(wrap.getInfo().getLiveInode())) {
			String url = asset.getHost() + asset.getPath() + asset.getFileName();

			APILocator.getSiteSearchAPI().deleteFromIndex(((PushPublisherConfig) config).getIndexName(), docId);
		}

	}
	@Override
	public List<Class> getBundlers() {
		List<Class> list = new ArrayList<Class>();

		list.add(PushPublisherBundler.class);
		return list;
	}

	private class FileDateSortComparator implements Comparator<File> {

		@Override
		public int compare(File o1, File o2) {
			if (o1 == null || o2 == null) {
				return 0;
			} else if (o1.lastModified() > o2.lastModified()) {
				return 1;
			} else if (o1.lastModified() < o2.lastModified()) {
				return -1;
			} else {
				return 0;
			}
		}

	}

}
