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

	@Override
	public List<Bundle> getUnsendBundles(String userId) throws DotDataException {
		return bundleFactory.findUnsendBundles(userId);
	}

    @Override
    public List<Bundle> getUnsendBundles ( String userId, int limit, int offset ) throws DotDataException {
        return bundleFactory.findUnsendBundles( userId, limit, offset );
    }

    @Override
    public List<Bundle> getUnsendBundlesByName ( String userId, String likeName, int limit, int offset ) throws DotDataException {
        return bundleFactory.findUnsendBundlesByName( userId, likeName, limit, offset );
    }

	@Override
	public Bundle getBundleByName(String name) throws DotDataException {
		return bundleFactory.getBundleByName(name);
	}

	@Override
	public Bundle getBundleById(String id) throws DotDataException {
		return bundleFactory.getBundleById(id);
	}

	@Override
	public void deleteBundle(String id) throws DotDataException {
		bundleFactory.deleteBundle(id);

	}

	@Override
	public void updateBundle(Bundle bundle) throws DotDataException {
		bundleFactory.updateBundle(bundle);
	}

	@Override
	public void deleteAssetFromBundle(String assetId, String bundleId)
			throws DotDataException {
		bundleFactory.deleteAssetFromBundle(assetId, bundleId);

	}

}
