package com.dotmarketing.startup.runonce;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This upgrade task will migrate all the Virtual Links that exits on the DB (virtual_link table),
 * if there is one virtual link with a site that no longer exists it will be deleted. The old
 * virtual_link fields are migrated like this: Title -> Title Url -> This is splited, one is the
 * Site and the other one the Uri Uri -> Forward To
 *
 * @author erickgonzalez
 * @author oswaldogallango
 * @version 4.2.0
 * @since Jun 8, 2017
 */
public class Task04205MigrateVanityURLToContent extends AbstractJDBCStartupTask {


    private static final String GET_HOST_STRUCTURE_INODE_QUERY = "select inode from structure where name='Host'";
    private static final String COUNT_VIRTUAL_LINK_QUERY = "select count(*) as total from virtual_link";
    private static final String SELECT_VIRTUAL_LINK_QUERY = "select inode, title, url, uri from virtual_link";
    private static final String UPDATE_INODE_QUERY = "update inode set type = 'contentlet' where type = 'virtual_link' ";
    private static final String SELECT_SITEID =
            "select contentlet.identifier, contentlet.text1, contentlet.text_area1 from contentlet"
                    + " join contentlet_version_info"
                    + " on contentlet.inode = contentlet_version_info.working_inode"
                    + " where contentlet.structure_inode = ?";
    private static final String SELECT_SITEID_BY_NAME_QUERY = SELECT_SITEID
            + " and lower(contentlet.text1) = ?";
    private static final String SELECT_SITEID_BY_ALIAS_NAME_QUERY = SELECT_SITEID
            + " and lower(contentlet.text_area1) like ?";
    private static final String SELECT_DEFAULT_LANGUAGE = "select id from language where language_code = ? and country_code = ?";
    private static final String INSERT_IDENTIFIER_QUERY = "insert into identifier(id, parent_path, "
            + "asset_name, host_inode, asset_type) values (?,?,?,?,?)";
    private String insertContentletQuery =
            "insert into contentlet(inode, show_on_menu, sort_order, title, mod_date, mod_user, "
                    + " friendly_name, structure_inode, identifier, language_id, text1, text2, text3, text4, "
                    + " integer1, integer2, integer3, integer4, integer5, integer6, integer7, integer8, "
                    + " integer9, integer10, integer11, integer12, integer13, integer14, integer15, "
                    + " integer16, integer17, integer18, integer19, integer20, integer21, integer22, "
                    + " integer23, integer24, integer25, "
                    + " \"float1\", \"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\","
                    + " \"float8\", \"float9\", \"float10\", \"float11\", \"float12\","
                    + " \"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\","
                    + " \"float19\", \"float20\", \"float21\", \"float22\", \"float23\", \"float24\", "
                    + " \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10,"
                    + " bool11, bool12, bool13, bool14, bool15, bool16, bool17, bool18, bool19, bool20,"
                    + " bool21, bool22, bool23, bool24, bool25) "
                    + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,"
                    + " ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,"
                    + " ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final String INSERT_CONTENTLET_VERSION_INFO_QUERY =
            "insert into contentlet_version_info(identifier, lang, working_inode, live_inode, deleted, locked_on, version_ts)"
                    + "values (?,?,?,?,?,?,?)";
    private static final String DELETE_INODE_BY_INODE_QUERY = "delete from inode where inode = ?";
    private static final String DELETE_VIRTUAL_LINK_QUERY = "delete from virtual_link";
    private static final String DROP_TABLE_VIRTUAL_LINK_QUERY = "drop table virtual_link";

    private static final String EXTERNAL_LINK_CONDITION = "//";
    private static final int CODE_301 = 301;
    private static final int CODE_200 = 200;
    private static final int FOR_LIMIT_48 = 48;
    private static final int FOR_LIMIT_25 = 25;
    private static final int INT_1 = 1;
    private static final int LIMIT = 100;

