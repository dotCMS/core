/**
 *
 */
package com.dotcms.publisher.assets.business;

import com.dotcms.publisher.assets.bean.PushedAsset;

public interface PushedAssetsCache {
	public PushedAsset getPushedAsset(String assetId, String environmentId);
	public void add(PushedAsset anAsset);
	public void removePushedAssetById(String assetId, String environmentId);
	public void clearCache();
}
