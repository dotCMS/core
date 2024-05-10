package com.dotmarketing.portlets.templates.business;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableSet;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ParseErrorException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dotmarketing.util.Constants.TEMPLATE_FOLDER_PATH;
import static com.dotmarketing.util.Constants.THEME_FOLDER_PATH;
import static com.dotmarketing.util.StringUtils.builder;

/**
 * This util is in charge of handling the creation of the FileAsset template based on the folder and their contains.
 * @author jsanca
 */
public class FileAssetTemplateUtil {

    private static final String TITLE                = "title";
    private static final String FRIENDLY_NAME = "friendly_name";
    private static final String IMAGE                = "image";
    private static final String THEME                = "theme";
    private static final String HOST_INDICATOR       = "//";

    private static String [] DEFAULT_META_DATA_NAMES_ARRAY = new String[] { TITLE, FRIENDLY_NAME,THEME,IMAGE };

    public static final String LAYOUT             = "layout.json";
    public static final String BODY               = "body.vtl";
    public static final String TEMPLATE_META_INFO = "properties.vtl";

    private static class SingletonHolder {
        private static final FileAssetTemplateUtil INSTANCE = new FileAssetTemplateUtil();
    }
    /**
     * Get the instance.
     * @return FileAssetTemplateUtil
     */
    public static FileAssetTemplateUtil getInstance() {

        return FileAssetTemplateUtil.SingletonHolder.INSTANCE;
    } // getInstance.

    public boolean isDataBaseTemplateId(final String templateIdentifier) {

        boolean isIdentifier = false;
        isIdentifier |= UUIDUtil.isUUID(templateIdentifier);

        if (!isIdentifier) {
            try {
                APILocator.getShortyAPI().validShorty(templateIdentifier);
                isIdentifier = true;
            } catch (Exception e) {
                isIdentifier = false;
            }
        }

        return isIdentifier;
    }

    public Host getHost (final String path) throws DotSecurityException, DotDataException {
        return this.getHostFromHostname(this.getHostName(path));
    }

