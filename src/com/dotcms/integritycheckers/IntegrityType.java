package com.dotcms.integritycheckers;

public enum IntegrityType {
    FOLDERS("push_publish_integrity_folders_conflicts", "folder", "FoldersToCheck.csv",
            "FoldersToFix.csv"),

    SCHEMES("push_publish_integrity_schemes_conflicts", "name", "SchemesToCheck.csv",
            "SchemesToFix.csv"),

    STRUCTURES("push_publish_integrity_structures_conflicts", "velocity_name",
            "StructuresToCheck.csv", "StructuresToFix.csv"),

    HTMLPAGES("push_publish_integrity_html_pages_conflicts", "html_page", "HtmlPagesToCheck.csv",
            "HtmlPagesToFix.csv"),

    CONTENTPAGES("push_publish_integrity_content_pages_conflicts", "html_page",
            "ContentPagesToCheck.csv", "ContentPagesToFix.csv", false),

    FILEASSETS("push_publish_integrity_content_file_assets_conflicts", "file_name",
            "ContentFileAssetsToCheck.csv", "ContentFileAssetsToFix.csv");

    private String label;
    private String firstDisplayColumnLabel;
    private String dataToCheckCSVName;
    private String dataToFixCSVName;
    // IMPORTANT: When a type has this flag in false, means that the process
    // results are handle by other integrity type, a good example is HTMLPAGES,
    // where the same results table is use by HTMLPAGE and CONTENTPAGE.
    // NOTE: This should be use as an EXCEPTION not as normal scenario.
    private boolean hasResultsTable;

    IntegrityType(String label, String firstDisplayColumnLabel, String dataToCheckCSVName,
            String dataToFixCSVName) {
        this.label = label;
        this.firstDisplayColumnLabel = firstDisplayColumnLabel;
        this.dataToCheckCSVName = dataToCheckCSVName;
        this.dataToFixCSVName = dataToFixCSVName;
        this.hasResultsTable = true;
    }

    IntegrityType(String label, String firstDisplayColumnLabel, String dataToCheckCSVName,
            String dataToFixCSVName, boolean hasResultsTable) {
        this.label = label;
        this.firstDisplayColumnLabel = firstDisplayColumnLabel;
        this.dataToCheckCSVName = dataToCheckCSVName;
        this.dataToFixCSVName = dataToFixCSVName;
        this.hasResultsTable = hasResultsTable;
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

    public IntegrityChecker createIntegrityCheckerInstance() {
        switch (this) {
        case FOLDERS:
            return new FolderIntegrityChecker();
        case SCHEMES:
            return new SchemeIntegrityChecker();
        case STRUCTURES:
            return new StructureIntegrityChecker();
        case HTMLPAGES:
            return new HtmlPageIntegrityChecker();
        case CONTENTPAGES:
            return new ContentPageIntegrityChecker();
        case FILEASSETS:
            return new ContentFileAssetIntegrityChecker();
        }

        throw new AssertionError("Unknown IntegrityChecker index: " + this);
    }
}
