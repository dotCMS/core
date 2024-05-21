package com.dotcms.publisher.util.dependencies;

import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfiguration;
import com.dotcms.util.AnnotationUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PushedAssetUtil {

    private PublisherConfig config;
    private Map<String,String> environmentsEndpointsAndPublisher = new HashMap<>();
    final List<Environment> environments;
    private static final String ENDPOINTS_SUFFIX = "_endpointIds";
    private static final String PUBLISHER_SUFFIX = "_publisher";

    public PushedAssetUtil(final PublisherConfig config) {
        this.config = config;

        environments = getEnvironments(config);
        final List<Environment> environmentsToRemove = new ArrayList<>();

        try {
            for (Environment environment : environments) {

                final EndpointAndPublishers endpointAndPublishers =
                        this.getEndpointAndPublishers(environment);

                if (endpointAndPublishers.endpoints != null &&
                        !endpointAndPublishers.endpoints.isEmpty()) {

                    final String publisher = endpointAndPublishers.publishers.stream()
                            .map(publisherClass -> publisherClass.getName())
                            .collect(Collectors.joining(","));

                    String endpointIds = endpointAndPublishers.endpoints.stream()
                            .map(endPoint -> endPoint.getId())
                            .collect(Collectors.joining(","));

                    if (!environment.getPushToAll() && endpointIds != null && endpointIds
                            .contains(",")) {
                        endpointIds = endpointIds.substring(0, endpointIds.indexOf(","));
                    }

                    //Add environment endpoints and publisher to map
                    environmentsEndpointsAndPublisher.put(environment.getId() + ENDPOINTS_SUFFIX,
                            endpointIds);
                    environmentsEndpointsAndPublisher.put(environment.getId() + PUBLISHER_SUFFIX,
                            publisher);
                } else {
                    //Remove env since env is Static, we don't to insert into publishing_pushed_assets table
                    environmentsToRemove.add(environment);
                }

            }

            environments.removeAll(environmentsToRemove);

        } catch (SecurityException | IllegalArgumentException | DotDataException e) {
            Logger.error(getClass(), "Can't get environments endpoints and publishers", e);
        }
    }

    private List<Environment> getEnvironments(PublisherConfig config) {
        try {
            return APILocator.getEnvironmentAPI().findEnvironmentsByBundleId(config.getId());
        } catch (DotDataException e) {
            Logger.error(getClass(), "Can't get environments", e);
            return Collections.emptyList();
        }
    }

    public EndpointAndPublishers getEndpointAndPublishers(final Environment environment ) throws DotDataException {
        final List<PublishingEndPoint> allEndpoints = APILocator.getPublisherEndPointAPI()
                .findSendingEndPointsByEnvironment(environment.getId());

        return new EndpointAndPublishers(allEndpoints);
    }

    public <T> void savePushedAssetForAllEnv(final T asset, final PusheableAsset pusheableAsset) {
        this.environments.forEach(environment ->
                savePushedAsset(DependencyManager.getBundleKey(asset), pusheableAsset, environment));
    }

    public <T> void removePushedAssetForAllEnv(final T asset, final PusheableAsset pusheableAsset) {
        final String assetId = DependencyManager.getBundleKey(asset);

        //For un-publish we always remove the asset from cache
        for ( Environment environment : environments ) {
            try {
                APILocator.getPushedAssetsAPI()
                        .deletePushedAssetsByEnvironment(assetId, environment.getId());
            } catch (DotDataException e) {
                Logger.error(this, e.getMessage(), e);
            }
        }
    }

    private void savePushedAsset(
            final String assetId,
            final PusheableAsset pusheableAsset,
            final Environment env) {

        try {
            //Insert the new pushed asset indicating to which endpoints will be sent and with what publisher class
            final PushedAsset
                    assetToPush =
                    new PushedAsset(this.config.getId(),
                            assetId, pusheableAsset.toString(), new Date(), env.getId(),
                            environmentsEndpointsAndPublisher.get(env.getId() + ENDPOINTS_SUFFIX),
                            environmentsEndpointsAndPublisher.get(env.getId() + PUBLISHER_SUFFIX));

            APILocator.getPushedAssetsAPI().savePushedAsset(assetToPush);
        } catch (DotDataException e) {
            Logger.error(getClass(), "Could not save PushedAsset. "
                    + "AssetId: " + assetId + ". AssetType: " + pusheableAsset + ". Env Id: " + env.getId(), e);
        }
    }

    private class EndpointAndPublishers {
        List<PublishingEndPoint> endpoints = new ArrayList<>();
        List<Class> publishers = new ArrayList<>();

        EndpointAndPublishers(final List<PublishingEndPoint> allEndpoints ){
            for(PublishingEndPoint endPoint : allEndpoints) {
                this.addEndPoint(endPoint);
            }
        }

        public void addEndPoint(final PublishingEndPoint endPoint) {
            final Class endPointPublisher = endPoint.getPublisher();
            PublisherConfiguration result = AnnotationUtils
                    .getBeanAnnotation(endPointPublisher, PublisherConfiguration.class);
            boolean isStaticEndpoint = result != null && result.isStatic();

            if (endPoint.isEnabled() && config.isStatic() == isStaticEndpoint) {
                endpoints.add(endPoint);

                if(!publishers.contains(endPointPublisher)){
                    publishers.add(endPointPublisher);
                }
            }
        }
    }
}
