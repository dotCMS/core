package com.dotcms.publishing.sitesearch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.codec.digest.DigestUtils;

import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.bundlers.BundlerUtil;
import com.dotcms.publishing.bundlers.FileAssetBundler;
import com.dotcms.publishing.bundlers.FileAssetWrapper;
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
				APILocator.getSiteSearchAPI().createSiteSearchIndex(myConf.getIndexName(), 0);
			} catch (Exception e) {
				throw new DotPublishingException(e.getMessage());
			}
		}

		return this.config;

	}

	@Override
	public PublisherConfig process() throws DotPublishingException {
		try {
			File bundleRoot = BundlerUtil.getBundleRoot(config);
			int numThreads = config.getAdditionalThreads() + 1;
			ExecutorService executor = Executors.newFixedThreadPool(numThreads);
			for (final IBundler b : config.getBundlers()) {

				List<File> files = FileUtil.listFilesRecursively(bundleRoot, b.getFileFilter());
				List<List<File>> listsOfFiles = Lists.partition(files, 10);

				for (final List<File> list : listsOfFiles) {

					Runnable worker = new Runnable() {
						@Override
						public void run() {
							if (b instanceof FileAssetBundler) {
								processFileObjects(list);
							} else if (b instanceof URLMapBundler) {
								processUrlMaps(list);

							}

						}
					};

					executor.execute(worker);

				}

			}
			executor.shutdown();

			return config;
		} catch (Exception e) {
			throw new DotPublishingException(e.getMessage());

		}
	}

	private void processFileObjects(List<File> files) {
		for (File f : files) {
			try {
				if (!f.isDirectory()) {
					processFileObject(f);
				}

			} catch (IOException e) {
				Logger.info(this.getClass(), "failed: " + f + " : " + e.getMessage());

			}
		}

	}

	private void processUrlMaps(List<File> files) {
		for (File f : files) {
			try {
				if (!f.isDirectory()) {
					processUrlMap(f);
				}

			} catch (Exception e) {
				Logger.info(this.getClass(), "failed: " + f + " : " + e.getMessage());

			}
		}

	}

	private void processUrlMap(File file) throws DotPublishingException {
		String docId=null;
		try {
		URLMapWrapper wrap = (URLMapWrapper) BundlerUtil.xmlToObject(file);
		if (wrap == null)
			return;
		
		File htmlFile = new File(file.getAbsolutePath().replaceAll(URLMapBundler.FILE_ASSET_EXTENSION, ""));
		String uri =getUriFromFilePath(htmlFile);
		Host h = APILocator.getHostAPI().find(wrap.getContent().getHost(), APILocator.getUserAPI().getSystemUser(), true);
		
		docId=DigestUtils.md5Hex(h.getIdentifier()+ uri);
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
					

					res.setHost(h.getIdentifier());

					res.setUri(getUriFromFilePath(htmlFile));
					res.setUrl(getUrlFromFilePath(htmlFile));

					res.setId(docId);

					APILocator.getSiteSearchAPI().putToIndex(((SiteSearchConfig) config).getIndexName(), res);
				}
				
				
				

		//if this is the deleted guy
			
		} else if (!UtilMethods.isSet(wrap.getInfo().getLiveInode())) {
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
		if (wrap == null){
			return;
		}
		FileAsset asset = wrap.getAsset();
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

					String md5 = DigestUtils.md5Hex(asset.getHost() + res.getUri());
					res.setId(md5);

					String x = asset.getMetaData();
					if (x != null) {

						Map<String, Object> m = com.dotmarketing.portlets.structure.model.KeyValueFieldUtil.JSONValueToHashMap(x);

						res.setAuthor((String) m.get("author"));
						res.setDescription((String) m.get("description"));
						res.setContent((String) m.get("content"));

					}

					APILocator.getSiteSearchAPI().putToIndex(((SiteSearchConfig) config).getIndexName(), res);

					Logger.info(this.getClass(), "adding: " + asset.getPath() + asset.getFileName());
				} catch (Exception e) {
					throw new DotPublishingException(e.getMessage());

				}
			} catch (Exception e) {
				Logger.error(this.getClass(), "site search indexPut failed: " + e.getMessage());
			}
			
			
			//if we need to delete
		} else if (!UtilMethods.isSet(wrap.getInfo().getLiveInode())) {
			String url = asset.getHost() + asset.getPath() + asset.getFileName();
			Logger.info(this.getClass(), "delete: " + url);
			String md5 = DigestUtils.md5Hex(url);

			APILocator.getSiteSearchAPI().deleteFromIndex(((SiteSearchConfig) config).getIndexName(), md5);
		}

	}


	
	
	
	


	@Override
	public List<Class> getBundlers() {
		List<Class> list = new ArrayList<Class>();

		list.add(FileAssetBundler.class);
		// list.add(StaticHTMLPageBundler.class);
		list.add(URLMapBundler.class);
		return list;
	}

}
