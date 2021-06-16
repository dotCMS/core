package com.dotcms.publisher.util.dependencies;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provide a set of util methods to be use by {@link PushPublishigDependencyProcesor}
 */
public class PushPublishigDependencyProvider {

    private User user;

    public PushPublishigDependencyProvider(final User user) {
        this.user = user;
    }

    public List<Contentlet> getContentletByLuceneQuery(final String luceneQuery)
            throws DotDataException, DotSecurityException {
        return APILocator.getContentletAPI()
                .search(luceneQuery, 0, 0, null, user, false);
    }

    public List<Template> getTemplatesByHost(final Host host)
            throws DotDataException, DotSecurityException {
        return APILocator.getTemplateAPI().findTemplatesAssignedTo(host);
    }

    public List<Rule> getRulesByHost(final Host host)
            throws DotDataException, DotSecurityException {
        return APILocator.getRulesAPI().getAllRulesByParent(host, user, false);
    }

    public List<Folder> getFoldersByHost(final Host host)
            throws DotDataException, DotSecurityException {
        return APILocator.getFolderAPI()
                .findFoldersByHost(host, user, false);
    }

    public List<Structure> getContentTypeByHost(final Host host)
            throws DotDataException, DotSecurityException {
        return StructureFactory
                .getStructuresUnderHost(host, user, false);
    }

    public List<Structure> getContentTypeByFolder(final Folder folder)
            throws DotDataException, DotSecurityException {
        return APILocator.getFolderAPI().getStructures(folder, user, false);
    }

    public List<FileAssetContainer> getFileContainersByHost(final Host host)
            throws DotDataException {
        return getContainersByHost(host).stream()
                .filter(FileAssetContainer.class::isInstance)
                .map(FileAssetContainer.class::cast)
                .collect(Collectors.toList());
    }

    public Folder getParentFolder(Folder folder) throws DotDataException, DotSecurityException {
        return APILocator.getFolderAPI().findParentFolder(folder, user, false);
    }

    public List<Link> getLinksByFolder(Folder folder)
            throws DotDataException, DotSecurityException {
        return APILocator.getMenuLinkAPI().findFolderMenuLinks(folder);
    }

    public List<Container> getContainersByHost(final Host host) throws DotDataException {
        return APILocator.getContainerAPI().findContainersUnder(host);
    }

    public Folder getThemeByTemplate(final Template template)
            throws DotDataException, DotSecurityException {

        final Folder themeFolder = APILocator.getFolderAPI()
                .find(template.getTheme(), user, false);

        return themeFolder;
    }

    public Host getHostByContainer(final Container container)
            throws DotDataException, DotSecurityException {
        return APILocator.getContainerAPI().getParentHost(container, user, false);
    }

    public Collection<Structure> getContentTypeByLiveContainer(final String containerId)
            throws DotDataException, DotSecurityException {
        return getContentTypeByContainer(containerId, true);
    }

    public Collection<Structure> getContentTypeByWorkingContainer(final String containerId)
            throws DotDataException, DotSecurityException {
        return getContentTypeByContainer(containerId, false);
    }

    public Collection<Structure> getContentTypeByContainer(final String containerId, final boolean live)
            throws DotDataException, DotSecurityException {

        final Container container = live ? APILocator.getContainerAPI()
                .getLiveContainerById(containerId, user, false)
                : APILocator.getContainerAPI()
                        .getWorkingContainerById(containerId, user, false);

        if (container == null) {
            return Collections.emptyList();
        }

        return APILocator.getContainerAPI().getContainerStructures(container).stream()
                .map(containerStructure ->
                        CacheLocator.getContentTypeCache()
                                .getStructureByInode(containerStructure.getStructureId()))
                .collect(Collectors.toSet());
    }

    public Folder getFolderById(final String id) throws DotDataException, DotSecurityException {
        return APILocator.getFolderAPI().find(id, user, false);
    }

    /**
     * Given that a FileAssetTemplate is defined by a bunch of files we need to collect the folder that enclose'em
     */
    public Folder getFileAssetTemplateRootFolder(final FileAssetTemplate fileAssetTemplate)
            throws DotDataException, DotSecurityException {
        try {
            final String path = fileAssetTemplate.getPath();
            return APILocator.getFolderAPI()
                    .findFolderByPath(path, fileAssetTemplate.getHost(), user, false);
        }catch (DotSecurityException | DotDataException e) {
            Logger.error(DependencyManager.class, "Error Collecting the Folder of the File Asset Template: " + fileAssetTemplate.getIdentifier(), e);
            return null;
        }
    }

