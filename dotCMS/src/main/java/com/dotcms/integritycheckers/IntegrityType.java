package com.dotcms.integritycheckers;

public enum IntegrityType {

	FOLDERS(
    	new FolderIntegrityChecker(), "push_publish_integrity_folders_conflicts", "folder",
    	"FoldersToCheck.csv", "FoldersToFix.csv"
    ),

    SCHEMES(
    	new SchemeIntegrityChecker(), "push_publish_integrity_schemes_conflicts", "name",
    	"SchemesToCheck.csv", "SchemesToFix.csv"
    ),

    STRUCTURES(
    	new StructureIntegrityChecker(), "push_publish_integrity_structures_conflicts", "velocity_name",
    	"StructuresToCheck.csv", "StructuresToFix.csv"
    ),

    HTMLPAGES(
    	new ContentPageIntegrityChecker(), "push_publish_integrity_html_pages_conflicts", "html_page",
    	"ContentPagesToCheck.csv", "ContentPagesToFix.csv"
    ),

    FILEASSETS(
    	new ContentFileAssetIntegrityChecker(), "push_publish_integrity_content_file_assets_conflicts", "file_name",
    	"ContentFileAssetsToCheck.csv", "ContentFileAssetsToFix.csv"
    ),

    CMS_ROLES(
    	new RoleIntegrityChecker(), "push_publish_integrity_cms_roles_conflicts", "name",
    	"CmsRolesToCheck.csv", "CmsRolesToFix.csv"
    );


	private IntegrityChecker integrityChecker;
    private String label;
    private String firstDisplayColumnLabel;
    private String dataToCheckCSVName;
    private String dataToFixCSVName;
    // IMPORTANT: When a type has this flag in false, means that the process
    // results are handle by other integrity type, a good example is HTMLPAGES,
    // where the same results table is use by HTMLPAGE and CONTENTPAGE.
    // NOTE: This should be use as an EXCEPTION not as normal scenario.
    private boolean hasResultsTable;


    IntegrityType(IntegrityChecker integrityChecker, String label, String firstDisplayColumnLabel, String dataToCheckCSVName,
            String dataToFixCSVName) {
    	this(integrityChecker, label, firstDisplayColumnLabel, dataToCheckCSVName, dataToFixCSVName, true);
    }

    IntegrityType(IntegrityChecker integrityChecker, String label, String firstDisplayColumnLabel, String dataToCheckCSVName,
            String dataToFixCSVName, boolean hasResultsTable) {
    	this.integrityChecker = integrityChecker;
        this.label = label;
        this.firstDisplayColumnLabel = firstDisplayColumnLabel;
        this.dataToCheckCSVName = dataToCheckCSVName;
        this.dataToFixCSVName = dataToFixCSVName;
        this.hasResultsTable = hasResultsTable;
    }

    public IntegrityChecker getIntegrityChecker() {
    	return integrityChecker;
    }

    public String getLabel() {
        return label;
    }
    
    public String getFirstDisplayColumnLabel() {
        return firstDisplayColumnLabel;
    }

    public String getDataToCheckCSVName() {
        return dataToCheckCSVName;
    }

    public String getDataToFixCSVName() {
        return dataToFixCSVName;
    }

    private static final String RESULT_TABLE_SUFFIX = "_ir";

    public String getResultsTableName() {
        return name().toLowerCase() + RESULT_TABLE_SUFFIX;
    }

    public boolean hasResultsTable() {
        return hasResultsTable;
    }
}
