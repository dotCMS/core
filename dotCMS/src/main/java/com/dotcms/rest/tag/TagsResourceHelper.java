package com.dotcms.rest.tag;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.TagResource;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.tag.business.GenericTagException;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides common-use or utility methods for the {@link com.dotcms.rest.api.v2.tags.TagResource}
 * class and the deprecated {@link TagResource} class.
 *
 * @author Fabrizzio Araya
 * @since Apr 25th, 2022
 */
public class TagsResourceHelper {

    private final TagAPI tagAPI;
    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;
    private final MultiPartUtils multiPartUtils = new MultiPartUtils();

    /**
     * constructor
     * @param tagAPI
     * @param hostAPI
     */
    public TagsResourceHelper(final TagAPI tagAPI, final HostAPI hostAPI, final FolderAPI folderAPI) {
        this.tagAPI = tagAPI;
        this.hostAPI = hostAPI;
        this.folderAPI = folderAPI;
    }

    /**
     * Verifies that the provided Site or Folder ID points to an existing Site or Folder. If no
     * value is provided, the currently selected Site in the HTTP Request will be returned instead.
     *
     * @param siteOrFolderId The Site or Folder ID to verify.
     * @param request        The current instance of the {@link HttpServletRequest}.
     * @param user           The {@link User} performing this action.
     *
     * @return The Site ID to use.
     *
     * @throws BadRequestException If the provided Site or Folder ID is not valid.
     */
    public String getSiteId (final String siteOrFolderId, final HttpServletRequest request, final User user) {
        String siteId = siteOrFolderId;
        if (UtilMethods.isSet(siteOrFolderId)) {
            final boolean frontEndRequest = user != null && user.isFrontendUser();
            Host siteOrFolder = Try.of(()->hostAPI.find(siteOrFolderId, user, frontEndRequest)).getOrNull();

            if ((!UtilMethods.isSet(siteOrFolder) || !UtilMethods.isSet(siteOrFolder.getInode()))
                    && UtilMethods.isSet(siteOrFolderId)) {
                siteOrFolder = Try.of(()->folderAPI
                        .find(siteOrFolderId, user, frontEndRequest).getHost()).getOrNull();
                if (siteOrFolder != null) {
                    siteId = siteOrFolder.getIdentifier();
                }
            }

            if(null == siteOrFolder || UtilMethods.isNotSet(siteOrFolder.getIdentifier())){
               throw new BadRequestException(String.format("Site or Folder ID '%s' isn't valid.",siteOrFolderId));
            }
        } else {
            final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
            if (null != currentHost) {
                return currentHost.getIdentifier();
            }
        }
        return siteId;
    }

    /**
     * Search tags method
     * @param tagName
     * @param siteId
     * @return
     */
    public List<Tag> searchTagsInternal(final String tagName, final String siteId) {
        List<Tag> tags;
        try {
            tags = tagAPI.getSuggestedTag(tagName, siteId);
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        }
        return tags;
    }

    public List<Tag> getTagsInternal() {
        try {
            return tagAPI.getAllTags();
        } catch (DotDataException e) {
            throw new BadRequestException(e, e.getMessage());
        }
    }


    /**
     * File Consumer interface
     * @param <T>
     */
    @FunctionalInterface
    interface FileConsumer<T> {

        /**
         * Whatever need to happen with the file and the multipart.Should be handled here
         */
        T apply(File file, Map<String, Object> bodyMultipart)
                throws DotDataException, IOException, DotSecurityException;
    }

    /**
     * Multipart common process function whatever specific needs be done in between has to take
     * place in the fileConsumer
     */
    private void processMultipart(final FormDataMultiPart multipart,
            final FileConsumer<File> consumer)
            throws IOException, DotDataException, JSONException, DotSecurityException {
        final List<File> files = multiPartUtils.getBinariesFromMultipart(multipart);
        try {
            if (!UtilMethods.isSet(files)) {
                throw new DotDataException(
                        "Unable to extract any files from multi-part request.");
            }

            final Map<String, Object> bodyMapFromMultipart = multiPartUtils
                    .getBodyMapFromMultipart(multipart);
            for (final File file : files) {
                try {
                    if (0 == file.length()) {
                        throw new IllegalArgumentException("Zero length file.");
                    }
                    consumer.apply(file, bodyMapFromMultipart);
                } finally {
                    file.delete();
                }
            }
        } finally {
            removeTempFolder(files.get(0).getParentFile());
        }
    }

