package com.dotcms.integritycheckers;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * File assets integrity checker implementation.
 * 
 * @author Rogelio Blanco
 * @version 1.0
 * @since 06-10-2015
 * 
 */
public class ContentFileAssetIntegrityChecker extends AbstractIntegrityChecker {

    public final IntegrityType getIntegrityType() {
        return IntegrityType.FILEASSETS;
    }

    @Override
    public File generateCSVFile(final String outputPath) throws DotDataException, IOException {
        final String outputFile = outputPath + File.separator
                + getIntegrityType().getDataToCheckCSVName();

        return generateContentletsCSVFile(outputFile, Structure.STRUCTURE_TYPE_FILEASSET);
    }

    @Override
    public boolean generateIntegrityResults(final String endpointId) throws Exception {
        try {
            // Create a temporary table and insert all the records coming from
            // the CSV file.
            createContentletTemporaryTable(endpointId);

            // Load data to the temporary table and if there is conflicts load
            // the results table
            processContentletIntegrityByStructureType(endpointId,
                    Structure.STRUCTURE_TYPE_FILEASSET);

            // Get data from results table
            DotConnect dc = new DotConnect();
			return (Long) dc.getRecordCount(getIntegrityType().getResultsTableName(), "where endpoint_id = '"+ endpointId+ "'") > 0;
        } catch (Exception e) {
            throw new Exception("Error running the File Assets Integrity Check", e);
        }
    }

    @Override
    public void executeFix(final String remoteIP) throws DotDataException, DotSecurityException {
        DotConnect dc = new DotConnect();
        // Get information from IR.
        final String getResultsQuery = new StringBuilder("SELECT ")
                .append(getIntegrityType().getFirstDisplayColumnLabel())
                .append(", local_identifier, remote_identifier, local_working_inode, remote_working_inode, local_live_inode, remote_live_inode, language_id FROM ")
                .append(getIntegrityType().getResultsTableName()).append(" WHERE endpoint_id = ?")
                .toString();
        dc.setSQL(getResultsQuery);
        dc.addParam(remoteIP);
        List<Map<String, Object>> results = dc.loadObjectResults();

        // Generate counter to know how many version are for each identifier
        // with conflicts
        Map<String, Integer> versionCount = new HashMap<String, Integer>();
        for (Map<String, Object> result : results) {
            final String oldIdentifier = (String) result.get("local_identifier");

            final Integer counter = versionCount.get(oldIdentifier);
            if (counter == null) {
                versionCount.put(oldIdentifier, 1);
            } else {
                versionCount.put(oldIdentifier, counter + 1);
            }
        }

        for (Map<String, Object> result : results) {
            final String oldIdentifier = (String) result.get("local_identifier");

            int counter = versionCount.get(oldIdentifier);
            boolean isTheLastConflict = counter == 1 ? true : false;

            fixContentletConflicts(result, Structure.STRUCTURE_TYPE_FILEASSET, isTheLastConflict);

            if (!isTheLastConflict) {
                // Decrease version counter if greater than 1
                versionCount.put(oldIdentifier, counter - 1);
            }

        }
    }

