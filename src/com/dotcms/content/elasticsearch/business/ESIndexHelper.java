package com.dotcms.content.elasticsearch.business;

import java.io.Serializable;
import java.util.Map;
import java.util.zip.ZipException;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.UtilMethods;

/**
 * A helper for {@link ESIndexAPI}
 */
public class ESIndexHelper implements Serializable{

	public final static ESIndexHelper INSTANCE = new ESIndexHelper();
	private final ESIndexAPI indexAPI;
	private final SiteSearchAPI searchAPI;

	private final String EXTENSION_PATTERN = "\\.[zZ][iI][pP]$";
	private final String FILE_PATTERN = ".*" + EXTENSION_PATTERN;

	private ESIndexHelper() {
		this.indexAPI = APILocator.getESIndexAPI();
		this.searchAPI = APILocator.getSiteSearchAPI();
	}

	@VisibleForTesting
	protected ESIndexHelper(ESIndexAPI indexAPI, SiteSearchAPI searchAPI) {
		this.indexAPI = indexAPI;
		this.searchAPI = searchAPI;
	}

	public String getIndexNameOrAlias(Map<String, String> map, String indexAttr, String aliasAttr) {
		String indexName = map.get(indexAttr);
		String indexAlias = map.get(aliasAttr);
		if (UtilMethods.isSet(indexAlias) && LicenseUtil.getLevel() >= 200) {
			String currentIndexName = indexAPI.getAliasToIndexMap(searchAPI.listIndices()).get(indexAlias);
			if (UtilMethods.isSet(currentIndexName))
				indexName = currentIndexName;
		}
		return indexName;
	}

	/**
	 * Snapshots file name will be used to determine the index name, the zip extension is required to do so
	 * the file name pattern is <index-name>.zip
	 * @param snapshotFilename snapshot file name
	 * @return true if the filename ends on ".zip"
	 */
	public boolean isSnapshotFilename(String snapshotFilename){
		return snapshotFilename.matches(FILE_PATTERN);
	}

	/**
	 * Snapshots file name will be used to determine the index name, the zip extension is required to do so
	 * the file name pattern is <index-name>.zip and this method removes the .zip part from the file name
	 * @param snapshotFilename snapshot file name
	 * @return file name with the .zip extension
	 */
	public String getIndexFromFilename(String snapshotFilename){
		return snapshotFilename.replaceAll(EXTENSION_PATTERN, "");
	}
}

