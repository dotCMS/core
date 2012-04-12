package com.dotmarketing.sitesearch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * 
 * @author Roger
 *
 */
@SuppressWarnings("deprecation")
public class CrawlerUtil {
	
	
	public static final String SEARCH_INDEX_DIR = "search_index";
	
	public static final String CRAWLER_PLUGINS_DIR = Config.CONTEXT.getRealPath("WEB-INF"+Path.SEPARATOR+"crawler_plugins"+Path.SEPARATOR+"plugins");
	
//	private static final Configuration conf = NutchConfiguration.createCrawlConfiguration();
	
	private static final int threads = Config.getIntProperty("NUMBER_OF_THREADS",10);
	
	private static final int depth = Config.getIntProperty("LINK_DEPTH",10)==0?10:Config.getIntProperty("LINK_DEPTH");
	
	private static final long topN = Config.getIntProperty("MAX_PAGES_PER_LEVEL",0)==0?Long.MAX_VALUE:new Long(Config.getIntProperty("MAX_PAGES_PER_LEVEL")).longValue();
	
	private static final ConcurrentMap<String,HostIndexBean> indexedHosts = new ConcurrentHashMap<String, HostIndexBean>();
	
	private static final HostAPI hostAPI = APILocator.getHostAPI();
	
	private static final UserAPI userAPI = APILocator.getUserAPI();
	
	private static final FileAPI fileAPI = APILocator.getFileAPI();
	
//	static{
//		conf.set("plugin.folders", CRAWLER_PLUGINS_DIR);
//		conf.set("http.agent.name", Config.getStringProperty("SEARCH_AGENT_NAME"));
//		conf.set("http.agent.description", Config.getStringProperty("SEARCH_AGENT_DESC"));
//		conf.set("http.agent.url", Config.getStringProperty("SEARCH_AGENT_URL"));
//		conf.set("http.agent.email", Config.getStringProperty("SEARCH_AGENT_EMAIL"));
//
//	}

	private List<Host> hosts;
	
	private List<String> pathsToIgnore;
	
	private List<String> extensionsToIgnore;
	
	private boolean queryStringEnabled;
	
	private boolean followExternalLinks;
	
	private String portNumber;

	
	public boolean isQueryStringEnabled() {
		return queryStringEnabled;
	}

	public void setQueryStringEnabled(boolean queryStringEnabled) {
		this.queryStringEnabled = queryStringEnabled;
	}

	public List<String> getPathsToIgnore() {
		return pathsToIgnore;
	}

	public void setPathsToIgnore(List<String> pathsToIgnore) {
		this.pathsToIgnore = pathsToIgnore;
	}

	public List<Host> getHosts() {
		return hosts;
	}

	public void setHosts(List<Host> hosts) {
		this.hosts = hosts;
	}
	

	public List<String> getExtensionsToIgnore() {
		return extensionsToIgnore;
	}

	public void setExtensionsToIgnore(List<String> extensionsToIgnore) {
		this.extensionsToIgnore = extensionsToIgnore;
	}

	private static String getDate() {
		return new SimpleDateFormat("yyyyMMddHHmmss").format
		(new Date(System.currentTimeMillis()));
	}
	
	
	public boolean isIgnoreExternalLinks() {
		return followExternalLinks;
	}

	public void setIgnoreExternalLinks(boolean ignoreExternalLinks) {
		this.followExternalLinks = ignoreExternalLinks;
	}
	

	public String getPortNumber() {
		if(portNumber==null || portNumber.trim().equals("80")){ 
			return null;
		}else{
			return portNumber.trim();
		}
	}

	public void setPortNumber(String portNumber) {
		this.portNumber = portNumber;
	}

	/**
	 * Performs the index on the given hosts
	 */
	public void index(){
		for(Host host : getHosts()){
			try{
				indexHost(host);
			}catch(Exception e){
				Logger.error(this, "Error indexing host " + host.getHostname() + " caused by = " + e);
			}
		}
	}
	


