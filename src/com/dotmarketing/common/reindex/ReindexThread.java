package com.dotmarketing.common.reindex;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.content.elasticsearch.util.ESReindexationProcessStatus;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.business.journal.DistributedJournalAPI;
import com.dotmarketing.common.business.journal.IndexJournal;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class ReindexThread extends Thread {

	private static final ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();
    private final LinkedList<IndexJournal<String>> remoteQ = new LinkedList<IndexJournal<String>>();
    private final LinkedList<IndexJournal<String>> remoteDelQ = new LinkedList<IndexJournal<String>>();
	private final DistributedJournalAPI<String> jAPI = APILocator.getDistributedJournalAPI();

	private static ReindexThread instance;

	private boolean start = false;
	private boolean work = false;
	private int sleep = 100;
	private int delay = 7500;
	private boolean reindexSleepDuringIndex = false;
	private int reindexSleepDuringIndexTime = 0;

	private void finish() {
		work = false;
		start = false;
	}

	private void addRecordsToDelete(List<IndexJournal<String>> records) {
	    synchronized(remoteDelQ) {
	        remoteDelQ.addAll(records);
	    }
	}
	
	private void startProcessing(int sleep, int delay) {
		this.sleep = sleep;
		this.delay = delay;
		start = true;
	}
	
	private boolean die=false;

	public void run() {

		while (!die) {

			if (start && !work) {
				if (delay > 0) {
					Logger.info(this, "Reindex Thread start delayed for "
							+ delay + " millis.");
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e) {

					}
				}
				Logger.info(this, "Reindex Thread started with a sleep of "
						+ sleep);
				start = false;
				work = true;
			}
			if (work) {
			    boolean wait=true;
				try {
					if(remoteQ.size()==0)
					    fillRemoteQ();
					
					if(remoteQ.size()==0 && ESReindexationProcessStatus.inFullReindexation() && jAPI.recordsLeftToIndexForServer()==0) {
					    Connection conn=DbConnectionFactory.getDataSource().getConnection();
					    try{
					        conn.setAutoCommit(false);
    					    lockCluster(conn);
    					    
    					    // we double check again. Only one node will enter this critical region
    					    // then others will enter just to see that the switchover is done
    					    if(ESReindexationProcessStatus.inFullReindexation(conn)) {
    					        if(jAPI.recordsLeftToIndexForServer(conn)==0) {
    					            Logger.info(this, "Running Reindex Switchover");
    					            
    					            // wait a bit while all records gets flushed to index
                                    Thread.sleep(2000L);
                                    
    					            indexAPI.fullReindexSwitchover(conn);
    					            
    					            // wait a bit while elasticsearch flushes it state
    	                            Thread.sleep(2000L);
    					        }
    					    }
					    
					    }finally{
					        try {
					            unlockCluster(conn);
					        }
					        finally {
					            try {
					                conn.commit();
					            }
					            catch(Exception ex) {
					                Logger.warn(this, ex.getMessage(), ex);
					                conn.rollback();
					            }
					            finally {
					                conn.close();
					            }
					        }
					    }
					}    
					
					if(!remoteDelQ.isEmpty()) {
					    synchronized(remoteDelQ) {
				            try {
				                List<IndexJournal<String>> toDelete=remoteDelQ;
				                if(toDelete.size()>=200) {
				                    toDelete=remoteDelQ.subList(0, 200);
				                }
				                jAPI.deleteReindexEntryForServer(toDelete);
				                if(toDelete.size()==remoteDelQ.size()) {
				                    remoteDelQ.clear();
				                } else {
				                    final int n=toDelete.size();
				                    for(int i=0;i<n;i++)
				                        remoteDelQ.removeFirst();
				                }
				            }
				            catch(Exception ex) {
				                Logger.warn(ReindexThread.class,"can't dele dist_reindex records. Will try again later", ex);
				            }
				        }
					}
					else if(!remoteQ.isEmpty()) {
					    wait=false;
					    Client client=new ESClient().getClient();
						BulkRequestBuilder bulk=client.prepareBulk();
						final ArrayList<IndexJournal<String>> recordsToDelete=new ArrayList<IndexJournal<String>>();
						while(!remoteQ.isEmpty()) {
    					    IndexJournal<String> idx = remoteQ.removeFirst();
					        writeDocumentToIndex(bulk,idx);
    				        recordsToDelete.add(idx);
    				        if(reindexSleepDuringIndex){
	    				        try {
	    							Thread.sleep(delay);
	    						} catch (InterruptedException e) {
	    							Logger.error(this, e.getMessage(), e);
	    						}
    				        }
						}
						HibernateUtil.closeSession();
				        if(bulk.numberOfActions()>0) {
				            bulk.execute(new ActionListener<BulkResponse>() {
				                void deleteRecords() {
				                    addRecordsToDelete(recordsToDelete);
				                }
                                public void onResponse(BulkResponse resp) {
                                    deleteRecords();
                                    if(resp.hasFailures())
                                        Logger.warn(this, resp.buildFailureMessage());
                                }
                                public void onFailure(Throwable ex) {
                                    Logger.error(ReindexThread.class,"Error indexing records", ex);
                                    deleteRecords();
                                }
                            });
				        }
				        else if(recordsToDelete.size()>0) {
				            addRecordsToDelete(recordsToDelete);
				        }
					}
					
				} catch (Exception ex) {
					Logger.error(this, "Unable to index record", ex);
				} finally {
					try {
						HibernateUtil.closeSession();
						try{
							DbConnectionFactory.closeConnection();
						}catch (Exception e) {
							Logger.debug(this, "Unable to close connection : " + e.getMessage(),e);
						}
						
					} catch (DotHibernateException e) {
						Logger.error(this, e.getMessage(), e);
						try{
							DbConnectionFactory.closeConnection();
						}catch (Exception e1) {
							Logger.debug(this, "Unable to close connection : " + e1.getMessage(),e1);
						}
					}
				}
				
				// if we have no records and are just waiting to check if we
				// are in a full reindex fired by another server
				if (wait && remoteQ.isEmpty()) {
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e) {
						Logger.error(this, e.getMessage(), e);
					}
				}
			} else {
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
					Logger.error(this, e.getMessage(), e);
				}
			}
		}
	}

	public void unlockCluster() throws DotDataException {
	    unlockCluster(DbConnectionFactory.getConnection());
	}
	
	public void unlockCluster(Connection lockConn) throws DotDataException {
	    try {
            if(DbConnectionFactory.isMySql()) {
                Statement smt=lockConn.createStatement();
                smt.executeUpdate("unlock tables");
                smt.close();
            }
        }
        catch(Exception ex) {
            throw new DotDataException(ex.getMessage(),ex);
        }
    }

	public void lockCluster() throws DotDataException {
	    lockCluster(DbConnectionFactory.getConnection());
	}
	
    public void lockCluster(Connection conn) throws DotDataException {
        try {
            String lock=null;
            /* this isn't working. A race condition might come
             * where we create new indexes and before we write 
             * records to index other server notice we're in
             * full reindexation and there is no records so it
             * will do the switchover
             * if(DbConnectionFactory.isMySql()) {
                lock="lock table dist_lock write";
                conn=DbConnectionFactory.getDataSource().getConnection();
            }*/
            if(DbConnectionFactory.isMySql())
                lock="lock table dist_reindex_journal write, contentlet read, identifier read, indicies write";
            else if(DbConnectionFactory.isMsSql())
                lock="SELECT * FROM dist_reindex_journal WITH (TABLOCKX)";
            else if(DbConnectionFactory.isOracle())
                lock="LOCK TABLE dist_reindex_journal IN EXCLUSIVE MODE";
            else if(DbConnectionFactory.isPostgres())
                lock="lock table dist_reindex_journal";
            
            Statement smt=conn.createStatement();
            smt.execute(lock);
            smt.close();
        }
        catch(Exception ex) {
            throw new DotDataException(ex.getMessage(),ex);
        }
    }

	/**
	 * Tells the thread to start processing. Starts the thread
	 */
	public synchronized static void startThread(int sleep, int delay) {
		Logger.info(ReindexThread.class,
				"ContentIndexationThread ordered to start processing");

		if (instance == null) {
			instance = new ReindexThread();
			instance.start();
		}
		instance.startProcessing(sleep, delay);
	}

	/**
	 * Tells the thread to stop processing. Doesn't shut down the thread.
	 */
	public synchronized static void stopThread() {
		if (instance != null && instance.isAlive()) {
			Logger.info(ReindexThread.class,
					"ContentIndexationThread ordered to stop processing");
			instance.finish();
		} else {
			Logger.error(ReindexThread.class,
					"No ContentIndexationThread available");
		}
	}

	/**
	 * Creates and starts a thread that doesn't process anything yet
	 */
	public synchronized static void createThread() {
		if (instance == null) {
			instance = new ReindexThread();
			instance.start();
			int i = Config.getIntProperty("REINDEX_SLEEP_DURING_INDEX", 0);
			if(i>0){
				instance.setReindexSleepDuringIndex(true);
				instance.setReindexSleepDuringIndexTime(i);
			}
		}

	}
	
    /**
     * Tells the thread to finish what it's down and stop
     */
	public synchronized static void shutdownThread() {
		if (instance!=null && instance.isAlive()) {
			Logger.info(ReindexThread.class, "ReindexThread shutdown initiated");
			instance.die=true;
		} else {
			Logger.warn(ReindexThread.class, "ReindexThread not running (or already shutting down)");
		}
	}

	/**
	 * This instance is intended to already be started. It will try to restart
	 * the thread if instance is null.
	 */
	public static ReindexThread getInstance() {
		if (instance == null) {
			createThread();
		}
		return instance;
	}

	private void fillRemoteQ() throws DotDataException {
	    try {
	        HibernateUtil.startTransaction();
	        remoteQ.addAll(jAPI.findContentReindexEntriesToReindex());
	        HibernateUtil.commitTransaction();
	    }
	    catch(Exception ex) {
	        HibernateUtil.rollbackTransaction();
	    }
	    finally {
	        HibernateUtil.closeSession();
	    }
	}

	@SuppressWarnings("unchecked")
	private void writeDocumentToIndex(BulkRequestBuilder bulk, IndexJournal<String> idx) throws DotDataException, DotSecurityException {
	    Logger.debug(this, "Indexing document "+idx.getIdentToIndex());
	    System.setProperty("IN_FULL_REINDEX", "true");
//	    String sql = "select distinct inode from contentlet join contentlet_version_info " +
//                " on (inode=live_inode or inode=working_inode) and contentlet.identifier=?";
	    
	    String sql = "select working_inode,live_inode from contentlet_version_info where identifier=?";
	    
        DotConnect dc = new DotConnect();
        dc.setSQL(sql);
        dc.addParam(idx.getIdentToIndex());
        List<Map<String,String>> ret = dc.loadResults();
        List<String> inodes = new ArrayList<String>(); 
        for(Map<String,String> m : ret) {
        	String workingInode = m.get("working_inode");
        	String liveInode = m.get("live_inode");
        	inodes.add(workingInode);
        	if(UtilMethods.isSet(liveInode) && !workingInode.equals(liveInode)){
        		inodes.add(liveInode);
        	}
        }
        
        for(String inode : inodes) {
        				
            Contentlet con = FactoryLocator.getContentletFactory().convertFatContentletToContentlet(
                    (com.dotmarketing.portlets.contentlet.business.Contentlet)
                        HibernateUtil.load(com.dotmarketing.portlets.contentlet.business.Contentlet.class, inode));
            
            if(idx.isDelete() && idx.getIdentToIndex().equals(con.getIdentifier()))
                // we delete contentlets from the identifier pointed on index journal record
                // its dependencies are reindexed in order to update its relationships fields
                indexAPI.removeContentFromIndex(con);
            else
                indexAPI.addContentToIndex(con,false,true,indexAPI.isInFullReindex(),bulk);
        }
	}
	
	int threadsPausing = 0;

	public synchronized void pause() {
		threadsPausing++;
		work=false;
	}

	public synchronized void unpause() {
		threadsPausing--;
		if (threadsPausing<1) {
			threadsPausing=0;
			work=true;
		}
	}
	
	public boolean isWorking() {
		return work;
	}
	
	public void stopFullReindexation() throws DotDataException {
	    HibernateUtil.startTransaction();
	    pause();
	    remoteQ.clear();
	    APILocator.getDistributedJournalAPI().cleanDistReindexJournal();
	    indexAPI.fullReindexAbort();
	    unpause();
	    HibernateUtil.commitTransaction();
	}

	/**
	 * @return the reindexSleepDuringIndex
	 */
	public boolean isReindexSleepDuringIndex() {
		return reindexSleepDuringIndex;
	}

	/**
	 * @param reindexSleepDuringIndex the reindexSleepDuringIndex to set
	 */
	public void setReindexSleepDuringIndex(boolean reindexSleepDuringIndex) {
		this.reindexSleepDuringIndex = reindexSleepDuringIndex;
	}

	/**
	 * @return the reindexSleepDuringIndexTime
	 */
	public int getReindexSleepDuringIndexTime() {
		return reindexSleepDuringIndexTime;
	}

	/**
	 * @param reindexSleepDuringIndexTime the reindexSleepDuringIndexTime to set
	 */
	public void setReindexSleepDuringIndexTime(int reindexSleepDuringIndexTime) {
		this.reindexSleepDuringIndexTime = reindexSleepDuringIndexTime;
	}
}
