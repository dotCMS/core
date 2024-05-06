package com.dotcms.publisher.bundle.business;

import com.dotcms.business.LazyUserAPIWrapper;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.publisher.assets.business.PushedAssetsAPI;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.output.TarGzipBundleOutput;
import com.dotcms.util.DotPreconditions;

import com.dotmarketing.util.*;

import io.vavr.control.Try;
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
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.GenerateBundlePublisher;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import org.apache.commons.lang3.mutable.MutableInt;

public class BundleAPIImpl implements BundleAPI {

	private final BundleFactory       bundleFactory;
	private final PushedAssetsAPI pushedAssetsAPI;
	private final PublishAuditAPI     publishAuditAPI;
	private final UserAPI             userAPI;

	public BundleAPIImpl() {

		this.bundleFactory       = FactoryLocator.getBundleFactory();
		this.pushedAssetsAPI     = APILocator.getPushedAssetsAPI();
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
			Logger.error(this,"Error Removing audit status for bundle: " + bundleId,e);
			throw new DotDataException(e);
		}

        //According to https://github.com/dotcms/core/issues/18025
        //any deleteBundle operation should NOT touch or delete pushedAssets.
        //pushedAsset should only get removed when calling deletePushedAssetsByEnvironment endpoint.

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

		try {
			final PublishAuditStatus publishAuditStatus =
					this.publishAuditAPI.getPublishAuditStatus(bundleId);
			Bundle bundle = null;
			if (!UtilMethods.isSet(publishAuditStatus)) {
				bundle = this.getBundleById(bundleId);
			}
			if (UtilMethods.isSet(publishAuditStatus) || UtilMethods.isSet(bundle)) {
				this.internalDeleteBundleAndDependencies(bundleId, user);
			} else {
				Logger.error(this, "The bundle id: " + bundleId + " does not exists");
				throw new NotFoundInDbException("The bundle id: " + bundleId + " does not exists");
			}
		} catch (DotPublisherException e) {

			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e);
		}
	}

	private void validateBundleDeletePermission(final User user, final String bundleId) throws DotDataException {

		//This is meanwhile we make bundles permissionable, if the user is not an admin only the owner of the bundle can delete it
		if(!userAPI.isCMSAdmin(user)){
			if(!getBundleById(bundleId).getOwner().equals(user.getUserId())){
				Logger.error(this,"User: " + user.getUserId() + " is not an admin or is not the owner of the bundle");
				throw new DotDataException("User: " + user.getUserId() + " is not an admin or is not the owner of the bundle");
			}
		}
	}

	// no mark as a wrap in transaction, it is one trax per bundle to delete.
	@Override
	public BundleDeleteResult deleteBundleAndDependenciesOlderThan(final Date olderThan, final User user) throws DotDataException {

		if(olderThan.after(new Date())){
			Logger.error(this,"To avoid deleting bundles that publish in the future, the date can not be after the current date");
			throw new IllegalArgumentException("To avoid deleting bundles that publish in the future, the date can not be after the current date");
		}

		final ImmutableSet.Builder<String> bundlesDeleted = new ImmutableSet.Builder<>();
		final ImmutableSet.Builder<String> bundlesFailed = new ImmutableSet.Builder<>();
		final boolean isAdmin = this.userAPI.isCMSAdmin(user);
		final int  bundleSleepCount   = Config.getIntProperty ("bundle.sleep.count",  10);  // each 10 deletes
		final long millisBundleSleep  = Config.getLongProperty("bundle.sleep.millis", 25l); // wait 25 millis
		final MutableInt deletedCount = new MutableInt(0);
		final MutableInt failedCount  = new MutableInt(0);

		try {

			Logger.info(this, "Deleting bundles older than: " + olderThan
					+ " by the user :" + user.getUserId());
			try (final Stream<String> bundleIds = this.getOlderBundleIds(olderThan, user.getUserId(), isAdmin)) {

				bundleIds.forEachOrdered(bundleId -> {

					if (this.internalDeleteBundleAndDependencies(bundleId, user,
							deletedCount.getValue(), bundleSleepCount, millisBundleSleep)) {
						bundlesDeleted.add(bundleId);
						deletedCount.increment();
					} else {
						bundlesFailed.add(bundleId);
						failedCount.increment();
					}
				});
			}

			Logger.info(this, "Deleted " + deletedCount.getValue()
					+ " bundles, failed " + failedCount.getValue() + " bundles");
		} catch (IOException e) {
			Logger.error(this,"Error Removing bundles older than: " + olderThan + " by the user :" + user.getUserId(),e);
			throw new DotDataException(e);
		}

		return new BundleDeleteResult(bundlesFailed.build(), bundlesDeleted.build());
	}

	@CloseDBIfOpened
	private Stream<String> getOlderBundleIds(final Date olderThan, final String userId, final boolean isAdmin)
			throws IOException {

		return this.getBundleIds(
				(Integer limit, Integer offset) -> {

					try {
						return isAdmin?
								this.bundleFactory.findSentBundles(olderThan, limit, offset):
								this.bundleFactory.findSentBundles(olderThan, userId, limit, offset);
					} catch (DotDataException e) {
						Logger.error(this,"Error getting bundles ids older than: " + olderThan + " by the user :" + userId,e);
						throw new DotRuntimeException(e);
					}
				},
				Bundle::getId
				);
	}

	private boolean internalDeleteBundleAndDependencies(final String bundleId, final User user,
														final int currentCount, final int bundleSleepCount,
														final long millisBundleSleep) {

		boolean success = true;
		try {

			this.internalDeleteBundleAndDependencies(bundleId, user);
			if (currentCount % bundleSleepCount == 0) {
				DateUtil.sleep(millisBundleSleep); // we decided to wait a bit in order to avoid starvation on the db connections
			}
		} catch (DotDataException e) {

			Logger.error(this,"Error Removing bundle: " + bundleId + " by the user :" + user.getUserId(), e);
			success = false;
		}

		return success;
	}

	// no mark as a wrap in transaction, it is one trax per bundle to delete.
	@Override
	public BundleDeleteResult deleteAllBundles(final User user,
										 final PublishAuditStatus.Status ...statuses) throws DotDataException {

		final ImmutableSet.Builder<String> bundlesFailed  = new ImmutableSet.Builder<>();
		final ImmutableSet.Builder<String> bundlesDeleted = new ImmutableSet.Builder<>();
		final boolean isAdmin         = this.userAPI.isCMSAdmin(user);
		final List<Status> statusList = Arrays.asList(statuses);
		final int  bundleSleepCount   = Config.getIntProperty ("bundle.sleep.count",  10);  // each 10 deletes
		final long millisBundleSleep  = Config.getLongProperty("bundle.sleep.millis", 25l); // wait 25 millis
		final MutableInt deletedCount = new MutableInt(0);
		final MutableInt failedCount = new MutableInt(0);

		try {

			Logger.info(this, "Deleting all bundles with statuses: " + Arrays.asList(statuses) +
					"by the user: " + user.getUserId());
			try (final Stream<String> bundleIds = this.getAllBundleIds(statusList, user.getUserId(), isAdmin)) {

				bundleIds.forEachOrdered(bundleId -> {

					if (this.internalDeleteBundleAndDependencies(bundleId, user,
							deletedCount.getValue(), bundleSleepCount, millisBundleSleep)) {
						bundlesDeleted.add(bundleId);
						deletedCount.increment();
					} else {
						bundlesFailed.add(bundleId);
						failedCount.increment();
					}
				});
			}
		} catch (IOException e) {
			Logger.error(this, "Deleting all bundles with statuses: " + Arrays.asList(statuses) +
					"by the user: " + user.getUserId(),e);
			throw new DotDataException(e);
		}

		Logger.info(this, "Deleted " + deletedCount.getValue()
				+ " bundles, failed " + failedCount.getValue() + " bundles");

		return new BundleDeleteResult(bundlesFailed.build(), bundlesDeleted.build());
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
						Logger.error(this, "Error getting all bundles with statuses: " + statusList.toString() +
								"by the user: " + userId,e);
						throw new DotRuntimeException(e);
					}
				},
				(String bundleId) -> bundleId
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

		final File tempFile       = FileUtil.createTemporaryFile("bundle-ids");
		final int limit           = 100;
		int offset                = 0;
		List<T> sentBundles       = bundleFinder.apply(limit, offset);


		// if the initial is less than limit, return it
		if (UtilMethods.isSet(sentBundles) && sentBundles.size() < limit) {

			return sentBundles.stream().map(bundleToIdentifierConverter::apply);
		}

		try (final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(tempFile))) {

			while (UtilMethods.isSet(sentBundles)) {

				for (final T bundle : sentBundles) {

					fileWriter.write(bundleToIdentifierConverter.apply(bundle));
					fileWriter.newLine();
				}

				fileWriter.flush();

				offset       += limit;
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
	public void deleteAssetFromBundleAndAuditStatus(String assetId, String bundleId)
			throws DotDataException {
		deleteAssetFromBundle(assetId, bundleId);

		List<PublishQueueElement> queueElements = Try.of(()->PublisherAPI.getInstance()
				.getQueueElementsByBundleId(bundleId)).getOrElse(Collections::emptyList);

		if(queueElements.isEmpty()) {
			Try.run(()->APILocator.getPublishAuditAPI().deletePublishAuditStatus(bundleId))
					.getOrElseThrow(DotDataException::new);
		}
	}

	@WrapInTransaction
	@Override
	public void deleteAssetFromBundle(String assetId, String bundleId)
			throws DotDataException {
		bundleFactory.deleteAssetFromBundle(assetId, bundleId);
	}

	/**
	 * This takes a Bundle, generates the folder/file structure and returns the resulting directory
	 * as a File handle. It will not delete the bundle directory if it already existed.
	 * @param bundle - Bundle to generate
	 * @return
	 */
    @CloseDBIfOpened
    public File generateTarGzipBundleFile(final Bundle bundle) {

        final PushPublisherConfig pushPublisherConfig = new PushPublisherConfig(bundle);
        pushPublisherConfig.setPublishers(Arrays.asList(GenerateBundlePublisher.class));

        try {
			final PublishStatus publishStatus =
					APILocator.getPublisherAPI().publish(pushPublisherConfig);
			return publishStatus.getOutputFiles().get(0);
        }
        catch(final Exception e) {
        	Logger.error(this,e.getMessage(),e);
            throw new DotRuntimeException(e);
        }
    }
	
	
}
