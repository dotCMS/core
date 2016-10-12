package com.dotcms.content.elasticsearch.business;

import java.io.Serializable;
import java.util.Map;

import com.dotcms.enterprise.LicenseService;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.UtilMethods;

/**
 * A helper for {@link ESIndexAPI}
 */
public class ESIndexHelper implements Serializable{

	public final static ESIndexHelper INSTANCE = new ESIndexHelper();
	private final ESIndexAPI esIndexAPI;
	private final SiteSearchAPI siteSearchAPI;
	private final LicenseService licenseService;

	private final String EXTENSION_PATTERN = "\\.[zZ][iI][pP]$";
	private final String FILE_PATTERN = ".*" + EXTENSION_PATTERN;
	private final String INDEX = "index";
	private final String ALIAS = "alias";

	private ESIndexHelper() {
		this.esIndexAPI = APILocator.getESIndexAPI();
		this.siteSearchAPI = APILocator.getSiteSearchAPI();
		this.licenseService = new LicenseService();
	}

	@VisibleForTesting
	protected ESIndexHelper(ESIndexAPI indexAPI, SiteSearchAPI searchAPI, LicenseService licenseService) {
		this.esIndexAPI = indexAPI;
		this.siteSearchAPI = searchAPI;
		this.licenseService = licenseService;
	}

	/**
	 * Obtains the index or alias reference from a map, using the index key name
	 * as "index" and the alias key name as "alias"
	 * @param map map containing the key (type) and the name
	 * @return
	 */
	public String getIndexNameOrAlias(Map<String, String> map) {
		return getIndexNameOrAlias(map, INDEX, ALIAS);
	}

	/**
	 * Obtains the index or alias reference from a map
	 * @param map map containing the key (type) and the name
	 * @param indexAttr index key name
	 * @param aliasAttr alias key name
	 * @return
	 */
	public String getIndexNameOrAlias(Map<String, String> map, String indexAttr, String aliasAttr) {
		String indexName = map.get(indexAttr);
		String indexAlias = map.get(aliasAttr);
		if (UtilMethods.isSet(indexAlias) && licenseService.getLevel() >= 200) {
			String currentIndexName = esIndexAPI.getAliasToIndexMap(siteSearchAPI.listIndices()).get(aliasAttr);
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
		if(snapshotFilename == null){
			return false;
		}
		return snapshotFilename.matches(FILE_PATTERN);
	}

	/**
	 * Snapshots file name will be used to determine the index name, the zip extension is required to do so
	 * the file name pattern is <index-name>.zip and this method removes the .zip part from the file name
	 * @param snapshotFilename snapshot file name
	 * @return file name with the .zip extension
	 */
	public String getIndexFromFilename(String snapshotFilename){
		if(snapshotFilename == null || StringUtils.isEmpty(snapshotFilename)){
			return null;
		}
		return snapshotFilename.toLowerCase().replaceAll(EXTENSION_PATTERN, "");
	}
}

