package com.dotcms.publisher.assets.business;

import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.rest.api.v1.content.PushedAssetHistory;
import com.dotmarketing.exception.DotDataException;

import java.util.List;

/**
 * Provides access to data related to the history of pushed assets in dotCMS. Every time a
 * "pusheable" object is sent to another server/environment, this table is updated with the main
 * information that allows users/administrators to track down when it was pushed. It also allows
 * the Push Publishing mechanism to determine when an asset was last pushed.
 *
 * @author Daniel Silva
 * @since Jul 16th, 2013
 */
public abstract class PushedAssetsFactory {

	protected static String INSERT_ASSETS = "INSERT INTO publishing_pushed_assets VALUES (?,?,?,?,?,?,?)";
	protected static String SELECT_ASSETS_BY_BUNDLE_ENV= "SELECT * FROM publishing_pushed_assets WHERE bundle_id = ? and environment_id = ?";
	protected static String SELECT_ASSETS_BY_ASSET_ID= "SELECT ppa.*, " +
            "       env.name as environment_name, " +
            "       bundle.owner as owner " +
            "FROM publishing_pushed_assets as ppa " +
            "    JOIN publishing_environment as env ON ppa.environment_id = env.id " +
            "    JOIN publishing_bundle as bundle ON ppa.bundle_id = bundle.id " +
            "WHERE asset_id = ? " +
            "ORDER BY push_date " +
             "OFFSET ? %s";

    protected static String SELECT__TOTAL_ASSETS_BY_ASSET_ID = "SELECT count(*) as total " +
            "FROM publishing_pushed_assets  " +
            "WHERE asset_id = ?";

	protected static String SELECT_ASSETS_BY_ENV_ID= "SELECT * FROM publishing_pushed_assets WHERE environment_id = ?";
	protected static String DELETE_ASSETS_BY_BUNDLE_ENV= "DELETE FROM publishing_pushed_assets WHERE bundle_id = ? and environment_id = ?";
	protected static String DELETE_ASSETS_BY_BUNDLE    = "DELETE FROM publishing_pushed_assets WHERE bundle_id = ?";
	protected static String DELETE_ASSETS_BY_ASSET_ID= "DELETE FROM publishing_pushed_assets WHERE asset_id = ?";
	protected static String DELETE_ASSETS_BY_ASSET_ID_AND_ENV = "DELETE FROM publishing_pushed_assets WHERE asset_id = ?  and environment_id = ?";
	protected static String DELETE_ASSETS_BY_ENVIRONMENT_ID= "DELETE FROM publishing_pushed_assets WHERE environment_id = ?";
	protected static String DELETE_ALL_ASSETS= "TRUNCATE TABLE publishing_pushed_assets";
	protected static String SELECT_ASSET_LAST_PUSHED = "SELECT * FROM publishing_pushed_assets WHERE asset_id = ? AND environment_id = ? AND endpoint_ids = ? ORDER BY push_date DESC";
	protected static String SELECT_ASSET_LAST_PUSHED_ORACLE = "SELECT * FROM publishing_pushed_assets WHERE asset_id = ? AND environment_id = ? AND to_char(endpoint_ids) = ? ORDER BY push_date DESC";

	public abstract void savePushedAsset(PushedAsset asset) throws DotDataException;

	/**
	 * Deletes all pushed assets for an environment and bundle
	 * from: publishing_pushed_assets
	 * @param bundleId      {@link String} bundle id
	 * @param environmentId {@link String} environment id
	 * @throws DotDataException
	 */
	public abstract void deletePushedAssets(String bundleId, String environmentId)  throws DotDataException;

	/**
	 * Deletes all pushed assets for a bundle in any environments
	 * from: publishing_pushed_assets
	 * @param bundleId {@link String} bundle id
	 * @throws DotDataException
	 */
	public abstract void deletePushedAssetsByBundle(String bundleId)  throws DotDataException;

	public abstract void deletePushedAssets(String assetId)  throws DotDataException;

	public abstract void deletePushedAssetsByEnvironment(String environmentId)  throws DotDataException;

	/**
	 * Deletes an asset for an environment
	 * @param assetId {@link String}
	 * @param environmentId {@link String}
	 * @throws DotDataException
	 */
	public abstract void deletePushedAssetsByEnvironment(String assetId, String environmentId)  throws DotDataException;

	public abstract List<PushedAsset> getPushedAssets(String bundleId, String environmentId)  throws DotDataException;

	public abstract void deleteAllPushedAssets() throws DotDataException;

	public abstract List<PushedAsset> getPushedAssets(String assetId) throws DotDataException;

    /**
     * Retrieves a paginated list of pushed asset history entries for a given asset.
     *
     * @param assetId the identifier of the asset whose push history is requested
     * @param offset  zero-based index of the first record to return
     * @param limit   maximum number of records to return
     * @return a list of {@link PushedAssetHistory} entries ordered by push date
     * @throws DotDataException if an error occurs while accessing the data source
     */
    public abstract List<PushedAssetHistory> getPushedAssets(final String assetId, final int offset, final int limit) throws DotDataException;

	public abstract List<PushedAsset> getPushedAssetsByEnvironment(String assetId) throws DotDataException;

	public abstract PushedAsset getLastPushForAsset(String assetId, String environmentId, String endpointIds)  throws DotDataException;

    /**
     * Returns the total number of push history entries for the specified asset.
     *
     * @param assetId the identifier of the asset
     * @return total number of push history records for the asset
     * @throws DotDataException if an error occurs while accessing the data source
     */
    public abstract long getTotalPushedAssets(final String assetId) throws DotDataException;
}