    public Host getHostFromHostname (final String hostname) throws DotSecurityException, DotDataException {
        return null == hostname?
                APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false) :
                APILocator.getHostAPI().resolveHostName(hostname, APILocator.systemUser(), false);
    }

    final List<String> pageModePrefixList = Stream.of(PageMode.values())
            .map(pageMode -> String.format("/%s/", pageMode.name()))
            .collect(Collectors.toList());


    //demo.dotcms.com/application/templates/test/
    public String getHostName(final String path) {
        try {
            String tmp = path;

            for (final String prefix : pageModePrefixList) {
                if (tmp.startsWith(prefix)) {
                    tmp = tmp.substring(prefix.length());
                    break;
                }
            }

            tmp = tmp.replaceAll(HOST_INDICATOR, StringPool.BLANK);
            tmp = tmp.substring(0, tmp.indexOf(TEMPLATE_FOLDER_PATH));
            final String finalString = UtilMethods.isSet(tmp) ? tmp : StringPool.NULL;
            Logger.debug(FileAssetTemplateUtil.class,
                    () -> String.format(" extracted hostName `%s`", finalString));

            return UtilMethods.isSet(tmp) ? tmp : null;
        } catch (Exception e) {
            Logger.error(FileAssetTemplateUtil.class, String.format(
                    "An error occurred while extracting host name from path `%s` defaulting to systemHost ",
                    path), e);
            return null;
        }
    }

    /**
     * Retrieve the site name from the path for a theme
     * @param path String
     * @return String
     */
    public String getSiteNameForTheme(final String path) {
        try {
            String tmp = path;

            for (final String prefix : pageModePrefixList) {
                if (tmp.startsWith(prefix)) {
                    tmp = tmp.substring(prefix.length());
                    break;
                }
            }

            tmp = tmp.replaceAll(HOST_INDICATOR, StringPool.BLANK);
            tmp = tmp.substring(0, tmp.indexOf(THEME_FOLDER_PATH));
            final String finalString = UtilMethods.isSet(tmp) ? tmp : StringPool.NULL;
            Logger.debug(FileAssetTemplateUtil.class,
                    () -> String.format(" extracted hostName `%s`", finalString));

            return UtilMethods.isSet(tmp) ? tmp : null;
        } catch (Exception e) {
            Logger.error(FileAssetTemplateUtil.class, String.format(
                    "An error occurred while extracting host name from path `%s`",
                    path), e);
            return null;
        }
    }

    /**
     * Remove the hostname from the fullPath (if it has the host) (must be not null)
     * @param hostname {@link String} host name to remove (must be not null)
     * @param fullPath {@link String} full path, could be relative or full (if full, the host will be removed)
     * @return returns the relative path
     */
    public String getPathFromFullPath(final String hostname, final String fullPath) {

        final int indexOf = fullPath.indexOf(hostname);

        return -1 != indexOf? fullPath.substring(indexOf + hostname.length()): fullPath;
    }

    public String getRelativePath(final String path) {

        final String hostName = this.getHostName(path);
        return this.getPathFromFullPath(hostName, path);
    }

    /**
     * Return true if path is a Template full path, otherwise return false
     * @return
     */
    public boolean isFullPath(final String path) {
        return path != null && path.startsWith(HOST_INDICATOR);
    }

    public boolean isFolderAssetTemplateId(final String templatePath) {

        return UtilMethods.isSet(templatePath) && templatePath.contains(TEMPLATE_FOLDER_PATH);
    }

    /**
     * Based on a host, folder and collection of asset, creates a fs template by convention.
     * @param host {@link Host} the host of the Template
     * @param templateFolder {@link Folder} this folder represents the Template
     * @param assets {@link List} of {@link FileAsset} has all meta info such as properties.vtl, body.vtl, layout.vtl.
     * @return Template
     * @throws DotDataException
     */
    public Template fromAssets(final Host host, final Folder templateFolder,
            final List<FileAsset> assets) throws DotDataException {

        final FileAssetTemplate template   = new FileAssetTemplate();
        Optional<String> body              = Optional.empty();
        Optional<String> layout            = Optional.empty();
        Optional<FileAsset> bodyAsset      = Optional.empty();
        Optional<FileAsset> layoutAsset    = Optional.empty();
        Optional<String> templateMetaInfo  = Optional.empty();
        FileAsset metaInfoFileAsset                 =  null;

        for (final FileAsset fileAsset : assets) {

            if (this.isBody(fileAsset)) {

                bodyAsset = Optional.of(fileAsset);
                body      = Optional.of(this.toString(fileAsset)); continue;
            }

            if (this.isTemplateMetaInfo(fileAsset)) {

                metaInfoFileAsset = fileAsset;
                templateMetaInfo = Optional.of(this.toString(fileAsset)); continue;
            }

            if (this.isLayout(fileAsset)) {

                layoutAsset = Optional.of(fileAsset);
                layout = Optional.of(this.toString(fileAsset)); continue;
            }
        }

        if (null == metaInfoFileAsset) {

            throw new NotFoundInDbException("On getting the template by folder, the folder: " + templateFolder.getPath() +
                    " is not valid, it must be under: " + TEMPLATE_FOLDER_PATH + " and must have a child file asset called: " +
                    Constants.TEMPLATE_META_INFO_FILE_NAME + " and one or more vtl assets to backup the supported contents." );
        }

        this.setTemplateData(host, templateFolder, metaInfoFileAsset, template,
                body, layout, bodyAsset, layoutAsset, templateMetaInfo.get());

        return template;
    }

    private boolean isLayout(final FileAsset fileAsset) {
        return UtilMethods.isSet(fileAsset.getFileName()) && LAYOUT.equalsIgnoreCase(fileAsset.getFileName());
    }

    private void setTemplateData(final Host                host,
            final Folder              templateFolder,
            final FileAsset           metaInfoFileAsset,
            final FileAssetTemplate   template,
            final Optional<String>    body,
            final Optional<String>    layout,
            final Optional<FileAsset> bodyAsset,
            final Optional<FileAsset> layoutAsset,
            final String              templateMetaInfo) {

        template.setIdentifier (buildPath(host,templateFolder,true));
        template.setInode      (metaInfoFileAsset.getInode());//inode of the template is the inode of the properties.vtl
        template.setOwner      (metaInfoFileAsset.getOwner());
        template.setIDate      (metaInfoFileAsset.getIDate());
        template.setModDate    (metaInfoFileAsset.getModDate());
        template.setModUser    (metaInfoFileAsset.getModUser());
        template.setShowOnMenu (true);
        template.setSortOrder  (templateFolder.getSortOrder());
        template.setTitle      (templateFolder.getTitle());
        template.setLanguage(metaInfoFileAsset.getLanguageId());
        template.setFriendlyName((String)metaInfoFileAsset.getMap().getOrDefault(FRIENDLY_NAME, template.getTitle()));
        template.setPath(this.buildPath(host, templateFolder, false));
        template.setHost(host);

        body.ifPresent (template::setBody);
        layout.ifPresent(template::setDrawedBody);
        layout.ifPresent(l -> template.setDrawed(true));
        this.setMetaInfo (templateMetaInfo, template);
        if(UtilMethods.isSet(template.getTheme())) {
            template.setThemeName(Try.of(()->APILocator.getThemeAPI()
                    .findThemeById(template.getTheme(), APILocator.systemUser(), false)).get().getName());
        }

        bodyAsset.ifPresent  (template::setBodyAsset);
        layoutAsset.ifPresent(template::setLayoutAsset);
    }

    private String buildPath(final Host host, final Folder templateFolder, final boolean includeHostOnPath) {

        return includeHostOnPath?
                builder(HOST_INDICATOR, host.getHostname(), templateFolder.getPath()).toString():
                templateFolder.getPath();
    }

    private boolean isTemplateMetaInfo(final FileAsset fileAsset) {

        return UtilMethods.isSet(fileAsset.getFileName()) && TEMPLATE_META_INFO.equalsIgnoreCase(fileAsset.getFileName())
                && fileAsset.getLanguageId() == APILocator.getLanguageAPI().getDefaultLanguage().getId();
    }

    private boolean isBody(final FileAsset fileAsset) {

        return UtilMethods.isSet(fileAsset.getFileName()) && BODY.equalsIgnoreCase(fileAsset.getFileName());
    }

    private void setMetaInfo(final String metaInfoVTLContent, final FileAssetTemplate template) {

        final Context context         = new VelocityContext();
        final StringWriter evalResult = new StringWriter();

        try {
            context.put("dotJSON", new DotJSON());
            VelocityUtil.getEngine().evaluate(context, evalResult, StringPool.BLANK, metaInfoVTLContent);
            final DotJSON dotJSON = (DotJSON) context.get("dotJSON");

            final Map<String, Object> map = dotJSON.getMap();
            if (UtilMethods.isSet(map)) {

                template.setTitle((String) map.getOrDefault(TITLE, template.getTitle()));
                template.setFriendlyName((String) map.getOrDefault(FRIENDLY_NAME, template.getFriendlyName()));
                template.setImage((String) map.getOrDefault(IMAGE, StringPool.BLANK));
                template.setTheme((String) map.getOrDefault(THEME, StringPool.BLANK));

                this.removeDefaultMetaDataNames(map);

                for (final Map.Entry<String, Object> entry : map.entrySet()) {

                    template.addMetaData(entry.getKey(), entry.getValue());
                }
            }
        } catch(ParseErrorException e) {
            Logger.error(this, "Parsing error, setting the meta data of the template: " + template.getName() +
                    ", velocity code: " + metaInfoVTLContent, e);
        }
    }

    private void removeDefaultMetaDataNames(Map<String, Object> map) {
        for (final String metaDataName : DEFAULT_META_DATA_NAMES_ARRAY) {

            if (map.containsKey(metaDataName)) {
                map.remove(metaDataName);
            }
        }
    }

    private String toString (final FileAsset fileAsset) {

        try (InputStream fileAssetStream = fileAsset.getInputStream()) {
            return IOUtils.toString(fileAssetStream,
                    UtilMethods.getCharsetConfiguration());
        } catch (IOException e) {
            return StringPool.BLANK;
        }
    }

    public Set<FileAsset> findTemplateAssets(final Folder templateFolder)
            throws DotDataException, DotSecurityException {
        return ImmutableSet.<FileAsset>builder().addAll(APILocator.getFileAssetAPI()
                .findFileAssetsByFolder(templateFolder, null, false, APILocator.systemUser(),
                        false))
                .build();
    }


    public boolean isFileAssetTemplate(final Template template) {
        return template instanceof FileAssetTemplate;
    }

    public boolean isFileAssetTemplate(final Contentlet contentlet) {
        if (null == contentlet || !contentlet.isFileAsset()) {
            return false;
        }
        try {
            final FileAsset asset = APILocator.getFileAssetAPI().fromContentlet(contentlet);
            final Folder parentFolder = APILocator.getFolderAPI()
                    .find(asset.getFolder(), APILocator.systemUser(), false);

            return (TEMPLATE_META_INFO.equals(asset.getFileName()) && UtilMethods
                    .isSet(parentFolder.getPath()) && parentFolder.getPath()
                    .startsWith(TEMPLATE_FOLDER_PATH));
        } catch (Exception e) {
            Logger.error(FileAssetTemplateUtil.class, "Unable to determine if contentlet: "+ contentlet.getIdentifier() +" is a fileAssetTemplate ", e);
        }
        return false;
    }

    /**
     * Return the full path for a {@link FileAssetTemplate}, with the follow syntax:
     *
     * //[host name]/[File Template path]
     *
     * @param template
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public String getFullPath(final FileAssetTemplate template) {
        return getFullPath(template.getHost(), template.getPath());
    }

    public String getFullPath(final String templatePath) {
        try {
            if (isFullPath(templatePath)) {
                return templatePath;
            } else {
                final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHost();
                return getFullPath(currentHost, templatePath);
            }
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    public String getFullPath(final Host host, final String templatePath) {
        return getFullPath(host.getHostname(), templatePath);
    }

    public String getFullPath(final String hostName, final String templatePath) {
        return builder(HOST_INDICATOR, hostName, templatePath).toString();
    }

    /**
     * If the templatePath is actually a path (starts with host indicator)
     * will retrieve the folder id associated to the theme id
     * Otherwise will retrieve whatever is pass as a parameter
     * @param themePath String
     * @return String
     */
    public String getThemeIdFromPath (final String themePath)  {

        if (Objects.nonNull(themePath) && themePath.startsWith(HOST_INDICATOR)) {

            try {
                final String hostname = this.getSiteNameForTheme(themePath);
                final Host host = this.getHostFromHostname(hostname);
                final String path = this.getPathFromFullPath(hostname, themePath);
                final Folder folder = APILocator.getFolderAPI().findFolderByPath(path, host, APILocator.systemUser(), false);
                return Objects.nonNull(folder) ? folder.getInode() : themePath;
            } catch (Exception e) {

                Logger.error(this, e.getMessage());
                Logger.debug(this, e.getMessage(), e);
            }
        }

        return themePath;
    }
}
