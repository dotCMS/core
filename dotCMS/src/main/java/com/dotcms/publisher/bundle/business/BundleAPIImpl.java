package com.dotcms.publisher.bundle.business;

import com.dotcms.publisher.assets.business.PushedAssetsFactory;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.util.DotPreconditions;

import java.util.Date;
import java.util.List;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class BundleAPIImpl implements BundleAPI {

	private final BundleFactory       bundleFactory;
	private final PushedAssetsFactory pushedAssetsFactory;
	private final PublishAuditAPI     publishAuditAPI;
	private final UserAPI             userAPI;

	public BundleAPIImpl() {

		this.bundleFactory       = FactoryLocator.getBundleFactory();
		this.pushedAssetsFactory = FactoryLocator.getPushedAssetsFactory();
		this.publishAuditAPI     = PublishAuditAPI.getInstance();
		this.userAPI             = APILocator.getUserAPI();
	}

	@WrapInTransaction
	@Override
	public void saveBundle(Bundle bundle) throws DotDataException {
		DotPreconditions.checkNotNull(bundle, IllegalArgumentException.class, "Bundle can't be null");
		DotPreconditions.checkNotNull(bundle.getOwner(), IllegalArgumentException.class, "Bundle needs to have an owner");

		bundleFactory.saveBundle(bundle);
	}

	@WrapInTransaction
	@Override
	public void saveBundle(Bundle bundle, List<Environment> envs) throws DotDataException {
		saveBundle(bundle);
		saveBundleEnvironments(bundle, envs);
	}

	@WrapInTransaction
	@Override
	public void saveBundleEnvironment(Bundle b, Environment e) throws DotDataException {
		bundleFactory.saveBundleEnvironment(b, e);
	}

	@WrapInTransaction
	@Override
	public void saveBundleEnvironments(Bundle b, List<Environment> envs) throws DotDataException {
		for (Environment e : envs) {
			bundleFactory.saveBundleEnvironment(b, e);
		}
	}

	@CloseDBIfOpened
	@Override
	public List<Bundle> getUnsendBundles(String userId) throws DotDataException {
		return bundleFactory.findUnsendBundles(userId);
	}

	@CloseDBIfOpened
    @Override
    public List<Bundle> getUnsendBundles ( String userId, int limit, int offset ) throws DotDataException {
        return bundleFactory.findUnsendBundles( userId, limit, offset );
    }

    @CloseDBIfOpened
    @Override
    public List<Bundle> getUnsendBundlesByName ( String userId, String likeName, int limit, int offset ) throws DotDataException {
        return bundleFactory.findUnsendBundlesByName( userId, likeName, limit, offset );
    }

    @CloseDBIfOpened
	@Override
	public Bundle getBundleByName(String name) throws DotDataException {
		return bundleFactory.getBundleByName(name);
	}

	@CloseDBIfOpened
	@Override
	public Bundle getBundleById(String id) throws DotDataException {
		return bundleFactory.getBundleById(id);
	}

	@WrapInTransaction
	@Override
	public void deleteBundle(String id) throws DotDataException {
		bundleFactory.deleteBundle(id);

	}

	@WrapInTransaction
	@Override
	public void deleteBundleAndDependencies(final String bundleId, final User user) throws DotDataException {

		// todo: check bundle deleting permissions with user

		try {

			this.publishAuditAPI.deletePublishAuditStatus(bundleId);
		} catch (DotPublisherException e) {

			throw new DotDataException(e);
		}

		Logger.info(this, "Removing all assets for a bundle");
		this.pushedAssetsFactory.deletePushedAssetsByBundle(bundleId);

		Logger.info(this, "Removing all assets from bundle: " + bundleId);
		this.bundleFactory.deleteAllAssetsFromBundle(bundleId);

		Logger.info(this, "Removing environments from bundle: " + bundleId);
		this.bundleFactory.deleteBundleEnvironmentByBundle(bundleId);

		Logger.info(this, "Removing bundle: " + bundleId);
		this.deleteBundle(bundleId);
	}

	@WrapInTransaction
	@Override
	public void deleteBundleAndDependenciesOlderThan(final Date olderThan, final User user) throws DotDataException {

		final int limit          = 100;
		int offset               = 0;
		final boolean isAdmin    = this.userAPI.isCMSAdmin(user);
		List<Bundle> sentBundles = isAdmin?
				this.bundleFactory.findSentBundles(olderThan, limit, offset):
				this.bundleFactory.findSentBundles(olderThan, user.getUserId(), limit, offset);

		Logger.info(this, "Deleting bundles older than: " + olderThan);
		while (UtilMethods.isSet(sentBundles)) {

			for (final Bundle bundle : sentBundles) {
				this.deleteBundleAndDependencies(bundle.getId(), user);
			}
			offset     += limit + 1;
			sentBundles = isAdmin?
					this.bundleFactory.findSentBundles(olderThan, limit, offset):
					this.bundleFactory.findSentBundles(olderThan, user.getUserId(), limit, offset);
		}
	}

	@WrapInTransaction
	@Override
	public void updateBundle(Bundle bundle) throws DotDataException {
		bundleFactory.updateBundle(bundle);
	}

	@WrapInTransaction
	@Override
    public void updateOwnerReferences( String userId, String replacementUserId ) throws DotDataException {
		bundleFactory.updateOwnerReferences(userId, replacementUserId);
	}

	@WrapInTransaction
	@Override
	public void deleteAssetFromBundle(String assetId, String bundleId)
			throws DotDataException {
		bundleFactory.deleteAssetFromBundle(assetId, bundleId);

	}

}
