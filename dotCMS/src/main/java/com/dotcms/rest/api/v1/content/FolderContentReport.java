package com.dotcms.rest.api.v1.content;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.util.pagination.ContentReportPaginator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
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

import static com.liferay.util.StringPool.FORWARD_SLASH;

/**
 * This implementation of the {@link ContentReport} class provides the Content Report for any
 * Folder in the dotCMS repository. This Helper has the ability to scan all sub-folders in all
 * levels under the specified "parent" folder.
 *
 * @author Jose Castro
 * @since Mar 15th, 2024
 */
public class FolderContentReport implements ContentReport {

    final User user;
    final ContentTypeAPI contentTypeAPI;
    final Lazy<HostAPI> siteAPI = Lazy.of(APILocator::getHostAPI);
    final Lazy<FolderAPI> folderAPI = Lazy.of(APILocator::getFolderAPI);

    /**
     * Creates a new instance of this Helper.
     *
     * @param user The {@link User} that will access the data provided by this Helper.
     */
    public FolderContentReport(final User user) {
        this.user = user;
        this.contentTypeAPI = APILocator.getContentTypeAPI(user, false);
    }

    @Override
    public List<ContentReportView> generateContentReport(final ContentReportParams params) throws DotDataException, DotSecurityException {
        final String folder = params.extraParam(ContentReportPaginator.FOLDER_PARAM);
        final String siteId = params.extraParam(ContentReportPaginator.SITE_PARAM);
        if (Objects.nonNull(siteId)) {
            final Host site = this.siteAPI.get().find(siteId, params.user(), false);
            if (null == site || UtilMethods.isNotSet(site.getIdentifier())) {
                throw new DoesNotExistException("The site with the given ID does not exist: " + siteId);
            }
        }
        final Optional<Folder> folderOpt = this.resolveFolder(folder, siteId, user);
        if (folderOpt.isEmpty()) {
            throw new DoesNotExistException("The folder with the given ID or path does not exist: " + folder);
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
                final String folderPath = '/' == folderIdOrPath.charAt(0) ? folderIdOrPath :
                        FORWARD_SLASH + folderIdOrPath;
                folderOpt = Optional.ofNullable(this.folderAPI.get().findFolderByPath(folderPath,
                        siteId, user, false));
            } else {
                Logger.error(this, String.format("Folder '%s' under Site '%s' was not found",
                        folderIdOrPath, siteIdOrKey));
            }
        }
        return folderOpt;
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