    /**
     * clean up routine
     * @param parentFolder
     */
    private void removeTempFolder(final File parentFolder) {
        final String parentFolderName = parentFolder.getName();
        if (parentFolder.isDirectory() && parentFolderName.startsWith("tmp_upload")) {
            if (parentFolder.delete()) {
                Logger.info(TagsResourceHelper.class,
                        String.format(" tmp upload directory `%s` removed successfully. ",
                                parentFolder.getAbsolutePath()));
            } else {
                Logger.info(TagsResourceHelper.class,
                        String.format(" Unable to remove tmp upload directory `%s`. ",
                                parentFolder.getAbsolutePath()));
            }
        }
    }

    /**
     *  import tags entry point
     * @param multipart
     * @param user
     * @param request
     * @throws IOException
     * @throws DotDataException
     * @throws JSONException
     * @throws DotSecurityException
     */
    public void importTags(final FormDataMultiPart multipart, final User user,
            final HttpServletRequest request)
            throws IOException, DotDataException, JSONException, DotSecurityException {
        processMultipart(multipart, (file, bodyMultipart) -> importTags(file, user, request));
    }

    /**
     * internal import tags
     * @param inputFile
     * @param user
     * @param request
     * @return
     * @throws IOException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private File importTags(final File inputFile, final User user, final HttpServletRequest request)
            throws IOException, DotDataException, DotSecurityException {
        int count = 0;
        final byte[] bytes = Files.readAllBytes(inputFile.toPath());
        try (final BufferedReader reader = new BufferedReader(
                new StringReader(new String(bytes)))) {
            String str;
            while ((str = reader.readLine()) != null) {
                final String[] tokens = str.split(",");
                if (tokens.length != 2 || tokens[0].isEmpty()) {
                    Logger.error(TagsResourceHelper.class, String.format("Tag in line %d cannot " +
                            "be imported because the tag_name or the host_id is empty. Trying " +
                            "with the next Tag...", count + 1));
                    continue;
                }
                String tagName = tokens[0];
                String siteId = tokens[1];
                if (isHeaderLine(tagName, siteId)) {
                    continue;
                }
                tagName = tagName.replaceAll("['\"]", "");
                siteId = siteId.replaceAll("['\"]", "");
                try {
                    final String validateSite = getValidateSite(siteId, user, request);
                    final Tag tag = tagAPI
                            .getTagAndCreate(tagName, StringPool.BLANK, validateSite);
                    if(null != tag){
                       count++;
                    }
                } catch (final GenericTagException e) {
                    Logger.error(TagsResourceHelper.class, String.format(
                            "Tag '%s' under Site ID '%s' cannot be imported: %s . Trying with the next Tag...",
                            tagName, siteId, ExceptionUtil.getErrorMessage(e)));
                }
            }
        }
        final int finalCount = count;
        Logger.debug(TagsResourceHelper.class, String.format("Tag import has finished. A total of %d Tags were created successfully.",finalCount));
        return inputFile;
    }

    /**
     * tell me if we're looking at the title line on thr cvs file
     * @param tagName
     * @param siteId
     * @return
     */
    private boolean isHeaderLine(String tagName, String siteId) {
        return tagName.toLowerCase().contains("tag name") && siteId.toLowerCase()
                .contains("host id");
    }

    /**
     * This does return a valid siteID if the passed param does not belong to any valid site,
     * System-host gets returned instead
     */
    public String getValidateSite(final String siteId, final User user,
            final HttpServletRequest request) {
        String validSite;
        final Host host = Try.of(() -> hostAPI.find(siteId, user, true)).getOrNull();
        if (host == null || UtilMethods.isNotSet(host.getIdentifier())) {
            validSite = fallbackSiteId(request);
            Logger.warn(TagResource.class, () -> String
                    .format("siteId `%s` isn't valid, DEFAULTING to siteId `%s`.",
                            siteId,
                            validSite));
        } else {
            validSite = host.getIdentifier();
        }
        return validSite;
    }


    /**
     * When no siteId is provided we should fallback to currently selected site and if that fails
     * too fallback to System-Host
     */
    private String fallbackSiteId(final HttpServletRequest request) {
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        return host != null ? host.getIdentifier() : Host.SYSTEM_HOST;
    }

    /**
     * Takes a regular Tag and transform it into a RestTag representation
     */
    public static Map<String, RestTag> toRestTagMap(final Tag... tags) {
        return toRestTagMap(Arrays.asList(tags));
    }

    /**
     * Takes a regular Tag and transform it into a RestTag representation
     */
    public static Map<String, RestTag> toRestTagMap(final List<Tag> tags) {
        final TagTransform transform = new TagTransform();
        return tags.stream()
                .collect(Collectors.toMap(Tag::getTagName, transform::appToRest,
                        (restTag, restTag2) -> restTag));
    }

    /**
     * Takes a single Tag and transforms it into a RestTag representation
     */
    public static RestTag toRestTag(final Tag tag) {
        final TagTransform transform = new TagTransform();
        return transform.appToRest(tag);
    }

}