    public Collection<WorkflowScheme> getWorkflowSchemasByContentType(final Structure structure){
        try{
            return APILocator.getWorkflowAPI().findSchemesForStruct(structure);
        } catch (DotDataException e) {
            Logger.debug(getClass(),
                    () -> "Could not get the Workflow Scheme Dependency for Structure ID: "
                            + structure.getInode());
            return null;
        }
    }

    public Folder getFolderByParentIdentifier(final Identifier identifier)
            throws DotDataException, DotSecurityException {

        return APILocator.getFolderAPI()
                .findFolderByPath(identifier.getParentPath(), identifier.getHostId(), user,
                        false);
    }

    public Host getHostById(final String hostId)
            throws DotDataException, DotSecurityException {

        return APILocator.getHostAPI().find(hostId, user, false);
    }

    public List<Contentlet> getContentletsByLink(final String linkId)
            throws DotDataException, DotSecurityException {

        final Link link = APILocator.getMenuLinkAPI()
                .findWorkingLinkById(linkId, user, false);

        if (link != null) {

            if (link.getLinkType().equals(Link.LinkType.INTERNAL.toString())) {
                final Identifier id = APILocator.getIdentifierAPI()
                        .find(link.getInternalLinkIdentifier());

                // add file/content dependencies. will also work with htmlpages as content
                if (InodeUtils.isSet(id.getInode()) && id.getAssetType()
                        .equals("contentlet")) {
                    return APILocator.getContentletAPI()
                            .search("+identifier:" + id.getId(), 0, 0, "moddate", user,
                                    false);
                }
            }
        }

        return null;
    }

    public Host getHostByTemplate(final Template template)
            throws DotDataException, DotSecurityException {
        return APILocator.getHostAPI()
                .find(APILocator.getTemplateAPI().getTemplateHost(template)
                        .getIdentifier(), user, false);
    }

    public List<Container> getContainerByTemplate(final Template template)
            throws DotDataException, DotSecurityException {
        return APILocator.getTemplateAPI()
                .getContainersInTemplate(template, user, false);
    }

    /**
     * Given that a FileAssetContainer is defined by a bunch of vtl files we need to collect the folder that enclose'em
     * @param fileAssetContainer
     * @return
     */
    public Folder getFileAssetContainerRootFolder(final FileAssetContainer fileAssetContainer) {
        try {
            final String path = fileAssetContainer.getPath();
            return APILocator.getFolderAPI()
                    .findFolderByPath(path, fileAssetContainer.getHost(), user, false);
        }catch (DotSecurityException | DotDataException e) {
            Logger.error(DependencyManager.class, e);
            return null;
        }
    }

    public List<Contentlet> getContentletsByPage(final IHTMLPage page)
            throws DotDataException, DotSecurityException {

        final Table<String, String, Set<PersonalizedContentlet>> pageMultiTrees =
                APILocator.getMultiTreeAPI().getPageMultiTrees(page, false);

        return pageMultiTrees.values().stream()
                .flatMap(personalizedContentlets -> personalizedContentlets.stream())
                .map(personalizedContentlet -> personalizedContentlet.getContentletId())
                .filter(Objects::nonNull)
                .map(contentletId -> findIdentifier(contentletId))
                .filter(Objects::nonNull)
                .flatMap(identifier -> findContentletsByIdentifier(identifier).stream())
                .collect(Collectors.toList());
    }

    public List<Rule> getRuleByPage(final IHTMLPage page)
            throws DotDataException, DotSecurityException {
        return APILocator.getRulesAPI().getAllRulesByParent(page, user, false);
    }

    private Identifier findIdentifier(final String id) {
        try {
            return APILocator.getIdentifierAPI().find(id);
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
            return null;
        }
    }

    private List<Contentlet> findContentletsByIdentifier(final Identifier identifier) {
        try {
            return APILocator.getContentletAPI()
                    .findAllVersions(identifier, false, user, false);
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public Set<IHTMLPage> getHTMLPages(Folder contFolder)
            throws DotDataException, DotSecurityException {
        final List<IHTMLPage> liveHtmlPages = APILocator.getHTMLPageAssetAPI()
                .getHTMLPages(contFolder, false, false, user, false);

        final List<IHTMLPage> workingHtmlPages = APILocator.getHTMLPageAssetAPI()
                .getHTMLPages(contFolder, true, false, user, false);

        return Stream.concat(liveHtmlPages.stream(), workingHtmlPages.stream())
                .collect(Collectors.toSet());
    }
}
