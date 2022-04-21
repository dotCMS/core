package com.dotcms.rest.tag;

import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.rest.TagResource;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
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
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

/**
 * Tags Resource  helper class
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
     * makes sure we're on a valid site
     * @param siteId
     * @param request
     * @return
     */
    public String getSiteId (final String siteId, final HttpServletRequest request) {

        if (UtilMethods.isSet(siteId)) {
            final Host host = Try.of(()->hostAPI.find(siteId, APILocator.systemUser(), false)).getOrNull();
            if(null == host || UtilMethods.isNotSet(host.getIdentifier())){
               throw new BadRequestException(String.format("Given site id `%s` isn't valid.",siteId));
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
     * @param siteOrFolderId
     * @param user
     * @return
     */
    public List<Tag> searchTagsInternal(final String tagName, final String siteOrFolderId,
            final User user) {
        List<Tag> tags;

        try {
            final boolean frontEndRequest = user.isFrontendUser();
            final Host host = hostAPI.find(siteOrFolderId, user, frontEndRequest);
            String internalSiteOrFolderId = siteOrFolderId;
            if ((!UtilMethods.isSet(host) || !UtilMethods.isSet(host.getInode()))
                    && UtilMethods.isSet(siteOrFolderId)) {
                internalSiteOrFolderId = folderAPI
                        .find(siteOrFolderId, user, frontEndRequest).getHostId();
            }

            tags = tagAPI.getSuggestedTag(tagName, internalSiteOrFolderId);
        } catch (DotDataException | DotSecurityException e) {
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
        final byte[] bytes = Files.readAllBytes(inputFile.toPath());

        try (final BufferedReader reader = new BufferedReader(
                new StringReader(new String(bytes)))) {
            String str;
            while ((str = reader.readLine()) != null) {
                final String[] tokens = str.split(",");
                if (tokens.length != 2 || tokens[0].isEmpty()) {
                    Logger.error(TagsResourceHelper.class,
                            "Tag can not be imported because the tag_name or the host_id is empty, trying with the next Tag");
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
                    tagAPI.getTagAndCreate(tagName, StringPool.BLANK, validateSite);
                } catch (GenericTagException e) {
                    Logger.error(TagsResourceHelper.class, String.format(
                            "Tag (name: %s  - host ID: %s ) can not be imported because %s , trying with the next Tag",
                            tagName, siteId, e.getMessage()));
                }
            }
        }
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

}
