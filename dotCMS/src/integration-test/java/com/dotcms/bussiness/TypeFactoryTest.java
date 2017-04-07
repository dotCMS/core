package com.dotcms.bussiness;

import com.dotcms.IntegrationTestBase;
import com.dotcms.business.TypeDAO;
import com.dotcms.business.TypeFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.util.UtilMethods;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test for {@link com.dotcms.business.TypeDAO}
 * @author jsanca
 */
public class TypeFactoryTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testGetInodeNull() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String  inodeId = null;

        final Class clazz = typeDAO.getInodeType(inodeId);

        assertNull("The class returned for null inodeid should be null!", clazz);
    } // testGetInodeCategoryType.

    @Test
    public void testGetInodeCategoryType() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String  inodeId = "20cdf9f1-bf66-4303-92e0-8822f8081cfc";

        final Class categoryClass = typeDAO.getInodeType(inodeId);

        assertNotNull("Category Class for " + inodeId + ", does not exists!", categoryClass);
        assertTrue   ("The class: " + categoryClass + ", is not a Category", Category.class.isAssignableFrom(categoryClass));
    } // testGetInodeCategoryType.


    @Test
    public void testGetInodeContainerType() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String  inodeId = "0ba8735d-bee3-4216-a05f-a8743967d28a";

        final Class containerClass = typeDAO.getInodeType(inodeId);

        assertNotNull("Container Class for " + inodeId + ", does not exists!", containerClass);
        assertTrue   ("The class: " + containerClass + ", is not a Container", Container.class.isAssignableFrom(containerClass));
    } // testGetInodeContainerType.

    @Test
    public void testGetInodeContentletType() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String  inodeId = "006de26b-376c-495a-9f7d-913a578b033d";

        final Class contentletClass = typeDAO.getInodeType(inodeId);

        assertNotNull("Contentlet Class for " + inodeId + ", does not exists!", contentletClass);
            assertTrue   ("The class: " + contentletClass + ", is not a Contentlet", Contentlet.class.isAssignableFrom(contentletClass));
    } // testGetInodeContentletType.

    @Test
    public void testGetInodeFieldType() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String  inodeId = "000ef75f-59e2-4c89-9cec-247e371ecd77";

        final Class fieldClass = typeDAO.getInodeType(inodeId);

        assertNotNull("Field Class for " + inodeId + ", does not exists!", fieldClass);
        assertTrue   ("The class: " + fieldClass + ", is not a Field", Field.class.isAssignableFrom(fieldClass));
    } // testGetInodeFieldType.

    @Test
    public void testGetInodeFolderType() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String  inodeId = "008dab22-8bc3-4eb2-93a0-79e3d5d0a4ab";

        final Class folderClass = typeDAO.getInodeType(inodeId);

        assertNotNull("Folder Class for " + inodeId + ", does not exists!", folderClass);
        assertTrue   ("The class: " + folderClass + ", is not a Folder ", Folder.class.isAssignableFrom(folderClass));
    } // testGetInodeFolderType.

    @Test
    public void testGetInodeLinksType() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String  inodeId = "290a232f-f1de-4618-b4a3-f100e2ec0144";

        final Class linkClass = typeDAO.getInodeType(inodeId);

        assertNotNull("Link Class for " + inodeId + ", does not exists!", linkClass);
        assertTrue   ("The class: " + linkClass + ", is not a Link ", Link.class.isAssignableFrom(linkClass));
    } // testGetInodeLinksType.


    @Test
    public void testGetInodeRelationshipType() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String  inodeId = "244c9fdf-1db8-4768-aede-8f83ea5778c2";

        final Class relationshipClass = typeDAO.getInodeType(inodeId);

        assertNotNull("Relationship Class for " + inodeId + ", does not exists!", relationshipClass);
        assertTrue   ("The class: " + relationshipClass + ", is not a Relationship ", Relationship.class.isAssignableFrom(relationshipClass));
    } // testGetInodeRelationshipType.

    @Test
    public void testGetInodeStructureType() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String inodeId = "074f3880-e771-4084-9c04-8db74fdbba10";

        final Class structureClass = typeDAO.getInodeType(inodeId);

        assertNotNull("Structure Class for " + inodeId + ", does not exists!", structureClass);
        assertTrue   ("The class: " + structureClass + ", is not a Structure ", Structure.class.isAssignableFrom(structureClass));
    } // testGetInodeStructureType.

    @Test
    public void testGetInodeTemplateType() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String inodeId = "10baf501-ffbe-4cec-9e52-cfee23e43c87";

        final Class templateClass = typeDAO.getInodeType(inodeId);

        assertNotNull("Template Class for " + inodeId + ", does not exists!", templateClass);
        assertTrue   ("The class: " + templateClass + ", is not a Template ", Template.class.isAssignableFrom(templateClass));
    } // testGetInodeTemplateType.

    @Test
    public void testGetInodeUserProxyType() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String inodeId = "07000663-738b-4cfd-990c-364b49f741f9";

        final Class userProxyClass = typeDAO.getInodeType(inodeId);

        assertNotNull("User Proxy Class for " + inodeId + ", does not exists!", userProxyClass);
        assertTrue   ("The class: " + userProxyClass + ", is not an User Proxy ", UserProxy.class.isAssignableFrom(userProxyClass));
    } // testGetInodeUserProxyType.

    @Test
    public void testGetInodeVirtualLinkType() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String inodeId = "4b8c55de-a9c1-4fd6-ab7b-cb66d574ac6b";

        final Class virtualLinkClass = typeDAO.getInodeType(inodeId);

        assertNotNull("Virtual Link Class for " + inodeId + ", does not exists!", virtualLinkClass);
        assertTrue   ("The class: " + virtualLinkClass + ", is not Virtual Link ", VirtualLink.class.isAssignableFrom(virtualLinkClass));
    } // testGetInodeVirtualLinkType.


    @Test
    public void testGetIdentifierTypeNull() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String identifier = null;

        final Class clazz = typeDAO.getIdentifierType(identifier);

        assertNull("The class returned for null identifier should be null!", clazz);
    } // testGetIdentifierTypeNull.

    @Test
    public void testGetIdentifierTypeContainers() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String identifier = "3f0255e8-b45d-46ea-8bb7-eb6597db4c1e";

        final Class clazz = typeDAO.getIdentifierType(identifier);

        assertNotNull("Container Class for " + identifier + ", does not exists!", clazz);
        assertTrue   ("The class: " + clazz + ", is not Container ", Container.class.isAssignableFrom(clazz));
    } // testGetIdentifierTypeContainers.

    @Test
    public void testGetIdentifierTypeContentlet() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String identifier = "0048140d-acac-4008-916a-343a3a10a191";

        final Class clazz = typeDAO.getIdentifierType(identifier);

        assertNotNull("Contentlet Class for " + identifier + ", does not exists!", clazz);
        assertTrue   ("The class: " + clazz + ", is not Contentlet ", Contentlet.class.isAssignableFrom(clazz));
    } // testGetIdentifierTypeContentlet.

    @Test
    public void testGetIdentifierTypeFolder() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String identifier = "01a85977-1dc9-43af-8af7-1cd3a39907f3";

        final Class clazz = typeDAO.getIdentifierType(identifier);

        assertNotNull("Folder Class for " + identifier + ", does not exists!", clazz);
        assertTrue   ("The class: " + clazz + ", is not Folder ", Folder.class.isAssignableFrom(clazz));
    } // testGetIdentifierTypeFolder.

    @Test
    public void testGetIdentifierTypeLinks() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String identifier = "14343a01-11c1-4d98-a2a1-2fc8cfcd92ea";

        final Class clazz = typeDAO.getIdentifierType(identifier);

        assertNotNull("Links Class for " + identifier + ", does not exists!", clazz);
        assertTrue   ("The class: " + clazz + ", is not Links ", Link.class.isAssignableFrom(clazz));
    } // testGetIdentifierTypeLinks.

    @Test
    public void testGetIdentifierTypeTemplate() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String identifier = "1763fa6e-91c0-464e-8b16-9a25d7ae6ce5";

        final Class clazz = typeDAO.getIdentifierType(identifier);

        assertNotNull("Template Class for " + identifier + ", does not exists!", clazz);
        assertTrue   ("The class: " + clazz + ", is not Template ", Template.class.isAssignableFrom(clazz));
    } // testGetIdentifierTypeTemplate.

    @Test
    public void testFindFirstInodeByIdentifierTemplate() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String identifier = "1763fa6e-91c0-464e-8b16-9a25d7ae6ce5";

        final Inode template = typeDAO.findFirstInodeByIdentifier(identifier);

        assertNotNull("Template for " + template + ", does not exists!", template);
        assertTrue   ("The identifier: " + template + ", is not Template ", template instanceof Template);
        assertEquals ("Title should be Intranet - 1 Column", "Intranet - 1 Column", template.getTitle());
    } // testGetIdentifierTypeTemplate.

    @Test
    public void testFindFirstInodeByIdentifierLinks() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String identifier = "14343a01-11c1-4d98-a2a1-2fc8cfcd92ea";

        final Inode link = typeDAO.findFirstInodeByIdentifier(identifier);

        assertNotNull("Link for " + link + ", does not exists!", link);
        assertTrue   ("The identifier: " + link + ", is not Template ", link instanceof Link);
        assertEquals ("Title should be /blogs/", "/blogs/", Link.class.cast(link).getUrl());
    } // testFindFirstInodeByIdentifierLinks.

    @Test
    public void testFindFirstInodeByIdentifierNull() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String identifier = null;

        final Inode inode = typeDAO.findFirstInodeByIdentifier(identifier);

        assertNull("Inode for null identifier should be null", inode);
    } // testFindFirstInodeByIdentifierNull.


    @Test
    public void testFindByInodeCategory() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String  inodeId = "20cdf9f1-bf66-4303-92e0-8822f8081cfc";

        final Category category = typeDAO.findByInode(inodeId, Category.class);

        assertNotNull("Category for " + inodeId + ", does not exists!", category);
        assertTrue   ("The class: " + category + ", is not a Category", Category.class.isInstance(category));
        assertEquals("The Category Name should be Wealth Management", "Wealth Management", category.getCategoryName());

        final Inode inode = typeDAO.findByInode(inodeId);

        assertNotNull("Category for " + inodeId + ", does not exists!", inode);
        assertTrue   ("The class: " + category + ", is not a Category", Category.class.isInstance(inode));
        assertEquals("The Category Name should be Wealth Management", "Wealth Management", Category.class.cast(inode).getCategoryName());
    } // testFindByInodeCategory.

    @Test
    public void testFindByInodeContainer() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String  inodeId = "0ba8735d-bee3-4216-a05f-a8743967d28a";

        final Container container = typeDAO.findByInode(inodeId, Container.class);

        assertNotNull("Container for " + inodeId + ", does not exists!", container);
        assertTrue   ("The class: " + container + ", is not a Container", Container.class.isInstance(container));
        assertEquals("The Container title should be Small Column (sm-1)", "Small Column (sm-1)", container.getTitle());

        final Inode inode = typeDAO.findByInode(inodeId);

        assertNotNull("Container for " + inodeId + ", does not exists!", inode);
        assertTrue   ("The class: " + container + ", is not a Container", Container.class.isInstance(inode));
        assertEquals("The Container title should be Small Column (sm-1)", "Small Column (sm-1)", Container.class.cast(inode).getTitle());
    } // testFindByInodeContainer.


    @Test
    public void testFindByInodeNull() throws Exception {

        final TypeDAO typeDAO = TypeFactory.getInstance().getTypeDAO();
        final String  inodeId = null;

        final Container container = typeDAO.findByInode(inodeId, Container.class);

        assertTrue("Container for null inode id should not exists", "".equals(container.getInode()));

        final Inode inode = typeDAO.findByInode(inodeId);

        assertTrue("INode for null inode id should not exists", "".equals(container.getInode()));
    } // testFindByInodeContainer.



} // E:O:F:TypeFactoryTest.
