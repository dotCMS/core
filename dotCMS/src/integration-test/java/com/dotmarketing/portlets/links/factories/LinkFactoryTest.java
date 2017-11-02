package com.dotmarketing.portlets.links.factories;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
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

        containerAPI = APILocator.getContainerAPI();
        contentletAPI = APILocator.getContentletAPI();
        folderAPI = APILocator.getFolderAPI();
        hostAPI = APILocator.getHostAPI();
        identifierAPI = APILocator.getIdentifierAPI();
        languageAPI = APILocator.getLanguageAPI();
        templateAPI = APILocator.getTemplateAPI();
        userAPI = APILocator.getUserAPI();
        systemUser = userAPI.getSystemUser();
    }

    @Test
    public void testGetLinkChildrenByCondition() throws Exception {

        Container container = null;
        Contentlet contentAsset = null;
        Folder ftest = null;
        Link link = null;
        Template template = null;

        Host host;
        Identifier internalLinkIdentifier;
        long id;
        String containerName, linkStr, page0Str, userId;
        StringBuffer myURL;

        host = hostAPI.findDefaultHost(systemUser, false);
        id = System.currentTimeMillis();
        linkStr = "link" + id;
        userId = systemUser.getUserId();

        try {
            //Create new folder
            ftest = folderAPI.createFolders("/folderTest" + id, host, systemUser, false);
            ftest.setOwner(userId);
            folderAPI.save(ftest, systemUser, false);

            /**
             * Create new container
             */
            container = new Container();
            containerName = "container" + id;

            container.setFriendlyName(containerName);
            container.setTitle(containerName);
            container.setOwner(userId);
            container.setMaxContentlets(5);
            container.setPreLoop("preloop code");
            container.setPostLoop("postloop code");

            List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
            ContainerStructure cs = new ContainerStructure();
            cs.setStructureId(
                    CacheLocator.getContentTypeCache().getStructureByVelocityVarName("SimpleWidget")
                            .getInode());
            cs.setCode("<div><h3>content $!{title}</h3><p>$!{body}</p></div>");
            csList.add(cs);
            container = containerAPI.save(container, csList, host, systemUser, false);
            PublishFactory.publishAsset(container, systemUser, false, false);

            /**
             * Create new template
             */
            String templateBody = "<html><body> #parseContainer('" + container.getIdentifier()
                    + "') </body></html>";
            String templateTitle = "template" + id;

            //Create template
            template = new Template();
            template.setTitle(templateTitle);
            template.setBody(templateBody);
            template.setOwner(systemUser.getUserId());
            template = templateAPI.saveTemplate(template, host, systemUser, false);
            PublishFactory.publishAsset(template, systemUser, false, false);

            //Create new page
            page0Str = "page" + id;
            contentAsset = new Contentlet();
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

            internalLinkIdentifier = identifierAPI.findFromInode(contentAsset.getIdentifier());

            myURL = new StringBuffer();
            if (InodeUtils.isSet(internalLinkIdentifier.getHostId())) {
                myURL.append(host.getHostname());
            }
            myURL.append(internalLinkIdentifier.getURI());

            link = new Link();
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

            List<Link> links = LinkFactory.getLinkChildrenByCondition(ftest, "");

            Assert.assertNotNull(links);
            Assert.assertFalse(links.isEmpty() || links.size() == 0);
            Assert.assertEquals(link.getIdentifier(), links.get(0).getIdentifier());
            Assert.assertEquals(link.getInode(), links.get(0).getInode());
        } finally {
            if (ftest != null) {
                folderAPI.delete(ftest, systemUser, false);
            }

            if (container != null && container.getInode() != null) {
                containerAPI.delete(container, systemUser, false);
            }

            if (template != null && template.getInode() != null) {
                templateAPI.delete(template, systemUser, false);
            }

            if (contentAsset != null && contentAsset.getInode() != null) {
                contentletAPI.delete(contentAsset, systemUser, false);
            }
        }

    }
}
