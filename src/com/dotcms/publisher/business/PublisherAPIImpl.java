package com.dotcms.publisher.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.mapper.PublishQueueMapper;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Implement the PublishQueueAPI abstract class methods
 * @author Oswaldo
 *
 */
public class PublisherAPIImpl extends PublisherAPI{

	private PublishQueueMapper mapper = null;

	private static PublisherAPIImpl instance= null;

	public static PublisherAPIImpl getInstance() {
		if(instance==null){
			instance = new PublisherAPIImpl();
		}

		return instance;
	}
	protected PublisherAPIImpl(){
		mapper = new PublishQueueMapper();
	}

	private static final String MANDATORY_FIELDS=
										"operation, "+
										"asset, "+
										"entered_date, "+
										"language_id, "+
										"publish_date, "+
										"type, "+
										"bundle_id ";
	private static final String MANDATORY_PLACE_HOLDER = "?,?,?,?,?,?,?" ;

//
	//"last_results, "+
	//	"last_try,  "+
	//	"num_of_tries, "+

	private static final String PGINSERTSQL="insert into publishing_queue("+MANDATORY_FIELDS+") values("+MANDATORY_PLACE_HOLDER+")";
	private static final String MYINSERTSQL="insert into publishing_queue("+MANDATORY_FIELDS+") values("+MANDATORY_PLACE_HOLDER+")";
	private static final String MSINSERTSQL="insert into publishing_queue("+MANDATORY_FIELDS+") values("+MANDATORY_PLACE_HOLDER+")";
	private static final String OCLINSERTSQL="insert into publishing_queue("+MANDATORY_FIELDS+") values("+MANDATORY_PLACE_HOLDER+")";

    /**
     * Prepare the given assets to be published adding them to the Publishing queue
     *
     * @param identifiers
     * @param bundleId
     * @param publishDate
     * @param user
     * @throws DotPublisherException
     */
    public void addContentsToPublish ( List<String> identifiers, String bundleId, Date publishDate, User user ) throws DotPublisherException {
    	addAssetsToQueue(identifiers, bundleId, publishDate, user, ADD_OR_UPDATE_ELEMENT);
    }

    /**
     * Prepare the given assets to be unpublished adding them to the Publishing queue
     *
     * @param identifiers
     * @param bundleId
     * @param unpublishDate
     * @param user
     * @throws DotPublisherException
     */
    public void addContentsToUnpublish ( List<String> identifiers, String bundleId, Date unpublishDate, User user ) throws DotPublisherException {
    	addAssetsToQueue(identifiers, bundleId, unpublishDate, user, DELETE_ELEMENT);
    }


    @Override
	public void saveBundleAssets(List<String> identifiers, String bundleId,
			User user) throws DotPublisherException {
    	addAssetsToQueue(identifiers, bundleId, null, user, -1);

	}

