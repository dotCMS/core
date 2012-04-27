package com.dotmarketing.sitesearch.business;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import com.dotmarketing.sitesearch.CrawlerUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * 
 * @author Roger
 *
 */
public class NutchBeanProxy {
	
//	private NutchBean nutchBean;
	
	private final Configuration conf = null;
	
	private final AtomicBoolean refreshing = new AtomicBoolean(false);
	
	private final Object lock = new Object();
	
	private final String hostId;

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
//	public NutchBean getNutchBean() {
//		return nutchBean;
//	}

	/**
	 * 
	 * @return
	 */
	public Configuration getConf() {
		return conf;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getHostId() {
		return hostId;
	}
	
    /**
     * 
     * @param hostId
     */
	public NutchBeanProxy(final String hostId){
		File hostIndexFolder = CrawlerUtil.getIndexFolderForHost(hostId);
		String path = hostIndexFolder!=null?hostIndexFolder.getAbsolutePath()+Path.SEPARATOR+"crawl-index":"";
//		Configuration conf = NutchConfiguration.create();
//		conf.set("plugin.folders", CrawlerUtil.CRAWLER_PLUGINS_DIR);
//		conf.set("searcher.dir", path);
//		this.conf = conf;
		this.hostId = hostId;
//		try{
//		   this.nutchBean = new NutchBean(conf);
//		}catch(Exception e){
//		   Logger.error(this,"Error getting nutchbean for host = " + hostId, e);
//		}
	}
	
	
    /**
     * Lazily updates the search index for the host IF and only if a new index is found on the FS.
     * The task is delegated to an anonymous thread since the time it takes to update the index is
     * proportional to the size of the index.
     */
	public void refresh() {
//		if(!this.refreshing.get()){
//			/*Since this check is outside the synchronized block, by using an atomic boolean we make sure that
//			  the check is in fact atomic.*/
//			final AtomicBoolean isReindexed = new AtomicBoolean(CrawlerUtil.isHostReIndexed(this.hostId));
//			if (isReindexed.get()) {
//				synchronized(this.lock) {
//					if(!this.refreshing.get()){
//					  try{
//						   this.refreshing.set(true);
//							Runnable indexUpdater = new Runnable(){
//								public void run(){
//									if(CrawlerUtil.isHostReIndexed(hostId)){
//										while(refreshing.get()){
//											try{
//												if(nutchBean!=null){
//													nutchBean.close();
//												}
//												CrawlerUtil.refreshIndexForHost(hostId);
//												final File hostIndexFolder = CrawlerUtil.getIndexFolderForHost(hostId);
//												final String path = hostIndexFolder!=null?hostIndexFolder.getAbsolutePath()+Path.SEPARATOR+"crawl-index":"";
//												if(UtilMethods.isSet(path)){
//													conf.set("searcher.dir", path);
//												}
//												nutchBean = new NutchBean(conf);
//											}catch(Exception e){
//												nutchBean = null;
//												Logger.error(this,"Error updating site search index for host = " + hostId, e);
//											}finally{
//												refreshing.set(false);
//											}
//										}
//									}
//								}
//							};
//							Thread indexUpdaterThread = new Thread(indexUpdater);
//							indexUpdaterThread.start();
//						}catch(Exception e){
//						   /*We must make sure the value of the atomic boolean variable resets to false,
//							 in case of an unexpected exception */
//							refreshing.set(false);
//						}
//					  }
//					}
//				 }
//			}
//			if(this.nutchBean==null){
//				try{
//					this.nutchBean = new NutchBean(this.conf);
//				}catch(Exception e){
//					this.nutchBean = null;
//					Logger.error(this,e.getMessage(), e);
//				}
//			}
		}
	
	public boolean equals(Object object){
		boolean returnValue = false;
//		if((object instanceof NutchBeanProxy)){
//			NutchBeanProxy proxy = (NutchBeanProxy) object;
//			if(this.hostId.equals(proxy.getHostId()) && 
//					this.nutchBean.equals(proxy.nutchBean)){
//				returnValue = true;
//			}
//		}
		return returnValue;
	}
	
	public int hashCode(){
		return HashCodeBuilder.reflectionHashCode(this);
	}


}
