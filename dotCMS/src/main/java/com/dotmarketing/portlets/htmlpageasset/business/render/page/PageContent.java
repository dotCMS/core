package com.dotmarketing.portlets.htmlpageasset.business.render.page;


import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.business.ContainerFinderByIdOrPathStrategy;
import com.dotmarketing.portlets.containers.business.LiveContainerFinderByIdOrPathStrategyResolver;
import com.dotmarketing.portlets.containers.business.WorkingContainerFinderByIdOrPathStrategyResolver;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class PageContent {
    private Table<String, String, Set<PersonalizedContentlet>> pageContents;
    private List<Container> containers;

    public PageContent(final IHTMLPage page, final boolean liveMode)
            throws DotDataException, DotSecurityException {

        containers = new ArrayList<>();
        pageContents = HashBasedTable.create();
        final List<MultiTree> multiTrees    = APILocator.getMultiTreeAPI().getMultiTrees(page.getIdentifier());

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();

        for (final MultiTree multiTree : multiTrees) {

            final Container container   = this.getContainer(multiTree, liveMode);

            if (container == null) {
                continue;
            } else {
                containers.add(container);
            }

            final String    containerId     = multiTree.getContainerAsID();
            final String    personalization = multiTree.getPersonalization();
            Contentlet contentlet = null;

            try {
                contentlet = contentletAPI.findContentletByIdentifierAnyLanguage(multiTree.getContentlet());
            } catch (DotDataException | DotSecurityException | DotContentletStateException e) {
                Logger.debug(this.getClass(), "invalid contentlet on multitree:" + multiTree
                        + ", msg: " + e.getMessage(), e);
                Logger.warn(this.getClass(), "invalid contentlet on multitree:" + multiTree);
            }

            if (contentlet != null) {

                final Set<PersonalizedContentlet> myContents = pageContents.contains(containerId, multiTree.getRelationType())
                        ? pageContents.get(containerId, multiTree.getRelationType())
                        : new LinkedHashSet<>();

                if (container != null && myContents.size() < container.getMaxContentlets()) {

                    myContents.add(new PersonalizedContentlet(multiTree.getContentlet(), personalization));
                }

                pageContents.put(containerId, multiTree.getRelationType(), myContents);
            }
        }

        this.addEmptyContainers(page, pageContents, liveMode);
    }

    private Container getContainer(final MultiTree multiTree, final boolean liveMode)
            throws DotSecurityException, DotDataException {

        final ContainerAPI containerAPI  = APILocator.getContainerAPI();

        final String containerId     = multiTree.getContainerAsID();
        final User systemUser = APILocator.systemUser();

        try {

            return liveMode?
                    containerAPI.getLiveContainerById(containerId, systemUser, false):
                    containerAPI.getWorkingContainerById(containerId, systemUser, false);


        } catch (NotFoundInDbException e) {

            Logger.debug(this, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Traverses the {@link Template} from an HTML Page and retrieves the Containers that are currently empty, i.e.,
     * Containers that have no content in them.
     *
     * @param page         The {@link IHTMLPage} object that will be inspected.
     * @param pageContents The parts that make up the {@link IHTMLPage} object.
     * @param liveMode     If set to {@code true}, only the live version of the Containers will be retrieved. If set to
     *                     {@code} false, only the working version will be retrieved.
     *
     * @throws DotDataException     An error occurred qhen retrieving the required information from the data source.
     * @throws DotSecurityException The internal APIs are not allowed to return data for the specified user.
     */
    private void addEmptyContainers(final IHTMLPage page,
                                    final Table<String, String, Set<PersonalizedContentlet>> pageContents,
                                    final boolean liveMode)
            throws DotDataException, DotSecurityException {

        try {

            final List<ContainerUUID> containersUUID;
            final Template template =
                    APILocator.getTemplateAPI().findWorkingTemplate(page.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false);
            try {
                containersUUID = template.isDrawed()?
                        this.getDrawedLayoutContainerUUIDs(page):
                        APILocator.getTemplateAPI().getContainersUUIDFromDrawTemplateBody(template.getBody());
            } catch (final Exception e) {
                Logger.error(this, String.format("An error occurred when retrieving empty Containers from page with " +
                        "ID '%s' in liveMode '%s': %s", page.getIdentifier(), liveMode, e.getMessage()), e);
                return;
            }

            for (final ContainerUUID containerUUID : containersUUID) {

                Container container = null;
                try {
                    // this read path or id.
                    container = liveMode ? this.getLiveContainerById(containerUUID.getIdentifier(), APILocator.systemUser(), template):
                            this.getWorkingContainerById(containerUUID.getIdentifier(), APILocator.systemUser(), template);

                    if (container == null && !liveMode) {
                        continue;
                    }
                } catch (final NotFoundInDbException| DotRuntimeException e) {
                    Logger.debug(this, e.getMessage(), e);
                    continue;
                }

                if (!doesPageContentsHaveContainer(pageContents, containerUUID, container)) {
                    pageContents.put(container.getIdentifier(), containerUUID.getUUID(), new LinkedHashSet<>());
                }
            }
        } catch (final RuntimeException e) {
            Logger.error(this, String.format("An error occurred when retrieving empty Containers from page with ID " +
                    "'%s' in liveMode '%s': %s", page.getIdentifier(), liveMode, e.getMessage()), e);
        }
    }

    /**
     * Returns the list of Containers from the drawn layout of a given Template.
     *
     * @param page The {@link IHTMLPage} object using the {@link Template} which holds the Containers.
     *
     * @return The list of {@link ContainerUUID} objects.
     *
     * @throws DotSecurityException The internal APIs are not allowed to return data for the specified user.
     * @throws DotDataException     The information for the Template could not be accessed.
     */
    private List<ContainerUUID> getDrawedLayoutContainerUUIDs (final IHTMLPage page) throws DotSecurityException, DotDataException {

        final TemplateLayout layout =
                DotTemplateTool.themeLayout(page.getTemplateId(), APILocator.systemUser(), false);
        return APILocator.getTemplateAPI().getContainersUUID(layout);
    }

    /**
     * Check if a container with the same id or path (in case of {@link FileAssetContainer}), exist into pageContents.
     * Also support legacy 'LEGACY_RELATION_TYPE' uuid value
     *
     * @param pageContents Table of the {@link MultiTree} into the page
     * @param containerUUID container's UUID link with the page
     * @param container container
     * @return true in case of the containerUUId is contains in pageContents
     */
    private boolean doesPageContentsHaveContainer(
            final Table<String, String, Set<PersonalizedContentlet>> pageContents,
            final ContainerUUID containerUUID,
            final Container container) {

        if(pageContents.contains(container.getIdentifier(), containerUUID.getUUID())){
            return true;
        } else if (ContainerUUID.UUID_LEGACY_VALUE.equals(containerUUID.getUUID())) {
            boolean pageContenstContains = pageContents.contains(containerUUID.getIdentifier(), ContainerUUID.UUID_START_VALUE);

            if (!pageContenstContains && container instanceof FileAssetContainer) {
                pageContenstContains = pageContents.contains(container.getIdentifier(), ContainerUUID.UUID_START_VALUE);
            }

            return pageContenstContains;
        } else {
            return false;
        }
    }

    private Container getLiveContainerById(final String containerIdOrPath, final User user, final Template template) throws NotFoundInDbException {

        final LiveContainerFinderByIdOrPathStrategyResolver strategyResolver =
                LiveContainerFinderByIdOrPathStrategyResolver.getInstance();
        final Optional<ContainerFinderByIdOrPathStrategy> strategy           = strategyResolver.get(containerIdOrPath);

        return this.geContainerById(containerIdOrPath, user, template, strategy, strategyResolver.getDefaultStrategy());
    }

    private Container getWorkingContainerById(final String containerIdOrPath, final User user, final Template template) throws NotFoundInDbException {

        final WorkingContainerFinderByIdOrPathStrategyResolver strategyResolver =
                WorkingContainerFinderByIdOrPathStrategyResolver.getInstance();
        final Optional<ContainerFinderByIdOrPathStrategy> strategy           = strategyResolver.get(containerIdOrPath);

        return this.geContainerById(containerIdOrPath, user, template, strategy, strategyResolver.getDefaultStrategy());
    }

    private Container geContainerById(final String containerIdOrPath, final User user, final Template template,
                                      final Optional<ContainerFinderByIdOrPathStrategy> strategy,
                                      final ContainerFinderByIdOrPathStrategy defaultContainerFinderByIdOrPathStrategy) throws NotFoundInDbException  {

        final Supplier<Host> resourceHostSupplier = Sneaky.sneaked(()->APILocator.getTemplateAPI().getTemplateHost(template));

        return strategy.isPresent()?
                strategy.get().apply(containerIdOrPath, user, false, resourceHostSupplier):
                defaultContainerFinderByIdOrPathStrategy.apply(containerIdOrPath, user, false, resourceHostSupplier);
    }

    public Collection<Container> getContainers() {
        return containers;
    }

    public Set<String> getUUID(final String containerId) {
        return pageContents.row(containerId).keySet().stream()
                .filter(uuid -> !ContainerUUID.UUID_DEFAULT_VALUE.equals(uuid))
                .collect(Collectors.toSet());
    }

    public Collection<Contentlet> getContents(
            final Container container,
            final String uniqueId,
            final String personaId,
            final PageMode mode,
            final Long languageId,
            final User user) {

        final String containerId = container.getIdentifier();
        final Set<PersonalizedContentlet> personalizedContentletSet = pageContents.get(containerId, uniqueId);

        return personalizedContentletSet.stream()
                .filter(personalizedContentlet ->  personalizedContentlet.getPersonalization().equals(personaId))
                .map(personalizedContentlet -> getContentlet(mode, languageId, user, personalizedContentlet.getContentletId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Collection<Contentlet> getContents(
            final Container container,
            final String uniqueId,
            final String personaId,
            final PageMode mode,
            final User user) {

        return this.getContents(container, uniqueId, personaId, mode, null, user);
    }


    public Collection<Contentlet> getContents(
            final Container container,
            final String uniqueId,
            final PageMode mode,
            final Long languageId,
            final User user) {

        final String containerId = container.getIdentifier();
        final Set<PersonalizedContentlet> personalizedContentletSet = pageContents.get(containerId, uniqueId);

        return personalizedContentletSet.stream()
                .map(personalizedContentlet -> personalizedContentlet.getContentletId())
                .distinct()
                .map(contentletId -> getContentlet(mode, languageId, user, contentletId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Collection<Contentlet> getContents(
            final Container container,
            final String uniqueId,
            final PageMode mode,
            final User user) {

        return this.getContents(container, uniqueId, mode, null, user);
    }

    public Collection<Contentlet> getContents(
            final PageMode mode,
            final long languageId,
            final User user) {

        return pageContents.values()
                .stream()
                .flatMap(cotentlets -> cotentlets.stream())
                .map(personalizedContentlet -> personalizedContentlet.getContentletId())
                .distinct()
                .map(contentletId -> getContentlet(mode, languageId, user, contentletId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Collection<PersonalizedContentlet> getPersonalizedContents(
            final Container container,
            final String uniqueId) {

        final String containerId = container.getIdentifier();
        return pageContents.get(containerId, uniqueId);
    }

    public Collection<String> getPersonalizations(final Container container, final String uniqueId) {

        final String containerId = container.getIdentifier();
        final Set<PersonalizedContentlet> personalizedContentletSet = pageContents.get(containerId, uniqueId);

        return personalizedContentletSet.stream()
                .map(personalizedContentlet -> personalizedContentlet.getPersonalization())
                .distinct()
                .collect(Collectors.toList());
    }

    public Collection<String> getPersonalizations() {

         return pageContents.values().stream()
                .flatMap(personalizedContentlets -> personalizedContentlets.stream())
                .map(personalizedContentlet -> personalizedContentlet.getPersonalization())
                .distinct()
                .collect(Collectors.toList());
    }

    @Nullable
    private Contentlet getContentlet(
            final PageMode mode,
            final Long languageId,
            final User user,
            final String contentletId) {
        try {
            final Optional<Contentlet> contentletOpt = languageId != null ?
                    APILocator.getContentletAPI().findContentletByIdentifierOrFallback(contentletId, mode.showLive, languageId, user, mode.respectAnonPerms)
                    : Optional.empty();

            return contentletOpt.isPresent()
                    ? contentletOpt.get()
                    : APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(contentletId);

        } catch (final Exception e) {
            return null;
        }
    }
}
