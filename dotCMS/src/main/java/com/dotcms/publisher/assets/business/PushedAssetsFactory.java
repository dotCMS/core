package com.dotcms.publisher.assets.business;

import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotmarketing.exception.DotDataException;

import java.util.List;

public abstract class PushedAssetsFactory {

	protected static String INSERT_ASSETS = "INSERT INTO publishing_pushed_assets VALUES (?,?,?,?,?,?,?)";
	protected static String SELECT_ASSETS_BY_BUNDLE_ENV= "SELECT * FROM publishing_pushed_assets WHERE bundle_id = ? and environment_id = ?";
	protected static String SELECT_ASSETS_BY_ASSET_ID= "SELECT * FROM publishing_pushed_assets WHERE asset_id = ? ORDER BY push_date";
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

	public abstract List<PushedAsset> getPushedAssetsByEnvironment(String assetId) throws DotDataException;

	public abstract PushedAsset getLastPushForAsset(String assetId, String environmentId, String endpointIds)  throws DotDataException;

}
