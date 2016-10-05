package com.dotmarketing.common.reindex;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.content.elasticsearch.util.ESReindexationProcessStatus;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.I18NMessage;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.business.journal.DistributedJournalAPI;
import com.dotmarketing.common.business.journal.DistributedJournalFactory;
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
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

/**
 * This thread is in charge of re-indexing the contenlet information placed in
 * the {@code dist_reindex_journal} table. This process is constantly checking
 * the existence of any record in the table and will add its information to the
 * Elastic index.
 * <p>
 * The records added to the table will have a priority level set by the
 * {@link DistributedJournalFactory#REINDEX_JOURNAL_PRIORITY_NEWINDEX} constant.
 * During the process, all the "correct" contents will be processed and
 * re-indexed first. All the "bad" records (contents that could not be
 * re-indexed) will be set a different priority level specified by the
 * {@link DistributedJournalFactory#REINDEX_JOURNAL_PRIORITY_FAILED_FIRST_ATTEMPT}
 * constant and will be given more opportunities to be re-indexed after all of
 * the correct contents have already been processed.
 * </p>
 * <p>
 * The number of times the bad contents can re-try the re-index process is
 * specified by the {@link DistributedJournalFactory#RETRY_FAILED_INDEX_TIMES}
 * property, which can be customized through the
 * {@code dotmarketing-config.properties} file. If a content cannot be
 * re-indexed after all the specified attempts, a notification will be sent to
 * the Notification Bar indicating the Identifier of the bad contentlet. This
 * way users can keep track of the failed records and check the logs to get more
 * information about the failure.
 * </p>
 * <p>
 * The reasons why a content cannot be re-indexed can be, for example:
 * <ul>
 * <li>Incorrect data format in the contentlet's data, such as malformed JSON
 * data.</li>
 * <li>Association to orphaned data, such as being associated to an Inode that
 * does not exist in the system.</li>
 * <li>A Content Page, in which one or more of its parent folders do not exist
 * in the system anymore.</li>
 * </ul>
 * </p>
 * 
 * @author root
 * @version 3.3
 * @since Mar 22, 2012
 *
 */
public class ReindexThread extends Thread {

	private static final ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();
    private final LinkedList<IndexJournal<String>> remoteQ = new LinkedList<IndexJournal<String>>();
    private final LinkedList<IndexJournal<String>> remoteDelQ = new LinkedList<IndexJournal<String>>();
    private final CopyOnWriteArrayList<String> notifiedFailingRecords = new CopyOnWriteArrayList<String>();
	private final DistributedJournalAPI<String> jAPI;
	private final NotificationAPI notificationAPI;

	private static ReindexThread instance;

	private boolean start = false;
	private boolean work = false;
	private int sleep = 100;
	private int delay = 7500;
	private int delayOnError = 500;
	private int failedAttemptsCount = 0;
	private boolean reindexSleepDuringIndex = false;
	private int reindexSleepDuringIndexTime = 0;

	public ReindexThread() {

		this(APILocator.getDistributedJournalAPI(), APILocator.getNotificationAPI());
	}

	@VisibleForTesting
	public ReindexThread(final DistributedJournalAPI<String> jAPI,
						 final NotificationAPI notificationAPI) {

		this.jAPI = jAPI;
		this.notificationAPI = notificationAPI;

	}

	private void finish() {
		work = false;
		start = false;
	}

	private void addRecordsToDelete(List<IndexJournal<String>> records) {
	    synchronized(remoteDelQ) {
	        remoteDelQ.addAll(records);
	    }
	}

