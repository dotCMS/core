package com.dotcms.publisher.util.dependencies;

import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.publisher.assets.business.PushedAssetsCache;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.PublisherConfiguration;
import com.dotcms.util.AnnotationUtils;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class DependencyModDateUtil extends HashSet<String> {

	/**
	 *
	 */
	private static final long serialVersionUID = 3048299770146564147L;
	private static final String ENDPOINTS_SUFFIX = "_endpointIds";
	private static final String PUBLISHER_SUFFIX = "_publisher";

	private PushedAssetsCache cache;
	private List<Environment> envs = new ArrayList<>();
	private String bundleId;
	private Bundle bundle;
	private boolean isDownload;
	private boolean isPublish;
	private Map<String,String> environmentsEndpointsAndPublisher = new HashMap<>();

	public DependencyModDateUtil(final PushPublisherConfig config) {
		this(config, config.isDownloading());
	}

	public DependencyModDateUtil(final PublisherConfig config) {
		this(config, false);
	}
	public DependencyModDateUtil(final PublisherConfig config, final boolean isDownload) {
		super();

		final String bundleId = config.getId();
		final boolean isStatic = config.isStatic();
		boolean isPublish = config.getOperation().equals(Operation.PUBLISH);

		cache = CacheLocator.getPushedAssetsCache();
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
				String endpointIds;
				String publisher;

				List<PublishingEndPoint> allEndpoints = APILocator.getPublisherEndPointAPI().findSendingEndPointsByEnvironment(env.getId());
				List<String> endpoints = new ArrayList<>();
				List<String> publishers = new ArrayList<>();
				//Filter Endpoints list
				for(PublishingEndPoint ep : allEndpoints) {
					Class endPointPublisher = ep.getPublisher();
					PublisherConfiguration result = AnnotationUtils.getBeanAnnotation(endPointPublisher, PublisherConfiguration.class);
					boolean isStaticEndpoint = result!=null && result.isStatic();

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

				if(!env.getPushToAll() && endpointIds != null && endpointIds.contains(",")){
					endpointIds = endpointIds.substring(0, endpointIds.indexOf(","));
				}

				//Add environment endpoints and publisher to map
				environmentsEndpointsAndPublisher.put(env.getId()+ENDPOINTS_SUFFIX, endpointIds);
				environmentsEndpointsAndPublisher.put(env.getId()+PUBLISHER_SUFFIX, publisher);

			}
			//Search and remove all the environments that are not going to be used by this dependencySet
			List<Environment> removeEnvironmentsList = new ArrayList<>();
			for (Environment env : envs) {
				if(StringUtils.isEmpty(environmentsEndpointsAndPublisher.get(env.getId()+ENDPOINTS_SUFFIX))){
					removeEnvironmentsList.add(env);
				}
			}
			if(!removeEnvironmentsList.isEmpty()){
				envs.removeAll(removeEnvironmentsList);
			}
		} catch (SecurityException | IllegalArgumentException | DotDataException e) {
			Logger.error(getClass(), "Can't get environments endpoints and publishers", e);
		}

		try {
			bundle = APILocator.getBundleAPI().getBundleById(bundleId);
		} catch (DotDataException e) {
			Logger.error(getClass(), "Can't get bundle. Bundle Id: " + bundleId , e);
		}
	}


	public <T> boolean excludeByModDate ( final T asset) {
		if (Contentlet.class.isInstance(asset)) {
			final Contentlet contentlet = Contentlet.class.cast(asset);
			return excludeByModDate(contentlet.getIdentifier(), PusheableAsset.CONTENTLET,
					contentlet.getModDate());
		} else if (Folder.class.isInstance(asset)) {
			final Folder folder = Folder.class.cast(asset);
			return excludeByModDate(folder.getInode(), PusheableAsset.FOLDER, folder.getModDate());
		} else if (Template.class.isInstance(asset)) {
			final Template template = Template.class.cast(asset);
			return excludeByModDate(template.getIdentifier(), PusheableAsset.TEMPLATE,
					template.getModDate());
		} else if (Container.class.isInstance(asset)) {
			final Container container = Container.class.cast(asset);
			return excludeByModDate(container.getIdentifier(), PusheableAsset.CONTAINER,
					container.getModDate());
		} else if (Structure.class.isInstance(asset)) {
			final Structure structure = Structure.class.cast(asset);
			return excludeByModDate(structure.getInode(), PusheableAsset.CONTENT_TYPE,
					structure.getModDate());
		}  else if (Link.class.isInstance(asset)) {
			final Link link = Link.class.cast(asset);
			return excludeByModDate(link.getIdentifier(), PusheableAsset.LINK, link.getModDate());
		}  else if (Rule.class.isInstance(asset)) {
			final Rule rule = Rule.class.cast(asset);
			return excludeByModDate(rule.getId(), PusheableAsset.RULE, rule.getModDate());
		}   else if (Relationship.class.isInstance(asset)) {
			final Relationship relationship = Relationship.class.cast(asset);
			return excludeByModDate(relationship.getInode(), PusheableAsset.RELATIONSHIP,
					relationship.getModDate());
		} else {
			throw new IllegalArgumentException();
		}
	}

	private synchronized boolean excludeByModDate ( final String assetId, final PusheableAsset pusheableAsset,
			final Date assetModDate) {

		if ( !isPublish ) {

			//For un-publish we always remove the asset from cache
			for ( Environment env : envs ) {
				cache.removePushedAssetById( assetId, env.getId() );
				try {
					APILocator.getPushedAssetsAPI().deletePushedAssetsByEnvironment(assetId, env.getId());
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage(), e);
				}
			}

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
				final PushedAsset asset;
				try {
					//Search the last pushed entry register of the pushed asset by asset Id, environment Id and endpoints Ids
					asset = APILocator.getPushedAssetsAPI()
							.getLastPushForAsset(assetId, env.getId(),
									environmentsEndpointsAndPublisher
											.get(env.getId() + ENDPOINTS_SUFFIX));

				} catch (DotDataException e1) {
					// Asset does not exist in db or cache, return true;
					return true;
				}

				modifiedOnCurrentEnv = (asset == null || (assetModDate != null && asset
						.getPushDate().before(assetModDate)));

				try {
					if (!modifiedOnCurrentEnv && PusheableAsset.CONTENTLET == pusheableAsset) {
						// check for versionInfo TS on content
						for (Language lang : APILocator.getLanguageAPI().getLanguages()) {
							Optional<ContentletVersionInfo> info = APILocator
									.getVersionableAPI()
									.getContentletVersionInfo(assetId, lang.getId());

							if (info.isPresent()) {
								modifiedOnCurrentEnv = modifiedOnCurrentEnv
										|| null == info.get().getVersionTs()
										|| asset.getPushDate()
										.before(info.get().getVersionTs());
							}
						}
					}
					if (!modifiedOnCurrentEnv && (PusheableAsset.TEMPLATE == pusheableAsset ||
							PusheableAsset.LINK == pusheableAsset ||
							PusheableAsset.CONTAINER == pusheableAsset)) {
						// check for versionInfo TS
						VersionInfo info = APILocator.getVersionableAPI()
								.getVersionInfo(assetId);
						if (info != null && InodeUtils.isSet(info.getIdentifier())) {
							modifiedOnCurrentEnv = asset.getPushDate()
									.before(info.getVersionTs());
						}
					}
				} catch (Exception e) {
					Logger.warn(getClass(),
							"Error checking versionInfo for assetType:" + pusheableAsset
									+ " assetId:" + assetId +
									" process continues without checking versionInfo.ts", e);
				}

                if (modifiedOnCurrentEnv) {
                    savePushedAsset(assetId, pusheableAsset, env);
                    //If the asset was modified at least in one environment, set this to true
                    modifiedOnAtLeastOneEnv = true;
                }
			}
		}

		if ( isForcePush || isDownload || !isPublish || modifiedOnAtLeastOneEnv ) {
			super.add( assetId );

			if(isForcePush) {
				envs.forEach((environment)->savePushedAsset(assetId, pusheableAsset, environment));
			}

			return true;
		}

		return false;
	}

    private void savePushedAsset(final String assetId, final PusheableAsset pusheableAsset, final Environment env) {
        try {
            //Insert the new pushed asset indicating to which endpoints will be sent and with what publisher class
            final PushedAsset
                assetToPush =
                new PushedAsset(bundleId, assetId, pusheableAsset.toString(), new Date(), env.getId(),
                    environmentsEndpointsAndPublisher.get(env.getId() + ENDPOINTS_SUFFIX),
                    environmentsEndpointsAndPublisher.get(env.getId() + PUBLISHER_SUFFIX));

            APILocator.getPushedAssetsAPI().savePushedAsset(assetToPush);
        } catch (DotDataException e) {
            Logger.error(getClass(), "Could not save PushedAsset. "
                + "AssetId: " + assetId + ". AssetType: " + pusheableAsset + ". Env Id: " + env.getId(), e);
        }
    }

}