    private void addAssetsToQueue(List<String> identifiers, String bundleId, Date operationDate, User user, long operationType ) throws DotPublisherException {
    	  if ( identifiers != null ) {

              try {
                  HibernateUtil.startTransaction();
                  for ( String identifier : identifiers ) {

                      DotConnect dc = new DotConnect();
                      if ( DbConnectionFactory.getDBType().equals( DbConnectionFactory.POSTGRESQL ) ) {
                          dc.setSQL( PGINSERTSQL );
                      } else if ( DbConnectionFactory.getDBType().equals( DbConnectionFactory.MYSQL ) ) {
                          dc.setSQL( MYINSERTSQL );
                      } else if ( DbConnectionFactory.getDBType().equals( DbConnectionFactory.MSSQL ) ) {
                          dc.setSQL( MSINSERTSQL );
                      } else {
                          dc.setSQL( OCLINSERTSQL );
                      }

                      PermissionAPI strPerAPI = APILocator.getPermissionAPI();

                      String type = "";

                      //First verify what kind of element we want to publish in order to avoid unnecessary calls
                      if ( identifier.contains( "user_" ) ) {//Trying to publish a user
                          type = "user";
                      } else if ( identifier.contains( ".jar" ) ) {//Trying to publish an OSGI jar bundle
                          type = "osgi";
                      } else {

                          Identifier iden = APILocator.getIdentifierAPI().find( identifier );

                          if ( !UtilMethods.isSet( iden.getId() ) ) { // we have an inode, not an identifier
                              try {
                                  // check if it is a structure
                                  Structure st = null;
                                  List<Structure> sts = StructureFactory.getStructures();
                                  for ( Structure s : sts ) {
                                      if ( s.getInode().equals( identifier ) ) {
                                          st = s;
                                      }
                                  }
                                  Folder folder;

                                  /**
                                   * ISSUE 2244: https://github.com/dotCMS/dotCMS/issues/2244
                                   *
                                   */
                                  // check if it is a category
                                  if ( CATEGORY.equals( identifier ) ) {
                                      type = "category";
                                  } else if ( UtilMethods.isSet( st ) ) {
                                      if ( !strPerAPI.doesUserHavePermission( st, PermissionAPI.PERMISSION_PUBLISH, user ) ) {
                                          Logger.info( PublisherAPIImpl.class, "User: " + user.getUserId() + " does not have Publish Permission over asset with Identifier: " + st.getIdentifier() );
                                          continue;
                                      }

                                      type = "structure";
                                  }

                                  // check if it is a folder
                                  else if ( UtilMethods.isSet( folder = APILocator.getFolderAPI().find( identifier, user, false ) ) ) {
                                      if ( !strPerAPI.doesUserHavePermission( folder, PermissionAPI.PERMISSION_PUBLISH, user ) ) {
                                          Logger.info( PublisherAPIImpl.class, "User: " + user.getUserId() + " does not have Publish Permission over asset with Identifier: " + folder.getIdentifier() );
                                          continue;
                                      }

                                      type = "folder";
                                  }
                              } catch ( Exception ex ) {
                            	  if ( UtilMethods.isSet( APILocator.getWorkflowAPI().findScheme(identifier) )) {
                                	  type = "workflow";
                                  }
                              }

                          } else {
                              if ( !strPerAPI.doesUserHavePermission( iden, PermissionAPI.PERMISSION_PUBLISH, user ) ) {
                                  Logger.info( PublisherAPIImpl.class, "User: " + user.getUserId() + " does not have Publish Permission over asset with Identifier: " + iden.getId() );
                                  continue;
                              }
                              type = UtilMethods.isSet( APILocator.getHostAPI().find( identifier, user, false ) ) ? "host" : iden.getAssetType();
                          }
                      }

                      String action = operationType==ADD_OR_UPDATE_ELEMENT?"Publish":operationType==DELETE_ELEMENT?"Delete":"Added by Browsing";
                      dc.addParam( operationType );

                      dc.addObject( identifier ); //asset
                      dc.addParam( new Date() ); // entered date
                      dc.addObject( 1 ); // language id

                      if(UtilMethods.isSet(operationDate)) {
                    	  dc.addParam( operationDate );
                      } else {
                    	  dc.addObject(null);
                      }

                      dc.addObject( type );
                      dc.addObject( bundleId );

                      dc.loadResult();


                      PushPublishLogger.log(getClass(), "Asset added to Push Publish Queue. Action: "+action+", Asset Type: " + type + ", Asset Id: " + identifier, bundleId, user);
                  }

                  HibernateUtil.commitTransaction();
              } catch ( Exception e ) {

                  try {
                      HibernateUtil.rollbackTransaction();
                  } catch ( DotHibernateException e1 ) {
                      Logger.error( PublisherAPIImpl.class, e.getMessage(), e1 );
                  }
                  Logger.error( PublisherAPIImpl.class, e.getMessage(), e );
                  throw new DotPublisherException( "Unable to add element to publish queue table:" + e.getMessage(), e );
              }
          }
    }

