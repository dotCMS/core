package com.dotcms.publisher.bundle.business;

import com.dotcms.business.LazyUserAPIWrapper;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.publisher.assets.business.PushedAssetsFactory;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.util.CloseUtils;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.util.DotPreconditions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.FileUtil;
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

	@WrapInTransaction
	private void internalDeleteBundleAndDependencies (final String bundleId, final User user) throws DotDataException { // todo: make this batcheable

		this.validateBundleDeletePermission(user, bundleId);

		Logger.info(this, "Removing bundle: " + bundleId +
				" and dependencies by: " + user.getUserId());

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

			this.internalDeleteBundleAndDependencies(bundleId, user);
		} else {

			throw new NotFoundInDbException("The bundle id: " + bundleId + " does not exists");
		}
	}

	private void validateBundleDeletePermission(final User user, final String bundleId) throws DotDataException {

		/**
		if (!APILocator.getPermissionAPI().doesUserHavePermission(bundle,
				PermissionAPI.PERMISSION_EDIT, user, false)) {

			throw new DotSecurityException("User : " + user.getUserId() +
					", is not allowed to delete the bundle: " + bundleId)
		}*/
		// 	todo: bundle is not a permissionable yet, so can not validate yet.
	}

	// no mark as a wrap in transaction, it is one trax per bundle to delete.
	@Override
	public Set<String> deleteBundleAndDependenciesOlderThan(final Date olderThan, final User user) throws DotDataException {

		final ImmutableSet.Builder<String> bundlesDeleted = new ImmutableSet.Builder<>();
		final boolean isAdmin = this.userAPI.isCMSAdmin(user);

		try {

			Logger.info(this, "Deleting bundles older than: " + olderThan
					+ " by the user :" + user.getUserId());
			try (final Stream<String> bundleIds = this.getOlderBundleIds(olderThan, user.getUserId(), isAdmin)) {

				bundleIds.forEachOrdered(bundleId -> {

					this.internalDeleteBundleAndDependenciesThrowRuntime(bundleId, user);
					bundlesDeleted.add(bundleId);
				});
			}
		} catch (IOException e) {

			throw new DotDataException(e);
		}

		return bundlesDeleted.build();
	}

	private Stream<String> getOlderBundleIds(final Date olderThan, final String userId, final boolean isAdmin)
			throws IOException {

		return this.getBundleIds(
				(Integer limit, Integer offset) -> {

					try {
						return isAdmin?
								this.bundleFactory.findSentBundles(olderThan, limit, offset):
								this.bundleFactory.findSentBundles(olderThan, userId, limit, offset);
					} catch (DotDataException e) {

						throw new DotRuntimeException(e);
					}
				},
				Bundle::getId
				);
	}

	private void internalDeleteBundleAndDependenciesThrowRuntime(final String bundleId, final User user) {

		try {

			this.internalDeleteBundleAndDependencies(bundleId, user);
		} catch (DotDataException e) {

			throw new DotRuntimeException(e);
		}
	}

	// no mark as a wrap in transaction, it is one trax per bundle to delete.
	@Override
	public Set<String> deleteAllBundles(final User user,
										 final PublishAuditStatus.Status ...statuses) throws DotDataException {

		final ImmutableSet.Builder<String> bundlesDeleted = new ImmutableSet.Builder<>();
		final boolean isAdmin         = this.userAPI.isCMSAdmin(user);
		final List<Status> statusList = Arrays.asList(statuses);

		try {

			Logger.info(this, "Deleting all bundles with statuses: " + Arrays.asList(statuses) +
					"by the user: " + user.getUserId());
			try (final Stream<String> bundleIds = this.getAllBundleIds(statusList, user.getUserId(), isAdmin)) {

				bundleIds.forEachOrdered(bundleId -> {

					this.internalDeleteBundleAndDependenciesThrowRuntime(bundleId, user);
					bundlesDeleted.add(bundleId);
				});
			}
		} catch (IOException e) {

			throw new DotDataException(e);
		}

		return bundlesDeleted.build();
	} // deleteAllBundles.

	private Stream<String> getAllBundleIds(final List<Status> statusList, final String userId, final boolean isAdmin)
			throws IOException {

		return this.getBundleIds(
				(Integer limit, Integer offset) -> {

					try {

						return isAdmin?
								this.publishAuditAPI.getBundleIdByStatus(statusList, limit, offset) :
								this.publishAuditAPI.getBundleIdByStatusFilterByOwner(statusList, limit, offset, userId);
					} catch (DotDataException e) {

						throw new DotRuntimeException(e);
					}
				},
				(String bundleId) -> bundleId
		);
	}

	// no mark as a wrap in transaction, it is one trax per bundle to delete.
	@Override
	public Set<String>  deleteAllBundles(final User user) throws DotDataException {

		final ImmutableSet.Builder<String> bundlesDeleted = new ImmutableSet.Builder<>();
		final boolean isAdmin = this.userAPI.isCMSAdmin(user);

		Logger.info(this, "Deleting all bundles by user: " + user.getUserId());

		try {

			Logger.info(this, "Deleting all bundles by user: " + user.getUserId());
			try (final Stream<String> bundleIds = this.getAllBundleIds(user.getUserId(), isAdmin)) {

				bundleIds.forEachOrdered(bundleId -> {

					this.internalDeleteBundleAndDependenciesThrowRuntime(bundleId, user);
					bundlesDeleted.add(bundleId);
				});
			}
		} catch (IOException e) {

			throw new DotDataException(e);
		}

		return bundlesDeleted.build();
	}

	private Stream<String> getAllBundleIds(final String userId, final boolean isAdmin)
			throws IOException {

		return this.getBundleIds(
				(Integer limit, Integer offset) -> {

					try {
						return isAdmin?
								this.bundleFactory.findSentBundles(limit, offset):
								this.bundleFactory.findSentBundles(userId, limit, offset);
					} catch (DotDataException e) {

						throw new DotRuntimeException(e);
					}
				},
				Bundle::getId
		);
	}

	/**
	 * Returns a streamable list of bundles id, candidates to delete.
	 * @param bundleFinder {@link BiFunction} this function will receive the limit and current offset and returns the list of T bundles
	 * @param bundleToIdentifierConverter {@link Function} this function will convert the T bundle to bundle id as String
	 * @param <T>
	 * @return Stream of String (streamable list of bundles)
	 * @throws IOException
	 */
	private <T> Stream<String> getBundleIds(final BiFunction<Integer, Integer, List<T>> bundleFinder,  // receives limit and offset and returns the list of T
											final Function<T, String> bundleToIdentifierConverter)    // convert T to bundle id
			throws IOException {

		final File tempFile       = FileUtil.createTemporalFile("bundle-ids");
		final int limit           = 100;
		int offset                = 0;
		List<T> sentBundles       = bundleFinder.apply(limit, offset);


		// if the initial is less than limit, return it
		if (UtilMethods.isSet(sentBundles) && sentBundles.size() <= limit) {

			return sentBundles.stream().map(bundleToIdentifierConverter::apply);
		}

		try (final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(tempFile))) {

			while (UtilMethods.isSet(sentBundles)) {

				for (final T bundle : sentBundles) {

					fileWriter.write(bundleToIdentifierConverter.apply(bundle));
					fileWriter.newLine();
				}

				fileWriter.flush();

				offset       += limit + 1;
				sentBundles   = bundleFinder.apply(limit, offset);
			}
		}

		return Files.lines(tempFile.toPath());
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
