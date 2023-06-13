package com.dotcms.enterprise.publishing.sitesearch;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.bundlers.*;
import com.dotcms.publishing.*;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.tika.TikaUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ESSiteSearchPublisher extends Publisher {

	public static final String SITE_SEARCH_INDEX = "SITE_SEARCH_INDEX";

	@Override
	public PublisherConfig init(PublisherConfig config) throws DotPublishingException {
	    if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            throw new RuntimeException("need an enterprise licence to run this");
	    
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
				throw new DotPublishingException(e.getMessage(),e);
			}
		}

		return this.config;

	}

	@Override
	public PublisherConfig process(final PublishStatus status) throws DotPublishingException {

		if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            throw new RuntimeException("need an enterprise licence to run this");
	    
		try {
			File bundleRoot = BundlerUtil.getBundleRoot(config);
			for (Class<IBundler> clazz : getBundlers()) {
				IBundler bundler = clazz.newInstance();

				List<File> files = FileUtil.listFilesRecursively(bundleRoot, bundler.getFileFilter());
				List<File> filteredFiles = new ArrayList<>();

				for (File f : files) {
					if (shouldProcess(f)) {
						Logger.debug(this, "######### processing: "  + f);
						filteredFiles.add(f);
					} else {
						Logger.debug(this, "######### skipping  : "  + f);
					}
				}
				files = filteredFiles;

				Collections.sort(files, new FileDateSortComparator());

				//Returns consecutive sublists of a list, each of the same size (10).
				List<List<File>> listsOfFiles = Lists.partition(files, 10);

				for (final List<File> list : listsOfFiles) {
					if (bundler instanceof FileAssetBundler) {
						processFileObjects(list, status);
					} else if (bundler instanceof URLMapBundler) {
						processUrlMaps(list, status);

					} else if (bundler instanceof HTMLPageAsContentBundler) {
						processHTMLPagesAsContent(list, status);
					}
				}
			}

			if (((SiteSearchConfig) config).switchIndexWhenDone()) {

				final String oldDefault=APILocator.getIndiciesAPI().loadIndicies().site_search;
				APILocator.getSiteSearchAPI().activateIndex(((SiteSearchConfig) config).getIndexName());

				// there is no previous search index
				if(null != oldDefault){
					new Thread(new Runnable() {
					 	// https://github.com/dotCMS/dotCMS/issues/694
	                    public void run() {
	                        try {
	                            // wait a bit while users stop using oldDefault index
	                            // and see the new default index
	                            Thread.sleep(5000L);
	                        }
	                        catch(InterruptedException ex) {
	                            Logger.warn(this, ex.getMessage(),ex);
	                        }
	                        
	                        APILocator.getESIndexAPI().delete(oldDefault);
	                        
	                    }
	                }).start();
				} else{
					Logger.debug(this.getClass(),"No previous search index to delete.");
				}
			}

			return config;

		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotPublishingException(e.getMessage(),e);
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
	
	private void processHTMLPagesAsContent(List<File> files, PublishStatus status) {
		for (File f : files) {
			try {

				processHTMLPageAsContent(f);
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
				Map<String, String> map = new TikaUtils()
						.getMetaDataMapForceMemory(wrap.getContent().getInode(), htmlFile);

				if (map != null) {
					if(map.get("content") != null){
						map.put("content", ((String) map.get("content")).replaceAll("\\s+", " "));
					}

					if (!UtilMethods.isSet(map.get("title")) && UtilMethods.isSet(wrap.getContent().getTitle())){
						map.put("title", wrap.getContent().getTitle());
					}
					
					SiteSearchResult res = new SiteSearchResult(map);

					// map contains [fileSize, content, author, title, keywords,
					// description, contentType, contentEncoding]

					res.setContentLength(htmlFile.length());
					res.setLanguage(wrap.getContent().getLanguageId());
					res.setMimeType(map.get("contentType"));
					res.setFileName(htmlFile.getName().replaceAll(".dotUrlMap", ""));

					res.setModified(wrap.getInfo().getVersionTs());
					res.setUri(getUriFromFilePath(htmlFile));
					res.setUrl(getUrlFromFilePath(htmlFile));
					res.setHost(getHostFromFilePath(htmlFile).getIdentifier());
					res.setId(docId);

					APILocator.getSiteSearchAPI().putToIndex(((SiteSearchConfig) config).getIndexName(), res, "URLMap");
				}

				// if this is the deleted guy
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
		if (wrap == null) {
			return;
		}
		FileAsset asset = wrap.getAsset();
		String docId = wrap.getId().getId();

		// is the live guy
		if (UtilMethods.isSet(wrap.getInfo().getLiveInode()) && !wrap.getInfo().isDeleted() 
		        && wrap.getInfo().getLiveInode().equals(wrap.getAsset().getInode())) {
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

					res.setMimeType(APILocator.getFileAssetAPI().getMimeType(asset.getFileName()));
					res.setModified(asset.getModDate());
					res.setModified(wrap.getInfo().getVersionTs());

					res.setLanguage(asset.getLanguageId());
					
					res.setId(docId);

					String x = asset.getMetaData();
					if (x != null) {

						Map<String, Object> m = com.dotmarketing.portlets.structure.model.KeyValueFieldUtil.JSONValueToHashMap(x);

						res.setAuthor((String) m.get("author"));
						res.setDescription((String) m.get("description"));
						
						File contentMeta=APILocator.getFileAssetAPI().getContentMetadataFile(asset.getInode());
						if(contentMeta.exists() && contentMeta.length()>0) {
		                    String contentData=APILocator.getFileAssetAPI().getContentMetadataAsString(contentMeta);
		                    res.setContent(contentData);
						}
						else {
						    res.setContent((String) m.get("content"));
						}

					}

					APILocator.getSiteSearchAPI().putToIndex(((SiteSearchConfig) config).getIndexName(), res, "FileObject");

				} catch (Exception e) {
					throw new DotPublishingException(e.getMessage(),e);

				}
			} catch (Exception e) {
				Logger.error(this.getClass(), "site search indexPut failed: " + e.getMessage());
			}

			// if we need to delete
		} else {
			APILocator.getSiteSearchAPI().deleteFromIndex(((SiteSearchConfig) config).getIndexName(), docId);
		}

	}
	
	private void processHTMLPageAsContent(File file) throws DotPublishingException {
		String docId = null;
		try {
			HTMLPageAsContentWrapper wrap = (HTMLPageAsContentWrapper) BundlerUtil.xmlToObject(file);
			if (wrap == null)
				return;

			File htmlFile = new File(file.getAbsolutePath().replaceAll(HTMLPageAsContentBundler.HTMLPAGE_ASSET_EXTENSION, ""));
			
			IHTMLPage page = wrap.getAsset();

			Host h = APILocator.getHostAPI().find(wrap.getId().getHostId(), APILocator.getUserAPI().getSystemUser(), true);
			docId = wrap.getId().getId()+"_"+((Contentlet)page).getLanguageId();

			// is the live guy
			if (UtilMethods.isSet(wrap.getInfo().getLiveInode()) && !wrap.getInfo().isDeleted() 
			        && wrap.getInfo().getLiveInode().equals(wrap.getAsset().getInode())) {

				Map<String, String> map = new TikaUtils()
						.getMetaDataMapForceMemory(page.getInode(), htmlFile);

				if (map != null) {
					if(map.get("content") != null){
						map.put("content", ((String) map.get("content")).replaceAll("\\s+", " "));
					}
					SiteSearchResult res = new SiteSearchResult(map);
					res.setLanguage(((Contentlet)page).getLanguageId());
					// map contains [fileSize, content, author, title, keywords,
					// description, contentType, contentEncoding]
					res.setTitle(page.getTitle());

					res.setContentLength(htmlFile.length());
					res.setMimeType(map.get("contentType"));
					res.setFileName(htmlFile.getName().replaceAll(".dotUrlMap", ""));
					res.setModified(wrap.getInfo().getVersionTs());

					res.setHost(h.getIdentifier());

					res.setUri(getUriFromFilePath(htmlFile));
					res.setUrl(getUrlFromFilePath(htmlFile));

					res.setId(docId);

					APILocator.getSiteSearchAPI().putToIndex(((SiteSearchConfig) config).getIndexName(), res, "HTMLPageAsContent");
				}

				// if this is the deleted guy
			} else if (!UtilMethods.isSet(wrap.getInfo().getLiveInode())) {
				APILocator.getSiteSearchAPI().deleteFromIndex(((SiteSearchConfig) config).getIndexName(), docId);

			}

		} catch (Exception e) {
			Logger.error(this.getClass(), "site search  failed: " + docId + " error" + e.getMessage());
		}

	}
	@Override
	public List<Class> getBundlers() {
		List<Class> list = new ArrayList<Class>();

		list.add(FileAssetBundler.class);
		list.add(URLMapBundler.class);
		list.add(HTMLPageAsContentBundler.class);
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