    private static final String TREE_QUERY = "select * from tree where child = ? or parent = ?";
	/**
	 * Get tree data of a content
	 * @param indentifier
	 * @return
	 * @throws DotPublisherException
	 */
	public List<Map<String,Object>> getContentTreeMatrix(String id) throws DotPublisherException {
		List<Map<String,Object>> res = null;
		DotConnect dc=new DotConnect();
		dc.setSQL(TREE_QUERY);
		dc.addParam(id);
		dc.addParam(id);

		try {
			res = dc.loadObjectResults();
		} catch (Exception e) {
			Logger.error(PublisherAPIImpl.class,e.getMessage(),e);
			throw new DotPublisherException("Unable find tree:" + e.getMessage(), e);
		}
		return res;
	}


	private static final String MULTI_TREE_QUERY = "select * from multi_tree where child = ?";
	/**
	 * Get multi tree data of a content
	 * @param indentifier
	 * @return
	 */
	public List<Map<String,Object>> getContentMultiTreeMatrix(String id) throws DotPublisherException {
		List<Map<String,Object>> res = null;
		DotConnect dc=new DotConnect();
		dc.setSQL(MULTI_TREE_QUERY);
		dc.addParam(id);

		try {
			res = dc.loadObjectResults();
		} catch (Exception e) {
			Logger.error(PublisherAPIImpl.class,e.getMessage(),e);
			throw new DotPublisherException("Unable find multi tree:" + e.getMessage(), e);
		}

		return res;
	}

	private static final String PSGETENTRIESBYSTATUS =
			"SELECT a.bundle_id, p.entered_date, p.asset, a.status, p.operation "+
			"FROM publishing_queue p, publishing_queue_audit a "+
			"where p.bundle_id = a.bundle_id "+
			"and a.status = ? ";
	private static final String MYGETENTRIESBYSTATUS =
			"SELECT a.bundle_id, p.entered_date, p.asset, a.status, p.operation "+
			"FROM publishing_queue p, publishing_queue_audit a "+
			"where p.bundle_id = a.bundle_id "+
			"and a.status = ? ";
	private static final String MSGETENTRIESBYSTATUS =
			"SELECT a.bundle_id, p.entered_date, p.asset, a.status, p.operation "+
			"FROM publishing_queue p, publishing_queue_audit a "+
			"where p.bundle_id = a.bundle_id "+
			"and a.status = ? ";
	private static final String OCLGETENTRIESBYSTATUS =
			"SELECT a.bundle_id, p.entered_date, p.asset, a.status, p.operation "+
			"FROM publishing_queue p, publishing_queue_audit a "+
			"where p.bundle_id = a.bundle_id "+
			"and a.status = ? ";

