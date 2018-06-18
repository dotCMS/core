package com.dotcms.rest.api.v1.page;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDB;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.*;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetNotFoundException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Table;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.jetbrains.annotations.NotNull;

/**
 * Provides the utility methods that interact with HTML Pages in dotCMS. These methods are used by
 * the Page REST end-point.
 *
 * @author Will Ezell
 * @author Jose Castro
 * @version 4.2
 * @since Oct 6, 2017
 */
public class PageResourceHelper implements Serializable {

    private static final long serialVersionUID = 296763857542258211L;

    private final HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    private final HTMLPageAssetAPI htmlPageAssetAPI = APILocator.getHTMLPageAssetAPI();
    private final TemplateAPI templateAPI = APILocator.getTemplateAPI();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private final HostAPI hostAPI = APILocator.getHostAPI();
    private final LanguageAPI langAPI = APILocator.getLanguageAPI();
    private final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
    private final UserAPI userAPI = APILocator.getUserAPI();
    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    /**
     * Private constructor
     */
    private PageResourceHelper() {

    }

    @WrapInTransaction
    public void saveContent(final String pageId, final List<PageContainerForm.ContainerEntry> containerEntries) throws DotDataException {

        final List<MultiTree> multiTrees = new ArrayList<>();

        for (final PageContainerForm.ContainerEntry containerEntry : containerEntries) {
            int i = 0;
            final  List<String> contentIds = containerEntry.getContentIds();

            for (final String contentletId : contentIds) {
                final MultiTree multiTree = new MultiTree().setContainer(containerEntry.getContainerId())
                        .setContentlet(contentletId)
                        .setRelationType(containerEntry.getContainerUUID())
                        .setTreeOrder(i++)
                        .setHtmlPage(pageId);

                multiTrees.add(multiTree);
            }
        }

        multiTreeAPI.saveMultiTrees(pageId, multiTrees);
    }

    public void saveMultiTree(final String containerId,
                              final String contentletId,
                              final int order,
                              final String uid,
                              final Contentlet page) throws DotDataException {

        final MultiTree multiTree = new MultiTree().setContainer(containerId)
                .setContentlet(contentletId)
                .setRelationType(uid)
                .setTreeOrder(order)
                .setHtmlPage(page.getIdentifier());

        multiTreeAPI.saveMultiTree(multiTree);
    }


    /**
     * Provides a singleton instance of the {@link PageResourceHelper}
     */
    private static class SingletonHolder {
        private static final PageResourceHelper INSTANCE = new PageResourceHelper();
    }

    /**
     * Returns a singleton instance of this class.
     *
     * @return A single instance of this class.
     */
    public static PageResourceHelper getInstance() {
        return PageResourceHelper.SingletonHolder.INSTANCE;
    }

    @WrapInTransaction
    public HTMLPageAsset saveTemplate(final User user, final HTMLPageAsset htmlPageAsset, final PageForm pageForm)

