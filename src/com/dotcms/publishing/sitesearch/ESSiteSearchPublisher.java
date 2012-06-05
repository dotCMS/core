package com.dotcms.publishing.sitesearch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.bundlers.FileAssetBundler;
import com.dotcms.publishing.bundlers.FileAssetWrapper;
import com.dotcms.publishing.bundlers.HTMLPageWrapper;
import com.dotcms.publishing.bundlers.StaticHTMLPageBundler;
import com.dotcms.publishing.bundlers.URLMapBundler;
import com.dotcms.publishing.bundlers.URLMapWrapper;
import com.dotcms.tika.TikaUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Lists;
import com.liferay.util.FileUtil;

public class ESSiteSearchPublisher extends Publisher {

	public static final String SITE_SEARCH_INDEX = "SITE_SEARCH_INDEX";

	@Override
	public PublisherConfig init(PublisherConfig config) throws DotPublishingException {
		this.config = super.init(config);
		SiteSearchConfig myConf = (SiteSearchConfig) config;

		// if we don't specify an index, use the current one
		if (myConf.getIndexName() == null) {
			try {
				String index = APILocator.getIndiciesAPI().loadIndicies().site_search;
				if (index != null) {
					myConf.setIndexName(index);
					this.config = myConf;
				} else {
					throw new DotPublishingException("Active Site Search Index:null");
				}
			} catch (Exception e) {
				throw new DotPublishingException(
						"You must either specify a valid site search index in your PublishingConfig or have current active site search index.  Make sure you have a current active site search index by going to the site search admin portlet and creating one");
			}

		}
		if (UtilMethods.isSet(myConf.getIndexName()) && !APILocator.getESIndexAPI().indexExists(myConf.getIndexName())) {
			try {
				APILocator.getSiteSearchAPI().createSiteSearchIndex(myConf.getIndexName(), "alias_"+myConf.getIndexName(), 0);
			} catch (Exception e) {
				throw new DotPublishingException(e.getMessage());
			}
		}

		return this.config;

	}

	@Override
	public PublisherConfig process(final PublishStatus status) throws DotPublishingException {
		try {
			File bundleRoot = BundlerUtil.getBundleRoot(config);
			// int numThreads = config.getAdditionalThreads() + 1;
			// ExecutorService executor =
			// Executors.newFixedThreadPool(numThreads);
			for (final IBundler b : config.getBundlers()) {

				List<File> files = FileUtil.listFilesRecursively(bundleRoot, b.getFileFilter());
				List<File> filteredFiles = new ArrayList<File>();
				for (File f : files) {
					
					
					
					
					
					
					if (shouldProcess(f)) {
						
						System.out.println("######### processing: "  + f);
						
						filteredFiles.add(f);
					}else {
						System.out.println("######### skipping  : "  + f);
					}

				}
				files = filteredFiles;
				filteredFiles = null;

				Collections.sort(files, new FileDateSortComparator());

				List<List<File>> listsOfFiles = Lists.partition(files, 10);

				for (final List<File> list : listsOfFiles) {

					// Runnable worker = new Runnable() {
					// @Override
					// public void run() {
					if (b instanceof FileAssetBundler) {
						processFileObjects(list, status);
					} else if (b instanceof URLMapBundler) {
						processUrlMaps(list, status);

					} else if (b instanceof StaticHTMLPageBundler) {
						processHTMLPages(list, status);

					}
				}
				// };
				// executor.execute(worker);
				// }
			}
			/*
			 * executor.shutdown(); Logger.info(this.getClass(),
			 * "Waiting for ES Publishing threads to complete"); try { // The
			 * tasks are now running concurrently. We wait until all work is
			 * done, // with a timeout of 1 day : boolean b =
			 * executor.awaitTermination(24, TimeUnit.HOURS); // If the
			 * execution timed out, false is returned:
			 * Logger.info(this.getClass(), "All ES Publishing threads done"); }
			 * catch (InterruptedException e) { Logger.error(this.getClass(),
			 * "ES Publishing threads failed to complete", e);
			 * 
			 * }
			 */

			if (((SiteSearchConfig) config).switchIndexWhenDone()) {
				APILocator.getSiteSearchAPI().activateIndex(((SiteSearchConfig) config).getIndexName());

			}

			return config;

		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotPublishingException(e.getMessage());

		}
	}

	private void processFileObjects(List<File> files, PublishStatus status) {
		for (File f : files) {
			try {

				processFileObject(f);
				status.addCurrentProgress();

			} catch (IOException e) {
				Logger.info(this.getClass(), "failed: " + f + " : " + e.getMessage());

			}
		}

	}

	private void processUrlMaps(List<File> files, PublishStatus status) {
		for (File f : files) {
			try {

				processUrlMap(f);
				status.addCurrentProgress();

			} catch (Exception e) {
				Logger.info(this.getClass(), "failed: " + f + " : " + e.getMessage());

			}
		}

	}

	private void processHTMLPages(List<File> files, PublishStatus status) {
		for (File f : files) {
			try {

				processHTMLPage(f);
				status.addCurrentProgress();

			} catch (Exception e) {
				Logger.info(this.getClass(), "failed: " + f + " : " + e.getMessage());

			}
		}

	}

