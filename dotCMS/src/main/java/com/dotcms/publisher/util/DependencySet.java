package com.dotcms.publisher.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.publisher.assets.business.PushedAssetsCache;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publishing.PublisherConfiguration;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.util.AnnotationUtils;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;

public class DependencySet extends HashSet<String> {

	/**
	 *
	 */
	private static final long serialVersionUID = 3048299770146564147L;
	private PushedAssetsCache cache;
	private List<Environment> envs = new ArrayList<Environment>();
	private String assetType;
	private String bundleId;
	private Bundle bundle;
	private boolean isDownload;
	private boolean isPublish;
	private Map<String,String> environmentsEndpointsAndPublisher = new HashMap<String, String>();
	private static final String ENDPOINTS_SUFFIX = "_endpointIds";
	private static final String PUBLISHER_SUFFIX = "_publisher"; 

	public DependencySet(String bundleId, String assetType, boolean isDownload, boolean isPublish, boolean isStatic) {
		super();
		cache = CacheLocator.getPushedAssetsCache();
		this.assetType = assetType;
		this.bundleId = bundleId;
		this.isDownload = isDownload;
		this.isPublish = isPublish;

		try {
			envs = APILocator.getEnvironmentAPI().findEnvironmentsByBundleId(bundleId);
		} catch (DotDataException e) {
			Logger.error(getClass(), "Can't get environments", e);
		}
		try {
			for (Environment env : envs) {
				String endpointIds = null;
				String publisher = null;

				List<PublishingEndPoint> allEndpoints = APILocator.getPublisherEndPointAPI().findSendingEndPointsByEnvironment(env.getId());
				List<String> endpoints = new ArrayList<String>();
				List<String> publishers = new ArrayList<String>();
				//Filter Endpoints list
				for(PublishingEndPoint ep : allEndpoints) {
					Class endPointPublisher = ep.getPublisher();
					PublisherConfiguration result = AnnotationUtils.getBeanAnnotation(endPointPublisher, PublisherConfiguration.class);
					boolean isStaticEndpoint = result == null? false: result.isStatic();
					
					if(isStatic && ep.isEnabled() && isStaticEndpoint) {
						//If the isStatic variable is true then get all the static endpoints
						endpoints.add(ep.getId());
						//Set that class name of the puh publisher used by these endpoints
						if(!publishers.contains(endPointPublisher.getName())){
							publishers.add(endPointPublisher.getName());
						}
					}else if(!isStatic && ep.isEnabled() && !isStaticEndpoint) {
						//If the isStatic variable is false then get all the dynamic endpoints
						endpoints.add(ep.getId());
						//Set that class name of the puh publisher used by these endpoints
						if(!publishers.contains(endPointPublisher.getName())){
							publishers.add(endPointPublisher.getName());
						}
					}
				}
				publisher = StringUtils.join(publishers,",");
				//comma separated string with the list of endpoint ids 
				endpointIds = StringUtils.join(endpoints,",");
				if(!env.getPushToAll()) {
					if(endpointIds != null && endpointIds.indexOf(",") != -1){
						endpointIds = endpointIds.substring(0, endpointIds.indexOf(","));
					}
				}
				
				//Add environment endpoints and publisher to map
				environmentsEndpointsAndPublisher.put(env.getId()+ENDPOINTS_SUFFIX, endpointIds);
				environmentsEndpointsAndPublisher.put(env.getId()+PUBLISHER_SUFFIX, publisher);
				
			}
			//Search and remove all the environments that are not going to be used by this dependencySet
			List<Environment> removeEnvironmentsList = new ArrayList<Environment>();
			for (Environment env : envs) {
				if(StringUtils.isEmpty(environmentsEndpointsAndPublisher.get(env.getId()+ENDPOINTS_SUFFIX))){
					removeEnvironmentsList.add(env);
				}
			}
			if(!removeEnvironmentsList.isEmpty()){
				envs.removeAll(removeEnvironmentsList);
			}
		} catch (SecurityException | IllegalArgumentException e) {
			Logger.error(getClass(), "Can't get environments endpoints and publishers", e);
		} catch (DotDataException e) {
			Logger.error(getClass(), "Can't get environments endpoints and publishers", e);
		}

		try {
			bundle = APILocator.getBundleAPI().getBundleById(bundleId);
		} catch (DotDataException e) {
			Logger.error(getClass(), "Can't get bundle. Bundle Id: " + bundleId , e);
		}
	}