	/**
	 * Performs complete crawling and indexing given a Host.
	 * @param host
	 * @throws Exception 
	 */
	private void indexHost(final Host host) throws Exception {

//		synchronized(host){
//
//			if(host==null){
//				throw new IllegalArgumentException("Host cannot be null");
//			}
//			AdminLogger.log(CrawlerUtil.class, "indexHost", "Starting an Site Search index on host " + host.getHostname());
//		
//			//If search index folder under assets does not exist, then create it.
//			File searchIndexFolder = new File(fileAPI.getRealAssetPath()+Path.SEPARATOR+SEARCH_INDEX_DIR);
//			if(!searchIndexFolder.getAbsoluteFile().exists()){
//				FileUtils.forceMkdir(searchIndexFolder);
//			}
//			File hostIndexFolder = null;
//			//Use Host name as default folder name
//			String hostFolderName = host.getHostname();
//			
//			hostIndexFolder = new File(searchIndexFolder.getAbsolutePath()+Path.SEPARATOR+hostFolderName);
//			
//			if(!hostIndexFolder.getAbsoluteFile().exists()){
//			
//			try{
//			   FileUtils.forceMkdir(hostIndexFolder);
//			}catch(IOException e){
//				//If an exception occurs then use the Host identifier   
//				Logger.warn(this,"Creating host site search index folder using host identifier " + host.getIdentifier());
//				hostIndexFolder = new File(searchIndexFolder.getAbsolutePath()+Path.SEPARATOR+host.getIdentifier());
//				if(!hostIndexFolder.getAbsoluteFile().exists()){
//					FileUtils.forceMkdir(hostIndexFolder);
//				}
//			  }
//			}
//			
//			//Create index folder for host
//			File realIndexFolder = new File(hostIndexFolder.getAbsolutePath()+Path.SEPARATOR+host.getIdentifier());
//			if(!realIndexFolder.getAbsoluteFile().exists()){
//				try{
//					FileUtils.forceMkdir(realIndexFolder);
//				}catch(IOException e){
//					Logger.error(this,"Error creating folder for index of host = " + host.getHostname(),e);
//					throw new DotRuntimeException(e.getMessage(), e);
//
//				}
//			}
//			
//			//Create temp folders
//			File tempHostIndexFolder = new File(hostIndexFolder.getAbsolutePath()+Path.SEPARATOR+host.getIdentifier()+"_temp");
//			if(tempHostIndexFolder.getAbsoluteFile().exists()){
//				FileUtils.forceDelete(tempHostIndexFolder);
//			}
//			try{
//				FileUtils.forceMkdir(tempHostIndexFolder);
//			}catch(IOException e){
//				Logger.error(this,"Error creating temp folder for index of host = " + host.getHostname(),e);
//				throw new DotRuntimeException(e.getMessage(), e);
//			}
//
//			
//			Path rootUrlDir = createSeedFolderFile(host,hostIndexFolder);
//
//			if(!followExternalLinks){
//				conf.set("db.ignore.external.links", "true");
//				
//			}
//			
//			if(!indexedHosts.isEmpty()){
//	    		final HostIndexBean bean = indexedHosts.get(host.getIdentifier());
//	    		if(bean!=null){
//	    			bean.setIndexing(true);
//	    		}
//	    	}else{
//	    		final HostIndexBean bean = getIndexedHostFromFS(host.getIdentifier());
//	    		if(bean!=null){
//	    			bean.setIndexing(true);
//	    			indexedHosts.putIfAbsent(host.getIdentifier(), bean);
//	    		}
//	    	}
//
//			createUrlCrawlFile(host,conf,hostIndexFolder.getAbsolutePath());
//			JobConf job = new NutchJob(conf);
//			Path dir = new Path(tempHostIndexFolder.getAbsolutePath()+Path.SEPARATOR+ "crawl-index");
//			String indexerName = "lucene";
//
//			try{
//				FileSystem fs = FileSystem.get(job);
//				
//				Logger.info(this,"site search crawl started in: " + dir);
//				Logger.info(this,"rootUrlDir = " + rootUrlDir);
//				Logger.info(this,"threads = " + threads);
//				Logger.info(this,"depth = " + depth);
//				Logger.info(this,"indexer=" + indexerName);
//				if (topN != Long.MAX_VALUE)
//				    Logger.info(this,"topN = " + topN);
//
//				Path crawlDb = new Path(dir + "/crawldb");
//				Path linkDb = new Path(dir + "/linkdb");
//				Path segments = new Path(dir + "/segments");
//				Path indexes = new Path(dir + "/indexes");
//				Path index = new Path(dir + "/index");
//
//				Path tmpDir = job.getLocalPath("crawl"+Path.SEPARATOR+getDate());
//				Logger.debug(this, "About to instaniate the Injector");
//				Injector injector = new Injector(conf);
//				Logger.debug(this, "About to instaniate the Generator");
//				Generator generator = new Generator(conf);
//				Logger.debug(this, "About to instaniate the Fetcher");
//				Fetcher fetcher = new Fetcher(conf);
//				Logger.debug(this, "About to instaniate the ParseSegment");
//				ParseSegment parseSegment = new ParseSegment(conf);
//				Logger.debug(this, "About to instaniate the CrawlDB");
//				CrawlDb crawlDbTool = new CrawlDb(conf);
//				Logger.debug(this, "About to instaniate the LinkDB");
//				LinkDb linkDbTool = new LinkDb(conf);
//				// initialize crawlDb
//				Logger.debug(this, "initializing the crawlDb");
//				injector.inject(crawlDb, rootUrlDir);
//				int i;
//				for (i = 0; i < depth; i++) {             // generate new segment
//					Path[] segs = generator.generate(crawlDb, segments, -1, topN, System
//							.currentTimeMillis());
//					if (segs == null) {
//						Logger.info(this,"Stopping at depth=" + i + " - no more URLs to fetch.");
//						break;
//					}
//					Logger.debug(this, "About to fetch into segment : " + segs[0].toUri().toString());
//					fetcher.fetch(segs[0], threads, org.apache.nutch.fetcher.Fetcher.isParsing(conf));  // fetch it
//					Logger.debug(this, "Done fetching into segment : " + segs[0].toUri().toString());
//					if (!Fetcher.isParsing(job)) {
//						parseSegment.parse(segs[0]);    // parse it, if needed
//					}
//					crawlDbTool.update(crawlDb, segs, true, true); // update crawldb
//				}
//				if (i > 0) {
//					linkDbTool.invert(linkDb, segments, true, true, false); // invert links
//
//					// index, dedup & merge
//					FileStatus[] fstats = fs.listStatus(segments, HadoopFSUtil.getPassDirectoriesFilter(fs));
//
//					DeleteDuplicates dedup = new DeleteDuplicates(conf);        
//					if(indexes != null) {
//						// Delete old indexes
//						if (fs.exists(indexes)) {
//							Logger.info(this,"Deleting old indexes: " + indexes);
//							fs.delete(indexes, true);
//						}
//
//						// Delete old index
//						if (fs.exists(index)) {
//							Logger.info(this,"Deleting old merged index: " + index);
//							fs.delete(index, true);
//						}
//					}
//
//					Indexer indexer = new Indexer(conf);
//					indexer.index(indexes, crawlDb, linkDb, 
//							Arrays.asList(HadoopFSUtil.getPaths(fstats)));
//
//					IndexMerger merger = new IndexMerger(conf);
//					if(indexes != null) {
//						dedup.dedup(new Path[] { indexes });
//						fstats = fs.listStatus(indexes, HadoopFSUtil.getPassDirectoriesFilter(fs));
//						Logger.debug(this, "Merging the index");
//						merger.merge(HadoopFSUtil.getPaths(fstats), index, tmpDir);
//					}
//
//				} else {
//					Logger.warn(this,"no URLs to fetch for host = " + host.getHostname());
//				}
//				Logger.info(this,"site search crawl finished: " + dir);
//			}catch(Exception e){
//				conf.set("urlfilter.regex.file", "crawl-urlfilter.txt");
//				Logger.error(this, e.getMessage(), e);
//				throw new DotRuntimeException(e.getMessage(), e);
//			}finally{
//				indexedHosts.replace(host.getIdentifier(), new HostIndexBean(realIndexFolder,false));
//			}
//			AdminLogger.log(CrawlerUtil.class, "indexHost", "Finished Site Search index on host " + host.getHostname());
//		}
	}