    /**
     * Directly updates the information of a given Contentlet - i.e., to resolve
     * the conflict (two contentlets with same path but different identifier)
     * found in the receiver server before a push publish is triggered. This new
     * HTML page is a specialized form of the {@link Contentlet} class.
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
     * @param contentletData
     *            - A {@link Map} with the page information that was captured
     *            when the conflict was detected.
     * @param structureTypeId
     *            - A {@link Structure} type of the contentlet, this is to reuse
     *            the method when we need to do a fix of other contentlets.
     * @param isTheLastConflict
     *            - If <code>true</code>, all existing versions of the page will
     *            be updated in order to keep data consistency. Otherwise, ONLY
     *            the specified page and language will be updated. This is when
     *            we need to do an update once not each time.
     * @throws DotDataException
     * @throws DotRuntimeException 
     * @throws Exception
     * @throws SQLException
     */
    private void fixContentletConflicts(final Map<String, Object> contentletData,
            final int structureTypeId, final boolean isTheLastConflict) throws DotDataException,
            DotSecurityException, DotRuntimeException {
        final String oldContentletIdentifier = (String) contentletData.get("local_identifier");
        final String newContentletIdentifier = (String) contentletData.get("remote_identifier");

        // Get file asset name from a URL
        final String assetURL = (String) contentletData.get(getIntegrityType()
                .getFirstDisplayColumnLabel());
        String[] assetURLArray = assetURL.split("/");
        final String assetName = assetURLArray[assetURLArray.length - 1];

        final String localWorkingInode = (String) contentletData.get("local_working_inode");
        final String localLiveInode = (String) contentletData.get("local_live_inode");
        final String remoteWorkingInode = (String) contentletData.get("remote_working_inode");
        final String remoteLiveInode = (String) contentletData.get("remote_live_inode");

        Long languageId;
        if (DbConnectionFactory.isOracle()) {
            BigDecimal lang = (BigDecimal) contentletData.get("language_id");
            languageId = new Long(lang.toPlainString());
        } else {
            languageId = (Long) contentletData.get("language_id");
        }

        ContentletAPI contentletAPI = APILocator.getContentletAPI();
        User systemUser = APILocator.getUserAPI().getSystemUser();
        Contentlet existingWorkingContentlet = contentletAPI.find(localWorkingInode, systemUser,
                false);
        Contentlet existingLiveContentlet = null;

        try {
            existingLiveContentlet = contentletAPI.find(localLiveInode, systemUser, false);
        } catch (DotHibernateException e) { /* No Live Version */
        }

        DotConnect dc = new DotConnect();
        dc.setSQL("SELECT id FROM identifier WHERE id = ?");
        dc.addParam(newContentletIdentifier);
        List<Map<String, Object>> results = dc.loadObjectResults();
        // If not existing, add the new Identifier with a temporary asset
        // name. We need to have a dummy asset name because there is a
        // constrain that limit us to use the final one
        if (results == null || results.size() == 0) {
            final String temporalAssetName = "TEMP_" + UUID.randomUUID().toString();
            dc.setSQL(new StringBuilder(
                    "INSERT INTO identifier (id, parent_path, asset_name, host_inode, asset_type, syspublish_date, sysexpire_date) ")
                    .append("SELECT ?, parent_path, '")
                    .append(temporalAssetName)
                    .append("', host_inode, asset_type, syspublish_date, sysexpire_date FROM identifier WHERE id = ?")
                    .toString());
            dc.addParam(newContentletIdentifier);
            dc.addParam(oldContentletIdentifier);
            dc.loadResult();
        }
        // Insert the new Inodes records so it can be used in the contentlet
        dc.setSQL("INSERT INTO inode(inode, owner, idate, type) SELECT ?, owner, idate, type FROM inode i WHERE i.inode = ?");
        dc.addParam(remoteWorkingInode);
        dc.addParam(localWorkingInode);
        dc.loadResult();

        if (!remoteWorkingInode.equals(remoteLiveInode) && UtilMethods.isSet(remoteLiveInode)
                && UtilMethods.isSet(localLiveInode)) {
            dc.setSQL("INSERT INTO inode(inode, owner, idate, type) SELECT ?, owner, idate, type FROM inode i WHERE i.inode = ?");
            dc.addParam(remoteLiveInode);
            dc.addParam(localLiveInode);
            dc.loadResult();
        }

        // Insert the new working Contentlet record with the new Inode
        String contentletQuery = new StringBuilder(
                "INSERT INTO contentlet(inode, show_on_menu, title, mod_date, mod_user, sort_order, friendly_name, structure_inode, last_review, next_review, review_interval, disabled_wysiwyg, identifier, language_id, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12, date13, date14, date15, date16, date17, date18, date19, date20, date21, date22, date23, date24, date25, text1, text2, text3, text4, text5, text6, text7, text8, text9, text10, text11, text12, text13, text14, text15, text16, text17, text18, text19, text20, text21, text22, text23, text24, text25, text_area1, text_area2, text_area3, text_area4, text_area5, text_area6, text_area7, text_area8, text_area9, text_area10, text_area11, text_area12, text_area13, text_area14, text_area15, text_area16, text_area17, text_area18, text_area19, text_area20, text_area21, text_area22, text_area23, text_area24, text_area25, integer1, integer2, integer3, integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14, integer15, integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, \"float1\", \"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\", \"float8\", \"float9\", \"float10\", \"float11\", \"float12\", \"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\", \"float19\", \"float20\", \"float21\", \"float22\", \"float23\", \"float24\", \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13, bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25) ")
                .append("SELECT ?, show_on_menu, title, mod_date, mod_user, sort_order, friendly_name, structure_inode, last_review, next_review, review_interval, disabled_wysiwyg, ?, ?, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12, date13, date14, date15, date16, date17, date18, date19, date20, date21, date22, date23, date24, date25, text1, text2, text3, text4, text5, text6, text7, text8, text9, text10, text11, text12, text13, text14, text15, text16, text17, text18, text19, text20, text21, text22, text23, text24, text25, text_area1, text_area2, text_area3, text_area4, text_area5, text_area6, text_area7, text_area8, text_area9, text_area10, text_area11, text_area12, text_area13, text_area14, text_area15, text_area16, text_area17, text_area18, text_area19, text_area20, text_area21, text_area22, text_area23, text_area24, text_area25, integer1, integer2, integer3, integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14, integer15, integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, \"float1\", \"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\", \"float8\", \"float9\", \"float10\", \"float11\", \"float12\", \"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\", \"float19\", \"float20\", \"float21\", \"float22\", \"float23\", \"float24\", \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13, bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25 ")
                .append("FROM contentlet c INNER JOIN contentlet_version_info cvi on (c.inode = cvi.working_inode) WHERE c.identifier = ? and c.language_id = ?")
                .toString();
        if (DbConnectionFactory.isMySql()) {
            // Use correct escape char when using reserved words as column
            // names
            contentletQuery = contentletQuery.replaceAll("\"", "`");
        }
        dc.setSQL(contentletQuery);
        dc.addParam(remoteWorkingInode);
        dc.addParam(newContentletIdentifier);
        dc.addParam(languageId);
        dc.addParam(oldContentletIdentifier);
        dc.addParam(languageId);
        dc.loadResult();

        if (!remoteWorkingInode.equals(remoteLiveInode) && UtilMethods.isSet(remoteLiveInode)
                && UtilMethods.isSet(localLiveInode)) {
            contentletQuery = new StringBuilder(
                    "INSERT INTO contentlet(inode, show_on_menu, title, mod_date, mod_user, sort_order, friendly_name, structure_inode, last_review, next_review, review_interval, disabled_wysiwyg, identifier, language_id, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12, date13, date14, date15, date16, date17, date18, date19, date20, date21, date22, date23, date24, date25, text1, text2, text3, text4, text5, text6, text7, text8, text9, text10, text11, text12, text13, text14, text15, text16, text17, text18, text19, text20, text21, text22, text23, text24, text25, text_area1, text_area2, text_area3, text_area4, text_area5, text_area6, text_area7, text_area8, text_area9, text_area10, text_area11, text_area12, text_area13, text_area14, text_area15, text_area16, text_area17, text_area18, text_area19, text_area20, text_area21, text_area22, text_area23, text_area24, text_area25, integer1, integer2, integer3, integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14, integer15, integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, \"float1\", \"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\", \"float8\", \"float9\", \"float10\", \"float11\", \"float12\", \"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\", \"float19\", \"float20\", \"float21\", \"float22\", \"float23\", \"float24\", \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13, bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25) ")
                    .append("SELECT ?, show_on_menu, title, mod_date, mod_user, sort_order, friendly_name, structure_inode, last_review, next_review, review_interval, disabled_wysiwyg, ?, ?, date1, date2, date3, date4, date5, date6, date7, date8, date9, date10, date11, date12, date13, date14, date15, date16, date17, date18, date19, date20, date21, date22, date23, date24, date25, text1, text2, text3, text4, text5, text6, text7, text8, text9, text10, text11, text12, text13, text14, text15, text16, text17, text18, text19, text20, text21, text22, text23, text24, text25, text_area1, text_area2, text_area3, text_area4, text_area5, text_area6, text_area7, text_area8, text_area9, text_area10, text_area11, text_area12, text_area13, text_area14, text_area15, text_area16, text_area17, text_area18, text_area19, text_area20, text_area21, text_area22, text_area23, text_area24, text_area25, integer1, integer2, integer3, integer4, integer5, integer6, integer7, integer8, integer9, integer10, integer11, integer12, integer13, integer14, integer15, integer16, integer17, integer18, integer19, integer20, integer21, integer22, integer23, integer24, integer25, \"float1\", \"float2\", \"float3\", \"float4\", \"float5\", \"float6\", \"float7\", \"float8\", \"float9\", \"float10\", \"float11\", \"float12\", \"float13\", \"float14\", \"float15\", \"float16\", \"float17\", \"float18\", \"float19\", \"float20\", \"float21\", \"float22\", \"float23\", \"float24\", \"float25\", bool1, bool2, bool3, bool4, bool5, bool6, bool7, bool8, bool9, bool10, bool11, bool12, bool13, bool14, bool15, bool16, bool17, bool18, bool19, bool20, bool21, bool22, bool23, bool24, bool25 ")
                    .append("FROM contentlet c INNER JOIN contentlet_version_info cvi on (c.inode = cvi.live_inode) WHERE c.identifier = ? and c.language_id = ?")
                    .toString();

            dc.setSQL(contentletQuery);
            dc.addParam(remoteLiveInode);
            dc.addParam(newContentletIdentifier);
            dc.addParam(languageId);
            dc.addParam(oldContentletIdentifier);
            dc.addParam(languageId);
            dc.loadResult();
        }

        // Insert the new Contentlet_version_info record with the new Inode

        if (UtilMethods.isSet(remoteLiveInode) && UtilMethods.isSet(localLiveInode)) {
            dc.setSQL(new StringBuilder(
                    "INSERT INTO contentlet_version_info(identifier, lang, working_inode, live_inode, deleted, locked_by, locked_on, version_ts) ")
                    .append("SELECT ?, ?, ?, ?, deleted, locked_by, locked_on, version_ts ")
                    .append("FROM contentlet_version_info WHERE identifier = ? AND working_inode = ? AND lang = ?")
                    .toString());
            dc.addParam(newContentletIdentifier);
            dc.addParam(languageId);
            dc.addParam(remoteWorkingInode);
            dc.addParam(remoteLiveInode);
            dc.addParam(oldContentletIdentifier);
            dc.addParam(localWorkingInode);
            dc.addParam(languageId);
            dc.loadResult();
        } else {
        	if(UtilMethods.isSet(localLiveInode) && !localWorkingInode.equals(localLiveInode)){
        		dc.setSQL(new StringBuilder(
        				"INSERT INTO contentlet_version_info(identifier, lang, working_inode, live_inode, deleted, locked_by, locked_on, version_ts) ")
        				.append("SELECT ?, ?, ?, live_inode, deleted, locked_by, locked_on, version_ts ")
        				.append("FROM contentlet_version_info WHERE identifier = ? AND working_inode = ? AND lang = ?")
        				.toString());
        	}else{
        		dc.setSQL(new StringBuilder(
			        "INSERT INTO contentlet_version_info(identifier, lang, working_inode, live_inode, deleted, locked_by, locked_on, version_ts) ")
					.append("SELECT ?, ?, ?, null, deleted, locked_by, locked_on, version_ts ")
					.append("FROM contentlet_version_info WHERE identifier = ? AND working_inode = ? AND lang = ?")
					.toString());
            }
            dc.addParam(newContentletIdentifier);
            dc.addParam(languageId);
            dc.addParam(remoteWorkingInode);
            dc.addParam(oldContentletIdentifier);
            dc.addParam(localWorkingInode);
            dc.addParam(languageId);
            dc.loadResult();
        }

        // Update other workflow task with new Identifier
        dc.setSQL("UPDATE workflow_task SET webasset = ? WHERE webasset = ? AND language_id = ?");
        dc.addParam(newContentletIdentifier);
        dc.addParam(oldContentletIdentifier);
        dc.addParam(languageId);
        dc.loadResult();

        // Remove the live_inode references from Contentlet_version_info
        dc.setSQL("DELETE FROM contentlet_version_info WHERE identifier = ? AND lang = ? AND working_inode = ?");
        dc.addParam(oldContentletIdentifier);
        dc.addParam(languageId);
        dc.addParam(localWorkingInode);
        dc.loadResult();

        // Remove the conflicting version of the Contentlet record
        dc.setSQL("DELETE FROM contentlet WHERE identifier = ? AND inode = ? AND language_id = ?");
        dc.addParam(oldContentletIdentifier);
        dc.addParam(localWorkingInode);
        dc.addParam(languageId);
        dc.loadResult();

        if (UtilMethods.isSet(localLiveInode) && UtilMethods.isSet(remoteLiveInode)
                && !localLiveInode.equals(localWorkingInode)) {
            // Remove the conflicting version of the Contentlet record
            dc.setSQL("DELETE FROM contentlet WHERE identifier = ? AND inode = ? AND language_id = ?");
            dc.addParam(oldContentletIdentifier);
            dc.addParam(localLiveInode);
            dc.addParam(languageId);
            dc.loadResult();
        }

        // Update other Contentlet languages with new Identifier
        dc.setSQL("UPDATE contentlet SET identifier = ? WHERE identifier = ? AND language_id = ?");
        dc.addParam(newContentletIdentifier);
        dc.addParam(oldContentletIdentifier);
        dc.addParam(languageId);
        dc.loadResult();

        // Update previous version of the Contentlet_version_info with
        // new Identifier
        dc.setSQL("UPDATE contentlet_version_info SET identifier = ? WHERE identifier = ? AND lang = ?");
        dc.addParam(newContentletIdentifier);
        dc.addParam(oldContentletIdentifier);
        dc.addParam(languageId);

        if (isTheLastConflict) {
            // Remove the old Identifier record
            dc.setSQL("DELETE FROM identifier WHERE id = ?");
            dc.addParam(oldContentletIdentifier);
            dc.loadResult();
            // Update the Identifier with the correct asset name
            dc.setSQL("UPDATE identifier SET asset_name = ? WHERE id = ?");
            dc.addParam(assetName);
            dc.addParam(newContentletIdentifier);
            dc.loadResult();

            if (structureTypeId == Structure.STRUCTURE_TYPE_HTMLPAGE) {
                // Update the content references in the page with the new
                // Identifier
                dc.setSQL("UPDATE multi_tree SET parent1 = ? WHERE parent1 = ?");
                dc.addParam(newContentletIdentifier);
                dc.addParam(oldContentletIdentifier);
                dc.loadResult();
            }


            if (structureTypeId == Structure.STRUCTURE_TYPE_FILEASSET && assetURL.contains(Constants.CONTAINER_FOLDER_PATH) &&
                        assetURL.endsWith("/container.vtl")) {

                this.fixFileAssetContainer(oldContentletIdentifier, newContentletIdentifier, localWorkingInode, dc);
            }
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

        // Create new contentlet using the current information, this is for
        // the working contentlet
        generateNewContentlet(existingWorkingContentlet, structureTypeId, newContentletIdentifier,
                remoteWorkingInode);

        if (UtilMethods.isSet(localLiveInode) && !localLiveInode.equals(localWorkingInode)) {
            // Create new contentlet using the current information, this is
            // for the live contentlet. This is need it when live and
            // working are different
            generateNewContentlet(existingLiveContentlet, structureTypeId, newContentletIdentifier,
                    remoteLiveInode);
        }

        // Remove the Lucene index for the old page
        cleanIndex(existingWorkingContentlet, existingLiveContentlet);
    }

    private void fixFileAssetContainer(final String oldContentletIdentifier, final String newContentletIdentifier,
                                       final String localWorkingInode, final DotConnect dotConnect) throws DotDataException {

        // Update the multitree with the new containerid
        dotConnect.setSQL("UPDATE multi_tree SET parent2 = ? WHERE parent2 = ?");
        dotConnect.addParam(newContentletIdentifier);
        dotConnect.addParam(oldContentletIdentifier);
        dotConnect.loadResult();
    }

    /**
     * Clean index from lucene
     * 
     * @param existingWorkingContentlet
     * @param existingLiveContentlet
     * @throws DotHibernateException
     */
    private void cleanIndex(final Contentlet existingWorkingContentlet,
            final Contentlet existingLiveContentlet) throws DotDataException {
        ContentletIndexAPI indexAPI = APILocator.getContentletIndexAPI();
        indexAPI.removeContentFromIndex(existingWorkingContentlet);

        if (UtilMethods.isSet(existingLiveContentlet)
                && !existingWorkingContentlet.getInode().equals(existingLiveContentlet.getInode())) {
            indexAPI.removeContentFromIndex(existingLiveContentlet);
        }
    }

    /**
     * Create a new contentlet from the old one. This method basically copy all
     * the information from the old contentlet and paste it in the new
     * contentlet, after that process is over we need to add the new contentlet
     * to lucene index.
     * 
     * @param existingContentlet
     *            contains the information that we need to copy to the new
     *            contentlet
     * @param newContentletIdentifier
     *            identifier for the new contentlet
     * @param remoteInode
     *            inode for the new contentlet
     * @return new generated contentlet
     * @throws DotContentletStateException
     * @throws DotRuntimeException
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException 
     */
    private Contentlet generateNewContentlet(Contentlet existingContentlet,
            final int structureTypeId, final String newContentletIdentifier,
            final String remoteInode) throws DotContentletStateException, DotRuntimeException,
            DotSecurityException, DotDataException {

        // If its an asset file, move the asset to a new location
        if (structureTypeId == Structure.STRUCTURE_TYPE_FILEASSET) {
            moveInodeFolder(existingContentlet, remoteInode);
        }

        Contentlet newContentlet = new Contentlet();
        newContentlet.setStructureInode(existingContentlet.getStructureInode());
        APILocator.getContentletAPI().copyProperties(newContentlet, existingContentlet.getMap());
        newContentlet.setIdentifier(newContentletIdentifier);
        newContentlet.setInode(remoteInode);

        if (structureTypeId == Structure.STRUCTURE_TYPE_FILEASSET) {
            // Get binary file from
            try {
                newContentlet.setBinary(FileAssetAPI.BINARY_FIELD, APILocator.getFileAssetAPI()
                        .fromContentlet(newContentlet).getFileAsset());
            } catch (IOException e) {
                throw new DotContentletStateException("Error getting file from the new location.",
                        e);
            }
        }

        // Add new contentlet to lucene index
        APILocator.getContentletIndexAPI().addContentToIndex(newContentlet);

        return newContentlet;
    }

    /**
     * Get inode folder from a contentlet
     * 
     * @param contentletAssetFile
     * @return file object that contains information at the inode
     */
    private File getInodeFolder(Contentlet contentletAssetFile) {
        File inodeFolder = null;

        try {
            File fileAsset = APILocator.getFileAssetAPI().fromContentlet(contentletAssetFile)
                    .getFileAsset();

            // We are going to copy the inode folder and copy it to the new
            // location. Example:
            // assets/6/6/660be9f9-2503-4e55-b3b4-57220782e717/fileAsset/binaryFile.jpg
            File fileAssetFolder = fileAsset.getParentFile();
            inodeFolder = fileAssetFolder.getParentFile();
        } catch (DotStateException e) {
            Logger.error(this, "Problem retrieving inode folder.", e);
        }

        return inodeFolder;
    }

    /**
     * Move inode folder to a new location
     * 
     * @param oldContentlet
     * @param newInode
     * @throws DotDataException
     * @throws IOException 
     */
    private void moveInodeFolder(Contentlet oldContentlet, final String newInode)
            throws DotDataException {

        // We need to copy files and folder from inode folder
        File currentInodeFolder = getInodeFolder(oldContentlet);
        if (currentInodeFolder == null) {
            Logger.error(this,
                    "We cannot move file assets because inode=[" + oldContentlet.getInode()
                            + "] folder doesn't exists.");
            return;
        }

        final String folderTree = new StringBuilder(String.valueOf(newInode.charAt(0)))
                .append(File.separator).append(String.valueOf(newInode.charAt(1)))
                .append(File.separator).append(newInode).toString();
        final String realAssetsPath = APILocator.getFileAssetAPI().getRealAssetsRootPath();
        File newInodeFolder = new File(realAssetsPath + File.separator + folderTree);

        if (!newInodeFolder.exists()) {
            if (newInodeFolder.mkdirs()) {
                try {
                    FileUtil.copyDirectory(currentInodeFolder, newInodeFolder);
                    FileUtil.deltree(currentInodeFolder, true);
                    Logger.info(this, "Relocated file assets from inode=[" + oldContentlet.getInode()
                        + "] to inode = [" + newInode + "]");
                }catch(IOException e){
                    throw new DotDataException(e.getMessage(),e);
                }
                return;
            }
        }

        final String errorMsg = "Error while copying the assets to the new folder.  Info: old inode=["
                + oldContentlet.getInode() + "] - new inode=[" + newInode + "]";
        Logger.error(this, errorMsg);
        throw new DotStateException(errorMsg);
    }
}