            throws BadRequestException, DotDataException, DotSecurityException {

        try {
            final Template templateSaved = this.saveTemplate(htmlPageAsset, user, pageForm);

            final String templateId = htmlPageAsset.getTemplateId();

            Contentlet contentlet = htmlPageAsset;

            if (!templateId.equals( templateSaved.getIdentifier() )) {
                contentlet = this.contentletAPI.checkout(htmlPageAsset.getInode(), user, false);
                contentlet.setStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD, templateSaved.getIdentifier());
                contentlet = this.contentletAPI.checkin(contentlet, user, false);
            }

            return contentlet instanceof  HTMLPageAsset ?
                    (HTMLPageAsset) contentlet :
                    this.htmlPageAssetAPI.fromContentlet(contentlet);
        } catch (BadRequestException | DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    @NotNull
    public IHTMLPage getPage(User user, String pageId) throws DotDataException, DotSecurityException {
        try {
            final Contentlet page = this.contentletAPI.findContentletByIdentifier(pageId, false,
                    langAPI.getDefaultLanguage().getId(), user, false);

            if (page == null) {
                throw new HTMLPageAssetNotFoundException(pageId);
            }

            return this.htmlPageAssetAPI.fromContentlet(page);
        }catch (DotContentletStateException e) {
            throw new HTMLPageAssetNotFoundException(pageId, e);
        }

    }

    @WrapInTransaction
    public Template saveTemplate(final IHTMLPage page, final User user, final PageForm pageForm)
            throws BadRequestException, DotDataException, DotSecurityException {

        
        try {
            final Host host = getHost(pageForm.getHostId(), user);
            final User systemUser = userAPI.getSystemUser();
            final Template template = getTemplate(page, systemUser, pageForm);
            final boolean hasPermission = template.isAnonymous() ?
                    permissionAPI.doesUserHavePermission(page, PermissionLevel.EDIT.getType(), user) :
                    permissionAPI.doesUserHavePermission(template, PermissionLevel.EDIT.getType(), user);

            if (!hasPermission) {
                throw new DotSecurityException("The user doesn't have permission to EDIT");
            }

            template.setDrawed(true);

            updateMultiTrees(page, pageForm);

            return this.templateAPI.saveTemplate(template, host, user, false);
        } catch (BadRequestException | DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    protected void updateMultiTrees(IHTMLPage page, PageForm pageForm) throws DotDataException, DotSecurityException {
        final Table<String, String, Set<String>> pageContents = multiTreeAPI.getPageMultiTrees(page, false);

        for (final String containerId : pageContents.rowKeySet()) {
            for (final String uniqueId : pageContents.row(containerId).keySet()) {
                final Map<String, Set<String>> row = pageContents.row(containerId);
                final Set<String> contents = row.get(uniqueId);

                if (!contents.isEmpty()) {
                    final String newUUID = getNewUUID(pageForm, containerId, uniqueId);

                    try {
                        if (newUUID != null && !newUUID.equals(uniqueId)) {
                            multiTreeAPI.updateMultiTree(page.getIdentifier(), containerId, uniqueId, newUUID);
                        }
                    } catch (DotDataException e) {
                        Logger.error(this.getClass(), "Exception on saveTemplate exception message: " +
                                e.getMessage(), e);
                    }
                }
            }
        }
    }

    private String getNewUUID(final PageForm pageForm, final String containerId, final String uniqueId) {
        if (ContainerUUID.UUID_DEFAULT_VALUE.equals(uniqueId)) {
            return pageForm.getNewlyContainerUUID(containerId);
        } else {
            ContainerUUIDChanged change = pageForm.getChange(containerId, uniqueId);
            return change != null ? change.getNew().getUUID() : ContainerUUID.UUID_DEFAULT_VALUE;
        }
    }

    public Template saveTemplate(final User user, final PageForm pageForm)
            throws BadRequestException, DotDataException, DotSecurityException, IOException {
        return this.saveTemplate(null, user, pageForm);
    }

    private Template getTemplate(final IHTMLPage page, final User user, final PageForm form)
            throws DotDataException, DotSecurityException {

        final Template oldTemplate = this.templateAPI.findWorkingTemplate(page.getTemplateId(), user, false);
        final Template saveTemplate;

        if (UtilMethods.isSet(oldTemplate) && (!form.isAnonymousLayout() || oldTemplate.isAnonymous())) {
            saveTemplate = oldTemplate;
        } else {
            saveTemplate = new Template();
        }

        saveTemplate.setTitle(form.getTitle());
        saveTemplate.setTheme((form.getThemeId()==null) ? oldTemplate.getTheme() : form.getThemeId());
        saveTemplate.setDrawedBody(form.getLayout());
        saveTemplate.setDrawed(true);
        
        return saveTemplate;
    }

    private Host getHost(final String hostId, final User user) {
        try {
            return UtilMethods.isSet(hostId) ? hostAPI.find(hostId, user, false) :
                        hostWebAPI.getCurrentHost(HttpServletRequestThreadLocal.INSTANCE.getRequest());
        } catch (DotDataException | DotSecurityException | PortalException | SystemException e) {
            throw new DotRuntimeException(e);
        }
    }
}
