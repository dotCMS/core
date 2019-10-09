package com.dotmarketing.portlets.links.factories;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class LinkFactoryTest extends IntegrationTestBase {

    private static ContainerAPI containerAPI;
    private static ContentletAPI contentletAPI;
    private static FolderAPI folderAPI;
    private static HostAPI hostAPI;
    private static IdentifierAPI identifierAPI;
    private static LanguageAPI languageAPI;
    private static TemplateAPI templateAPI;
    private static User systemUser;
    private static UserAPI userAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        containerAPI  = APILocator.getContainerAPI();
        contentletAPI = APILocator.getContentletAPI();
        folderAPI     = APILocator.getFolderAPI();
        hostAPI       = APILocator.getHostAPI();
        identifierAPI = APILocator.getIdentifierAPI();
        languageAPI   = APILocator.getLanguageAPI();
        templateAPI   = APILocator.getTemplateAPI();
        userAPI       = APILocator.getUserAPI();
        systemUser    = userAPI.getSystemUser();
    }

    @Test
    public void testGetLinkChildrenByCondition() throws Exception {

        String containerName, linkStr, page0Str, userId;

        Host host = hostAPI.findDefaultHost(systemUser, false);
        long id = System.currentTimeMillis();
        linkStr = "link" + id;
        userId = systemUser.getUserId();

        final ContentType widgetContentType = TestDataUtils.getWidgetLikeContentType();

        //Create new folder
        Folder ftest = folderAPI.createFolders("/folderTest" + id, host, systemUser, false);
        ftest.setOwner(userId);
        folderAPI.save(ftest, systemUser, false);

        /**
         * Create new container
         */
        Container container = new Container();
        containerName = "container" + id;

        container.setFriendlyName(containerName);
        container.setTitle(containerName);
        container.setOwner(userId);
        container.setMaxContentlets(5);
        container.setPreLoop("preloop code");
        container.setPostLoop("postloop code");

        final List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
        final ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(widgetContentType.inode());
        cs.setCode("<div><h3>content $!{title}</h3><p>$!{body}</p></div>");
        csList.add(cs);
        container = containerAPI.save(container, csList, host, systemUser, false);
        PublishFactory.publishAsset(container, systemUser, false, false);

        /**
         * Create new template
         */
        final String templateBody = "<html><body> #parseContainer('" + container.getIdentifier()
                + "') </body></html>";
        final String templateTitle = "template" + id;

        //Create template
        Template template = new Template();
        template.setTitle(templateTitle);
        template.setBody(templateBody);
        template.setOwner(systemUser.getUserId());
        template.setDrawedBody(templateBody);
        template = templateAPI.saveTemplate(template, host, systemUser, false);
        PublishFactory.publishAsset(template, systemUser, false, false);

        //Create new page
        page0Str = "page" + id;
        Contentlet contentAsset = new Contentlet();
        contentAsset
                .setContentTypeId(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
        contentAsset.setHost(host.getIdentifier());
        contentAsset.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, page0Str);
        contentAsset.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, page0Str);
        contentAsset.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, page0Str);
        contentAsset.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
        contentAsset.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
        contentAsset.setLanguageId(languageAPI.getDefaultLanguage().getId());
        contentAsset.setFolder(ftest.getInode());
        contentAsset = contentletAPI.checkin(contentAsset, systemUser, false);
        contentletAPI.publish(contentAsset, systemUser, false);

        Identifier internalLinkIdentifier = identifierAPI
                .findFromInode(contentAsset.getIdentifier());

        StringBuffer myURL = new StringBuffer();
        if (InodeUtils.isSet(internalLinkIdentifier.getHostId())) {
            myURL.append(host.getHostname());
        }
        myURL.append(internalLinkIdentifier.getURI());

        Link link = new Link();
        link.setTitle(linkStr);
        link.setFriendlyName(linkStr);
        link.setParent(ftest.getInode());
        link.setTarget("_blank");
        link.setOwner(userId);
        link.setModUser(userId);
        link.setLinkType(Link.LinkType.INTERNAL.toString());
        link.setInternalLinkIdentifier(internalLinkIdentifier.getId());
        link.setProtocal("http://");
        link.setUrl(myURL.toString());
        WebAssetFactory.createAsset(link, userId, ftest);

        final List<Link> links = LinkFactory.getLinkChildrenByCondition(ftest, "");

        Assert.assertNotNull(links);
        Assert.assertFalse(links.isEmpty());

        Link result = links.get(0);
        Assert.assertEquals(link.getIdentifier(), result.getIdentifier());
        Assert.assertEquals(link.getInode(), result.getInode());
        Assert.assertTrue(link.getOwner() != null && link.getOwner().equals(result.getOwner()));

    }

}