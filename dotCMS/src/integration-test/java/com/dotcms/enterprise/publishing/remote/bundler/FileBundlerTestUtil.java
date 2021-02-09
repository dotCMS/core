package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Provide methods to get the path that should have a File into a bundle root directory according to its assets
 */
public class FileBundlerTestUtil {

    private FileBundlerTestUtil(){}

    public static File getRelationshipPath(final Relationship relationship, final File bundleRoot)
            throws DotSecurityException, DotDataException {

        final String liveWorking = hasLiveVersion(relationship.getIdentifier()) ? "live" : "working";
        Structure parent = CacheLocator.getContentTypeCache().getStructureByInode(relationship.getParentStructureInode());
        Host h = APILocator.getHostAPI().find(parent.getHost(), APILocator.systemUser(), false);

        String relationshipFilePath = bundleRoot.getPath() + File.separator
                + liveWorking + File.separator
                + h.getHostname() +File.separator + relationship.getInode() + ".relationship.xml";

        return new File(relationshipFilePath);
    }

    public static File getWorkflowFilePath(final WorkflowScheme workflowScheme, final File bundleRoot) {

        final String contentTypeFilePath = bundleRoot.getAbsolutePath() +
                File.separator + workflowScheme.getId() + ".workflow.xml";

        final File file = new File(contentTypeFilePath);
        return file;
    }

    public static File getContentTypeFilePath(final ContentType contentType, final Host host, final File bundleRoot)
            throws DotSecurityException {
        final String liveWorking = hasLiveVersion(contentType.id()) ? "live" : "working";

        final String contentTypeFilePath = bundleRoot.getAbsolutePath() +
                File.separator + liveWorking + File.separator + host.getHostname() + File.separator + contentType.inode()
                + ContentTypeBundler.CONTENT_TYPE_EXTENSION;

        final File file = new File(contentTypeFilePath);
        return file;
    }

    private static boolean hasLiveVersion(final String id) throws DotSecurityException {
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final Optional<ContentletVersionInfo> contentletVersionInfo = APILocator.getVersionableAPI().getContentletVersionInfo(id, defaultLanguage.getId());
        return contentletVersionInfo.isPresent();
    }

    public static File getHostFilePath(final Contentlet host, final File bundleRoot) throws DotSecurityException, DotDataException {

        final String uri = APILocator.getIdentifierAPI().find(host).getURI().replace("/", File.separator);

        final Host parentHost = APILocator.getHostAPI().find(host.getHost(), APILocator.systemUser(), false);

        final String liveWorking = host.isLive() ? "live" : "working";

        String hostFilePath = bundleRoot.getPath() + File.separator
                + liveWorking + File.separator
                + parentHost.getHostname() + File.separator
                + host.getLanguageId() + uri + ".host.xml";

        return new File(hostFilePath);
    }

    public static File getFolderFilePath(final Folder folder, final File bundleRoot)
            throws DotSecurityException, DotDataException {
        final User systemUser = APILocator.systemUser();
        String path = "";

        Folder parentFolder = APILocator.getFolderAPI().findParentFolder(folder, systemUser, false);

        while(parentFolder != null) {
            path = File.separator + parentFolder.getName() + path;
            parentFolder = APILocator.getFolderAPI().findParentFolder(parentFolder, systemUser, false);
        }

        String folderFilePath = bundleRoot.getPath() +
                File.separator + "ROOT" + path +
                File.separator +
                folder.getIdentifier() + ".folder.xml";

        return new File(folderFilePath);
    }

    public static File getCategoryPath(final Category category, final File bundleRoot)
            throws DotSecurityException, DotDataException {

        final String liveWorking = hasLiveVersion(category.getIdentifier()) ? "live" : "working";

        final String categoryFilePath = bundleRoot.getPath()
                + File.separator
                + liveWorking + File.separator
                + APILocator.getHostAPI().findSystemHost().getHostname()
                + File.separator
                + category.getInode() + ".category.dpc.xml";

        return new File(categoryFilePath);
    }

    public static File getTemplatePath(final Template template, final File bundleRoot) throws DotDataException, DotSecurityException {
        final Identifier identifier = APILocator.getIdentifierAPI().find(template.getIdentifier());
        final Host host = getHost(template.getIdentifier());

        final String liveWorking = template.isLive() ? "live" : "working";

        final String templateFilePath = bundleRoot.getPath() + File.separator
                + liveWorking + File.separator
                + host.getHostname() + identifier.getURI().replace("/", File.separator) + ".template.xml";

        return new File(templateFilePath);
    }

