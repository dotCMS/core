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
import com.dotmarketing.business.Versionable;
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
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * Provide util method
 */
public class DependencyModDateUtil extends HashSet<String> {

	/**
	 *
	 */
	private static final long serialVersionUID = 3048299770146564147L;

	private PushedAssetsCache cache;
	private List<Environment> envs = new ArrayList<>();
	private Bundle bundle;
	private boolean isDownload;
	private boolean isPublish;

	public DependencyModDateUtil(final PushPublisherConfig config) {
		this(config, config.isDownloading());
	}

	public DependencyModDateUtil(final PublisherConfig config) {
		this(config, false);
	}
	public DependencyModDateUtil(final PublisherConfig config, final boolean isDownload) {
		super();

		final String bundleId = config.getId();
		boolean isPublish = config.getOperation().equals(Operation.PUBLISH);

		cache = CacheLocator.getPushedAssetsCache();
		this.isDownload = isDownload;
		this.isPublish = isPublish;

		try {
			final List<Environment> allEnvs =
					APILocator.getEnvironmentAPI().findEnvironmentsByBundleId(bundleId);

			try {
				for (Environment env : allEnvs) {
					List<PublishingEndPoint> allEndpoints =
							APILocator.getPublisherEndPointAPI()
									.findSendingEndPointsByEnvironment(env.getId());

					if (allEndpoints != null && !allEndpoints.isEmpty()) {
						this.envs.add(env);
					}
				}
			} catch (SecurityException | IllegalArgumentException | DotDataException e) {
				Logger.error(getClass(), "Can't get environments endpoints and publishers", e);
			}
		} catch (DotDataException e) {
			Logger.error(getClass(), "Can't get environments", e);
		}

		try {
			bundle = APILocator.getBundleAPI().getBundleById(bundleId);
		} catch (DotDataException e) {
			Logger.error(getClass(), "Can't get bundle. Bundle Id: " + bundleId , e);
		}
	}

	/**
	 * Return true if the <code>asset</code> should be exclude from a bundle create today
	 * because it was not modified after the last Push.
	 *
	 * @param asset asset to be add into a bundle
	 * @param pusheableAsset asset's {@link PusheableAsset}
	 * @param <T>
	 * @return
	 */
	public <T> boolean excludeByModDate ( final T asset, final PusheableAsset pusheableAsset) {
		if (Rule.class.isInstance(asset)) {
			return excludeByModDate(Rule.class.cast(asset));
		} else 	if (WorkflowScheme.class.isInstance(asset)) {
			return excludeByModDate(WorkflowScheme.class.cast(asset));
		} else 	if (Relationship.class.isInstance(asset)) {
			return excludeByModDate(Relationship.class.cast(asset));
		} else if (Versionable.class.isInstance(asset)) {
			return excludeByModDate((Versionable) asset, pusheableAsset);
		} else {
			throw new IllegalArgumentException(String.format("Type not expected: %s'",
					asset.getClass()));
		}
	}

	/**
	 * Return true if a {@link Relationship} should be exclude from a bundle create today
	 * because it was not modified after the last Push.
	 *
	 * @param relationship
	 * @return
	 */
	public boolean excludeByModDate ( final Relationship relationship) {
		return excludeByModDate(DependencyManager. getKey(relationship), PusheableAsset.RELATIONSHIP,
				relationship.getModDate());
	}

	/**
	 * Return true if a {@link Rule} should be exclude from a bundle create today
	 * because it was not modified after the last Push.
	 *
	 * @param rule
	 * @return
	 */
	public boolean excludeByModDate ( final Rule rule) {
		return excludeByModDate(DependencyManager.getKey(rule), PusheableAsset.RULE,
				rule.getModDate());
	}

	/**
	 * Return true if a {@link WorkflowScheme} should be exclude from a bundle create today
	 * because it was not modified after the last Push.
	 *
	 * @param workflowScheme
	 * @return
	 */
	public boolean excludeByModDate ( final WorkflowScheme workflowScheme) {
		return excludeByModDate(DependencyManager.getKey(workflowScheme), PusheableAsset.WORKFLOW,
				workflowScheme.getModDate());
	}

