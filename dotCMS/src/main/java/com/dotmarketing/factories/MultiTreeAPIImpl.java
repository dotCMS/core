package com.dotmarketing.factories;

import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.util.Logger;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;

public class MultiTreeAPIImpl implements MultiTreeAPI {

    final VersionableAPI versionableAPI = APILocator.getVersionableAPI();
    final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    final User systemUser = APILocator.systemUser();
    final TemplateAPI templateAPI = APILocator.getTemplateAPI();

    public void saveMultiTrees(final String pageId, final List<MultiTree> multiTrees) throws DotDataException {
        Logger.info(this, String.format("Saving MutiTrees: pageId -> %s multiTrees-> %s", pageId, multiTrees));
        MultiTreeFactory.saveMultiTrees(pageId, multiTrees);
    }

    public void saveMultiTree(final MultiTree multiTree) throws DotDataException {
        Logger.info(this, String.format("Saving MutiTree: %s", multiTree));
        MultiTreeFactory.saveMultiTree(multiTree);
    }

    public Table<String, String, Set<String>>  getPageMultiTrees(final IHTMLPage page, final boolean liveMode)
            throws DotDataException, DotSecurityException {
        
        final Table<String, String, Set<String>> pageContents = HashBasedTable.create();
        final List<MultiTree> multiTres = MultiTreeFactory.getMultiTrees(page.getIdentifier());

        for (final MultiTree multiTree : multiTres) {
            final Container container = (liveMode) ? (Container) versionableAPI.findLiveVersion(multiTree.getContainer(),
                    systemUser, false)
                    : (Container) versionableAPI.findWorkingVersion(multiTree.getContainer(), systemUser, false);
            if(container==null && ! liveMode) {
                continue;
            }

            Contentlet contentlet = null;
            try {
                contentlet = contentletAPI.findContentletByIdentifier(multiTree.getContentlet(), liveMode, -1,
                        systemUser, false);
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
            TemplateLayout layout = DotTemplateTool.themeLayout(page.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false);
            List<ContainerUUID> containersUUID = this.templateAPI.getContainersUUID(layout);

            for (ContainerUUID containerUUID : containersUUID) {

                final Container container = (liveMode) ? (Container) versionableAPI.findLiveVersion(containerUUID.getIdentifier(),
                        systemUser, false)
                        : (Container) versionableAPI.findWorkingVersion(containerUUID.getIdentifier(), systemUser, false);
                if(container==null && ! liveMode) {
                    continue;
                }

                if (!pageContents.contains(containerUUID.getIdentifier(), containerUUID.getUUID())) {
                    pageContents.put(containerUUID.getIdentifier(), containerUUID.getUUID(), new LinkedHashSet<>());
                }
            }
        } catch (RuntimeException e) {

        }
    }

    public void updateMultiTree(final String pageId, final String containerId, final String oldRelationType,
                         final String newRelationType) throws DotDataException {
        MultiTreeFactory.updateMultiTree(pageId, containerId, oldRelationType, newRelationType);
    }
}
