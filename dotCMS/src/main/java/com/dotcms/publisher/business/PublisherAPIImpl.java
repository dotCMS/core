package com.dotcms.publisher.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.mapper.PublishQueueMapper;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.PublisherConfig.DeliveryStrategy;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.system.event.local.type.publish.AddedToQueueEvent;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * Provides utility methods to interact with asset information added to the
 * Publishing Queue. When the user selects one or more assets to be pushed, they
 * are added to a queue that the Push Publishing mechanism uses to add them to a
 * specific bundle and sends them to the destination server(s).
 * 
 * @author Alberto
 * @version 1.0
 * @since Oct 10, 2012
 *
 */
public class PublisherAPIImpl extends PublisherAPI{

	private final PublishQueueMapper mapper;

	private static PublisherAPIImpl instance= null;

	private final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();

	/**
	 * Returns a single instance of this class.
	 * 
	 * @return An instance of {@link PublisherAPI}.
	 */
	public static PublisherAPIImpl getInstance() {
		if(instance==null){
			instance = new PublisherAPIImpl();
		}

		return instance;
	}

	/**
	 * Default class constructor.
	 */
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

	private static final String INSERTSQL="insert into publishing_queue("+MANDATORY_FIELDS+") values("+MANDATORY_PLACE_HOLDER+")";
	private static final String SELECT_ASSET ="SELECT asset FROM publishing_queue WHERE bundle_id = ?";

	@Override
    public Map<String, Object> addContentsToPublish ( List<String> identifiers, String bundleId, Date publishDate, User user ) throws DotPublisherException {
    	return addAssetsToQueue(identifiers, bundleId, publishDate, user, ADD_OR_UPDATE_ELEMENT, DeliveryStrategy.ALL_ENDPOINTS);
    }

	@Override
    public Map<String, Object> addContentsToPublish ( List<String> identifiers, String bundleId, Date publishDate, User user, DeliveryStrategy deliveryStrategy) throws DotPublisherException {
    	return addAssetsToQueue(identifiers, bundleId, publishDate, user, ADD_OR_UPDATE_ELEMENT, deliveryStrategy);
    }

	@Override
    public Map<String, Object> addContentsToUnpublish ( List<String> identifiers, String bundleId, Date unpublishDate, User user ) throws DotPublisherException {
    	return addAssetsToQueue(identifiers, bundleId, unpublishDate, user, DELETE_ELEMENT, DeliveryStrategy.ALL_ENDPOINTS);
    }

	@Override
    public Map<String, Object> addContentsToUnpublish ( List<String> identifiers, String bundleId, Date unpublishDate, User user, DeliveryStrategy deliveryStrategy) throws DotPublisherException {
    	return addAssetsToQueue(identifiers, bundleId, unpublishDate, user, DELETE_ELEMENT, deliveryStrategy);
    }

    @Override
	public Map<String, Object> saveBundleAssets(List<String> identifiers, String bundleId,
			User user) throws DotPublisherException {
    	return addAssetsToQueue(identifiers, bundleId, null, user, -1, DeliveryStrategy.ALL_ENDPOINTS);
	}