	/**
	 * Return true if a {@link Versionable} should be exclude from a bundle create today
	 * because it was not modified after the last Push.
	 *
	 * @param asset
	 * @param pusheableAsset
	 * @return
	 */
	public boolean excludeByModDate ( final Versionable asset, final PusheableAsset pusheableAsset) {
		return excludeByModDate(DependencyManager.getKey(asset), pusheableAsset,
				asset.getModDate());
	}

	private synchronized boolean excludeByModDate ( final String assetId, final PusheableAsset pusheableAsset,
			final Date assetModDate) {

		// we need to check if all environments have the last version of the asset in
		// order to skip adding it to the Set

		// if the asset hasn't been sent to at least one environment or an older version was sen't,
		// we need to add it to the cache

		if ( !bundle.isForcePush() && !isDownload && isPublish ) {


			for (Environment environment : envs) {
				final Optional<PushedAsset> pushedAssetOptional =
						getPushedAsset(assetId, environment);

				if (!pushedAssetOptional.isPresent()) {
					return false;
				}

				final PushedAsset pushedAsset = pushedAssetOptional.get();
				boolean modifiedOnCurrentEnv = isModifiedAfterPushAsset(assetId, assetModDate, pushedAsset,
						pusheableAsset);

				if (modifiedOnCurrentEnv) {
					return false;
				}
			}

			return true;
		}

		return false;
	}

	private boolean isModifiedAfterPushAsset(
			final String assetId, final Date assetModDate,
			final PushedAsset pushedAsset, final PusheableAsset pusheableAsset) {

		final boolean isModifiedAfterPushAsset = assetModDate != null && pushedAsset.getPushDate().before(assetModDate);

		try {

			if (!isModifiedAfterPushAsset) {
				if (PusheableAsset.CONTENTLET == pusheableAsset) {
					return chekModDateInAllLanguages(assetId, pushedAsset);
				}

				if (PusheableAsset.TEMPLATE == pusheableAsset ||
						PusheableAsset.LINK == pusheableAsset ||
						PusheableAsset.CONTAINER == pusheableAsset) {

					return checkModDateInVersionInfoTS(assetId, pushedAsset);
				}
			}
		} catch (Exception e) {
			Logger.warn(getClass(),
					"Error checking versionInfo for assetType:" + pusheableAsset
							+ " assetId:" + assetId +
							" process continues without checking versionInfo.ts", e);
		}

		return isModifiedAfterPushAsset;

	}

	private boolean checkModDateInVersionInfoTS(String assetId, PushedAsset pushedAsset) throws DotDataException {
		// check for versionInfo TS
		VersionInfo info = APILocator.getVersionableAPI()
				.getVersionInfo(assetId);
		if (info != null && InodeUtils.isSet(info.getIdentifier())) {
			return pushedAsset.getPushDate().before(info.getVersionTs());
		}
		return false;
	}

	private boolean chekModDateInAllLanguages(String assetId, PushedAsset pushedAsset) {
		// check for versionInfo TS on content
		for (Language lang : APILocator.getLanguageAPI().getLanguages()) {
			Optional<ContentletVersionInfo> info = APILocator
					.getVersionableAPI()
					.getContentletVersionInfo(assetId, lang.getId());

			if (info.isPresent() &&
					pushedAsset.getPushDate().before(info.get().getVersionTs())) {
				return true;
			}
		}

		return false;
	}

	private Optional<PushedAsset> getPushedAsset(String assetId, Environment environment) {
		try {
			final String  endPoints = APILocator.getPublisherEndPointAPI()
					.findSendingEndPointsByEnvironment(environment.getId())
					.stream()
					.map(endPoint -> endPoint.getId())
					.collect(Collectors.joining(","));

			return Optional.ofNullable(
					APILocator.getPushedAssetsAPI()
							.getLastPushForAsset(assetId, environment.getId(), endPoints)
			);
		} catch (DotDataException e) {
			return Optional.empty();
		}
	}
}