	private void processUrlMap(File file) throws DotPublishingException {
		String docId = null;
		try {
			URLMapWrapper wrap = (URLMapWrapper) BundlerUtil.xmlToObject(file);
			if (wrap == null)
				return;

			File htmlFile = new File(file.getAbsolutePath().replaceAll(URLMapBundler.FILE_ASSET_EXTENSION, ""));

			docId = wrap.getId().getId();

			// is the live guy
			if (UtilMethods.isSet(wrap.getInfo().getLiveInode()) && wrap.getInfo().getLiveInode().equals(wrap.getContent().getInode())) {
				Map<String, String> map = new TikaUtils().getMetaDataMap(htmlFile, "text/html");

				if (map != null) {

					SiteSearchResult res = new SiteSearchResult(map);

					// map contains [fileSize, content, author, title, keywords,
					// description, contentType, contentEncoding]

					res.setContentLength(htmlFile.length());
					res.setMimeType(map.get("contentType"));
					res.setFileName(htmlFile.getName().replaceAll(".dotUrlMap", ""));

					res.setModified(wrap.getInfo().getVersionTs());
					res.setUri(getUriFromFilePath(htmlFile));
					res.setUrl(getUrlFromFilePath(htmlFile));

					res.setId(docId);

					APILocator.getSiteSearchAPI().putToIndex(((SiteSearchConfig) config).getIndexName(), res);
				}

				// if this is the deleted guy
			} else if (!UtilMethods.isSet(wrap.getInfo().getLiveInode())) {
				APILocator.getSiteSearchAPI().deleteFromIndex(((SiteSearchConfig) config).getIndexName(), docId);

			}

		} catch (Exception e) {

			Logger.error(this.getClass(), "site search  failed: " + docId + " error" + e.getMessage());
		}

	}

	private void processHTMLPage(File file) throws DotPublishingException {
		String docId = null;
		try {
			HTMLPageWrapper wrap = (HTMLPageWrapper) BundlerUtil.xmlToObject(file);
			if (wrap == null)
				return;

			File htmlFile = new File(file.getAbsolutePath().replaceAll(StaticHTMLPageBundler.HTML_ASSET_EXTENSION, ""));

			Host h = APILocator.getHostAPI().find(wrap.getIdentifier().getHostId(), APILocator.getUserAPI().getSystemUser(), true);
			docId = wrap.getIdentifier().getId()+"_"+wrap.getLanguageId();

			// is the live guy
			if (UtilMethods.isSet(wrap.getVersionInfo().getLiveInode())
					&& wrap.getVersionInfo().getLiveInode().equals(wrap.getPage().getInode())) {

				Map<String, String> map = new TikaUtils().getMetaDataMap(htmlFile, "text/html");

				if (map != null) {

					SiteSearchResult res = new SiteSearchResult(map);

					// map contains [fileSize, content, author, title, keywords,
					// description, contentType, contentEncoding]

					res.setContentLength(htmlFile.length());
					res.setMimeType(map.get("contentType"));
					res.setFileName(htmlFile.getName().replaceAll(".dotUrlMap", ""));
					res.setModified(wrap.getVersionInfo().getVersionTs());

					res.setHost(h.getIdentifier());

					res.setUri(getUriFromFilePath(htmlFile));
					res.setUrl(getUrlFromFilePath(htmlFile));

					res.setId(docId);

					APILocator.getSiteSearchAPI().putToIndex(((SiteSearchConfig) config).getIndexName(), res);
				}

				// if this is the deleted guy
			} else if (!UtilMethods.isSet(wrap.getVersionInfo().getLiveInode())) {
				APILocator.getSiteSearchAPI().deleteFromIndex(((SiteSearchConfig) config).getIndexName(), docId);

			}

		} catch (Exception e) {
			Logger.error(this.getClass(), "site search  failed: " + docId + " error" + e.getMessage());
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

					res.setId(docId);

					String x = asset.getMetaData();
					if (x != null) {

						Map<String, Object> m = com.dotmarketing.portlets.structure.model.KeyValueFieldUtil.JSONValueToHashMap(x);

						res.setAuthor((String) m.get("author"));
						res.setDescription((String) m.get("description"));
						res.setContent((String) m.get("content"));

					}

					APILocator.getSiteSearchAPI().putToIndex(((SiteSearchConfig) config).getIndexName(), res);

				} catch (Exception e) {
					throw new DotPublishingException(e.getMessage());

				}
			} catch (Exception e) {
				Logger.error(this.getClass(), "site search indexPut failed: " + e.getMessage());
			}

			// if we need to delete
		} else if (!UtilMethods.isSet(wrap.getInfo().getLiveInode())) {
			String url = asset.getHost() + asset.getPath() + asset.getFileName();

			APILocator.getSiteSearchAPI().deleteFromIndex(((SiteSearchConfig) config).getIndexName(), docId);
		}

	}
	@Override
	public List<Class> getBundlers() {
		List<Class> list = new ArrayList<Class>();

		list.add(FileAssetBundler.class);
		list.add(StaticHTMLPageBundler.class);
		list.add(URLMapBundler.class);
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