    private static final String ERROR_MESSAGE = "Error executing Task04205MigrateVanityURLToContent: %s";
    private Map<String, String> siteIds = new HashMap<>();

    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException {

        Connection conn =null;
        Logger.info(this, "============= Running Task04205MigrateVanityURLToContent =============");
        try {

			/* drop virtual link constraints*/
            dropConstraints();

            conn = DbConnectionFactory.getConnection();
            conn.setAutoCommit(true);
            DotConnect dc = new DotConnect();

            //Update Inode Table, since previously the type was 'virtual_link' and now needs to be 'contentlet'
            dc.setSQL(UPDATE_INODE_QUERY);
            dc.loadResult();
            Logger.info(this, "Type in the Inode Table has been changed to contentlet");

            //Getting amount of virtual links to process on batch of 100
            dc.setSQL(COUNT_VIRTUAL_LINK_QUERY);
            int virtualLinkCount = Integer
                    .parseInt(((Map<String, String>) dc.loadResults().get(0)).get("total"));

            int totalCycles = Math.floorDiv(virtualLinkCount, LIMIT);

            //Gets the default language Id, so when saving the Vanity URL it's the same lang
            int defaultLanguageId = findDefaultLanguageId(dc);
            //Get the Inode for the Host Content Type
            String hostContentTypeInode = findHostContentTypeInode(dc);

            //Processing on batch of 100
            for (int batchCycles = 0; batchCycles <= totalCycles; batchCycles++) {
                int counter = LIMIT * batchCycles;

                //Get all the virtual_links of the current batch cycle
                dc.setSQL(SELECT_VIRTUAL_LINK_QUERY);
                dc.setStartRow(counter);
                dc.setMaxRows(LIMIT);
                List<Map<String, String>> virtualLinks = dc.loadResults();

                for (Map<String, String> virtualLink : virtualLinks) {
                    //process the current vitual link
                    processVirtualLink(dc, virtualLink, hostContentTypeInode, defaultLanguageId);
                }
            }

            // Deletes all rows in the Virtual Link Table
            dc.setSQL(DELETE_VIRTUAL_LINK_QUERY);
            dc.loadResult();

            //Drop virtual link table
            dc.setSQL(DROP_TABLE_VIRTUAL_LINK_QUERY);
            dc.loadResult();
        } catch (Exception e) {
            Logger.error(this,
                    String.format(ERROR_MESSAGE,
                            e.getMessage()), e);
        } finally {
            try {
                conn.close();
            }catch (SQLException e){
                Logger.error(this,
                        String.format(ERROR_MESSAGE,
                                e.getMessage()), e);
            }

        }
        Logger.info(this,
                "============= Finished Task04205MigrateVanityURLToContent =============");
    }

