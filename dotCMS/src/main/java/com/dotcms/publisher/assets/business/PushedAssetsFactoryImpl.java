package com.dotcms.publisher.assets.business;

import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.rest.api.v1.content.PushedAssetHistory;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * This class extends/implements the {@link PushedAssetsFactory}.
 *
 * @author Daniel Silva
 * @since Jul 16th, 2013
 */
public class PushedAssetsFactoryImpl extends PushedAssetsFactory {

	private final PushedAssetsCache cache = CacheLocator.getPushedAssetsCache();

    @Override
	public void savePushedAsset(PushedAsset asset) throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(INSERT_ASSETS);
		db.addParam(asset.getBundleId());
		db.addParam(asset.getAssetId());
		db.addParam(asset.getAssetType());
		db.addParam(asset.getPushDate());
		db.addParam(asset.getEnvironmentId());
		db.addParam(asset.getEndpointIds());
		db.addParam(asset.getPublisher());
		db.loadResult();
		cache.removePushedAssetById(asset.getAssetId(), asset.getEnvironmentId());
	}

	@Override
	public void deletePushedAssets(String bundleId, String environmentId)
			throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(DELETE_ASSETS_BY_BUNDLE_ENV);
		db.addParam(bundleId);
		db.addParam(environmentId);
		db.loadResult();
		cache.clearCache();

	}

	@Override
	public void deletePushedAssetsByBundle(final String bundleId)  throws DotDataException {

		final DotConnect db = new DotConnect();
		db.setSQL(DELETE_ASSETS_BY_BUNDLE);
		db.addParam(bundleId);
		db.loadResult();
		cache.clearCache();
	}

	@Override
	public void deletePushedAssets(String assetId)
			throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(DELETE_ASSETS_BY_ASSET_ID);
		db.addParam(assetId);
		db.loadResult();
		cache.clearCache();
	}

	@Override
	public void deletePushedAssetsByEnvironment(final String assetId, final String environmentId)  throws DotDataException {

		new DotConnect().setSQL(DELETE_ASSETS_BY_ASSET_ID_AND_ENV)
			.addParam(assetId).addParam(environmentId).loadResult();
		cache.removePushedAssetById(assetId, environmentId);
	}

	@Override
	public void deletePushedAssetsByEnvironment(String environmentId)
			throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(DELETE_ASSETS_BY_ENVIRONMENT_ID);
		db.addParam(environmentId);
		db.loadResult();
		cache.clearCache();
	}

	@Override
	public List<PushedAsset> getPushedAssets(String bundleId, String environmentId)
			throws DotDataException {
		List<PushedAsset> assets = new ArrayList<>();

		if(!UtilMethods.isSet(bundleId) || !UtilMethods.isSet(environmentId)) {
			return assets;
		}

		DotConnect dc = new DotConnect();
		dc.setSQL(SELECT_ASSETS_BY_BUNDLE_ENV);
		dc.addParam(bundleId);
		dc.addParam(environmentId);

		List<Map<String, Object>> res = dc.loadObjectResults();

		for(Map<String, Object> row : res){
			PushedAsset asset = PublisherUtil.getPushedAssetByMap(row);
			assets.add(asset);
		}

		return assets;

	}

	@Override
	public void deleteAllPushedAssets() throws DotDataException {
		final DotConnect db = new DotConnect();
		db.setSQL(DELETE_ALL_ASSETS);
		db.loadResult();
		cache.clearCache();
	}

	@Override
	public List<PushedAsset> getPushedAssets(String assetId)
			throws DotDataException {
        return getPushedAssetsInner(assetId, 0, -1, PublisherUtil::getPushedAssetByMapList);
	}

    @Override
    public List<PushedAssetHistory> getPushedAssets(final String assetId, final int offset, final int limit) throws DotDataException {
        return getPushedAssetsInner(assetId, offset, limit, rows -> new PushedAssetHistoryTransformer(rows).asList());
    }

    /**
     * Retrieves a paginated list of pushed asset history entries for a given asset.
     *
     * @param assetId   the identifier of the asset whose push history is requested
     * @param offset    zero-based index of the first record to return
     * @param limit     maximum number of records to return
     * @param transform The {@link Function} used o transform the information from the database into
     *                  the expected object list.
     * @param <T> {@link PushedAsset}, {@link PushedAssetHistory}
     *
     * @return A list of objects containing pushed asset history.
     *
     * @throws DotDataException An error occurred while accessing the data source.
     */
    @SuppressWarnings("unchecked")
    private <T> T getPushedAssetsInner(String assetId, int offset, int limit,
                                       Function<List<Map<String, Object>>, T> transform) throws DotDataException {
        if (!UtilMethods.isSet(assetId)) {
            return (T) List.of();
        }

        final String query = limit >= 1 ? String.format(SELECT_ASSETS_BY_ASSET_ID, " LIMIT " + limit) :
                String.format(SELECT_ASSETS_BY_ASSET_ID, StringPool.BLANK);

        DotConnect dc = new DotConnect();
        dc.setSQL(query);
        dc.addParam(assetId);

        if (offset > 0) {
            dc.addParam(offset);
        } else {
            dc.addParam(0);
        }

        List<Map<String, Object>> res = dc.loadObjectResults();
        return transform.apply(res);
    }

    @Override
	public List<PushedAsset> getPushedAssetsByEnvironment(String environmentId)
			throws DotDataException {
		List<PushedAsset> assets = new ArrayList<>();

		if(!UtilMethods.isSet(environmentId)) {
			return assets;
		}

		DotConnect dc = new DotConnect();
		dc.setSQL(SELECT_ASSETS_BY_ENV_ID);
		dc.addParam(environmentId);

		List<Map<String, Object>> res = dc.loadObjectResults();

		for(Map<String, Object> row : res){
			PushedAsset asset = PublisherUtil.getPushedAssetByMap(row);
			assets.add(asset);
		}

		return assets;
	}
	
	@Override
	public PushedAsset getLastPushForAsset(final String assetId, final String environmentId, final String endpointIds)  throws DotDataException {
		
		PushedAsset asset = cache.getPushedAsset(assetId, environmentId);

		if(null == asset ){
			final DotConnect dc = new DotConnect();
			if(DbConnectionFactory.isOracle()){
				dc.setSQL(SELECT_ASSET_LAST_PUSHED_ORACLE);
			} else {
				dc.setSQL(SELECT_ASSET_LAST_PUSHED);
			}
			dc.addParam(assetId);
			dc.addParam(environmentId);
			dc.addParam(endpointIds);
			dc.setMaxRows(1);

			final List<Map<String, Object>> results = dc.loadObjectResults();
	
			for(final Map<String, Object> row : results) {

				asset = PublisherUtil.getPushedAssetByMap(row);
				cache.add(asset);
			}
		}
		
		return asset;
	}

    @Override
    public long getTotalPushedAssets(String assetId) throws DotDataException {
        final List<Map<String, Object>> result = new DotConnect()
                .setSQL(SELECT__TOTAL_ASSETS_BY_ASSET_ID)
                .addParam(assetId)
                .loadObjectResults();

        return Long.parseLong(result.get(0).get("total").toString());

    }

}