	/**
	 * Adds a list of given identifiers to the Push Publish Queue. Each asset in
	 * the queue table will be analyzed and classified depending on its type:
	 * OSGi bundle, Content Type, Contentlet, Category, etc. An asset might not
	 * make it to the queue if one of the following conditions applies:
	 * <ol>
	 * <li>The user doesn't have permissions to access the selected asset(s).</li>
	 * <li>The type of asset is not currently supported by the Push Publish
	 * mechanism.</li>
	 * </ol>
	 *
	 * @param identifiers
	 *            - Identifiers to add to the Push Publish Queue.
	 * @param bundleId
	 *            - The id of the bundle the assets will be part of.
	 * @param operationDate
	 *            - When to apply the operation.
	 * @param user
	 *            - The current user.
	 * @param operationType
	 *            - Publish/Un-publish.
	 * @return A map with the results of the operation, this map contains 3
	 *         properties:
	 *         <ul>
	 *         <li><strong>total</strong>: The total number of assets added to
	 *         the queue.</li>
	 *         <li><strong>errors</strong>: The number of assets that failed to
	 *         be added to the queue.</li>
	 *         <li><strong>errorMessages</strong>: The list of error messages
	 *         for the failed assets.</li>
	 *         </ul>
	 * @throws DotPublisherException
	 *             An error occurred during the analysis of data that is being
	 *             added to the queue.
	 */
	@WrapInTransaction
    public Map<String, Object> addAssetsToQueue(final List<String> identifiers,
												 final String bundleId,
												 final Date operationDate,
												 final User user,
												 final long operationType,
												 final DeliveryStrategy deliveryStrategy) throws DotPublisherException {

        //Map to store the results and errors adding Assets to que Queue
        final Map<String, Object> resultMap = new HashMap<>();
        final List<String> errorsList = new ArrayList<>();

    	  if ( identifiers != null ) {
    		  String idToProcess = null;

              try {
				  Collection<String> assets = getAssets(bundleId);

				  for ( String identifier : identifiers ) {
					  idToProcess = identifier;

					  if (assets.contains(identifier)){
						throw new AssetAlreadyLinkWithBundleException( user, identifier );
					  }

                      DotConnect dc = new DotConnect();
                      dc.setSQL( INSERTSQL );
                      PermissionAPI strPerAPI = APILocator.getPermissionAPI();

                      String type = "";

                      //First verify what kind of element we want to publish in order to avoid unnecessary calls
                      if ( identifier.contains( "user_" ) ) {//Trying to publish a user
                          type = PusheableAsset.USER.getType();
                      } else if ( identifier.contains( ".jar" ) ) {//Trying to publish an OSGI jar bundle
                          type = PusheableAsset.OSGI.getType();
                      } else {
                          Identifier iden = APILocator.getIdentifierAPI().find( identifier );

                          if ( !UtilMethods.isSet( iden.getId() ) ) { // we have an inode, not an identifier
                              try {
                                  // check if it is a Content Type
                                  Structure contentType = null;
                                  List<Structure> contentTypes = StructureFactory.getStructures();
                                  for ( Structure contentTypeItem : contentTypes ) {
                                      if ( contentTypeItem.getInode().equals( identifier ) ) {
                                          contentType = contentTypeItem;
                                          type = PusheableAsset.CONTENT_TYPE.getType();
                                      }
                                  }
                                  if ( UtilMethods.isSet( contentType ) ) {
                                      if ( !strPerAPI.doesUserHavePermission( contentType, PermissionAPI.PERMISSION_PUBLISH, user ) ) {
                                          //Generate and append the error message
                                          appendError( errorsList, ErrorType.PERMISSION, user, "structure", contentType.getName(), contentType.getIdentifier() );
                                          continue;
                                      }
                                  }
                                  Folder folder;
								  Lazy<Optional<Experiment>> lazyExperiment = Lazy.of(()-> {
									  try {
										  return APILocator.getExperimentsAPI().find(identifier, user);
									  } catch (DotDataException | DotSecurityException e) {
										  throw new RuntimeException(e);
									  }
								  });

                                  // check if it is a category
                                  if ( !UtilMethods.isSet(type) && CATEGORY.equals( identifier ) ) {
                                      type = PusheableAsset.CATEGORY.getType();
                                  } // check if it is a language
                                  else if(!UtilMethods.isSet(type) && APILocator.getLanguageAPI().isAssetTypeLanguage(identifier)) {
                                      type = PusheableAsset.LANGUAGE.getType();
                                  } // Check if it's a Rule
                                  else if (!UtilMethods.isSet(type) && APILocator.getRulesAPI()
  										.getRuleById(identifier, user, false) != null) {
                                	  type = PusheableAsset.RULE.getType();
                                  }
                                  // check if it is a folder
                                  else if ( !UtilMethods.isSet(type) && UtilMethods.isSet( folder = APILocator.getFolderAPI().find( identifier, user, false ) ) ) {
                                      if ( !strPerAPI.doesUserHavePermission( folder, PermissionAPI.PERMISSION_PUBLISH, user ) ) {
                                          //Generate and append the error message
                                          appendError( errorsList, ErrorType.PERMISSION, user, "folder", folder.getName(), folder.getIdentifier() );
                                          continue;
                                      }
                                      type = PusheableAsset.FOLDER.getType();
                                  }  // check if it is an Experiment
								  else if ( !UtilMethods.isSet(type) && lazyExperiment.get().isPresent()) {
									  final Experiment experiment = lazyExperiment.get().get();
									  Logger.info(this, "Experiment found: " + experiment.pageId());
									  Logger.info(this, "Content: " + APILocator.getContentletAPI()
											  .findContentletByIdentifierAnyLanguage(experiment.pageId()));

									  final HTMLPageAsset pageAsset = APILocator.getHTMLPageAssetAPI().fromContentlet(
											  APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(experiment.pageId()));

									  Logger.info(this, "Page asset found: " + pageAsset);

									  if ( !strPerAPI.doesUserHavePermission( pageAsset, PermissionAPI.PERMISSION_PUBLISH, user ) ) {
										  //Generate and append the error message
										  appendError( errorsList, ErrorType.PERMISSION, user, "page", pageAsset.getName(), pageAsset.getIdentifier() );
										  continue;
									  }
									  type = PusheableAsset.EXPERIMENT.getType();
								  }
                              } catch ( Exception ex ) {
                            	  try {
									if ( UtilMethods.isSet( APILocator.getWorkflowAPI().findScheme(identifier) )) {
										  type = PusheableAsset.WORKFLOW.getType();
									  }
									} catch (DotDataException e) {
										// The identifier to process cannot be 
										// mapped to a valid asset type
										appendError(errorsList, ErrorType.DATA, user, type, "", identifier);
									}
                              }
                          } else {
							  if ( !iden.getAssetType().equalsIgnoreCase("folder") && !strPerAPI.doesUserHavePermission( iden, PermissionAPI.PERMISSION_PUBLISH, user ) ) {
                                  //Generate and append the error message
                                  appendError( errorsList, ErrorType.PERMISSION, user, iden.getAssetType(), null, iden.getId() );
                                  continue;
                              }
                              type = UtilMethods.isSet( APILocator.getHostAPI().find( identifier, user, false ) ) ? PusheableAsset.SITE.getType() : iden.getAssetType();
                          }
                      }

                      String action = operationType==ADD_OR_UPDATE_ELEMENT?"Publish":operationType==DELETE_ELEMENT?"Delete":"Added by Browsing";
                      dc.addParam( operationType );

                      dc.addObject( identifier ); //asset
                      dc.addParam( new Date() ); // entered date
                      dc.addObject( 1 ); // language id //TODO: We are not using this property, we will try to push the content regardless of the language

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
              } catch ( Exception e ) {

                  Logger.error( PublisherAPIImpl.class, e.getMessage(), e );
                  throw new DotPublisherException( "Unable to add element " + idToProcess + " to publish queue table: " + e.getMessage(), e );
              }
		  }

    	  Map<String, Object> dataMap = Map.of("deliveryStrategy", deliveryStrategy);
		  firePublisherQueueNow(dataMap);

		  //Preparing and returning the response status object
		  resultMap.put( "errorMessages", errorsList );
		  resultMap.put( "errors", errorsList.size() );
		  resultMap.put( "bundleId", bundleId );
		  resultMap.put( "total", identifiers != null ? identifiers.size() : 0 );

		  //Triggering event listener
		  try {

			  HibernateUtil.addCommitListener(() -> this.sendQueueElements(bundleId), 1000);
		  } catch (DotHibernateException e) {
			  Logger.error(this, e.getMessage(), e);
		  }

		return resultMap;
    }

    private void sendQueueElements (final String bundleId) {

		try {

			this.localSystemEventsAPI.asyncNotify(new AddedToQueueEvent(getQueueElementsByBundleId(bundleId)));
		} catch (DotPublisherException e) {
			Logger.error(this, e.getMessage(), e);
		}
	}

    @Override
    public void firePublisherQueueNow(Map<String, Object> dataMap){
		try {
		    Scheduler sched = QuartzUtils.getScheduler();
		    JobDetail job = sched.getJobDetail("PublishQueueJob"  , "dotcms_jobs");
			if(job==null) {
				return;
			}
			job.setJobDataMap(new JobDataMap(dataMap));
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.SECOND, Config.getIntProperty("PUSH_PUBLISHING_FIRING_DELAY_SEC", 2));
			//SCHEDULE PUBLISH QUEUE JOB for NOW
			Trigger trigger = new SimpleTrigger("PublishQueueJob"+ System.currentTimeMillis(),calendar.getTime() );
			trigger.setJobName(job.getName());
			trigger.setJobGroup(job.getGroup());
			trigger.setJobDataMap(job.getJobDataMap());
			sched.scheduleJob(trigger);
		} catch (ObjectAlreadyExistsException e) {
			// Quartz will throw this error if it is already running
		    Logger.debug(this.getClass(), e.getMessage(),e);
		} catch (Exception e) {
		    Logger.error(this.getClass(), e.getMessage(),e);
		}
    }

