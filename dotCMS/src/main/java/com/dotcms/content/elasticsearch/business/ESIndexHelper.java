package com.dotcms.content.elasticsearch.business;

import com.dotcms.enterprise.LicenseService;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.UtilMethods;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

/**
 * A helper for {@link ESIndexAPI}
 */
public class ESIndexHelper implements Serializable{

	public final static ESIndexHelper INSTANCE = new ESIndexHelper();
	private final SiteSearchAPI siteSearchAPI;
	private final LicenseService licenseService;

	private final static String EXTENSION_PATTERN = "\\.[zZ][iI][pP]$";
	private final static String FILE_PATTERN = ".*" + EXTENSION_PATTERN;
	private final static String INDEX = "index";
	private final static String ALIAS = "alias";
	public final static String SNAPSHOT_PREFIX = "snapshot-";
	private final static String SNAPSHOT_PREFIX_SHORT = "snap-";

	private ESIndexHelper() {
		this.siteSearchAPI = APILocator.getSiteSearchAPI();
		this.licenseService = new LicenseService();
	}

	@VisibleForTesting
	protected ESIndexHelper(SiteSearchAPI searchAPI, LicenseService licenseService) {
		this.siteSearchAPI = searchAPI;
		this.licenseService = licenseService;
	}

	/**
	 * Obtains the index or alias reference from a map, using the index key name
	 * as "index" and the alias key name as "alias"
	 * @param map map containing the key (type) and the name
	 * @return
	 */
	public String getIndexNameOrAlias(Map<String, String> map, ESIndexAPI esIndexAPI) {
		return getIndexNameOrAlias(map, INDEX, ALIAS, esIndexAPI);
	}

	/**
	 * Obtains the index or alias reference from a map
	 * @param map map containing the key (type) and the name
	 * @param indexAttr index key name
	 * @param aliasAttr alias key name
	 * @return
	 */
	public String getIndexNameOrAlias(Map<String, String> map, String indexAttr, String aliasAttr, ESIndexAPI esIndexAPI) {
		String indexName = map.get(indexAttr);
		String indexAlias = map.get(aliasAttr);
		if (UtilMethods.isSet(indexAlias) && licenseService.getLevel() >= LicenseLevel.STANDARD.level) {
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

	/**
	 * Finds file within the directory named "snapshot-"
	 * @param snapshotDirectory
	 * @return
	 * @throws IOException
	 */
	public String findSnapshotName(File snapshotDirectory) throws IOException {
		String name = null;
		if (snapshotDirectory.isDirectory()) {
			class SnapshotVisitor extends SimpleFileVisitor<Path> {
				private String fileName;

				public String getFileName() {
					return fileName;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.getFileName().toString().startsWith(SNAPSHOT_PREFIX) ||
						file.getFileName().toString().startsWith(SNAPSHOT_PREFIX_SHORT)) {
						fileName = file.getFileName().toString();
						return FileVisitResult.TERMINATE;
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			}
			Path directory = Paths.get(snapshotDirectory.getAbsolutePath());
			SnapshotVisitor snapshotVisitor = new SnapshotVisitor();
			Files.walkFileTree(directory, snapshotVisitor);

			String fullName = snapshotVisitor.getFileName();
			if (fullName != null) {
				name = fullName.replaceFirst(SNAPSHOT_PREFIX, "");
				name = name.replaceFirst(SNAPSHOT_PREFIX_SHORT, "");
			}
		}
		return name;
	}
}

