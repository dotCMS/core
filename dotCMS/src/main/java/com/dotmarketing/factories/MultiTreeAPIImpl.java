package com.dotmarketing.factories;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MultiTreeAPIImpl implements MultiTreeAPI {

    final VersionableAPI versionableAPI = APILocator.getVersionableAPI();
    final ContainerAPI   containerAPI   = APILocator.getContainerAPI();
    final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    final User systemUser = APILocator.systemUser();
    final TemplateAPI templateAPI = APILocator.getTemplateAPI();

    @WrapInTransaction
    @Override
    public void saveMultiTrees(final String pageId, final List<MultiTree> multiTrees) throws DotDataException {
        Logger.info(this, String.format("Saving MutiTrees: pageId -> %s multiTrees-> %s", pageId, multiTrees));
        MultiTreeFactory.saveMultiTrees(pageId, multiTrees);
    }

    @WrapInTransaction
    @Override
    public void saveMultiTree(final MultiTree multiTree) throws DotDataException {
        Logger.info(this, String.format("Saving MutiTree: %s", multiTree));
        MultiTreeFactory.saveMultiTree(multiTree);
    }

    @WrapInTransaction
    @Override
    public void deleteMultiTree(final MultiTree multiTree) throws DotDataException {
        Logger.info(this, String.format("Deleting MutiTree: %s", multiTree));
        MultiTreeFactory.deleteMultiTree(multiTree);

    }

    @WrapInTransaction
    @Override
    public void deleteMultiTreeByIdentifier(final Identifier identifier) throws DotDataException {

        final List<MultiTree> multiTrees = MultiTreeFactory.getMultiTrees(identifier);
        if(UtilMethods.isSet(multiTrees)) {

            for(final MultiTree multiTree : multiTrees) {
                MultiTreeFactory.deleteMultiTree(multiTree);
            }
        }
    }

    @CloseDBIfOpened
    @Override
    public Table<String, String, Set<String>>  getPageMultiTrees(final IHTMLPage page, 
                                                                 final boolean liveMode) throws DotDataException, DotSecurityException {

        final Table<String, String, Set<String>> pageContents = HashBasedTable.create();
        final List<MultiTree> multiTres = MultiTreeFactory.getMultiTrees(page.getIdentifier());

        for (final MultiTree multiTree : multiTres) {

            Container container = null;

            try {

                container = (liveMode) ?
                        this.containerAPI.getLiveContainerById(multiTree.getContainer(), systemUser, false) :
                        this.containerAPI.getWorkingContainerById(multiTree.getContainer(), systemUser, false);
                if (container == null && !liveMode) {
                    continue;
                }
            } catch (NotFoundInDbException e) {
                Logger.debug(this, e.getMessage(), e);
                continue;
            }

            Contentlet contentlet = null;
            try {
                contentlet = contentletAPI.findContentletByIdentifierAnyLanguage(multiTree.getContentlet());;
            }catch(DotDataException | DotSecurityException | DotContentletStateException e){
                Logger.warn(this.getClass(), "invalid contentlet on multitree:" + multiTree);
            }
            if(contentlet!=null ) {
                final Set<String> myContents = pageContents.contains(multiTree.getContainer(), multiTree.getRelationType())
                        ? pageContents.get(multiTree.getContainer(), multiTree.getRelationType())
                        : new LinkedHashSet<>();
                if(container != null && myContents.size() < container.getMaxContentlets()) {
                    myContents.add(multiTree.getContentlet());
                }

                pageContents.put(multiTree.getContainer(), multiTree.getRelationType(), myContents);
             };

        }

        this.addEmptyContainers(page, pageContents, liveMode);

        return pageContents;
    }

    private void addEmptyContainers(final IHTMLPage page, Table<String, String, Set<String>> pageContents,
                                    final boolean liveMode) throws DotDataException, DotSecurityException {

        try {
            final TemplateLayout layout = DotTemplateTool.themeLayout(page.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false);
            final List<ContainerUUID> containersUUID = this.templateAPI.getContainersUUID(layout);

            for (final ContainerUUID containerUUID : containersUUID) {

                Container container = null;
                try {
                    container = (liveMode) ?
                            this.containerAPI.getLiveContainerById   (containerUUID.getIdentifier(),      systemUser, false):
                            this.containerAPI.getWorkingContainerById(containerUUID.getIdentifier(), systemUser, false);

                    if(container==null && ! liveMode) {
                        continue;
                    }
                } catch (NotFoundInDbException e) {
                    Logger.debug(this, e.getMessage(), e);
                    continue;
                }


                if (!pageContents.contains(containerUUID.getIdentifier(), containerUUID.getUUID())) {
                    final boolean isLegacyValue = ContainerUUID.UUID_LEGACY_VALUE.equals(containerUUID.getUUID());

                    if (!isLegacyValue || !pageContents.contains(containerUUID.getIdentifier(), ContainerUUID.UUID_START_VALUE)) {
                        pageContents.put(containerUUID.getIdentifier(), containerUUID.getUUID(), new LinkedHashSet<>());
                    }
                }
            }
        } catch (RuntimeException e) {

        }
    }

    @WrapInTransaction
    @Override
    public void updateMultiTree(final String pageId, final String containerId, final String oldRelationType,
                         final String newRelationType) throws DotDataException {
        MultiTreeFactory.updateMultiTree(pageId, containerId, oldRelationType, newRelationType);
    }
}
