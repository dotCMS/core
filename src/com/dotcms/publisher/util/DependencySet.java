package com.dotcms.publisher.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.publisher.assets.business.PushedAssetsCache;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
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

	public DependencySet(String bundleId, String assetType, boolean isDownload, boolean isPublish) {
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
			bundle = APILocator.getBundleAPI().getBundleById(bundleId);
		} catch (DotDataException e) {
			Logger.error(getClass(), "Can't get bundle. Bundle Id: " + bundleId , e);
		}
	}

    public boolean add ( String assetId, Date assetModDate ) {
        return addOrClean( assetId, assetModDate, false );
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
        return addOrClean( assetId, assetModDate, true );
    }

    private boolean addOrClean ( String assetId, Date assetModDate, Boolean cleanForUnpublish ) {

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

		boolean modified = false;

		// we need to check if all environments have the last version of the asset in
		// order to skip adding it to the Set

		// if the asset hasn't been sent to at least one environment or an older version was sen't,
		// we need to add it to the cache

		if(!bundle.isForcePush() && !isDownload && isPublish ) {
			for (Environment env : envs) {
				PushedAsset asset = cache.getPushedAsset(assetId, env.getId());

				if(modified |= (asset==null || asset.getPushDate().before(assetModDate))) {
					try {
						asset = new PushedAsset(bundleId, assetId, assetType, new Date(), env.getId());
						APILocator.getPushedAssetsAPI().savePushedAsset(asset);
					} catch (DotDataException e) {
						Logger.error(getClass(), "Could not save PushedAsset. "
								+ "AssetId: " + assetId + ". AssetType: " + assetType + ". Env Id: " + env.getId(), e);
					}
					cache.add(asset);
				}

			}

		}

		if(bundle.isForcePush() || isDownload || !isPublish || modified) {
			super.add(assetId);
			return true;
		}

		return false;
	}

}
