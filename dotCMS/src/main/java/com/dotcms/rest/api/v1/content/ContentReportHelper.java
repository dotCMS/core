package com.dotcms.rest.api.v1.content;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.pagination.ContentReportPaginator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This helper class provides utility methods to generate Content Reports for different dotCMS
 * objects.
 * <p> Each dotCMS object may have its own way to generate the Content Report. For instance, the
 * report for a Site uses the Content Type API, whereas the report for a Folder uses the Folder API
 * to retrieve the required data. You just need to make sure that the implementation you add for a
 * specific dotCMS object transforms its data into the expected {@link ContentReportView}
 * object.</p>
 *
 * @author Jose Castro
 * @since Mar 7th, 2024
 */
public class ContentReportHelper {

    final ContentTypeAPI contentTypeAPI;
    private final Lazy<HostAPI> siteAPI = Lazy.of(APILocator::getHostAPI);
    private final Lazy<FolderAPI> folderAPI = Lazy.of(APILocator::getFolderAPI);

    /**
     * Default class constructor. Specifying a User is required in order to access the Content Type
     * API.
     *
     * @param user The {@link User} accessing this Helper.
     */
    public ContentReportHelper(final User user) {
        this.contentTypeAPI = APILocator.getContentTypeAPI(user, false);
    }

    /**
     * Generates the Content Report for a specific Site.
     *
     * @param params The {@link ContentReportParams} object with the filtering criteria used to
     *               generate the report.
     *
     * @return The list of the {@link ContentReportView} objects that make up the report.
     *
     * @throws DotDataException     An error occurred when interacting with the database.
     * @throws DotSecurityException The specified User does not have the necessary permissions to
     *                              perform this action.
     */
    public List<ContentReportView> generateSiteContentReport(final ContentReportParams params) throws DotDataException, DotSecurityException {
        final String siteId = params.extraParam(ContentReportPaginator.SITE_PARAM);
        final Host site = this.siteAPI.get().find(siteId, params.user(), false);
        if (null == site || UtilMethods.isNotSet(site.getIdentifier())) {
            return List.of();
        }
        final String orderByParam = SQLUtil.getOrderByAndDirectionSql(params.orderBy(),
                params.orderDirection());
        final List<ContentType> contentTypes = this.contentTypeAPI.search(params.filter(),
                BaseContentType.ANY, orderByParam, params.perPage(), params.page(),
                site.getIdentifier());
        return this.siteReportToView(contentTypes);
    }

    /**
     * Generates the Content Report for a specific Folder.
     *
     * @param params The {@link ContentReportParams} object with the filtering criteria used to
     *               generate the report.
     *
     * @return The list of the {@link ContentReportView} objects that make up the report.
     *
     * @throws DotDataException     An error occurred when interacting with the database.
     * @throws DotSecurityException The specified User does not have the necessary permissions to
     *                              perform this action.
     */
    public List<ContentReportView> generateFolderContentReport(final ContentReportParams params) throws DotDataException, DotSecurityException {
        final User user = params.user();
        final String folder = params.extraParam(ContentReportPaginator.FOLDER_PARAM);
        final String site = params.extraParam(ContentReportPaginator.SITE_PARAM);
        final Optional<Folder> folderOpt = this.resolveFolder(folder, site, user);
        if (folderOpt.isEmpty()) {
            return List.of();
        }
        final List<Map<String, Object>> contentReport =
                this.folderAPI.get().getContentReport(folderOpt.get(), params.orderBy(),
                        params.orderDirection().toString(), params.perPage(), params.page(), user);
        return this.folderReportToView(contentReport);
    }

    /**
     * Takes the Identifier or path of a Folder, and returns the actual Folder object it represents.
     * It's worth noting that specifying the Site ID or Key is required when you pass down a folder
     * path. If you pass down the Folder Identifier instead, then it is not necessary.
     *
     * @param folderIdOrPath The Identifier or path of the Folder to resolve.
     * @param siteIdOrKey    The Identifier or Key of the Site where the Folder lives.
     * @param user           The {@link User} calling this method.
     *
     * @return The resolved {@link Folder} object.
     *
     * @throws DotDataException     An error occurred when interacting with the database.
     * @throws DotSecurityException The specified User does not have the necessary permissions to
     *                              perform this action.
     */
    public Optional<Folder> resolveFolder(final String folderIdOrPath, final String siteIdOrKey,
                                          final User user) throws DotDataException,
            DotSecurityException {
        Optional<Folder> folderOpt = Optional.empty();
        if (UUIDUtil.isUUID(folderIdOrPath) || NumberUtils.isParsable(folderIdOrPath)) {
            folderOpt = Optional.ofNullable(this.folderAPI.get().find(folderIdOrPath, user, false));
        } else {
            final Optional<Host> siteOpt = this.siteAPI.get().findByIdOrKey(siteIdOrKey, user,
                    false);
            if (siteOpt.isPresent()) {
                final String siteId = siteOpt.get().getIdentifier();
                folderOpt = Optional.ofNullable(this.folderAPI.get().findFolderByPath(folderIdOrPath,
                        siteId, user, false));
            } else {
                Logger.warn(this, String.format("Site '%s' was not found", siteIdOrKey));
            }
        }
        return folderOpt;
    }

    /**
     * Generates the final Content Report for a specific Site. It's composed of several
     * {@link ContentReportView} objects, each one representing a different Content Type and the
     * total number of its contents.
     *
     * @param contentTypes The list of {@link ContentType} objects to analyze.
     *
     * @return The list of the {@link ContentReportView} objects that make up the report.
     */
    private List<ContentReportView> siteReportToView(final List<ContentType> contentTypes) {
        if (UtilMethods.isNotSet(contentTypes)) {
            return List.of();
        }
        Map<String, Long> entriesByContentTypes = null;
        try {
            entriesByContentTypes = this.contentTypeAPI.getEntriesByContentTypes();
        } catch (final DotStateException | DotDataException e) {
            final String errorMsg = String.format("Error trying to retrieve total entries in all Content Types: " +
                    "%s", ExceptionUtil.getErrorMessage(e));
            Logger.error(this, errorMsg);
            Logger.debug(this, e, () -> errorMsg);
        }
        final Map<String, Long> finalEntriesByContentTypes = null != entriesByContentTypes ?
                entriesByContentTypes : Map.of();
        return contentTypes.stream().map(contentType -> {

            final ContentReportView.Builder builder = new ContentReportView.Builder();
            builder.contentTypeName(contentType.name());
            builder.entries(finalEntriesByContentTypes.getOrDefault(Objects.requireNonNull(contentType.variable()).toLowerCase(), 0L));
            return builder.build();

        }).collect(Collectors.toList());
    }

    /**
     * Generates the final Content Report for a specific Folder. It's composed of several
     * {@link ContentReportView} objects, each one representing a different Content Type and the
     * total number of its contents.
     *
     * @param contentReport A list of Maps, each one representing a different Content Type and the
     *                      number of contents for each of them.
     *
     * @return The list of the {@link ContentReportView} objects that make up the report.
     */
    private List<ContentReportView> folderReportToView(final List<Map<String, Object>> contentReport) {
        if (UtilMethods.isNotSet(contentReport)) {
            return List.of();
        }
        return contentReport.stream().map(contentTypeData -> {

            final ContentReportView.Builder builder = new ContentReportView.Builder();
            builder.contentTypeName((String) contentTypeData.get("contentTypeName"));
            builder.entries((long) contentTypeData.get("entries"));
            return builder.build();

        }).collect(Collectors.toList());
    }

}