    /**
     * Creates the seed file of url's to crawl needed by hadoop for a given host.
     * @param host
     * @param hostIndexFolder
     * @return
     */
	private Path createSeedFolderFile(final Host host, final File hostIndexFolder){

		Writer writer = null;
		//Create seed file & folder from host
		File seedFolder = new File(hostIndexFolder.getAbsolutePath()+Path.SEPARATOR+Path.SEPARATOR+"url_folder");
		seedFolder.mkdir();
		File seedFile = new File(seedFolder.getAbsolutePath()+Path.SEPARATOR+"urls.txt");
		try{
			StringBuffer text = new StringBuffer();
			
			String hostName= host.getHostname().replaceAll("^A-Za-z0-9", "");
			
			if(hostName.contains("*")){
				Logger.error(this,"Error creating url seed file for host " + hostName + " host name is not valid ") ;
				throw new DotRuntimeException("Error creating url seed file for host " + hostName + " host name is not valid ");
			}
			
			if(!hostName.startsWith("http://")){
				hostName = "http://"+hostName;
			}
			if(UtilMethods.isSet( getPortNumber())){
				hostName += ":"+getPortNumber(); 
			}
			text.append(hostName.toLowerCase()+"\r\n"); 
						
			writer = new BufferedWriter(new FileWriter(seedFile));
			writer.write(text.toString());
		} catch (FileNotFoundException e){
			Logger.error(this,"Error creating url seed file for host " + host.getHostname(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (IOException e){
			Logger.error(this,"Error creating url seed file for host " + host.getHostname(), e);
			throw new DotRuntimeException(e.getMessage(), e);	
		} finally{
			try{
				if (writer != null){
					writer.close();
				}
			}catch (IOException e){
				Logger.error(this,"Error creating url seed file for host " + host.getHostname(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
		}
		
		return new Path(seedFolder.getAbsolutePath()+Path.SEPARATOR);

	}
	
	/**
	 * Creates the crawl filter file for a given host
	 * @param host
	 * @param conf
	 * @param hostIndexFolder
	 */
    private void createUrlCrawlFile(final Host host,final Configuration conf, final String hostIndexFolder){
    	
		Writer writer = null;
		//Create url filter file
		File urlFilterFile = new File(hostIndexFolder+Path.SEPARATOR+ "crawl-urlfilter.txt");
		try{
			StringBuffer text = new StringBuffer();
			text.append("# skip file:, ftp:, & mailto: urls\r\n");
			text.append("-^(file|ftp|mailto):\r\n");
			text.append("\r\n");
			text.append("# skip image and other suffixes we can't yet parse\r\n");
			List<String> extensionsToIgnore = getExtensionsToIgnore();
			if(extensionsToIgnore!=null && !extensionsToIgnore.isEmpty()){
				text.append("-\\.(gif|GIF|jpg|JPG|png|PNG|ico|ICO|css|sit|eps|wmf|zip|ppt|mpg|xls|gz|rpm|tgz|mov|MOV|exe|jpeg|JPEG|bmp|BMP");
				for(String ext : extensionsToIgnore){
					text.append("|"+ext.toLowerCase()+"|"+ext.toUpperCase());
				}
				text.append(")$\r\n");
			}else{
				text.append("-\\.(gif|GIF|jpg|JPG|png|PNG|ico|ICO|css|sit|eps|wmf|zip|ppt|mpg|xls|gz|rpm|tgz|mov|MOV|exe|jpeg|JPEG|bmp|BMP)$\r\n");
			}
			text.append("\r\n");
		
		
			text.append("# skip URLs with slash-delimited segment that repeats 3+ times, to break loops\r\n");
			text.append("-.*(/[^/]+)/[^/]+\\1/[^/]+\\1/\r\n");
			text.append("\r\n");
			
			if(!isQueryStringEnabled()){
				text.append("# skip URLs containing certain characters as probable queries, etc.\r\n");
				text.append("-[?*!@=]\r\n");
				text.append("\r\n");
			}
			
			List<String> paths = getPathsToIgnore();
			if(paths!=null && !paths.isEmpty()){
				for(String path : getPathsToIgnore()){
					path = path.replaceAll("^A-Za-z0-9", "");
					if(!path.startsWith("/")){
						path = "/" + path;
					}
					if(!path.endsWith("/")){
						path = path + "/";
					}
					String hostName = host.getHostname().replaceAll("^A-Za-z0-9", "").toLowerCase();
					if(UtilMethods.isSet( getPortNumber())){
						hostName += ":"+getPortNumber(); 
					}
					text.append("# ignore path "+ path +" in host "+hostName+"\r\n");
					text.append("-^http://"+hostName+path+"\r\n");
					text.append("\r\n");
				}
			}
			
			String hostName = host.getHostname().replaceAll("^A-Za-z0-9", "").toLowerCase();

			if(!hostName.contains("*")){
				if(UtilMethods.isSet( getPortNumber())){
					hostName += ":"+getPortNumber(); 
				}
				text.append("# accept hosts in "+hostName+"\r\n");
				text.append("+^http://"+hostName+"/\r\n");
				text.append("\r\n");
			}
			
			if(UtilMethods.isSet(host.getAliases())){
				String[] aliases = host.getAliases().split("\n");
				for(String alias: aliases){
					alias = alias.replaceAll("^A-Za-z0-9", "");
					if(!alias.contains("*")){
						if(UtilMethods.isSet( getPortNumber())){
							alias += ":"+getPortNumber(); 
						}
						if(!hostName.equalsIgnoreCase(alias)){
							text.append("# accept hosts in "+alias.toLowerCase()+"\r\n");
							text.append("+^http://"+alias.toLowerCase()+"/\r\n");
							text.append("\r\n");
						}
						hostName = alias;
					}
				}
			}
			
			
			text.append("# skip everything else\r\n");
			text.append("-.\r\n");
			text.append("\r\n");

			writer = new BufferedWriter(new FileWriter(urlFilterFile));
			writer.write(text.toString());
		} catch (FileNotFoundException e){
			Logger.error(this,"Error creating crawl filter file for host " + host.getHostname(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (IOException e){
			Logger.error(this,"Error creating crawl filter file for host " + host.getHostname(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} finally{
			try{
				if (writer != null){
					writer.close();
				}
			}catch (IOException e){
				Logger.error(this,"Error creating crawl filter file for host " + host.getHostname(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
		}
	
		conf.set("urlfilter.regex.file", urlFilterFile.getAbsolutePath());
	}
	
    /**
     * 
     * @param hostId
     * @return
     */
    public static boolean isHostIndexed(final String hostId){
    	if(!indexedHosts.isEmpty()){
    		final HostIndexBean bean = indexedHosts.get(hostId);
    		if(bean!=null){
    			return true;
    		}
    	}
    	return false;
    }
	
    /**
     * 
     * @param hostId
     * @return
     */
    public static boolean isHostReIndexed(final String hostId){
      	if(!indexedHosts.isEmpty()){
    		HostIndexBean bean = indexedHosts.get(hostId);
    		if(bean!=null){
    			return (bean.isReIndexed() && !bean.isIndexing())?true:false;
    		}else{
    			bean = getIndexedHostFromFS(hostId);
        		if(bean!=null){
        			indexedHosts.putIfAbsent(hostId, bean);
        			return (bean.isReIndexed() && !bean.isIndexing())?true:false;
        		}
    		}
    	}else{
    		HostIndexBean bean = getIndexedHostFromFS(hostId);
    		if(bean!=null){
    			indexedHosts.putIfAbsent(hostId, bean);
    			return (bean.isReIndexed() && !bean.isIndexing())?true:false;
    		}
    	}
    	return false;
    }
    
    /**
     * 
     * @param hostId
     * @throws IOException
     */
    public static void refreshIndexForHost(final String hostId) throws IOException{

    	if(!indexedHosts.isEmpty()){
    		final HostIndexBean bean = indexedHosts.get(hostId);
    		if(bean!=null){
    			final File indexFolder = bean.getIndexFolder();
    			final File tempIndexFolder = new File(indexFolder.getAbsolutePath()+"_temp");
    			if(tempIndexFolder.getAbsoluteFile().exists()){
    				FileUtils.forceDelete(indexFolder);
    				FileUtils.moveDirectory(tempIndexFolder, indexFolder);
    				indexedHosts.replace(hostId, bean);		
    			}
    		}
    	}
    }
    
    /**
     * 
     * @param hostId
     * @return
     */
    public static File getIndexFolderForHost(final String hostId){
    	
    	File indexFolder = null;
    	if(!indexedHosts.isEmpty()){
    		HostIndexBean bean = indexedHosts.get(hostId);
    		if(bean==null || (bean!=null && !bean.getIndexFolder().getAbsoluteFile().exists())){
    			bean = getIndexedHostFromFS(hostId);
        		if(bean!=null){
        			indexedHosts.replace(hostId, bean);
        		}
    		}
    		indexFolder = bean.getIndexFolder();
    	}else{
    		final HostIndexBean bean = getIndexedHostFromFS(hostId);
    		if(bean!=null){
    			indexedHosts.putIfAbsent(hostId, bean);
    			indexFolder = bean.getIndexFolder();
    		}
    	}
    	
    	return indexFolder;
    	
    }
    
    /**
     * 
     * @param hostId
     * @return
     */
    private static HostIndexBean getIndexedHostFromFS(final String hostId){
    	
    	HostIndexBean bean = null;
    	
    	Host host = null;
    	
    	try {
			host = hostAPI.find(hostId, userAPI.getSystemUser() , false);
		} catch (Exception e) {
			throw new DotRuntimeException(e.getMessage(), e);
		} 
		if(host!=null){
    	File hostFolder = new File(fileAPI.getRealAssetPath()
				+ Path.SEPARATOR + CrawlerUtil.SEARCH_INDEX_DIR
				+ Path.SEPARATOR + host.getHostname() );
		if (!hostFolder.getAbsoluteFile().exists()) {
			hostFolder = new File(fileAPI.getRealAssetPath()
					+ Path.SEPARATOR + CrawlerUtil.SEARCH_INDEX_DIR
					+ Path.SEPARATOR + host.getIdentifier());
		}
		
		
		File hostIndexFolder = new File(hostFolder.getAbsolutePath()+Path.SEPARATOR+host.getIdentifier());

		if(hostIndexFolder.getAbsoluteFile().exists()){
			bean = new HostIndexBean(hostIndexFolder);

		  }
		}
		
		return bean;
    }
    
    /**
     * 
     * @author Roger
     *
     */
    static final class HostIndexBean{
    	
    	private File indexFolder;
    	
    	private boolean indexing;
    	
    	HostIndexBean(File indexFolder){
    		this.indexFolder = indexFolder;
    		
    	}
    	
    	HostIndexBean(File indexFolder, boolean indexing){
    		this.indexFolder = indexFolder;
    		this.indexing = indexing;
    		
    	}
    	
		public File getIndexFolder() {
			return indexFolder;
		}

		public void setIndexFolder(File indexFolder) {
			this.indexFolder = indexFolder;
		}

		public boolean isReIndexed() {
			File hostTempIndexFolder = new File(indexFolder.getAbsolutePath()+"_temp");
			return hostTempIndexFolder.exists();
		}
		
		public boolean isIndexing() {
			return indexing;
		}
		
		public void setIndexing(boolean indexing) {
			this.indexing = indexing;
		}
		
		public boolean equals(Object object){
			boolean returnValue = false;
			if((object instanceof HostIndexBean)){
				HostIndexBean o = (HostIndexBean) object;
				if(this.indexFolder.equals(o.indexFolder) && 
						this.indexing == o.indexing){
					returnValue = true;
				}
			}
			return returnValue;
		}
		
		public int hashCode(){
			return HashCodeBuilder.reflectionHashCode(this);
		}
    	
    }




}
