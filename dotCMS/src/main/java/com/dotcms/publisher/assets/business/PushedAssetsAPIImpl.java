package com.dotcms.publisher.assets.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.rest.api.v1.content.PushedAssetHistory;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;

import java.util.List;

/**
 * This class implements the {@link PushedAssetsAPI}.
 *
 * @author Daniel Silva
 * @since Jul 16th, 2013
 */
public class PushedAssetsAPIImpl implements PushedAssetsAPI {

	private final PushedAssetsFactory pushedAssetsFactory;

	public PushedAssetsAPIImpl() {
		pushedAssetsFactory = FactoryLocator.getPushedAssetsFactory();
	}

	@WrapInTransaction
	@Override
	public void savePushedAsset(PushedAsset asset)
			throws DotDataException {
		pushedAssetsFactory.savePushedAsset(asset);

	}

	@WrapInTransaction
	@Override
	public void deletePushedAssets(String bundleId, String environmentId)
			throws DotDataException {

		List<PushedAsset> assets = pushedAssetsFactory.getPushedAssets(bundleId, environmentId);

		pushedAssetsFactory.deletePushedAssets(bundleId, environmentId);

		// clear the deleted entries from the cache
		if(assets!=null && assets.size()>0) {
			for (PushedAsset asset : assets) {
				CacheLocator.getPushedAssetsCache().removePushedAssetById(asset.getAssetId(), asset.getEnvironmentId());
			}
		}

	}

	@WrapInTransaction
	@Override
	public void deletePushedAssets(String assetId)
			throws DotDataException {

		List<PushedAsset> assets = pushedAssetsFactory.getPushedAssets(assetId);

		pushedAssetsFactory.deletePushedAssets(assetId);

		// clear the deleted entries from the cache
		if(assets!=null && assets.size()>0) {
			for (PushedAsset asset : assets) {
				CacheLocator.getPushedAssetsCache().removePushedAssetById(asset.getAssetId(), asset.getEnvironmentId());
			}
		}

	}

	@WrapInTransaction
	@Override
	public void deletePushedAssetsByEnvironment(final String assetId, final String environmentId)  throws DotDataException {

		final List<PushedAsset> assets = pushedAssetsFactory.getPushedAssets(assetId, environmentId);

		pushedAssetsFactory.deletePushedAssetsByEnvironment(assetId, environmentId);

		// clear the deleted entries from the cache
		if(UtilMethods.isSet(assets)) {
			for (final PushedAsset asset : assets) {
				CacheLocator.getPushedAssetsCache().removePushedAssetById(asset.getAssetId(), asset.getEnvironmentId());
			}
		}
	}

	@WrapInTransaction
	@Override
	public void deletePushedAssetsByEnvironment(String environmentId)
			throws DotDataException {

		List<PushedAsset> assets = pushedAssetsFactory.getPushedAssetsByEnvironment(environmentId);

		pushedAssetsFactory.deletePushedAssetsByEnvironment(environmentId);

		// clear the deleted entries from the cache
		if(assets!=null && assets.size()>0) {
			for (PushedAsset asset : assets) {
				CacheLocator.getPushedAssetsCache().removePushedAssetById(asset.getAssetId(), asset.getEnvironmentId());
			}
		}

	}

	@WrapInTransaction
	@Override
	public void deleteAllPushedAssets() throws DotDataException {
		pushedAssetsFactory.deleteAllPushedAssets();
		CacheLocator.getPushedAssetsCache().clearCache();

	}

	@CloseDBIfOpened
	@Override
	public List<PushedAsset> getPushedAssets(String assetId)
			throws DotDataException {
		return pushedAssetsFactory.getPushedAssets(assetId);
	}
	
	
	@CloseDBIfOpened
	@Override
	public PushedAsset getLastPushForAsset(final String assetId, final String environmentId, final String endpointIds)  throws DotDataException{

		if(!UtilMethods.isSet(environmentId) || !UtilMethods.isSet(assetId)) {
			return null;
		}

		return pushedAssetsFactory.getLastPushForAsset(assetId,environmentId,endpointIds);
		
	}

	@WrapInTransaction
	@Override
	public void deletePushedAssetsByBundleId(final String bundleId) throws DotDataException {
		pushedAssetsFactory.deletePushedAssetsByBundle(bundleId);

	}

	@CloseDBIfOpened
	@Override
	public List<PushedAsset> getPushedAssetsByBundleIdAndEnvironmentId(final String bundleId,
			final String environmentId) throws DotDataException {
		return pushedAssetsFactory.getPushedAssets(bundleId,environmentId);
	}

    @CloseDBIfOpened
    @Override
    public List<PushedAssetHistory> getPushedAssets(final String assetId, final int offset, final int limit) throws DotDataException {
        return pushedAssetsFactory.getPushedAssets(assetId, offset, limit);
    }

    @CloseDBIfOpened
    @Override
    public long getTotalPushedAssets(final String assetId) throws DotDataException {
        return pushedAssetsFactory.getTotalPushedAssets(assetId);
    }

}