	/**
	 * Counts the failed attempts when indexing and handles error notifications
	 */
	private void addIndexingFailedAttempt () {

		synchronized (remoteQ) {

			failedAttemptsCount += 1;

			if ( failedAttemptsCount == 10 ) {//We just want to create one notification error

				try {
					String languageKey = "notification.reindexing.error";
					String defaultMsg = "An error has occurred during the indexing process, please check your logs and retry later";

					final User systemUser = APILocator.getUserAPI().getSystemUser();
					sendNotification(languageKey, null, defaultMsg, systemUser);
				} catch ( DotDataException | LanguageException e ) {
					Logger.error(this, "Error creating a system notification informing about problems in the indexing process.", e);
				}
			} else {

				/*
				Check every TEN failures if we still have records on the dist_reindex_journal as a fall back
				in case the user wants to finish the re-index process clearing that table or if we have some endless
				running thread because the server didn't notice the reindex was cancelled.
				 */
				if ( failedAttemptsCount % 10 == 0 ) {

					Connection conn = null;

					try {

						conn = DbConnectionFactory.getDataSource().getConnection();
						conn.setAutoCommit(false);
						long foundRecords = jAPI.recordsLeftToIndexForServer(conn);
						if ( foundRecords == 0 ) {
							stopFullReindexation();
							stopThread();
						}

					} catch ( Exception e ) {
						Logger.error(this, "Error verifying pending records for indexing", e);
						try {
							stopFullReindexation();
							stopThread();
						} catch ( DotDataException e1 ) {
							Logger.error(this, "Error forcing the index thread to stop", e);
						}
					} finally {
						if ( conn != null ) {
							try {
								conn.close();
							} catch ( SQLException e ) {
								Logger.error(this, "Error closing connection", e);
							}
						}
					}
				}
			}
		}
	}
	
	private void startProcessing(int sleep, int delay) {
		this.sleep = sleep;
		this.delay = delay;
		start = true;
	}
	
	private boolean die=false;

	/**
	 * This method is constantly verifying the existence of records in the
	 * {@code dist_reindex_journal} table. If a record is found, then it must be
	 * added to the Elastic index. If that's not possible, a notification
	 * containing the content identifier will be sent to the user via the
	 * Notifications API to take care of the problem as soon as possible.
	 */
	public void run() {
		this.notifiedFailingRecords.clear();
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
						// The re-indexation process has finished successfully
						reindexSwitchover(false);
						final User systemUser = APILocator.getUserAPI().getSystemUser();
						sendNotification("notification.reindexing.success", null, null, systemUser);
					} else if (remoteQ.size() == 0 && ESReindexationProcessStatus.inFullReindexation()
							&& jAPI.recordsLeftToIndexForServer() > 0) {
						// Fill the re-index queue with failed records now after
						// all the "good" records have been processed
						fillRemoteQ(true);
					}
					
