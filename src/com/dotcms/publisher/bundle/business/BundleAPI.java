package com.dotcms.publisher.bundle.business;

import java.util.List;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.exception.DotDataException;

public interface BundleAPI {

	/**
	 * persists the given Bundle object to the underlying data layer.
	 *
	 * @param	bundle	the bundle to be persisted
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public void saveBundle(Bundle bundle)  throws DotDataException;

	/**
	 * persists the given Bundle object, and sets the given Environment List as the destination environments
	 * of the bundle.
	 *
	 * The environments included in the given List must already exist, they will not be created.
	 *
	 * @param	bundle	the bundle to be persisted
	 * @param	environments	the list of destination environments
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public void saveBundle(Bundle bundle, List<Environment> environments)  throws DotDataException;

	/**
	 * persists the given Environment as a destination of the given Bundle.
	 *
	 * Both the environment and the bundle must exist, they will not be created.
	 *
	 * @param	bundle	the bundle whose destination the given environment will be added to
	 * @param	environments	the environment to be saved as destination of the given bundle
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public void saveBundleEnvironment(Bundle b, Environment e) throws DotDataException;

	/**
	 * persists the given Environment List as a destination of the given Bundle
	 *
	 * Both the environments in the List and the bundle must exist, they will not be created.
	 *
	 * @param	bundle	the bundle whose destination the given environments will be added to
	 * @param	environments	the environments to be saves as destinations of the given bundle
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public void saveBundleEnvironments(Bundle b, List<Environment> envs) throws DotDataException;

	/**
	 * returns a List of bundles that haven't been sent to any Environment
	 *
	 *
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public List<Bundle> getUnsendBundles(String userId) throws DotDataException;

    /**
     * Returns a list on un-send (haven't been sent to any Environment) bundles filtered by owner
     *
     * @param userId
     * @param limit  -1 for no limit
     * @param offset
     * @return
     * @throws DotDataException
     */
    public List<Bundle> getUnsendBundles ( String userId, int limit, int offset ) throws DotDataException;

    /**
     * Returns a list on un-send bundles (haven't been sent to any Environment) filtered by owner and name.
     * <br/>This method will return a list of bundles that start by the given name (like (likeName%))
     *
     * @param userId
     * @param likeName
     * @param limit    -1 for no limit
     * @param offset
     * @return
     * @throws DotDataException
     */
    public List<Bundle> getUnsendBundlesByName ( String userId, String likeName, int limit, int offset ) throws DotDataException;

	/**
	 * returns the Bundle object with the given name, if any
	 *
	 *
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */
	public Bundle getBundleByName(String name) throws DotDataException;

	/**
	 * returns the Bundle object with the given id, if any
	 *
	 *
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */
	public Bundle getBundleById(String id) throws DotDataException;

	/**
	 * deletes the Bundle with the given id
	 *
	 *
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */
	public void deleteBundle(String id) throws DotDataException;

	/**
	 * updates the Bundle with the given id
	 *
	 *
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */
	public void updateBundle(Bundle bundle) throws DotDataException;

	/**
	 * deletes the Asset with the given assetId from the Bundle with the given bundleId
	 *
	 *
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */
	public void deleteAssetFromBundle(String assetId, String bundleId) throws DotDataException;



}
