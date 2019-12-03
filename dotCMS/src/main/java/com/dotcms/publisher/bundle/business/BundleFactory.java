package com.dotcms.publisher.bundle.business;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.exception.DotDataException;

import java.util.Date;
import java.util.List;

public abstract class BundleFactory {

    protected final static String INSERT_BUNDLE = "INSERT INTO publishing_bundle (id,name,publish_date,expire_date,owner,force_push) VALUES (?,?,?,?,?,?)";

    protected final static String INSERT_BUNDLE_ENVIRONMENT = "INSERT INTO publishing_bundle_environment (id,bundle_id,environment_id) VALUES (?,?,?)";

    protected final static String SELECT_UNSEND_BUNDLES = "SELECT * FROM publishing_bundle where publish_date is null and expire_date is null and owner = ? order by id desc";

    protected final static String SELECT_SENT_BUNDLES_BY_OWNER            = "SELECT * FROM publishing_bundle where publish_date is not null and owner = ? order by id desc";
    protected final static String SELECT_SENT_BUNDLES_BY_ADMIN            = "SELECT * FROM publishing_bundle where publish_date is not null order by id desc";

    protected final static String SELECT_SENT_BUNDLES_OLDER_THAN_BY_OWNER = "SELECT * FROM publishing_bundle where publish_date < ? and owner = ? order by id desc";
    protected final static String SELECT_SENT_BUNDLES_ORDER_THAN_BY_ADMIN = "SELECT * FROM publishing_bundle where publish_date < ? order by id desc";

    protected final static String SELECT_UNSEND_BUNDLES_LIKE_NAME = "SELECT * FROM publishing_bundle " +
            "where publish_date is null and expire_date is null and owner = ? and UPPER(name) like UPPER(?) order by publish_date desc";

    protected final static String SELECT_BUNDLE_BY_NAME = "SELECT * FROM publishing_bundle where UPPER(name) = UPPER(?)";

    protected final static String SELECT_BUNDLE_BY_ID = "SELECT * FROM publishing_bundle where id = ?";

    protected final static String DELETE_BUNDLE = "DELETE FROM publishing_bundle where id = ?";

    protected final static String UPDATE_BUNDLE = "UPDATE publishing_bundle SET name = ?, publish_date = ?, expire_date = ?, force_push = ? where id = ?";

    protected final static String UPDATE_BUNDLE_OWNER_REFERENCES = "UPDATE publishing_bundle SET owner = ? where owner = ?";

    protected final static String DELETE_ASSET_FROM_BUNDLE = "DELETE from publishing_queue where asset = ? and bundle_id = ?";

    protected final static String DELETE_ALL_ASSETS_FROM_BUNDLE = "DELETE from publishing_queue where bundle_id = ?";

    protected final static String DELETE_BUNDLE_ENVIRONMENT_BY_ENV = "DELETE from publishing_bundle_environment where environment_id = ?";

    protected final static String DELETE_BUNDLE_ENVIRONMENT_BY_BUNDLE = "DELETE from publishing_bundle_environment where bundle_id = ?";

    public abstract void saveBundle ( Bundle bundle ) throws DotDataException;

    public abstract void saveBundleEnvironment ( Bundle bundle, Environment e ) throws DotDataException;

    /**
     * Returns a list on un-send bundles (haven't been sent to any Environment) filtered by owner
     *
     * @param userId
     * @return
     * @throws DotDataException
     */
    public abstract List<Bundle> findUnsendBundles ( String userId ) throws DotDataException;

    /**
     * Returns a list on un-send bundles (haven't been sent to any Environment) filtered by owner
     *
     * @param userId
     * @param limit  -1 for no limit
     * @param offset
     * @return
     * @throws DotDataException
     */
    public abstract List<Bundle> findUnsendBundles ( String userId, int limit, int offset ) throws DotDataException;

    /**
     * Finds sent bundles older than "olderThan" when the owner is usedId
     * @param olderThan {@link Date}    will remove the bundles older than
     * @param userId    {@link String}  will remove the bundles own by userId
     * @param limit     {@link Integer} limit for pagination
     * @param offset    {@link Integer} offset for pagination
     * @return List of Bundles, empty if nothing to return
     * @throws DotDataException
     */
    public abstract List<Bundle> findSentBundles(Date olderThan, String userId, int limit, int offset)  throws DotDataException;

    /**
     * Finds sent bundles older than "olderThan" (suppose the user that calls this method is an admin)
     * @param olderThan {@link Date}    will remove the bundles older than
     * @param limit     {@link Integer} limit for pagination
     * @param offset    {@link Integer} offset for pagination
     * @return List of Bundles, empty if nothing to return
     * @throws DotDataException
     */
    public abstract List<Bundle> findSentBundles(Date olderThan, int limit, int offset)  throws DotDataException;

    /**
     * Finds sent bundles older than "olderThan" (suppose the user that calls this method is an admin)
     * @param limit     {@link Integer} limit for pagination
     * @param offset    {@link Integer} offset for pagination
     * @return List of Bundles, empty if nothing to return
     * @throws DotDataException
     */
    public abstract List<Bundle> findSentBundles(int limit, int offset)  throws DotDataException;

    /**
     * Finds sent bundles older than "olderThan" (suppose the user that calls this method is an admin)
     * @param userId    {@link String}  will remove the bundles own by userId
     * @param limit     {@link Integer} limit for pagination
     * @param offset    {@link Integer} offset for pagination
     * @return List of Bundles, empty if nothing to return
     * @throws DotDataException
     */
    public abstract List<Bundle> findSentBundles(String userId, int limit, int offset)  throws DotDataException;

    /**
     * Returns a list on un-send bundles (haven't been sent to any Environment) filtered by owner and name.
     * <br/>This method will return a list of bundles that contains the given words (like (%likeName%))
     *
     * @param userId
     * @param likeName
     * @param limit    -1 for no limit
     * @param offset
     * @return
     * @throws DotDataException
     */
    public abstract List<Bundle> findUnsendBundlesByName ( String userId, String likeName, int limit, int offset ) throws DotDataException;

    public abstract Bundle getBundleByName ( String name ) throws DotDataException;

    public abstract Bundle getBundleById ( String id ) throws DotDataException;

    public abstract void deleteBundle ( String id ) throws DotDataException;

    public abstract void updateBundle ( Bundle bundle ) throws DotDataException;

    public abstract void updateOwnerReferences ( String userId, String replacementUserId ) throws DotDataException;

    /**
     * Deletes an asset for a bundle
     * From the table: publishing_queue
     * @param assetId   {@link String} asset id
     * @param bundleId  {@link String} bundle id
     * @throws DotDataException
     */
    public abstract void deleteAssetFromBundle ( String assetId, String bundleId ) throws DotDataException;

    /**
     * Deletes all assets for a bundle
     * @param bundleId {@link String} bundle id
     * @throws DotDataException
     */
    public abstract void deleteAllAssetsFromBundle (String bundleId ) throws DotDataException;

    public abstract void deleteBundleEnvironmentByEnvironment ( String environmentId ) throws DotDataException;

	public abstract void deleteBundleEnvironmentByBundle(String bundleId) throws DotDataException;


}
