package com.dotmarketing.quartz.job;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides the required information to the {@link DropOldContentVersionsJob} Quartz Job so that it
 * can decide if old and/or excessive Contentlet versions must be deleted or not.
 *
 * @author Jose Castro
 * @since Sep 20th, 2023
 */
public class DropOldContentVersionsJobHelper {

    private static final String FIND_CONTENTS_WITH_VERSIONS_GREATER_THAN = "SELECT COUNT(inode) " +
            "versions, identifier FROM contentlet " + "WHERE language_id = ? " + "GROUP BY " +
            "identifier HAVING COUNT(inode) > ? " + "ORDER BY COUNT(inode) DESC";
    private static final String FIND_CONTENT_VERSIONS_GREATER_THAN = "SELECT DISTINCT inode, c" +
            ".identifier, mod_date FROM contentlet c, contentlet_version_info cvi " + "WHERE c" +
            ".identifier = ? AND c.language_id = ? " + "AND cvi.working_inode <> c.inode AND cvi" +
            ".live_inode <> c.inode AND cvi.lang = c.language_id " + "ORDER BY mod_date DESC " +
            "OFFSET ?";
    private static final User SYSTEM_USER =
            Try.of(() -> APILocator.getUserAPI().getSystemUser()).getOrNull();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();

    /**
     * Returns a limited number of Contentlets that have more than X number of versions.
     *
     * @param versions  The number of versions that a Contentlet must have to be analyzed.
     * @param batchSize The maximum number of Contentlets to analyze in the current Job run.
     *
     * @return The limited list of Contentlet Identifiers with more than X number of versions.
     */
    @CloseDBIfOpened
    public List<String> findContentsWithTotalVersionsGreaterThan(final int versions,
                                                                 final long languageId,
                                                                 final int batchSize) {
        final DotConnect dc = new DotConnect().setSQL(FIND_CONTENTS_WITH_VERSIONS_GREATER_THAN,
                batchSize);
        dc.addParam(languageId);
        dc.addParam(versions);
        try {
            final List<Map<String, Object>> data = dc.loadObjectResults();
            if (UtilMethods.isNotSet(data)) {
                return new ArrayList<>();
            }
            return data.stream().map(content -> ((String) content.get("identifier"))).collect(Collectors.toList());
        } catch (final DotDataException e) {
            Logger.warnAndDebug(this.getClass(),
                    String.format("Failed to find Contentlets in " + "language '%s' with more " +
                            "than %d versions: %s", languageId, versions,
                            ExceptionUtil.getErrorMessage(e)), e);
            return new ArrayList<>();
        }
    }

    /**
     * Returns the list of Inodes for the specified Contentlet Identifier and language ID that
     * exceed the number of versions that will be kept in dotCMS.
     *
     * @param identifier  The ID of the Contentlet that some versions wil be deleted from.
     * @param greaterThan The number of versions that will be kept in the repository.
     *
     * @return The list of Inodes that will be deleted.
     */
    @CloseDBIfOpened
    public List<String> findContentVersionsGreaterThan(final String identifier,
                                                       final long languageId,
                                                       final int greaterThan) {
        final DotConnect dc = new DotConnect().setSQL(FIND_CONTENT_VERSIONS_GREATER_THAN);
        dc.addParam(identifier);
        dc.addParam(languageId);
        dc.addParam(greaterThan);
        try {
            return dc.loadObjectResults().stream().map(content -> (String) content.get("inode")).collect(Collectors.toList());
        } catch (final DotDataException e) {
            Logger.warnAndDebug(this.getClass(), String.format("Failed to find versions of " +
                    "Contentlet '%s' in language '%s' greater than %d: %s", identifier,
                    languageId, greaterThan, ExceptionUtil.getErrorMessage(e)), e);
            return new ArrayList<>();
        }
    }

    /**
     * Finds and delete the specified Contentlet version.
     *
     * @param inode The Inode - version - of the Contentlet that will be deleted.
     *
     * @return If the Contentlet version was deleted, returns {@code true}.
     *
     * @throws DotDataException     An error occurred when interacting with the database.
     * @throws DotSecurityException An error occurred when interacting with the database.
     */
    public boolean deleteContentVersion(final String inode) throws DotDataException,
            DotSecurityException {
        final Contentlet contentlet = this.contentletAPI.find(inode, SYSTEM_USER, false);
        if (null == contentlet) {
            Logger.warn(this, String.format("Contentlet with Inode '%s' could not be found",
                    inode));
            return false;
        }
        this.contentletAPI.deleteVersion(contentlet, SYSTEM_USER, false);
        return true;
    }

}
