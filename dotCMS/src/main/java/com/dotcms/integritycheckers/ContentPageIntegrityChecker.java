package com.dotcms.integritycheckers;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Contentlet page integrity checker implementation.
 * <p>
 * Note: Only generateCSVFile method is implemented because it has a different
 * output than {@link HtmlPageIntegrityChecker} class. The methods that are not
 * implemented please use them from {@link HtmlPageIntegrityChecker} class.
 * </p>
 * <p>
 * This class is a workaround because we need to support legacy html pages and
 * the new implementation using contentlets type "page"
 * </p>
 * 
 * @author Rogelio Blanco
 * @version 1.0
 * @since 06-10-2015
 * 
 */
public class ContentPageIntegrityChecker extends AbstractIntegrityChecker {

    @Override
    public final IntegrityType getIntegrityType() {
        // Legacy HTML pages and contentlet pages share the same result table
        return IntegrityType.HTMLPAGES;
    }

    /**
     * Creates CSV file for contenlet HTML Pages information from End Point
     * server.
     * <p>
     * NOTE: This method is required because we need to generate a different
     * .csv file for content pages
     * </p>
     *
     * @param outputPath
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

        return generateContentletsCSVFile(outputFile, Structure.STRUCTURE_TYPE_HTMLPAGE);
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

            // Legacy HTML pages and contentlet pages share the same result table
            return (Long) dc.getRecordCount(getIntegrityType().getResultsTableName(), "where endpoint_id = '"+ endpointId+ "'") > 0;
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
     *             action.
     */
    @Override
    public void executeFix(final String remoteIP) throws DotDataException, DotSecurityException {
        // Get the information of the IR.
        DotConnect dc = new DotConnect();
        dc.setSQL("SELECT " + getIntegrityType().getFirstDisplayColumnLabel() + ", local_identifier, remote_identifier, local_working_inode, remote_working_inode, local_live_inode, remote_live_inode, language_id FROM "
                + getIntegrityType().getResultsTableName() + " WHERE endpoint_id = ? ORDER BY " + getIntegrityType().getFirstDisplayColumnLabel());
        dc.addParam(remoteIP);
        List<Map<String, Object>> results = dc.loadObjectResults();

        // We need to load the map with the affected identifiers, inodes and languages
        Map<String, AffectedIdentifierInfoBucket> affectedIdentifiers = new HashMap<String, AffectedIdentifierInfoBucket>();
        for (Map<String, Object> result : results) {
            final String oldIdentifier = (String) result.get("local_identifier");
            final Long languageId = (Long) (DbConnectionFactory.isOracle() ? ((BigDecimal) result.get("language_id")).longValue() : (Long) result.get("language_id"));

            AffectedIdentifierInfoBucket identifierAffected = affectedIdentifiers.get(oldIdentifier);
            if(identifierAffected == null) {
                affectedIdentifiers.put(oldIdentifier, AffectedIdentifierInfoBucket.build((String) result.get("remote_identifier")).addAffectedLanguage(languageId, (String) result.get("remote_live_inode"), (String) result.get("remote_working_inode")));
            } else {
                identifierAffected.addAffectedLanguage(languageId, (String) result.get("remote_live_inode"), (String) result.get("remote_working_inode"));
            }
        }

        for (Map<String, Object> result : results) {
            final String oldIdentifier = (String) result.get("local_identifier");


                AffectedIdentifierInfoBucket affectedIdentifier = affectedIdentifiers.get(oldIdentifier);
                boolean fixAllVersions = affectedIdentifier.getCounter() == 1 ? true : false;

                // Verify and fix conflicts when page was moved
                if(affectedIdentifier.needsToCheckPossibleConflicts()) {
                    // We need to solved conflict once per affected identifier
                    checkAndFixIdentifierConflicts(affectedIdentifier);
                }

                fixContentPageConflicts(result, fixAllVersions);
                if (!fixAllVersions) {
                    // Decrease version counter if greater than 1
                    affectedIdentifier.decrementCounter();
                }
            
        }
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
        final String INSERT_TEMP_TABLE = "insert into " + tempTableName + " (working_inode, live_inode, identifier, full_path_lc, host_identifier, language_id) values(?,?,?,?,?,?)";
        boolean hasResultsToCheck = false;
        while (htmlpages.readRecord()) {
        	hasResultsToCheck = true;
            String htmlPageIdentifier = null;
            
			try {
				htmlPageIdentifier = getStringIfNotBlank("identifier",
						htmlpages.get(2));
				final String workingInode = getStringIfNotBlank(
						"working_inode", htmlpages.get(0));
				final String liveInode = htmlpages.get(1);
				final String htmlPageParentPath = getStringIfNotBlank(
						"parent_path", htmlpages.get(3));
				final String htmlPageAssetName = getStringIfNotBlank(
						"asset_name", htmlpages.get(4));
				final String htmlPageHostIdentifier = getStringIfNotBlank(
						"host_identifier", htmlpages.get(5));
				final String htmlPageLanguage = getStringIfNotBlank(
						"language_id", htmlpages.get(6));
				dc.setSQL(INSERT_TEMP_TABLE);
				dc.addParam(workingInode);
				dc.addParam(liveInode);
				dc.addParam(htmlPageIdentifier);
				dc.addParam((htmlPageParentPath + htmlPageAssetName).toLowerCase());
				dc.addParam(htmlPageHostIdentifier);
				dc.addParam(new Long(htmlPageLanguage));
				dc.loadResult();
			} catch (DotDataException e) {
				htmlpages.close();
				final String assetId = UtilMethods.isSet(htmlPageIdentifier) ? htmlPageIdentifier
						: "";
				throw new DotDataException(
						"An error occured when generating temp table for asset: "
								+ assetId, e);
			}
        }
        htmlpages.close();
        if (!hasResultsToCheck) {
        	return;
        }

        // Compare the data from the CSV to the local database data and see if
        // we have conflicts.
        String selectSQL = null;
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
                    + " t ON (li.full_path_lc = t.full_path_lc "
                    + "AND li.host_inode = host_identifier AND lc.identifier <> t.identifier "
                    + "AND lc.language_id = t.language_id)";
        

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
                // Query the new content pages
                insertSQL = "insert into "
                        // Legacy HTML pages and contentlet pages share the same
                        // result table
                        + getIntegrityType().getResultsTableName() 
                        + " (" + getIntegrityType().getFirstDisplayColumnLabel() + ", local_working_inode, local_live_inode, remote_working_inode, remote_live_inode, " 
                        + "local_identifier, remote_identifier, endpoint_id, language_id)"
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
						+ " t ON (li.full_path_lc = t.full_path_lc "
                        + "AND li.host_inode = host_identifier AND lc.identifier <> t.identifier "
                        + "AND lc.language_id = t.language_id)";
            

            if (DbConnectionFactory.isOracle()) {
                insertSQL = insertSQL.replaceAll(" as ", " ");
            }

            dc.executeStatement(insertSQL);
        }
    }

    /**
     * Before starting to fix the reported Integrity Checker conflicts, dotCMS
     * has to make sure that the new information is unique in the system. This
     * method checks that the new values that will be set for the Identifier,
     * the working Inode and the live Inode <b>DO NOT EXIST</b> in the database
     * of the specified end point.
     * <p>
     * If they do, it means that the page was previously fixed by the Integrity
     * Checker, and then moved to another folder or had its URL changed.
     * Therefore, the Integrity Checker didn't detect it and generates errors if
     * this situation is not solved before moving on.
     * </p>
     * 
     * @param affectedIdentifier
     *            The information that will be used to solve the integrity
     *            conflict. Its Identifier and Inodes might exist already, so
     *            the Integrity Checker must handle that first. This object
     *            contains the list of the languages and inodes affected, for
     *            the specific identifier
     * @throws DotDataException
     *             An error occurred when running one of the SQL queries.
     * @throws DotSecurityException
     *             The specified user does not have permissions to perform the
     *             selected action.
     */
    private void checkAndFixIdentifierConflicts(AffectedIdentifierInfoBucket affectedIdentifier) throws DotDataException, DotSecurityException {

        // Get the data of the existing page that has the identifier that MUST
		// be changed before fixing the reported conflict(s)
		DotConnect dc = new DotConnect();
		StringBuilder query = new StringBuilder();
		query.append(
				"SELECT cvi.working_inode, cvi.live_inode, cvi.lang, i.parent_path, i.asset_name ")
				.append("FROM contentlet_version_info cvi INNER JOIN identifier i ON cvi.identifier = i.id ")
				.append("WHERE cvi.identifier = ?");
		dc.setSQL(query.toString());
		dc.addParam(affectedIdentifier.getAffectedIdentifier());
		List<Map<String, Object>> conflictingPages = dc.loadObjectResults();
		if (conflictingPages != null && !conflictingPages.isEmpty()) {
			if (conflictingPages.size() > 1) {
			    // Add in-front of the list languages affected
			    conflictingPages = getOrderedConflictingPages(conflictingPages, affectedIdentifier);
			}
			// Fix previous conflicts before fixing the reported problems
			updateConflictingPageIds(conflictingPages, affectedIdentifier);
		}
	}

    /**
     * Utility method that places the main conflicting page version as the first
     * record based on the language ID, so that the integrity fix process takes
     * it first.
     * 
     * @param conflictingPages
     *            - The list of conflicting pages.
     * @param languageId
     *            - The language ID of the main conflicting page.
     * @return ordered list of conflicting pages
     *             The first elements of the list MUST be those with conflicts
     *  
     */
    private List<Map<String, Object>> getOrderedConflictingPages(List<Map<String, Object>> conflictingPages, AffectedIdentifierInfoBucket conflictIdentifierInfo) {
        LinkedList<Map<String, Object>> orderedConflictingPages = new LinkedList<Map<String, Object>>();

        for (Map<String, Object> pageInfo : conflictingPages) {
            final Long languageId = DbConnectionFactory.isOracle() ? ((BigDecimal) pageInfo.get("lang")).longValue() : (Long) pageInfo.get("lang");

            if(conflictIdentifierInfo.isLanguageAffected(languageId)) {
                // We need to add to the begging of the list those languages that has conflicts
                orderedConflictingPages.addFirst(pageInfo);
            } else {
                orderedConflictingPages.addLast(pageInfo);
            }
        }

        return orderedConflictingPages;
    }

    /**
     * Takes all the different versions (i.e., languages) of the page containing
     * the Identifier and Inodes that must be replaced and gets the data ready
     * to perform the change. Depending on the amount of information that must
     * be changed, this method takes two steps:
     * <ol>
     * <li>If the page version info needs to have its <b>Identifier and Inodes
     * updated</b>, the page needs to go through a special integrity fix
     * process, which is slightly different from the common one. In the new one,
     * we need to specify different working and live Inodes to get the version
     * info from (since we'll be fixing an older version of the page), but we
     * <b>WILL NOT</b> be adding a new version info, as we always do. Then, just
     * run the re-index on the page and flush the respective caches.</li>
     * <li>If the page version info needs to have its <b>Identifier updated
     * ONLY</b>, the page just needs to be re-indexed and also flush the
     * respective caches. This approach happens when the page has more than one
     * language, which also means all the required information has been added by
     * the first step.</li>
     * </ol>
     * 
     * @param pageLanguages
     *            - The different versions (languages) of the page whose
     *            Identifier and Inodes must be updated.
     * @param affectedIdentifier
     *            It contains the affected identifier with the following
     *            information:
     *            <ul>
     *            <li>The identifier that must be replaced.</li>
     *            <li>The working Inode that must be replaced.</li>
     *            <li>The live Inode that must be replaced.</li>
     *            <li>The page language associated to the working/live Inode
     *            that must be replaced.</li>
     *            </ul>
     * @throws DotDataException
     *             An error occurred when running one of the SQL queries.
     * @throws DotSecurityException
     *             The specified user does not have permissions to perform the
     *             selected action.
     */
    private void updateConflictingPageIds(List<Map<String, Object>> pageLanguages,
            AffectedIdentifierInfoBucket affectedIdentifier) throws DotDataException, DotSecurityException {
        // New Identifier that will be set for the conflicting page
        final String newIdentifier = UUIDGenerator.generateUuid();
        final String identifierToChange = affectedIdentifier.getAffectedIdentifier();

        int versionCounter = pageLanguages.size();

		for (Map<String, Object> pageInfo : pageLanguages) {
			final boolean fixAllVersions = versionCounter == 1 ? true : false;
			final String existingParentPath = (String) pageInfo.get("parent_path");
			final String existingAssetName = (String) pageInfo.get("asset_name");

			// Current working and live inodes for the existing page used to 
			// re-index the page after changing its data
			final String workingInode = (String) pageInfo.get("working_inode");
			final String liveInode = (String) pageInfo.get("live_inode");
			final Long languageId = DbConnectionFactory.isOracle() ? ((BigDecimal) pageInfo.get("lang")).longValue() : (Long) pageInfo.get("lang");

			if (affectedIdentifier.isLanguageAffected(languageId)) {
			    // Language was affected so we need to fix it
			    final String liveInodeToChange = affectedIdentifier.getLiveAffectedInode(languageId);
			    final String workingInodeToChange = affectedIdentifier.getWorkingAffectedInode(languageId);

                // New Inodes that will be set for the existing page
				final String newWorkingInode = UUIDGenerator.generateUuid();
				final String newLiveInode = (!workingInodeToChange.equalsIgnoreCase(liveInodeToChange)) ? UUIDGenerator.generateUuid() : newWorkingInode;

				Map<String, Object> existingPageData = new HashMap<String, Object>();
				existingPageData.put("local_identifier", identifierToChange);
				existingPageData.put("remote_identifier", newIdentifier);
				existingPageData.put(getIntegrityType().getFirstDisplayColumnLabel(), existingParentPath + existingAssetName);
				existingPageData.put("local_working_inode", workingInodeToChange);
				existingPageData.put("local_live_inode", liveInodeToChange);
				existingPageData.put("remote_working_inode", newWorkingInode);
				existingPageData.put("remote_live_inode", newLiveInode);
				// Important: We need to change the language to prevent errors with Oracle
				existingPageData.put("language_id", DbConnectionFactory.isOracle() ? new BigDecimal(languageId) : languageId);

				boolean addNewVersionInfo = (workingInode.equalsIgnoreCase(workingInodeToChange) && liveInode.equalsIgnoreCase(liveInodeToChange)) ? true : false;
				fixContentPageConflicts(existingPageData, fixAllVersions,
						addNewVersionInfo, workingInode, liveInode);
			} else {
			    // We don't need to change anything just reindex.
				reindexExistingPage(identifierToChange, existingAssetName,
						newIdentifier, workingInode, liveInode, languageId,
						fixAllVersions);
			}
 	        versionCounter--;
 		}
 	}

	/**
	 * Re-indexes a page that only had its Identifier changed. This method is
	 * called to process the other languages of a conflicting page. In order for
	 * this method to perform correctly, the IDs of the conflicting page in the
	 * conflicting language should have run first so that this method will have
	 * all the information it needs.
	 * 
	 * @param existingIdentifier
	 *            - The Identifier that must be changed
	 * @param assetName
	 *            - The name of the Content Page.
	 * @param newIdentifier
	 *            - The newly generated Identifier.
	 * @param workingInode
	 *            - The working Inode used to get the Page object that will be
	 *            updated and re-indexed.
	 * @param liveInode
	 *            - If available, the live Inode used to get the Page object
	 *            that will be updated and re-indexed.
	 * @param languageId
	 *            - The language ID of the page.
	 * @param fixAllVersions
	 *            - If <code>true</code>, all the records associated to the old
	 *            Identifier will be updated to have the new Identifier, and
	 *            that will be the final step of the integrity fix process.
	 * @throws DotDataException
	 *             An error occurred when running one of the SQL queries.
	 * @throws DotSecurityException
	 *             The specified user does not have permissions to perform the
	 *             selected action.
	 */
	private void reindexExistingPage(String existingIdentifier, String assetName, String newIdentifier, String workingInode,
			String liveInode, Long languageId, boolean fixAllVersions) throws DotDataException, DotSecurityException {
		ContentletAPI contentletAPI = APILocator.getContentletAPI();
        User systemUser = APILocator.getUserAPI().getSystemUser();
		Contentlet oldWorkingPage = contentletAPI.find(workingInode,
				systemUser, false);
        Contentlet oldLivePage = null;
        try {
			oldLivePage = contentletAPI.find(liveInode,
					systemUser, false);
        } catch (DotHibernateException e) { /* No Live Version */
        }
        if (fixAllVersions) {
			fixFinalPageVersion(newIdentifier, existingIdentifier, assetName);
        }
        // Add a new Lucene index for the updated page
        Contentlet updatedWorkingPage = new Contentlet();
        Contentlet updatedLivePage = null;
        updatedWorkingPage.setStructureInode(oldWorkingPage.getStructureInode());
        contentletAPI.copyProperties(updatedWorkingPage, oldWorkingPage.getMap());
        updatedWorkingPage.setIdentifier(newIdentifier);
        // If the working Inode is different from the live Inode...
        if (UtilMethods.isSet(liveInode) && !liveInode.equals(workingInode)) {
            updatedLivePage = new Contentlet();
            updatedLivePage.setStructureInode(oldLivePage.getStructureInode());
            contentletAPI.copyProperties(updatedLivePage, oldLivePage.getMap());
            updatedLivePage.setIdentifier(newIdentifier);
        }
		reindexFixedPage(updatedWorkingPage, oldWorkingPage, updatedLivePage,
				oldLivePage, workingInode, liveInode, languageId,
				fixAllVersions);
	}

	/**
	 * Contains the information that will fix the integrity conflict. Such
	 * information will be used to update the end point data that will end up in
	 * having the sender and receiver environments with the same consistent data
	 * (see
	 * {@link #fixContentPageConflicts(Map, boolean, boolean, String, String)})
	 * 
	 * @param pageData
	 *            - The information used to update the page data in the end
	 *            point.
	 * @param fixAllVersions
	 *            - If <code>true</code>, all the records associated to the old
	 *            Identifier will be updated to have the new Identifier, and
	 *            that will be the final step of the integrity fix process.
	 * @throws DotDataException
	 *             An error occurred when running one of the SQL queries.
	 * @throws DotSecurityException
	 *             The specified user does not have permissions to perform the
	 *             selected action.
	 */
	private void fixContentPageConflicts(Map<String, Object> pageData,
			boolean fixAllVersions) throws DotDataException,
			DotSecurityException {
		fixContentPageConflicts(pageData, fixAllVersions, true, null, null);
	}

    /**
	 * Directly updates the information of a given Content Page - i.e., the new
	 * {@link IHTMLPage} - to resolve the conflict (two pages with same path but
	 * different identifier) found in the receiver server before a push publish
	 * is triggered. This new HTML page is a specialized form of the
	 * {@link Contentlet} class. This method presents slight variations when 
	 * used to fix conflicts for a page that was previously fixed by the 
	 * Integrity Checker, and then was moved to another folder or had its URL 
	 * changed. 
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
	 * @param addNewVersionInfo
	 *            - If <code>true</code>, a new version info of the page will be
	 *            created, which applies to common integrity fixes. This DO NOT
	 *            apply if we're solving conflicts caused by a page that was
	 *            previously fixed and moved or had its URL changed.
	 * @param workingInode
	 *            - For conflicting pages that were moved or had its URL
	 *            changed, this value allows us to get a specific working
	 *            version info object, and not just the most current one.
	 * @param liveInode
	 *            - For conflicting pages that were moved or had its URL
	 *            changed, this value allows us to get a specific live version
	 *            info object, and not just the most current one.
	 * @throws DotDataException
	 *             An error occurred when interacting with the database.
	 * @throws DotSecurityException
	 *             The current user does not have permission to perform this
	 *             action.
	 */
	private void fixContentPageConflicts(Map<String, Object> pageData,
			boolean fixAllVersions, boolean addNewVersionInfo,
			String workingInode, String liveInode) throws DotDataException,
			DotSecurityException {

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
        // Get the asset name form the page URL
        String[] assetUrl = assetName.split("/");
        assetName = assetUrl[assetUrl.length - 1];
        ContentletAPI contentletAPI = APILocator.getContentletAPI();
        User systemUser = APILocator.getUserAPI().getSystemUser();
        // Get working and live pages to re-index them after applying the fix
		Contentlet existingWorkingContentPage = contentletAPI.find(UtilMethods
				.isSet(workingInode) ? workingInode : localWorkingInode,
				systemUser, false);
        Contentlet existingLiveContentPage = null;
        try {
			existingLiveContentPage = contentletAPI.find(
					UtilMethods.isSet(liveInode) ? liveInode : localLiveInode,
					systemUser, false);
        } catch (DotHibernateException e) { /* No Live Version */
        }
        // If not existing, add the new Identifier with a temporary asset name
        dc.setSQL("SELECT id FROM identifier WHERE id = ?");
        dc.addParam(newHtmlPageIdentifier);
        List<Map<String, Object>> results = dc.loadObjectResults();
        if (results == null || results.size() == 0) {
            dc.setSQL("INSERT INTO identifier(id, parent_path, asset_name, host_inode, asset_type, syspublish_date, sysexpire_date) "
                    + "SELECT ? , parent_path, 'TEMP_CONTENTPAGE_NAME', host_inode, asset_type, syspublish_date, sysexpire_date FROM identifier WHERE id = ?");
            dc.addParam(newHtmlPageIdentifier);
            dc.addParam(oldHtmlPageIdentifier);
            dc.loadResult();
        }
        // Insert the new working Inode record
        dc.setSQL("INSERT INTO inode(inode, owner, idate, type) SELECT ?, owner, idate, type FROM inode i WHERE i.inode = ?");
        dc.addParam(remoteWorkingInode);
        dc.addParam(localWorkingInode);
        dc.loadResult();
        // If different from working, insert the new live Inode record
        if (!remoteWorkingInode.equals(remoteLiveInode) && UtilMethods.isSet(remoteLiveInode)
                && UtilMethods.isSet(localLiveInode)) {
            dc.setSQL("INSERT INTO inode(inode, owner, idate, type) "
                    + "SELECT ?, owner, idate, type FROM inode i WHERE i.inode = ?");
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
        // If different from working, Insert the new live Contentlet record
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
        // If the new page requires a new version info...
        if (addNewVersionInfo) {
        	// Insert the new Contentlet_version_info record with the new Inode
	        if (UtilMethods.isSet(remoteLiveInode) && UtilMethods.isSet(localLiveInode)) {
	            dc.setSQL("INSERT INTO contentlet_version_info(identifier, lang, working_inode, live_inode, deleted, locked_by, locked_on, version_ts) "
	                    + "SELECT ?, ?, ?, ?, deleted, locked_by, locked_on, version_ts FROM contentlet_version_info WHERE identifier = ? AND working_inode = ? AND lang = ?");
	            dc.addParam(newHtmlPageIdentifier);
	            dc.addParam(languageId);
	            dc.addParam(remoteWorkingInode);
	            dc.addParam(remoteLiveInode);
	            dc.addParam(oldHtmlPageIdentifier);
	            dc.addParam(localWorkingInode);
	            dc.addParam(languageId);
	            dc.loadResult();
	        } else {
	        	if(UtilMethods.isSet(localWorkingInode) && UtilMethods.isSet(localLiveInode) && localWorkingInode.equals(localLiveInode)){
					dc.setSQL("INSERT INTO contentlet_version_info(identifier, lang, working_inode, live_inode, deleted, locked_by, locked_on, version_ts) "
							+ "SELECT ?, ?, ?, ?, deleted, locked_by, locked_on, version_ts FROM contentlet_version_info WHERE identifier = ? AND working_inode = ? AND lang = ?");
					dc.addParam(newHtmlPageIdentifier);
					dc.addParam(languageId);
					dc.addParam(remoteWorkingInode);
					dc.addParam(remoteWorkingInode);
					dc.addParam(oldHtmlPageIdentifier);
					dc.addParam(localWorkingInode);
					dc.addParam(languageId);
					dc.loadResult();
				}else{
		        	dc.setSQL("INSERT INTO contentlet_version_info(identifier, lang, working_inode, live_inode, deleted, locked_by, locked_on, version_ts) "
		                    + "SELECT ?, ?, ?, live_inode, deleted, locked_by, locked_on, version_ts FROM contentlet_version_info WHERE identifier = ? AND working_inode = ? AND lang = ?");
		            dc.addParam(newHtmlPageIdentifier);
		            dc.addParam(languageId);
		            dc.addParam(remoteWorkingInode);
		            dc.addParam(oldHtmlPageIdentifier);
		            dc.addParam(localWorkingInode);
		            dc.addParam(languageId);
		            dc.loadResult();
		        }
	        }
	        // Remove the live_inode references from Contentlet_version_info
	        dc.setSQL("DELETE FROM contentlet_version_info WHERE identifier = ? AND lang = ? AND working_inode = ?");
	        dc.addParam(oldHtmlPageIdentifier);
	        dc.addParam(languageId);
	        dc.addParam(localWorkingInode);
	        dc.loadResult();
        }

		// Update the webasset (identifier) in case the page is in a Workflow step
		if (UtilMethods.isSet(oldHtmlPageIdentifier) && UtilMethods.isSet(newHtmlPageIdentifier)
				&& !oldHtmlPageIdentifier.equals(newHtmlPageIdentifier)) {

			dc.setSQL("UPDATE workflow_task SET webasset = ? WHERE webasset = ?");
			dc.addParam(newHtmlPageIdentifier);
			dc.addParam(oldHtmlPageIdentifier);
			dc.loadResult();
		}

        // Remove the conflicting working version of the Contentlet record
        dc.setSQL("DELETE FROM contentlet WHERE identifier = ? AND inode = ? AND language_id = ?");
        dc.addParam(oldHtmlPageIdentifier);
        dc.addParam(localWorkingInode);
        dc.addParam(languageId);
        dc.loadResult();
        // If different from working, remove the conflicting live version too
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
			fixFinalPageVersion(newHtmlPageIdentifier, oldHtmlPageIdentifier,
					assetName);
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
            dc.loadResult();
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
        Contentlet newLiveContentPage = null;
        newWorkingContentPage.setStructureInode(existingWorkingContentPage.getStructureInode());
        contentletAPI.copyProperties(newWorkingContentPage, existingWorkingContentPage.getMap());
        newWorkingContentPage.setIdentifier(newHtmlPageIdentifier);
        if (addNewVersionInfo) {
        	newWorkingContentPage.setInode(remoteWorkingInode);
        }
        if (UtilMethods.isSet(localLiveInode) && !localLiveInode.equals(localWorkingInode)) {
            newLiveContentPage = new Contentlet();
            newLiveContentPage.setStructureInode(existingLiveContentPage.getStructureInode());
            contentletAPI.copyProperties(newLiveContentPage, existingLiveContentPage.getMap());
            newLiveContentPage.setIdentifier(newHtmlPageIdentifier);
            if (addNewVersionInfo) {
            	newLiveContentPage.setInode(remoteLiveInode);
            }
        }
		reindexFixedPage(newWorkingContentPage, existingWorkingContentPage,
				newLiveContentPage, existingLiveContentPage, localWorkingInode,
				localLiveInode, languageId, fixAllVersions);
    }

	/**
	 * Runs the queries that will wrap up the integrity fix process. They will
	 * replace ALL of the old Identifiers in the end point database, which will
	 * cause the old Identifier to be removed and we'll not be able to look for
	 * data based on the old Identifier. These queries must be run once and
	 * involve:
	 * <ol>
	 * <li>Updating the Contentlet records.</li>
	 * <li>Updating the Version info records.</li>
	 * <li>Deleting the old Identifier.</li>
	 * <li>Updating the temporary asset name in the Identifier table with the
	 * correct one.</li>
	 * <li>Updating the multi-tree records that hold the references to the page
	 * contents.</li>
	 * </ol>
	 * 
	 * @param newIdentifier
	 *            - The new Identifier for the page.
	 * @param oldIdentifier
	 *            - The previous Identifier of the page.
	 * @param assetName
	 *            - The correct asset name.
	 * @throws DotDataException
	 *             An error occurred when running one of the SQL queries.
	 */
	private void fixFinalPageVersion(String newIdentifier,
			String oldIdentifier, String assetName) throws DotDataException {
		DotConnect dc = new DotConnect();
		// Update other Contentlet languages with new Identifier
		dc.setSQL("UPDATE contentlet SET identifier = ? WHERE identifier = ?");
		dc.addParam(newIdentifier);
		dc.addParam(oldIdentifier);
		dc.loadResult();
		// Update previous version of the Contentlet_version_info with new
		// Identifier
		dc.setSQL("UPDATE contentlet_version_info SET identifier = ? WHERE identifier = ?");
		dc.addParam(newIdentifier);
		dc.addParam(oldIdentifier);
		dc.loadResult();
		// Remove the old Identifier record
		dc.setSQL("DELETE FROM identifier WHERE id = ?");
		dc.addParam(oldIdentifier);
		dc.loadResult();
		// Update the Identifier with the correct asset name
		dc.setSQL("UPDATE identifier SET asset_name = ? WHERE id = ?");
		dc.addParam(assetName);
		dc.addParam(newIdentifier);
		dc.loadResult();
		// Update the content references in the page with the new Identifier
		dc.setSQL("UPDATE multi_tree SET parent1 = ? WHERE parent1 = ?");
		dc.addParam(newIdentifier);
		dc.addParam(oldIdentifier);
		dc.loadResult();
	}

	/**
	 * Re-indexes a Content Page after its information has been updated/fixed by
	 * the Integrity Checker process. Changing information on a page involves
	 * performing two actions (with no particular order):
	 * <ul>
	 * <li>Re-indexing the page object.</li>
	 * <li>Clearing specific values/objects from the dotCMS caches to make sure
	 * they reflect the latest changes.</li>
	 * </ul>
	 * Working with the re-index and caches <b>is crucial</b> because the
	 * integrity fix process uses direct SQL queries to fix the inconsistencies,
	 * not the APIs. If a value is not evicted form the right cache, fresh
	 * information will not be visible and might lead to system and data
	 * inconsistencies.
	 * 
	 * @param updatedWorkingPage
	 *            - The working page object with the updated information.
	 * @param oldWorkingPage
	 *            - The working page object WITHOUT the updated information.
	 * @param updatedLivePage
	 *            - The live page object with the updated information.
	 * @param oldLivePage
	 *            - The live page object WITHOUT the updated information.
	 * @param localWorkingInode
	 *            - The working Inode that was replaced.
	 * @param localLiveInode
	 *            - The live Inode that was replaced.
	 * @param languageId
	 *            - The language ID of the page.
	 * @param fixAllVersions
	 *            - If <code>true</code>, all the Inodes associated to the page
	 *            will be evicted from the cache.
	 * @throws DotDataException
	 *             An error occurred when running one of the SQL queries.
	 * @throws DotSecurityException
	 *             The specified user does not have permissions to perform the
	 *             selected action.
	 */
	private void reindexFixedPage(Contentlet updatedWorkingPage,
			Contentlet oldWorkingPage, Contentlet updatedLivePage,
			Contentlet oldLivePage, String localWorkingInode,
			String localLiveInode, Long languageId, boolean fixAllVersions)
			throws DotDataException, DotSecurityException {
		ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();
		String identifier = updatedWorkingPage.getIdentifier();
		// If page was removed before in the end point, remove the NOTFOUND flag
		// for that page from the cache
		indexAPI.addContentToIndex(updatedWorkingPage);
		// If working and live pages are different...
		if (updatedLivePage != null) {

			indexAPI.addContentToIndex(updatedLivePage);
		}
		// Remove the Lucene index for the old page
		indexAPI.removeContentFromIndex(oldWorkingPage);
		if (oldLivePage != null
				&& !oldWorkingPage.getInode().equals(oldLivePage.getInode())) {
			indexAPI.removeContentFromIndex(oldLivePage);
		}
	}

    /**
     * This private class is used to stored information related with the identifier that has a conflict.
     * to fix conflicts when a page is moved in the receiver
     *
     */
    private static class AffectedIdentifierInfoBucket {
        private final String affectedIdentifier;
        private int counter = 0;
        // key = language id // value = affected working and live inodes
        private final HashMap<Long, AffectedContentletVersion> affectedContentletVersions = new HashMap<Long, AffectedContentletVersion>();

        static AffectedIdentifierInfoBucket build(final String affectedIdentifier) {
            return new AffectedIdentifierInfoBucket(affectedIdentifier);
        }

        private AffectedIdentifierInfoBucket(final String affectedIdentifier) {
            this.affectedIdentifier = affectedIdentifier;
        }

        public boolean isLanguageAffected(Long languageId) {
            if (languageId != null) {
                return affectedContentletVersions.get(languageId) != null;
            }

            return false;
        }

        public boolean needsToCheckPossibleConflicts() {
            return affectedContentletVersions.size() == counter;
        }

        public AffectedIdentifierInfoBucket addAffectedLanguage(final Long language,
                final String affectedLiveInode, final String affectedWorkingInode) {
            if (language != null) {
                this.counter++;
                this.affectedContentletVersions.put(language, new AffectedContentletVersion(affectedLiveInode, affectedWorkingInode));
            }

            return this;
        }

        public AffectedIdentifierInfoBucket decrementCounter() {
            counter--;
            return this;
        }

        public String getAffectedIdentifier() {
            return affectedIdentifier;
        }

        public int getCounter() {
            return counter;
        }
        
        public String getWorkingAffectedInode(Long languageId) {
            return this.affectedContentletVersions.get(languageId).getAffectedWorkingInode();
        }
        
        public String getLiveAffectedInode(Long languageId) {
            return this.affectedContentletVersions.get(languageId).getAffectedLiveInode();
        }

        /**
         * This class was made to store working and live inodes that may cause
         * conflicts
         */
        private class AffectedContentletVersion {
            private final String workingInode;
            private final String liveInode;

            AffectedContentletVersion(final String liveInode, final String workingInode) {
                this.workingInode = workingInode;
                this.liveInode = liveInode;
            }

            public String getAffectedLiveInode() {
                return liveInode;
            }

            public String getAffectedWorkingInode() {
                return workingInode;
            }
        }
    }
}