					if(!remoteDelQ.isEmpty()) {
					    synchronized(remoteDelQ) {
				            try {
				                List<IndexJournal<String>> toDelete=remoteDelQ;
				                if(toDelete.size()>=200) {
				                    toDelete=remoteDelQ.subList(0, 200);
				                }

								//Delete from the dist_reindex_journal the records that were successfully indexed.
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
						final ArrayList<IndexJournal<String>> recordsToDelete= new ArrayList<>();
						while(!remoteQ.isEmpty()) {

							IndexJournal<String> idx = remoteQ.removeFirst();

							try {
								writeDocumentToIndex(bulk, idx);
							} catch ( Exception e ) {

								Logger.error(this, "Unable to index record with id [" + idx.getIdentToIndex() + "]", e);

								//Counts the failed attempts when indexing and handles error notifications
								addIndexingFailedAttempt();

								try {
									/*
									Reset to null the server id of the failed records in the reindex journal table
									in order to make them available again for the reindex process.
									 */
									List<IndexJournal<String>> failedRecords = new ArrayList<>();
									failedRecords.add(idx);
									jAPI.resetServerForReindexEntry(failedRecords);
								} catch ( DotDataException dataException ) {
									Logger.error(this, "Error adding back failed records to reindex queue", dataException);
								}
								// The total number of re-tries minus 1 will
								// indicate the last opportunity of a record to
								// be re-indexed.
								int totalAttempts = (DistributedJournalFactory.REINDEX_JOURNAL_PRIORITY_FAILED_FIRST_ATTEMPT + DistributedJournalFactory.RETRY_FAILED_INDEX_TIMES);
								String identToIndex = idx.getIdentToIndex();
								if (!this.notifiedFailingRecords.contains(identToIndex) && idx.getPriority() >= totalAttempts) {
									// The record was not able to be re-indexed,
									// so a notification will be generated and
									// the record will not be processed anymore
									final User systemUser = APILocator.getUserAPI().getSystemUser();
									String msg = "Could not re-index record with the Identifier '"
											+ identToIndex
											+ "'. The record is in a bad state or can be associated to orphaned records. You can try running the Fix Assets Inconsistencies tool and re-start the reindex.";
									sendNotification("notification.reindexing.error.processrecord", new Object[] { identToIndex }, msg, systemUser);
									this.notifiedFailingRecords.add(identToIndex);
								}

								try {
									Thread.sleep(delayOnError);
								} catch ( InterruptedException ie ) {
									Logger.error(this, ie.getMessage(), ie);
								}

								/*
								This continue will avoid to remove this failed record from the index journal table so
								can be grab it again in another iteration.
								 */
								continue;
							}

							recordsToDelete.add(idx);

							//If the REINDEX_SLEEP_DURING_INDEX was set
							if ( reindexSleepDuringIndex ) {
								try {
									int sleepTime = getReindexSleepDuringIndexTime();
									Thread.sleep(sleepTime);
								} catch ( InterruptedException e ) {
									Logger.error(this, e.getMessage(), e);
								}
							}

						}

						HibernateUtil.closeSession();
				        if(bulk.numberOfActions()>0) {
				            bulk.execute(new ActionListener<BulkResponse>() {

								void handleRecords (List<IndexJournal<String>> failedRecords) {

									//List of records to delete from the reindex journal table
									addRecordsToDelete(recordsToDelete);

									try {
										if ( failedRecords != null && !failedRecords.isEmpty() ) {
											/*
											Reset to null the server id of the failed records in the reindex journal table
											in order to make them available again for the reindex process.
											 */
											jAPI.resetServerForReindexEntry(failedRecords);
										}
									} catch ( DotDataException e ) {
										Logger.error(this, "Error adding back failed records to reindex queue", e);
									}
								}

								public void onResponse ( BulkResponse resp ) {

									//Handle failures on the re-index process if any
									List<IndexJournal<String>> failedRecords = failureHandler(resp);

									//Handle the processed records
									handleRecords(failedRecords);
								}

								public void onFailure ( Throwable ex ) {

									Logger.error(ReindexThread.class, "Indexing process failed", ex);

									//Handle the processed records
									handleRecords(null);

									//Reset the failed attempts count as the onFailure will finish the indexing process
									failedAttemptsCount = 0;
								}

								/**
								 * Checks if we had failures when indexing, on failure we will retry the indexing process of the records that failed,
								 * the process WON'T continue with failed records.
								 *
								 * @param resp
								 */
								private List<IndexJournal<String>> failureHandler ( BulkResponse resp ) {

									//List of records that failed and will be added to the queue for more attempts
									List<IndexJournal<String>> failedRecords = new ArrayList<>();

									//Verify if we have failures to handle
									if ( resp.hasFailures() && isWorking() ) {

										Logger.error(this, "Error indexing content [" + resp.buildFailureMessage() + "]");

										//Counts the failed attempts when indexing and handles error notifications
										addIndexingFailedAttempt();

										//Search for the failed items
										for ( BulkItemResponse itemResponse : resp.getItems() ) {

											//Check if the indexing process failed for this item
											if ( itemResponse.isFailed() ) {

												//Get the data of the failed record
												String initialId = itemResponse.getId();
												//Remove the language from the id in order to get just the inode/identifier
												int languageIndex = initialId.lastIndexOf("_");
												String failedId = initialId;
												if ( languageIndex != -1 ) {
													failedId = initialId.substring(0, languageIndex);
												}

												//Search the failed record into the list of records to delete
												Iterator<IndexJournal<String>> toDeleteIterator = recordsToDelete.iterator();
												while ( toDeleteIterator.hasNext() ) {

													IndexJournal<String> indexToDelete = toDeleteIterator.next();
													if ( indexToDelete.getInodeToIndex().equals(failedId) || indexToDelete.getIdentToIndex().equals(failedId) ) {

														//Add it to the list of records that failed and needs to be added back to the reindex queue
														if ( !exist(failedRecords, indexToDelete) ) {
															failedRecords.add(indexToDelete);
														}

														/*
														Remove the record from the list of contents to remove from the index journal table
														as it indexing process failed and we want a re-try with those records.
														 */
														toDeleteIterator.remove();
													}
												}
											}
										}

										if ( !failedRecords.isEmpty() ) {

											Logger.error(this, "Reindex thread will try to re-index [" + String.valueOf(failedRecords.size()) + "] failed records.");

											try {
												Thread.sleep(delayOnError);
											} catch ( InterruptedException e ) {
												Logger.error(this, e.getMessage(), e);
											}
										}
									}

									return failedRecords;
								}

								/**
								 * Checks if a given record already exist on a given list
								 *
								 * @param toRestore
								 * @param toCompare
								 * @return
								 */
								private boolean exist ( List<IndexJournal<String>> toRestore, IndexJournal<String> toCompare ) {

									boolean exist = false;
									for ( IndexJournal<String> current : toRestore ) {

										if ( current.getId() == toCompare.getId() ) {
											exist = true;
											break;
										}
									}

									return exist;
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
                lock="lock table dist_reindex_journal write, contentlet_version_info read, identifier read, indicies write, log_mapper read";
            else if(DbConnectionFactory.isMsSql())
                lock="SELECT * FROM dist_reindex_journal WITH (TABLOCKX)";
            else if(DbConnectionFactory.isOracle())
                lock="LOCK TABLE dist_reindex_journal IN EXCLUSIVE MODE";
            else if(DbConnectionFactory.isPostgres())
                lock="lock table dist_reindex_journal";
            else if(DbConnectionFactory.isH2())
                lock="SELECT * from dist_reindex_journal FOR UPDATE";
            
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
	public synchronized static void startThread ( int sleep, int delay ) {

		Logger.info(ReindexThread.class, "ContentIndexationThread ordered to start processing");

		//Creates and starts a thread
		createThread();

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
			instance.sleep = Config.getIntProperty("reindex.thread.sleep", 100);
			instance.delay = Config.getIntProperty("reindex.thread.delay", 7500);
			instance.delayOnError = Config.getIntProperty("reindex.thread.delayonerror", 500);
			instance.reindexSleepDuringIndex = Config.getBooleanProperty("reindex.thread.reindexsleepduringindex", false);
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
	    fillRemoteQ(false);
	}
	
	private void fillRemoteQ (boolean includeFailedRecords) throws DotDataException {
	    try {
	        HibernateUtil.startTransaction();
	        remoteQ.addAll(jAPI.findContentReindexEntriesToReindex(includeFailedRecords));
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
	    this.remoteQ.clear();
	    this.notifiedFailingRecords.clear();
	    APILocator.getDistributedJournalAPI().cleanDistReindexJournal();
	    indexAPI.fullReindexAbort();
	    unpause();
	    HibernateUtil.commitTransaction();
	}

	/**
	 * Stops the current re-indexation process and switches the current index to
	 * the new one. The main goal of this method is to allow users to switch to
	 * the new index even if one or more contents could not be re-indexed.
	 * <p>
	 * This is very useful because the new index can be created and used
	 * immediately. The user can have the new re-indexed content available and
	 * then work on the conflicting contents, which can be either fixed or
	 * removed from the database.
	 * </p>
	 * 
	 * @throws SQLException
	 *             An error occurred when interacting with the database.
	 * @throws DotDataException
	 *             The process to switch to the new failed.
	 * @throws InterruptedException
	 *             The established pauses to switch to the new index failed.
	 */
	public void stopFullReindexationAndSwitchover() throws SQLException, DotDataException, InterruptedException {
		this.remoteQ.clear();
		reindexSwitchover(true);
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

	/**
	 * Generates a new notification displayed at the top left side of the
	 * back-end page in dotCMS. This utility method allows you to send reports
	 * to the user regarding the operations performed during the re-index,
	 * whether they succeeded or failed.
	 * 
	 * @param key
	 *            - The message key that should be present in the language
	 *            properties files.
	 * @param msgParams
	 *            - The parameters, if any, that will replace potential
	 *            placeholders in the message. E.g.: "This is {0} test."
	 * @param defaultMsg
	 *            - If set, the default message in case the key does not exist
	 *            in the properties file. Otherwise, the message key will be
	 *            returned.
	 * @param systemUser
	 * 			  - The user is needed to get the default locale for the messages.
	 * @throws DotDataException
	 *             The notification could not be posted to the system.
	 * @throws LanguageException
	 *             The language properties could not be retrieved.
	 */
	protected void sendNotification(final String key, final Object[] msgParams, final String defaultMsg, final User systemUser) throws DotDataException,
			LanguageException {

		this.notificationAPI.generateNotification(
				new I18NMessage("notification.reindex.error.title"), // title = Reindex Notification
				new I18NMessage(key, defaultMsg, msgParams),
				null, // no actions
				NotificationLevel.ERROR,
				NotificationType.GENERIC,
				systemUser.getUserId(),
				systemUser.getLocale()
		);
	}

	/**
	 * Switches the current index structure to the new re-indexed data. This
	 * method also allows users to switch to the new re-indexed data even if
	 * there are still remaining contents in the {@code dist_reindex_journal}
	 * table.
	 * 
	 * @param forceSwitch
	 *            - If {@code true}, the new index will be used, even if there
	 *            are contents that could not be processed. Otherwise, set to
	 *            {@code false} and the index switch will only happen if ALL
	 *            contents were re-indexed.
	 * @throws SQLException
	 *             An error occurred when interacting with the database.
	 * @throws DotDataException
	 *             The process to switch to the new failed.
	 * @throws InterruptedException
	 *             The established pauses to switch to the new index failed.
	 */
	private void reindexSwitchover(boolean forceSwitch) throws SQLException, DotDataException, InterruptedException {
		Connection conn = DbConnectionFactory.getDataSource().getConnection();
		try {
			conn.setAutoCommit(false);
			lockCluster(conn);
			// We double check again. Only one node will enter this critical
			// region, then others will enter just to see that the switchover is
			// done
			if (ESReindexationProcessStatus.inFullReindexation(conn)) {
				if (forceSwitch || jAPI.recordsLeftToIndexForServer(conn) == 0) {
					Logger.info(this, "Running Reindex Switchover");
					// Wait a bit while all records gets flushed to index
					Thread.sleep(2000L);
					indexAPI.fullReindexSwitchover(conn);
					failedAttemptsCount = 0;
					// Wait a bit while elasticsearch flushes it state
					Thread.sleep(2000L);
				}
			}
		} finally {
			try {
				unlockCluster(conn);
			} finally {
				try {
					conn.commit();
					if (forceSwitch) {
						APILocator.getDistributedJournalAPI().cleanDistReindexJournal();
					}
				} catch (Exception ex) {
					Logger.warn(this, ex.getMessage(), ex);
					conn.rollback();
				} finally {
					conn.close();
				}
			}
			this.notifiedFailingRecords.clear();
		}
	}

}