	public boolean add(String assetId, Date assetModDate) {
		return addOrClean( assetId, assetModDate, false);
	}

	/**
	 * Is this method is called and in case of an <strong>UN-PUBLISH</strong> instead of adding elements it will remove them
	 * from cache.<br>
	 * For <strong>PUBLISH</strong> do the same as the <strong>add</strong> method.
	 *
	 * @param assetId
	 * @param assetModDate
	 * @return
	 */
	public boolean addOrClean ( String assetId, Date assetModDate) {
		return addOrClean( assetId, assetModDate, true);
	}

	private boolean addOrClean ( String assetId, Date assetModDate, Boolean cleanForUnpublish) {

		if ( !isPublish ) {

			//For un-publish we always remove the asset from cache
			for ( Environment env : envs ) {
				cache.removePushedAssetById( assetId, env.getId() );
			}

			//Return if we are here just to clean up dependencies from cache
			if ( cleanForUnpublish ) {
				return true;
			}
		}

		// check if it was already added to the set
		if(super.contains(assetId)) {
			return true;
		}

		boolean modifiedOnCurrentEnv = false;
		boolean modifiedOnAtLeastOneEnv = false;

		// we need to check if all environments have the last version of the asset in
		// order to skip adding it to the Set

		// if the asset hasn't been sent to at least one environment or an older version was sen't,
		// we need to add it to the cache

		Boolean isForcePush = false;
		if ( bundle != null ) {
			isForcePush = bundle.isForcePush();
		}

		if ( !isForcePush && !isDownload && isPublish ) {
			for (Environment env : envs) {
				PushedAsset asset;
				try {
					//Search the last pushed entry register of the pushed asset by asset Id, environment Id and endpoints Ids
					asset = APILocator.getPushedAssetsAPI().getLastPushForAsset(assetId, env.getId(), environmentsEndpointsAndPublisher.get(env.getId()+ENDPOINTS_SUFFIX));

				} catch (DotDataException e1) {
					// Asset does not exist in db or cache, return true;
					return true;
				}

				modifiedOnCurrentEnv = (asset==null || (assetModDate!=null && asset.getPushDate().before(assetModDate)));

				try {
					if(!modifiedOnCurrentEnv && assetType.equals("content")) {
						// check for versionInfo TS on content
						for(Language lang : APILocator.getLanguageAPI().getLanguages()) {
							ContentletVersionInfo info=APILocator.getVersionableAPI().getContentletVersionInfo(assetId, lang.getId());
							if(info!=null && InodeUtils.isSet(info.getIdentifier())) {
								modifiedOnCurrentEnv = modifiedOnCurrentEnv || (null == info.getVersionTs()) || asset.getPushDate().before(info.getVersionTs());
							}
						}
					}
					if(!modifiedOnCurrentEnv && (assetType.equals("template") || assetType.equals("links") || assetType.equals("container") || assetType.equals("htmlpage"))) {
						// check for versionInfo TS
						VersionInfo info=APILocator.getVersionableAPI().getVersionInfo(assetId);
						if(info!=null && InodeUtils.isSet(info.getIdentifier())) {
							modifiedOnCurrentEnv = asset.getPushDate().before(info.getVersionTs());
						}
					}
				} catch (Exception e) {
					Logger.warn(getClass(), "Error checking versionInfo for assetType:"+assetType+" assetId:"+assetId+
							" process continues without checking versionInfo.ts",e);
				}

				if(modifiedOnCurrentEnv) {
					try {
						//Insert the new pushed asset indicating to wish endpoints will be sent and with what publisher class
						asset = new PushedAsset(bundleId, assetId, assetType, new Date(), env.getId(), environmentsEndpointsAndPublisher.get(env.getId()+ENDPOINTS_SUFFIX), environmentsEndpointsAndPublisher.get(env.getId()+PUBLISHER_SUFFIX));

						APILocator.getPushedAssetsAPI().savePushedAsset(asset);
						//If the asset was modified at least in one environment, set this to true
						modifiedOnAtLeastOneEnv = true;
					} catch (DotDataException e) {
						Logger.error(getClass(), "Could not save PushedAsset. "
								+ "AssetId: " + assetId + ". AssetType: " + assetType + ". Env Id: " + env.getId(), e);
					}
				}
			}
		}

		if ( isForcePush || isDownload || !isPublish || modifiedOnAtLeastOneEnv ) {
			super.add( assetId );
			return true;
		}

		return false;
	}

}