    /**
     * 
     * @param bundleId
     * @return
     * @throws DotDataException
     */
	private Collection<String> getAssets(String bundleId) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL( SELECT_ASSET );

		dc.addParam( bundleId );
		List<Map<String, Object>> maps = dc.loadObjectResults();
		List<String> result = new ArrayList<>();

		for (Map<String, Object> map : maps) {
			Object asset = map.get("asset");
			result.add( asset.toString() );
		}

		return result;
	}

	/**
	 * Utility enum to identify the type of error that might be generated during
	 * the addition of assets to the publishing queue.
	 * 
	 * @author Jose Castro
	 * @version 1.0
	 * @since Mar 9, 2016
	 *
	 */
    private enum ErrorType {
		PERMISSION,
		DATA
	}

	/**
	 * Generate and append error messages in case anything goes wrong when
	 * adding the selected assets to the publishing queue. An asset might failed
	 * to be added to the queue under the following circumstances:
	 * <ul>
	 * <li>The user does not have permissions to access it.</li>
	 * <li>The asset type is not valid for Push Publish (is not pusheable).</li>
	 * <li>The asset does not exist in the database anymore.</li>
	 * </ul>
	 * 
	 * @param errorsList
	 *            - The list of errors reported during the execution process.
	 * @param errorType
	 *            - The {@link ErrorType} marking the root cause of the problem.
	 * @param user
	 *            - The {@link User} performing the action.
	 * @param assetType
	 *            - The type of asset that presented a problem (optional).
	 * @param assetName
	 *            - The name of the asset that presented a problem (optional).
	 * @param identifier
	 *            - The asset identifier that presented a problem.
	 */
    private void appendError ( List<String> errorsList, ErrorType errorType, User user, String assetType, String assetName, String identifier ) {

        //First we should get the authentication type for this company
        Company company = PublicCompanyFactory.getDefaultCompany();
        String authType = company.getAuthType();

        String userData;
        if ( authType.equals( Company.AUTH_TYPE_ID ) ) {
            userData = user.getUserId();
        } else {
            userData = user.getEmailAddress();
        }

        if (assetName == null) {
            assetName = PublishAuditUtil.getInstance().getTitle( assetType, identifier );
        }

        //Generate and append the error message
		String logErrorMessage = "";
		String uiErrorMessage = "";
		switch (errorType) {
		case PERMISSION:
			logErrorMessage = "User: " + userData + " does not have Publish Permission over " + assetType + ": "
					+ identifier;
			uiErrorMessage = "User: " + userData + " does not have Publish Permission over " + assetType + ": " + assetName;
			break;
		case DATA:
			logErrorMessage = "Object " + identifier + " cannot be mapped to a valid pusheable type";
			uiErrorMessage = "Object " + (StringUtils.isNotBlank(assetName) ? assetName : identifier)
					+ " cannot be mapped to a valid pusheable type";
			break;
		}
        Logger.warn( PublisherAPIImpl.class, logErrorMessage );
        errorsList.add( uiErrorMessage );
    }

    private static final String TREE_QUERY = "select * from tree where child = ? or parent = ?";

    @CloseDBIfOpened
    @Override
	public List<Map<String,Object>> getContentTreeMatrix(final String id) throws DotPublisherException {
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

    private static final String MULTI_TREE_QUERY = "select multi_tree.* from multi_tree join contentlet_version_info page_version on page_version.identifier = multi_tree.parent1 "
    		+ "join container_version_info on container_version_info.identifier = multi_tree.parent2 "
    		+ "join contentlet_version_info on contentlet_version_info.identifier = multi_tree.child where multi_tree.child = ? "
    		+ "and page_version.deleted = ? and container_version_info.deleted = ? and contentlet_version_info.deleted = ?";

	@CloseDBIfOpened
	@Override
	public List<Map<String,Object>> getContentMultiTreeMatrix(final String id) throws DotPublisherException {
		List<Map<String,Object>> res = null;
		DotConnect dc=new DotConnect();
		dc.setSQL(MULTI_TREE_QUERY);
		dc.addParam(id);
		dc.addParam(Boolean.FALSE);
		dc.addParam(Boolean.FALSE);
		dc.addParam(Boolean.FALSE);
		try {
			res = dc.loadObjectResults();
		} catch (Exception e) {
			Logger.error(PublisherAPIImpl.class,e.getMessage(),e);
			throw new DotPublisherException("Unable find multi tree:" + e.getMessage(), e);
		}

		return res;
	}

	private static final String GETENTRIESBYSTATUS =
			"SELECT a.bundle_id, p.entered_date, p.asset, a.status, p.operation "+
			"FROM publishing_queue p, publishing_queue_audit a "+
			"where p.bundle_id = a.bundle_id "+
			"and a.status = ? ";

	@CloseDBIfOpened
	@Override
	public List<Map<String,Object>> getQueueElementsByStatus(Status status) throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(GETENTRIESBYSTATUS);

			dc.addParam(status.getCode());

			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	private static final String GETENTRIES =
			"SELECT * "+
			"FROM publishing_queue p order by bundle_id ";

	@Override
	@CloseDBIfOpened
	public List<PublishQueueElement> getQueueElements() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(GETENTRIES);
			return mapper.mapRows(dc.loadObjectResults());
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	private static final String COUNTENTRIES="select count(*) as count from publishing_queue ";

	@Override
	@CloseDBIfOpened
	public Integer countQueueElements() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(COUNTENTRIES);

			return Integer.parseInt(dc.loadObjectResults().get(0).get("count").toString());
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	private static final String ETENTRIESGROUPED=
			"SELECT a.bundle_id, p.entered_date, a.status, p.operation " +
			"FROM publishing_queue p, publishing_queue_audit a " +
			"where p.bundle_id = a.bundle_id group by bundle_id ";

	@CloseDBIfOpened
	@Override
	public List<Map<String,Object>> getQueueElementsGroupByBundleId() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(ETENTRIESGROUPED);
			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	@CloseDBIfOpened
	@Override
	public List<Map<String,Object>> getQueueElementsGroupByBundleId(String offset, String limit) throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(ETENTRIESGROUPED);
			dc.setStartRow(offset);
			dc.setMaxRows(limit);

			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	private static final String ETBUNDLES="select distinct(bundle_id) as bundle_id, publish_date, operation from publishing_queue where publish_date is not null order by publish_date";

	private static final String COUNTBUNDLES="select count(distinct(bundle_id)) as bundle_count from publishing_queue where publish_date is not null";

	@CloseDBIfOpened
	@Override
	public Integer countQueueBundleIds() throws DotPublisherException {
		DotConnect dc = new DotConnect();
		dc.setSQL(COUNTBUNDLES);
		try{
			Object total = dc.loadObjectResults().get(0).get("bundle_count");
			return Integer.parseInt(total.toString());
		}
		catch(Exception e){
			Logger.error(PublisherAPIImpl.class, e.getMessage());
			throw new DotPublisherException(e.getMessage(),e);
		}
	}

	@Override
	@CloseDBIfOpened
	public List<Map<String,Object>> getQueueBundleIds(final int limit, final int offest) throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(ETBUNDLES);
			dc.setMaxRows(limit);
			dc.setStartRow(offest);
			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	private String SQLGETBUNDLESTOPROCESS =
			"select distinct(p.bundle_id) as bundle_id, " +
			"publish_date, operation, a.status "+
			"from publishing_queue p "+
			"left join publishing_queue_audit a "+
			"ON p.bundle_id=a.bundle_id "+
			"where "+
			"((a.status != ? and a.status != ? AND a.status != ? AND a.status != ?) or a.status is null ) "+
			"and p.publish_date is not null and p.publish_date <= ? "+
			"order by publish_date ASC,operation ASC";

	@Override
	@CloseDBIfOpened
	public List<Map<String, Object>> getQueueBundleIdsToProcess() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SQLGETBUNDLESTOPROCESS);
			dc.addParam(Status.BUNDLE_SENT_SUCCESSFULLY.getCode());
			dc.addParam(Status.PUBLISHING_BUNDLE.getCode());
			dc.addParam(Status.WAITING_FOR_PUBLISHING.getCode());
			dc.addParam(Status.FAILED_INTEGRITY_CHECK.getCode());
			dc.addParam(new Date());
			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of bundles to process: " + e.getMessage(), e);
		}
	}

	private static final String GETENTRIESBYBUNDLE=
			"SELECT * "+
			"FROM publishing_queue p where bundle_id = ? order by asset ";

	@Override
	@CloseDBIfOpened
	public List<PublishQueueElement> getQueueElementsByBundleId(String bundleId) throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(GETENTRIESBYBUNDLE);
			dc.addParam(bundleId);

			return mapper.mapRows(dc.loadObjectResults());
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	private static final String COUNTENTRIESGROUPED="select count(distinct(bundle_id)) as count from publishing_queue ";

	@Override
	@CloseDBIfOpened
	public Integer countQueueElementsGroupByBundleId() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(COUNTENTRIESGROUPED);
			return Integer.parseInt(dc.loadObjectResults().get(0).get("count").toString());
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	private static final String GETENTRY="select * from publishing_queue where asset = ?";

	@CloseDBIfOpened
	@Override
	public List<PublishQueueElement> getQueueElementsByAsset(final String asset) throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(GETENTRY);

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
	private static final String UPDATEELEMENTFROMQUEUESQL="UPDATE publishing_queue SET last_try=?, num_of_tries=?, in_error=?, last_results=? where id=?";

	@WrapInTransaction
	@Override
	public void updateElementStatusFromPublishQueueTable(final long id,
														 final Date lastTry,
														 final int numOfTries,
														 final boolean inError,
														 final String lastResults ) throws DotPublisherException {


		try {
			final DotConnect dc = new DotConnect();
			dc.setSQL(UPDATEELEMENTFROMQUEUESQL);
			dc.addParam(lastTry);
			dc.addParam(numOfTries);
			dc.addParam(inError);
			dc.addParam(lastResults);
			dc.addParam(id);
			dc.loadResult();
		} catch(Exception e) {

			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to update element "+id+" :"+e.getMessage(), e);
		}
	}

	/**
	 * Delete element from publishing_queue table by id
	 */
	private static final String DELETEELEMENTFROMQUEUESQL="DELETE FROM publishing_queue where asset=?";

	private static final String DELETE_ELEMENT_IN_LANGUAGE_FROM_QUEUE = "DELETE FROM publishing_queue WHERE asset = ? AND language_id = ?";

	@Override
	public void deleteElementFromPublishQueueTable(final String identifier) throws DotPublisherException{
		deleteElementFromPublishQueueTable(identifier, 0);
	}

	@Override
	public void deleteElementFromPublishQueueTableAndAuditStatus(final String identifier) throws DotPublisherException{
		final List<PublishQueueElement> queueElements = getQueueElementsByAsset(identifier);
		String bundleId = Try.of(()->queueElements.get(0).getBundleId()).getOrNull();

		deleteElementFromPublishQueueTable(identifier, 0);

		List<PublishQueueElement> queueElementsAfterDelete = Try.of(()->PublisherAPI.getInstance()
				.getQueueElementsByBundleId(bundleId)).getOrElse(Collections::emptyList);

		if(queueElementsAfterDelete.isEmpty()) {
			APILocator.getPublishAuditAPI().deletePublishAuditStatus(bundleId);
		}
	}

	@WrapInTransaction
	@Override
	public void deleteElementFromPublishQueueTable(final String identifier, final long languageId) throws DotPublisherException{
		try{
			final DotConnect dc = new DotConnect();
			if (languageId > 0) {
				dc.setSQL(DELETE_ELEMENT_IN_LANGUAGE_FROM_QUEUE);
				dc.addParam(identifier);
				dc.addParam(languageId);
			} else {
				dc.setSQL(DELETEELEMENTFROMQUEUESQL);
				dc.addParam(identifier);
			}
			dc.loadResult();
		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to delete element "+identifier+" :"+e.getMessage(), e);
		}
	}

	@WrapInTransaction
	@Override
	public void deleteElementsFromPublishQueueTable(final List<String> identifiers, final long languageId) throws DotPublisherException{
		try{
			final List<Params> params = list();

			for (String identifier : identifiers) {
				if(languageId > 0) {
					params.add(new Params.Builder().add(identifier, languageId).build());
				} else {
					params.add(new Params.Builder().add(identifier).build());
				}
			}

			final DotConnect dc = new DotConnect();
			String sql;

			if (languageId > 0) {
				sql = DELETE_ELEMENT_IN_LANGUAGE_FROM_QUEUE;
			} else {
				sql = DELETEELEMENTFROMQUEUESQL;
			}

			dc.executeBatch(sql, params);

		}catch(Exception e){
			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to delete elements from Publish Queue :"+e.getMessage(), e);
		}
	}

	/**
	 * Delete element(s) from publishing_queue table by id
	 */
	private static final String DELETEELEMENTSFROMQUEUESQL="DELETE FROM publishing_queue where bundle_id=?";


	@WrapInTransaction
	@Override
	public void deleteElementsFromPublishQueueTableAndAuditStatus(final String bundleId) throws DotPublisherException{

		deleteElementsFromPublishQueueTable(bundleId);

		APILocator.getPublishAuditAPI().deletePublishAuditStatus(bundleId);
	}

	@WrapInTransaction
	@Override
	public void deleteElementsFromPublishQueueTable(final String bundleId) throws DotPublisherException{

		try{
			final DotConnect dc = new DotConnect();
			dc.setSQL(DELETEELEMENTSFROMQUEUESQL);
			dc.addParam(bundleId);
			dc.loadResult();
		}catch(Exception e){

			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to delete element(s) "+bundleId+" :"+e.getMessage(), e);
		}
	}

	private static final String DELETEALLELEMENTFROMQUEUESQL="DELETE FROM publishing_queue";

	@WrapInTransaction
	@Override
	public void deleteAllElementsFromPublishQueueTable() throws DotPublisherException{

		try{
			final DotConnect dc = new DotConnect();
			dc.setSQL(DELETEALLELEMENTFROMQUEUESQL);
			dc.loadResult();
		} catch(Exception e){

			Logger.error(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to delete elements :"+e.getMessage(), e);
		}
	}

	private static final String MULTI_TREE_CONTAINER_QUERY = new StringBuilder("select multi_tree.* from multi_tree ")
	.append("join contentlet_version_info page_version on page_version.identifier = multi_tree.parent1 ")
    .append("join container_version_info on container_version_info.identifier = multi_tree.parent2 ")
    .append("join contentlet_version_info on contentlet_version_info.identifier = multi_tree.child ")
    .append("where multi_tree.parent1 = ? ")
    .append("and (page_version.deleted = ? ")
	.append("and container_version_info.deleted = ? ")
    .append("and contentlet_version_info.deleted = ?) ")
    .append("group by multi_tree.child, multi_tree.parent1, multi_tree.parent2, multi_tree.relation_type, multi_tree.tree_order")
    .append(" UNION ALL ")
    .append("select multi_tree.* from multi_tree ")
	.append("join contentlet_version_info page_version on page_version.identifier = multi_tree.parent1 ")
    .append("join container_version_info on container_version_info.identifier = multi_tree.parent2 ")
    .append("join contentlet_version_info on contentlet_version_info.identifier = multi_tree.child ")
    .append("where multi_tree.parent2 = ? ")
    .append("and (page_version.deleted = ? ")
	.append("and container_version_info.deleted = ? ")
    .append("and contentlet_version_info.deleted = ?) ")
    .append("group by multi_tree.child, multi_tree.parent1, multi_tree.parent2, multi_tree.relation_type, multi_tree.tree_order")
    .append(" UNION ALL ")
    .append("select multi_tree.* from multi_tree ")
	.append("join contentlet_version_info page_version on page_version.identifier = multi_tree.parent1 ")
    .append("join container_version_info on container_version_info.identifier = multi_tree.parent2 ")
    .append("join contentlet_version_info on contentlet_version_info.identifier = multi_tree.child ")
    .append("where multi_tree.child = ? ")
    .append("and (page_version.deleted = ? ")
	.append("and container_version_info.deleted = ? ")
    .append("and contentlet_version_info.deleted = ?) ")
    .append("group by multi_tree.child, multi_tree.parent1, multi_tree.parent2, multi_tree.relation_type, multi_tree.tree_order").toString();

	@CloseDBIfOpened
	@Override
	public List<Map<String, Object>> getContainerMultiTreeMatrix(final String id) throws DotPublisherException {
		List<Map<String,Object>> res;
		final DotConnect dc=new DotConnect();
		dc.setSQL(MULTI_TREE_CONTAINER_QUERY);
		dc.addParam(id);
		dc.addParam(Boolean.FALSE);
		dc.addParam(Boolean.FALSE);
		dc.addParam(Boolean.FALSE);
		dc.addParam(id);
		dc.addParam(Boolean.FALSE);
		dc.addParam(Boolean.FALSE);
		dc.addParam(Boolean.FALSE);
		dc.addParam(id);
		dc.addParam(Boolean.FALSE);
		dc.addParam(Boolean.FALSE);
		dc.addParam(Boolean.FALSE);

		try {
			res = dc.loadObjectResults();
		} catch (Exception e) {
			Logger.error(PublisherAPIImpl.class,e.getMessage(),e);
			throw new DotPublisherException("Unable find multi tree:" + e.getMessage(), e);
		}

		return res;
	}

	@WrapInTransaction
	@Override
	public void publishBundleAssets(final String bundleId, final Date publishDate)
			throws DotPublisherException {

		// update the already existing assets in the queue list with the publish operation and publish date

		final DotConnect dc = new DotConnect();
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

	@WrapInTransaction
	@Override
	public void unpublishBundleAssets(final String bundleId, final Date expireDate)
			throws DotPublisherException {

		// update the already existing assets in the queue list with the unpublish operation and expiration date

		final DotConnect dc = new DotConnect();
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

	@WrapInTransaction
	@Override
	public void publishAndExpireBundleAssets(final String bundleId, final Date publishDate,
											 final Date expireDate, final User user) throws DotPublisherException {

		// update the already existing assets in the queue list with the publish operation and publish date

		publishBundleAssets(bundleId, publishDate);

        // insert a new version of each asset but with the unpublish operation, the expiration date AND a NEW BUNDLE ID

		List<PublishQueueElement> assets = getQueueElementsByBundleId(bundleId);

		List<String> ids = new ArrayList<>();

		for (PublishQueueElement asset : assets) {
			ids.add(asset.getAsset());
		}

		try {

			Bundle publishBundle = APILocator.getBundleAPI().getBundleById(bundleId);
			List<Environment> envsToSendTo = APILocator.getEnvironmentAPI().findEnvironmentsByBundleId(bundleId);

			Bundle deleteBundle = new Bundle(publishBundle.getName(), publishDate, expireDate, user.getUserId(), publishBundle.isForcePush(),publishBundle.getFilterKey());
	    	APILocator.getBundleAPI().saveBundle(deleteBundle, envsToSendTo);

	        addContentsToUnpublish( ids, deleteBundle.getId(), expireDate, user );
		} catch (DotDataException e) {
			throw new DotPublisherException(e);
		}

	}

}
