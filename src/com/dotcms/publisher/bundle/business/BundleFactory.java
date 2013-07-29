package com.dotcms.publisher.bundle.business;

import java.util.List;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.exception.DotDataException;

public abstract class BundleFactory {

	protected static String INSERT_BUNDLE = "INSERT INTO publishing_bundle VALUES (?,?,?,?,?,?)";

	protected static String INSERT_BUNDLE_ENVIRONMENT = "INSERT INTO publishing_bundle_environment VALUES (?,?,?)";

	protected static String SELECT_UNSEND_BUNDLES = "SELECT * FROM publishing_bundle where publish_date is null and expire_date is null and owner = ?";

	protected static String SELECT_BUNDLE_BY_NAME = "SELECT * FROM publishing_bundle where name = ?";

	protected static String SELECT_BUNDLE_BY_ID = "SELECT * FROM publishing_bundle where id = ?";

	protected static String DELETE_BUNDLE = "DELETE FROM publishing_bundle where id = ?";

	protected static String UPDATE_BUNDLE = "UPDATE publishing_bundle SET name = ?, publish_date = ?, expire_date = ? where id = ?";

	protected static String DELETE_ASSET_FROM_BUNDLE = "DELETE from publishing_queue where asset = ? and bundle_id = ?";

	protected static String DELETE_BUNDLE_ENVIRONMENT_BY_ENV = "DELETE from publishing_bundle_environment where environment_id = ?";

	protected static String DELETE_BUNDLE_ENVIRONMENT_BY_BUNDLE = "DELETE from publishing_bundle_environment where bundle_id = ?";

	public abstract void saveBundle(Bundle bundle) throws DotDataException;

	public abstract void saveBundleEnvironment(Bundle bundle, Environment e) throws DotDataException;

	public abstract List<Bundle> findUnsendBundles(String userId) throws DotDataException;

	public abstract Bundle getBundleByName(String name) throws DotDataException;

	public abstract Bundle getBundleById(String id) throws DotDataException;

	public abstract void deleteBundle(String id) throws DotDataException;

	public abstract void updateBundle(Bundle bundle) throws DotDataException;

	public abstract void deleteAssetFromBundle(String assetId, String bundleId) throws DotDataException;

	public abstract void deleteBundleEnvironmentByEnvironment(String environmentId) throws DotDataException;

	public abstract void deleteBundleEnvironmentByBundle(String bundleId) throws DotDataException;

}