    /**
     * This method generate the new vanityUrl contentlet from the virtual Link.
     *
     * @param dc The DotConnect object
     * @param virtualLink The virtualLink Map object
     * @param hostContentTypeInode The inode of the host content type
     * @param defaultLanguageId The default language Id
     */
    private void processVirtualLink(DotConnect dc, Map<String, String> virtualLink,
            String hostContentTypeInode, int defaultLanguageId) throws DotDataException {

        String oldURL = virtualLink.get("url");
        String inode = virtualLink.get("inode");
        String title = virtualLink.get("title");
        String oldUri = virtualLink.get("uri");
        Logger.info(this,
                "Transforming Virtual Link into Vanity URL (inode:" + inode
                        + " - url:"
                        + oldURL + " - uri:" + oldUri + ")");

        // Calls the method that splits the URL into the site and the uri
        Map<String, String> siteAndUriMap = splitURL(oldURL, hostContentTypeInode);

        String site = siteAndUriMap.get("siteId");
        String uri = siteAndUriMap.get("uri");

        //If the site returns null means that there is no host
        if (site == null) {
            Logger.warn(this,
                    "Vanity URL cannot be migrated to Content because the Host does not exist."
                            + " You will need to create it manually, Title: "
                            + title + ", URL: " + oldURL + ", URI: " + oldUri);
            dc.setSQL(DELETE_INODE_BY_INODE_QUERY);
            dc.addParam(inode);
            dc.loadResult();
        } else {

            String uuid = UUIDGenerator.generateUuid();

            // Insert on Identifier Table
            dc.setSQL(INSERT_IDENTIFIER_QUERY);
            dc.addParam(uuid); // identifier
            dc.addParam("/"); // parent_path
            dc.addParam("content." + inode); // asset_name
            dc.addParam(site); // host_inode
            dc.addParam("contentlet"); // asset_type
            dc.loadResult();

            // Insert on Contentlet Table
            if (DbConnectionFactory.isMySql()) {
                // Use correct escape char when using reserved words as column names
                insertContentletQuery = insertContentletQuery
                        .replaceAll("\"", "`");
            }
            dc.setSQL(insertContentletQuery);
            dc.addParam(inode); // inode
            dc.addParam(false); // show_on_menu
            dc.addParam(0); // sort_order
            dc.addParam(title); // title
            dc.addParam(new Date()); // mod_date
            dc.addParam(UserAPI.SYSTEM_USER_ID); // mod_user
            dc.addParam(title); // friendly_name
            dc.addParam("8e850645-bb92-4fda-a765-e67063a59be0"); // structure_inode
            dc.addParam(uuid); // identifier
            dc.addParam(defaultLanguageId); // language_id
            dc.addParam(title); // text1(title)
            dc.addParam(site); // text2(site)
            dc.addParam(uri); // text3(uri)
            dc.addParam(oldUri); // text4(forwardTo)

            //If the uri contains // then redirect if not do forward
            if(oldUri.contains(EXTERNAL_LINK_CONDITION)){
                dc.addParam(CODE_301); // integer1(action)
            }else {
                dc.addParam(CODE_200); // integer1(action)
            }

            dc.addParam(0); // integer2(sort)
            for (int i = 0; i < FOR_LIMIT_48; i++) {
                dc.addParam(0); // all other integer and float fields
            }
            for (int i = 0; i < FOR_LIMIT_25; i++) {
                dc.addParam(false); // all bool fields
            }

            dc.loadResult();

            // Insert on Contentlet_Version_Info
            dc.setSQL(INSERT_CONTENTLET_VERSION_INFO_QUERY);
            dc.addParam(uuid); // identifier
            dc.addParam(defaultLanguageId); // lang
            dc.addParam(inode); // live_inode
            dc.addParam(inode); // working_inode
            dc.addParam(false); // deleted
            dc.addParam(new Date()); // locked_on
            dc.addParam(new Date()); // version_ts
            dc.loadResult();
        }

    }

    /**
     * This method splits the old Url into the site and uri. Also make the call for the {@link
     * #findSiteId(String, String)} that returns the site identifier
     *
     * @param url The value of the url column on the virtual_link table.
     * @param hostContentTypeInode The inode of the Host content type
     * @return a map with the site identifier and uri
     */
    private Map<String, String> splitURL(String url, String hostContentTypeInode)
            throws DotDataException {
        String hostId = Host.SYSTEM_HOST;
        String newURL = "";
        int hostSplit = url.indexOf(':');
        if (hostSplit != -1) {
            String siteName = url.substring(0, url.lastIndexOf(':'));
            newURL = url.substring(hostSplit + INT_1);
            hostId = findSiteId(siteName.toLowerCase(), hostContentTypeInode);
        } else {
            newURL = url;
        }

        return Map.of("siteId", hostId, "uri", newURL);
    }

    /**
     * This method searches the site identifier, if it doesn't exists it returns null.
     *
     * @param siteName The site name
     * @param hostContentTypeInode The Host content type identifier
     * @return the identifier of the site
     */
    private String findSiteId(String siteName, String hostContentTypeInode)
            throws DotDataException {
        String siteId = siteIds.get(siteName);

        if (siteId == null) {
            DotConnect dc = new DotConnect();
            dc.setSQL(SELECT_SITEID_BY_NAME_QUERY);
            dc.addParam(hostContentTypeInode);
            dc.addParam(siteName);
            List<Map<String, String>> result = dc.loadResults();

            if (!result.isEmpty()) {
                siteId = result.get(0).get("identifier");
                siteIds.put(siteName, siteId);
            } else {
                //search the site by alias
                siteId = findSiteIdByAlias(siteName, hostContentTypeInode);
            }
        }
        return siteId;
    }

