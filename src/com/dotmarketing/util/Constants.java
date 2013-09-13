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
    public static final String JOBS_LIST = "jobs_list";
    public static final String FOUR_OH_FOUR_RESPONSE = "FOUR_OH_FOUR_RESPONSE";
    public static final String RESUMES_LIST = "resumes_list";
   	public static final String REORDER = "reorder";
    public static final String SAVE = "save";
   	public static final String ACTIVATE = "activate";
	public static final String UNDELETE = "undelete";
	public static final String DELETE = "delete";
	public static final String ARCHIVE = "archive";
	public static final String UNARCHIVE = "unarchive";
	public static final String FULL_DELETE = "full_delete";	
	public static final String FULL_DELETE_LIST = "full_delete_list";
	public static final String DELETEVERSION = "deleteversion";
	public static final String PUBLISH = "publish";
	public static final String FULL_PUBLISH_LIST = "full_publish_list";
    public static final String ASSIGNTO = "assignto";
	public static final String PREPUBLISH = "prepublish";
	public static final String PREPUBLISHWORKFLOW = "prepublishworkflow";
	public static final String UNPUBLISH = "unpublish";
	public static final String FULL_UNPUBLISH_LIST = "full_unpublish_list";
	public static final String FULL_REINDEX_LIST = "full_reindex_list";
	public static final String GETVERSIONBACK = "getversionback";
	public static final String ASSETVERSIONS = "assetversions";
	public static final String LOCK = "lock";
	public static final String UNLOCK = "unlock";
	public static final String GETPARENTS = "getparents";
	public static final String COPY = "copy";
	public static final String MOVE = "move";
	public static final String NEW = "new";
	public static final String NEW_EDIT = "newedit";
	public static final String ADD_PARENTS = "addparents";
	public static final String DEL_PARENTS = "delparents";
	public static final String PREVIEW = "preview";
	public static final String SET_AS_DEFAULT = "set_as_default";
    public static final String CHANGE_STATUS = "change_status";
    public static final String ADD_COMMENT = "add_comment";
    public static final String ADD_FILE = "add_file";
    public static final String REMOVE_FILE = "remove_file";
    public static final String ASSIGN_TASK = "assign_task";
    public static final String RENAME = "rename";
    public static final String UPDATE_CAMPAIGN_PERMISSIONS_ONLY = "update_campaign_permissions_only";
    public static final String FULL_ARCHIVE_LIST = "full_archive_list";
    public static final String FULL_UNARCHIVE_LIST = "full_unarchive_list";
    public static final String APPLY_SERIES = "apply_series";

    public static final String CONTENTLET_MAIN_IMAGE = "main_image";
   	public static final String CONTENTLET_MAIN_LINK = "main_link";
	
    public static final String TEMPLATE_ADD_CONTAINER = "add_container";
    public static final String TEMPLATE_ADD_FILE = "add_file";
    public static final String TEMPLATE_PREPROCESS = "#parse (\"preprocess.vl\")";
    public static final String TEMPLATE_POSTPROCESS = "#parse (\"postprocess.vl\")";
    
    public static final String CONTAINER_ADD_VARIABLE = "add_variable";
    
    public static final String HTML_PAGE_TMP_DIR = "/tmp_jsps";
    public static final String HTML_PAGE_DIR = "/live_jsps";
    
	public static final String SAVE_SERIES = "saveSeries";
	public static final String DELETE_SERIES = "deleteSeries";
	public static final String SHOW_REGISTRATIONS = "showRegistrations";


	public static final String ONE = "1";
	public static final String RESET = "reset";
	
    public static final String DELETE_LIST = "deleteList";
    public static final String RESET_STATUS = "resetStatus";
	//Events
	public static final String[] EVENT_APPROVAL_STATUSES = {"Waiting for Approval", "Approved", "Disapproved"};
	public static final int[] EVENT_APPROVAL_STATUS_VALUES = {0, 1, 2};
	
	public static final int EVENT_WAITING_APPROVAL_STATUS = 0;
	public static final int EVENT_APPROVED_STATUS = 1;
	public static final int EVENT_DISAPPROVED_STATUS = 2;
	public static String APPROVE = "approve";
	public static String DISAPPROVE = "disapprove";
	
	public static final int APPLY_CHILD_PERMISSION_THREAD_SLEEP = 100;
	public static final int APPLY_CHILD_PERMISSION_THREAD_COMMIT = 3;
	
	public static final String FULL_UNLOCK_LIST = "full_unlock_list";
	


	//Database
	public static final String DATABASE_DEFAULT_DATASOURCE = "jdbc/dotCMSPool";
	
	//Reg Ex validation
	public static final String REG_EX_VALIDATION_DATE_WITH_FORWARDSLASH = "^[0-2]?[0-9](/|-)[0-3]?[0-9](/|-)[1-2][0-9][0-9][0-9]$";
	
	//Used to set a url to serve for the LocalResourcesServlet
	public static final String SERVE_URL = "com.dotmarketing.util.Constants.SERVER_URL";

	// http://jira.dotmarketing.net/browse/DOTCMS-2178
	// Used to create cache directory
	public static final String CACHE_PATH = "assets" + java.io.File.separator + "cache";

	public static final String EDIT_TEXT = "editText";
	public static final String RFC2822_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
	
	//http://jira.dotmarketing.net/browse/DOTCMS-6442
	public static final String WYSIWYG_PLAIN_SEPARATOR = "@PLAIN";

    public static final String PROPERTIES_UPDATE_FILE_LOCATION = "com/dotcms/autoupdater/update.properties";
    public static final String PROPERTY_UPDATE_FILE_UPDATE_URL = "update.url";

    //Request Headers
    public static String USER_AGENT_DOTCMS_BROWSER = "DOTCMS-BROWSER";
    public static String USER_AGENT_DOTCMS_CMIS = "DOTCMS-CMIS";
    public static String USER_AGENT_DOTCMS_HTMLPAGEDIFF = "DOTCMS-HTMLPAGEDIFF";
    public static String USER_AGENT_DOTCMS_SITESEARCH = "DOTCMS-SITESEARCH";
    public static String USER_AGENT_DOTCMS_TIMEMACHINE = "DOTCMS-TIMEMACHINE";
    
}