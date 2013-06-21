package com.dotcms.publisher.bundle.business;

import java.util.List;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;

public class BundleAPIImpl implements BundleAPI {

	private BundleFactory bundleFactory;

	public BundleAPIImpl() {
		bundleFactory = FactoryLocator.getBundleFactory();
	}

	@Override
	public void saveBundle(Bundle bundle) throws DotDataException {
		bundleFactory.saveBundle(bundle);
	}

	@Override
	public void saveBundle(Bundle bundle, List<Environment> envs) throws DotDataException {
		saveBundle(bundle);
		saveBundleEnvironments(bundle, envs);
	}

	@Override
	public void saveBundleEnvironment(Bundle b, Environment e) throws DotDataException {
		bundleFactory.saveBundleEnvironment(b, e);
	}

	@Override
	public void saveBundleEnvironments(Bundle b, List<Environment> envs) throws DotDataException {
		for (Environment e : envs) {
			bundleFactory.saveBundleEnvironment(b, e);
		}
	}

}