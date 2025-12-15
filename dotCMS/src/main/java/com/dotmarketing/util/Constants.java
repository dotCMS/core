package com.dotmarketing.util;

/**
 * @author Steven Sajous
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public final class Constants {
    
    //Commands CMD
   	public static final String REORDER = "reorder";
    public static final String SAVE = "save";
	public static final String UNDELETE = "undelete";
	public static final String DELETE = "delete";
	public static final String ARCHIVE = "archive";
	public static final String UNARCHIVE = "unarchive";
	public static final String FULL_DELETE = "full_delete";	
	public static final String FULL_DELETE_LIST = "full_delete_list";
	public static final String DELETEVERSION = "deleteversion";
	public static final String PUBLISH = "publish";
	public static final String FULL_PUBLISH_LIST = "full_publish_list";
	public static final String PREPUBLISH = "prepublish";
	public static final String UNPUBLISH = "unpublish";
	public static final String FULL_UNPUBLISH_LIST = "full_unpublish_list";
	public static final String FULL_REINDEX_LIST = "full_reindex_list";
	public static final String GETVERSIONBACK = "getversionback";
	public static final String ASSETVERSIONS = "assetversions";
	public static final String UNLOCK = "unlock";
	public static final String COPY = "copy";
	public static final String MOVE = "move";
	public static final String NEW = "new";
	public static final String NEW_EDIT = "newedit";
	public static final String PREVIEW = "preview";
    public static final String ADD_COMMENT = "add_comment";
    public static final String ADD_FILE = "add_file";
    public static final String REMOVE_FILE = "remove_file";
    public static final String RENAME = "rename";
    public static final String FULL_ARCHIVE_LIST = "full_archive_list";
    public static final String FULL_UNARCHIVE_LIST = "full_unarchive_list";

    public static final String CONTAINER_ADD_VARIABLE = "add_variable";

	public static final String ONE = "1";
	public static final String RESET = "reset";
	
	public static final String FULL_UNLOCK_LIST = "full_unlock_list";

	//Database
	public static final String DATABASE_DEFAULT_DATASOURCE = "jdbc/dotCMSPool";

	public static final String RFC2822_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
	
	//http://jira.dotmarketing.net/browse/DOTCMS-6442
	public static final String WYSIWYG_PLAIN_SEPARATOR = "@PLAIN";
	public static final String TOGGLE_EDITOR_SEPARATOR = "@ToggleEditor";
	
    public static final String PROPERTIES_UPDATE_FILE_LOCATION = "com/dotcms/autoupdater/update.properties";
    public static final String PROPERTY_UPDATE_FILE_UPDATE_URL = "update.url";

    //Request Headers
    public static String USER_AGENT_DOTCMS_BROWSER = "DOTCMS-BROWSER";
    public static String USER_AGENT_DOTCMS_SITESEARCH = "DOTCMS-SITESEARCH";
    public static String USER_AGENT_DOTCMS_TIMEMACHINE = "DOTCMS-TIMEMACHINE";
    public static String USER_AGENT_DOTCMS_PUSH_PUBLISH = "DOTCMS-PUSHPUBLISH";

	// REGEX to validate emails
	public static final String REG_EX_EMAIL = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";

	public static final String CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD = "DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD";

	/**
	 * Base path that contains the folder container definitions
	 */
	public static final String CONTAINER_FOLDER_PATH   = "/application/containers";

	/**
	 * This is the name of the meta info for a file based container.
	 */
	public static final String CONTAINER_META_INFO_FILE_NAME   = "container.vtl";

	/**
	 * Base path that contains the folder template definitions
	 */
	public static final String TEMPLATE_FOLDER_PATH   = "/application/templates";

	/**
	 * This is the name of the meta info for a file based template.
	 */
	public static final String TEMPLATE_META_INFO_FILE_NAME   = "properties.vtl";

	/**
	 * Base path that contains the folder theme definitions
	 */
	public static final String THEME_FOLDER_PATH   = "/application/themes";

	/**
	 * This is the name of the meta info for a file based theme.
	 */
	public static final String THEME_META_INFO_FILE_NAME   = "template.vtl";

	/**
	 * Extension for the velocity file: .vtl
	 */
	public static final String VELOCITY_FILE_EXTENSION = ".vtl";

	/**
	 * Extension for the JSON file: .json
	 */
	public static final String JSON_FILE_EXTENSION = ".json";

	public static final boolean RESPECT_FRONT_END_ROLES = Boolean.TRUE;
	public static final boolean DONT_RESPECT_FRONT_END_ROLES = Boolean.FALSE;

	public static final String CONTENT_EDITOR2_ENABLED = "CONTENT_EDITOR2_ENABLED";

}