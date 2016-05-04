package com.dotcms.integritycheckers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.repackage.com.csvreader.CsvWriter;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageCache;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Page (legacy html pages and contentlet page) integrity checker
 * implementation.
 * 
 * @author Rogelio Blanco
 * @version 1.0
 * @since 06-10-2015
 * 
 */
public class HtmlPageIntegrityChecker extends AbstractIntegrityChecker {

    @Override
    public final IntegrityType getIntegrityType() {
        // Legacy HTML pages and contentlet pages share the same result table
        return IntegrityType.HTMLPAGES;
    }

    /**
     * Creates CSV file with legacy HTML Pages information from End Point
     * server.
     *
     * @param outputFile
     *            - The file containing the list of pages.
     * @return a {@link File} with the page information.
     * @throws DotDataException
     *             An error occurred when querying the database.
     * @throws IOException
     *             An error occurred when writing to the file.
     */
    @Override
    public File generateCSVFile(final String outputPath) throws DotDataException, IOException {
        final String outputFile = outputPath + File.separator
                + getIntegrityType().getDataToCheckCSVName();

        File csvFile = null;
        CsvWriter writer = null;

        try {
            csvFile = new File(outputFile);
            writer = new CsvWriter(new FileWriter(csvFile, true), '|');

            // Query the legacy pages
            Connection conn = DbConnectionFactory.getConnection();
            try (PreparedStatement statement = conn
                    .prepareStatement("select distinct hvi.working_inode, hvi.live_inode, h.identifier, i.parent_path, i.asset_name, i.host_inode "
                            + "from htmlpage h join identifier i on h.identifier = i.id join htmlpage_version_info hvi on i.id = hvi.identifier")) {
                try (ResultSet rs = statement.executeQuery()) {
                    int count = 0;

                    while (rs.next()) {
                        writer.write(rs.getString("working_inode"));
                        writer.write(rs.getString("live_inode"));
                        writer.write(rs.getString("identifier"));
                        writer.write(rs.getString("parent_path"));
                        writer.write(rs.getString("asset_name"));
                        writer.write(rs.getString("host_inode"));
                        // Pass meaningless value for legacy pages
                        writer.write("0");

                        writer.endRecord();
                        count++;

                        if (count == 1000) {
                            writer.flush();
                            count = 0;
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DotDataException(e.getMessage(), e);
            }
        } finally {
            // Close writer
            if (writer != null) {
                writer.close();
            }
        }

        return csvFile;
    }

    /**
     * Checks possible conflicts with HTMLPages.
     *
     * @param endpointId
     *            Information of the Server you want to examine.
     * @return
     * @throws Exception
     */
    @Override
    public boolean generateIntegrityResults(final String endpointId) throws Exception {
        try {
            DotConnect dc = new DotConnect();

            // Create a temporary table and insert all the records coming from
            // the CSV file.
            createContentletTemporaryTable(endpointId);

            // Load data to the temporary table
            checkPages(endpointId, IntegrityType.HTMLPAGES);
            checkPages(endpointId, IntegrityType.CONTENTPAGES);

            // Legacy HTML pages and contentlet pages share the same result
            // table
            dc.setSQL("select * from " + getIntegrityType().getResultsTableName());
            List<Map<String, Object>> results = dc.loadObjectResults();
            return !results.isEmpty();
        } catch (Exception e) {
            throw new Exception("Error running the HTML Pages Integrity Check", e);
        }
    }

    /**
     * Replace Identifier with same Identifier from the other server. For the
     * new Content Pages, it is necessary to take the following 2 situations
     * into consideration:
     * <ol>
     * <li>If the page to fix only has one specified language, fix all existing
     * versions.</li>
     * <li>If the page to fix only has more than one language, fix the versions
     * <b>ONE BY ONE</b>. Otherwise, the second time the next language is found,
     * the old identifier will not be present anymore since it was already
     * changed, which will cause an issue.</li>
     * </ol>
     *
     * @param serverId
     *            - The ID of the endpoint where the data will be fixed.
     * @throws DotDataException
     *             An error occurred when interacting with the database.
     * @throws DotSecurityException
     *             The current user does not have permission to perform this
     *             ation.
     */
    @Override
    public void executeFix(final String endpointId) throws DotDataException, DotSecurityException {
        DotConnect dc = new DotConnect();
        // Get the information of the IR.
        dc.setSQL("SELECT " + getIntegrityType().getFirstDisplayColumnLabel() + ", local_identifier, remote_identifier, local_working_inode, remote_working_inode, local_live_inode, remote_live_inode, language_id FROM "
                + getIntegrityType().getResultsTableName() + " WHERE endpoint_id = ?");
        dc.addParam(endpointId);
        List<Map<String, Object>> results = dc.loadObjectResults();
        Map<String, Integer> versionCount = new HashMap<String, Integer>();
        for (Map<String, Object> result : results) {
            String oldIdentifier = (String) result.get("local_identifier");
            
            Integer counter = versionCount.get(oldIdentifier);
            if(counter == null) {
                versionCount.put(oldIdentifier, 1);
            } else {
                versionCount.put(oldIdentifier, counter + 1);
            }
        }
        for (Map<String, Object> result : results) {
            String oldIdentifier = (String) result.get("local_identifier");
            Identifier identifier = APILocator.getIdentifierAPI().find(oldIdentifier);
            if ("htmlpage".equals(identifier.getAssetType())) {
                fixLegacyPageConflicts(result);
            } else {
                int counter = versionCount.get(oldIdentifier);
                boolean fixAllVersions = counter == 1 ? true : false;
                fixContentPageConflicts(result, fixAllVersions);
                if (!fixAllVersions) {
                    // Decrease version counter if greater than 1
                    versionCount.put(oldIdentifier, counter - 1);
                }
            }
        }

        discardConflicts(endpointId);
    }

    /**
     * Checks the existence of conflicts with both the legacy HTML pages and
     * Content Pages.
     *
     * @param endpointId
     *            - The ID of the endpoint where conflicts will be detected.
     * @param type
     *            - The type of HTML asset to check: Legacy Page, or Content
     *            Page.
     * @throws IOException
     *             An error occurred when reading the file containing the page
     *             data.
     * @throws SQLException
     *             There's a syntax error in a SQL statement.
     * @throws DotDataException
     *             An error occurred when interacting with the database.
     */
    private void checkPages(final String endpointId, IntegrityType type) throws IOException,
            SQLException, DotDataException {
        CsvReader htmlpages = new CsvReader(ConfigUtils.getIntegrityPath() + File.separator
                + endpointId + File.separator + type.getDataToCheckCSVName(), '|',
                Charset.forName("UTF-8"));

        DotConnect dc = new DotConnect();
        String tempTableName = getTempTableName(endpointId);

        final String INSERT_TEMP_TABLE = "insert into " + tempTableName + " values(?,?,?,?,?,?,?)";
        while (htmlpages.readRecord()) {

            String workingInode = null;
            String liveInode = null;

            workingInode = htmlpages.get(0);
            liveInode = htmlpages.get(1);

            String htmlPageIdentifier = htmlpages.get(2);
            String htmlPageParentPath = htmlpages.get(3);
            String htmlPageAssetName = htmlpages.get(4);
            String htmlPageHostIdentifier = htmlpages.get(5);
            String htmlPageLanguage = htmlpages.get(6);
            dc.setSQL(INSERT_TEMP_TABLE);

            dc.addParam(workingInode);
            dc.addParam(liveInode);
            dc.addParam(htmlPageIdentifier);
            dc.addParam(htmlPageParentPath);
            dc.addParam(htmlPageAssetName);
            dc.addParam(htmlPageHostIdentifier);
            dc.addParam(new Long(htmlPageLanguage));

            dc.loadResult();
        }
        htmlpages.close();

        // Compare the data from the CSV to the local database data and see if
        // we have conflicts.
        String selectSQL = null;
        if (type.equals(IntegrityType.HTMLPAGES)) {
            // Query the legacy pages
            selectSQL = "select lh.page_url as " + getIntegrityType().getFirstDisplayColumnLabel() + ", lh.inode as local_inode, "
                    + "ri.working_inode as remote_inode, " + "li.id as local_identifier, "
                    + "ri.identifier as remote_identifier, ri.language_id "
                    + "from identifier as li " + "join htmlpage as lh "
                    + "on lh.identifier = li.id " + "and li.asset_type = 'htmlpage' " + "join "
                    + tempTableName + " as ri " + "on li.asset_name = ri.asset_name "
                    + "and li.parent_path = ri.parent_path "
                    + "and li.host_inode = ri.host_identifier " + "and li.id <> ri.identifier";
        } else {
            // Query the new content pages
            selectSQL = "SELECT DISTINCT "
                    + "li.asset_name as " + getIntegrityType().getFirstDisplayColumnLabel() + ", lcvi.working_inode as local_working_inode, lcvi.live_inode as local_live_inode, "
                    + "t.working_inode as remote_working_inode, t.live_inode as remote_live_inode, "
                    + "lc.identifier as local_identifier, t.identifier as remote_identifier, "
                    + "lc.language_id "
                    + "FROM "
                    + "identifier li "
                    + "INNER JOIN contentlet lc ON (lc.identifier = li.id and li.asset_type = 'contentlet') "
                    + "INNER JOIN contentlet_version_info lcvi ON (lc.identifier = lcvi.identifier) "
                    + "INNER JOIN structure ls ON (lc.structure_inode = ls.inode and ls.structuretype = 5) "
                    + "INNER JOIN " + tempTableName
                    + " t ON (li.asset_name = t.asset_name AND li.parent_path = t.parent_path "
                    + "AND li.host_inode = host_identifier AND lc.identifier <> t.identifier "
                    + "AND lc.language_id = t.language_id)";
        }

        if (DbConnectionFactory.isOracle()) {
            selectSQL = selectSQL.replaceAll(" as ", " ");
        }

        dc.setSQL(selectSQL);

        List<Map<String, Object>> results = dc.loadObjectResults();

        // If we have conflicts, lets create a table out of them.
        if (!results.isEmpty()) {
            String fullHtmlPage = " li.parent_path || li.asset_name ";

            if (DbConnectionFactory.isMySql()) {
                fullHtmlPage = " concat(li.parent_path,li.asset_name) ";
            } else if (DbConnectionFactory.isMsSql()) {
                fullHtmlPage = " li.parent_path + li.asset_name ";
            }

            String insertSQL = null;
            if (type.equals(IntegrityType.HTMLPAGES)) {
                // Query the legacy pages
                insertSQL = "insert into "
                        // Legacy HTML pages and contentlet pages share the same
                        // result table
                        + getIntegrityType().getResultsTableName()
                        + " (" + getIntegrityType().getFirstDisplayColumnLabel() + ", local_working_inode, remote_working_inode, local_identifier, remote_identifier, endpoint_id, language_id) "
                        + " select " + fullHtmlPage + " as " + getIntegrityType().getFirstDisplayColumnLabel() + ", "
                        + "lh.inode as local_inode, " + "ri.inode as remote_inode, "
                        + "li.id as local_identifier, " + "ri.identifier as remote_identifier, '" 
                        + endpointId + "', ri.language_id " + "from identifier as li "
                        + "join htmlpage as lh " + "on lh.identifier = li.id "
                        + "and li.asset_type = 'htmlpage' " + "join " + tempTableName + " as ri "
                        + "on li.asset_name = ri.asset_name "
                        + "and li.parent_path = ri.parent_path "
                        + "and li.host_inode = ri.host_identifier " + "and li.id <> ri.identifier";
            } else {
                // Query the new content pages
                insertSQL = "insert into "
                        // Legacy HTML pages and contentlet pages share the same
                        // result table
                        + getIntegrityType().getResultsTableName()
                        + " select DISTINCT "
                        + fullHtmlPage
                        + " as " + getIntegrityType().getFirstDisplayColumnLabel() + ", "
                        + "lcvi.working_inode as local_working_inode, lcvi.live_inode as local_live_inode, "
                        + "t.working_inode as remote_working_inode, t.live_inode as remote_live_inode, "
                        + "lc.identifier as local_identifier, t.identifier as remote_identifier, '"
                        + endpointId
                        + "', t.language_id as language_id "
                        + "FROM identifier li "
                        + "INNER JOIN contentlet lc ON (lc.identifier = li.id and li.asset_type = 'contentlet') "
                        + "INNER JOIN contentlet_version_info lcvi ON (lc.identifier = lcvi.identifier and lc.language_id = lcvi.lang) "
                        + "INNER JOIN structure ls ON (lc.structure_inode = ls.inode and ls.structuretype = 5) "
                        + "INNER JOIN " + tempTableName
                        + " t ON (li.asset_name = t.asset_name AND li.parent_path = t.parent_path "
                        + "AND li.host_inode = host_identifier AND lc.identifier <> t.identifier "
                        + "AND lc.language_id = t.language_id)";
            }

            if (DbConnectionFactory.isOracle()) {
                insertSQL = insertSQL.replaceAll(" as ", " ");
            }

            dc.executeStatement(insertSQL);
        }
    }

    /**
     * Directly updates the information of a given HTML Page - i.e., the legacy
     * {@link HTMLPage} - to resolve the conflict (two pages with same path but
     * different identifier) found in the receiver server before a push publish
     * is triggered.
     * <p>
     * This method is the same for solving both local and remote conflicts. The
     * only difference is in what server (either the sender or the receiver)
     * this method is called.
     * </p>
     *
     * @param pageData
     *            - A {@link Map} with the page information that was generated
     *            when the conflict was detected.
     * @throws DotDataException
     *             An error occurred when interacting with the database.
     */
    private void fixLegacyPageConflicts(Map<String, Object> pageData) throws DotDataException {
        HTMLPageCache htmlPageCache = CacheLocator.getHTMLPageCache();
        DotConnect dc = new DotConnect();
        String oldHtmlPageIdentifier = (String) pageData.get("local_identifier");
        String newHtmlPageIdentifier = (String) pageData.get("remote_identifier");
        String assetName = (String) pageData.get(getIntegrityType().getFirstDisplayColumnLabel());
        String localInode = (String) pageData.get("local_working_inode");
        // We need only the last part of the url, not the whole path.
        String[] assetNamebits = assetName.split("/");
        assetName = assetNamebits[assetNamebits.length - 1];
        htmlPageCache.remove(oldHtmlPageIdentifier);
        CacheLocator.getIdentifierCache().removeFromCacheByInode(localInode);
        // Fixing by SQL queries
        dc.setSQL("INSERT INTO identifier(id, parent_path, asset_name, host_inode, asset_type, syspublish_date, sysexpire_date) "
                + "SELECT ? , parent_path, 'TEMP_ASSET_NAME', host_inode, asset_type, syspublish_date, sysexpire_date "
                + "FROM identifier WHERE id = ?");
        dc.addParam(newHtmlPageIdentifier);
        dc.addParam(oldHtmlPageIdentifier);
        dc.loadResult();
        dc.setSQL("UPDATE htmlpage SET identifier = ? WHERE identifier = ?");
        dc.addParam(newHtmlPageIdentifier);
        dc.addParam(oldHtmlPageIdentifier);
        dc.loadResult();
        dc.setSQL("UPDATE htmlpage_version_info SET identifier = ? WHERE identifier = ?");
        dc.addParam(newHtmlPageIdentifier);
        dc.addParam(oldHtmlPageIdentifier);
        dc.loadResult();
        dc.setSQL("DELETE FROM identifier WHERE id = ?");
        dc.addParam(oldHtmlPageIdentifier);
        dc.loadResult();
        dc.setSQL("UPDATE identifier SET asset_name = ? WHERE id = ?");
        dc.addParam(assetName);
        dc.addParam(newHtmlPageIdentifier);
        dc.loadResult();
        dc.setSQL("UPDATE multi_tree SET parent1 = ? WHERE parent1 = ?");
        dc.addParam(newHtmlPageIdentifier);
        dc.addParam(oldHtmlPageIdentifier);
        dc.loadResult();
    }

    /**
     * Directly updates the information of a given Content Page - i.e., the new
     * {@link IHTMLPage} - to resolve the conflict (two pages with same path but
     * different identifier) found in the receiver server before a push publish
     * is triggered. This new HTML page is a specialized form of the
     * {@link Contentlet} class.
     * <p>
     * This method is the same for solving both local and remote conflicts. The
     * only difference is in what server (either the sender or the receiver)
     * this method is called and the distribution of data fields, which is
     * handled by a previous method. Bearing this in mind, the conflict
     * resolution process performs the following plain SQL queries (the
     * execution order is very important to avoid foreign key conflicts):
     * <ol>
     * <li>Create the <code>Inode</code> and <code>Identifier</code> records,
     * which <b>MUST EXIST</b> before a contentlet (content page) can be
     * created. Initially, the <code>asset_Name</code> of the new Identifier
     * will have a temporary name.</li>
     * <li>Create the new <code>Contentlet</code> and
     * <code>Contentlet_Version_Info</code> records <b>WITHOUT DELETING</b> the
     * old page records. Otherwise, exceptions will be thrown regarding foreign
     * key constraints with several other tables.</li>
     * <li>Delete the old <code>Contentlet_Version_Info</code> and
     * <code>Contentlet</code> records.</li>
     * <li>Update all the existing versions of the page in the
     * <code>Contentlet</code> table so they point to the new identifier.</li>
     * <li>Delete the old <code>Identifier</code> and <code>Inode</code>
     * records.</li>
     * <li>Update the <code>Identifier</code> record with the correct
     * <code>asset_name</code>.</li>
     * <li>Update the <code>Multi_Tree</code> records so that the change in the
     * identifier does not cause the page content to be missing.</li>
     * <li>Remove the old page entries from <code>Contentlet</code> and
     * <code>Identifier</code> cache. This also includes removing the entries
     * for any existing versions of the page so that older versions of the page
     * can be brought back under the new identifier without any errors.</li>
     * </ol>
     * </p>
     *
     * @param pageData
     *            - A {@link Map} with the page information that was captured
     *            when the conflict was detected.
     * @param fixAllVersions
     *            - If <code>true</code>, all existing versions of the page will
     *            be updated in order to keep data consistency. Otherwise, ONLY
     *            the specified page and language will be updated.
     * @throws DotDataException
     *             An error occurred when interacting with the database.
     * @throws DotSecurityException
     *             The current user does not have permission to perform this
     *             action.
     */
    private void fixContentPageConflicts(Map<String, Object> pageData, boolean fixAllVersions)
            throws DotDataException, DotSecurityException {
        DotConnect dc = new DotConnect();
        String oldHtmlPageIdentifier = (String) pageData.get("local_identifier");
        String newHtmlPageIdentifier = (String) pageData.get("remote_identifier");
        String assetName = (String) pageData.get(getIntegrityType().getFirstDisplayColumnLabel());
        String localWorkingInode = (String) pageData.get("local_working_inode");
        String localLiveInode = (String) pageData.get("local_live_inode");
        String remoteWorkingInode = (String) pageData.get("remote_working_inode");
        String remoteLiveInode = (String) pageData.get("remote_live_inode");

        Long languageId;
        if (DbConnectionFactory.isOracle()) {
            BigDecimal lang = (BigDecimal) pageData.get("language_id");
            languageId = new Long(lang.toPlainString());
        } else {
            languageId = (Long) pageData.get("language_id");
        }

        String[] assetUrl = assetName.split("/");
        assetName = assetUrl[assetUrl.length - 1];
        ContentletAPI contentletAPI = APILocator.getContentletAPI();
        ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();
        User systemUser = APILocator.getUserAPI().getSystemUser();
        Contentlet existingWorkingContentPage = contentletAPI.find(localWorkingInode, systemUser,
                false);
        Contentlet existingLiveContentPage = null;

        try {
            existingLiveContentPage = contentletAPI.find(localLiveInode, systemUser, false);
        } catch (DotHibernateException e) { /* No Live Version */
        }

        dc.setSQL("SELECT id FROM identifier WHERE id = ?");
        dc.addParam(newHtmlPageIdentifier);
        List<Map<String, Object>> results = dc.loadObjectResults();
        // If not existing, add the new Identifier with a temporary asset name
        if (results == null || results.size() == 0) {
            dc.setSQL("INSERT INTO identifier(id, parent_path, asset_name, host_inode, asset_type, syspublish_date, sysexpire_date) "
                    + "SELECT ? , parent_path, 'TEMP_CONTENTPAGE_NAME', host_inode, asset_type, syspublish_date, sysexpire_date "
                    + "FROM identifier WHERE id = ?");
            dc.addParam(newHtmlPageIdentifier);
            dc.addParam(oldHtmlPageIdentifier);
            dc.loadResult();
        }
        // Insert the new Inodes records so it can be used in the contentlet
        dc.setSQL("INSERT INTO inode(inode, owner, idate, type) " + "SELECT ?, owner, idate, type "
                + "FROM inode i WHERE i.inode = ?");
        dc.addParam(remoteWorkingInode);
        dc.addParam(localWorkingInode);
        dc.loadResult();

        if (!remoteWorkingInode.equals(remoteLiveInode) && UtilMethods.isSet(remoteLiveInode)
                && UtilMethods.isSet(localLiveInode)) {
            dc.setSQL("INSERT INTO inode(inode, owner, idate, type) "
                    + "SELECT ?, owner, idate, type " + "FROM inode i WHERE i.inode = ?");
            dc.addParam(remoteLiveInode);
            dc.addParam(localLiveInode);
            dc.loadResult();
        }

        // Insert the new working Contentlet record with the new Inode
        String contentletQuery = "INSERT INTO contentlet(inode, show_on_menu, title, mod_date, mod_user, sort_order, friendly_name, structure_inode, last_review, next_review, review_interval, disabled_wysiwyg, identifier, language_id, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12, date13, date14, date15, date16, date17, date18, date19, date20, date21, date22, date23, date24, date25, text1, text2, text3, text4, text5, text6, text7, text8, text9, text10, text11, text12, text13, text14, text15, text16, text17, text18, text19, text20, text21, text22, text23, text24, text25, text_area1, text_area2, text_area3, text_area4, text_area5, text_area6, text_area7, text_area8, text_area9, text_area10, text_area11, text_area12, text_area13, text_area14, text_area15, text_area16, text_area17, text_area18, text_area19, text_area20, text_area21, text_area22, text_area23, text_area24, text_area25, integer1, integer2, integer3, integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14, integer15, integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, \"float1\", \"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\", \"float8\", \"float9\", \"float10\", \"float11\", \"float12\", \"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\", \"float19\", \"float20\", \"float21\", \"float22\", \"float23\", \"float24\", \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13, bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25) "
                + "SELECT ?, show_on_menu, title, mod_date, mod_user, sort_order, friendly_name, structure_inode, last_review, next_review, review_interval, disabled_wysiwyg, ?, ?, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12, date13, date14, date15, date16, date17, date18, date19, date20, date21, date22, date23, date24, date25, text1, text2, text3, text4, text5, text6, text7, text8, text9, text10, text11, text12, text13, text14, text15, text16, text17, text18, text19, text20, text21, text22, text23, text24, text25, text_area1, text_area2, text_area3, text_area4, text_area5, text_area6, text_area7, text_area8, text_area9, text_area10, text_area11, text_area12, text_area13, text_area14, text_area15, text_area16, text_area17, text_area18, text_area19, text_area20, text_area21, text_area22, text_area23, text_area24, text_area25, integer1, integer2, integer3, integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14, integer15, integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, \"float1\", \"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\", \"float8\", \"float9\", \"float10\", \"float11\", \"float12\", \"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\", \"float19\", \"float20\", \"float21\", \"float22\", \"float23\", \"float24\", \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13, bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25 "
                + "FROM contentlet c INNER JOIN contentlet_version_info cvi on (c.inode = cvi.working_inode) WHERE c.identifier = ? and c.language_id = ?";
        if (DbConnectionFactory.isMySql()) {
            // Use correct escape char when using reserved words as column names
            contentletQuery = contentletQuery.replaceAll("\"", "`");
        }
        dc.setSQL(contentletQuery);
        dc.addParam(remoteWorkingInode);
        dc.addParam(newHtmlPageIdentifier);
        dc.addParam(languageId);
        dc.addParam(oldHtmlPageIdentifier);
        dc.addParam(languageId);
        dc.loadResult();

        if (!remoteWorkingInode.equals(remoteLiveInode) && UtilMethods.isSet(remoteLiveInode)
                && UtilMethods.isSet(localLiveInode)) {
            contentletQuery = "INSERT INTO contentlet(inode, show_on_menu, title, mod_date, mod_user, sort_order, friendly_name, structure_inode, last_review, next_review, review_interval, disabled_wysiwyg, identifier, language_id, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12, date13, date14, date15, date16, date17, date18, date19, date20, date21, date22, date23, date24, date25, text1, text2, text3, text4, text5, text6, text7, text8, text9, text10, text11, text12, text13, text14, text15, text16, text17, text18, text19, text20, text21, text22, text23, text24, text25, text_area1, text_area2, text_area3, text_area4, text_area5, text_area6, text_area7, text_area8, text_area9, text_area10, text_area11, text_area12, text_area13, text_area14, text_area15, text_area16, text_area17, text_area18, text_area19, text_area20, text_area21, text_area22, text_area23, text_area24, text_area25, integer1, integer2, integer3, integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14, integer15, integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, \"float1\", \"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\", \"float8\", \"float9\", \"float10\", \"float11\", \"float12\", \"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\", \"float19\", \"float20\", \"float21\", \"float22\", \"float23\", \"float24\", \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13, bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25) "
                    + "SELECT ?, show_on_menu, title, mod_date, mod_user, sort_order, friendly_name, structure_inode, last_review, next_review, review_interval, disabled_wysiwyg, ?, ?, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12, date13, date14, date15, date16, date17, date18, date19, date20, date21, date22, date23, date24, date25, text1, text2, text3, text4, text5, text6, text7, text8, text9, text10, text11, text12, text13, text14, text15, text16, text17, text18, text19, text20, text21, text22, text23, text24, text25, text_area1, text_area2, text_area3, text_area4, text_area5, text_area6, text_area7, text_area8, text_area9, text_area10, text_area11, text_area12, text_area13, text_area14, text_area15, text_area16, text_area17, text_area18, text_area19, text_area20, text_area21, text_area22, text_area23, text_area24, text_area25, integer1, integer2, integer3, integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14, integer15, integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, \"float1\", \"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\", \"float8\", \"float9\", \"float10\", \"float11\", \"float12\", \"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\", \"float19\", \"float20\", \"float21\", \"float22\", \"float23\", \"float24\", \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13, bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25 "
                    + "FROM contentlet c INNER JOIN contentlet_version_info cvi on (c.inode = cvi.live_inode) WHERE c.identifier = ? and c.language_id = ?";

            dc.setSQL(contentletQuery);
            dc.addParam(remoteLiveInode);
            dc.addParam(newHtmlPageIdentifier);
            dc.addParam(languageId);
            dc.addParam(oldHtmlPageIdentifier);
            dc.addParam(languageId);
            dc.loadResult();
        }

        // Insert the new Contentlet_version_info record with the new Inode

        if (UtilMethods.isSet(remoteLiveInode) && UtilMethods.isSet(localLiveInode)) {
            dc.setSQL("INSERT INTO contentlet_version_info(identifier, lang, working_inode, live_inode, deleted, locked_by, locked_on, version_ts) "
                    + "SELECT ?, ?, ?, ?, deleted, locked_by, locked_on, version_ts "
                    + "FROM contentlet_version_info WHERE identifier = ? AND working_inode = ? AND lang = ?");
            dc.addParam(newHtmlPageIdentifier);
            dc.addParam(languageId);
            dc.addParam(remoteWorkingInode);
            dc.addParam(remoteLiveInode);
            dc.addParam(oldHtmlPageIdentifier);
            dc.addParam(localWorkingInode);
            dc.addParam(languageId);
            dc.loadResult();
        } else {
            dc.setSQL("INSERT INTO contentlet_version_info(identifier, lang, working_inode, live_inode, deleted, locked_by, locked_on, version_ts) "
                    + "SELECT ?, ?, ?, live_inode, deleted, locked_by, locked_on, version_ts "
                    + "FROM contentlet_version_info WHERE identifier = ? AND working_inode = ? AND lang = ?");
            dc.addParam(newHtmlPageIdentifier);
            dc.addParam(languageId);
            dc.addParam(remoteWorkingInode);
            dc.addParam(oldHtmlPageIdentifier);
            dc.addParam(localWorkingInode);
            dc.addParam(languageId);
            dc.loadResult();
        }

        // Remove the live_inode references from Contentlet_version_info
        dc.setSQL("DELETE FROM contentlet_version_info WHERE identifier = ? AND lang = ? AND working_inode = ?");
        dc.addParam(oldHtmlPageIdentifier);
        dc.addParam(languageId);
        dc.addParam(localWorkingInode);
        dc.loadResult();

        // Remove the conflicting version of the Contentlet record
        dc.setSQL("DELETE FROM contentlet WHERE identifier = ? AND inode = ? AND language_id = ?");
        dc.addParam(oldHtmlPageIdentifier);
        dc.addParam(localWorkingInode);
        dc.addParam(languageId);
        dc.loadResult();

        if (UtilMethods.isSet(localLiveInode) && UtilMethods.isSet(remoteLiveInode)
                && !localLiveInode.equals(localWorkingInode)) {
            // Remove the conflicting version of the Contentlet record
            dc.setSQL("DELETE FROM contentlet WHERE identifier = ? AND inode = ? AND language_id = ?");
            dc.addParam(oldHtmlPageIdentifier);
            dc.addParam(localLiveInode);
            dc.addParam(languageId);
            dc.loadResult();
        }

        // If fixing all versions, or last version of the same Identifier
        if (fixAllVersions) {
            // Update other Contentlet languages with new Identifier
            dc.setSQL("UPDATE contentlet SET identifier = ? WHERE identifier = ?");
            dc.addParam(newHtmlPageIdentifier);
            dc.addParam(oldHtmlPageIdentifier);
            dc.loadResult();
            // Update previous version of the Contentlet_version_info with new
            // Identifier
            dc.setSQL("UPDATE contentlet_version_info SET identifier = ? WHERE identifier = ?");
            dc.addParam(newHtmlPageIdentifier);
            dc.addParam(oldHtmlPageIdentifier);
            dc.loadResult();
            // Remove the old Identifier record
            dc.setSQL("DELETE FROM identifier WHERE id = ?");
            dc.addParam(oldHtmlPageIdentifier);
            dc.loadResult();
            // Update the Identifier with the correct asset name
            dc.setSQL("UPDATE identifier SET asset_name = ? WHERE id = ?");
            dc.addParam(assetName);
            dc.addParam(newHtmlPageIdentifier);
            dc.loadResult();
            // Update the content references in the page with the new Identifier
            dc.setSQL("UPDATE multi_tree SET parent1 = ? WHERE parent1 = ?");
            dc.addParam(newHtmlPageIdentifier);
            dc.addParam(oldHtmlPageIdentifier);
            dc.loadResult();
        } else {
            // Update other Contentlet languages with new Identifier
            dc.setSQL("UPDATE contentlet SET identifier = ? WHERE identifier = ? AND language_id = ?");
            dc.addParam(newHtmlPageIdentifier);
            dc.addParam(oldHtmlPageIdentifier);
            dc.addParam(languageId);
            dc.loadResult();
            // Update previous version of the Contentlet_version_info with new
            // Identifier
            dc.setSQL("UPDATE contentlet_version_info SET identifier = ? WHERE identifier = ? AND lang = ?");
            dc.addParam(newHtmlPageIdentifier);
            dc.addParam(oldHtmlPageIdentifier);
            dc.addParam(languageId);
        }
        // Remove the old Inode record
        dc.setSQL("DELETE FROM inode WHERE inode = ?");
        dc.addParam(localWorkingInode);
        dc.loadResult();

        if (UtilMethods.isSet(localLiveInode) && UtilMethods.isSet(remoteLiveInode)
                && !localLiveInode.equals(localWorkingInode)) {
            // Remove the old Inode record
            dc.setSQL("DELETE FROM inode WHERE inode = ?");
            dc.addParam(localLiveInode);
            dc.loadResult();
        }

        // Add a new Lucene index for the updated page
        Contentlet newWorkingContentPage = new Contentlet();
        newWorkingContentPage.setStructureInode(existingWorkingContentPage.getStructureInode());
        contentletAPI.copyProperties(newWorkingContentPage, existingWorkingContentPage.getMap());
        newWorkingContentPage.setIdentifier(newHtmlPageIdentifier);
        newWorkingContentPage.setInode(remoteWorkingInode);
        indexAPI.addContentToIndex(newWorkingContentPage);

        if (UtilMethods.isSet(localLiveInode) && !localLiveInode.equals(localWorkingInode)) {
            Contentlet newLiveContentPage = new Contentlet();
            newLiveContentPage.setStructureInode(existingLiveContentPage.getStructureInode());
            contentletAPI.copyProperties(newLiveContentPage, existingLiveContentPage.getMap());
            newLiveContentPage.setIdentifier(newHtmlPageIdentifier);
            newLiveContentPage.setInode(remoteLiveInode);
            indexAPI.addContentToIndex(newLiveContentPage);
        }

        // Remove the Lucene index for the old page
        indexAPI.removeContentFromIndex(existingWorkingContentPage);

        if (UtilMethods.isSet(existingLiveContentPage)
                && !existingWorkingContentPage.getInode()
                        .equals(existingLiveContentPage.getInode())) {
            indexAPI.removeContentFromIndex(existingLiveContentPage);
        }

        // Clear cache entries of ALL versions of the Contentlet too
        CacheLocator.getContentletCache().remove(localWorkingInode);

        if (UtilMethods.isSet(localLiveInode) && !localWorkingInode.equals(localLiveInode)) {
            CacheLocator.getContentletCache().remove(localLiveInode);
        }

        CacheLocator.getIdentifierCache().removeFromCacheByInode(localWorkingInode);

        if (UtilMethods.isSet(localLiveInode) && !localWorkingInode.equals(localLiveInode)) {
            CacheLocator.getIdentifierCache().removeFromCacheByInode(localLiveInode);
        }
        // Refresh all or only language-specific versions of the page
        if (fixAllVersions) {
            dc.setSQL("SELECT inode FROM contentlet WHERE identifier = ?");
            dc.addParam(newHtmlPageIdentifier);
        } else {
            dc.setSQL("SELECT inode FROM contentlet WHERE identifier = ? AND language_id = ?");
            dc.addParam(newHtmlPageIdentifier);
            dc.addParam(languageId);
        }
        List<Map<String, Object>> versions = dc.loadObjectResults();
        for (Map<String, Object> result : versions) {
            String historyInode = (String) result.get("inode");
            CacheLocator.getContentletCache().remove(historyInode);
        }
    }
}
