package com.dotcms.publisher.bundle.business;

import com.dotcms.business.LazyUserAPIWrapper;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.publisher.assets.business.PushedAssetsFactory;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.util.DotPreconditions;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;

public class BundleAPIImpl implements BundleAPI {

	private final BundleFactory       bundleFactory;
	private final PushedAssetsFactory pushedAssetsFactory;
	private final PublishAuditAPI     publishAuditAPI;
	private final UserAPI             userAPI;

	public BundleAPIImpl() {

		this.bundleFactory       = FactoryLocator.getBundleFactory();
		this.pushedAssetsFactory = FactoryLocator.getPushedAssetsFactory();
		this.publishAuditAPI     = APILocator.getPublishAuditAPI();
		this.userAPI             = new LazyUserAPIWrapper(); // we need this lazyness to avoid init issues.
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

	private void deleteBundleAndDependencies (final Bundle bundle, final User user) throws DotDataException {

		final String bundleId = bundle.getId();
		this.validateBundleDeletePermission(user, bundle);

		Logger.info(this, "Removing bundle: " + bundleId + " and dependencies by: " + user.getUserId());

		try {

			Logger.info(this, "Removing audit status for a bundle: " + bundleId);
			this.publishAuditAPI.deletePublishAuditStatus(bundleId);
		} catch (DotPublisherException e) {

			throw new DotDataException(e);
		}

		Logger.info(this, "Removing all pushed assets for a bundle: " + bundleId);
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
	public void deleteBundleAndDependencies(final String bundleId, final User user) throws DotDataException {

		final Bundle bundle = this.getBundleById(bundleId);
		if (null != bundle) {

			this.deleteBundleAndDependencies(bundle, user);
		} else {

			throw new NotFoundInDbException("The bundle id: " + bundleId + " does not exists");
		}
	}

	private void validateBundleDeletePermission(final User user, final Bundle bundle) throws DotDataException {

		/**
		if (!APILocator.getPermissionAPI().doesUserHavePermission(bundle,
				PermissionAPI.PERMISSION_EDIT, user, false)) {

			throw new DotSecurityException("User : " + user.getUserId() +
					", is not allowed to delete the bundle: " + bundleId)
		}*/
		// todo: bundle is not a permissionable yet, so can not validate yet.
	}

	@WrapInTransaction
	@Override
	public Set<String> deleteBundleAndDependenciesOlderThan(final Date olderThan, final User user) throws DotDataException {

		final ImmutableSet.Builder<String> bundlesDeleted = new ImmutableSet.Builder<>();
		final int limit          = 100;
		int offset               = 0;
		final boolean isAdmin    = this.userAPI.isCMSAdmin(user);
		List<Bundle> sentBundles = isAdmin?
				this.bundleFactory.findSentBundles(olderThan, limit, offset):
				this.bundleFactory.findSentBundles(olderThan, user.getUserId(), limit, offset);

		Logger.info(this, "Deleting bundles older than: " + olderThan);
		while (UtilMethods.isSet(sentBundles)) {

			for (final Bundle bundle : sentBundles) {

				this.deleteBundleAndDependencies(bundle, user);
				bundlesDeleted.add(bundle.getId());
			}

			sentBundles = isAdmin?
					this.bundleFactory.findSentBundles(olderThan, limit, offset):
					this.bundleFactory.findSentBundles(olderThan, user.getUserId(), limit, offset);
		}

		return bundlesDeleted.build();
	}

	@WrapInTransaction
	@Override
	public Set<String>  deleteAllBundles(final User user,
										 final PublishAuditStatus.Status ...statuses) throws DotDataException {

		final ImmutableSet.Builder<String> bundlesDeleted = new ImmutableSet.Builder<>();
		final int limit          = 100;
		final int offset         = 0;
		final boolean isAdmin    = this.userAPI.isCMSAdmin(user);
		final List<Status> statusList = Arrays.asList(statuses);
		List<String> bundleIDs = isAdmin ? publishAuditAPI.getBundleIdByStatus(statusList,limit,offset) :
				publishAuditAPI.getBundleIdByStatusFilterByOwner(statusList,limit,offset,user.getUserId());

		Logger.info(this, "Deleting all bundles with statuses: " + Arrays.asList(statuses));

		while (UtilMethods.isSet(bundleIDs)) {

			for (final String bundleId : bundleIDs) {

				this.deleteBundleAndDependencies(this.getBundleById(bundleId),user);
				bundlesDeleted.add(bundleId);
			}

			bundleIDs = isAdmin ? publishAuditAPI.getBundleIdByStatus(statusList,limit,offset) :
					publishAuditAPI.getBundleIdByStatusFilterByOwner(statusList,limit,offset,user.getUserId());
		}

		return bundlesDeleted.build();
	} // deleteAllBundles.

	@WrapInTransaction
	@Override
	public Set<String>  deleteAllBundles(final User user)//todo: this should pull the bundles from the publishing_queue_audit??
			throws DotDataException {

		final ImmutableSet.Builder<String> bundlesDeleted = new ImmutableSet.Builder<>();
		final int limit          = 100;
		final int offset         = 0;
		final boolean isAdmin    = this.userAPI.isCMSAdmin(user);
		List<Bundle> sentBundles = isAdmin?
				this.bundleFactory.findSentBundles(limit, offset):
				this.bundleFactory.findSentBundles(user.getUserId(), limit, offset);

		Logger.info(this, "Deleting all bundles...");
		while (UtilMethods.isSet(sentBundles)) {

			for (final Bundle bundle : sentBundles) {

				this.deleteBundleAndDependencies(bundle.getId(), user);
				bundlesDeleted.add(bundle.getId());
			}

			sentBundles = isAdmin?
					this.bundleFactory.findSentBundles(limit, offset):
					this.bundleFactory.findSentBundles(user.getUserId(), limit, offset);
		}

		return bundlesDeleted.build();
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
