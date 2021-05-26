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
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;

import java.util.*;

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

	public static <T> String getKey ( final T asset) {
		if (Contentlet.class.isInstance(asset)) {
			final Contentlet contentlet = Contentlet.class.cast(asset);
			return contentlet.getIdentifier();
		} else if (Folder.class.isInstance(asset)) {
			final Folder folder = Folder.class.cast(asset);
			return folder.getInode();
		} else if (Template.class.isInstance(asset)) {
			final Template template = Template.class.cast(asset);
			return template.getIdentifier();
		} else if (Container.class.isInstance(asset)) {
			final Container container = Container.class.cast(asset);
			return container.getIdentifier();
		} else if (Structure.class.isInstance(asset)) {
			final Structure structure = Structure.class.cast(asset);
			return structure.getInode();
		}  else if (Link.class.isInstance(asset)) {
			final Link link = Link.class.cast(asset);
			return link.getIdentifier();
		}  else if (Rule.class.isInstance(asset)) {
			final Rule rule = Rule.class.cast(asset);
			return rule.getId();
		}   else if (Relationship.class.isInstance(asset)) {
			final Relationship relationship = Relationship.class.cast(asset);
			return relationship.getInode();
		} else {
			throw new IllegalArgumentException();
		}
	}

	public <T> boolean excludeByModDate ( final T asset) {
		return Rule.class.isInstance(asset) ?
				excludeByModDate(Rule.class.cast(asset)) :
				excludeByModDate((Versionable) asset);
	}

	public boolean excludeByModDate ( final Rule rule) {
		return excludeByModDate(DependencyModDateUtil.getKey(rule), PusheableAsset.CONTENTLET,
				rule.getModDate());
	}

	public boolean excludeByModDate ( final Versionable asset) {
		return excludeByModDate(DependencyModDateUtil.getKey(asset), PusheableAsset.CONTENTLET,
				asset.getModDate());
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

		// we need to check if all environments have the last version of the asset in
		// order to skip adding it to the Set

		// if the asset hasn't been sent to at least one environment or an older version was sen't,
		// we need to add it to the cache

		if ( !bundle.isForcePush() && !isDownload && isPublish ) {
			for (Environment environment : envs) {
				final Optional<PushedAsset> pushedAssetOptional =
						getPushedAsset(assetId, environment);

				if (!pushedAssetOptional.isPresent()) {
					continue;
				}

				final PushedAsset pushedAsset = pushedAssetOptional.get();
				boolean modifiedOnCurrentEnv = isModifiedAfterPushAsset(assetId, assetModDate, pushedAsset,
						pusheableAsset);

				if (modifiedOnCurrentEnv) {
					return true;
				}
			}
		}

		return true;
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
			return true;
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