	public List<Map<String,Object>> getQueueElementsByStatus(Status status) throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSGETENTRIESBYSTATUS);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYGETENTRIESBYSTATUS);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSGETENTRIESBYSTATUS);
			}else{
				dc.setSQL(OCLGETENTRIESBYSTATUS);
			}

			dc.addParam(status.getCode());

			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	private static final String PSGETENTRIES =
			"SELECT * "+
			"FROM publishing_queue p order by bundle_id ";
	private static final String MYGETENTRIES =
			"SELECT * "+
			"FROM publishing_queue p order by bundle_id ";
	private static final String MSGETENTRIES =
			"SELECT * "+
			"FROM publishing_queue p order by bundle_id ";
	private static final String OCLGETENTRIES =
			"SELECT * "+
			"FROM publishing_queue p order by bundle_id ";

	public List<PublishQueueElement> getQueueElements() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSGETENTRIES);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYGETENTRIES);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSGETENTRIES);
			}else{
				dc.setSQL(OCLGETENTRIES);
			}

			return mapper.mapRows(dc.loadObjectResults());
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}

	private static final String PSCOUNTENTRIES="select count(*) as count from publishing_queue ";
	private static final String MYCOUNTENTRIES="select count(*) as count from publishing_queue ";
	private static final String MSCOUNTENTRIES="select count(*) as count from publishing_queue ";
	private static final String OCLCOUNTENTRIES="select count(*) as count from publishing_queue ";

	public Integer countQueueElements() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSCOUNTENTRIES);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYCOUNTENTRIES);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSCOUNTENTRIES);
			}else{
				dc.setSQL(OCLCOUNTENTRIES);
			}

			return Integer.parseInt(dc.loadObjectResults().get(0).get("count").toString());
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}

	private static final String PSGETENTRIESGROUPED=
			"SELECT a.bundle_id, p.entered_date, a.status, p.operation " +
			"FROM publishing_queue p, publishing_queue_audit a " +
			"where p.bundle_id = a.bundle_id group by bundle_id ";
	private static final String MYGETENTRIESGROUPED=
			"SELECT a.bundle_id, p.entered_date, a.status, p.operation " +
			"FROM publishing_queue p, publishing_queue_audit a " +
			"where p.bundle_id = a.bundle_id group by bundle_id ";
	private static final String MSGETENTRIESGROUPED=
			"SELECT a.bundle_id, p.entered_date, a.status, p.operation " +
			"FROM publishing_queue p, publishing_queue_audit a " +
			"where p.bundle_id = a.bundle_id group by bundle_id ";
	private static final String OCLGETENTRIESGROUPED=
			"SELECT a.bundle_id, p.entered_date, a.status, p.operation " +
			"FROM publishing_queue p, publishing_queue_audit a " +
			"where p.bundle_id = a.bundle_id group by bundle_id ";


	public List<Map<String,Object>> getQueueElementsGroupByBundleId() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSGETENTRIESGROUPED);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYGETENTRIESGROUPED);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSGETENTRIESGROUPED);
			}else{
				dc.setSQL(OCLGETENTRIESGROUPED);
			}
			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	public List<Map<String,Object>> getQueueElementsGroupByBundleId(String offset, String limit) throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSGETENTRIESGROUPED);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYGETENTRIESGROUPED);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSGETENTRIESGROUPED);
			}else{
				dc.setSQL(OCLGETENTRIESGROUPED);
			}

			dc.setStartRow(offset);
			dc.setMaxRows(limit);

			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	private static final String PSGETBUNDLES="select distinct(bundle_id) as bundle_id, publish_date, operation from publishing_queue where publish_date is not null order by publish_date";
	private static final String MYGETBUNDLES="select distinct(bundle_id) as bundle_id, publish_date, operation from publishing_queue where publish_date is not null order by publish_date";
	private static final String MSGETBUNDLES="select distinct(bundle_id) as bundle_id, publish_date, operation from publishing_queue where publish_date is not null order by publish_date";
	private static final String OCLGETBUNDLES="select distinct(bundle_id) as bundle_id, publish_date, operation from publishing_queue where publish_date is not null order by publish_date";

	private static final String COUNTBUNDLES="select count(distinct(bundle_id)) as bundle_count from publishing_queue ";

	/**
	 * Gets the count of the bundles to be published
	 * @return
	 */
	public Integer countQueueBundleIds() throws DotPublisherException {
		DotConnect dc = new DotConnect();
		dc.setSQL(COUNTBUNDLES);
		try{
			Object total = dc.loadObjectResults().get(0).get("bundle_count");
			return Integer.parseInt(total.toString());
		}
		catch(Exception e){
			Logger.error(PublisherAPIImpl.class, e.getMessage());
			throw new DotPublisherException(e.getMessage());
		}
	}




	/**
	 * get bundle_ids available
	 * @return List<Map<String,Object>>
	 * @throws DotPublisherException
	 */
	public List<Map<String,Object>> getQueueBundleIds(int limit, int offest) throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSGETBUNDLES);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYGETBUNDLES);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSGETBUNDLES);
			}else{
				dc.setSQL(OCLGETBUNDLES);
			}
			dc.setMaxRows(limit);
			dc.setStartRow(offest);
			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}

	private String SQLGETBUNDLESTOPROCESS =
			"select distinct(p.bundle_id) as bundle_id, " +
			"publish_date, operation, a.status "+
			"from publishing_queue p "+
			"left join publishing_queue_audit a "+
			"ON p.bundle_id=a.bundle_id "+
			"where "+
			"((a.status != ? and a.status != ?) or a.status is null ) and p.publish_date is not null "+
			"order by publish_date ASC,operation ASC";


	public List<Map<String, Object>> getQueueBundleIdsToProcess() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();

			dc.setSQL(SQLGETBUNDLESTOPROCESS);

			dc.addParam(Status.BUNDLE_SENT_SUCCESSFULLY.getCode());
			dc.addParam(Status.PUBLISHING_BUNDLE.getCode());
			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}


	private static final String PSGETENTRIESBYBUNDLE=
			"SELECT * "+
			"FROM publishing_queue p where bundle_id = ? order by asset ";
	private static final String MYGETENTRIESBYBUNDLE =
			"SELECT * "+
			"FROM publishing_queue p where bundle_id = ? order by asset ";
	private static final String MSGETENTRIESBYBUNDLE =
			"SELECT * "+
			"FROM publishing_queue p where bundle_id = ? order by asset ";
	private static final String OCLGETENTRIESBYBUNDLE =
			"SELECT * "+
			"FROM publishing_queue p where bundle_id = ? order by asset ";

	/**
	 * get queue elements by bundle_id
	 * @return List<Map<String,Object>>
	 * @throws DotPublisherException
	 */
	public List<PublishQueueElement> getQueueElementsByBundleId(String bundleId) throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSGETENTRIESBYBUNDLE);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYGETENTRIESBYBUNDLE);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSGETENTRIESBYBUNDLE);
			}else{
				dc.setSQL(OCLGETENTRIESBYBUNDLE);
			}

			dc.addParam(bundleId);

			return mapper.mapRows(dc.loadObjectResults());
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}

	private static final String PSCOUNTENTRIESGROUPED="select count(distinct(bundle_id)) as count from publishing_queue ";
	private static final String MYCOUNTENTRIESGROUPED="select count(distinct(bundle_id)) as count from publishing_queue ";
	private static final String MSCOUNTENTRIESGROUPED="select count(distinct(bundle_id)) as count from publishing_queue ";
	private static final String OCLCOUNTENTRIESGROUPED="select count(distinct(bundle_id)) as count from publishing_queue ";

	public Integer countQueueElementsGroupByBundleId() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSCOUNTENTRIESGROUPED);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYCOUNTENTRIESGROUPED);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSCOUNTENTRIESGROUPED);
			}else{
				dc.setSQL(OCLCOUNTENTRIESGROUPED);
			}

			return Integer.parseInt(dc.loadObjectResults().get(0).get("count").toString());
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	private static final String PSGETENTRY="select * from publishing_queue where asset = ?";
	private static final String MYGETENTRY="select * from publishing_queue where asset = ?";
	private static final String MSGETENTRY="select * from publishing_queue where asset = ?";
	private static final String OCLGETENTRY="select * from publishing_queue where asset = ?";

	public List<PublishQueueElement> getQueueElementsByAsset(String asset) throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSGETENTRY);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYGETENTRY);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSGETENTRY);
			}else{
				dc.setSQL(OCLGETENTRY);
			}

			dc.addParam(asset);

			return mapper.mapRows(dc.loadObjectResults());
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}


	/**
	 * update element from publishing_queue table by id
	 */
	private static final String PSUPDATEELEMENTFROMQUEUESQL="UPDATE publishing_queue SET last_try=?, num_of_tries=?, in_error=?, last_results=? where id=?";
	private static final String MYUPDATEELEMENTFROMQUEUESQL="UPDATE publishing_queue SET last_try=?, num_of_tries=?, in_error=?, last_results=? where id=?";
	private static final String MSUPDATEELEMENTFROMQUEUESQL="UPDATE publishing_queue SET last_try=?, num_of_tries=?, in_error=?, last_results=? where id=?";
	private static final String OCLUPDATEELEMENTFROMQUEUESQL="UPDATE publishing_queue SET last_try=?, num_of_tries=?, in_error=?, last_results=? where id=?";
	/**
	 * update element from publishing_queue table by id
	 * @param id ID of the element in the publishing_queue
	 * @param next_try date of the next intent to execute the query
	 * @param in_error bolean indication if there was an error
	 * @param last_results error message
	 * @throws DotPublisherException
	 */
	public void updateElementStatusFromPublishQueueTable(long id, Date last_try,int num_of_tries, boolean in_error,String last_results ) throws DotPublisherException{
		try{
			HibernateUtil.startTransaction();
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				/*Validate if the table doesn't exist then is created*/
				dc.setSQL(PSUPDATEELEMENTFROMQUEUESQL);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYUPDATEELEMENTFROMQUEUESQL);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSUPDATEELEMENTFROMQUEUESQL);
			}else{
				dc.setSQL(OCLUPDATEELEMENTFROMQUEUESQL);
			}
			dc.addParam(last_try);
			dc.addParam(num_of_tries);
			dc.addParam(in_error);
			dc.addParam(last_results);
			dc.addParam(id);
			dc.loadResult();
			HibernateUtil.commitTransaction();
		}catch(Exception e){
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.error(PublisherAPIImpl.class,e.getMessage(),e1);
			}
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to update element "+id+" :"+e.getMessage(), e);
		}
	}

	/**
	 * Delete element from publishing_queue table by id
	 */
	private static final String PSDELETEELEMENTFROMQUEUESQL="DELETE FROM publishing_queue where asset=?";
	private static final String MYDELETEELEMENTFROMQUEUESQL="DELETE FROM publishing_queue where asset=?";
	private static final String MSDELETEELEMENTFROMQUEUESQL="DELETE FROM publishing_queue where asset=?";
	private static final String OCLDELETEELEMENTFROMQUEUESQL="DELETE FROM publishing_queue where asset=?";
	/**
	 * Delete element from publishing_queue table by bundleId
	 * @param id ID of the element in the table
	 * @return boolean
	 * @throws DotPublisherException
	 */
	public void deleteElementFromPublishQueueTable(String identifier) throws DotPublisherException{
		try{
			HibernateUtil.startTransaction();
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				/*Validate if the table doesn't exist then is created*/
				dc.setSQL(PSDELETEELEMENTFROMQUEUESQL);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYDELETEELEMENTFROMQUEUESQL);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSDELETEELEMENTFROMQUEUESQL);
			}else{
				dc.setSQL(OCLDELETEELEMENTFROMQUEUESQL);
			}
			dc.addParam(identifier);
			dc.loadResult();
			HibernateUtil.commitTransaction();
		}catch(Exception e){
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.error(PublisherAPIImpl.class,e.getMessage(),e1);
			}
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to delete element "+identifier+" :"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}

	/**
	 * Delete element(s) from publishing_queue table by id
	 */
	private static final String PSDELETEELEMENTSFROMQUEUESQL="DELETE FROM publishing_queue where bundle_id=?";
	private static final String MYDELETEELEMENTSFROMQUEUESQL="DELETE FROM publishing_queue where bundle_id=?";
	private static final String MSDELETEELEMENTSFROMQUEUESQL="DELETE FROM publishing_queue where bundle_id=?";
	private static final String OCLDELETEELEMENTSFROMQUEUESQL="DELETE FROM publishing_queue where bundle_id=?";
	/**
	 * Delete element from publishing_queue table by bundleId
	 * @param id ID of the element in the table
	 * @return boolean
	 * @throws DotPublisherException
	 */
	public void deleteElementsFromPublishQueueTable(String bundleId) throws DotPublisherException{
		try{
			HibernateUtil.startTransaction();
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				/*Validate if the table doesn't exist then is created*/
				dc.setSQL(PSDELETEELEMENTSFROMQUEUESQL);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYDELETEELEMENTSFROMQUEUESQL);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSDELETEELEMENTSFROMQUEUESQL);
			}else{
				dc.setSQL(OCLDELETEELEMENTSFROMQUEUESQL);
			}
			dc.addParam(bundleId);
			dc.loadResult();
			HibernateUtil.commitTransaction();
		}catch(Exception e){
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.error(PublisherAPIImpl.class,e.getMessage(),e1);
			}
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to delete element(s) "+bundleId+" :"+e.getMessage(), e);
		}
	}

	private static final String PSDELETEALLELEMENTFROMQUEUESQL="DELETE FROM publishing_queue";
	private static final String MYDELETEALLELEMENTFROMQUEUESQL="DELETE FROM publishing_queue";
	private static final String MSDELETEALLELEMENTFROMQUEUESQL="DELETE FROM publishing_queue";
	private static final String OCLDELETEALLELEMENTFROMQUEUESQL="DELETE FROM publishing_queue";
	/**
	 * Delete all elements from publishing_queue table
	 * @return boolean
	 */
	public void deleteAllElementsFromPublishQueueTable() throws DotPublisherException{
		try{
			HibernateUtil.startTransaction();
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				/*Validate if the table doesn't exist then is created*/
				dc.setSQL(PSDELETEALLELEMENTFROMQUEUESQL);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYDELETEALLELEMENTFROMQUEUESQL);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSDELETEALLELEMENTFROMQUEUESQL);
			}else{
				dc.setSQL(OCLDELETEALLELEMENTFROMQUEUESQL);
			}
			dc.loadResult();
			HibernateUtil.commitTransaction();
		}catch(Exception e){
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.error(PublisherAPIImpl.class,e.getMessage(),e1);
			}
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to delete elements :"+e.getMessage(), e);
		}
	}

	private static final String MULTI_TREE_CONTAINER_QUERY = "select * from multi_tree where parent1 = ? or parent2 = ? or child = ?";

	@Override
	public List<Map<String, Object>> getContainerMultiTreeMatrix(String id) throws DotPublisherException {
		List<Map<String,Object>> res = null;
		DotConnect dc=new DotConnect();
		dc.setSQL(MULTI_TREE_CONTAINER_QUERY);
		dc.addParam(id);
		dc.addParam(id);
		dc.addParam(id);

		try {
			res = dc.loadObjectResults();
		} catch (Exception e) {
			Logger.error(PublisherAPIImpl.class,e.getMessage(),e);
			throw new DotPublisherException("Unable find multi tree:" + e.getMessage(), e);
		}

		return res;
	}
	@Override
	public void publishBundleAssets(String bundleId, Date publishDate)
			throws DotPublisherException {

		// update the already existing assets in the queue list with the publish operation and publish date

		DotConnect dc = new DotConnect();
        dc.setSQL( "UPDATE publishing_queue SET operation = ?, publish_date = ? where bundle_id = ?" );
        dc.addParam(ADD_OR_UPDATE_ELEMENT);
        dc.addParam(publishDate);
        dc.addParam(bundleId);

        try {
			dc.loadResult();
		} catch (DotDataException e) {
			Logger.error(getClass(), "Error updating bundles in publishing queue");
			throw new DotPublisherException("Error updating bundles in publishing queue", e);
		}

	}
	@Override
	public void unpublishBundleAssets(String bundleId, Date expireDate)
			throws DotPublisherException {

		// update the already existing assets in the queue list with the unpublish operation and expiration date

		DotConnect dc = new DotConnect();
        dc.setSQL( "UPDATE publishing_queue SET operation = ?, publish_date = ? where bundle_id = ?" );
        dc.addParam(DELETE_ELEMENT);
        dc.addParam(expireDate);
        dc.addParam(bundleId);

        try {
			dc.loadResult();
		} catch (DotDataException e) {
			Logger.error(getClass(), "Error updating bundles in publishing queue");
			throw new DotPublisherException("Error updating bundles in publishing queue", e);
		}

	}
	@Override
	public void publishAndExpireBundleAssets(String bundleId, Date publishDate,
			Date expireDate, User user) throws DotPublisherException {

		// update the already existing assets in the queue list with the publish operation and publish date

		publishBundleAssets(bundleId, publishDate);

        // insert a new version of each asset but with the unpublish operation and the expiration date

		List<PublishQueueElement> assets = getQueueElementsByBundleId(bundleId);

		List<String> ids = new ArrayList<String>();

		for (PublishQueueElement asset : assets) {
			ids.add(asset.getAsset());
		}

		addAssetsToQueue(ids, bundleId, expireDate, user, DELETE_ELEMENT);
	}

}