    public static File getContainerPath(final Container container, final File bundleRoot)
            throws DotSecurityException, DotDataException {

        final Identifier identifier = APILocator.getIdentifierAPI().find(container.getIdentifier());

        final Host host = getHost(container.getIdentifier());

        final String liveWorking = container.isLive() ? "live" : "working";

        String containerFilePath = bundleRoot.getPath() + File.separator
                + liveWorking + File.separator
                + host.getHostname() + identifier.getURI() + ".container.xml";

        return new File(containerFilePath);
    }

    public static File getLinkPath(final Link link, final File bundleRoot) throws DotDataException, DotSecurityException {

        final Host host = getHost(link.getIdentifier());

        final String liveWorking = link.isLive() ? "live" : "working";

        String linkFilePath = bundleRoot.getPath() + File.separator
                + liveWorking + File.separator
                + host.getHostname() + File.separator + link.getURI() + ".link.xml";

        return new File(linkFilePath);
    }

    public static File getContentletPath(final Contentlet contentlet, final File bundleRoot) throws DotSecurityException, DotDataException {

        final String liveWorking = contentlet.isLive() ? "live" : "working";
        final Identifier identifier = APILocator.getIdentifierAPI().find(contentlet.getIdentifier());


        String assetName = contentlet.isFileAsset() ? contentlet.getInode() : identifier.getURI().replace("/", File.separator);
        assetName = assetName.indexOf("content.") != -1 ? assetName.substring(assetName.indexOf("content.")) : assetName;

        final int countOrder = getCountOrder(bundleRoot, assetName, contentlet.isLive());
        final Host host = getHost(contentlet.getIdentifier());

        String contentletFilePath = bundleRoot.getPath() + File.separator
                + liveWorking + File.separator
                + host.getHostname() + File.separator +
                + contentlet.getLanguageId() + File.separator + countOrder + "-" + assetName + ".content.xml";

        return new File(contentletFilePath);
    }

    private static int getCountOrder(final File bundleRoot, final String inode, final boolean live) {
        final List<File> files = FileUtil.listFilesRecursively(bundleRoot)
                .stream()
                .filter(file -> file.getName().endsWith(".content.xml"))
                .collect(Collectors.toList());

        final String liveWorking = live ? "live" : "working";

        for (final File file : files) {
            if (file.getName().contains(String.format("%s.content.xml", inode)) &&
                    file.getAbsolutePath().contains(liveWorking)) {
                return file.getName().charAt(0) - 48;
            }
        }

        return -1;
    }

    private static Host getHost(final String identifierStr) throws DotDataException, DotSecurityException {
        final Identifier identifier = APILocator.getIdentifierAPI().find(identifierStr);
        return APILocator.getHostAPI().find(identifier.getHostId(), APILocator.systemUser(), false);
    }

    public static File getWorkflowTaskFilePath(final Contentlet contentlet, final File bundleRoot) throws DotSecurityException, DotDataException {
        final String liveWorking = contentlet.isLive() ? "live" : "working";
        final Host host = getHost(contentlet.getIdentifier());

        final String workflowtaskFilepath = bundleRoot.getPath() + File.separator + liveWorking + File.separator+
                host.getHostname() + File.separator+
                contentlet.getLanguageId() + File.separator + contentlet.getIdentifier() + ".contentworkflow.xml";

        return new File(workflowtaskFilepath);
    }

    public static File getLanguageFilePath(final Language language, final File bundleRoot) throws DotSecurityException, DotDataException {

        final Host host = APILocator.getHostAPI().findSystemHost();

        String languageFilePath = bundleRoot.getPath() + File.separator
                + "live" + File.separator
                + host.getHostname() +File.separator + language.getLanguageCode() + "_" + language.getCountryCode()
                + ".language.xml";

        return new File(languageFilePath);
    }

    public static File getLanguageVariableFilePath(final Language language, final File bundleRoot) {

        try {
            final String filePath = bundleRoot.getPath() + File.separator
                    + "live" + File.separator
                    + APILocator.getHostAPI().findSystemHost().getHostname() +File.separator + language.getId() + "properties";

            return new File(filePath);
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }
}
