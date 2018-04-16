package com.dotcms.rest.api.v1.page;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
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
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
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


    /**
     * Private constructor
     */
    private PageResourceHelper() {

    }

    @WrapInTransaction
    public void saveContent(final String pageId, final List<PageContainerForm.ContainerEntry> containerEntries) throws DotDataException {

        MultiTreeFactory.deleteMultiTreeByParent(pageId);

        final List<MultiTree> multiTres = new ArrayList<>();

        for (final PageContainerForm.ContainerEntry containerEntry : containerEntries) {
            int i = 0;
            final  List<String> contentIds = containerEntry.getContentIds();

            for (final String contentletId : contentIds) {
                final MultiTree multiTree = new MultiTree().setContainer(containerEntry.getContainerId())
                        .setContentlet(contentletId)
                        .setRelationType(containerEntry.getContainerUUID())
                        .setTreeOrder(i++)
                        .setHtmlPage(pageId);

                multiTres.add(multiTree);
            }
        }

        if (!multiTres.isEmpty()) {
            multiTreeAPI.saveMultiTrees(pageId, multiTres);
        }
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

    public Template saveTemplate(final User user, IHTMLPage htmlPageAsset, final PageForm pageForm)

            throws BadRequestException, DotDataException, DotSecurityException, IOException {

        try {
            
            
            Template templateSaved = this.saveTemplate(htmlPageAsset, user, pageForm);

            String templateId = htmlPageAsset.getTemplateId();

            if (!templateId.equals( templateSaved.getIdentifier() )) {
                htmlPageAsset.setInode(null);
                htmlPageAsset.setTemplateId(templateSaved.getIdentifier());
                this.contentletAPI.checkin((HTMLPageAsset)htmlPageAsset, user, false);
            }

            return templateSaved;
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    @NotNull
    public IHTMLPage getPage(User user, String pageId) throws DotDataException, DotSecurityException {
        final Contentlet page = this.contentletAPI.findContentletByIdentifier(pageId, false,
                langAPI.getDefaultLanguage().getId(), user, false);

        if (page == null) {
            throw new NotFoundException("An error occurred when proccessing the JSON request");
        }

        return this.htmlPageAssetAPI.fromContentlet(page);

    }

    public Template saveTemplate(final IHTMLPage page, final User user, final PageForm pageForm)
            throws BadRequestException, DotDataException, DotSecurityException, IOException {

        
        try {
            final Host host = getHost(pageForm.getHostId(), user);
            Template template = getTemplate(page, user, pageForm);
            template.setDrawed(true);
            return this.templateAPI.saveTemplate(template, host, user, false);
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public Template saveTemplate(final User user, final PageForm pageForm)
            throws BadRequestException, DotDataException, DotSecurityException, IOException {
        return this.saveTemplate(null, user, pageForm);
    }

    private Template getTemplate(IHTMLPage page, User user, PageForm form) throws DotDataException, DotSecurityException {

        final Template oldTemplate = this.templateAPI.findWorkingTemplate(page.getTemplateId(), user, false);
        Template saveTemplate;

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

    public Contentlet getContentlet(final User user, final PageMode mode, final Language id, final String contentletId)
            throws DotDataException, DotSecurityException {

        final ShortyId contentShorty = APILocator.getShortyAPI()
                .getShorty(contentletId)
                .orElseGet(() -> {
                    throw new ResourceNotFoundException("Can't find contentlet:" + contentletId);
                });

        return APILocator.getContentletAPI()
                .findContentletByIdentifier(contentShorty.longId, mode.showLive, id.getId(), user, mode.isAdmin);
    }

    public Container getContainer(final String containerId, final User user, final PageMode mode)
            throws DotDataException, DotSecurityException {

        final ShortyId containerShorty = APILocator.getShortyAPI()
                .getShorty(containerId)
                .orElseGet(() -> {
                    throw new ResourceNotFoundException("Can't find Container:" + containerId);
                });
        return (mode.showLive) ? (Container) APILocator.getVersionableAPI()
                .findLiveVersion(containerShorty.longId, user, !mode.isAdmin)
                : (Container) APILocator.getVersionableAPI()
                .findWorkingVersion(containerShorty.longId, user, !mode.isAdmin);
    }
}
