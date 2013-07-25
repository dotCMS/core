package com.dotcms.publisher.assets.business;

import java.util.List;

import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;

public class PushedAssetsAPIImpl implements PushedAssetsAPI {

	private PushedAssetsFactory pushedAssetsFactory;

	public PushedAssetsAPIImpl() {
		pushedAssetsFactory = FactoryLocator.getPushedAssetsFactory();
	}

	@Override
	public void savePushedAsset(PushedAsset asset)
			throws DotDataException {
		pushedAssetsFactory.savePushedAsset(asset);

	}

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

	@Override
	public void deleteAllPushedAssets() throws DotDataException {
		pushedAssetsFactory.deleteAllPushedAssets();
		CacheLocator.getPushedAssetsCache().clearCache();

	}

	@Override
	public List<PushedAsset> getPushedAssets(String assetId)
			throws DotDataException {
		return pushedAssetsFactory.getPushedAssets(assetId);
	}

}
