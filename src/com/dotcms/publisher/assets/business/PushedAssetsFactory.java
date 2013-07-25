package com.dotcms.publisher.assets.business;

import java.util.List;

import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotmarketing.exception.DotDataException;

public abstract class PushedAssetsFactory {

	protected static String INSERT_ASSETS = "INSERT INTO publishing_pushed_assets VALUES (?,?,?,?,?)";
	protected static String SELECT_ASSETS_BY_BUNDLE_ENV= "SELECT * FROM publishing_pushed_assets WHERE bundle_id = ? and environment_id = ?";
	protected static String SELECT_ASSETS_BY_ASSET_ID= "SELECT * FROM publishing_pushed_assets WHERE asset_id = ? ORDER BY push_date";
	protected static String SELECT_ASSETS_BY_ENV_ID= "SELECT * FROM publishing_pushed_assets WHERE environment_id = ?";
	protected static String DELETE_ASSETS_BY_BUNDLE_ENV= "DELETE FROM publishing_pushed_assets WHERE bundle_id = ? and environment_id = ?";
	protected static String DELETE_ASSETS_BY_ASSET_ID= "DELETE FROM publishing_pushed_assets WHERE asset_id = ?";
	protected static String DELETE_ASSETS_BY_ENVIRONMENT_ID= "DELETE FROM publishing_pushed_assets WHERE environment_id = ?";
	protected static String DELETE_ALL_ASSETS= "TRUNCATE publishing_pushed_assets";

	public abstract void savePushedAsset(PushedAsset asset) throws DotDataException;

	public abstract void deletePushedAssets(String bundleId, String environmentId)  throws DotDataException;

	public abstract void deletePushedAssets(String assetId)  throws DotDataException;

	public abstract void deletePushedAssetsByEnvironment(String environmentId)  throws DotDataException;

	public abstract List<PushedAsset> getPushedAssets(String bundleId, String environmentId)  throws DotDataException;

	public abstract void deleteAllPushedAssets() throws DotDataException;

	public abstract List<PushedAsset> getPushedAssets(String assetId) throws DotDataException;

	public abstract List<PushedAsset> getPushedAssetsByEnvironment(String assetId) throws DotDataException;

}