    /**
     * This method searches the site identifier by alias, if it doesn't exists it returns null.
     *
     * @param siteName The site name
     * @param hostContentTypeInode The Host content type identifier
     * @return the identifier of the site
     */
    private String findSiteIdByAlias(String siteName, String hostContentTypeInode)
            throws DotDataException {
        String siteId = null;

        DotConnect dc = new DotConnect();
        dc.setSQL(SELECT_SITEID_BY_ALIAS_NAME_QUERY);
        dc.addParam(hostContentTypeInode);
        dc.addParam("%" + siteName + "%");
        List<Map<String, String>> result = dc.loadResults();
        if (!result.isEmpty()) {
            for (Map<String, String> site : result) {
                siteId = site.get("identifier");
                String aliases = site.get("text_area1");
                for (String value : aliases.split("\n")) {
                    siteIds.put(value, siteId);
                }
            }

        }
        //here get the id from the map to avoid false/positives
        return siteIds.get(siteName);
    }

    /**
     * This method searches for the dotCMS default Language Id.
     *
     * @param dc DotConnect Object
     * @return The default language Id
     */
    private int findDefaultLanguageId(DotConnect dc) throws DotDataException {

        dc.setSQL(SELECT_DEFAULT_LANGUAGE);
        dc.addParam(Config.getStringProperty("DEFAULT_LANGUAGE_CODE", "en"));
        dc.addParam(Config.getStringProperty("DEFAULT_LANGUAGE_COUNTRY_CODE", "US"));

        List<Map<String, String>> siteLanguage = dc.loadResults();
        String siteLang = siteLanguage.get(0).get("id");

        return Integer.parseInt(siteLang);
    }

    /**
     * Get the content type inode for Host content type
     *
     * @param dc DotConnect Object
     * @return the inode of the Host content Type
     */
    private String findHostContentTypeInode(DotConnect dc) throws DotDataException {
        dc.setSQL(GET_HOST_STRUCTURE_INODE_QUERY);
        List<Map<String, String>> host = dc.loadResults();

        return host.get(0).get("inode");
    }

    /**
     * Remove the virtual link table constraints
     */
    private void dropConstraints() throws DotDataException {

        Connection conn = null;
        try {
            conn = DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(true);

            List<String> tables = getTablesToDropConstraints();
            if (tables != null) {
                boolean executeDrop = true;
                logTaskProgress("==> Retrieving foreign keys [Drop objects? " + executeDrop + "]");
                getForeingKeys(conn, tables, executeDrop);
                logTaskProgress("==> Retrieving primary keys [Drop objects? " + executeDrop + "]");
                getPrimaryKey(conn, tables, executeDrop);
                logTaskProgress("==> Retrieving indexes [Drop objects? " + executeDrop + "]");
                getIndexes(conn, tables, executeDrop);
                logTaskProgress(
                        "==> Retrieving default constraints [Drop objects? " + executeDrop + "]");
                getDefaultConstraints(conn, tables, executeDrop);
                logTaskProgress(
                        "==> Retrieving check constraints [Drop objects? " + executeDrop + "]");
                getCheckConstraints(conn, tables, executeDrop);
                if (DbConnectionFactory.isMsSql()) {
                    // for mssql we pass again as we might have index dependencies
                    getPrimaryKey(conn, tables, executeDrop);
                }
            }
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.error(this,
                        String.format(ERROR_MESSAGE,
                                ex.getMessage()), ex);
            }
        }
    }

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public String getPostgresScript() {
        return null;
    }

    @Override
    public String getMySQLScript() {
        return null;
    }

    @Override
    public String getOracleScript() {
        return null;
    }

    @Override
    public String getMSSQLScript() {
        return null;
    }

    /**
     * Remove the virtualLink constraint
     *
     * @return List of table to remove constraints
     */
    @Override
    protected List<String> getTablesToDropConstraints() {
        final List<String> tableList = new ArrayList<>();
        tableList.add("virtual_link");
        return tableList;
    }

}
