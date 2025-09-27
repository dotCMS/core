package com.dotcms.publisher.assets.business;

import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.rest.api.v1.content.PushedAssetHistory;
import com.dotmarketing.exception.DotDataException;

import java.util.List;

public interface PushedAssetsAPI {

	/**
	 * persists the given PushedAsset object to the underlying data layer.
	 *
	 * @param	asset	the pushed asset to be persisted
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public void savePushedAsset(PushedAsset asset)  throws DotDataException;

	/**
	 * deletes the push assets entries for the given Bundle Id and Environment Id.
	 *
	 * @param	bundleId	the id of the bundle
	 * @param	environmentId	the id of the environment
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public void deletePushedAssets(String bundleId, String environmentId)  throws DotDataException;

	/**
	 * deletes all the push assets entries
	 *
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public void deleteAllPushedAssets()  throws DotDataException;

	/**
	 * deletes the push assets entries for the given Asset Id.
	 *
	 * @param	assetId	the id of the asset whose pushed assets records will be deleted
	 *
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public void deletePushedAssets(String assetId)  throws DotDataException;

	/**
	 * deletes the push assets entries for the given environment Id.
	 *
	 * @param	environmentId	the id of the environment whose pushed assets records will be deleted
	 *
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */
	public void deletePushedAssetsByEnvironment(String environmentId)  throws DotDataException;

	/**
	 * deletes the push assets entries for the given assetId and environment Id.
	 *
	 * @param	assetId	the id of the asset whose pushed assets records will be deleted
	 * @param	environmentId	the id of the environment whose pushed assets records will be deleted
	 *
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public void deletePushedAssetsByEnvironment(String assetId, String environmentId)  throws DotDataException;

	/**
	 * returns all the push assets for a given assetId
	 *
	 * @param	assetId	the id of the asset
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public List<PushedAsset> getPushedAssets(String assetId)  throws DotDataException;


	/**
	 * return the most recent pushed asset entry for a given assetId, environmentId and endpointIds. 
	 * If there is no register then return null
	 * @param assetId the id of the asset
	 * @param environmentId the id of the environment
	 * @param endpointIds the ids of the environment used
	 * @return the most recent register of the pushed asset
	 * @throws DotDataException
	 */
	public PushedAsset getLastPushForAsset(String assetId, String environmentId, String endpointIds)  throws DotDataException;

	/**
	 * deletes the push assets entries for the given Bundle Id
	 *
	 * @param	bundleId	the id of the bundle
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public void deletePushedAssetsByBundleId(final String bundleId)  throws DotDataException;
	/**
	 * returns all the push assets for a given bundleId and EnvironmentId
	 *
	 * @param	bundleId	the id of the bundle
	 * @param	environmentId	the id of the environment
	 * @throws	DotDataException	thrown when an error in the underlying data layer occurs
	 */

	public List<PushedAsset> getPushedAssetsByBundleIdAndEnvironmentId(final String bundleId, final String environmentId)  throws DotDataException;

    /**
     * Retrieves a paginated list of pushed asset history entries for a given asset.
     *
     * @param assetId the identifier of the asset whose push history is requested
     * @param offset  zero-based index of the first record to return
     * @param limit   maximum number of records to return
     * @return a list of {@link PushedAssetHistory} entries ordered by push date
     * @throws DotDataException if an error occurs while accessing the data source
     */
    List<PushedAssetHistory> getPushedAssets(String assetId, int offset, int limit) throws DotDataException;

    /**
     * Returns the total number of push history entries for the specified asset.
     *
     * @param assetId the identifier of the asset
     * @return total number of push history records for the asset
     * @throws DotDataException if an error occurs while accessing the data source
     */
    long getTotalPushedAssets(String assetId) throws DotDataException;
}
