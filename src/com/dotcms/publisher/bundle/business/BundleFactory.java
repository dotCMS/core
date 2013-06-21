package com.dotcms.publisher.bundle.business;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.exception.DotDataException;

public abstract class BundleFactory {

	protected static String INSERT_BUNDLE = "INSERT INTO publishing_bundle VALUES (?,?,?,?,?)";

	protected static String INSERT_BUNDLE_ENVIRONMENT = "INSERT INTO publishing_bundle_environment VALUES (?,?,?)";

	public abstract void saveBundle(Bundle bundle) throws DotDataException;

	public abstract void saveBundleEnvironment(Bundle bundle, Environment e) throws DotDataException;

}