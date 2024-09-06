package com.dotcms.content.elasticsearch.constants;

/**
 * This class define the Contentlets ESMapping constants
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 21, 2017
 */
public final class ESMappingConstants {



    /**
     * Constructor
     */
    private ESMappingConstants(){}

    /**
     * Field contentlets elastic types
     */
    public static final String FIELD_ELASTIC_TYPE_INTEGER = "integer";
    public static final String FIELD_ELASTIC_TYPE_DATE = "date";
    public static final String FIELD_ELASTIC_TYPE_BOOLEAN = "bool";
    public static final String FIELD_ELASTIC_TYPE_FLOAT = "float";
    public static final String FIELD_ELASTIC_TYPE_STRING = "string";

    /**
     * Field TYPE
     */
    public static final String FIELD_TYPE_TAG = "tag";
    public static final String FIELD_TYPE_SYSTEM_FIELD = "system_field";
    public static final String FIELD_TYPE_SECTION_DIVIDER = "section_divider";
    public static final String FIELD_TYPE_TIME = "time";
    public static final String FIELD_TYPE_DATE_TIME = "date_time";
    public static final String FIELD_TYPE_CATEGORY = "category";
    public static final String FIELD_TYPE_CHECKBOX = "checkbox";
    public static final String FIELD_TYPE_MULTI_SELECT = "multi_select";
    public static final String FIELD_TYPE_KEY_VALUE = "key_value";
    public static final String FIELD_TYPE_RELATIONSHIP = "relationship";
    /**
     * Contentlet properties
     */
    public static final String TITLE = "title";
    public static final String STRUCTURE_NAME = "structureName";
    public static final String CONTENT_TYPE = "contentType";
    public static final String STRUCTURE_TYPE = "structureType";
    public static final String BASE_TYPE = "baseType";
    public static final String TYPE ="type";
    public static final String CONTENT = "content";
    public static final String INODE = "inode";

    // boolean that says if a content type is or not a system.
    public static final String SYSTEM_TYPE = "systemType";

    public static final String MOD_DATE = "modDate";
    public static final String CREATION_DATE = "creationDate";
    public static final String OWNER = "owner";
    public static final String MOD_USER = "modUser";
    public static final String LIVE = "live";
    public static final String WORKING = "working";
    public static final String LOCKED = "locked";
    public static final String DELETED = "deleted";
    public static final String LANGUAGE_ID = "languageId";
    public static final String VARIANT = "variant";
    public static final String IDENTIFIER = "identifier";
    public static final String CONTENTLET_HOST = "conHost";
    public static final String CONTENTLET_HOSTNAME = "conHostName";
    public static final String CONTENTLET_FOLER = "conFolder";
    public static final String PARENT_PATH = "parentPath";
    public static final String PATH = "path";
    public static final String SHORT_ID = "shortId";
    public static final String SHORT_INODE ="shortInode";
    public static final String WORKFLOW_CREATED_BY = "wfcreatedBy";
    public static final String WORKFLOW_ASSIGN = "wfassign";
    public static final String WORKFLOW_STEP   = "wfstep";
    public static final String WORKFLOW_CURRENT_STEP   = "wfCurrentStepName";
    public static final String WORKFLOW_CURRENT_STEP_NOT_ASSIGNED_VALUE   = "notassigned";
    public static final String WORKFLOW_SCHEME = "wfscheme";
    public static final String WORKFLOW_MOD_DATE = "wfModDate";
    public static final String PUBLISH_DATE = "pubdate";
    public static final String EXPIRE_DATE = "expdate";
    public static final String VERSION_TS = "versionTs";
    public static final String SYS_PUBLISH_DATE = "sysPublishDate";
    public static final String SYS_PUBLISH_USER = "sysPublishUser";
    public static final String URL_MAP = "urlMap";
    public static final String VANITY_URL = "vanityUrl";
    public static final String CATEGORIES = "categories";
    public static final String TAGS = "tags";
    public static final String PERSONAS = "personas";
    public static final String PERSONA_KEY_TAG = "personaKeyTag";

    public static final String DOT_INDEX_PATTERN = "dotIndexPattern";
    public static final String PERMISSIONS = "permissions";
    public static final String OWNER_CAN_READ = "ownerCanRead";
    public static final String OWNER_CAN_WRITE = "ownerCanWrite";
    public static final String OWNER_CAN_PUBLISH = "ownerCanPublish";

    public static final String CHILD = "child";
    public static final String PARENT = "parent";
    public static final String RELATION_TYPE = "relation_type";
    public static final String TREE_ORDER = "tree_order";

    @Deprecated
    public static final String SUFFIX_CHILD = "-child";

    @Deprecated
    public static final String SUFFIX_PARENT = "-parent";

    @Deprecated
    public static final String SUFFIX_ORDER = "-order";


}
